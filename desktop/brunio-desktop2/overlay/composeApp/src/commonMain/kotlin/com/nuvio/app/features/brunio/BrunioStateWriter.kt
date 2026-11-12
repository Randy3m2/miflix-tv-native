package com.nuvio.app.features.brunio

import com.nuvio.app.core.auth.AuthRepository
import com.nuvio.app.core.auth.AuthState
import com.nuvio.app.core.network.SupabaseProvider
import com.nuvio.app.features.library.LibraryItem
import com.nuvio.app.features.profiles.ProfilePushPayload
import com.nuvio.app.features.watchprogress.WatchProgressEntry
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import kotlin.time.Clock

/**
 * BruniO-compatible writers. Each mutation re-reads the freshest user state,
 * merges only its own fields, and upserts to the existing BruniO table. They
 * NEVER call Nuvio sync_* RPCs or store private TMDB/add-on keys in Nuvio sync.
 *
 * Cross-device write races need a backend JSONB merge RPC for full atomicity;
 * this matches the existing BruniO PC REST upsert contract in the meantime.
 */
internal object BrunioStateWriter {
    private val writer = Mutex()
    private val cloudIdPattern = Regex("^tmdb:(movie|series):[0-9]+$")

    private fun context(profileIndex: Int): Pair<String, String> {
        val user = AuthRepository.state.value as? AuthState.Authenticated
            ?: error("BruniO login required")
        check(!user.isAnonymous) { "Guest sessions cannot write to BruniO" }
        val profile = BrunioCloud.profileId(profileIndex)
            ?: error("BruniO profile is not active")
        return user.userId to profile
    }

    private suspend fun save(userId: String, profileId: String, state: JsonObject) {
        val user = AuthRepository.state.value as? AuthState.Authenticated
        check(user != null && !user.isAnonymous && user.userId == userId) {
            "BruniO account changed during save"
        }
        val row = buildJsonObject {
            put("user_id", userId)
            put("profile_id", profileId)
            put("state", state)
            put("updated_at", Clock.System.now().toString())
        }
        SupabaseProvider.client.postgrest.from("miflix_user_state").upsert(row) {
            onConflict = "user_id,profile_id"
        }
    }

    suspend fun saveFavorites(profileIndex: Int, local: Collection<LibraryItem>) = writer.withLock {
        val (user, profile) = context(profileIndex)
        val remote = BrunioCloud.loadProfileState(profileIndex)
        val preserved = (remote["favorites"] as? JsonArray).orEmpty()
            .mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
            .filterNot(cloudIdPattern::matches)
        val current = local.map(LibraryItem::id).filter(cloudIdPattern::matches)
        val merged = (preserved + current).distinct().take(5000)
        save(user, profile, JsonObject(remote + ("favorites" to JsonArray(merged.map(::JsonPrimitive)))))
    }

    suspend fun saveProgress(profileIndex: Int, entry: WatchProgressEntry) = writer.withLock {
        val (user, profile) = context(profileIndex)
        val parent = Regex("tmdb:(?:movie|series):[0-9]+").find(entry.parentMetaId)
            ?.value ?: return@withLock
        val season = entry.seasonNumber?.takeIf { it > 0 }
        val episode = entry.episodeNumber?.takeIf { it > 0 }
        val episodeKey = if (season != null && episode != null)
            "$parent:s${season}e$episode" else parent
        val position = entry.lastPositionMs.coerceAtLeast(0L) / 1000.0
        val duration = entry.durationMs.coerceAtLeast(0L) / 1000.0
        val percent = (entry.progressPercent?.toDouble()
            ?: if (duration > 0.0) position / duration * 100.0 else 0.0)
            .coerceIn(0.0, 100.0)
        val now = Clock.System.now().toEpochMilliseconds()
        val payload = buildJsonObject {
            put("position", position)
            put("duration", duration)
            put("percent", percent)
            put("updatedAt", now)
            put("season", season ?: 0)
            put("episode", episode ?: 0)
        }
        val remote = BrunioCloud.loadProfileState(profileIndex)
        val progress = (remote["progress"] as? JsonObject)?.toMutableMap() ?: mutableMapOf()
        progress[episodeKey] = payload
        if (episodeKey != parent) progress[parent] = payload
        save(user, profile, JsonObject(remote + ("progress" to JsonObject(progress))))
    }

