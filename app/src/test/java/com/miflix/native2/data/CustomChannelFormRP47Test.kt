package com.miflix.native2.data

import org.junit.Assert.*
import org.junit.Test

class CustomChannelFormRP47Test {
    private val base="http://192.168.1.31:12345"
    private val secret="test-only-form-secret"
    @Test fun allowsChromeNoReferrerFormWithValidSecret() {
        for(origin in listOf(null,"","null",base)) {
            assertTrue(CustomChannelServerRP45.acceptsForm(base,origin,secret,secret))
        }
    }
    @Test fun neverAcceptsMissingOrIncorrectFormSecret() {
        for(origin in listOf(null,"","null",base)) {
            assertFalse(CustomChannelServerRP45.acceptsForm(base,origin,secret,""))
            assertFalse(CustomChannelServerRP45.acceptsForm(base,origin,secret,"wrong"))
        }
        assertFalse(CustomChannelServerRP45.acceptsForm(base,base,"",""))
    }
    @Test fun rejectsOtherOriginsEvenWithValidSecret() {
        assertFalse(CustomChannelServerRP45.acceptsForm(base,"https://other.example",secret,secret))
        assertFalse(CustomChannelServerRP45.acceptsForm(base,"http://192.168.1.31:54321",secret,secret))
    }
}
