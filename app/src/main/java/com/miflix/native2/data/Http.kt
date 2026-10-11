package com.miflix.native2.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

internal object Http {
    suspend fun text(
        url: String,
        method: String = "GET",
        headers: Map<String, String> = emptyMap(),
        body: String? = null,
        timeoutMs: Int = 12000
    ): String = withContext(Dispatchers.IO) {
        val c = URL(url).openConnection() as HttpURLConnection
        kotlinx.coroutines.suspendCancellableCoroutine<String> { continuation ->
        continuation.invokeOnCancellation { c.disconnect() }
        try {
        if(!continuation.isActive) return@suspendCancellableCoroutine
        c.requestMethod = method
        c.connectTimeout = timeoutMs
        c.readTimeout = timeoutMs
        c.instanceFollowRedirects = true
        c.setRequestProperty("Accept", "application/json")
        headers.forEach { (k, v) -> c.setRequestProperty(k, v) }
        if (body != null) {
            c.doOutput = true
            c.setRequestProperty("Content-Type", "application/json")
            c.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        }
        val code = c.responseCode
        val stream = if (code in 200..299) c.inputStream else c.errorStream
        val result = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (code !in 200..299) {
            val message = runCatching {
                val j = JSONObject(result)
                j.optString("message", j.optString("msg", j.optString("error_description", result)))
            }.getOrDefault(result)
            throw IllegalStateException("HTTP $code: $message")
        }
        if(continuation.isActive) continuation.resumeWith(Result.success(result))
        } catch(e: Exception) {
            if(continuation.isActive) continuation.resumeWith(Result.failure(e))
        } finally { c.disconnect() }
        }
    }
}
