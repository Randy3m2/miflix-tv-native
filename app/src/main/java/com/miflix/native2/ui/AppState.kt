package com.miflix.native2.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.miflix.native2.BuildConfig
import com.miflix.native2.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.CancellationException
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
    private val socialPrefs = context.getSharedPreferences("miflix_social", Context.MODE_PRIVATE)
    private val playbackPrefs = context.getSharedPreferences("miflix_playback", Context.MODE_PRIVATE)
    var audioLanguage by mutableStateOf(playbackPrefs.getString("audio", "es") ?: "es")
    var subtitleLanguage by mutableStateOf(playbackPrefs.getString("subtitle", "es") ?: "es")
    var autoplayNext by mutableStateOf(playbackPrefs.getBoolean("autoplay", true))
    val notifications = mutableStateListOf<ReleaseNotice>()
    private var checkingNotices = false
    private val introRepo = IntroRepository()
    fun savePlaybackPreferences(audio: String, subtitle: String, autoplay: Boolean) {
        audioLanguage = audio; subtitleLanguage = subtitle; autoplayNext = autoplay
        playbackPrefs.edit().putString("audio",audio).putString("subtitle",subtitle).putBoolean("autoplay",autoplay).apply()
    }
    private fun noticesKey() = "notices_${session?.userId ?: "local"}_${activeProfile.id}"
    private fun knownKey() = "known_${session?.userId ?: "local"}_${activeProfile.id}"
    private fun persistNotices() {
        val a = org.json.JSONArray()
        notifications.forEach { n -> a.put(org.json.JSONObject().put("id",n.id).put("cloud",n.item.cloudId).put("title",n.item.title).put("overview",n.item.overview).put("poster",n.item.poster).put("message",n.message).put("date",n.date)) }
        playbackPrefs.edit().putString(noticesKey(),a.toString()).apply()
    }
    fun clearNotifications() { notifications.clear(); persistNotices() }
    suspend fun checkNotifications() {
        if(checkingNotices || tmdb.token.isBlank()) return
        checkingNotices = true
        val ownerKey = noticesKey(); val knownOwner = knownKey(); val ids = favorites.toList()
        try {
            val stored = org.json.JSONArray(playbackPrefs.getString(ownerKey,"[]"))
            val existing = (0 until stored.length()).mapNotNull { i ->
                val n = stored.getJSONObject(i); val parts = n.optString("cloud").split(":")
                if(parts.size != 3) null else ReleaseNotice(n.getString("id"),MediaSummary(parts[2].toInt(),parts[1],n.getString("title"),n.optString("overview"),null,n.optString("poster").takeIf { it.isNotBlank() },0.0,""),n.getString("message"),n.optString("date"))
            }.toMutableList()
            val known = org.json.JSONObject(playbackPrefs.getString(knownOwner,"{}"))
            for(id in ids) {
                val item = runCatching { tmdb.byCloudId(id) }.getOrNull() ?: continue
                val releaseResult = runCatching { tmdb.latestRelease(item) }
                if(releaseResult.isFailure) continue
                val release = releaseResult.getOrNull()
                if(!known.has(id)) known.put(id, release?.id ?: "pending")
                else if(release != null && known.optString(id) != release.id) {
                    if(existing.none { it.id == release.id }) existing.add(0,release)
                    known.put(id,release.id)
                }
            }
            if(ownerKey != noticesKey()) return
            notifications.clear(); notifications.addAll(existing.take(100))
            playbackPrefs.edit().putString(knownOwner,known.toString()).apply(); persistNotices()
        } finally { checkingNotices = false }
    }
    suspend fun playbackSegments(req: PlayerRequest): PlaybackSegments = runCatching {
        val imdb = tmdb.details(req.item).imdbId ?: return@runCatching PlaybackSegments()
        introRepo.segments(imdb,req.season,req.episode,req.item.type == "movie")
    }.getOrDefault(PlaybackSegments())
    suspend fun playerSources(req: PlayerRequest, position: Long) {
        if(req.live) { error = "Live channels use their configured stream"; return }
        val imdb = tmdb.details(req.item).imdbId ?: return
        chooseSources(req.item,imdb,req.season,req.episode)
        sourceSelection = sourceSelection?.copy(resumeMs = position)
    }
    suspend fun nextEpisode(req: PlayerRequest): EpisodeSummary? {
        if(req.live || req.item.type != "series") return null
        val today = java.text.SimpleDateFormat("yyyy-MM-dd",Locale.US).format(java.util.Date())
        val current = tmdb.season(req.item.id,req.season)
        current.firstOrNull { it.episode > req.episode && it.airDate.isNotBlank() && it.airDate <= today }?.let { return it }
        val count = tmdb.details(req.item).seasonCount
        if(req.season < count) return tmdb.season(req.item.id,req.season+1).firstOrNull { it.airDate.isNotBlank() && it.airDate <= today }
        return null
    }
    suspend fun playNext(req: PlayerRequest, ep: EpisodeSummary) {
        if(partyRole == PartyRole.GUEST) return
        val imdb = tmdb.details(req.item).imdbId ?: return
        selected = req.item; details = tmdb.details(req.item); currentSeason = ep.season
        episodes.clear(); episodes.addAll(tmdb.season(req.item.id,ep.season))
        playResolved(req.item,imdb,ep.season,ep.episode,resumeOverride = 0)
    }
    suspend fun requestPartyPlayback(req: PlayerRequest, playing: Boolean) {
        val s=session ?: return; val room=watchParty?.roomCode ?: return
        social.playback(s,room,req.item.cloudId,req.season,req.episode,playing)
    }
    suspend fun takePartyPlayback(): Boolean? {
        val s=session ?: return null; val room=watchParty?.roomCode ?: return null
        return social.takePlayback(s,room)
    }
    suspend fun requestPartyPause(req: PlayerRequest) {
        val s = session ?: return; val room = watchParty?.roomCode ?: return
        social.pause(s,room,req.item.cloudId,req.season,req.episode)
    }
    suspend fun takePartyPause(): Boolean {
        val s = session ?: return false; val room = watchParty?.roomCode ?: return false
        return social.takePause(s,room)
    }
    val ratings=mutableStateMapOf<String,Int>()
    val forYou=mutableStateListOf<MediaSummary>()
    val comingMovies=mutableStateListOf<MediaSummary>()
    val comingSeries=mutableStateListOf<MediaSummary>()
    private val ratingRepo=RatingRepository(BuildConfig.SUPABASE_URL,BuildConfig.SUPABASE_KEY)
    private fun ratingsKey()="ratings_${session?.userId ?: "local"}_${activeProfile.id}"
    suspend fun loadRatings() {
        val owner=ratingsKey()
        val j=org.json.JSONObject(playbackPrefs.getString(owner,"{}").orEmpty())
        forYou.clear()
        ratings.clear(); j.keys().forEach { id -> j.optInt(id).takeIf { it in 1..10 }?.let { ratings[id]=it } }
        val cached=ratings.toMap()
        session?.let { s ->
            val remote=runCatching { ratingRepo.load(s,activeProfile.id) }.getOrNull()
            if(remote!=null && owner==ratingsKey() && cached==ratings.toMap()) { ratings.clear(); ratings.putAll(remote); persistRatings() }
        }
        if(owner==ratingsKey()) refreshForYou()
    }
    private fun persistRatings() {
        val j=org.json.JSONObject(); ratings.forEach { (id,score) -> j.put(id,score) }
        playbackPrefs.edit().putString(ratingsKey(),j.toString()).apply()
    }
    suspend fun rate(item: MediaSummary,score: Int) {
        require(score in 1..10)
        val owner=ratingsKey(); val profile=activeProfile.id
        session?.let { ratingRepo.save(it,profile,item.cloudId,score) }
        if(owner!=ratingsKey()) return
        ratings[item.cloudId]=score; persistRatings(); refreshForYou()
    }
    suspend fun recommendationsFor(context: MediaSummary? = null): List<MediaSummary> = coroutineScope {
        val scores=ratings.toMap()
        val seeds=(scores.filterValues { it>=6 }.entries.sortedByDescending { it.value }.take(4)
            +scores.filterValues { it<=4 }.entries.sortedBy { it.value }.take(2))
        val jobs=seeds.map { (id,score) -> async {
            val rows=runCatching { tmdb.byCloudId(id)?.let { tmdb.recommendations(it) } }.getOrNull().orEmpty()
            (score-5).toDouble() to rows
        } }
        val base=context?.let { async { runCatching { tmdb.recommendations(it) }.getOrDefault(emptyList()) } }
        val weighted=jobs.awaitAll().toMutableList()
        base?.let { weighted.add(1.0 to it.await()) }
        val ranking=mutableMapOf<String,Double>(); val candidates=linkedMapOf<String,MediaSummary>()
        weighted.forEach { (weight,rows) -> rows.forEachIndexed { index,item ->
            candidates[item.cloudId]=item
            ranking[item.cloudId]=(ranking[item.cloudId] ?: 0.0)+weight*(1.0-index/40.0)
        } }
        candidates.values.filter { it.cloudId!=context?.cloudId && !scores.containsKey(it.cloudId) && (ranking[it.cloudId] ?: 0.0)>0 }
            .sortedWith(compareByDescending<MediaSummary> { ranking[it.cloudId] ?: 0.0 }.thenByDescending { it.rating }).take(30)
    }
    suspend fun refreshForYou() {
        val owner=ratingsKey(); val revision=ratings.toMap()
        val rows=if(revision.values.any { it>=6 }) recommendationsFor() else emptyList()
        if(owner==ratingsKey() && revision==ratings.toMap()) { forYou.clear(); forYou.addAll(rows) }
    }
    suspend fun loadComingSoon() = coroutineScope {
        val movies=async { tmdb.comingSoon("movie") }; val shows=async { tmdb.comingSoon("series") }
        comingMovies.clear(); comingMovies.addAll(movies.await())
        comingSeries.clear(); comingSeries.addAll(shows.await())
    }
    private val livePrefs = context.getSharedPreferences("miflix_live", Context.MODE_PRIVATE)
    private val social = SocialRepository(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_KEY)
    val trakt = TraktRepository(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_KEY)
    var nickname by mutableStateOf("")
    val partyMembers = mutableStateListOf<SocialPerson>()
    val friendActivity = mutableStateListOf<FriendActivity>()
    val accessRequests = mutableStateListOf<AccessRequest>()
    var pendingPartyCode by mutableStateOf<String?>(null)
    suspend fun refreshFriendsDirectory() {
        val s=session ?: return
        refreshFriends()
        val people=social.activity(s)
        friendActivity.clear(); friendActivity.addAll(people)
        accessRequests.clear(); accessRequests.addAll(social.requests(s))
    }
    suspend fun refreshSocialActivity() {
        val s=session ?: return
        val req=playerRequest.takeIf { screen==Screen.PLAYER || watchParty!=null }
        social.presence(s,req?.item?.title.orEmpty(),req?.item?.cloudId.orEmpty(),req?.season ?: 0,req?.episode ?: 0,watchParty?.roomCode)
        social.publishAvatar(s,activeProfile.avatarValue ?: ProfileAvatars[(activeProfile.id.hashCode() and Int.MAX_VALUE)%4].key)
        if(screen!=Screen.FRIENDS && screen!=Screen.WATCH_PARTY) {
            accessRequests.clear(); accessRequests.addAll(social.requests(s))
        }
        pendingPartyCode?.let { code ->
            val decision=try { social.requestAccess(s,code) } catch(e: Exception) {
                if(e is CancellationException) throw e
                if(e.message.orEmpty().contains("closed or expired")) { pendingPartyCode=null; partyStatus="La sala cerró o expiró" }
                return
            }
            when(decision) {
                "approved" -> { pendingPartyCode=null; joinWatchParty(code) }
                "rejected" -> { pendingPartyCode=null; partyStatus="El host rechazó la solicitud" }
            }
        }
    }
    suspend fun decideAccess(request: AccessRequest, approve: Boolean) = socialAction {
        val s=session ?: return@socialAction
        social.decide(s,request,approve); accessRequests.remove(request)
    }
    val friendRows = mutableStateListOf<Friendship>()
    val friendPeople = mutableStateMapOf<String, String>()
    val partyMessages = mutableStateListOf<PartyEvent>()
    val floatingEvents = mutableStateListOf<PartyEvent>()
    val partyPhrases = mutableStateListOf<String>().apply {
        addAll(socialPrefs.getString("phrases", "¡Qué buena escena!|Un momento, por favor|¡No spoilers!").orEmpty().split("|").filter { it.isNotBlank() })
    }
    private var lastEventId = -1L
    private val traktSent = mutableSetOf<String>()
    var traktConnected by mutableStateOf(false)
    var traktStatus by mutableStateOf("")
    var traktDevice by mutableStateOf<org.json.JSONObject?>(null)
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

    private fun loadOfflineProfiles() {
        profiles.clear(); profiles.addAll(local.offlineProfiles().ifEmpty { listOf(Profile("default","Main",primary=true)) })
        activeProfile=profiles.firstOrNull { it.id==local.profileId() } ?: profiles.first()
    }
    suspend fun bootstrap() {
        if(session==null) loadOfflineProfiles()
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
        session?.let { account ->
            runCatching { trakt.call(account,"status").optBoolean("connected") }.onSuccess { traktConnected=it }
            runCatching { nickname = social.nickname(account) }
            val room = socialPrefs.getString("room_${account.userId}", "").orEmpty()
            if (room.isNotBlank()) runCatching {
                cloud.readWatchParty(account, room)?.let { party ->
                    watchParty = party; partyRole = if (party.hostUserId == account.userId) PartyRole.HOST else PartyRole.GUEST
                    social.join(account, room)
                }
            }
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

    suspend fun createProfile(name: String, avatar: String = "ai:astronaut") {
        val clean = name.trim().take(24)
        if (clean.isBlank()) return
        val p = Profile(UUID.randomUUID().toString(), clean, avatarValue=avatar, primary = false)
        profiles.add(p)
        session?.let {
            cloud.upsertProfiles(it, profiles.toList())
            cloud.upsertProfile(it, p.id, emptySet(), emptyMap())
        }
        if(session==null) local.saveOfflineProfiles(profiles.toList())
    }
    suspend fun setProfileAvatar(profile: Profile, avatar: String) {
        require(ProfileAvatars.any { it.key==avatar }) { "Elige un avatar disponible" }
        val changed=profile.copy(avatarValue=avatar)
        val next=profiles.map { if(it.id==profile.id) changed else it }
        session?.let { cloud.upsertProfiles(it,next) }
        profiles.clear(); profiles.addAll(next)
        if(activeProfile.id==profile.id) activeProfile=changed
        if(session==null) local.saveOfflineProfiles(next)
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
        if(partyRole==PartyRole.GUEST) {
            val remote=refreshParty() ?: return
            if(remote.cloudId!=selection.item.cloudId || remote.season!=selection.season || remote.episode!=selection.episode) {
                error="El host cambió de contenido. Abre Playback links nuevamente."; switchToPartyState(remote); return
            }
        }
        playResolved(selection.item, selection.imdb, selection.season, selection.episode, resumeOverride = selection.resumeMs, selectedStream = stream)
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
        if (req.live || duration <= 0) return
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

    suspend fun createWatchParty(withCurrentTitle: Boolean = false): WatchParty? {
        val s = session ?: run { error = "Sign in before creating a Watch Party"; return null }
        watchParty?.let { return it }
        val item = if (withCurrentTitle) selected ?: playerRequest?.item else null
        val req = if (withCurrentTitle && playerRequest?.item?.cloudId == item?.cloudId) playerRequest else null
        val saved = item?.let { progress[it.cloudId] }
        val room = (100000..999999).random().toString()
        val party = WatchParty(
            roomCode = room,
            hostUserId = s.userId,
            cloudId = item?.cloudId.orEmpty(),
            season = req?.season ?: saved?.season ?: if (item?.type == "series") 1 else 0,
            episode = req?.episode ?: saved?.episode ?: if (item?.type == "series") 1 else 0,
            positionMs = req?.resumeMs ?: saved?.position ?: 0L,
            playing = false,
            updatedAt = System.currentTimeMillis(),
            liveChannelId = req?.liveChannelId.orEmpty(),
            liveProvider = req?.liveProvider.orEmpty(),
            liveType = req?.liveType ?: "tv",
            liveTitle = if (req?.live == true) req.item.title else ""
        )
        return runCatching { cloud.createWatchParty(s, party); watchParty = party; partyRole = PartyRole.HOST; partyStatus = "Room ready · choose any title"; social.join(s, room); rememberRoom(room); lastEventId = -1; party }
            .onFailure { error = it.message }.getOrNull()
    }

    suspend fun joinWatchParty(code: String) {
        val s = session ?: run { error = "Sign in before joining a Watch Party"; return }
        require(code.trim().matches(Regex("[0-9]{6}"))) { "Usa el código de 6 dígitos" }
        if(watchParty!=null && watchParty?.roomCode!=code.trim()) { error="Sal del party actual antes de solicitar otra sesión"; return }
        val decision=social.requestAccess(s,code.trim())
        if(decision!="approved") {
            pendingPartyCode=if(decision=="pending") code.trim() else null
            partyStatus=if(decision=="pending") "Solicitud enviada · esperando aprobación del host" else "El host rechazó la solicitud"
            return
        }
        pendingPartyCode=null
        social.join(s, code.trim())
        val party = cloud.readWatchParty(s, code.trim()) ?: run { error = "Watch Party room not found"; return }
        watchParty = party
        partyRole = if (party.hostUserId == s.userId) PartyRole.HOST else PartyRole.GUEST
        rememberRoom(party.roomCode)
        lastEventId = -1
        partyStatus = "Joined ${party.roomCode}"
        if (party.cloudId.isBlank()) { screen = Screen.WATCH_PARTY; return }
        switchToPartyState(party)
    }

    suspend fun hostPartyUpdate(req: PlayerRequest, positionMs: Long, playing: Boolean) {
        val s = session ?: return
        if (partyRole != PartyRole.HOST) return
        val current = watchParty ?: return
        val next = current.copy(
            cloudId = req.item.cloudId,
            liveChannelId = req.liveChannelId,
            liveProvider = req.liveProvider,
            liveType = req.liveType,
            liveTitle = if (req.live) req.item.title else "",
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

    private var switchingParty = false
    suspend fun switchToPartyState(remote: WatchParty) {
        if(switchingParty)return
        switchingParty=true
        try {
        val current = playerRequest
        if (current != null && current.item.cloudId == remote.cloudId && current.season == remote.season && current.episode == remote.episode) return
        if (remote.cloudId.isBlank()) return
        if (remote.liveChannelId.isNotBlank()) {
            val manifest = if (remote.liveProvider == "sports") livePrefs.getString("sports_manifest", "https://sportsfree-us2.highfly.to/manifest.json").orEmpty()
                else "https://stremio-addon-wheat.vercel.app/manifest.json"
            val channel = LiveChannel(remote.liveChannelId, remote.liveType, remote.liveTitle, null, "")
            val stream = LiveRepository().streams(manifest, channel).firstOrNull() ?: return
            val item = MediaSummary(remote.cloudId.substringAfterLast(':').toIntOrNull() ?: channel.id.hashCode(), "live", channel.name, "", null, null, 0.0, "LIVE")
            selected=item
            playerRequest = PlayerRequest(item, stream, 0, 0, 0L, true, channel.id, remote.liveProvider, liveType=remote.liveType)
            screen = Screen.PLAYER
            return
        }
        val item = tmdb.byCloudId(remote.cloudId) ?: return
        selected = item
        details = tmdb.details(item)
        if (item.type == "series") loadSeason(remote.season.coerceAtLeast(1))
        val imdb = details?.imdbId ?: return
        playResolved(item, imdb, remote.season, remote.episode, remote.positionMs)
        } finally { switchingParty=false }
    }

    suspend fun leaveWatchParty() {
        val s = session
        val p = watchParty
        if (s != null && p != null && partyRole == PartyRole.HOST) runCatching { cloud.deleteWatchParty(s, p.roomCode) }
        if (s != null && p != null) runCatching { social.leave(s, p.roomCode) }
        s?.let { socialPrefs.edit().remove("room_${it.userId}").apply() }
        partyMessages.clear(); floatingEvents.clear(); partyMembers.clear(); lastEventId = -1
        watchParty = null
        partyRole = null
        partyStatus = ""
    }

    private fun rememberRoom(code: String) { session?.let { socialPrefs.edit().putString("room_${it.userId}", code).apply() } }

    suspend fun refreshAccountSession() {
        val account=session ?: return
        if(account.refreshToken.isBlank())return
        runCatching { cloud.refresh(account.refreshToken) }.onSuccess { refreshed ->
            if(session?.userId==account.userId) { session=refreshed; local.saveSession(refreshed) }
        }
    }

    suspend fun socialAction(block: suspend () -> Unit) {
        try { block() } catch (e: Exception) {
            if (e is CancellationException) throw e
            error = e.message
        }
    }
    suspend fun saveNickname(value: String) = socialAction {
        val s = session ?: error("Sign in first")
        val nick = value.trim().lowercase(Locale.US)
        social.saveNickname(s, nick); nickname = nick
    }
    fun savePhrases(value: String) {
        val values = value.split("|").map { it.trim().take(120) }.filter { it.isNotEmpty() }.distinct().take(8)
        partyPhrases.clear(); partyPhrases.addAll(values)
        socialPrefs.edit().putString("phrases", values.joinToString("|")).apply()
    }
    suspend fun sendPartyEvent(kind: String, text: String) = socialAction {
        val s = session ?: error("Sign in first")
        val room = watchParty?.roomCode ?: error("Join a room first")
        social.send(s, room, kind, text)
    }
    suspend fun refreshFriends() = socialAction {
        val s = session ?: return@socialAction
        nickname = social.nickname(s)
        friendRows.clear(); friendRows.addAll(social.friends(s))
        val ids = friendRows.flatMap { listOf(it.sender,it.receiver) }.distinct()
        social.people(s, ids).forEach { friendPeople[it.id] = it.nickname }
    }
    suspend fun addFriend(id: String) = socialAction {
        val s = session ?: error("Sign in first")
        require(id != s.userId) { "This is your account" }
        val existing = social.friends(s).firstOrNull { it.sender == id || it.receiver == id }
        if (existing?.status == "accepted") return@socialAction
        if (existing?.receiver == s.userId) social.accept(s, id)
        else if (existing == null) social.requestFriend(s, id)
        refreshFriends()
    }
    suspend fun findAndAddFriend(nick: String) = socialAction {
        val s = session ?: error("Sign in first")
        val person = social.find(s, nick.trim()) ?: error("Nickname not found")
        addFriend(person.id)
    }
    suspend fun pollSocialRoom() {
        val s = session ?: return
        val p = watchParty ?: return
        val fresh = cloud.readWatchParty(s, p.roomCode)
        if (fresh == null) { watchParty = null; partyRole = null; partyStatus = "Room closed or expired"; return }
        if (partyRole == PartyRole.GUEST) {
            watchParty = fresh
            if (fresh.cloudId.isNotBlank()) switchToPartyState(fresh)
        }
        partyMembers.clear(); partyMembers.addAll(social.members(s,p.roomCode))
        val events = social.events(s,p.roomCode,lastEventId)
        if (lastEventId < 0) {
            partyMessages.clear(); partyMessages.addAll(events)
            lastEventId = events.lastOrNull()?.id ?: 0
        } else {
            for (event in events) {
                lastEventId = event.id
                partyMessages.add(event)
                floatingEvents.add(event)
                while (floatingEvents.size > 3) floatingEvents.removeAt(0)
                launch { delay(4500); floatingEvents.removeAll { it.id == event.id } }
            }
            while (partyMessages.size > 60) partyMessages.removeAt(0)
        }
    }
    suspend fun heartbeatRoom() {
        val s=session ?: return
        if(partyRole==PartyRole.HOST) watchParty?.let { cloud.updateWatchParty(s,it) }
    }

    suspend fun traktRefresh() = socialAction {
        val s = session ?: error("Sign in first")
        val result = trakt.call(s,"status")
        traktConnected = result.optBoolean("connected")
        traktStatus = if (traktConnected) "Connected" else if (result.optBoolean("configured")) "Not connected" else "Trakt server setup required"
    }
    suspend fun traktStart() = socialAction {
        val s = session ?: error("Sign in first")
        traktDevice = trakt.call(s,"start")
        traktStatus = "Authorize the code on your phone"
    }
    suspend fun traktPoll(): Boolean {
        val s = session ?: return true
        val result=trakt.call(s,"poll")
        if(result.optBoolean("slow_down")) traktDevice=traktDevice?.put("interval",(traktDevice?.optLong("interval",5)?:5)+5)
        if (result.optBoolean("connected")) { traktConnected=true; traktDevice=null; traktStatus="Connected"; return true }
        return false
    }
    suspend fun traktDisconnect() = socialAction {
        val s=session ?: return@socialAction
        trakt.call(s,"disconnect"); traktConnected=false; traktDevice=null; traktStatus="Disconnected"
    }
    suspend fun traktImport() = socialAction {
        val s=session ?: error("Sign in first")
        val rows=trakt.call(s,"watchlist").optJSONArray("items") ?: return@socialAction
        for(i in 0 until rows.length()) {
            val row=rows.getJSONObject(i)
            val movie=row.optJSONObject("movie")
            val show=row.optJSONObject("show")
            val media=movie ?: show ?: continue
            val id=media.optJSONObject("ids")?.optInt("tmdb",0) ?: 0
            if(id>0) { val key="tmdb:${if(movie!=null) "movie" else "series"}:$id"; if(!favorites.contains(key))favorites.add(key) }
        }
        pushCloud(); traktStatus="Watchlist imported to My List"
    }
    suspend fun traktMarkWatched(req: PlayerRequest, position: Long, duration: Long) {
        if(!traktConnected || req.live || duration<=0 || position.toDouble()/duration<0.8)return
        val s=session ?: return
        val event=req.playbackId
        if(traktSent.contains(event))return
        if(!traktSent.add(event))return
        runCatching {
            trakt.call(s,"watched",org.json.JSONObject().put("tmdb",req.item.id).put("type",req.item.type)
                .put("season",req.season).put("episode",req.episode).put("event",event))
        }.onFailure { traktSent.remove(event); traktStatus="Could not sync watched title: ${it.message}" }
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
        watchParty=null; partyRole=null; nickname=""
        partyMessages.clear(); floatingEvents.clear(); partyMembers.clear()
        friendRows.clear(); friendPeople.clear(); friendActivity.clear(); accessRequests.clear(); pendingPartyCode=null; lastEventId=-1
        traktConnected=false; traktDevice=null; traktSent.clear()
        session = null
        local.saveSession(null)
        favorites.clear(); progress.clear(); profiles.clear(); continueWatching.clear(); ratings.clear(); forYou.clear()
        loadOfflineProfiles()
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
    DETAILS, PLAYER, LIVE_TV, FRIENDS, TRAKT, NOTIFICATIONS
}

data class PlayerRequest(
    val item: MediaSummary,
    val stream: StreamChoice,
    val season: Int,
    val episode: Int,
    val resumeMs: Long,
    val live: Boolean = false,
    val liveChannelId: String = "",
    val liveProvider: String = "",
    val playbackId: String = java.util.UUID.randomUUID().toString(),
    val liveType: String = "tv"
)

data class SourceSelection(
    val item: MediaSummary,
    val imdb: String,
    val season: Int,
    val episode: Int,
    val streams: List<StreamChoice>,
    val resumeMs: Long? = null
)
