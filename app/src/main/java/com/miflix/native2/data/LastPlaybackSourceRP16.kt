package com.miflix.native2.data

import com.miflix.native2.model.StreamChoice
import com.miflix.native2.model.SubtitleChoice
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.security.MessageDigest

/** Device-local successful sources. One bounded document, opaque account/profile/context keys. */
internal class LastPlaybackSourceRP16(private val read:()->String,private val write:(String)->Unit) {
    companion object {
        fun key(scope:String,cloud:String,season:Int,episode:Int,context:String):String =
            MessageDigest.getInstance("SHA-256").digest(JSONArray(listOf(scope,cloud,season,episode,context)).toString().toByteArray(Charsets.UTF_8))
                .joinToString("") {"%02x".format(it)}
    }
    private fun document()=runCatching {JSONObject(read())}.getOrDefault(JSONObject())
    fun get(key:String):StreamChoice? = runCatching {
        val row=document().optJSONObject(key)?.optJSONObject("stream") ?: return null
        val url=row.getString("url")
        if(URI(url).scheme !in setOf("http","https")) return null
        val headers=row.optJSONObject("headers")
        val subs=row.optJSONArray("subs")
        val audio=row.optJSONArray("audio")
        StreamChoice(name=row.optString("name"),url=url,title=row.optString("title"),filename=row.optString("filename"),
            videoHash=row.optString("hash"),videoSize=row.optLong("size"),addonName=row.optString("addon"),
            sourceUrl=row.optString("source"),sourceAddonHost=row.optString("host"),
            requestHeaders=headers?.keys()?.asSequence()?.associateWith {headers.getString(it)} ?: emptyMap(),
            audioLanguages=(0 until (audio?.length() ?: 0)).map {audio!!.getString(it)},
            subtitles=(0 until (subs?.length() ?: 0)).map {i ->val s=subs!!.getJSONObject(i);SubtitleChoice(s.getString("url"),s.optString("lang"),s.optString("label"),s.optString("source"))})
    }.getOrNull()
    fun save(key:String,s:StreamChoice) {
        if(s.nativeSource!=null || runCatching {URI(s.url).scheme !in setOf("http","https")}.getOrDefault(true)) return
        val row=JSONObject().put("url",s.url).put("name",s.name).put("title",s.title).put("filename",s.filename)
            .put("hash",s.videoHash).put("size",s.videoSize).put("addon",s.addonName).put("source",s.sourceUrl).put("host",s.sourceAddonHost)
            .put("headers",JSONObject(s.requestHeaders)).put("audio",JSONArray(s.audioLanguages))
            .put("subs",JSONArray(s.subtitles.map {JSONObject().put("url",it.url).put("lang",it.lang).put("label",it.label).put("source",it.source)}))
        if(row.toString().toByteArray(Charsets.UTF_8).size>32768) return
        val doc=document().put(key,JSONObject().put("at",System.currentTimeMillis()).put("stream",row))
        while(doc.length()>60 || doc.toString().toByteArray(Charsets.UTF_8).size>1_000_000) {
            val oldest=doc.keys().asSequence().minByOrNull {doc.optJSONObject(it)?.optLong("at") ?: 0} ?: break
            doc.remove(oldest)
        }
        write(doc.toString())
    }
    fun remove(key:String) {val doc=document();if(doc.has(key)) {doc.remove(key);write(doc.toString())}}
}
