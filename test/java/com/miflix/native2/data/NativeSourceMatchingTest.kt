package com.miflix.native2.data

import org.junit.Assert.*
import org.junit.Test

class NativeSourceMatchingTest {
    @Test fun matchesExactEpisodeNotSimilarNumbersOrMultipleEpisodes() {
        assertTrue(NativeSourceMatching.episodeMatches("Show/Show.S01E02.1080p.mkv",1,2))
        assertTrue(NativeSourceMatching.episodeMatches("Show 1x02.mkv",1,2))
        assertFalse(NativeSourceMatching.episodeMatches("Show.S01E20.mkv",1,2))
        assertFalse(NativeSourceMatching.episodeMatches("Show.S02E02.mkv",1,2))
        assertFalse(NativeSourceMatching.episodeMatches("Show.S01E02E03.mkv",1,2))
        assertFalse(NativeSourceMatching.episodeMatches("Show.S01E02-03.mkv",1,2))
        assertFalse(NativeSourceMatching.episodeMatches("Show.S01/02.mkv",1,2))
    }
    @Test fun titleBoundaryAndSamplesAreRespected() {
        assertTrue(NativeSourceMatching.titleMatches("[GROUP] Alien.1979.1080p",listOf("Alien")))
        assertFalse(NativeSourceMatching.titleMatches("Aliens.1986.1080p",listOf("Alien")))
        assertFalse(NativeSourceMatching.titleMatches("The.Alien.1979",listOf("Alien")))
        assertTrue(NativeSourceMatching.video("Example.1080p.mkv"))
        assertFalse(NativeSourceMatching.video("Example.sample.mkv"))
        assertFalse(NativeSourceMatching.video("Example.mkv.exe"))
    }
}
