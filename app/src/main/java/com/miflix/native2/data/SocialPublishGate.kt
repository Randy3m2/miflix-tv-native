package com.miflix.native2.data

/** Repository mutex protects this gate. Only successful writes advance the deadline. */
internal class SocialPublishGate(private val intervalMs: Long, private val changedOnly: Boolean) {
    private var owner: String? = null
    private var signature: String? = null
    private var publishedAt = 0L
    fun due(user: String, value: String, now: Long): Boolean {
        if (owner != user) return true
        if (changedOnly && signature == value) return false
        return now - publishedAt >= intervalMs
    }
    fun published(user: String, value: String, now: Long) {
        owner = user; signature = value; publishedAt = now
    }
}
