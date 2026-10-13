package com.miflix.native2.data
import com.miflix.native2.model.CloudSession
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
class SessionRenewalTest {
    private fun session(token: String="expired",owner: String="a")=CloudSession(token,"refresh-$token",owner,"test@example.org")
    @Test fun concurrentRequestsRotateOnce()=runBlocking {
        var current:CloudSession?=session();var calls=0
        val renewal=SessionRenewal({current},{current=it},{it.accessToken=="expired"}) { calls++;delay(20);session("fresh") }
        val tokens=(1..30).map { async { renewal.valid("a").accessToken } }.awaitAll()
        assertEquals(1,calls);assertTrue(tokens.all { it=="fresh" })
    }
    @Test fun simultaneousUnauthorizedResponsesReuseNewToken()=runBlocking {
        var current:CloudSession?=session("old");var calls=0
        val renewal=SessionRenewal({current},{current=it},{false}) { calls++;delay(20);session("fresh") }
        (1..20).map { async { renewal.valid("a","old") } }.awaitAll()
        assertEquals(1,calls);assertEquals("fresh",current!!.accessToken)
    }
    @Test fun logoutDuringRefreshCannotRestoreOldAccount()=runBlocking {
        var current:CloudSession?=session();var saved=false
        val renewal=SessionRenewal({current},{saved=true;current=it},{true}) { current=session("other","b");session("fresh") }
        try { renewal.valid("a");fail("Must cancel stale refresh") } catch(_:CancellationException) { }
        assertFalse(saved);assertEquals("b",current!!.userId)
    }
    @Test fun rejectedRefreshPreservesStoredSession()=runBlocking {
        var current:CloudSession?=session()
        val renewal=SessionRenewal({current},{current=it},{true}) { error("offline") }
        try { renewal.valid("a");fail() } catch(_:IllegalStateException) { }
        assertEquals("expired",current!!.accessToken)
    }
    @Test fun transientFailureWaitsBeforeRetryAndIncreasesDelay()=runBlocking {
        var current:CloudSession?=session();var calls=0;var now=0L
        val renewal=SessionRenewal({current},{current=it},{true}) { calls++;error("offline") }
        renewal.nowMs={ now }
        repeat(20) { try { renewal.valid("a") } catch(_:IllegalStateException) { } }
        assertEquals(1,calls)
        now=14_999;try { renewal.valid("a") } catch(_:IllegalStateException) { };assertEquals(1,calls)
        now=15_000;try { renewal.valid("a") } catch(_:IllegalStateException) { };assertEquals(2,calls)
        now=44_999;try { renewal.valid("a") } catch(_:IllegalStateException) { };assertEquals(2,calls)
        now=45_000;try { renewal.valid("a") } catch(_:IllegalStateException) { };assertEquals(3,calls)
    }
    @Test fun invalidCredentialRefreshesOnlyOnceUntilNewSignIn()=runBlocking {
        var current:CloudSession?=session();var calls=0
        val renewal=SessionRenewal({current},{current=it},{true}) {
            calls++
            if(it=="refresh-expired") throw HttpFailure(400,"Invalid refresh token")
            session("fresh")
        }
        repeat(20) { try { renewal.valid("a") } catch(_:SessionSignInRequired) { } }
        assertEquals(1,calls)
        current=session("new-login")
        assertEquals("fresh",renewal.valid("a").accessToken)
        assertEquals(2,calls)
    }
    @Test fun cancelledRefreshDoesNotStartCooldown()=runBlocking {
        var current:CloudSession?=session();var calls=0
        val renewal=SessionRenewal({current},{current=it},{true}) {
            calls++;if(calls==1) throw CancellationException("Cancelled");session("fresh")
        }
        try { renewal.valid("a") } catch(_:CancellationException) { }
        assertEquals("fresh",renewal.valid("a").accessToken)
        assertEquals(2,calls)
    }

}
