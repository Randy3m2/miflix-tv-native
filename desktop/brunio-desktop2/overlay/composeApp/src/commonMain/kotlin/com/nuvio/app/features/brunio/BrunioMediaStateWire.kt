package com.nuvio.app.features.brunio

import com.nuvio.app.features.library.LibraryItem
import com.nuvio.app.features.watchprogress.WatchProgressEntry
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull

/** BruniO resume positions are seconds, whereas the Desktop engine uses ms. */
internal object BrunioMediaStateWire {
    private val id = Regex("^tmdb:(movie|series):([0-9]+)(?::s([0-9]+)e([0-9]+))?$")

    fun favoriteItems(state: JsonObject): List<LibraryItem> {
        val array = state["favorites"] as? JsonArray ?: return emptyList()
        return array.mapNotNull { value ->
            val cloud = (value as? JsonPrimitive)?.content ?: return@mapNotNull null
            val match = id.matchEntire(cloud) ?: return@mapNotNull null
            val kind = match.groupValues[1]
            if (match.groupValues[3].isNotEmpty()) return@mapNotNull null
            LibraryItem(
                id = cloud, type = kind,
                name = if (kind == "movie") "Movie ${match.groupValues[2]}" else "Series ${match.groupValues[2]}",
                tmdbId = match.groupValues[2].toIntOrNull(), savedAtEpochMs = 0L,
            )
        }.distinctBy { it.id }
    }

    fun progressEntries(state: JsonObject): List<WatchProgressEntry> {
        val progress = state["progress"] as? JsonObject ?: return emptyList()
        val keys = progress.keys
        return progress.mapNotNull { (key, raw) ->
            val match = id.matchEntire(key) ?: return@mapNotNull null
            val value = raw as? JsonObject ?: return@mapNotNull null
            val kind = match.groupValues[1]
            val base = "tmdb:$kind:${match.groupValues[2]}"
            val explicitSeason = match.groupValues[3].toIntOrNull()
            val explicitEpisode = match.groupValues[4].toIntOrNull()
            val season = explicitSeason ?: number(value, "season")?.toInt()?.takeIf { it > 0 }
            val episode = explicitEpisode ?: number(value, "episode")?.toInt()?.takeIf { it > 0 }
            // BruniO may store the same episode on both series and episode keys.
            if (key == base && season != null && episode != null &&
                "$base:s${season}e$episode" in keys
            ) return@mapNotNull null
            val position = secondsToMs(number(value, "position") ?: 0.0)
            val duration = secondsToMs(number(value, "duration") ?: 0.0)
            val percent = number(value, "percent")?.toFloat()?.coerceIn(0f, 100f)
            val updated = (value["updatedAt"] as? JsonPrimitive)?.longOrNull ?: 0L
            val episodeKey = if (kind == "series" && season != null && episode != null)
                "$base:s${season}e$episode" else base
            WatchProgressEntry(
                contentType = kind, parentMetaId = base, parentMetaType = kind,
                videoId = episodeKey,
                title = if (kind == "movie") "Movie ${match.groupValues[2]}" else "Series ${match.groupValues[2]}",
                seasonNumber = season, episodeNumber = episode,
                lastPositionMs = position, durationMs = duration,
                progressPercent = percent,
                lastUpdatedEpochMs = updated.coerceAtLeast(0L),
                progressKey = episodeKey,
                isCompleted = percent != null && percent >= 90f,
            )
        }.distinctBy { it.progressKey }
    }

    private fun number(row: JsonObject, field: String): Double? =
        (row[field] as? JsonPrimitive)?.doubleOrNull?.takeIf { it.isFinite() }

    private fun secondsToMs(value: Double): Long =
        (value.coerceAtLeast(0.0).coerceAtMost(Long.MAX_VALUE.toDouble() / 1000.0) * 1000.0).toLong()
}
