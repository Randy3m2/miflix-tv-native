package com.brunio.desktop

import androidx.compose.runtime.*
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.time.YearMonth

data class QueueEntry(val item:JSONObject,val season:Int=0,val episode:Int=0)

class AppController(private val scope: CoroutineScope, val bridge: BridgeClient) {
    var account by mutableStateOf<JSONObject?>(null); private set
    var profilePicker by mutableStateOf(false); private set
    var tab by mutableStateOf(DesktopTab.Home); private set
    var screen by mutableStateOf(DesktopScreenState()); private set
    var message by mutableStateOf<String?>(null)
    val queue=mutableStateListOf<QueueEntry>()
    var queueDialog by mutableStateOf(false)
    val multiview=mutableStateListOf<PlaybackSession>()
    var multiPicker by mutableStateOf(false)
    var multiChoices by mutableStateOf<List<MediaTile>>(emptyList())
    var playback by mutableStateOf<PlaybackSession?>(null)
    var episodes by mutableStateOf<List<JSONObject>?>(null)
    var detail by mutableStateOf<JSONObject?>(null)
    var settings by mutableStateOf<JSONObject?>(null)
    var addFriend by mutableStateOf(false)
    var month by mutableStateOf(YearMonth.now()); private set
    private var revision = 0L
    private var searchJob: Job? = null
    private var episodeRevision = 0L
    private var remoteLoading: String? = null
    private var rawTiles = emptyList<MediaTile>()
    private val cache = mutableMapOf<DesktopTab, DesktopScreenState>()

