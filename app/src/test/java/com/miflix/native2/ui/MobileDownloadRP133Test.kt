package com.miflix.native2.ui

import org.junit.Assert.*
import org.junit.Test

class MobileDownloadRP133Test {
    @Test fun signedFileKeepsQueryWithoutMistakingItForAnExtension() {
        assertTrue(MobileDownloadRP133.direct("https://cdn.example/video.MP4?token=private&expired=123"))
        assertTrue(MobileDownloadRP133.direct("http://cdn.example/movie.ts?name=video.m3u8"))
    }
    @Test fun playlistsAndWebPagesAreNeverSavedAsCompleteVideos() {
        listOf("https://cdn.example/live.m3u8","https://cdn.example/manifest.mpd",
            "https://example/watch?id=video.mp4","https://example/player.html?file=video.mp4").forEach {
            assertFalse(it,MobileDownloadRP133.direct(it))
        }
    }
    @Test fun nonNetworkAndCredentialUrlsAreRejected() {
        listOf("file:///storage/movie.mp4","content://downloads/movie.mp4","javascript:video.mp4",
            "https://user:pass@example/movie.mp4","https:///movie.mp4","not a url").forEach {
            assertFalse(it,MobileDownloadRP133.direct(it))
        }
    }
    @org.junit.Test fun opaqueFileLinksUseMetadataButManifestsNeverBecomeOfflineFiles() {
        val file=com.miflix.native2.model.StreamChoice("CDN","https://cdn.example.com/download?id=1","Movie",filename="Movie.mkv")
        org.junit.Assert.assertTrue(MobileDownloadRP133.direct(file))
        org.junit.Assert.assertFalse(MobileDownloadRP133.direct(file.copy(url="https://cdn.example.com/live.m3u8")))
        org.junit.Assert.assertFalse(MobileDownloadRP133.direct(file.copy(url="https://cdn.example.com/live.mpd")))
        org.junit.Assert.assertFalse(MobileDownloadRP133.direct(file.copy(filename="")))
    }
}
