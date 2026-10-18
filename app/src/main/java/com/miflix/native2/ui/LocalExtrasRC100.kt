package com.miflix.native2.ui

import android.content.SharedPreferences
import android.os.SystemClock
import androidx.compose.runtime.*
import com.miflix.native2.model.MediaSummary
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** Device-only settings. No API, stream resolver, or cloud sync is used here. */
internal class LocalExtrasRC100(private val prefs:SharedPreferences) {
    var revision by mutableIntStateOf(0)
        private set
    fun enabled(key:String):Boolean {revision;return prefs.getBoolean("enabled:$key",false)}
    fun enable(key:String,value:Boolean) {
        require(key in flags);prefs.edit().putBoolean("enabled:$key",value).apply();revision++
        if(key=="sleep" && !value) clearTimer()
        if(key=="still" && !value) {stillPrompt=false;unattendedEpisodes=0}
    }
    val flags=setOf("sleep","still","lists","hide","filters","tracks","technical","backup")
    fun data(scope:String):JSONObject {revision;return runCatching {JSONObject(prefs.getString("profile:$scope","{}")!!)}.getOrDefault(JSONObject())}
    private fun save(scope:String,body:JSONObject) {val text=body.toString();require(text.toByteArray(Charsets.UTF_8).size<=1_800_000) {"Local storage limit reached"};prefs.edit().putString("profile:$scope",text).apply();revision++}
    private fun mutate(scope:String,change:(JSONObject)->Unit) {val body=data(scope);change(body);save(scope,body)}
    fun lists(scope:String):List<JSONObject> {val a=data(scope).optJSONArray("lists") ?: JSONArray();return (0 until a.length()).map {a.getJSONObject(it)}}
    fun createList(scope:String,name:String):String {
        val title=name.trim().take(60);require(title.isNotEmpty());require(lists(scope).size<100)
        val id=UUID.randomUUID().toString()
        mutate(scope) {val a=it.optJSONArray("lists") ?: JSONArray();a.put(JSONObject().put("id",id).put("name",title).put("items",JSONArray()));it.put("lists",a)}
        return id
    }
    fun renameList(scope:String,id:String,name:String) {require(name.trim().isNotEmpty());editList(scope,id) {it.put("name",name.trim().take(60))}}
    private fun editList(scope:String,id:String,change:(JSONObject)->Unit) {mutate(scope) {body -> val a=body.optJSONArray("lists") ?: JSONArray();for(i in 0 until a.length()) if(a.getJSONObject(i).optString("id")==id) change(a.getJSONObject(i));body.put("lists",a)}}
    fun deleteList(scope:String,id:String) {mutate(scope) {it.put("lists",JSONArray(lists(scope).filter {row -> row.optString("id")!=id}))}}
    fun listItems(row:JSONObject):List<MediaSummary> {
        val a=row.optJSONArray("items") ?: JSONArray()
        return (0 until a.length()).mapNotNull {runCatching {decode(a.getJSONObject(it))}.getOrNull()}
    }
    fun add(scope:String,id:String,item:MediaSummary) {editList(scope,id) {row ->
        val a=row.optJSONArray("items") ?: JSONArray()
        require(a.length()<1000)
        if((0 until a.length()).none {a.getJSONObject(it).optString("cloud")==item.cloudId}) a.put(encode(item))
        row.put("items",a)
    }}
    fun remove(scope:String,id:String,cloud:String) {editList(scope,id) {row -> val a=row.optJSONArray("items") ?: JSONArray();row.put("items",JSONArray((0 until a.length()).map {a.getJSONObject(it)}.filter {it.optString("cloud")!=cloud}))}}
    fun hide(scope:String,cloud:String,days:Int) {mutate(scope) {val h=it.optJSONObject("hidden") ?: JSONObject();h.put(cloud,System.currentTimeMillis()+days*86_400_000L);it.put("hidden",h)}}
    fun hidden(scope:String,cloud:String):Boolean=enabled("hide") && data(scope).optJSONObject("hidden")?.optLong(cloud,0L)?.let {it>System.currentTimeMillis()}==true
    fun restoreHidden(scope:String) {mutate(scope) {it.remove("hidden")}}
    fun preference(scope:String,key:String):JSONObject=data(scope).optJSONObject(key) ?: JSONObject()
    fun savePreference(scope:String,key:String,value:JSONObject) {mutate(scope) {it.put(key,value)}}
    fun trackChoice(scope:String,item:MediaSummary):JSONObject?=if(enabled("tracks") && item.type=="series") data(scope).optJSONObject("tracks:${item.cloudId}") else null
    fun rememberTracks(scope:String,item:MediaSummary,player:androidx.media3.common.Player,delay:Long) {
        if(!enabled("tracks") || item.type!="series") return
        val formats=player.currentTracks.groups
        fun format(type:Int)=formats.filter {it.type==type}.firstNotNullOfOrNull {g -> (0 until g.length).firstOrNull {g.isTrackSelected(it)}?.let {g.getTrackFormat(it)}}
        val a=format(androidx.media3.common.C.TRACK_TYPE_AUDIO);val t=format(androidx.media3.common.C.TRACK_TYPE_TEXT)
        val previous=trackChoice(scope,item) ?: JSONObject()
        val choice=JSONObject().put("delay",delay)
            .put("textOff",player.trackSelectionParameters.disabledTrackTypes.contains(androidx.media3.common.C.TRACK_TYPE_TEXT))
            .put("audio",a?.language ?: previous.optString("audio"))
            .put("audioLabel",a?.label ?: previous.optString("audioLabel"))
            .put("text",t?.language ?: player.trackSelectionParameters.preferredTextLanguages.firstOrNull().orEmpty())
        savePreference(scope,"tracks:${item.cloudId}",choice)
    }
    fun clearTracks(scope:String) {mutate(scope) {body ->body.keys().asSequence().toList().filter {it.startsWith("tracks:")}.forEach {body.remove(it)}}}
    fun export(scope:String):JSONObject=JSONObject().put("schema",1).put("scope",scope).put("profile",data(scope)).put("stillLimit",stillLimit).put("flags",JSONObject().also {o -> flags.forEach {o.put(it,enabled(it))}})
    fun validateImport(scope:String,body:JSONObject) {
        require(body.getInt("schema")==1 && body.getString("scope")==scope) {"Backup belongs to another profile or version"}
        val profile=body.getJSONObject("profile");require(profile.toString().toByteArray(Charsets.UTF_8).size<=1_800_000)
        val a=profile.optJSONArray("lists") ?: JSONArray();require(a.length()<=100)
        val ids=mutableSetOf<String>()
        for(i in 0 until a.length()) {val row=a.getJSONObject(i);require(row.getString("id").isNotBlank() && ids.add(row.getString("id")));require(row.getString("name").length in 1..60)
            val items=row.getJSONArray("items");require(items.length()<=1000);for(j in 0 until items.length()) decode(items.getJSONObject(j))}
        profile.keys().forEach {require(it in setOf("lists","hidden") || it.startsWith("filter:") || it.startsWith("tracks:"))}
        require(body.optInt("stillLimit",3) in listOf(2,3,5))
        val f=body.getJSONObject("flags");f.keys().forEach {require(it in flags);f.getBoolean(it)}
    }
    fun importData(scope:String,body:JSONObject) {validateImport(scope,body);val edit=prefs.edit().putString("profile:$scope",body.getJSONObject("profile").toString());val f=body.getJSONObject("flags");flags.forEach {edit.putBoolean("enabled:$it",f.optBoolean(it,false))};edit.putInt("still_limit",body.optInt("stillLimit",3));edit.apply();stillLimit=body.optInt("stillLimit",3);clearTimer();stillPrompt=false;revision++}
    var timerDeadline by mutableStateOf(0L)
        private set
    var timerEpisodes by mutableIntStateOf(0)
        private set
    var sleepStopped by mutableStateOf(false)
        private set
    var stillPrompt by mutableStateOf(false)
        private set
    var unattendedEpisodes by mutableIntStateOf(0)
        private set
    private var lastEpisode=""
    private var safetyScope=""
    var stillLimit by mutableIntStateOf(prefs.getInt("still_limit",3))
        private set
    fun saveStillLimit(value:Int) {require(value in listOf(2,3,5));stillLimit=value;prefs.edit().putInt("still_limit",value).apply()}
    fun startMinutes(minutes:Int) {require(minutes in listOf(30,60,90));clearTimer();timerDeadline=SystemClock.elapsedRealtime()+minutes*60_000L}
    fun startEpisodes(count:Int) {require(count in 1..5);clearTimer();timerEpisodes=count}
    fun clearTimer() {timerDeadline=0;timerEpisodes=0;sleepStopped=false}
    fun tick():Boolean {if(enabled("sleep") && timerDeadline>0 && SystemClock.elapsedRealtime()>=timerDeadline) {clearTimer();sleepStopped=true;return true};return false}
    fun playbackStarted(scope:String) {if(scope!=safetyScope) {clearTimer();stillPrompt=false;unattendedEpisodes=0;safetyScope=scope};sleepStopped=false}
    fun touch() {unattendedEpisodes=0}
    fun acknowledge() {stillPrompt=false;unattendedEpisodes=0;sleepStopped=false}
    fun stopSafety() {stillPrompt=false;sleepStopped=true;unattendedEpisodes=0}
    fun episodeEnded(id:String,series:Boolean) {
        if(lastEpisode==id) return;lastEpisode=id
        if(enabled("sleep") && timerEpisodes>0) {timerEpisodes--;if(timerEpisodes==0) {sleepStopped=true;timerDeadline=0}}
        if(series && enabled("still") && !sleepStopped) {unattendedEpisodes++;if(unattendedEpisodes>=stillLimit) stillPrompt=true}
    }
    companion object {
        fun encode(item:MediaSummary):JSONObject=JSONObject().put("id",item.id).put("type",item.type).put("cloud",item.cloudId).put("title",item.title).put("overview",item.overview).put("poster",item.poster ?: "").put("backdrop",item.backdrop ?: "").put("rating",item.rating).put("year",item.year)
        fun decode(o:JSONObject):MediaSummary {val type=o.getString("type");require(type in listOf("movie","series"));require(o.getInt("id")>0)
            return MediaSummary(o.getInt("id"),type,o.getString("title"),o.optString("overview"),o.optString("backdrop").takeIf {it.isNotBlank()},o.optString("poster").takeIf {it.isNotBlank()},o.optDouble("rating",0.0),o.optString("year"))}
    }
}
