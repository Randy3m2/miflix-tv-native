package com.miflix.native2.data;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

/** A browseable LAN MediaServer. No Roku ECP commands and no arbitrary proxy URL endpoint. */
public final class RokuDlnaServerRP182 implements AutoCloseable {
    public static final class Media {
        public final String title,url,mime,category;
        public final Map<String,String> headers;
        public Media(String title,String url,String mime,String category,Map<String,String> headers) {
            this.title=title;this.url=url;this.mime=mime;this.category=category;
            this.headers=new HashMap<>(headers);
        }
    }
    private static final String CD="urn:schemas-upnp-org:service:ContentDirectory:1";
    private static final String CM="urn:schemas-upnp-org:service:ConnectionManager:1";
    private static final String DEVICE="urn:schemas-upnp-org:device:MediaServer:1";
    private static final String[] FOLDERS={"Now Playing","Movies","Episodes","Live TV","Live Sports"};
    private final Inet4Address address;
    private final int prefix;
    private final Media media;
    private final URI origin;
    private final String token,uuid="uuid:"+UUID.randomUUID();
    private final ServerSocket http;
    private final ExecutorService workers=new ThreadPoolExecutor(0,6,20,TimeUnit.SECONDS,new SynchronousQueue<>(),new ThreadPoolExecutor.AbortPolicy());
    private final Set<HttpURLConnection> upstream=ConcurrentHashMap.newKeySet();
    private final Set<Socket> clients=ConcurrentHashMap.newKeySet();
    private final Map<String,URI> routes=new ConcurrentHashMap<>();
    private final Map<String,String> routeNames=new ConcurrentHashMap<>();
    private final Deque<String> routeOrder=new ConcurrentLinkedDeque<>();
    private final ScheduledExecutorService announcements=Executors.newSingleThreadScheduledExecutor();
    private MulticastSocket ssdp;
    private NetworkInterface network;
    private volatile boolean closed;
    public volatile String lastError="";

