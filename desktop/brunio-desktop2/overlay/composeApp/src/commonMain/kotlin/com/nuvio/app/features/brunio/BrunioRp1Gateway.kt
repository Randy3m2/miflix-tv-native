package com.nuvio.app.features.brunio

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

/**
 * BruniO RP1 social and Party API. Calls are scoped to BruniO's existing
 * six-digit room codes and string profile IDs, NOT Nuvio's watch_party_* RPCs.
 *
 * The caller supplies an authenticated Supabase REST transport which MUST:
 * - add Authorization: Bearer <current access token> and the public apikey
 * - add x-brunio-profile: <call.profileId>
 * - enforce HTTPS to the configured user's BruniO Supabase project
 * - drop cached credentials when the user signs out
 *
 * This is the HTTP/serialization contract. Compose Nuvio PartyRepository and
 * player event wiring are separate and NOT yet implemented.
 */
internal data class BrunioRp1Call(
    val profileId: String,
    val method: String,
    val path: String,
    val jsonBody: JsonObject? = null,
)

internal fun interface BrunioRp1Transport {
    suspend fun execute(call: BrunioRp1Call): JsonElement
}

internal class BrunioRp1Gateway(
    private val profile: String,
    private val transport: BrunioRp1Transport,
) {
    init {
        require(profile.isNotBlank() && profile.length <= 160 && '\n' !in profile)
    }

    private var actorId: String? = null

    private suspend fun send(
        method: String,
        path: String,
        body: JsonObject? = null,
    ): JsonElement = transport.execute(BrunioRp1Call(profile, method, path, body))

    suspend fun actor(): String {
        actorId?.let { return it }
        val response = send("POST", "rpc/brunio_rp1_ensure", buildJsonObject {
            put("p_profile", profile)
        }).jsonObject
        val id = response["actor"]?.jsonPrimitive?.contentOrNull.orEmpty()
        require(id.isNotBlank()) { "BruniO RP1 social identity was not established" }
        actorId = id
        return id
    }

    suspend fun createRoom(
        roomCode: String,
        expiresAt: String,
        nowIso: String,
    ): JsonElement {
        requireValidCode(roomCode)
        val user = actor()
        val body = buildJsonObject {
            put("room_code", roomCode)
            put("host_user_id", user)
            put("state", buildJsonObject {
                put("cloudId", "")
                put("season", 0)
                put("episode", 0)
                put("positionMs", 0L)
                put("playing", false)
                put("updatedAt", 0L)
                put("liveChannelId", "")
                put("liveProvider", "")
                put("liveTitle", "")
                put("liveType", "tv")
            })
            put("updated_at", nowIso)
            put("expires_at", expiresAt)
        }
        val response = send("POST", "rp1_miflix_watch_parties", body)
        send("POST", "rpc/rp1_miflix_join_room", buildJsonObject { put("code", roomCode) })
        return response
    }

    suspend fun requestToJoin(roomCode: String): String {
        requireValidCode(roomCode)
        actor()
        val decision = send("POST", "rpc/rp1_miflix_request_access", buildJsonObject {
            put("code", roomCode)
        })
        val outcome = decision.jsonPrimitive.contentOrNull.orEmpty().trim('"')
        if (outcome == "approved") {
            send("POST", "rpc/rp1_miflix_join_room", buildJsonObject { put("code", roomCode) })
        }
        return outcome
    }

    suspend fun approveAccess(roomCode: String, guestActor: String, approve: Boolean): JsonElement {
        requireValidCode(roomCode)
        require(guestActor.isNotBlank())
        actor()
        return send("POST", "rpc/rp1_miflix_decide_access", buildJsonObject {
            put("code", roomCode)
            put("guest", guestActor)
            put("approve", approve)
        })
    }

    suspend fun accessRequests(): JsonElement {
        actor()
        return send("POST", "rpc/rp1_miflix_access_requests", buildJsonObject {})
    }

    suspend fun readRoom(roomCode: String): JsonElement {
        requireValidCode(roomCode)
        actor()
        return send("GET", "rp1_miflix_watch_parties?room_code=eq.$roomCode&select=room_code,host_user_id,state,expires_at")
    }

    suspend fun members(roomCode: String): JsonElement {
        requireValidCode(roomCode)
        actor()
        return send("GET", "rp1_miflix_party_members?room_code=eq.$roomCode&select=user_id")
    }

    suspend fun hostUpdate(roomCode: String, state: JsonObject, nowIso: String, expiresAt: String): JsonElement {
        requireValidCode(roomCode)
        actor()
        return send("PATCH", "rp1_miflix_watch_parties?room_code=eq.$roomCode", buildJsonObject {
            put("state", state)
            put("updated_at", nowIso)
            put("expires_at", expiresAt)
        })
    }

    suspend fun requestPlayback(roomCode: String, cloudId: String, season: Int, episode: Int, playing: Boolean): JsonElement {
        requireValidCode(roomCode)
        require(cloudId.isNotBlank() && cloudId.length <= 200)
        actor()
        return send("POST", "rpc/rp1_miflix_request_playback", buildJsonObject {
            put("code", roomCode)
            put("media", cloudId)
            put("s", season.coerceAtLeast(0))
            put("e", episode.coerceAtLeast(0))
            put("playing", playing)
        })
    }

    suspend fun setPresence(roomCode: String?, cloudId: String?, title: String, season: Int, episode: Int): JsonElement {
        if (roomCode != null) requireValidCode(roomCode)
        actor()
        return send("POST", "rpc/rp1_miflix_set_presence", buildJsonObject {
            put("title", title.take(256))
            put("media", cloudId ?: "")
            put("s", season.coerceAtLeast(0))
            put("e", episode.coerceAtLeast(0))
            put("room", roomCode?.let(::JsonPrimitive) ?: JsonNull)
        })
    }

    suspend fun sendEvent(roomCode: String, kind: String, text: String): JsonElement {
        requireValidCode(roomCode)
        require(kind in setOf("chat", "emoji", "phrase"))
        val bodyText = text.trim().take(400)
        require(bodyText.isNotEmpty())
        val user = actor()
        return send("POST", "rp1_miflix_party_events", buildJsonObject {
            put("room_code", roomCode)
            put("user_id", user)
            put("kind", kind)
            put("body", bodyText)
        })
    }

    suspend fun events(roomCode: String, afterEventId: Long? = null): JsonElement {
        requireValidCode(roomCode)
        actor()
        val extra = if (afterEventId != null) "&id=gt.${afterEventId.coerceAtLeast(0)}" else ""
        return send("GET", "rp1_miflix_party_events?room_code=eq.$roomCode&select=id,user_id,kind,body&order=id.asc&limit=60$extra")
    }

    suspend fun leave(roomCode: String, asHost: Boolean): JsonElement {
        requireValidCode(roomCode)
        val actor = actor()
        return if (asHost) send("DELETE", "rp1_miflix_watch_parties?room_code=eq.$roomCode")
        else send("DELETE", "rp1_miflix_party_members?room_code=eq.$roomCode&user_id=eq.$actor")
    }

    private fun requireValidCode(code: String) {
        require(code.length == 6 && code.all(Char::isDigit)) {
            "BruniO Party room requires exactly six digits"
        }
    }
}
