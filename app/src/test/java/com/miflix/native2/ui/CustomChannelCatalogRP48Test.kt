package com.miflix.native2.ui

import org.junit.Assert.*
import org.junit.Test

class CustomChannelCatalogRP48Test {
    @Test fun personalCategoryUsesOthersName() {
        assertEquals("Others",CustomChannelsRP45.categoryRP48().name)
        assertEquals("custom-rp48",CustomChannelsRP45.categoryRP48().id)
    }
    @Test fun splitScreenRequestKeepsSourceIdentityAndHlsToken() {
        val url="https://example.com/live.m3u8?token=abc%2B123&x=2"
        val request=CustomChannelsRP45.request(CustomChannelRP45("unique-id","My channel",url))
        assertTrue(request.live)
        assertEquals(CustomChannelsRP45.provider,request.liveProvider)
        assertEquals("unique-id",request.liveChannelId)
        assertEquals(url,request.stream.url)
        assertEquals("application/x-mpegURL",request.liveMime)
    }
    @Test fun editedUrlCanChangeStreamFormatWithoutChangingChannelId() {
        val channel=CustomChannelRP45("same-id","My channel","https://example.com/live.m3u8")
        val replacement=CustomChannelsRP45.request(channel.copy(url="https://example.com/live.mpd"))
        assertEquals(channel.id,replacement.liveChannelId)
        assertEquals("application/dash+xml",replacement.liveMime)
    }
}
