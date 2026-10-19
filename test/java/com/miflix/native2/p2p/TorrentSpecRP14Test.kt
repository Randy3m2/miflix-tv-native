package com.miflix.native2.p2p

import org.junit.Assert.*
import org.junit.Test

class TorrentSpecRP14Test {
    private val hash="0123456789abcdef0123456789abcdef01234567"
    @Test fun uriRoundTripPreservesFileAndTrackers() {
        val s=TorrentSpecRP14.create(hash.uppercase(),7,listOf("udp://tracker.test:80/announce","https://tracker.test/announce?key=a&b=c"))!!
        assertEquals(s,TorrentSpecRP14.parse(s.uri()))
        assertEquals(hash,s.hash)
    }
    @Test fun missingIndexSelectsVideoInsteadOfAssumingFirstFile() {
        val s=TorrentSpecRP14.create(hash,null,emptyList())!!
        assertNull(TorrentSpecRP14.parse(s.uri())!!.fileIndex)
    }
    @Test fun malformedHashAndNegativeIndexAreRejected() {
        assertNull(TorrentSpecRP14.create("invalid",0,emptyList()))
        assertNull(TorrentSpecRP14.create(hash,-2,emptyList()))
        assertNull(TorrentSpecRP14.parse("brunio-p2p://$hash/-2"))
    }
    @Test fun magnetCarriesTrackersAndExplicitIndex() {
        val original=TorrentSpecRP14.create(hash,3,listOf("https://tracker.test/announce"))!!
        assertEquals(original,TorrentSpecRP14.fromMagnet(original.magnet(),3))
    }
    @Test fun trackerListIsDeduplicatedAndRestricted() {
        val s=TorrentSpecRP14.create(hash,0,listOf("file:///etc/passwd","https://user:password@tracker.test/","https://tracker.test/a","https://tracker.test/a"))!!
        assertEquals(listOf("https://tracker.test/a"),s.trackers)
    }
    @Test fun invalidExternalUrisDoNotBecomeTorrents() {
        assertNull(TorrentSpecRP14.parse("https://example.test/movie.mp4"))
        assertNull(TorrentSpecRP14.fromMagnet("magnet:?xt=urn:btih:invalid"))
    }
}
