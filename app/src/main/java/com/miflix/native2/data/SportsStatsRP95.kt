package com.miflix.native2.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

internal data class StatsRowRP95(val id:String,val name:String,val values:List<String>,val photo:String="",val number:String="")
internal data class StatsGroupRP95(val team:String,val columns:List<String>,val rows:List<StatsRowRP95>,val teamId:String="")
internal data class StatsTeamRP95(val name:String,val short:String,val logo:String,val score:String,val values:Map<String,String>)
internal data class GameStatsRP95(val teams:List<StatsTeamRP95>,val phase:String,val final:Boolean,
    val players:List<StatsGroupRP95>,val pitching:List<StatsGroupRP95> = emptyList(),val updatedAt:Long=System.currentTimeMillis(),val live:Boolean=true)

/** Public game data only. No account, playback URL, Supabase, progress or presence dependencies. */
internal class SportsStatsRP95(private val fetch:suspend(String)->String={StatsHttpRP105.text(it)},
    private val now:()->Long={System.currentTimeMillis()}) {
    private val scores=listOf("NBA","MLB","LIDOM").associateWith {LiveScoresRP11(fetch)}
    private val locks=Array(16) {Mutex()}
    private val cache=linkedMapOf<String,GameStatsRP95>()
    suspend fun games(league:String,date:String):List<LiveScoreRP11> = withContext(Dispatchers.Default) {
        require(league in listOf("NBA","MLB","LIDOM"));scores.getValue(league).games(league,date)
    }
    fun cached(game:LiveScoreRP11):GameStatsRP95?=synchronized(cache) {cache["${game.league}:${game.id}"]}
    suspend fun stats(game:LiveScoreRP11,force:Boolean=false):GameStatsRP95=locks[("${game.league}:${game.id}".hashCode() and Int.MAX_VALUE)%locks.size].withLock {
        require(game.league in listOf("NBA","MLB","LIDOM") && game.id.matches(Regex("[0-9]{1,12}")))
        val key="${game.league}:${game.id}"
        synchronized(cache) {cache[key]}?.takeIf {!force && now()-it.updatedAt in 0 until if(it.final) 300_000L else 30_000L}?.let {return@withLock it}
        val url=if(game.league=="NBA") "https://site.api.espn.com/apis/site/v2/sports/basketball/nba/summary?event=${game.id}"
            else "https://statsapi.mlb.com/api/v1.1/game/${game.id}/feed/live?fields="+MLB_FIELDS
        val raw=fetch(url)
        val value=withContext(Dispatchers.Default) {
            val json=JSONObject(raw)
            (if(game.league=="NBA") nba(json,game) else baseball(json,game)).copy(updatedAt=now())
        }
        currentCoroutineContext().ensureActive()
        synchronized(cache) {
            cache.remove(key);cache[key]=value
            while(cache.size>8) cache.remove(cache.keys.first())
        }
        value
    }
    companion object {
        private val sportsTitle=Regex("(?i)\\b(nba|mlb|espn|sports?|deportes?|basketball|baseball|baloncesto|beisbol|béisbol|dsports|tyc|fox sports|tnt sports)\\b")
        internal fun available(live:Boolean,recording:Boolean,provider:String,title:String):Boolean = live && !recording &&
            (provider=="sports" || sportsTitle.containsMatchIn(title))
        private const val MLB_FIELDS="gamePk,gameData,status,detailedState,abstractGameState,liveData,linescore,currentInning,inningState,inningHalf,outs,balls,strikes,teams,home,away,runs,hits,errors,boxscore,team,name,id,teamStats,batting,pitching,players,person,fullName,jerseyNumber,batters,pitchers,stats,atBats,rbi,baseOnBalls,strikeOuts,homeRuns,inningsPitched,earnedRuns,numberOfPitches"
        private fun objects(a:JSONArray?)=(0 until (a?.length() ?: 0)).mapNotNull {a?.optJSONObject(it)}
        private fun text(j:JSONObject?,key:String)=if(j==null || j.isNull(key)) "—" else j.optString(key).takeIf {it.isNotBlank()} ?: "—"
        private fun list(j:JSONObject?,key:String)=objects(j?.optJSONArray(key))
        private fun phaseNBA(status:JSONObject):String {
            val type=status.optJSONObject("type") ?: JSONObject()
            return if(type.optBoolean("completed")) "Final" else {
                val quarter=status.optInt("period")
                if(quarter>0) (if(quarter<=4) "Q$quarter" else "OT${quarter-4}")+" · "+status.optString("displayClock")
                else type.optString("shortDetail",type.optString("description"))
            }
        }
        internal fun nba(json:JSONObject,game:LiveScoreRP11):GameStatsRP95 {
            val competition=json.optJSONObject("header")?.optJSONArray("competitions")?.optJSONObject(0)
                ?: error("NBA game data unavailable")
            require(competition.optString("id")==game.id) {"NBA game identity mismatch"}
            val status=competition.optJSONObject("status") ?: JSONObject()
            val competitors=list(competition,"competitors")
            val box=json.optJSONObject("boxscore") ?: JSONObject()
            val teamRows=list(box,"teams")
            val teams=listOf(game.away,game.home).map {original ->
                val row=teamRows.firstOrNull {it.optJSONObject("team")?.optString("id")==original.id}
                val stats=list(row,"statistics").associate {it.optString("name") to text(it,"displayValue")}
                val competitor=competitors.firstOrNull {it.optString("id")==original.id}
                StatsTeamRP95(original.name,original.short,original.logo,text(competitor,"score").takeUnless {it=="—"} ?: original.score,stats)
            }
            val playerGroups=list(box,"players").flatMap {group ->
                list(group,"statistics").map {table ->
                    val labels=table.optJSONArray("labels") ?: table.optJSONArray("names") ?: JSONArray()
                    val columns=(0 until labels.length()).map {labels.optString(it)}
                    val rows=list(table,"athletes").filterNot {it.optBoolean("didNotPlay")}.map {row ->
                        val athlete=row.optJSONObject("athlete") ?: JSONObject()
                        val values=row.optJSONArray("stats") ?: JSONArray()
                        StatsRowRP95(athlete.optString("id"),athlete.optString("displayName"),columns.indices.map {values.optString(it,"—")},athlete.optJSONObject("headshot")?.optString("href").orEmpty().ifBlank {athlete.optString("id").takeIf {it.matches(Regex("[0-9]+"))}?.let {"https://a.espncdn.com/i/headshots/nba/players/full/$it.png"}.orEmpty()},athlete.optString("jersey"))
                    }
                    StatsGroupRP95(group.optJSONObject("team")?.optString("displayName").orEmpty(),columns,rows,group.optJSONObject("team")?.optString("id").orEmpty())
                }
            }
            return GameStatsRP95(teams,phaseNBA(status),status.optJSONObject("type")?.optBoolean("completed")==true,playerGroups,live=status.optJSONObject("type")?.optString("state")=="in")
        }
        internal fun baseball(json:JSONObject,game:LiveScoreRP11):GameStatsRP95 {
            require(json.optLong("gamePk").toString()==game.id) {"MLB game data unavailable"}
            val live=json.optJSONObject("liveData") ?: JSONObject()
            val box=live.optJSONObject("boxscore")?.optJSONObject("teams") ?: JSONObject()
            val line=live.optJSONObject("linescore") ?: JSONObject()
            val status=json.optJSONObject("gameData")?.optJSONObject("status") ?: JSONObject()
            val finished=status.optString("abstractGameState")=="Final"
            val teams=listOf("away" to game.away,"home" to game.home).map {(side,original) ->
                val teamStats=box.optJSONObject(side)?.optJSONObject("teamStats")
                val batting=teamStats?.optJSONObject("batting")
                val pitching=teamStats?.optJSONObject("pitching")
                val score=line.optJSONObject("teams")?.optJSONObject(side)
                val values=linkedMapOf<String,String>()
                for(k in listOf("hits","homeRuns","rbi","baseOnBalls","strikeOuts")) values[k]=text(batting,k)
                values["errors"]=text(score,"errors");values["inningsPitched"]=text(pitching,"inningsPitched")
                StatsTeamRP95(original.name,original.short,original.logo,text(score,"runs").takeUnless {it=="—"} ?: original.score,values)
            }
            fun groups(kind:String,ids:String,keys:List<String>,labels:List<String>):List<StatsGroupRP95> = listOf("away","home").map {side ->
                val team=box.optJSONObject(side) ?: JSONObject();val people=team.optJSONObject("players") ?: JSONObject()
                val order=team.optJSONArray(ids) ?: JSONArray()
                val rows=(0 until order.length()).mapNotNull {index ->
                    val id=order.optInt(index);val row=people.optJSONObject("ID$id") ?: return@mapNotNull null
                    val stats=row.optJSONObject("stats")?.optJSONObject(kind)
                    if(stats==null || stats.length()==0) return@mapNotNull null
                    StatsRowRP95(id.toString(),row.optJSONObject("person")?.optString("fullName").orEmpty(),keys.map {text(stats,it)},if(id>0) "https://img.mlbstatic.com/mlb-photos/image/upload/w_80,q_auto:good,f_auto/v1/people/$id/headshot/67/current" else "",row.optString("jerseyNumber"))
                }
                StatsGroupRP95(team.optJSONObject("team")?.optString("name").orEmpty(),labels,rows,team.optJSONObject("team")?.optString("id").orEmpty())
            }
            val inning=line.optInt("currentInning")
            val phase=if(finished) "Final" else if(inning>0) "${if(line.optString("inningHalf")=="Top") "▲" else "▼"} $inning · ${line.optInt("outs")} OUT"
                else status.optString("detailedState",game.phase)
            return GameStatsRP95(teams,phase,finished,
                groups("batting","batters",listOf("atBats","hits","runs","rbi","homeRuns","baseOnBalls","strikeOuts"),listOf("AB","H","R","RBI","HR","BB","K")),
                groups("pitching","pitchers",listOf("inningsPitched","hits","earnedRuns","strikeOuts","baseOnBalls","numberOfPitches"),listOf("IP","H","ER","K","BB","P")),live=status.optString("abstractGameState")=="Live")
        }
        internal fun match(title:String,games:List<LiveScoreRP11>):LiveScoreRP11? {
            fun normalized(s:String)=" "+java.text.Normalizer.normalize(s,java.text.Normalizer.Form.NFD)
                .replace(Regex("\\p{M}+"),"").uppercase(java.util.Locale.ROOT).replace(Regex("[^A-Z0-9]+")," ").trim()+" "
            val hay=normalized(title)
            fun team(t:ScoreTeamRP11):Boolean {
                val words=t.name.split(" ").filter {it.length>2}
                val aliases=listOf(t.name,t.short,words.lastOrNull().orEmpty())
                return aliases.filter {it.length>=3}.any {hay.contains(normalized(it))}
            }
            return games.filter {team(it.home) && team(it.away)}.singleOrNull()
        }
    }
}
