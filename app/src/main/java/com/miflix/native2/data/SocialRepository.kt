package com.miflix.native2.data

import com.miflix.native2.model.CloudSession
import org.json.JSONObject
import org.json.JSONArray
import java.net.URLEncoder
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class SocialPerson(val id: String, val nickname: String, val avatar: String? = null)
data class PartyEvent(val id: Long, val userId: String, val kind: String, val body: String, val createdAt: Long = 0L)
data class FriendActivity(val id: String, val nickname: String, val title: String, val season: Int, val episode: Int, val room: String?, val online: Boolean, val avatar: String? = null, val cloudId: String = "")
data class FriendRecommendation(val owner: String, val nickname: String, val cloud: String, val title: String, val poster: String?)

data class PartyInvitation(val room: String,val host: String,val title: String)

data class AccessRequest(val room: String, val id: String, val nickname: String)
data class Friendship(val sender: String, val receiver: String, val status: String)

data class QueueEntry(val id: String, val cloud: String, val title: String, val season: Int, val episode: Int)
data class CalendarEvent(val id: String, val owner: String, val title: String, val kind: String, val starts: String, val cloud: String?, val season: Int, val episode: Int, val guests: List<String>, val channelId: String="", val channelType: String="tv")

data class SharedMoment(val id: String,val sender: String,val nickname: String,val cloud: String,val title: String,
    val season: Int,val episode: Int,val startMs: Long,val endMs: Long)

