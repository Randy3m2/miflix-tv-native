package com.miflix.native2.data

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.net.*

internal class MobileRemoteRP167(address:String) {
 val endpoint=RemotePolicyRP167.endpoint(address)
 private var token=""
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
 suspend fun connect(code:String)=mutex.withLock {require(code.matches(Regex("[0-9]{6}"))) {"Enter the six-digit code on the TV"};token=post("/connect",mapOf("code" to code),false).getString("token")}
 suspend fun send(action:String,value:String="")=mutex.withLock {
  require(RemotePolicyRP167.valid(action,value));check(token.isNotBlank())
  post("/remote",mapOf("action" to action,"value" to value,"request" to java.util.UUID.randomUUID().toString()))
 }
 fun disconnect(){token=""}
}
