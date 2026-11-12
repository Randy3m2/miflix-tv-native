package com.nuvio.app.features.brunio

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class BrunioAccountGatewayTest {
    @Test
    fun readsAccountUsingBrunioState() = runTest {
        val seen = mutableListOf<BrunioRp1Call>()
        val transport = BrunioRp1Transport { request ->
            seen += request
            Json.parseToJsonElement(
                """[{"state":{"profiles":[{"id":"abc","name":"Cinema"}],"privateSetup":{"addonManifests":[]}}}]"""
            )
        }
        val gateway = BrunioAccountGateway(
            "25b3cbb2-87b2-43e6-bd74-ec2f7ac0b1d5", "abc", transport,
        )
        val projection = gateway.loadProfiles()
        assertEquals("abc", projection.brunioId(1))
        assertEquals("Cinema", projection.nuvioProfiles.single().name)
        assertTrue(seen.single().path.startsWith("miflix_user_state?"))
        assertEquals("abc", seen.single().profileId)
    }

    @Test
    fun percentEncodedProfileIdsCannotInjectFilters() {
        assertEquals("a%26profile_id%3Deq.attacker", encodeQueryValue("a&profile_id=eq.attacker"))
        assertEquals("a%2Fb%20c", encodeQueryValue("a/b c"))
    }

    @Test
    fun rejectsMalformedAccountIdentity() {
        val transport = BrunioRp1Transport { _ -> JsonArray(emptyList()) }
        assertFailsWith<IllegalArgumentException> {
            BrunioAccountGateway("injected&profile_id=eq.x", "default", transport)
        }
    }
}
