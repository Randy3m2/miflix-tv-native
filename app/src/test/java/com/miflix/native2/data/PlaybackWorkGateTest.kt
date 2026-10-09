package com.miflix.native2.data

import org.junit.Assert.*
import org.junit.Test

class PlaybackWorkGateTest {
    @Test fun lateRoomAndLinkResultsStayInvalidAfterLeavingAndRejoining() {
        val gate=PlaybackWorkGate()
        val old=gate.begin()
        gate.invalidate()
        val joined=gate.begin()
        assertFalse(gate.current(old))
        assertTrue(gate.current(joined))
        gate.invalidate()
        assertFalse(gate.current(joined))
    }
    @Test fun newestSourceRequestOwnsTheResultAndBusyState() {
        val gate=PlaybackWorkGate()
        val first=gate.begin();val second=gate.begin()
        assertFalse(gate.current(first));assertTrue(gate.current(second))
    }
}
