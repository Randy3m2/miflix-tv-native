package com.miflix.native2.data

import com.miflix.native2.model.CloudSession
import com.miflix.native2.model.PlaybackProgress
import com.miflix.native2.model.PrivateSetup
import com.miflix.native2.model.Profile
import com.miflix.native2.model.WatchParty
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class SupabaseRepository(private val url: String, private val key: String) {
    private val favoriteBaselines = mutableMapOf<String, Set<String>>()
    private val syncMutex = kotlinx.coroutines.sync.Mutex()
    private fun headers(session: CloudSession? = null) = buildMap {
        put("apikey", key)
        put("Content-Type", "application/json")
        if (session != null) put("Authorization", "Bearer ${session.accessToken}")
    }

    suspend fun login(email: String, password: String): CloudSession {
        val body = JSONObject().put("email", email).put("password", password).toString()
        val j = JSONObject(Http.text("$url/auth/v1/token?grant_type=password", "POST", headers(), body))
        return CloudSession(
            accessToken = j.getString("access_token"),
            refreshToken = j.optString("refresh_token"),
            userId = j.getJSONObject("user").getString("id"),
            email = j.getJSONObject("user").optString("email", email)
        )
    }

    suspend fun refresh(refreshToken: String): CloudSession {
        val body = JSONObject().put("refresh_token", refreshToken).toString()
        val j = JSONObject(Http.text("$url/auth/v1/token?grant_type=refresh_token", "POST", headers(), body))
        return CloudSession(
            accessToken = j.getString("access_token"), refreshToken = j.optString("refresh_token", refreshToken),
            userId = j.getJSONObject("user").getString("id"), email = j.getJSONObject("user").optString("email")
        )
    }

    suspend fun account(session: CloudSession): Pair<List<Profile>, PrivateSetup> {
        val row = readRow(session, "__account__") ?: return emptyList<Profile>() to PrivateSetup()
        val state = row.optJSONObject("state") ?: JSONObject()
        val setupObj = state.optJSONObject("privateSetup") ?: JSONObject()
        val addonManifests = setupObj.optJSONArray("addonManifests")?.let { a ->
            (0 until a.length()).map { a.optString(it) }.filter { it.isNotBlank() }
        } ?: emptyList()
        val legacyTorrentio = setupObj.optString("torrentioManifest")
        val setup = PrivateSetup(
            tmdbToken = setupObj.optString("tmdbToken"),
            torrentioManifest = legacyTorrentio,
            addonManifests = (addonManifests + legacyTorrentio).filter { it.isNotBlank() }.distinct()
        )
        val pa = state.optJSONArray("profiles") ?: JSONArray()
        val profiles = (0 until pa.length()).mapNotNull { i ->
            val p = pa.optJSONObject(i) ?: return@mapNotNull null
            val id = p.optString("id").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            Profile(id, p.optString("name", "Profile"), p.optString("avatarValue"), p.optBoolean("primary", false),p.optBoolean("isKids") || id==KidsContentPolicy.PROFILE_ID)
        }
        return profiles to setup
    }

    suspend fun deleteProfile(session: CloudSession,profileId: String): List<Profile> {
        val body=JSONObject().put("target_profile",profileId).toString()
        val rows=JSONArray(Http.text("$url/rest/v1/rpc/miflix_delete_profile","POST",headers(session),body))
        return (0 until rows.length()).map { i -> rows.getJSONObject(i).let { p ->
            Profile(p.getString("id"),p.getString("name"),p.optString("avatarValue").takeUnless { it.isBlank() || it=="null" },p.optBoolean("primary"),p.optBoolean("isKids") || p.optString("id")==KidsContentPolicy.PROFILE_ID)
        } }
    }

    suspend fun upsertProfiles(session: CloudSession, profiles: List<Profile>) {
        val existing = readRow(session, "__account__")?.optJSONObject("state") ?: JSONObject()
        val arr = JSONArray()
        profiles.forEach { p ->
            arr.put(JSONObject().put("id", p.id).put("name", p.name).put("avatarValue", p.avatarValue).put("primary", p.primary).put("isKids",p.isKids))
        }
        existing.put("profiles", arr)
        if (!existing.has("privateSetup")) existing.put("privateSetup", JSONObject())
        existing.put("updatedAt", System.currentTimeMillis())
        upsertAccountState(session, existing)
    }

    suspend fun upsertPrivateSetup(session: CloudSession, setup: PrivateSetup) {
        val existing = readRow(session, "__account__")?.optJSONObject("state") ?: JSONObject()
        val manifests = (setup.addonManifests + setup.torrentioManifest).filter { it.isNotBlank() }.distinct()
        existing.put(
            "privateSetup",
            JSONObject()
                .put("tmdbToken", setup.tmdbToken)
                .put("torrentioManifest", setup.torrentioManifest.ifBlank { manifests.firstOrNull().orEmpty() })
                .put("addonManifests", JSONArray(manifests))
        )
        if (!existing.has("profiles")) existing.put("profiles", JSONArray())
        existing.put("updatedAt", System.currentTimeMillis())
        upsertAccountState(session, existing)
    }

    private suspend fun upsertAccountState(session: CloudSession, state: JSONObject) {
        val body = JSONObject()
            .put("user_id", session.userId)
            .put("profile_id", "__account__")
            .put("state", state)
            .put("updated_at", isoNow())
            .toString()
        Http.text(
            "$url/rest/v1/miflix_user_state?on_conflict=user_id,profile_id",
            "POST",
            headers(session) + ("Prefer" to "resolution=merge-duplicates,return=minimal"),
            body
        )
    }

    suspend fun profileState(session: CloudSession, profileId: String): Pair<Set<String>, MutableMap<String, PlaybackProgress>> {
        val row = readRow(session, profileId) ?: run { favoriteBaselines["${session.userId}:$profileId"] = emptySet(); return emptySet<String>() to mutableMapOf() }
        val state = row.optJSONObject("state") ?: JSONObject()
        val fav = state.optJSONArray("favorites")?.let { a -> (0 until a.length()).map { a.optString(it) }.filter { it.isNotBlank() }.toSet() } ?: emptySet()
        val progress = mutableMapOf<String, PlaybackProgress>()
        val po = state.optJSONObject("progress") ?: JSONObject()
        val keys = po.keys()
        while (keys.hasNext()) {
            val k = keys.next(); val p = po.optJSONObject(k) ?: continue
            progress[k] = PlaybackProgress(
                p.optDouble("percent", 0.0), p.optLong("position", 0L).let { if (it < 1_000_000) it * 1000 else it },
                p.optLong("duration", 0L).let { if (it < 1_000_000) it * 1000 else it }, p.optLong("updatedAt", 0L), p.optInt("season", 0), p.optInt("episode", 0)
            )
        }
        favoriteBaselines["${session.userId}:$profileId"] = fav
        return fav to progress
    }

    suspend fun upsertProfile(session: CloudSession, profileId: String, favorites: Set<String>, progress: Map<String, PlaybackProgress>): Set<String> {
        syncMutex.lock()
        try {
        val owner = "${session.userId}:$profileId"
        val baseline = favoriteBaselines[owner] ?: profileState(session, profileId).first
        val po = JSONObject()
        progress.forEach { (k, p) ->
            po.put(k, JSONObject().put("percent", p.percent).put("position", p.position / 1000.0).put("duration", p.duration / 1000.0)
                .put("updatedAt", p.updatedAt).put("season", p.season).put("episode", p.episode))
        }
        val body = JSONObject().put("p_profile", profileId)
            .put("p_adds", JSONArray((favorites - baseline).toList()))
            .put("p_removes", JSONArray((baseline - favorites).toList()))
            .put("p_progress", po).toString()
        val result = JSONObject(Http.text("$url/rest/v1/rpc/brunio_sync_profile", "POST", headers(session), body))
        val array = result.optJSONArray("favorites") ?: JSONArray()
        val merged = (0 until array.length()).map { array.getString(it) }.toSet()
        favoriteBaselines[owner] = merged
        return merged
        } finally { syncMutex.unlock() }
    }

    suspend fun createWatchParty(session: CloudSession, party: WatchParty) {
        val body = JSONObject()
            .put("room_code", party.roomCode)
            .put("host_user_id", session.userId)
            .put("state", partyStateJson(party))
            .put("updated_at", isoNow())
            .put("expires_at", isoFromMillis(System.currentTimeMillis() + 8 * 60 * 60 * 1000L))
            .toString()
        Http.text(
            "$url/rest/v1/rp1_miflix_watch_parties?on_conflict=room_code",
            "POST",
            ProfileSocialIdentityRP1.headers(session,url,key) + ("Prefer" to "resolution=merge-duplicates,return=minimal"),
            body
        )
    }

    suspend fun updateWatchParty(session: CloudSession, party: WatchParty) {
        val body = JSONObject().put("state", partyStateJson(party)).put("updated_at", isoNow()).put("expires_at", isoFromMillis(System.currentTimeMillis() + 8 * 60 * 60 * 1000L)).toString()
        val code = URLEncoder.encode(party.roomCode, "UTF-8")
        Http.text(
            "$url/rest/v1/rp1_miflix_watch_parties?room_code=eq.$code",
            "PATCH",
            ProfileSocialIdentityRP1.headers(session,url,key) + ("Prefer" to "return=minimal"),
            body
        )
    }

    suspend fun readWatchParty(session: CloudSession, roomCode: String): WatchParty? {
        val code = URLEncoder.encode(roomCode.uppercase(Locale.US), "UTF-8")
        val text = Http.text(
            "$url/rest/v1/rp1_miflix_watch_parties?room_code=eq.$code&select=room_code,host_user_id,state,updated_at,expires_at",
            headers = ProfileSocialIdentityRP1.headers(session,url,key)
        )
        val a = JSONArray(text)
        val row = a.optJSONObject(0) ?: return null
        val state = row.optJSONObject("state") ?: return null
        return WatchParty(
            roomCode = row.optString("room_code"),
            hostUserId = row.optString("host_user_id"),
            cloudId = state.optString("cloudId"),
            season = state.optInt("season", 0),
            episode = state.optInt("episode", 0),
            positionMs = state.optLong("positionMs", 0L),
            playing = state.optBoolean("playing", false),
            updatedAt = state.optLong("updatedAt", 0L),
            liveChannelId = state.optString("liveChannelId"),
            liveProvider = state.optString("liveProvider"),
            liveTitle = state.optString("liveTitle"),
            liveType = state.optString("liveType","tv"),
            title=state.optString("title"),overview=state.optString("overview"),
            poster=state.optString("poster").takeIf {it.startsWith("https://image.tmdb.org/")},
            logo=state.optString("logo").takeIf {it.startsWith("https://image.tmdb.org/")},
            sourceName=state.optString("sourceName"),sourceFilename=state.optString("sourceFilename"),
            sourceSize=state.optLong("sourceSize"),sourceKey=state.optString("sourceKey"),
            shareStream=state.optBoolean("shareStream"),hostOnlyControls=state.optBoolean("hostOnlyControls")
        )
    }

    suspend fun deleteWatchParty(session: CloudSession, roomCode: String) {
        val code = URLEncoder.encode(roomCode.uppercase(Locale.US), "UTF-8")
        Http.text("$url/rest/v1/rp1_miflix_watch_parties?room_code=eq.$code", "DELETE", ProfileSocialIdentityRP1.headers(session,url,key) + ("Prefer" to "return=minimal"))
    }

    private fun partyStateJson(p: WatchParty) = JSONObject()
        .put("cloudId", p.cloudId)
        .put("liveChannelId", p.liveChannelId)
        .put("liveProvider", p.liveProvider)
        .put("liveTitle", p.liveTitle)
        .put("liveType", p.liveType)
        .put("season", p.season)
        .put("episode", p.episode)
        .put("positionMs", p.positionMs)
        .put("playing", p.playing)
        .put("updatedAt", p.updatedAt)
        .put("title",p.title).put("overview",p.overview).put("poster",p.poster ?: JSONObject.NULL).put("logo",p.logo ?: JSONObject.NULL)
        .put("sourceName",p.sourceName).put("sourceFilename",p.sourceFilename).put("sourceSize",p.sourceSize).put("sourceKey",p.sourceKey)
        .put("shareStream",p.shareStream).put("hostOnlyControls",p.hostOnlyControls)

    suspend fun sharePartySource(session: CloudSession,p: WatchParty,stream: com.miflix.native2.model.StreamChoice?) {
        val code=URLEncoder.encode(p.roomCode,"UTF-8")
        if(stream==null) { Http.text("$url/rest/v1/rp1_miflix_party_sources?room_code=eq.$code","DELETE",ProfileSocialIdentityRP1.headers(session,url,key));return }
        require(stream.url.startsWith("https://") || stream.url.startsWith("http://")) {"This source cannot be shared"}
        val requestHeaders=JSONObject();stream.requestHeaders.forEach {(k,v) -> requestHeaders.put(k,v)}
        val body=JSONObject().put("room_code",p.roomCode).put("cloud_id",p.cloudId).put("season",p.season).put("episode",p.episode)
            .put("source_key",p.sourceKey).put("url",stream.url).put("request_headers",requestHeaders)
            .put("expires_at",isoFromMillis(System.currentTimeMillis()+3*60*60*1000L))
        Http.text("$url/rest/v1/rp1_miflix_party_sources?on_conflict=room_code","POST",ProfileSocialIdentityRP1.headers(session,url,key)+("Prefer" to "resolution=merge-duplicates,return=minimal"),body.toString())
    }
    suspend fun readPartySource(session: CloudSession,p: WatchParty): com.miflix.native2.model.StreamChoice? {
        if(!p.shareStream) return null
        val code=URLEncoder.encode(p.roomCode,"UTF-8")
        val a=JSONArray(Http.text("$url/rest/v1/rp1_miflix_party_sources?room_code=eq.$code&select=*",headers=ProfileSocialIdentityRP1.headers(session,url,key)))
        val x=a.optJSONObject(0) ?: return null
        if(x.optString("cloud_id")!=p.cloudId || x.optInt("season")!=p.season || x.optInt("episode")!=p.episode || x.optString("source_key")!=p.sourceKey) return null
        val h=x.optJSONObject("request_headers") ?: JSONObject();val parsed=mutableMapOf<String,String>()
        h.keys().forEach {key -> parsed[key]=h.getString(key)}
        val url=x.optString("url").takeIf {it.startsWith("http://") || it.startsWith("https://")} ?: return null
        return com.miflix.native2.model.StreamChoice(p.sourceName.ifBlank {"Party source"},url,p.sourceName,filename=p.sourceFilename,videoSize=p.sourceSize,requestHeaders=parsed,addonName="Party")
    }

    private suspend fun readRow(session: CloudSession, profileId: String): JSONObject? {
        val pid = URLEncoder.encode(profileId, "UTF-8")
        val text = Http.text("$url/rest/v1/miflix_user_state?user_id=eq.${session.userId}&profile_id=eq.$pid&select=state,updated_at", headers = headers(session))
        val a = JSONArray(text)
        return if (a.length() > 0) a.optJSONObject(0) else null
    }

    private fun isoNow(): String = isoFromMillis(System.currentTimeMillis())

    private fun isoFromMillis(ms: Long): String = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }.format(Date(ms))
}
