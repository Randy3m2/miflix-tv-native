package com.miflix.native2.data

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import org.json.*
import java.time.LocalDate

internal data class SportsSnapshotRP121<T>(val rows:T,val stale:Boolean,val updated:Long)
internal object SportsBrowseRepositoryRP121 {
    private val locks=Array(16) {Mutex()}
    fun season(league:String,date:LocalDate=LocalDate.now())=when(league) {"NBA"->date.year+if(date.monthValue>=7) 1 else 0;"LIDOM"->date.year-if(date.monthValue<7) 1 else 0;else->date.year}
    private suspend fun cached(c:Context,key:String,force:Boolean,load:suspend ()->JSONArray):SportsSnapshotRP121<JSONArray> = withContext(Dispatchers.IO) {
        locks[(key.hashCode() and Int.MAX_VALUE)%locks.size].withLock {
            val prefs=c.getSharedPreferences("brunio_sports_browse_rp121",0);val now=System.currentTimeMillis()
            val raw=prefs.getString(key,null);val at=prefs.getLong("at:$key",0)
            val saved=raw?.let {runCatching {JSONArray(it)}.getOrNull()}
            if(!force && saved!=null && now>=at && now-at<6*3600000L) return@withLock SportsSnapshotRP121(saved,false,at)
            try {
                val result=load();currentCoroutineContext().ensureActive()
                prefs.edit().putString(key,result.toString()).putLong("at:$key",now).apply()
                SportsSnapshotRP121(result,false,now)
            } catch(e:CancellationException) {throw e} catch(e:Exception) {if(saved!=null) SportsSnapshotRP121(saved,true,at) else throw e}
        }
    }
    private suspend fun json(url:String)=JSONObject(SportsPublicHttpRP121.text(url))
    suspend fun leaders(c:Context,league:String,force:Boolean=false):SportsSnapshotRP121<List<SportsLeadersRP121>> {
        val year=season(league)
        val snapshot=cached(c,"leaders:$league:$year",force) {
            val teams=CalendarSourcesRP120.teams(c)
            val groups=when(league) {
                "NBA"->SportsParsersRP121.nbaLeaders(json("https://site.api.espn.com/apis/site/v3/sports/basketball/nba/leaders?season=$year&seasontype=2&limit=5"),year)
                "MLB","LIDOM"->coroutineScope {
                    val sport=if(league=="MLB") 1 else 17
                    listOf("hitting" to "homeRuns,battingAverage,runsBattedIn,stolenBases,hits","pitching" to "earnedRunAverage,strikeouts,wins,whip").map {(group,categories) ->async {
                        SportsParsersRP121.baseballLeaders(json("https://statsapi.mlb.com/api/v1/stats/leaders?leaderCategories=$categories&statGroup=$group&season=$year&sportId=$sport"+(if(league=="LIDOM") "&leagueId=131" else "")+"&limit=5"),league,group,teams,year)
                    }}.awaitAll().flatten()
                }
                "F1"->coroutineScope {
                    val standings=async {json("https://api.jolpi.ca/ergast/f1/$year/driverStandings.json?limit=100")}
                    val grid=async {json("https://api.jolpi.ca/ergast/f1/$year/last/results.json?limit=100")}
                    val heads=async {json("https://sports.core.api.espn.com/v3/sports/racing/f1/athletes?limit=1000")}
                    SportsParsersRP121.f1Leaders(standings.await(),SportsParsersRP121.f1People(grid.await(),heads.await(),year),year)
                }
                else->emptyList()
            }
            JSONArray(groups.map {it.json()})
        }
        return withContext(Dispatchers.Default) {SportsSnapshotRP121((0 until snapshot.rows.length()).map {SportsLeadersRP121.parse(snapshot.rows.getJSONObject(it))},snapshot.stale,snapshot.updated)}
    }
    suspend fun players(c:Context,league:String,teamId:String="",force:Boolean=false):SportsSnapshotRP121<List<SportsPersonRP121>> {
        val year=season(league)
        val snapshot=cached(c,"players:$league:$year:${if(league=="NBA") teamId else "all"}",force) {
            val teams=CalendarSourcesRP120.teams(c);val selected=teams.firstOrNull {it.league==league && it.id==teamId}
            val people=when(league) {
                "NBA"->if(selected!=null) SportsParsersRP121.nbaRoster(json("https://site.api.espn.com/apis/site/v2/sports/basketball/nba/teams/${selected.id}/roster"),selected)
                    else SportsParsersRP121.nbaActive(json("https://sports.core.api.espn.com/v3/sports/basketball/nba/athletes?limit=1000&active=true"))
                "MLB"->SportsParsersRP121.mlbActive(json("https://statsapi.mlb.com/api/v1/sports/1/players?season=$year&hydrate=currentTeam"),teams)
                "LIDOM"->coroutineScope {
                    val limit=Semaphore(2)
                    teams.filter {it.league==league}.map {team ->async {
                        limit.withPermit {SportsParsersRP121.baseballRoster(json("https://statsapi.mlb.com/api/v1/teams/${team.id}/roster?rosterType=active&season=$year&hydrate=person"),team)}
                    }}.awaitAll().flatten()
                }
                "F1"->coroutineScope {
                    val grid=async {json("https://api.jolpi.ca/ergast/f1/$year/last/results.json?limit=100")}
                    val heads=async {json("https://sports.core.api.espn.com/v3/sports/racing/f1/athletes?limit=1000")}
                    SportsParsersRP121.f1People(grid.await(),heads.await(),year)
                }
                else->emptyList()
            }
            JSONArray(SportsParsersRP121.sorted(people).map {it.json()})
        }
        return withContext(Dispatchers.Default) {SportsSnapshotRP121((0 until snapshot.rows.length()).map {SportsPersonRP121.parse(snapshot.rows.getJSONObject(it))}.filter {league=="NBA" || teamId.isBlank() || it.teamId==teamId},snapshot.stale,snapshot.updated)}
    }
}
