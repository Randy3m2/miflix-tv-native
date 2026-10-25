package com.miflix.native2.data

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.*
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Shared cancellable public-data client; never copies session/add-on headers. */
internal object SportsPublicHttpRP121 {
    private val client=OkHttpClient.Builder().connectTimeout(4,TimeUnit.SECONDS).readTimeout(8,TimeUnit.SECONDS)
        .callTimeout(12,TimeUnit.SECONDS).retryOnConnectionFailure(false).build()
    suspend fun text(url:String):String=suspendCancellableCoroutine {continuation ->
        val call=client.newCall(Request.Builder().url(url).get().build())
        continuation.invokeOnCancellation {call.cancel()}
        call.enqueue(object:Callback {
            override fun onFailure(call:Call,e:IOException) {if(continuation.isActive) continuation.resumeWithException(e)}
            override fun onResponse(call:Call,response:Response) {
                try {
                    val text=response.use {r ->
                        check(r.isSuccessful) {"Sports source HTTP ${r.code}"}
                        val body=r.body ?: error("Empty response")
                        check(body.contentLength()<=4*1024*1024) {"Response too large"}
                        val output=ByteArrayOutputStream();val buffer=ByteArray(8192)
                        body.byteStream().use {input ->while(true) {
                            val count=input.read(buffer);if(count<0) break
                            check(output.size()+count<=4*1024*1024) {"Response too large"}
                            output.write(buffer,0,count)
                        }}
                        output.toString("UTF-8")
                    }
                    if(continuation.isActive) continuation.resume(text)
                } catch(e:Exception) {if(continuation.isActive) continuation.resumeWithException(e)}
            }
        })
    }
}
