package com.nuvio.app.features.brunio

import com.nuvio.app.core.auth.AuthRepository
import com.nuvio.app.core.auth.AuthState
import com.nuvio.app.core.network.SupabaseProvider
import com.nuvio.app.features.home.HomeCatalogSection
import com.nuvio.app.features.profiles.ProfileRepository
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull

internal data class BrunioHomeRow(
    val id: String,
    val label: String,
    val endpoint: String,
    val kind: String,
    val query: Map<String, String> = emptyMap(),
)

/** Provider IDs and discovery concept match the original BruniO TV Home. */
internal val BrunioOptionalHomeRows = listOf(
    BrunioHomeRow("netflix", "Netflix", "discover/movie", "movie",
        mapOf("with_watch_providers" to "8|175", "watch_region" to "US")),
    BrunioHomeRow("disney", "Disney+", "discover/movie", "movie",
        mapOf("with_watch_providers" to "337", "watch_region" to "US")),
    BrunioHomeRow("prime", "Prime Video", "discover/movie", "movie",
        mapOf("with_watch_providers" to "9|119", "watch_region" to "US")),
    BrunioHomeRow("apple", "Apple TV", "discover/movie", "movie",
        mapOf("with_watch_providers" to "350", "watch_region" to "US")),
    BrunioHomeRow("hbo", "HBO Max", "discover/movie", "movie",
        mapOf("with_watch_providers" to "1899|384", "watch_region" to "US")),
    BrunioHomeRow("crunchyroll", "Crunchyroll", "discover/tv", "series",
        mapOf("with_watch_providers" to "283", "watch_region" to "US")),
    BrunioHomeRow("hulu", "Hulu", "discover/movie", "movie",
        mapOf("with_watch_providers" to "15", "watch_region" to "US")),
    BrunioHomeRow("paramount", "Paramount+", "discover/movie", "movie",
        mapOf("with_watch_providers" to "531|582", "watch_region" to "US")),
    BrunioHomeRow("amcplus", "AMC+", "discover/movie", "movie",
        mapOf("with_watch_providers" to "526|528", "watch_region" to "US")),
    BrunioHomeRow("action", "Acción", "discover/movie", "movie",
        mapOf("with_genres" to "28")),
    BrunioHomeRow("scifi", "Ciencia ficción", "discover/movie", "movie",
        mapOf("with_genres" to "878")),
    BrunioHomeRow("comedy", "Comedias", "discover/movie", "movie",
        mapOf("with_genres" to "35")),
    BrunioHomeRow("animation", "Animación", "discover/movie", "movie",
        mapOf("with_genres" to "16")),
    BrunioHomeRow("documentary", "Documentales", "discover/tv", "series",
        mapOf("with_genres" to "99")),
)

internal expect fun loadBrunioHomeRowIds(): Set<String>
internal expect fun saveBrunioHomeRowIds(ids: Set<String>)

internal object BrunioHomePreferences {
    private val defaults = setOf("netflix", "disney", "prime", "hbo", "hulu")
    private val _selected = MutableStateFlow(loadBrunioHomeRowIds().takeIf { it.isNotEmpty() } ?: defaults)
    val selected: StateFlow<Set<String>> = _selected

    fun toggle(row: String) {
        if (BrunioOptionalHomeRows.none { it.id == row }) return
        val next = if (row in _selected.value) _selected.value - row else _selected.value + row
        _selected.value = next
        saveBrunioHomeRowIds(next)
    }
}

internal expect fun brunioStreamingLogoUri(id: String): String?
internal expect fun brunioDefaultSportsManifest(): String
internal expect fun brunioSavedSportsManifest(): String
internal expect fun brunioSaveSportsManifest(url: String)

internal object BrunioHomeExtras {
    suspend fun load(): List<HomeCatalogSection> = coroutineScope {
        val permits = Semaphore(3)
        BrunioOptionalHomeRows.filter { it.id in BrunioHomePreferences.selected.value }
            .map { item ->
                async {
                    permits.withPermit {
                        try {
                            val movies = BrunioTmdb.catalog(item.endpoint, item.kind,
                                item.query + mapOf("sort_by" to "popularity.desc"))
                            if (movies.isEmpty()) null
                            else BrunioTmdb.section(item.id, item.label, item.kind, movies)
                        } catch (cancel: CancellationException) {
                            throw cancel
                        } catch (_: Exception) { null }
                    }
                }
            }.awaitAll().filterNotNull()
    }
}

/** Reads only the authenticated profile's TV rating scores from miflix_ratings. */
internal object BrunioRatingsRecommendations {
    private val json = Json { ignoreUnknownKeys = true }
    private data class Rated(val id: String, val score: Int)

    suspend fun load(): List<HomeCatalogSection> {
        val auth = AuthRepository.state.value as? AuthState.Authenticated ?: return emptyList()
        if (auth.isAnonymous) return emptyList()
        val profileId = BrunioCloud.profileId(ProfileRepository.activeProfileId) ?: return emptyList()
        val results = SupabaseProvider.client.postgrest.from("miflix_ratings").select {
            filter {
                eq("user_id", auth.userId)
                eq("profile_id", profileId)
            }
        }.data
        val rows = json.parseToJsonElement(results) as? JsonArray ?: return emptyList()
        val ratings = rows.mapNotNull { row ->
            val record = row as? JsonObject ?: return@mapNotNull null
            val id = (record["cloud_id"] as? JsonPrimitive)?.contentOrNull ?: return@mapNotNull null
            val score = (record["score"] as? JsonPrimitive)?.intOrNull ?: return@mapNotNull null
            if (!Regex("^tmdb:(movie|series):[0-9]+$").matches(id) || score !in 7..10)
                return@mapNotNull null
            Rated(id, score)
        }.sortedByDescending { it.score }.take(5)
        if (ratings.isEmpty()) return emptyList()
        return coroutineScope {
            val permits = Semaphore(3)
            val candidates = ratings.map { rated ->
                async {
                    permits.withPermit {
                        val kind = if (rated.id.contains(":series:")) "tv" else "movie"
                        val id = rated.id.substringAfterLast(':')
                        val type = if (kind == "tv") "series" else "movie"
                        try {
                            BrunioTmdb.catalog("$kind/$id/recommendations", type)
                                .take(12).map { item -> item to rated.score }
                        } catch (cancel: CancellationException) {
                            throw cancel
                        } catch (_: Exception) { emptyList() }
                    }
                }
            }.awaitAll().flatten()
            val consumedIds = ratings.map { it.id }.toSet()
            val ranked = candidates.filterNot { it.first.id in consumedIds }
                .groupBy { it.first.id }
                .values.map { matches ->
                    matches.first().first to matches.sumOf { it.second }
                }.sortedByDescending { it.second }.map { it.first }.take(18)
            if (ranked.isEmpty()) emptyList()
            else BrunioTmdb.sections("Para ti · Según tus valoraciones", ranked)
        }
    }
}
