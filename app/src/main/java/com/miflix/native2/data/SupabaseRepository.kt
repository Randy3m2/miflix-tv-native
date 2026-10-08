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
            Profile(id, p.optString("name", "Profile"), p.optString("avatarValue"), p.optBoolean("primary", false))
        }
        return profiles to setup
    }

    suspend fun upsertProfiles(session: CloudSession, profiles: List<Profile>) {
        val existing = readRow(session, "__account__")?.optJSONObject("state") ?: JSONObject()
        val arr = JSONArray()
        profiles.forEach { p ->
            arr.put(JSONObject().put("id", p.id).put("name", p.name).put("avatarValue", p.avatarValue).put("primary", p.primary))
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
        val row = readRow(session, profileId) ?: return emptySet<String>() to mutableMapOf()
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
        return fav to progress
    }

    suspend fun upsertProfile(session: CloudSession, profileId: String, favorites: Set<String>, progress: Map<String, PlaybackProgress>) {
        val po = JSONObject()
        progress.forEach { (k, p) ->
            po.put(k, JSONObject().put("percent", p.percent).put("position", p.position / 1000.0).put("duration", p.duration / 1000.0)
                .put("updatedAt", p.updatedAt).put("season", p.season).put("episode", p.episode))
        }
        val state = JSONObject().put("favorites", JSONArray(favorites.toList())).put("progress", po).put("updatedAt", System.currentTimeMillis())
        val body = JSONObject().put("user_id", session.userId).put("profile_id", profileId).put("state", state).put("updated_at", isoNow()).toString()
        Http.text("$url/rest/v1/miflix_user_state?on_conflict=user_id,profile_id", "POST", headers(session) + ("Prefer" to "resolution=merge-duplicates,return=minimal"), body)
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
            "$url/rest/v1/miflix_watch_parties?on_conflict=room_code",
            "POST",
            headers(session) + ("Prefer" to "resolution=merge-duplicates,return=minimal"),
            body
        )
    }

    suspend fun updateWatchParty(session: CloudSession, party: WatchParty) {
        val body = JSONObject().put("state", partyStateJson(party)).put("updated_at", isoNow()).put("expires_at", isoFromMillis(System.currentTimeMillis() + 8 * 60 * 60 * 1000L)).toString()
        val code = URLEncoder.encode(party.roomCode, "UTF-8")
        Http.text(
            "$url/rest/v1/miflix_watch_parties?room_code=eq.$code",
            "PATCH",
            headers(session) + ("Prefer" to "return=minimal"),
            body
        )
    }

    suspend fun readWatchParty(session: CloudSession, roomCode: String): WatchParty? {
        val code = URLEncoder.encode(roomCode.uppercase(Locale.US), "UTF-8")
        val text = Http.text(
            "$url/rest/v1/miflix_watch_parties?room_code=eq.$code&select=room_code,host_user_id,state,updated_at,expires_at",
            headers = headers(session)
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
            liveType = state.optString("liveType","tv")
        )
    }

    suspend fun deleteWatchParty(session: CloudSession, roomCode: String) {
        val code = URLEncoder.encode(roomCode.uppercase(Locale.US), "UTF-8")
        Http.text("$url/rest/v1/miflix_watch_parties?room_code=eq.$code", "DELETE", headers(session) + ("Prefer" to "return=minimal"))
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
