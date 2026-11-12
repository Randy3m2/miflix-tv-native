package com.nuvio.app.features.brunio

import com.nuvio.app.core.auth.AuthRepository
import com.nuvio.app.core.auth.AuthState
import com.nuvio.app.core.network.SupabaseProvider
import com.nuvio.app.features.library.LibraryRepository
import com.nuvio.app.features.watchprogress.WatchProgressRepository
import com.nuvio.app.features.tmdb.TmdbSettingsRepository
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Account-scoped read bridge to the existing BruniO TV/mobile schema.
 *
 * This is deliberately NOT Nuvio sync: no sync_pull_profiles, addons, or
 * watch_party_* calls are permitted against the BruniO Supabase project.
 * Only the account/profile-specific state is loaded; no periodic Home polling.
 */
internal object BrunioCloud {
    const val enabled: Boolean = true

    private val json = Json { ignoreUnknownKeys = true }
    private var accountUserId: String? = null
    private var accountSnapshot: JsonObject? = null
    private var profileProjection: BrunioProfileProjection? = null

    suspend fun pullProfiles(userId: String): BrunioProfileProjection {
        val rows = SupabaseProvider.client.postgrest.from("miflix_user_state").select {
            filter {
                eq("user_id", userId)
                eq("profile_id", "__account__")
            }
        }.data
        val row = (json.parseToJsonElement(rows) as? JsonArray)
            ?.firstOrNull() as? JsonObject ?: JsonObject(emptyMap())
        val account = (row["state"] as? JsonObject) ?: JsonObject(emptyMap())
        val projection = BrunioProfileWire.fromAccountRow(account, userId)
        accountUserId = userId
        accountSnapshot = account
        profileProjection = projection
        return projection
    }

    fun accountState(): JsonObject = accountSnapshot ?: JsonObject(emptyMap())

    fun profileId(profileIndex: Int): String? =
        runCatching { profileProjection?.brunioId(profileIndex) }.getOrNull()

    fun manifestUrls(): List<String> {
        val setup = accountSnapshot?.get("privateSetup") as? JsonObject ?: JsonObject(emptyMap())
        val urls = ((setup["addonManifests"] as? JsonArray)
            ?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
            ?: emptyList()) +
            listOfNotNull((setup["torrentioManifest"] as? JsonPrimitive)?.contentOrNull) +
            listOf(
                (setup["sportsManifest"] as? JsonPrimitive)?.contentOrNull
                    ?.takeIf { it.startsWith("https://") }
                    ?: brunioDefaultSportsManifest()
            )
        // Reuse the Nuvio Stremio parser; never disclose private manifest URLs in logs.
        return urls.map(String::trim)
            .filter { it.startsWith("https://") && it.length <= 2048 }
            .distinct()
            .take(40)
    }

    /**
     * Called only when an authenticated BruniO profile enters the Nuvio shell.
     * TMDB account token is installed in memory, NOT written to Nuvio's settings.
     */
    suspend fun activateProfile(index: Int) {
        val auth = AuthRepository.state.value as? AuthState.Authenticated ?: return
        if (auth.isAnonymous) return
        if (accountUserId != auth.userId || accountSnapshot == null) {
            pullProfiles(auth.userId)
        }
        check(profileId(index) != null) { "Selected BruniO profile is unavailable" }
        val setup = accountSnapshot?.get("privateSetup") as? JsonObject ?: JsonObject(emptyMap())
        val token = (setup["tmdbToken"] as? JsonPrimitive)?.contentOrNull.orEmpty()
        val language = (accountSnapshot?.get("language") as? JsonPrimitive)?.contentOrNull
            ?: "es-ES"
        if (token.isNotBlank()) {
            TmdbSettingsRepository.applyBrunioAccountSettings(token, language)
        } else {
            TmdbSettingsRepository.ensureLoaded()
        }
        val state = loadProfileState(index)
        LibraryRepository.applyBrunioSnapshot(BrunioMediaStateWire.favoriteItems(state))
        WatchProgressRepository.applyBrunioSnapshot(BrunioMediaStateWire.progressEntries(state))
    }

    suspend fun loadProfileState(index: Int): JsonObject {
        val auth = AuthRepository.state.value as? AuthState.Authenticated
            ?: return JsonObject(emptyMap())
        val profile = profileId(index)
            ?: return JsonObject(emptyMap())
        val rows = SupabaseProvider.client.postgrest.from("miflix_user_state").select {
            filter {
                eq("user_id", auth.userId)
                eq("profile_id", profile)
            }
        }.data
        val entry = (json.parseToJsonElement(rows) as? JsonArray)
            ?.firstOrNull() as? JsonObject
        return (entry?.get("state") as? JsonObject) ?: JsonObject(emptyMap())
    }

    fun clear() {
        accountUserId = null
        accountSnapshot = null
        profileProjection = null
    }
}
