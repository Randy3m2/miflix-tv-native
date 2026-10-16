package com.miflix.native2.data

import com.miflix.native2.model.StreamChoice
import java.security.MessageDigest

/** Public source identity excludes URLs, provider credentials and private headers. */
object PartySourcePolicy {
    fun label(value: String): String = value.replace(Regex("https?://\\S+",RegexOption.IGNORE_CASE),"")
        .filter { !it.isISOControl() }.trim().take(180)
    fun key(stream: StreamChoice): String {
        val identity=listOf(label(stream.addonName),label(stream.name),label(stream.filename),stream.videoSize.toString()).joinToString("|")
        return MessageDigest.getInstance("SHA-256").digest(identity.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }
}
