package com.miflix.native2.data;

import org.junit.Test;
import org.junit.Assume;
import static org.junit.Assert.*;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.*;

public class AddonQrRP128Test {
    private static String request(URI uri,String method,String path,String origin,String body) throws Exception {
        try(Socket socket=new Socket(uri.getHost(),uri.getPort())) {
            socket.setSoTimeout(5000);
            byte[] bytes=body.getBytes(StandardCharsets.UTF_8);
            String headers=method+" "+path+" HTTP/1.1\r\nHost: "+uri.getHost()+":"+uri.getPort()+"\r\n"+
                (origin==null?"":"Origin: "+origin+"\r\n")+"Content-Type: application/x-www-form-urlencoded\r\nContent-Length: "+bytes.length+"\r\n\r\n";
            socket.getOutputStream().write(headers.getBytes(StandardCharsets.UTF_8));socket.getOutputStream().write(bytes);socket.getOutputStream().flush();
            ByteArrayOutputStream response=new ByteArrayOutputStream();byte[] buffer=new byte[4096];int n;
            while((n=socket.getInputStream().read(buffer))>=0) response.write(buffer,0,n);
            return response.toString("UTF-8");
        }
    }
    private static InterfaceAddress lan() throws Exception {
        for(NetworkInterface iface:Collections.list(NetworkInterface.getNetworkInterfaces())) for(InterfaceAddress address:iface.getInterfaceAddresses())
            if(address.getAddress() instanceof Inet4Address && LocalCastServer.isPrivate(address.getAddress().getAddress()) && address.getNetworkPrefixLength()>=8 && address.getNetworkPrefixLength()<=30) return address;
        return null;
    }
    private static String token(String html) {
        Matcher m=Pattern.compile("name='csrf' value='([^']+)'").matcher(html);assertTrue(m.find());return m.group(1);
    }
    @Test public void acceptsConfiguredAndStremioLinks() {
        assertTrue(AddonManifestServerRP128.valid("https://addon.example/private/manifest.json"));
        assertTrue(AddonManifestServerRP128.valid("stremio://addon.example/private/manifest.json"));
        assertFalse(AddonManifestServerRP128.valid("javascript:bad"));
        assertFalse(AddonManifestServerRP128.valid("https://addon.example/manifest.json?secret=1"));
    }
    @Test public void requiresFormSecretEvenWithoutOrigin() {
        assertTrue(AddonManifestServerRP128.acceptsForm("http://tv",null,"secret","secret"));
        assertTrue(AddonManifestServerRP128.acceptsForm("http://tv","null","secret","secret"));
        assertFalse(AddonManifestServerRP128.acceptsForm("http://tv",null,"secret",""));
    }
    @Test public void rejectsForeignOriginAndWrongSecret() {
        assertFalse(AddonManifestServerRP128.acceptsForm("http://tv","https://other.example","secret","secret"));
        assertFalse(AddonManifestServerRP128.acceptsForm("http://tv","http://tv","secret","wrong"));
    }
    @Test public void localFormDeliversOnceWithoutEchoingPrivateUrl() throws Exception {
        InterfaceAddress address=lan();Assume.assumeNotNull(address);
        String link="https://addon.example/private-configuration/manifest.json";
        AtomicInteger calls=new AtomicInteger();
        try(AddonManifestServerRP128 server=new AddonManifestServerRP128((Inet4Address)address.getAddress(),address.getNetworkPrefixLength(),value->{assertEquals(link,value);calls.incrementAndGet();return true;})) {
            URI uri=URI.create(server.getUrl());assertFalse(uri.toString().contains(link));
            String html=request(uri,"GET",uri.getPath(),null,"");assertTrue(html.startsWith("HTTP/1.1 200"));assertTrue(html.contains("name='manifest'"));
            String path=uri.getPath().replace("/add/","/save/");
            String body="csrf="+token(html)+"&manifest="+URLEncoder.encode(link,"UTF-8");
            assertTrue(request(uri,"POST",path,"https://other.example",body).startsWith("HTTP/1.1 403"));assertEquals(0,calls.get());
            assertTrue(request(uri,"POST",path,server.getBase(),"csrf=wrong&manifest="+URLEncoder.encode(link,"UTF-8")).startsWith("HTTP/1.1 403"));
            String result=request(uri,"POST",path,server.getBase(),body);assertTrue(result.startsWith("HTTP/1.1 200"));assertFalse(result.contains(link));assertEquals(1,calls.get());
            assertTrue(request(uri,"POST",path,server.getBase(),body).startsWith("HTTP/1.1 409"));assertEquals(1,calls.get());
        }
    }
    @Test public void rejectedDeliveryCanBeRetried() throws Exception {
        InterfaceAddress address=lan();Assume.assumeNotNull(address);AtomicInteger attempts=new AtomicInteger();
        try(AddonManifestServerRP128 server=new AddonManifestServerRP128((Inet4Address)address.getAddress(),address.getNetworkPrefixLength(),value->attempts.incrementAndGet()>1)) {
            URI uri=URI.create(server.getUrl());String html=request(uri,"GET",uri.getPath(),null,"");
            String path=uri.getPath().replace("/add/","/save/");String body="csrf="+token(html)+"&manifest=https%3A%2F%2Faddon.example%2Fmanifest.json";
            assertTrue(request(uri,"POST",path,server.getBase(),body).startsWith("HTTP/1.1 409"));
            assertTrue(request(uri,"POST",path,server.getBase(),body).startsWith("HTTP/1.1 200"));assertEquals(2,attempts.get());
        }
    }
}
