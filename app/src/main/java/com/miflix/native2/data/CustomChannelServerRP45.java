package com.miflix.native2.data;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;

/** Short-lived LAN form. The save callback may inspect a playlist for its name; no video proxy. */
public final class CustomChannelServerRP45 implements AutoCloseable {
    public interface Save { boolean save(String name,String url); }
    private final Inet4Address address;
    private final int prefix;
    private final ServerSocket socket;
    private final LocalCastAccess access=new LocalCastAccess("unused",System::currentTimeMillis);
    private final ThreadPoolExecutor workers=new ThreadPoolExecutor(2,2,0L,TimeUnit.SECONDS,new ArrayBlockingQueue<>(8));
    private final String formToken=UUID.randomUUID().toString()+UUID.randomUUID().toString();
    private final Save save;
    private final String initialName,initialUrl;
    private volatile boolean closed;
    public CustomChannelServerRP45(Inet4Address address,int prefix,String name,String url,Save save) throws IOException {
        if(!LocalCastServer.isPrivate(address.getAddress()) || prefix<8 || prefix>30) throw new IOException("LAN required");
        this.address=address;this.prefix=prefix;this.save=save;initialName=name;initialUrl=url;
        socket=new ServerSocket();socket.bind(new InetSocketAddress(address,0),8);
        Thread thread=new Thread(this::accept,"brunio-custom-channel");thread.setDaemon(true);thread.start();
    }
    public long getUntil(){return access.pairUntil;}
    public String getBase(){return "http://"+address.getHostAddress()+":"+socket.getLocalPort();}
    public String getUrl(){return getBase()+"/add/"+access.getLinkTicket();}
    public static boolean valid(String name,String url) {
        if(name==null || name.trim().isEmpty() || name.trim().length()>80 || url==null || url.length()>8192) return false;
        for(int i=0;i<name.length();i++) if(Character.isISOControl(name.charAt(i))) return false;
        try {
            URI uri=new URI(url);
            return ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme())) && uri.getHost()!=null && uri.getUserInfo()==null && uri.getFragment()==null;
        } catch(Exception e){return false;}
    }
    private void accept(){while(!closed)try {
        Socket s=socket.accept();
        if(!LocalQrNetworkRP129.acceptsPeer(s.getInetAddress().getAddress())) {s.close();continue;}
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
        String ticket=access.getLinkTicket();
        if(!access.validLink(ticket)) {reply(s,410,"QR expired / QR vencido. Generate a new QR on the TV.");return;}
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
        String name=value(fields,"name","").trim(),url=value(fields,"url","").trim();
        if(!valid(name.isEmpty()?"Auto":name,url)) {reply(s,400,page("Revisa el nombre y la URL / Check name and URL", "Use an HTTP(S) stream URL and a name of up to 80 characters. Return to the previous page to correct it."));return;}
        if(closed || !save.save(name,url)) {reply(s,409,page("No se pudo guardar / Could not save","Keep the QR open on the TV and try again. The limit is 100 personal channels per profile; included channels do not count."));return;}
        reply(s,200,page("Canal guardado / Channel saved","Ya aparece en Mis canales en la TV. / Your channel is now available under My channels on the TV."));
    }catch(Exception ignored){/* Do not log stream URLs, tokens or form contents. */}}
    public static boolean acceptsForm(String base,String origin,String expected,String submitted) {
        return expected!=null && !expected.isEmpty() && submitted!=null
            && (origin==null || origin.isEmpty() || origin.equals("null") || origin.equals(base))
            && MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),submitted.getBytes(StandardCharsets.UTF_8));
    }
    private static String value(Map<String,String> map,String key,String fallback){String v=map.get(key);return v==null?fallback:v;}
    private static String escape(String text){return text.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");}
    private static String page(String title,String text){return shell("<h1>"+escape(title)+"</h1><p>"+escape(text)+"</p>");}
    private static String shell(String body){return "<!doctype html><html lang='es'><meta charset='utf-8'><meta name='viewport' content='width=device-width,initial-scale=1'><title>BruniO · My channels</title><style>body{margin:0;background:#080808;color:#fff;font:16px system-ui}main{max-width:480px;margin:6vh auto;padding:28px}small,p{color:#aaa;line-height:1.6}h1{font-size:29px}label{display:block;margin-top:22px}input,button{box-sizing:border-box;width:100%;padding:16px;margin-top:9px;border-radius:14px;border:1px solid #444;background:#171717;color:white;font:inherit}button{background:#eee;color:#111;font-weight:700;margin-top:28px}input:focus{outline:2px solid #b4a0d4}</style><main><small>BRUNIO · LIVE</small>"+body+"</main></html>";}
    private String form(String ticket){return shell("<h1>Tu canal, en BruniO</h1><p>Pega el enlace del stream. Deja el nombre vacío para detectarlo.<br>Paste the stream URL. Leave the name blank to detect it.</p><form method='post' action='/save/"+ticket+"'><input type='hidden' name='csrf' value='"+formToken+"'><label>Nombre opcional / Optional name<input name='name' placeholder='Detectar automáticamente / Auto-detect' maxlength='80' autocomplete='off' value='"+escape(initialName)+"'></label><label>URL del stream / Stream URL<input name='url' type='url' required maxlength='8192' autocomplete='off' placeholder='https://…/live.m3u8' value='"+escape(initialUrl)+"'></label><button>Guardar canal / Save channel</button></form><p>Los enlaces temporales pueden caducar. / Temporary links can expire.</p>");}
    private static void reply(Socket s,int status,String html) throws IOException {
        byte[] body=html.getBytes(StandardCharsets.UTF_8);
        String header="HTTP/1.1 "+status+" Response\r\nContent-Type: text/html; charset=utf-8\r\nContent-Length: "+body.length+"\r\nConnection: close\r\nCache-Control: no-store\r\nReferrer-Policy: no-referrer\r\nX-Content-Type-Options: nosniff\r\nContent-Security-Policy: default-src 'none'; style-src 'unsafe-inline'; form-action 'self'; frame-ancestors 'none'; base-uri 'none'\r\n\r\n";
        OutputStream out=s.getOutputStream();out.write(header.getBytes(StandardCharsets.UTF_8));out.write(body);out.flush();
    }
    @Override public void close(){closed=true;access.clear();try{socket.close();}catch(IOException ignored){}workers.shutdownNow();}
}
