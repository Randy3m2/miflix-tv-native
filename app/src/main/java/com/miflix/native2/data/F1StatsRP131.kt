package com.miflix.native2.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

internal data class F1DriverRP131(val id:String,val name:String,val flag:String,val position:String,val lap:String,val gap:String)
internal data class F1SessionRP131(val id:String,val event:String,val type:String,val date:String,
    val state:String,val lap:Int,val drivers:List<F1DriverRP131>) {
    val live get()=state=="in"
    val final get()=state=="post"
}
internal data class F1SnapshotRP131(val sessions:List<F1SessionRP131>,val updatedAt:Long)

/** One bounded, cancelable scoreboard request; no per-driver fan-out or private credentials. */
internal class F1StatsRP131(private val fetch:suspend (String)->String={StatsHttpRP105.text(it)},
    private val now:()->Long={System.currentTimeMillis()}) {
    private val lock=Mutex()
    @Volatile private var snapshot:F1SnapshotRP131?=null
    fun cached()=snapshot
    suspend fun load():F1SnapshotRP131=lock.withLock {
        snapshot?.takeIf {now()-it.updatedAt in 0 until 10_000L}?.let {return@withLock it}
        val body=fetch(URL)
        val parsed=withContext(Dispatchers.Default) {F1SnapshotRP131(parse(JSONObject(body)),now())}
        currentCoroutineContext().ensureActive()
        snapshot=parsed
        parsed
    }
    companion object {
        const val URL="https://site.api.espn.com/apis/site/v2/sports/racing/f1/scoreboard"
        fun channel(provider:String,title:String)=provider=="f1" ||
            Regex("(?i)(\\bf1\\b|formula\\s*(1|one|uno)|fórmula\\s*1)").containsMatchIn(title)
        fun automatic(rows:List<F1SessionRP131>):F1SessionRP131? =
            rows.filter {it.live}.maxByOrNull {it.date}
                ?: rows.filter {it.final}.maxByOrNull {it.date}
                ?: rows.minByOrNull {it.date}
        private fun objects(array:JSONArray?)=(0 until (array?.length() ?: 0)).mapNotNull {array?.optJSONObject(it)}
        fun parse(json:JSONObject):List<F1SessionRP131> = objects(json.optJSONArray("events")).flatMap {event->
            objects(event.optJSONArray("competitions")).mapNotNull {competition->
                val id=competition.optString("id")
                if(!id.matches(Regex("[0-9]{1,12}"))) return@mapNotNull null
                val status=competition.optJSONObject("status") ?: JSONObject()
                val type=status.optJSONObject("type") ?: JSONObject()
                val state=when {
                    type.optBoolean("completed")->"post"
                    type.optString("state")=="in"->"in"
                    else->"pre"
                }
                val drivers=objects(competition.optJSONArray("competitors")).mapNotNull {row->
                    val athlete=row.optJSONObject("athlete") ?: return@mapNotNull null
                    val name=athlete.optString("shortName",athlete.optString("displayName"))
                    if(name.isBlank()) return@mapNotNull null
                    val stats=objects(row.optJSONArray("statistics")).associate {it.optString("name") to it.optString("displayValue","—")}
                    fun metric(vararg keys:String)=keys.firstNotNullOfOrNull {stats[it]?.takeIf {value->value.isNotBlank() && value!="null"}} ?: "—"
                    val order=row.optInt("order")
                    F1DriverRP131(row.optString("id"),name,athlete.optJSONObject("flag")?.optString("href").orEmpty(),
                        if(state!="pre" && order>0) order.toString() else "—",
                        metric("lapsCompleted","laps"),metric("gapToLeader","timeBehindLeader","gap"))
                }.distinctBy {it.id}.sortedBy {it.position.toIntOrNull() ?: Int.MAX_VALUE}
                F1SessionRP131(id,event.optString("name"),competition.optJSONObject("type")?.optString("abbreviation").orEmpty(),
                    competition.optString("date",event.optString("date")),state,status.optInt("period"),drivers)
            }
        }.distinctBy {it.id}.sortedBy {it.date}
    }
}
