package com.nuvio.app.features.brunio

import com.nuvio.app.features.addons.httpGetTextWithHeaders
import com.nuvio.app.features.catalog.CatalogTarget
import com.nuvio.app.features.home.HomeCatalogSection
import com.nuvio.app.features.home.MetaPreview
import com.nuvio.app.features.tmdb.TmdbSettingsRepository
import io.ktor.http.encodeURLParameter
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * BruniO uses TMDB for discovery and stream add-ons only for playable sources.
 * Nuvio's default Home uses Stremio catalog add-ons; Torrentio/Comet only expose
 * stream resources, so Home/Search would otherwise be permanently empty.
 *
 * Supports TMDB v3 API key and TMDB v4 JWT read token.
 */
internal object BrunioTmdb {
    private val json = Json { ignoreUnknownKeys = true }
    private val image = "https://image.tmdb.org/t/p/"

    suspend fun fetchRaw(endpoint: String, query: Map<String, String> = emptyMap()): String {
        val token = TmdbSettingsRepository.effectiveApiKey().trim()
        check(token.isNotBlank()) { "Configura tu token de TMDB en BruniO TV o PC" }
        require(endpoint.isNotBlank() && !endpoint.startsWith("/") &&
            !endpoint.contains("..") && !endpoint.contains("?"))
        val params = linkedMapOf("language" to TmdbSettingsRepository.snapshot().language.ifBlank { "es-ES" })
        params.putAll(query)
        val bearer = token.startsWith("eyJ")
        if (!bearer) params["api_key"] = token
        val suffix = params.entries.joinToString("&") { (k, v) ->
            "${k.encodeURLParameter()}=${v.encodeURLParameter()}"
        }
        val url = "https://api.themoviedb.org/3/$endpoint?$suffix"
        return httpGetTextWithHeaders(url, if (bearer) {
            mapOf("Authorization" to "Bearer $token", "Accept" to "application/json")
        } else mapOf("Accept" to "application/json"))
    }

    suspend fun catalog(endpoint: String, kind: String? = null): List<MetaPreview> =
        decodeResults(fetchRaw(endpoint), kind)

    suspend fun search(term: String): List<MetaPreview> {
        val normalized = term.trim().take(120)
        if (normalized.isEmpty()) return emptyList()
        return decodeResults(fetchRaw("search/multi", mapOf(
            "query" to normalized,
            "include_adult" to "false",
        ))).filter { it.type == "movie" || it.type == "series" }
    }

    fun sections(label: String, source: List<MetaPreview>): List<HomeCatalogSection> =
        source.groupBy { it.type }.map { (kind, items) ->
            section("\$label-\$kind",
                if (kind == "series") "$label · Series" else "$label · Películas",
                kind, items)
        }

    fun section(key: String, label: String, kind: String, items: List<MetaPreview>): HomeCatalogSection =
        HomeCatalogSection(
            key = "brunio-tmdb-$key",
            title = label,
            subtitle = "The Movie Database",
            addonName = "TMDB",
            target = CatalogTarget.Addon(
                manifestUrl = "https://api.themoviedb.org/3",
                contentType = kind, catalogId = key, supportsPagination = false,
            ),
            items = items.take(18),
            availableItemCount = 0, // no fake "View all" Stremio catalog navigation
            hasMore = false,
        )

    internal fun decodeResults(raw: String, forcedKind: String? = null): List<MetaPreview> {
        val rows = (json.parseToJsonElement(raw) as? JsonObject)
            ?.get("results") as? JsonArray ?: return emptyList()
        return rows.mapNotNull { value ->
            val row = value as? JsonObject ?: return@mapNotNull null
            val numericId = (row["id"] as? JsonPrimitive)?.intOrNull?.takeIf { it > 0 }
                ?: return@mapNotNull null
            val kind = forcedKind ?: when (row.string("media_type")) {
                "movie" -> "movie"
                "tv" -> "series"
                else -> if (row["first_air_date"] != null) "series" else "movie"
            }
            if (kind != "movie" && kind != "series") return@mapNotNull null
            val title = row.string("title") ?: row.string("name") ?: return@mapNotNull null
            val poster = row.string("poster_path")
                ?.takeIf { it.startsWith("/") }
                ?.let { image + "w500" + it }
            val backdrop = row.string("backdrop_path")
                ?.takeIf { it.startsWith("/") }
                ?.let { image + "w1280" + it }
            MetaPreview(
                id = "tmdb:$kind:$numericId",
                type = kind,
                name = title,
                poster = poster,
                banner = backdrop,
                description = row.string("overview"),
                releaseInfo = (row.string("release_date") ?: row.string("first_air_date"))
                    ?.take(4),
            )
        }.distinctBy { it.id }
    }

    private fun JsonObject.string(name: String): String? =
        (this[name] as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf(String::isNotBlank)
}
