package com.miflix.native2.data

import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.time.*

/** Public MotoGP calendar only. No account, streams, private credentials or invented session times. */
internal object MotoGpCalendarRP138 {
    fun matches(value:String)=Regex("(?i)\\bmoto\\s*gp\\b").containsMatchIn(value)
    fun url(year:Int)="https://api.pulselive.motogp.com/motogp/v1/events?seasonYear=$year"
    private fun instant(raw:String):Instant?=runCatching {
        OffsetDateTime.parse(raw.replace(Regex("([+-]\\d{2})(\\d{2})$"),"$1:$2")).toInstant()
    }.getOrNull()
    suspend fun schedule(start:LocalDate,end:LocalDate):List<SportEventRP120> = withContext(Dispatchers.IO) {
        val rows=mutableListOf<SportEventRP120>()
        for(year in start.year..end.year) {
            currentCoroutineContext().ensureActive()
            val raw=Http.text(url(year),timeoutMs=12000)
            require(raw.length<=4*1024*1024) {"MotoGP calendar too large"}
            rows+=parse(JSONArray(raw),year)
        }
        rows.distinctBy {it.key}.filter {it.day()>=start.toString() && it.day()<=end.toString()}.sortedBy {it.starts}
    }
    fun parse(events:JSONArray,year:Int):List<SportEventRP120> = buildList {
        require(events.length()<=300)
        for(i in 0 until events.length()) {
            val event=events.optJSONObject(i) ?: continue
            if(event.optJSONObject("season")?.optInt("year",year)!=year || !event.optBoolean("show_in_calendar",true)) continue
            val name=event.optString("name").trim();val id=event.optString("id")
            if(name.isBlank() || id.isBlank()) continue
            val cancelled=listOf("CANCEL","POSTPON").any {event.optString("status").contains(it,true)}
            val broadcasts=event.optJSONArray("broadcasts") ?: JSONArray()
            var published=false
            for(n in 0 until broadcasts.length()) {
                val session=broadcasts.optJSONObject(n) ?: continue
                val category=session.optJSONObject("category") ?: continue
                if(category.optString("acronym")!="MGP" && !category.optString("name").equals("MotoGP",true)) continue
                if(!session.optBoolean("show_in_calendar",true)) continue
                val time=instant(session.optString("date_start")) ?: continue
                val sessionId=session.optString("id");if(sessionId.isBlank()) continue
                val label=session.optString("name",session.optString("shortname"))
                add(SportEventRP120("$id-$sessionId","MOTOGP","$name · $label",time.toString().take(10),time.toEpochMilli(),
                    logos=listOf(CalendarSourcesRP120.logo("MOTOGP")),cancelled=cancelled || listOf("CANCEL","POSTPON").any {session.optString("status").contains(it,true)}))
                published=true
            }
            if(!published && event.optString("kind")=="GP" && event.optJSONObject("business_unit")?.optString("acronym")=="MGP") {
                // The weekend end date is known, but the provider has not published a race start.
                val date=event.optString("date_end").take(10)
                if(runCatching {LocalDate.parse(date)}.isSuccess) add(SportEventRP120("$id-date","MOTOGP",name,date,0,
                    logos=listOf(CalendarSourcesRP120.logo("MOTOGP")),timed=false,cancelled=cancelled))
            }
        }
    }.distinctBy {it.key}
}
