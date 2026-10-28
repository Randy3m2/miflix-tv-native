package com.brunio.desktop
import kotlin.test.*
class PlaybackActionsTest {
    @Test fun movieHasNoEpisodeOrChannelActions() {
        val ids = playbackActions(PlaybackUi("Movie")).map { it.id }
        assertFalse("episodes" in ids); assertFalse("channels" in ids)
        assertTrue(ids.containsAll(listOf("sources", "audio", "subtitles", "party", "chat")))
    }
    @Test fun seriesNextIsDisabledWithoutAnEpisode() {
        val actions = playbackActions(PlaybackUi("Series", series = true))
        assertFalse(actions.first { it.id == "next" }.enabled)
        assertTrue(actions.any { it.id == "episodes" })
    }
    @Test fun liveUsesChannelActionsAndNoQueue() {
        val ids = playbackActions(PlaybackUi("Live", live = true)).map { it.id }
        assertTrue("channels" in ids); assertFalse("queue" in ids)
    }
    @Test fun clocksUseMilliseconds() {
        assertEquals("01:05", playbackTime(65000)); assertEquals("1:01:01", playbackTime(3661000))
        assertEquals("00:00", playbackTime(-10))
    }
}
