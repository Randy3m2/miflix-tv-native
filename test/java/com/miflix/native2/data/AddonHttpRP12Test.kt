package com.miflix.native2.data

import java.io.Closeable
import java.io.IOException
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class AddonHttpRP12Test {
    @Test fun requestDeadlineReturnsBeforeSlowServerAndHttpErrorsAreSafe() = runBlocking {
        LocalHttpFixture().use {server ->
            // Warm the client and verify the HTTP status before timing the slow response.
            val http=runCatching {AddonHttpRP12.text(server.url("/error"),2000)}.exceptionOrNull()
            assertTrue(http is AddonHttpFailureRP12)
            assertEquals(503,(http as AddonHttpFailureRP12).status)
            assertFalse(http.message.orEmpty().contains("127.0.0.1"))

            val started=System.nanoTime()
            val failure=runCatching {AddonHttpRP12.text(server.url("/slow"),250)}.exceptionOrNull()
            val elapsedMs=TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-started)
            assertNotNull(failure)
            assertTrue("The slow endpoint was not reached",server.slowSeen.await(1,TimeUnit.SECONDS))
            assertTrue("Request did not honor its deadline: $elapsedMs ms",elapsedMs<2000)
        }
    }

    /** Uses Java/Android public APIs; no jdk.httpserver/com.sun dependency. */
    private class LocalHttpFixture:Closeable {
        private val server=ServerSocket(0,8,InetAddress.getByName("127.0.0.1"))
        private val sockets=Collections.synchronizedSet(mutableSetOf<Socket>())
        private val workers=Executors.newCachedThreadPool {r ->
            Thread(r,"addon-http-test").apply {isDaemon=true}
        }
        val slowSeen=CountDownLatch(1)
        init {
            workers.execute {
                while(!server.isClosed) {
                    try {
                        val socket=server.accept()
                        sockets.add(socket)
                        workers.execute {serve(socket)}
                    } catch(_:IOException) {break}
                }
            }
        }
        fun url(path:String)="http://127.0.0.1:${server.localPort}$path"
        private fun serve(socket:Socket) {
            try {
                socket.use {s ->
                    s.soTimeout=2000
                    val input=s.getInputStream().bufferedReader(Charsets.US_ASCII)
                    val request=input.readLine().orEmpty()
                    while(true) {
                        val line=input.readLine() ?: break
                        if(line.isEmpty()) break
                    }
                    val slow=request.startsWith("GET /slow ")
                    if(slow) {slowSeen.countDown();Thread.sleep(3000)}
                    val response=if(slow) "HTTP/1.1 200 OK\r\nContent-Length: 2\r\nConnection: close\r\n\r\n{}"
                        else "HTTP/1.1 503 Service Unavailable\r\nContent-Length: 0\r\nConnection: close\r\n\r\n"
                    s.getOutputStream().apply {write(response.toByteArray(Charsets.US_ASCII));flush()}
                }
            } catch(_:IOException) {
                // The deadline intentionally closes the client's socket.
            } catch(_:InterruptedException) {
                Thread.currentThread().interrupt()
            } finally {sockets.remove(socket)}
        }
        override fun close() {
            server.close()
            synchronized(sockets) {sockets.forEach {runCatching {it.close()}}}
            workers.shutdownNow()
            workers.awaitTermination(1,TimeUnit.SECONDS)
        }
    }
}
