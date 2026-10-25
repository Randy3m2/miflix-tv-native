package com.miflix.native2.data

import kotlinx.coroutines.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class F1StatsRP131Test {
    private fun fixture()=checkNotNull(javaClass.classLoader?.getResourceAsStream("f1_scoreboard_rp131.json")).bufferedReader().use {it.readText()}
    private fun json()=JSONObject(fixture())
    @Test fun realScoreboardHasFiveSessionsAndTwentyTwoDrivers() {
        val rows=F1StatsRP131.parse(json())
        assertEquals(5,rows.size)
        assertTrue(rows.all {it.drivers.size==22 && it.final && !it.live})
        assertEquals("Race",F1StatsRP131.automatic(rows)?.type)
        assertTrue(rows.last().drivers.all {it.gap=="—" && it.lap=="—"})
    }
    @Test fun completedCannotBeMislabelledLiveEvenIfProviderStateContradictsIt() {
        val j=json();val type=j.getJSONArray("events").getJSONObject(0).getJSONArray("competitions").getJSONObject(0).getJSONObject("status").getJSONObject("type")
        type.put("state","in")
        val session=F1StatsRP131.parse(j).first()
        assertTrue(session.final);assertFalse(session.live)
    }
    @Test fun liveSessionWinsOverNewerCompletedSessionWithoutChangingManualIds() {
        val rows=F1StatsRP131.parse(json())
        val live=rows.first().copy(state="in")
        assertEquals(live.id,F1StatsRP131.automatic(listOf(live)+rows.drop(1))?.id)
        assertEquals(rows.last().id,rows.first {it.id==rows.last().id}.id)
    }
    @Test fun scheduledOrderIsNotPresentedAsRacePosition() {
        val j=json();val type=j.getJSONArray("events").getJSONObject(0).getJSONArray("competitions").getJSONObject(0).getJSONObject("status").getJSONObject("type")
        type.put("completed",false).put("state","pre")
        val row=F1StatsRP131.parse(j).first()
        assertTrue(row.drivers.all {it.position=="—"});assertFalse(row.live)
    }
    @Test fun recognisesCommonChannelsWithoutTreatingFs1AsF1() {
        assertTrue(F1StatsRP131.channel("sports","SKY SPORTS F1"))
        assertTrue(F1StatsRP131.channel("iptv","DAZN F1 HD"))
        assertTrue(F1StatsRP131.channel("tv","Movistar Fórmula 1"))
        assertFalse(F1StatsRP131.channel("tv","FS1 USA"))
        assertFalse(F1StatsRP131.channel("sports","NBA"))
    }
    @Test fun simultaneousPanelsShareTenSecondCacheAndPreserveLastFrameOnFailure()=runBlocking {
        var calls=0;var time=0L;var fail=false
        val repo=F1StatsRP131(fetch={calls++;delay(5);if(fail) error("offline");fixture()},now={time})
        coroutineScope {listOf(async {repo.load()},async {repo.load()}).awaitAll()}
        assertEquals(1,calls)
        val old=repo.cached()
        time=10_000;fail=true
        try {repo.load();fail("Expected failure")} catch(_:IllegalStateException) {}
        assertSame(old,repo.cached())
    }
    @Test fun cancelledFetchCannotPublishAFrame()=runBlocking {
        val entered=CompletableDeferred<Unit>()
        val repo=F1StatsRP131(fetch={entered.complete(Unit);awaitCancellation()})
        val job=launch {repo.load()};entered.await();job.cancelAndJoin()
        assertNull(repo.cached())
    }
    @Test fun missingAndMalformedSessionsAreSafe() {
        assertTrue(F1StatsRP131.parse(JSONObject()).isEmpty())
        val j=json();j.getJSONArray("events").getJSONObject(0).getJSONArray("competitions").getJSONObject(0).put("id","../bad")
        assertEquals(4,F1StatsRP131.parse(j).size)
    }
}
