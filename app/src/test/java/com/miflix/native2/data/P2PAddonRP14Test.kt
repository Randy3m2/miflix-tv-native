package com.miflix.native2.data

import com.miflix.native2.p2p.TorrentSpecRP14
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class P2PAddonRP14Test {
    @Test fun torrentsRequireOptInAndKeepFileIndexAndTracker()=runBlocking {
        val repo=StreamRepository("https://addon.test/manifest.json") {url,_ ->
            if(url.endsWith("manifest.json")) """{"name":"Test","resources":["stream"]}""" else
                """{"streams":[{"name":"Latino","title":"1080p Latino","infoHash":"0123456789abcdef0123456789abcdef01234567","fileIdx":2,"sources":["tracker:udp://tracker.test:80/announce"]},{"name":"English","url":"https://video.test/direct.mp4"}]}"""
        }
        assertEquals(1,repo.resolve("tt1","movie").size)
        repo.p2pEnabled=true
        val rows=repo.resolve("tt1","movie",preferredAudio="es")
        assertEquals(2,rows.size)
        val torrent=TorrentSpecRP14.parse(rows.first().url)!!
        assertEquals(2,torrent.fileIndex)
        assertEquals(listOf("udp://tracker.test:80/announce"),torrent.trackers)
        assertEquals("https://video.test/direct.mp4",rows.last().url)
    }
}
