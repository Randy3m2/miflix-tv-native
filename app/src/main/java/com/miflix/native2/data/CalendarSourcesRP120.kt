package com.miflix.native2.data

import android.content.Context
import kotlinx.coroutines.*
import org.json.*
import java.time.*

internal data class CalendarTeamRP120(val league:String,val id:String,val name:String,val logo:String)
internal data class SportEventRP120(val id:String,val league:String,val title:String,val date:String,val starts:Long,
    val teams:List<String> = emptyList(),val logos:List<String> = emptyList(),val timed:Boolean=true,val cancelled:Boolean=false) {
    val key get()="$league:$id"
    val noticeKey get()=CalendarPolicyRP120.noticeKey(league,id,starts)
    fun day():String=if(timed) Instant.ofEpochMilli(starts).atZone(ZoneId.systemDefault()).toLocalDate().toString() else date
    fun json()=JSONObject().put("id",id).put("league",league).put("title",title).put("date",date).put("starts",starts)
        .put("teams",JSONArray(teams)).put("logos",JSONArray(logos)).put("timed",timed).put("cancelled",cancelled)
    companion object {
        fun parse(j:JSONObject):SportEventRP120 {
            fun strings(k:String)=j.optJSONArray(k)?.let {a ->(0 until a.length()).map {a.optString(it)}.filter {it.isNotBlank()}}.orEmpty()
            val iso=j.optString("startsAt")
            val starts=if(iso.isBlank()) j.optLong("starts") else Instant.parse(iso).toEpochMilli()
            val date=j.optString("date",iso.take(10));LocalDate.parse(date)
            val timed=j.optBoolean("timed",starts>0)
            require(!timed || starts>0) {"Timed events require a valid start"}
            require(j.optString("id").isNotBlank() && j.getString("title").isNotBlank())
            return SportEventRP120(j.optString("id"),j.getString("league"),j.getString("title"),date,starts,strings("teams"),strings("logos"),timed,j.optBoolean("cancelled"))
        }
    }
}

