package com.nuvio.app.features.brunio

import com.nuvio.app.features.plugins.PluginsUiState
import com.nuvio.app.features.streams.PlaybackAvailability
import kotlin.test.Test
import kotlin.test.assertTrue

/** BruniO identifiers must open source selection before IMDb resolution. */
class BrunioPlaybackAvailabilityTest {
    private val availability = PlaybackAvailability(
        addons = emptyList(),
        plugins = PluginsUiState(pluginsEnabled = false),
    )

    @Test
    fun movieFromBrunioTmdbCanOpenSourcePicker() {
        assertTrue(availability.canStream("movie", "tmdb:movie:603"))
    }

    @Test
    fun episodeFromBrunioTmdbCanOpenSourcePicker() {
        assertTrue(availability.canStream("series", "tmdb:series:123:s1e4"))
    }
}
