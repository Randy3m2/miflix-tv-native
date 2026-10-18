package com.miflix.native2.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.miflix.native2.BuildConfig
import com.miflix.native2.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
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
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.Locale
import java.util.UUID

class AppState(context: Context) {
    init { com.miflix.native2.data.AwardSelections.configure(context.assets.open("awards_catalog.json").bufferedReader().use {it.readText()}) }
    private val appContext=context.applicationContext
    var clearingCache by mutableStateOf(false)
    var cacheCleared by mutableStateOf(false)
    suspend fun clearCache() {
        if(clearingCache) return
        clearingCache=true; cacheCleared=false
        try {
            kotlinx.coroutines.withContext(Dispatchers.IO) {
                val loader=coil3.SingletonImageLoader.get(appContext)
                loader.memoryCache?.clear()
                loader.diskCache?.clear()
            }
            cacheCleared=true
        } finally { clearingCache=false }
    }

    private val socialPrefs = context.getSharedPreferences("miflix_social", Context.MODE_PRIVATE)
    private val playbackPrefs = context.getSharedPreferences("miflix_playback", Context.MODE_PRIVATE)
    private val audioDecoderMimes by lazy {
        runCatching {android.media.MediaCodecList(android.media.MediaCodecList.REGULAR_CODECS).codecInfos
            .filter {!it.isEncoder}.flatMap {codec ->codec.supportedTypes.filter {it.startsWith("audio/")}}.distinct().toTypedArray()}.getOrDefault(emptyArray<String>())
    }
    private fun audioSourceRank(stream:StreamChoice):Int {
        if(audioDecoderMimes.isEmpty()) return 1
        val info=StreamPresentation.parse(stream.addonName,stream.name,stream.title,stream.filename,stream.videoSize)
        return AudioSourcePolicy.rank(info.audio,audioDecoderMimes)
    }

    var adaptiveSource by mutableStateOf(playbackPrefs.getBoolean("adaptive_source",false))
        private set
    fun saveAdaptiveSource(enabled:Boolean) {adaptiveSource=enabled;playbackPrefs.edit().putBoolean("adaptive_source",enabled).apply()}
    private val cinemaStats=LocalCinemaStats(playbackPrefs)
    fun recordViewing(req:PlayerRequest,ms:Long) {
        if(!req.live && !isMomentOnly(req) && appForeground && screen==Screen.PLAYER && playerRequest?.playbackId==req.playbackId)
            cinemaStats.record(ratingsKey(),req.item,details?.takeIf {it.summary.cloudId==req.item.cloudId},ms)
    }
    fun flushCinemaStats()=cinemaStats.flush()
    fun cinemaReport()=cinemaStats.snapshot(ratingsKey())
    private var homeOrderRevision by mutableIntStateOf(0)
    val homeRowOrder:List<String> get() {homeOrderRevision;val a=runCatching {org.json.JSONArray(playbackPrefs.getString("home_order:${ratingsKey()}","[]"))}.getOrDefault(org.json.JSONArray());return (0 until a.length()).map {a.getString(it)}.distinct()}
    fun saveHomeRowOrder(keys:List<String>) {playbackPrefs.edit().putString("home_order:${ratingsKey()}",org.json.JSONArray(keys.distinct()).toString()).apply();homeOrderRevision++}

    var hiddenAvatars by mutableStateOf(playbackPrefs.getStringSet("hidden_avatars",emptySet()).orEmpty().toSet())
    fun hideAvatar(key: String) {
        hiddenAvatars=hiddenAvatars+key
        playbackPrefs.edit().putStringSet("hidden_avatars",hiddenAvatars).apply()
    }
    fun restoreAvatars() {
        hiddenAvatars=emptySet()
        playbackPrefs.edit().remove("hidden_avatars").apply()
    }
    var changelogOpen by mutableStateOf(false)
    @Volatile internal var localNowPlaying: String = "{\"active\":false}"
    var localCastPairing by mutableStateOf(false)
    var socialProfileEditor by mutableStateOf(false)
    var contextTitle by mutableStateOf<MediaSummary?>(null)
    var contextContinue by mutableStateOf(false)
    fun titleActions(item: MediaSummary, continuing: Boolean = false) {contextContinue=continuing;contextTitle=item}
    var focusColor by mutableIntStateOf(playbackPrefs.getInt("focus_color",0xFFB792FF.toInt()))
        private set
    var rememberLastProfile by mutableStateOf(playbackPrefs.getBoolean("remember_last_profile",false))
        private set
    init { FocusTheme.argb=focusColor }
    fun saveFocusColor(value: Int) { focusColor=value;FocusTheme.argb=value;playbackPrefs.edit().putInt("focus_color",value).apply() }
    fun setRememberProfile(value: Boolean) { rememberLastProfile=value;playbackPrefs.edit().putBoolean("remember_last_profile",value).apply() }
    private fun readNoticesKey()="read_"+noticesKey()
    var noticesRevision by mutableIntStateOf(0)
        private set
    fun noticeRead(id: String): Boolean { noticesRevision;return id in playbackPrefs.getStringSet(readNoticesKey(),emptySet()).orEmpty() }
    val unreadNoticeCount: Int get() = notifications.count { !noticeRead(it.id) } + if(noticeRead(UpdateNotes.id)) 0 else 1
    fun markNoticesRead(id: String? = null) {
        val ids=playbackPrefs.getStringSet(readNoticesKey(),emptySet()).orEmpty().toSet()+(id?.let {setOf(it)} ?: (notifications.map {it.id}.toSet()+UpdateNotes.id))
        playbackPrefs.edit().putStringSet(readNoticesKey(),ids).apply();noticesRevision++
    }
    var interfaceLanguage by mutableStateOf(playbackPrefs.getString("interface", "en") ?: "en")
    init { UiLanguage.code=interfaceLanguage }
    suspend fun setInterfaceLanguage(code: String) {
        require(code in listOf("en","es"))
        interfaceLanguage=code; UiLanguage.code=code
        playbackPrefs.edit().putString("interface",code).apply()
        tmdb.language=if(code=="es") "es-ES" else "en-US"
        extrasLoaded=false
        comingMovies.clear(); comingSeries.clear(); forYou.clear()
        seasonalItems.clear(); seasonalKey=""; seasonalRetryAt=0L; comingSoonKey=""
        if(tmdb.token.isNotBlank()) loadHome()
    }
    private val playbackWork = PlaybackWorkGate()
    private var partyEpoch = 0L
    private var switchingPartyEpoch: Long? = null
    var streamSizeLimitGb by mutableStateOf((playbackPrefs.getString("stream_size_gb","0")?.toDoubleOrNull() ?: 0.0))
        private set
    private val streamSizeBytes: Long get() = (streamSizeLimitGb * 1_000_000_000.0).toLong()
    fun saveStreamSizeLimit(gb: Double) {
        require(gb.isFinite() && gb in 0.0..500.0)
        streamSizeLimitGb=gb
        playbackPrefs.edit().putString("stream_size_gb",gb.toString()).apply()
        invalidatePlaybackWork()
    }
    private fun invalidatePlaybackWork() {
        playbackWork.invalidate(); sourceFallback=null; sourceSelection=null; busyMessage=null;playbackLoading=null
        playbackSubtitleJobs.values.forEach { it.cancel() }; playbackSubtitleJobs.clear()
    }
    private fun sameParty(epoch: Long, owner: String, room: String): Boolean =
        partyEpoch==epoch && session?.userId==owner && watchParty?.roomCode==room
    var partyClosedNotice by mutableStateOf(false)
    var partyJoinedNotice by mutableStateOf<String?>(null)
    private var joinedNoticeSequence=0L
    private var knownPartyMembers: Set<String>? = null
    private fun showJoined(name: String) {
        val sequence=++joinedNoticeSequence
        partyJoinedNotice="$name · Joined"
        launch { delay(4500); if(joinedNoticeSequence==sequence) partyJoinedNotice=null }
    }
    var partyPlaybackNotice by mutableStateOf<PartyPlaybackNotice?>(null)
        private set
    private var playbackNoticeSequence=0L
    private fun showPlaybackNotice(name: String,action: String) {
        val sequence=++playbackNoticeSequence
        partyPlaybackNotice=PartyPlaybackNoticePolicy.notice(name,action)
        launch {delay(3500);if(playbackNoticeSequence==sequence) partyPlaybackNotice=null}
    }
    suspend fun publishPartyPlaybackNotice(playing: Boolean) {
        val account=session ?: return;val room=watchParty ?: return;val epoch=partyEpoch
        if(kidsMode) return
        val localOnly=partyRole==PartyRole.GUEST && room.hostOnlyControls
        val action=(if(playing) "resumed" else "paused")+(if(localOnly) "-local" else "")
        showPlaybackNotice(nickname.ifBlank {activeProfile.name},action)
        // One small event per deliberate remote-control action; no new poll or heartbeat.
        try {if(sameParty(epoch,account.userId,room.roomCode)) social.send(account,room.roomCode,"playback",action)}
        catch(e: CancellationException) {throw e} catch(_: Exception) { /* Cosmetic failure must not interrupt playback. */ }
    }
    var audioLanguage by mutableStateOf(playbackPrefs.getString("audio", "es") ?: "es")
    var subtitleLanguage by mutableStateOf(playbackPrefs.getString("subtitle", "es") ?: "es")
    var subtitleColor by mutableStateOf(playbackPrefs.getInt("subtitleColor",android.graphics.Color.WHITE))
    var subtitleScale by mutableStateOf(playbackPrefs.getFloat("subtitleScale",1f).coerceIn(0.8f,1.4f))
    fun saveSubtitleStyle(color: Int, scale: Float) {
        subtitleColor=color; subtitleScale=scale.coerceIn(0.8f,1.4f)
        playbackPrefs.edit().putInt("subtitleColor",subtitleColor).putFloat("subtitleScale",subtitleScale).apply()
    }
    var autoplayNext by mutableStateOf(playbackPrefs.getBoolean("autoplay", true))
    val notifications = mutableStateListOf<ReleaseNotice>()
    private var checkingNotices = false
    private val introRepo = IntroRepository()
    fun savePlaybackPreferences(audio: String, subtitle: String, autoplay: Boolean) {
        if(audioLanguage != audio) invalidatePlaybackWork()
        audioLanguage = audio; subtitleLanguage = subtitle; autoplayNext = autoplay
        playbackPrefs.edit().putString("audio",audio).putString("subtitle",subtitle).putBoolean("autoplay",autoplay).apply()
    }
    private var queueRevision by mutableStateOf(0)
    private fun queueKey()="queue_${session?.userId ?: "local"}_${activeProfile.id}"
    val queueEntries: List<QueueEntry> get() {
        queueRevision
        val a=runCatching { org.json.JSONArray(playbackPrefs.getString(queueKey(),"[]")) }.getOrDefault(org.json.JSONArray())
        return (0 until a.length()).mapNotNull { i -> runCatching { val x=a.getJSONObject(i); QueueEntry(x.getString("id"),x.getString("cloud"),x.getString("title"),x.optInt("season"),x.optInt("episode")) }.getOrNull() }
    }
    private fun saveQueue(rows: List<QueueEntry>) {
        val a=org.json.JSONArray();rows.forEach { x -> a.put(org.json.JSONObject().put("id",x.id).put("cloud",x.cloud).put("title",x.title).put("season",x.season).put("episode",x.episode)) }
        playbackPrefs.edit().putString(queueKey(),a.toString()).apply();queueRevision++
    }
    fun enqueue(item: MediaSummary, season: Int=0, episode: Int=0) {
        require(queueEntries.size<100) { tr("La cola permite hasta 100 contenidos","Queue supports up to 100 titles") }
        saveQueue(queueEntries+QueueEntry(UUID.randomUUID().toString(),item.cloudId,item.title,season,episode))
    }
    fun removeQueued(id: String)=saveQueue(queueEntries.filterNot { it.id==id })
    fun clearQueue()=saveQueue(emptyList())
    fun moveQueued(id: String, delta: Int) {
        val rows=queueEntries.toMutableList();val i=rows.indexOfFirst { it.id==id };val target=i+delta
        if(i>=0 && target in rows.indices) { java.util.Collections.swap(rows,i,target);saveQueue(rows) }
    }
    var queueStarting by mutableStateOf(false)
        private set
    suspend fun playQueued(req: PlayerRequest?, entry: QueueEntry) {
        if(queueStarting) return
        queueStarting=true
        try {
        if(partyRole==PartyRole.GUEST || playerRequest?.playbackId!=req?.playbackId) return
        val owner=queueKey()
        val item=tmdb.byCloudId(entry.cloud)
            ?: throw IllegalStateException(tr("Contenido no disponible; permanece en la cola","Title unavailable; it remains in the queue"))
        val d=tmdb.details(item)
        if(partyRole==PartyRole.GUEST || owner!=queueKey() || playerRequest?.playbackId!=req?.playbackId || queueEntries.none { it.id==entry.id }) return
        val imdb=d.imdbId ?: throw IllegalStateException(tr("IMDb no disponible","IMDb unavailable"))
        var started: PlayerRequest?=null
        playResolved(item,imdb,entry.season,entry.episode,resumeOverride=0,onStarted={ newRequest ->
            if(owner==queueKey()) { started=newRequest;selected=item;details=d;currentSeason=entry.season;removeQueued(entry.id) }
        })
        val active=started ?: return
        if(partyRole==PartyRole.HOST) hostPartyUpdate(active,0L,true)
        if(item.type=="series") {
            val rows=runCatching { tmdb.season(item.id,entry.season) }.getOrDefault(emptyList())
            if(owner==queueKey() && playerRequest?.playbackId==active.playbackId) { episodes.clear();episodes.addAll(rows) }
        }
        } finally { queueStarting=false }
    }

