package com.miflix.native2.data
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.After
import org.junit.Test
class HttpTest {
    @After fun reset() { Http.authorize=null }
    private fun server(reply:(String?)->Pair<Int,String>):HttpServer = HttpServer.create(InetSocketAddress("127.0.0.1",0),0).apply {
        createContext("/test") { e -> val (status,body)=reply(e.requestHeaders.getFirst("Authorization"));val bytes=body.toByteArray();e.sendResponseHeaders(status,bytes.size.toLong());e.responseBody.use { it.write(bytes) };e.close() }
        start()
    }
    @Test fun unauthorizedRequestRetriesOnceWithNewToken()=runBlocking {
        var requests=0;var refreshes=0
        val server=server { bearer -> requests++;if(bearer=="Bearer fresh") 200 to "ok" else 401 to "expired" }
        try {
            Http.authorize={_,headers,force->if(force) { refreshes++;headers+("Authorization" to "Bearer fresh") } else headers}
            assertEquals("ok",Http.text("http://127.0.0.1:${server.address.port}/test",headers=mapOf("Authorization" to "Bearer old")))
            assertEquals(2,requests);assertEquals(1,refreshes)
        } finally { server.stop(0) }
    }
    @Test fun persistentUnauthorizedDoesNotLoop()=runBlocking {
        var requests=0
        val server=server { requests++;401 to "expired" }
        try {
            Http.authorize={_,headers,force->if(force) headers+("Authorization" to "Bearer fresh") else headers}
            try { Http.text("http://127.0.0.1:${server.address.port}/test");fail() } catch(e:HttpFailure) { assertEquals(401,e.status) }
            assertEquals(2,requests)
        } finally { server.stop(0) }
    }
    @Test fun forbiddenIsNotRetried()=runBlocking {
        var requests=0;var refreshes=0
        val server=server { requests++;403 to "forbidden" }
        try {
            Http.authorize={_,headers,force->if(force)refreshes++;headers}
            try { Http.text("http://127.0.0.1:${server.address.port}/test");fail() } catch(e:HttpFailure) { assertEquals(403,e.status) }
            assertEquals(1,requests);assertEquals(0,refreshes)
        } finally { server.stop(0) }
    }
}
