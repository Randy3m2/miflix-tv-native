package com.miflix.native2.data

import android.graphics.BitmapFactory
import android.util.Base64
import com.miflix.native2.model.CloudSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/** Avatar bytes arrive encrypted through a one-time pairing QR; only the TV uploads to its own account. */
internal class AvatarRepository(private val base: String,private val key: String) {
    fun owns(url: String,owner: String): Boolean = url.startsWith("$base/storage/v1/object/public/miflix-avatars/$owner/") &&
        Regex("[A-Za-z0-9_-]+/[a-f0-9-]+\\.jpg").matches(url.substringAfter("/miflix-avatars/$owner/"))
    suspend fun upload(s: CloudSession,profile: String,data: String): String = withContext(Dispatchers.IO) {
        require(Regex("[A-Za-z0-9_-]{1,80}").matches(profile)) { "Invalid profile" }
        require(data.startsWith("data:image/jpeg;base64,") && data.length<=700_000) { "Invalid avatar image" }
        val bytes=Base64.decode(data.substringAfter(','),Base64.NO_WRAP)
        require(bytes.size in 1..512_000) { "Avatar too large" }
        val bounds=BitmapFactory.Options().apply { inJustDecodeBounds=true }
        BitmapFactory.decodeByteArray(bytes,0,bytes.size,bounds)
        require(bounds.outMimeType=="image/jpeg" && bounds.outWidth in 1..512 && bounds.outHeight in 1..512) { "Invalid avatar dimensions" }
        val path="${s.userId}/$profile/${UUID.randomUUID()}.jpg"
        val c=URL("$base/storage/v1/object/miflix-avatars/$path").openConnection() as HttpURLConnection
        try {
            c.requestMethod="POST"; c.connectTimeout=12000; c.readTimeout=20000; c.instanceFollowRedirects=false
            c.setRequestProperty("apikey",key); c.setRequestProperty("Authorization","Bearer ${s.accessToken}")
            c.setRequestProperty("Content-Type","image/jpeg"); c.doOutput=true; c.setFixedLengthStreamingMode(bytes.size)
            c.outputStream.use { it.write(bytes) }
            check(c.responseCode in 200..299) { "Avatar upload failed (HTTP ${c.responseCode}). Run supabase_rc18_upgrade.sql first." }
        } finally { c.disconnect() }
        "$base/storage/v1/object/public/miflix-avatars/$path"
    }
    suspend fun remove(s: CloudSession,url: String) {
        if(!owns(url,s.userId)) return
        val path=url.substringAfter("/miflix-avatars/")
        Http.text("$base/storage/v1/object/miflix-avatars/$path","DELETE",mapOf("apikey" to key,"Authorization" to "Bearer ${s.accessToken}"))
    }
}