    val calendarEvents=mutableStateListOf<CalendarEvent>()
    suspend fun refreshCalendar() {
        val owner=session ?: return
        val events=social.calendar(owner)
        if(session?.userId==owner.userId) { calendarEvents.clear();calendarEvents.addAll(events) }
    }
    suspend fun createCalendarEvent(title: String, kind: String, date: String, cloud: String?, season: Int, episode: Int, guests: List<String>, channel: LiveChannel?=null) {
        val owner=session ?: throw IllegalStateException(tr("Inicia sesión","Sign in first"))
        require(title.trim().isNotEmpty() && title.length<=160) { tr("Escribe un título","Enter a title") }
        val parser=java.text.SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.US).apply { isLenient=false }
        val value=parser.parse(date) ?: throw IllegalArgumentException(tr("Fecha inválida","Invalid date"))
        require(parser.format(value)==date) { tr("Usa YYYY-MM-DD HH:mm","Use YYYY-MM-DD HH:mm") }
        val utc=java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'",Locale.US).apply { timeZone=java.util.TimeZone.getTimeZone("UTC") }
        social.createCalendar(owner,title.trim(),kind,utc.format(value),cloud,season,episode,guests,channel)
        refreshCalendar()
    }
    fun calendarLiveManifest(kind: String): String = when(kind) {
        "iptv", "iptv:do", "iptv:tur", "iptv:mx", "iptv:es", "iptv:us" -> IptvRepository.manifest(kind)
        "sports" -> livePrefs.getString("sports_manifest","https://sportsfree-us2.highfly.to/manifest.json").orEmpty()
        else -> "https://stremio-addon-wheat.vercel.app/manifest.json"
    }
    suspend fun openCalendarEvent(event: CalendarEvent) {
        require(!kidsMode) {"Calendar playback is not available in Kids."}
        if(event.kind=="live" || event.kind=="sports") {
            require(partyRole!=PartyRole.GUEST) { tr("El anfitrión elige el contenido de la sala","The host chooses the room content") }
            val owner=session?.userId;val epoch=partyEpoch
            val channel=LiveChannel(event.channelId,event.channelType,event.title,null,"")
            val stream=LiveRepository().streams(calendarLiveManifest(event.kind),channel).firstOrNull()
                ?: throw IllegalStateException(tr("Este evento no tiene enlaces disponibles ahora","No streams are available for this event right now"))
            if(session?.userId!=owner || partyEpoch!=epoch) return
            invalidatePlaybackWork()
            val media=MediaSummary(channel.id.hashCode(),"live",channel.name,"",null,null,0.0,"LIVE")
            selected=media;playerRequest=PlayerRequest(media,stream,0,0,0L,true,channel.id,if(event.kind=="sports") "sports" else "nauta",liveType=channel.type);screen=Screen.PLAYER
        } else {
            val media=event.cloud?.let { tmdb.byCloudId(it) } ?: throw IllegalStateException(tr("Contenido no disponible","Title unavailable"))
            this.open(media)
        }
    }
    suspend fun deleteCalendarEvent(id: String) { session?.let { social.deleteCalendar(it,id);refreshCalendar() } }
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
                if(kidsMode && !tmdb.kidsAllowed(item)) continue
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
            // Preserve subscriptions added while this check was awaiting TMDB.
            val fresh=org.json.JSONObject(playbackPrefs.getString(knownOwner,"{}"))
            val keys=fresh.keys()
            while(keys.hasNext()) { val key=keys.next();if(!known.has(key)) known.put(key,fresh.optString(key)) }
            playbackPrefs.edit().putString(knownOwner,known.toString()).apply(); persistNotices()
        } finally { checkingNotices = false }
    }
    suspend fun playbackSegments(req: PlayerRequest): PlaybackSegments = runCatching {
        val imdb = tmdb.details(req.item).imdbId ?: return@runCatching PlaybackSegments()
        introRepo.segments(imdb,req.season,req.episode,req.item.type == "movie")
    }.getOrDefault(PlaybackSegments())
    suspend fun playerSources(req: PlayerRequest, position: Long) {
        if(req.liveProvider=="lidom") {showLidomSources(req);return}
        if(req.live) { error=tr("Los canales en vivo usan su enlace configurado","Live channels use their configured stream"); return }
        val generation=playbackWork.generation
        val imdb=tmdb.details(req.item).imdbId ?: return
        if(generation!=playbackWork.generation || playerRequest?.playbackId!=req.playbackId) return
        chooseSources(req.item,imdb,req.season,req.episode,position)
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
    private val partyPlaybackMutex=Mutex()
    suspend fun requestPartyPlayback(req: PlayerRequest, playing: Boolean) {
        if(watchParty?.hostOnlyControls==true && partyRole==PartyRole.GUEST) return
        val s=session ?: return; val room=watchParty?.roomCode ?: return
        partyPlaybackMutex.withLock {
            if(session?.userId==s.userId && watchParty?.roomCode==room && playerRequest?.playbackId==req.playbackId) {
                social.playback(s,room,req.item.cloudId,req.season,req.episode,playing)
            }
        }
    }
    suspend fun takePartyPlayback(): Boolean? {
        if(watchParty?.hostOnlyControls==true) return null
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
    private val releaseCardDates=mutableStateMapOf<String,String>()
    private val episodeCardDates=mutableStateMapOf<String,String>()
    private val cardDateLoadedAt=mutableMapOf<String,Long>()
    private val cardDateInflight=mutableSetOf<String>()
    private val cardDateSlots=Semaphore(3)
    fun cardFreshness(item:MediaSummary):String? {
        val now=System.currentTimeMillis()
        if(item.type=="series" && com.miflix.native2.data.CardFreshnessPolicy.newEpisode(episodeCardDates[item.cloudId],now))
            return tr("Nuevo episodio","New Episode")
        if(com.miflix.native2.data.CardFreshnessPolicy.recentRelease(releaseCardDates[item.cloudId] ?: item.releaseDate,now))
            return tr("Recién agregado","Recently Added")
        return null
    }
    fun requestCardFreshness(item:MediaSummary) {
        if(tmdb.token.isBlank() || item.type !in listOf("movie","series") || item.id<=0) return
        val key=item.cloudId;val now=System.currentTimeMillis()
        if(now-(cardDateLoadedAt[key] ?: 0L)<3*3600000L || key in cardDateInflight) return
        // Movie catalog rows already carry their release date. Only missing metadata/series need a lookup.
        if(item.type=="movie" && item.releaseDate.isNotBlank()) return
        if(cardDateInflight.size>=80) return
        cardDateInflight.add(key)
        appScope.launch {
            try {cardDateSlots.withPermit {
                val dates=tmdb.cardDates(item)
                releaseCardDates[key]=dates.first;episodeCardDates[key]=dates.second;cardDateLoadedAt[key]=System.currentTimeMillis()
                if(cardDateLoadedAt.size>500) {
                    val oldest=cardDateLoadedAt.minByOrNull {it.value}?.key
                    oldest?.let {cardDateLoadedAt.remove(it);releaseCardDates.remove(it);episodeCardDates.remove(it)}
                }
            }} catch(e:CancellationException) {throw e} catch(_:Exception) {
                // Brief backoff for unavailable metadata; never retry in a timer or per recomposition.
                cardDateLoadedAt[key]=System.currentTimeMillis()-3*3600000L+60000L
            } finally {cardDateInflight.remove(key)}
        }
    }
    val romancePicks=mutableStateListOf<MediaSummary>()
    val koreanPicks=mutableStateListOf<MediaSummary>()
    val classicPicks=mutableStateListOf<MediaSummary>()
    val horrorPicks=mutableStateListOf<MediaSummary>()
    val christmasPicks=mutableStateListOf<MediaSummary>()
    val becauseWatchedPicks=mutableStateListOf<MediaSummary>()
    var becauseWatchedTitle by mutableStateOf("")
        private set
    private var becauseWatchedKey by mutableStateOf("")
    private val becauseWatchedMutex=Mutex()
    val becauseWatchedMatches:Boolean get()=becauseWatchedKey=="${ratingsKey()}:${tmdb.language}:$discoveryHistoryKey"
    suspend fun loadBecauseWatched() = becauseWatchedMutex.withLock {
        val owner=ratingsKey();val locale=tmdb.language;val history=discoveryHistoryKey
        val key="$owner:$locale:$history"
        if(kidsMode || tmdb.token.isBlank()) {becauseWatchedPicks.clear();becauseWatchedTitle="";return@withLock}
        if(key==becauseWatchedKey) return@withLock
        becauseWatchedPicks.clear();becauseWatchedTitle=""
        val id=history.split("|").firstOrNull {it.isNotBlank()} ?: return@withLock
        val seed=(continueWatching+movies+series+topRated+bestSeries+myListTitles).firstOrNull {it.cloudId==id}
            ?: try {tmdb.byCloudId(id)} catch(e:CancellationException) {throw e} catch(_:Exception) {null}
            ?: return@withLock
        val rows=try {tmdb.recommendations(seed)} catch(e:CancellationException) {throw e} catch(_:Exception) {return@withLock}
        if(owner==ratingsKey() && locale==tmdb.language && history==discoveryHistoryKey && !kidsMode) {
            becauseWatchedPicks.replaceWith(rows.filter {it.cloudId!=id && (progress[it.cloudId]?.percent ?: 0.0)<90.0 && (ratings[it.cloudId] ?: 6)>4})
            becauseWatchedTitle=seed.title;becauseWatchedKey=key
        }
    }

    val oscarPicks=mutableStateListOf<MediaSummary>()
    val emmyPicks=mutableStateListOf<MediaSummary>()
    var holidayHomeLoading by mutableStateOf(false)
        private set
    var holidayHomeMessage by mutableStateOf("")
        private set
    private var holidayHomeOwner by mutableStateOf("")
    private val holidayHomeMutex=Mutex()
    val holidayHomeMatches:Boolean get()=holidayHomeOwner=="${ratingsKey()}:${tmdb.language}"
    suspend fun loadHolidayHome() = holidayHomeMutex.withLock {
        if(kidsMode || tmdb.token.isBlank()) return@withLock
        val owner=ratingsKey();val locale=tmdb.language
        if(!holidayHomeMatches) {horrorPicks.clear();christmasPicks.clear()}
        holidayHomeLoading=true;holidayHomeMessage=""
        try {supervisorScope {
            val horror=async {tmdb.familiarCatalog("horror")}
            val christmas=async {tmdb.familiarCatalog("christmas")}
            val horrorRows=try {horror.await()} catch(e:CancellationException) {throw e} catch(_:Exception) {emptyList()}
            val christmasRows=try {christmas.await()} catch(e:CancellationException) {throw e} catch(_:Exception) {emptyList()}
            if(owner==ratingsKey() && locale==tmdb.language && !kidsMode) {
                horrorPicks.replaceWith(horrorRows);christmasPicks.replaceWith(christmasRows);holidayHomeOwner="$owner:$locale"
                if(christmasRows.isEmpty()) holidayHomeMessage=tr("No se pudo cargar Navidad. Reintenta.","Could not load Christmas Movies. Try again.")
            }
        }} catch(e:CancellationException) {throw e} finally {holidayHomeLoading=false}
    }
    val oscarYears:List<Int> get()=com.miflix.native2.data.AwardSelections.years()
    var oscarYear by mutableIntStateOf(com.miflix.native2.data.AwardSelections.latestYear("oscar"))
        private set
    val oscarYearPicks=mutableStateListOf<MediaSummary>()
    var oscarYearLoading by mutableStateOf(false)
        private set
    var oscarYearMessage by mutableStateOf("")
        private set
    private var oscarYearGeneration=0
    suspend fun loadOscarYear(year:Int) {
        if(kidsMode || year !in oscarYears) return
        val generation=++oscarYearGeneration;val owner=ratingsKey();val locale=tmdb.language
        oscarYear=year;oscarYearPicks.clear();oscarYearLoading=true;oscarYearMessage=""
        try {
            val rows=tmdb.awardCatalog("oscar",year)
            if(generation==oscarYearGeneration && owner==ratingsKey() && locale==tmdb.language && !kidsMode) {
                oscarYearPicks.replaceWith(rows)
                if(rows.isEmpty()) oscarYearMessage=tr("No se pudo cargar esta selección.","Could not load this selection.")
            }
        } catch(e:CancellationException) {throw e} catch(_:Exception) {if(generation==oscarYearGeneration) oscarYearMessage=tr("No se pudo cargar esta selección.","Could not load this selection.")}
        finally {if(generation==oscarYearGeneration) oscarYearLoading=false}
    }
    private var simpleHomeRevision by mutableIntStateOf(0)
    val simpleHome: Boolean get() {simpleHomeRevision;return playbackPrefs.getBoolean("simple_home:${ratingsKey()}",false)}
    fun saveSimpleHome(enabled: Boolean) {
        playbackPrefs.edit().putBoolean("simple_home:${ratingsKey()}",enabled).apply();simpleHomeRevision++
    }
    val discoveryHistoryKey: String get() = progress.entries
        .filter {it.key.matches(Regex("^tmdb:(movie|series):[0-9]+$")) && it.value.position>=5*60*1000L && (ratings[it.key] ?: 6)>4}
        .sortedByDescending {it.value.updatedAt}.take(3).joinToString("|") {it.key}
    private var familiarHomeOwner by mutableStateOf("")
    val familiarHomeMatches: Boolean get() = familiarHomeOwner=="${ratingsKey()}:${tmdb.language}"
    private val familiarHomeMutex=Mutex()
    suspend fun loadFamiliarHome() = familiarHomeMutex.withLock {
        if(kidsMode || tmdb.token.isBlank()) return@withLock
        val owner=ratingsKey();val locale=tmdb.language
        if(!familiarHomeMatches) {romancePicks.clear();koreanPicks.clear();classicPicks.clear();oscarPicks.clear();emmyPicks.clear();horrorPicks.clear();christmasPicks.clear()}
        familiarHomeOwner="$owner:$locale"
        suspend fun load(kind: String,target: MutableList<MediaSummary>) {
            val rows=try {tmdb.familiarCatalog(kind)} catch(e: CancellationException) {throw e} catch(_: Exception) {emptyList()}
            if(owner==ratingsKey() && locale==tmdb.language && !kidsMode) target.replaceWith(rows)
        }
        // Bounded public catalogs, cached locally; no Supabase calls or background polling.
        load("romance",romancePicks);load("kdrama",koreanPicks);load("classics",classicPicks)
        coroutineScope {
            suspend fun loadAwards(kind:String,target:MutableList<MediaSummary>) {
                val rows=try {tmdb.awardCatalog(kind)} catch(e:CancellationException) {throw e} catch(_:Exception) {emptyList()}
                if(owner==ratingsKey() && locale==tmdb.language && !kidsMode) target.replaceWith(rows)
            }
            val oscars=async {loadAwards("oscar",oscarPicks)}
            val emmys=async {loadAwards("emmy",emmyPicks)}
            oscars.await();emmys.await()
        }
        if(owner==ratingsKey() && locale==tmdb.language) refreshForYou()
    }
    val comingMovies=mutableStateListOf<MediaSummary>()
    val comingSeries=mutableStateListOf<MediaSummary>()
    private val ratingRepo=RatingRepository(BuildConfig.SUPABASE_URL,BuildConfig.SUPABASE_KEY)
    private var loadedRatingsOwner=""
    private fun ratingsKey()="ratings_${session?.userId ?: "local"}_${activeProfile.id}"
    suspend fun loadRatings() {
        val owner=ratingsKey()
        val j=org.json.JSONObject(playbackPrefs.getString(owner,"{}").orEmpty())
        forYou.clear()
        ratings.clear(); j.keys().forEach { id -> j.optInt(id).takeIf { it in 1..10 }?.let { ratings[id]=it } }
        loadedRatingsOwner=owner
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
    private fun recommendationSeed(id: String): MediaSummary? {
        val match=Regex("^tmdb:(movie|series):([0-9]+)$").matchEntire(id) ?: return null
        return MediaSummary(match.groupValues[2].toIntOrNull() ?: return null,match.groupValues[1],"","",null,null,0.0,"")
    }
    suspend fun recommendationsFor(context: MediaSummary? = null): List<MediaSummary> = coroutineScope {
        val scores=if(loadedRatingsOwner==ratingsKey()) ratings.toMap() else emptyMap()
        val seeds=(scores.filterValues { it>=6 }.entries.sortedByDescending { it.value }.take(4)
            +scores.filterValues { it<=4 }.entries.sortedBy { it.value }.take(2))
        val history=discoveryHistoryKey.split("|").filter {it.isNotBlank() && it !in scores}.take(2)
        val historyJobs=history.map { id -> async {
            val rows=runCatching {recommendationSeed(id)?.let {tmdb.recommendations(it)}}.getOrNull().orEmpty()
            1.5 to rows
        } }
        val jobs=seeds.map { (id,score) -> async {
            val rows=runCatching { recommendationSeed(id)?.let { tmdb.recommendations(it) } }.getOrNull().orEmpty()
            (score-5).toDouble() to rows
        } }
        val base=context?.let { async { runCatching { tmdb.recommendations(it) }.getOrDefault(emptyList()) } }
        val weighted=(jobs.awaitAll()+historyJobs.awaitAll()).toMutableList()
        base?.let { weighted.add(1.0 to it.await()) }
        val ranking=mutableMapOf<String,Double>(); val candidates=linkedMapOf<String,MediaSummary>()
        weighted.forEach { (weight,rows) -> rows.forEachIndexed { index,item ->
            candidates[item.cloudId]=item
            ranking[item.cloudId]=(ranking[item.cloudId] ?: 0.0)+weight*(1.0-index/40.0)
        } }
        candidates.values.filter { it.cloudId!=context?.cloudId && !scores.containsKey(it.cloudId) && it.cloudId !in history && (progress[it.cloudId]?.percent ?: 0.0)<90.0 && (ranking[it.cloudId] ?: 0.0)>0 }
            .sortedWith(compareByDescending<MediaSummary> { ranking[it.cloudId] ?: 0.0 }.thenByDescending { it.rating }).take(30)
    }
    suspend fun refreshForYou() {
        if(kidsMode) {forYou.clear();return}
        val owner=ratingsKey(); val revision=ratings.toMap();val history=discoveryHistoryKey;val locale=tmdb.language
        val rows=if(revision.values.any { it>=6 } || history.isNotBlank()) recommendationsFor() else emptyList()
        val refined=try {refinedTastePicks()} catch(e:CancellationException) {throw e} catch(_:Exception) {emptyList()}
        if(owner==ratingsKey() && revision==ratings.toMap() && history==discoveryHistoryKey && locale==tmdb.language) {
            forYou.clear();forYou.addAll((refined+rows).filter {(ratings[it.cloudId] ?: 10)>4 && (progress[it.cloudId]?.percent ?: 0.0)<90}.distinctBy {it.cloudId}.take(30))
        }
    }
    private val comingSoonMutex=Mutex()
    private var comingSoonKey=""
    suspend fun loadComingSoon() = comingSoonMutex.withLock {
        if(tmdb.token.isBlank()) return@withLock
        val language=tmdb.language
        val date=java.util.Calendar.getInstance()
        val key="$language:${date.get(java.util.Calendar.YEAR)}:${date.get(java.util.Calendar.DAY_OF_YEAR)}"
        if(comingSoonKey==key) return@withLock
        coroutineScope {
            val movies=async { tmdb.comingSoon("movie") }; val shows=async { tmdb.comingSoon("series") }
            val movieRows=movies.await();val showRows=shows.await()
            if(language==tmdb.language) {
                comingMovies.clear();comingMovies.addAll(movieRows)
                comingSeries.clear();comingSeries.addAll(showRows)
                comingSoonKey=key
            }
        }
    }
    private val livePrefs = context.getSharedPreferences("miflix_live", Context.MODE_PRIVATE)
    private val social = SocialRepository(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_KEY)
    val trakt = TraktRepository(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_KEY)
    var nickname by mutableStateOf("")
    val partyMembers = mutableStateListOf<SocialPerson>()
    val partyInvitations = mutableStateListOf<PartyInvitation>()
    var partyOptionsBusy by mutableStateOf(false)
        private set
    private var sharedSourcePlaybackId: String? = null
    suspend fun inviteToParty(friendId: String) {
        val account=session ?: return;val room=watchParty ?: return
        check(partyRole==PartyRole.HOST)
        social.inviteParty(account,room.roomCode,friendId)
        partyStatus=tr("Invitación enviada","Invitation sent")
    }
    suspend fun previewParty(code: String): org.json.JSONObject? {
        if(!code.matches(Regex("[0-9]{6}"))) return null
        return social.previewParty(session ?: return null,code).takeIf {it.length()>0}
    }
    private fun partyPresentation(p: WatchParty,req: PlayerRequest): WatchParty = p.copy(
        title=req.item.title,overview=req.item.overview.take(800),poster=req.item.poster,
        logo=details?.takeIf {it.summary.cloudId==req.item.cloudId}?.logo,
        sourceName=PartySourcePolicy.label(req.stream.addonName+" · "+req.stream.name),
        sourceFilename=PartySourcePolicy.label(req.stream.filename),sourceSize=req.stream.videoSize,
        sourceKey=PartySourcePolicy.key(req.stream))
    suspend fun configureParty(share: Boolean?=null,hostOnly: Boolean?=null) = partyPublishMutex.withLock {
        val account=session ?: return@withLock;val old=watchParty ?: return@withLock
        check(partyRole==PartyRole.HOST)
        if(partyOptionsBusy) return@withLock
        partyOptionsBusy=true
        try {
            val req=playerRequest
            if(share==true) require(req!=null && req.item.cloudId==old.cloudId && req.season==old.season && req.episode==old.episode) {tr("Cambia primero el party a este contenido","Switch the party to this title first")}
            val matching=req?.takeIf {it.item.cloudId==old.cloudId && it.season==old.season && it.episode==old.episode}
            val next=(matching?.let {partyPresentation(old,it)} ?: old).copy(
                shareStream=share ?: old.shareStream,hostOnlyControls=hostOnly ?: old.hostOnlyControls)
            if(next.shareStream && share==true) {
                require(req!=null) {tr("Inicia primero un contenido","Start a title first")}
                cloud.sharePartySource(account,next,req.stream)
                sharedSourcePlaybackId=req.playbackId
            }
            cloud.updateWatchParty(account,next)
            if(session?.userId==account.userId && watchParty?.roomCode==old.roomCode) watchParty=next
            if(share==false) sharedSourcePlaybackId=null
        } finally { partyOptionsBusy=false }
    }
    suspend fun usePartySource() {
        val account=session ?: return;val remote=refreshParty() ?: return;val epoch=partyEpoch
        val stream=cloud.readPartySource(account,remote) ?: error(tr("El host ya no comparte ese enlace o expiró","The host stopped sharing this source or it expired"))
        if(!sameParty(epoch,account.userId,remote.roomCode)) return
        val current=playerRequest ?: return
        if(current.item.cloudId!=remote.cloudId || current.season!=remote.season || current.episode!=remote.episode) return
        playerRequest=current.copy(stream=stream,resumeMs=remote.positionMs,playbackId=UUID.randomUUID().toString())
        sourceSelection=null;screen=Screen.PLAYER
    }

    val friendActivity = mutableStateListOf<FriendActivity>()
    val accessRequests = mutableStateListOf<AccessRequest>()
    var pendingPartyCode by mutableStateOf<String?>(null)
    suspend fun friendRecommendations(): List<FriendRecommendation> = social.recommendations(
        session ?: error(tr("Inicia sesión primero","Sign in first")))
    suspend fun recommendToFriends(item: MediaSummary) {
        val account=session ?: error(tr("Inicia sesión primero","Sign in first"))
        social.recommend(account,item)
    }
    suspend fun removeFriendRecommendation(cloudId: String) {
        social.removeRecommendation(session ?: error(tr("Inicia sesión primero","Sign in first")),cloudId)
    }
    suspend fun openRecommendation(cloudId: String) {
        val item=tmdb.byCloudId(cloudId) ?: error(tr("Contenido no disponible","Content unavailable"))
        this.open(item)
    }
    // Called only after the UI confirms leaving any existing party.
    suspend fun watchFriendIndependently(friend: FriendActivity) {
        val item=tmdb.byCloudId(friend.cloudId) ?: error(tr("Contenido no disponible","Content unavailable"))
        val ep=if(item.type=="series" && friend.season>0 && friend.episode>0) {
            tmdb.season(item.id,friend.season).firstOrNull { it.episode==friend.episode }
                ?: error(tr("Episodio no disponible","Episode unavailable"))
        } else null
        if(watchParty!=null || pendingPartyCode!=null) leaveWatchParty()
        this.open(item)
        if(item.type=="movie") playMovie() else if(ep!=null) playEpisode(ep) else resumeSeries()
    }
    var accessRequestsLoading by mutableStateOf(false)
        private set
    /** Incoming requests are fetched only from explicit Refresh requests controls. */
    suspend fun refreshPartyRequests() {
        val s=session ?: return
        if(accessRequestsLoading) return
        val epoch=partyEpoch
        accessRequestsLoading=true
        try {
            val requests=social.requests(s)
            if(session?.userId==s.userId && partyEpoch==epoch) {
                accessRequests.clear();accessRequests.addAll(requests)
            }
        } finally { accessRequestsLoading=false }
    }
    val sharedMoments=mutableStateListOf<com.miflix.native2.data.SharedMoment>()
    var sharedMomentsOwner by mutableStateOf("")
        private set
    var sharedMomentsLoading by mutableStateOf(false)
        private set
    /** Called only by the explicit Moments → Refresh button, never by a social timer. */
    suspend fun refreshSharedMoments() {
        val s=session ?: error(tr("Inicia sesión primero","Sign in first"))
        if(sharedMomentsLoading) return
        sharedMomentsLoading=true
        try {
            val rows=social.receivedMoments(s)
            if(session?.userId==s.userId) {sharedMoments.replaceWith(rows);sharedMomentsOwner=s.userId}
        } finally {sharedMomentsLoading=false}
    }
    suspend fun momentRecipients(): List<com.miflix.native2.data.SocialPerson> {
        val s=session ?: error(tr("Inicia sesión primero","Sign in first"))
        val rows=social.friends(s).filter {it.status=="accepted"}
        val ids=rows.map {if(it.sender==s.userId) it.receiver else it.sender}.distinct()
        if(ids.isEmpty()) return emptyList()
        val people=social.people(s,ids)
        return if(session?.userId==s.userId) people else emptyList()
    }
    suspend fun sendMoment(req: PlayerRequest,recipient: String,start: Long,end: Long) {
        val s=session ?: error(tr("Inicia sesión primero","Sign in first"))
        require(!req.live && playerRequest?.playbackId==req.playbackId)
        social.shareMoment(s,recipient,req.playbackId,req.item.cloudId,req.item.title,req.season,req.episode,start,end)
    }
    suspend fun removeSharedMoment(moment: com.miflix.native2.data.SharedMoment) {
        val s=session ?: return
        social.deleteMoment(s,moment.id)
        if(session?.userId==s.userId) sharedMoments.removeAll {it.id==moment.id}
    }
    suspend fun playSharedMoment(moment: com.miflix.native2.data.SharedMoment) {
        val account=session?.userId ?: error(tr("Inicia sesión primero","Sign in first"))
        val profile=activeProfile.id
        val item=tmdb.byCloudId(moment.cloud) ?: error(tr("Contenido no disponible","Content unavailable"))
        if(session?.userId!=account || activeProfile.id!=profile) return
        require(moment.startMs>=0 && moment.endMs>moment.startMs)
        if(watchParty!=null || pendingPartyCode!=null) leaveWatchParty()
        this.open(item)
        if(session?.userId!=account || activeProfile.id!=profile) return
        check(selected?.cloudId==item.cloudId && details?.summary?.cloudId==item.cloudId) {tr("Contenido no disponible","Content unavailable")}
        val imdb=details?.imdbId ?: error(tr("No se pudo identificar el título","Could not identify title"))
        playResolved(item,imdb,moment.season,moment.episode,moment.startMs,onStarted={ req ->
            playerRequest=req.copy(momentStartMs=moment.startMs,momentEndMs=moment.endMs,momentSender=moment.nickname)
        })
    }
    suspend fun refreshFriendsDirectory() {
        retrySocialPresenceEvent()
        val s=session ?: return
        refreshFriends()
        val people=social.activity(s)
        val invitations=try {social.partyInvitations(s)} catch(e: CancellationException) {throw e} catch(_: Exception) {emptyList()}
        if(session?.userId!=s.userId) return
        friendActivity.clear(); friendActivity.addAll(people)
        partyInvitations.clear();partyInvitations.addAll(invitations)
    }
    suspend fun refreshSocialActivity() {
        val s=session ?: return
        val epoch=partyEpoch
        fun valid()=partyEpoch==epoch && session?.userId==s.userId
        social.publishAvatar(s,activeProfile.avatarValue ?: ProfileAvatars[(activeProfile.id.hashCode() and Int.MAX_VALUE)%4].key)
        pendingPartyCode?.let { code ->
            val decision=try { social.requestAccess(s,code) } catch(e: Exception) {
                if(e is CancellationException) throw e
                if(valid() && pendingPartyCode==code && e.message.orEmpty().contains("closed or expired")) { pendingPartyCode=null; partyStatus=tr("La sala cerró o expiró","Room closed or expired") }
                return
            }
            if(!valid() || pendingPartyCode!=code) return
            when(decision) {
                "approved" -> { pendingPartyCode=null; joinWatchParty(code) }
                "rejected" -> { pendingPartyCode=null; partyStatus=tr("El host rechazó la solicitud","The host rejected your request") }
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
        addAll(socialPrefs.getString("phrases", tr("¡Qué buena escena!|Un momento, por favor|¡No spoilers!","What a great scene!|One moment, please|No spoilers!")).orEmpty().split("|").filter { it.isNotBlank() })
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

    private val presenceEvents = com.miflix.native2.data.EventPresencePublisher(appContext,social,appScope)
    var socialReadyPlaybackId by mutableStateOf("")
        private set
    var socialEndedPlaybackId by mutableStateOf("")
        private set
    private var socialExpiresAt=0L
    /** Capture the lease once at first frame, never from the progress/seek ticker. */
    fun socialPlaybackStarted(req: PlayerRequest, position: Long, duration: Long) {
        if(playerRequest?.playbackId!=req.playbackId || socialReadyPlaybackId==req.playbackId) return
        val remaining=if(req.live) 4*60*60*1000L
            else if(duration>0) (duration-position.coerceAtLeast(0)).coerceAtLeast(0)+30*60*1000L
            else if(req.item.type=="series") 90*60*1000L else 3*60*60*1000L
        socialExpiresAt=System.currentTimeMillis()+remaining.coerceIn(60_000L,12*60*60*1000L)
        socialReadyPlaybackId=req.playbackId
    }
    fun socialPlaybackReady(req: PlayerRequest, position: Long, duration: Long) {
        if(socialEndedPlaybackId==req.playbackId) {
            socialEndedPlaybackId="";socialReadyPlaybackId=""
            socialPlaybackStarted(req,position,duration)
        }
    }
    fun socialPlaybackEnded(req: PlayerRequest) {
        if(playerRequest?.playbackId==req.playbackId) socialEndedPlaybackId=req.playbackId
    }
    fun publishSocialPresenceEvent() {
        val s=session ?: return
        val req=playerRequest?.takeIf { appForeground && (screen==Screen.PLAYER || miniPlayer) &&
            it.playbackId==socialReadyPlaybackId && it.playbackId!=socialEndedPlaybackId }
        presenceEvents.queue(s,req?.item?.title.orEmpty(),req?.item?.cloudId.orEmpty(),
            req?.season ?: 0,req?.episode ?: 0,if(req!=null) watchParty?.roomCode else null,
            if(req!=null) socialExpiresAt else System.currentTimeMillis())
    }
    fun retrySocialPresenceEvent() { session?.takeUnless {sessionNeedsSignIn}?.let {presenceEvents.retry(it)} }

    val tmdb = TmdbRepository(BuildConfig.MIFLIX_TMDB_TOKEN).also { it.language=if(interfaceLanguage=="es") "es-ES" else "en-US" }
    val streamRepo = StreamRepository("")
    private val nativeStore=NativeSourceStore(context)
    private val nativeSources=NativeSourceRepository()
    private var nativeRevision by mutableStateOf(0)
    var nativeSourceStatus by mutableStateOf("")
    var nativeSourceSaving by mutableStateOf(false)
    val nativeSourceEnabled: Boolean get() { nativeRevision;return nativeStore.enabled(session?.userId) }
    val nativeSourceConfigured: Boolean get() { nativeRevision;return nativeStore.hasKey(session?.userId) }
    fun toggleNativeSources() {
        val owner=session?.userId ?: run { error=tr("Inicia sesión primero","Sign in first");return }
        if(!nativeSourceEnabled && nativeStore.token(owner).isBlank()) { error=tr("Guarda primero tu API key de TorBox","Save your TorBox API key first");return }
        nativeStore.enable(owner,!nativeSourceEnabled);nativeRevision++;nativeSources.invalidate();invalidatePlaybackWork()
    }
    suspend fun saveNativeSourceKey(value: String) {
        val owner=session?.userId ?: error(tr("Inicia sesión primero","Sign in first"))
        if(nativeSourceSaving) return
        nativeSourceSaving=true;nativeSourceStatus=""
        try {
            val token=value.trim();require(token.length in 16..256 && token.none { it.isWhitespace() }) { tr("API key inválida","Invalid API key") }
            nativeSources.validate(token)
            if(session?.userId!=owner) return
            nativeStore.save(owner,token);nativeRevision++;nativeSources.invalidate();invalidatePlaybackWork()
            nativeSourceStatus=tr("Clave verificada y guardada. Puedes activar el motor.","Key verified and saved. You can enable the engine.")
        } finally { nativeSourceSaving=false }
    }
    fun clearNativeSourceKey() {
        session?.userId?.let { nativeStore.clear(it) };nativeRevision++;nativeSources.invalidate();invalidatePlaybackWork();nativeSourceStatus=""
    }
    private data class PlaybackCacheKey(
        val owner: String?,val profile: String,val cloudId: String,val imdb: String,
        val season: Int,val episode: Int,val year: String,val title: String,
        val manifests: List<String>,val nativeEnabled: Boolean,val nativeVersion: Int,
        val sizeLimit: Long,val audio: String
    )
    private val playbackLinkCache=com.miflix.native2.data.PlaybackLinkCache<PlaybackCacheKey,List<StreamChoice>>(12) {
        android.os.SystemClock.elapsedRealtime()
    }
    private val playbackCacheMutex=Mutex()
    private fun playbackCacheKey(item: MediaSummary,imdb: String,season: Int,episode: Int)=PlaybackCacheKey(
        session?.userId,activeProfile.id,item.cloudId,imdb,season,episode,item.year,item.title,
        streamRepo.manifests().toList(),nativeSourceEnabled,nativeRevision,streamSizeBytes,audioLanguage)
    private suspend fun resolvePlaybackSources(item: MediaSummary,imdb: String,season: Int,episode: Int): List<StreamChoice> = playbackCacheMutex.withLock {
        val key=playbackCacheKey(item,imdb,season,episode)
        playbackLinkCache.get(key)?.let { return@withLock it }
        val streams=fetchPlaybackSources(item,imdb,season,episode)
        // Cache successful lists only; account/settings changes cannot publish stale results.
        if(streams.isNotEmpty() && key==playbackCacheKey(item,imdb,season,episode)) playbackLinkCache.put(key,streams)
        streams
    }
    private suspend fun fetchPlaybackSources(item: MediaSummary,imdb: String,season: Int,episode: Int): List<StreamChoice> = supervisorScope {
        val owner=session?.userId
        val native=owner!=null && nativeStore.enabled(owner)
        // Disabled is the default and leaves the existing add-on path unchanged.
        if(!native) return@supervisorScope StreamOrdering.sorted(
            streamRepo.resolve(imdb,item.type,season,episode,item.year,streamSizeBytes,item.title,audioLanguage), audioLanguage)
        val token=nativeStore.token(owner)
        val addon=async {
            if(streamRepo.manifests().isEmpty()) emptyList() else streamRepo.resolve(imdb,item.type,season,episode,item.year,streamSizeBytes,item.title,audioLanguage)
        }
        var failure: String?=null
        val builtIn=try {
            check(token.isNotBlank()) { tr("Vuelve a guardar tu API key de TorBox","Save your TorBox API key again") }
            val detail=details?.takeIf { selected?.cloudId==item.cloudId } ?: tmdb.details(item)
            kotlinx.coroutines.withTimeout(25000) { nativeSources.search(owner!!,token,listOf(detail.originalTitle,
                if(com.miflix.native2.data.StreamLanguagePriority.prefersSpanish(audioLanguage))
                    try {tmdb.spanishTitle(item).ifBlank {item.title}} catch(e: CancellationException) {throw e} catch(_: Exception) {item.title}
                else item.title),item.type,item.year,season,episode,streamSizeBytes,audioLanguage) }
        } catch(e: kotlinx.coroutines.TimeoutCancellationException) { failure=tr("Motor nativo: tiempo agotado","Native engine: timed out");emptyList() } catch(e: CancellationException) { throw e } catch(e: Exception) { failure=e.message;emptyList() }
        val links=StreamOrdering.sorted((addon.await()+builtIn).filter { StreamFilters.allowed(it,item.type,item.year,streamSizeBytes,item.title) }, audioLanguage)
        if(session?.userId==owner) nativeSourceStatus=failure ?: tr("Motor nativo: ","Native engine: ")+builtIn.size+tr(" enlaces disponibles"," available links")
        if(links.isEmpty() && failure!=null) throw IllegalStateException(failure)
        links
    }
    val addonManifests=mutableStateListOf<String>()
    private fun setAccountManifests(values: List<String>) {
        invalidatePlaybackWork()
        streamRepo.setManifests(values)
        addonManifests.clear(); addonManifests.addAll(streamRepo.manifests())
    }

    private var currentScreen by mutableStateOf(Screen.SPLASH)
    var screen: Screen
        get()=currentScreen
        set(value) {
            currentScreen=value
            if(value==Screen.FRIENDS) refreshFriendsTab()
        }
    var friendsTabLoading by mutableStateOf(false)
        private set
    private var friendsTabPending=false
    fun refreshFriendsTab() {
        friendsTabPending=true
        if(friendsTabLoading) return
        friendsTabLoading=true
        launch {
            try {
                while(friendsTabPending) {
                    friendsTabPending=false
                    socialAction { refreshFriendsDirectory() }
                }
            } finally { friendsTabLoading=false }
        }
    }
    var franchiseBrand by mutableStateOf("marvel")
    var franchiseType by mutableStateOf("movie")
    val franchiseItems=mutableStateListOf<MediaSummary>()
    var franchiseLoading by mutableStateOf(false)
    var franchiseError by mutableStateOf("")
    var franchiseMore by mutableStateOf(false)
    var franchisePage=0
    var franchiseScrollIndex=0
    var franchiseScrollOffset=0
    suspend fun openFranchise(brand: String) {
        if(franchiseLoading) return
        franchiseBrand=brand;franchiseType="movie";franchiseItems.clear();franchisePage=0
        franchiseMore=false;franchiseError=""
        franchiseScrollIndex=0;franchiseScrollOffset=0;screen=Screen.FRANCHISE
        loadFranchise("movie")
    }
    suspend fun loadFranchise(type: String,more: Boolean=false) {
        if(franchiseLoading) return
        franchiseLoading=true;franchiseError=""
        try {
            val page=if(more) franchisePage+1 else 1
            val result=tmdb.franchiseCatalog(franchiseBrand,type,page)
            if(!more) { franchiseItems.clear();franchiseScrollIndex=0;franchiseScrollOffset=0 }
            franchiseItems.addAll(result.items.filter { row -> franchiseItems.none { it.cloudId==row.cloudId } })
            franchiseType=type;franchiseMore=result.hasMore;franchisePage=page
        } catch(e: CancellationException) { throw e } catch(e: Exception) { franchiseError=e.message.orEmpty() }
        finally { franchiseLoading=false }
    }
    var customFilter by mutableStateOf(CustomCatalogFilter())
    var appliedCustomFilter by mutableStateOf<CustomCatalogFilter?>(null)
    val customFilterResults=mutableStateListOf<MediaSummary>()
    var customFilterLoading by mutableStateOf(false)
    var customFilterError by mutableStateOf("")
    var customFilterMore by mutableStateOf(false)
    var customFilterPage=0
    var customFilterScrollIndex=0
    var customFilterScrollOffset=0
    suspend fun applyCustomFilter(filter: CustomCatalogFilter,more: Boolean=false) {
        if(customFilterLoading) return
        customFilterLoading=true;customFilterError=""
        try {
            filter.validate()
            val page=if(more) customFilterPage+1 else 1
            val result=tmdb.customCatalog(filter,page)
            if(!more) { customFilterResults.clear();customFilterScrollIndex=0;customFilterScrollOffset=0 }
            customFilterResults.addAll(result.items.filter { row -> customFilterResults.none { it.cloudId==row.cloudId } })
            customFilter=filter;appliedCustomFilter=filter;customFilterPage=page;customFilterMore=result.hasMore
        } catch(e: CancellationException) { throw e } catch(e: Exception) { customFilterError=e.message.orEmpty() }
        finally { customFilterLoading=false }
    }
    var appForeground by mutableStateOf(false)
    var sessionNeedsSignIn by mutableStateOf(false)
    var session by mutableStateOf(local.loadSession())
    private fun jwt(token: String): org.json.JSONObject = runCatching {
        org.json.JSONObject(String(android.util.Base64.decode(token.split('.')[1],android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP),Charsets.UTF_8))
    }.getOrDefault(org.json.JSONObject())
    private val sessionRenewal=SessionRenewal({ session },{ session=it;local.saveSession(it) },
        { jwt(it.accessToken).optLong("exp",0)*1000L<=System.currentTimeMillis()+90000L },{ cloud.refresh(it) })
    init {
        Http.authorize={ url,headers,force ->
            val root=BuildConfig.SUPABASE_URL.trimEnd('/')
            val bearer=headers["Authorization"]?.removePrefix("Bearer ")
            if(bearer==null || !(url.startsWith("$root/rest/v1/") || url.startsWith("$root/functions/v1/") || url.startsWith("$root/storage/v1/"))) headers
            else {
                val original=jwt(bearer);val owner=original.optString("sub")
                val current=session ?: throw CancellationException("Signed out")
                if(owner!=current.userId || original.optString("session_id")!=jwt(current.accessToken).optString("session_id")) throw CancellationException("Account changed")
                val valid=try { sessionRenewal.valid(owner,if(force) bearer else null) }
                catch(e: com.miflix.native2.data.SessionSignInRequired) {
                    if(!sessionNeedsSignIn) {
                        sessionNeedsSignIn=true
                        error=tr("Tu sesión ya no es válida. Ve a Settings, cierra sesión y vuelve a iniciarla.","Your session is no longer valid. Open Settings, sign out and sign in again.")
                    }
                    throw e
                }
                headers+("Authorization" to "Bearer ${valid.accessToken}")
            }
        }
    }

    var activeProfile by mutableStateOf(Profile("default", "Main", primary = true))
    val profiles = mutableStateListOf<Profile>()
    val favorites = mutableStateListOf<String>()
    var myListLimit by mutableStateOf(25)
    var myListLoading by mutableStateOf(false)
    var myListMessage by mutableStateOf("")
    val myListTitles=mutableStateListOf<MediaSummary>()
    private var myListOwner=""
    private var myListGeneration=0
    private val myListCache=mutableMapOf<String,MediaSummary>()
    suspend fun loadMyListPage(owner: String,ids: List<String>) = coroutineScope {
        if(myListOwner!=owner) { myListOwner=owner;myListLimit=25;myListCache.clear();myListTitles.clear() }
        val generation=++myListGeneration
        myListLoading=true;myListMessage=""
        val desired=ids.take(myListLimit);val permits=Semaphore(4)
        try {
            val rows=desired.map { id -> async { myListCache[id] ?: permits.withPermit {
                try { tmdb.byCloudId(id) } catch(e: CancellationException) { throw e } catch(_: Exception) { null }
            } } }.awaitAll().filterNotNull()
            if(myListOwner!=owner || generation!=myListGeneration || owner!="${session?.userId}:${activeProfile.id}") return@coroutineScope
            rows.forEach { myListCache[it.cloudId]=it };myListCache.keys.retainAll(favorites.toSet())
            val visible=if(kidsMode) tmdb.kidsFilter(rows) else rows
            if(myListOwner!=owner || generation!=myListGeneration || owner!="${session?.userId}:${activeProfile.id}") return@coroutineScope
            myListTitles.clear();myListTitles.addAll(visible)
            if(rows.size<desired.size) myListMessage=tr("Algunos títulos no pudieron cargar.","Some titles could not load.")
        } finally { if(generation==myListGeneration) myListLoading=false }
    }

    val progress = mutableStateMapOf<String, PlaybackProgress>()

    val genreCovers=mutableStateMapOf<String,String>()
    val directors=mutableStateListOf(
        DirectorCollection("nolan","Christopher Nolan",listOf("Christopher Nolan")),
        DirectorCollection("russo","Joe & Anthony Russo",listOf("Joe Russo","Anthony Russo")),
        DirectorCollection("spielberg","Steven Spielberg",listOf("Steven Spielberg")),
        DirectorCollection("villeneuve","Denis Villeneuve",listOf("Denis Villeneuve")),
        DirectorCollection("scorsese","Martin Scorsese",listOf("Martin Scorsese")),
        DirectorCollection("tarantino","Quentin Tarantino",listOf("Quentin Tarantino")),
        DirectorCollection("gerwig","Greta Gerwig",listOf("Greta Gerwig"))
    )
    var directorTitle by mutableStateOf("")
    val directorItems=mutableStateListOf<MediaSummary>()
    private var loadingDiscovery=false
    suspend fun loadDiscoveryArt() {
        if(loadingDiscovery || tmdb.token.isBlank()) return
        loadingDiscovery=true
        val permits=Semaphore(4)
        try {
            if(genreCovers.size==genres.size && genreCovers.values.filter {it.isNotBlank()}.distinct().size==genreCovers.values.count {it.isNotBlank()}) return
            val candidates=supervisorScope {
                genres.map {genre ->async {
                    val rows=try {permits.withPermit {tmdb.discoverMovieGenre(genre.movieGenreId,pages=1,limit=20)}}
                        catch(e:CancellationException) {throw e} catch(_:Exception) {emptyList()}
                    genre.id to rows
                }}.awaitAll()
            }
            // Assign after all requests complete: stable genre order, no parallel duplicate-selection race.
            val usedTitles=mutableSetOf<String>();val usedImages=mutableSetOf<String>()
            candidates.forEach { (id,rows) ->
                val candidate=rows.firstOrNull {item ->
                    val image=item.backdrop ?: item.poster
                    image!=null && item.cloudId !in usedTitles && image.substringAfterLast('/') !in usedImages
                }
                val image=candidate?.let {it.backdrop ?: it.poster}
                if(candidate!=null && image!=null) {
                    usedTitles.add(candidate.cloudId);usedImages.add(image.substringAfterLast('/'));genreCovers[id]=image
                } else genreCovers[id]=""
            }
        } finally {loadingDiscovery=false}
    }

    suspend fun openDirector(group: DirectorCollection) {
        busyMessage="${tr("Cargando","Loading")} ${group.title}…"
        try {
            val people=if(group.people.size==group.names.size) group.people else group.names.mapNotNull { tmdb.directorPerson(it) }
            require(people.size==group.names.size) { tr("No se pudo encontrar este director. Intenta de nuevo.","Could not find this director. Try again.") }
            val rows=tmdb.directedMovies(people)
            directorTitle=if(group.key=="russo") tr("Hermanos Russo","Russo Brothers") else group.title; directorItems.clear(); directorItems.addAll(rows)
            screen=Screen.DIRECTOR_DETAIL
        } finally { busyMessage=null }
    }

    val seasonalItems = mutableStateListOf<MediaSummary>()
    var seasonalTheme by mutableStateOf("discovery")
        private set
    private val seasonalMutex=Mutex()
    private var seasonalKey=""
    private var seasonalRetryAt=0L
    suspend fun loadSeasonalProgramming() = seasonalMutex.withLock {
        if(tmdb.token.isBlank()) return@withLock
        val date=java.util.Calendar.getInstance()
        val theme=SeasonalProgramming.theme(date.get(java.util.Calendar.MONTH)+1,date.get(java.util.Calendar.DAY_OF_MONTH))
        val language=tmdb.language
        val key="$language:${date.get(java.util.Calendar.YEAR)}:${date.get(java.util.Calendar.DAY_OF_YEAR)}:$theme"
        if(key==seasonalKey || System.currentTimeMillis()<seasonalRetryAt) return@withLock
        if(theme!=seasonalTheme) seasonalItems.clear()
        seasonalTheme=theme
        try {
            val rows=tmdb.seasonal(theme)
            // A language switch may finish while the request is in flight.
            if(language==tmdb.language) {
                val offset=if(rows.isEmpty()) 0 else date.get(java.util.Calendar.DAY_OF_YEAR)%rows.size
                seasonalItems.replaceWith(rows.drop(offset)+rows.take(offset))
                seasonalKey=key
            }
        } catch(e: CancellationException) { throw e }
        catch(_: Exception) { seasonalRetryAt=System.currentTimeMillis()+5*60*1000L }
    }

    val trending = mutableStateListOf<MediaSummary>()
    val movies = mutableStateListOf<MediaSummary>()
    val series = mutableStateListOf<MediaSummary>()
    val topRated = mutableStateListOf<MediaSummary>()
    val newMovies=mutableStateListOf<MediaSummary>()
    val bestSeries=mutableStateListOf<MediaSummary>()
    val comedy=mutableStateListOf<MediaSummary>()
    val thrillers=mutableStateListOf<MediaSummary>()
    var randomChoosing by mutableStateOf(false)

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
    var miniPlayer by mutableStateOf(false)
    val multiViewRequests = mutableStateListOf<PlayerRequest>()
    var parkedPlaybackId by mutableStateOf<String?>(null)
        private set
    fun isPlaybackParked(req: PlayerRequest)=parkedPlaybackId==req.playbackId
    fun unparkPlayback(req: PlayerRequest) {if(parkedPlaybackId==req.playbackId) parkedPlaybackId=null}
    /** Retain the actual exit position, not the position from the original play request. */
    fun parkPlayback(req: PlayerRequest, position: Long): Boolean {
        val current=playerRequest ?: return false
        if(current.playbackId!=req.playbackId) return false
        val newlyParked=parkedPlaybackId!=req.playbackId
        parkedPlaybackId=req.playbackId
        playerRequest=current.copy(resumeMs=if(req.live) 0L else position.coerceAtLeast(0L))
        if(partyRole==PartyRole.HOST) watchParty?.takeIf {it.cloudId==req.item.cloudId && it.season==req.season && it.episode==req.episode}?.let {
            watchParty=it.copy(positionMs=position.coerceAtLeast(0L),playing=false,updatedAt=System.currentTimeMillis())
        }
        return newlyParked
    }
    fun returnToPlayer() {
        val old=playerRequest ?: return
        val parked=isPlaybackParked(old)
        val next=if(miniPlayer) old else old.copy(playbackId=java.util.UUID.randomUUID().toString())
        playerRequest=next
        parkedPlaybackId=if(parked) next.playbackId else null
        selected=next.item;miniPlayer=false;screen=Screen.PLAYER
        // Restore the episode picker after browsing another title, without delaying the return.
        launch {runCatching {
            val info=tmdb.details(next.item)
            if(playerRequest?.playbackId!=next.playbackId || selected?.cloudId!=next.item.cloudId) return@runCatching
            details=info
            if(next.item.type=="series") {
                val rows=tmdb.season(next.item.id,next.season.coerceAtLeast(1))
                if(playerRequest?.playbackId==next.playbackId && selected?.cloudId==next.item.cloudId) {
                    currentSeason=next.season.coerceAtLeast(1);episodes.replaceWith(rows)
                }
            }
        }}
    }

    var lidomTeamId by mutableStateOf("")
    var lidomShowSources by mutableStateOf(false)
    var lidomConfigure by mutableStateOf(false)
    fun showLidomSources(req: PlayerRequest) {
        lidomTeamId=req.lidomTeam;lidomShowSources=true;miniPlayer=false;screen=Screen.LIDOM
    }
    fun markLidomSourceWorking(req: PlayerRequest) {
        if(req.liveProvider=="lidom" && playerRequest?.playbackId==req.playbackId && req.lidomTeam.isNotBlank())
            com.miflix.native2.data.LidomRepository(appContext,"${session?.userId ?: "local"}:${activeProfile.id}").working(req.lidomTeam,req.lidomSource)
    }

    fun openLivePlayer(req: PlayerRequest) {
        require(!kidsMode) {tr("TV en vivo no está disponible en Kids.","Live TV is not available in Kids.") }
        invalidatePlaybackWork()
        if(req.liveProvider.isNotBlank() && req.liveProvider!="lidom" && liveProviderTab!=req.liveProvider) { liveProviderTab=req.liveProvider;liveCategoryId=null }
        miniPlayer=false;selected=req.item;playerRequest=req;screen=Screen.PLAYER
    }
    fun beginMultiView(req: PlayerRequest) {
        require(!kidsMode) {"Multiview is not available in Kids."}
        require(req.live)
        if(req.liveProvider.isNotBlank() && req.liveProvider!="lidom") liveProviderTab=req.liveProvider
        miniPlayer=false;multiViewRequests.clear();multiViewRequests.add(req)
        playerRequest=null;screen=Screen.MULTIVIEW
    }
    var liveSearch by mutableStateOf("")
    var liveProviderTab by mutableStateOf("nauta")
    var liveSports: Boolean
        get()=liveProviderTab=="sports"
        set(value) { liveProviderTab=if(value) "sports" else "nauta" }
    var liveCategoryId by mutableStateOf<String?>(null)
    fun searchHistoryOwner()="search_history_${session?.userId ?: "local"}_${activeProfile.id}"
    fun searchHistory(): List<String> = runCatching {
        val a=org.json.JSONArray(playbackPrefs.getString(searchHistoryOwner(),"[]"))
        (0 until a.length()).map { a.getString(it) }.take(10)
    }.getOrDefault(emptyList())
    fun rememberSearch(query: String) {
        val next=com.miflix.native2.data.SearchHistory.remember(searchHistory(),query)
        playbackPrefs.edit().putString(searchHistoryOwner(),org.json.JSONArray(next).toString()).apply()
    }
    var searchQuery by mutableStateOf("")
    var playerRequest by mutableStateOf<PlayerRequest?>(null)
    internal var playbackLoading by mutableStateOf<PlaybackLoading?>(null)
        private set
    fun beginPlaybackLoading(item: MediaSummary,season: Int=0,episode: Int=0,art: String?=null) {
        playbackLoading=PlaybackLoading(item,season,episode,art ?: episodes.firstOrNull {it.season==season && it.episode==episode}?.still ?: item.backdrop ?: item.poster)
    }
    fun abortPlaybackLoading() {playbackLoading=null}
    fun cancelPlaybackStart() {invalidatePlaybackWork();playerRequest=null;miniPlayer=false;screen=if(selected!=null) Screen.DETAILS else Screen.HOME}
    fun finishPlaybackLoading(id: String) {if(playbackLoading?.playbackId==id) playbackLoading=null}
    var busyMessage by mutableStateOf<String?>(null)
    var error by mutableStateOf<String?>(null)

    var updateInfo by mutableStateOf<UpdateInfo?>(null)
    var updateDownloading by mutableStateOf(false)
    var updateBytes by mutableStateOf(0L)
    var updateTotal by mutableStateOf(0L)
    var downloadedUpdate by mutableStateOf<java.io.File?>(null)
    var updateStatus by mutableStateOf("")
    private var updateDownloadJob: kotlinx.coroutines.Job?=null
    var updateChecking by mutableStateOf(false)

    var pairingRequest by mutableStateOf<PairingRequest?>(null)
    var pairingStatus by mutableStateOf("")

    var partyFollowHost by mutableStateOf(true)
    var watchParty by mutableStateOf<WatchParty?>(null)
    var partyRole by mutableStateOf<PartyRole?>(null)
    var partyStatus by mutableStateOf("")

    val genres get() = listOf(
        GenreDefinition("action", tr("Acción","Action"), 28, 10759, "https://raw.githubusercontent.com/rrevanth/nuvio-assets/main/genres/action/action-landscape.png"),
        GenreDefinition("adventure", tr("Aventura","Adventure"), 12, 10759),
        GenreDefinition("animation", tr("Animación","Animation"), 16, 16),
        GenreDefinition("comedy", tr("Comedia","Comedy"), 35, 35),
        GenreDefinition("crime", tr("Crimen","Crime"), 80, 80),
        GenreDefinition("documentary", tr("Documental","Documentary"), 99, 99),
        GenreDefinition("drama", tr("Drama","Drama"), 18, 18),
        GenreDefinition("family", tr("Familia","Family"), 10751, 10751),
        GenreDefinition("fantasy", tr("Fantasía","Fantasy"), 14, 10765),
        GenreDefinition("history", tr("Historia","History"), 36, 36),
        GenreDefinition("horror", tr("Terror","Horror"), 27, 9648),
        GenreDefinition("mystery", tr("Misterio","Mystery"), 9648, 9648),
        GenreDefinition("romance", tr("Romance","Romance"), 10749, 10749),
        GenreDefinition("scifi", tr("Ciencia ficción","Science Fiction"), 878, 10765, "https://raw.githubusercontent.com/rrevanth/nuvio-assets/main/genres/sci-fi/sci-fi-landscape.png"),
        GenreDefinition("thriller", tr("Suspenso","Thriller"), 53, 10768),
        GenreDefinition("war", tr("Guerra","War"), 10752, 10768)
    )

    private val providers = mapOf(
        "netflix" to listOf(8, 175),
        "disney" to listOf(337),
        "prime" to listOf(9, 119),
        "apple" to listOf(350),
        "hbo" to listOf(1899, 384),
        "hulu" to listOf(15),
        "crunchyroll" to listOf(283)
    )

    fun launch(block: suspend CoroutineScope.() -> Unit) = appScope.launch {
        try { block() } catch(e: CancellationException) { throw e } catch(e: Exception) {playbackLoading=null; error=e.message ?: tr("No se pudo completar la operación","Could not complete the operation") }
    }

    val kidsMode: Boolean get()=activeProfile.isKids || activeProfile.id==KidsContentPolicy.PROFILE_ID
    val kidsMovies=mutableStateListOf<MediaSummary>()
    val kidsSeries=mutableStateListOf<MediaSummary>()
    val kidsContinue=mutableStateListOf<MediaSummary>()
    var kidsLoading by mutableStateOf(false)
        private set
    var kidsMessage by mutableStateOf("")
        private set
    private val kidsCatalogMutex=Mutex()
    private var kidsCatalogKey=""
    private var kidsCatalogAt=0L
    suspend fun loadKidsHome()=kidsCatalogMutex.withLock {
        if(!kidsMode || tmdb.token.isBlank()) return@withLock
        val owner=ratingsKey();val language=tmdb.language
        val key="$owner:$language"
        if(key==kidsCatalogKey && System.currentTimeMillis()-kidsCatalogAt<6*60*60*1000L) return@withLock
        kidsLoading=true;kidsMessage=""
        try {
            coroutineScope {
                val films=async {tmdb.kidsCatalog(false)};val shows=async {tmdb.kidsCatalog(true)}
                val movieRows=films.await();val showRows=shows.await()
                if(kidsMode && owner==ratingsKey() && language==tmdb.language) {
                    kidsMovies.replaceWith(movieRows);kidsSeries.replaceWith(showRows)
                    kidsCatalogKey=key;kidsCatalogAt=System.currentTimeMillis()
                    if(movieRows.isEmpty() && showRows.isEmpty()) kidsMessage=tr("No hay títulos infantiles verificados disponibles.","No verified kids titles are available.")
                }
            }
        } catch(e: CancellationException) {throw e}
        catch(_: Exception) {kidsMessage=tr("No pudimos cargar Kids. Intenta de nuevo.","Could not load Kids. Try again.")}
        finally {kidsLoading=false}
    }
    suspend fun loadKidsContinue() {
        val owner=ratingsKey()
        val rows=tmdb.kidsFilter(resolveContinueWatching())
        if(kidsMode && owner==ratingsKey()) kidsContinue.replaceWith(rows)
    }
    private fun ensureKidsProfile() {
        if(profiles.none {it.isKids || it.id==KidsContentPolicy.PROFILE_ID}) {
            profiles.add(Profile(KidsContentPolicy.PROFILE_ID,"Kids","ai:fox",isKids=true))
        }
    }

    private fun loadOfflineProfiles() {
        profiles.clear(); profiles.addAll(local.offlineProfiles().ifEmpty { listOf(Profile("default","Main",primary=true)) })
        ensureKidsProfile();local.saveOfflineProfiles(profiles.toList())
        activeProfile=profiles.firstOrNull { it.id==local.profileId() } ?: profiles.first()
    }
    suspend fun bootstrap() {
        if(session==null) loadOfflineProfiles()
        session?.let { s ->
            runCatching { sessionRenewal.valid(s.userId) }
            runCatching { syncFromCloud() }
        }
        if(profiles.isEmpty()) profiles.add(Profile("default","Main",primary=true))
        ensureKidsProfile()
        if (tmdb.token.isNotBlank()) {
            runCatching { loadHome() }.onFailure { error = it.message }
        }
        session?.let { account ->
            runCatching { trakt.call(account,"status").optBoolean("connected") }.onSuccess { traktConnected=it }
            runCatching { nickname = social.nickname(account) }
            val room = socialPrefs.getString("room_${account.userId}", "").orEmpty()
            if (room.isNotBlank() && !kidsMode) runCatching {
                cloud.readWatchParty(account, room)?.let { party ->
                    watchParty = party; partyRole = if (party.hostUserId == account.userId) PartyRole.HOST else PartyRole.GUEST
                    social.join(account, room)
                }
            }
        }
        screen = if (tmdb.token.isBlank()) Screen.SETTINGS else if(rememberLastProfile) Screen.HOME else Screen.PROFILES
    }

    suspend fun login(email: String, password: String) {
        busyMessage = tr("Iniciando sesión…","Signing in…")
        error = null
        runCatching { cloud.login(email, password) }.onSuccess {
            setAccountManifests(emptyList())
            sourceSelection=null
            playerRequest=null
            detachParty()
            calendarEvents.clear()
            session = it
            local.saveSession(it)
            syncFromCloud()
            loadHome()
            screen = Screen.PROFILES
        }.onFailure { error = it.message }
            .also { busyMessage = null }
    }

    suspend fun syncFromCloud() {
        val s = session ?: return
        setAccountManifests(emptyList())
        val (remoteProfiles, remoteSetup) = cloud.account(s)
        if(session?.userId!=s.userId) return

        val chosenTmdb = remoteSetup.tmdbToken.ifBlank { tmdb.token }
        // Account data is authoritative; never inherit an APK, device or previous user's add-on.
        val chosenManifests = (remoteSetup.addonManifests + remoteSetup.torrentioManifest)
            .filter { it.isNotBlank() }.distinct()
        if (chosenTmdb.isNotBlank()) tmdb.token = chosenTmdb
        setAccountManifests(chosenManifests)

        profiles.clear()
        profiles.addAll(if (remoteProfiles.isEmpty()) listOf(Profile("default", "Main", primary = true)) else remoteProfiles)
        val neededKids=profiles.none {it.isKids || it.id==KidsContentPolicy.PROFILE_ID}
        ensureKidsProfile()
        if (remoteProfiles.isEmpty() || neededKids) runCatching { cloud.upsertProfiles(s, profiles.toList()) }
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
        invalidatePlaybackWork()
        playerRequest=null;sourceSelection=null;miniPlayer=false;selected=null;details=null;profilePhotoEditor=null
        favorites.clear();progress.clear();continueWatching.clear();kidsContinue.clear();notifications.clear()
        activeProfile = p
        randomEpisodeCloud=null
        contextTitle=null;socialProfileEditor=false;localCastPairing=false
        if(kidsMode) detachParty()
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
        val next=profiles.toList()+p
        session?.let {
            cloud.upsertProfiles(it, next)
            cloud.upsertProfile(it, p.id, emptySet(), emptyMap())
        }
        profiles.add(p)
        if(session==null) local.saveOfflineProfiles(profiles.toList())
    }
    private val avatarRepo=AvatarRepository(BuildConfig.SUPABASE_URL,BuildConfig.SUPABASE_KEY)
    var avatarUploadProfile by mutableStateOf<Profile?>(null)
    var avatarUploadRequest by mutableStateOf<PairingRequest?>(null)
    var avatarUploadStatus by mutableStateOf("")
    private var avatarUploadEpoch=0L
    suspend fun startAvatarUpload(profile: Profile) {
        val s=session ?: error(tr("Inicia sesión para subir un avatar","Sign in to upload an avatar"))
        closeAvatarUpload()
        val epoch=avatarUploadEpoch
        avatarUploadProfile=profile; avatarUploadStatus=tr("Preparando QR…","Preparing QR…")
        val request=pairingRepo.create(avatar=true)
        if(avatarUploadEpoch!=epoch || session?.userId!=s.userId) { pairingRepo.finish(request); return }
        avatarUploadRequest=request; avatarUploadStatus=tr("Escanea y elige una foto · QR válido por 10 minutos","Scan and choose a photo · QR valid for 10 minutes")
    }
    fun closeAvatarUpload() {
        avatarUploadEpoch++
        avatarUploadRequest?.let { request -> launch { pairingRepo.finish(request) } }
        avatarUploadRequest=null; avatarUploadProfile=null; avatarUploadStatus=""
    }
    suspend fun pollAvatarUpload(): Boolean {
        val request=avatarUploadRequest ?: return false; val profile=avatarUploadProfile ?: return false
        val s=session ?: return false; val epoch=avatarUploadEpoch
        fun valid()=epoch==avatarUploadEpoch && session?.userId==s.userId && profiles.any { it.id==profile.id }
        val payload=pairingRepo.poll(request) ?: return false
        if(!valid()) return true
        require(payload.avatarData.isNotBlank()) { tr("No se recibió una imagen","No image received") }
        avatarUploadStatus=tr("Guardando avatar…","Saving avatar…")
        val url=avatarRepo.upload(s,profile.id,payload.avatarData)
        if(!valid()) { avatarRepo.remove(s,url); return true }
        try {
            val current=profiles.first { it.id==profile.id }
            setProfileAvatar(current,url)
        } catch(e: Exception) { runCatching { avatarRepo.remove(s,url) }; throw e }
        pairingRepo.finish(request)
        if(valid()) { avatarUploadRequest=null; avatarUploadStatus=tr("Avatar actualizado","Avatar updated") }
        return true
    }

    var profilePhotoEditor by mutableStateOf<Profile?>(null)
    suspend fun setProfileAvatar(profile: Profile, avatar: String) {
        require(avatar=="none" || ProfileAvatars.any { it.key==avatar } || characterAvatar(avatar)!=null || session?.let { avatarRepo.owns(avatar,it.userId) }==true) { tr("Elige un avatar disponible","Choose an available avatar") }
        val owner=session
        val changed=profile.copy(avatarValue=avatar)
        val next=profiles.map { if(it.id==profile.id) changed else it }
        owner?.let { cloud.upsertProfiles(it,next) }
        if(session?.userId!=owner?.userId || profiles.none { it.id==profile.id }) return
        profiles.clear(); profiles.addAll(next)
        if(activeProfile.id==profile.id) activeProfile=changed
        if(session==null) local.saveOfflineProfiles(next)
        if(owner!=null && profile.avatarValue!=avatar) profile.avatarValue?.let { old -> runCatching { avatarRepo.remove(owner,old) } }
    }

    suspend fun renameProfile(profile: Profile, name: String) {
        val clean=name.trim().replace(Regex("\\s+")," ")
        require(clean.isNotBlank() && clean.length<=40) { tr("Escribe un nombre de 1 a 40 caracteres","Enter a name of 1–40 characters") }
        val owner=session
        val next=profiles.map { if(it.id==profile.id) it.copy(name=clean) else it }
        require(next.any { it.id==profile.id }) { tr("El perfil ya no existe","This profile no longer exists") }
        owner?.let { cloud.upsertProfiles(it,next) }
        if(session?.userId!=owner?.userId || profiles.none { it.id==profile.id }) return
        profiles.clear(); profiles.addAll(next)
        if(activeProfile.id==profile.id) activeProfile=next.first { it.id==profile.id }
        if(owner==null) local.saveOfflineProfiles(next)
    }

    suspend fun deleteProfile(profile: Profile) {
        require(!profile.isKids && profile.id!=KidsContentPolicy.PROFILE_ID) {tr("El perfil Kids se conserva.","The Kids profile is kept by default.") }
        if(avatarUploadProfile?.id==profile.id) closeAvatarUpload()
        require(profiles.size>1) { tr("Conserva al menos un perfil","Keep at least one profile") }
        val owner=session
        var next=profiles.filterNot { it.id==profile.id }
        if(next.none { it.primary }) next=next.mapIndexed { i,p -> if(i==0) p.copy(primary=true) else p }
        owner?.let { next=cloud.deleteProfile(it,profile.id) }
        playbackPrefs.edit().remove("ratings_${owner?.userId ?: "local"}_${profile.id}").remove("notices_${owner?.userId ?: "local"}_${profile.id}").remove("known_${owner?.userId ?: "local"}_${profile.id}").apply()
        profiles.clear(); profiles.addAll(next)
        if(owner==null) local.saveOfflineProfiles(next)
        if(activeProfile.id==profile.id) { notifications.clear(); selectProfile(next.firstOrNull { it.primary } ?: next.first()) }
        if(owner!=null) profile.avatarValue?.let { old -> runCatching { avatarRepo.remove(owner,old) } }
    }

    private val profileSyncMutex = kotlinx.coroutines.sync.Mutex()
    suspend fun pushCloud() {
        profileSyncMutex.lock()
        try {
        val owner = session ?: return
        val profile = activeProfile.id
        val snapshot = favorites.toSet()
        val server = cloud.upsertProfile(owner, profile, snapshot, progress.toMap())
        if (session?.userId == owner.userId && activeProfile.id == profile) {
            val current = favorites.toSet()
            val merged = (server + (current - snapshot)) - (snapshot - current)
            if (merged != current) { favorites.clear(); favorites.addAll(merged) }
        }
        } finally { profileSyncMutex.unlock() }
    }

    suspend fun addAddonManifest(url: String) {
        val owner=session ?: throw IllegalStateException(tr("Inicia sesión para guardar tus complementos","Sign in to save your add-ons"))
        val clean=url.trim()
        if(clean.isBlank()) return
        require(clean.startsWith("https://") || clean.startsWith("http://")) { tr("Usa una URL HTTP válida","Use a valid HTTP URL") }
        val normalized=if(clean.endsWith("/manifest.json")) clean else clean.trimEnd('/')+"/manifest.json"
        val next=(streamRepo.manifests()+normalized).distinct()
        cloud.upsertPrivateSetup(owner,PrivateSetup(tmdb.token,next.firstOrNull().orEmpty(),next))
        if(session?.userId==owner.userId) setAccountManifests(next)
    }
    suspend fun removeAddonManifest(manifest: String) {
        val owner=session ?: return
        val next=streamRepo.manifests().filterNot { it==manifest }
        cloud.upsertPrivateSetup(owner,PrivateSetup(tmdb.token,next.firstOrNull().orEmpty(),next))
        if(session?.userId==owner.userId) setAccountManifests(next)
    }

    suspend fun loadHome() = supervisorScope {
        if(kidsMode) return@supervisorScope
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
        if (kidsMode || extrasLoaded || extrasLoading || tmdb.token.isBlank()) return
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
                val newJob=async { runCatching { tmdb.nowPlayingMovies() } }
                val bestTvJob=async { runCatching { tmdb.topRatedSeries() } }
                val comedyJob=async { runCatching { tmdb.discoverMovieGenre(35,pages=1,limit=20) } }
                val thrillerJob=async { runCatching { tmdb.discoverMovieGenre(53,pages=1,limit=20) } }

                actionJob.await().onSuccess { action.replaceWith(it) }
                sciFiJob.await().onSuccess { sciFi.replaceWith(it) }
                netflixJob.await().onSuccess { netflix.replaceWith(it) }
                disneyJob.await().onSuccess { disney.replaceWith(it) }
                primeJob.await().onSuccess { prime.replaceWith(it) }
                appleJob.await().onSuccess { apple.replaceWith(it) }
                hboJob.await().onSuccess { hbo.replaceWith(it) }
                continueJob.await().onSuccess { continueWatching.replaceWith(it) }
                newJob.await().onSuccess { newMovies.replaceWith(it) }
                bestTvJob.await().onSuccess { bestSeries.replaceWith(it) }
                comedyJob.await().onSuccess { comedy.replaceWith(it) }
                thrillerJob.await().onSuccess { thrillers.replaceWith(it) }
            }
            extrasLoaded = true
        } finally {
            extrasLoading = false
        }
    }

    suspend fun openCollection(id: String, title: String) {
        if(id=="natgeo") {
            busyMessage=tr("Cargando National Geographic…","Loading National Geographic…")
            try { collectionItems.replaceWith(tmdb.nationalGeographic());collectionTitle="National Geographic";screen=Screen.COLLECTION_DETAIL }
            finally { busyMessage=null }
            return
        }
        if(id.startsWith("series-year:") || id.startsWith("documentary-year:")) {
            openYear(id.substringAfter(':').toIntOrNull() ?: return,if(id.startsWith("series")) "series" else "documentary")
            return
        }
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
        busyMessage = "${tr("Cargando","Loading")} $title…"
        platformId = id
        platformTitle = title
        platformSections.clear()
        error = null
        try {
            val top = async { tmdb.providerTop10(providerIds) }
            val moviesJob = async { tmdb.discoverProviderType(providerIds, "movie", pages = 2, limit = 40) }
            val seriesJob = async { tmdb.discoverProviderType(providerIds, "series", pages = 2, limit = 40) }
            platformSections += CatalogSection("top10", "Top 10 ${tr("en","on")} $title", top.await(), landscape = true)
            platformSections += CatalogSection("movies", tr("Películas","Movies"), moviesJob.await())
            platformSections += CatalogSection("series", tr("Series","Series"), seriesJob.await())
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
                                CatalogSection("${g.id}-latest", "${g.title} · ${tr("Estrenos","Latest")}", (latestMovies.await() + latestTv.await()).distinctBy { it.cloudId }.sortedByDescending { it.year }.take(20)),
                                CatalogSection("${g.id}-rated", "${g.title} · ${tr("Mejor valorados","Best Rated")}", (bestMovies.await() + bestTv.await()).distinctBy { it.cloudId }.sortedByDescending { it.rating }.take(20))
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

    var genreLoading by mutableStateOf(false)
        private set
    var genreHasMore by mutableStateOf(true)
        private set
    var genreMessage by mutableStateOf("")
        private set
    var genreReturnScreen by mutableStateOf(Screen.COLLECTIONS)
        private set
    private var homeShelfSeed=""
    private var homeShelfTheme=""
    suspend fun openHomeShelf(key: String,title: String,rows: List<MediaSummary>) {
        if(kidsMode) return
        genreGeneration++;currentGenre=GenreDefinition("home:$key",title,0,0);genreTitle=if(key=="ranked_movies") tr("Más películas populares","More popular movies") else title
        genreReturnScreen=Screen.HOME
        homeShelfSeed=discoveryHistoryKey.split("|").firstOrNull {it.isNotBlank()}.orEmpty()
        homeShelfTheme=seasonalTheme
        genreItems.clear();genreBacklog.clear();genrePageNumber=0
        genreBacklog.offer(rows) {it.cloudId}
        genreRemoteMore=tmdb.pageableHomeShelf(key);genreHasMore=true;genreMessage=""
        screen=Screen.GENRE_DETAIL
        loadMoreGenre()
    }
    val genreUpcoming:Boolean get()=currentGenre?.id=="home:coming_soon_home"
    private var currentGenre: GenreDefinition?=null
    private var genrePageNumber=0
    private var genreRemoteMore=true
    private var genreGeneration=0L
    private val genreBacklog=UniquePageBuffer<MediaSummary>()
    private val genreMutex=Mutex()
    val christmasCollectionPicks=mutableStateListOf<MediaSummary>()
    var christmasCollectionLoading by mutableStateOf(false)
        private set
    var christmasCollectionMessage by mutableStateOf("")
        private set
    private var christmasCollectionOwner=""
    private val christmasCollectionMutex=Mutex()
    suspend fun loadChristmasCollection()=christmasCollectionMutex.withLock {
        val owner="${session?.userId}:${activeProfile.id}:${tmdb.language}"
        if(kidsMode || (christmasCollectionOwner==owner && christmasCollectionPicks.isNotEmpty())) return@withLock
        christmasCollectionLoading=true
        christmasCollectionMessage=""
        if(christmasCollectionOwner!=owner) christmasCollectionPicks.clear()
        try {
            val rows=tmdb.christmasPage(1).rows
            if(owner=="${session?.userId}:${activeProfile.id}:${tmdb.language}" && !kidsMode) {
                christmasCollectionPicks.replaceWith(rows)
                christmasCollectionOwner=owner
                if(rows.isEmpty()) christmasCollectionMessage=tr("No se encontraron títulos.","No titles found.")
            }
        } catch(e: CancellationException) {
            throw e
        } catch(e: Exception) {
            christmasCollectionMessage=tr("No se pudo cargar Navidad. Reintenta.","Could not load Christmas Movies. Try again.")
        } finally {
            christmasCollectionLoading=false
        }
    }
    suspend fun openChristmasCollection() {
        openGenre(GenreDefinition("christmas",tr("Películas navideñas · Mejor valoradas","Christmas Movies · Best rated"),0,0))
    }
    suspend fun openGenre(g: GenreDefinition) {
        if(kidsMode) return
        genreGeneration++;currentGenre=g;genreTitle=g.title;genreReturnScreen=Screen.COLLECTIONS
        genreItems.clear();genreBacklog.clear();genrePageNumber=0;genreRemoteMore=true;genreHasMore=true
        genreMessage="";screen=Screen.GENRE_DETAIL
        loadMoreGenre()
    }
    suspend fun loadMoreGenre()=genreMutex.withLock {
        val g=currentGenre ?: return@withLock
        if(kidsMode || !genreHasMore) return@withLock
        val generation=genreGeneration;val language=tmdb.language;val owner="${session?.userId}:${activeProfile.id}"
        genreLoading=true;genreMessage=""
        fun current()=generation==genreGeneration && language==tmdb.language && owner=="${session?.userId}:${activeProfile.id}" && !kidsMode
        try {
            var batches=0
            while(genreBacklog.pendingCount()<25 && genreRemoteMore && batches<6) {
                val page=genrePageNumber+1
                val slice=when {
                    g.id=="christmas" -> tmdb.christmasPage(page)
                    g.id.startsWith("home:") -> tmdb.homeShelfPage(g.id.substringAfter(":"),page,homeShelfSeed,homeShelfTheme)
                    else -> tmdb.genrePage(g.movieGenreId,g.tvGenreId,page)
                }
                if(!current()) return@withLock
                genreBacklog.offer(slice.rows) {it.cloudId}
                genrePageNumber=page;genreRemoteMore=slice.hasMore;batches++
            }
            if(!current()) return@withLock
            val next=genreBacklog.take(25)
            genreItems.addAll(next)
            genreHasMore=genreBacklog.pendingCount()>0 || genreRemoteMore
        } catch(e: CancellationException) {throw e}
        catch(e: Exception) {if(current()) {
            genreMessage=e.message ?: tr("No se pudo cargar.","Could not load.")
            // Keep already loaded titles usable when the next remote page fails.
            genreItems.addAll(genreBacklog.take(25))
            genreHasMore=genreBacklog.pendingCount()>0 || genreRemoteMore
        }}
        finally {if(generation==genreGeneration) genreLoading=false}
    }

    suspend fun openYear(year: Int, category: String="movie") {
        busyMessage = "${tr("Cargando películas de","Loading movies from")} $year…"
        error = null
        runCatching { tmdb.catalogByYear(year,category) }
            .onSuccess {
                yearTitle = (when(category) { "series" -> tr("Series","Series"); "documentary" -> tr("Documentales","Documentaries"); else -> tr("Películas","Movies") })+" · $year"
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

    var detailsReturnScreen by mutableStateOf(Screen.HOME)
        private set
    var selectedUpcoming by mutableStateOf(false)
        private set
    private var detailsEpoch=0L
    private val detailsCache=linkedMapOf<String,Pair<Long,MediaDetails>>()
    suspend fun open(item: MediaSummary,upcoming: Boolean=false) {
        if(kidsMode && !tmdb.kidsAllowed(item)) {error=tr("Este título no está disponible en Kids.","This title is not available in Kids.");return}
        val ticket=++detailsEpoch
        val language=tmdb.language
        val cacheKey="$language:${item.cloudId}"
        if(screen!=Screen.DETAILS && screen!=Screen.PLAYER) detailsReturnScreen=screen
        selected=item
        selectedUpcoming=upcoming || (comingMovies+comingSeries).any { it.cloudId==item.cloudId }
        details=detailsCache[cacheKey]?.takeIf { System.currentTimeMillis()-it.first<15*60*1000L }?.second
        episodes.clear();currentSeason=1
        screen=Screen.DETAILS
        try {
            val loaded=details ?: tmdb.details(item)
            if(ticket!=detailsEpoch || selected?.cloudId!=item.cloudId || language!=tmdb.language) return
            if(detailsCache.size>=12) detailsCache.remove(detailsCache.keys.first())
            detailsCache[cacheKey]=System.currentTimeMillis() to loaded
            details=loaded
            val today=java.text.SimpleDateFormat("yyyy-MM-dd",Locale.US).format(java.util.Date())
            if(loaded.releaseDate.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))  ) selectedUpcoming=loaded.releaseDate>today
            if(item.type=="series" && loaded.seasonCount>0) loadSeason(1)
        } catch(e: CancellationException) { throw e }
        catch(e: Exception) { if(ticket==detailsEpoch) error=e.message }
    }

    fun remindMe(item: MediaSummary) {
        if(!favorites.contains(item.cloudId)) favorites.add(0,item.cloudId)
        val key=knownKey()
        val known=org.json.JSONObject(playbackPrefs.getString(key,"{}"))
        if(!known.has(item.cloudId)) known.put(item.cloudId,"pending")
        playbackPrefs.edit().putString(key,known.toString()).apply()
    }

    suspend fun loadSeason(season: Int) {
        val item = selected ?: return
        if(season < 1 || season > (details?.seasonCount ?: 0)) return
        currentSeason = season
        episodes.clear()
        try {
            val rows = tmdb.season(item.id, season)
            if(selected?.cloudId == item.cloudId && currentSeason == season) episodes.replaceWith(rows)
        } catch(e: CancellationException) { throw e }
        catch(e: Exception) { if(selected?.cloudId == item.cloudId && currentSeason == season) error=e.message }
    }

    fun toggleFavorite(item: MediaSummary) {
        if (favorites.contains(item.cloudId)) favorites.remove(item.cloudId) else favorites.add(0,item.cloudId)
    }

    var randomEpisodeCloud by mutableStateOf<String?>(null)
        private set
    var randomEpisodeBusy by mutableStateOf(false)
        private set
    fun stopRandomEpisodes() {randomEpisodeCloud=null}
    suspend fun playRandomEpisode(item: MediaSummary) {
        if(randomEpisodeBusy) return
        check(partyRole!=PartyRole.GUEST) {tr("El host elige el episodio.","The host chooses the episode.")}
        randomEpisodeBusy=true
        val owner=activeProfile.id;val account=session?.userId;val generation=playbackWork.generation
        beginPlaybackLoading(item)
        try {
            val previous=playerRequest?.takeIf {it.item.cloudId==item.cloudId}?.let {it.season to it.episode}
            val episode=tmdb.randomEpisode(item,previous)
            if(owner!=activeProfile.id || account!=session?.userId || generation!=playbackWork.generation) return
            if(selected?.cloudId!=item.cloudId || details?.summary?.cloudId!=item.cloudId) open(item)
            if(owner!=activeProfile.id || account!=session?.userId || generation!=playbackWork.generation) return
            check(selected?.cloudId==item.cloudId && details?.summary?.cloudId==item.cloudId) {tr("Serie no disponible","Series unavailable")}
            randomEpisodeCloud=item.cloudId
            playEpisode(episode,true)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if(owner==activeProfile.id && account==session?.userId) {
                randomEpisodeCloud=null
                playbackLoading=null
            }
            throw e
        } finally {
            randomEpisodeBusy=false
        }
    }

    suspend fun playRandomMovie() {
        if(randomChoosing) return
        if(partyRole==PartyRole.GUEST) { error=tr("El host elige el contenido de la sala.","The host chooses the room's content.");return }
        randomChoosing=true
        val owner=ratingsKey(); val generation=playbackWork.generation
        try {
            busyMessage=tr("Buscando una película para ti…","Choosing a movie for you…")
            val seedIds=(ratings.filterValues { it>=6 }.entries.sortedByDescending { it.value }.map { it.key }
                +progress.entries.filter { it.value.percent>=10 }.sortedByDescending { it.value.updatedAt }.map { it.key }
                +favorites).filter { it.startsWith("tmdb:movie:") }.distinct().take(4)
            val seeds=seedIds.mapNotNull { runCatching { tmdb.byCloudId(it) }.getOrNull() }
            val recommendations=if(forYou.any { it.type=="movie" }) { forYou.filter { it.type=="movie" } } else {
                supervisorScope { seeds.take(3).map { seed -> async { runCatching { tmdb.recommendations(seed) }.getOrDefault(emptyList()) } }.awaitAll().flatten() }
            }
            if(owner!=ratingsKey() || generation!=playbackWork.generation) return
            val all=(recommendations+newMovies+topRated+movies+action+sciFi+comedy+thrillers).distinctBy { it.cloudId }
            val target=seeds.map { it.rating }.filter { it>0 }.takeIf { it.isNotEmpty() }?.average()
            val historyKey="random_$owner"
            val stored=org.json.JSONArray(playbackPrefs.getString(historyKey,"[]"))
            val recent=(0 until stored.length()).map { stored.getString(it) }
            val excluded=(progress.filterValues { it.percent>=80 }.keys+ratings.filterValues { it<=4 }.keys+seedIds).toSet()
            val pool=RandomMovieSelector.pool(all,recommendations.map { it.cloudId }.toSet(),target,excluded,recent.toSet())
            var chosen: Pair<MediaSummary,MediaDetails>?=null
            val today=java.text.SimpleDateFormat("yyyy-MM-dd",Locale.US).format(java.util.Date())
            for(candidate in pool.shuffled().take(5)) {
                val detail=try { tmdb.details(candidate) } catch(e: CancellationException) { throw e } catch(_: Exception) { continue }
                if(detail.imdbId.isNullOrBlank()) continue
                if(detail.releaseInfo.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) && detail.releaseInfo>today) continue
                chosen=candidate to detail
                break
            }
            val selectedMovie=chosen ?: error(tr("No encontré una película disponible. Intenta otra vez.","No available movie found. Try again."))
            if(owner!=ratingsKey() || generation!=playbackWork.generation) return
            val (movie,detail)=selectedMovie
            playbackPrefs.edit().putString(historyKey,org.json.JSONArray((listOf(movie.cloudId)+recent).distinct().take(20)).toString()).apply()
            selected=movie;details=detail
            busyMessage=null
            playResolved(movie,detail.imdbId!!,0,0,resumeOverride=0)
        } catch(e: CancellationException) { throw e } catch(e: Exception) { if(generation==playbackWork.generation) error=e.message } finally { randomChoosing=false; if(generation==playbackWork.generation) busyMessage=null }
    }

    suspend fun playMovie(fromStart: Boolean=false) {
        val item = selected ?: return
        beginPlaybackLoading(item)
        val generation=playbackWork.generation
        val d = details ?:  tmdb.details(item).also { details = it }
        if(generation!=playbackWork.generation || selected?.cloudId!=item.cloudId) return
        val imdb = d.imdbId ?: throw IllegalStateException(tr("IMDb ID no disponible","IMDb ID unavailable"))
        playResolved(item, imdb, 0, 0,if(fromStart) 0L else null)
    }

    suspend fun playEpisode(ep: EpisodeSummary, fromStart: Boolean=false) {
        val item = selected ?: return
        beginPlaybackLoading(item,ep.season,ep.episode,ep.still)
        val generation=playbackWork.generation
        val d = details ?:  tmdb.details(item).also { details = it }
        if(generation!=playbackWork.generation || selected?.cloudId!=item.cloudId) return
        val imdb = d.imdbId ?: throw IllegalStateException(tr("IMDb ID no disponible","IMDb ID unavailable"))
        playResolved(item, imdb, ep.season, ep.episode,if(fromStart) 0L else null)
    }

    suspend fun switchPlayerEpisode(ep: EpisodeSummary) {
        val current = playerRequest
        if (current != null) updateProgress(current, current.resumeMs, 0)
        playEpisode(ep)
    }

    suspend fun resumeSeries() {
        val item = selected ?: return
        val d = details ?: tmdb.details(item).also { details = it }
        val imdb = d.imdbId ?: throw IllegalStateException(tr("IMDb ID no disponible","IMDb ID unavailable"))
        val p = progress[item.cloudId]
        val season = p?.season?.takeIf { it > 0 } ?: 1
        val episode = p?.episode?.takeIf { it > 0 } ?: 1
        playResolved(item, imdb, season, episode)
    }

    suspend fun playSeriesFromStart() {
        val item = selected ?: return
        val d = details ?: tmdb.details(item).also { details = it }
        val imdb = d.imdbId ?: throw IllegalStateException(tr("IMDb ID no disponible","IMDb ID unavailable"))
        playResolved(item, imdb, 1, 1,resumeOverride=0)
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

    private suspend fun chooseSources(item: MediaSummary, imdb: String, season: Int, episode: Int, resumeMs: Long? = null) {
        val ticket=playbackWork.begin(); val owner=session?.userId; val profile=activeProfile.id
        fun valid()=playbackWork.current(ticket) && session?.userId==owner && activeProfile.id==profile
        sourceSelection=null; busyMessage=tr("Buscando todos los enlaces…","Finding all playback links…"); error=null
        try {
            val links=resolvePlaybackSources(item,imdb,season,episode)
            if(!valid()) return
            if(links.isEmpty()) error=tr("No hay enlaces que cumplan los filtros. Revisa Size Limit en Settings.","No links match your filters. Check Size Limit in Settings.")
            else sourceSelection=SourceSelection(item,imdb,season,episode,links,resumeMs,ticket.generation,ticket.sequence)
        } catch(e: CancellationException) { throw e } catch(e: Exception) { if(valid()) error=e.message } finally { if(valid()) busyMessage=null }
    }

    suspend fun refreshPlaybackLinks(selection: SourceSelection) {
        val ticket=PlaybackWorkGate.Ticket(selection.workGeneration,selection.workSequence)
        if(sourceSelection!=selection || !playbackWork.current(ticket)) return
        playbackCacheMutex.withLock {
            if(sourceSelection!=selection || !playbackWork.current(ticket)) return
            playbackLinkCache.remove(playbackCacheKey(selection.item,selection.imdb,selection.season,selection.episode))
            nativeSources.invalidate()
        }
        if(sourceSelection==selection && playbackWork.current(ticket))
            chooseSources(selection.item,selection.imdb,selection.season,selection.episode,selection.resumeMs)
    }

    suspend fun playSelectedSource(selection: SourceSelection, stream: StreamChoice) {
        val ticket=PlaybackWorkGate.Ticket(selection.workGeneration,selection.workSequence)
        if(sourceSelection!=selection || !playbackWork.current(ticket)) return
        if(stream !in selection.streams || !StreamFilters.allowed(stream,selection.item.type,selection.item.year,streamSizeBytes,selection.item.title)) return
        sourceSelection=null
        if(partyRole==PartyRole.GUEST) {
            val remote=refreshParty() ?: return
            if(!playbackWork.current(ticket)) return
            if(remote.cloudId!=selection.item.cloudId || remote.season!=selection.season || remote.episode!=selection.episode) {
                error=tr("El host cambió de contenido. Abre Playback links nuevamente.","The host changed content. Open Playback links again."); switchToPartyState(remote); return
            }
        }
        val moment=playerRequest?.takeIf {it.momentEndMs>0 && it.item.cloudId==selection.item.cloudId &&
            it.season==selection.season && it.episode==selection.episode}
        if(playbackWork.current(ticket)) playResolved(selection.item,selection.imdb,selection.season,selection.episode,selection.resumeMs,stream,fallbackStreams=selection.streams,
            onStarted={ req -> if(moment!=null) playerRequest=req.copy(momentStartMs=moment.momentStartMs,momentEndMs=moment.momentEndMs,momentSender=moment.momentSender) })
    }

    private val searchResultCache=linkedMapOf<String,Pair<Long,List<MediaSummary>>>()
    fun tasteAspects(item:MediaSummary):Set<String> = playbackPrefs.getStringSet("taste:${ratingsKey()}:${item.cloudId}",emptySet()).orEmpty().toSet()
    fun saveTasteAspects(item:MediaSummary,traits:Set<String>) {
        val owner=ratingsKey()
        playbackPrefs.edit().putStringSet("taste:$owner:${item.cloudId}",traits).putString("taste_recent:$owner",item.cloudId).apply()
    }
    private val refinedTasteCache=linkedMapOf<String,Pair<Long,List<MediaSummary>>>()
    private suspend fun refinedTastePicks():List<MediaSummary> {
        val owner=ratingsKey();val locale=tmdb.language
        val seedId=playbackPrefs.getString("taste_recent:$owner","").orEmpty()
        val traits=playbackPrefs.getStringSet("taste:$owner:$seedId",emptySet()).orEmpty().toSet()
        if(seedId.isBlank() || traits.isEmpty()) return emptyList()
        val key="$owner:$locale:$seedId:${traits.sorted().joinToString()}"
        refinedTasteCache[key]?.takeIf {System.currentTimeMillis()-it.first<30*60*1000L}?.let {return it.second}
        val rows=withTimeoutOrNull(12_000) {
            val seed=tmdb.byCloudId(seedId) ?: return@withTimeoutOrNull emptyList<MediaSummary>()
            tmdb.tasteMatches(seed,traits,6)
        } ?: return emptyList()
        if(refinedTasteCache.size>=6) refinedTasteCache.remove(refinedTasteCache.keys.first())
        refinedTasteCache[key]=System.currentTimeMillis() to rows
        return rows
    }
    suspend fun findTasteMatches(item:MediaSummary,traits:Set<String>) {
        val owner=ratingsKey();val locale=tmdb.language
        val rows=tmdb.tasteMatches(item,traits)
        if(owner!=ratingsKey() || locale!=tmdb.language) return
        if(rows.isEmpty()) {error=tr("No hay coincidencias verificables con esos aspectos. Prueba otro aspecto.","No metadata matches for these aspects. Try another aspect.");return}
        forYou.clear();forYou.addAll(rows)
        openHomeShelf("taste",tr("Más como ","More like ")+item.title,rows)
    }

    fun savedCinemaJourney()=playbackPrefs.getString("journey:${ratingsKey()}","").orEmpty()
    fun saveCinemaJourney(value:String) {playbackPrefs.edit().putString("journey:${ratingsKey()}",value).apply()}
    fun cinemaJourneyChronological()=playbackPrefs.getBoolean("journey_order:${ratingsKey()}",false)
    fun saveCinemaJourneyChronological(value:Boolean) {playbackPrefs.edit().putBoolean("journey_order:${ratingsKey()}",value).apply()}

    var discoveryPreviewActive by mutableStateOf(false)
        internal set

    private fun discoverySeen(key:String):List<String> {
        val a=runCatching {org.json.JSONArray(playbackPrefs.getString("discovery_seen:${ratingsKey()}:$key","[]"))}.getOrDefault(org.json.JSONArray())
        return (0 until a.length()).map {a.getString(it)}
    }
    fun discoveryOrder(key:String,rows:List<MediaSummary>):List<MediaSummary> {
        val history=discoverySeen(key);val seen=history.toSet();val unique=rows.distinctBy {it.cloudId}
        return unique.filter {it.cloudId !in seen}.shuffled()+unique.filter {it.cloudId in seen}.sortedByDescending {history.indexOf(it.cloudId)}
    }
    fun recordDiscovery(key:String,item:MediaSummary) {
        val ids=(listOf(item.cloudId)+discoverySeen(key)).distinct().take(500)
        playbackPrefs.edit().putString("discovery_seen:${ratingsKey()}:$key",org.json.JSONArray(ids).toString()).apply()
    }
    fun freshDiscoveryRows(key:String,rows:List<MediaSummary>):List<MediaSummary> {
        val ordered=discoveryOrder(key,rows)
        val unseen=ordered.filter {it.cloudId !in discoverySeen(key).toSet()}
        val selected=(if(unseen.isNotEmpty()) unseen else ordered).take(25)
        val ids=(selected.map {it.cloudId}+discoverySeen(key)).distinct().take(500)
        playbackPrefs.edit().putString("discovery_seen:${ratingsKey()}:$key",org.json.JSONArray(ids).toString()).apply()
        return selected
    }
    suspend fun discoveryRun(mood:String):List<MediaSummary> {
        val key="mood_page:${ratingsKey()}:$mood"
        val page=playbackPrefs.getInt(key,1)
        playbackPrefs.edit().putInt(key,if(page>=19) 1 else page+2).apply()
        suspend fun fetch(p:Int):List<MediaSummary> = when(mood) {
            "christmas" ->tmdb.christmasPage(p).rows+tmdb.christmasPage(p+1).rows
            "halloween" ->tmdb.homeShelfPage("horror_movies",p,"","").rows+tmdb.homeShelfPage("horror_movies",p+1,"","").rows
            else ->tmdb.moodPicks(mood,p)
        }
        val rows=fetch(page).ifEmpty {fetch(1)}
        return if(kidsMode) tmdb.kidsFilter(rows) else rows
    }

    suspend fun discoveryPicks(mood: String): List<MediaSummary> {
        val rows=tmdb.moodPicks(mood)
        val allowed=if(kidsMode || mood=="family") tmdb.kidsFilter(rows) else rows
        // Only reorder within the selected mood; quick-watch duration remains a hard filter.
        val personal=forYou.map {it.cloudId}.toSet()
        return allowed.sortedWith(compareBy<MediaSummary> {ratings[it.cloudId]?.let {score ->score<=4} ?: false}
            .thenBy {progress[it.cloudId]?.percent?.let {percent ->percent>=80} ?: false}
            .thenByDescending {it.cloudId in personal}.thenByDescending {it.rating})
    }

    suspend fun surpriseDiscovery(rows: List<MediaSummary>) {
        if(randomChoosing || rows.isEmpty()) return
        if(partyRole==PartyRole.GUEST) {error=tr("El host elige el contenido.","The host chooses the content.");return}
        randomChoosing=true
        val owner=activeProfile.id;val account=session?.userId;val locale=tmdb.language
        val ticket=playbackWork.begin()
        fun valid()=owner==activeProfile.id && account==session?.userId && locale==tmdb.language && playbackWork.current(ticket)
        try {
            val fresh=rows.filter { (progress[it.cloudId]?.percent ?: 0.0)<80 && (ratings[it.cloudId] ?: 10)>4 }
            val item=(fresh.ifEmpty {rows}).shuffled().first()
            if(kidsMode && !tmdb.kidsAllowed(item)) return
            if(!valid()) return
            val season=if(item.type=="series") 1 else 0
            beginPlaybackLoading(item,season,season)
            busyMessage=tr("Eligiendo una historia para ti…","Choosing a story for you…")
            val loaded=detailsCache["$locale:${item.cloudId}"]?.takeIf {System.currentTimeMillis()-it.first<15*60*1000L}?.second ?: tmdb.details(item)
            if(!valid()) return
            val imdb=loaded.imdbId ?: throw IllegalStateException(tr("IMDb ID no disponible","IMDb ID unavailable"))
            selected=item;details=loaded;selectedUpcoming=false
            busyMessage=null
            // Go straight to playback preparation; do not flash the details destination.
            playResolved(item,imdb,season,season,resumeOverride=0)
        } catch(e: CancellationException) {throw e} catch(e: Exception) {if(valid()) error=e.message}
        finally {randomChoosing=false;if(valid()) {busyMessage=null;playbackLoading=null}}
    }

    fun localSearch(query: String): List<MediaSummary> {
        val normalized=FlexibleSearch.normalize(query)
        if(normalized.length<2) return emptyList()
        val rows=if(kidsMode) kidsMovies+kidsSeries else trending+movies+series+topRated+continueWatching+forYou+collectionItems+directorItems+myListTitles+comingMovies+comingSeries
        return FlexibleSearch.rank(rows.distinctBy {it.cloudId}.filter {FlexibleSearch.score(it.title,normalized)>=650},normalized).take(30)
    }
    suspend fun searchFlexible(query: String): List<MediaSummary> = supervisorScope {
        TasteQuery.parse(query)?.let {request ->
            val seed=FlexibleSearch.rank(tmdb.search(request[0]),request[0]).firstOrNull() ?: return@supervisorScope emptyList()
            val matches=tmdb.tasteMatches(seed,setOf(request[1]))
            return@supervisorScope if(kidsMode) tmdb.kidsFilter(matches) else matches
        }
        val intent=DiscoveryMood.intent(query)
        if(intent.isNotBlank()) return@supervisorScope discoveryPicks(intent)
        val normalized=FlexibleSearch.normalize(query)
        if(normalized.length<2) return@supervisorScope emptyList()
        val cacheKey="${tmdb.language}:$kidsMode:$normalized"
        searchResultCache[cacheKey]?.takeIf {System.currentTimeMillis()-it.first<5*60*1000L}?.let {return@supervisorScope it.second}
        val local=localSearch(normalized)
        // Extra queries only when the normal search has no close title match.
        val remote=mutableListOf<MediaSummary>()
        var primaryFailed=false
        try {remote.addAll(tmdb.search(normalized))} catch(e: CancellationException) {throw e} catch(_: Exception) {primaryFailed=true}
        if(!primaryFailed && (local+remote).none {FlexibleSearch.score(it.title,normalized)>=650}) {
            for(variant in FlexibleSearch.variants(normalized)) {
                kotlinx.coroutines.currentCoroutineContext().ensureActive()
                try {remote.addAll(tmdb.search(variant))} catch(e: CancellationException) {throw e} catch(_: Exception) {break}
                if(remote.any {FlexibleSearch.score(it.title,normalized)>=650}) break
            }
        }
        val all=FlexibleSearch.rank(local+remote,normalized).take(if(kidsMode) 20 else 60)
        val ranked=if(kidsMode) tmdb.kidsFilter(all) else all
        if(searchResultCache.size>=12) searchResultCache.remove(searchResultCache.keys.first())
        if(!primaryFailed) searchResultCache[cacheKey]=System.currentTimeMillis() to ranked
        ranked
    }

    private data class SourceFallbackPlan(val owner: String?,val profile: String,val generation: Long,
        val rows: List<StreamChoice>,val budget: com.miflix.native2.player.SourceFallbackBudget,
        var playbackId: String,var busy: Boolean=false)
    private var sourceFallback: SourceFallbackPlan?=null
    private suspend fun materializeAutomaticSource(rows: List<StreamChoice>,budget: com.miflix.native2.player.SourceFallbackBudget,
        owner: String?,valid: () -> Boolean): StreamChoice {
        var index=0
        while(true) {
            kotlinx.coroutines.currentCoroutineContext().ensureActive()
            check(valid()) {"Playback changed"}
            val candidate=rows[index]
            try {
                return if(candidate.nativeSource==null) candidate else {
                    check(nativeStore.enabled(owner)) {tr("El motor nativo está apagado","Native engine is disabled")}
                    withTimeoutOrNull(15_000) {nativeSources.materialize(candidate,nativeStore.token(owner),valid)}
                        ?: throw java.io.IOException("Source preparation timed out")
                }
            } catch(e: CancellationException) {throw e} catch(e: Exception) {
                val next=budget.nextIndex()
                if(next<0) throw e
                index=next
                playbackLoading=playbackLoading?.copy(message=sourceRetryMessage(index,rows.size))
            }
        }
    }
    private fun sourceRetryMessage(index: Int,count: Int): String =
        tr("El enlace falló. Probando el siguiente…","Source failed. Trying the next link…")+" (${index+1}/${count.coerceAtMost(com.miflix.native2.player.SourceFallbackBudget.MAX_ATTEMPTS)})"
    /** Reuses the already loaded list; no add-on search or Supabase read is made here. */
    suspend fun tryNextPlaybackSource(req: PlayerRequest,position: Long,playing: Boolean): Boolean {
        val plan=sourceFallback ?: return false
        fun valid()=sourceFallback===plan && plan.playbackId==req.playbackId && playerRequest?.playbackId==req.playbackId &&
            session?.userId==plan.owner && activeProfile.id==plan.profile && playbackWork.generation==plan.generation &&
            screen==Screen.PLAYER && appForeground && watchParty==null && !req.live
        if(!valid() || plan.busy) return false
        val manager=appContext.getSystemService(android.net.ConnectivityManager::class.java)
        val online=runCatching {manager.getNetworkCapabilities(manager.activeNetwork)?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)!=false}.getOrDefault(true)
        if(!online) return false
        plan.busy=true
        try {
            while(valid()) {
                val index=plan.budget.nextIndex()
                if(index<0) {sourceFallback=null;return false}
                val candidate=plan.rows[index]
                beginPlaybackLoading(req.item,req.season,req.episode)
                playbackLoading=playbackLoading?.copy(message=sourceRetryMessage(index,plan.rows.size))
                val resolved=try {
                    if(candidate.nativeSource==null) candidate else withTimeoutOrNull(15_000) {
                        nativeSources.materialize(candidate,nativeStore.token(plan.owner),::valid)
                    } ?: throw java.io.IOException("Source preparation timed out")
                } catch(e: CancellationException) {throw e} catch(_: Exception) {continue}
                if(!valid()) return false
                val subs=withTimeoutOrNull(50) {playbackSubtitleJobs[subtitleKey(req.item,req.season,req.episode)]?.await()}.orEmpty()
                val next=req.copy(stream=resolved.copy(subtitles=(resolved.subtitles+subs).distinctBy {it.url}),
                    resumeMs=position.coerceAtLeast(0),playbackId=UUID.randomUUID().toString(),sourceFallbackPlaying=playing)
                plan.playbackId=next.playbackId
                // Same title/episode: changing source must not publish an off/on presence pair.
                if(socialReadyPlaybackId==req.playbackId) socialReadyPlaybackId=next.playbackId
                playerRequest=next
                playbackLoading=playbackLoading?.copy(playbackId=next.playbackId)
                return true
            }
            return false
        } finally {
            plan.busy=false
            if(playerRequest?.playbackId==req.playbackId) playbackLoading=null
        }
    }
    fun isSourceFallbackReplacement(req: PlayerRequest): Boolean = playerRequest?.let {
        it.playbackId!=req.playbackId && it.sourceFallbackPlaying!=null && it.item.cloudId==req.item.cloudId &&
            it.season==req.season && it.episode==req.episode
    }==true
    private val playbackSubtitleJobs = mutableMapOf<String, Deferred<List<SubtitleChoice>>>()
    private fun subtitleKey(item: MediaSummary, season: Int, episode: Int) = "${item.cloudId}:$season:$episode"
    suspend fun playbackSubtitles(req: PlayerRequest): List<SubtitleChoice> {
        val rows=playbackSubtitleJobs[subtitleKey(req.item,req.season,req.episode)]?.await().orEmpty()
        if(rows.isNotEmpty()) return rows
        val imdb=details?.takeIf { selected?.cloudId==req.item.cloudId }?.imdbId ?: tmdb.details(req.item).imdbId ?: return emptyList()
        return subtitleRepo.resolve(imdb,req.item.type,req.season,req.episode,req.stream)
    }

    private suspend fun playResolved(item: MediaSummary, imdb: String, season: Int, episode: Int, resumeOverride: Long? = null, selectedStream: StreamChoice? = null, fallbackStreams: List<StreamChoice> = emptyList(), onStarted: ((PlayerRequest) -> Unit)? = null) = coroutineScope {
        if(kidsMode) require(tmdb.kidsAllowed(item)) { tr("Este título no está disponible en Kids.","This title is not available in Kids.") }
        if(randomEpisodeCloud!=item.cloudId) randomEpisodeCloud=null
        val ticket=playbackWork.begin()
        sourceFallback=null
        val playbackOwner=session?.userId; val playbackProfile=activeProfile.id
        fun valid()=playbackWork.current(ticket) && session?.userId==playbackOwner && activeProfile.id==playbackProfile
        if(playbackLoading?.let {it.item.cloudId==item.cloudId && it.season==season && it.episode==episode}!=true) beginPlaybackLoading(item,season,episode)
        busyMessage = tr("Buscando el mejor enlace…","Finding the best source…")
        error = null
        val streamsJob = async { if (selectedStream != null) (listOf(selectedStream)+fallbackStreams).distinctBy {it.url}.filter { StreamFilters.allowed(it,item.type,item.year,streamSizeBytes,item.title) } else resolvePlaybackSources(item,imdb,season,episode) }
        val subtitleKey=subtitleKey(item,season,episode)
        if(playbackSubtitleJobs.size>=16 && subtitleKey !in playbackSubtitleJobs) {
            val oldest=playbackSubtitleJobs.keys.first(); playbackSubtitleJobs.remove(oldest)?.cancel()
        }
        // Subtitle services must not gate opening the video. Keep late results for the CC menu.
        val subsJob = playbackSubtitleJobs.getOrPut(subtitleKey) { appScope.async {
            runCatching { subtitleRepo.resolve(imdb, item.type, season, episode, null) }.getOrDefault(emptyList())
        } }
        runCatching {
            val streams = streamsJob.await()
            if(!valid()) return@coroutineScope
            val bandwidth=if(adaptiveSource) androidx.media3.exoplayer.upstream.DefaultBandwidthMeter.getSingletonInstance(appContext).bitrateEstimate else 0L
            val seconds=(details?.takeIf {it.summary.cloudId==item.cloudId}?.runtimeMinutes ?: 0)*60
            val chosen = selectedStream?.takeIf {it in streams} ?: StreamOrdering.best(streams,audioLanguage,bandwidth,seconds,::audioSourceRank)
                ?: throw IllegalStateException(tr("No se encontraron enlaces reproducibles","No playable sources found"))
            val automaticFallback=watchParty==null
            val retryRows=if(automaticFallback) {
                val candidates=StreamOrdering.fallbackCandidates(streams,audioLanguage,bandwidth,seconds,::audioSourceRank).filter {it.url!=chosen.url}
                val alternatives=if(com.miflix.native2.data.StreamLanguagePriority.prefersSpanish(audioLanguage) && StreamOrdering.languageRank(chosen)==0)
                    candidates.filter {StreamOrdering.languageRank(it)==0}.take(2)+candidates.filter {StreamOrdering.languageRank(it)!=0}
                    else candidates
                (listOf(chosen)+alternatives).distinctBy {it.url}.take(com.miflix.native2.player.SourceFallbackBudget.MAX_ATTEMPTS)
            } else listOf(chosen)
            val budget=com.miflix.native2.player.SourceFallbackBudget(retryRows.size)
            val best=materializeAutomaticSource(retryRows,budget,playbackOwner,::valid)
            val externalSubs = withTimeoutOrNull(800) { subsJob.await() }.orEmpty()
            val mergedSubs = (best.subtitles + externalSubs)
                .distinctBy { it.url }
                .sortedWith(compareBy<SubtitleChoice> {
                    when (it.lang.lowercase(Locale.US)) { "es", "spa" -> 0; "en", "eng" -> 1; else -> 2 }
                }.thenBy { it.label })
            if(!valid()) return@coroutineScope
            val enriched = best.copy(subtitles = mergedSubs)
            val key = if (item.type == "series") "${item.cloudId}:s${season}e${episode}" else item.cloudId
            val saved = progress[key] ?: progress[item.cloudId]
            playerRequest = PlayerRequest(item, enriched, season, episode, resumeOverride ?: saved?.position ?: 0L)
            playbackLoading=playbackLoading?.copy(playbackId=playerRequest?.playbackId)
            screen = Screen.PLAYER
            playerRequest?.let { onStarted?.invoke(it) }
            val req=playerRequest
            if(automaticFallback && req!=null && retryRows.size>1) sourceFallback=SourceFallbackPlan(playbackOwner,playbackProfile,ticket.generation,
                retryRows,budget,req.playbackId)
        }.onFailure { if(it is CancellationException) throw it; if(valid()) {playbackLoading=null;error=it.message} }
        if(valid()) busyMessage=null
    }

    fun markWatched(item: MediaSummary, season: Int = 0, episode: Int = 0) {
        val s=if(item.type=="series") season.takeIf {it>0} ?: progress[item.cloudId]?.season?.takeIf {it>0} ?: 1 else 0
        val e=if(item.type=="series") episode.takeIf {it>0} ?: progress[item.cloudId]?.episode?.takeIf {it>0} ?: 1 else 0
        val key=if(s>0) "${item.cloudId}:s${s}e${e}" else item.cloudId
        val old=progress[key] ?: progress[item.cloudId]
        val duration=old?.duration?.takeIf {it>0} ?: 1L
        val row=PlaybackProgress(100.0,duration,duration,System.currentTimeMillis(),s,e)
        progress[key]=row;progress[item.cloudId]=row
        if(s>0) playbackPrefs.edit().putStringSet(watchedEpisodesKey(),playbackPrefs.getStringSet(watchedEpisodesKey(),emptySet()).orEmpty().toSet()+key).apply()
        continueWatching.removeAll {it.cloudId==item.cloudId};kidsContinue.removeAll {it.cloudId==item.cloudId}
        launch {runCatching {pushCloud()}.onFailure {error=it.message}}
    }
    /** Batch a whole season in one profile sync, without toggling already watched episodes. */
    suspend fun markSeasonWatched(item: MediaSummary, season: Int) {
        if(item.type!="series" || season<1) return
        val account=session;val profile=activeProfile.id
        fun valid()=session?.userId==account?.userId && activeProfile.id==profile
        val today=java.time.LocalDate.now().toString()
        val rows=tmdb.season(item.id,season).filter {it.season==season && it.episode>0 && it.airDate.isNotBlank() && it.airDate<=today}
        if(!valid()) return
        require(rows.isNotEmpty()) {tr("Esta temporada no tiene episodios estrenados.","This season has no released episodes.")}
        profileSyncMutex.lock()
        try {
            if(!valid()) return
            val now=System.currentTimeMillis()
            val watched=playbackPrefs.getStringSet(watchedEpisodesKey(),emptySet()).orEmpty().toMutableSet()
            for(ep in rows) {
                val key="${item.cloudId}:s${season}e${ep.episode}"
                val duration=progress[key]?.duration?.takeIf {it>0} ?: 1000L
                progress[key]=PlaybackProgress(100.0,duration,duration,now,season,ep.episode)
                watched.add(key)
            }
            playbackPrefs.edit().putStringSet(watchedEpisodesKey(),watched).apply()
            val current=progress[item.cloudId]
            if(current==null || current.season==season) {
                val last=rows.maxBy {it.episode}
                progress[item.cloudId]=progress["${item.cloudId}:s${season}e${last.episode}"]!!
                continueWatching.removeAll {it.cloudId==item.cloudId};kidsContinue.removeAll {it.cloudId==item.cloudId}
            }
            if(account!=null) cloud.upsertProfile(account,profile,favorites.toSet(),progress.toMap())
        } finally {profileSyncMutex.unlock()}
    }

    fun removeContinue(item: MediaSummary) {
        // A timestamped zero-progress row survives the cloud merge and hides the title.
        val old=progress[item.cloudId]
        progress[item.cloudId]=PlaybackProgress(0.0,0L,old?.duration ?: 0L,System.currentTimeMillis(),old?.season ?: 0,old?.episode ?: 0)
        continueWatching.removeAll {it.cloudId==item.cloudId};kidsContinue.removeAll {it.cloudId==item.cloudId}
        launch {runCatching {pushCloud()}.onFailure {error=it.message}}
    }
    private fun watchedEpisodesKey()="watched_episodes_${session?.userId ?: "local"}_${activeProfile.id}"
    fun episodeWatched(item: MediaSummary, ep: EpisodeSummary): Boolean {
        val key="${item.cloudId}:s${ep.season}e${ep.episode}"
        val exact=progress[key]
        val last=progress[item.cloudId]?.takeIf { it.season==ep.season && it.episode==ep.episode }
        return (exact?.percent ?: last?.percent ?: 0.0)>=96.0 ||
            playbackPrefs.getStringSet(watchedEpisodesKey(),emptySet()).orEmpty().contains(key)
    }

    private fun isMomentOnly(req: PlayerRequest): Boolean =
        if(playerRequest?.playbackId==req.playbackId) (playerRequest?.momentEndMs ?: 0L)>0 else req.momentEndMs>0
    fun updateProgress(req: PlayerRequest, position: Long, duration: Long, ended: Boolean = false) {
        if(isMomentOnly(req)) return
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
        if(req.item.type=="series" && (row.percent>=96.0 || (progress[key]?.percent ?: 0.0)>=96.0)) {
            val saved=playbackPrefs.getStringSet(watchedEpisodesKey(),emptySet()).orEmpty()
            if(key !in saved) playbackPrefs.edit().putStringSet(watchedEpisodesKey(),saved.toSet()+key).apply()
        }
        progress[key] = row
        progress[req.item.cloudId] = row
        if (ended) continueWatching.removeAll { it.cloudId == req.item.cloudId }
    }

    suspend fun createWatchParty(withCurrentTitle: Boolean = false): WatchParty? {
        val s = session ?: run { error = tr("Inicia sesión antes de crear una sala","Sign in before creating a Watch Party"); return null }
        watchParty?.let { return it }
        val item = if (withCurrentTitle) playerRequest?.item ?: selected else null
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
            liveTitle = if (req?.live == true) req.item.title else "",
            title=item?.title.orEmpty(),overview=item?.overview.orEmpty().take(800),poster=item?.poster,
            logo=details?.takeIf {it.summary.cloudId==item?.cloudId}?.logo
        )
        val epoch=partyEpoch
        return runCatching {
            cloud.createWatchParty(s,party); social.join(s,room)
            if(partyEpoch!=epoch || session?.userId!=s.userId) return@runCatching null
            partyEpoch++; invalidatePlaybackWork(); knownPartyMembers=setOf(s.userId)
            watchParty=party; partyRole=PartyRole.HOST; partyStatus=tr("Sala lista · elige un contenido","Room ready · choose any title")
            rememberRoom(room); lastEventId=-1; party
        }
            .onFailure { error = it.message }.getOrNull()
    }

    suspend fun joinWatchParty(code: String) {
        require(!kidsMode) {tr("Watch Party no está disponible en Kids.","Watch Party is not available in Kids.") }
        val s = session ?: run { error = tr("Inicia sesión antes de unirte a una sala","Sign in before joining a Watch Party"); return }
        require(code.trim().matches(Regex("[0-9]{6}"))) { tr("Usa el código de 6 dígitos","Use the 6-digit code") }
        if(watchParty!=null && watchParty?.roomCode!=code.trim()) { error=tr("Sal del party actual antes de solicitar otra sesión","Leave your current party before requesting another session"); return }
        val epoch=partyEpoch
        val decision=social.requestAccess(s,code.trim())
        if(partyEpoch!=epoch || session?.userId!=s.userId) return
        if(decision!="approved") {
            pendingPartyCode=if(decision=="pending") code.trim() else null
            partyStatus=if(decision=="pending") tr("Solicitud enviada · esperando aprobación del host","Request sent · Waiting for host approval") else tr("El host rechazó la solicitud","The host rejected your request")
            return
        }
        pendingPartyCode=null
        social.join(s, code.trim())
        if(partyEpoch!=epoch || session?.userId!=s.userId) return
        val party = cloud.readWatchParty(s, code.trim()) ?: run { error = tr("No se encontró la sala","Watch Party room not found"); return }
        if(partyEpoch!=epoch || session?.userId!=s.userId) return
        partyEpoch++; invalidatePlaybackWork(); knownPartyMembers=null
        partyFollowHost=true
        watchParty = party
        partyRole = if (party.hostUserId == s.userId) PartyRole.HOST else PartyRole.GUEST
        rememberRoom(party.roomCode)
        lastEventId = -1
        partyStatus = "${tr("Te uniste a","Joined")} ${party.roomCode}"
        if (party.cloudId.isBlank()) { screen = Screen.WATCH_PARTY; return }
        switchToPartyState(party)
    }

    private val partyPublishMutex=Mutex()
    suspend fun hostPartyUpdate(req: PlayerRequest, positionMs: Long, playing: Boolean) = partyPublishMutex.withLock {
        val s = session ?: return@withLock
        if (partyRole != PartyRole.HOST || playerRequest?.playbackId!=req.playbackId) return@withLock
        val current = watchParty ?: return@withLock
        var next = partyPresentation(current,req).copy(
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
        if(next.shareStream && sharedSourcePlaybackId!=req.playbackId) {
            try {cloud.sharePartySource(s,next,req.stream);sharedSourcePlaybackId=req.playbackId}
            catch(e: CancellationException) {throw e}
            catch(_: Exception) {next=next.copy(shareStream=false);error=tr("No se pudo compartir la nueva fuente","Could not share the new source")}
        }
        watchParty = next
        try { cloud.updateWatchParty(s,next) } catch(e: CancellationException) { throw e } catch(e: Exception) { error=e.message }
    }

    /** Publish the exit pause even if the mini player was closed, without overwriting a new title. */
    suspend fun publishParkedParty(req: PlayerRequest, position: Long, roomCode: String) = partyPublishMutex.withLock {
        val account=session ?: return@withLock
        val room=watchParty ?: return@withLock
        if(partyRole!=PartyRole.HOST || room.roomCode!=roomCode || room.hostUserId!=account.userId) return@withLock
        val current=playerRequest
        if(current!=null && (current.playbackId!=req.playbackId || parkedPlaybackId!=req.playbackId)) return@withLock
        if(room.cloudId!=req.item.cloudId || room.season!=req.season || room.episode!=req.episode) return@withLock
        val next=room.copy(positionMs=position.coerceAtLeast(0L),playing=false,updatedAt=System.currentTimeMillis())
        watchParty=next
        cloud.updateWatchParty(account,next)
    }

    suspend fun refreshParty(): WatchParty? {
        val s=session ?: return null; val current=watchParty ?: return null; val epoch=partyEpoch
        val fresh=try { cloud.readWatchParty(s,current.roomCode) } catch(e: CancellationException) { throw e } catch(_: Exception) { null }
        if(!sameParty(epoch,s.userId,current.roomCode)) return null
        return fresh?.also { watchParty=it }
    }

    suspend fun switchToPartyState(remote: WatchParty, openPlayer: Boolean = false) {
        if(kidsMode) return
        val owner=session?.userId ?: return; val epoch=partyEpoch
        fun valid()=sameParty(epoch,owner,remote.roomCode)
        if(!valid() || switchingPartyEpoch==epoch) return
        switchingPartyEpoch=epoch
        try {
            val current=playerRequest
            if(current!=null && current.item.cloudId==remote.cloudId && current.season==remote.season && current.episode==remote.episode) {
                if(openPlayer) {
                    if(screen!=Screen.PLAYER && !miniPlayer) playerRequest=current.copy(resumeMs=partyTargetPosition(remote),playbackId=java.util.UUID.randomUUID().toString())
                    partyFollowHost=true;miniPlayer=false;screen=Screen.PLAYER
                }
                return
            }
            if(openPlayer) {partyFollowHost=true;miniPlayer=false}
            if(remote.cloudId.isBlank()) return
            if(remote.shareStream) {
                val shared=try {cloud.readPartySource(session ?: return,remote)} catch(e: CancellationException) {throw e} catch(_: Exception) {null}
                if(!valid()) return
                if(shared!=null) {
                    val item=if(remote.liveChannelId.isNotBlank()) MediaSummary(remote.liveChannelId.hashCode(),"live",remote.title,remote.overview,null,remote.poster,0.0,"LIVE") else tmdb.byCloudId(remote.cloudId)
                    if(!valid()) return
                    if(item!=null) {
                        selected=item;playerRequest=PlayerRequest(item,shared,remote.season,remote.episode,remote.positionMs,
                            remote.liveChannelId.isNotBlank(),remote.liveChannelId,remote.liveProvider,liveType=remote.liveType)
                        screen=Screen.PLAYER;return
                    }
                }
            }
            if(remote.liveChannelId.isNotBlank()) {
                val manifest=calendarLiveManifest(remote.liveProvider)
                val channel=LiveChannel(remote.liveChannelId,remote.liveType,remote.liveTitle,null,"")
                val stream=LiveRepository().streams(manifest,channel).firstOrNull() ?: return
                if(!valid()) return
                val item=MediaSummary(remote.cloudId.substringAfterLast(':').toIntOrNull() ?: channel.id.hashCode(),"live",channel.name,"",null,null,0.0,"LIVE")
                selected=item; playerRequest=PlayerRequest(item,stream,0,0,0L,true,channel.id,remote.liveProvider,liveType=remote.liveType); screen=Screen.PLAYER
                return
            }
            val item=tmdb.byCloudId(remote.cloudId) ?: return
            if(!valid()) return
            val detail=tmdb.details(item)
            if(!valid()) return
            val eps=if(item.type=="series") tmdb.season(item.id,remote.season.coerceAtLeast(1)) else emptyList()
            if(!valid()) return
            selected=item; details=detail
            if(item.type=="series") { currentSeason=remote.season.coerceAtLeast(1); episodes.clear(); episodes.addAll(eps) }
            val imdb=detail.imdbId ?: return
            playResolved(item,imdb,remote.season,remote.episode,remote.positionMs)
        } finally { if(switchingPartyEpoch==epoch) switchingPartyEpoch=null }
    }

    private fun detachParty() {
        miniPlayer=false
        partyEpoch++; switchingPartyEpoch=null; invalidatePlaybackWork()
        session?.let { socialPrefs.edit().remove("room_${it.userId}").apply() }
        watchParty=null; partyRole=null;partyFollowHost=true; pendingPartyCode=null; accessRequests.clear()
        partyMessages.clear(); floatingEvents.clear(); partyMembers.clear(); lastEventId=-1
        knownPartyMembers=null; partyJoinedNotice=null; joinedNoticeSequence++;sharedSourcePlaybackId=null
        partyPlaybackNotice=null;playbackNoticeSequence++
        partyStatus=""; error=null; partyClosedNotice=false
        if(screen==Screen.PLAYER) screen=if(playerRequest?.live==true) Screen.LIVE_TV else if(selected!=null) Screen.DETAILS else Screen.HOME
        playerRequest=null
    }
    suspend fun leaveWatchParty() {
        val s=session; val p=watchParty; val role=partyRole
        // Release player and pending work immediately, before waiting for the network.
        detachParty()
        if(s!=null && p!=null) {
            if(role==PartyRole.HOST) runCatching { cloud.deleteWatchParty(s,p.roomCode) }.onFailure { if(it is CancellationException) throw it }
            runCatching { social.leave(s,p.roomCode) }.onFailure { if(it is CancellationException) throw it }
        }
    }

    private fun rememberRoom(code: String) { session?.let { socialPrefs.edit().putString("room_${it.userId}", code).apply() } }

    suspend fun refreshAccountSession() {
        val account=session ?: return
        try { sessionRenewal.valid(account.userId) } catch(e: CancellationException) { throw e } catch(_: Exception) { }
    }

    suspend fun socialAction(block: suspend () -> Unit) {
        try { block() } catch (e: Exception) {
            if (e is CancellationException) throw e
            error = e.message
        }
    }
    suspend fun saveNickname(value: String) = socialAction {
        val s = session ?: error(tr("Inicia sesión primero","Sign in first"))
        val nick = value.trim().lowercase(Locale.US)
        social.saveNickname(s, nick); nickname = nick
    }
    fun savePhrases(value: String) {
        val values = value.split("|").map { it.trim().take(120) }.filter { it.isNotEmpty() }.distinct().take(8)
        partyPhrases.clear(); partyPhrases.addAll(values)
        socialPrefs.edit().putString("phrases", values.joinToString("|")).apply()
    }
    suspend fun sendPartyEvent(kind: String, text: String) = socialAction {
        val s = session ?: error(tr("Inicia sesión primero","Sign in first"))
        val room = watchParty?.roomCode ?: error(tr("Únete primero a una sala","Join a room first"))
        social.send(s, room, kind, text)
    }
    suspend fun refreshFriends() = socialAction {
        val s = session ?: return@socialAction
        val name=social.nickname(s)
        val rows=social.friends(s)
        val ids=rows.flatMap { listOf(it.sender,it.receiver) }.distinct()
        val people=social.people(s,ids)
        if(session?.userId!=s.userId) return@socialAction
        nickname=name;friendRows.clear();friendRows.addAll(rows)
        friendPeople.clear();people.forEach { friendPeople[it.id]=it.nickname }
    }
    suspend fun addFriend(id: String) = socialAction {
        val s = session ?: error(tr("Inicia sesión primero","Sign in first"))
        require(id != s.userId) { tr("Esta es tu cuenta","This is your account") }
        val existing = social.friends(s).firstOrNull { it.sender == id || it.receiver == id }
        if (existing?.status == "accepted") return@socialAction
        if (existing?.receiver == s.userId) social.accept(s, id)
        else if (existing == null) social.requestFriend(s, id)
        refreshFriends()
    }
    suspend fun findAndAddFriend(nick: String) = socialAction {
        val s = session ?: error(tr("Inicia sesión primero","Sign in first"))
        val person = social.find(s, nick.trim()) ?: error(tr("No se encontró el apodo","Nickname not found"))
        addFriend(person.id)
    }
    suspend fun pollSocialRoom(refreshMembers: Boolean = true) {
        val s=session ?: return; val p=watchParty ?: return; val epoch=partyEpoch
        fun valid()=sameParty(epoch,s.userId,p.roomCode)
        val fresh=if((screen==Screen.PLAYER || miniPlayer) && !refreshMembers) p else cloud.readWatchParty(s,p.roomCode)
        if(!valid()) return
        if(fresh==null) {
            val wasGuest=partyRole==PartyRole.GUEST
            detachParty()
            partyStatus=tr("El party fue cerrado","The party was closed")
            partyClosedNotice=wasGuest
            return
        }
        if(partyRole==PartyRole.GUEST && screen!=Screen.PLAYER && !miniPlayer) { watchParty=fresh; if(fresh.cloudId.isNotBlank() && playerRequest?.let {isPlaybackParked(it)}!=true) switchToPartyState(fresh) }
        if(!valid()) return
        val members=if(refreshMembers) social.members(s,p.roomCode) else partyMembers.toList()
        if(!valid()) return
        val before=knownPartyMembers
        if(partyRole==PartyRole.HOST && before!=null) members.filter { it.id!=s.userId && it.id !in before }.forEach { showJoined(it.nickname) }
        knownPartyMembers=members.map { it.id }.toSet()
        partyMembers.clear(); partyMembers.addAll(members)
        val events=social.events(s,p.roomCode,lastEventId)
        if(!valid()) return
        if(lastEventId<0) { partyMessages.clear(); partyMessages.addAll(events.filter {it.kind!="playback"}); lastEventId=events.lastOrNull()?.id ?: 0 }
        else {
            for(event in events) {
                lastEventId=event.id
                if(event.kind=="playback") {
                    if(event.userId!=s.userId && PartyPlaybackNoticePolicy.accepts(event.kind,event.body,event.createdAt,System.currentTimeMillis())) {
                        val name=members.firstOrNull {it.id==event.userId}?.nickname ?: tr("Invitado","Guest")
                        showPlaybackNotice(name,event.body)
                    }
                    continue
                }
                partyMessages.add(event); floatingEvents.add(event)
                while(floatingEvents.size>3) floatingEvents.removeAt(0)
                launch { delay(4500); if(valid()) floatingEvents.removeAll { it.id==event.id } }
            }
            while(partyMessages.size>60) partyMessages.removeAt(0)
        }
    }

    suspend fun heartbeatRoom() = partyPublishMutex.withLock {
        val s=session ?: return@withLock
        if(partyRole==PartyRole.HOST && screen!=Screen.PLAYER && !miniPlayer) watchParty?.let { cloud.updateWatchParty(s,it) }
    }

    suspend fun traktRefresh() = socialAction {
        val s = session ?: error(tr("Inicia sesión primero","Sign in first"))
        val result = trakt.call(s,"status")
        traktConnected = result.optBoolean("connected")
        traktStatus = if (traktConnected) tr("Conectado","Connected") else if (result.optBoolean("configured")) tr("Sin conectar","Not connected") else tr("Configura Trakt en el servidor","Trakt server setup required")
    }
    suspend fun traktStart() = socialAction {
        val s = session ?: error(tr("Inicia sesión primero","Sign in first"))
        traktDevice = trakt.call(s,"start")
        traktStatus = tr("Autoriza el código desde tu celular","Authorize the code on your phone")
    }
    suspend fun traktPoll(): Boolean {
        val s = session ?: return true
        val result=trakt.call(s,"poll")
        if(result.optBoolean("slow_down")) traktDevice=traktDevice?.put("interval",(traktDevice?.optLong("interval",5)?:5)+5)
        if (result.optBoolean("connected")) { traktConnected=true; traktDevice=null; traktStatus=tr("Conectado","Connected"); return true }
        return false
    }
    suspend fun traktDisconnect() = socialAction {
        val s=session ?: return@socialAction
        trakt.call(s,"disconnect"); traktConnected=false; traktDevice=null; traktStatus=tr("Desconectado","Disconnected")
    }
    suspend fun traktImport() = socialAction {
        val s=session ?: error(tr("Inicia sesión primero","Sign in first"))
        val rows=trakt.call(s,"watchlist").optJSONArray("items") ?: return@socialAction
        for(i in 0 until rows.length()) {
            val row=rows.getJSONObject(i)
            val movie=row.optJSONObject("movie")
            val show=row.optJSONObject("show")
            val media=movie ?: show ?: continue
            val id=media.optJSONObject("ids")?.optInt("tmdb",0) ?: 0
            if(id>0) { val key="tmdb:${if(movie!=null) "movie" else "series"}:$id"; if(!favorites.contains(key))favorites.add(key) }
        }
        pushCloud(); traktStatus=tr("Lista de Trakt importada a Mi lista","Watchlist imported to My List")
    }
    suspend fun traktMarkWatched(req: PlayerRequest, position: Long, duration: Long) {
        if(isMomentOnly(req)) return
        if(!traktConnected || req.live || duration<=0 || position.toDouble()/duration<0.8)return
        val s=session ?: return
        val event=req.playbackId
        if(traktSent.contains(event))return
        if(!traktSent.add(event))return
        runCatching {
            trakt.call(s,"watched",org.json.JSONObject().put("tmdb",req.item.id).put("type",req.item.type)
                .put("season",req.season).put("episode",req.episode).put("event",event))
        }.onFailure { traktSent.remove(event); traktStatus="${tr("No se pudo sincronizar el contenido visto","Could not sync watched title")}: ${it.message}" }
    }

    suspend fun startPairing() {
        pairingStatus = tr("Creando el QR seguro…","Creating secure pairing QR…")
        runCatching { pairingRepo.create() }
            .onSuccess { pairingRequest = it; pairingStatus = tr("Escanea con tu celular","Scan with your phone") }
            .onFailure { error = it.message; pairingStatus = "" }
    }

    suspend fun pollPairing(): Boolean {
        val req = pairingRequest ?: return false
        val payload = runCatching { pairingRepo.poll(req) }.getOrNull() ?: return false
        if (payload.accessToken.isNotBlank() && payload.userId.isNotBlank()) {
            val cloudSession = CloudSession(payload.accessToken, payload.refreshToken, payload.userId, payload.email)
            setAccountManifests(emptyList()); sourceSelection=null; playerRequest=null
            detachParty()
            session = cloudSession
            local.saveSession(cloudSession)
            runCatching { syncFromCloud() }
        }
        if (payload.addonManifest.isNotBlank()) runCatching { addAddonManifest(payload.addonManifest) }
        pairingStatus = buildString {
            if (payload.accessToken.isNotBlank()) append(tr("Cuenta conectada","Account connected"))
            if (payload.addonManifest.isNotBlank()) {
                if (isNotBlank()) append(" · ")
                append(tr("Complemento añadido","Add-on added"))
            }
        }.ifBlank { tr("Vinculación recibida","Pairing received") }
        pairingRepo.finish(req)
        pairingRequest = null
        return true
    }

    suspend fun checkForUpdates() {
        if(updateChecking || updateDownloading) return
        updateChecking=true;error=null;updateStatus=""
        try {
            val found=updates.check(BuildConfig.VERSION_NAME,BuildConfig.VERSION_CODE)
            if(updateInfo?.sha256!=found.sha256) downloadedUpdate=null
            updateInfo=found
        } catch(e: CancellationException) { throw e } catch(e: Exception) { error="${tr("Falló la búsqueda de actualizaciones","Update check failed")}: ${e.message}" } finally { updateChecking=false }
    }
    fun downloadUpdate() {
        val info=updateInfo?.takeIf { it.isNewer } ?: return
        if(updateDownloading || updateChecking) return
        updateDownloading=true;updateBytes=0;updateTotal=info.sizeBytes;downloadedUpdate=null;updateStatus="";error=null
        updateDownloadJob=appScope.launch {
            try {
                val file=updates.download(appContext,info) { bytes,total ->
                    kotlinx.coroutines.withContext(Dispatchers.Main.immediate) { updateBytes=bytes;updateTotal=total }
                }
                downloadedUpdate=file;updateStatus=tr("Descarga verificada. Lista para instalar.","Verified download. Ready to install.")
            } catch(e: CancellationException) { updateStatus=tr("Descarga cancelada","Download cancelled");throw e } catch(e: Exception) { error="${tr("No se pudo descargar la actualización","Could not download the update")}: ${e.message}" } finally { updateDownloading=false }
        }
    }
    fun cancelUpdateDownload() { updateDownloadJob?.cancel() }
    fun installUpdate(context: Context) {
        val file=downloadedUpdate ?: return;val info=updateInfo ?: return
        runCatching { updates.install(context,file,info.versionCode) }
            .onSuccess { launched -> updateStatus=if(launched) tr("Confirma Instalar en la ventana de Android.","Confirm Install in the Android window.")
                else tr("Permite instalar desde BruniO, vuelve y pulsa Instalar de nuevo.","Allow installs from BruniO, return and press Install again.") }
            .onFailure { error="${tr("No se pudo abrir el instalador","Could not open the installer")}: ${it.message}" }
    }

    fun signOut() {
        sharedMoments.clear();sharedMomentsOwner=""
        session?.let { presenceEvents.queue(it,"","",0,0,null,System.currentTimeMillis()) }
        playbackLinkCache.clear()
        closeAvatarUpload()
        detachParty()
        setAccountManifests(emptyList()); sourceSelection=null; playerRequest=null
        watchParty=null; partyRole=null; nickname=""
        partyMessages.clear(); floatingEvents.clear(); partyMembers.clear()
        partyInvitations.clear();calendarEvents.clear();friendRows.clear(); friendPeople.clear(); friendActivity.clear(); accessRequests.clear(); pendingPartyCode=null; lastEventId=-1
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
    SPLASH, HOME, SEARCH, MOVIES, SERIES, COLLECTIONS, OSCARS, COLLECTION_DETAIL, PLATFORM_DETAIL,
    GENRES, GENRE_DETAIL, YEAR_DETAIL, MY_LIST, SETTINGS, PROFILES, WATCH_PARTY, PAIR_DEVICE,
    DETAILS, CUSTOM_FILTER, FRANCHISE, PLAYER, MULTIVIEW, LIVE_TV, LIDOM, FRIENDS, TRAKT, NOTIFICATIONS, DIRECTOR_DETAIL
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
    val liveType: String = "tv",
    val liveMime: String = "",
    val lidomTeam: String = "",
    val lidomSource: String = "",
    val returnScreen: Screen? = null,
    val momentStartMs: Long = 0L,
    val momentEndMs: Long = 0L,
    val momentSender: String = "",
    val sourceFallbackPlaying: Boolean? = null
)

data class SourceSelection(
    val item: MediaSummary,
    val imdb: String,
    val season: Int,
    val episode: Int,
    val streams: List<StreamChoice>,
    val resumeMs: Long? = null,
    val workGeneration: Long = -1,
    val workSequence: Long = -1
)

internal fun partyTargetPosition(room: com.miflix.native2.model.WatchParty, now: Long = System.currentTimeMillis()): Long =
    room.positionMs + if(room.playing) (now-room.updatedAt).coerceIn(0L,5000L) else 0L
