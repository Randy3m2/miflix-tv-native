package com.miflix.native2.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.miflix.native2.BuildConfig
import com.miflix.native2.data.LocalStore
import com.miflix.native2.data.PairingServer
import com.miflix.native2.data.StreamRepository
import com.miflix.native2.data.SupabaseRepository
import com.miflix.native2.data.TmdbRepository
import com.miflix.native2.data.UpdateRepository
import com.miflix.native2.model.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import java.net.URI
import java.util.Locale
import java.util.UUID

class AppState(context: Context) {
    private val local = LocalStore(context)
    private val cloud = SupabaseRepository(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_KEY)
    private val updates = UpdateRepository()
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var pairingServer: PairingServer? = null

    val tmdb = TmdbRepository(BuildConfig.MIFLIX_TMDB_TOKEN)
    val streamRepo = StreamRepository(BuildConfig.MIFLIX_TORRENTIO_MANIFEST)

    var screen by mutableStateOf(Screen.SPLASH)
    var session by mutableStateOf(local.loadSession())
    var activeProfile by mutableStateOf(Profile("default", "Main", primary = true))
    val profiles = mutableStateListOf<Profile>()
    val favorites = mutableStateListOf<String>()
    val progress = mutableStateMapOf<String, PlaybackProgress>()
    val addons = mutableStateListOf<AddonConfig>()

    val trending = mutableStateListOf<MediaSummary>()
    val movies = mutableStateListOf<MediaSummary>()
    val series = mutableStateListOf<MediaSummary>()
    val topRated = mutableStateListOf<MediaSummary>()
    val continueWatching = mutableStateListOf<MediaSummary>()
    var extrasLoaded by mutableStateOf(false)
    var extrasLoading by mutableStateOf(false)

    var selectedProvider by mutableStateOf<ProviderConfig?>(null)
    val providerTop = mutableStateListOf<MediaSummary>()
    val providerMovies = mutableStateListOf<MediaSummary>()
    val providerSeries = mutableStateListOf<MediaSummary>()
    val providerLatest = mutableStateListOf<MediaSummary>()
    val providerTopRated = mutableStateListOf<MediaSummary>()
    val providerGenreRows = mutableStateMapOf<String, List<MediaSummary>>()
    var providerLoading by mutableStateOf(false)
    var selectedProviderGenre by mutableStateOf<GenreConfig?>(null)
    val providerGenrePopular = mutableStateListOf<MediaSummary>()
    val providerGenreLatest = mutableStateListOf<MediaSummary>()
    val providerGenreTopRated = mutableStateListOf<MediaSummary>()

    var collectionTitle by mutableStateOf("")
    val collectionItems = mutableStateListOf<MediaSummary>()

    var selected by mutableStateOf<MediaSummary?>(null)
    var details by mutableStateOf<MediaDetails?>(null)
    val episodes = mutableStateListOf<EpisodeSummary>()
    var currentSeason by mutableStateOf(1)
    var playerRequest by mutableStateOf<PlayerRequest?>(null)
    var busyMessage by mutableStateOf<String?>(null)
    var error by mutableStateOf<String?>(null)

    var updateInfo by mutableStateOf<UpdateInfo?>(null)
    var updateChecking by mutableStateOf(false)

    var pairingInfo by mutableStateOf<PairingInfo?>(null)
    var pairingStatus by mutableStateOf("")

    var watchParty by mutableStateOf<WatchPartySession?>(null)
    var watchPartyRoom by mutableStateOf<WatchPartyRoom?>(null)
    var partyBusy by mutableStateOf(false)

