package com.nuvio.app.features.brunio

import androidx.compose.ui.Modifier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class BrunioPlayerRouteTest {
    @Test
    fun movieRequestPreservesSourceHeadersAndResume() {
        val route = BrunioPlayerRoute(
            cloudId = "tmdb:movie:333",
            title = "Film",
            sourceUrl = "https://example.org/film.m3u8",
            providerName = "BruniO",
            streamTitle = "4K",
            sourceHeaders = mapOf("Referer" to "https://example.org"),
            initialPositionMs = 64_000,
        )
        val args = route.toNuvioArgs(0, onBack = { _ -> }, modifier = Modifier)
        assertEquals(64_000L, args.initialPositionMs)
        assertEquals("333", args.parentMetaId)
        assertEquals("movie", args.parentMetaType)
        assertEquals("https://example.org", args.sourceHeaders["Referer"])
    }

    @Test
    fun episodeRequestPreservesIdentity() {
        val route = BrunioPlayerRoute(
            cloudId = "tmdb:series:444",
            title = "Series",
            sourceUrl = "https://example.org/show.mkv",
            providerName = "BruniO",
            streamTitle = "Episode 2",
            season = 1, episode = 2,
        )
        val args = route.toNuvioArgs(0, onBack = { _ -> })
        assertEquals(1, args.seasonNumber)
        assertEquals(2, args.episodeNumber)
        assertEquals("tmdb:series:444:1:2", args.videoId)
    }

    @Test
    fun rejectsNonMediaAndHeaderInjection() {
        val invalid = BrunioPlayerRoute("bad:44", "Movie", "https://example.com/file", "BruniO", "HD")
        assertFailsWith<IllegalArgumentException> { invalid.validate() }
        val injection = BrunioPlayerRoute(
            "tmdb:movie:7", "Movie", "https://example.com/file", "BruniO", "HD",
            sourceHeaders = mapOf("User-Agent" to "browser\nmalicious"),
        )
        assertFailsWith<IllegalArgumentException> { injection.validate() }
    }
}
