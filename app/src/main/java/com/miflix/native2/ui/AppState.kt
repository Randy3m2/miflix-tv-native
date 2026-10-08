package com.miflix.native2.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.miflix.native2.BuildConfig
import com.miflix.native2.data.LocalStore
import com.miflix.native2.data.PairingRepository
import com.miflix.native2.data.StreamRepository
import com.miflix.native2.data.SubtitleRepository
import com.miflix.native2.data.SupabaseRepository
import com.miflix.native2.data.TmdbRepository
import com.miflix.native2.data.UpdateRepository
import com.miflix.native2.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import java.util.Locale
import java.util.UUID

class AppState(context: Context) {
    private val local = LocalStore(context)
    private val cloud = SupabaseRepository(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_KEY)
    private val pairingRepo = PairingRepository(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_KEY)
    private val updates = UpdateRepository()
    private val subtitleRepo = SubtitleRepository()
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

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

    val continueWatching = mutableStateListOf<MediaSummary>()
    val action = mutableStateListOf<MediaSummary>()
    val sciFi = mutableStateListOf<MediaSummary>()
    val netflix = mutableStateListOf<MediaSummary>()
    val disney = mutableStateListOf<MediaSummary>()
    val prime = mutableStateListOf<MediaSummary>()
    val apple = mutableStateListOf<MediaSummary>()
    val hbo = mutableStateListOf<MediaSummary>()
    var extrasLoaded by mutableStateOf(false)
    var extrasLoading by mutableStateOf(false)

    var collectionTitle by mutableStateOf("")
    val collectionItems = mutableStateListOf<MediaSummary>()

    var platformId by mutableStateOf("")
    var platformTitle by mutableStateOf("")
    val platformSections = mutableStateListOf<CatalogSection>()

    var genreTitle by mutableStateOf("")
    val genreItems = mutableStateListOf<MediaSummary>()

    var yearTitle by mutableStateOf("")
    val yearItems = mutableStateListOf<MediaSummary>()

    var selected by mutableStateOf<MediaSummary?>(null)
    var details by mutableStateOf<MediaDetails?>(null)
    val episodes = mutableStateListOf<EpisodeSummary>()
    var currentSeason by mutableStateOf(1)
    var sourceSelection by mutableStateOf<SourceSelection?>(null)
    var playerRequest by mutableStateOf<PlayerRequest?>(null)
    var busyMessage by mutableStateOf<String?>(null)
    var error by mutableStateOf<String?>(null)

    var updateInfo by mutableStateOf<UpdateInfo?>(null)
    var updateChecking by mutableStateOf(false)

    var pairingRequest by mutableStateOf<PairingRequest?>(null)
    var pairingStatus by mutableStateOf("")

    var watchParty by mutableStateOf<WatchParty?>(null)
    var partyRole by mutableStateOf<PartyRole?>(null)
    var partyStatus by mutableStateOf("")

    val genres = listOf(
        GenreDefinition("action", "Action", 28, 10759, "https://raw.githubusercontent.com/rrevanth/nuvio-assets/main/genres/action/action-landscape.png"),
        GenreDefinition("adventure", "Adventure", 12, 10759),
        GenreDefinition("animation", "Animation", 16, 16),
        GenreDefinition("comedy", "Comedy", 35, 35),
        GenreDefinition("crime", "Crime", 80, 80),
        GenreDefinition("documentary", "Documentary", 99, 99),
        GenreDefinition("drama", "Drama", 18, 18),
        GenreDefinition("family", "Family", 10751, 10751),
        GenreDefinition("fantasy", "Fantasy", 14, 10765),
        GenreDefinition("history", "History", 36, 36),
        GenreDefinition("horror", "Horror", 27, 9648),
        GenreDefinition("mystery", "Mystery", 9648, 9648),
        GenreDefinition("romance", "Romance", 10749, 10749),
        GenreDefinition("scifi", "Science Fiction", 878, 10765, "https://raw.githubusercontent.com/rrevanth/nuvio-assets/main/genres/sci-fi/sci-fi-landscape.png"),
        GenreDefinition("thriller", "Thriller", 53, 10768),
        GenreDefinition("war", "War", 10752, 10768)
    )

