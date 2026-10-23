package com.miflix.native2.data

import com.miflix.native2.model.StreamChoice
import org.json.JSONObject
import java.net.URLEncoder

data class LiveCategory(val type: String, val id: String, val name: String, val genre: String? = null, val paginated: Boolean = false, val searchable: Boolean = false)
data class LiveChannel(val id: String, val type: String, val name: String, val poster: String?, val description: String, val number: Int = 0,val tvgId: String = "")

class LiveRepository {
    private data class RankedSportsRP84(val stream:StreamChoice,val resolution:Int,val bitrate:Long,val index:Int)
    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8").replace("+", "%20")

    suspend fun categories(manifest: String): List<LiveCategory> {
        if(IptvRepository.isPlaylist(manifest)) return IptvRepository.categories(manifest)
        val json = JSONObject(Http.text(manifest, timeoutMs = 20000))
        val catalogs = json.optJSONArray("catalogs") ?: return emptyList()
        return buildList {
            for (i in 0 until catalogs.length()) {
                val c = catalogs.getJSONObject(i)
                val extras = c.optJSONArray("extra")
                var paginated = false
                var searchable = false
                var genres: org.json.JSONArray? = null
                if (extras != null) for (k in 0 until extras.length()) {
                    val e = extras.getJSONObject(k)
                    if (e.optString("name") == "search") searchable = true
                    if (e.optString("name") == "skip") paginated = true
                    if (e.optString("name") == "genre") genres = e.optJSONArray("options")
                }
                val type = c.optString("type", "tv")
                val id = c.getString("id")
                add(LiveCategory(type, id, c.optString("name", id), paginated = paginated, searchable = searchable))
                genres?.let { g -> for (k in 0 until g.length()) {
                    val genre = g.getString(k)
                    add(LiveCategory(type, id, genre, genre, paginated, searchable))
                } }
            }
        }.distinctBy { it.name.lowercase() }
    }

    suspend fun channels(manifest: String, category: LiveCategory, skip: Int = 0, search: String = ""): List<LiveChannel> {
        if(IptvRepository.isPlaylist(manifest)) return IptvRepository.channels(category,skip,search,manifest)
        val extra = buildList {
            if (search.isNotBlank() && category.searchable) add("search=${enc(search.trim())}")
            category.genre?.let { add("genre=${enc(it)}") }
            if (skip > 0 && category.paginated) add("skip=$skip")
        }.joinToString("&")
        val suffix = if (extra.isEmpty()) ".json" else "/$extra.json"
        val j = JSONObject(Http.text(LiveManifestUrlRP49.resource(manifest,"catalog/${enc(category.type)}/${enc(category.id)}$suffix"), timeoutMs = 20000))
        val rows = j.optJSONArray("metas") ?: return emptyList()
        return (0 until rows.length()).map { i ->
            val m = rows.getJSONObject(i)
            LiveChannel(m.getString("id"), m.optString("type", category.type), m.optString("name", "Channel"),
                m.optString("poster").takeIf { it.isNotBlank() }, m.optString("description"))
        }
    }

    suspend fun streams(manifest: String, channel: LiveChannel): List<StreamChoice> {
        if(IptvRepository.isPlaylist(manifest)) return IptvRepository.streams(channel,manifest)
        val j = JSONObject(Http.text(LiveManifestUrlRP49.resource(manifest,"stream/${enc(channel.type)}/${enc(channel.id)}.json"), timeoutMs = 25000))
        val rows = j.optJSONArray("streams") ?: return emptyList()
        val orderSports=SportsPolicyRP71.ordersSports(manifest)
        val streams=(0 until rows.length()).mapNotNull { i ->
            val s = rows.getJSONObject(i)
            val url = s.optString("url").takeIf { it.startsWith("https://") || it.startsWith("http://") } ?: return@mapNotNull null
            val headers = s.optJSONObject("behaviorHints")?.optJSONObject("proxyHeaders")?.optJSONObject("request")
            val requestHeaders = mutableMapOf<String, String>()
            headers?.keys()?.forEach { key -> requestHeaders[key] = headers.getString(key) }
            val stream=StreamChoice(s.optString("name", channel.name), url, s.optString("title", channel.name), requestHeaders = requestHeaders)
            val hints=s.optJSONObject("behaviorHints")
            val video=s.optJSONObject("video") ?: s.optJSONObject("resolution")
            val resolutionValue=s.opt("resolution")
            val qualityValue=s.opt("quality")
            val description=listOf(stream.name,stream.title,s.optString("description"),
                resolutionValue?.takeUnless {it is JSONObject}?.toString().orEmpty().let {if(it.all {c ->c.isDigit()} && it.isNotEmpty()) it+"p" else it},
                qualityValue?.toString().orEmpty().let {if(it.all {c ->c.isDigit()} && it.isNotEmpty()) it+"p" else it}).joinToString(" ")
            val width=s.optInt("width",video?.optInt("width",0) ?: 0)
            val height=s.optInt("height",video?.optInt("height",0) ?: 0)
            val bitrate=listOf(s.optString("bitrate"),s.optString("bandwidth"),s.optString("averageBitrate"),hints?.optString("bitrate").orEmpty(),video?.optString("bitrate").orEmpty())
                .maxOfOrNull {SportsQualityRP84.explicitBitrate(it)} ?: 0L
            RankedSportsRP84(stream,SportsQualityRP84.resolution(width,height,description),maxOf(bitrate,SportsQualityRP84.bitrate(description)),i)
        }
        return (if(orderSports) streams.sortedWith(Comparator {a,b ->
            SportsQualityRP84.compare(a.resolution,a.bitrate,b.resolution,b.bitrate).let {if(it!=0) it else a.index.compareTo(b.index)}
        }) else streams).map {it.stream}.distinctBy {it.url}
    }
}
