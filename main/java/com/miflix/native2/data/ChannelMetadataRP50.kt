package com.miflix.native2.data

import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URI
import java.net.URLDecoder
import java.util.concurrent.TimeUnit

internal data class ChannelIdentityRP50(val name:String,val number:Int)

/** Reads one bounded manifest, never video segments. Used on the QR server's worker. */
internal object ChannelMetadataRP50 {
    private const val MAX_BYTES=65536
    private val client=OkHttpClient.Builder().callTimeout(2,TimeUnit.SECONDS)
        .connectTimeout(1500,TimeUnit.MILLISECONDS).readTimeout(1500,TimeUnit.MILLISECONDS).build()
    fun number(url:String):Int=runCatching {
        val file=URI(url).path.orEmpty().substringAfterLast('/')
        Regex("^([0-9]{1,6})\\.m3u8$",RegexOption.IGNORE_CASE).matchEntire(file)
            ?.groupValues?.get(1)?.toIntOrNull()?.takeIf {it>0} ?: 0
    }.getOrDefault(0)
    private fun clean(value:String):String? {
        val text=value.replace(Regex("[_\\s]+")," ").trim().take(80)
        return text.takeIf {it.isNotBlank() && it.none {c ->c.isISOControl()} && !it.all {c ->c.isDigit()}}
    }
    fun parse(url:String,manifest:String):ChannelIdentityRP50 {
        val n=number(url)
        val fallback=if(n>0) "Canal $n" else "Canal personal"
        if(!manifest.trimStart('\uFEFF',' ','\r','\n','\t').startsWith("#EXTM3U")) return ChannelIdentityRP50(fallback,n)
        val lines=manifest.take(MAX_BYTES).lineSequence().toList()
        for(line in lines) {
            if(line.startsWith("#EXT-X-SESSION-DATA:") && line.contains("DATA-ID=\"com.apple.hls.title\"")) {
                Regex("VALUE=\"([^\"]+)\"").find(line)?.groupValues?.get(1)?.let {clean(it)}?.let {return ChannelIdentityRP50(it,n)}
            }
        }
        for(line in lines) {
            val path=line.trim().takeIf {it.isNotBlank() && !it.startsWith('#')} ?: continue
            val parts=runCatching {URI(url).resolve(path).rawPath.orEmpty().split('/').filter {it.isNotBlank()}
                .map {URLDecoder.decode(it.replace("+","%2B"),"UTF-8")}}.getOrNull() ?: continue
            // This provider names the channel immediately before the numbered TS segment.
            val at=parts.indexOf("get_ts")
            if(at>=0 && parts.size>=at+5 && parts.last().endsWith(".ts",true)) {
                clean(parts[parts.lastIndex-1])?.let {return ChannelIdentityRP50(it,n)}
            }
        }
        return ChannelIdentityRP50(fallback,n)
    }
    fun resolve(url:String,requestedName:String):ChannelIdentityRP50 {
        val explicit=requestedName.trim()
        if(explicit.isNotEmpty()) return ChannelIdentityRP50(explicit,number(url))
        val fallback=parse(url,"")
        val uri=runCatching {URI(url)}.getOrNull() ?: return fallback
        if(uri.scheme !in listOf("http","https") || uri.host==null || !uri.path.orEmpty().endsWith(".m3u8",true)) return fallback
        return try {
            client.newCall(Request.Builder().url(url).header("User-Agent","Mozilla/5.0").build()).execute().use {response ->
                if(!response.isSuccessful) fallback else response.body?.byteStream()?.use {stream ->
                    val bytes=ByteArray(MAX_BYTES);var count=0
                    while(count<bytes.size) {val read=stream.read(bytes,count,bytes.size-count);if(read<0) break;count+=read}
                    parse(url,String(bytes,0,count,Charsets.UTF_8))
                } ?: fallback
            }
        } catch(_:Exception) {fallback}
    }
}
