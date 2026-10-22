package com.miflix.native2.data;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;

/** Ephemeral LAN credential form. Key is POST-only, never echoed or placed in the QR URL. */
public final class TorBoxKeyServerRP76 implements AutoCloseable {
    public interface Save { boolean save(String key); }
    private final Inet4Address address;
    private final int prefix;
    private final ServerSocket socket;
    private final LocalCastAccess access=new LocalCastAccess("unused",System::currentTimeMillis);
    private final ThreadPoolExecutor workers=new ThreadPoolExecutor(2,2,0L,TimeUnit.SECONDS,new ArrayBlockingQueue<>(8));
    private final String formToken=UUID.randomUUID().toString()+UUID.randomUUID().toString();
    private final Save save;
    private boolean received;
    private volatile boolean closed;
    public TorBoxKeyServerRP76(Inet4Address address,int prefix,Save save) throws IOException {
        if(!LocalCastServer.isPrivate(address.getAddress()) || prefix<8 || prefix>30) throw new IOException("LAN required");
        this.address=address;this.prefix=prefix;this.save=save;
        socket=new ServerSocket();socket.bind(new InetSocketAddress(address,0),8);
        Thread thread=new Thread(this::accept,"brunio-torbox-key");thread.setDaemon(true);thread.start();
    }
    public long getUntil(){return access.pairUntil;}
    public String getBase(){return "http://"+address.getHostAddress()+":"+socket.getLocalPort();}
    public String getUrl(){return getBase()+"/add/"+access.getLinkTicket();}
    public static boolean valid(String key) {
        if(key==null || key.length()<16 || key.length()>256) return false;
        for(int i=0;i<key.length();i++) if(key.charAt(i)<33 || key.charAt(i)>126) return false;
        return true;
    }
    private void accept(){while(!closed)try {
        Socket s=socket.accept();
        if(!LocalCastServer.sameSubnet(address.getAddress(),s.getInetAddress().getAddress(),prefix)) {s.close();continue;}
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
        String key=value(fields,"apiKey","").trim();
        if(!valid(key)) {reply(s,400,page("Clave no válida / Invalid key", "Use a TorBox API key between 16 and 256 characters, without spaces."));return;}
        synchronized(this) {
            if(received) {reply(s,409,page("Clave ya recibida / Key already received","Generate a new QR on the TV to send another key."));return;}
            if(closed || !save.save(key)) {reply(s,409,page("No se pudo recibir / Could not receive","Keep Settings and this QR open on the TV, then try again."));return;}
            received=true;
        }
        reply(s,200,page("Clave recibida / Key received","En la TV, vuelve a Settings y pulsa Verificar y guardar. El motor no se activa automáticamente. / On the TV, return to Settings and select Verify and save. The engine is not enabled automatically."));
    }catch(Exception ignored){/* Do not log API keys, tokens or form contents. */}}
    public static boolean acceptsForm(String base,String origin,String expected,String submitted) {
        return expected!=null && !expected.isEmpty() && submitted!=null
            && (origin==null || origin.isEmpty() || origin.equals("null") || origin.equals(base))
            && MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),submitted.getBytes(StandardCharsets.UTF_8));
    }
    private static String value(Map<String,String> map,String key,String fallback){String v=map.get(key);return v==null?fallback:v;}
    private static String escape(String text){return text.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");}
    private static String page(String title,String text){return shell("<h1>"+escape(title)+"</h1><p>"+escape(text)+"</p>");}
    private static String shell(String body){return "<!doctype html><html lang='es'><meta charset='utf-8'><meta name='viewport' content='width=device-width,initial-scale=1'><title>BruniO · TorBox</title><style>body{margin:0;background:#080808;color:#fff;font:16px system-ui}main{max-width:480px;margin:6vh auto;padding:28px}small,p{color:#aaa;line-height:1.6}h1{font-size:29px}label{display:block;margin-top:22px}input,button{box-sizing:border-box;width:100%;padding:16px;margin-top:9px;border-radius:14px;border:1px solid #444;background:#171717;color:white;font:inherit}button{background:#eee;color:#111;font-weight:700;margin-top:28px}input:focus{outline:2px solid #b4a0d4}</style><main><small>BRUNIO · TORBOX</small>"+body+"</main></html>";}
    private String form(String ticket){return shell("<h1>Conecta tu TorBox</h1><p>Pega tu API key. Se enviará a esta TV por tu red local.<br>Paste your API key. It will be sent to this TV over your local network.</p><form method='post' action='/save/"+ticket+"'><input type='hidden' name='csrf' value='"+formToken+"'><label>TorBox API key<input name='apiKey' type='password' required minlength='16' maxlength='256' autocomplete='off' autocapitalize='none' spellcheck='false' placeholder='API key'></label><button>Enviar a la TV / Send to TV</button></form><p>Luego verifica y guarda en Settings de la TV. El motor seguirá con su estado actual. / Then verify and save in TV Settings. The engine keeps its current state.</p>");}
    private static void reply(Socket s,int status,String html) throws IOException {
        byte[] body=html.getBytes(StandardCharsets.UTF_8);
        String header="HTTP/1.1 "+status+" Response\r\nContent-Type: text/html; charset=utf-8\r\nContent-Length: "+body.length+"\r\nConnection: close\r\nCache-Control: no-store\r\nReferrer-Policy: no-referrer\r\nX-Content-Type-Options: nosniff\r\nContent-Security-Policy: default-src 'none'; style-src 'unsafe-inline'; form-action 'self'; frame-ancestors 'none'; base-uri 'none'\r\n\r\n";
        OutputStream out=s.getOutputStream();out.write(header.getBytes(StandardCharsets.UTF_8));out.write(body);out.flush();
    }
    @Override public void close(){closed=true;access.clear();try{socket.close();}catch(IOException ignored){}workers.shutdownNow();}
}