class SocialRepository(private val url: String, private val key: String) {
    private val presenceMutex=Mutex()
    private val avatarMutex=Mutex()
    private val avatarGate=SocialPublishGate(240_000L,changedOnly=true)
    private suspend fun headers(s: CloudSession) = ProfileSocialIdentityRP1.headers(s,url,key)
    private fun enc(s: String) = URLEncoder.encode(s,"UTF-8")
    private suspend fun rows(s: CloudSession, path: String) = JSONArray(Http.text("$url/rest/v1/$path", headers=headers(s)))
    private suspend fun write(s: CloudSession, path: String, body: JSONObject, method: String = "POST") = Http.text("$url/rest/v1/$path",method,headers(s)+( "Prefer" to "return=minimal"),body.toString())
    suspend fun calendar(s: CloudSession): List<CalendarEvent> {
        val a=rows(s,"rp1_miflix_calendar_events?select=*&order=starts_at.asc&limit=500")
        return (0 until a.length()).map { i -> val x=a.getJSONObject(i);val guests=x.optJSONArray("guests") ?: JSONArray()
            CalendarEvent(x.getString("id"),x.getString("owner_id"),x.getString("title"),x.getString("kind"),x.getString("starts_at"),x.optString("cloud_id").takeUnless { it.isBlank() || it=="null" },x.optInt("season"),x.optInt("episode"),(0 until guests.length()).map { guests.getString(it) },x.optString("channel_id"),x.optString("channel_type","tv")) }
    }
    suspend fun createCalendar(s: CloudSession,title: String,kind: String,starts: String,cloud: String?,season: Int,episode: Int,guests: List<String>,channel: LiveChannel?=null) {
        write(s,"rp1_miflix_calendar_events",JSONObject().put("owner_id",s.userId).put("title",title).put("kind",kind).put("starts_at",starts).put("cloud_id",cloud ?: JSONObject.NULL).put("season",season).put("episode",episode).put("guests",JSONArray(guests)).put("channel_id",channel?.id.orEmpty()).put("channel_type",channel?.type ?: "tv"))
    }
    suspend fun deleteCalendar(s: CloudSession,id: String) { Http.text("$url/rest/v1/rp1_miflix_calendar_events?id=eq.$id","DELETE",headers(s)) }
    suspend fun publishAvatar(s: CloudSession,avatar: String) = avatarMutex.withLock {
        val signature=s.userId+":"+avatar
        val now=android.os.SystemClock.elapsedRealtime()
        if(!avatarGate.due(s.userId,signature,now)) return@withLock
        write(s,"rp1_miflix_social_profiles?user_id=eq.${s.userId}",JSONObject().put("avatar_value",avatar),"PATCH")
        avatarGate.published(s.userId,signature,android.os.SystemClock.elapsedRealtime())
    }
    /** Event RPC is deliberately independent of all watch-party control/heartbeat RPCs. */
    internal suspend fun presenceEvent(s: CloudSession, body: JSONObject) = presenceMutex.withLock {
        write(s,"rpc/rp1_brunio_presence_event",body)
    }
    suspend fun receivedMoments(s: CloudSession): List<SharedMoment> {
        val a=JSONArray(Http.text("$url/rest/v1/rpc/rp1_brunio_received_moments","POST",headers(s),"{}"))
        return (0 until a.length()).map { a.getJSONObject(it).let { x ->
            SharedMoment(x.getString("id"),x.getString("sender_id"),x.getString("nickname"),x.getString("cloud_id"),
                x.getString("title"),x.getInt("season"),x.getInt("episode"),x.getLong("start_ms"),x.getLong("end_ms")) } }
    }
    suspend fun shareMoment(s: CloudSession,recipient: String,reqId: String,cloud: String,title: String,
        season: Int,episode: Int,start: Long,end: Long) {
        require(start>=0 && end>start && end<=86_400_000L)
        val id=java.util.UUID.nameUUIDFromBytes("${s.userId}:$recipient:$reqId:$start:$end".toByteArray()).toString()
        val body=JSONObject().put("id",id).put("sender_id",s.userId).put("recipient_id",recipient)
            .put("cloud_id",cloud).put("title",title.take(200)).put("season",season).put("episode",episode)
            .put("start_ms",start).put("end_ms",end)
        Http.text("$url/rest/v1/rp1_brunio_shared_moments?on_conflict=id","POST",
            headers(s)+("Prefer" to "resolution=ignore-duplicates,return=minimal"),body.toString())
    }
    suspend fun deleteMoment(s: CloudSession,id: String) {
        require(runCatching {java.util.UUID.fromString(id)}.isSuccess)
        Http.text("$url/rest/v1/rp1_brunio_shared_moments?id=eq.$id","DELETE",headers(s))
    }
    suspend fun activity(s: CloudSession): List<FriendActivity> {
        val a=JSONArray(Http.text("$url/rest/v1/rpc/rp1_miflix_friend_activity_v30","POST",headers(s),"{}"))
        return (0 until a.length()).map { a.getJSONObject(it).let { x -> FriendActivity(x.getString("user_id"),x.getString("nickname"),x.optString("title"),x.optInt("season"),x.optInt("episode"),if(x.isNull("room_code")) null else x.optString("room_code"),x.optBoolean("online"),x.optString("avatar_value").takeUnless { it.isBlank() || it=="null" },x.optString("cloud_id").takeUnless { it=="null" }.orEmpty()) } }
    }
    suspend fun recommendations(s: CloudSession): List<FriendRecommendation> {
        val a=JSONArray(Http.text("$url/rest/v1/rpc/rp1_miflix_friend_recommendations","POST",headers(s),"{}"))
        return (0 until a.length()).map { i -> val x=a.getJSONObject(i)
            FriendRecommendation(x.getString("owner_id"),x.getString("nickname"),x.getString("cloud_id"),x.getString("title"),x.optString("poster").takeUnless { it.isBlank() || it=="null" }) }
    }
    suspend fun recommend(s: CloudSession, item: com.miflix.native2.model.MediaSummary) {
        Http.text("$url/rest/v1/rp1_miflix_recommendations?on_conflict=owner_id,cloud_id","POST",
            headers(s)+("Prefer" to "resolution=merge-duplicates,return=minimal"),
            JSONObject().put("owner_id",s.userId).put("cloud_id",item.cloudId).put("title",item.title)
                .put("poster",item.poster ?: JSONObject.NULL).toString())
    }
    suspend fun removeRecommendation(s: CloudSession, cloud: String) {
        Http.text("$url/rest/v1/rp1_miflix_recommendations?owner_id=eq.${s.userId}&cloud_id=eq.${enc(cloud)}","DELETE",headers(s))
    }
    suspend fun inviteParty(s: CloudSession,room: String,friend: String) {
        write(s,"rpc/rp1_miflix_invite_party",JSONObject().put("code",room).put("friend",friend))
    }
    suspend fun partyInvitations(s: CloudSession): List<PartyInvitation> {
        val a=rows(s,"rp1_miflix_party_invites?recipient_id=eq.${s.userId}&select=room_code,host_name,title&order=created_at.desc&limit=20")
        return (0 until a.length()).map { a.getJSONObject(it).let { x -> PartyInvitation(x.getString("room_code"),x.optString("host_name"),x.optString("title")) } }
    }
    suspend fun previewParty(s: CloudSession,room: String): JSONObject = JSONObject(Http.text("$url/rest/v1/rpc/rp1_miflix_party_preview","POST",headers(s),JSONObject().put("code",room).toString()))
    suspend fun requestAccess(s: CloudSession, room: String): String = Http.text("$url/rest/v1/rpc/rp1_miflix_request_access","POST",headers(s),JSONObject().put("code",room).toString()).trim().trim('"')
    suspend fun requests(s: CloudSession): List<AccessRequest> {
        val a=JSONArray(Http.text("$url/rest/v1/rpc/rp1_miflix_access_requests","POST",headers(s),"{}"))
        return (0 until a.length()).map { a.getJSONObject(it).let { x -> AccessRequest(x.getString("room_code"),x.getString("user_id"),x.getString("nickname")) } }
    }
    suspend fun decide(s: CloudSession, request: AccessRequest, approve: Boolean) {
        write(s,"rpc/rp1_miflix_decide_access",JSONObject().put("code",request.room).put("guest",request.id).put("approve",approve))
    }
    suspend fun playback(s: CloudSession, room: String, cloud: String, season: Int, episode: Int, playing: Boolean) {
        write(s,"rpc/rp1_miflix_request_playback",JSONObject().put("code",room).put("media",cloud).put("s",season).put("e",episode).put("playing",playing))
    }
    suspend fun takePlayback(s: CloudSession, room: String): Boolean? = when(Http.text("$url/rest/v1/rpc/rp1_miflix_take_playback","POST",headers(s),JSONObject().put("code",room).toString()).trim()) {
        "true" -> true
        "false" -> false
        else -> null
    }
    suspend fun pause(s: CloudSession, room: String, cloud: String, season: Int, episode: Int) {
        write(s,"rpc/rp1_miflix_request_pause",JSONObject().put("code",room).put("media",cloud).put("s",season).put("e",episode))
    }
    suspend fun takePause(s: CloudSession, room: String): Boolean = Http.text("$url/rest/v1/rpc/rp1_miflix_take_pause","POST",headers(s),JSONObject().put("code",room).toString()).trim() == "true"
    suspend fun nickname(s: CloudSession): String = rows(s,"rp1_miflix_social_profiles?user_id=eq.${s.userId}&select=nickname").optJSONObject(0)?.optString("nickname").orEmpty()
    suspend fun saveNickname(s: CloudSession, nick: String) {
        require(nick.matches(Regex("[a-z0-9_]{3,24}"))) { "Use 3–24 letters, numbers or underscores" }
        Http.text("$url/rest/v1/rp1_miflix_social_profiles?on_conflict=user_id","POST",headers(s)+( "Prefer" to "resolution=merge-duplicates,return=minimal"),JSONObject().put("user_id",s.userId).put("nickname",nick).toString())
    }
    suspend fun find(s: CloudSession, nick: String): SocialPerson? {
        val p=rows(s,"rp1_miflix_social_profiles?nickname=eq.${enc(nick.lowercase())}&select=user_id,nickname,avatar_value").optJSONObject(0) ?: return null
        return SocialPerson(p.getString("user_id"),p.getString("nickname"),p.optString("avatar_value").takeUnless { it.isBlank() || it=="null" })
    }
    suspend fun people(s: CloudSession, ids: List<String>): List<SocialPerson> {
        if(ids.isEmpty())return emptyList()
        val a=rows(s,"rp1_miflix_social_profiles?user_id=in.(${ids.joinToString(",")})&select=user_id,nickname,avatar_value")
        return (0 until a.length()).map { a.getJSONObject(it).let { p -> SocialPerson(p.getString("user_id"),p.getString("nickname"),p.optString("avatar_value").takeUnless { it.isBlank() || it=="null" }) } }
    }
    suspend fun join(s: CloudSession, room: String) { write(s,"rpc/rp1_miflix_join_room",JSONObject().put("code",room)) }
    suspend fun leave(s: CloudSession, room: String) { Http.text("$url/rest/v1/rp1_miflix_party_members?room_code=eq.${enc(room)}&user_id=eq.${s.userId}","DELETE",headers(s)) }
    suspend fun members(s: CloudSession, room: String): List<SocialPerson> {
        val a=rows(s,"rp1_miflix_party_members?room_code=eq.${enc(room)}&select=user_id")
        val ids=(0 until a.length()).map { a.getJSONObject(it).getString("user_id") }
        val known=people(s,ids).associateBy { it.id }
        return ids.map { known[it] ?: SocialPerson(it,"Guest") }
    }
    suspend fun events(s: CloudSession, room: String, after: Long): List<PartyEvent> {
        val order=if(after<0) "desc" else "asc"
        val filter=if(after<0) "" else "&id=gt.$after"
        val a=rows(s,"rp1_miflix_party_events?room_code=eq.${enc(room)}$filter&order=id.$order&limit=60")
        return (0 until a.length()).map { a.getJSONObject(it).let { x -> PartyEvent(x.getLong("id"),x.getString("user_id"),x.getString("kind"),x.getString("body"),
            runCatching {java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss",java.util.Locale.US).apply {timeZone=java.util.TimeZone.getTimeZone("UTC")}.parse(x.optString("created_at").take(19))?.time ?: 0L}.getOrDefault(0L)) } }.sortedBy { it.id }
    }
    suspend fun send(s: CloudSession, room: String, kind: String, text: String) {
        write(s,"rp1_miflix_party_events",JSONObject().put("room_code",room).put("user_id",s.userId).put("kind",kind).put("body",text.trim().take(400)))
    }
    suspend fun friends(s: CloudSession): List<Friendship> {
        val a=rows(s,"rp1_miflix_friendships?select=sender_id,receiver_id,status")
        return (0 until a.length()).map { a.getJSONObject(it).let { f -> Friendship(f.getString("sender_id"),f.getString("receiver_id"),f.getString("status")) } }
    }
    suspend fun requestFriend(s: CloudSession, id: String) { write(s,"rp1_miflix_friendships",JSONObject().put("sender_id",s.userId).put("receiver_id",id)) }
    suspend fun accept(s: CloudSession, sender: String) { write(s,"rp1_miflix_friendships?sender_id=eq.$sender&receiver_id=eq.${s.userId}",JSONObject().put("status","accepted"),"PATCH") }
}
