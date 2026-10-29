package com.miflix.native2.data;

import org.junit.Test;
import static org.junit.Assert.*;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.*;

public class SearchKeyboardRP136Test {
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
        Matcher m=Pattern.compile("csrf:'([^']+)'").matcher(html);assertTrue("Form must contain CSRF input; status: "+html.lines().findFirst().orElse("empty response"),m.find());return m.group(1);
    }
    @Test public void repeatedEditsDeleteAndRejectStaleOrForeignRequests() throws Exception {
        List<String> edits=new ArrayList<>();
        try(SearchKeyboardServerRP136 server=SearchKeyboardServerRP136.loopbackForTests(value->{edits.add(value);return true;})) {
            URI uri=URI.create(server.getUrl());String html=request(uri,"GET",uri.getPath(),null,"");
            assertTrue(html.startsWith("HTTP/1.1 200"));assertTrue(html.contains("addEventListener('input'"));
            String path=uri.getPath().replace("/add/","/save/");String csrf="csrf="+token(html);
            assertTrue(request(uri,"POST",path,"https://evil.example",csrf+"&sequence=1&text=bad").startsWith("HTTP/1.1 403"));
            assertTrue(request(uri,"POST",path,null,"csrf=wrong&sequence=1&text=bad").startsWith("HTTP/1.1 403"));
            assertTrue(request(uri,"POST",path,null,csrf+"&sequence=1&text="+URLEncoder.encode("El niño","UTF-8")).startsWith("HTTP/1.1 200"));
            assertTrue(request(uri,"POST",path,null,csrf+"&sequence=3&text=Naruto").startsWith("HTTP/1.1 200"));
            request(uri,"POST",path,null,csrf+"&sequence=2&text=stale");
            request(uri,"POST",path,null,csrf+"&sequence=4&text=");
            assertEquals(Arrays.asList("El niño","Naruto",""),edits);
            assertFalse(server.getUrl().contains("El niño"));
        }
    }
    @Test public void rejectedDeliveryRetriesSameRevision() throws Exception {
        AtomicInteger calls=new AtomicInteger();
        try(SearchKeyboardServerRP136 server=SearchKeyboardServerRP136.loopbackForTests(value->calls.incrementAndGet()>1)) {
            URI uri=URI.create(server.getUrl());String html=request(uri,"GET",uri.getPath(),null,"");
            String body="csrf="+token(html)+"&sequence=1&text=hello";
            String path=uri.getPath().replace("/add/","/save/");
            assertTrue(request(uri,"POST",path,null,body).startsWith("HTTP/1.1 409"));
            assertTrue(request(uri,"POST",path,null,body).startsWith("HTTP/1.1 200"));assertEquals(2,calls.get());
        }
    }
    @Test public void phoneDictationKeepsTheLocalTextDeliveryPath() throws Exception {
        List<String> edits=new ArrayList<>();
        try(SearchKeyboardServerRP136 server=SearchKeyboardServerRP136.loopbackForTests(value->{edits.add(value);return true;})) {
            URI uri=URI.create(server.getUrl());String html=request(uri,"GET",uri.getPath(),null,"");
            assertTrue(html.contains("id='dictate'"));
            assertTrue(html.contains("input.focus()"));
            assertTrue(html.contains("inputmode='text'"));
            assertFalse(html.contains("getUserMedia"));
            String body="csrf="+token(html)+"&sequence=1&text="+URLEncoder.encode("Películas de acción","UTF-8");
            assertTrue(request(uri,"POST",uri.getPath().replace("/add/","/save/"),null,body).startsWith("HTTP/1.1 200"));
            assertEquals(Collections.singletonList("Películas de acción"),edits);
        }
    }
    @Test public void boundsAndNetworkRules() {
        assertTrue(SearchKeyboardServerRP136.valid(""));assertTrue(SearchKeyboardServerRP136.valid("acción 한국어"));
        assertFalse(SearchKeyboardServerRP136.valid("x".repeat(257)));assertFalse(SearchKeyboardServerRP136.valid("a\nb"));
        assertTrue(SearchKeyboardServerRP136.acceptsPeer(new byte[]{(byte)192,(byte)168,2,3}));
        assertFalse(SearchKeyboardServerRP136.acceptsPeer(new byte[]{8,8,8,8}));
    }
}
