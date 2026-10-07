package com.miflix.native2.data

import com.miflix.native2.model.StreamChoice
import com.miflix.native2.model.SubtitleChoice
import org.json.JSONObject

class StreamRepository(var manifestUrl: String) {
    suspend fun resolve(imdbId: String, type: String, season: Int = 0, episode: Int = 0): List<StreamChoice> {
        val manifest = manifestUrl.trim()
        if (manifest.isBlank()) throw IllegalStateException("Torrentio is not configured")
        val base = manifest.removeSuffix("/manifest.json")
        val stremioType = if (type == "series") "series" else "movie"
        val mediaId = if (stremioType == "series") "$imdbId:$season:$episode" else imdbId
        val json = JSONObject(Http.text("$base/stream/$stremioType/$mediaId.json", timeoutMs = 16000))
        val a = json.optJSONArray("streams") ?: return emptyList()
        val rows = (0 until a.length()).mapNotNull { i ->
            val s = a.optJSONObject(i) ?: return@mapNotNull null
            val url = s.optString("url").takeIf { it.startsWith("http") } ?: return@mapNotNull null
            val subs = s.optJSONArray("subtitles")?.let { sa ->
                (0 until sa.length()).mapNotNull { si ->
                    val sub = sa.optJSONObject(si) ?: return@mapNotNull null
                    val su = sub.optString("url").takeIf { it.startsWith("http") } ?: return@mapNotNull null
                    SubtitleChoice(su, sub.optString("lang"), sub.optString("label"))
                }
            } ?: emptyList()
            StreamChoice(
                name = s.optString("name", "Torrentio"),
                title = s.optString("title", s.optString("name", "Stream")),
                url = url,
                subtitles = subs
            )
        }
        return rows.sortedByDescending { score(it) }
    }

    private fun score(s: StreamChoice): Int {
        val t = (s.name + " " + s.title).lowercase()
        var score = 0
        if ("1080" in t) score += 60
        if ("720" in t) score += 30
        if ("h264" in t || "avc" in t) score += 35
        if ("web-dl" in t || "webrip" in t) score += 15
        if ("4k" in t || "2160" in t) score += 10
        if ("hevc" in t || "h265" in t || "x265" in t) score -= 15
        if ("dolby vision" in t || " dv " in " $t ") score -= 35
        if ("av1" in t) score -= 45
        return score
    }
}