    public RokuDlnaServerRP182(Inet4Address address,int prefix,Media media) throws IOException {this(address,prefix,media,true);}
    RokuDlnaServerRP182(Inet4Address address,int prefix,Media media,boolean advertise) throws IOException {
        if(!privateIp(address) && !(address.isLoopbackAddress() && !advertise)) throw new IOException("A private Wi-Fi address is required");
        if(prefix<8 || prefix>32) throw new IOException("Invalid local network");
        this.address=address;this.prefix=prefix;this.media=media;
        try {origin=new URI(media.url);validate(origin);} catch(Exception e) {throw new IOException("Unsupported source URL");}
        byte[] secret=new byte[24];new SecureRandom().nextBytes(secret);
        token=Base64.getUrlEncoder().withoutPadding().encodeToString(secret);
        http=new ServerSocket();http.setReuseAddress(true);http.bind(new InetSocketAddress(address,0),8);
        routes.put("root",origin);
        Thread accept=new Thread(this::accept,"brunio-roku-http");accept.setDaemon(true);accept.start();
        if(advertise) try {startSsdp();} catch(IOException e) {close();throw new IOException("Could not advertise on Wi-Fi");}
    }
    public String location() {return "http://"+address.getHostAddress()+":"+http.getLocalPort();}
    public String mediaUrl() {return location()+"/media/"+token+"/root"+(media.mime.contains("mpegurl")?".m3u8":media.mime.equals("video/mp2t")?".ts":".mp4");}
    private boolean local(InetAddress peer) {
        byte[] a=address.getAddress(),b=peer.getAddress();if(a.length!=b.length)return false;
        for(int i=0;i<a.length;i++) {int bits=Math.min(8,Math.max(0,prefix-i*8));int mask=bits==0?0:0xff<<(8-bits);if((a[i]&mask)!=(b[i]&mask))return false;}
        return true;
    }
    private static boolean privateIp(InetAddress a) {byte[] b=a.getAddress();return b.length==4 && ((b[0]&255)==10 || (b[0]&255)==192 && (b[1]&255)==168 || (b[0]&255)==172 && (b[1]&255)>=16 && (b[1]&255)<=31);}
    private void accept() {
        while(!closed) try {
            Socket socket=http.accept();socket.setSoTimeout(12_000);
            if(!local(socket.getInetAddress())) {socket.close();continue;}
            clients.add(socket);
            try {workers.execute(()->serve(socket));} catch(RejectedExecutionException e) {clients.remove(socket);socket.close();}
        } catch(IOException e) {if(!closed) lastError="Local connection interrupted";}
    }
    private static String line(InputStream in,int limit) throws IOException {
        ByteArrayOutputStream out=new ByteArrayOutputStream();int c;
        while((c=in.read())!=-1) {if(c=='\n')break;if(c!='\r')out.write(c);if(out.size()>limit)throw new IOException("Request too large");}
        return out.toString("UTF-8");
    }
    private void serve(Socket socket) {
        try(Socket ignored=socket) {
            InputStream in=socket.getInputStream();OutputStream out=socket.getOutputStream();
            String[] request=line(in,4096).split(" ");if(request.length<2)return;
            String method=request[0],path=request[1];Map<String,String> headers=new HashMap<>();
            int total=0;while(true) {String h=line(in,8192);if(h.isEmpty())break;total+=h.length();if(total>16384)throw new IOException();int colon=h.indexOf(':');if(colon>0)headers.put(h.substring(0,colon).toLowerCase(Locale.US),h.substring(colon+1).trim());}
            if(path.equals("/device.xml")) {xml(out,description(),method.equals("HEAD"));return;}
            if(path.equals("/cd/scpd.xml") || path.equals("/cm/scpd.xml")) {xml(out,scpd(path.startsWith("/cd")),method.equals("HEAD"));return;}
            if(method.equals("POST") && (path.equals("/cd/control") || path.equals("/cm/control"))) {
                int length=Integer.parseInt(headers.getOrDefault("content-length","0"));if(length<0 || length>65536)throw new IOException();
                byte[] body=new byte[length];int count=0;while(count<length) {int n=in.read(body,count,length-count);if(n<0)throw new EOFException();count+=n;}
                soap(out,path,new String(body,StandardCharsets.UTF_8),headers.getOrDefault("soapaction",""));return;
            }
            if((method.equals("GET") || method.equals("HEAD")) && path.startsWith("/media/"+token+"/")) {
                String name=path.substring(("/media/"+token+"/").length()).split("\\?",2)[0];int dot=name.indexOf('.');String key=dot<0?name:name.substring(0,dot);
                URI uri=routes.get(key);if(uri==null) {reply(out,404,"text/plain","Not found");return;}
                relay(out,uri,method.equals("HEAD"),headers.get("range"));return;
            }
            reply(out,404,"text/plain","Not found");
        } catch(Exception e) {if(!closed)lastError="The source or local connection is unavailable";} finally {clients.remove(socket);}
    }
    private static void reply(OutputStream out,int code,String mime,String body) throws IOException {
        byte[] bytes=body.getBytes(StandardCharsets.UTF_8);
        out.write(("HTTP/1.1 "+code+" "+(code==200?"OK":"Unavailable")+"\r\nContent-Type: "+mime+"; charset=utf-8\r\nContent-Length: "+bytes.length+"\r\nCache-Control: no-store\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));out.write(bytes);out.flush();
    }
    private static void xml(OutputStream out,String body,boolean head) throws IOException {
        byte[] data=body.getBytes(StandardCharsets.UTF_8);out.write(("HTTP/1.1 200 OK\r\nContent-Type: text/xml; charset=utf-8\r\nContent-Length: "+data.length+"\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));if(!head)out.write(data);out.flush();
    }
    private static String x(String s) {return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&apos;");}
    private String description() {
        return "<?xml version=\"1.0\"?><root xmlns=\"urn:schemas-upnp-org:device-1-0\"><specVersion><major>1</major><minor>0</minor></specVersion><device><deviceType>"+DEVICE+"</deviceType><friendlyName>BruniO Mobile</friendlyName><manufacturer>BruniO</manufacturer><modelName>Roku LAN Media</modelName><UDN>"+uuid+"</UDN><serviceList>"+service(CD,"cd")+service(CM,"cm")+"</serviceList></device></root>";
    }
    private static String service(String type,String id) {return "<service><serviceType>"+type+"</serviceType><serviceId>urn:upnp-org:serviceId:"+(id.equals("cd")?"ContentDirectory":"ConnectionManager")+"</serviceId><SCPDURL>/"+id+"/scpd.xml</SCPDURL><controlURL>/"+id+"/control</controlURL><eventSubURL>/"+id+"/events</eventSubURL></service>";}
    private static String scpd(boolean directory) {
        String browse="<action><name>Browse</name><argumentList>";
        String[][] args={{"ObjectID","in","A_ARG_TYPE_ObjectID"},{"BrowseFlag","in","A_ARG_TYPE_BrowseFlag"},{"Filter","in","A_ARG_TYPE_Filter"},{"StartingIndex","in","A_ARG_TYPE_Index"},{"RequestedCount","in","A_ARG_TYPE_Count"},{"SortCriteria","in","A_ARG_TYPE_SortCriteria"},{"Result","out","A_ARG_TYPE_Result"},{"NumberReturned","out","A_ARG_TYPE_Count"},{"TotalMatches","out","A_ARG_TYPE_Count"},{"UpdateID","out","SystemUpdateID"}};
        for(String[] a:args)browse+="<argument><name>"+a[0]+"</name><direction>"+a[1]+"</direction><relatedStateVariable>"+a[2]+"</relatedStateVariable></argument>";
        browse+="</argumentList></action>";
        String vars="";for(String name:new String[]{"ObjectID","BrowseFlag","Filter","SortCriteria","Result","Index","Count"})vars+="<stateVariable sendEvents=\"no\"><name>A_ARG_TYPE_"+name+"</name><dataType>"+(name.equals("Index")||name.equals("Count")?"ui4":"string")+"</dataType></stateVariable>";
        return "<?xml version=\"1.0\"?><scpd xmlns=\"urn:schemas-upnp-org:service-1-0\"><specVersion><major>1</major><minor>0</minor></specVersion><actionList>"+(directory?browse+"<action><name>GetSystemUpdateID</name><argumentList><argument><name>Id</name><direction>out</direction><relatedStateVariable>SystemUpdateID</relatedStateVariable></argument></argumentList></action>":"<action><name>GetProtocolInfo</name><argumentList><argument><name>Source</name><direction>out</direction><relatedStateVariable>SourceProtocolInfo</relatedStateVariable></argument><argument><name>Sink</name><direction>out</direction><relatedStateVariable>SinkProtocolInfo</relatedStateVariable></argument></argumentList></action>")+"</actionList><serviceStateTable>"+(directory?vars+"<stateVariable sendEvents=\"yes\"><name>SystemUpdateID</name><dataType>ui4</dataType></stateVariable>":"<stateVariable sendEvents=\"yes\"><name>SourceProtocolInfo</name><dataType>string</dataType></stateVariable><stateVariable sendEvents=\"yes\"><name>SinkProtocolInfo</name><dataType>string</dataType></stateVariable>")+"</serviceStateTable></scpd>";
    }
    private static String arg(String body,String name,String fallback) {Matcher m=Pattern.compile("<(?:\\w+:)?"+name+"[^>]*>([^<]*)</").matcher(body);return m.find()?m.group(1):fallback;}
    private static int number(String s) {try{return Math.max(0,Integer.parseInt(s));}catch(Exception e){return 0;}}
    private String container(String id,String parent,String name) {return "<container id=\""+id+"\" parentID=\""+parent+"\" restricted=\"1\"><dc:title>"+x(name)+"</dc:title><upnp:class>object.container</upnp:class></container>";}
    private String item(String parent) {return "<item id=\"selected\" parentID=\""+parent+"\" restricted=\"1\"><dc:title>"+x(media.title)+"</dc:title><upnp:class>object.item.videoItem</upnp:class><res protocolInfo=\"http-get:*:"+x(media.mime)+":*\">"+x(mediaUrl())+"</res></item>";}
    private void soap(OutputStream out,String path,String body,String action) throws IOException {
        String name=action.replace("\"","");name=name.substring(name.lastIndexOf('#')+1);String result="";
        if(name.equals("Browse")) {
            String id=arg(body,"ObjectID","0"),flag=arg(body,"BrowseFlag","BrowseDirectChildren");List<String> entries=new ArrayList<>();
            if(flag.equals("BrowseMetadata")) {
                if(id.equals("0")) entries.add(container("0","-1","BruniO Mobile"));
                else if(id.equals("selected")) entries.add(item("f0"));
                else if(id.matches("f[0-4]")) entries.add(container(id,"0",FOLDERS[id.charAt(1)-'0']));
            } else if(id.equals("0")) {for(int i=0;i<FOLDERS.length;i++)entries.add(container("f"+i,"0",FOLDERS[i]));}
            else if(id.equals("f0") || id.matches("f[1-4]") && FOLDERS[id.charAt(1)-'0'].equals(media.category)) entries.add(item(id));
            int start=Math.min(entries.size(),number(arg(body,"StartingIndex","0"))),count=number(arg(body,"RequestedCount","0"));
            int end=count==0?entries.size():Math.min(entries.size(),start+Math.min(count,100));
            String didl="<DIDL-Lite xmlns=\"urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:upnp=\"urn:schemas-upnp-org:metadata-1-0/upnp/\">"+String.join("",entries.subList(start,end))+"</DIDL-Lite>";
            result="<Result>"+x(didl)+"</Result><NumberReturned>"+(end-start)+"</NumberReturned><TotalMatches>"+entries.size()+"</TotalMatches><UpdateID>1</UpdateID>";
        } else if(name.equals("GetSystemUpdateID")) result="<Id>1</Id>";
        else if(name.equals("GetSearchCapabilities")) result="<SearchCaps></SearchCaps>";
        else if(name.equals("GetSortCapabilities")) result="<SortCaps></SortCaps>";
        else if(name.equals("GetProtocolInfo")) result="<Source>http-get:*:"+x(media.mime)+":*</Source><Sink></Sink>";
        else if(name.equals("GetCurrentConnectionIDs")) result="<ConnectionIDs>0</ConnectionIDs>";
        else {reply(out,500,"text/xml","Unsupported action");return;}
        String type=path.startsWith("/cd")?CD:CM;
        xml(out,"<?xml version=\"1.0\"?><s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\"><s:Body><u:"+name+"Response xmlns:u=\""+type+"\">"+result+"</u:"+name+"Response></s:Body></s:Envelope>",false);
    }
    private static boolean sameOrigin(URI a,URI b) {return a.getScheme().equalsIgnoreCase(b.getScheme()) && a.getHost().equalsIgnoreCase(b.getHost()) && a.getPort()==b.getPort();}
    private static void validate(URI uri) throws IOException {
        if(uri.getHost()==null || !("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme())) || uri.getUserInfo()!=null)throw new IOException("Unsupported URL");
    }
    private HttpURLConnection connect(URI target,boolean head,String range) throws IOException {
        URI uri=target;
        for(int redirects=0;redirects<4;redirects++) {
            validate(uri);
            if(!sameOrigin(origin,uri)) for(InetAddress a:InetAddress.getAllByName(uri.getHost())) if(a.isAnyLocalAddress()||a.isLoopbackAddress()||a.isLinkLocalAddress()||a.isSiteLocalAddress())throw new IOException("Unsafe redirect");
            HttpURLConnection connection=(HttpURLConnection)uri.toURL().openConnection();connection.setInstanceFollowRedirects(false);connection.setConnectTimeout(8000);connection.setReadTimeout(15000);connection.setRequestMethod(head?"HEAD":"GET");
            connection.setRequestProperty("Accept-Encoding","identity");
            if(range!=null && range.matches("bytes=(?:[0-9]+-[0-9]*|-[0-9]+)")) connection.setRequestProperty("Range",range);
            if(sameOrigin(origin,uri)) for(Map.Entry<String,String> h:media.headers.entrySet()) if(!h.getKey().equalsIgnoreCase("Host") && !h.getKey().equalsIgnoreCase("Content-Length") && !h.getKey().equalsIgnoreCase("Connection"))connection.setRequestProperty(h.getKey(),h.getValue());
            upstream.add(connection);int status;
            try {status=connection.getResponseCode();} catch(IOException e) {upstream.remove(connection);connection.disconnect();throw e;}
            if(status>=300 && status<400 && connection.getHeaderField("Location")!=null) {URI next=uri.resolve(connection.getHeaderField("Location"));connection.disconnect();upstream.remove(connection);uri=next;continue;}
            return connection;
        }
        throw new IOException("Too many redirects");
    }
    private String route(URI uri) throws IOException {
        validate(uri);String known=routeNames.get(uri.toString());if(known!=null)return known;
        String id=UUID.randomUUID().toString().replace("-","");routes.put(id,uri);String name="/media/"+token+"/"+id;
        routeNames.put(uri.toString(),name);routeOrder.addLast(id);
        while(routes.size()>2048) {String old=routeOrder.pollFirst();if(old==null)break;URI removed=routes.remove(old);if(removed!=null)routeNames.remove(removed.toString(),"/media/"+token+"/"+old);}
        return name;
    }
    private static final class UnsupportedMedia extends IOException {UnsupportedMedia(String reason){super(reason);}}
    private String playlist(String text,URI base) throws IOException {
        if(!text.trim().startsWith("#EXTM3U")) throw new UnsupportedMedia("The source did not return an HLS playlist");
        if(text.contains("SAMPLE-AES") || text.toLowerCase(Locale.US).contains("widevine") || text.toLowerCase(Locale.US).contains("playready"))throw new UnsupportedMedia("DRM is unsupported");
        StringBuilder rewritten=new StringBuilder();Pattern quoted=Pattern.compile("URI=\"([^\"]+)\"");
        for(String line:text.split("\\r?\\n")) {
            if(line.startsWith("#")) {
                Matcher matcher=quoted.matcher(line);StringBuffer b=new StringBuffer();
                while(matcher.find()) matcher.appendReplacement(b,Matcher.quoteReplacement("URI=\""+route(base.resolve(matcher.group(1)))+"\""));matcher.appendTail(b);rewritten.append(b);
            } else if(!line.trim().isEmpty()) rewritten.append(route(base.resolve(line.trim())));
            rewritten.append('\n');
        }
        return rewritten.toString();
    }
    private void relay(OutputStream out,URI uri,boolean head,String range) throws IOException {
        HttpURLConnection connection=null;boolean sent=false;
        try {
            connection=connect(uri,head,range);int status=connection.getResponseCode();
            if(status!=200 && status!=206) {reply(out,status,"text/plain","Source unavailable");return;}
            String mime=Optional.ofNullable(connection.getContentType()).orElse(uri.equals(origin)?media.mime:"application/octet-stream");
            boolean hls=mime.toLowerCase(Locale.US).contains("mpegurl") || uri.getPath().toLowerCase(Locale.US).endsWith(".m3u8");
            InputStream body=null;
            if(!head) {
                BufferedInputStream checked=new BufferedInputStream(connection.getInputStream());
                checked.mark(16);byte[] prefix=new byte[16];int n=checked.read(prefix);checked.reset();body=checked;
                if(n>0 && new String(prefix,0,n,StandardCharsets.UTF_8).replace("\ufeff","").trim().startsWith("#EXTM3U")) hls=true;
            }
            String lower=mime.toLowerCase(Locale.US);
            if(!hls && !(lower.startsWith("video/") || lower.startsWith("audio/") || lower.startsWith("text/vtt") || lower.startsWith("application/octet-stream"))) throw new UnsupportedMedia("Unsupported source format");
            if(hls && !head) {
                ByteArrayOutputStream b=new ByteArrayOutputStream();try(InputStream input=body) {byte[] buffer=new byte[8192];int n;while((n=input.read(buffer))!=-1) {if(b.size()+n>2_000_000)throw new IOException("Playlist too large");b.write(buffer,0,n);}}
                reply(out,200,"application/vnd.apple.mpegurl",playlist(b.toString("UTF-8"),connection.getURL().toURI()));return;
            }
            StringBuilder headers=new StringBuilder("HTTP/1.1 "+status+" "+(status==206?"Partial Content":"OK")+"\r\nConnection: close\r\nContent-Type: "+mime+"\r\n");
            for(String field:new String[]{"Content-Length","Content-Range","Accept-Ranges"}) {String value=connection.getHeaderField(field);if(value!=null)headers.append(field).append(": ").append(value).append("\r\n");}
            headers.append("\r\n");out.write(headers.toString().getBytes(StandardCharsets.US_ASCII));sent=true;
            if(!head) try(InputStream input=body) {byte[] buffer=new byte[32768];int n;while(!closed && (n=input.read(buffer))!=-1)out.write(buffer,0,n);}
            out.flush();
        } catch(UnsupportedMedia e) {lastError=e.getMessage();if(!sent)reply(out,415,"text/plain",lastError);}
        catch(URISyntaxException e) {if(!sent)reply(out,502,"text/plain","Unsupported playlist");}
        catch(IOException e) {lastError="The source is unavailable or unsupported";if(!sent)reply(out,502,"text/plain",lastError);}
        finally {if(connection!=null) {upstream.remove(connection);connection.disconnect();}}
    }
    private void startSsdp() throws IOException {
        network=NetworkInterface.getByInetAddress(address);if(network==null)throw new IOException();
        ssdp=new MulticastSocket(null);ssdp.setReuseAddress(true);ssdp.bind(new InetSocketAddress(1900));ssdp.setNetworkInterface(network);ssdp.setTimeToLive(2);
        ssdp.joinGroup(new InetSocketAddress("239.255.255.250",1900),network);
        Thread receive=new Thread(()->{byte[] buffer=new byte[4096];while(!closed)try {
            DatagramPacket packet=new DatagramPacket(buffer,buffer.length);ssdp.receive(packet);if(!local(packet.getAddress()))continue;
            String data=new String(packet.getData(),packet.getOffset(),packet.getLength(),StandardCharsets.US_ASCII);
            if(!data.startsWith("M-SEARCH ") || !data.toLowerCase(Locale.US).contains("ssdp:discover"))continue;
            Matcher match=Pattern.compile("(?im)^ST:\\s*([^\\r\\n]+)").matcher(data);if(!match.find())continue;String st=match.group(1).trim();
            for(String type:new String[]{"upnp:rootdevice",DEVICE,CD,CM,uuid}) if(st.equals("ssdp:all") || st.equals(type)) {
                String response="HTTP/1.1 200 OK\r\nCACHE-CONTROL: max-age=90\r\nEXT:\r\nLOCATION: "+location()+"/device.xml\r\nSERVER: Android/1.0 UPnP/1.0 BruniO/1.0\r\nST: "+type+"\r\nUSN: "+uuid+(type.equals(uuid)?"":"::"+type)+"\r\n\r\n";
                byte[] bytes=response.getBytes(StandardCharsets.US_ASCII);ssdp.send(new DatagramPacket(bytes,bytes.length,packet.getAddress(),packet.getPort()));
            }
        } catch(IOException e) {if(!closed)lastError="Wi-Fi discovery interrupted";break;}},"brunio-roku-discovery");receive.setDaemon(true);receive.start();
        announcements.scheduleAtFixedRate(()->notifySsdp("ssdp:alive"),0,30,TimeUnit.SECONDS);
    }
    private void notifySsdp(String nts) {
        if(ssdp==null || ssdp.isClosed())return;
        for(String type:new String[]{"upnp:rootdevice",DEVICE,CD,CM,uuid}) try {
            String message="NOTIFY * HTTP/1.1\r\nHOST: 239.255.255.250:1900\r\nCACHE-CONTROL: max-age=90\r\nLOCATION: "+location()+"/device.xml\r\nNT: "+type+"\r\nNTS: "+nts+"\r\nUSN: "+uuid+(type.equals(uuid)?"":"::"+type)+"\r\n\r\n";
            byte[] b=message.getBytes(StandardCharsets.US_ASCII);ssdp.send(new DatagramPacket(b,b.length,InetAddress.getByName("239.255.255.250"),1900));
        } catch(IOException ignored) {}
    }
    @Override public void close() {
        if(closed)return;closed=true;notifySsdp("ssdp:byebye");announcements.shutdownNow();
        if(ssdp!=null)ssdp.close();try {http.close();}catch(IOException ignored){}
        for(HttpURLConnection c:upstream)c.disconnect();upstream.clear();
        for(Socket c:clients)try{c.close();}catch(IOException ignored){}clients.clear();workers.shutdownNow();routes.clear();routeNames.clear();routeOrder.clear();
    }
}
