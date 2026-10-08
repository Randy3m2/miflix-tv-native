package com.miflix.native2.data

import com.miflix.native2.model.CloudSession
import com.miflix.native2.model.PlaybackProgress
import com.miflix.native2.model.PrivateSetup
import com.miflix.native2.model.Profile
import com.miflix.native2.model.WatchPartyRoom
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
        val addonArray = setupObj.optJSONArray("addonManifests")
        val addons = if (addonArray != null) (0 until addonArray.length()).map { addonArray.optString(it) }.filter { it.isNotBlank() } else emptyList()
        val setup = PrivateSetup(
            tmdbToken = setupObj.optString("tmdbToken"),
            torrentioManifest = setupObj.optString("torrentioManifest"),
            addonManifests = addons
        )
        val pa = state.optJSONArray("profiles") ?: JSONArray()
        val profiles = (0 until pa.length()).mapNotNull { i ->
            val p = pa.optJSONObject(i) ?: return@mapNotNull null
            Profile(p.optString("id"), p.optString("name", "Profile"), p.optString("avatarValue"), p.optBoolean("primary", false))
        }.filter { it.id.isNotBlank() }
        return profiles to setup
    }

    suspend fun upsertPrivateSetup(session: CloudSession, setup: PrivateSetup) {
        val existing = accountState(session)
        existing.put(
            "privateSetup",
            JSONObject()
                .put("tmdbToken", setup.tmdbToken)
                .put("torrentioManifest", setup.torrentioManifest)
                .put("addonManifests", JSONArray(setup.allAddons()))
        )
        if (!existing.has("profiles")) existing.put("profiles", JSONArray())
        writeAccountState(session, existing)
    }

    suspend fun upsertProfiles(session: CloudSession, profiles: List<Profile>) {
        val existing = accountState(session)
        existing.put("profiles", JSONArray(profiles.map { p ->
            JSONObject().put("id", p.id).put("name", p.name).put("avatarValue", p.avatarValue ?: "").put("primary", p.primary)
        }))
        if (!existing.has("privateSetup")) existing.put("privateSetup", JSONObject())
        writeAccountState(session, existing)
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

    suspend fun createParty(session: CloudSession, room: WatchPartyRoom) {
        val expires = isoAt(System.currentTimeMillis() + 4 * 60 * 60 * 1000L)
        val body = partyJson(room, expires).toString()
        Http.text(
            "$url/rest/v1/miflix_watch_parties?on_conflict=room_code",
            "POST",
            headers(session) + ("Prefer" to "resolution=merge-duplicates,return=minimal"),
            body
        )
    }

    suspend fun updateParty(session: CloudSession, room: WatchPartyRoom) {
        val body = partyJson(room, isoAt(System.currentTimeMillis() + 4 * 60 * 60 * 1000L)).toString()
        Http.text(
            "$url/rest/v1/miflix_watch_parties?on_conflict=room_code",
            "POST",
            headers(session) + ("Prefer" to "resolution=merge-duplicates,return=minimal"),
            body
        )
    }

    suspend fun readParty(session: CloudSession, roomCode: String): WatchPartyRoom? {
        val code = URLEncoder.encode(roomCode.uppercase(Locale.US), "UTF-8")
        val raw = Http.text(
            "$url/rest/v1/miflix_watch_parties?room_code=eq.$code&select=room_code,host_user_id,cloud_id,media_type,season,episode,position_ms,is_playing,updated_at&limit=1",
            headers = headers(session)
        )
        val a = JSONArray(raw)
        if (a.length() == 0) return null
        val j = a.getJSONObject(0)
        return WatchPartyRoom(
            roomCode = j.optString("room_code"), hostUserId = j.optString("host_user_id"), cloudId = j.optString("cloud_id"),
            mediaType = j.optString("media_type"), season = j.optInt("season"), episode = j.optInt("episode"),
            positionMs = j.optLong("position_ms"), isPlaying = j.optBoolean("is_playing"), updatedAt = parseIso(j.optString("updated_at"))
        )
    }

    suspend fun deleteParty(session: CloudSession, roomCode: String) {
        val code = URLEncoder.encode(roomCode.uppercase(Locale.US), "UTF-8")
        Http.text("$url/rest/v1/miflix_watch_parties?room_code=eq.$code&host_user_id=eq.${session.userId}", "DELETE", headers(session) + ("Prefer" to "return=minimal"))
    }

    private fun partyJson(room: WatchPartyRoom, expiresAt: String) = JSONObject()
        .put("room_code", room.roomCode.uppercase(Locale.US))
        .put("host_user_id", room.hostUserId)
        .put("cloud_id", room.cloudId)
        .put("media_type", room.mediaType)
        .put("season", room.season)
        .put("episode", room.episode)
        .put("position_ms", room.positionMs)
        .put("is_playing", room.isPlaying)
        .put("updated_at", isoNow())
        .put("expires_at", expiresAt)

    private suspend fun accountState(session: CloudSession): JSONObject = readRow(session, "__account__")?.optJSONObject("state") ?: JSONObject()

    private suspend fun writeAccountState(session: CloudSession, state: JSONObject) {
        state.put("updatedAt", System.currentTimeMillis())
        val body = JSONObject()
            .put("user_id", session.userId).put("profile_id", "__account__").put("state", state).put("updated_at", isoNow()).toString()
        Http.text(
            "$url/rest/v1/miflix_user_state?on_conflict=user_id,profile_id",
            "POST",
            headers(session) + ("Prefer" to "resolution=merge-duplicates,return=minimal"),
            body
        )
    }

    private suspend fun readRow(session: CloudSession, profileId: String): JSONObject? {
        val pid = URLEncoder.encode(profileId, "UTF-8")
        val text = Http.text("$url/rest/v1/miflix_user_state?user_id=eq.${session.userId}&profile_id=eq.$pid&select=state,updated_at", headers = headers(session))
        val a = JSONArray(text)
        return if (a.length() > 0) a.optJSONObject(0) else null
    }

    private fun isoNow(): String = isoAt(System.currentTimeMillis())
    private fun isoAt(ms: Long): String = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date(ms))
    private fun parseIso(value: String): Long = runCatching {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US).parse(value)?.time ?: 0L
    }.getOrElse { 0L }
}
