package com.miflix.native2.data

import android.util.Base64
import com.miflix.native2.model.PairingPayload
import com.miflix.native2.model.PairingRequest
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class PairingRepository(
    private val supabaseUrl: String,
    private val publishableKey: String,
    private val webBaseUrl: String = "https://randy3m2.github.io/miflix-tv-native/pair/"
) {
    private fun headers() = mapOf("apikey" to publishableKey, "Content-Type" to "application/json")

    suspend fun create(avatar: Boolean = false): PairingRequest {
        val random = SecureRandom()
        val secret = ByteArray(32).also { random.nextBytes(it) }
        val codeBytes = ByteArray(12).also { random.nextBytes(it) }
        val code = codeBytes.joinToString("") { "%02x".format(it) }.take(18).uppercase(Locale.US)
        val secretText = Base64.encodeToString(secret, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        val body = JSONObject()
            .put("pair_code", code)
            .put("status", "pending")
            .put("expires_at", isoFromMillis(System.currentTimeMillis() + 10 * 60 * 1000L))
            .toString()
        Http.text(
            "$supabaseUrl/rest/v1/miflix_pairing",
            "POST",
            headers() + ("Prefer" to "return=minimal"),
            body
        )
        val url = "$webBaseUrl?code=${URLEncoder.encode(code, "UTF-8")}${if(avatar) "&mode=avatar" else ""}#key=$secretText"
        return PairingRequest(code, secretText, url)
    }

    suspend fun poll(request: PairingRequest): PairingPayload? {
        val code = URLEncoder.encode(request.code, "UTF-8")
        val text = Http.text(
            "$supabaseUrl/rest/v1/miflix_pairing?pair_code=eq.$code&select=status,payload_enc,iv",
            headers = headers()
        )
        val row = JSONArray(text).optJSONObject(0) ?: return null
        if (row.optString("status") != "approved") return null
        val cipherText = row.optString("payload_enc")
        val iv = row.optString("iv")
        if (cipherText.isBlank() || iv.isBlank()) return null
        val json = decrypt(request.secretBase64Url, iv, cipherText)
        val j = JSONObject(json)
        return PairingPayload(
            accessToken = j.optString("accessToken"),
            refreshToken = j.optString("refreshToken"),
            userId = j.optString("userId"),
            email = j.optString("email"),
            addonManifest = j.optString("addonManifest"),
            avatarData = j.optString("avatarData")
        )
    }

    suspend fun finish(request: PairingRequest) {
        val code = URLEncoder.encode(request.code, "UTF-8")
        runCatching {
            Http.text(
                "$supabaseUrl/rest/v1/miflix_pairing?pair_code=eq.$code",
                "DELETE",
                headers() + ("Prefer" to "return=minimal")
            )
        }
    }

    private fun decrypt(secretB64: String, ivB64: String, cipherB64: String): String {
        val secret = Base64.decode(secretB64, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        val iv = Base64.decode(ivB64, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        val encrypted = Base64.decode(cipherB64, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(secret, "AES"), GCMParameterSpec(128, iv))
        return String(cipher.doFinal(encrypted), Charsets.UTF_8)
    }

    private fun isoFromMillis(ms: Long): String = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }.format(Date(ms))
}
