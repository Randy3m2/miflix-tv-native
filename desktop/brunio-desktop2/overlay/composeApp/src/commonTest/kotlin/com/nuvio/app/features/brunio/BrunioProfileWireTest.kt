package com.nuvio.app.features.brunio

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class BrunioProfileWireTest {
    @Test
    fun preservesOriginalProfileIdsAcrossNuvioUi() {
        val row = Json.parseToJsonElement(
            """{"state":{"profiles":[{"id":"default","name":"Main","primary":true},{"id":"abc-custom","name":"Family","avatarId":"dragon"}]}}"""
        ).jsonObject
        val projection=BrunioProfileWire.fromAccountRow(row,"auth-user")
        assertEquals(2,projection.nuvioProfiles.size)
        assertEquals("default",projection.brunioId(1))
        assertEquals("abc-custom",projection.brunioId(2))
        assertEquals(2,projection.nuvioIndex("abc-custom"))
        assertEquals("Family",projection.nuvioProfiles[1].name)
        assertEquals("dragon",projection.nuvioProfiles[1].avatarId)
        assertEquals("auth-user",projection.nuvioProfiles[0].userId)
    }

    @Test
    fun accountWithoutProfilesDoesNotCreateRandomNewProfileId() {
        val row=Json.parseToJsonElement("""{"state":{"privateSetup":{}}}""").jsonObject
        val value=BrunioProfileWire.fromAccountRow(row,"abc")
        assertEquals("default",value.brunioId(1))
        assertEquals("Main",value.nuvioProfiles[0].name)
    }

    @Test
    fun duplicateBrunioIdsAreRejectedBeforeAnySync() {
        val row=Json.parseToJsonElement(
            """{"profiles":[{"id":"duplicate","name":"A"},{"id":"duplicate","name":"B"}]}"""
        ).jsonObject
        assertFailsWith<IllegalArgumentException> {BrunioProfileWire.fromAccountRow(row,"abc")}
    }

    @Test
    fun absentIndicesCannotSilentlyFallBackToDifferentProfile() {
        val row=Json.parseToJsonElement("""{"profiles":[{"id":"only","name":"One"}]}""").jsonObject
        val projection=BrunioProfileWire.fromAccountRow(row,"abc")
        assertFailsWith<IllegalArgumentException> { projection.brunioId(2) }
    }
}
