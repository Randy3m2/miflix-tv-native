package com.miflix.native2.data;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;

/** Ephemeral LAN manifest input. Private URL is POST-only and never echoed. */
public final class AddonManifestServerRP128 implements AutoCloseable {
    public interface Save { boolean save(String key); }
    private final Inet4Address address;
    private final int prefix;
    private final ServerSocket socket;
    private final String ticket=UUID.randomUUID().toString()+UUID.randomUUID().toString();
    private final long until=System.currentTimeMillis()+120000L;
    private final boolean loopbackOnly;
    private final ThreadPoolExecutor workers=new ThreadPoolExecutor(2,2,0L,TimeUnit.SECONDS,new ArrayBlockingQueue<>(8));
    private final String formToken=UUID.randomUUID().toString()+UUID.randomUUID().toString();
    private final Save save;
    private boolean received;
    private volatile boolean closed;
    public AddonManifestServerRP128(Inet4Address address,int prefix,Save save) throws IOException {
        this(address,prefix,save,false);
    }
    /** Package-private fixture: only binds localhost; cannot relax LAN admission. */
    static AddonManifestServerRP128 loopbackForTests(Save save) throws IOException {
        return new AddonManifestServerRP128((Inet4Address)InetAddress.getByName("127.0.0.1"),8,save,true);
    }
    private AddonManifestServerRP128(Inet4Address address,int prefix,Save save,boolean loopbackOnly) throws IOException {
        if(prefix<8 || prefix>30 || (loopbackOnly?!address.isLoopbackAddress():!privateAddress(address.getAddress()))) throw new IOException("LAN required");
        this.address=address;this.prefix=prefix;this.save=save;this.loopbackOnly=loopbackOnly;
        socket=new ServerSocket();socket.bind(new InetSocketAddress(address,0),8);
        Thread thread=new Thread(this::accept,"brunio-addon-manifest");thread.setDaemon(true);thread.start();
    }
    static boolean privateAddress(byte[] ip) {
        return ip.length==4 && ((ip[0]&255)==10 || (ip[0]&255)==172 && (ip[1]&255)>=16 && (ip[1]&255)<=31 || (ip[0]&255)==192 && (ip[1]&255)==168);
    }
    static boolean acceptsPeer(byte[] ip) {return LocalQrNetworkRP129.acceptsPeer(ip);}
    static boolean sameLan(byte[] own,byte[] peer,int prefix) {
        if(!privateAddress(own)||!privateAddress(peer)||prefix<8||prefix>30) return false;
        for(int bit=0;bit<prefix;bit++) if((own[bit/8]&(1<<(7-bit%8)))!=(peer[bit/8]&(1<<(7-bit%8)))) return false;
        return true;
    }
    public long getUntil(){return until;}
    public String getBase(){return "http://"+address.getHostAddress()+":"+socket.getLocalPort();}
    public String getUrl(){return getBase()+"/add/"+ticket;}
    public static boolean valid(String key) {
        if(key==null || key.length()>8192) return false;
        try {AddonManifestRP126.normalize(key);return true;}
        catch(IllegalArgumentException e) {return false;}
    }
    private void accept(){while(!closed)try {
        Socket s=socket.accept();
        if(loopbackOnly?!s.getInetAddress().isLoopbackAddress():!acceptsPeer(s.getInetAddress().getAddress())) {s.close();continue;}
        try {workers.execute(()->handle(s));} catch(RejectedExecutionException e){s.close();}
    }catch(IOException e){if(!closed)close();}}
    private static String line(InputStream in,int max) throws IOException {
        ByteArrayOutputStream b=new ByteArrayOutputStream();int c;
        while((c=in.read())!=-1) {if(c=='\n')return b.toString("UTF-8").replace("\r","");if(b.size()>=max)throw new IOException();b.write(c);}
        throw new EOFException();
    }
    private void handle(Socket client){try(Socket s=client) {
        s.setSoTimeout(4000);InputStream in=s.getInputStream();String[] request=line(in,2048).split(" ");if(request.length!=3)return;
        Map<String,String> headers=new HashMap<>();int total=0;
        while(true) {String l=line(in,4096);total+=l.length();if(total>8192)return;if(l.isEmpty())break;int colon=l.indexOf(':');if(colon<1)return;headers.put(l.substring(0,colon).toLowerCase(Locale.ROOT),l.substring(colon+1).trim());}
        if(!Objects.equals(headers.get("host"),address.getHostAddress()+":"+socket.getLocalPort())) {reply(s,403,"Link unavailable.");return;}
        if(closed || System.currentTimeMillis()>=until) {reply(s,410,"QR expired / QR vencido. Generate a new QR on the TV.");return;}
        if(request[0].equals("GET") && request[1].equals("/add/"+ticket)) {reply(s,200,form(ticket));return;}
        if(!request[0].equals("POST") || !request[1].equals("/save/"+ticket)) {reply(s,404,"Not found");return;}
        // Chrome may send Origin: null (or omit it) with a no-referrer form.
        // In every case require the independent secret embedded in this QR's form.
        String origin=value(headers,"origin","");
        if(!origin.isEmpty() && !origin.equals("null") && !origin.equals(getBase())) {reply(s,403,page("Formulario no válido / Invalid form","Scan the QR on the TV again."));return;}
        if(!value(headers,"content-type","").startsWith("application/x-www-form-urlencoded")) {reply(s,415,"Invalid form");return;}
        int length=Integer.parseInt(value(headers,"content-length","0"));if(length<1 || length>32768) {reply(s,413,"Invalid form size");return;}
        byte[] body=new byte[length];int offset=0;while(offset<length) {int n=in.read(body,offset,length-offset);if(n<0)throw new EOFException();offset+=n;}
        Map<String,String> fields=new HashMap<>();for(String part:new String(body,StandardCharsets.UTF_8).split("&")) {int at=part.indexOf('=');if(at>=0)fields.put(URLDecoder.decode(part.substring(0,at),"UTF-8"),URLDecoder.decode(part.substring(at+1),"UTF-8"));}
        if(!acceptsForm(getBase(),origin,formToken,value(fields,"csrf",""))) {reply(s,403,page("Formulario no válido / Invalid form","Scan the QR on the TV again."));return;}
        String key=value(fields,"manifest","").trim();
        if(!valid(key)) {reply(s,400,page("Enlace no válido / Invalid link", "Use the configured manifest.json installation URL."));return;}
        synchronized(this) {
            if(received) {reply(s,409,page("Enlace ya recibido / Link already received","Generate a new QR on the TV to send another link."));return;}
            if(closed || !save.save(key)) {reply(s,409,page("No se pudo recibir / Could not receive","Keep Settings and this QR open on the TV, then try again."));return;}
            received=true;
        }
        reply(s,200,page("Enlace recibido / Link received","En la TV, vuelve al formulario para comprobar el add-on e instalarlo. / On the TV, return to the form to check the add-on and install it."));
    }catch(Exception ignored){/* Do not log API keys, tokens or form contents. */}}
    public static boolean acceptsForm(String base,String origin,String expected,String submitted) {
        return expected!=null && !expected.isEmpty() && submitted!=null
            && (origin==null || origin.isEmpty() || origin.equals("null") || origin.equals(base))
            && MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),submitted.getBytes(StandardCharsets.UTF_8));
    }
    private static String value(Map<String,String> map,String key,String fallback){String v=map.get(key);return v==null?fallback:v;}
    private static String escape(String text){return text.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");}
    private static String page(String title,String text){return shell("<h1>"+escape(title)+"</h1><p>"+escape(text)+"</p>");}
    private static String shell(String body){return "<!doctype html><html lang='es'><meta charset='utf-8'><meta name='viewport' content='width=device-width,initial-scale=1'><title>BruniO · Add-ons</title><style>body{margin:0;background:#080808;color:#fff;font:16px system-ui}main{max-width:480px;margin:6vh auto;padding:28px}small,p{color:#aaa;line-height:1.6}h1{font-size:29px}label{display:block;margin-top:22px}input,button{box-sizing:border-box;width:100%;padding:16px;margin-top:9px;border-radius:14px;border:1px solid #444;background:#171717;color:white;font:inherit}button{background:#eee;color:#111;font-weight:700;margin-top:28px}input:focus{outline:2px solid #b4a0d4}</style><main><small>BRUNIO · ADD-ONS</small>"+body+"</main></html>";}
    private String form(String ticket){return shell("<h1>Tus add-ons, en BruniO</h1><p>Pega el enlace manifest.json que te da la página del add-on.<br>Paste the manifest.json installation link.</p><form method='post' action='/save/"+ticket+"'><input type='hidden' name='csrf' value='"+formToken+"'><label>URL del add-on<input name='manifest' type='text' required maxlength='8192' autocomplete='off' autocapitalize='none' spellcheck='false' placeholder='https://…/manifest.json'></label><button>Enviar a la TV / Send to TV</button></form><p>La instalación se confirma en la TV. / Confirm installation on the TV.</p>");}
    private static void reply(Socket s,int status,String html) throws IOException {
        byte[] body=html.getBytes(StandardCharsets.UTF_8);
        String header="HTTP/1.1 "+status+" Response\r\nContent-Type: text/html; charset=utf-8\r\nContent-Length: "+body.length+"\r\nConnection: close\r\nCache-Control: no-store\r\nReferrer-Policy: no-referrer\r\nX-Content-Type-Options: nosniff\r\nContent-Security-Policy: default-src 'none'; style-src 'unsafe-inline'; form-action 'self'; frame-ancestors 'none'; base-uri 'none'\r\n\r\n";
        OutputStream out=s.getOutputStream();out.write(header.getBytes(StandardCharsets.UTF_8));out.write(body);out.flush();
    }
    @Override public void close(){closed=true;try{socket.close();}catch(IOException ignored){}workers.shutdownNow();}
}
