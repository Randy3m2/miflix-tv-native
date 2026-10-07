package com.miflix.native2.data

import com.miflix.native2.model.CastMember
import com.miflix.native2.model.EpisodeSummary
import com.miflix.native2.model.MediaDetails
import com.miflix.native2.model.MediaSummary
import com.miflix.native2.model.TrailerSummary
import org.json.JSONObject
import java.net.URLEncoder
import java.util.Locale

class TmdbRepository(var token: String) {
    private val base = "https://api.themoviedb.org/3"

    private suspend fun get(path: String, params: Map<String, String> = emptyMap()): JSONObject {
        val all = LinkedHashMap(params)
        if (!all.containsKey("language")) all["language"] = "en-US"
        val credential = token.trim()
        if (credential.isBlank()) throw IllegalStateException("TMDB is not configured")
        if (!credential.startsWith("eyJ")) all["api_key"] = credential
        val query = all.entries.joinToString("&") { (k, v) ->
            URLEncoder.encode(k, "UTF-8") + "=" + URLEncoder.encode(v, "UTF-8")
        }
        val headers = if (credential.startsWith("eyJ")) mapOf("Authorization" to "Bearer $credential") else emptyMap()
        return JSONObject(Http.text("$base$path?$query", headers = headers))
    }

    suspend fun trending(): List<MediaSummary> = parseList(get("/trending/all/week"))
    suspend fun popularMovies(): List<MediaSummary> = parseList(get("/movie/popular"), "movie")
    suspend fun popularSeries(): List<MediaSummary> = parseList(get("/tv/popular"), "series")
    suspend fun topRatedMovies(): List<MediaSummary> = parseList(get("/movie/top_rated"), "movie")
    suspend fun search(query: String): List<MediaSummary> = parseList(get("/search/multi", mapOf("query" to query)))

    suspend fun discoverGenre(genreId: Int): List<MediaSummary> = parseList(
        get("/discover/movie", mapOf("with_genres" to genreId.toString(), "sort_by" to "popularity.desc")),
        "movie"
    )

    suspend fun discoverProvider(providerId: Int, region: String = "US"): List<MediaSummary> = discoverProviders(listOf(providerId), region)

    suspend fun discoverProviders(providerIds: List<Int>, region: String = "US"): List<MediaSummary> = parseList(
        get(
            "/discover/movie",
            mapOf(
                "with_watch_providers" to providerIds.joinToString("|"),
                "watch_region" to region,
                "sort_by" to "popularity.desc"
            )
        ),
        "movie"
    )

    suspend fun details(item: MediaSummary): MediaDetails {
        val kind = if (item.type == "series") "tv" else "movie"
        val j = get("/$kind/${item.id}", mapOf("append_to_response" to "external_ids,credits,videos"))
        val ext = j.optJSONObject("external_ids")
        val imdb = if (kind == "movie") j.optString("imdb_id") else ext?.optString("imdb_id")
        val genres = j.optJSONArray("genres")?.let { a ->
            (0 until a.length()).mapNotNull { i -> a.optJSONObject(i)?.optString("name")?.takeIf { it.isNotBlank() } }
        } ?: emptyList()

        val runtime = if (kind == "movie") {
            val mins = j.optInt("runtime", 0)
            if (mins > 0) "${mins / 60}h ${mins % 60}m" else "Movie"
        } else {
            val mins = j.optJSONArray("episode_run_time")?.optInt(0, 0) ?: 0
            if (mins > 0) "${mins}m episodes" else "${j.optInt("number_of_seasons", 0)} seasons"
        }

        val release = if (kind == "movie") j.optString("release_date") else j.optString("first_air_date")
        val origin = if (kind == "movie") {
            j.optJSONArray("production_countries")?.let { a ->
                (0 until a.length()).mapNotNull { i -> a.optJSONObject(i)?.optString("name")?.takeIf { it.isNotBlank() } }.take(2).joinToString(", ")
            }.orEmpty()
        } else {
            j.optJSONArray("origin_country")?.let { a ->
                (0 until a.length()).map { i -> a.optString(i) }.filter { it.isNotBlank() }.joinToString(", ")
            }.orEmpty()
        }
        val originalLanguage = j.optString("original_language").uppercase(Locale.US)

        val cast = j.optJSONObject("credits")?.optJSONArray("cast")?.let { a ->
            (0 until minOf(a.length(), 10)).mapNotNull { i ->
                val x = a.optJSONObject(i) ?: return@mapNotNull null
                val name = x.optString("name").takeIf { it.isNotBlank() } ?: return@mapNotNull null
                CastMember(
                    name = name,
                    character = x.optString("character"),
                    profile = image(x.optString("profile_path"), "w185")
                )
            }
        } ?: emptyList()

        val trailers = j.optJSONObject("videos")?.optJSONArray("results")?.let { a ->
            val rows = (0 until a.length()).mapNotNull { i ->
                val x = a.optJSONObject(i) ?: return@mapNotNull null
                val key = x.optString("key").takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val site = x.optString("site")
                if (!site.equals("YouTube", ignoreCase = true)) return@mapNotNull null
                TrailerSummary(
                    name = x.optString("name", "Trailer"),
                    key = key,
                    site = site,
                    type = x.optString("type", "Trailer"),
                    official = x.optBoolean("official", false)
                )
            }
            rows.sortedWith(compareByDescending<TrailerSummary> { it.official }.thenBy { if (it.type.equals("Trailer", true)) 0 else 1 }).take(6)
        } ?: emptyList()

        return MediaDetails(
            summary = item,
            imdbId = imdb?.takeIf { it.isNotBlank() },
            runtimeText = runtime,
            genres = genres,
            seasonCount = j.optInt("number_of_seasons", 0),
            releaseInfo = release.take(4).ifBlank { item.year },
            originCountry = origin,
            originalLanguage = originalLanguage,
            cast = cast,
            trailers = trailers
        )
    }

