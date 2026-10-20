package com.miflix.native2.data

import org.junit.Assert.*
import org.junit.Test

class CustomChannelUrlRP45Test {
    @Test fun acceptsTokenizedStreamWithoutRequiringFileExtension() {
        assertTrue(CustomChannelServerRP45.valid("Mi canal", "https://example.com/live?token=a%2Bb%26c&part=2"))
        assertTrue(CustomChannelServerRP45.valid("Mi canal", "http://192.168.1.4/live.m3u8"))
    }
    @Test fun rejectsNonNetworkSchemesAndCredentials() {
        for(url in listOf("javascript:alert(1)","file:///video.mp4","https://name:password@example.com/live","https:///missing-host")) {
            assertFalse(url,CustomChannelServerRP45.valid("Canal",url))
        }
    }
    @Test fun rejectsMalformedOrOversizedInput() {
        assertFalse(CustomChannelServerRP45.valid(" ","https://example.com/live"))
        assertFalse(CustomChannelServerRP45.valid("Canal\nOtro","https://example.com/live"))
        assertFalse(CustomChannelServerRP45.valid("x".repeat(81),"https://example.com/live"))
        assertFalse(CustomChannelServerRP45.valid("Canal","https://example.com/"+"a".repeat(8192)))
        assertFalse(CustomChannelServerRP45.valid("Canal","https://example.com/live?invalid space"))
    }
}
