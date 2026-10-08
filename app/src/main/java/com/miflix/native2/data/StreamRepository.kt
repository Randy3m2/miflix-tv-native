package com.miflix.native2.data

import com.miflix.native2.model.AddonConfig
import com.miflix.native2.model.StreamChoice
import com.miflix.native2.model.SubtitleChoice
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.json.JSONObject

class StreamRepository(initialManifest: String) {
    private var manifests: List<String> = listOf(initialManifest).map { it.trim() }.filter { it.isNotBlank() }

    var manifestUrl: String
        get() = manifests.firstOrNull().orEmpty()
        set(value) { manifests = listOf(value.trim()).filter { it.isNotBlank() } }

    fun manifestUrls(): List<String> = manifests

    fun setManifests(values: List<String>) {
        manifests = values.map { it.trim() }.filter { it.isNotBlank() }.distinct()
    }

    fun addManifest(value: String) {
        val normalized = value.trim()
        if (normalized.isNotBlank()) manifests = (manifests + normalized).distinct()
    }

    suspend fun inspectManifest(url: String): AddonConfig {
        val normalized = normalizeManifest(url)
        val j = JSONObject(Http.text(normalized, timeoutMs = 12000))
        val name = j.optString("name").ifBlank { j.optString("id", "Stremio add-on") }
        val resources = j.optJSONArray("resources")
        var hasStream = false
        if (resources != null) {
            for (i in 0 until resources.length()) {
                val item = resources.opt(i)
                if (item is String && item == "stream") hasStream = true
                if (item is JSONObject && item.optString("name") == "stream") hasStream = true
            }
        }
        if (!hasStream) throw IllegalStateException("This add-on does not expose Stremio stream resources")
        return AddonConfig(name = name, manifestUrl = normalized)
    }

    suspend fun resolve(imdbId: String, type: String, season: Int = 0, episode: Int = 0): List<StreamChoice> = coroutineScope {
        val urls = manifests.filter { it.isNotBlank() }.distinct()
        if (urls.isEmpty()) throw IllegalStateException("No streaming add-on is configured")
        val rows = urls.map { manifest ->
            async { runCatching { resolveOne(manifest, imdbId, type, season, episode) }.getOrDefault(emptyList()) }
        }.awaitAll().flatten().distinctBy { it.url }
        if (rows.isEmpty()) throw IllegalStateException("No playable sources found from your configured add-ons")
        rows.sortedByDescending { score(it) }
    }

    private suspend fun resolveOne(manifestUrl: String, imdbId: String, type: String, season: Int, episode: Int): List<StreamChoice> {
        val manifest = normalizeManifest(manifestUrl)
        val base = manifest.removeSuffix("/manifest.json")
        val stremioType = if (type == "series") "series" else "movie"
        val mediaId = if (stremioType == "series") "$imdbId:$season:$episode" else imdbId
        val json = JSONObject(Http.text("$base/stream/$stremioType/$mediaId.json", timeoutMs = 18000))
        val a = json.optJSONArray("streams") ?: return emptyList()
        return (0 until a.length()).mapNotNull { i ->
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
                name = s.optString("name", "Stremio add-on"),
                title = s.optString("title", s.optString("name", "Stream")),
                url = url,
                subtitles = subs
            )
        }
    }

    private fun normalizeManifest(value: String): String {
        val v = value.trim()
        if (v.isBlank()) return v
        return if (v.endsWith("manifest.json", true)) v else v.trimEnd('/') + "/manifest.json"
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
