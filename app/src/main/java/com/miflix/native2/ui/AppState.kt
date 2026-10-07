package com.miflix.native2.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.miflix.native2.BuildConfig
import com.miflix.native2.data.LocalStore
import com.miflix.native2.data.StreamRepository
import com.miflix.native2.data.SupabaseRepository
import com.miflix.native2.data.TmdbRepository
import com.miflix.native2.model.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.supervisorScope

class AppState(context: Context) {
    private val local = LocalStore(context)
    private val cloud = SupabaseRepository(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_KEY)
    val tmdb = TmdbRepository(BuildConfig.MIFLIX_TMDB_TOKEN)
    val streamRepo = StreamRepository(BuildConfig.MIFLIX_TORRENTIO_MANIFEST)

    var screen by mutableStateOf(Screen.SPLASH)
    var session by mutableStateOf(local.loadSession())
    var activeProfile by mutableStateOf(Profile("default", "Main", primary = true))
    val profiles = mutableStateListOf<Profile>()
    val favorites = mutableStateListOf<String>()
    val progress = mutableStateMapOf<String, PlaybackProgress>()

    val trending = mutableStateListOf<MediaSummary>()
    val movies = mutableStateListOf<MediaSummary>()
    val series = mutableStateListOf<MediaSummary>()
    val topRated = mutableStateListOf<MediaSummary>()

    // Alpha 2 shelves load after the core Home is already usable.
    val continueWatching = mutableStateListOf<MediaSummary>()
    val action = mutableStateListOf<MediaSummary>()
    val sciFi = mutableStateListOf<MediaSummary>()
    val netflix = mutableStateListOf<MediaSummary>()
    val disney = mutableStateListOf<MediaSummary>()
    var extrasLoaded by mutableStateOf(false)
    var extrasLoading by mutableStateOf(false)

    var selected by mutableStateOf<MediaSummary?>(null)
    var details by mutableStateOf<MediaDetails?>(null)
    val episodes = mutableStateListOf<EpisodeSummary>()
    var currentSeason by mutableStateOf(1)
    var playerRequest by mutableStateOf<PlayerRequest?>(null)
    var busyMessage by mutableStateOf<String?>(null)
    var error by mutableStateOf<String?>(null)

    suspend fun bootstrap() {
        session?.let { s ->
            if (s.refreshToken.isNotBlank()) {
                runCatching { cloud.refresh(s.refreshToken) }.onSuccess {
                    session = it
                    local.saveSession(it)
                }
            }
            runCatching { syncFromCloud() }
        }
        if (tmdb.token.isNotBlank()) {
            runCatching { loadHome() }.onFailure { error = it.message }
        }
        screen = if (tmdb.token.isNotBlank()) Screen.HOME else Screen.SETTINGS
    }

    suspend fun login(email: String, password: String) {
        busyMessage = "Signing in…"
        error = null
        runCatching { cloud.login(email, password) }.onSuccess {
            session = it
            local.saveSession(it)
            syncFromCloud()
            loadHome()
            screen = Screen.HOME
        }.onFailure { error = it.message }
            .also { busyMessage = null }
    }

    suspend fun syncFromCloud() {
        val s = session ?: return
        val (remoteProfiles, remoteSetup) = cloud.account(s)

        // Prefer the user's private cloud setup. If it is absent, use the build bootstrap
        // values and save them into the authenticated account row for future TV installs.
        val chosenTmdb = remoteSetup.tmdbToken.ifBlank { tmdb.token }
        val chosenTorrentio = remoteSetup.torrentioManifest.ifBlank { streamRepo.manifestUrl }
        if (chosenTmdb.isNotBlank()) tmdb.token = chosenTmdb
        if (chosenTorrentio.isNotBlank()) streamRepo.manifestUrl = chosenTorrentio
        if ((remoteSetup.tmdbToken.isBlank() && chosenTmdb.isNotBlank()) ||
            (remoteSetup.torrentioManifest.isBlank() && chosenTorrentio.isNotBlank())) {
            runCatching { cloud.upsertPrivateSetup(s, PrivateSetup(chosenTmdb, chosenTorrentio)) }
        }

        profiles.clear()
        profiles.addAll(if (remoteProfiles.isEmpty()) listOf(Profile("default", "Main", primary = true)) else remoteProfiles)
        val wanted = local.profileId()
        activeProfile = profiles.firstOrNull { it.id == wanted }
            ?: profiles.firstOrNull { it.primary }
            ?: profiles.first()
        local.saveProfileId(activeProfile.id)

        val (fav, prog) = cloud.profileState(s, activeProfile.id)
        favorites.clear()
        favorites.addAll(fav)
        progress.clear()
        progress.putAll(prog)
        extrasLoaded = false
    }

    suspend fun selectProfile(p: Profile) {
        activeProfile = p
        local.saveProfileId(p.id)
        session?.let {
            val (fav, prog) = cloud.profileState(it, p.id)
            favorites.clear(); favorites.addAll(fav)
            progress.clear(); progress.putAll(prog)
        }
        extrasLoaded = false
        screen = Screen.HOME
    }

    suspend fun pushCloud() {
        session?.let { cloud.upsertProfile(it, activeProfile.id, favorites.toSet(), progress.toMap()) }
    }

    suspend fun loadHome() = supervisorScope {
        val a = async { runCatching { tmdb.trending() } }
        val b = async { runCatching { tmdb.popularMovies() } }
        val c = async { runCatching { tmdb.popularSeries() } }
        val d = async { runCatching { tmdb.topRatedMovies() } }

        a.await().onSuccess { trending.replaceWith(it) }
        b.await().onSuccess { movies.replaceWith(it) }
        c.await().onSuccess { series.replaceWith(it) }
        d.await().onSuccess { topRated.replaceWith(it) }
    }

