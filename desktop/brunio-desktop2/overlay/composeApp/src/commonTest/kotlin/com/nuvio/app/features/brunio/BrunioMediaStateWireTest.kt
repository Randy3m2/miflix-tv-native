package com.nuvio.app.features.brunio

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BrunioMediaStateWireTest {
    @Test
    fun readsBrunioFavoritesWithStableTmdbIds() {
        val state = Json.parseToJsonElement(
            """{"favorites":["tmdb:movie:88","tmdb:series:123","tmdb:series:123","bad"]}"""
        ).jsonObject
        val items = BrunioMediaStateWire.favoriteItems(state)
        assertEquals(2, items.size)
        assertEquals("tmdb:movie:88", items[0].id)
        assertEquals("movie", items[0].type)
        assertEquals(123, items[1].tmdbId)
    }

    @Test
    fun secondsAreConvertedToMillisecondsAndEpisodeIsNotDuplicated() {
        val state = Json.parseToJsonElement(
            """{"progress":{
                "tmdb:series:123":{"position":110.5,"duration":1000,"percent":11.05,"updatedAt":100,"season":1,"episode":3},
                "tmdb:series:123:s1e3":{"position":110.5,"duration":1000,"percent":11.05,"updatedAt":100,"season":1,"episode":3},
                "tmdb:movie:88":{"position":960,"duration":1000,"percent":96,"updatedAt":200}
            }}"""
        ).jsonObject
        val entries = BrunioMediaStateWire.progressEntries(state)
        assertEquals(2, entries.size)
        val series = entries.first { it.contentType == "series" }
        assertEquals(110500L, series.lastPositionMs)
        assertEquals(1_000_000L, series.durationMs)
        assertEquals(1, series.seasonNumber)
        assertEquals(3, series.episodeNumber)
        val movie = entries.first { it.contentType == "movie" }
        assertTrue(movie.isEffectivelyCompleted)
        assertEquals(960_000L, movie.lastPositionMs)
    }

    @Test
    fun invalidAndNegativeProgressDoesNotProduceNegativeSeek() {
        val state = Json.parseToJsonElement(
            """{"progress":{"tmdb:movie:9":{"position":-25,"duration":100,"percent":-5}}}"""
        ).jsonObject
        val entry = BrunioMediaStateWire.progressEntries(state).single()
        assertEquals(0L, entry.lastPositionMs)
        assertEquals(0f, entry.normalizedProgressPercent)
    }
}
