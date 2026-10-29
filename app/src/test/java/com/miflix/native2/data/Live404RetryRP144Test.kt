package com.miflix.native2.data

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class Live404RetryRP144Test {
    @Test fun budgetBacksOffAndStopsAfterFourRetries() {
        val b=Live404RetryBudgetRP144()
        assertEquals(listOf(1000L,2000L,4000L,8000L),List(4) {b.nextDelay()})
        repeat(4) {assertNull(b.nextDelay())}
    }
    @Test fun scopeExcludesVodRecordingsSportsAndYoutube() {
        for(provider in listOf("lidom","nauta","iptv","custom")) assertTrue(Live404RetryBudgetRP144.eligible(true,false,provider,"tv"))
        assertFalse(Live404RetryBudgetRP144.eligible(false,false,"lidom","tv"))
        assertFalse(Live404RetryBudgetRP144.eligible(true,true,"lidom","tv"))
        assertFalse(Live404RetryBudgetRP144.eligible(true,false,"sports","tv"))
        assertFalse(Live404RetryBudgetRP144.eligible(true,false,"lidom","youtube"))
    }
    @Test fun authorizationCodecAndOtherHttpFailuresDoNotRetry()=runBlocking {
        var calls=0
        val s=Live404RetrySessionRP144(this,true,{true},{true},{false},{calls++},{},{},wait={})
        for(code in listOf(null,401,403,410,500)) assertFalse(s.offer(Exception(),code))
        yield();assertEquals(0,calls);s.close()
    }
    @Test fun duplicateErrorsScheduleOnlyOneReload()=runBlocking {
        val gate=CompletableDeferred<Unit>();var calls=0
        val s=Live404RetrySessionRP144(this,true,{true},{true},{false},{calls++},{},{},wait={gate.await()})
        assertTrue(s.offer(Exception(),404));assertTrue(s.offer(Exception(),404))
        yield();gate.complete(Unit);yield();assertEquals(1,calls);s.close()
    }
    @Test fun removedOwnerNeverReloadsAfterItsDelay()=runBlocking {
        val gate=CompletableDeferred<Unit>();var current=true;var calls=0
        val s=Live404RetrySessionRP144(this,true,{current},{true},{false},{calls++},{},{},wait={gate.await()})
        s.offer(Exception(),404);yield();current=false;gate.complete(Unit);yield()
        assertEquals(0,calls);s.close()
    }
    @Test fun disposedSessionCancelsPendingWork()=runBlocking {
        val gate=CompletableDeferred<Unit>();var calls=0
        val s=Live404RetrySessionRP144(this,true,{true},{true},{false},{calls++},{},{},wait={gate.await()})
        s.offer(Exception(),404);yield();s.close();gate.complete(Unit);yield()
        assertEquals(0,calls);assertFalse(s.offer(Exception(),404))
    }
    @Test fun pauseOrBackgroundPreventsAutoplay()=runBlocking {
        val gate=CompletableDeferred<Unit>();var allowed=true;var calls=0;var failures=0
        val s=Live404RetrySessionRP144(this,true,{true},{allowed},{false},{calls++},{},{failures++},wait={gate.await()})
        s.offer(Exception(),404);yield();allowed=false;gate.complete(Unit);yield()
        assertEquals(0,calls);assertEquals(1,failures);s.close()
    }
    @Test fun shortPlaybackDoesNotResetBudgetButStablePlaybackDoes()=runBlocking {
        var stableGate=CompletableDeferred<Unit>()
        val s=Live404RetrySessionRP144(this,true,{true},{true},{true},{},{},{},wait={if(it==Live404RetryBudgetRP144.STABLE_MS) stableGate.await()})
        repeat(4) {assertTrue(s.offer(Exception(),404));yield()}
        s.onPlaying(true);yield();s.onPlaying(false);stableGate.complete(Unit);yield()
        assertFalse(s.offer(Exception(),404))
        stableGate=CompletableDeferred();s.onPlaying(true);yield();stableGate.complete(Unit);yield()
        assertTrue(s.offer(Exception(),404));yield();s.close()
    }
    @Test fun reloadExceptionBecomesNormalFailureInsteadOfCrashing()=runBlocking {
        var failures=0;val original=Exception("HTTP 404")
        val s=Live404RetrySessionRP144(this,true,{true},{true},{false},{error("released")},{},{assertSame(original,it);failures++},wait={})
        assertTrue(s.offer(original,404));yield();assertEquals(1,failures);s.close()
    }
}
