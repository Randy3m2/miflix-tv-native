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
}
