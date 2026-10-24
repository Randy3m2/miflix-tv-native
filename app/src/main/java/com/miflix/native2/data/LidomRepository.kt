package com.miflix.native2.data

import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.CancellationException
import android.content.Context
import android.net.Uri
import org.json.JSONObject
import org.json.JSONArray
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import com.miflix.native2.model.StreamChoice

internal data class LidomTeam(val id:String,val mlb:Int,val name:String,val short:String,val color:String,val website:String,val logo:String)
internal data class LidomSource(val id:String,val team:String,val name:String,val kind:String,val url:String,val reference:String="",val note:String="",val aliases:List<String> = emptyList())
internal data class LidomGame(val id:Int,val home:LidomTeam,val away:LidomTeam,val time:String,val status:String,val broadcasts:List<String> = emptyList())
internal data class LidomVideo(val id:String,val title:String)

internal class LidomRepository(private val context:Context, owner:String) {
    private val prefs=context.getSharedPreferences("brunio_lidom",Context.MODE_PRIVATE)
    private val prefix=java.security.MessageDigest.getInstance("SHA-256").digest(owner.toByteArray()).joinToString("") {"%02x".format(it)}+":"
    private val seed=JSONObject(context.assets.open("lidom_teams.json").bufferedReader().use {it.readText()})
    val teams:List<LidomTeam> = seed.getJSONArray("teams").let {rows -> (0 until rows.length()).map {i ->
        val x=rows.getJSONObject(i);LidomTeam(x.getString("id"),x.getInt("mlb"),x.getString("name"),x.getString("short"),x.getString("color"),x.getString("website"),x.getString("logo"))}}
    fun team(id:String)=teams.firstOrNull {it.id==id}
    private fun decode(x:JSONObject)=LidomSource(x.getString("id"),x.getString("team"),x.getString("name"),x.getString("kind"),x.optString("url"),x.optString("reference"),x.optString("note"),x.optJSONArray("aliases")?.let {a -> (0 until a.length()).map {a.getString(it)}} ?: emptyList())
    private fun custom():List<LidomSource> = runCatching {JSONArray(prefs.getString(prefix+"sources","[]")).let {a -> (0 until a.length()).map {decode(a.getJSONObject(it))}}}.getOrDefault(emptyList())
    fun sources(team:String):List<LidomSource> {
        val seeds=seed.getJSONArray("teams").let {a -> (0 until a.length()).map {a.getJSONObject(it)}.first {it.getString("id")==team}.getJSONArray("sources")}.let {a -> (0 until a.length()).map {decode(a.getJSONObject(it))}}
        val overrides=custom();val hidden=prefs.getStringSet(prefix+"hidden",emptySet()).orEmpty()
        return (seeds.filter {s -> overrides.none {it.id==s.id}}+overrides.filter {it.team==team}).filter {it.id !in hidden}.sortedBy {if(it.kind=="YOUTUBE") 0 else 1}
    }
    fun save(source:LidomSource) {
        require(team(source.team)!=null && source.name.isNotBlank() && source.name.length<=100)
        require(source.kind in setOf("YOUTUBE","HLS","DASH","TV"))
        require(source.url.length<=4096 && (source.kind=="TV" || httpUrl(source.url))) {"Enter an HTTP(S) URL."}
        require(source.kind!="TV" || source.url.isBlank()) {"Choose HLS, DASH or YouTube to use a direct URL."}
        if(source.kind=="YOUTUBE") require(youtubeId(source.url)!=null || youtubeChannel(source.url)) {"Use a YouTube video or channel URL."}
        val rows=custom().filter {it.id!=source.id}+source
        val array=JSONArray();rows.forEach {s -> array.put(JSONObject().put("id",s.id).put("team",s.team).put("name",s.name).put("kind",s.kind).put("url",s.url).put("reference",s.reference).put("note",s.note).put("aliases",JSONArray(s.aliases)))}
        prefs.edit().putString(prefix+"sources",array.toString()).putStringSet(prefix+"hidden",prefs.getStringSet(prefix+"hidden",emptySet()).orEmpty()-source.id).apply()
    }
    fun delete(source:LidomSource) {prefs.edit().putStringSet(prefix+"hidden",prefs.getStringSet(prefix+"hidden",emptySet()).orEmpty()+source.id).apply()}
    fun favorite(team:String)=team in prefs.getStringSet(prefix+"favorites",emptySet()).orEmpty()
    fun toggleFavorite(team:String) {val ids=prefs.getStringSet(prefix+"favorites",emptySet()).orEmpty().toSet();prefs.edit().putStringSet(prefix+"favorites",if(team in ids) ids-team else ids+team).apply()}
    fun last(team:String)=prefs.getString(prefix+"last:"+team,null)
    fun lastVideo(team:String)=prefs.getString(prefix+"video:"+team,null)
    fun working(team:String,source:String,video:String="") {
        if(this.team(team)==null) return
        val edit=prefs.edit().putString(prefix+"last:"+team,source)
        if(video.matches(Regex("[A-Za-z0-9_-]{11}"))) edit.putString(prefix+"video:"+team,video)
        edit.apply()
    }
    suspend fun games(date:String):List<LidomGame> {
        require(date.matches(Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}")))
        val j=JSONObject(Http.text("https://statsapi.mlb.com/api/v1/schedule?sportId=17&leagueId=131&date=$date&hydrate=broadcasts",timeoutMs=15000))
        val dates=j.optJSONArray("dates") ?: return emptyList()
        return buildList {for(i in 0 until dates.length()) {
            val games=dates.getJSONObject(i).optJSONArray("games") ?: continue
            for(k in 0 until games.length()) {
                val g=games.getJSONObject(k);val t=g.getJSONObject("teams")
                val home=teams.firstOrNull {it.mlb==t.getJSONObject("home").getJSONObject("team").getInt("id")} ?: continue
                val away=teams.firstOrNull {it.mlb==t.getJSONObject("away").getJSONObject("team").getInt("id")} ?: continue
                val dateTime=runCatching {SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'",Locale.US).apply {timeZone=TimeZone.getTimeZone("UTC")}.parse(g.getString("gameDate"))}.getOrNull()
                val time=dateTime?.let {SimpleDateFormat("h:mm a",Locale.US).apply {timeZone=TimeZone.getTimeZone("America/Santo_Domingo")}.format(it)}.orEmpty()
                val broadcasts=g.optJSONArray("broadcasts")?.let {rows ->
                    (0 until rows.length()).mapNotNull {index ->
                        val broadcast=rows.optJSONObject(index) ?: return@mapNotNull null
                        broadcast.optString("name").trim().takeIf {
                            broadcast.optString("type")=="TV" && it.isNotBlank()
                        }
                    }.distinct()
                } ?: emptyList()
                val start=if(g.getJSONObject("status").optBoolean("startTimeTBD",false)) "" else time
                add(LidomGame(g.getInt("gamePk"),home,away,start,g.getJSONObject("status").optString("detailedState"),broadcasts))
            }
        }}
    }
    suspend fun resolve(source:LidomSource, manifest:String?=null):List<StreamChoice> {
        if(source.kind in setOf("HLS","DASH")) {require(httpUrl(source.url));return listOf(StreamChoice(source.name,source.url,source.name,addonName="LIDOM"))}
        require(source.kind=="TV")
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val labels=(source.aliases+source.name+source.reference.takeUnless {it.contains("://")}.orEmpty()).filter {it.isNotBlank()}.distinct()
            val exact=labels.map {FinalPlaylistRP97.nameKey(it)}.toSet()
            FinalCatalogRP97.rows().filter {row ->FinalPlaylistRP97.stationMatches(row.name,labels)}
                .sortedBy {if(FinalPlaylistRP97.nameKey(it.name) in exact) 0 else 1}.take(12)
                .map {row ->StreamChoice(source.name+" · "+row.name,row.url,row.name,requestHeaders=row.headers,addonName="LIDOM · Live TV")}
                .distinctBy {it.url to it.requestHeaders}
        }
    }

    private val youtubeChannelsRP61=mutableMapOf<String,String>()
    suspend fun videos(source:LidomSource):List<LidomVideo> {
        val channel=LidomUrlPolicy.channelId(source.url) ?: youtubeChannelsRP61[source.url] ?: run {
            if(!youtubeChannel(source.url)) return emptyList()
            val page=Http.text(source.url,timeoutMs=15000)
            val found=Regex("\"(?:channelId|externalId)\"\\s*:\\s*\"(UC[A-Za-z0-9_-]{22})\"").find(page)?.groupValues?.get(1)
                ?: Regex("youtube.com/channel/(UC[A-Za-z0-9_-]{22})").find(page)?.groupValues?.get(1)
                ?: return emptyList()
            youtubeChannelsRP61[source.url]=found
            found
        }
        val text=Http.text("https://www.youtube.com/feeds/videos.xml?channel_id=$channel",timeoutMs=15000)
        val parser=android.util.Xml.newPullParser();parser.setInput(text.reader());val rows=mutableListOf<LidomVideo>();var id="";var title="";var entry=false
        while(parser.eventType!=org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
            if(parser.eventType==org.xmlpull.v1.XmlPullParser.START_TAG) when(parser.name.substringAfter(':')) {"entry" -> {entry=true;id="";title=""};"videoId" -> if(entry) id=parser.nextText();"title" -> if(entry) title=parser.nextText()}
            else if(parser.eventType==org.xmlpull.v1.XmlPullParser.END_TAG && parser.name=="entry") {if(id.matches(Regex("[A-Za-z0-9_-]{11}"))) rows.add(LidomVideo(id,title));entry=false}
            parser.next()
        }
        return rows.take(15)
    }
    companion object {
        fun httpUrl(value:String)=LidomUrlPolicy.httpUrl(value)
        fun youtubeChannel(value:String)=LidomUrlPolicy.channelUrl(value)
        fun youtubeId(value:String):String?=LidomUrlPolicy.videoId(value)
        fun matchesStation(name:String,alias:String)=LidomUrlPolicy.matchesStation(name,alias)
        fun today()=SimpleDateFormat("yyyy-MM-dd",Locale.US).apply {timeZone=TimeZone.getTimeZone("America/Santo_Domingo")}.format(java.util.Date())
    }
}
