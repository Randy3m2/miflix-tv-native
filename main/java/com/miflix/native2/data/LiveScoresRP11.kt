package com.miflix.native2.data

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.time.LocalDate
import java.time.ZoneId

internal data class ScoreTeamRP11(val id:String,val name:String,val short:String,val logo:String,val score:String)
internal data class LiveScoreRP11(
    val id:String,val league:String,val date:String,val home:ScoreTeamRP11,val away:ScoreTeamRP11,
    val state:String,val phase:String,val starts:String,val updatedAt:Long=System.currentTimeMillis()
)
internal object ScorePolicyRP11 {
    fun delayMs(state:String,failures:Int=0):Long {
        if(state=="final") return 0
        if(failures>0) return (30000L*(1L shl failures.coerceAtMost(4))).coerceAtMost(300000L)
        return if(state=="live") 30000 else 120000
    }
    fun coordinate(value:Float)=if(value.isFinite()) value.coerceIn(0f,1f) else 0f
}
internal class LiveScoresRP11(private val fetch:suspend (String)->String = {Http.text(it,timeoutMs=12000)}) {
    private val mutex=Mutex()
    private val cache=mutableMapOf<String,Pair<Long,List<LiveScoreRP11>>>()
    suspend fun games(league:String,date:String):List<LiveScoreRP11> = mutex.withLock {
        require(league in listOf("NBA","MLB","LIDOM"))
        LocalDate.parse(date)
        val key="$league:$date";val now=System.currentTimeMillis()
        cache[key]?.takeIf {now-it.first<30000L}?.let {return@withLock it.second}
        val url=if(league=="NBA") "https://site.api.espn.com/apis/site/v2/sports/basketball/nba/scoreboard?dates=${date.replace("-","")}&limit=100"
            else "https://statsapi.mlb.com/api/v1/schedule?sportId=${if(league=="LIDOM") 17 else 1}"+
                (if(league=="LIDOM") "&leagueId=131" else "")+"&date=$date&hydrate=linescore"
        val json=JSONObject(fetch(url))
        val rows=if(league=="NBA") nba(json,date) else baseball(json,league,date)
        if(cache.size>=8) cache.remove(cache.keys.first())
        cache[key]=now to rows
        rows
    }
    companion object {
        fun today()=LocalDate.now(ZoneId.of("America/Santo_Domingo")).toString()
        private val mlbCodes=mapOf(108 to "LAA",109 to "ARI",110 to "BAL",111 to "BOS",112 to "CHC",
            113 to "CIN",114 to "CLE",115 to "COL",116 to "DET",117 to "HOU",118 to "KC",119 to "LAD",
            120 to "WSH",121 to "NYM",133 to "ATH",134 to "PIT",135 to "SD",136 to "SEA",137 to "SF",
            138 to "STL",139 to "TB",140 to "TEX",141 to "TOR",142 to "MIN",143 to "PHI",144 to "ATL",
            145 to "CWS",146 to "MIA",147 to "NYY",158 to "MIL")
        fun nba(json:JSONObject,date:String):List<LiveScoreRP11> {
            val events=json.optJSONArray("events") ?: return emptyList()
            return (0 until events.length()).mapNotNull {i ->
                val e=events.optJSONObject(i) ?: return@mapNotNull null
                val c=e.optJSONArray("competitions")?.optJSONObject(0) ?: return@mapNotNull null
                val teams=c.optJSONArray("competitors") ?: return@mapNotNull null
                val rows=(0 until teams.length()).mapNotNull {teams.optJSONObject(it)}
                val h=rows.firstOrNull {it.optString("homeAway")=="home"} ?: return@mapNotNull null
                val a=rows.firstOrNull {it.optString("homeAway")=="away"} ?: return@mapNotNull null
                val status=e.optJSONObject("status") ?: c.optJSONObject("status") ?: JSONObject()
                val type=status.optJSONObject("type") ?: JSONObject()
                val state=when {type.optBoolean("completed")->"final";type.optString("state")=="in"->"live";else->"scheduled"}
                fun team(row:JSONObject):ScoreTeamRP11 {
                    val t=row.optJSONObject("team") ?: JSONObject()
                    return ScoreTeamRP11(t.optString("id"),t.optString("displayName"),
                        t.optString("abbreviation"),t.optString("logo"),if(state=="scheduled") "—" else row.optString("score","—"))
                }
                val phase=when(state) {
                    "final"->"Final"
                    "live"->"Q${status.optInt("period")} · ${status.optString("displayClock")}"
                    else->type.optString("shortDetail",type.optString("description"))
                }
                LiveScoreRP11(e.optString("id"),"NBA",date,team(h),team(a),state,phase,e.optString("date"))
            }.filter {it.id.isNotBlank()}
        }
        fun baseball(json:JSONObject,league:String,date:String):List<LiveScoreRP11> {
            val dates=json.optJSONArray("dates") ?: return emptyList()
            return buildList {
                for(d in 0 until dates.length()) {
                    val games=dates.optJSONObject(d)?.optJSONArray("games") ?: continue
                    for(i in 0 until games.length()) {
                        val g=games.optJSONObject(i) ?: continue
                        val teams=g.optJSONObject("teams") ?: continue
                        val status=g.optJSONObject("status") ?: JSONObject()
                        val state=when(status.optString("abstractGameState")) {"Live"->"live";"Final"->"final";else->"scheduled"}
                        fun team(side:String):ScoreTeamRP11 {
                            val row=teams.optJSONObject(side) ?: JSONObject()
                            val t=row.optJSONObject("team") ?: JSONObject()
                            val id=t.optInt("id")
                            val code=(if(league=="MLB") mlbCodes[id] else null) ?: t.optString("name").split(" ").filter {it.isNotBlank()}.map {it.first()}.joinToString("").take(4)
                            val logo=if(league=="MLB" && id in mlbCodes) "https://a.espncdn.com/i/teamlogos/mlb/500/${code.lowercase(java.util.Locale.ROOT)}.png" else ""
                            return ScoreTeamRP11(id.toString(),t.optString("name"),code,logo,if(row.has("score")) row.optString("score") else "—")
                        }
                        val line=g.optJSONObject("linescore")
                        val inning=line?.optInt("currentInning") ?: 0
                        val phase=when {
                            state=="final"->"Final"
                            state=="live" && inning>0->(if(line?.optBoolean("isTopInning")==true) "▲ " else "▼ ")+inning
                            else->status.optString("detailedState")
                        }
                        add(LiveScoreRP11(g.optString("gamePk"),league,date,team("home"),team("away"),state,phase,g.optString("gameDate")))
                    }
                }
            }
        }
        fun encode(g:LiveScoreRP11):String {
            fun team(t:ScoreTeamRP11)=JSONObject().put("id",t.id).put("name",t.name).put("short",t.short).put("logo",t.logo).put("score",t.score)
            return JSONObject().put("id",g.id).put("league",g.league).put("date",g.date).put("home",team(g.home)).put("away",team(g.away))
                .put("state",g.state).put("phase",g.phase).put("starts",g.starts).put("updated",g.updatedAt).toString()
        }
        fun decode(raw:String):LiveScoreRP11 {
            val j=JSONObject(raw)
            fun team(key:String):ScoreTeamRP11 {val t=j.getJSONObject(key);return ScoreTeamRP11(t.getString("id"),t.getString("name"),t.optString("short"),t.optString("logo"),t.optString("score","—"))}
            val league=j.getString("league");require(league in listOf("NBA","MLB","LIDOM"))
            val date=j.getString("date");LocalDate.parse(date)
            return LiveScoreRP11(j.getString("id"),league,date,team("home"),team("away"),j.getString("state"),j.optString("phase"),j.optString("starts"),j.optLong("updated"))
        }
    }
}
