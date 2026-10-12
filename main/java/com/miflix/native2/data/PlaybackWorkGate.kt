package com.miflix.native2.data

/** Main-thread work generations prevent late network responses from restoring a departed session. */
internal class PlaybackWorkGate {
    data class Ticket(val generation: Long, val sequence: Long)
    var generation: Long = 0
        private set
    private var sequence = 0L
    fun begin(): Ticket = Ticket(generation, ++sequence)
    fun current(ticket: Ticket): Boolean = ticket.generation == generation && ticket.sequence == sequence
    fun invalidate() { generation++; sequence++ }
}
