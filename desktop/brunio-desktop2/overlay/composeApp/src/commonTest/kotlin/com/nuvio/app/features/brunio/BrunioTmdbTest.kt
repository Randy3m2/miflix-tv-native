package com.nuvio.app.features.brunio

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BrunioTmdbTest {
    @Test
    fun tmdbItemsHaveCanonicalBrunioIdsAndUsableArt() {
        val input = """{"results":[
          {"id":42,"media_type":"movie","title":"A Movie",
            "poster_path":"/p.jpg","backdrop_path":"/b.jpg","release_date":"2025-11-03"},
          {"id":17,"media_type":"tv","name":"A Series","first_air_date":"2026-03-01"},
          {"id":23,"media_type":"person","name":"Actor"},
          {"id":42,"media_type":"movie","title":"A Movie"}
        ]}"""
        val output = BrunioTmdb.decodeResults(input)
        assertEquals(2, output.size)
        assertEquals("tmdb:movie:42", output[0].id)
        assertEquals("movie", output[0].type)
        assertEquals("https://image.tmdb.org/t/p/w500/p.jpg", output[0].poster)
        assertEquals("https://image.tmdb.org/t/p/w1280/b.jpg", output[0].banner)
        assertEquals("tmdb:series:17", output[1].id)
        assertEquals("series", output[1].type)
        assertTrue(BrunioTmdb.sections("Tendencias", output).all { it.items.isNotEmpty() })
    }
}