    suspend fun deleteProgress(profileIndex: Int, entries: Collection<WatchProgressEntry>) = writer.withLock {
        val (user, profile) = context(profileIndex)
        val remote = BrunioCloud.loadProfileState(profileIndex)
        val progress = (remote["progress"] as? JsonObject)?.toMutableMap() ?: return@withLock
        entries.forEach { item ->
            val parent = Regex("tmdb:(?:movie|series):[0-9]+").find(item.parentMetaId)?.value
                ?: return@forEach
            val season = item.seasonNumber?.takeIf { it > 0 }
            val episode = item.episodeNumber?.takeIf { it > 0 }
            if (season != null && episode != null) {
                progress.remove("$parent:s${season}e$episode")
                if (progress[parent]?.let { (it as? JsonObject)?.get("season") as? JsonPrimitive }
                    ?.intOrNull == season) progress.remove(parent)
            } else progress.remove(parent)
        }
        save(user, profile, JsonObject(remote + ("progress" to JsonObject(progress))))
    }

    private suspend fun updateAccountSetup(transform: (JsonObject) -> JsonObject) {
        val user = AuthRepository.state.value as? AuthState.Authenticated
            ?: error("BruniO login required")
        check(!user.isAnonymous)
        BrunioCloud.pullProfiles(user.userId)
        val account = BrunioCloud.accountState()
        val setup = account["privateSetup"] as? JsonObject ?: JsonObject(emptyMap())
        val next = JsonObject(account + ("privateSetup" to transform(setup)))
        save(user.userId, "__account__", next)
        BrunioCloud.pullProfiles(user.userId)
    }

    suspend fun saveTmdbToken(token: String) = writer.withLock {
        val trimmed = token.trim()
        require(trimmed.length <= 4096)
        updateAccountSetup { state ->
            JsonObject(state + ("tmdbToken" to JsonPrimitive(trimmed)))
        }
    }

    suspend fun saveAddonManifests(raw: List<String>) = writer.withLock {
        val urls = raw.map(String::trim)
            .filter { it.startsWith("https://") && it.length <= 2048 }
            .distinct().take(40)
        updateAccountSetup { state ->
            JsonObject(state +
                ("addonManifests" to JsonArray(urls.map(::JsonPrimitive))) +
                ("torrentioManifest" to JsonPrimitive(urls.firstOrNull().orEmpty()))
            )
        }
    }

    suspend fun saveProfiles(payloads: List<ProfilePushPayload>) = writer.withLock {
        val auth = AuthRepository.state.value as? AuthState.Authenticated
            ?: error("BruniO login required")
        check(!auth.isAnonymous && payloads.size in 1..6)
        val projection = BrunioCloud.pullProfiles(auth.userId)
        // Re-read __account__ so unrelated setup/settings are preserved.
        val account = BrunioCloud.accountState()
        val existing = (account["profiles"] as? JsonArray).orEmpty()
        val rewritten = payloads.map { profile ->
            val old = existing.getOrNull(profile.profileIndex - 1) as? JsonObject
            val stableId = old?.get("id")?.let { (it as? JsonPrimitive)?.contentOrNull }
                ?: if (profile.profileIndex == 1) "default" else
                    "pc-${Clock.System.now().toEpochMilliseconds()}-${profile.profileIndex}"
            buildJsonObject {
                if (old != null) old.forEach { (key, value) -> put(key, value) }
                put("id", stableId)
                put("name", profile.name.trim().take(24))
                put("avatarValue", profile.avatarUrl ?: profile.avatarId ?: "ai:astronaut")
                put("primary", profile.profileIndex == 1)
            }
        }
        val state = JsonObject(account + ("profiles" to JsonArray(rewritten)))
        save(auth.userId, "__account__", state)
        BrunioCloud.pullProfiles(auth.userId)
    }

    suspend fun deleteProfile(profileIndex: Int) = writer.withLock {
        val (user, profile) = context(profileIndex)
        check(profileIndex != 1) { "Cannot delete the primary BruniO profile" }
        SupabaseProvider.client.postgrest.rpc("miflix_delete_profile", buildJsonObject {
            put("target_profile", profile)
        })
        BrunioCloud.pullProfiles(user)
    }
}
