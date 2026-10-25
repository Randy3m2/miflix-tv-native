package com.miflix.native2.data
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class SportsHttpRP121Test {
    private fun headers(socket:Socket):List<String> {
        val input=socket.getInputStream().bufferedReader();val rows=mutableListOf<String>()
        while(true) {val line=input.readLine() ?: break;if(line.isEmpty()) break;rows+=line}
        return rows
    }
    @Test fun smallResponseAndNoSessionHeaders()=runBlocking {
        val server=ServerSocket(0);val executor=Executors.newSingleThreadExecutor()
        try {
            val incoming=executor.submit<List<String>> {server.accept().use {s ->val h=headers(s);s.getOutputStream().write("HTTP/1.1 200 OK\r\nContent-Length: 11\r\nConnection: close\r\n\r\n{\"ok\":true}".toByteArray());h}}
            assertEquals("{\"ok\":true}",SportsPublicHttpRP121.text("http://127.0.0.1:${server.localPort}/"))
            assertTrue(incoming.get(3,TimeUnit.SECONDS).none {it.startsWith("Authorization:",true)})
        } finally {server.close();executor.shutdownNow()}
    }
    @Test fun oversizedBodiesRejectedBeforeReading()=runBlocking {
        val server=ServerSocket(0);val executor=Executors.newSingleThreadExecutor()
        try {
            executor.submit {server.accept().use {s ->headers(s);s.getOutputStream().write("HTTP/1.1 200 OK\r\nContent-Length: 5000000\r\nConnection: close\r\n\r\n".toByteArray())}}
            assertTrue(runCatching {SportsPublicHttpRP121.text("http://127.0.0.1:${server.localPort}/")}.exceptionOrNull()?.message.orEmpty().contains("Response too large"))
        } finally {server.close();executor.shutdownNow()}
    }
    @Test fun cancellingTabClosesTheSocket()=runBlocking {
        val server=ServerSocket(0);val executor=Executors.newSingleThreadExecutor();val started=CompletableDeferred<Unit>()
        try {
            val closed=executor.submit<Boolean> {server.accept().use {s ->s.soTimeout=8000;headers(s);started.complete(Unit);try {s.getInputStream().read()==-1} catch(_:java.net.SocketTimeoutException) {false} catch(_:java.io.IOException) {true}}}
            val loading=launch(Dispatchers.Default) {SportsPublicHttpRP121.text("http://127.0.0.1:${server.localPort}/")}
            withTimeout(10000) {started.await();loading.cancelAndJoin()}
            assertTrue(closed.get(10,TimeUnit.SECONDS))
        } finally {server.close();executor.shutdownNow()}
    }
}
