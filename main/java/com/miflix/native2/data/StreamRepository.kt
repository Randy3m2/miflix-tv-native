package com.miflix.native2.data

import com.miflix.native2.model.StreamChoice
import com.miflix.native2.model.SubtitleChoice
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.json.JSONObject
import java.net.URI

class StreamRepository(initialManifestUrl: String) {
    private val manifestUrls = linkedSetOf<String>()

    init {
        addManifest(initialManifestUrl)
    }

    var manifestUrl: String
        get() = manifestUrls.firstOrNull().orEmpty()
        set(value) {
            if (value.isNotBlank()) {
                manifestUrls.remove(value.trim())
                val copy = manifestUrls.toList()
                manifestUrls.clear()
                manifestUrls.add(value.trim())
                manifestUrls.addAll(copy)
            }
        }

    fun manifests(): List<String> = manifestUrls.toList()

    fun setManifests(values: Collection<String>) {
        manifestUrls.clear()
        values.map { it.trim() }.filter { it.isNotBlank() }.forEach { addManifest(it) }
    }

    fun addManifest(value: String): Boolean {
        val clean = value.trim()
        if (clean.isBlank()) return false
        val normalized = if (clean.endsWith("/manifest.json")) clean else clean.trimEnd('/') + "/manifest.json"
        return manifestUrls.add(normalized)
    }

    fun removeManifest(value: String) {
        manifestUrls.remove(value.trim())
    }

    suspend fun resolve(imdbId: String, type: String, season: Int = 0, episode: Int = 0, movieYear: String? = null, maxBytes: Long = 0, movieTitle: String? = null): List<StreamChoice> = coroutineScope {
        val manifests = manifestUrls.toList()
        if (manifests.isEmpty()) throw IllegalStateException("No stream add-on is configured")
        val rows = manifests.map { manifest ->
            async { runCatching { resolveOne(manifest, imdbId, type, season, episode) }.onFailure { if(it is kotlinx.coroutines.CancellationException) throw it }.getOrDefault(emptyList()) }
        }.awaitAll().flatten()

        StreamOrdering.sorted(rows.filter { StreamFilters.allowed(it,type,movieYear,maxBytes,movieTitle) })
    }

    private suspend fun resolveOne(manifest: String, imdbId: String, type: String, season: Int, episode: Int): List<StreamChoice> {
        val base = manifest.removeSuffix("/manifest.json")
        val stremioType = if (type == "series") "series" else "movie"
        val mediaId = if (stremioType == "series") "$imdbId:$season:$episode" else imdbId
        val addonName = runCatching { URI(manifest).host ?: "Add-on" }.getOrDefault("Add-on")
        val json = JSONObject(Http.text("$base/stream/$stremioType/$mediaId.json", timeoutMs = 18000))
        val a = json.optJSONArray("streams") ?: return emptyList()
        return (0 until a.length()).mapNotNull { i ->
            val s = a.optJSONObject(i) ?: return@mapNotNull null
            val url = s.optString("url").takeIf { it.startsWith("http") } ?: return@mapNotNull null
            val hints = s.optJSONObject("behaviorHints")
            val filename = hints?.optString("filename").orEmpty()
            val videoHash = hints?.optString("videoHash").orEmpty()
            val videoSize = hints?.optLong("videoSize", 0L) ?: 0L
            val subs = s.optJSONArray("subtitles")?.let { sa ->
                (0 until sa.length()).mapNotNull { si ->
                    val sub = sa.optJSONObject(si) ?: return@mapNotNull null
                    val su = sub.optString("url").takeIf { it.startsWith("http") } ?: return@mapNotNull null
                    SubtitleChoice(su, normalizeLang(sub.optString("lang")), sub.optString("label"), source = addonName)
                }
            } ?: emptyList()
            StreamChoice(
                name = s.optString("name", addonName),
                title = s.optString("title", s.optString("name", "Stream")),
                url = url,
                subtitles = subs,
                filename = filename,
                videoHash = videoHash,
                videoSize = videoSize,
                addonName = addonName
            )
        }
    }

    private fun normalizeLang(raw: String): String {
        val x = raw.trim().lowercase()
        return when (x) {
            "spa", "es-es", "es_419", "spanish", "español" -> "es"
            "eng", "en-us", "en-gb", "english" -> "en"
            else -> x.ifBlank { raw }
        }
    }

}
