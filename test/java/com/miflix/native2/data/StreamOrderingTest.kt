package com.miflix.native2.data

import com.miflix.native2.model.StreamChoice
import org.junit.Assert.*
import org.junit.Test

class StreamOrderingTest {
    private fun stream(id: String, title: String, bytes: Long=0)=StreamChoice("Comet", "https://example.test/$id",title,videoSize=bytes)
    @Test fun resolutionPrecedesSizeAndCodec() {
        val small4k=stream("4k","4K HEVC",100)
        val large1080=stream("1080","1080p AVC",9000)
        val unknown=stream("unknown","Unknown",99999)
        assertEquals(listOf(small4k,large1080,unknown),StreamOrdering.sorted(listOf(unknown,large1080,small4k)))
    }
    @Test fun sizeDescendingWithinResolutionWithStableTies() {
        val small=stream("small","1080p",100)
        val large=stream("large","1080p",200)
        val tie=stream("tie","1080p",200)
        assertEquals(listOf(large,tie,small),StreamOrdering.sorted(listOf(small,large,tie,small)))
    }
    @Test fun excludesUncachedTbDownloadsWithoutRemovingCachedTb() {
        val cached=stream("cached","[TB+] 2160p · cached")
        val download=stream("download","[TB Download] 2160p",999999)
        val punctuation=stream("punctuation","[TB+] Download · 1080p")
        assertEquals(listOf(cached),StreamOrdering.sorted(listOf(download,cached,punctuation)))
    }
    @Test fun parsesLabelsButPrefersMetadata() {
        assertEquals(2500000000L,StreamOrdering.bytes(stream("a","1080p · 2,5 GB")))
        assertEquals(1073741824L,StreamOrdering.bytes(stream("b","1 GiB")))
        assertEquals(100L,StreamOrdering.bytes(stream("c","10 GB",100)))
        assertEquals(2160,StreamOrdering.quality(stream("d","UHD · 1080 audio metadata")))
        assertEquals(4320,StreamOrdering.quality(stream("e","8K")))
        assertEquals(0L,StreamOrdering.bytes(stream("f","No size")))
    }
}
