package com.nuvio.app.features.brunio

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * BruniO profile identity and state; never calls Nuvio's incompatible sync RPCs.
 * Deliberately read-only until conflict-safe writes are implemented and tested.
 */
internal class BrunioAccountGateway(
    private val userId: String,
    private val selectedProfileId: String,
    private val transport: BrunioRp1Transport,
) {
    init {
        require(userId.matches(Regex("[0-9a-fA-F-]{36}"))) { "Invalid BruniO user ID" }
        require(selectedProfileId.isNotBlank() && selectedProfileId.length <= 160)
    }

    suspend fun loadAccount(): JsonObject {
        val rows = transport.execute(BrunioRp1Call(
            profileId = selectedProfileId,
            method = "GET",
            path = "miflix_user_state?user_id=eq.$userId&profile_id=eq.__account__&select=state",
        ))
        return (rows as? JsonArray)?.firstOrNull()
            ?.let { it as? JsonObject }
            ?.get("state")
            ?.let { it as? JsonObject }
            ?: buildJsonObject {
                put("profiles", JsonArray(listOf(buildJsonObject {
                    put("id", "default")
                    put("name", "Main")
                    put("primary", true)
                })))
            }
    }

    suspend fun loadProfiles(): BrunioProfileProjection =
        BrunioProfileWire.fromAccountRow(loadAccount(), userId)

    suspend fun loadProfileState(profileId: String): JsonObject? {
        require(profileId.isNotBlank() && profileId.length <= 160)
        val safeProfile = encodeQueryValue(profileId)
        val rows = transport.execute(BrunioRp1Call(
            profileId = selectedProfileId,
            method = "GET",
            path = "miflix_user_state?user_id=eq.$userId&profile_id=eq.$safeProfile&select=state",
        ))
        return (rows as? JsonArray)?.firstOrNull()
            ?.let { it as? JsonObject }
            ?.get("state")
            ?.let { it as? JsonObject }
    }
}

/** Percent-encode opaque profile IDs, without letting IDs inject REST filters. */
internal fun encodeQueryValue(value: String): String = buildString {
    for (byte in value.encodeToByteArray()) {
        val x = byte.toInt() and 255
        val safe = x in 65..90 || x in 97..122 || x in 48..57 ||
            x == 45 || x == 46 || x == 95 || x == 126
        if (safe) append(x.toChar())
        else {
            append('%')
            append(x.toString(16).uppercase().padStart(2, '0'))
        }
    }
}
