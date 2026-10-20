package com.miflix.native2.data;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;

/** Short-lived LAN form. The save callback may inspect a playlist for its name; no video proxy. */
public final class M3UListServerRP61 implements AutoCloseable {
    public interface Save { boolean save(String name,String url,String playlist); }
    private final Inet4Address address;
    private final int prefix;
    private final ServerSocket socket;
    private final LocalCastAccess access=new LocalCastAccess("unused",System::currentTimeMillis);
    private final ThreadPoolExecutor workers=new ThreadPoolExecutor(2,2,0L,TimeUnit.SECONDS,new ArrayBlockingQueue<>(8));
    private final String formToken=UUID.randomUUID().toString()+UUID.randomUUID().toString();
    private final Save save;
    private final String initialName,initialUrl;
    private volatile boolean closed;
    public M3UListServerRP61(Inet4Address address,int prefix,String name,String url,Save save) throws IOException {
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
        if(!LocalCastServer.sameSubnet(address.getAddress(),s.getInetAddress().getAddress(),prefix)) {s.close();continue;}
        try {workers.execute(()->handle(s));} catch(RejectedExecutionException e){s.close();}
    }catch(IOException e){if(!closed)close();}}
    private static String line(InputStream in,int max) throws IOException {
        ByteArrayOutputStream b=new ByteArrayOutputStream();int c;
        while((c=in.read())!=-1) {if(c=='\n')return b.toString("UTF-8").replace("\r","");if(b.size()>=max)throw new IOException();b.write(c);}
        throw new EOFException();
    }
    private void handle(Socket client){try(Socket s=client) {
        s.setSoTimeout(15000);InputStream in=s.getInputStream();String[] request=line(in,2048).split(" ");if(request.length!=3)return;
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
        String contentType=value(headers,"content-type","");
        if(!contentType.startsWith("multipart/form-data")) {reply(s,415,"Invalid form");return;}
        int length=Integer.parseInt(value(headers,"content-length","0"));if(length<1 || length>2*1024*1024) {reply(s,413,"Maximum upload: 2 MB");return;}
        byte[] body=new byte[length];int offset=0;while(offset<length) {int n=in.read(body,offset,length-offset);if(n<0)throw new EOFException();offset+=n;}
        java.util.regex.Matcher boundaryMatch=java.util.regex.Pattern.compile("boundary=(?:\"([^\"]+)\"|([^;]+))").matcher(contentType);
        if(!boundaryMatch.find()) {reply(s,400,"Invalid form");return;}
        String boundary=boundaryMatch.group(1)!=null?boundaryMatch.group(1):boundaryMatch.group(2).trim();
        if(boundary.length()>150) {reply(s,400,"Invalid form");return;}
        Map<String,String> fields=new HashMap<>();
        for(String part:new String(body,StandardCharsets.UTF_8).split(java.util.regex.Pattern.quote("--"+boundary))) {
            int split=part.indexOf("\r\n\r\n");if(split<0)continue;
            String header=part.substring(0,split);
            java.util.regex.Matcher field=java.util.regex.Pattern.compile("(?:^|[; ])name=\"([^\"]+)\"").matcher(header);
            if(!field.find())continue;
            String value=part.substring(split+4);if(value.endsWith("\r\n")) value=value.substring(0,value.length()-2);
            fields.put(field.group(1),value);
        }
        if(!acceptsForm(getBase(),origin,formToken,value(fields,"csrf",""))) {reply(s,403,page("Formulario no válido / Invalid form","Scan the QR on the TV again."));return;}
        String name=value(fields,"name","").trim(),url=value(fields,"url","").trim();
        String playlist=value(fields,"playlist","");
        if(name.isEmpty() || name.length()>80 || (playlist.trim().isEmpty() && !valid(name,url))) {reply(s,400,page("Revisa el nombre y la URL / Check name and URL", "Choose an M3U file or paste its HTTP(S) URL, and enter a list name."));return;}
        if(closed || !save.save(name,url,playlist)) {reply(s,409,page("No se pudo guardar / Could not save","Check the message on the TV. Keep the QR open and submit a valid M3U channel list."));return;}
        reply(s,200,page("Lista guardada / Playlist saved","Ya aparece con su nombre en Live TV. / Your list is now available by name in Live TV."));
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
    private String form(String ticket){return shell("<h1>Tu lista, en BruniO</h1><p>Elige un archivo M3U o pega su URL. Ponle el nombre que quieras. / Choose an M3U file or paste its URL and name your list.</p><form method='post' enctype='multipart/form-data' action='/save/"+ticket+"'><input type='hidden' name='csrf' value='"+formToken+"'><label>Nombre / Name<input name='name' required maxlength='80' autocomplete='off'></label><label>Archivo M3U / M3U file<input name='playlist' type='file' accept='.m3u,.m3u8,text/plain,audio/x-mpegurl,application/vnd.apple.mpegurl'></label><label>O URL de la lista / Or playlist URL<input name='url' type='url' maxlength='8192' autocomplete='off' placeholder='https://…/channels.m3u'></label><button>Guardar lista / Save playlist</button></form><p>Máximo 2 MB y 5000 canales por lista. Guardado en este perfil de esta TV. / Maximum 2 MB and 5000 channels per list. Saved to this TV profile.</p>");}
    private static void reply(Socket s,int status,String html) throws IOException {
        byte[] body=html.getBytes(StandardCharsets.UTF_8);
        String header="HTTP/1.1 "+status+" Response\r\nContent-Type: text/html; charset=utf-8\r\nContent-Length: "+body.length+"\r\nConnection: close\r\nCache-Control: no-store\r\nReferrer-Policy: no-referrer\r\nX-Content-Type-Options: nosniff\r\nContent-Security-Policy: default-src 'none'; style-src 'unsafe-inline'; form-action 'self'; frame-ancestors 'none'; base-uri 'none'\r\n\r\n";
        OutputStream out=s.getOutputStream();out.write(header.getBytes(StandardCharsets.UTF_8));out.write(body);out.flush();
    }
    @Override public void close(){closed=true;access.clear();try{socket.close();}catch(IOException ignored){}workers.shutdownNow();}
}