    fun restore() = scope.launch {
        try { account = bridge.call("session.restore") as JSONObject; profilePicker = account!!.optBoolean("signedIn") }
        catch (e: Exception) { message = e.message }
    }
    fun login(email: String, password: String) = scope.launch {
        screen = screen.copy(busy = true)
        try { account = bridge.call("session.login", JSONObject().put("email", email).put("password", password)) as JSONObject; settings = null; profilePicker = true }
        catch (e: Exception) { message = e.message }
        finally { screen = screen.copy(busy = false) }
    }
    fun selectProfile(id: String) = scope.launch {
        ++revision; cache.clear();queue.clear();queueDialog=false;detail = null
        try { account = bridge.call("profiles.select", JSONObject().put("profile_id", id)) as JSONObject; profilePicker = false; select(DesktopTab.Home, true) }
        catch (e: Exception) { message = e.message }
    }
    fun accountChanged(value: JSONObject) { revision++; cache.clear();queue.clear();queueDialog=false; playback=null; account=value; profilePicker=true }
    fun profiles(): List<MediaTile> = (account?.optJSONArray("profiles") ?: JSONArray()).objects().map {
        MediaTile(it.getString("id"), it.optString("name"), it.optString("avatar"))
    }
    fun showProfiles() { ++revision; searchJob?.cancel(); detail = null; settings = null; profilePicker = true }
    fun select(next: DesktopTab, refresh: Boolean = false) {
        searchJob?.cancel(); tab = next; val ticket = ++revision; detail = null
        cache[next]?.takeUnless { refresh }?.let { screen = it; rawTiles = it.tiles; return }
        screen = DesktopScreenState(busy = true)
        scope.launch {
            try {
                val loaded = when (next) {
                    DesktopTab.Home -> {
                        val trending = array("catalog.home").map { tile(it).copy(image = it.optString("backdrop").ifBlank { it.optString("poster") }) }
                        if (ticket == revision) screen = DesktopScreenState(busy = true, hero = trending.firstOrNull())
                        val rows = mutableListOf<ContentRow>()
                        suspend fun append(title: String, method: String, params: JSONObject = JSONObject(), wide: Boolean = false) {
                            val cards = array(method, params).map { row -> tile(row).let { if(wide) it.copy(image = row.optString("backdrop").ifBlank { row.optString("poster") }) else it } }
                            rows.add(ContentRow(title, cards, wide))
                            if (ticket == revision) screen = screen.copy(rows = rows.toList())
                        }
                        append("Seguir viendo", "catalog.continue", wide = true)
                        append("Streaming", "catalog.providers", wide = true)
                        append("Explorar por género", "catalog.genres", wide = true)
                        append("Para ti · Tus valoraciones", "catalog.shelf", JSONObject().put("key", "recommendations"))
                        append("Películas populares", "catalog.shelf", JSONObject().put("key", "popular_movies"))
                        append("Series populares", "catalog.shelf", JSONObject().put("key", "popular_series"))
                        DesktopScreenState(hero = trending.firstOrNull(), rows = rows)
                    }
                    DesktopTab.Search -> DesktopScreenState(tiles = array("catalog.home").map(::tile))
                    DesktopTab.Library -> DesktopScreenState(tiles = array("catalog.saved").map(::tile))
                    DesktopTab.Live, DesktopTab.Sports -> DesktopScreenState(tiles = array("live.list", JSONObject().put("provider", if (next == DesktopTab.Live) "tv" else "sports")).map {
                        MediaTile("live:${if(next == DesktopTab.Live) "tv" else "sports"}:${it.getString("id")}", it.optString("name"), it.optString("logo").ifBlank { it.optString("poster") }, it.optString("category"))
                    })
                    DesktopTab.Friends -> {
                        val response = bridge.call("friends.list") as JSONObject
                        val activity = (response.optJSONArray("activity") ?: JSONArray()).objects().map {
                            FeedEntry(it.optString("user_id", it.optString("nickname")), "@"+it.optString("nickname"), it.optString("title").ifBlank { if (it.optBoolean("online")) "En línea" else "Desconectado" })
                        }
                        val pending = (response.optJSONArray("pending") ?: JSONArray()).objects().map {
                            FeedEntry("accept:"+it.getString("sender_id"), "Solicitud de amistad", it.getString("sender_id").take(8), "Aceptar")
                        }
                        DesktopScreenState(entries = pending + activity)
                    }
                    DesktopTab.Calendar -> DesktopScreenState(calendarMonth = month.toString(), entries = array("calendar.list", JSONObject().put("month", month.toString())).map {
                        FeedEntry(it.optString("cloud_id"), it.optString("title", "Evento"), it.optString("starts_at"), if (it.optString("cloud_id").isNotBlank()) "Ver contenido" else null)
                    })
                    DesktopTab.Notifications -> {
                        val data=bridge.call("notifications.list") as JSONObject
                        val rows=data.getJSONArray("items")
                        DesktopScreenState(entries=(0 until rows.length()).map{FeedEntry(it.toString(),rows.getString(it),"Novedades de tu lista")})
                    }
                    DesktopTab.Settings -> DesktopScreenState()
                    else -> DesktopScreenState(error = "Esta función todavía no está conectada en la reconstrucción de PC.")
                }
                if (ticket == revision) { screen = loaded; rawTiles = loaded.tiles; cache[next] = loaded }
                if(next==DesktopTab.Calendar && ticket==revision) {
                    val sports=bridge.call("calendar.sports",JSONObject().put("month",month.toString())) as JSONObject
                    if(ticket==revision) {
                        val events=sports.getJSONArray("events").objects().map { FeedEntry(it.optString("cloud_id"),it.optString("title"),it.optString("starts_at")) }
                        val warnings=sports.getJSONArray("warnings")
                        screen=screen.copy(entries=loaded.entries+events,error=if(warnings.length()>0)"Algunas ligas no respondieron: "+(0 until warnings.length()).joinToString{warnings.getString(it)}else null)
                        cache[next]=screen
                    }
                }
            } catch (e: Exception) { if (ticket == revision) screen = screen.copy(busy = false, error = e.message) }
        }
    }
    fun search(query: String) {
        searchJob?.cancel(); val ticket = ++revision
        searchJob = scope.launch {
            delay(300); screen = screen.copy(busy = true)
            try {
                val cards = array(if (query.isBlank()) "catalog.home" else "catalog.search", if(query.isBlank()) JSONObject() else JSONObject().put("query", query)).map(::tile)
                if (ticket == revision) screen = DesktopScreenState(tiles = cards)
            } catch(e: Exception) { if (ticket == revision) screen = screen.copy(busy = false, error = e.message) }
        }
    }
    fun changeMonth(delta: Long) { month = month.plusMonths(delta); select(DesktopTab.Calendar, true) }
    fun sendFriend(nickname: String) = scope.launch {
        try { bridge.call("friends.add", JSONObject().put("nickname", nickname)); addFriend = false; message = "Solicitud enviada"; select(DesktopTab.Friends, true) }
        catch(e: Exception) { message = e.message }
    }
    fun action(id: String) {
        if (id == "retry") { select(tab, true); return }
        if (id == "friends:add") { addFriend = true; return }
        if(id=="notifications:refresh"){refreshNotices();return}
        if(id=="notifications:clear"){clearNotices();return}
        if (id.startsWith("filter:")) {
            val filter = id.substringAfter(":")
            screen = screen.copy(tiles = rawTiles.filter { filter == "Todos" || it.subtitle == filter ||
                filter == "Películas" && it.id.startsWith("tmdb:movie:") || filter == "Series" && it.id.startsWith("tmdb:series:") })
            return
        }
        val ticket = revision
        scope.launch {
            try {
                when {
                    id=="settings:Cerrar sesión" -> {
                        account=bridge.call("session.logout") as JSONObject;revision++;cache.clear();queue.clear();playback=null;profilePicker=false;detail=null;settings=null
                    }
                    id.startsWith("settings:") -> {
                        val supported = listOf("Autoplay", "Reintentar otra fuente", "Límite de tamaño para PC", "Idioma de audio", "Idioma de subtítulos", "Límite de resultados", "Límite de tamaño de descarga")
                        if(id.substringAfter(":") in supported) settings = bridge.call("settings.get") as JSONObject
                        else message = "Ese ajuste aún no está migrado a PC."
                    }
                    id.startsWith("live:") -> {
                        val parts = id.split(":", limit = 3)
                        val rows = array("live.sources", JSONObject().put("provider", parts[1]).put("channel_id", parts[2]))
                        val title = rawTiles.firstOrNull { it.id == id }?.title ?: "Live"
                        if(ticket == revision && rows.isNotEmpty()) playback = PlaybackSession(JSONObject().put("title", title).put("type", "live").put("cloudId","tmdb:live:${parts[2].hashCode()}").put("liveChannelId",parts[2]).put("liveProvider",if(parts[1]=="tv")"nauta" else "sports").put("liveType","tv"), rows)
                    }
                    id.startsWith("tmdb:") -> { val value = bridge.call("catalog.details", JSONObject().put("cloud_id", id)) as JSONObject; if (ticket == revision) detail = value }
                    id.startsWith("provider:") || id.startsWith("genre:") -> {
                        val params = JSONObject().put(if(id.startsWith("provider:")) "provider" else "genre", id.substringAfter(":").toInt())
                        val cards = array("catalog.discover", params).map(::tile)
                        if(ticket == revision) { tab = DesktopTab.Search; screen = DesktopScreenState(tiles = cards) }
                    }
                    id.startsWith("accept:") -> { bridge.call("friends.accept", JSONObject().put("sender_id", id.substringAfter(":"))); if(ticket == revision) select(DesktopTab.Friends, true) }
                    else -> message = "Esta acción aún necesita su controlador en la versión nueva de PC."
                }
            } catch(e: Exception) { message = e.message }
        }
    }
    fun play(item: JSONObject, season: Int = 0, episode: Int = 0, position:Long? = null) = scope.launch {
        val ticket = ++revision
        message = null
        try {
            val result = bridge.call("catalog.sources", JSONObject().put("cloud_id", item.getString("cloudId")).put("season", season).put("episode", episode)) as JSONObject
            val streams = result.getJSONArray("streams").objects()
            if(streams.isEmpty()) { message = "Los complementos no devolvieron fuentes compatibles"; return@launch }
            if(ticket == revision) {
                detail = null; episodes = null
                val settings = bridge.call("settings.get") as JSONObject
                val next = if(item.optString("type") == "series") runCatching {
                    bridge.call("catalog.next", JSONObject().put("cloud_id", item.getString("cloudId")).put("season", season).put("episode", episode)) is JSONObject
                }.getOrDefault(false) else false
                if(ticket != revision) return@launch
                playback = PlaybackSession(item, streams, result.getString("scope"), season, episode,
                    result.optString("audio", "es"), result.optString("sub", "es"), startMs = position ?: result.optLong("resume_ms"), retry = settings.optBoolean("retry", true), autoplay = settings.optBoolean("autoplay", true), nextAvailable = next, sourceLimit=settings.optInt("source_limit",7))
            }
        } catch(e: Exception) { if(ticket == revision) message = e.message }
    }
    fun chooseEpisodes(item: JSONObject, season: Int) = scope.launch {
        val ticket = revision; val request = ++episodeRevision; episodes = null
        try { val rows = array("catalog.episodes", JSONObject().put("cloud_id", item.getString("cloudId")).put("season", season)); if(ticket == revision && request == episodeRevision) episodes = rows }
        catch(e: Exception) { if(ticket == revision) message = e.message }
    }
    fun saveProgress(session: PlaybackSession, position: Long, duration: Long) = scope.launch {
        try { bridge.call("playback.progress", JSONObject().put("cloud_id", session.item.getString("cloudId")).put("scope", session.owner)
            .put("position_ms", position).put("duration_ms", duration).put("season", session.season).put("episode", session.episode)) }
        catch (_: Exception) { /* A changed profile must never receive stale playback progress. */ }
    }
    fun nextEpisode(session: PlaybackSession) = scope.launch {
        if(playback !== session)return@launch
        if(queue.isNotEmpty()){val next=queue.removeAt(0);play(next.item,next.season,next.episode);return@launch}
        if(session.item.optString("type")!="series")return@launch
        try {
            val result = bridge.call("catalog.next", JSONObject().put("cloud_id", session.item.getString("cloudId")).put("season", session.season).put("episode", session.episode))
            if(playback === session && result is JSONObject) play(session.item, result.getInt("season"), result.getInt("episode"))
        } catch(e: Exception) { if(playback === session) message = e.message }
    }
    fun applyParty(value:JSONObject) {
        if(value.optString("scope")!=account?.optString("scope") || value.optBoolean("host") || value.isNull("room"))return
        val state=value.optJSONObject("state")?:return
        val cloud=state.optString("cloudId")
        if(cloud.isBlank())return
        val current=playback
        if(current?.item?.optString("cloudId")==cloud && current.season==state.optInt("season") && current.episode==state.optInt("episode"))return
        val key=cloud+":"+state.optInt("season")+":"+state.optInt("episode")
        if(remoteLoading==key)return
        remoteLoading=key
        val ticket=revision
        scope.launch {
            try {
                if(state.optString("liveChannelId").isNotBlank()) {
                    val provider=if(state.optString("liveProvider")=="sports")"sports" else "tv"
                    array("live.list",JSONObject().put("provider",provider))
                    val rows=array("live.sources",JSONObject().put("provider",provider).put("channel_id",state.getString("liveChannelId")))
                    if(ticket==revision && remoteLoading==key)playback=PlaybackSession(JSONObject().put("type","live").put("title",state.optString("liveTitle")).put("cloudId",cloud).put("liveChannelId",state.getString("liveChannelId")).put("liveProvider",state.optString("liveProvider")).put("liveType",state.optString("liveType","tv")),rows)
                } else {
                    val details=bridge.call("catalog.details",JSONObject().put("cloud_id",cloud)) as JSONObject
                    val sources=bridge.call("catalog.sources",JSONObject().put("cloud_id",cloud).put("season",state.optInt("season")).put("episode",state.optInt("episode"))) as JSONObject
                    if(ticket==revision && remoteLoading==key)playback=PlaybackSession(details.getJSONObject("item"),sources.getJSONArray("streams").objects(),sources.getString("scope"),state.optInt("season"),state.optInt("episode"),sources.optString("audio","es"),sources.optString("sub","es"),startMs=state.optLong("positionMs"),autoplay=false)
                }
            } catch(e:Exception) { if(ticket==revision)message=e.message }
            finally { if(remoteLoading==key)remoteLoading=null }
        }
    }
    fun beginMulti() {
        val current=playback?:return
        if(current.item.optString("type")!="live")return
        closePlayer();multiview.add(current);multiPicker=true;loadMulti("tv")
    }
    fun loadMulti(provider:String)=scope.launch {
        val ticket=revision
        try {
            val rows=array("live.list",JSONObject().put("provider",provider)).map { MediaTile("live:$provider:${it.getString("id")}",it.optString("name"),it.optString("logo").ifBlank{it.optString("poster")},it.optString("category")) }
            if(ticket==revision)multiChoices=rows
        } catch(e:Exception){message=e.message}
    }
    fun addMulti(tile:MediaTile)=scope.launch {
        if(multiview.size>=4)return@launch
        val ticket=revision;val parts=tile.id.split(":",limit=3)
        val cloud="tmdb:live:${parts[2].hashCode()}"
        if(multiview.any{it.item.optString("cloudId")==cloud})return@launch
        try {
            val streams=array("live.sources",JSONObject().put("provider",parts[1]).put("channel_id",parts[2]))
            if(ticket==revision && streams.isNotEmpty() && multiview.size<4){multiview.add(PlaybackSession(JSONObject().put("type","live").put("title",tile.title).put("cloudId",cloud),streams));multiPicker=false}
        }catch(e:Exception){message=e.message}
    }
    fun exitMulti(){revision++;multiview.clear();multiPicker=false}
    fun playQueued(index:Int) { val next=queue.getOrNull(index)?:return;queue.removeAt(index);queueDialog=false;play(next.item,next.season,next.episode) }
    fun closePlayer() { revision++;remoteLoading=null;playback = null; cache.remove(DesktopTab.Home) }
    fun saveSetting(key: String, value: Any) = scope.launch {
        val current = settings ?: return@launch
        try {
            val result = bridge.call("settings.set", JSONObject().put("key", key).put("value", value).put("scope", current.getString("scope"))) as JSONObject
            if(settings?.optString("scope") == current.getString("scope")) settings = result
            cache.clear()
        } catch(e: Exception) { message = e.message }
    }
    fun refreshNotices()=scope.launch {
        val ticket=revision
        try {
            val plan=bridge.call("notifications.plan") as JSONObject
            val ids=plan.getJSONArray("ids")
            for(index in 0 until ids.length()) {
                if(ticket!=revision)return@launch
                bridge.call("notifications.scan",JSONObject().put("scope",plan.getString("scope")).put("cloud_id",ids.getString(index)))
            }
            if(ticket==revision)select(DesktopTab.Notifications,true)
        }catch(e:Exception){if(ticket==revision)message=e.message}
    }
    fun clearNotices()=scope.launch {
        try { val current=bridge.call("notifications.list") as JSONObject;bridge.call("notifications.clear",JSONObject().put("scope",current.getString("scope")));select(DesktopTab.Notifications,true) }
        catch(e:Exception){message=e.message}
    }
    private suspend fun array(method: String, params: JSONObject = JSONObject()): List<JSONObject> = (bridge.call(method, params) as JSONArray).objects()
    private fun tile(row: JSONObject): MediaTile {
        val progress = row.optJSONObject("progress")?.optDouble("percent", 0.0)?.div(100)?.toFloat()
        return MediaTile(row.optString("cloudId", row.optString("id")), row.optString("title"), row.optString("poster").ifBlank { row.optString("backdrop") }, row.optString("overview").ifBlank { row.optString("year") }, progress)
    }
}
internal fun JSONArray.objects(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }
