package com.miflix.native2.data

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

internal class AddonHttpFailureRP12(val status:Int):IOException("HTTP $status")

/** Independent cancellable public add-on requests. No auth/session refresh or Supabase involved. */
internal object AddonHttpRP12 {
    private val client=OkHttpClient.Builder().connectTimeout(4,TimeUnit.SECONDS)
        .readTimeout(8,TimeUnit.SECONDS).callTimeout(8,TimeUnit.SECONDS).build()
    suspend fun text(url:String,timeoutMs:Int):String = suspendCancellableCoroutine {continuation ->
        val call=client.newBuilder().callTimeout(timeoutMs.toLong(),TimeUnit.MILLISECONDS).build()
            .newCall(Request.Builder().url(url.replace("|","%7C")).header("Accept","application/json")
                .header("User-Agent","BruniO/RP12").build())
        continuation.invokeOnCancellation {call.cancel()}
        call.enqueue(object:Callback {
            override fun onFailure(call:Call,e:IOException) {
                if(continuation.isActive) continuation.resumeWith(Result.failure(IOException("Add-on request failed")))
            }
            override fun onResponse(call:Call,response:Response) {
                try {
                    val result=response.use {r ->
                        if(!r.isSuccessful) throw AddonHttpFailureRP12(r.code)
                        val body=r.body ?: throw IOException("Empty add-on response")
                        val out=ByteArrayOutputStream()
                        body.byteStream().use {input ->
                            val buffer=ByteArray(8192)
                            while(true) {
                                if(call.isCanceled()) throw IOException("Request cancelled")
                                val n=input.read(buffer);if(n<0) break
                                if(out.size()+n>6000000) throw IOException("Add-on response too large")
                                out.write(buffer,0,n)
                            }
                        }
                        out.toString("UTF-8")
                    }
                    if(continuation.isActive) continuation.resumeWith(Result.success(result))
                } catch(e:Exception) {
                    val safe=if(e is AddonHttpFailureRP12) e else IOException("Add-on response unavailable")
                    if(continuation.isActive) continuation.resumeWith(Result.failure(safe))
                }
            }
        })
    }
}
