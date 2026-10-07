package com.miflix.native2.data

import android.content.Context
import com.miflix.native2.model.CloudSession
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

    fun saveProfileId(id: String) = p.edit().putString("profile", id).apply()
    fun profileId(): String? = p.getString("profile", null)
}
