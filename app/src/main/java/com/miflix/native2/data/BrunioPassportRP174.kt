package com.miflix.native2.data

data class BrunioPassportSessionRP174(
    val sessionId: String, val ownerUserId: String, val profileId: String,
    val hostDeviceId: String, val expiresAtEpochMs: Long,
    val importedFields: Set<String> = setOf("favorites","progress","languages","collections"),
)
object BrunioPassportPolicyRP174 {
    const val maxMinutes = 120
    val allowedFields = setOf("favorites","progress","languages","collections")
    fun valid(session: BrunioPassportSessionRP174, now: Long): Boolean =
        now < session.expiresAtEpochMs && session.ownerUserId.isNotBlank() && session.hostDeviceId.isNotBlank()
}
