package com.miflix.native2.player

import org.junit.Assert.*
import org.junit.Test

class SubtitleAutoSyncRP206Test {
    @Test fun parsesSrtAndWebVttTimeCodes() {
        val cues=SubtitleAutoSyncMathRP206.parse("""
1
00:00:10,100 --> 00:00:11,700
Hello

00:00:14.250 --> 00:00:16.100
World

00:01:17.000 --> 00:01:19.000
Next
""".trimIndent())
        assertEquals(3,cues.size)
        assertEquals(10100L,cues.first().startMs)
        assertEquals(16100L,cues[1].endMs)
        assertEquals(77000L,cues[2].startMs)
    }
    @Test fun invalidAndHugeCuesAreRejected() {
        val input="""
00:00:20,000 --> 00:00:19,000
Invalid
00:00:21,000 --> 00:00:50,000
Too long
""".trimIndent()
        assertTrue(SubtitleAutoSyncMathRP206.parse(input).isEmpty())
    }
    @Test fun noAudioOrNoMatchNeverChangesSubtitleTiming() {
        val cues=listOf(
            SubtitleCueRP206(1000,2000),SubtitleCueRP206(3000,4000),
            SubtitleCueRP206(5000,6000),SubtitleCueRP206(7000,8000),
            SubtitleCueRP206(9000,10000)
        )
        assertNull(SubtitleAutoSyncMathRP206.estimate(cues,emptyMap(),1500))
        assertNull(SubtitleAutoSyncMathRP206.estimate(cues,(0L..130L).associateWith {0.0},1500))
    }
}
