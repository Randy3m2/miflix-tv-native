package com.miflix.native2.data

import java.net.InetAddress
import java.net.ServerSocket
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class LiveRecoveryHttpRP137Test {
    @Before fun before() {Http.authorize=null}
    @After fun after() {Http.authorize=null}
    @Test fun refreshedResolutionUsesSameEventRouteAndNewAuthorization()=runBlocking {
        val server=ServerSocket(0,4,InetAddress.getByName("127.0.0.1"))
        val executor=Executors.newSingleThreadExecutor()
        try {
            val received=executor.submit<List<List<String>>> {
                (1..2).map { revision ->server.accept().use {client ->
                    client.soTimeout=5000
                    val input=client.getInputStream().bufferedReader()
                    val lines=mutableListOf<String>()
                    while(true) {val line=input.readLine() ?: break;if(line.isEmpty()) break;lines+=line}
                    val body="""{"streams":[{"url":"https://cdn.example/live?token=$revision","behaviorHints":{"proxyHeaders":{"request":{"Authorization":"Bearer $revision"}}}}]}""".toByteArray()
                    client.getOutputStream().write("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: ${body.size}\r\nConnection: close\r\n\r\n".toByteArray())
                    client.getOutputStream().write(body);client.getOutputStream().flush();lines
                }}
            }
            val manifest="http://127.0.0.1:${server.localPort}/configured/manifest.json"
            val event=LiveChannel("game:123","tv","Test game",null,"")
            val repo=LiveRepository()
            val original=repo.streams(manifest,event).single()
            val fresh=repo.streams(manifest,event,forceRefreshRP137=true).single()
            assertNotEquals(original.url,fresh.url)
            assertEquals("Bearer 2",fresh.requestHeaders["Authorization"])
            val headers=received.get(5,TimeUnit.SECONDS)
            assertEquals(headers[0].first(),headers[1].first())
            assertTrue(headers[1].first().contains("/configured/stream/tv/game%3A123.json"))
            assertTrue(headers[1].any {it.equals("Cache-Control: no-cache",true)})
            assertTrue(headers[1].any {it.equals("Pragma: no-cache",true)})
        } finally {server.close();executor.shutdownNow()}
    }
}
