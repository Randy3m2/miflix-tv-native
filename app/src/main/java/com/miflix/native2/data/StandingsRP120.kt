package com.miflix.native2.data

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.*

internal data class StandingRowRP120(val id:String,val rank:String,val name:String,val logo:String,val values:List<String>)
internal data class StandingGroupRP120(val title:String,val columns:List<String>,val rows:List<StandingRowRP120>)
internal object StandingsRP120 {
    private val mutex=Mutex()
    suspend fun load(c:Context,league:String,season:Int,force:Boolean=false):Pair<List<StandingGroupRP120>,Boolean> = mutex.withLock {
        require(league in listOf("NBA","MLB","LIDOM","F1"));require(season in 1950..2100)
        val prefs=c.getSharedPreferences("brunio_standings_rp120",0);val key="$league:$season";val now=System.currentTimeMillis()
        val teams=withContext(Dispatchers.IO) {CalendarSourcesRP120.teams(c)}
        fun parse(raw:String):List<StandingGroupRP120> {val j=JSONObject(raw);return when(league) {"NBA"->nba(j);"F1"->f1(j);else->baseball(j,league,teams)}}
        val saved=prefs.getString(key,null)
        if(!force && saved!=null && now-prefs.getLong("time:$key",0)<21600000L) return@withLock parse(saved) to false
        val url=when(league) {
            "NBA"->"https://site.api.espn.com/apis/v2/sports/basketball/nba/standings?season=$season"
            "F1"->"https://api.jolpi.ca/ergast/f1/$season/constructorStandings.json?limit=100"
            else->"https://statsapi.mlb.com/api/v1/standings?leagueId=${if(league=="LIDOM") "131" else "103,104"}&season=$season&standingsTypes=regularSeason"
        }
        try {
            val raw=withContext(Dispatchers.IO) {
                val original=Http.text(url,timeoutMs=8000)
                if(league=="F1") JSONObject(original).put("driversRP120",JSONObject(Http.text("https://api.jolpi.ca/ergast/f1/$season/driverStandings.json?limit=100",timeoutMs=8000))).toString() else original
            }
            val rows=parse(raw)
            prefs.edit().putString(key,raw).putLong("time:$key",now).apply()
            rows to false
        } catch(e:CancellationException) {throw e} catch(e:Exception) {if(saved!=null) parse(saved) to true else throw e}
    }
    fun nba(j:JSONObject):List<StandingGroupRP120> {
        val groups=j.optJSONArray("children") ?: return emptyList()
        return (0 until groups.length()).mapNotNull {n ->
            val group=groups.getJSONObject(n);val entries=group.optJSONObject("standings")?.optJSONArray("entries") ?: return@mapNotNull null
            val rows=(0 until entries.length()).map {i ->
                val entry=entries.getJSONObject(i);val team=entry.getJSONObject("team");val stats=entry.optJSONArray("stats") ?: JSONArray()
                fun value(vararg names:String):String {for(x in 0 until stats.length()) {val s=stats.getJSONObject(x);if(s.optString("name") in names) return s.optString("displayValue",s.optString("value","—"))};return "—"}
                StandingRowRP120(team.optString("id"),value("playoffSeed","rank").takeIf {it!="—"} ?: (i+1).toString(),team.optString("displayName",team.optString("name")),team.optJSONArray("logos")?.optJSONObject(0)?.optString("href").orEmpty(),listOf(value("wins"),value("losses"),value("gamesBehind"),value("Last Ten Games","lastTen","last10")))
            }.sortedBy {it.rank.toIntOrNull() ?: 999}
            StandingGroupRP120(group.optString("name"),listOf("W","L","GB","L10"),rows)
        }
    }
    fun baseball(j:JSONObject,league:String,teams:List<CalendarTeamRP120>):List<StandingGroupRP120> {
        val groups=j.optJSONArray("records") ?: return emptyList()
        val divisionNames=mapOf(201 to "AL East",202 to "AL Central",200 to "AL West",204 to "NL East",205 to "NL Central",203 to "NL West")
        return (0 until groups.length()).map {n ->
            val group=groups.getJSONObject(n);val entries=group.optJSONArray("teamRecords") ?: JSONArray()
            val rows=(0 until entries.length()).map {i ->
                val entry=entries.getJSONObject(i);val team=entry.getJSONObject("team");val id=team.optString("id")
                val info=teams.firstOrNull {it.league==league && it.id==id}
                StandingRowRP120(id,entry.optString(if(league=="LIDOM") "leagueRank" else "divisionRank",(i+1).toString()),info?.name ?: team.optString("name"),info?.logo.orEmpty(),listOf(entry.optString("wins","—"),entry.optString("losses","—"),entry.optString(if(league=="LIDOM") "leagueGamesBack" else "gamesBack","—"),entry.optString("winningPercentage","—")))
            }.sortedBy {it.rank.toIntOrNull() ?: 999}
            StandingGroupRP120(if(league=="LIDOM") "LIDOM" else divisionNames[group.optJSONObject("division")?.optInt("id")] ?: "MLB",listOf("W","L","GB","PCT"),rows)
        }
    }
    fun f1(j:JSONObject):List<StandingGroupRP120> {
        val list=j.getJSONObject("MRData").getJSONObject("StandingsTable").optJSONArray("StandingsLists")?.optJSONObject(0) ?: return emptyList()
        val entries=list.optJSONArray("ConstructorStandings") ?: return emptyList()
        val constructors=StandingGroupRP120("Constructors",listOf("PTS","W"),(0 until entries.length()).map {i ->
            val e=entries.getJSONObject(i);val team=e.getJSONObject("Constructor");val id=team.getString("constructorId")
            StandingRowRP120(id,e.optString("position"),team.optString("name"),f1Logo(id),listOf(e.optString("points"),e.optString("wins")))
        })
        val drivers=j.optJSONObject("driversRP120")?.optJSONObject("MRData")?.optJSONObject("StandingsTable")?.optJSONArray("StandingsLists")?.optJSONObject(0)?.optJSONArray("DriverStandings")
        val driverRows=if(drivers==null) emptyList() else (0 until drivers.length()).map {i ->
            val e=drivers.getJSONObject(i);val driver=e.getJSONObject("Driver")
            val constructor=e.optJSONArray("Constructors")?.optJSONObject(0)?.optString("constructorId").orEmpty()
            StandingRowRP120(driver.getString("driverId"),e.optString("position"),driver.optString("givenName")+" "+driver.optString("familyName"),f1Logo(constructor),listOf(e.optString("points"),e.optString("wins")))
        }
        return listOf(StandingGroupRP120("Drivers",listOf("PTS","W"),driverRows),constructors).filter {it.rows.isNotEmpty()}
    }
    fun f1Logo(id:String)=mapOf(
        "aston_martin" to "https://r2.thesportsdb.com/images/media/team/badge/ez5rlk1740774066.png",
        "audi" to "https://r2.thesportsdb.com/images/media/team/badge/3uce6h1773158180.png",
        "alpine" to "https://r2.thesportsdb.com/images/media/team/badge/ozhoj31740774899.png",
        "cadillac" to "https://r2.thesportsdb.com/images/media/team/badge/2wqnjo1769429652.png",
        "mclaren" to "https://r2.thesportsdb.com/images/media/team/badge/kzqi7v1743602056.png",
        "mercedes" to "https://r2.thesportsdb.com/images/media/team/badge/6caw0r1744037679.png",
        "haas" to "https://r2.thesportsdb.com/images/media/team/badge/9yp3s51740773680.png",
        "red_bull" to "https://r2.thesportsdb.com/images/media/team/badge/nhlev81679826274.png",
        "ferrari" to "https://r2.thesportsdb.com/images/media/team/badge/rxwsqv1420417429.png",
        "rb" to "https://r2.thesportsdb.com/images/media/team/badge/ot7pjx1740775883.png"
        ,"williams" to "https://r2.thesportsdb.com/images/media/team/badge/fp1cil1740776050.png"
    )[id].orEmpty()
}