    suspend fun loadHomeExtras() {
        if (extrasLoaded || extrasLoading || tmdb.token.isBlank()) return
        extrasLoading = true
        try {
            supervisorScope {
                val actionJob = async { runCatching { tmdb.discoverGenre(28) } }
                val sciFiJob = async { runCatching { tmdb.discoverGenre(878) } }
                val netflixJob = async { runCatching { tmdb.discoverProvider(8) } }
                val disneyJob = async { runCatching { tmdb.discoverProvider(337) } }
                val continueJob = async { runCatching { resolveContinueWatching() } }

                actionJob.await().onSuccess { action.replaceWith(it) }
                sciFiJob.await().onSuccess { sciFi.replaceWith(it) }
                netflixJob.await().onSuccess { netflix.replaceWith(it) }
                disneyJob.await().onSuccess { disney.replaceWith(it) }
                continueJob.await().onSuccess { continueWatching.replaceWith(it) }
            }
            extrasLoaded = true
        } finally {
            extrasLoading = false
        }
    }

    private suspend fun resolveContinueWatching(): List<MediaSummary> = coroutineScope {
        val exactCloudId = Regex("^tmdb:(movie|series):\\d+$")
        val ids = progress.entries
            .filter { (id, p) -> exactCloudId.matches(id) && p.percent in 1.0..95.0 }
            .sortedByDescending { it.value.updatedAt }
            .map { it.key }
            .distinct()
            .take(10)

        ids.map { id -> async { runCatching { tmdb.byCloudId(id) }.getOrNull() } }
            .awaitAll()
            .filterNotNull()
    }

    suspend fun open(item: MediaSummary) {
        selected = item
        details = null
        episodes.clear()
        currentSeason = 1
        screen = Screen.DETAILS
        runCatching { tmdb.details(item) }.onSuccess {
            details = it
            if (item.type == "series" && it.seasonCount > 0) loadSeason(1)
        }.onFailure { error = it.message }
    }

    suspend fun loadSeason(season: Int) {
        val item = selected ?: return
        currentSeason = season
        runCatching { tmdb.season(item.id, season) }.onSuccess {
            episodes.clear(); episodes.addAll(it)
        }.onFailure { error = it.message }
    }

    fun toggleFavorite(item: MediaSummary) {
        if (favorites.contains(item.cloudId)) favorites.remove(item.cloudId) else favorites.add(item.cloudId)
    }

    suspend fun playMovie() {
        val item = selected ?: return
        val d = details ?: tmdb.details(item).also { details = it }
        val imdb = d.imdbId ?: throw IllegalStateException("IMDb ID unavailable")
        playResolved(item, imdb, 0, 0)
    }

    suspend fun playEpisode(ep: EpisodeSummary) {
        val item = selected ?: return
        val d = details ?: tmdb.details(item).also { details = it }
        val imdb = d.imdbId ?: throw IllegalStateException("IMDb ID unavailable")
        playResolved(item, imdb, ep.season, ep.episode)
    }

    suspend fun resumeSeries() {
        val item = selected ?: return
        val d = details ?: tmdb.details(item).also { details = it }
        val imdb = d.imdbId ?: throw IllegalStateException("IMDb ID unavailable")
        val p = progress[item.cloudId]
        val season = p?.season?.takeIf { it > 0 } ?: 1
        val episode = p?.episode?.takeIf { it > 0 } ?: 1
        playResolved(item, imdb, season, episode)
    }

    private suspend fun playResolved(item: MediaSummary, imdb: String, season: Int, episode: Int) {
        busyMessage = "Finding the best source…"
        error = null
        runCatching { streamRepo.resolve(imdb, item.type, season, episode) }.onSuccess { streams ->
            val best = streams.firstOrNull() ?: throw IllegalStateException("No playable sources found")
            val key = if (item.type == "series") "${item.cloudId}:s${season}e${episode}" else item.cloudId
            val saved = progress[key] ?: progress[item.cloudId]
            playerRequest = PlayerRequest(item, best, season, episode, saved?.position ?: 0L)
            screen = Screen.PLAYER
        }.onFailure { error = it.message }
            .also { busyMessage = null }
    }

    fun updateProgress(req: PlayerRequest, position: Long, duration: Long, ended: Boolean = false) {
        if (duration <= 0) return
        val key = if (req.item.type == "series") "${req.item.cloudId}:s${req.season}e${req.episode}" else req.item.cloudId
        val row = PlaybackProgress(
            percent = if (ended) 100.0 else position.toDouble() / duration * 100.0,
            position = position,
            duration = duration,
            updatedAt = System.currentTimeMillis(),
            season = req.season,
            episode = req.episode
        )
        progress[key] = row
        progress[req.item.cloudId] = row
        if (ended) continueWatching.removeAll { it.cloudId == req.item.cloudId }
    }

    fun signOut() {
        session = null
        local.saveSession(null)
        favorites.clear(); progress.clear(); profiles.clear(); continueWatching.clear()
        extrasLoaded = false
        screen = Screen.SETTINGS
    }

    private fun <T> MutableList<T>.replaceWith(rows: Collection<T>) {
        clear(); addAll(rows)
    }
}

enum class Screen {
    SPLASH, HOME, SEARCH, MOVIES, SERIES, COLLECTIONS, MY_LIST, SETTINGS, PROFILES, DETAILS, PLAYER
}

data class PlayerRequest(
    val item: MediaSummary,
    val stream: StreamChoice,
    val season: Int,
    val episode: Int,
    val resumeMs: Long
)