    suspend fun season(showId: Int, season: Int): List<EpisodeSummary> {
        val j = get("/tv/$showId/season/$season")
        val a = j.optJSONArray("episodes") ?: return emptyList()
        return (0 until a.length()).mapNotNull { i ->
            val x = a.optJSONObject(i) ?: return@mapNotNull null
            EpisodeSummary(
                season = season,
                episode = x.optInt("episode_number", i + 1),
                title = x.optString("name", "Episode ${i + 1}"),
                overview = x.optString("overview"),
                still = image(x.optString("still_path"), "w500"),
                rating = x.optDouble("vote_average", 0.0),
                airDate = x.optString("air_date")
            )
        }
    }

    suspend fun byCloudId(cloudId: String): MediaSummary? {
        val m = Regex("^tmdb:(movie|series):(\\d+)$").find(cloudId) ?: return null
        val type = m.groupValues[1]
        val id = m.groupValues[2].toIntOrNull() ?: return null
        val kind = if (type == "series") "tv" else "movie"
        val j = get("/$kind/$id")
        return mapItem(j, type)
    }

    private fun parseList(j: JSONObject, forceType: String? = null): List<MediaSummary> {
        val a = j.optJSONArray("results") ?: return emptyList()
        return (0 until a.length()).mapNotNull { i ->
            val x = a.optJSONObject(i) ?: return@mapNotNull null
            val mediaType = forceType ?: when (x.optString("media_type")) {
                "tv" -> "series"
                "movie" -> "movie"
                else -> return@mapNotNull null
            }
            mapItem(x, mediaType)
        }.filter { it.backdrop != null || it.poster != null }.take(20)
    }

    private fun mapItem(x: JSONObject, type: String): MediaSummary {
        val isSeries = type == "series"
        val title = if (isSeries) x.optString("name", x.optString("original_name")) else x.optString("title", x.optString("original_title"))
        val date = if (isSeries) x.optString("first_air_date") else x.optString("release_date")
        return MediaSummary(
            id = x.optInt("id"),
            type = type,
            title = title.ifBlank { "Untitled" },
            overview = x.optString("overview"),
            backdrop = image(x.optString("backdrop_path"), "w1280"),
            poster = image(x.optString("poster_path"), "w500"),
            rating = x.optDouble("vote_average", 0.0),
            year = date.take(4)
        )
    }

    private fun image(path: String?, size: String): String? = path
        ?.takeIf { it.isNotBlank() && it != "null" }
        ?.let { "https://image.tmdb.org/t/p/$size$it" }
}
