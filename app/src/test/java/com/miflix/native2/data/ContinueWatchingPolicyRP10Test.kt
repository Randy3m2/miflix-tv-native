package com.miflix.native2.data

import com.miflix.native2.model.PlaybackProgress
import org.junit.Assert.*
import org.junit.Test

class ContinueWatchingPolicyRP10Test {
    private fun row(position:Long,duration:Long=7200000L,time:Long=1)=PlaybackProgress(
        position.toDouble()/duration*100.0,position,duration,time)
    @Test fun fiveSecondsQualifiesEvenBelowOnePercent() {
        assertFalse(ContinueWatchingPolicyRP10.eligible(row(4999)))
        assertTrue(ContinueWatchingPolicyRP10.eligible(row(5000)))
        assertTrue(row(5000).percent<1.0)
    }
    @Test fun finishedRemovedAndUnknownDurationStayOut() {
        assertFalse(ContinueWatchingPolicyRP10.eligible(row(96000,100000)))
        assertFalse(ContinueWatchingPolicyRP10.eligible(row(100000,100000)))
        assertFalse(ContinueWatchingPolicyRP10.eligible(PlaybackProgress(0.0,0,7200000)))
        assertFalse(ContinueWatchingPolicyRP10.eligible(PlaybackProgress(5.0,5000,0)))
        assertTrue(ContinueWatchingPolicyRP10.eligible(row(95000,100000)))
    }
    @Test fun onlyTitleRowsAppearAndNewestTenComeFirst() {
        val progress=(1..12).associate {"tmdb:movie:$it" to row(5000,time=it.toLong())}.toMutableMap()
        progress["tmdb:series:22:s1e1"]=row(5000,time=100)
        progress["live:5"]=row(5000,time=101)
        assertEquals((12 downTo 3).map {"tmdb:movie:$it"},ContinueWatchingPolicyRP10.ids(progress))
    }
    @Test fun seriesUsesItsLastEpisodeAggregateOnly() {
        val progress=mapOf("tmdb:series:1" to row(5000),
            "tmdb:series:1:s1e1" to row(5000))
        assertEquals(listOf("tmdb:series:1"),ContinueWatchingPolicyRP10.ids(progress))
    }
}
