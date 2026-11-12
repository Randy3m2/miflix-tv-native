package com.nuvio.app.features.brunio

import com.nuvio.app.features.library.LibraryItem
import com.nuvio.app.features.library.LibraryRepository
import com.nuvio.app.features.profiles.ProfileRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/** Asynchronously enrich BruniO saved TMDB IDs without touching their membership. */
internal object BrunioLibraryHydrator {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val json = Json { ignoreUnknownKeys = true }
    private val cache = mutableMapOf<String, LibraryItem>()
    private var job: Job? = null

    fun refresh(profileIndex: Int, favorites: List<LibraryItem>) {
        job?.cancel()
        val targetUser = com.nuvio.app.core.auth.AuthRepository.state.value
        job = scope.launch {
            val limit = Semaphore(4)
            val targets = favorites.filter { it.tmdbId != null && it.poster.isNullOrEmpty() }
            targets.chunked(12).forEach { batch ->
                val hydrated = batch.map { item ->
                    async {
                        limit.withPermit {
                            val cached = synchronized(cache) { cache[item.id] }
                            if (cached != null) return@withPermit cached
                            runCatching {
                                val type = if (item.type == "series") "tv" else "movie"
                                val raw = BrunioTmdb.fetchRaw("$type/${item.tmdbId}")
                                val meta = json.parseToJsonElement(raw) as? JsonObject
                                    ?: return@runCatching null
                                fun field(key: String): String? =
                                    (meta[key] as? JsonPrimitive)?.contentOrNull?.trim()
                                        ?.takeIf(String::isNotEmpty)
                                val name = field("title") ?: field("name") ?: item.name
                                val poster = field("poster_path")?.takeIf { it.startsWith("/") }
                                    ?.let { "https://image.tmdb.org/t/p/w500$it" }
                                val banner = field("backdrop_path")?.takeIf { it.startsWith("/") }
                                    ?.let { "https://image.tmdb.org/t/p/w1280$it" }
                                item.copy(
                                    name = name, poster = poster, banner = banner,
                                    description = field("overview"),
                                    releaseInfo = (field("release_date") ?: field("first_air_date"))
                                        ?.take(4),
                                ).also { new -> synchronized(cache) { cache[item.id] = new } }
                            }.getOrNull()
                        }
                    }
                }.awaitAll().filterNotNull()
                if (hydrated.isNotEmpty() &&
                    ProfileRepository.activeProfileId == profileIndex &&
                    com.nuvio.app.core.auth.AuthRepository.state.value == targetUser
                ) {
                    LibraryRepository.applyBrunioMetadata(hydrated)
                } else if (ProfileRepository.activeProfileId != profileIndex) {
                    return@launch
                }
            }
        }
    }
}
