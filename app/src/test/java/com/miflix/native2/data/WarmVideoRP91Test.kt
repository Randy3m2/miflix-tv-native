package com.miflix.native2.data

import org.junit.Assert.*
import org.junit.Test

class WarmVideoRP91Test {
    @Test fun warmBufferStopsOnEitherThreshold() {
        assertTrue(WarmVideoPolicyRP91.continueWarm(14_999_999,1000))
        assertFalse(WarmVideoPolicyRP91.continueWarm(15_000_000,1000))
        assertFalse(WarmVideoPolicyRP91.continueWarm(1000,32*1024*1024))
    }
    @Test fun lowerRamTvKeepsThePreviousMemoryBudget() {
        assertEquals(16*1024*1024,WarmVideoPolicyRP91.bufferBytes(true))
        assertEquals(32*1024*1024,WarmVideoPolicyRP91.bufferBytes(false))
        assertFalse(WarmVideoPolicyRP91.continueWarm(1_000_000,16*1024*1024,true))
        assertTrue(WarmVideoPolicyRP91.continueWarm(1_000_000,16*1024*1024,false))
        assertFalse(WarmVideoPolicyRP91.continueWarm(15_000_000,1000,true))
        assertEquals(250L,DetailsPrefetchRP90.DELAY_MS)
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
