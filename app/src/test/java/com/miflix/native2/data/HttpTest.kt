package com.miflix.native2.data
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import kotlin.concurrent.thread
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.After
import org.junit.Test
class HttpTest {
    @After fun reset() { Http.authorize=null }
    /** Minimal local HTTP server (the JDK's com.sun.net.httpserver is not on the Android unit-test classpath). */
    private class TestServer(val reply:(String?)->Pair<Int,String>) {
        private val socket=ServerSocket(0,50,InetAddress.getByName("127.0.0.1"))
        val address:InetSocketAddress get()=socket.localSocketAddress as InetSocketAddress
        init {
            thread(isDaemon=true) {
                while(!socket.isClosed) {
                    val client=try { socket.accept() } catch(_:Exception) { break }
                    thread(isDaemon=true) {
                        client.use { c ->
                            try {
                                val input=c.getInputStream().bufferedReader(Charsets.ISO_8859_1)
                                var auth:String?=null
                                while(true) {
                                    val line=input.readLine()?:return@thread
                                    if(line.isEmpty()) break
                                    if(line.startsWith("Authorization:",ignoreCase=true)) auth=line.substringAfter(':').trim()
                                }
                                val (status,body)=reply(auth);val bytes=body.toByteArray()
                                val out=c.getOutputStream()
                                out.write("HTTP/1.1 $status X\r\nContent-Type: text/plain\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n".toByteArray(Charsets.ISO_8859_1))
                                out.write(bytes);out.flush()
                            } catch(_:Exception) {}
                        }
                    }
                }
            }
        }
        fun stop(@Suppress("UNUSED_PARAMETER") delay:Int) { runCatching { socket.close() } }
    }
    private fun server(reply:(String?)->Pair<Int,String>)=TestServer(reply)
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
