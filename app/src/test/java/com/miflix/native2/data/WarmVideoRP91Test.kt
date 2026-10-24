package com.miflix.native2.data

import org.junit.Assert.*
import org.junit.Test

class WarmVideoRP91Test {
    @Test fun warmBufferStopsOnEitherThreshold() {
        assertTrue(WarmVideoPolicyRP91.continueWarm(4_999_999,1000))
        assertFalse(WarmVideoPolicyRP91.continueWarm(5_000_000,1000))
        assertFalse(WarmVideoPolicyRP91.continueWarm(1000,16*1024*1024))
    }
    @Test fun handoffRequiresFreshEntryAndIdenticalResumePosition() {
        assertTrue(WarmVideoPolicyRP91.fresh(59_999,120_000,120_000))
        assertFalse(WarmVideoPolicyRP91.fresh(60_000,120_000,120_000))
        assertFalse(WarmVideoPolicyRP91.fresh(1000,120_000,0))
        assertFalse(WarmVideoPolicyRP91.fresh(-1,0,0))
    }
    @Test fun visibleOwnerDoesNotLoseNewPlayerOnOldDisposal() {
        val owner=PlayerReleaseRP87()
        var closes=0
        assertFalse(owner.hasActive())
        val old=owner.bind {closes++}
        owner.beforeReplace()
        val next=owner.bind {closes++}
        owner.dispose(old)
        assertTrue(owner.hasActive())
        assertEquals(1,closes)
        owner.dispose(next)
        assertFalse(owner.hasActive())
        assertEquals(2,closes)
    }
}