    private val providers = mapOf(
        "netflix" to listOf(8, 175),
        "disney" to listOf(337),
        "prime" to listOf(9, 119),
        "apple" to listOf(350),
        "hbo" to listOf(1899, 384)
    )

    fun launch(block: suspend CoroutineScope.() -> Unit) = appScope.launch(block = block)

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

        val chosenTmdb = remoteSetup.tmdbToken.ifBlank { tmdb.token }
        val localManifests = streamRepo.manifests()
        val chosenManifests = (remoteSetup.addonManifests + remoteSetup.torrentioManifest + localManifests)
            .filter { it.isNotBlank() }.distinct()
        if (chosenTmdb.isNotBlank()) tmdb.token = chosenTmdb
        if (chosenManifests.isNotEmpty()) streamRepo.setManifests(chosenManifests)
        if (remoteSetup.tmdbToken.isBlank() || remoteSetup.addonManifests.isEmpty()) {
            runCatching {
                cloud.upsertPrivateSetup(
                    s,
                    PrivateSetup(chosenTmdb, chosenManifests.firstOrNull().orEmpty(), chosenManifests)
                )
            }
        }

        profiles.clear()
        profiles.addAll(if (remoteProfiles.isEmpty()) listOf(Profile("default", "Main", primary = true)) else remoteProfiles)
        if (remoteProfiles.isEmpty()) runCatching { cloud.upsertProfiles(s, profiles.toList()) }
        val wanted = local.profileId()
        activeProfile = profiles.firstOrNull { it.id == wanted }
            ?: profiles.firstOrNull { it.primary }
            ?: profiles.first()
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
        val clean = name.trim().take(24)
        if (clean.isBlank()) return
        val p = Profile(UUID.randomUUID().toString(), clean, primary = false)
        profiles.add(p)
        session?.let {
            cloud.upsertProfiles(it, profiles.toList())
            cloud.upsertProfile(it, p.id, emptySet(), emptyMap())
        }
    }

    suspend fun pushCloud() {
        session?.let { cloud.upsertProfile(it, activeProfile.id, favorites.toSet(), progress.toMap()) }
    }

    suspend fun addAddonManifest(url: String) {
        val clean = url.trim()
        if (clean.isBlank()) return
        streamRepo.addManifest(clean)
        session?.let {
            cloud.upsertPrivateSetup(
                it,
                PrivateSetup(tmdb.token, streamRepo.manifestUrl, streamRepo.manifests())
            )
        }
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
                val actionJob = async { runCatching { tmdb.discoverMovieGenre(28, pages = 1, limit = 20) } }
                val sciFiJob = async { runCatching { tmdb.discoverMovieGenre(878, pages = 1, limit = 20) } }
                val netflixJob = async { runCatching { tmdb.discoverProviderType(providers.getValue("netflix"), "movie") } }
                val disneyJob = async { runCatching { tmdb.discoverProviderType(providers.getValue("disney"), "movie") } }
                val primeJob = async { runCatching { tmdb.discoverProviderType(providers.getValue("prime"), "movie") } }
                val appleJob = async { runCatching { tmdb.discoverProviderType(providers.getValue("apple"), "movie") } }
                val hboJob = async { runCatching { tmdb.discoverProviderType(providers.getValue("hbo"), "movie") } }
                val continueJob = async { runCatching { resolveContinueWatching() } }

                actionJob.await().onSuccess { action.replaceWith(it) }
                sciFiJob.await().onSuccess { sciFi.replaceWith(it) }
                netflixJob.await().onSuccess { netflix.replaceWith(it) }
                disneyJob.await().onSuccess { disney.replaceWith(it) }
                primeJob.await().onSuccess { prime.replaceWith(it) }
                appleJob.await().onSuccess { apple.replaceWith(it) }
                hboJob.await().onSuccess { hbo.replaceWith(it) }
                continueJob.await().onSuccess { continueWatching.replaceWith(it) }
            }
            extrasLoaded = true
        } finally {
            extrasLoading = false
        }
    }

    suspend fun openCollection(id: String, title: String) {
        if (providers.containsKey(id)) {
            openPlatform(id, title)
            return
        }
        if (id.startsWith("year:")) {
            openYear(id.substringAfter(':').toIntOrNull() ?: return)
            return
        }
        genres.firstOrNull { it.id == id }?.let {
            openGenre(it)
            return
        }

        loadHomeExtras()
        collectionTitle = title
        val source = when (id) {
            "action" -> action
            "scifi" -> sciFi
            else -> emptyList()
        }
        collectionItems.clear(); collectionItems.addAll(source)
        screen = Screen.COLLECTION_DETAIL
    }

    suspend fun openPlatform(id: String, title: String) = supervisorScope {
        val providerIds = providers[id] ?: return@supervisorScope
        busyMessage = "Loading $title…"
        platformId = id
        platformTitle = title
        platformSections.clear()
        error = null
        try {
            val top = async { tmdb.providerTop10(providerIds) }
            val moviesJob = async { tmdb.discoverProviderType(providerIds, "movie", pages = 2, limit = 40) }
            val seriesJob = async { tmdb.discoverProviderType(providerIds, "series", pages = 2, limit = 40) }
            platformSections += CatalogSection("top10", "Top 10 on $title", top.await(), landscape = true)
            platformSections += CatalogSection("movies", "Movies", moviesJob.await())
            platformSections += CatalogSection("series", "Series", seriesJob.await())
            screen = Screen.PLATFORM_DETAIL
            busyMessage = null

            val focusGenres = genres.filter { it.id in setOf("action", "comedy", "horror", "drama", "scifi", "thriller", "animation", "romance") }
            focusGenres.forEach { g ->
                appScope.launch {
                    runCatching {
                        coroutineScope {
                            val latestMovies = async { tmdb.discoverProviderType(providerIds, "movie", sortBy = "primary_release_date.desc", genreId = g.movieGenreId, limit = 20) }
                            val latestTv = async { tmdb.discoverProviderType(providerIds, "series", sortBy = "first_air_date.desc", genreId = g.tvGenreId, limit = 20) }
                            val bestMovies = async { tmdb.discoverProviderType(providerIds, "movie", sortBy = "vote_average.desc", genreId = g.movieGenreId, limit = 20) }
                            val bestTv = async { tmdb.discoverProviderType(providerIds, "series", sortBy = "vote_average.desc", genreId = g.tvGenreId, limit = 20) }
                            listOf(
                                CatalogSection("${g.id}-latest", "${g.title} · Latest", (latestMovies.await() + latestTv.await()).distinctBy { it.cloudId }.sortedByDescending { it.year }.take(20)),
                                CatalogSection("${g.id}-rated", "${g.title} · Best Rated", (bestMovies.await() + bestTv.await()).distinctBy { it.cloudId }.sortedByDescending { it.rating }.take(20))
                            )
                        }
                    }.onSuccess { sections ->
                        platformSections.addAll(sections.filter { it.items.isNotEmpty() })
                    }
                }
            }
        } catch (e: Exception) {
            error = e.message
            busyMessage = null
        }
    }

    suspend fun openGenre(g: GenreDefinition) {
        busyMessage = "Loading ${g.title}…"
        error = null
        runCatching { tmdb.discoverGenreMixed(g.movieGenreId, g.tvGenreId, 50) }
            .onSuccess {
                genreTitle = g.title
                genreItems.replaceWith(it)
                screen = Screen.GENRE_DETAIL
            }
            .onFailure { error = it.message }
        busyMessage = null
    }

    suspend fun openYear(year: Int) {
        busyMessage = "Loading $year movies…"
        error = null
        runCatching { tmdb.moviesByYear(year, 50) }
            .onSuccess {
                yearTitle = "Movies · $year"
                yearItems.replaceWith(it)
                screen = Screen.YEAR_DETAIL
            }
            .onFailure { error = it.message }
        busyMessage = null
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
            episodes.replaceWith(it)
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

    suspend fun switchPlayerEpisode(ep: EpisodeSummary) {
        val current = playerRequest
        if (current != null) updateProgress(current, current.resumeMs, 0)
        playEpisode(ep)
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

    suspend fun playSeriesFromStart() {
        val item = selected ?: return
        val d = details ?: tmdb.details(item).also { details = it }
        val imdb = d.imdbId ?: throw IllegalStateException("IMDb ID unavailable")
        playResolved(item, imdb, 1, 1)
    }

    suspend fun chooseEpisodeSources(ep: EpisodeSummary) {
        val item = selected ?: return
        val imdb = details?.imdbId ?: return
        chooseSources(item, imdb, ep.season, ep.episode)
    }

    suspend fun chooseCurrentSources() {
        val item = selected ?: return
        val imdb = details?.imdbId ?: return
        val saved = progress[item.cloudId]
        val resume = (saved?.season ?: 0) > 0 && (saved?.episode ?: 0) > 0 && (saved?.percent ?: 0.0) < 96.0
        chooseSources(item, imdb, if (item.type == "series") (if (resume) saved!!.season else 1) else 0,
            if (item.type == "series") (if (resume) saved!!.episode else 1) else 0)
    }

    private suspend fun chooseSources(item: MediaSummary, imdb: String, season: Int, episode: Int) {
        busyMessage = "Finding all playback links…"
        error = null
        try {
            val links = streamRepo.resolve(imdb, item.type, season, episode)
            if (links.isEmpty()) error = "No playable sources found"
            else sourceSelection = SourceSelection(item, imdb, season, episode, links)
        } catch (e: Exception) {
            error = e.message
        } finally { busyMessage = null }
    }

    suspend fun playSelectedSource(selection: SourceSelection, stream: StreamChoice) {
        sourceSelection = null
        playResolved(selection.item, selection.imdb, selection.season, selection.episode, selectedStream = stream)
    }

    private suspend fun playResolved(item: MediaSummary, imdb: String, season: Int, episode: Int, resumeOverride: Long? = null, selectedStream: StreamChoice? = null) = coroutineScope {
        busyMessage = "Finding the best source…"
        error = null
        val streamsJob = async { if (selectedStream != null) listOf(selectedStream) else streamRepo.resolve(imdb, item.type, season, episode) }
        val subsJob = async { runCatching { subtitleRepo.resolve(imdb, item.type, season, episode, null) }.getOrDefault(emptyList()) }
        runCatching {
            val streams = streamsJob.await()
            val best = streams.firstOrNull() ?: throw IllegalStateException("No playable sources found")
            var externalSubs = subsJob.await()
            if (externalSubs.isEmpty()) {
                externalSubs = runCatching { subtitleRepo.resolve(imdb, item.type, season, episode, best) }.getOrDefault(emptyList())
            }
            val mergedSubs = (best.subtitles + externalSubs)
                .distinctBy { it.url }
                .sortedWith(compareBy<SubtitleChoice> {
                    when (it.lang.lowercase(Locale.US)) { "es", "spa" -> 0; "en", "eng" -> 1; else -> 2 }
                }.thenBy { it.label })
            val enriched = best.copy(subtitles = mergedSubs)
            val key = if (item.type == "series") "${item.cloudId}:s${season}e${episode}" else item.cloudId
            val saved = progress[key] ?: progress[item.cloudId]
            playerRequest = PlayerRequest(item, enriched, season, episode, resumeOverride ?: saved?.position ?: 0L)
            screen = Screen.PLAYER
        }.onFailure { error = it.message }
        busyMessage = null
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

    suspend fun createWatchParty(): WatchParty? {
        val s = session ?: run { error = "Sign in before creating a Watch Party"; return null }
        val item = selected ?: playerRequest?.item ?: run { error = "Open a movie or series first"; return null }
        val req = playerRequest
        val saved = progress[item.cloudId]
        val room = (100000..999999).random().toString()
        val party = WatchParty(
            roomCode = room,
            hostUserId = s.userId,
            cloudId = item.cloudId,
            season = req?.season ?: saved?.season ?: if (item.type == "series") 1 else 0,
            episode = req?.episode ?: saved?.episode ?: if (item.type == "series") 1 else 0,
            positionMs = req?.resumeMs ?: saved?.position ?: 0L,
            playing = false,
            updatedAt = System.currentTimeMillis()
        )
        return runCatching { cloud.createWatchParty(s, party); watchParty = party; partyRole = PartyRole.HOST; partyStatus = "Room ready"; party }
            .onFailure { error = it.message }.getOrNull()
    }

    suspend fun joinWatchParty(code: String) {
        val s = session ?: run { error = "Sign in before joining a Watch Party"; return }
        val party = cloud.readWatchParty(s, code.trim()) ?: run { error = "Watch Party room not found"; return }
        val item = tmdb.byCloudId(party.cloudId) ?: run { error = "This title is not available"; return }
        watchParty = party
        partyRole = PartyRole.GUEST
        partyStatus = "Joined ${party.roomCode}"
        selected = item
        details = tmdb.details(item)
        if (item.type == "series") loadSeason(party.season.coerceAtLeast(1))
        val imdb = details?.imdbId ?: run { error = "IMDb ID unavailable"; return }
        playResolved(item, imdb, party.season, party.episode, party.positionMs)
    }

    suspend fun hostPartyUpdate(req: PlayerRequest, positionMs: Long, playing: Boolean) {
        val s = session ?: return
        if (partyRole != PartyRole.HOST) return
        val current = watchParty ?: return
        val next = current.copy(
            cloudId = req.item.cloudId,
            season = req.season,
            episode = req.episode,
            positionMs = positionMs,
            playing = playing,
            updatedAt = System.currentTimeMillis()
        )
        watchParty = next
        runCatching { cloud.updateWatchParty(s, next) }
    }

    suspend fun refreshParty(): WatchParty? {
        val s = session ?: return null
        val current = watchParty ?: return null
        return runCatching { cloud.readWatchParty(s, current.roomCode) }.getOrNull()?.also { watchParty = it }
    }

    suspend fun switchToPartyState(remote: WatchParty) {
        val current = playerRequest
        if (current != null && current.item.cloudId == remote.cloudId && current.season == remote.season && current.episode == remote.episode) return
        val item = tmdb.byCloudId(remote.cloudId) ?: return
        selected = item
        details = tmdb.details(item)
        if (item.type == "series") loadSeason(remote.season.coerceAtLeast(1))
        val imdb = details?.imdbId ?: return
        playResolved(item, imdb, remote.season, remote.episode, remote.positionMs)
    }

    suspend fun leaveWatchParty() {
        val s = session
        val p = watchParty
        if (s != null && p != null && partyRole == PartyRole.HOST) runCatching { cloud.deleteWatchParty(s, p.roomCode) }
        watchParty = null
        partyRole = null
        partyStatus = ""
    }

    suspend fun startPairing() {
        pairingStatus = "Creating secure pairing QR…"
        runCatching { pairingRepo.create() }
            .onSuccess { pairingRequest = it; pairingStatus = "Scan with your phone" }
            .onFailure { error = it.message; pairingStatus = "" }
    }

    suspend fun pollPairing(): Boolean {
        val req = pairingRequest ?: return false
        val payload = runCatching { pairingRepo.poll(req) }.getOrNull() ?: return false
        if (payload.accessToken.isNotBlank() && payload.userId.isNotBlank()) {
            val cloudSession = CloudSession(payload.accessToken, payload.refreshToken, payload.userId, payload.email)
            session = cloudSession
            local.saveSession(cloudSession)
            runCatching { syncFromCloud() }
        }
        if (payload.addonManifest.isNotBlank()) runCatching { addAddonManifest(payload.addonManifest) }
        pairingStatus = buildString {
            if (payload.accessToken.isNotBlank()) append("Account connected")
            if (payload.addonManifest.isNotBlank()) {
                if (isNotBlank()) append(" · ")
                append("Add-on added")
            }
        }.ifBlank { "Pairing received" }
        pairingRepo.finish(req)
        pairingRequest = null
        return true
    }

    suspend fun checkForUpdates() {
        updateChecking = true
        error = null
        runCatching { updates.check(BuildConfig.VERSION_NAME) }
            .onSuccess { updateInfo = it }
            .onFailure { error = "Update check failed: ${it.message}" }
        updateChecking = false
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
    SPLASH, HOME, SEARCH, MOVIES, SERIES, COLLECTIONS, COLLECTION_DETAIL, PLATFORM_DETAIL,
    GENRES, GENRE_DETAIL, YEAR_DETAIL, MY_LIST, SETTINGS, PROFILES, WATCH_PARTY, PAIR_DEVICE,
    DETAILS, PLAYER
}

data class PlayerRequest(
    val item: MediaSummary,
    val stream: StreamChoice,
    val season: Int,
    val episode: Int,
    val resumeMs: Long
)

data class SourceSelection(
    val item: MediaSummary,
    val imdb: String,
    val season: Int,
    val episode: Int,
    val streams: List<StreamChoice>
)
