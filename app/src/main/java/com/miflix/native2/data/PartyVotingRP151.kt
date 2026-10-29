package com.miflix.native2.data

import org.json.JSONArray
import org.json.JSONObject

internal data class PartyPickRP151(val id:Int,val type:String,val title:String,val season:Int=0,val episode:Int=0) {
    val cloud get()="tmdb:$type:$id"
    fun json()=JSONArray().put(id).put(type).put(title.take(32)).put(season).put(episode)
}
internal data class PartyBallotRP151(val token:String,val opened:Long,val eventId:Long,val options:List<PartyPickRP151>,val votes:Map<String,Int> = emptyMap(),val closed:Boolean=false) {
    fun active(now:Long)=!closed && now in opened..(opened+600_000L)
    fun winners():List<Int> {val totals=options.indices.map {i->votes.values.count {it==i}};val max=totals.maxOrNull() ?: 0;return if(max==0) emptyList() else totals.indices.filter {totals[it]==max}}
}
/** Existing chat transport: no new heartbeat or schema required. New clients hide protocol messages. */
internal object PartyVotingRP151 {
    const val PREFIX="[BRUNIO-VOTE1]"
    fun body(j:JSONObject):String=(PREFIX+j.toString()).also {require(it.length<=400)}
    fun start(options:List<PartyPickRP151>):String {
        require(options.size in 2..3)
        return body(JSONObject().put("a","start").put("p",java.util.UUID.randomUUID().toString()).put("o",JSONArray(options.map {it.json()})))
    }
    fun vote(token:String,index:Int)=body(JSONObject().put("a","vote").put("p",token).put("i",index))
    fun close(token:String)=body(JSONObject().put("a","close").put("p",token))
    fun accepts(event:PartyEvent)=event.kind=="chat" && event.body.startsWith(PREFIX)
    fun reduce(old:PartyBallotRP151?,event:PartyEvent,host:String,members:Set<String>,now:Long):PartyBallotRP151? {
        if(!accepts(event)) return old
        return try {
            val j=JSONObject(event.body.removePrefix(PREFIX));val token=j.getString("p")
            if(!token.matches(Regex("[a-fA-F0-9-]{36}"))) return old
            if(j.getString("a")=="start") {
                if(event.userId!=host || event.createdAt<=0 || now-event.createdAt !in -30_000L..600_000L || event.id<=(old?.eventId ?: -1)) return old
                val a=j.getJSONArray("o");if(a.length() !in 2..3) return old
                val options=(0 until a.length()).map {i->val v=a.getJSONArray(i);PartyPickRP151(v.getInt(0),v.getString(1),v.getString(2).take(32),v.optInt(3),v.optInt(4))}
                if(options.any {it.id<=0 || it.type !in listOf("movie","series") || it.title.isBlank() || it.season !in 0..200 || it.episode !in 0..2000 || it.type=="series" && (it.season<1 || it.episode<1)} || options.distinctBy {"${it.cloud}:${it.season}:${it.episode}"}.size!=options.size) return old
                PartyBallotRP151(token,event.createdAt.coerceAtMost(now),event.id,options)
            } else {
                val current=old ?: return old
                if(token!=current.token || event.id<=current.eventId || !current.active(now)) return old
                when(j.getString("a")) {
                    "vote" -> {val index=j.getInt("i");if(event.userId !in members || index !in current.options.indices) old else current.copy(eventId=event.id,votes=current.votes+(event.userId to index))}
                    "close" ->if(event.userId==host) current.copy(eventId=event.id,closed=true) else old
                    else ->old
                }
            }
        } catch(_:Exception) {old}
    }
}
