package com.miflix.native2.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LidomYouTubeLiveTest {
    @Test
    fun skipsReplaysAndUpcomingStreamsEvenWhenTheirTitleSaysLive() {
        val page = channelPage(
            """{"videoRenderer":{"videoId":"past1234567","title":{"simpleText":"LIVE · Repetición"}}}""",
            """{"videoRenderer":{"videoId":"next1234567","title":{"simpleText":"Próximo LIVE"},"upcomingEventData":{}}}"""
        )
        assertNull(LidomYouTubeLive.find(page))
    }

    @Test
    fun readsLiveBadgeAndTitleFromTheSameCard() {
        val page = channelPage(
            """{"videoRenderer":{"videoId":"past1234567","title":{"simpleText":"Repetición"}}}""",
            """{"videoRenderer":{"videoId":"live1234567","title":{"runs":[{"text":"Juego "},{"text":"de hoy"}]},"badges":[{"metadataBadgeRenderer":{"style":"BADGE_STYLE_TYPE_LIVE_NOW"}}]}}"""
        )
        assertEquals(LidomVideo("live1234567", "Juego de hoy"), LidomYouTubeLive.find(page))
    }

    @Test
    fun sharedChannelSkipsAnotherTeamsLiveAndReadsModernCards() {
        val page = channelPage(
            """{"videoRenderer":{"videoId":"live1234567","title":{"simpleText":"Tigres vs. Toros"},"isLiveNow":true}}""",
            """{"lockupViewModel":{"contentId":"live7654321","metadata":{"lockupMetadataViewModel":{"title":{"content":"Leones del Escogido vs. Gigantes"}}},"contentImage":{"thumbnailViewModel":{"overlays":[{"thumbnailOverlayBadgeViewModel":{"thumbnailBadges":[{"thumbnailBadgeViewModel":{"style":"THUMBNAIL_BADGE_STYLE_LIVE"}}]}}]}}}}"""
        )
        assertEquals("live7654321", LidomYouTubeLive.find(page, "Escogido")?.id)
    }

    private fun channelPage(vararg cards: String): String =
        """<script>var ytInitialData = {"contents":[${cards.joinToString(",")}]};</script>"""
}
