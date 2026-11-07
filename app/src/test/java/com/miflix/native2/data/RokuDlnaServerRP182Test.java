package com.miflix.native2.data;

import org.junit.Test;
import static org.junit.Assert.*;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/** HTTP/DLNA contract fixtures only. These tests do not claim physical Roku compatibility. */
public class RokuDlnaServerRP182Test {
    // Socket fixture rather than the JDK HTTP-server module, which Android unit tests exclude.
    private interface Handler {void handle(Exchange exchange) throws IOException;}
    private static final class Headers extends HashMap<String,String> {
        void set(String key,String value){put(key.toLowerCase(Locale.US),value);}
        String getFirst(String key){return get(key.toLowerCase(Locale.US));}
    }
    private static final class Exchange implements Closeable {
        final Socket socket;final String method;final Headers request=new Headers(),response=new Headers();
        final ByteArrayOutputStream body=new ByteArrayOutputStream();int status=200;long length=-1;boolean closed;
        Exchange(Socket socket,String method){this.socket=socket;this.method=method;}
        Headers getRequestHeaders(){return request;}Headers getResponseHeaders(){return response;}
        String getRequestMethod(){return method;}OutputStream getResponseBody(){return body;}
        void sendResponseHeaders(int code,long size){status=code;length=size;}
        public void close() throws IOException {
            if(closed)return;closed=true;
            OutputStream out=socket.getOutputStream();StringBuilder h=new StringBuilder("HTTP/1.1 "+status+" Fixture\r\nConnection: close\r\n");
            if(response.getFirst("Content-Length")==null && length>=0)response.set("Content-Length",Long.toString(length));
            for(Map.Entry<String,String> header:response.entrySet())h.append(header.getKey()).append(": ").append(header.getValue()).append("\r\n");
            h.append("\r\n");out.write(h.toString().getBytes(StandardCharsets.US_ASCII));if(!method.equals("HEAD"))body.writeTo(out);out.flush();socket.close();
        }
    }
    private static final class HttpServer {
        final ServerSocket listener;final Map<String,Handler> handlers=new HashMap<>();
        HttpServer(InetSocketAddress address) throws IOException {listener=new ServerSocket();listener.bind(address);}
        static HttpServer create(InetSocketAddress address,int backlog) throws IOException {return new HttpServer(address);}
        void createContext(String path,Handler handler){handlers.put(path,handler);}
        InetSocketAddress getAddress(){return (InetSocketAddress)listener.getLocalSocketAddress();}
        void start(){Thread accept=new Thread(()->{while(!listener.isClosed())try {
            Socket client=listener.accept();client.setSoTimeout(3000);
            Thread worker=new Thread(()->{try {
                BufferedReader in=new BufferedReader(new InputStreamReader(client.getInputStream(),StandardCharsets.US_ASCII));String first=in.readLine();if(first==null){client.close();return;}
                String[] parts=first.split(" ");Exchange exchange=new Exchange(client,parts[0]);String line;
                while((line=in.readLine())!=null && !line.isEmpty()){int colon=line.indexOf(':');if(colon>0)exchange.request.set(line.substring(0,colon),line.substring(colon+1).trim());}
                Handler handler=handlers.get(parts[1]);if(handler!=null)handler.handle(exchange);else exchange.sendResponseHeaders(404,0);exchange.close();
            } catch(IOException e){try{client.close();}catch(IOException ignored){}}});worker.setDaemon(true);worker.start();
        } catch(IOException e){break;}});accept.setDaemon(true);accept.start();}
        void stop(int delay){try{listener.close();}catch(IOException ignored){}}
    }
    private static String text(HttpURLConnection c) throws IOException {
        InputStream in=c.getResponseCode()>=400?c.getErrorStream():c.getInputStream();
        ByteArrayOutputStream out=new ByteArrayOutputStream();if(in!=null)try(InputStream input=in){byte[] b=new byte[2048];int n;while((n=input.read(b))!=-1)out.write(b,0,n);}return out.toString("UTF-8");
    }
    private static RokuDlnaServerRP182 server(String origin,String mime,Map<String,String> headers) throws Exception {
        return new RokuDlnaServerRP182((Inet4Address)InetAddress.getByName("127.0.0.1"),8,
            new RokuDlnaServerRP182.Media("Live & selected",origin,mime,"Live TV",headers),false);
    }
    @Test public void descriptionAndBrowseExposeOnlyLocalSelectedMedia() throws Exception {
        try(RokuDlnaServerRP182 s=server("https://private.test/movie?credential=hidden","video/mp4",Collections.emptyMap())) {
            String description=text((HttpURLConnection)new URL(s.location()+"/device.xml").openConnection());
            assertTrue(description.contains("MediaServer:1"));assertTrue(description.contains("ContentDirectory:1"));
            HttpURLConnection c=(HttpURLConnection)new URL(s.location()+"/cd/control").openConnection();c.setRequestMethod("POST");c.setDoOutput(true);c.setRequestProperty("SOAPACTION","\"urn:schemas-upnp-org:service:ContentDirectory:1#Browse\"");
            c.getOutputStream().write("<Browse><ObjectID>f0</ObjectID><BrowseFlag>BrowseDirectChildren</BrowseFlag></Browse>".getBytes(StandardCharsets.UTF_8));
            String browse=text(c);assertTrue(browse.contains("Live &amp;amp; selected"));assertTrue(browse.contains("/media/"));assertFalse(browse.contains("credential"));assertFalse(browse.contains("private.test"));
            javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(browse.getBytes(StandardCharsets.UTF_8)));
        }
    }
    @Test public void mp4RangesAndHeadAreForwarded() throws Exception {
        HttpServer origin=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        AtomicReference<String> range=new AtomicReference<>();
        origin.createContext("/movie.mp4",exchange->{
            range.set(exchange.getRequestHeaders().getFirst("Range"));exchange.getResponseHeaders().set("Content-Type","video/mp4");
            if(exchange.getRequestMethod().equals("HEAD")){exchange.getResponseHeaders().set("Content-Length","10");exchange.sendResponseHeaders(200,-1);}
            else {exchange.getResponseHeaders().set("Content-Range","bytes 2-4/10");exchange.sendResponseHeaders(206,3);exchange.getResponseBody().write("234".getBytes(StandardCharsets.US_ASCII));}exchange.close();
        });origin.start();
        try(RokuDlnaServerRP182 s=server("http://127.0.0.1:"+origin.getAddress().getPort()+"/movie.mp4","video/mp4",Collections.emptyMap())) {
            HttpURLConnection c=(HttpURLConnection)new URL(s.mediaUrl()).openConnection();c.setRequestProperty("Range","bytes=2-4");
            assertEquals("234",text(c));assertEquals(206,c.getResponseCode());assertEquals("bytes=2-4",range.get());assertEquals("bytes 2-4/10",c.getHeaderField("Content-Range"));
            HttpURLConnection head=(HttpURLConnection)new URL(s.mediaUrl()).openConnection();head.setRequestMethod("HEAD");assertEquals(200,head.getResponseCode());assertEquals("10",head.getHeaderField("Content-Length"));head.disconnect();
        } finally {origin.stop(0);}
    }
    @Test public void livePlaylistAndSegmentsStayLocalAndKeepPrivateHeadersUpstream() throws Exception {
        HttpServer origin=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);AtomicReference<String> auth=new AtomicReference<>();
        origin.createContext("/live.m3u8",exchange->{exchange.getResponseHeaders().set("Content-Type","application/vnd.apple.mpegurl");byte[] b="#EXTM3U\n#EXT-X-TARGETDURATION:4\n#EXTINF:4,\nsegment.ts\n".getBytes(StandardCharsets.UTF_8);exchange.sendResponseHeaders(200,b.length);exchange.getResponseBody().write(b);exchange.close();});
        origin.createContext("/segment.ts",exchange->{auth.set(exchange.getRequestHeaders().getFirst("Authorization"));byte[] b="segment".getBytes(StandardCharsets.UTF_8);exchange.sendResponseHeaders(200,b.length);exchange.getResponseBody().write(b);exchange.close();});origin.start();
        try(RokuDlnaServerRP182 s=server("http://127.0.0.1:"+origin.getAddress().getPort()+"/live.m3u8","application/vnd.apple.mpegurl",Collections.singletonMap("Authorization","Bearer fixture-secret"))) {
            String playlist=text((HttpURLConnection)new URL(s.mediaUrl()).openConnection());assertFalse(playlist.contains("fixture-secret"));assertFalse(playlist.contains("segment.ts"));assertTrue(playlist.contains("#EXTINF:4"));
            String path=Arrays.stream(playlist.split("\n")).filter(line->line.startsWith("/media/")).findFirst().get();
            assertEquals("segment",text((HttpURLConnection)new URL(s.location()+path).openConnection()));assertEquals("Bearer fixture-secret",auth.get());
        } finally {origin.stop(0);}
    }
    @Test public void arbitraryProxyUrlsAndWrongTokensAreRejectedAndStopReleasesPort() throws Exception {
        RokuDlnaServerRP182 s=server("https://example.test/movie.mp4","video/mp4",Collections.emptyMap());String location=s.location();
        assertEquals(404,((HttpURLConnection)new URL(location+"/proxy?url=http://example.com").openConnection()).getResponseCode());
        assertEquals(404,((HttpURLConnection)new URL(location+"/media/wrong/root.mp4").openConnection()).getResponseCode());s.close();
        try {new URL(location+"/device.xml").openConnection().getInputStream();fail("Server must release its listener");} catch(IOException expected) {}
    }
    @Test public void drmPlaylistsReturnExplicitUnsupportedFailure() throws Exception {
        HttpServer origin=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);origin.createContext("/drm.m3u8",exchange->{byte[] b="#EXTM3U\n#EXT-X-KEY:METHOD=SAMPLE-AES,URI=\"private.key\"".getBytes(StandardCharsets.UTF_8);exchange.getResponseHeaders().set("Content-Type","application/vnd.apple.mpegurl");exchange.sendResponseHeaders(200,b.length);exchange.getResponseBody().write(b);exchange.close();});origin.start();
        try(RokuDlnaServerRP182 s=server("http://127.0.0.1:"+origin.getAddress().getPort()+"/drm.m3u8","application/vnd.apple.mpegurl",Collections.emptyMap())) {
            HttpURLConnection c=(HttpURLConnection)new URL(s.mediaUrl()).openConnection();assertEquals(415,c.getResponseCode());assertEquals("DRM is unsupported",text(c));
        } finally {origin.stop(0);}
    }
}
