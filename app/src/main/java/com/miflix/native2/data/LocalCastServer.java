package com.miflix.native2.data;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.*;

/** Foreground-only, private IPv4 Wi-Fi receiver. No cloud commands, streams or credentials. */
public final class LocalCastServer implements AutoCloseable {
    public interface Commands { void send(String cloud, String action, int season, int episode); }
    private final Inet4Address address;
    private final int prefix;
    private final ServerSocket server;
    private final byte[] page;
    private final Commands commands;
    private final String code;
    private final LocalCastAccess access;
    private final ThreadPoolExecutor workers=new ThreadPoolExecutor(2,2,0L,TimeUnit.SECONDS,new ArrayBlockingQueue<>(8));
    private volatile boolean closed;
    public LocalCastServer(Inet4Address address,int prefix,byte[] page,Commands commands) throws IOException {
        if(!isPrivate(address.getAddress()) || prefix<8 || prefix>30) throw new IOException("Private Wi-Fi IPv4 required");
        this.address=address;this.prefix=prefix;this.page=page;this.commands=commands;
        code=String.format(Locale.US,"%06d",new SecureRandom().nextInt(1000000));
        access=new LocalCastAccess(code,System::currentTimeMillis);
        server=new ServerSocket();server.setReuseAddress(true);server.bind(new InetSocketAddress(address,8765),8);
        Thread thread=new Thread(this::accept,"brunio-local-cast");thread.setDaemon(true);thread.start();
    }
    public String getCode(){return code;}
    public String getUrl(){return "http://"+address.getHostAddress()+":"+server.getLocalPort();}
    public long getPairUntil(){return access.pairUntil;}
    public static boolean isPrivate(byte[] ip){return ip.length==4 && ((ip[0]&255)==10 || (ip[0]&255)==172 && (ip[1]&255)>=16 && (ip[1]&255)<=31 || (ip[0]&255)==192 && (ip[1]&255)==168);}
    public static boolean sameSubnet(byte[] a,byte[] b,int prefix){
        if(!isPrivate(a)||!isPrivate(b)||prefix<8||prefix>30)return false;
        for(int i=0;i<4;i++){int bits=Math.min(8,Math.max(0,prefix-i*8));int mask=bits==0?0:(255<<(8-bits))&255;if(((a[i]^b[i])&mask)!=0)return false;}return true;
    }
    private void accept(){while(!closed){try{Socket socket=server.accept();if(!sameSubnet(address.getAddress(),socket.getInetAddress().getAddress(),prefix)){socket.close();continue;}try{workers.execute(()->handle(socket));}catch(RejectedExecutionException e){socket.close();}}catch(IOException e){if(!closed)close();}}}
    private static String line(InputStream in,int max) throws IOException {
        ByteArrayOutputStream out=new ByteArrayOutputStream();int c;while((c=in.read())!=-1){if(c=='\n')return out.toString("UTF-8").replace("\r","");out.write(c);if(out.size()>max)throw new IOException("Header too large");}throw new EOFException();
    }
    private static String value(Map<String,String> map,String key,String fallback){String result=map.get(key);return result==null?fallback:result;}
    private void handle(Socket socket){try(Socket s=socket){s.setSoTimeout(3000);InputStream in=s.getInputStream();String[] request=line(in,2048).split(" ");if(request.length!=3)return;
        Map<String,String> h=new HashMap<>();int total=0;for(;;){String l=line(in,4096);total+=l.length();if(total>8192)return;if(l.isEmpty())break;int at=l.indexOf(':');if(at<=0)return;h.put(l.substring(0,at).toLowerCase(Locale.US),l.substring(at+1).trim());}
        String origin=value(h,"origin","");String host=value(h,"host","");
        if(!host.equals(address.getHostAddress()+":"+server.getLocalPort())){reply(s,403,"{}",origin);return;}
        if(!origin.isEmpty() && !origin.equals(getUrl()) && !origin.startsWith("https://")){reply(s,403,"{}",origin);return;}
        if(request[0].equals("OPTIONS")){reply(s,204,"",origin);return;}
        if(request[0].equals("GET") && request[1].equals("/")){write(s,200,"text/html; charset=utf-8",page,origin);return;}
        if(!request[0].equals("POST") || h.containsKey("transfer-encoding")){reply(s,405,"{}",origin);return;}
        int length=Integer.parseInt(value(h,"content-length","0"));if(length<=0||length>4096){reply(s,413,"{}",origin);return;}
        byte[] bytes=new byte[length];int read=0;while(read<length){int n=in.read(bytes,read,length-read);if(n<0)throw new EOFException();read+=n;}
        // URL encoded fields keep the protocol independently testable without Android JSON.
        Map<String,String> form=new HashMap<>();for(String field:new String(bytes,StandardCharsets.UTF_8).split("&")){String[] pair=field.split("=",2);if(pair.length==2)form.put(URLDecoder.decode(pair[0],"UTF-8"),URLDecoder.decode(pair[1],"UTF-8"));}
        if(request[1].equals("/connect")){
            String token=access.connect(value(form,"code",""),origin,s.getInetAddress().getHostAddress());
            reply(s,token==null?403:200,token==null?"{\"ok\":false}":"{\"ok\":true,\"token\":\""+token+"\"}",origin);return;
        }
        String auth=value(h,"authorization","");String token=auth.startsWith("Bearer ")?auth.substring(7):"";
        if(!access.authenticated(token,origin)){reply(s,401,"{\"ok\":false}",origin);return;}
        if(request[1].equals("/send")){
            String cloud=value(form,"cloud","");String action=value(form,"action","details");String id=value(form,"request","");
            if(!cloud.matches("tmdb:(movie|series):[1-9][0-9]{0,8}")||!action.matches("details|play")||!id.matches("[a-zA-Z0-9-]{8,80}")){reply(s,400,"{}",origin);return;}
            int season=Integer.parseInt(value(form,"season","1"));int episode=Integer.parseInt(value(form,"episode","1"));if(season<1||season>10000||episode<1||episode>10000){reply(s,400,"{}",origin);return;}
            if(!access.duplicate(token,id))commands.send(cloud,action,season,episode);
            reply(s,202,"{\"ok\":true,\"accepted\":true}",origin);return;
        }
        reply(s,404,"{}",origin);
    }catch(Exception ignored){/* Failed local requests never generate cloud logs. */}}
    private void reply(Socket s,int status,String body,String origin)throws IOException{write(s,status,"application/json; charset=utf-8",body.getBytes(StandardCharsets.UTF_8),origin);}
    private void write(Socket s,int status,String type,byte[] body,String origin)throws IOException{
        String cors=origin.isEmpty()?"":"Access-Control-Allow-Origin: "+origin+"\r\nVary: Origin\r\n";
        String header="HTTP/1.1 "+status+" Response\r\nContent-Type: "+type+"\r\nContent-Length: "+body.length+"\r\nConnection: close\r\nCache-Control: no-store\r\nReferrer-Policy: no-referrer\r\nX-Content-Type-Options: nosniff\r\n"+cors+"Access-Control-Allow-Methods: POST, OPTIONS\r\nAccess-Control-Allow-Headers: Authorization, Content-Type\r\nAccess-Control-Allow-Private-Network: true\r\n\r\n";
        OutputStream out=s.getOutputStream();out.write(header.getBytes(StandardCharsets.UTF_8));out.write(body);out.flush();
    }
    @Override public void close(){closed=true;try{server.close();}catch(IOException ignored){}workers.shutdownNow();access.clear();}
}
