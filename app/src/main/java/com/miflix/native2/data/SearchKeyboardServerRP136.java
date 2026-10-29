package com.miflix.native2.data;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;

/** Ephemeral LAN search input. Text is POST-only and never logged. */
public final class SearchKeyboardServerRP136 implements AutoCloseable {
    public interface Save { boolean save(String key); }
    private final Inet4Address address;
    private final int prefix;
    private final ServerSocket socket;
    private final String ticket=UUID.randomUUID().toString()+UUID.randomUUID().toString();
    private final long until=System.currentTimeMillis()+900000L;
    private final boolean loopbackOnly;
    private final boolean secretInputRP150;
    private final ThreadPoolExecutor workers=new ThreadPoolExecutor(2,2,0L,TimeUnit.SECONDS,new ArrayBlockingQueue<>(8));
    private final String formToken=UUID.randomUUID().toString()+UUID.randomUUID().toString();
    private final Save save;
    private long lastSequence=-1;
    private volatile boolean closed;
    private final Set<Socket> clients=ConcurrentHashMap.newKeySet();
    public SearchKeyboardServerRP136(Inet4Address address,int prefix,Save save) throws IOException {
        this(address,prefix,save,false,false);
    }
    public SearchKeyboardServerRP136(Inet4Address address,int prefix,Save save,boolean secret) throws IOException {this(address,prefix,save,false,secret);}
    /** Package-private fixture: only binds localhost; cannot relax LAN admission. */
    static SearchKeyboardServerRP136 loopbackForTests(Save save) throws IOException {
        return new SearchKeyboardServerRP136((Inet4Address)InetAddress.getByName("127.0.0.1"),8,save,true,false);
    }
    static SearchKeyboardServerRP136 secretLoopbackForTests(Save save) throws IOException {
        return new SearchKeyboardServerRP136((Inet4Address)InetAddress.getByName("127.0.0.1"),8,save,true,true);
    }
    private SearchKeyboardServerRP136(Inet4Address address,int prefix,Save save,boolean loopbackOnly,boolean secret) throws IOException {
        if(prefix<8 || prefix>30 || (loopbackOnly?!address.isLoopbackAddress():!privateAddress(address.getAddress()))) throw new IOException("LAN required");
        this.address=address;this.prefix=prefix;this.save=save;this.loopbackOnly=loopbackOnly;this.secretInputRP150=secret;
        socket=new ServerSocket();socket.bind(new InetSocketAddress(address,0),8);
        Thread thread=new Thread(this::accept,"brunio-search-keyboard");thread.setDaemon(true);thread.start();
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
    public static boolean valid(String key) {return key!=null && key.length()<=256 && key.codePoints().noneMatch(c->Character.isISOControl(c));}
    private void accept(){while(!closed)try {
        Socket s=socket.accept();
        if(loopbackOnly?!s.getInetAddress().isLoopbackAddress():!acceptsPeer(s.getInetAddress().getAddress())) {s.close();continue;}
        clients.add(s);
        try {workers.execute(()->handle(s));} catch(RejectedExecutionException e){clients.remove(s);s.close();}
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
        String key=value(fields,"text","");
        long sequence=Long.parseLong(value(fields,"sequence","-1"));
        if(!valid(key)) {reply(s,400,page("Texto no válido / Invalid text", "Use up to 256 characters."));return;}
        synchronized(this) {
            if(sequence<0) {reply(s,400,"Invalid sequence");return;}
            if(sequence<=lastSequence) {reply(s,200,"OK");return;}
            if(closed || !save.save(key)) {reply(s,409,"Search unavailable. Reopen the QR on TV.");return;}
            lastSequence=sequence;
        }
        reply(s,200,"OK");
    }catch(Exception ignored){/* Search text and pairing tokens stay local and are never logged. */}finally{clients.remove(client);}}
    public static boolean acceptsForm(String base,String origin,String expected,String submitted) {
        return expected!=null && !expected.isEmpty() && submitted!=null
            && (origin==null || origin.isEmpty() || origin.equals("null") || origin.equals(base))
            && MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),submitted.getBytes(StandardCharsets.UTF_8));
    }
    private static String value(Map<String,String> map,String key,String fallback){String v=map.get(key);return v==null?fallback:v;}
    private static String escape(String text){return text.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");}
    private static String page(String title,String text){return shell("<h1>"+escape(title)+"</h1><p>"+escape(text)+"</p>");}
    private static String shell(String body){return "<!doctype html><html lang='es'><meta charset='utf-8'><meta name='viewport' content='width=device-width,initial-scale=1'><title>BruniO · Search</title><style>body{margin:0;background:#080808;color:#fff;font:16px system-ui}main{max-width:480px;margin:6vh auto;padding:28px}small,p{color:#aaa;line-height:1.6}h1{font-size:29px}label{display:block;margin-top:22px}input,button{box-sizing:border-box;width:100%;padding:16px;margin-top:9px;border-radius:14px;border:1px solid #444;background:#171717;color:white;font:inherit}button{background:#eee;color:#111;font-weight:700;margin-top:28px}input:focus{outline:2px solid #b4a0d4}</style><main><small>BRUNIO · SEARCH</small>"+body+"</main></html>";}
    private String form(String ticket){if(secretInputRP150)return secretFormRP150(ticket);return shell("<h1>Escribe en tu TV</h1><p>Write a title, actor or director. / Escribe un título, actor o director.</p><label for='text'>Título, actor o director / Title, actor or director</label><input id='text' type='text' inputmode='text' maxlength='256' autocomplete='off' placeholder='Buscar / Search' aria-describedby='voice-help'><button id='dictate' type='button'>🎙 Dictar con mi teclado / Use keyboard dictation</button><p id='voice-help'>Toca el micrófono de Gboard, Samsung o el teclado de tu celular. Lo que dictes se enviará a la TV. El idioma se elige en tu teclado. / Tap your phone keyboard microphone. Dictated text is sent to your TV; choose the language in your keyboard.</p><p id='status'>Conectado · Connected</p><script>const input=document.getElementById('text'),status=document.getElementById('status');document.getElementById('dictate').addEventListener('click',()=>{input.focus();status.textContent='Toca el micrófono del teclado de tu celular / Tap your phone keyboard microphone';});let sequence=Date.now(),timer,busy=false,pending=null;async function send(){if(busy||!pending)return;busy=true;const job=pending;pending=null;try{const r=await fetch('/save/"+ticket+"',{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:new URLSearchParams({csrf:'"+formToken+"',text:job.text,sequence:job.sequence})});status.textContent=r.ok?'✓ Enviado a la TV / Sent to TV':'Vuelve a abrir el QR en TV / Reopen QR on TV';}catch(e){status.textContent='Sin conexión / Disconnected';}finally{busy=false;if(pending)send();}}input.addEventListener('input',()=>{clearTimeout(timer);timer=setTimeout(()=>{pending={text:input.value,sequence:++sequence};send();},180);});</script>");}
    private String secretFormRP150(String ticket) {return shell("<h1>BruniO · Gemini</h1><p>Use your own free-tier project. Only your API key is sent to this TV; it is not displayed or included in the URL.</p><form id='form'><label for='key'>Gemini API key</label><input id='key' type='password' autocomplete='off' maxlength='160'><button>Save to TV / Guardar en TV</button></form><p id='status'></p><script>document.getElementById('form').onsubmit=async e=>{e.preventDefault();const b=e.target.querySelector('button');b.disabled=true;try{const r=await fetch('/save/"+ticket+"',{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:new URLSearchParams({csrf:'"+formToken+"',sequence:Date.now(),text:document.getElementById('key').value.trim()})});document.getElementById('status').textContent=r.ok?'Received by TV / Recibida en TV':'Could not save. Retry / Reintenta';if(r.ok)document.getElementById('key').value='';}catch(e){document.getElementById('status').textContent='Connection failed / Sin conexión';}finally{b.disabled=false;}};</script>");}
    private static void reply(Socket s,int status,String html) throws IOException {
        byte[] body=html.getBytes(StandardCharsets.UTF_8);
        String header="HTTP/1.1 "+status+" Response\r\nContent-Type: text/html; charset=utf-8\r\nContent-Length: "+body.length+"\r\nConnection: close\r\nCache-Control: no-store\r\nReferrer-Policy: no-referrer\r\nX-Content-Type-Options: nosniff\r\nContent-Security-Policy: default-src 'none'; style-src 'unsafe-inline'; script-src 'unsafe-inline'; connect-src 'self'; form-action 'self'; frame-ancestors 'none'; base-uri 'none'\r\n\r\n";
        OutputStream out=s.getOutputStream();out.write(header.getBytes(StandardCharsets.UTF_8));out.write(body);out.flush();
    }
    @Override public void close(){closed=true;try{socket.close();}catch(IOException ignored){}workers.shutdownNow();for(Socket client:clients)try{client.close();}catch(IOException ignored){}clients.clear();}
}
