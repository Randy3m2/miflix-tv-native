package com.miflix.native2.data

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Shared connections; cancelling the panel cancels its socket, not just a UI job. */
internal object StatsHttpRP105 {
    private val client=OkHttpClient.Builder().connectTimeout(3,TimeUnit.SECONDS)
        .readTimeout(6,TimeUnit.SECONDS).callTimeout(7,TimeUnit.SECONDS)
        .retryOnConnectionFailure(false).build()
    suspend fun text(url:String):String=suspendCancellableCoroutine {continuation ->
        val call=client.newCall(Request.Builder().url(url).header("Accept","application/json").build())
        continuation.invokeOnCancellation {call.cancel()}
        call.enqueue(object:Callback {
            override fun onFailure(call:Call,e:IOException) {if(continuation.isActive) continuation.resumeWithException(e)}
            override fun onResponse(call:Call,response:Response) {
                try {
                    val value=response.use {
                        check(it.isSuccessful) {"Stats HTTP ${it.code}"}
                        val body=it.body ?: error("Empty stats response")
                        val out=java.io.ByteArrayOutputStream()
                        body.byteStream().use {input ->
                            val buffer=ByteArray(8192)
                            while(true) {
                                val count=input.read(buffer);if(count<0) break
                                check(out.size()+count<=2*1024*1024) {"Stats response too large"}
                                out.write(buffer,0,count)
                            }
                        }
                        out.toString("UTF-8")
                    }
                    if(continuation.isActive) continuation.resume(value)
                } catch(e:Exception) {if(continuation.isActive) continuation.resumeWithException(e)}
            }
        })
    }
}
