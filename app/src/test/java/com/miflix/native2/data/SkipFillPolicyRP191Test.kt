package com.miflix.native2.data

import org.junit.Assert.*
import org.junit.Test

class SkipFillPolicyRP191Test {
    @Test fun introFillsAcrossItsOwnInterval() {
        assertEquals(0f,SkipFillPolicyRP191.fraction(10_000,10_000,90_000),0f)
        assertEquals(.5f,SkipFillPolicyRP191.fraction(50_000,10_000,90_000),.0001f)
        assertEquals(1f,SkipFillPolicyRP191.fraction(90_000,10_000,90_000),0f)
    }
    @Test fun creditsFillUntilTheEffectiveEpisodeEnd() {
        assertEquals(.75f,SkipFillPolicyRP191.fraction(1_350_000,1_200_000,1_400_000),.0001f)
    }
    @Test fun bufferingOrPauseCannotAdvanceFillWithoutPlaybackTime() {
        repeat(20) {assertEquals(.25f,SkipFillPolicyRP191.fraction(30_000,10_000,90_000),.0001f)}
    }
    @Test fun backwardSeekReducesFillAndForwardSeekIncreasesIt() {
        assertTrue(SkipFillPolicyRP191.fraction(30_000,10_000,90_000)<SkipFillPolicyRP191.fraction(70_000,10_000,90_000))
    }
    @Test fun outOfBoundsPositionsAndInvalidMarkersAreSafe() {
        assertEquals(0f,SkipFillPolicyRP191.fraction(0,10,20),0f)
        assertEquals(1f,SkipFillPolicyRP191.fraction(30,10,20),0f)
        assertEquals(0f,SkipFillPolicyRP191.fraction(10,20,20),0f)
        assertEquals(0f,SkipFillPolicyRP191.fraction(10,-1,20),0f)
        assertEquals(0f,SkipFillPolicyRP191.fraction(10,20,10),0f)
    }
}
