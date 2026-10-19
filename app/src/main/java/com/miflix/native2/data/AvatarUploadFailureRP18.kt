package com.miflix.native2.data

import org.json.JSONObject

/** Return fixed messages: storage responses can contain private paths or tokens. */
internal object AvatarUploadFailureRP18 {
    fun message(status:Int,body:String):String {
        val json=runCatching {JSONObject(body)}.getOrNull()
        val text=(json?.optString("message").orEmpty()+" "+json?.optString("error").orEmpty()).lowercase(java.util.Locale.ROOT)
        val code=json?.optString("code").orEmpty().lowercase(java.util.Locale.ROOT)
        val prefix="Avatar upload failed (HTTP $status). "
        return prefix+when {
            ("bucket" in text && listOf("not found","does not exist","missing").any {it in text}) || code=="nosuchbucket" ->
                "The miflix-avatars bucket is missing. Run supabase_rc18_upgrade.sql."
            status==401 || "jwt" in text || "token expired" in text ->"Session expired or unauthorized. Sign in again."
            "row-level security" in text || "row level security" in text || status==403 ->
                "Storage permissions rejected the upload. Check the miflix-avatars owner policies in supabase_rc18_upgrade.sql."
            status==413 || "size" in text || "too large" in text ->"Avatar exceeds the storage size limit (512000 bytes)."
            "mime" in text || "content-type" in text ->"The avatar bucket must accept image/jpeg."
            else ->"Storage rejected the image. Check the avatar bucket, JPEG limits and owner permissions."
        }
    }
}
