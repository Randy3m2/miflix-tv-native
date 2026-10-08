package com.miflix.native2.data

import com.miflix.native2.model.StreamChoice
import org.json.JSONObject
import java.net.URLEncoder

data class LiveCategory(val type: String, val id: String, val name: String, val genre: String? = null, val paginated: Boolean = false)
data class LiveChannel(val id: String, val type: String, val name: String, val poster: String?, val description: String)

class LiveRepository {
    private fun base(manifest: String) = manifest.trim().removeSuffix("/manifest.json")
    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8").replace("+", "%20")

    suspend fun categories(manifest: String): List<LiveCategory> {
        val json = JSONObject(Http.text(manifest, timeoutMs = 20000))
        val catalogs = json.optJSONArray("catalogs") ?: return emptyList()
        return buildList {
            for (i in 0 until catalogs.length()) {
                val c = catalogs.getJSONObject(i)
                val extras = c.optJSONArray("extra")
                var paginated = false
                var genres: org.json.JSONArray? = null
                if (extras != null) for (k in 0 until extras.length()) {
                    val e = extras.getJSONObject(k)
                    if (e.optString("name") == "skip") paginated = true
                    if (e.optString("name") == "genre") genres = e.optJSONArray("options")
                }
                val type = c.optString("type", "tv")
                val id = c.getString("id")
                add(LiveCategory(type, id, c.optString("name", id), paginated = paginated))
                genres?.let { g -> for (k in 0 until g.length()) {
                    val genre = g.getString(k)
                    add(LiveCategory(type, id, genre, genre, paginated))
                } }
            }
        }.distinctBy { it.name.lowercase() }
    }

    suspend fun channels(manifest: String, category: LiveCategory, skip: Int = 0): List<LiveChannel> {
        val extra = buildList {
            category.genre?.let { add("genre=${enc(it)}") }
            if (skip > 0 && category.paginated) add("skip=$skip")
        }.joinToString("&")
        val suffix = if (extra.isEmpty()) ".json" else "/$extra.json"
        val j = JSONObject(Http.text("${base(manifest)}/catalog/${enc(category.type)}/${enc(category.id)}$suffix", timeoutMs = 20000))
        val rows = j.optJSONArray("metas") ?: return emptyList()
        return (0 until rows.length()).map { i ->
            val m = rows.getJSONObject(i)
            LiveChannel(m.getString("id"), m.optString("type", category.type), m.optString("name", "Channel"),
                m.optString("poster").takeIf { it.isNotBlank() }, m.optString("description"))
        }
    }

    suspend fun streams(manifest: String, channel: LiveChannel): List<StreamChoice> {
        val j = JSONObject(Http.text("${base(manifest)}/stream/${enc(channel.type)}/${enc(channel.id)}.json", timeoutMs = 25000))
        val rows = j.optJSONArray("streams") ?: return emptyList()
        return (0 until rows.length()).mapNotNull { i ->
            val s = rows.getJSONObject(i)
            val url = s.optString("url").takeIf { it.startsWith("https://") || it.startsWith("http://") } ?: return@mapNotNull null
            val headers = s.optJSONObject("behaviorHints")?.optJSONObject("proxyHeaders")?.optJSONObject("request")
            val requestHeaders = mutableMapOf<String, String>()
            headers?.keys()?.forEach { key -> requestHeaders[key] = headers.getString(key) }
            StreamChoice(s.optString("name", channel.name), url, s.optString("title", channel.name), requestHeaders = requestHeaders)
        }.distinctBy { it.url }
    }
}
