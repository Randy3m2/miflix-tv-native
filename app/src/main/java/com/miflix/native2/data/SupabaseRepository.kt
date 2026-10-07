package com.miflix.native2.data

import com.miflix.native2.model.CloudSession
import com.miflix.native2.model.PlaybackProgress
import com.miflix.native2.model.PrivateSetup
import com.miflix.native2.model.Profile
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

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
        val setup = PrivateSetup(setupObj.optString("tmdbToken"), setupObj.optString("torrentioManifest"))
        val pa = state.optJSONArray("profiles") ?: JSONArray()
        val profiles = (0 until pa.length()).mapNotNull { i ->
            val p = pa.optJSONObject(i) ?: return@mapNotNull null
            Profile(p.optString("id"), p.optString("name", "Profile"), p.optString("avatarValue"), p.optBoolean("primary", false))
        }
        return profiles to setup
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
        val isoNow = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }.format(java.util.Date())
        val body = JSONObject().put("user_id", session.userId).put("profile_id", profileId).put("state", state).put("updated_at", isoNow).toString()
        Http.text("$url/rest/v1/miflix_user_state?on_conflict=user_id,profile_id", "POST", headers(session) + ("Prefer" to "resolution=merge-duplicates,return=minimal"), body)
    }

    private suspend fun readRow(session: CloudSession, profileId: String): JSONObject? {
        val pid = URLEncoder.encode(profileId, "UTF-8")
        val text = Http.text("$url/rest/v1/miflix_user_state?user_id=eq.${session.userId}&profile_id=eq.$pid&select=state,updated_at", headers = headers(session))
        val a = JSONArray(text)
        return if (a.length() > 0) a.optJSONObject(0) else null
    }
}
