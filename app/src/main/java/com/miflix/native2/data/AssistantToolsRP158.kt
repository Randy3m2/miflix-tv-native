package com.miflix.native2.data

import android.content.Context
import org.json.*
import java.time.*

internal enum class AssistantToolRP158 { ANALYZE, LIVE_FAVORITES, BOOKMARK, JUMP, HOME, ZAP, MULTIVIEW, FRANCHISE, VISUAL, HISTORY, AUTOPLAY, TRAILERS, EVENTS, STORY_ORDER, LISTS }
internal data class AssistantToolRequestRP158(val kind:AssistantToolRP158,val text:String="")
internal object AssistantToolsRP158 {
    fun parse(text:String):AssistantToolRequestRP158? {
        val q=DiscoveryMood.normalize(text)
        val kind=when {
            Regex("mis listas (?:ai|ia)|my ai lists").containsMatchIn(q)->AssistantToolRP158.LISTS
            ("favorit" in q) && ("live tv" in q || "canales" in q || "tv en vivo" in q)->AssistantToolRP158.LIVE_FAVORITES
            Regex("guarda.*(?:escena|momento)|(?:save|bookmark) (?:this|the) (?:scene|moment)").containsMatchIn(q)->AssistantToolRP158.BOOKMARK
            Regex("llevame a cuando|salta a|scene jump|jump to|busca la escena|mis escenas|mis marcadores").containsMatchIn(q)->AssistantToolRP158.JUMP
            Regex("episodios.*(?:relacionados|historia|arco)|episodes.*(?:story|arc)|storyline").containsMatchIn(q)->AssistantToolRP158.STORY_ORDER
            "multiview" in q->AssistantToolRP158.MULTIVIEW
            "halloween" in q && ("home" in q || "primero" in q)->AssistantToolRP158.EVENTS
            ("home" in q || "continue watching" in q || "streaming" in q) && Regex("mueve|pon |ordena|move |arrange|restaura").containsMatchIn(q)->AssistantToolRP158.HOME
            Regex("canales? de|channels? (?:of|for)|documentary channels").containsMatchIn(q)->AssistantToolRP158.ZAP
            ("cronologic" in q || "chronologic" in q || "organiza" in q) && Regex("x.?men|star wars|marvel|walking dead|game of thrones").containsMatchIn(q)->AssistantToolRP158.FRANCHISE
            Regex("que significa|explica.*(?:pantalla|opcion)|explain.*(?:screen|option)").containsMatchIn(q)->AssistantToolRP158.VISUAL
            Regex("(?:que|cual).*(?:vi |vimos|watched)|historial|watch history").containsMatchIn(q)->AssistantToolRP158.HISTORY
            Regex("cuando termine|despues de este episodio|after this episode|when this.*ends").containsMatchIn(q)->AssistantToolRP158.AUTOPLAY
            "trailer" in q->AssistantToolRP158.TRAILERS
            Regex("analiza|analisis|analyze|que ritmo tiene|requiere concentracion|como es esta pelicula|analyze pacing").containsMatchIn(q)->AssistantToolRP158.ANALYZE
            else->return null
        }
        return AssistantToolRequestRP158(kind,text.take(500))
    }
    val aliases=linkedMapOf("continue" to listOf("continue watching","continuar viendo"),"streaming" to listOf("streaming","plataformas"),"genres_home" to listOf("generos","genres"),"horror_movies" to listOf("terror","horror"),"action" to listOf("accion","action"),"series" to listOf("series"),"movies" to listOf("peliculas","movies"))
    fun homeOrder(text:String,previous:List<String>,defaults:List<String>):List<String> {
        val q=DiscoveryMood.normalize(text)
        if("restaura" in q || "reset" in q) return defaults.distinct()
        val order=(previous+defaults).distinct().toMutableList()
        for(clause in q.split(Regex("\\s+y\\s+(?=mueve|pon)|,|\\s+and\\s+(?=move|put)"))) {
            val mentioned=aliases.mapNotNull {(key,names)->names.mapNotNull {name->clause.indexOf(name).takeIf {it>=0}}.minOrNull()?.let {key to it}}.sortedBy {it.second}.map {it.first}
            val target=mentioned.firstOrNull() ?: continue
            val anchor=mentioned.getOrNull(1)
            order.remove(target)
            when {
                anchor!=null && ("debajo" in clause || "despues" in clause || "below" in clause || "after" in clause)->order.add((order.indexOf(anchor)+1).coerceAtLeast(0),target)
                anchor!=null && ("encima" in clause || "antes" in clause || "above" in clause || "before" in clause)->order.add(order.indexOf(anchor).coerceAtLeast(0),target)
                "arriba" in clause || "primero" in clause || "top" in clause->order.add(0,target)
                else->order.add(target)
            }
        }
        return order
    }
    fun historyRange(text:String,today:LocalDate,zone:ZoneId):LongRange {
        val q=DiscoveryMood.normalize(text)
        val start=if("semana pasada" in q || "last week" in q) today.with(java.time.DayOfWeek.MONDAY).minusWeeks(1) else if("ayer" in q || "yesterday" in q) today.minusDays(1) else today.minusDays(30)
        val end=if("semana pasada" in q || "last week" in q) start.plusWeeks(1) else if("ayer" in q || "yesterday" in q) today else today.plusDays(1)
        return start.atStartOfDay(zone).toInstant().toEpochMilli() until end.atStartOfDay(zone).toInstant().toEpochMilli()
    }
    fun matchingMarks(text:String,marks:List<AssistantBookmarkRP158>):List<AssistantBookmarkRP158> {
        fun words(value:String)=DiscoveryMood.normalize(value).replace(Regex("transform[a-z]*"),"transform").split(' ').filter {it.length>2 && it !in setOf("llevame","cuando","salta","busca","quiero","escena","escenas","mis","marcadores","jump","scene","favoritas","guardadas","saltar","por","vez","the")}
        val wanted=words(text)
        return if(wanted.isEmpty()) marks else marks.filter {mark->val label=words(mark.label);wanted.all {it in label}}
    }
    fun checkedOrder(raw:String,ids:Set<String>):List<String> {
        val a=JSONObject(raw.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()).getJSONArray("order")
        val order=(0 until a.length()).map {a.getString(it)}
        require(order.size==ids.size && order.toSet()==ids)
        return order
    }
}
internal data class AssistantBookmarkRP158(val cloud:String,val season:Int,val episode:Int,val position:Long,val duration:Long,val source:String,val label:String,val created:Long)
internal object AssistantLocalRP158 {
    private fun prefs(c:Context)=c.getSharedPreferences("brunio_tools_rp158",0)
    fun bookmarks(c:Context,owner:String):List<AssistantBookmarkRP158> = runCatching {
        val a=JSONArray(prefs(c).getString("marks:$owner","[]"))
        (0 until a.length()).map {a.getJSONObject(it)}.map {AssistantBookmarkRP158(it.getString("cloud"),it.optInt("season"),it.optInt("episode"),it.getLong("position"),it.getLong("duration"),it.getString("source"),it.getString("label"),it.optLong("created"))}
    }.getOrDefault(emptyList())
    fun save(c:Context,owner:String,b:AssistantBookmarkRP158) {
        require(b.cloud.startsWith("tmdb:") && b.position>=0 && b.duration>0 && b.position<b.duration && b.source.isNotBlank() && b.label.isNotBlank())
        val rows=(listOf(b)+bookmarks(c,owner).filterNot {it.cloud==b.cloud && it.season==b.season && it.episode==b.episode && kotlin.math.abs(it.position-b.position)<1000}).take(100)
        val a=JSONArray();rows.forEach {a.put(JSONObject().put("cloud",it.cloud).put("season",it.season).put("episode",it.episode).put("position",it.position).put("duration",it.duration).put("source",it.source).put("label",it.label).put("created",it.created))}
        check(prefs(c).edit().putString("marks:$owner",a.toString()).commit())
    }
    fun remove(c:Context,owner:String,b:AssistantBookmarkRP158) {val rows=bookmarks(c,owner).filterNot {it==b};prefs(c).edit().remove("marks:$owner").commit();rows.reversed().forEach {save(c,owner,it)}}
    fun recordWatch(c:Context,owner:String,item:com.miflix.native2.model.MediaSummary,season:Int,episode:Int,playbackId:String) {
        if(item.type !in setOf("movie","series")) return
        val p=prefs(c);val a=runCatching {JSONArray(p.getString("history:$owner","[]"))}.getOrDefault(JSONArray())
        if((0 until a.length()).any {a.optJSONObject(it)?.optString("playback")==playbackId}) return
        val row=JSONObject().put("cloud",item.cloudId).put("season",season).put("episode",episode).put("at",System.currentTimeMillis()).put("playback",playbackId)
        val next=JSONArray().put(row);for(i in 0 until minOf(a.length(),499)) next.put(a.getJSONObject(i))
        p.edit().putString("history:$owner",next.toString()).apply()
    }
    fun history(c:Context,owner:String,range:LongRange):List<String> {
        val a=runCatching {JSONArray(prefs(c).getString("history:$owner","[]"))}.getOrDefault(JSONArray())
        return (0 until a.length()).map {a.getJSONObject(it)}.filter {it.optLong("at") in range}.map {it.getString("cloud")}.distinct()
    }
    fun event(c:Context,owner:String,enabled:Boolean) {prefs(c).edit().putBoolean("halloween:$owner",enabled).apply()}
    fun event(c:Context,owner:String)=prefs(c).getBoolean("halloween:$owner",false)
    fun seasonalOrder(order:List<String>,enabled:Boolean,date:LocalDate):List<String> = if(enabled && date.monthValue==10) (listOf("continue","horror_movies","seasonal")+order).distinct() else order
    fun compatible(b:AssistantBookmarkRP158,current:AssistantBookmarkRP158)=b.cloud==current.cloud && b.season==current.season && b.episode==current.episode && b.source==current.source && kotlin.math.abs(b.duration-current.duration)<=2000 && b.position<current.duration
}
