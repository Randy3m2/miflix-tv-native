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
    @Test fun spanishAudioPrecedesEnglishEvenAtLowerResolution() {
        val english=stream("en","2160p English",9000)
        val latin=stream("lat","720p Latino",100)
        val spain=stream("es","1080p Castellano",200)
        assertEquals(listOf(spain,latin,english),StreamOrdering.sorted(listOf(english,latin,spain),"es"))
    }
    @Test fun englishFallbackPrecedesUnknownAndOtherAudio() {
        val french=stream("fr","2160p French",9000)
        val unknown=stream("unknown","4K MULTI",100)
        val english=stream("en","720p ENG",200)
        assertEquals(listOf(english,unknown,french),StreamOrdering.sorted(listOf(french,unknown,english),"es"))
    }
    @Test fun subtitlesDoNotClaimSpanishAudio() {
        val subOnly=stream("subs","2160p\nSpanish subtitles").copy(subtitles=listOf(com.miflix.native2.model.SubtitleChoice("https://test/sub.srt","es","Español")))
        val english=stream("en","720p English")
        assertEquals(2,StreamOrdering.languageRank(subOnly))
        assertEquals(listOf(english,subOnly),StreamOrdering.sorted(listOf(subOnly,english),"es"))
    }
    @Test fun explicitAudioMetadataAndSpanishRegionsAreRecognized() {
        val spanish=stream("es","720p").copy(audioLanguages=listOf("es-419"))
        val english=stream("en","2160p").copy(audioLanguages=listOf("en"))
        assertEquals(listOf(spanish,english),StreamOrdering.sorted(listOf(english,spanish),"es-ES"))
        assertEquals(listOf(english,spanish),StreamOrdering.sorted(listOf(english,spanish)))
    }
    @Test fun duplicateUrlKeepsSpanishEvidence() {
        val english=stream("same","2160p English")
        val spanish=stream("same","1080p Castellano")
        assertEquals(listOf(spanish),StreamOrdering.sorted(listOf(english,spanish),"es"))
    }

    @Test fun englishPlaybackHasFiveDistinctFallbackCandidates() {
        val rows=(1..7).map {stream("en$it","1080p English",it.toLong())}
        val result=StreamOrdering.fallbackCandidates(rows+rows,"en")
        assertEquals(rows.reversed().take(5),result)
        assertEquals(5,result.map {it.url}.distinct().size)
    }
    @Test fun spanishFallbackUsesRemainingSlotsForEnglish() {
        val spanish=stream("es","720p Latino")
        val english=(1..6).map {stream("en$it","1080p English",it.toLong())}
        assertEquals(listOf(spanish)+english.reversed().take(4),StreamOrdering.fallbackCandidates(english+spanish,"es"))
    }
    @Test fun spanishFallbackKeepsThreeSpanishBeforeEnglish() {
        val spanish=(1..5).map {stream("es$it","720p Castellano",it.toLong())}
        val english=(1..5).map {stream("en$it","1080p English",it.toLong())}
        assertEquals(spanish.reversed().take(3)+english.reversed().take(2),StreamOrdering.fallbackCandidates(english+spanish,"es"))
    }

}
