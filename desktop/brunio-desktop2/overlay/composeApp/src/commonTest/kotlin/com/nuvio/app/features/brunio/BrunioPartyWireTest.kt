package com.nuvio.app.features.brunio

import com.nuvio.app.features.party.PartyMedia
import com.nuvio.app.features.party.PartyPlayback
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BrunioPartyWireTest {
    @Test
    fun readsExistingBrunioRp1MovieRoom() {
        val original = Json.parseToJsonElement(
            """{"cloudId":"tmdb:movie:101","season":0,"episode":0,"positionMs":9100,"playing":true,"updatedAt":1700000000000,"unknown":"preserved"}"""
        ).jsonObject
        val result = assertNotNull(BrunioPartyWire.readBrunioState(original, seq = 3))
        assertEquals("tmdb:movie:101", result.media.metaId)
        assertEquals(9100L, result.playback.positionMs)
        assertFalse(result.playback.paused)
        assertEquals(1700000000000L, result.playback.at)
    }

    @Test
    fun leavesUnknownFieldsInBrunioRoomWhenUpdating() {
        val original = Json.parseToJsonElement("""{"liveProvider":"custom","retained":true}""").jsonObject
        val result = BrunioPartyWire.writeBrunioState(
            original,
            PartyMedia(type = "series", metaId = "tmdb:series:42", metaType = "series", season = 2, episode = 4),
            PartyPlayback(seq = 7L, paused = true, positionMs = 300L, at = 0L),
            serverTimeMs = 1000L,
        )
        assertEquals("tmdb:series:42", result["cloudId"]?.jsonPrimitive?.content)
        assertEquals("true", result["retained"]?.toString())
        assertEquals("false", result["playing"]?.toString())
        assertEquals("300", result["positionMs"]?.toString())
    }

    @Test
    fun rejectsUnknownVideoIdsRatherThanGuessingTheMedia() {
        val original = Json.parseToJsonElement("""{"cloudId":"imdb:tt123","positionMs":10000}""").jsonObject
        assertNull(BrunioPartyWire.readBrunioState(original))
    }
}
