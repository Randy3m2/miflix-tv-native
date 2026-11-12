package com.nuvio.app.features.brunio

import com.nuvio.app.features.party.PartyMedia
import com.nuvio.app.features.party.PartyPlayback
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * BruniO RP1 <-> Nuvio UI payload mapping ONLY.
 *
 * BruniO RP1 authorization, host approvals, member lookup, guest permissions,
 * chat, events and realtime are NOT provided by this pure codec; a real
 * BruniOPartyGateway must enforce all of them before Nuvio UI is enabled.
 */
internal object BrunioPartyWire {
    const val BRUNIO_IDENTITY_RPC = "brunio_rp1_ensure"
    const val BRUNIO_ROOM_TABLE = "rp1_miflix_watch_parties"
    const val BRUNIO_ROOM_JOIN_RPC = "rp1_miflix_join_room"
    const val BRUNIO_REQUEST_ACCESS_RPC = "rp1_miflix_request_access"
    const val BRUNIO_PLAYBACK_REQUEST_RPC = "rp1_miflix_request_playback"
    const val BRUNIO_PRESENCE_RPC = "rp1_miflix_set_presence"

    data class RoomPlayback(val media: PartyMedia, val playback: PartyPlayback)

    /**
     * BruniO cloudId values look like tmdb:movie:123 or tmdb:series:456.
     * No media URL or credentials are put into the Party payload.
     */
    fun readBrunioState(source: JsonObject, seq: Long = 0L, memberId: String? = null): RoomPlayback? {
        val cloudId = source["cloudId"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val type = when {
            cloudId.startsWith("tmdb:movie:") -> "movie"
            cloudId.startsWith("tmdb:series:") -> "series"
            else -> return null
        }
        val contentId = cloudId.substringAfterLast(":")
        if (contentId.toLongOrNull() == null) return null
        val season = source["season"]?.jsonPrimitive?.intOrNull?.takeIf { it > 0 }
        val episode = source["episode"]?.jsonPrimitive?.intOrNull?.takeIf { it > 0 }
        val state = PartyMedia(
            type = type,
            metaId = cloudId,
            metaType = type,
            season = season,
            episode = episode,
            title = source["title"]?.jsonPrimitive?.contentOrNull.orEmpty(),
        )
        val playback = PartyPlayback(
            seq = seq,
            memberId = memberId,
            paused = source["playing"]?.jsonPrimitive?.booleanOrNull != true,
            positionMs = source["positionMs"]?.jsonPrimitive?.longOrNull?.coerceAtLeast(0) ?: 0L,
            rate = 1f,
            at = source["updatedAt"]?.jsonPrimitive?.longOrNull ?: 0L,
            mediaKey = state.key,
        )
        return RoomPlayback(state, playback)
    }

    /**
     * Preserve BruniO's existing room JSON format. Unknown fields in a room
     * are left untouched so a PC update cannot remove newer TV-only fields.
     */
    fun writeBrunioState(
        original: JsonObject,
        media: PartyMedia,
        playback: PartyPlayback,
        serverTimeMs: Long,
    ): JsonObject {
        val fields = original.toMutableMap()
        fields["cloudId"] = JsonPrimitive(media.metaId)
        fields["season"] = JsonPrimitive(media.season ?: 0)
        fields["episode"] = JsonPrimitive(media.episode ?: 0)
        fields["positionMs"] = JsonPrimitive(playback.positionMs.coerceAtLeast(0L))
        fields["playing"] = JsonPrimitive(!playback.paused)
        fields["updatedAt"] = JsonPrimitive(serverTimeMs.coerceAtLeast(0L))
        return JsonObject(fields)
    }
}
