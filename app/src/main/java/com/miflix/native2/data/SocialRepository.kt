package com.miflix.native2.data

import com.miflix.native2.model.CloudSession
import org.json.JSONObject
import org.json.JSONArray
import java.net.URLEncoder

data class SocialPerson(val id: String, val nickname: String)
data class PartyEvent(val id: Long, val userId: String, val kind: String, val body: String)
data class Friendship(val sender: String, val receiver: String, val status: String)

class SocialRepository(private val url: String, private val key: String) {
    private fun headers(s: CloudSession) = mapOf("apikey" to key, "Authorization" to "Bearer ${s.accessToken}", "Content-Type" to "application/json")
    private fun enc(s: String) = URLEncoder.encode(s,"UTF-8")
    private suspend fun rows(s: CloudSession, path: String) = JSONArray(Http.text("$url/rest/v1/$path", headers=headers(s)))
    private suspend fun write(s: CloudSession, path: String, body: JSONObject, method: String = "POST") = Http.text("$url/rest/v1/$path",method,headers(s)+( "Prefer" to "return=minimal"),body.toString())
    suspend fun pause(s: CloudSession, room: String, cloud: String, season: Int, episode: Int) {
        write(s,"rpc/miflix_request_pause",JSONObject().put("code",room).put("media",cloud).put("s",season).put("e",episode))
    }
    suspend fun takePause(s: CloudSession, room: String): Boolean = Http.text("$url/rest/v1/rpc/miflix_take_pause","POST",headers(s),JSONObject().put("code",room).toString()).trim() == "true"
    suspend fun nickname(s: CloudSession): String = rows(s,"miflix_social_profiles?user_id=eq.${s.userId}&select=nickname").optJSONObject(0)?.optString("nickname").orEmpty()
    suspend fun saveNickname(s: CloudSession, nick: String) {
        require(nick.matches(Regex("[a-z0-9_]{3,24}"))) { "Use 3–24 letters, numbers or underscores" }
        Http.text("$url/rest/v1/miflix_social_profiles?on_conflict=user_id","POST",headers(s)+( "Prefer" to "resolution=merge-duplicates,return=minimal"),JSONObject().put("user_id",s.userId).put("nickname",nick).toString())
    }
    suspend fun find(s: CloudSession, nick: String): SocialPerson? {
        val p=rows(s,"miflix_social_profiles?nickname=eq.${enc(nick.lowercase())}&select=user_id,nickname").optJSONObject(0) ?: return null
        return SocialPerson(p.getString("user_id"),p.getString("nickname"))
    }
    suspend fun people(s: CloudSession, ids: List<String>): List<SocialPerson> {
        if(ids.isEmpty())return emptyList()
        val a=rows(s,"miflix_social_profiles?user_id=in.(${ids.joinToString(",")})&select=user_id,nickname")
        return (0 until a.length()).map { a.getJSONObject(it).let { p -> SocialPerson(p.getString("user_id"),p.getString("nickname")) } }
    }
    suspend fun join(s: CloudSession, room: String) { write(s,"rpc/miflix_join_room",JSONObject().put("code",room)) }
    suspend fun leave(s: CloudSession, room: String) { Http.text("$url/rest/v1/miflix_party_members?room_code=eq.${enc(room)}&user_id=eq.${s.userId}","DELETE",headers(s)) }
    suspend fun members(s: CloudSession, room: String): List<SocialPerson> {
        val a=rows(s,"miflix_party_members?room_code=eq.${enc(room)}&select=user_id")
        val ids=(0 until a.length()).map { a.getJSONObject(it).getString("user_id") }
        val known=people(s,ids).associateBy { it.id }
        return ids.map { known[it] ?: SocialPerson(it,"Guest") }
    }
    suspend fun events(s: CloudSession, room: String, after: Long): List<PartyEvent> {
        val order=if(after<0) "desc" else "asc"
        val filter=if(after<0) "" else "&id=gt.$after"
        val a=rows(s,"miflix_party_events?room_code=eq.${enc(room)}$filter&order=id.$order&limit=60")
        return (0 until a.length()).map { a.getJSONObject(it).let { x -> PartyEvent(x.getLong("id"),x.getString("user_id"),x.getString("kind"),x.getString("body")) } }.sortedBy { it.id }
    }
    suspend fun send(s: CloudSession, room: String, kind: String, text: String) {
        write(s,"miflix_party_events",JSONObject().put("room_code",room).put("user_id",s.userId).put("kind",kind).put("body",text.trim().take(400)))
    }
    suspend fun friends(s: CloudSession): List<Friendship> {
        val a=rows(s,"miflix_friendships?select=sender_id,receiver_id,status")
        return (0 until a.length()).map { a.getJSONObject(it).let { f -> Friendship(f.getString("sender_id"),f.getString("receiver_id"),f.getString("status")) } }
    }
    suspend fun requestFriend(s: CloudSession, id: String) { write(s,"miflix_friendships",JSONObject().put("sender_id",s.userId).put("receiver_id",id)) }
    suspend fun accept(s: CloudSession, sender: String) { write(s,"miflix_friendships?sender_id=eq.$sender&receiver_id=eq.${s.userId}",JSONObject().put("status","accepted"),"PATCH") }
}
