package com.miflix.native2.data

enum class WatchPartyModeRP176 { OFFLINE_LAN, BROWSER_INVITE }
enum class WatchPartyRoleRP176 { HOST, GUEST }
enum class WatchPartyCommandRP176 { PLAY, PAUSE, SEEK, NEXT, REACTION, CHAT, CLOSE }

data class WatchPartyMediaRP176(
    val mediaId: String, val title: String, val sourceUrl: String?,
    val durationMs: Long, val drmProtected: Boolean = false,
)
data class WatchPartyEventRP176(
    val roomId: String, val sequence: Long, val command: WatchPartyCommandRP176,
    val positionMs: Long = 0, val payload: String = "", val senderId: String,
)
data class WatchPartyRoomRP176(
    val roomId: String, val mode: WatchPartyModeRP176, val role: WatchPartyRoleRP176,
    val hostId: String, val expiresAtEpochMs: Long, val media: WatchPartyMediaRP176?,
    val allowGuestsToControl: Boolean = false,
)
object WatchPartyPolicyRP176 {
    const val offlineMaxGuests = 8
    const val browserMaxGuests = 20
    const val browserRoomMinutes = 240L
    fun canShare(room: WatchPartyRoomRP176, media: WatchPartyMediaRP176): Boolean =
        !media.drmProtected && media.sourceUrl != null && media.sourceUrl.startsWith("http")
    fun canRun(room: WatchPartyRoomRP176, now: Long): Boolean =
        now < room.expiresAtEpochMs && (room.mode != WatchPartyModeRP176.OFFLINE_LAN || room.role == WatchPartyRoleRP176.HOST)
    fun canControl(room: WatchPartyRoomRP176, role: WatchPartyRoleRP176): Boolean =
        role == WatchPartyRoleRP176.HOST || room.allowGuestsToControl
    fun maxGuests(mode: WatchPartyModeRP176): Int =
        if (mode == WatchPartyModeRP176.OFFLINE_LAN) offlineMaxGuests else browserMaxGuests
}
