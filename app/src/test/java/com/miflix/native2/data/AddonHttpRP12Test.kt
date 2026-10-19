package com.miflix.native2.data
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.util.concurrent.Executors
class AddonHttpRP12Test {
 @Test fun requestDeadlineReturnsBeforeSlowServerAndHttpErrorsAreSafe()=runBlocking {
  val server=HttpServer.create(InetSocketAddress("127.0.0.1",0),0)
  val workers=Executors.newCachedThreadPool()
  server.executor=workers
  server.createContext("/slow") {exchange ->
   try {Thread.sleep(2500);exchange.sendResponseHeaders(200,2);exchange.responseBody.use {it.write("{}".toByteArray())}}
   catch(_:Exception) {} finally {exchange.close()}
  }
  server.createContext("/error") {exchange ->exchange.sendResponseHeaders(503,-1);exchange.close()}
  server.start()
  try {
   val base="http://127.0.0.1:${server.address.port}"
   val started=System.nanoTime()
   val failure=runCatching {AddonHttpRP12.text("$base/slow",100)}.exceptionOrNull()
   assertNotNull(failure)
   assertTrue((System.nanoTime()-started)/1000000<1500)
   val http=runCatching {AddonHttpRP12.text("$base/error",1000)}.exceptionOrNull()
   assertTrue(http is AddonHttpFailureRP12)
   assertEquals(503,(http as AddonHttpFailureRP12).status)
   assertFalse(http.message.orEmpty().contains("127.0.0.1"))
  } finally {server.stop(0);workers.shutdownNow()}
 }
}
