package com.miflix.native2.data

import org.junit.Assert.*
import org.junit.Test

class GeminiAccessRP159Test {
    @Test fun recentRequestHasSpecificCountdownInsteadOfFailedRequirement() {
        val failure=GeminiAccessRP159.check(1,10000,15200)!!
        assertEquals(GeminiErrorRP159.COOLDOWN,failure.reason);assertEquals(15,failure.seconds)
        assertFalse(GeminiAccessRP159.message(failure,false).contains("Failed requirement"))
        assertEquals(14800,GeminiAccessRP159.waitMillis(10000,15200).toInt())
        assertNull(GeminiAccessRP159.check(1,10000,30000))
    }
    @Test fun dailyLocalLimitDoesNotMasqueradeAsProviderQuotaOrCooldown() {
        val failure=GeminiAccessRP159.check(20,10000,11000)!!
        assertEquals(GeminiErrorRP159.DAILY_LIMIT,failure.reason)
        assertTrue(GeminiAccessRP159.message(failure,true).contains("local"))
        assertTrue(GeminiAccessRP159.message(failure,false).contains("tomorrow"))
        assertEquals(GeminiErrorRP159.DAILY_LIMIT,GeminiAccessRP159.check(20,10000,11000,true)!!.reason)
    }
    @Test fun linkedFollowupCannotBypassDailyLimit() {
        assertNull(GeminiAccessRP159.check(19,10000,11000,true))
        assertEquals(GeminiErrorRP159.DAILY_LIMIT,GeminiAccessRP159.check(20,10000,11000,true)!!.reason)
    }
    @Test fun initialRequestAndClockResetDoNotWait() {
        assertNull(GeminiAccessRP159.check(0,0,1000))
        assertNull(GeminiAccessRP159.check(1,900000,5000))
    }
    @Test fun providerErrorsAreActionableAndDoNotEchoPrivateBodies() {
        val secret="private key or request text must not leak"
        val auth=GeminiAccessRP159.provider(HttpFailure(403,secret))
        val quota=GeminiAccessRP159.provider(HttpFailure(429,secret))
        assertEquals(GeminiErrorRP159.AUTH,auth.reason)
        assertEquals(GeminiErrorRP159.PROVIDER_LIMIT,quota.reason)
        assertFalse(GeminiAccessRP159.message(auth,false).contains(secret))
        assertTrue(GeminiAccessRP159.message(quota,true).contains("429"))
        assertEquals(GeminiErrorRP159.NETWORK,GeminiAccessRP159.provider(java.net.SocketTimeoutException()).reason)
    }
}
