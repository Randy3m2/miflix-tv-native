package com.miflix.native2.ui

import android.content.SharedPreferences
import androidx.compose.runtime.*
import com.miflix.native2.model.MediaSummary
import com.miflix.native2.data.DirectorPerson
import org.json.JSONArray
import org.json.JSONObject

/** Profile-owned local interests. Selection never marks a title watched or creates a rating. */
internal class TasteProfileRC102(private val prefs:SharedPreferences) {
    var revision by mutableIntStateOf(0)
        private set
    fun data(owner:String):JSONObject {revision;return runCatching {JSONObject(prefs.getString("profile:$owner","{}")!!)}.getOrDefault(JSONObject())}
    private fun save(owner:String,body:JSONObject) {
        body.put("version",body.optLong("version",0)+1)
        prefs.edit().putString("profile:$owner",body.toString()).apply();revision++
    }
    fun done(owner:String)=data(owner).optBoolean("done",false)
    fun enabled(owner:String)=data(owner).optBoolean("enabled",true)
    fun version(owner:String):Long=data(owner).optLong("version",0)
    fun enable(owner:String,on:Boolean) {save(owner,data(owner).put("enabled",on))}
    fun skip(owner:String) {if(!done(owner)) save(owner,data(owner).put("done",true))}
    fun seeds(owner:String):List<MediaSummary> {val a=data(owner).optJSONArray("titles") ?: JSONArray();return (0 until a.length()).mapNotNull {runCatching {LocalExtrasRC100.decode(a.getJSONObject(it))}.getOrNull()}}
    fun actorIds(owner:String):List<Int> {val a=data(owner).optJSONArray("actors") ?: JSONArray();return (0 until a.length()).mapNotNull {a.optJSONObject(it)?.optInt("id")?.takeIf {id ->id>0}}.distinct()}
    fun actors(owner:String):List<DirectorPerson> {val a=data(owner).optJSONArray("actors") ?: JSONArray();return (0 until a.length()).mapNotNull {i ->runCatching {val p=a.getJSONObject(i);DirectorPerson(p.getInt("id"),p.getString("name"),p.optString("photo").takeIf {it.isNotBlank()})}.getOrNull()}}
    fun genres(owner:String):Set<String> {val a=data(owner).optJSONArray("genres") ?: JSONArray();return (0 until a.length()).map {a.optString(it)}.filter {it in TasteGenresRC102.map {g ->g.key}}.toSet()}
    fun configure(owner:String,titles:List<MediaSummary>,actors:List<DirectorPerson>,genres:Set<String>) {
        val body=data(owner)
        body.put("done",true).put("titles",JSONArray(titles.distinctBy {it.cloudId}.take(30).map {LocalExtrasRC100.encode(it)}))
            .put("actors",JSONArray(actors.distinctBy {it.id}.take(12).map {JSONObject().put("id",it.id).put("name",it.name).put("photo",it.photo ?: "")}))
            .put("genres",JSONArray(genres.filter {key ->TasteGenresRC102.any {it.key==key}}.take(12)))
        save(owner,body)
    }
    private val playedMillis=linkedMapOf<String,Long>()
    fun viewing(owner:String,item:MediaSummary,ms:Long) {
        if(!enabled(owner)) return
        val key="$owner:${item.cloudId}";playedMillis[key]=(playedMillis[key] ?: 0L)+ms.coerceIn(0L,6000L)
        if(playedMillis.size>240) playedMillis.remove(playedMillis.keys.first())
    }
    /** Called by normal progress writes. Only increasing milestones cause a local write. */
    fun observe(owner:String,item:MediaSummary,position:Long,duration:Long,ended:Boolean) {
        if(!enabled(owner) || duration<=0 || item.type !in listOf("movie","series")) return
        val fraction=position.toDouble()/duration
        val watched=playedMillis["$owner:${item.cloudId}"] ?: 0L
        val stage=when {(ended || fraction>=.9) && watched>=minOf(duration/2,5*60_000L) ->3;watched>=30*60_000L ->2;watched>=5*60_000L || duration<20*60_000L && watched>=minOf(duration/2,60_000L) ->1;else ->0}
        if(stage==0) return
        val body=data(owner);val learned=body.optJSONObject("learned") ?: JSONObject()
        if((learned.optJSONObject(item.cloudId)?.optInt("stage",0) ?: 0)>=stage) return
        learned.put(item.cloudId,JSONObject().put("stage",stage).put("time",System.currentTimeMillis()).put("item",LocalExtrasRC100.encode(item)))
        if(learned.length()>120) learned.keys().asSequence().toList().minByOrNull {learned.optJSONObject(it)?.optLong("time",0) ?: 0L}?.let {learned.remove(it)}
        body.put("learned",learned);save(owner,body)
    }
    fun learned(owner:String):List<MediaSummary> {val l=data(owner).optJSONObject("learned") ?: JSONObject()
        return l.keys().asSequence().mapNotNull {key ->l.optJSONObject(key)}.sortedByDescending {it.optLong("time",0)}.mapNotNull {runCatching {LocalExtrasRC100.decode(it.getJSONObject("item"))}.getOrNull()}.take(3).toList()
    }
    fun clearLearning(owner:String) {playedMillis.keys.filter {it.startsWith("$owner:")}.toList().forEach {playedMillis.remove(it)};save(owner,data(owner).put("learned",JSONObject()))}
    fun cached(key:String,maxAge:Long):JSONObject? {
        val body=runCatching {JSONObject(prefs.getString("cache:$key","{}")!!)}.getOrDefault(JSONObject())
        return body.takeIf {it.has("payload") && System.currentTimeMillis()-it.optLong("at",0) in 0..maxAge}?.optJSONObject("payload")
    }
    fun cache(key:String,body:JSONObject) {prefs.edit().putString("cache:$key",JSONObject().put("at",System.currentTimeMillis()).put("payload",body).toString()).apply()}
    fun validate(body:JSONObject) {
        require(body.toString().toByteArray(Charsets.UTF_8).size<300_000)
        val titles=body.optJSONArray("titles") ?: JSONArray();require(titles.length()<=30)
        for(i in 0 until titles.length()) LocalExtrasRC100.decode(titles.getJSONObject(i))
        val actors=body.optJSONArray("actors") ?: JSONArray();require(actors.length()<=12)
        for(i in 0 until actors.length()) {val p=actors.getJSONObject(i);require(p.getInt("id")>0);require(p.getString("name").length<=160)}
        val learned=body.optJSONObject("learned") ?: JSONObject();require(learned.length()<=120)
        learned.keys().forEach {val row=learned.getJSONObject(it);require(row.getInt("stage") in 1..3);LocalExtrasRC100.decode(row.getJSONObject("item"))}
    }
    fun restore(owner:String,body:JSONObject) {validate(body);save(owner,JSONObject(body.toString()))}
}

internal data class TasteGenreRC102(val key:String,val es:String,val en:String,val movie:Int,val tv:Int)
internal val TasteGenresRC102=listOf(
    TasteGenreRC102("action","Acción","Action",28,10759),TasteGenreRC102("comedy","Comedia","Comedy",35,35),
    TasteGenreRC102("drama","Drama","Drama",18,18),TasteGenreRC102("romance","Romance","Romance",10749,18),
    TasteGenreRC102("soap","Novelas","Telenovelas",0,10766),TasteGenreRC102("thriller","Suspenso","Thriller",53,9648),
    TasteGenreRC102("horror","Terror","Horror",27,0),TasteGenreRC102("scifi","Ciencia ficción","Science fiction",878,10765),
    TasteGenreRC102("crime","Crimen","Crime",80,80),TasteGenreRC102("animation","Animación","Animation",16,16),
    TasteGenreRC102("documentary","Documentales","Documentaries",99,99),TasteGenreRC102("family","Familia","Family",10751,10751)
)