internal object CalendarSourcesRP120 {
    val leagues=listOf("NBA","MLB","LIDOM","F1","BOXING","GRAMMYS","OSCARS","MISS_UNIVERSE")
    fun logo(league:String)=when(league) {
        "NBA","MLB" ->"https://a.espncdn.com/i/teamlogos/leagues/500/${league.lowercase()}.png"
        "F1"->"drawable:calendar_f1_rp120"
        "BOXING"->"drawable:calendar_boxing_rp120"
        "GRAMMYS"->"drawable:calendar_music_rp120"
        "OSCARS"->"drawable:calendar_trophy_rp120"
        "MISS_UNIVERSE"->"drawable:calendar_crown_rp120"
        "LIDOM"->"drawable:calendar_baseball_rp120"
        else->""
    }
    fun teams(context:Context):List<CalendarTeamRP120> {
        val a=JSONArray(context.assets.open("calendar_teams_rp120.json").bufferedReader().use {it.readText()})
        return (0 until a.length()).map {a.getJSONObject(it)}.map {CalendarTeamRP120(it.getString("league"),it.optString("id"),it.getString("name"),it.optString("logo"))}
    }
    private suspend fun json(url:String)=JSONObject(withContext(Dispatchers.IO) {Http.text(url,timeoutMs=8000).also {require(it.length<=2*1024*1024) {"Schedule response too large"}}})
    suspend fun schedule(league:String,start:LocalDate,end:LocalDate,teams:List<CalendarTeamRP120>):List<SportEventRP120> {
        if(league=="F1") {
            val events=mutableListOf<SportEventRP120>()
            for(year in start.year..end.year) events+=f1(json("https://api.jolpi.ca/ergast/f1/$year.json?limit=100"))
            return events.filter {it.date>=start.toString() && it.date<=end.toString()}.map {it.copy(teams=teams.filter {team ->team.league=="F1"}.map {team ->team.id})}
        }
        if(league !in listOf("NBA","MLB","LIDOM")) return emptyList()
        val events=mutableListOf<SportEventRP120>()
        var from=start
        while(!from.isAfter(end)) {
            val to=minOf(from.plusDays(30),end)
            if(league=="NBA") {
                val j=json("https://site.api.espn.com/apis/site/v2/sports/basketball/nba/scoreboard?dates=${from.toString().replace("-","")}-${to.toString().replace("-","")}&limit=1000")
                events+=nba(j)
            } else {
                val j=json("https://statsapi.mlb.com/api/v1/schedule?sportId=${if(league=="MLB") 1 else 17}"+
                    (if(league=="LIDOM") "&leagueId=131" else "")+"&startDate=$from&endDate=$to")
                events+=baseball(j,league,teams)
            }
            from=to.plusDays(1)
        }
        return events.distinctBy {it.key}
    }
    fun nba(j:JSONObject):List<SportEventRP120> {
        val a=j.optJSONArray("events") ?: return emptyList()
        return (0 until a.length()).mapNotNull {i ->runCatching {
            val e=a.getJSONObject(i);val c=e.getJSONArray("competitions").getJSONObject(0)
            val teams=c.getJSONArray("competitors");val ids=mutableListOf<String>();val logos=mutableListOf<String>()
            for(n in 0 until teams.length()) {val team=teams.getJSONObject(n).getJSONObject("team");ids+=team.optString("id");team.optString("logo").takeIf {it.isNotBlank()}?.let {logos+=it}}
            val time=Instant.parse(e.getString("date"));val status=e.optJSONObject("status")?.optJSONObject("type")
            SportEventRP120(e.optString("id"),"NBA",e.getString("name"),time.toString().take(10),time.toEpochMilli(),ids,logos,
                timed=c.optBoolean("timeValid",true),cancelled=status?.optString("name").orEmpty().contains("CANCEL",true) || status?.optString("name").orEmpty().contains("POSTPON",true))
        }.getOrNull()}
    }
    fun baseball(j:JSONObject,league:String,teams:List<CalendarTeamRP120>):List<SportEventRP120> {
        val dates=j.optJSONArray("dates") ?: return emptyList()
        return buildList {
            for(d in 0 until dates.length()) {
                val games=dates.getJSONObject(d).optJSONArray("games") ?: continue
                for(i in 0 until games.length()) runCatching {
                    val g=games.getJSONObject(i);val pair=g.getJSONObject("teams")
                    val home=pair.getJSONObject("home").getJSONObject("team");val away=pair.getJSONObject("away").getJSONObject("team")
                    val ids=listOf(away.optString("id"),home.optString("id"));val time=Instant.parse(g.getString("gameDate"))
                    val status=g.optJSONObject("status")?.optString("detailedState").orEmpty()
                    add(SportEventRP120(g.optString("gamePk"),league,"${away.getString("name")} vs ${home.getString("name")}",g.optString("officialDate",time.toString().take(10)),time.toEpochMilli(),ids,
                        ids.mapNotNull {id ->teams.firstOrNull {it.league==league && it.id==id}?.logo},
                        timed=g.optString("status").isNotBlank() && g.optString("startTimeTBD")!="true" && !status.contains("TBD",true),
                        cancelled=status.contains("Postpon",true) || status.contains("Cancel",true)))
                }
            }
        }
    }
    fun f1(j:JSONObject):List<SportEventRP120> {
        val a=j.getJSONObject("MRData").getJSONObject("RaceTable").getJSONArray("Races")
        return buildList {
            for(i in 0 until a.length()) {
                val race=a.getJSONObject(i);val id=race.optString("season")+"-"+race.optString("round")
                fun addSession(key:String,label:String,data:JSONObject) {
                    val date=data.optString("date");if(date.isBlank()) return
                    val clock=data.optString("time");val time=if(clock.isBlank()) 0L else Instant.parse(date+"T"+clock).toEpochMilli()
                    add(SportEventRP120("$id-$key","F1",race.getString("raceName")+" · "+label,date,time,timed=clock.isNotBlank()))
                }
                addSession("race","Race",race)
                for((key,label) in listOf("Qualifying" to "Qualifying","Sprint" to "Sprint","FirstPractice" to "Practice 1","SecondPractice" to "Practice 2","ThirdPractice" to "Practice 3","SprintQualifying" to "Sprint qualifying"))
                    race.optJSONObject(key)?.let {addSession(key,label,it)}
            }
        }
    }
    fun specials():List<SportEventRP120> = listOf(
        SportEventRP120("2027","OSCARS","99th Oscars","2027-03-14",ZonedDateTime.of(2027,3,14,19,0,0,0,ZoneId.of("America/New_York")).toInstant().toEpochMilli()),
        SportEventRP120("2027","GRAMMYS","2027 Grammy Awards","2027-02-07",0,timed=false),
        SportEventRP120("2026-final","MISS_UNIVERSE","Miss Universe · Final","2026-11-24",0,timed=false))
    suspend fun feed(url:String):List<SportEventRP120> {
        require(url.startsWith("https://") && java.net.URI(url).userInfo==null)
        val j=json(url);val a=j.getJSONArray("events");require(a.length()<=2000)
        return (0 until a.length()).map {SportEventRP120.parse(a.getJSONObject(it))}.filter {it.league in leagues}
    }
}
