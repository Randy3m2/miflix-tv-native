package com.nuvio.app.features.brunio

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BrunioRp1GatewayTest {
    private class Fake : BrunioRp1Transport {
        val calls = mutableListOf<BrunioRp1Call>()
        var decision = "pending"
        override suspend fun execute(call: BrunioRp1Call): JsonElement {
            calls += call
            return when (call.path) {
                "rpc/brunio_rp1_ensure" ->
                    Json.parseToJsonElement("""{"actor":"brunio-user-id"}""")
                "rpc/rp1_miflix_request_access" -> JsonPrimitive(decision)
                else -> JsonNull
            }
        }
    }

    @Test
    fun pendingAccessNeverJoinsARoom() = runBlocking {
        val remote = Fake()
        val gateway = BrunioRp1Gateway("my-profile", remote)
        assertEquals("pending", gateway.requestToJoin("123456"))
        assertEquals(listOf("rpc/brunio_rp1_ensure", "rpc/rp1_miflix_request_access"), remote.calls.map { it.path })
        assertTrue(remote.calls.all { it.profileId == "my-profile" })
    }

    @Test
    fun approvedAccessUsesBrunioRpcNotNuvioPartyBackend() = runBlocking {
        val remote = Fake().apply { decision = "approved" }
        val gateway = BrunioRp1Gateway("my-profile", remote)
        assertEquals("approved", gateway.requestToJoin("123456"))
        assertEquals("rpc/rp1_miflix_join_room", remote.calls.last().path)
        assertFalse(remote.calls.any { "watch_party_join" in it.path })
    }

    @Test
    fun partyChatIsTrimmedAndScopedToBrunioActor() = runBlocking {
        val remote = Fake()
        val gateway = BrunioRp1Gateway("my-profile", remote)
        gateway.sendEvent("123456", "chat", " Hello " + "x".repeat(900))
        val payload = remote.calls.last().jsonBody!!
        assertEquals("brunio-user-id", (payload["user_id"] as JsonPrimitive).content)
        assertEquals("123456", (payload["room_code"] as JsonPrimitive).content)
        assertEquals(400, (payload["body"] as JsonPrimitive).content.length)
    }

    @Test
    fun refusesUnapprovedEventTypesAndBadRoomCodes() = runBlocking {
        val remote = Fake()
        val gateway = BrunioRp1Gateway("profile", remote)
        assertFailsWith<IllegalArgumentException> { gateway.sendEvent("12abc3", "chat", "Hi") }
        assertFailsWith<IllegalArgumentException> { gateway.sendEvent("123456", "admin", "Hi") }
        assertTrue(remote.calls.isEmpty())
    }
}
