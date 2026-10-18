package com.miflix.native2.data

import com.miflix.native2.model.CloudSession
import com.miflix.native2.model.Profile
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** Identity changes, never authentication: the bearer token still belongs to the parent account. */
internal object ProfileSocialIdentityRP1 {
    data class Scope(val account:String,val profile:String,val actor:String)
    private val scopes=ConcurrentHashMap<String,Scope>()
    private val ready=mutableSetOf<Scope>()
    private val mutex=Mutex()
    fun actor(account:String,profile:String,primary:Boolean)=if(primary) account else
        UUID.nameUUIDFromBytes("brunio-rp1:$account:$profile".toByteArray(Charsets.UTF_8)).toString()
    fun session(account:CloudSession,profile:Profile):CloudSession {
        require(!profile.isKids && profile.id!="kids") {"Social features are unavailable in Kids"}
        val id=actor(account.userId,profile.id,profile.primary)
        scopes[id]=Scope(account.userId,profile.id,id)
        return account.copy(userId=id)
    }
    fun scope(s:CloudSession)=scopes[s.userId] ?: error("Select a profile before opening social features")
    suspend fun headers(s:CloudSession,url:String,key:String):Map<String,String> {
        val identity=scope(s)
        val headers=mapOf("apikey" to key,"Authorization" to "Bearer ${s.accessToken}","Content-Type" to "application/json","x-brunio-profile" to identity.profile)
        mutex.withLock {
            if(identity !in ready) {
                val result=JSONObject(Http.text("$url/rest/v1/rpc/brunio_rp1_ensure","POST",headers,JSONObject().put("p_profile",identity.profile).toString()))
                check(result.getString("actor")==identity.actor) {"Social profile identity mismatch"}
                ready.add(identity)
            }
        }
        return headers
    }
}
