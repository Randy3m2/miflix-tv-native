package com.miflix.native2.data

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class StatsHttpRP105Test {
    private class Server(val handler:(Socket)->Unit):AutoCloseable {
        val socket=ServerSocket(0)
        val url="http://127.0.0.1:${socket.localPort}/stats"
        val thread=Thread {try {socket.accept().use {peer ->
            peer.soTimeout=4000
            val reader=peer.getInputStream().bufferedReader()
            while(reader.readLine()?.isNotEmpty()==true) { }
            handler(peer)
        }} catch(_:Exception) { }}.apply {isDaemon=true;start()}
        override fun close() {socket.close();thread.join(1000)}
    }
    @Test fun readsJsonResponse()=runBlocking {
        Server {peer ->peer.getOutputStream().apply {write("HTTP/1.1 200 OK\r\nContent-Length: 2\r\nConnection: close\r\n\r\n{}".toByteArray());flush()}}.use {server ->
            assertEquals("{}",StatsHttpRP105.text(server.url))
        }
    }
    @Test fun closingPanelCancelsSocketWithoutWaitingForReadTimeout()=runBlocking {
        val entered=CountDownLatch(1);val disconnected=CountDownLatch(1)
        Server {peer ->
            peer.getOutputStream().apply {write("HTTP/1.1 200 OK\r\nContent-Length: 1000\r\n\r\n".toByteArray());flush()}
            entered.countDown()
            if(peer.getInputStream().read()==-1) disconnected.countDown()
        }.use {server ->
            val job=launch {StatsHttpRP105.text(server.url)}
            assertTrue(withContext(Dispatchers.IO) {entered.await(3,TimeUnit.SECONDS)})
            withTimeout(1500) {job.cancelAndJoin()}
            assertTrue(withContext(Dispatchers.IO) {disconnected.await(2,TimeUnit.SECONDS)})
        }
    }
    @Test fun httpErrorsAreReportedWithoutPretendingThereAreStats()=runBlocking {
        Server {peer ->peer.getOutputStream().apply {write("HTTP/1.1 503 Unavailable\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".toByteArray());flush()}}.use {server ->
            try {StatsHttpRP105.text(server.url);fail("Expected HTTP error")} catch(e:IllegalStateException) {assertTrue(e.message.orEmpty().contains("503"))}
        }
    }
}
