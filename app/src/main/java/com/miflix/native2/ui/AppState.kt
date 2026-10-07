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
import kotlinx.coroutines.coroutineScope

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
    var selected by mutableStateOf<MediaSummary?>(null)
    var details by mutableStateOf<MediaDetails?>(null)
    val episodes = mutableStateListOf<EpisodeSummary>()
    var currentSeason by mutableStateOf(1)
    var playerRequest by mutableStateOf<PlayerRequest?>(null)
    var busyMessage by mutableStateOf<String?>(null)
    var error by mutableStateOf<String?>(null)

    suspend fun bootstrap() {
        session?.let { s ->
            if (s.refreshToken.isNotBlank()) runCatching { cloud.refresh(s.refreshToken) }.onSuccess { session = it; local.saveSession(it) }
            runCatching { syncFromCloud() }
        }
        if (tmdb.token.isNotBlank()) runCatching { loadHome() }.onFailure { error = it.message }
        screen = if (tmdb.token.isNotBlank()) Screen.HOME else Screen.SETTINGS
    }

    suspend fun login(email: String, password: String) {
        busyMessage = "Signing in…"; error = null
        runCatching { cloud.login(email, password) }.onSuccess {
            session = it; local.saveSession(it); syncFromCloud(); loadHome(); screen = Screen.HOME
        }.onFailure { error = it.message }.also { busyMessage = null }
    }

    suspend fun syncFromCloud() {
        val s = session ?: return
        val (remoteProfiles, setup) = cloud.account(s)
        if (setup.tmdbToken.isNotBlank()) tmdb.token = setup.tmdbToken
        if (setup.torrentioManifest.isNotBlank()) streamRepo.manifestUrl = setup.torrentioManifest
        profiles.clear(); profiles.addAll(if (remoteProfiles.isEmpty()) listOf(Profile("default", "Main", primary = true)) else remoteProfiles)
        val wanted = local.profileId()
        activeProfile = profiles.firstOrNull { it.id == wanted } ?: profiles.firstOrNull { it.primary } ?: profiles.first()
        local.saveProfileId(activeProfile.id)
        val (fav, prog) = cloud.profileState(s, activeProfile.id)
        favorites.clear(); favorites.addAll(fav)
        progress.clear(); progress.putAll(prog)
    }

    suspend fun selectProfile(p: Profile) {
        activeProfile = p; local.saveProfileId(p.id)
        session?.let { val (fav, prog) = cloud.profileState(it, p.id); favorites.clear(); favorites.addAll(fav); progress.clear(); progress.putAll(prog) }
        screen = Screen.HOME
    }

    suspend fun pushCloud() { session?.let { cloud.upsertProfile(it, activeProfile.id, favorites.toSet(), progress.toMap()) } }

    suspend fun loadHome() = coroutineScope {
        val a = async { tmdb.trending() }; val b = async { tmdb.popularMovies() }; val c = async { tmdb.popularSeries() }; val d = async { tmdb.topRatedMovies() }
        trending.clear(); trending.addAll(a.await()); movies.clear(); movies.addAll(b.await()); series.clear(); series.addAll(c.await()); topRated.clear(); topRated.addAll(d.await())
    }

    suspend fun open(item: MediaSummary) {
        selected = item; details = null; episodes.clear(); currentSeason = 1; screen = Screen.DETAILS
        runCatching { tmdb.details(item) }.onSuccess {
            details = it
            if (item.type == "series" && it.seasonCount > 0) loadSeason(1)
        }.onFailure { error = it.message }
    }

    suspend fun loadSeason(season: Int) {
        val item = selected ?: return; currentSeason = season
        runCatching { tmdb.season(item.id, season) }.onSuccess { episodes.clear(); episodes.addAll(it) }.onFailure { error = it.message }
    }

    fun toggleFavorite(item: MediaSummary) {
        if (favorites.contains(item.cloudId)) favorites.remove(item.cloudId) else favorites.add(item.cloudId)
    }

    suspend fun playMovie() {
        val item = selected ?: return; val d = details ?: tmdb.details(item).also { details = it }
        val imdb = d.imdbId ?: throw IllegalStateException("IMDb ID unavailable")
        playResolved(item, imdb, 0, 0)
    }

    suspend fun playEpisode(ep: EpisodeSummary) {
        val item = selected ?: return; val d = details ?: tmdb.details(item).also { details = it }
        val imdb = d.imdbId ?: throw IllegalStateException("IMDb ID unavailable")
        playResolved(item, imdb, ep.season, ep.episode)
    }

    private suspend fun playResolved(item: MediaSummary, imdb: String, season: Int, episode: Int) {
        busyMessage = "Finding the best source…"; error = null
        runCatching { streamRepo.resolve(imdb, item.type, season, episode) }.onSuccess { streams ->
            val best = streams.firstOrNull() ?: throw IllegalStateException("No playable sources found")
            val key = if (item.type == "series") "${item.cloudId}:s${season}e${episode}" else item.cloudId
            val saved = progress[key] ?: progress[item.cloudId]
            playerRequest = PlayerRequest(item, best, season, episode, saved?.position ?: 0L)
            screen = Screen.PLAYER
        }.onFailure { error = it.message }.also { busyMessage = null }
    }

    fun updateProgress(req: PlayerRequest, position: Long, duration: Long, ended: Boolean = false) {
        if (duration <= 0) return
        val key = if (req.item.type == "series") "${req.item.cloudId}:s${req.season}e${req.episode}" else req.item.cloudId
        val row = PlaybackProgress(if (ended) 100.0 else position.toDouble() / duration * 100.0, position, duration, System.currentTimeMillis(), req.season, req.episode)
        progress[key] = row; progress[req.item.cloudId] = row
    }

    fun signOut() { session = null; local.saveSession(null); favorites.clear(); progress.clear(); profiles.clear(); screen = Screen.SETTINGS }
}

enum class Screen { SPLASH, HOME, SEARCH, MOVIES, SERIES, MY_LIST, SETTINGS, PROFILES, DETAILS, PLAYER }

data class PlayerRequest(val item: MediaSummary, val stream: StreamChoice, val season: Int, val episode: Int, val resumeMs: Long)
