package com.miflix.native2.data

import android.os.SystemClock
import com.miflix.native2.model.CloudSession
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** Bounded, profile-scoped cache. No timer, heartbeat or completion writes. */
internal class WatchedFriendsRP93(private val url:String,private val key:String) {
    private val cache=WatchedCacheRP93<List<SocialPerson>>()
    private val mutex=Mutex()
    private suspend fun call(s:CloudSession,cloud:String?=null,share:Boolean?=null):JSONObject {
        val headers=ProfileSocialIdentityRP1.headers(s,url,key)
        val body=JSONObject().put("p_cloud",cloud ?: JSONObject.NULL).put("p_share",share ?: JSONObject.NULL).toString()
        val raw=Http.text("$url/rest/v1/rpc/brunio_watched_rp93","POST",headers,body,timeoutMs=5000)
        return withContext(Dispatchers.Default) {JSONObject(raw)}
    }
    suspend fun friends(s:CloudSession,cloud:String):List<SocialPerson> = mutex.withLock {
        require(cloud.matches(Regex("tmdb:(movie|series):[0-9]+")))
        val owner=ProfileSocialIdentityRP1.scope(s)
        val id="${owner.account}:${owner.profile}:$cloud"
        val now=SystemClock.elapsedRealtime()
        cache.get(id,now)?.let {return@withLock it}
        var success=true
        val people=try {
            val result=call(s,cloud)
            withContext(Dispatchers.Default) {
                val rows=result.optJSONArray("friends")
                if(rows==null) emptyList() else (0 until minOf(rows.length(),50)).map {i ->
                    val x=rows.getJSONObject(i)
                    SocialPerson(x.getString("id"),x.optString("nickname").take(24),
                        x.optString("avatar").takeUnless {it.isBlank() || it=="null"})
                }.distinctBy {it.id}
            }
        } catch(e:CancellationException) {throw e} catch(_:Exception) {success=false;emptyList()}
        cache.put(id,people,SystemClock.elapsedRealtime(),success)
        people
    }
    suspend fun sharing(s:CloudSession):Boolean = mutex.withLock {call(s).getBoolean("sharing")}
    suspend fun saveSharing(s:CloudSession,value:Boolean):Boolean = mutex.withLock {
        val saved=call(s,share=value).getBoolean("sharing")
        cache.clear()
        saved
    }
}
