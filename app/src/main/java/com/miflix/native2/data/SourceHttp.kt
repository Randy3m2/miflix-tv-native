package com.miflix.native2.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicReference

/** Bounded, cancellable source requests. Never expose URLs, tokens or response bodies in errors. */
internal object SourceHttp {
    private val workers=Executors.newFixedThreadPool(3) { task -> Thread(task,"BruniO-source").apply { isDaemon=true } }
    suspend fun text(url: String, service: String, headers: Map<String,String> = emptyMap(),
                     body: String? = null, contentType: String = "application/json", limit: Int = 6_000_000): String = suspendCancellableCoroutine { continuation ->
        val connection=AtomicReference<HttpURLConnection?>()
        val pending=AtomicReference<Future<*>?>()
        continuation.invokeOnCancellation { connection.get()?.disconnect();pending.get()?.cancel(true) }
        val task=workers.submit {
            if(!continuation.isActive) return@submit
            var c: HttpURLConnection?=null
            try {
                val current=URL(url).openConnection() as HttpURLConnection
                c=current;connection.set(current)
                if(!continuation.isActive) throw CancellationException()
                current.connectTimeout=10000;current.readTimeout=12000;current.instanceFollowRedirects=false
                current.setRequestProperty("User-Agent","BruniO/RC34")
                headers.forEach { (key,value) -> current.setRequestProperty(key,value) }
                if(body!=null) {
                    current.requestMethod="POST";current.doOutput=true;current.setRequestProperty("Content-Type",contentType)
                    current.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                }
                val status=current.responseCode
                check(status in 200..299) { "$service · HTTP $status" }
                val bytes=java.io.ByteArrayOutputStream()
                current.inputStream.use { input ->
                    val buffer=ByteArray(8192)
                    while(true) {
                        if(!continuation.isActive) throw CancellationException()
                        val n=input.read(buffer);if(n<0) break
                        check(bytes.size()+n<=limit) { "$service · response too large" };bytes.write(buffer,0,n)
                    }
                }
                if(continuation.isActive) continuation.resumeWith(Result.success(bytes.toString("UTF-8")))
            } catch(e: Exception) {
                // HttpURLConnection can include the authenticated URL in exception messages.
                val safe=if(e is IllegalStateException && e.message?.startsWith("$service ·")==true) e
                    else IllegalStateException("$service · connection unavailable")
                if(continuation.isActive) continuation.resumeWith(Result.failure(safe))
            } finally { c?.disconnect();connection.set(null) }
        }
        pending.set(task)
        if(!continuation.isActive) task.cancel(true)
    }
}
