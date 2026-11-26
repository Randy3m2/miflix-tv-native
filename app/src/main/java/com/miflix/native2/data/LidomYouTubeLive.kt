package com.miflix.native2.data

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.stream.JsonReader

internal data class LidomVideo(val id: String, val title: String)

internal object LidomYouTubeLive {
    private val initialData = Regex("""(?:\bytInitialData|window\["ytInitialData"\])\s*=\s*(?=\{)""")
    private val videoId = Regex("[A-Za-z0-9_-]{11}")

    fun find(page: String, teamName: String? = null): LidomVideo? {
        val start = initialData.find(page)?.range?.last?.plus(1)
            ?: error("Missing YouTube channel data")
        val data = JsonParser.parseReader(JsonReader(page.substring(start).reader())).asJsonObject
        val contents = data.get("contents") ?: error("Missing YouTube channel contents")
        return entries(contents).firstOrNull { video ->
            teamName == null || LidomUrlPolicy.matchesStation(video.title, teamName)
        }
    }

    private fun entries(node: JsonElement): Sequence<LidomVideo> = sequence {
        when {
            node.isJsonArray -> node.asJsonArray.forEach { yieldAll(entries(it)) }
            node.isJsonObject -> node.asJsonObject.entrySet().forEach { (key, value) ->
                if (key == "videoRenderer" || key == "lockupViewModel") {
                    if (value.isJsonObject) liveVideo(value.asJsonObject)?.let { yield(it) }
                } else {
                    yieldAll(entries(value))
                }
            }
        }
    }

    private fun liveVideo(card: JsonObject): LidomVideo? {
        val id = card["videoId"]?.asString ?: card["contentId"]?.asString ?: return null
        if (!videoId.matches(id) || !hasLiveBadge(card)) return null
        val title = card["title"]?.asJsonObject?.let { text ->
            text["simpleText"]?.asString ?: text["runs"]?.asJsonArray
                ?.joinToString("") { it.asJsonObject["text"].asString }
        } ?: card["metadata"]?.asJsonObject?.get("lockupMetadataViewModel")?.asJsonObject
            ?.get("title")?.asJsonObject?.get("content")?.asString
        return LidomVideo(id, title.orEmpty())
    }

    private fun hasLiveBadge(node: JsonElement): Boolean = when {
        node.isJsonArray -> node.asJsonArray.any(::hasLiveBadge)
        node.isJsonObject -> node.asJsonObject.entrySet().any { (key, value) ->
            when {
                key == "isLiveNow" && value.isJsonPrimitive && value.asJsonPrimitive.isBoolean ->
                    value.asBoolean
                key == "style" && value.isJsonPrimitive ->
                    value.asString in setOf("BADGE_STYLE_TYPE_LIVE_NOW", "LIVE", "THUMBNAIL_OVERLAY_BADGE_STYLE_LIVE", "THUMBNAIL_BADGE_STYLE_LIVE")
                else -> hasLiveBadge(value)
            }
        }
        else -> false
    }
}
