package com.miflix.native2.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class P2PAddonRP14Test {
    @Test fun torrentRowsAreIgnoredAndDirectLinksRemain()=runBlocking {
        val repo=StreamRepository("https://addon.test/manifest.json") {url,_ ->
            if(url.endsWith("manifest.json")) """{"name":"Test","resources":["stream"]}""" else
                """{"streams":[{"infoHash":"0123456789abcdef0123456789abcdef01234567"},{"url":"magnet:?xt=urn:btih:0123456789abcdef0123456789abcdef01234567"},{"url":"https://video.test/direct.mp4"}]}"""
        }
        assertEquals(listOf("https://video.test/direct.mp4"),repo.resolve("tt1","movie").map {it.url})
        assertTrue(repo.status(repo.manifests().first()).contains("Torrents ignored: 2"))
    }
}
