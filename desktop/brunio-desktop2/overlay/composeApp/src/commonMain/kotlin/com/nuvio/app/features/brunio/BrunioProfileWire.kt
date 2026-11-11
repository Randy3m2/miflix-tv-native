package com.nuvio.app.features.brunio

import com.nuvio.app.features.profiles.MAX_PROFILES
import com.nuvio.app.features.profiles.NuvioProfile
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull

/** Projection only; the real BruniO IDs stay strings in all REST/RP1 calls. */
internal class BrunioProfileProjection(
    val nuvioProfiles: List<NuvioProfile>,
    private val brunioIdsByNuvioIndex: Map<Int, String>,
) {
    fun brunioId(profileIndex: Int): String =
        requireNotNull(brunioIdsByNuvioIndex[profileIndex]) {
            "No BruniO profile identity for Nuvio index $profileIndex"
        }

    fun nuvioIndex(brunioId: String): Int =
        requireNotNull(brunioIdsByNuvioIndex.entries.firstOrNull { it.value == brunioId }?.key) {
            "No Nuvio UI profile for BruniO profile $brunioId"
        }
}

/** Translates BruniO's per-user __account__.profiles to Nuvio's existing profile picker model.
 * Does NOT call Nuvio's incompatible sync_pull_profiles or write remote account state.
 */
internal object BrunioProfileWire {
    fun fromAccountRow(row: JsonObject, authenticatedUserId: String): BrunioProfileProjection {
        require(authenticatedUserId.isNotBlank())
        val account = (row["state"] as? JsonObject) ?: row
        val raw = account["profiles"] as? JsonArray
        if (raw == null || raw.isEmpty()) {
            return BrunioProfileProjection(
                listOf(NuvioProfile(
                    id = "brunio:${authenticatedUserId}:default",
                    userId = authenticatedUserId,
                    profileIndex = 1,
                    name = "Main",
                )),
                mapOf(1 to "default"),
            )
        }
        require(raw.size <= MAX_PROFILES) {
            "BruniO has ${raw.size} profiles but this Nuvio picker supports at most $MAX_PROFILES"
        }
        val ids = linkedMapOf<Int, String>()
        val profiles = raw.mapIndexed { index, element ->
            val data = element as? JsonObject
                ?: error("Invalid BruniO profile at index $index")
            val profileId = data.text("id")?.trim().orEmpty()
            require(profileId.isNotBlank() && profileId.length <= 160 && '\n' !in profileId)
            require(profileId !in ids.values) { "Duplicate BruniO profile ID" }
            val n = index + 1
            ids[n] = profileId
            NuvioProfile(
                id = "brunio:${authenticatedUserId}:$profileId",
                userId = authenticatedUserId,
                profileIndex = n,
                name = data.text("name")?.trim()?.takeIf(String::isNotEmpty) ?: "Profile $n",
                avatarId = data.text("avatarId") ?: data.text("icon"),
                avatarUrl = (data.text("avatarUrl") ?: data.text("avatar_url"))
                    ?.takeIf { it.startsWith("https://", true) },
                avatarColorHex = data.text("avatarColor") ?: data.text("avatar_color_hex") ?: "#1E88E5",
                usesPrimaryAddons = (data["primary"] as? JsonPrimitive)?.booleanOrNull ?: (index == 0),
                pinEnabled = (data["pinEnabled"] as? JsonPrimitive)?.booleanOrNull == true,
            )
        }
        return BrunioProfileProjection(profiles, ids)
    }

    private fun JsonObject.text(name: String): String? =
        (this[name] as? JsonPrimitive)?.contentOrNull
}
