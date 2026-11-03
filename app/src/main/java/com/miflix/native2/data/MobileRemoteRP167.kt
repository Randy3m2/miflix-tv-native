package com.miflix.native2.data

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.net.*
import java.util.concurrent.ConcurrentLinkedQueue

internal class MobileRemoteRP167(address:String,initialToken:String="") {
 val endpoint=RemotePolicyRP167.endpoint(address)
 private var token=initialToken
 private val mutex=Mutex()
 private suspend fun post(path:String,values:Map<String,String>,authenticated:Boolean=true):JSONObject=withContext(Dispatchers.IO) {
  val connection=URL(endpoint+path).openConnection() as HttpURLConnection
  try {
   connection.requestMethod="POST";connection.connectTimeout=3500;connection.readTimeout=3500;connection.instanceFollowRedirects=false
   connection.setRequestProperty("Content-Type","application/x-www-form-urlencoded")
   if(authenticated) connection.setRequestProperty("Authorization","Bearer $token")
   connection.doOutput=true
   val body=values.entries.joinToString("&") {URLEncoder.encode(it.key,"UTF-8")+"="+URLEncoder.encode(it.value,"UTF-8")}
   connection.outputStream.use {it.write(body.toByteArray(Charsets.UTF_8))}
   val code=connection.responseCode
   if(code==401) error("Remote session expired. Open Connect phone / PC on the TV and pair again.")
   if(code !in 200..299) error(if(path=="/connect") "Pairing failed. Check the TV code and generate a new code if it expired." else "The TV could not accept this command ($code).")
   val response=connection.inputStream.use {input->val out=java.io.ByteArrayOutputStream();val buffer=ByteArray(2048);while(true) {val n=input.read(buffer);if(n<0) break;out.write(buffer,0,n);check(out.size()<=65536)};out.toByteArray()}
   JSONObject(String(response,Charsets.UTF_8))
  } finally {connection.disconnect()}
 }
 suspend fun connect(code:String):String=mutex.withLock {require(code.matches(Regex("[0-9]{6}"))) {"Enter the six-digit code on the TV"};token=post("/connect",mapOf("code" to code),false).getString("token");token}
 suspend fun send(action:String,value:String="")=mutex.withLock {
  require(RemotePolicyRP167.valid(action,value));check(token.isNotBlank())
  post("/remote",mapOf("action" to action,"value" to value,"request" to java.util.UUID.randomUUID().toString()))
 }
 fun disconnect(){token=""}
}

internal data class RemoteDiscoveryResultRP170(val address:String,val label:String)

internal object RemoteDiscoveryScannerRP170 {
 suspend fun scan():List<RemoteDiscoveryResultRP170> = withContext(Dispatchers.IO) {
  val local=NetworkInterface.getNetworkInterfaces().asSequence().flatMap {it.interfaceAddresses.asSequence()}
   .firstOrNull {it.address is Inet4Address && !it.address.isLoopbackAddress && it.address.isSiteLocalAddress}
   ?: return@withContext emptyList()
  val ip=(local.address as Inet4Address).hostAddress
  val parts=ip.split('.')
  if(parts.size!=4)return@withContext emptyList()
  val prefix=parts.take(3).joinToString(".")
  val found=ConcurrentLinkedQueue<RemoteDiscoveryResultRP170>()
  coroutineScope {
   (1..254).map {last->launch(Dispatchers.IO) {
    val host="$prefix.$last"; if(host==ip)return@launch
    runCatching {
     val c=(URL("http://$host:8765/").openConnection() as HttpURLConnection)
     c.connectTimeout=180;c.readTimeout=220;c.instanceFollowRedirects=false;c.requestMethod="GET"
     if(c.responseCode==200) {val body=c.inputStream.bufferedReader().use {it.readText().take(256)};if(body.contains("BruniO",true))found.add(RemoteDiscoveryResultRP170(host,"BruniO TV · $host"))}
     c.disconnect()
    }
   }}.joinAll()
  }
  found.toList().sortedBy {it.address}
 }
}
