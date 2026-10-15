import com.miflix.native2.data.LocalCastServer;
import com.miflix.native2.data.LocalCastAccess;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

public final class LocalCastCheck {
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    static byte[] ip(int a,int b,int c,int d){return new byte[]{(byte)a,(byte)b,(byte)c,(byte)d};}
    static String call(LocalCastServer s,InetAddress local,String method,String path,String body,String token,String origin) throws Exception {
        URL u=new URL(s.getUrl());try(Socket socket=new Socket()) {
            socket.bind(new InetSocketAddress(local,0));socket.connect(new InetSocketAddress(u.getHost(),u.getPort()),2000);socket.setSoTimeout(4000);
            byte[] b=body.getBytes(StandardCharsets.UTF_8);String h=method+" "+path+" HTTP/1.1\r\nHost: "+u.getHost()+":"+u.getPort()+"\r\nOrigin: "+origin+"\r\nContent-Length: "+b.length+"\r\n"+(token.isEmpty()?"":"Authorization: Bearer "+token+"\r\n")+"\r\n";
            socket.getOutputStream().write(h.getBytes(StandardCharsets.UTF_8));socket.getOutputStream().write(b);return new String(socket.getInputStream().readAllBytes(),StandardCharsets.UTF_8);
        }
    }
    public static void main(String[] args)throws Exception{
        check(LocalCastServer.sameSubnet(ip(192,168,1,2),ip(192,168,1,99),24),"same Wi-Fi subnet");
        check(!LocalCastServer.sameSubnet(ip(192,168,1,2),ip(192,168,2,2),24),"other subnet rejected");
        check(!LocalCastServer.sameSubnet(ip(10,1,0,2),ip(8,8,8,8),8),"public peer rejected");
        check(!LocalCastServer.sameSubnet(ip(172,16,1,2),ip(172,16,2,3),24),"prefix enforced");
        check(!LocalCastServer.isPrivate(ip(172,32,1,2)) && LocalCastServer.isPrivate(ip(172,31,1,2)),"RFC1918 boundaries");
        check(!LocalCastServer.sameSubnet(new byte[16],ip(10,0,0,1),24),"IPv6 not silently accepted");
        final long[] clock={1000L};LocalCastAccess access=new LocalCastAccess("123456",()->clock[0]);
        check(access.connect("bad", "https://brunio.example","192.168.1.2")==null,"wrong code rejected");
        String sessionToken=access.connect("123456","https://brunio.example","192.168.1.2");
        check(sessionToken!=null && sessionToken.length()==48,"random session sessionToken");
        check(access.authenticated(sessionToken,"https://brunio.example"),"valid origin accepted");
        check(!access.authenticated(sessionToken,"https://other.example") && !access.authenticated("bad","https://brunio.example"),"wrong origin and sessionToken rejected");
        check(!access.duplicate(sessionToken,"request123") && access.duplicate(sessionToken,"request123"),"duplicate request suppressed");
        check(!access.duplicate("other-sessionToken","request123"),"different sessions do not share duplicate IDs");
        clock[0]+=90001;check(!access.duplicate(sessionToken,"request123"),"dedupe expires");
        for(int i=0;i<5;i++)access.connect("bad","https://brunio.example","192.168.1.3");
        check(access.connect("123456","https://brunio.example","192.168.1.3")==null,"pairing rate limit");
        clock[0]=121000;check(access.connect("123456","https://brunio.example","192.168.1.4")==null,"pair code expires after two minutes");
        check(access.authenticated(sessionToken,"https://brunio.example"),"paired session outlives pairing code");
        clock[0]=1801000;check(!access.authenticated(sessionToken,"https://brunio.example"),"session expires after thirty minutes");
        clock[0]=1000;access.clear();check(!access.authenticated(sessionToken,"https://brunio.example"),"disconnect revokes sessions");
        Inet4Address address=null;int prefix=24;
        for(java.util.Enumeration<NetworkInterface> n=NetworkInterface.getNetworkInterfaces();n.hasMoreElements();) {
            for(InterfaceAddress a:n.nextElement().getInterfaceAddresses())if(a.getAddress() instanceof Inet4Address && LocalCastServer.isPrivate(a.getAddress().getAddress())){address=(Inet4Address)a.getAddress();prefix=a.getNetworkPrefixLength();}
        }
        if(address==null){System.out.println("PASS: subnet boundaries, pairing, origin-bound auth, deduplication, rate limit, expiry and revocation. SKIP socket transport: no private interface in this runtime.");return;}
        AtomicInteger sent=new AtomicInteger();String origin="https://brunio.example";
        try(LocalCastServer s=new LocalCastServer(address,prefix,"local controller".getBytes(StandardCharsets.UTF_8),(cloud,action,season,episode)->sent.incrementAndGet())) {
            check(call(s,address,"GET","/","","",origin).contains("local controller"),"controller served");
            check(call(s,address,"POST","/send","cloud=tmdb%3Amovie%3A1&action=play&request=abcdefgh","",origin).contains("401"),"no token rejected");
            check(call(s,address,"POST","/connect","code=000000x","",origin).contains("403"),"wrong code rejected");
            String paired=call(s,address,"POST","/connect","code="+s.getCode(),"",origin);
            check(paired.contains("200"),"correct pairing code");String token=paired.split("\"token\":\"")[1].split("\"")[0];
            String body="cloud=tmdb%3Amovie%3A123&action=play&request=abcdefgh&season=1&episode=1";
            check(call(s,address,"POST","/send",body,token,origin).contains("202"),"authenticated send accepted");
            call(s,address,"POST","/send",body,token,origin);check(sent.get()==1,"duplicate request dispatched once");
            check(call(s,address,"POST","/send",body,token,"https://other.example").contains("401"),"token bound to browser origin");
            check(call(s,address,"POST","/send",body.replace("tmdb%3Amovie%3A123","https%3A%2F%2Fexample.com"),token,origin).contains("400"),"stream URLs rejected");
            check(call(s,address,"POST","/send",body.replace("season=1","season=0"),token,origin).contains("400"),"bad episode input rejected");
            check(call(s,address,"OPTIONS","/send","","",origin).contains("Access-Control-Allow-Headers: Authorization"),"CORS preflight supported");
            for(int i=0;i<5;i++)call(s,address,"POST","/connect","code=not-a-code","",origin);
            check(call(s,address,"POST","/connect","code="+s.getCode(),"",origin).contains("403"),"pairing attempts rate limited");
        }
        System.out.println("PASS: private subnet, peer exclusion, pairing, auth, origin binding, deduplication, input validation, CORS and rate limit");
    }
}
