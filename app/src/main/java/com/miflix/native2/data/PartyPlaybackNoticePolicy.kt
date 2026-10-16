package com.miflix.native2.data

data class PartyPlaybackNotice(val actor: String,val resumed: Boolean,val localOnly: Boolean)

/** Only fresh, explicit user actions appear; old chat history never replays as a notice. */
object PartyPlaybackNoticePolicy {
    val actions=setOf("paused","resumed","paused-local","resumed-local")
    fun accepts(kind: String,body: String,createdAt: Long,now: Long): Boolean =
        kind=="playback" && body in actions && createdAt>0 && now-createdAt in -2000L..10000L
    fun notice(name: String,body: String) = PartyPlaybackNotice(name.take(40),body.startsWith("resumed"),body.endsWith("-local"))
}
