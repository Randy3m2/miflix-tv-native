package com.nuvio.app.features.brunio

import com.nuvio.app.features.party.PartyMedia
import com.nuvio.app.features.profiles.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlin.random.Random

internal data class BrunioPartyRequest(val roomCode: String, val userId: String, val nickname: String)
internal data class BrunioPartyMessage(val id: Long, val sender: String, val kind: String, val body: String)
internal data class BrunioPartyView(
    val roomCode: String = "",
    val pendingCode: String = "",
    val host: Boolean = false,
    val busy: Boolean = false,
    val members: List<String> = emptyList(),
    val requests: List<BrunioPartyRequest> = emptyList(),
    val messages: List<BrunioPartyMessage> = emptyList(),
    val media: PartyMedia? = null,
    val error: String? = null,
)

/**
 * Actual BruniO RP1 join/approval/chat/read service.
 * The upstream Nuvio watch_party_* service is never called by this class.
 * The playback transport remains per-user: only title/state go into RP1.
 */
internal object BrunioPartyService {
    private val _state = MutableStateFlow(BrunioPartyView())
    val state: StateFlow<BrunioPartyView> = _state.asStateFlow()
    private var gateway: BrunioRp1Gateway? = null
    private var gatewayProfile: String? = null
    private var selfActor: String? = null
    private var roomHostId: String? = null

    private fun service(): BrunioRp1Gateway {
        val id = BrunioCloud.profileId(ProfileRepository.activeProfileId)
            ?: error("Select a BruniO profile first")
        if (id != gatewayProfile || gateway == null) {
            reset()
            gatewayProfile = id
            gateway = BrunioRp1Gateway(id, brunioPartyTransport())
        }
        return requireNotNull(gateway)
    }

    suspend fun createRoom(): String {
        val client = service()
        val (now, expires) = brunioPartyClock()
        val actor = client.actor()
        selfActor = actor
        // Generate 6-digit room code; retry if a room code is occupied.
        var lastError: Throwable? = null
        repeat(4) {
            val code = Random.nextInt(100000, 1000000).toString()
            try {
                client.createRoom(code, expires, now)
                roomHostId = actor
                _state.value = BrunioPartyView(roomCode = code, host = true)
                refresh()
                return code
            } catch (error: Throwable) {
                lastError = error
            }
        }
        error("BruniO Party could not create a free room")
    }

    suspend fun joinRoom(code: String): String {
        require(code.length == 6 && code.all(Char::isDigit))
        val client = service()
        val decision = client.requestToJoin(code)
        if (decision == "approved") {
            selfActor = client.actor()
            _state.value = BrunioPartyView(roomCode = code)
            refresh()
        } else {
            _state.value = BrunioPartyView(pendingCode = if (decision == "pending") code else "")
        }
        return decision
    }

    suspend fun refresh() {
        val current = _state.value
        val code = current.roomCode
        if (code.isEmpty()) return
        val client = service()
        val rooms = client.readRoom(code) as? JsonArray ?: JsonArray(emptyList())
        val room = rooms.firstOrNull() as? JsonObject ?: run {
            reset()
            return
        }
        val host = field(room, "host_user_id")
        roomHostId = host
        val members = (client.members(code) as? JsonArray).orEmpty()
            .mapNotNull { (it as? JsonObject)?.let { row -> field(row, "user_id") } }
        val requests = if (selfActor == host) {
            (client.accessRequests() as? JsonArray).orEmpty()
                .mapNotNull { it as? JsonObject }
                .filter { field(it, "room_code") == code }
                .mapNotNull {
                    val id = field(it, "user_id")
                    if (id.isBlank()) null else BrunioPartyRequest(code, id, field(it, "nickname"))
                }
        } else emptyList()
        val after = current.messages.maxOfOrNull { it.id }
        val updates = (client.events(code, after) as? JsonArray).orEmpty()
            .mapNotNull { it as? JsonObject }.mapNotNull { row ->
                val id = (row["id"] as? JsonPrimitive)?.longOrNull ?: return@mapNotNull null
                BrunioPartyMessage(id, field(row, "user_id"), field(row, "kind"), field(row, "body"))
            }
        val mediaState = room["state"] as? JsonObject
        _state.value = current.copy(
            host = selfActor != null && host == selfActor,
            members = members,
            requests = requests,
            messages = (current.messages + updates).distinctBy { it.id }.takeLast(100),
            media = mediaState?.let { BrunioPartyWire.readBrunioState(it)?.media },
            error = null,
        )
    }

    suspend fun approve(userId: String, approve: Boolean) {
        val code = state.value.roomCode
        check(code.isNotEmpty() && state.value.host)
        service().approveAccess(code, userId, approve)
        refresh()
    }

    suspend fun message(kind: String, body: String) {
        val code = state.value.roomCode
        check(code.isNotEmpty())
        service().sendEvent(code, kind, body)
        refresh()
    }

    suspend fun leave() {
        val current = state.value
        if (current.roomCode.isNotBlank()) {
            service().leave(current.roomCode, current.host)
        }
        reset()
    }

    fun reset() {
        gateway = null
        gatewayProfile = null
        selfActor = null
        roomHostId = null
        _state.value = BrunioPartyView()
    }

    private fun field(row: JsonObject, key: String): String =
        (row[key] as? JsonPrimitive)?.contentOrNull.orEmpty()
}