    fun launchTask(block: suspend () -> Unit) {
        appScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                error = t.message ?: t::class.java.simpleName
            }
        }
    }

    fun pushCloudAsync() = launchTask { pushCloud() }

    suspend fun bootstrap(initialPartyCode: String? = null) {
        session?.let { s ->
            if (s.refreshToken.isNotBlank()) {
                runCatching { cloud.refresh(s.refreshToken) }.onSuccess {
                    session = it
                    local.saveSession(it)
                }
            }
            runCatching { syncFromCloud() }
        }
        if (tmdb.token.isNotBlank()) runCatching { loadHome() }.onFailure { error = it.message }
        screen = if (tmdb.token.isNotBlank()) Screen.HOME else Screen.SETTINGS
        if (!initialPartyCode.isNullOrBlank() && session != null) joinWatchParty(initialPartyCode)
    }

    suspend fun login(email: String, password: String) {
        busyMessage = "Signing in…"
        error = null
        try {
            val logged = cloud.login(email, password)
            session = logged
            local.saveSession(logged)
            syncFromCloud()
            loadHome()
            screen = Screen.HOME
        } finally {
            busyMessage = null
        }
    }

    suspend fun syncFromCloud() {
        val s = session ?: return
        val (remoteProfiles, remoteSetup) = cloud.account(s)

        val chosenTmdb = remoteSetup.tmdbToken.ifBlank { tmdb.token }
        if (chosenTmdb.isNotBlank()) tmdb.token = chosenTmdb

        val bootstrapAddons = (remoteSetup.allAddons() + streamRepo.manifestUrls() + listOf(BuildConfig.MIFLIX_TORRENTIO_MANIFEST))
            .map { it.trim() }.filter { it.isNotBlank() }.distinct()
        streamRepo.setManifests(bootstrapAddons)
        addons.replaceWith(bootstrapAddons.map { AddonConfig(addonLabel(it), it) })

        if (remoteSetup.tmdbToken.isBlank() || remoteSetup.allAddons().isEmpty()) {
            runCatching { cloud.upsertPrivateSetup(s, PrivateSetup(chosenTmdb, bootstrapAddons.firstOrNull().orEmpty(), bootstrapAddons)) }
        }

        profiles.clear()
        profiles.addAll(if (remoteProfiles.isEmpty()) listOf(Profile("default", "Main", primary = true)) else remoteProfiles)
        if (remoteProfiles.isEmpty()) runCatching { cloud.upsertProfiles(s, profiles.toList()) }

        val wanted = local.profileId()
        activeProfile = profiles.firstOrNull { it.id == wanted } ?: profiles.firstOrNull { it.primary } ?: profiles.first()
        local.saveProfileId(activeProfile.id)

        val (fav, prog) = cloud.profileState(s, activeProfile.id)
        favorites.clear(); favorites.addAll(fav)
        progress.clear(); progress.putAll(prog)
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

    suspend fun createProfile(name: String) {
        val s = session ?: throw IllegalStateException("Sign in before creating profiles")
        val clean = name.trim().take(24)
        if (clean.isBlank()) throw IllegalStateException("Profile name is required")
        if (profiles.size >= 6) throw IllegalStateException("MiFlix supports up to 6 profiles")
        val p = Profile(UUID.randomUUID().toString(), clean, primary = false)
        profiles.add(p)
        cloud.upsertProfiles(s, profiles.toList())
        cloud.upsertProfile(s, p.id, emptySet(), emptyMap())
    }

    suspend fun pushCloud() {
        session?.let { cloud.upsertProfile(it, activeProfile.id, favorites.toSet(), progress.toMap()) }
    }

    suspend fun savePrivateSetup() {
        val s = session ?: return
        val urls = streamRepo.manifestUrls()
        cloud.upsertPrivateSetup(s, PrivateSetup(tmdb.token, urls.firstOrNull().orEmpty(), urls))
    }

    suspend fun addAddonManifest(manifest: String) {
        pairingStatus = "Checking add-on…"
        val addon = streamRepo.inspectManifest(manifest)
        if (streamRepo.manifestUrls().none { it.equals(addon.manifestUrl, true) }) streamRepo.addManifest(addon.manifestUrl)
        addons.replaceWith(streamRepo.manifestUrls().map { url ->
            if (url.equals(addon.manifestUrl, true)) addon else addons.firstOrNull { it.manifestUrl == url } ?: AddonConfig(addonLabel(url), url)
        })
        savePrivateSetup()
        pairingStatus = "${addon.name} added ✓"
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
            continueWatching.replaceWith(runCatching { resolveContinueWatching() }.getOrDefault(emptyList()))
            extrasLoaded = true
        } finally {
            extrasLoading = false
        }
    }

    suspend fun openProvider(provider: ProviderConfig) {
        selectedProvider = provider
        providerTop.clear(); providerMovies.clear(); providerSeries.clear(); providerLatest.clear(); providerTopRated.clear(); providerGenreRows.clear()
        providerLoading = true
        screen = Screen.PROVIDER
        try {
            supervisorScope {
                val top = async { runCatching { tmdb.providerTop(provider.providerIds) } }
                val mov = async { runCatching { tmdb.discoverProviderMedia(provider.providerIds, "movie", limit = 20) } }
                val tv = async { runCatching { tmdb.discoverProviderMedia(provider.providerIds, "series", limit = 20) } }
                val latest = async { runCatching { tmdb.providerLatest(provider.providerIds) } }
                val rated = async { runCatching { tmdb.providerTopRated(provider.providerIds) } }
                top.await().onSuccess { providerTop.replaceWith(it) }
                mov.await().onSuccess { providerMovies.replaceWith(it) }
                tv.await().onSuccess { providerSeries.replaceWith(it) }
                latest.await().onSuccess { providerLatest.replaceWith(it) }
                rated.await().onSuccess { providerTopRated.replaceWith(it) }
            }
        } finally { providerLoading = false }
    }


    suspend fun openProviderGenre(genre: GenreConfig) {
        val provider = selectedProvider ?: return
        selectedProviderGenre = genre
        providerGenrePopular.clear(); providerGenreLatest.clear(); providerGenreTopRated.clear()
        providerLoading = true
        screen = Screen.PROVIDER_GENRE
        try {
            supervisorScope {
                val popular = async { runCatching { tmdb.providerGenreMixed(provider.providerIds, genre, limit = 20) } }
                val latest = async { runCatching { tmdb.providerGenreLatest(provider.providerIds, genre, limit = 20) } }
                val rated = async { runCatching { tmdb.providerGenreTopRated(provider.providerIds, genre, limit = 20) } }
                popular.await().onSuccess { providerGenrePopular.replaceWith(it) }
                latest.await().onSuccess { providerGenreLatest.replaceWith(it) }
                rated.await().onSuccess { providerGenreTopRated.replaceWith(it) }
            }
        } finally { providerLoading = false }
    }
    suspend fun loadProviderGenre(genre: GenreConfig) {
        val provider = selectedProvider ?: return
        if (providerGenreRows.containsKey(genre.id)) return
        val rows = runCatching { tmdb.providerGenreMixed(provider.providerIds, genre, limit = 20) }.getOrDefault(emptyList())
        providerGenreRows[genre.id] = rows
    }

    suspend fun openGenre(genre: GenreConfig) {
        busyMessage = "Loading ${genre.title}…"
        collectionTitle = genre.title
        screen = Screen.COLLECTION_DETAIL
        try { collectionItems.replaceWith(tmdb.discoverGenreMixed(genre, 50)) }
        finally { busyMessage = null }
    }

    suspend fun openYear(year: Int) {
        busyMessage = "Loading movies from $year…"
        collectionTitle = "Movies · $year"
        screen = Screen.COLLECTION_DETAIL
        try { collectionItems.replaceWith(tmdb.discoverMoviesByYear(year, 50)) }
        finally { busyMessage = null }
    }

    private suspend fun resolveContinueWatching(): List<MediaSummary> = coroutineScope {
        val exactCloudId = Regex("^tmdb:(movie|series):\\d+$")
        val ids = progress.entries.filter { (id, p) -> exactCloudId.matches(id) && p.percent in 1.0..95.0 }
            .sortedByDescending { it.value.updatedAt }.map { it.key }.distinct().take(12)
        ids.map { id -> async { runCatching { tmdb.byCloudId(id) }.getOrNull() } }.awaitAll().filterNotNull()
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
        runCatching { tmdb.season(item.id, season) }.onSuccess { episodes.replaceWith(it) }.onFailure { error = it.message }
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
        playResolved(item, imdb, p?.season?.takeIf { it > 0 } ?: 1, p?.episode?.takeIf { it > 0 } ?: 1)
    }

    suspend fun playSeriesFromStart() {
        val item = selected ?: return
        val d = details ?: tmdb.details(item).also { details = it }
        val imdb = d.imdbId ?: throw IllegalStateException("IMDb ID unavailable")
        playResolved(item, imdb, 1, 1)
    }

    private suspend fun playResolved(item: MediaSummary, imdb: String, season: Int, episode: Int, resumeOverride: Long? = null) {
        busyMessage = "Finding the best source…"
        error = null
        try {
            val streams = streamRepo.resolve(imdb, item.type, season, episode)
            val best = streams.firstOrNull() ?: throw IllegalStateException("No playable sources found")
            val key = if (item.type == "series") "${item.cloudId}:s${season}e${episode}" else item.cloudId
            val saved = progress[key] ?: progress[item.cloudId]
            playerRequest = PlayerRequest(item, best, season, episode, resumeOverride ?: saved?.position ?: 0L)
            screen = Screen.PLAYER
            val party = watchParty
            val s = session
            if (party?.isHost == true && s != null) {
                val room = WatchPartyRoom(party.roomCode, s.userId, item.cloudId, item.type, season, episode, resumeOverride ?: saved?.position ?: 0L, true, System.currentTimeMillis())
                watchPartyRoom = room
                runCatching { cloud.updateParty(s, room) }
            }
        } finally { busyMessage = null }
    }

    fun updateProgress(req: PlayerRequest, position: Long, duration: Long, ended: Boolean = false) {
        if (duration <= 0) return
        val key = if (req.item.type == "series") "${req.item.cloudId}:s${req.season}e${req.episode}" else req.item.cloudId
        val row = PlaybackProgress(
            percent = if (ended) 100.0 else position.toDouble() / duration * 100.0,
            position = position, duration = duration, updatedAt = System.currentTimeMillis(), season = req.season, episode = req.episode
        )
        progress[key] = row
        progress[req.item.cloudId] = row
        if (ended) continueWatching.removeAll { it.cloudId == req.item.cloudId }
    }

    suspend fun createWatchParty() {
        val s = session ?: throw IllegalStateException("Sign in before creating a Watch Party")
        val item = selected ?: throw IllegalStateException("Open a movie or series first")
        partyBusy = true
        try {
            var code: String
            do { code = randomRoomCode() } while (runCatching { cloud.readParty(s, code) }.getOrNull() != null)
            val season = if (item.type == "series") currentSeason.coerceAtLeast(1) else 0
            val episode = if (item.type == "series") 1 else 0
            val room = WatchPartyRoom(code, s.userId, item.cloudId, item.type, season, episode, 0L, false, System.currentTimeMillis())
            cloud.createParty(s, room)
            watchParty = WatchPartySession(code, true)
            watchPartyRoom = room
            screen = Screen.WATCH_PARTY
        } finally { partyBusy = false }
    }

    suspend fun joinWatchParty(codeInput: String) {
        val s = session ?: throw IllegalStateException("Sign in before joining a Watch Party")
        val code = codeInput.trim().uppercase(Locale.US)
        if (code.length < 4) throw IllegalStateException("Enter a valid Watch Party code")
        partyBusy = true
        try {
            val room = cloud.readParty(s, code) ?: throw IllegalStateException("Watch Party not found or expired")
            watchParty = WatchPartySession(room.roomCode, room.hostUserId == s.userId)
            watchPartyRoom = room
            if (room.hostUserId == s.userId) {
                screen = Screen.WATCH_PARTY
            } else {
                playPartyRoom(room)
            }
        } finally { partyBusy = false }
    }

    suspend fun startHostPartyPlayback() {
        val room = watchPartyRoom ?: throw IllegalStateException("Create a Watch Party first")
        playPartyRoom(room.copy(isPlaying = true))
    }

    private suspend fun playPartyRoom(room: WatchPartyRoom) {
        val item = tmdb.byCloudId(room.cloudId) ?: throw IllegalStateException("Party title is unavailable")
        selected = item
        val d = tmdb.details(item)
        details = d
        val imdb = d.imdbId ?: throw IllegalStateException("IMDb ID unavailable for party title")
        if (item.type == "series") {
            currentSeason = room.season.coerceAtLeast(1)
            runCatching { loadSeason(currentSeason) }
        }
        playResolved(item, imdb, room.season, room.episode, room.positionMs)
    }

    suspend fun hostPartyTick(req: PlayerRequest, positionMs: Long, isPlaying: Boolean) {
        val p = watchParty?.takeIf { it.isHost } ?: return
        val s = session ?: return
        val room = WatchPartyRoom(p.roomCode, s.userId, req.item.cloudId, req.item.type, req.season, req.episode, positionMs, isPlaying, System.currentTimeMillis())
        watchPartyRoom = room
        cloud.updateParty(s, room)
    }

    suspend fun guestPartySnapshot(): WatchPartyRoom? {
        val p = watchParty?.takeIf { !it.isHost } ?: return null
        val s = session ?: return null
        val room = cloud.readParty(s, p.roomCode)
        if (room != null) watchPartyRoom = room
        return room
    }

    suspend fun switchGuestToRemote(room: WatchPartyRoom) {
        val req = playerRequest
        val same = req?.item?.cloudId == room.cloudId && req.season == room.season && req.episode == room.episode
        if (!same) playPartyRoom(room)
    }

    fun leaveWatchParty() {
        val p = watchParty
        val s = session
        watchParty = null
        watchPartyRoom = null
        if (p?.isHost == true && s != null) launchTask { runCatching { cloud.deleteParty(s, p.roomCode) } }
    }

    fun startPairing() {
        pairingServer?.stop()
        pairingStatus = ""
        val server = PairingServer(
            onLogin = { email, password -> launchTask { login(email, password) } },
            onAddon = { manifest -> launchTask { addAddonManifest(manifest) } }
        )
        pairingServer = server
        pairingInfo = server.start()
    }

    fun stopPairing() {
        pairingServer?.stop(); pairingServer = null; pairingInfo = null
    }

    suspend fun checkForUpdates() {
        updateChecking = true
        error = null
        runCatching { updates.check(BuildConfig.VERSION_NAME) }.onSuccess { updateInfo = it }.onFailure { error = "Update check failed: ${it.message}" }
        updateChecking = false
    }

    fun signOut() {
        leaveWatchParty()
        session = null
        local.saveSession(null)
        favorites.clear(); progress.clear(); profiles.clear(); continueWatching.clear(); addons.clear()
        extrasLoaded = false
        screen = Screen.SETTINGS
    }

    private fun addonLabel(url: String): String = runCatching {
        val host = URI(url).host.orEmpty().lowercase()
        when {
            "torrentio" in host -> "Torrentio"
            "comet" in host -> "Comet"
            host.isNotBlank() -> host.substringBefore('.')
            else -> "Stremio add-on"
        }
    }.getOrDefault("Stremio add-on")

    private fun randomRoomCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..6).map { chars.random() }.joinToString("")
    }

    private fun <T> MutableList<T>.replaceWith(rows: Collection<T>) { clear(); addAll(rows) }
}

enum class Screen {
    SPLASH, HOME, SEARCH, MOVIES, SERIES, COLLECTIONS, COLLECTION_DETAIL, PROVIDER, PROVIDER_GENRE, MY_LIST, SETTINGS, PROFILES,
    DETAILS, PLAYER, PAIRING, WATCH_PARTY
}

data class PlayerRequest(
    val item: MediaSummary,
    val stream: StreamChoice,
    val season: Int,
    val episode: Int,
    val resumeMs: Long
)
