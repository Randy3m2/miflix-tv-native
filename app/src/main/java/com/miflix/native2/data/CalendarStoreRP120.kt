package com.miflix.native2.data

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.*
import java.time.*

internal object CalendarStoreRP120 {
    val revision=MutableStateFlow(0L)
    private val mutex=Mutex()
    private val localWork=CoroutineScope(SupervisorJob()+Dispatchers.IO)
    private fun prefs(c:Context)=c.getSharedPreferences("brunio_calendar_rp120",Context.MODE_PRIVATE)
    private fun changed() {synchronized(this) {revision.value=revision.value+1}}
    fun owner(c:Context,profile:String):String {
        val account=runCatching {JSONObject(c.getSharedPreferences("miflix_native2",0).getString("session","{}")).optString("uid","local")}.getOrDefault("local")
        return "$account:$profile"
    }
    fun active(c:Context)=prefs(c).getString("active","").orEmpty()
    fun activate(c:Context,owner:String,name:String,starting:String="Starting now") {
        val previous=active(c)
        prefs(c).edit().putString("active",owner).putString("name:$owner",name).putString("starting:$owner",starting).apply()
        if(previous.isNotBlank() && previous!=owner) CalendarAlarmsRP120.schedule(c,previous)
        CalendarAlarmsRP120.schedule(c,owner)
    }
    fun starting(c:Context,owner:String)=prefs(c).getString("starting:$owner","Starting now").orEmpty()
    fun name(c:Context,owner:String)=prefs(c).getString("name:$owner","").orEmpty()
    fun subscriptions(c:Context,owner:String)=prefs(c).getStringSet("subs:$owner",emptySet()).orEmpty().toSet()
    fun feed(c:Context,owner:String)=prefs(c).getString("feed:$owner","").orEmpty()
    fun setFeed(c:Context,owner:String,url:String) {prefs(c).edit().putString("feed:$owner",url.trim()).putLong("refresh:$owner",0).apply();changed()}
    fun toggle(c:Context,owner:String,key:String) {
        val set=subscriptions(c,owner).toMutableSet();if(!set.add(key)) set.remove(key)
        prefs(c).edit().putStringSet("subs:$owner",set).putLong("refresh:$owner",0).apply();changed()
        localWork.launch {CalendarAlarmsRP120.schedule(c.applicationContext,owner)}
    }
    fun follows(c:Context,owner:String,e:SportEventRP120)=CalendarPolicyRP120.follows(subscriptions(c,owner),e.league,e.id,e.teams)
    fun events(c:Context,owner:String):List<SportEventRP120> = runCatching {
        val a=JSONArray(prefs(c).getString("events:$owner","[]"))
        (0 until a.length()).mapNotNull {i ->runCatching {SportEventRP120.parse(a.getJSONObject(i))}.getOrNull()}
    }.getOrDefault(emptyList()).filter {follows(c,owner,it) && !it.cancelled}.sortedWith(compareBy<SportEventRP120> {it.day()}.thenBy {it.starts})
    fun lastRefresh(c:Context,owner:String)=prefs(c).getLong("refresh:$owner",0)
    fun error(c:Context,owner:String)=prefs(c).getString("error:$owner","").orEmpty()
    suspend fun refresh(c:Context,owner:String,force:Boolean=false,month:java.time.YearMonth?=null) = mutex.withLock {
        val now=System.currentTimeMillis()
        val cacheKey=if(month==null) "refresh:$owner" else "month:$owner:$month"
        if(!CalendarPolicyRP120.refresh(prefs(c).getLong(cacheKey,0),now,force)) return@withLock
        val subs=subscriptions(c,owner)
        if(subs.isEmpty()) return@withLock
        val today=LocalDate.now();val start=month?.atDay(1)?.minusDays(1) ?: today.minusDays(1)
        val end=month?.atEndOfMonth()?.plusDays(1) ?: today.plusDays(93)
        val leagues=subs.map {if(it.startsWith("EVENT:")) it.split(":")[1] else it.substringBefore(":")}.distinct()
        val teams=withContext(Dispatchers.IO) {CalendarSourcesRP120.teams(c)}
        val failures=mutableListOf<String>()
        val results=supervisorScope {leagues.map {league ->async {
            try {league to CalendarSourcesRP120.schedule(league,start,end,teams)}
            catch(e:CancellationException) {throw e}
            catch(_:Exception) {synchronized(failures) {failures+=league};null}
        }}.awaitAll().filterNotNull()}
        val old=runCatching {JSONArray(prefs(c).getString("events:$owner","[]"))}.getOrDefault(JSONArray())
        var all=(0 until old.length()).mapNotNull {runCatching {SportEventRP120.parse(old.getJSONObject(it))}.getOrNull()}
        for((league,rows) in results) {
            if(league in listOf("NBA","MLB","LIDOM","F1")) all=all.filterNot {it.league==league && it.date>=start.toString() && it.date<=end.toString()}+rows
        }
        all=all+CalendarSourcesRP120.specials()
        val url=feed(c,owner)
        if(url.isNotBlank()) try {val rows=CalendarSourcesRP120.feed(url);val keys=rows.map {it.key}.toSet();all=all.filterNot {it.key in keys}+rows}
            catch(e:CancellationException) {throw e} catch(_:Exception) {failures+="EVENTS"}
        // Remove stale/irrelevant events, and deduplicate league/team subscriptions.
        all=all.distinctBy {it.key}.filter {it.date>=today.minusDays(7).toString() && CalendarPolicyRP120.follows(subs,it.league,it.id,it.teams)}.take(7000)
        prefs(c).edit().putString("events:$owner",JSONArray(all.map {it.json()}).toString())
            .putLong(cacheKey,if(subscriptions(c,owner)==subs) now else 0).putString("error:$owner",failures.distinct().joinToString(", ")).commit()
        changed();CalendarAlarmsRP120.schedule(c,owner)
    }
    fun notices(c:Context,owner:String)=events(c,owner).filter {it.noticeKey in prefs(c).getStringSet("sent:$owner",emptySet()).orEmpty() && it.noticeKey !in prefs(c).getStringSet("hidden:$owner",emptySet()).orEmpty()}
    fun clearNotices(c:Context,owner:String) {val hidden=prefs(c).getStringSet("hidden:$owner",emptySet()).orEmpty()+notices(c,owner).map {it.noticeKey};prefs(c).edit().putStringSet("hidden:$owner",hidden.takeLastSetRP120(1000)).apply();changed()}
    fun unread(c:Context,owner:String)=notices(c,owner).count {it.noticeKey !in prefs(c).getStringSet("read:$owner",emptySet()).orEmpty()}
    fun markRead(c:Context,owner:String) {prefs(c).edit().putStringSet("read:$owner",notices(c,owner).map {it.noticeKey}.toSet()).apply();changed()}
    @Synchronized fun tick(c:Context,owner:String) {
        if(active(c)!=owner) return
        val sent=prefs(c).getStringSet("sent:$owner",emptySet()).orEmpty().toMutableSet();var dirty=false
        for(event in events(c,owner)) if(CalendarPolicyRP120.due(event.starts,System.currentTimeMillis(),event.timed,event.cancelled,event.noticeKey in sent)) {
            sent+=event.noticeKey;dirty=true;CalendarAlarmsRP120.notify(c,owner,event)
        }
        if(dirty) {prefs(c).edit().putStringSet("sent:$owner",sent.takeLastSetRP120(1000)).commit();changed()}
    }
    private fun Set<String>.takeLastSetRP120(size:Int)=toList().takeLast(size).toSet()
}
