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
    internal val assistantRP150 by lazy { AssistantRepositoryRP150(tmdb) }
    internal fun prepareGuestRP150():Boolean {
        if(playerRequest!=null || playbackLoading!=null || watchParty!=null || busyMessage!=null) return false
        discardPreparedVideoRP91();sourceWarmupJobRP90?.cancel();sourceWarmupJobRP90=null;sourceWarmupKeyRP90=null
        return true
    }
    internal fun disposeGuestRP150() {discardPreparedVideoRP91();appScope.coroutineContext[kotlinx.coroutines.Job]?.cancel()}
    private val rapidPlaybackRP87=RapidActionRP87()
    private val latestPlaybackRP87=LatestPlaybackRP87()
    internal val playerReleaseRP87=PlayerReleaseRP87()
    init {if(context !is GuestContextRP150) LocalCrashRP87.install(context)}
    init { com.miflix.native2.data.AwardSelections.configure(context.assets.open("awards_catalog.json").bufferedReader().use {it.readText()}) }
    private val appContext=context.applicationContext
    internal var focusScorePinRP15:(() -> Unit)?=null
    var showLiveScoresRP11 by mutableStateOf(false)
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
    internal var assistantPendingSceneRP158 by mutableStateOf<Pair<String,com.miflix.native2.data.AssistantBookmarkRP158>?>(null)
    internal var assistantToolRP158 by mutableStateOf<com.miflix.native2.data.AssistantToolRequestRP158?>(null)
    internal var assistantSnapshotRP158:(()->com.miflix.native2.data.AssistantBookmarkRP158?)?=null
    internal var assistantSeekRP158:((com.miflix.native2.data.AssistantBookmarkRP158)->Boolean)?=null
    internal var assistantLiveEntryRP158 by mutableStateOf<String?>(null)
    internal var assistantHomeEpochRP158 by mutableIntStateOf(0)
    internal var assistantDescriptionRP157 by mutableStateOf<String?>(null)
    internal var assistantEpisodeRP157 by mutableStateOf<com.miflix.native2.data.AssistantSceneHitRP156?>(null)
    internal var assistantQueryRP156 by mutableStateOf<String?>(null)
    internal var assistantLeagueRP156 by mutableStateOf<String?>(null)
    var assistantEnabledRP155 by mutableStateOf(playbackPrefs.getBoolean("assistant_enabled_rp155",false))
        private set
    fun saveAssistantEnabledRP155(enabled:Boolean) {assistantEnabledRP155=enabled;playbackPrefs.edit().putBoolean("assistant_enabled_rp155",enabled).apply()}
    var assistantSettingsCategoryRP155 by mutableStateOf<String?>(null)
    internal var assistantPlaybackRP155:((String)->Boolean)?=null

    private val lastSourcePrefsRP16=context.getSharedPreferences("brunio_last_source_rp16",Context.MODE_PRIVATE)
    private val lastSourceRP16=LastPlaybackSourceRP16({lastSourcePrefsRP16.getString("rows","{}").orEmpty()},
        {lastSourcePrefsRP16.edit().putString("rows",it).apply()})
    private fun lastSourceKeyRP16(item:MediaSummary,season:Int,episode:Int)=LastPlaybackSourceRP16.key(
        localExtrasScope()+(if(com.miflix.native2.data.DbzEpisodeRP107.enabled(item.type,item.id)) ":dbz-absolute-rp107" else ""),item.cloudId,season,episode,
        org.json.JSONArray(listOf(easyModeRP79,easySpanishOnlyRP83,audioLanguage,secondaryAudioLanguageRP62,streamSizeBytes,nativeSourceEnabled,nativeRevision,nativeOnlyRP75,nativeSpanishOnlyRP77,nativeSizeAscendingRP77,
            org.json.JSONArray(streamRepo.manifests()))).toString())
    internal fun rememberSuccessfulPlaybackRP16(req:PlayerRequest) {
        if(req.live || req.momentEndMs>0 || watchParty!=null || playerRequest?.playbackId!=req.playbackId || req.sourceMemoryKeyRP16.isBlank()) return
        if(req.sourceMemoryKeyRP16==lastSourceKeyRP16(req.item,req.season,req.episode)) lastSourceRP16.save(req.sourceMemoryKeyRP16,req.stream)
    }
    private data class RememberedStartRP16(val playbackId:String,val imdb:String,val owner:String?,val profile:String,val generation:Long)
    private var rememberedStartRP16:RememberedStartRP16?=null

    var adaptiveSource by mutableStateOf(playbackPrefs.getBoolean("adaptive_source",false))
        private set
    fun saveAdaptiveSource(enabled:Boolean) {adaptiveSource=enabled;playbackPrefs.edit().putBoolean("adaptive_source",enabled).apply()}
    var autoplayNextLink by mutableStateOf(playbackPrefs.getBoolean("autoplay_next_link",true))
        private set
    fun saveAutoplayNextLink(enabled:Boolean) {
        autoplayNextLink=enabled
        playbackPrefs.edit().putBoolean("autoplay_next_link",enabled).apply()
        if(!enabled) sourceFallback=null
    }
    internal val tasteProfile=TasteProfileRC102(context.getSharedPreferences("brunio_taste",Context.MODE_PRIVATE))
    var tasteSetupPromptEnabled by mutableStateOf(playbackPrefs.getBoolean("taste_setup_prompt",true))
        private set
    fun saveTasteSetupPrompt(enabled:Boolean) {tasteSetupPromptEnabled=enabled;playbackPrefs.edit().putBoolean("taste_setup_prompt",enabled).apply()}
    internal fun shouldOfferTasteSetup()=!kidsMode && tasteSetupPromptEnabled && !tasteProfile.done(ratingsKey())
    internal fun moodArtworkRC102():Map<String,String> {
        fun art(rows:List<MediaSummary>,offset:Int=0):String? {val images=rows.mapNotNull {it.backdrop ?: it.poster}.distinct();return if(images.isEmpty()) null else images[offset%images.size]}
        val fallback=art(trending+movies)
        return listOf(
            "" to (art(forYou) ?: fallback),"christmas" to (art(christmasPicks) ?: genreCovers["family"]),
            "happy" to art(comedy),"sad" to art(romancePicks,1),"documentary" to genreCovers["documentary"],
            "marathon" to art(series),"background" to art(comedy,2),"twist" to art(thrillers),
            "cozy" to art(romancePicks,3),"romantic" to art(romancePicks),"adrenaline" to art(action),
            "mindbending" to art(sciFi),"family" to genreCovers["family"],"nostalgia" to art(classicPicks),
            "quick" to art(newMovies,2),"meaningful" to art(topRated,2),"halloween" to art(horrorPicks)
        ).mapNotNull {(key,value) ->(value ?: fallback)?.takeIf {it.isNotBlank()}?.let {key to it}}.toMap()
    }

    val novelasPicks=mutableStateListOf<MediaSummary>()
    var novelasLoading by mutableStateOf(false)
        private set
    var novelasMessage by mutableStateOf("")
        private set
    private var novelasLocale by mutableStateOf("")
    val novelasMatch:Boolean get()=!kidsMode && novelasLocale==tmdb.language
    private val novelasMutex=Mutex()
    internal suspend fun loadNovelasHome()=novelasMutex.withLock {
        if(kidsMode || tmdb.token.isBlank()) return@withLock
        val locale=tmdb.language
        fun publish(rows:List<MediaSummary>) {if(locale==tmdb.language && !kidsMode) {novelasPicks.clear();novelasPicks.addAll(rows);novelasLocale=locale}}
        val cacheKey="novelas:$locale"
        val cached=tasteProfile.cached(cacheKey,24*60*60_000L)
        if(cached!=null) {val a=cached.optJSONArray("rows") ?: org.json.JSONArray();publish((0 until a.length()).mapNotNull {runCatching {LocalExtrasRC100.decode(a.getJSONObject(it))}.getOrNull()});return@withLock}
        novelasLoading=true;novelasMessage=""
        try {
            val rows=tmdb.novelaPage(1).rows
            if(rows.isNotEmpty()) {tasteProfile.cache(cacheKey,org.json.JSONObject().put("rows",org.json.JSONArray(rows.map {LocalExtrasRC100.encode(it)})));publish(rows)}
            else novelasMessage=tr("No se pudieron cargar las novelas. Reintenta.","Telenovelas could not be loaded. Try again.")
        } catch(e:CancellationException) {throw e} catch(_:Exception) {novelasMessage=tr("No se pudieron cargar las novelas. Reintenta.","Telenovelas could not be loaded. Try again.")}
        finally {novelasLoading=false}
    }
    val comfortComedyPicks=mutableStateListOf<MediaSummary>()
    var comfortComedyLoading by mutableStateOf(false)
        private set
    var comfortComedyMessage by mutableStateOf("")
        private set
    private var comfortComedyLocale by mutableStateOf("")
    val comfortComedyMatch:Boolean get()=!kidsMode && comfortComedyLocale==tmdb.language
    private val comfortComedyMutex=Mutex()
    internal suspend fun loadComfortComedyHome()=comfortComedyMutex.withLock {
        if(kidsMode || tmdb.token.isBlank()) return@withLock
        val locale=tmdb.language
        fun publish(rows:List<MediaSummary>) {if(locale==tmdb.language && !kidsMode) {comfortComedyPicks.clear();comfortComedyPicks.addAll(rows);comfortComedyLocale=locale}}
        comfortComedyLoading=true;comfortComedyMessage=""
        try {
            val rows=tmdb.comfortComedyPage(1).rows
            if(rows.isNotEmpty()) {publish(rows)}
            else comfortComedyMessage=tr("No se pudieron cargar las comedias. Reintenta.","Comedies could not be loaded. Try again.")
        } catch(e:CancellationException) {throw e} catch(_:Exception) {comfortComedyMessage=tr("No se pudieron cargar las comedias. Reintenta.","Comedies could not be loaded. Try again.")}
        finally {comfortComedyLoading=false}
    }
    private val tasteActorMutex=Mutex()
    internal suspend fun onboardingActors(query:String=""):List<DirectorPerson> = tasteActorMutex.withLock {
        val locale=tmdb.language
        if(query.isNotBlank()) return@withLock tmdb.tasteActors(query)
        val key="actors:$locale"
        tasteProfile.cached(key,24*60*60_000L)?.optJSONArray("rows")?.let {a ->return@withLock (0 until a.length()).map {val p=a.getJSONObject(it);DirectorPerson(p.getInt("id"),p.getString("name"),p.optString("photo").takeIf {it.isNotBlank()})}}
        val actors=coroutineScope {
            val popular=async {tmdb.tasteActors()}
            val latin=listOf("William Levy","Danna García","Mario Cimarro","Carmen Villalobos").map {name ->async {
                try {tmdb.tasteActors(name).firstOrNull {FlexibleSearch.normalize(it.name)==FlexibleSearch.normalize(name)}} catch(e:CancellationException) {throw e} catch(_:Exception) {null}
            }}.awaitAll().filterNotNull()
            (latin+popular.await()).distinctBy {it.id}.take(24)
        }
        if(actors.isNotEmpty()) tasteProfile.cache(key,org.json.JSONObject().put("rows",org.json.JSONArray(actors.map {org.json.JSONObject().put("id",it.id).put("name",it.name).put("photo",it.photo ?: "")})))
        actors
    }
    private val personalTasteMutex=Mutex()
    private suspend fun initialTasteRecommendations():List<MediaSummary> = personalTasteMutex.withLock {
        if(kidsMode || !tasteProfile.enabled(ratingsKey()) || tmdb.token.isBlank()) return@withLock emptyList()
        val owner=ratingsKey();val locale=tmdb.language;val day=System.currentTimeMillis()/86_400_000L
        val version=tasteProfile.version(owner);val signature="$version:$day:${ratings.toMap().hashCode()}"
        val cacheKey="recommendations:$owner:$locale"
        val cached=tasteProfile.cached(cacheKey,6*60*60_000L)
        if(cached?.optString("signature")==signature) {val a=cached.optJSONArray("rows") ?: org.json.JSONArray();return@withLock (0 until a.length()).mapNotNull {runCatching {LocalExtrasRC100.decode(a.getJSONObject(it))}.getOrNull()}}
        fun <T> rotate(rows:List<T>):List<T> {if(rows.isEmpty()) return rows;val offset=(day%rows.size).toInt();return rows.drop(offset)+rows.take(offset)}
        val manual=rotate(tasteProfile.seeds(owner).filter {(ratings[it.cloudId] ?: 10)>4}).take(3)
        val learned=tasteProfile.learned(owner).filter {(ratings[it.cloudId] ?: 10)>4 && it.cloudId !in manual.map {m ->m.cloudId}}.take(2)
        val actors=rotate(tasteProfile.actorIds(owner)).take(2)
        val genres=rotate(TasteGenresRC102.filter {it.key in tasteProfile.genres(owner)}).take(2)
        if(manual.isEmpty() && learned.isEmpty() && actors.isEmpty() && genres.isEmpty()) return@withLock emptyList()
        val batches=coroutineScope {
            val jobs=mutableListOf<Deferred<Pair<Double,List<MediaSummary>>>>()
            for(seed in manual+learned) jobs.add(async {val rows=try {tmdb.recommendations(seed)} catch(e:CancellationException) {throw e} catch(_:Exception) {emptyList()};(if(seed in manual) 1.5 else .8) to rows})
            for(actor in actors) jobs.add(async {val rows=try {tmdb.actorCredits(actor)} catch(e:CancellationException) {throw e} catch(_:Exception) {emptyList()};1.2 to rows.take(30)})
            for(genre in genres) jobs.add(async {val rows=try {when {genre.key=="soap" ->tmdb.novelaPage(1).rows;genre.movie<=0 ->tmdb.discoverTvGenre(genre.tv,pages=1,limit=25);genre.tv<=0 ->tmdb.discoverMovieGenre(genre.movie,pages=1,limit=25);else ->tmdb.discoverGenreMixed(genre.movie,genre.tv,25)}} catch(e:CancellationException) {throw e} catch(_:Exception) {emptyList()};1.0 to rows})
            jobs.awaitAll()
        }
        val scores=mutableMapOf<String,Double>();val items=linkedMapOf<String,MediaSummary>()
        for((weight,rows) in batches) rows.forEachIndexed {index,item ->items[item.cloudId]=item;scores[item.cloudId]=(scores[item.cloudId] ?: 0.0)+weight*(1.0-index.coerceAtMost(39)/40.0)}
        val selected=tasteProfile.seeds(owner).map {it.cloudId}.toSet()
        val rows=items.values.filter {it.cloudId !in selected && (ratings[it.cloudId] ?: 10)>4 && (progress[it.cloudId]?.percent ?: 0.0)<90.0}
            .sortedWith(compareByDescending<MediaSummary> {scores[it.cloudId] ?: 0.0}.thenByDescending {it.rating}).take(40)
        if(owner==ratingsKey() && locale==tmdb.language && version==tasteProfile.version(owner) && rows.isNotEmpty()) tasteProfile.cache(cacheKey,org.json.JSONObject().put("signature",signature).put("rows",org.json.JSONArray(rows.map {LocalExtrasRC100.encode(it)})))
        rows
    }
    internal val localExtras=LocalExtrasRC100(context.getSharedPreferences("brunio_local_extras",Context.MODE_PRIVATE))
    internal fun localExtrasScope()=ratingsKey()
    internal fun localRecommendations(rows:List<MediaSummary>)=rows.filterNot {localExtras.hidden(ratingsKey(),it.cloudId)}
    internal fun exportLocalBackupRC100():org.json.JSONObject=org.json.JSONObject()
        .put("brunioLocalBackup",100).put("extras",localExtras.export(ratingsKey()))
        .put("homeOrder",org.json.JSONArray(homeRowOrder)).put("hiddenRows",org.json.JSONArray(hiddenHomeRows.toList()))
        .put("interface",interfaceLanguage).put("audio",playbackPrefs.getString(profileAudioKeyRP77(),legacyAudioRP77) ?: legacyAudioRP77).put("subtitle",playbackPrefs.getString(profileSubtitleKeyRP78(),legacySubtitleRP78) ?: legacySubtitleRP78)
        .put("easySubtitle",playbackPrefs.getString(easySubtitleKeyRP87(),"es") ?: "es")
        .put("autoplay",autoplayNext).put("nextLink",autoplayNextLink).put("adaptive",adaptiveSource)
        .put("previousLiveChannel",previousLiveChannelEnabled)
        .put("tasteProfile",tasteProfile.data(ratingsKey())).put("tastePrompt",tasteSetupPromptEnabled)
        .put("subtitleColor",subtitleColor).put("subtitleScale",subtitleScale.toDouble()).put("focusColor",focusColor)
        .also {require(it.toString().toByteArray(Charsets.UTF_8).size<=2_000_000) {"Backup exceeds 2 MB"}}
    internal fun validateLocalBackupRC100(body:org.json.JSONObject) {
        require(body.getInt("brunioLocalBackup")==100)
        localExtras.validateImport(ratingsKey(),body.getJSONObject("extras"))
        for(key in listOf("homeOrder","hiddenRows")) {val a=body.getJSONArray(key);require(a.length()<=100);for(i in 0 until a.length()) require(a.getString(i).length in 1..100)}
        require(body.getString("interface") in com.miflix.native2.ui.AppLanguagesRP148.codes)
        require(body.getString("audio").length<=10 && body.getString("subtitle").length<=10)
        if(body.has("easySubtitle")) require(body.getString("easySubtitle").length in 1..10)
        for(key in listOf("autoplay","nextLink","adaptive")) body.getBoolean(key)
        body.optJSONObject("tasteProfile")?.let {tasteProfile.validate(it)}
        if(body.has("tastePrompt")) body.getBoolean("tastePrompt")
        if(body.has("previousLiveChannel")) body.getBoolean("previousLiveChannel")
        body.getInt("subtitleColor");body.getInt("focusColor");require(body.getDouble("subtitleScale") in 0.8..1.4)
    }
    internal suspend fun importLocalBackupRC100(body:org.json.JSONObject,reloadHome:Boolean=true) {
        validateLocalBackupRC100(body)
        localExtras.importData(ratingsKey(),body.getJSONObject("extras"))
        val order=body.getJSONArray("homeOrder");saveHomeRowOrder((0 until order.length()).map {order.getString(it)})
        val hidden=body.getJSONArray("hiddenRows")
        playbackPrefs.edit().putStringSet("hidden_home_rows:${ratingsKey()}",(0 until hidden.length()).map {hidden.getString(it)}.toSet()).apply();homeOrderRevision++
        savePlaybackPreferences(body.getString("audio"),
            if(easyModeRP79) body.optString("easySubtitle","es") else body.getString("subtitle"),body.getBoolean("autoplay"))
        playbackPrefs.edit().putString(profileSubtitleKeyRP78(),body.getString("subtitle"))
            .putString(easySubtitleKeyRP87(),body.optString("easySubtitle","es")).apply()
        subtitleRevisionRP78++
        saveAutoplayNextLink(body.getBoolean("nextLink"));saveAdaptiveSource(body.getBoolean("adaptive"))
        body.optJSONObject("tasteProfile")?.let {tasteProfile.restore(ratingsKey(),it)}
        if(body.has("tastePrompt")) saveTasteSetupPrompt(body.getBoolean("tastePrompt"))
        savePreviousLiveChannelEnabled(body.optBoolean("previousLiveChannel",true))
        saveSubtitleStyle(body.getInt("subtitleColor"),body.getDouble("subtitleScale").toFloat())
        focusColor=body.getInt("focusColor");FocusTheme.argb=focusColor;playbackPrefs.edit().putInt("focus_color",focusColor).apply()
        restoreCustomFilterRC100()
        if(reloadHome) setInterfaceLanguage(body.getString("interface")) else {
            playbackPrefs.edit().putString(interfaceLanguageKey(),body.getString("interface")).apply()
            applyProfileInterfaceLanguage()
        }
    }
    private val cinemaStats=LocalCinemaStats(playbackPrefs)
    fun recordViewing(req:PlayerRequest,ms:Long) {
        if(!req.live && !isMomentOnly(req) && appForeground && screen==Screen.PLAYER && playerRequest?.playbackId==req.playbackId)
            {cinemaStats.record(ratingsKey(),req.item,details?.takeIf {it.summary.cloudId==req.item.cloudId},ms)
             if(!kidsMode) tasteProfile.viewing(ratingsKey(),req.item,ms)}
    }
    fun flushCinemaStats()=cinemaStats.flush()
    fun cinemaReport()=cinemaStats.snapshot(ratingsKey())
    private var homeOrderRevision by mutableIntStateOf(0)
    val homeRowOrder:List<String> get() {homeOrderRevision;val a=runCatching {org.json.JSONArray(playbackPrefs.getString("home_order:${ratingsKey()}","[]"))}.getOrDefault(org.json.JSONArray());val keys=(0 until a.length()).map {a.getString(it)}.distinct().toMutableList()
        if(keys.isNotEmpty() && "novelas" !in keys) {val anchor=listOf("genres_home","streaming","continue").firstOrNull {it in keys};keys.add(if(anchor!=null) keys.indexOf(anchor)+1 else 0,"novelas")};if(keys.isNotEmpty() && "comfort_comedies" !in keys) {val anchor=listOf("genres_home","streaming","continue").firstOrNull {it in keys};keys.add(if(anchor!=null) keys.indexOf(anchor)+1 else 0,"comfort_comedies")};assistantHomeEpochRP158;return com.miflix.native2.data.AssistantLocalRP158.seasonalOrder(keys,com.miflix.native2.data.AssistantLocalRP158.event(appContext,localExtrasScope()),java.time.LocalDate.now())}
    internal fun savedHomeOrderRP158():List<String> = runCatching {val a=org.json.JSONArray(playbackPrefs.getString("home_order:${ratingsKey()}","[]"));(0 until a.length()).map {a.getString(it)}}.getOrDefault(emptyList())
    fun saveHomeRowOrder(keys:List<String>) {playbackPrefs.edit().putString("home_order:${ratingsKey()}",org.json.JSONArray(keys.distinct()).toString()).apply();homeOrderRevision++}

    val hiddenHomeRows:Set<String> get() {
        homeOrderRevision
        assistantHomeEpochRP158
        val hidden=playbackPrefs.getStringSet("hidden_home_rows:${ratingsKey()}",emptySet()).orEmpty().toSet()
        return if(com.miflix.native2.data.AssistantLocalRP158.event(appContext,localExtrasScope()) && java.time.LocalDate.now().monthValue==10) hidden-setOf("horror_movies","seasonal") else hidden
    }
    fun setHomeRowVisible(key:String,visible:Boolean) {
        val hidden=hiddenHomeRows
        playbackPrefs.edit().putStringSet("hidden_home_rows:${ratingsKey()}",if(visible) hidden-key else hidden+key).apply()
        homeOrderRevision++
    }
    fun restoreHomeRows() {
        playbackPrefs.edit().remove("hidden_home_rows:${ratingsKey()}").apply()
        homeOrderRevision++
    }

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
    fun titleActions(item: MediaSummary, continuing: Boolean = false) {if(!easyModeRP79) {contextContinue=continuing;contextTitle=item}}
    var focusColor by mutableIntStateOf(playbackPrefs.getInt("focus_color",0xFFB792FF.toInt()))
        private set
    private fun rememberedProfileEnabledRP142():Boolean {
        val key=if(BuildConfig.APPLICATION_ID.startsWith("com.brunio.mobile") && playbackPrefs.contains("mobile_remember_last_profile_rp142")) "mobile_remember_last_profile_rp142" else "remember_last_profile"
        return playbackPrefs.getBoolean(key,false)
    }
    var rememberLastProfile by mutableStateOf(rememberedProfileEnabledRP142())
        private set
    init { FocusTheme.argb=focusColor }
    fun saveFocusColor(value: Int) { focusColor=value;FocusTheme.argb=value;playbackPrefs.edit().putInt("focus_color",value).apply() }
    fun setRememberProfile(value:Boolean) {
        rememberLastProfile=value
        val edit=playbackPrefs.edit().putBoolean("remember_last_profile",value)
        if(BuildConfig.APPLICATION_ID.startsWith("com.brunio.mobile")) edit.putBoolean("mobile_remember_last_profile_rp142",value)
        edit.apply()
    }
    private fun readNoticesKey()="read_"+noticesKey()
    var noticesRevision by mutableIntStateOf(0)
        private set
    fun noticeRead(id: String): Boolean { noticesRevision;return id in playbackPrefs.getStringSet(readNoticesKey(),emptySet()).orEmpty() }
    val unreadNoticeCount: Int get() = notifications.count { !noticeRead(it.id) } + if(noticeRead(UpdateNotes.id)) 0 else 1
    fun markNoticesRead(id: String? = null) {
        val ids=playbackPrefs.getStringSet(readNoticesKey(),emptySet()).orEmpty().toSet()+(id?.let {setOf(it)} ?: (notifications.map {it.id}.toSet()+UpdateNotes.id))
        playbackPrefs.edit().putStringSet(readNoticesKey(),ids).apply();noticesRevision++
    }
    private val legacyInterfaceLanguage=playbackPrefs.getString("interface","en").takeIf {it in com.miflix.native2.ui.AppLanguagesRP148.codes} ?: "en"
    var interfaceLanguage by mutableStateOf(legacyInterfaceLanguage)
        private set
    init { UiLanguage.code=interfaceLanguage }
    private fun interfaceLanguageKey()="interface:${ratingsKey()}"
    private fun applyProfileInterfaceLanguage() {
        val key=interfaceLanguageKey()
        val code=playbackPrefs.getString(key,legacyInterfaceLanguage).takeIf {it in com.miflix.native2.ui.AppLanguagesRP148.codes} ?: "en"
        // Pin the legacy default once; edits in another profile never change it.
        if(!playbackPrefs.contains(key)) playbackPrefs.edit().putString(key,code).apply()
        val changed=interfaceLanguage!=code
        interfaceLanguage=code;UiLanguage.code=code
        tmdb.language=AppLanguagesRP148.locale(code)
        if(changed) clearTranslatedCatalogRC100()
    }
    private fun clearTranslatedCatalogRC100() {
        extrasLoaded=false;continueHomeReadyRP129=false
        trending.clear();movies.clear();series.clear();topRated.clear()
        action.clear();sciFi.clear();netflix.clear();disney.clear();prime.clear();apple.clear();hbo.clear()
        newMovies.clear();bestSeries.clear();comedy.clear();thrillers.clear()
        comingMovies.clear();comingSeries.clear();forYou.clear()
        seasonalItems.clear();seasonalKey="";seasonalRetryAt=0L;comingSoonKey=""
    }
    suspend fun setInterfaceLanguage(code: String) {
        require(code in com.miflix.native2.ui.AppLanguagesRP148.codes)
        playbackPrefs.edit().putString(interfaceLanguageKey(),code).apply()
        val changed=interfaceLanguage!=code
        applyProfileInterfaceLanguage()
        if(changed && tmdb.token.isNotBlank()) loadHome()
    }
    private val playbackWork = PlaybackWorkGate()
    private var partyEpoch = 0L
    private var switchingPartyEpoch: Long? = null
    private val streamSizePreferenceKeyRP134=if(BuildConfig.APPLICATION_ID.startsWith("com.brunio.mobile")) "mobile_stream_size_gb_rp134" else "stream_size_gb"
    var streamSizeLimitGb by mutableStateOf((playbackPrefs.getString(streamSizePreferenceKeyRP134,"0")?.toDoubleOrNull() ?: 0.0))
        private set
    private val streamSizeBytes: Long get() = (streamSizeLimitGb * 1_000_000_000.0).toLong()
    fun saveStreamSizeLimit(gb: Double) {
        require(gb.isFinite() && gb in 0.0..500.0)
        streamSizeLimitGb=gb
        playbackPrefs.edit().putString(streamSizePreferenceKeyRP134,gb.toString()).apply()
        invalidatePlaybackWork()
    }
    private fun invalidatePlaybackWork() {
        discardPreparedVideoRP91()
        sourceWarmupJobRP90?.cancel();sourceWarmupJobRP90=null;sourceWarmupKeyRP90=null
        latestPlaybackRP87.cancel()
        playbackWork.invalidate(); sourceFallback=null; rememberedStartRP16=null; sourceSelection=null; busyMessage=null;playbackLoading=null
        playbackSubtitleJobs.values.forEach { it.cancel() }; playbackSubtitleJobs.clear()
    }
    private fun sameParty(epoch: Long, owner: String, room: String): Boolean =
        partyEpoch==epoch && socialIdentityId==owner && watchParty?.roomCode==room
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
        val account=profileSocialSession() ?: return;val room=watchParty ?: return;val epoch=partyEpoch
        if(kidsMode) return
        val localOnly=partyRole==PartyRole.GUEST && room.hostOnlyControls
        val action=(if(playing) "resumed" else "paused")+(if(localOnly) "-local" else "")
        showPlaybackNotice(nickname.ifBlank {activeProfile.name},action)
        // One small event per deliberate remote-control action; no new poll or heartbeat.
        try {if(sameParty(epoch,account.userId,room.roomCode)) social.send(account,room.roomCode,"playback",action)}
        catch(e: CancellationException) {throw e} catch(_: Exception) { /* Cosmetic failure must not interrupt playback. */ }
    }
    // Freeze the old shared value as a migration default; subsequent edits belong to one profile.
    private val legacyAudioRP77=playbackPrefs.getString("audio","es") ?: "es"
    private var audioRevisionRP77 by mutableIntStateOf(0)
    private fun profileAudioKeyRP77()="audio_rp77:${localExtrasScope()}"
    var audioLanguage:String
        get() {audioRevisionRP77;if(easyModeRP79 && easySpanishOnlyRP83) return "es";return playbackPrefs.getString(profileAudioKeyRP77(),legacyAudioRP77) ?: legacyAudioRP77}
        set(value) {playbackPrefs.edit().putString(profileAudioKeyRP77(),value).apply();audioRevisionRP77++}
    private var secondaryAudioRevisionRP62 by mutableIntStateOf(0)
    private fun secondaryAudioKeyRP62()="secondary_audio_rp62:${localExtrasScope()}"
    val secondaryAudioLanguageRP62:String get() {
        secondaryAudioRevisionRP62
        return playbackPrefs.getString(secondaryAudioKeyRP62(),"").orEmpty().takeUnless {it==audioLanguage}.orEmpty()
    }
    fun saveSecondaryAudioRP62(value:String) {
        require(value in setOf("","es","en","pt","fr","ko"))
        playbackPrefs.edit().putString(secondaryAudioKeyRP62(),value).apply()
        secondaryAudioRevisionRP62++;invalidatePlaybackWork()
    }
    private val legacySubtitleRP78=playbackPrefs.getString("subtitle","es") ?: "es"
    private var subtitleRevisionRP78 by mutableIntStateOf(0)
    private fun profileSubtitleKeyRP78()="subtitle_rp78:${localExtrasScope()}"
    private fun easySubtitleKeyRP87()="easy_subtitle_rp87:${localExtrasScope()}"
    var subtitleLanguage:String
        get() {
            subtitleRevisionRP78
            return com.miflix.native2.data.EasySubtitleRP87.profileLanguage(easyModeRP79,
                playbackPrefs.getString(profileSubtitleKeyRP78(),legacySubtitleRP78),
                playbackPrefs.getString(easySubtitleKeyRP87(),null))
        }
        set(value) {
            val key=if(easyModeRP79) easySubtitleKeyRP87() else profileSubtitleKeyRP78()
            playbackPrefs.edit().putString(key,value).apply();subtitleRevisionRP78++
        }
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
        playbackPrefs.edit().putBoolean("autoplay",autoplay).apply()
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
            if(owner==queueKey()) { started=newRequest;selected=item;details=d;currentSeason=if(com.miflix.native2.data.AbsoluteEpisodeRP102.enabled(item.id) && item.type=="series") 1 else entry.season;removeQueued(entry.id) }
        })
        val active=started ?: return
        if(partyRole==PartyRole.HOST) hostPartyUpdate(active,0L,true)
        if(item.type=="series") {
            val rows=runCatching { tmdb.season(item.id,entry.season) }.getOrDefault(emptyList())
            if(owner==queueKey() && playerRequest?.playbackId==active.playbackId) { episodes.clear();episodes.addAll(rows) }
        }
        } finally { queueStarting=false }
    }

    internal suspend fun syncCalendarSubscriptionsRP134() {
        val current=session ?: return
        val profile=activeProfile.id
        val owner="${current.userId}:$profile"
        val local=com.miflix.native2.data.CalendarStoreRP120.syncEntriesRP134(appContext,owner)
        val remote=cloud.calendarSubscriptionsRP134(current,profile,local)
        if(session?.userId==current.userId && activeProfile.id==profile)
            com.miflix.native2.data.CalendarStoreRP120.applySyncRP134(appContext,owner,remote)
    }
    val calendarEvents=mutableStateListOf<CalendarEvent>()
    suspend fun refreshCalendar() {
        try {syncCalendarSubscriptionsRP134()} catch(e:CancellationException) {throw e} catch(_:Exception) { }
        val owner=profileSocialSession() ?: return
        val events=social.calendar(owner)
        if(isCurrentSocialSession(owner)) { calendarEvents.clear();calendarEvents.addAll(events) }
    }
    suspend fun createCalendarEvent(title: String, kind: String, date: String, cloud: String?, season: Int, episode: Int, guests: List<String>, channel: LiveChannel?=null) {
        val owner=profileSocialSession() ?: throw IllegalStateException(tr("Inicia sesión","Sign in first"))
        require(title.trim().isNotEmpty() && title.length<=160) { tr("Escribe un título","Enter a title") }
        val parser=java.text.SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.US).apply { isLenient=false }
        val value=parser.parse(date) ?: throw IllegalArgumentException(tr("Fecha inválida","Invalid date"))
        require(parser.format(value)==date) { tr("Usa YYYY-MM-DD HH:mm","Use YYYY-MM-DD HH:mm") }
        val utc=java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'",Locale.US).apply { timeZone=java.util.TimeZone.getTimeZone("UTC") }
        social.createCalendar(owner,title.trim(),kind,utc.format(value),cloud,season,episode,guests,channel)
        refreshCalendar()
    }
    fun calendarLiveManifest(kind: String): String = when(kind) {
        com.miflix.native2.data.NuvioTestConfigRP49.PROVIDER -> error("This source has been removed")
        "iptv", "iptv:do", "iptv:tur", "iptv:mx", "iptv:es", "iptv:us" -> com.miflix.native2.data.FinalCatalogRP97.MANIFEST
        "sports" -> com.miflix.native2.data.SportsSourceRP71.read(appContext)
        else -> com.miflix.native2.data.FinalCatalogRP97.MANIFEST
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
    suspend fun deleteCalendarEvent(id: String) { profileSocialSession()?.let { social.deleteCalendar(it,id);refreshCalendar() } }
    private fun noticesKey() = "notices_${session?.userId ?: "local"}_${activeProfile.id}"
    private fun knownKey() = "known_${session?.userId ?: "local"}_${activeProfile.id}"
    private fun persistNotices() {
        val a = org.json.JSONArray()
        notifications.forEach { n -> a.put(org.json.JSONObject().put("id",n.id).put("cloud",n.item.cloudId).put("title",n.item.title).put("overview",n.item.overview).put("poster",n.item.poster).put("message",n.message).put("date",n.date)) }
        playbackPrefs.edit().putString(noticesKey(),a.toString()).apply()
    }
    fun clearNotifications() { notifications.clear(); persistNotices() }
    suspend fun checkNotifications() {
        if(easyModeRP79 || checkingNotices || tmdb.token.isBlank()) return
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
        if(easyModeRP79) return
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
        if(com.miflix.native2.data.AbsoluteEpisodeRP102.enabled(req.item.id)) {
            val index=current.indexOfFirst {it.season==req.season && it.episode==req.episode}
            return if(index<0) null else current.drop(index+1).firstOrNull {it.airDate.isNotBlank() && it.airDate<=today}
        }
        current.firstOrNull { it.episode > req.episode && it.airDate.isNotBlank() && it.airDate <= today }?.let { return it }
        val count = tmdb.details(req.item).seasonCount
        if(req.season < count) return tmdb.season(req.item.id,req.season+1).firstOrNull { it.airDate.isNotBlank() && it.airDate <= today }
        return null
    }
    suspend fun playNext(req: PlayerRequest, ep: EpisodeSummary) {
        if(partyRole==PartyRole.GUEST || playerRequest?.playbackId!=req.playbackId) return
        if(!rapidPlaybackRP87.accept("${localExtrasScope()}:next:${req.playbackId}:${ep.season}:${ep.episode}",android.os.SystemClock.uptimeMillis())) return
        latestPlaybackRP87.cancel()
        val ticket=playbackWork.begin()
        val owner=localExtrasScope()
        fun valid()=playbackWork.current(ticket) && owner==localExtrasScope() && playerRequest?.playbackId==req.playbackId
        val detail=tmdb.details(req.item)
        if(!valid()) return
        val imdb=detail.imdbId ?: return
        val rows=tmdb.season(req.item.id,ep.season)
        if(!valid()) return
        selected=req.item;details=detail;currentSeason=if(com.miflix.native2.data.AbsoluteEpisodeRP102.enabled(req.item.id)) 1 else ep.season
        episodes.clear();episodes.addAll(rows)
        playResolved(req.item,imdb,ep.season,ep.episode,resumeOverride=0)
    }
    private val partyPlaybackMutex=Mutex()
    suspend fun requestPartyPlayback(req: PlayerRequest, playing: Boolean) {
        if(watchParty?.hostOnlyControls==true && partyRole==PartyRole.GUEST) return
        val s=profileSocialSession() ?: return; val room=watchParty?.roomCode ?: return
        partyPlaybackMutex.withLock {
            if(isCurrentSocialSession(s) && watchParty?.roomCode==room && playerRequest?.playbackId==req.playbackId) {
                social.playback(s,room,req.item.cloudId,req.season,req.episode,playing)
            }
        }
    }
    suspend fun takePartyPlayback(): Boolean? {
        if(watchParty?.hostOnlyControls==true) return null
        val s=profileSocialSession() ?: return null; val room=watchParty?.roomCode ?: return null
        return social.takePlayback(s,room)
    }
    suspend fun requestPartyPause(req: PlayerRequest) {
        val s = profileSocialSession() ?: return; val room = watchParty?.roomCode ?: return
        social.pause(s,room,req.item.cloudId,req.season,req.episode)
    }
    suspend fun takePartyPause(): Boolean {
        val s = profileSocialSession() ?: return false; val room = watchParty?.roomCode ?: return false
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
        val owner=ratingsKey(); val revision=ratings.toMap();val history=discoveryHistoryKey;val locale=tmdb.language;val tasteVersion=tasteProfile.version(owner)
        val rows=if(revision.values.any { it>=6 } || history.isNotBlank()) recommendationsFor() else emptyList()
        val personalized=try {initialTasteRecommendations()} catch(e:CancellationException) {throw e} catch(_:Exception) {emptyList()}
        val refined=try {refinedTastePicks()} catch(e:CancellationException) {throw e} catch(_:Exception) {emptyList()}
        if(owner==ratingsKey() && revision==ratings.toMap() && history==discoveryHistoryKey && locale==tmdb.language && tasteVersion==tasteProfile.version(owner)) {
            val mixed=(0 until maxOf(personalized.size,refined.size,rows.size)).flatMap {index ->listOfNotNull(personalized.getOrNull(index),refined.getOrNull(index),rows.getOrNull(index))}
            forYou.clear();forYou.addAll((mixed).filter {(ratings[it.cloudId] ?: 10)>4 && (progress[it.cloudId]?.percent ?: 0.0)<90}.distinctBy {it.cloudId}.take(30))
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
    init {com.miflix.native2.data.FinalCatalogRP97.initialize(context)}
    private val livePrefs = context.getSharedPreferences("miflix_live", Context.MODE_PRIVATE)
    val socialIdentityId:String? get()=session?.let {ProfileSocialIdentityRP1.actor(it.userId,activeProfile.id,activeProfile.primary)}
    private fun profileSocialSession(account:CloudSession?=session):CloudSession? = if(kidsMode || easyModeRP79) null else account?.let {ProfileSocialIdentityRP1.session(it,activeProfile)}
    private fun isCurrentSocialSession(actor:CloudSession):Boolean {
        val scope=ProfileSocialIdentityRP1.scope(actor)
        return session?.userId==scope.account && activeProfile.id==scope.profile && !kidsMode
    }
    private fun socialRoomKey()="room_rp1_${session?.userId}:${activeProfile.id}"
    private val social = SocialRepository(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_KEY)
    private val watchedFriendsRP93=com.miflix.native2.data.WatchedFriendsRP93(BuildConfig.SUPABASE_URL,BuildConfig.SUPABASE_KEY)
    internal val watchedFriendsOwnerRP93:String? get()=profileSocialSession()?.userId
    internal suspend fun friendsWhoWatchedRP93(cloudId:String):List<SocialPerson> {
        val actor=profileSocialSession() ?: return emptyList()
        val rows=watchedFriendsRP93.friends(actor,cloudId)
        return if(isCurrentSocialSession(actor) && !easyModeRP79) rows else emptyList()
    }
    internal suspend fun watchedSharingRP93():Boolean? {
        val actor=profileSocialSession() ?: return null
        val value=watchedFriendsRP93.sharing(actor)
        return value.takeIf {isCurrentSocialSession(actor) && !easyModeRP79}
    }
    internal suspend fun saveWatchedSharingRP93(value:Boolean):Boolean? {
        val actor=profileSocialSession() ?: return null
        val saved=watchedFriendsRP93.saveSharing(actor,value)
        return saved.takeIf {isCurrentSocialSession(actor) && !easyModeRP79}
    }

    val trakt = TraktRepository(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_KEY)
    var nickname by mutableStateOf("")
    val partyMembers = mutableStateListOf<SocialPerson>()
    val recentPartyMembersRP88=mutableStateListOf<SocialPerson>()
    private var recentPartyRoomRP88:String?=null
    var partyReturnBusyRP88 by mutableStateOf(false)
        private set
    var partyReturnPulseRP88 by mutableIntStateOf(0)
        private set
    suspend fun bringFriendBackRP88(friendId:String) {
        val actor=profileSocialSession() ?: return
        val room=watchParty ?: return
        val epoch=partyEpoch
        if(easyModeRP79 || partyRole!=PartyRole.HOST || room.hostUserId!=actor.userId || friendId==actor.userId || partyReturnBusyRP88) return
        if(!rapidPlaybackRP87.accept("${localExtrasScope()}:party-return:$friendId",android.os.SystemClock.uptimeMillis())) return
        val member=partyMembers.any {it.id==friendId}
        if(!member && recentPartyMembersRP88.none {it.id==friendId} && friendActivity.none {it.id==friendId}) return
        partyReturnBusyRP88=true
        try {
            if(member) social.send(actor,room.roomCode,"playback","return:"+friendId)
            else social.inviteParty(actor,room.roomCode,friendId)
            if(sameParty(epoch,actor.userId,room.roomCode)) partyStatus=if(member)
                tr("Solicitud de regreso enviada","Return request sent") else tr("Invitación para volver enviada","Return invitation sent")
        } finally {partyReturnBusyRP88=false}
    }

    val partyInvitations = mutableStateListOf<PartyInvitation>()
    var partyOptionsBusy by mutableStateOf(false)
        private set
    private var sharedSourcePlaybackId: String? = null
    suspend fun inviteToParty(friendId: String) {
        val account=profileSocialSession() ?: return;val room=watchParty ?: return
        check(partyRole==PartyRole.HOST)
        social.inviteParty(account,room.roomCode,friendId)
        partyStatus=tr("Invitación enviada","Invitation sent")
    }
    suspend fun previewParty(code: String): org.json.JSONObject? {
        if(!code.matches(Regex("[0-9]{6}"))) return null
        return social.previewParty(profileSocialSession() ?: return null,code).takeIf {it.length()>0}
    }
    private fun partyPresentation(p: WatchParty,req: PlayerRequest): WatchParty = p.copy(
        title=req.item.title,overview=req.item.overview.take(800),poster=req.item.poster,
        logo=details?.takeIf {it.summary.cloudId==req.item.cloudId}?.logo,
        sourceName=PartySourcePolicy.label(req.stream.addonName+" · "+req.stream.name),
        sourceFilename=PartySourcePolicy.label(req.stream.filename),sourceSize=req.stream.videoSize,
        sourceKey=PartySourcePolicy.key(req.stream))
    suspend fun configureParty(share: Boolean?=null,hostOnly: Boolean?=null) = partyPublishMutex.withLock {
        val account=profileSocialSession() ?: return@withLock;val old=watchParty ?: return@withLock
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
            if(isCurrentSocialSession(account) && watchParty?.roomCode==old.roomCode) watchParty=next
            if(share==false) sharedSourcePlaybackId=null
        } finally { partyOptionsBusy=false }
    }
    suspend fun usePartySource() {
        val account=profileSocialSession() ?: return;val remote=refreshParty() ?: return;val epoch=partyEpoch
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
        profileSocialSession() ?: error(tr("Inicia sesión primero","Sign in first")))
    suspend fun recommendToFriends(item: MediaSummary) {
        val account=profileSocialSession() ?: error(tr("Inicia sesión primero","Sign in first"))
        social.recommend(account,item)
    }
    suspend fun removeFriendRecommendation(cloudId: String) {
        social.removeRecommendation(profileSocialSession() ?: error(tr("Inicia sesión primero","Sign in first")),cloudId)
    }
    suspend fun openRecommendation(cloudId: String) {
        val item=tmdb.byCloudId(cloudId) ?: error(tr("Contenido no disponible","Content unavailable"))
        this.open(item)
    }
    // Called only after the UI confirms leaving any existing party.
    suspend fun watchFriendIndependently(friend: FriendActivity) {
        val item=tmdb.byCloudId(friend.cloudId) ?: error(tr("Contenido no disponible","Content unavailable"))
        val ep=if(item.type=="series" && friend.season>0 && friend.episode>0) {
            tmdb.season(item.id,friend.season).firstOrNull { it.season==friend.season && it.episode==friend.episode }
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
        val s=profileSocialSession() ?: return
        if(accessRequestsLoading) return
        val epoch=partyEpoch
        accessRequestsLoading=true
        try {
            val requests=social.requests(s)
            if(isCurrentSocialSession(s) && partyEpoch==epoch) {
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
        val s=profileSocialSession() ?: error(tr("Inicia sesión primero","Sign in first"))
        if(sharedMomentsLoading) return
        sharedMomentsLoading=true
        try {
            val rows=social.receivedMoments(s)
            if(isCurrentSocialSession(s)) {sharedMoments.replaceWith(rows);sharedMomentsOwner=s.userId}
        } finally {sharedMomentsLoading=false}
    }
    suspend fun momentRecipients(): List<com.miflix.native2.data.SocialPerson> {
        val s=profileSocialSession() ?: error(tr("Inicia sesión primero","Sign in first"))
        val rows=social.friends(s).filter {it.status=="accepted"}
        val ids=rows.map {if(it.sender==s.userId) it.receiver else it.sender}.distinct()
        if(ids.isEmpty()) return emptyList()
        val people=social.people(s,ids)
        return if(isCurrentSocialSession(s)) people else emptyList()
    }
    suspend fun sendMoment(req: PlayerRequest,recipient: String,start: Long,end: Long) {
        val s=profileSocialSession() ?: error(tr("Inicia sesión primero","Sign in first"))
        require(!req.live && playerRequest?.playbackId==req.playbackId)
        social.shareMoment(s,recipient,req.playbackId,req.item.cloudId,req.item.title,req.season,req.episode,start,end)
    }
    suspend fun removeSharedMoment(moment: com.miflix.native2.data.SharedMoment) {
        val s=profileSocialSession() ?: return
        social.deleteMoment(s,moment.id)
        if(isCurrentSocialSession(s)) sharedMoments.removeAll {it.id==moment.id}
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
        val s=profileSocialSession() ?: return
        refreshFriends()
        val people=social.activity(s)
        val invitations=try {social.partyInvitations(s)} catch(e: CancellationException) {throw e} catch(_: Exception) {emptyList()}
        if(!isCurrentSocialSession(s)) return
        friendActivity.clear(); friendActivity.addAll(people)
        partyInvitations.clear();partyInvitations.addAll(invitations)
    }
    suspend fun refreshSocialActivity() {
        val s=profileSocialSession() ?: return
        val epoch=partyEpoch
        fun valid()=partyEpoch==epoch && isCurrentSocialSession(s)
        if(cloudIdentityReadyRP83) social.publishAvatar(s,activeProfile.avatarValue ?: ProfileAvatars[(activeProfile.id.hashCode() and Int.MAX_VALUE)%4].key)
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
        val s=profileSocialSession() ?: return@socialAction
        social.decide(s,request,approve); accessRequests.remove(request)
    }
    val friendRows = mutableStateListOf<Friendship>()
    val friendPeople = mutableStateMapOf<String, String>()
    internal var partyBallotRP151 by mutableStateOf<PartyBallotRP151?>(null)
    private var ballotRoomRP151:String?=null
    internal fun currentBallotRP151()=partyBallotRP151.takeIf {ballotRoomRP151==watchParty?.roomCode}
    internal suspend fun ballotVoterRP151()=profileSocialSession()?.userId.orEmpty()
    internal suspend fun sendBallotRP151(body:String) {
        val s=profileSocialSession() ?: return
        val room=watchParty ?: return
        social.send(s,room.roomCode,"chat",body)
    }
    val partyMessages = mutableStateListOf<PartyEvent>()
    val floatingEvents = mutableStateListOf<PartyEvent>()
    val partyPhrases = mutableStateListOf<String>()
    private fun restoreProfileSocialLocalRP1() {
        val defaults=tr("¡Qué buena escena!|Un momento, por favor|¡No spoilers!","What a great scene!|One moment, please|No spoilers!")
        val legacy=if(activeProfile.primary) socialPrefs.getString("phrases",defaults) else defaults
        val value=socialPrefs.getString("phrases_rp1_${session?.userId ?: "local"}:${activeProfile.id}",legacy).orEmpty()
        partyPhrases.clear();partyPhrases.addAll(value.split("|").filter {it.isNotBlank()})
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
    init {appScope.launch(Dispatchers.IO) {runCatching {com.miflix.native2.data.FinalCatalogRP97.rows()}}}

    private val presenceEvents = com.miflix.native2.data.EventPresencePublisher(appContext,social,appScope)
    var socialReadyPlaybackId by mutableStateOf("")
        private set
    var socialEndedPlaybackId by mutableStateOf("")
        private set
    private var socialExpiresAt=0L
    /** Capture the lease once at first frame, never from the progress/seek ticker. */
    fun socialPlaybackStarted(req: PlayerRequest, position: Long, duration: Long) {
        if(easyModeRP79 || playerRequest?.playbackId!=req.playbackId || socialReadyPlaybackId==req.playbackId) return
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
    private var splitPresenceIdRP67=""
    private var splitPresenceExpiresRP67=0L
    fun publishSocialPresenceEvent() {
        if(easyModeRP79) return
        val s=profileSocialSession() ?: return
        val split=multiViewRequests.firstOrNull()?.takeIf {appForeground && screen==Screen.MULTIVIEW}
        if(split?.playbackId!=splitPresenceIdRP67) {
            splitPresenceIdRP67=split?.playbackId.orEmpty()
            splitPresenceExpiresRP67=if(split!=null) System.currentTimeMillis()+4*60*60*1000L else 0L
        }
        val req=split ?: playerRequest?.takeIf { appForeground && (screen==Screen.PLAYER || miniPlayer) &&
            it.playbackId==socialReadyPlaybackId && it.playbackId!=socialEndedPlaybackId }
        presenceEvents.queue(s,req?.item?.title.orEmpty(),req?.item?.cloudId.orEmpty(),
            req?.season ?: 0,req?.episode ?: 0,if(req!=null) watchParty?.roomCode else null,
            if(split!=null) splitPresenceExpiresRP67 else if(req!=null) socialExpiresAt else System.currentTimeMillis())
    }
    fun retrySocialPresenceEvent() { profileSocialSession()?.takeUnless {sessionNeedsSignIn}?.let {presenceEvents.retry(it)} }

    val tmdb = TmdbRepository(BuildConfig.MIFLIX_TMDB_TOKEN).also { it.language=AppLanguagesRP148.locale(interfaceLanguage) }
    val streamRepo = StreamRepository("")
    private val nativeStore=NativeSourceStore(context)
    private val nativeSources=NativeSourceRepository()
    private var nativeRevision by mutableStateOf(0)
    var nativeSourceStatus by mutableStateOf("")
    var nativeSourceSaving by mutableStateOf(false)
    val nativeSourceEnabled: Boolean get() { nativeRevision;return nativeStore.enabled(session?.userId) }
    val nativeSourceConfigured: Boolean get() { nativeRevision;return nativeStore.hasKey(session?.userId) }
    val nativeOnlyRP75:Boolean get() {nativeRevision;return nativeStore.onlyNative(session?.userId)}
    internal val offerNativeSetupRP75:Boolean get() {nativeRevision;return nativeStore.offerSetup(session?.userId)}
    internal fun dismissNativeSetupRP75() {
        session?.userId?.let {nativeStore.dismissSetup(it);nativeRevision++}
    }
    fun setNativeOnlyRP75(value:Boolean) {
        val owner=session?.userId ?: return
        nativeStore.setOnlyNative(owner,value);nativeRevision++;nativeSources.invalidate();invalidatePlaybackWork()
    }
    private var nativeOptionsRevisionRP77 by mutableIntStateOf(0)
    private fun nativeOptionKeyRP77(name:String)="native_rp77:$name:${localExtrasScope()}"
    val nativeSpanishOnlyRP77:Boolean get() {nativeOptionsRevisionRP77;return playbackPrefs.getBoolean(nativeOptionKeyRP77("spanish_only"),true)}
    val nativeSizeAscendingRP77:Boolean get() {nativeOptionsRevisionRP77;return playbackPrefs.getBoolean(nativeOptionKeyRP77("size_ascending"),false)}
    fun saveNativeSpanishOnlyRP77(value:Boolean) {saveNativeOptionRP77("spanish_only",value)}
    fun saveNativeSizeAscendingRP77(value:Boolean) {saveNativeOptionRP77("size_ascending",value)}
    private fun saveNativeOptionRP77(name:String,value:Boolean) {
        playbackPrefs.edit().putBoolean(nativeOptionKeyRP77(name),value).apply()
        nativeOptionsRevisionRP77++;nativeSources.invalidate();invalidatePlaybackWork()
    }
    fun toggleNativeSources() {
        val owner=session?.userId ?: run { error=tr("Inicia sesión primero","Sign in first");return }
        if(!nativeSourceEnabled && nativeStore.token(owner).isBlank()) { error=tr("Guarda primero tu API key de TorBox","Save your TorBox API key first");return }
        nativeStore.enable(owner,!nativeSourceEnabled);nativeRevision++;nativeSources.invalidate();invalidatePlaybackWork()
    }
    suspend fun saveNativeSourceKey(value: String,activateRP75:Boolean=false,onlyNativeRP75:Boolean=true) {
        val owner=session?.userId ?: error(tr("Inicia sesión primero","Sign in first"))
        if(nativeSourceSaving) return
        nativeSourceSaving=true;nativeSourceStatus=""
        try {
            val token=value.trim();require(token.length in 16..256 && token.none { it.isWhitespace() }) { tr("API key inválida","Invalid API key") }
            nativeSources.validate(token)
            if(session?.userId!=owner) return
            nativeStore.save(owner,token)
            nativeStore.dismissSetup(owner)
            if(activateRP75) {nativeStore.setOnlyNative(owner,onlyNativeRP75);nativeStore.enable(owner,true)}
            nativeRevision++;nativeSources.invalidate();invalidatePlaybackWork()
            nativeSourceStatus=if(activateRP75) tr("TorBox conectado. Motor nativo activado.","TorBox connected. Native engine enabled.")
                else tr("Clave verificada y guardada. Puedes activar el motor.","Key verified and saved. You can enable the engine.")
        } finally { nativeSourceSaving=false }
    }
    fun clearNativeSourceKey() {
        session?.userId?.let { nativeStore.clear(it) };nativeRevision++;nativeSources.invalidate();invalidatePlaybackWork();nativeSourceStatus=""
    }
    private data class PlaybackCacheKey(
        val owner: String?,val profile: String,val cloudId: String,val imdb: String,
        val season: Int,val episode: Int,val year: String,val title: String,
        val manifests: List<String>,val nativeEnabled: Boolean,val nativeVersion: Int,val nativeOnly:Boolean,val nativeSpanishOnly:Boolean,val nativeSizeAscending:Boolean,
        val sizeLimit: Long,val audio: String,val secondaryAudio: String,val easy:Boolean
    )
    private val playbackLinkCache=com.miflix.native2.data.PlaybackLinkCache<PlaybackCacheKey,List<StreamChoice>>(12) {
        android.os.SystemClock.elapsedRealtime()
    }
    private val playbackCacheMutex=Mutex()
    private var sourcePrefetchRevisionRP90 by mutableIntStateOf(0)
    private fun sourcePrefetchKeyRP90()="source_prefetch_rp90:${localExtrasScope()}"
    internal val sourcePrefetchEnabledRP90:Boolean get() {sourcePrefetchRevisionRP90;return playbackPrefs.getBoolean(sourcePrefetchKeyRP90(),false)}
    internal fun saveSourcePrefetchRP90(enabled:Boolean) {
        playbackPrefs.edit().putBoolean(sourcePrefetchKeyRP90(),enabled).apply()
        sourcePrefetchRevisionRP90++
        if(!enabled) {discardPreparedVideoRP91();sourceWarmupJobRP90?.cancel();sourceWarmupJobRP90=null;sourceWarmupKeyRP90=null}
    }
    private var sourceWarmupJobRP90:kotlinx.coroutines.Job?=null
    private var sourceWarmupKeyRP90:PlaybackCacheKey?=null
    private data class WarmIdentityRP91(val sources:PlaybackCacheKey,val subtitle:String,val tracksRevision:Int)
    private var preparedVideoRP91:PreparedVideoRP91?=null
    private data class WarmRetryPlanRP92(
        val item:MediaSummary,val imdb:String,val season:Int,val episode:Int,val identity:Any,
        val rows:List<StreamChoice>,val position:Long,val createdAt:Long,
        var next:Int=0,var running:Boolean=false,var job:kotlinx.coroutines.Job?=null,
        val rejected:MutableSet<String> = linkedSetOf()
    )
    private var warmRetryPlanRP92:WarmRetryPlanRP92?=null
    private fun validWarmRetryRP92(plan:WarmRetryPlanRP92)=warmRetryPlanRP92===plan &&
        detailsWarmupKeyRP90(plan.item)==plan.identity && !playerReleaseRP87.hasActive() &&
        android.os.SystemClock.elapsedRealtime()-plan.createdAt in 0 until WarmVideoPolicyRP91.TTL_MS

    internal fun trackSettingsRawRP92(item:MediaSummary,easy:Boolean):String {
        if(item.type!="series" || !localExtras.enabled("tracks")) return ""
        val scope=localExtrasScope()+if(easy) ":easy" else ""
        return runCatching {appContext.getSharedPreferences("brunio_local_extras",Context.MODE_PRIVATE).getString("profile:$scope","{}").orEmpty()}.getOrDefault("")
    }
    private fun warmIdentityRP91(item:MediaSummary,imdb:String,season:Int,episode:Int)=WarmIdentityRP91(
        playbackCacheKey(item,imdb,season,episode),subtitleLanguage,localExtras.revision)
    private fun discardPreparedVideoRP91(keepRetry:Boolean=false) {
        if(!keepRetry) {val plan=warmRetryPlanRP92;warmRetryPlanRP92=null;plan?.job?.cancel()}
        val old=preparedVideoRP91 ?: return
        preparedVideoRP91=null
        old.expiryJob?.cancel()
        old.listener?.let {old.player.removeListener(it)}
        try {old.player.release()} catch(e:Exception) {LocalCrashRP87.record(appContext,e)}
    }
    private fun preparedMatchRP91(request:PlayerRequest):PreparedVideoRP91? {
        val entry=preparedVideoRP91 ?: return null
        if(!sourcePrefetchEnabledRP90 || easyModeRP79 || kidsMode || watchParty!=null || request.live || request.momentEndMs>0 ||
            request.item.cloudId!=entry.request.item.cloudId || request.season!=entry.request.season || request.episode!=entry.request.episode ||
            request.stream.url!=entry.request.stream.url || request.stream.requestHeaders!=entry.request.stream.requestHeaders ||
            entry.identity!=warmIdentityRP91(request.item,entry.imdb,request.season,request.episode) || entry.player.playerError!=null ||
            !WarmVideoPolicyRP91.fresh(android.os.SystemClock.elapsedRealtime()-entry.createdAt,entry.request.resumeMs,request.resumeMs)) return null
        return entry
    }
    internal fun preparedTimingRP91(request:PlayerRequest)=preparedMatchRP91(request)?.timing
    internal fun takePreparedPlayerRP91(request:PlayerRequest):androidx.media3.exoplayer.ExoPlayer? {
        val entry=preparedMatchRP91(request)
        if(entry==null) {discardPreparedVideoRP91();return null}
        preparedVideoRP91=null
        val plan=warmRetryPlanRP92;warmRetryPlanRP92=null;plan?.job?.cancel()
        entry.expiryJob?.cancel()
        entry.listener?.let {entry.player.removeListener(it)}
        entry.gate.activate()
        return entry.player
    }
    private suspend fun prepareVideoRP91(item:MediaSummary,imdb:String,season:Int,episode:Int,identity:Any,streams:List<StreamChoice>) {
        if(detailsWarmupKeyRP90(item)!=identity || playerReleaseRP87.hasActive()) return
        val progressKey=if(item.type=="series") "${item.cloudId}:s${season}e${episode}" else item.cloudId
        val saved=progress[progressKey] ?: progress[item.cloudId]?.takeIf {item.type!="series" || (it.season==season && it.episode==episode)}
        val position=saved?.position ?: 0L
        val memory=if(position>0) lastSourceRP16.get(lastSourceKeyRP16(item,season,episode))?.takeIf {
            StreamOrdering.nativeAllowedRP77(it,audioLanguage,nativeSpanishOnlyRP77) && StreamFilters.allowed(it,item.type,item.year,streamSizeBytes,item.title) &&
                runCatching {java.net.URI(it.url).scheme in setOf("http","https")}.getOrDefault(false)
        } else null
        val bandwidth=if(adaptiveSource) androidx.media3.exoplayer.upstream.DefaultBandwidthMeter.getSingletonInstance(appContext).bitrateEstimate else 0L
        val seconds=(details?.runtimeMinutes ?: 0)*60
        val audio=audioLanguage;val secondary=secondaryAudioLanguageRP62;val order=sourceProviderOrderRP2()
        val ascending=nativeSizeAscendingRP77;val spanish=nativeSpanishOnlyRP77
        val choices=kotlinx.coroutines.withContext(Dispatchers.Default) {
            (memory ?: StreamOrdering.best(streams,audio,bandwidth,seconds,order,secondary,ascending,spanish)) to
                StreamOrdering.fallbackCandidates(streams,audio,bandwidth,seconds,order,secondary,ascending,spanish)
        }
        if(detailsWarmupKeyRP90(item)!=identity || playerReleaseRP87.hasActive()) return
        val chosen=choices.first ?: return
        val alternatives=choices.second
        val ordered=(listOf(chosen)+alternatives).distinctBy {it.url}
        val candidates=ordered.take(WarmRetryPolicyRP92.limit(ordered.size,autoplayNextLink))
        discardPreparedVideoRP91()
        val plan=WarmRetryPlanRP92(item,imdb,season,episode,identity,candidates,position,android.os.SystemClock.elapsedRealtime())
        warmRetryPlanRP92=plan
        plan.job=kotlinx.coroutines.currentCoroutineContext()[kotlinx.coroutines.Job]
        prepareNextWarmRP92(plan)
    }
    private fun queueWarmRetryRP92(plan:WarmRetryPlanRP92) {
        if(!validWarmRetryRP92(plan) || plan.running || plan.next>=plan.rows.size) return
        val task=appScope.launch(start=kotlinx.coroutines.CoroutineStart.LAZY) {
            val current=kotlinx.coroutines.currentCoroutineContext()[kotlinx.coroutines.Job]
            try {prepareNextWarmRP92(plan)} finally {
                if(sourceWarmupJobRP90===current) {sourceWarmupJobRP90=null;sourceWarmupKeyRP90=null}
            }
        }
        plan.job=task;sourceWarmupJobRP90=task
        sourceWarmupKeyRP90=playbackCacheKey(plan.item,plan.imdb,plan.season,plan.episode)
        task.start()
    }
    private suspend fun prepareNextWarmRP92(plan:WarmRetryPlanRP92) {
        if(plan.running) return
        plan.running=true
        try {
            while(validWarmRetryRP92(plan) && plan.next<plan.rows.size) {
                val candidate=plan.rows[plan.next++]
                try {
                    val owner=session?.userId
                    val best=if(candidate.nativeSource==null) candidate else {
                        if(!nativeStore.enabled(owner)) return
                        withTimeoutOrNull(15000L) {nativeSources.materialize(candidate,nativeStore.token(owner)) {validWarmRetryRP92(plan)}}
                            ?: throw java.io.IOException("Warm source preparation timed out")
                    }
                    if(!validWarmRetryRP92(plan)) return
                    discardPreparedVideoRP91(keepRetry=true)
                    val timing=com.miflix.native2.player.SubtitleTiming()
                    val request=PlayerRequest(plan.item,best,plan.season,plan.episode,plan.position)
                    val memory=appContext.getSystemService(Context.ACTIVITY_SERVICE) as? android.app.ActivityManager
                    val gate=WarmLoadControlRP91(lowMemory=memory==null || memory.isLowRamDevice || memory.memoryClass<=128)
                    val player=createWarmPlaybackRP92(appContext,this,request,timing,gate) {validWarmRetryRP92(plan)}
                    if(!validWarmRetryRP92(plan)) {player.release();return}
                    val entry=PreparedVideoRP91(plan.identity,plan.imdb,request,candidate.url,player,timing,gate,android.os.SystemClock.elapsedRealtime())
                    preparedVideoRP91=entry
                    val listener=object:androidx.media3.common.Player.Listener {
                        override fun onPlayerError(error:androidx.media3.common.PlaybackException) {
                            if(preparedVideoRP91!==entry) return
                            plan.rejected.add(candidate.url)
                            discardPreparedVideoRP91(keepRetry=true)
                            queueWarmRetryRP92(plan)
                        }
                    }
                    entry.listener=listener;player.addListener(listener)
                    entry.expiryJob=launch {delay(WarmVideoPolicyRP91.TTL_MS);if(preparedVideoRP91===entry) discardPreparedVideoRP91()}
                    return
                } catch(e:CancellationException) {throw e} catch(_:Exception) {plan.rejected.add(candidate.url)}
            }
        } finally {plan.running=false}
    }

    private fun warmupTargetRP90(item:MediaSummary):Pair<Int,Int> {
        val saved=progress[item.cloudId]
        val target=DetailsPrefetchRP90.target(item.type,saved?.season ?: 0,saved?.episode ?: 0,saved?.percent ?: 0.0)
        return target[0] to target[1]
    }
    internal fun detailsWarmupKeyRP90(item:MediaSummary):Any? {
        if(!sourcePrefetchEnabledRP90 || easyModeRP79 || kidsMode || item.type !in listOf("movie","series") || !appForeground || screen!=Screen.DETAILS || miniPlayer || watchParty!=null || selectedUpcoming || selected?.cloudId!=item.cloudId) return null
        val imdb=details?.takeIf {it.summary.cloudId==item.cloudId}?.imdbId?.takeIf {it.isNotBlank()} ?: return null
        val target=warmupTargetRP90(item)
        return warmIdentityRP91(item,imdb,target.first,target.second)
    }
    internal suspend fun prefetchDetailsRP90(item:MediaSummary,identity:Any) {
        delay(DetailsPrefetchRP90.DELAY_MS)
        // An earlier Play already owns startup; do not start a parallel decoder/materialization.
        if(playbackLoading!=null || detailsWarmupKeyRP90(item)!=identity) return
        val imdb=details?.imdbId ?: return
        val target=warmupTargetRP90(item)
        val key=playbackCacheKey(item,imdb,target.first,target.second)
        val job=kotlinx.coroutines.currentCoroutineContext()[kotlinx.coroutines.Job] ?: return
        sourceWarmupJobRP90?.takeUnless {it===job}?.cancel()
        sourceWarmupJobRP90=job;sourceWarmupKeyRP90=key
        try {
            // Keep source selection/materialization shared with an early Play; video buffers asynchronously.
            val streams=kotlinx.coroutines.withTimeoutOrNull(12000L) {resolveCachedSourcesRP90(item,imdb,target.first,target.second)}.orEmpty()
            if(streams.isNotEmpty()) prepareVideoRP91(item,imdb,target.first,target.second,identity,streams)
        } catch(e:CancellationException) {throw e} catch(_:Exception) {
            // Speculative failure is silent and is never cached; Play can retry normally.
        } finally {
            if(sourceWarmupJobRP90===job) {sourceWarmupJobRP90=null;sourceWarmupKeyRP90=null}
        }
    }
    internal fun endVideoWarmupRP91(identity:Any?) {
        val entry=preparedVideoRP91
        if(entry?.identity!=identity && warmRetryPlanRP92?.identity!=identity) return
        if(screen==Screen.PLAYER && playerRequest?.let {preparedMatchRP91(it)}!=null) return
        discardPreparedVideoRP91()
    }
    internal fun leaveDetailsWarmupRP90(cloudId:String) {
        if(screen!=Screen.PLAYER || playerRequest?.let {preparedMatchRP91(it)}==null) discardPreparedVideoRP91()
        if(screen!=Screen.DETAILS && screen!=Screen.PLAYER && selected?.cloudId==cloudId && playbackLoading?.item?.cloudId==cloudId) invalidatePlaybackWork()
    }

    private fun playbackCacheKey(item: MediaSummary,imdb: String,season: Int,episode: Int)=PlaybackCacheKey(
        session?.userId,activeProfile.id,item.cloudId,imdb,season,episode,item.year,item.title,
        streamRepo.manifests().toList(),nativeSourceEnabled,nativeRevision,nativeOnlyRP75,nativeSpanishOnlyRP77,nativeSizeAscendingRP77,streamSizeBytes,audioLanguage,secondaryAudioLanguageRP62,easyModeRP79)
    private suspend fun resolvePlaybackSources(item: MediaSummary,imdb: String,season: Int,episode: Int):List<StreamChoice> {
        val key=playbackCacheKey(item,imdb,season,episode)
        if(sourceWarmupKeyRP90!=null && sourceWarmupKeyRP90!=key) sourceWarmupJobRP90?.cancel()
        return resolveCachedSourcesRP90(item,imdb,season,episode)
    }
    private suspend fun sortPlaybackRowsRP92(rows:List<StreamChoice>,item:MediaSummary?=null):List<StreamChoice> {
        val audio=audioLanguage;val secondary=secondaryAudioLanguageRP62;val order=sourceProviderOrderRP2()
        val ascending=nativeSizeAscendingRP77;val spanish=nativeSpanishOnlyRP77;val maxBytes=streamSizeBytes
        return kotlinx.coroutines.withContext(Dispatchers.Default) {
            val accepted=if(item==null) rows else rows.filter {StreamFilters.allowed(it,item.type,item.year,maxBytes,item.title)}
            StreamOrdering.sorted(accepted,audio,providerOrder=order,secondaryAudio=secondary,nativeSizeAscending=ascending,nativeSpanishOnly=spanish)
        }
    }
    private suspend fun resolveCachedSourcesRP90(item: MediaSummary,imdb: String,season: Int,episode: Int): List<StreamChoice> = playbackCacheMutex.withLock {
        val key=playbackCacheKey(item,imdb,season,episode)
        playbackLinkCache.get(key)?.let {return@withLock sortPlaybackRowsRP92(it)}
        val streams=fetchPlaybackSources(item,imdb,season,episode)
        // Cache successful lists only; account/settings changes cannot publish stale results.
        if(streams.isNotEmpty() && key==playbackCacheKey(item,imdb,season,episode)) playbackLinkCache.put(key,streams)
        streams
    }
    private suspend fun providerEpisodeRP107(item:MediaSummary,season:Int,episode:Int):Pair<Int,Int> {
        if(!com.miflix.native2.data.DbzEpisodeRP107.enabled(item.type,item.id)) return season to episode
        return com.miflix.native2.data.DbzEpisodeRP107.coordinates(season,episode,tmdb.season(item.id,1))
    }
    private suspend fun fetchPlaybackSources(item:MediaSummary,imdb:String,season:Int,episode:Int):List<StreamChoice> {
        if(com.miflix.native2.data.DbzEpisodeRP107.enabled(item.type,item.id)) {
            val target=providerEpisodeRP107(item,season,episode)
            // DBZ is ALWAYS S1:absolute at the provider boundary; never query saga seasons first.
            return fetchCanonicalSourcesRP102(item,imdb,target.first,target.second)
        }
        val normal=fetchCanonicalSourcesRP102(item,imdb,season,episode)
        if(normal.isNotEmpty() || item.type!="series" || !com.miflix.native2.data.AbsoluteEpisodeRP102.enabled(item.id)) return normal
        val absolute=tmdb.absoluteNumberRP102(item.id,season,episode) ?: return normal
        if(season==1 && episode==absolute) return normal
        // Only an empty result triggers the alternate numbering; never mix two episode identities.
        return fetchCanonicalSourcesRP102(item,imdb,1,absolute)
    }
    private suspend fun fetchCanonicalSourcesRP102(item: MediaSummary,imdb: String,season: Int,episode: Int): List<StreamChoice> = supervisorScope {
        val owner=session?.userId
        if(easyModeRP79) return@supervisorScope StreamOrdering.sorted(
            easyStreamsRP79.resolve(imdb,item.type,season,episode,item.year,streamSizeBytes,item.title,audioLanguage),
            audioLanguage,secondaryAudio=secondaryAudioLanguageRP62)
        val native=owner!=null && nativeStore.enabled(owner)
        // Disabled is the default and leaves the existing add-on path unchanged.
        if(!native) return@supervisorScope sortPlaybackRowsRP92(
            streamRepo.resolve(imdb,item.type,season,episode,item.year,streamSizeBytes,item.title,audioLanguage))
        val token=nativeStore.token(owner)
        val onlyNative=nativeOnlyRP75
        val addon=async {
            if(onlyNative || streamRepo.manifests().isEmpty()) emptyList() else streamRepo.resolve(imdb,item.type,season,episode,item.year,streamSizeBytes,item.title,audioLanguage)
        }
        var failure: String?=null
        val builtIn=try {
            check(token.isNotBlank()) { tr("Vuelve a guardar tu API key de TorBox","Save your TorBox API key again") }
            val detail=details?.takeIf { selected?.cloudId==item.cloudId } ?: tmdb.details(item)
            kotlinx.coroutines.withTimeout(10000) { nativeSources.search(owner!!,token,listOf(detail.originalTitle,
                if(com.miflix.native2.data.StreamLanguagePriority.prefersSpanish(audioLanguage))
                    try {tmdb.spanishTitle(item).ifBlank {item.title}} catch(e: CancellationException) {throw e} catch(_: Exception) {item.title}
                else item.title),item.type,item.year,season,episode,streamSizeBytes,audioLanguage,spanishOnly=nativeSpanishOnlyRP77,sizeAscending=nativeSizeAscendingRP77) }
        } catch(e: kotlinx.coroutines.TimeoutCancellationException) { failure=tr("Motor nativo: tiempo agotado","Native engine: timed out");emptyList() } catch(e: CancellationException) { throw e } catch(e: Exception) { failure=e.message;emptyList() }
        val links=sortPlaybackRowsRP92(addon.await()+builtIn,item)
        if(session?.userId==owner) nativeSourceStatus=failure ?: tr("Motor nativo: ","Native engine: ")+builtIn.size+tr(" enlaces disponibles"," available links")
        if(links.isEmpty() && failure!=null) throw IllegalStateException(failure)
        links
    }
    private var addonLimitRevisionRP7 by mutableIntStateOf(0)
    private fun addonLimitKeyRP7(manifest:String)="addon_limit_rp7:${profileAddonScope()}:${AddonPriorityRP2.id(manifest)}"
    fun addonLimitRP7(manifest:String):Int {
        addonLimitRevisionRP7
        return playbackPrefs.getInt(addonLimitKeyRP7(manifest),com.miflix.native2.data.SourcePaginationRP12.DEFAULT_SIZE).coerceAtLeast(0)
    }
    fun saveAddonLimitRP7(manifest:String,limit:Int) {
        require(limit in 0..1000)
        playbackPrefs.edit().putInt(addonLimitKeyRP7(manifest),limit).apply()
        addonLimitRevisionRP7++;invalidatePlaybackWork()
    }
    internal fun sourcePageSizesRP12():Map<String,Int> =
        streamRepo.manifests().associate {streamRepo.addonLabel(it) to addonLimitRP7(it)}
    private var addonOrderRevision by mutableIntStateOf(0)
    var addonOrderLoading by mutableStateOf(false)
        private set
    private fun addonOrderKey(language:String)="addon_order_rp2:${profileAddonScope()}:${AddonPriorityRP2.language(language)}"
    private fun savedAddonOrder(language:String):List<String> {
        addonOrderRevision
        val a=runCatching {org.json.JSONArray(playbackPrefs.getString(addonOrderKey(language),"[]"))}.getOrDefault(org.json.JSONArray())
        val saved=(0 until a.length()).map {a.optString(it)}.filter {it.length==64}.distinct()
        val migration="mandanguita_order_rp62:${profileAddonScope()}:${AddonPriorityRP2.language(language)}"
        val id=AddonPriorityRP2.id(BundledAddonsRP12.mandanguita)
        if(!playbackPrefs.getBoolean(migration,false) && streamRepo.manifests().contains(BundledAddonsRP12.mandanguita)) {
            val baseline=AddonPriorityRP2.ordered(streamRepo.sourceAddons(),saved,language).map {it.id}.filterNot {it==id}.toMutableList()
            baseline.add(if(com.miflix.native2.data.StreamLanguagePriority.prefersSpanish(language)) 0 else minOf(1,baseline.size),id)
            playbackPrefs.edit().putBoolean(migration,true).putString(addonOrderKey(language),org.json.JSONArray(baseline).toString()).apply()
            return baseline
        }
        return saved
    }
    internal fun orderedSourceAddonsRP2(language:String=audioLanguage):List<InstalledSourceAddonRP2> {
        addonOrderRevision;profileAddonRevision
        return AddonPriorityRP2.ordered(streamRepo.sourceAddons(),savedAddonOrder(language),language)
    }
    private fun sourceProviderOrderRP2()=if(easyModeRP79) easyStreamsRP79.sourceAddonNames() else orderedSourceAddonsRP2().map {it.name}
    suspend fun inspectAddonOrderRP2() {
        val scope=profileAddonScope();val manifests=streamRepo.manifests()
        addonOrderLoading=true
        try {streamRepo.inspectSourceAddons();if(scope==profileAddonScope() && manifests==streamRepo.manifests()) addonOrderRevision++}
        finally {addonOrderLoading=false}
    }
    internal fun moveSourceAddonRP2(id:String,delta:Int,language:String) {
        val rows=orderedSourceAddonsRP2(language).toMutableList();val index=rows.indexOfFirst {it.id==id};val target=index+delta
        if(index<0 || target !in rows.indices) return
        java.util.Collections.swap(rows,index,target)
        playbackPrefs.edit().putString(addonOrderKey(language),org.json.JSONArray(rows.map {it.id}).toString()).apply()
        addonOrderRevision++;invalidatePlaybackWork()
    }
    fun resetSourceAddonOrderRP2(language:String) {
        playbackPrefs.edit().remove(addonOrderKey(language)).apply();addonOrderRevision++;invalidatePlaybackWork()
    }
    val addonManifests=mutableStateListOf<String>()
    private var primaryAddonManifests:List<String> = emptyList()
    private var profileAddonRevision by mutableIntStateOf(0)
    private fun profileAddonScope(profileId:String=activeProfile.id)="${session?.userId ?: "local"}:$profileId"
    val profileAddonsInherited:Boolean get() {profileAddonRevision;return activeProfile.primary || playbackPrefs.getBoolean("addon_inherit:${profileAddonScope()}",true)}
    private fun profileOwnAddons(profileId:String):List<String> {
        val a=runCatching {org.json.JSONArray(playbackPrefs.getString("profile_addons:${profileAddonScope(profileId)}","[]"))}.getOrDefault(org.json.JSONArray())
        return (0 until a.length()).map {a.optString(it)}.filter {it.startsWith("https://") || it.startsWith("http://")}.distinct()
    }
    private val bundledManifestsRP4=BundledAddonsRP12.defaults
    private val retiredBundledManifestsRP12=BundledAddonsRP12.retired
    private fun bundledScopeRP12(ownProfile:Boolean)=if(ownProfile) profileAddonScope() else "${session?.userId ?: "local"}:account"
    private fun retiredAddonKeyRP12(manifest:String,ownProfile:Boolean = !profileAddonsInherited)=
        "retired_addon_optin_rp12:${bundledScopeRP12(ownProfile)}:${AddonPriorityRP2.id(manifest)}"
    private fun bundledRemovedKeyRP4(manifest:String,ownProfile:Boolean = !profileAddonsInherited):String =
        "bundled_removed_rp4:${bundledScopeRP12(ownProfile)}:${AddonPriorityRP2.id(manifest)}"
    private fun effectiveBundledAddonsRP4(values:List<String>):List<String> =
        BundledAddonsRP12.effective(values,
            bundledManifestsRP4.filter {playbackPrefs.getBoolean(bundledRemovedKeyRP4(it),false)}.toSet(),
            retiredBundledManifestsRP12.filter {playbackPrefs.getBoolean(retiredAddonKeyRP12(it),false)}.toSet())
    private fun applyEffectiveManifests(values:List<String>) {
        invalidatePlaybackWork()
        streamRepo.setManifests(values)
        addonManifests.clear();addonManifests.addAll(streamRepo.manifests())
    }
    private fun applyProfileAddons() {
        applyEffectiveManifests(effectiveBundledAddonsRP4(if(profileAddonsInherited) primaryAddonManifests else profileOwnAddons(activeProfile.id)))
        profileAddonRevision++
    }
    private fun clearAccountManifests() {primaryAddonManifests=emptyList();applyEffectiveManifests(emptyList())}
    private fun setAccountManifests(values: List<String>) {primaryAddonManifests=values.distinct();applyProfileAddons()}
    fun saveProfileAddonInheritance(inherit:Boolean) {
        if(activeProfile.primary) return
        val scope=profileAddonScope();val edit=playbackPrefs.edit().putBoolean("addon_inherit:$scope",inherit)
        if(!inherit && !playbackPrefs.contains("profile_addons:$scope")) edit.putString("profile_addons:$scope",org.json.JSONArray(streamRepo.manifests()).toString())
        edit.apply();applyProfileAddons()
    }
    private fun saveOwnProfileAddons(values:List<String>) {
        val scope=profileAddonScope()
        playbackPrefs.edit().putBoolean("addon_inherit:$scope",false).putString("profile_addons:$scope",org.json.JSONArray(values.distinct()).toString()).apply()
        applyProfileAddons()
    }

    internal var normalLiveEntryRP85 by mutableStateOf(false)
    internal var liveAutoEntryRP83 by mutableStateOf(false)
    internal var livePanelPlaybackRP83:String?=null
    private var currentScreen by mutableStateOf(Screen.SPLASH)
    var screen: Screen
        get()=currentScreen
        set(value) {
            val destination=if(easyModeRP79 && !com.miflix.native2.data.EasyModePolicyRP79.allowsScreen(value.name)) Screen.HOME else value
            if(destination==Screen.LIVE_TV && currentScreen!=Screen.PLAYER) {
                if(easyModeRP79) {if(currentScreen!=Screen.LIVE_TV) liveAutoEntryRP83=true}
                else {normalLiveEntryRP85=true;liveAutoEntryRP83=false}
            }
            if(destination!=Screen.LIVE_TV && destination!=Screen.PLAYER) liveAutoEntryRP83=false
            currentScreen=destination
            if(destination==Screen.FRIENDS) refreshFriendsTab()
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
        franchiseBrand=brand;franchiseType=com.miflix.native2.data.FranchiseCatalogRP29.initialType(brand);franchiseItems.clear();franchisePage=0
        franchiseMore=false;franchiseError=""
        franchiseScrollIndex=0;franchiseScrollOffset=0;screen=Screen.FRANCHISE
        loadFranchise(franchiseType)
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
    internal fun restoreCustomFilterRC100() {
        val f=if(localExtras.enabled("filters")) localExtras.preference(ratingsKey(),"filter:custom") else org.json.JSONObject()
        customFilter=runCatching {CustomCatalogFilter().copy(
            type=f.optString("type","movie"),fromYear=f.optString("fromYear"),toYear=f.optString("toYear"),
            sort=f.optString("sort","popular"),minimumRating=f.optInt("minimumRating",CustomCatalogFilter().minimumRating),
            genre=f.optJSONObject("genre")?.let {CatalogGenre(it.getInt("id"),it.getString("name"))}
        )}.getOrDefault(CustomCatalogFilter())
        appliedCustomFilter=null;customFilterResults.clear();customFilterPage=0;customFilterScrollIndex=0;customFilterScrollOffset=0
    }
    suspend fun applyCustomFilter(filter: CustomCatalogFilter,more: Boolean=false) {
        if(customFilterLoading) return
        customFilterLoading=true;customFilterError=""
        try {
            filter.validate()
            val filterOwner=ratingsKey();val filterLanguage=interfaceLanguage
            val page=if(more) customFilterPage+1 else 1
            val result=tmdb.customCatalog(filter,page)
            if(filterOwner!=ratingsKey() || filterLanguage!=interfaceLanguage) return
            if(localExtras.enabled("filters")) localExtras.savePreference(filterOwner,"filter:custom",org.json.JSONObject()
                .put("type",filter.type).put("fromYear",filter.fromYear).put("toYear",filter.toYear).put("sort",filter.sort)
                .put("minimumRating",filter.minimumRating).put("genre",filter.genre?.let {org.json.JSONObject().put("id",it.id).put("name",it.name)} ?: org.json.JSONObject.NULL))
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
        if(context !is GuestContextRP150) Http.authorize={ url,headers,force ->
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
    var myListLimit by mutableStateOf(15)
    var myListLoading by mutableStateOf(false)
    var myListMessage by mutableStateOf("")
    val myListTitles=mutableStateListOf<MediaSummary>()
    private var myListOwner=""
    private var myListGeneration=0
    private val myListCache=mutableMapOf<String,MediaSummary>()
    suspend fun loadMyListPage(owner: String,ids: List<String>) = coroutineScope {
        if(myListOwner!=owner) { myListOwner=owner;myListLimit=15;myListCache.clear();myListTitles.clear() }
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
    var continueHomeReadyRP129 by mutableStateOf(false)
        private set
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
                    currentSeason=if(com.miflix.native2.data.AbsoluteEpisodeRP102.enabled(next.item.id)) 1 else next.season.coerceAtLeast(1);episodes.replaceWith(rows)
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

    var previousLiveChannelEnabled by mutableStateOf(playbackPrefs.getBoolean("previous_live_channel",true))
        private set
    private val previousLiveChannels=com.miflix.native2.player.PreviousLiveChannelHistory<PlayerRequest>()
    private var previousLiveRevision by mutableIntStateOf(0)
    private fun liveHistoryKey(req:PlayerRequest)=if(req.liveProvider=="lidom") "lidom:${req.lidomTeam.ifBlank {req.item.cloudId}}" else "${req.liveProvider}:${req.liveChannelId.ifBlank {req.item.cloudId}}"
    fun savePreviousLiveChannelEnabled(enabled:Boolean) {
        previousLiveChannelEnabled=enabled
        playbackPrefs.edit().putBoolean("previous_live_channel",enabled).apply()
        if(!enabled) {previousLiveChannels.clear();previousLiveRevision++}
        else playerRequest?.takeIf {it.playbackId==socialReadyPlaybackId}?.let {recordSuccessfulLiveChannel(it)}
    }
    fun recordSuccessfulLiveChannel(req:PlayerRequest) {
        if(!previousLiveChannelEnabled || !req.live || playerRequest?.playbackId!=req.playbackId || kidsMode) return
        previousLiveChannels.started(ratingsKey(),liveHistoryKey(req),req)
        previousLiveRevision++
    }
    fun previousLiveChannel(req:PlayerRequest):PlayerRequest? {
        previousLiveRevision
        if(!previousLiveChannelEnabled || !req.live || kidsMode) return null
        return previousLiveChannels.previous(ratingsKey(),liveHistoryKey(req))
    }
    fun returnToPreviousLiveChannel(req:PlayerRequest) {
        if(!req.live || playerRequest?.playbackId!=req.playbackId || partyRole==PartyRole.GUEST || kidsMode) return
        val previous=previousLiveChannel(req) ?: return
        // Reuse the last successful request, but start a new live playback at its live edge.
        openLivePlayer(previous.copy(playbackId=UUID.randomUUID().toString(),resumeMs=0L,sourceFallbackPlaying=null,sportsFailedRP137=emptySet(),sportsRefreshedRP137=false))
    }
    internal fun sportsRequestRP71(req:PlayerRequest,rows:List<StreamChoice>):PlayerRequest {
        val choices=rows.distinctBy {it.url}.take(com.miflix.native2.data.SportsRecoveryRP137.MAX_LINKS)
        require(choices.isNotEmpty()) {tr("No hay enlaces disponibles","No links available")}
        return req.copy(stream=choices.first(),sportsLinksRP71=choices,sportsAttemptRP71=0,sportsOwnerRP71="${session?.userId ?: "local"}:${activeProfile.id}",sportsFailedRP137=emptySet(),sportsRefreshedRP137=false)
    }
    private val sportsRecoveriesRP137=mutableSetOf<String>()
    private fun sportsRecoveryCurrentRP137(req:PlayerRequest):PlayerRequest? {
        val current=playerRequest ?: return null
        return current.takeIf {it.playbackId==req.playbackId && it.live && !it.sportsRecordingRP72 && it.liveProvider=="sports" &&
            screen==Screen.PLAYER && appForeground && !kidsMode && partyRole!=PartyRole.GUEST &&
            it.sportsOwnerRP71=="${session?.userId ?: "local"}:${activeProfile.id}"}
    }
    /** Main-thread ownership checks bracket the IO; stale resolutions cannot switch games. */
    internal suspend fun recoverSportsSourceRP137(req:PlayerRequest,allowed:()->Boolean={true}):Boolean {
        if(!allowed()) return false
        var current=sportsRecoveryCurrentRP137(req) ?: return false
        if(!sportsRecoveriesRP137.add(req.playbackId)) return true
        try {
            var failed=current.sportsFailedRP137+current.stream.url
            var rows=current.sportsLinksRP71.ifEmpty {listOf(current.stream)}
            var index=SportsRecoveryRP137.next(rows.map {it.url},rows.indexOfFirst {it.url==current.stream.url},failed)
            var refreshed=current.sportsRefreshedRP137
            if(index<0 && !refreshed) {
                refreshed=true
                // The in-flight set prevents duplicate resolvers; commit the budget only if still active.
                val manifest=calendarLiveManifest("sports")
                rows=try {
                    kotlinx.coroutines.withContext(Dispatchers.IO) {
                        LiveRepository().streams(manifest,LiveChannel(current.liveChannelId,current.liveType,current.item.title,current.item.backdrop,current.item.overview),forceRefreshRP137=true)
                    }.distinctBy {it.url}.take(SportsRecoveryRP137.MAX_LINKS)
                } catch(e:CancellationException) {throw e} catch(_:Exception) {emptyList()}
                kotlinx.coroutines.currentCoroutineContext().ensureActive()
                current=sportsRecoveryCurrentRP137(req) ?: return false
                if(calendarLiveManifest("sports")!=manifest) return false
                // A fresh batch may renew authorization headers even when a URL is unchanged.
                failed=emptySet()
                index=SportsRecoveryRP137.next(rows.map {it.url},-1,failed)
            }
            current=sportsRecoveryCurrentRP137(req) ?: return false
            if(!allowed()) return false
            if(index<0) {playerRequest=current.copy(sportsFailedRP137=failed,sportsRefreshedRP137=refreshed);return false}
            if(rows[index].url==current.stream.url) {delay(450);current=sportsRecoveryCurrentRP137(req) ?: return false}
            if(!allowed()) return false
            openLivePlayer(current.copy(stream=rows[index],sportsLinksRP71=rows,sportsAttemptRP71=index,sportsFailedRP137=failed,
                sportsRefreshedRP137=refreshed,playbackId=UUID.randomUUID().toString(),resumeMs=0L,liveMime=""))
            return true
        } finally {sportsRecoveriesRP137.remove(req.playbackId)}
    }
    internal fun sportsHealthyRP137(req:PlayerRequest) {
        val current=sportsRecoveryCurrentRP137(req) ?: return
        if(current.sportsFailedRP137.isNotEmpty() || current.sportsRefreshedRP137)
            playerRequest=current.copy(sportsFailedRP137=emptySet(),sportsRefreshedRP137=false)
    }
    fun openLivePlayer(req: PlayerRequest) {
        require(!kidsMode) {tr("TV en vivo no está disponible en Kids.","Live TV is not available in Kids.") }
        if(!rapidPlaybackRP87.accept("${localExtrasScope()}:live:${req.liveProvider}:${req.liveChannelId}:${req.stream.url}",android.os.SystemClock.uptimeMillis())) return
        val scopedSportsRP137=if(req.live && req.liveProvider=="sports" && !req.sportsRecordingRP72 && req.sportsOwnerRP71.isBlank())
            req.copy(sportsOwnerRP71="${session?.userId ?: "local"}:${activeProfile.id}",sportsLinksRP71=req.sportsLinksRP71.ifEmpty {listOf(req.stream)}) else req
        val canonicalRP97=if(req.liveChannelId.startsWith(com.miflix.native2.data.FinalPlaylistRP97.PREFIX)) req.copy(liveProvider=CustomChannelsRP45.provider) else scopedSportsRP137
        val playback=if(req.liveProvider=="sports" && com.miflix.native2.data.SportsRecordingPolicyRP72.recording(req.liveChannelId,req.item.title,req.item.overview)) req.copy(sportsRecordingRP72=true) else canonicalRP97
        invalidatePlaybackWork()
        if(req.liveProvider.isNotBlank() && req.liveProvider!="lidom" && liveProviderTab!=req.liveProvider) { liveProviderTab=req.liveProvider;liveCategoryId=null }
        miniPlayer=false;selected=playback.item;playerRequest=playback;screen=Screen.PLAYER
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
    fun searchHistory(): List<String> = if(easyModeRP79) emptyList() else runCatching {
        val a=org.json.JSONArray(playbackPrefs.getString(searchHistoryOwner(),"[]"))
        (0 until a.length()).map { a.getString(it) }.take(10)
    }.getOrDefault(emptyList())
    fun rememberSearch(query: String) {
        if(easyModeRP79) return
        val next=com.miflix.native2.data.SearchHistory.remember(searchHistory(),query)
        playbackPrefs.edit().putString(searchHistoryOwner(),org.json.JSONArray(next).toString()).apply()
    }
    var searchQuery by mutableStateOf("")
    private var currentPlayerRequestRP87 by mutableStateOf<PlayerRequest?>(null)
    var playerRequest:PlayerRequest?
        get()=currentPlayerRequestRP87
        set(value) {
            if(value==null || preparedMatchRP91(value)==null) discardPreparedVideoRP91()
            if(value?.playbackId!=currentPlayerRequestRP87?.playbackId) {
                try {playerReleaseRP87.beforeReplace(value!=null)} catch(e:Exception) {LocalCrashRP87.record(appContext,e)}
            }
            currentPlayerRequestRP87=value
        }
    internal var playbackLoading by mutableStateOf<PlaybackLoading?>(null)
        private set
    fun beginPlaybackLoading(item: MediaSummary,season: Int=0,episode: Int=0,art: String?=null) {
        val knownLogo=details?.takeIf {it.summary.cloudId==item.cloudId}?.logo
        val load=PlaybackLoading(item,season,episode,item.backdrop ?: item.poster,logo=knownLogo)
        playbackLoading=load
        if(knownLogo.isNullOrBlank()) launch {
            try {
                val logo=tmdb.browseLogo(item)
                val current=playbackLoading
                if(current!=null && current.artworkId==load.artworkId && !logo.isNullOrBlank()) playbackLoading=current.copy(logo=logo)
            } catch(e:CancellationException) {throw e} catch(_:Exception) { }
        }
    }
    fun abortPlaybackLoading() {playbackLoading=null}
    fun cancelPlaybackStart() {
        val target=playbackLoading?.item ?: playerRequest?.item ?: selected
        invalidatePlaybackWork();playerRequest=null;miniPlayer=false
        if(target==null) {screen=Screen.HOME;return}
        selected=target;screen=Screen.DETAILS
        if(details?.summary?.cloudId!=target.cloudId) {
            details=null
            val owner=session?.userId;val profile=activeProfile.id
            launch {if(selected?.cloudId==target.cloudId && screen==Screen.DETAILS && session?.userId==owner && activeProfile.id==profile) open(target)}
        }
    }
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
        "paramount" to listOf(531,582),
        "amcplus" to listOf(526,528),
        "crunchyroll" to listOf(283)
    )

    fun launch(block: suspend CoroutineScope.() -> Unit) = appScope.launch {
        try { block() } catch(e: CancellationException) { throw e } catch(e: Exception) {playbackLoading=null; error=e.message ?: tr("No se pudo completar la operación","Could not complete the operation") }
    }

    private var easyRevisionRP79 by mutableIntStateOf(0)
    val easyModeRP79:Boolean get() {
        easyRevisionRP79
        return !kidsMode && playbackPrefs.getBoolean(com.miflix.native2.data.EasyModePolicyRP79.key(session?.userId,activeProfile.id),false)
    }
    private var easySpanishRevisionRP83 by mutableIntStateOf(0)
    val easySpanishOnlyRP83:Boolean get() {easySpanishRevisionRP83;return playbackPrefs.getBoolean("easy_spanish_rp83:${localExtrasScope()}",true)}
    fun saveEasySpanishOnlyRP83(value:Boolean) {
        playbackPrefs.edit().putBoolean("easy_spanish_rp83:${localExtrasScope()}",value).apply()
        easySpanishRevisionRP83++;invalidatePlaybackWork()
    }
    private val easyStreamsRP79=StreamRepository("").apply {
        setManifests(com.miflix.native2.data.EasyModePolicyRP79.sourceManifests())
    }
    private var easyHomeOwnerRP79=""
    private fun easySnapshotKeyRP79()=com.miflix.native2.data.EasyModePolicyRP79.key(session?.userId,activeProfile.id)+":watching"
    private fun saveEasySnapshotRP79() {
        val rows=org.json.JSONObject()
        progress.entries.sortedByDescending {it.value.updatedAt}.take(1000).forEach {(key,p) ->rows.put(key,
            org.json.JSONObject().put("percent",p.percent).put("position",p.position).put("duration",p.duration)
                .put("updatedAt",p.updatedAt).put("season",p.season).put("episode",p.episode))}
        playbackPrefs.edit().putString(easySnapshotKeyRP79(),org.json.JSONObject().put("progress",rows)
            .put("favorites",org.json.JSONArray(favorites.toList())).toString()).apply()
    }
    private fun restoreEasySnapshotRP79() {
        if(!easyModeRP79) return
        val body=runCatching {org.json.JSONObject(playbackPrefs.getString(easySnapshotKeyRP79(),"{}").orEmpty())}.getOrNull() ?: return
        val rows=body.optJSONObject("progress") ?: return
        rows.keys().forEach {key ->runCatching {
            val p=rows.getJSONObject(key)
            val saved=PlaybackProgress(p.getDouble("percent"),p.getLong("position"),p.getLong("duration"),p.getLong("updatedAt"),p.optInt("season"),p.optInt("episode"))
            if(saved.updatedAt>(progress[key]?.updatedAt ?: 0L)) progress[key]=saved
        }}
        if(session==null) body.optJSONArray("favorites")?.let {a ->
            favorites.clear();favorites.addAll((0 until a.length()).map {a.getString(it)})
        }
    }
    suspend fun setEasyModeRP79(enabled:Boolean) {
        if(kidsMode) return
        val owner=session?.userId;val profile=activeProfile.id
        val key=com.miflix.native2.data.EasyModePolicyRP79.key(owner,profile)
        val actor=profileSocialSession()
        if(enabled) leaveWatchParty()
        if(owner!=session?.userId || profile!=activeProfile.id) return
        playbackPrefs.edit().putBoolean(key,enabled).apply();easyRevisionRP79++
        invalidatePlaybackWork();sourceSelection=null;contextTitle=null;socialProfileEditor=false
        localCastPairing=false;profilePhotoEditor=null;miniPlayer=false;playerRequest=null
        showLiveScoresRP11=false;pendingPartyCode=null;randomEpisodeCloud=null
        partyClosedNotice=false;partyJoinedNotice=null;changelogOpen=false
        screen=Screen.HOME
        if(enabled) {saveEasySnapshotRP79();presenceEvents.stopForEasyMode(actor)}
        else {extrasLoaded=false;continueHomeReadyRP129=false;launch {flushSettingsRP83()};launch {if(!easyModeRP79) loadHome()}}
    }
    suspend fun loadEasyHomeRP79() = supervisorScope {
        val owner="${localExtrasScope()}:${interfaceLanguage}"
        if(!easyModeRP79 || tmdb.token.isBlank() || easyHomeOwnerRP79==owner) return@supervisorScope
        easyHomeOwnerRP79=owner
        restoreEasySnapshotRP79()
        val a=async {runCatching {tmdb.popularMovies()}}
        val b=async {runCatching {tmdb.popularSeries()}}
        val c=async {runCatching {resolveContinueWatching()}}
        fun current()=easyModeRP79 && owner=="${localExtrasScope()}:${interfaceLanguage}"
        val movieResult=a.await();val seriesResult=b.await()
        movieResult.onSuccess {if(current()) movies.replaceWith(it)}
        seriesResult.onSuccess {if(current()) series.replaceWith(it)}
        c.await().onSuccess {if(current()) continueWatching.replaceWith(it)}
        if(movieResult.isFailure || seriesResult.isFailure) easyHomeOwnerRP79=""
    }
    private val easyMlbByOwnerRP80=mutableStateMapOf<String,Boolean>()
    var easyMlbRP80:Boolean
        get()=easyMlbByOwnerRP80[localExtrasScope()] ?: false
        set(value) {easyMlbByOwnerRP80[localExtrasScope()]=value}
    val easyRowsRP80=mutableStateMapOf<String,List<MediaSummary>>()
    var easyRowsLoadingRP80 by mutableStateOf(false)
        private set
    var easyRowsMessageRP80 by mutableStateOf("")
        private set
    private var easyRowsOwnerRP80 by mutableStateOf("")
    private var easyRowsAttemptedRP80=false
    private val easyRowsMutexRP80=Mutex()
    private fun easyCatalogOwnerRP80()="${localExtrasScope()}:${tmdb.language}"
    val easyRowsMatchRP80:Boolean get()=easyRowsOwnerRP80==easyCatalogOwnerRP80()
    suspend fun loadEasyRowsRP80(retry:Boolean=false) = easyRowsMutexRP80.withLock {
        if(!easyModeRP79 || tmdb.token.isBlank()) return@withLock
        val owner=easyCatalogOwnerRP80()
        if(easyRowsOwnerRP80!=owner) {
            easyRowsRP80.clear();easyRowsOwnerRP80=owner;easyRowsAttemptedRP80=false
        }
        if(easyRowsAttemptedRP80 && !retry) return@withLock
        val pending=EasyHomeRowsRP80.rows.filter {it.id !in easyRowsRP80}
        if(pending.isEmpty()) return@withLock
        easyRowsLoadingRP80=true;easyRowsMessageRP80=""
        var completed=false
        fun current()=easyModeRP79 && owner==easyCatalogOwnerRP80() && owner==easyRowsOwnerRP80
        try {
            val permits=Semaphore(3)
            supervisorScope {
                pending.map {row ->async {permits.withPermit {
                    if(!current()) return@withPermit
                    val titles=try {row.load(tmdb).filter {it.type=="movie" || it.type=="series"}.distinctBy {it.cloudId}.take(20)}
                        catch(e:CancellationException) {throw e} catch(_:Exception) {emptyList()}
                    if(current() && titles.isNotEmpty()) easyRowsRP80[row.id]=titles
                }}}.awaitAll()
            }
            completed=true
            if(current()) {
                easyRowsAttemptedRP80=true
                if(EasyHomeRowsRP80.rows.any {it.id !in easyRowsRP80}) easyRowsMessageRP80=tr(
                    "Algunas filas no pudieron cargar. Puedes volver a intentarlo.",
                    "Some rows could not load. You can try again.")
            }
        } finally {
            if(owner==easyRowsOwnerRP80) {
                easyRowsLoadingRP80=false
                if(!completed) easyRowsAttemptedRP80=false
            }
        }
    }
    suspend fun easyResumeRP79(item:MediaSummary) {
        this.open(item)
        if(!easyModeRP79 || selected?.cloudId!=item.cloudId || details==null || selectedUpcoming) return
        if(item.type!="series") playMovie()
        else {
            val saved=progress[item.cloudId]
            val season=saved?.season?.coerceAtLeast(1) ?: 1
            if(currentSeason!=season) loadSeason(season)
            val episode=episodes.firstOrNull {it.season==(saved?.season ?: 1) && it.episode==(saved?.episode ?: 1)} ?: episodes.firstOrNull()
            episode?.let {playEpisode(it)}
        }
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
        applyProfileInterfaceLanguage()
        applyProfileAddons()
        restoreCustomFilterRC100()
        restoreProfileSocialLocalRP1()
    }
    suspend fun bootstrap() {
        if(!playbackPrefs.getBoolean("direct_only_cleanup_rp62",false)) {
            kotlinx.coroutines.withContext(Dispatchers.IO) {
                val cleaned=java.io.File(appContext.cacheDir,"p2p-rp14").deleteRecursively()
                if(cleaned) {
                    appContext.getSharedPreferences("brunio_p2p",Context.MODE_PRIVATE).edit().clear().apply()
                    playbackPrefs.edit().putBoolean("direct_only_cleanup_rp62",true).apply()
                }
            }
        }
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
        session?.takeUnless {easyModeRP79}?.let { account ->
            runCatching { trakt.call(account,"status").optBoolean("connected") }.onSuccess { traktConnected=it }
            profileSocialSession(account)?.let {actor ->runCatching {val name=social.nickname(actor);if(isCurrentSocialSession(actor)) nickname=name}}
            val actor=profileSocialSession(account)
            val room = socialPrefs.getString(socialRoomKey(), "").orEmpty()
            if (room.isNotBlank() && actor!=null) runCatching {
                cloud.readWatchParty(actor, room)?.let { party ->
                    watchParty = party; partyRole = if (party.hostUserId == actor.userId) PartyRole.HOST else PartyRole.GUEST
                    social.join(actor, room)
                }
            }
        }
        screen = if (tmdb.token.isBlank()) Screen.SETTINGS else if(rememberLastProfile || easyModeRP79) Screen.HOME else Screen.PROFILES
    }

    suspend fun login(email: String, password: String) {
        busyMessage = tr("Iniciando sesión…","Signing in…")
        error = null
        runCatching { cloud.login(email, password) }.onSuccess {
            clearAccountManifests()
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
        cloudIdentityReadyRP83=false
        clearAccountManifests()
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
        val wanted = playbackPrefs.getString("selected_profile_rp83:${s.userId}",null)?.takeIf {id ->profiles.any {it.id==id}}
            ?: cloud.lastProfileRP83 ?: local.profileId()
        activeProfile = profiles.firstOrNull { it.id == wanted }
            ?: profiles.firstOrNull { it.primary }
            ?: profiles.first()
        restoreSettingsRP83()
        applyProfileInterfaceLanguage()
        applyProfileAddons()
        restoreCustomFilterRC100()
        restoreProfileSocialLocalRP1()
        local.saveProfileId(activeProfile.id)
        playbackPrefs.edit().putString("selected_profile_rp83:${s.userId}",activeProfile.id).apply()

        val (fav, prog) = cloud.profileState(s, activeProfile.id)
        favorites.clear(); favorites.addAll(fav)
        progress.clear(); progress.putAll(prog)
        easyHomeOwnerRP79=""
        extrasLoaded=false;continueHomeReadyRP129=false
        cloudIdentityReadyRP83=true
        flushSettingsRP83()
    }

    suspend fun selectProfile(p: Profile) {
        flushSettingsRP83()
        cloudIdentityReadyRP83=false
        easyHomeOwnerRP79=""
        if(p.id!=activeProfile.id) {
            profileSocialSession()?.let {presenceEvents.queue(it,"","",0,0,null,System.currentTimeMillis())}
            leaveWatchParty()
            nickname="";friendRows.clear();friendPeople.clear();friendActivity.clear();partyInvitations.clear();accessRequests.clear();sharedMoments.clear();sharedMomentsOwner=""
            calendarEvents.clear();partyMembers.clear();partyMessages.clear();floatingEvents.clear()
        }
        invalidatePlaybackWork()
        playerRequest=null;sourceSelection=null;miniPlayer=false;selected=null;details=null;profilePhotoEditor=null
        favorites.clear();progress.clear();continueWatching.clear();kidsContinue.clear();notifications.clear()
        activeProfile = p
        restoreSettingsRP83()
        applyProfileInterfaceLanguage()
        applyProfileAddons()
        restoreCustomFilterRC100()
        restoreProfileSocialLocalRP1()
        randomEpisodeCloud=null
        contextTitle=null;socialProfileEditor=false;localCastPairing=false
        if(kidsMode) detachParty()
        local.saveProfileId(p.id)
        session?.let {playbackPrefs.edit().putString("selected_profile_rp83:${it.userId}",p.id).apply()}
        session?.let {
            val (fav, prog) = cloud.profileState(it, p.id)
            favorites.clear(); favorites.addAll(fav)
            progress.clear(); progress.putAll(prog)
        }
        extrasLoaded=false;continueHomeReadyRP129=false
        cloudIdentityReadyRP83=session!=null
        easyHomeOwnerRP79=""
        flushSettingsRP83(force=true)
        if(easyModeRP79) loadEasyHomeRP79()
        screen = Screen.HOME
    }

    suspend fun createProfile(name: String, avatar: String = "ai:astronaut",inheritMainAddons:Boolean=true) {
        val clean = name.trim().take(24)
        if (clean.isBlank()) return
        val createOwner=session?.userId
        val p = Profile(UUID.randomUUID().toString(), clean, avatarValue=avatar, primary = false)
        val next=profiles.toList()+p
        session?.let {
            cloud.upsertProfiles(it, next)
            cloud.upsertProfile(it, p.id, emptySet(), emptyMap())
        }
        if(session?.userId!=createOwner) return
        val addonScope=profileAddonScope(p.id)
        playbackPrefs.edit().putBoolean("addon_inherit:$addonScope",inheritMainAddons)
            .putString("profile_addons:$addonScope","[]").apply()
        profiles.add(p)
        if(session==null) local.saveOfflineProfiles(profiles.toList())
        selectProfile(p)
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
        val clean=url.trim().replace("|","%7C")
        if(clean.isBlank()) return
        require(clean.startsWith("https://") || clean.startsWith("http://")) { tr("Usa una URL HTTP válida","Use a valid HTTP URL") }
        val normalized=if(clean.endsWith("/manifest.json")) clean else clean.trimEnd('/')+"/manifest.json"
        val next=(streamRepo.manifests()+normalized).distinct()
        if(!activeProfile.primary) {
            if(normalized in bundledManifestsRP4) playbackPrefs.edit().remove(bundledRemovedKeyRP4(normalized,true)).apply()
            if(normalized in retiredBundledManifestsRP12) playbackPrefs.edit().putBoolean(retiredAddonKeyRP12(normalized,true),true).apply()
            saveOwnProfileAddons(next);return
        }
        cloud.upsertPrivateSetup(owner,PrivateSetup(tmdb.token,next.firstOrNull().orEmpty(),next))
        if(session?.userId==owner.userId) {
            if(normalized in bundledManifestsRP4) playbackPrefs.edit().remove(bundledRemovedKeyRP4(normalized,false)).apply()
            if(normalized in retiredBundledManifestsRP12) playbackPrefs.edit().putBoolean(retiredAddonKeyRP12(normalized,false),true).apply()
            setAccountManifests(next)
        }
    }
    suspend fun removeAddonManifest(manifest: String) {
        val owner=session
        val next=streamRepo.manifests().filterNot { it==manifest }
        if(owner==null) {
            // Factory opt-outs stay local and must also work without an account.
            if(manifest in bundledManifestsRP4) {
                playbackPrefs.edit().putBoolean(bundledRemovedKeyRP4(manifest,!activeProfile.primary),true).apply()
                if(activeProfile.primary) applyProfileAddons() else saveOwnProfileAddons(next)
            }
            return
        }
        if(!activeProfile.primary) {
            if(manifest in bundledManifestsRP4) playbackPrefs.edit().putBoolean(bundledRemovedKeyRP4(manifest,true),true).apply()
            if(manifest in retiredBundledManifestsRP12) playbackPrefs.edit().remove(retiredAddonKeyRP12(manifest,true)).apply()
            saveOwnProfileAddons(next);return
        }
        cloud.upsertPrivateSetup(owner,PrivateSetup(tmdb.token,next.firstOrNull().orEmpty(),next))
        if(session?.userId==owner.userId) {
            if(manifest in bundledManifestsRP4) playbackPrefs.edit().putBoolean(bundledRemovedKeyRP4(manifest,false),true).apply()
            if(manifest in retiredBundledManifestsRP12) playbackPrefs.edit().remove(retiredAddonKeyRP12(manifest,false)).apply()
            setAccountManifests(next)
        }
    }

    suspend fun loadHome() = supervisorScope {
        if(easyModeRP79) {loadEasyHomeRP79();return@supervisorScope}
        if(kidsMode) return@supervisorScope
        val owner=ratingsKey();val language=interfaceLanguage
        fun current()=owner==ratingsKey() && language==interfaceLanguage
        val a = async { runCatching { tmdb.trending() } }
        val b = async { runCatching { tmdb.popularMovies() } }
        val c = async { runCatching { tmdb.popularSeries() } }
        val d = async { runCatching { tmdb.topRatedMovies() } }

        a.await().onSuccess { if(current()) trending.replaceWith(it) }
        b.await().onSuccess { if(current()) movies.replaceWith(it) }
        c.await().onSuccess { if(current()) series.replaceWith(it) }
        d.await().onSuccess { if(current()) topRated.replaceWith(it) }
    }

    suspend fun loadHomeExtras() {
        if (kidsMode || extrasLoaded || extrasLoading || tmdb.token.isBlank()) return
        val owner=ratingsKey();val language=interfaceLanguage
        fun current()=owner==ratingsKey() && language==interfaceLanguage
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
                val continueJob = async {
                    try {
                        val rows=resolveContinueWatching()
                        if(current()) {continueWatching.replaceWith(rows);continueHomeReadyRP129=true}
                    } catch(e:CancellationException) {throw e}
                    catch(_:Exception) {if(current()) continueHomeReadyRP129=true}
                }
                val newJob=async { runCatching { tmdb.nowPlayingMovies() } }
                val bestTvJob=async { runCatching { tmdb.topRatedSeries() } }
                val comedyJob=async { runCatching { tmdb.discoverMovieGenre(35,pages=1,limit=20) } }
                val thrillerJob=async { runCatching { tmdb.discoverMovieGenre(53,pages=1,limit=20) } }

                actionJob.await().onSuccess { if(current()) action.replaceWith(it) }
                sciFiJob.await().onSuccess { if(current()) sciFi.replaceWith(it) }
                netflixJob.await().onSuccess { if(current()) netflix.replaceWith(it) }
                disneyJob.await().onSuccess { if(current()) disney.replaceWith(it) }
                primeJob.await().onSuccess { if(current()) prime.replaceWith(it) }
                appleJob.await().onSuccess { if(current()) apple.replaceWith(it) }
                hboJob.await().onSuccess { if(current()) hbo.replaceWith(it) }
                continueJob.await()
                newJob.await().onSuccess { if(current()) newMovies.replaceWith(it) }
                bestTvJob.await().onSuccess { if(current()) bestSeries.replaceWith(it) }
                comedyJob.await().onSuccess { if(current()) comedy.replaceWith(it) }
                thrillerJob.await().onSuccess { if(current()) thrillers.replaceWith(it) }
            }
            if(current()) extrasLoaded = true
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
    suspend fun openHomeShelf(key: String,title: String,rows: List<MediaSummary>,mood:String="") {
        if(kidsMode) return
        genreGeneration++;currentGenre=GenreDefinition(if(mood.isNotBlank()) "mood:$mood" else "home:$key",title,0,0);genreTitle=if(key=="ranked_movies") tr("Más películas populares","More popular movies") else title
        genreReturnScreen=Screen.HOME
        homeShelfSeed=discoveryHistoryKey.split("|").firstOrNull {it.isNotBlank()}.orEmpty()
        homeShelfTheme=seasonalTheme
        genreItems.clear();genreBacklog.clear();genrePageNumber=0
        genreBacklog.offer(rows) {it.cloudId}
        genreRemoteMore=mood.isNotBlank() || tmdb.pageableHomeShelf(key);genreHasMore=true;genreMessage=""
        screen=Screen.GENRE_DETAIL
        loadMoreGenre()
    }
    val genreUpcoming:Boolean get()=currentGenre?.id=="home:coming_soon_home"
    internal fun localCatalogKeyRC100(title:String)=if(screen==Screen.GENRE_DETAIL) "genre:${currentGenre?.id ?: title}" else "${screen}:$title"
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
                    g.id=="mood:christmas" || g.id=="christmas" -> tmdb.christmasPage(page)
                    g.id=="mood:halloween" -> tmdb.homeShelfPage("horror_movies",page,"","")
                    g.id.startsWith("mood:") -> tmdb.moodPage(g.id.substringAfter(":"),page)
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
        val ids = com.miflix.native2.data.ContinueWatchingPolicyRP10.ids(progress)

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
        if(selected?.cloudId!=item.cloudId) invalidatePlaybackWork()
        val ticket=++detailsEpoch
        val language=tmdb.language
        val cacheKey="$language:${item.cloudId}"
        if(screen!=Screen.DETAILS && screen!=Screen.PLAYER) detailsReturnScreen=screen
        assistantDescriptionRP157=null;assistantEpisodeRP157=null
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
            if(item.type=="series" && loaded.seasonCount>0) loadSeason(if(easyModeRP79) (progress[item.cloudId]?.season ?: 1).coerceIn(1,loaded.seasonCount) else 1)
        } catch(e: CancellationException) { throw e } catch(e: Exception) { if(ticket==detailsEpoch) error=e.message }
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
        if(easyModeRP79) saveEasySnapshotRP79()
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

    var mobileDownloadBusyRP134 by mutableStateOf(false)
        private set
    var mobileDownloadStatusRP134 by mutableStateOf("")
        private set
    internal fun openMobileDownloadRP134(entry:MobileDownloadEntryRP134) {
        require(com.miflix.native2.BuildConfig.APPLICATION_ID.startsWith("com.brunio.mobile"))
        require(MobileDownloadsRP134.entries(appContext,MobileDownloadsRP134.owner(appContext)).any {it.id==entry.id && it.uri==entry.uri})
        val uri=entry.uri ?: return
        invalidatePlaybackWork();selected=entry.item;details=null;episodes.clear()
        miniPlayer=false
        playerRequest=PlayerRequest(entry.item,StreamChoice("Offline",uri,"Offline"),entry.season,entry.episode,0,returnScreen=Screen.DOWNLOADS)
        screen=Screen.PLAYER
    }
    suspend fun downloadMobileItemRP134(item:MediaSummary,season:Int=0,episode:Int=0) {
        downloadMobileEpisodesRP134(item,season,if(episode>0) listOf(episode) else listOf(0),false)
    }
    suspend fun downloadMobileSeasonRP134() {
        val item=selected ?: return
        val season=currentSeason
        val today=java.time.LocalDate.now().toString()
        val rows=episodes.filter {it.season==season && it.airDate.isNotBlank() && it.airDate<=today}.map {it.episode}.distinct()
        require(rows.isNotEmpty()) {tr("No hay episodios estrenados disponibles.","No released episodes available.")}
        downloadMobileEpisodesRP134(item,season,rows,true)
    }
    private suspend fun downloadMobileEpisodesRP134(item:MediaSummary,season:Int,numbers:List<Int>,wholeSeason:Boolean) {
        if(mobileDownloadBusyRP134) return
        require(BuildConfig.APPLICATION_ID.startsWith("com.brunio.mobile") && !kidsMode)
        val account=session?.userId;val profile=activeProfile.id
        val scope=MobileDownloadsRP134.owner(appContext)
        fun valid()=session?.userId==account && activeProfile.id==profile
        mobileDownloadBusyRP134=true
        var queued=0;var failed=0
        try {
            val imdb=tmdb.details(item).imdbId ?: error("IMDb ID unavailable")
            if(wholeSeason) MobileDownloadsRP134.planned(appContext,scope,item,season,numbers.size)
            for(episode in numbers) {
                if(!valid()) break
                mobileDownloadStatusRP134=tr("Preparando descarga ","Preparing download ")+"${queued+failed+1}/${numbers.size}"
                try {
                    val rows=resolvePlaybackSources(item,imdb,season,episode)
                    var ready:StreamChoice?=null
                    for(candidate in rows.take(5)) {
                        try {
                            val source=if(candidate.nativeSource!=null) withTimeoutOrNull(15_000) {nativeSources.materialize(candidate,nativeStore.token(account),::valid)} else candidate
                            if(source!=null && MobileDownloadRP133.direct(source)) {ready=source;break}
                        } catch(e:CancellationException) {throw e} catch(_:Exception) { }
                    }
                    val source=ready ?: error("No downloadable file")
                    if(!valid()) break
                    kotlinx.coroutines.withContext(Dispatchers.IO) {MobileDownloadsRP134.enqueue(appContext,item,source,season,episode,scope)}
                    queued++
                } catch(e:CancellationException) {throw e} catch(_:Exception) {failed++}
            }
            mobileDownloadStatusRP134=tr("En cola: ","Queued: ")+queued+if(failed>0) tr(" · Sin enlace descargable: "," · No downloadable source: ")+failed else ""
        } finally {mobileDownloadBusyRP134=false}
    }

    suspend fun playMovie(fromStart: Boolean=false) {
        val item = selected ?: return
        if(!rapidPlaybackRP87.accept("${localExtrasScope()}:playMovie:${item.cloudId}:$fromStart",android.os.SystemClock.uptimeMillis())) return
        latestPlaybackRP87.cancel()
        beginPlaybackLoading(item)
        val entryTicket=playbackWork.begin()
        val d = details ?:  tmdb.details(item).also { details = it }
        if(!playbackWork.current(entryTicket) || selected?.cloudId!=item.cloudId) return
        val imdb = d.imdbId ?: throw IllegalStateException(tr("IMDb ID no disponible","IMDb ID unavailable"))
        playResolved(item, imdb, 0, 0,if(fromStart) 0L else null)
    }

    suspend fun playEpisode(ep: EpisodeSummary, fromStart: Boolean=false) {
        val item = selected ?: return
        if(!rapidPlaybackRP87.accept("${localExtrasScope()}:playEpisode:${item.cloudId}:${ep.season}:${ep.episode}:$fromStart",android.os.SystemClock.uptimeMillis())) return
        latestPlaybackRP87.cancel()
        beginPlaybackLoading(item,ep.season,ep.episode,ep.still)
        val entryTicket=playbackWork.begin()
        val d = details ?:  tmdb.details(item).also { details = it }
        if(!playbackWork.current(entryTicket) || selected?.cloudId!=item.cloudId) return
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

    private suspend fun chooseSources(item: MediaSummary, imdb: String, season: Int, episode: Int, resumeMs: Long? = null) = latestPlaybackRP87.run {
        val ticket=playbackWork.begin(); val owner=session?.userId; val profile=activeProfile.id
        fun valid()=playbackWork.current(ticket) && session?.userId==owner && activeProfile.id==profile
        sourceSelection=null; busyMessage=tr("Buscando todos los enlaces…","Finding all playback links…"); error=null
        try {
            val links=resolvePlaybackSources(item,imdb,season,episode)
            if(!valid()) return@run
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
        if(!rapidPlaybackRP87.accept("${localExtrasScope()}:source:${stream.url}",android.os.SystemClock.uptimeMillis())) return
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

    private var voiceRevisionRP81 by mutableIntStateOf(0)
    val voiceLocaleRP81:String get() {
        voiceRevisionRP81
        return SearchQueryRP81.voiceLocale(playbackPrefs.getString("voice_rp81:${localExtrasScope()}",null),interfaceLanguage)
    }
    fun setVoiceLocaleRP81(value:String) {
        require(value in setOf("es-MX","en-US"))
        playbackPrefs.edit().putString("voice_rp81:${localExtrasScope()}",value).apply();voiceRevisionRP81++
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
        if(normalized.isBlank()) return emptyList()
        val rows=if(kidsMode) kidsMovies+kidsSeries else trending+movies+series+topRated+continueWatching+forYou+collectionItems+directorItems+myListTitles+comingMovies+comingSeries
        return FlexibleSearch.rank(rows.distinctBy {it.cloudId}.filter {FlexibleSearch.score(it.title,normalized)>=650},normalized).take(30)
    }
    suspend fun searchFlexible(query: String): List<MediaSummary> = supervisorScope {
        val parsed=SearchQueryRP81.parse(query)
        if(!easyModeRP79 && !parsed.explicitPerson) TasteQuery.parse(query)?.let {request ->
            val seed=FlexibleSearch.rank(tmdb.search(request[0]),request[0]).firstOrNull() ?: return@supervisorScope emptyList()
            val matches=tmdb.tasteMatches(seed,setOf(request[1]))
            return@supervisorScope if(kidsMode) tmdb.kidsFilter(matches) else matches
        }
        val intent=DiscoveryMood.intent(query)
        if(!easyModeRP79 && !parsed.explicitPerson && intent.isNotBlank()) return@supervisorScope discoveryPicks(intent)
        val normalized=parsed.query
        if(normalized.length<2) return@supervisorScope emptyList()
        val locale=tmdb.language;val owner=localExtrasScope();val wasKids=kidsMode;val wasEasy=easyModeRP79
        val cacheKey="$owner:$locale:$wasKids:$wasEasy:${parsed.role}:${parsed.mediaType}:$normalized"
        searchResultCache[cacheKey]?.takeIf {System.currentTimeMillis()-it.first<5*60*1000L}?.let {return@supervisorScope it.second}
        val local=localSearch(normalized)
        val peopleJob=async {
            try {withTimeoutOrNull(6500L) {tmdb.searchPeopleRP81(parsed,locale)}} catch(e:CancellationException) {throw e} catch(_:Exception) {null}
        }
        val remote=mutableListOf<MediaSummary>()
        val aliases=mutableMapOf<String,Int>()
        var primaryFailed=false
        suspend fun fetchTitlesRP81(text:String,language:String) {
            val rows=withTimeoutOrNull(4500L) {tmdb.search(text,language)} ?: throw IllegalStateException("Search timed out")
            remote.addAll(rows)
            rows.forEach {item ->aliases[item.cloudId]=maxOf(aliases[item.cloudId] ?: 0,FlexibleSearch.score(item.title,normalized))}
        }
        fun titleScore(item:MediaSummary)=maxOf(FlexibleSearch.score(item.title,normalized),aliases[item.cloudId] ?: 0)
        try {fetchTitlesRP81(normalized,locale)} catch(e:CancellationException) {throw e} catch(_:Exception) {primaryFailed=true}
        val people=peopleJob.await()
        // A Spanish title can be entered even with an English interface, and vice versa.
        // Keep primary-locale metadata first, using translated titles only for ranking.
        if(!primaryFailed && !parsed.explicitPerson && (local+remote).none {titleScore(it)>=650}) {
            try {fetchTitlesRP81(normalized,if(locale.startsWith("es")) "en-US" else "es-MX")}
            catch(e:CancellationException) {throw e} catch(_:Exception) { }
        }
        if(!primaryFailed && people?.rows.isNullOrEmpty() && (local+remote).none {titleScore(it)>=650}) {
            for(variant in FlexibleSearch.variants(normalized)) {
                kotlinx.coroutines.currentCoroutineContext().ensureActive()
                try {fetchTitlesRP81(variant,locale)} catch(e:CancellationException) {throw e} catch(_:Exception) {break}
                if(remote.any {titleScore(it)>=650}) break
            }
        }
        val titles=(remote+local).distinctBy {it.cloudId}.filter {parsed.mediaType=="all" || it.type==parsed.mediaType}
            .sortedWith(compareByDescending<MediaSummary> {titleScore(it)}.thenByDescending {it.rating})
        val filmography=people?.rows.orEmpty()
        val peopleFirst=filmography.isNotEmpty() && (parsed.explicitPerson ||
            (people?.confidence ?: 0)>=900 && (normalized.contains(' ') || titles.none {titleScore(it)>=950}) ||
            (people?.confidence ?: 0)>=750 && titles.none {titleScore(it)>=850})
        if(parsed.explicitPerson && people==null) throw IllegalStateException(tr("No pudimos consultar la filmografía. Reintenta.","Could not load the filmography. Try again."))
        val all=(if(parsed.explicitPerson) filmography else if(peopleFirst) filmography+titles else titles+filmography).distinctBy {it.cloudId}.take(if(wasKids) 40 else if(filmography.isNotEmpty()) 120 else 60)
        val ranked=if(wasKids) tmdb.kidsFilter(all) else all
        if(ranked.isEmpty() && primaryFailed && people==null) throw IllegalStateException(tr("No pudimos completar la búsqueda.","Could not complete the search."))
        if(owner!=localExtrasScope() || locale!=tmdb.language || wasKids!=kidsMode || wasEasy!=easyModeRP79) return@supervisorScope emptyList()
        if(searchResultCache.size>=12) searchResultCache.remove(searchResultCache.keys.first())
        if(!primaryFailed && people!=null) searchResultCache[cacheKey]=System.currentTimeMillis() to ranked
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
                if(!autoplayNextLink) throw e
                val next=budget.nextIndex()
                if(next<0) throw e
                index=next
                playbackLoading=playbackLoading?.copy(message=sourceRetryMessage(index,rows.size))
            }
        }
    }
    private fun sourceRetryMessage(index: Int,count: Int): String =
        tr("El enlace falló. Probando el siguiente…","Source failed. Trying the next link…")+" (${index+1}/${count})"
    /** Reuses the already loaded list; no add-on search or Supabase read is made here. */
    suspend fun tryNextPlaybackSource(req: PlayerRequest,position: Long,playing: Boolean): Boolean {
        if(playerRequest?.playbackId!=req.playbackId || req.live) return false
        if(req.sourceMemoryKeyRP16.isNotBlank()) lastSourceRP16.remove(req.sourceMemoryKeyRP16)
        if(!autoplayNextLink || !appForeground || screen!=Screen.PLAYER || watchParty!=null) return false
        val remembered=rememberedStartRP16?.takeIf {it.playbackId==req.playbackId}
        if(remembered!=null && sourceFallback==null) {
            rememberedStartRP16=null
            fun stillValid()=playerRequest?.playbackId==req.playbackId && session?.userId==remembered.owner &&
                activeProfile.id==remembered.profile && playbackWork.generation==remembered.generation && autoplayNextLink && watchParty==null
            if(!stillValid()) return false
            val fresh=try {
                playbackLinkCache.clear()
                resolvePlaybackSources(req.item,remembered.imdb,req.season,req.episode)
            } catch(e:CancellationException) {throw e} catch(_:Exception) {return false}
            if(!stillValid()) return false
            val candidates=StreamOrdering.fallbackCandidates(fresh,audioLanguage,providerOrder=sourceProviderOrderRP2(),secondaryAudio=secondaryAudioLanguageRP62,nativeSizeAscending=nativeSizeAscendingRP77,nativeSpanishOnly=nativeSpanishOnlyRP77).filter {it.url!=req.stream.url}
            val rows=listOf(req.stream)+candidates
            if(rows.size<2) return false
            sourceFallback=SourceFallbackPlan(remembered.owner,remembered.profile,remembered.generation,rows,
                com.miflix.native2.player.SourceFallbackBudget(rows.size,com.miflix.native2.data.StreamLanguagePriority.prefersSpanish(audioLanguage)),req.playbackId)
        }
        val plan=sourceFallback ?: return false
        fun valid()=autoplayNextLink && sourceFallback===plan && plan.playbackId==req.playbackId && playerRequest?.playbackId==req.playbackId &&
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
    private fun subtitleKey(item: MediaSummary, season: Int, episode: Int) = "${localExtrasScope()}:$subtitleLanguage:${item.cloudId}:$season:$episode"
    suspend fun playbackSubtitles(req: PlayerRequest): List<SubtitleChoice> {
        val rows=playbackSubtitleJobs[subtitleKey(req.item,req.season,req.episode)]?.await().orEmpty()
        if(rows.isNotEmpty()) return rows
        val imdb=details?.takeIf { selected?.cloudId==req.item.cloudId }?.imdbId ?: tmdb.details(req.item).imdbId ?: return emptyList()
        val target=providerEpisodeRP107(req.item,req.season,req.episode)
        return subtitleRepo.resolve(imdb,req.item.type,target.first,target.second,req.stream,preferredLanguage=subtitleLanguage)
    }

    private suspend fun playResolved(item: MediaSummary, imdb: String, season: Int, episode: Int, resumeOverride: Long? = null, selectedStream: StreamChoice? = null, fallbackStreams: List<StreamChoice> = emptyList(), onStarted: ((PlayerRequest) -> Unit)? = null) = latestPlaybackRP87.run {
        if(kidsMode) require(tmdb.kidsAllowed(item)) { tr("Este título no está disponible en Kids.","This title is not available in Kids.") }
        if(randomEpisodeCloud!=item.cloudId) randomEpisodeCloud=null
        val ticket=playbackWork.begin()
        sourceFallback=null
        rememberedStartRP16=null
        val playbackOwner=session?.userId; val playbackProfile=activeProfile.id
        fun valid()=playbackWork.current(ticket) && session?.userId==playbackOwner && activeProfile.id==playbackProfile
        if(playbackLoading?.let {it.item.cloudId==item.cloudId && it.season==season && it.episode==episode}!=true) beginPlaybackLoading(item,season,episode)
        busyMessage = tr("Buscando el mejor enlace…","Finding the best source…")
        error = null
        val memoryKeyRP16=lastSourceKeyRP16(item,season,episode)
        val progressKeyRP16=if(item.type=="series") "${item.cloudId}:s${season}e${episode}" else item.cloudId
        val targetProgressRP16=progress[progressKeyRP16] ?: progress[item.cloudId]?.takeIf {item.type!="series" || (it.season==season && it.episode==episode)}
        val rememberedRP16=if(selectedStream==null && watchParty==null && (resumeOverride ?: targetProgressRP16?.position ?: 0L)>0L)
            lastSourceRP16.get(memoryKeyRP16)?.takeIf {StreamOrdering.nativeAllowedRP77(it,audioLanguage,nativeSpanishOnlyRP77) && StreamFilters.allowed(it,item.type,item.year,streamSizeBytes,item.title) &&
                runCatching {java.net.URI(it.url).scheme in setOf("http","https")}.getOrDefault(false)} else null
        if(selectedStream==null) {
            val warmKey=playbackCacheKey(item,imdb,season,episode)
            var joins=0
            while(sourceWarmupKeyRP90==warmKey && joins++<WarmRetryPolicyRP92.MAX_ATTEMPTS) {
                val pending=sourceWarmupJobRP90 ?: break
                if(pending===kotlinx.coroutines.currentCoroutineContext()[kotlinx.coroutines.Job]) break
                pending.join()
                if(sourceWarmupJobRP90===pending) break
            }
        }
        if(!valid()) return@run
        val rejectedWarmRP92=warmRetryPlanRP92?.takeIf {it.identity==warmIdentityRP91(item,imdb,season,episode)}?.rejected?.toSet().orEmpty()
        val expectedPositionRP91=resumeOverride ?: targetProgressRP16?.position ?: 0L
        val preparedRP91=if(selectedStream==null) preparedVideoRP91?.takeIf {entry ->
            entry.request.item.cloudId==item.cloudId && entry.request.season==season && entry.request.episode==episode &&
            entry.identity==warmIdentityRP91(item,imdb,season,episode) && entry.player.playerError==null &&
            WarmVideoPolicyRP91.fresh(android.os.SystemClock.elapsedRealtime()-entry.createdAt,entry.request.resumeMs,expectedPositionRP91)
        } else null
        if(preparedRP91==null) discardPreparedVideoRP91()
        val streamsJob = async {
            val rows=if(rememberedRP16!=null && rememberedRP16.url !in rejectedWarmRP92) listOf(rememberedRP16)
                else if(selectedStream!=null) (listOf(selectedStream)+fallbackStreams).distinctBy {it.url}.filter {StreamFilters.allowed(it,item.type,item.year,streamSizeBytes,item.title)}
                else resolvePlaybackSources(item,imdb,season,episode)
            if(selectedStream!=null) rows else rows.filter {it.url !in rejectedWarmRP92}
        }
        val preferredSubtitleRP78=subtitleLanguage
        val subtitleKey=subtitleKey(item,season,episode)
        if(playbackSubtitleJobs.size>=16 && subtitleKey !in playbackSubtitleJobs) {
            val oldest=playbackSubtitleJobs.keys.first(); playbackSubtitleJobs.remove(oldest)?.cancel()
        }
        // Subtitle services must not gate opening the video. Keep late results for the CC menu.
        val subsJob = playbackSubtitleJobs.getOrPut(subtitleKey) { appScope.async {
            if(easyModeRP79) emptyList() else runCatching {
                val target=providerEpisodeRP107(item,season,episode)
                subtitleRepo.resolve(imdb,item.type,target.first,target.second,null,preferredLanguage=preferredSubtitleRP78)
            }.getOrDefault(emptyList())
        } }
        runCatching {
            val streams = streamsJob.await()
            if(!valid()) return@run
            val bandwidth=if(adaptiveSource) androidx.media3.exoplayer.upstream.DefaultBandwidthMeter.getSingletonInstance(appContext).bitrateEstimate else 0L
            val seconds=(details?.takeIf {it.summary.cloudId==item.cloudId}?.runtimeMinutes ?: 0)*60
            val chosen = preparedRP91?.request?.stream ?: rememberedRP16?.takeIf {it.url !in rejectedWarmRP92} ?: selectedStream?.takeIf {it in streams} ?: StreamOrdering.best(streams,audioLanguage,bandwidth,seconds,sourceProviderOrderRP2(),secondaryAudioLanguageRP62,nativeSizeAscendingRP77,nativeSpanishOnlyRP77)
                ?: throw IllegalStateException(if(streamRepo.hasTorrentOnlyResults())
                    tr("Estos complementos solo devolvieron torrents. Se necesitan enlaces directos HTTP/HTTPS.","These add-ons only returned torrents. Direct HTTP/HTTPS links are required.")
                    else tr("No se encontraron enlaces reproducibles","No playable sources found"))
            val automaticFallback=autoplayNextLink && watchParty==null
            val retryRows=if(automaticFallback) {
                val candidates=StreamOrdering.fallbackCandidates(streams,audioLanguage,bandwidth,seconds,sourceProviderOrderRP2(),secondaryAudioLanguageRP62,nativeSizeAscendingRP77,nativeSpanishOnlyRP77).filter {it.url!=chosen.url && it.url!=preparedRP91?.originalUrl}
                val ordered=(listOf(chosen)+candidates).distinctBy {it.url}
                if(com.miflix.native2.data.StreamLanguagePriority.prefersSpanish(audioLanguage)) ordered else ordered.take(com.miflix.native2.player.SourceFallbackBudget.MAX_ATTEMPTS)
            } else listOf(chosen)
            val budget=com.miflix.native2.player.SourceFallbackBudget(retryRows.size,com.miflix.native2.data.StreamLanguagePriority.prefersSpanish(audioLanguage))
            val best=preparedRP91?.request?.stream ?: materializeAutomaticSource(retryRows,budget,playbackOwner,::valid)
            val externalSubs = if(subsJob.isCompleted) subsJob.await() else emptyList()
            val mergedSubs = (best.subtitles + externalSubs)
                .distinctBy { it.url }
                .sortedWith(compareBy<SubtitleChoice> {
                    when {com.miflix.native2.data.StreamLanguagePriority.languageMatches(it.lang,subtitleLanguage) ->0;it.lang.lowercase(Locale.US) in setOf("es","spa") ->1;it.lang.lowercase(Locale.US) in setOf("en","eng") ->2;else ->3}
                }.thenBy { it.label })
            if(!valid()) return@run
            val enriched = best.copy(subtitles = mergedSubs)
            val key = if (item.type == "series") "${item.cloudId}:s${season}e${episode}" else item.cloudId
            val saved = progress[key] ?: progress[item.cloudId]
            playerRequest = PlayerRequest(item, enriched, season, episode, resumeOverride ?: saved?.position ?: 0L,sourceMemoryKeyRP16=memoryKeyRP16)
            if(rememberedRP16!=null && rememberedRP16.url !in rejectedWarmRP92) playerRequest?.let {rememberedStartRP16=RememberedStartRP16(it.playbackId,imdb,playbackOwner,playbackProfile,ticket.generation)}
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
        val rows=tmdb.season(item.id,season).filter {(com.miflix.native2.data.AbsoluteEpisodeRP102.enabled(item.id) || it.season==season) && it.episode>0 && it.airDate.isNotBlank() && it.airDate<=today}
        if(!valid()) return
        require(rows.isNotEmpty()) {tr("Esta temporada no tiene episodios estrenados.","This season has no released episodes.")}
        profileSyncMutex.lock()
        try {
            if(!valid()) return
            val now=System.currentTimeMillis()
            val watched=playbackPrefs.getStringSet(watchedEpisodesKey(),emptySet()).orEmpty().toMutableSet()
            for(ep in rows) {
                val key="${item.cloudId}:s${ep.season}e${ep.episode}"
                val duration=progress[key]?.duration?.takeIf {it>0} ?: 1000L
                progress[key]=PlaybackProgress(100.0,duration,duration,now,ep.season,ep.episode)
                watched.add(key)
            }
            playbackPrefs.edit().putStringSet(watchedEpisodesKey(),watched).apply()
            val current=progress[item.cloudId]
            if(current==null || current.season==season) {
                val last=rows.last()
                progress[item.cloudId]=progress["${item.cloudId}:s${last.season}e${last.episode}"]!!
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
    private var unwatchedVisibilityRevisionRP20 by mutableIntStateOf(0)
    internal val showUnwatchedDetailsRP20:Boolean
        get() {unwatchedVisibilityRevisionRP20;return playbackPrefs.getBoolean("unwatched_visible:${ratingsKey()}",false)}
    internal fun saveShowUnwatchedDetailsRP20(value:Boolean) {
        playbackPrefs.edit().putBoolean("unwatched_visible:${ratingsKey()}",value).apply()
        unwatchedVisibilityRevisionRP20++
    }
    private var contentWarningsRevisionRP31 by mutableIntStateOf(0)
    val contentWarningsRP31:Boolean get() {contentWarningsRevisionRP31;return playbackPrefs.getBoolean("content_warnings_rp31:${ratingsKey()}",true)}
    fun saveContentWarningsRP31(value:Boolean) {playbackPrefs.edit().putBoolean("content_warnings_rp31:${ratingsKey()}",value).apply();contentWarningsRevisionRP31++}
    internal fun hideEpisodeSpoilersRP20(item:MediaSummary,ep:EpisodeSummary)=!showUnwatchedDetailsRP20 && !episodeWatched(item,ep)
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
        if(!kidsMode && playerRequest?.playbackId==req.playbackId) tasteProfile.observe(ratingsKey(),req.item,position,duration,ended)
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
        if(easyModeRP79) saveEasySnapshotRP79()
        val shelf=if(kidsMode) kidsContinue else continueWatching
        val available=(shelf.toList()+req.item).associateBy {it.cloudId}
        val ids=com.miflix.native2.data.ContinueWatchingPolicyRP10.ids(progress)
        val rows=ids.mapNotNull {available[it]}
        if(shelf.toList()!=rows) {shelf.clear();shelf.addAll(rows)}
    }

    suspend fun createWatchParty(withCurrentTitle: Boolean = false): WatchParty? {
        val s = profileSocialSession() ?: run { error = tr("Inicia sesión antes de crear una sala","Sign in before creating a Watch Party"); return null }
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
            if(partyEpoch!=epoch || !isCurrentSocialSession(s)) return@runCatching null
            partyEpoch++; invalidatePlaybackWork(); knownPartyMembers=setOf(s.userId)
            watchParty=party; partyRole=PartyRole.HOST; partyStatus=tr("Sala lista · elige un contenido","Room ready · choose any title")
            rememberRoom(room); lastEventId=-1; party
        }
            .onFailure { error = it.message }.getOrNull()
    }

    suspend fun joinWatchParty(code: String) {
        require(!kidsMode) {tr("Watch Party no está disponible en Kids.","Watch Party is not available in Kids.") }
        val s = profileSocialSession() ?: run { error = tr("Inicia sesión antes de unirte a una sala","Sign in before joining a Watch Party"); return }
        require(code.trim().matches(Regex("[0-9]{6}"))) { tr("Usa el código de 6 dígitos","Use the 6-digit code") }
        if(watchParty!=null && watchParty?.roomCode!=code.trim()) { error=tr("Sal del party actual antes de solicitar otra sesión","Leave your current party before requesting another session"); return }
        val epoch=partyEpoch
        val decision=social.requestAccess(s,code.trim())
        if(partyEpoch!=epoch || !isCurrentSocialSession(s)) return
        if(decision!="approved") {
            pendingPartyCode=if(decision=="pending") code.trim() else null
            partyStatus=if(decision=="pending") tr("Solicitud enviada · esperando aprobación del host","Request sent · Waiting for host approval") else tr("El host rechazó la solicitud","The host rejected your request")
            return
        }
        pendingPartyCode=null
        social.join(s, code.trim())
        if(partyEpoch!=epoch || !isCurrentSocialSession(s)) return
        val party = cloud.readWatchParty(s, code.trim()) ?: run { error = tr("No se encontró la sala","Watch Party room not found"); return }
        if(partyEpoch!=epoch || !isCurrentSocialSession(s)) return
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
        val s = profileSocialSession() ?: return@withLock
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
        val account=profileSocialSession() ?: return@withLock
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
        val s=profileSocialSession() ?: return null; val current=watchParty ?: return null; val epoch=partyEpoch
        val fresh=try { cloud.readWatchParty(s,current.roomCode) } catch(e: CancellationException) { throw e } catch(_: Exception) { null }
        if(!sameParty(epoch,s.userId,current.roomCode)) return null
        return fresh?.also { watchParty=it }
    }

    suspend fun switchToPartyState(remote: WatchParty, openPlayer: Boolean = false) {
        if(kidsMode) return
        val owner=profileSocialSession()?.userId ?: return; val epoch=partyEpoch
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
                val shared=try {cloud.readPartySource(profileSocialSession() ?: return,remote)} catch(e: CancellationException) {throw e} catch(_: Exception) {null}
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
            if(item.type=="series") { currentSeason=if(com.miflix.native2.data.AbsoluteEpisodeRP102.enabled(item.id)) 1 else remote.season.coerceAtLeast(1); episodes.clear(); episodes.addAll(eps) }
            val imdb=detail.imdbId ?: return
            playResolved(item,imdb,remote.season,remote.episode,remote.positionMs)
        } finally { if(switchingPartyEpoch==epoch) switchingPartyEpoch=null }
    }

    private fun detachParty() {
        miniPlayer=false
        partyEpoch++; switchingPartyEpoch=null; invalidatePlaybackWork()
        session?.let { socialPrefs.edit().remove(socialRoomKey()).apply() }
        watchParty=null; partyRole=null;partyFollowHost=true; pendingPartyCode=null; accessRequests.clear()
        partyMessages.clear(); floatingEvents.clear(); partyMembers.clear(); lastEventId=-1;partyBallotRP151=null;ballotRoomRP151=null
        recentPartyMembersRP88.clear();recentPartyRoomRP88=null;partyReturnPulseRP88=0
        knownPartyMembers=null; partyJoinedNotice=null; joinedNoticeSequence++;sharedSourcePlaybackId=null
        partyPlaybackNotice=null;playbackNoticeSequence++
        partyStatus=""; error=null; partyClosedNotice=false
        if(screen==Screen.PLAYER) screen=if(playerRequest?.live==true) Screen.LIVE_TV else if(selected!=null) Screen.DETAILS else Screen.HOME
        playerRequest=null
    }
    suspend fun leaveWatchParty() {
        val s=profileSocialSession(); val p=watchParty; val role=partyRole
        // Release player and pending work immediately, before waiting for the network.
        detachParty()
        if(s!=null && p!=null) {
            if(role==PartyRole.HOST) runCatching { cloud.deleteWatchParty(s,p.roomCode) }.onFailure { if(it is CancellationException) throw it }
            runCatching { social.leave(s,p.roomCode) }.onFailure { if(it is CancellationException) throw it }
        }
    }

    private fun rememberRoom(code: String) { session?.let { socialPrefs.edit().putString(socialRoomKey(), code).apply() } }

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
        val s = profileSocialSession() ?: error(tr("Inicia sesión primero","Sign in first"))
        val nick = value.trim().lowercase(Locale.US)
        social.saveNickname(s, nick); if(isCurrentSocialSession(s)) nickname = nick
    }
    fun savePhrases(value: String) {
        val values = value.split("|").map { it.trim().take(120) }.filter { it.isNotEmpty() }.distinct().take(8)
        partyPhrases.clear(); partyPhrases.addAll(values)
        socialPrefs.edit().putString("phrases_rp1_${session?.userId ?: "local"}:${activeProfile.id}", values.joinToString("|")).apply()
    }
    suspend fun sendPartyEvent(kind: String, text: String) = socialAction {
        val s = profileSocialSession() ?: error(tr("Inicia sesión primero","Sign in first"))
        val room = watchParty?.roomCode ?: error(tr("Únete primero a una sala","Join a room first"))
        social.send(s, room, kind, text)
    }
    suspend fun refreshFriends() = socialAction {
        val s = profileSocialSession() ?: return@socialAction
        val name=social.nickname(s)
        val rows=social.friends(s)
        val ids=rows.flatMap { listOf(it.sender,it.receiver) }.distinct()
        val people=social.people(s,ids)
        if(!isCurrentSocialSession(s)) return@socialAction
        nickname=name;friendRows.clear();friendRows.addAll(rows)
        friendPeople.clear();people.forEach { friendPeople[it.id]=it.nickname }
    }
    suspend fun addFriend(id: String) = socialAction {
        val s = profileSocialSession() ?: error(tr("Inicia sesión primero","Sign in first"))
        require(id != s.userId) { tr("Esta es tu cuenta","This is your account") }
        val existing = social.friends(s).firstOrNull { it.sender == id || it.receiver == id }
        if(!isCurrentSocialSession(s)) return@socialAction
        if (existing?.status == "accepted") return@socialAction
        if (existing?.receiver == s.userId) social.accept(s, id)
        else if (existing == null) social.requestFriend(s, id)
        refreshFriends()
    }
    suspend fun findAndAddFriend(nick: String) = socialAction {
        val s = profileSocialSession() ?: error(tr("Inicia sesión primero","Sign in first"))
        val person = social.find(s, nick.trim()) ?: error(tr("No se encontró el apodo","Nickname not found"))
        if(isCurrentSocialSession(s)) addFriend(person.id)
    }
    suspend fun pollSocialRoom(refreshMembers: Boolean = true) {
        val s=profileSocialSession() ?: return; val p=watchParty ?: return; val epoch=partyEpoch
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
        if(recentPartyRoomRP88!=p.roomCode) {recentPartyMembersRP88.clear();recentPartyRoomRP88=p.roomCode}
        val recent=(members+recentPartyMembersRP88).distinctBy {it.id}.take(32)
        recentPartyMembersRP88.clear();recentPartyMembersRP88.addAll(recent)
        partyMembers.clear(); partyMembers.addAll(members)
        val events=social.events(s,p.roomCode,lastEventId)
        if(!valid()) return
        if(ballotRoomRP151!=p.roomCode) {ballotRoomRP151=p.roomCode;partyBallotRP151=null}
        for(event in events) if(PartyVotingRP151.accepts(event)) {
            partyBallotRP151=PartyVotingRP151.reduce(partyBallotRP151,event,fresh.hostUserId,members.map {it.id}.toSet()+fresh.hostUserId,System.currentTimeMillis())
        }
        val firstPollRP88=lastEventId<0
        if(firstPollRP88) {partyMessages.clear();partyMessages.addAll(events.filter {it.kind!="playback" && !PartyVotingRP151.accepts(it)});lastEventId=events.lastOrNull()?.id ?: 0}
        run {
            for(event in events) {
                lastEventId=maxOf(lastEventId,event.id)
                if(PartyVotingRP151.accepts(event)) continue
                if(firstPollRP88 && event.kind!="playback") continue
                if(event.kind=="playback") {
                    if(partyRole==PartyRole.GUEST && com.miflix.native2.data.PartyReturnPolicyRP88.accepts(
                            event.userId,fresh.hostUserId,event.body,s.userId,event.createdAt,System.currentTimeMillis())) {
                        val latest=cloud.readWatchParty(s,p.roomCode)
                        if(!valid()) return
                        if(latest!=null && latest.hostUserId==event.userId) {
                            watchParty=latest;partyFollowHost=true;parkedPlaybackId=null
                            switchToPartyState(latest,openPlayer=true)
                            if(valid()) partyReturnPulseRP88++
                        }
                        continue
                    }
                    if(!firstPollRP88 && event.userId!=s.userId && PartyPlaybackNoticePolicy.accepts(event.kind,event.body,event.createdAt,System.currentTimeMillis())) {
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
        val s=profileSocialSession() ?: return@withLock
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
        if(easyModeRP79) return
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
            clearAccountManifests(); sourceSelection=null; playerRequest=null
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
        previousLiveChannels.clear();previousLiveRevision++
        sharedMoments.clear();sharedMomentsOwner=""
        profileSocialSession()?.let { presenceEvents.queue(it,"","",0,0,null,System.currentTimeMillis()) }
        playbackLinkCache.clear()
        closeAvatarUpload()
        detachParty()
        clearAccountManifests(); sourceSelection=null; playerRequest=null
        watchParty=null; partyRole=null; nickname=""
        partyMessages.clear(); floatingEvents.clear(); partyMembers.clear()
        partyInvitations.clear();calendarEvents.clear();friendRows.clear(); friendPeople.clear(); friendActivity.clear(); accessRequests.clear(); pendingPartyCode=null; lastEventId=-1
        traktConnected=false; traktDevice=null; traktSent.clear()
        session = null
        local.saveSession(null)
        favorites.clear(); progress.clear(); profiles.clear(); continueWatching.clear(); ratings.clear(); forYou.clear()
        loadOfflineProfiles()
        extrasLoaded=false;continueHomeReadyRP129=false
        screen = Screen.SETTINGS
    }

    private fun <T> MutableList<T>.replaceWith(rows: Collection<T>) {
        clear(); addAll(rows)
    }
    private var cloudIdentityReadyRP83=false
    var settingsBackupStatusRP83 by mutableStateOf("")
        private set
    private var restoringSettingsRP83=false
    private val settingsMutexRP83=Mutex()
    private val settingsReadyRP83=mutableSetOf<String>()
    private val settingsDirtyRP83=mutableSetOf<String>()
    private val settingsRevisionRP83=mutableMapOf<String,Long>()
    private val settingsSignatureRP83=mutableMapOf<String,String>()
    private var settingsJobRP83:kotlinx.coroutines.Job?=null
    private fun settingsOwnerRP83()="${session?.userId.orEmpty()}:${activeProfile.id}"
    private fun settingsStoresRP83():List<Pair<String,android.content.SharedPreferences>> = listOf(
        "playback" to playbackPrefs,
        "extras" to appContext.getSharedPreferences("brunio_local_extras",Context.MODE_PRIVATE),
        "taste" to appContext.getSharedPreferences("brunio_taste",Context.MODE_PRIVATE),
        "live" to livePrefs,
        "favorites" to LiveLibrary.prefs(appContext),
        "channels" to appContext.getSharedPreferences("brunio_custom_channels_rp97",Context.MODE_PRIVATE),
        "lists" to appContext.getSharedPreferences("brunio_m3u_lists_rp61",Context.MODE_PRIVATE),
        "lastLive" to appContext.getSharedPreferences("brunio_live_last_rp41",Context.MODE_PRIVATE)
    )
    private fun settingsStoreKeyRP83(store:String,key:String)=SettingsKeysRP83.allows(store,key,session?.userId,activeProfile.id)
    private fun settingsSnapshotRP83():org.json.JSONObject {
        val stores=org.json.JSONObject()
        for((name,prefs) in settingsStoresRP83()) {
            val values=org.json.JSONObject()
            for((key,value) in prefs.all.toSortedMap()) if(settingsStoreKeyRP83(name,key)) {
                val type=when(value) {is Boolean->"b";is Int->"i";is Long->"l";is Float->"f";is String->"s";is Set<*>->"set";else->continue}
                values.put(key,org.json.JSONObject().put("t",type).put("v",if(value is Set<*>) org.json.JSONArray(value.filterIsInstance<String>().sorted()) else value))
            }
            stores.put(name,values)
        }
        return org.json.JSONObject().put("schema",83).put("backup",exportLocalBackupRC100()).put("stores",stores)
            .also {require(it.toString().toByteArray(Charsets.UTF_8).size<=2_000_000) {"Settings backup exceeds 2 MB"}}
    }
    private suspend fun applySettingsRP83(body:org.json.JSONObject) {
        require(body.getInt("schema")==83)
        val backup=body.getJSONObject("backup")
        validateLocalBackupRC100(backup)
        val stores=body.getJSONObject("stores")
        // Validate all remote values before changing preferences; unrecognised keys never reach an editor.
        for((name,_) in settingsStoresRP83()) {
            val values=stores.optJSONObject(name) ?: continue
            values.keys().forEach {key ->if(settingsStoreKeyRP83(name,key)) {
                val row=values.getJSONObject(key)
                when(row.getString("t")) {
                    "b" -> row.getBoolean("v")
                    "i" -> row.getInt("v")
                    "l" -> row.getLong("v")
                    "f" -> require(row.getDouble("v").isFinite())
                    "s" -> require(row.getString("v").length<=1_800_000)
                    "set" -> {val a=row.getJSONArray("v");require(a.length()<=10000);for(i in 0 until a.length()) a.getString(i)}
                    else -> error("Invalid saved setting")
                }
            }}
        }
        val oldEasyRP83=easyModeRP79
        val oldActorRP83=profileSocialSession()
        restoringSettingsRP83=true
        try {
            for((name,prefs) in settingsStoresRP83()) {
                val values=stores.optJSONObject(name) ?: continue
                val edit=prefs.edit()
                // Snapshot semantics also preserve intentionally removed/reset settings.
                prefs.all.keys.filter {settingsStoreKeyRP83(name,it) && !values.has(it)}.forEach {edit.remove(it)}
                values.keys().forEach {key ->if(settingsStoreKeyRP83(name,key)) {
                    val row=values.getJSONObject(key)
                    when(row.getString("t")) {
                        "b" -> edit.putBoolean(key,row.getBoolean("v"))
                        "i" -> edit.putInt(key,row.getInt("v"))
                        "l" -> edit.putLong(key,row.getLong("v"))
                        "f" -> edit.putFloat(key,row.getDouble("v").toFloat())
                        "s" -> edit.putString(key,row.getString("v"))
                        "set" -> {val a=row.getJSONArray("v");edit.putStringSet(key,(0 until a.length()).map {a.getString(it)}.toSet())}
                    }
                }}
                edit.apply()
            }
            easyRevisionRP79++;easySpanishRevisionRP83++;secondaryAudioRevisionRP62++;voiceRevisionRP81++
            simpleHomeRevision++;unwatchedVisibilityRevisionRP20++;contentWarningsRevisionRP31++;addonOrderRevision++;profileAddonRevision++
            streamSizeLimitGb=(playbackPrefs.getString(streamSizePreferenceKeyRP134,"0")?.toDoubleOrNull() ?: 0.0).coerceIn(0.0,500.0)
            rememberLastProfile=rememberedProfileEnabledRP142()
            hiddenAvatars=playbackPrefs.getStringSet("hidden_avatars",emptySet()).orEmpty().toSet()
            LiveLibrary.revision++;CustomChannelsRP45.changesRP48.value++
            importLocalBackupRC100(backup,reloadHome=false)
            applyProfileAddons()
            if(easyModeRP79 && !oldEasyRP83) {
                invalidatePlaybackWork();detachParty();miniPlayer=false;playerRequest=null;sourceSelection=null
                contextTitle=null;showLiveScoresRP11=false;pendingPartyCode=null
                if(screen !in listOf(Screen.SPLASH,Screen.PROFILES)) screen=Screen.HOME
                presenceEvents.stopForEasyMode(oldActorRP83)
            }
        } finally {restoringSettingsRP83=false}
    }
    internal suspend fun restoreSettingsRP83(discardLocal:Boolean=false) = settingsMutexRP83.withLock {
        val account=session ?: return@withLock
        val profile=activeProfile.id;val owner=settingsOwnerRP83()
        settingsReadyRP83.remove(owner)
        val pendingKey="settings_pending_rp83:$owner"
        val revisionKey="settings_revision_rp83:$owner"
        val pending=!discardLocal && (owner in settingsDirtyRP83 || playbackPrefs.getBoolean(pendingKey,false))
        val knownRevision=settingsRevisionRP83[owner] ?: playbackPrefs.getLong(revisionKey,-1L)
        try {
            val (body,revision)=cloud.settingsRP83(account,profile)
            if(settingsOwnerRP83()!=owner) return@withLock
            if(pending && body!=null && knownRevision!=revision) {
                settingsBackupStatusRP83=tr("Hay ajustes locales pendientes y una copia diferente en la cuenta. Puedes conservar los locales o usar Restaurar ajustes de la cuenta.","Local settings are pending and the account copy differs. Keep using local settings or choose Restore account settings.")
                return@withLock
            }
            if(body!=null && !pending) {
                applySettingsRP83(body)
                if(settingsOwnerRP83()!=owner) return@withLock
                settingsDirtyRP83.remove(owner)
                playbackPrefs.edit().remove(pendingKey).apply()
            }
            settingsRevisionRP83[owner]=revision
            playbackPrefs.edit().putLong(revisionKey,revision).apply()
            settingsReadyRP83.add(owner)
            if(body==null || pending) {settingsDirtyRP83.add(owner);playbackPrefs.edit().putBoolean(pendingKey,true).apply()}
            else if(owner !in settingsDirtyRP83) settingsSignatureRP83[owner]=settingsSnapshotRP83().toString()
            settingsBackupStatusRP83=tr("Ajustes recuperados de tu cuenta","Settings restored from your account")
        } catch(e:CancellationException) {throw e} catch(_:Exception) {
            if(settingsOwnerRP83()==owner) {
                settingsReadyRP83.remove(owner)
                settingsBackupStatusRP83=tr("Respaldo pendiente. Revisa conexión y ejecuta supabase_rp83_settings.sql.","Backup pending. Check your connection and run supabase_rp83_settings.sql.")
            }
        }
    }
    internal suspend fun flushSettingsRP83(force:Boolean=false) = settingsMutexRP83.withLock {
        val account=session ?: return@withLock
        val owner=settingsOwnerRP83();val profile=activeProfile.id
        if(restoringSettingsRP83 || owner !in settingsReadyRP83) return@withLock
        try {
            val snapshot=settingsSnapshotRP83();val signature=snapshot.toString()
            if(!force && signature==settingsSignatureRP83[owner]) {settingsDirtyRP83.remove(owner);playbackPrefs.edit().remove("settings_pending_rp83:$owner").apply();return@withLock}
            val revision=cloud.saveSettingsRP83(account,profile,snapshot,settingsRevisionRP83[owner] ?: 0L)
            if(owner!=settingsOwnerRP83()) return@withLock
            settingsRevisionRP83[owner]=revision;settingsSignatureRP83[owner]=signature
            val stillCurrent=settingsSnapshotRP83().toString()==signature
            playbackPrefs.edit().putLong("settings_revision_rp83:$owner",revision).putBoolean("settings_pending_rp83:$owner",!stillCurrent).apply()
            if(stillCurrent) settingsDirtyRP83.remove(owner)
            settingsBackupStatusRP83=tr("Ajustes guardados en tu cuenta ✓","Settings saved to your account ✓")
        } catch(e:CancellationException) {throw e} catch(_:Exception) {
            if(owner==settingsOwnerRP83()) settingsBackupStatusRP83=tr("No se guardaron los ajustes. Reintenta; si cambiaron en otro dispositivo, restaura la copia de la cuenta.","Settings were not saved. Retry; if changed on another device, restore the account copy.")
        }
    }
    suspend fun saveSettingsNowRP83() {
        if(settingsOwnerRP83() !in settingsReadyRP83) restoreSettingsRP83()
        flushSettingsRP83(force=true)
    }
    private fun scheduleSettingsRP83() {
        if(restoringSettingsRP83 || session==null) return
        val owner=settingsOwnerRP83()
        settingsDirtyRP83.add(owner)
        playbackPrefs.edit().putBoolean("settings_pending_rp83:$owner",true).apply()
        settingsJobRP83?.cancel()
        settingsJobRP83=appScope.launch {
            delay(1500)
            if(owner==settingsOwnerRP83()) flushSettingsRP83()
        }
    }
    private val settingsListenersRP83=mutableListOf<Pair<android.content.SharedPreferences,android.content.SharedPreferences.OnSharedPreferenceChangeListener>>()
    init {
        for((name,prefs) in settingsStoresRP83()) {
            if(name=="lastLive") continue // Save with the next settings/background flush, not on every channel zap.
            val listener=android.content.SharedPreferences.OnSharedPreferenceChangeListener {_,key ->
                if(key!=null && settingsStoreKeyRP83(name,key)) scheduleSettingsRP83()
            }
            settingsListenersRP83.add(prefs to listener)
            prefs.registerOnSharedPreferenceChangeListener(listener)
        }
    }

}

enum class Screen {
    SPLASH, HOME, SEARCH, MOVIES, SERIES, COLLECTIONS, OSCARS, COLLECTION_DETAIL, PLATFORM_DETAIL,
    GENRES, GENRE_DETAIL, YEAR_DETAIL, MY_LIST, DOWNLOADS, SETTINGS, PROFILES, WATCH_PARTY, PAIR_DEVICE,
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
    val sourceFallbackPlaying: Boolean? = null,
    val sourceMemoryKeyRP16: String = "",
    val sportsLinksRP71: List<StreamChoice> = emptyList(),
    val sportsAttemptRP71: Int = 0,
    val sportsOwnerRP71: String = "",
    val sportsRecordingRP72: Boolean = false,
    val sportsFailedRP137: Set<String> = emptySet(),
    val sportsRefreshedRP137: Boolean = false
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
