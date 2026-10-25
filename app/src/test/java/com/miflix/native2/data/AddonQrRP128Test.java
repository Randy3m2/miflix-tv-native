package com.miflix.native2.data;

import org.junit.Test;
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
    private static String token(String html) {
        Matcher m=Pattern.compile("name='csrf' value='([^']+)'").matcher(html);assertTrue("Form must contain CSRF input; status: "+html.lines().findFirst().orElse("empty response"),m.find());return m.group(1);
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
        String link="https://addon.example/private-configuration/manifest.json";
        AtomicInteger calls=new AtomicInteger();
        try(AddonManifestServerRP128 server=AddonManifestServerRP128.loopbackForTests(value->{assertEquals(link,value);calls.incrementAndGet();return true;})) {
            URI uri=URI.create(server.getUrl());assertFalse(uri.toString().contains(link));
            String html=request(uri,"GET",uri.getPath(),null,"");assertTrue("Expected GET 200: "+html.lines().findFirst().orElse("empty response"),html.startsWith("HTTP/1.1 200"));assertTrue(html.contains("name='manifest'"));
            String path=uri.getPath().replace("/add/","/save/");
            String body="csrf="+token(html)+"&manifest="+URLEncoder.encode(link,"UTF-8");
            assertTrue(request(uri,"POST",path,"https://other.example",body).startsWith("HTTP/1.1 403"));assertEquals(0,calls.get());
            assertTrue(request(uri,"POST",path,server.getBase(),"csrf=wrong&manifest="+URLEncoder.encode(link,"UTF-8")).startsWith("HTTP/1.1 403"));
            String result=request(uri,"POST",path,server.getBase(),body);assertTrue(result.startsWith("HTTP/1.1 200"));assertFalse(result.contains(link));assertEquals(1,calls.get());
            assertTrue(request(uri,"POST",path,server.getBase(),body).startsWith("HTTP/1.1 409"));assertEquals(1,calls.get());
        }
    }
    @Test public void rejectedDeliveryCanBeRetried() throws Exception {
        AtomicInteger attempts=new AtomicInteger();
        try(AddonManifestServerRP128 server=AddonManifestServerRP128.loopbackForTests(value->attempts.incrementAndGet()>1)) {
            URI uri=URI.create(server.getUrl());String html=request(uri,"GET",uri.getPath(),null,"");
            String path=uri.getPath().replace("/add/","/save/");String body="csrf="+token(html)+"&manifest=https%3A%2F%2Faddon.example%2Fmanifest.json";
            assertTrue(request(uri,"POST",path,server.getBase(),body).startsWith("HTTP/1.1 409"));
            assertTrue(request(uri,"POST",path,server.getBase(),body).startsWith("HTTP/1.1 200"));assertEquals(2,attempts.get());
        }
    }
    @Test public void lanAdmissionAcceptsRoutedPrivatePeersAndRejectsPublic() throws Exception {
        assertTrue(AddonManifestServerRP128.sameLan(InetAddress.getByName("192.168.1.2").getAddress(),InetAddress.getByName("192.168.1.3").getAddress(),24));
        assertTrue(AddonManifestServerRP128.acceptsPeer(InetAddress.getByName("192.168.2.3").getAddress()));
        assertTrue(AddonManifestServerRP128.acceptsPeer(InetAddress.getByName("10.0.0.8").getAddress()));
        assertFalse(AddonManifestServerRP128.privateAddress(InetAddress.getByName("8.8.8.8").getAddress()));
    }
    @Test(expected=IOException.class) public void publicConstructorDoesNotEnableLoopbackFixture() throws Exception {
        try(AddonManifestServerRP128 ignored=new AddonManifestServerRP128((Inet4Address)InetAddress.getByName("127.0.0.1"),8,value->true)) {fail("Must reject non-LAN address");}
    }

}
