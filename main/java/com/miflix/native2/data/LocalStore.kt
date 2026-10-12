package com.miflix.native2.data

import android.content.Context
import com.miflix.native2.model.CloudSession
import com.miflix.native2.model.Profile
import org.json.JSONArray
import org.json.JSONObject

class LocalStore(context: Context) {
    private val p = context.getSharedPreferences("miflix_native2", Context.MODE_PRIVATE)

    fun saveSession(s: CloudSession?) {
        if (s == null) p.edit().remove("session").apply()
        else p.edit().putString("session", JSONObject().put("access", s.accessToken).put("refresh", s.refreshToken).put("uid", s.userId).put("email", s.email).toString()).apply()
    }

    fun loadSession(): CloudSession? = runCatching {
        val raw = p.getString("session", null) ?: return null
        val j = JSONObject(raw)
        CloudSession(j.getString("access"), j.optString("refresh"), j.getString("uid"), j.optString("email"))
    }.getOrNull()

    fun offlineProfiles(): List<Profile> = runCatching {
        val a=JSONArray(p.getString("offline_profiles","[]"))
        (0 until a.length()).map { a.getJSONObject(it).let { x -> Profile(x.getString("id"),x.getString("name"),x.optString("avatarValue").takeUnless { it.isBlank() || it=="null" },x.optBoolean("primary")) } }
    }.getOrDefault(emptyList())
    fun saveOfflineProfiles(profiles: List<Profile>) {
        val a=JSONArray()
        profiles.forEach { a.put(JSONObject().put("id",it.id).put("name",it.name).put("avatarValue",it.avatarValue).put("primary",it.primary)) }
        p.edit().putString("offline_profiles",a.toString()).apply()
    }
    fun saveProfileId(id: String) = p.edit().putString("profile", id).apply()
    fun profileId(): String? = p.getString("profile", null)
}
