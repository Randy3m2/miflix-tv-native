import com.miflix.native2.data.TorBoxKeyServerRP76;
import com.miflix.native2.data.LocalCastServer;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.*;

public class TorBoxQrRP76Check {
    static void check(boolean ok) {if(!ok) throw new AssertionError();}
    static String request(Inet4Address a,int port,String method,String path,String origin,String body) throws Exception {
        try(Socket socket=new Socket(a,port)) {
            socket.setSoTimeout(4000);
            byte[] bytes=body.getBytes(StandardCharsets.UTF_8);
            String headers=method+" "+path+" HTTP/1.1\r\nHost: "+a.getHostAddress()+":"+port+"\r\n"+
                (origin==null?"":"Origin: "+origin+"\r\n")+"Content-Type: application/x-www-form-urlencoded\r\nContent-Length: "+bytes.length+"\r\n\r\n";
            socket.getOutputStream().write(headers.getBytes(StandardCharsets.UTF_8));socket.getOutputStream().write(bytes);socket.getOutputStream().flush();
            ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[4096];int n;
            while((n=socket.getInputStream().read(buffer))>=0)out.write(buffer,0,n);
            return out.toString("UTF-8");
        }
    }
    public static void main(String[] args) throws Exception {
        check(TorBoxKeyServerRP76.valid("a".repeat(16)));
        check(!TorBoxKeyServerRP76.valid("short"));check(!TorBoxKeyServerRP76.valid("a".repeat(257)));
        check(!TorBoxKeyServerRP76.valid("a".repeat(16)+"\n"));check(!TorBoxKeyServerRP76.valid("a".repeat(16)+" "));
        check(TorBoxKeyServerRP76.acceptsForm("http://tv","http://tv","secret","secret"));
        check(!TorBoxKeyServerRP76.acceptsForm("http://tv","https://evil.example","secret","secret"));
        check(!TorBoxKeyServerRP76.acceptsForm("http://tv","http://tv","secret","wrong"));
        Inet4Address address=null;int prefix=0;
        for(NetworkInterface iface:Collections.list(NetworkInterface.getNetworkInterfaces()))for(InterfaceAddress ia:iface.getInterfaceAddresses())
            if(ia.getAddress() instanceof Inet4Address && LocalCastServer.isPrivate(ia.getAddress().getAddress()) && ia.getNetworkPrefixLength()>=8 && ia.getNetworkPrefixLength()<=30) {
                address=(Inet4Address)ia.getAddress();prefix=ia.getNetworkPrefixLength();break;
            }
        if(address==null) {System.out.println("PASS validation and Origin/CSRF; SKIP socket test: no private IPv4 LAN");return;}
        AtomicInteger calls=new AtomicInteger();String apiKey="test-local-key-0123456789";
        try(TorBoxKeyServerRP76 server=new TorBoxKeyServerRP76(address,prefix,key->{check(key.equals(apiKey));calls.incrementAndGet();return true;})) {
            URI u=URI.create(server.getUrl());String page=request(address,u.getPort(),"GET",u.getPath(),null,"");
            check(page.startsWith("HTTP/1.1 200"));check(page.contains("type='password'"));check(!page.contains(apiKey));
            Matcher m=Pattern.compile("name='csrf' value='([^']+)'").matcher(page);check(m.find());String csrf=m.group(1);
            String path=u.getPath().replace("/add/","/save/");String body="csrf="+csrf+"&apiKey="+URLEncoder.encode(apiKey,"UTF-8");
            check(request(address,u.getPort(),"POST",path,"https://evil.example",body).startsWith("HTTP/1.1 403"));check(calls.get()==0);
            check(request(address,u.getPort(),"POST",path,server.getBase(),"csrf=bad&apiKey="+apiKey).startsWith("HTTP/1.1 403"));
            String result=request(address,u.getPort(),"POST",path,server.getBase(),body);
            check(result.startsWith("HTTP/1.1 200") && !result.contains(apiKey));check(calls.get()==1);
            check(request(address,u.getPort(),"POST",path,server.getBase(),body).startsWith("HTTP/1.1 409"));check(calls.get()==1);
        }
        System.out.println("PASS validation, Origin/CSRF, LAN form, secret-free response, one-use delivery");
    }
}
