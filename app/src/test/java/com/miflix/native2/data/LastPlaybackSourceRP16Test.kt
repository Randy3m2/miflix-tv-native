package com.miflix.native2.data

import com.miflix.native2.model.StreamChoice
import com.miflix.native2.model.SubtitleChoice
import org.junit.Assert.*
import org.junit.Test

class LastPlaybackSourceRP16Test {
    private var saved="{}"
    private fun store()=LastPlaybackSourceRP16({saved},{saved=it})
    @Test fun successfulSourceSurvivesRestartWithHeadersAndSubtitles() {
        val stream=StreamChoice("Addon","https://video.test/movie?token=one","Latino",requestHeaders=mapOf("Referer" to "https://addon.test/"),
            subtitles=listOf(SubtitleChoice("https://subs.test/a.vtt","es","Latino")),audioLanguages=listOf("es-419"),sourceAddonHost="addon.test")
        store().save("movie",stream)
        assertEquals(stream,store().get("movie"))
    }
    @Test fun profilesEpisodesAndSettingsHaveSeparateKeys() {
        val key=LastPlaybackSourceRP16.key("a:p1","tv:10",1,2,"es")
        assertNotEquals(key,LastPlaybackSourceRP16.key("a:p2","tv:10",1,2,"es"))
        assertNotEquals(key,LastPlaybackSourceRP16.key("b:p1","tv:10",1,2,"es"))
        assertNotEquals(key,LastPlaybackSourceRP16.key("a:p1","tv:10",1,3,"es"))
        assertNotEquals(key,LastPlaybackSourceRP16.key("a:p1","tv:10",1,2,"en"))
    }
    @Test fun legacyTorrentSourceIsIgnored() {
        val stream=StreamChoice("P2P","brunio-p2p://0123456789abcdef0123456789abcdef01234567/4?tr=udp%3A%2F%2Ftracker.test%3A80","Latino")
        store().save("p2p",stream);assertNull(store().get("p2p"))
    }
    @Test fun failedSourceIsRemovedWithoutDeletingAnotherTitle() {
        val stream=StreamChoice("A","https://video.test/a","Video")
        val s=store();s.save("one",stream);s.save("two",stream);s.remove("one")
        assertNull(s.get("one"));assertNotNull(s.get("two"))
    }
    @Test fun storageIsBounded() {
        val s=store()
        repeat(75) {s.save("item$it",StreamChoice("A","https://video.test/$it","Video"))}
        assertEquals(60,org.json.JSONObject(saved).length())
        assertTrue(saved.toByteArray().size<=1_000_000)
    }
    @Test fun corruptDataAndUnsupportedSchemesAreIgnored() {
        saved="broken";assertNull(store().get("movie"))
        store().save("bad",StreamChoice("A","file:///tmp/private","Video"))
        assertNull(store().get("bad"))
    }
}
