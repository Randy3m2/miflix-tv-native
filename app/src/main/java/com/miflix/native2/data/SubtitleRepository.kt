package com.miflix.native2.data

import com.miflix.native2.model.StreamChoice
import com.miflix.native2.model.SubtitleChoice
import org.json.JSONObject
import java.net.URLEncoder

class SubtitleRepository(
    private val v3BaseUrl: String = "https://opensubtitles-v3.strem.io",
    private val officialFallbackBaseUrl: String = "https://opensubtitles.strem.io/stremio/v1"
) {
    suspend fun resolve(
        imdbId: String,
        type: String,
        season: Int = 0,
        episode: Int = 0,
        stream: StreamChoice? = null
    ): List<SubtitleChoice> {
        val primary = resolveFrom(v3BaseUrl, "OpenSubtitles v3", imdbId, type, season, episode, stream)
        val hasEs = primary.any { it.lang == "es" }
        val hasEn = primary.any { it.lang == "en" }
        if (hasEs && hasEn) return prioritize(primary)

        val fallback = runCatching {
            resolveFrom(officialFallbackBaseUrl, "OpenSubtitles official fallback", imdbId, type, season, episode, stream)
        }.getOrDefault(emptyList())
        return prioritize((primary + fallback).distinctBy { it.url })
    }

    private suspend fun resolveFrom(
        baseUrl: String,
        sourceName: String,
        imdbId: String,
        type: String,
        season: Int,
        episode: Int,
        stream: StreamChoice?
    ): List<SubtitleChoice> {
        val stremioType = if (type == "series") "series" else "movie"
        val videoId = if (stremioType == "series") "$imdbId:$season:$episode" else imdbId
        val extra = buildList {
            stream?.videoHash?.takeIf { it.isNotBlank() }?.let { add("videoHash=${enc(it)}") }
            stream?.videoSize?.takeIf { it > 0 }?.let { add("videoSize=$it") }
            stream?.filename?.takeIf { it.isNotBlank() }?.let { add("filename=${enc(it)}") }
        }.joinToString("&").ifBlank { "*" }

        val urls = buildList {
            add("$baseUrl/subtitles/$stremioType/$videoId/$extra.json")
            if (extra != "*") add("$baseUrl/subtitles/$stremioType/$videoId/*.json")
        }

        // A Sequence callback cannot call suspend HTTP functions. Try URLs in order.
        for (url in urls) {
            val rows = runCatching {
                parse(Http.text(url, timeoutMs = 12000), sourceName)
            }.getOrNull()
            if (!rows.isNullOrEmpty()) return rows
        }
        return emptyList()
    }

    private fun parse(text: String, sourceName: String): List<SubtitleChoice> {
        val j = JSONObject(text)
        val a = j.optJSONArray("subtitles") ?: return emptyList()
        return (0 until a.length()).mapNotNull { i ->
            val x = a.optJSONObject(i) ?: return@mapNotNull null
            val url = x.optString("url").takeIf { it.startsWith("http") } ?: return@mapNotNull null
            val lang = normalizeLang(x.optString("lang"))
            val label = x.optString("label").ifBlank {
                when (lang) {
                    "es" -> "Español · $sourceName"
                    "en" -> "English · $sourceName"
                    else -> "${x.optString("lang", "Subtitle")} · $sourceName"
                }
            }
            SubtitleChoice(url, lang, label, source = sourceName)
        }
    }

    private fun prioritize(rows: List<SubtitleChoice>): List<SubtitleChoice> = rows
        .distinctBy { it.url }
        .sortedWith(compareBy<SubtitleChoice> { languageRank(it.lang) }.thenBy { it.label.lowercase() })

    private fun normalizeLang(raw: String): String {
        val x = raw.trim().lowercase()
        return when (x) {
            "spa", "es-es", "es-la", "es_419", "spanish", "español" -> "es"
            "eng", "en-us", "en-gb", "english" -> "en"
            else -> x
        }
    }

    private fun languageRank(lang: String): Int = when (normalizeLang(lang)) {
        "es" -> 0
        "en" -> 1
        else -> 2
    }

    private fun enc(value: String): String = URLEncoder.encode(value, "UTF-8")
}
