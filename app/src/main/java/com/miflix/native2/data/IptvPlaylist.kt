package com.miflix.native2.data

import java.security.MessageDigest
import java.text.Normalizer
import java.util.Locale

internal data class IptvEntry(val id: String,val name: String,val logo: String?,val groups: Set<String>,val url: String,val headers: Map<String,String>,val tvgId: String="")

internal object IptvPlaylist {
    fun searchKey(value: String)=Normalizer.normalize(value,Normalizer.Form.NFD).replace(Regex("\\p{M}+"),"").lowercase(Locale.ROOT)
    private fun digest(value: String)=MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }.take(24)
    private fun http(value: String)=value.startsWith("https://",true) || value.startsWith("http://",true)
    fun parse(text: String): List<IptvEntry> {
        val result=mutableListOf<IptvEntry>()
        var attrs=emptyMap<String,String>();var name="";var headers=mutableMapOf<String,String>();var drm=false
        for(raw in text.lineSequence()) {
            val line=raw.trim().removePrefix("\uFEFF")
            when {
                line.startsWith("#EXTINF:",true) -> {
                    attrs=Regex("([\\w-]+)=\"([^\"]*)\"").findAll(line).associate { it.groupValues[1].lowercase(Locale.ROOT) to it.groupValues[2] }
                    var quoted=false;val comma=line.indexOfFirst { ch -> if(ch=='\"') quoted=!quoted;ch==',' && !quoted }
                    name=if(comma>=0) line.substring(comma+1).trim() else attrs["tvg-name"].orEmpty()
                    headers=mutableMapOf();drm=false
                }
                line.startsWith("#EXTVLCOPT:http-user-agent=",true) -> headers["User-Agent"]=line.substringAfter('=')
                line.startsWith("#EXTVLCOPT:http-referrer=",true) -> headers["Referer"]=line.substringAfter('=')
                line.startsWith("#KODIPROP:",true) && line.contains("license",true) -> drm=true
                line.isNotBlank() && !line.startsWith('#') -> {
                    if(name.isNotBlank() && http(line) && !drm) {
                        val identity=attrs["tvg-id"].orEmpty().ifBlank { searchKey(name) }
                        result.add(IptvEntry("iptv:"+digest(identity),name,attrs["tvg-logo"]?.takeIf(::http),
                            attrs["group-title"].orEmpty().split(';').map { it.trim() }.filter { it.isNotBlank() }.toSet().ifEmpty { setOf("Undefined") },line,headers.toMap(),attrs["tvg-id"].orEmpty()))
                    }
                    name="";attrs=emptyMap();headers=mutableMapOf();drm=false
                }
            }
        }
        return result.distinctBy { it.id to it.url }
    }
}
