package com.miflix.native2.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

internal object Http {
    var authorize: (suspend (String,Map<String,String>,Boolean) -> Map<String,String>)? = null
    suspend fun text(
        url: String,
        method: String = "GET",
        headers: Map<String, String> = emptyMap(),
        body: String? = null,
        timeoutMs: Int = 12000
    ): String {
        val hook=authorize
        val fresh=hook?.invoke(url,headers,false) ?: headers
        return try { request(url,method,fresh,body,timeoutMs) } catch(e: HttpFailure) {
            if(e.status!=401 || hook==null) throw e
            val renewed=hook(url,fresh,true)
            if(renewed==fresh) throw e
            request(url,method,renewed,body,timeoutMs)
        }
    }
    private suspend fun request(url: String,method: String,headers: Map<String,String>,body: String?,timeoutMs: Int): String = withContext(Dispatchers.IO) {
        val c = URL(url).openConnection() as HttpURLConnection
        try {
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
            throw HttpFailure(code,"HTTP $code: $message")
        }
        result
        } finally { c.disconnect() }
    }
}

internal class HttpFailure(val status: Int,message: String): IllegalStateException(message)
