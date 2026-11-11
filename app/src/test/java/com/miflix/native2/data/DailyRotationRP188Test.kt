package com.miflix.native2.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class DailyRotationRP188Test {
    @Test fun selectionIsStableForOneDateAndChangesForTheNextDay() {
        val a=DailyRotationPolicyRP188.candidates(2026,312)
        assertEquals(a,DailyRotationPolicyRP188.candidates(2026,312))
        assertNotEquals(a.map {it.title},DailyRotationPolicyRP188.candidates(2026,313).map {it.title})
        assertTrue(a.any {it.isMovie});assertTrue(a.any {!it.isMovie})
        assertEquals(a.size,a.map {it.isMovie to it.tmdbId}.distinct().size)
        assertTrue(a.size<=DailyRotationPolicyRP188.maxProbes)
    }
    @Test fun onlyVerifiedCandidatesReachTheRailAndCardCountIsBounded()=runBlocking {
        val candidates=DailyRotationPolicyRP188.candidates(2026,312)
        val expected=candidates.filterIndexed {index,_->index%2==0}.take(DailyRotationPolicyRP188.maxCards)
        val seen=mutableListOf<TirsoScheduleItemRP174>()
        val repo=DailyRotationRepositoryRP188()
        val result=repo.load("account:a:day:312",candidates,{it in expected},{seen.add(it)})
        assertEquals(expected,result);assertEquals(expected,seen)
    }
    @Test fun successfulResultsStopProbingWhenEightCardsAreReady()=runBlocking {
        var probes=0
        val rows=DailyRotationRepositoryRP188().load("key",DailyRotationPolicyRP188.candidates(2026,312),{probes++;true},{})
        assertEquals(8,rows.size);assertEquals(8,probes)
    }
    @Test fun emptyResultsAreCachedButExpireAndCanBeRetried()=runBlocking {
        var now=0L;var probes=0
        val repo=DailyRotationRepositoryRP188 {now}
        val rows=DailyRotationPolicyRP188.candidates(2026,312)
        val verify:suspend (TirsoScheduleItemRP174)->Boolean={probes++;false}
        repo.load("a",rows,verify,{});val first=probes
        assertTrue(first>0);repo.load("a",rows,verify,{});assertEquals(first,probes)
        now=300001;repo.load("a",rows,verify,{});assertEquals(first*2,probes)
        repo.invalidate("a");repo.load("a",rows,verify,{});assertEquals(first*3,probes)
    }
    @Test fun cachedSelectionsDoNotLeakAcrossAccountOrDayKeys()=runBlocking {
        val repo=DailyRotationRepositoryRP188();val rows=DailyRotationPolicyRP188.candidates(2026,312)
        assertTrue(repo.load("account-a:312",rows,{true},{}).isNotEmpty())
        assertTrue(repo.load("account-b:312",rows,{false},{}).isEmpty())
        assertTrue(repo.load("account-a:313",rows,{false},{}).isEmpty())
    }
    @Test fun cancellationNeverPublishesAnEmptyCachedSelection()=runBlocking {
        val repo=DailyRotationRepositoryRP188();val rows=DailyRotationPolicyRP188.candidates(2026,312)
        try {repo.load("a",rows,{throw CancellationException()},{});fail("Must cancel")} catch(_:CancellationException) {}
        assertTrue(repo.load("a",rows,{true},{}).isNotEmpty())
    }
    @Test fun dailyRotationIsAnExploreShortcutNotAHomeShelf() {
        val result=DailyRotationPolicyRP188.homeOrder(listOf("daily_rotation","streaming","genres","movies","series"))
        assertFalse(result.contains("daily_rotation"))
        assertEquals("continue",result.first())
        assertFalse(DailyRotationPolicyRP188.homeOrder(listOf("streaming")).contains("daily_rotation"))
    }
    @Test fun unseenDailyRotationExcludesPastMoviesAndSeriesAcrossDates() {
        val first=DailyRotationPolicyRP188.unseenCandidates(2026,315,emptySet())
        val chosen=first.take(6).map {DailyRotationPolicyRP188.identity(it)}.toSet()
        val future=DailyRotationPolicyRP188.unseenCandidates(2026,316,chosen)
        assertTrue(future.none {DailyRotationPolicyRP188.identity(it) in chosen})
        assertEquals(future.size,future.map {DailyRotationPolicyRP188.identity(it)}.distinct().size)
    }
    @Test fun exhaustedMovieCatalogIsNotRecycled() {
        val all=DailyRotationPolicyRP188.unseenCandidates(2026,315,emptySet(),"movie")
        val exhausted=all.map {DailyRotationPolicyRP188.identity(it)}.toSet()
        assertTrue(DailyRotationPolicyRP188.unseenCandidates(2026,316,exhausted,"movie").isEmpty())
    }
    @Test fun interruptedLoadingReusesVerifiedCardsAndResumesUncheckedSources()=runBlocking {
        val repo=DailyRotationRepositoryRP188();val rows=DailyRotationPolicyRP188.candidates(2026,312)
        val probes=mutableListOf<String>()
        try {repo.load("a",rows,{probes.add(it.id);true},{throw CancellationException()});fail("Must cancel")} catch(_:CancellationException) {}
        val found=mutableListOf<String>()
        val result=repo.load("a",rows,{probes.add(it.id);true},{found.add(it.id)})
        assertEquals(8,result.size);assertEquals(8,probes.size)
        assertEquals(probes.distinct(),probes);assertEquals(result.map {it.id},found)
    }
    @Test fun interruptionAfterEighthResultCannotPublishANinthCard()=runBlocking {
        val repo=DailyRotationRepositoryRP188();val rows=DailyRotationPolicyRP188.candidates(2026,312)
        var found=0
        try {repo.load("a",rows,{true},{if(++found==8) throw CancellationException()});fail("Must cancel")} catch(_:CancellationException) {}
        val result=repo.load("a",rows,{fail("Must not recheck");false},{})
        assertEquals(8,result.size)
    }
}
