package com.miflix.native2.player

import org.junit.Assert.*
import org.junit.Test

class HeadphoneMixTest {
    @Test fun speakerRouteRetainsEveryChannel() {
        for (channels in 1..8) {
            val mix = headphoneMixCoefficients(channels, false)
            assertEquals(channels * channels, mix.size)
            for (i in 0 until channels) for (j in 0 until channels)
                assertEquals(if (i == j) 1f else 0f, mix[i * channels + j], 0f)
        }
    }
    @Test fun headphoneMixPreservesDialogueAndIncludesAllChannelsWithoutPeakClipping() {
        for (channels in 3..8) {
            val mix = headphoneMixCoefficients(channels, true)
            assertEquals(channels * 2, mix.size)
            for (i in 0 until channels) assertTrue(mix[i * 2] + mix[i * 2 + 1] > 0f)
            for (ear in 0..1) {
                var sum = 0f
                for (i in 0 until channels) sum += mix[i * 2 + ear]
                assertTrue(sum <= 1.00001f)
            }
            if (channels != 4) { assertTrue(mix[4] > 0f); assertTrue(mix[5] > 0f) }
        }
    }
    @Test fun quadRearChannelsStayOnTheirOwnSide() {
        val mix = headphoneMixCoefficients(4, true)
        assertTrue(mix[4] > 0f); assertEquals(0f, mix[5], 0f)
        assertEquals(0f, mix[6], 0f); assertTrue(mix[7] > 0f)
    }
    @Test fun monoAndStereoStayUntouched() {
        assertArrayEquals(floatArrayOf(1f), headphoneMixCoefficients(1, true), 0f)
        assertArrayEquals(floatArrayOf(1f,0f,0f,1f), headphoneMixCoefficients(2, true), 0f)
    }
}
