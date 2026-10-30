package com.miflix.native2.data

import com.miflix.native2.model.CloudSession
import org.json.JSONArray

internal data class NowPlayingEntryRP162(
    val profile:String,val cloud:String,val title:String,val season:Int,val episode:Int,val validUntil:Long
)
internal object NowPlayingRP162 {
    fun deviceTag(id:String,television:Boolean)=(if(television) "tv:" else "mobile:")+id
    fun decode(raw:String,now:Long):List<NowPlayingEntryRP162> {
        val array=JSONArray(raw)
        return (0 until minOf(array.length(),12)).mapNotNull {i ->
            val row=array.optJSONObject(i) ?: return@mapNotNull null
            val cloud=row.optString("cloud_id").trim();val title=row.optString("title").trim()
            val valid=row.optLong("valid_for_ms").coerceIn(0,12*60*60_000L)
            if(cloud.isBlank() || cloud.length>100 || cloud.contains("://") || cloud.any {it.isISOControl()} || title.isBlank() || valid<=0) null
            else NowPlayingEntryRP162(row.optString("profile_id").take(100),cloud,title.take(200),row.optInt("season").coerceIn(0,1000),row.optInt("episode").coerceIn(0,10000),now+valid)
        }.distinctBy {it.profile}
    }
    fun active(rows:List<NowPlayingEntryRP162>,now:Long)=rows.filter {it.validUntil>now}
}
internal class NowPlayingRepositoryRP162(
    private val url:String,private val key:String,
    private val fetch:suspend (String,Map<String,String>)->String={endpoint,headers->Http.text(endpoint,"POST",headers,"{}")}
) {
    // Uses the real account session, never a friend or profile's synthetic actor identity.
    suspend fun read(session:CloudSession,now:()->Long):List<NowPlayingEntryRP162> {
        require(session.userId.isNotBlank() && session.accessToken.isNotBlank())
        val raw=fetch(url.trimEnd('/')+"/rest/v1/rpc/brunio_now_playing_rp162",mapOf("apikey" to key,"Authorization" to "Bearer ${session.accessToken}","Content-Type" to "application/json"))
        return NowPlayingRP162.decode(raw,now())
    }
}
