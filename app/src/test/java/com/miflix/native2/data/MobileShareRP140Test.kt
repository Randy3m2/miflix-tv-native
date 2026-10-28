package com.miflix.native2.data

import com.miflix.native2.model.MediaSummary
import org.junit.Assert.*
import org.junit.Test

class MobileShareRP140Test {
    private fun item(type:String="movie",overview:String="A story")=MediaSummary(42,type,"A title",overview,null,null,8.0,"2026")
    @Test fun movieAndSeriesLinksOpenTheirOwnMetadataRoutes() {
        for(type in listOf("movie","series")) assertEquals(item(type).cloudId,MobileShareRP140.cloudId(MobileShareRP140.link(item(type))))
    }
    @Test fun sharedTextContainsSynopsisAndAppLinkWithoutPlaybackUrl() {
        val text=MobileShareRP140.text(item())
        assertTrue(text.contains("A title"));assertTrue(text.contains("A story"));assertTrue(text.endsWith(MobileShareRP140.link(item())))
    }
    @Test fun malformedAndForeignLinksCannotBecomeContentRoutes() {
        for(url in listOf(null,"https://example.com/movie/42","brunio://other/movie/42","brunio://content/movie/-1","brunio://content/movie/0","brunio://content/movie/abc","brunio://content/movie/42/extra","brunio://content/person/42","brunio://user@content/movie/42","brunio://content/movie/42?token=secret","brunio://content/movie/42#x")) assertNull(url,MobileShareRP140.cloudId(url))
    }
    @Test fun internalFallbackLinkAndWebLinkResolveTheSameTitle() {
        assertEquals("tmdb:series:42",MobileShareRP140.cloudId("brunio://content/series/42"))
        assertNull(MobileShareRP140.cloudId("https://miflix-tv-native-web.vercel.app/watch.html?type=movie&id=42&id=43"))
        assertNull(MobileShareRP140.cloudId("https://evil.example/watch.html?type=movie&id=42"))
    }
    @Test fun imdbSharingUsesAnExactTitleLinkAndPreservesCaption() {
        val url=MobileShareRP140.imdbLink("tt12345678")
        assertEquals("https://www.imdb.com/title/tt12345678/",url)
        assertTrue(MobileShareRP140.text(item(),url!!).contains("A story"))
        assertFalse(MobileShareRP140.text(item(),url).contains("watch.html"))
        assertNull(MobileShareRP140.imdbLink(null));assertNull(MobileShareRP140.imdbLink("tt1234567/evil"))
    }
    @Test fun missingSynopsisDoesNotExposeNullOrEmptyCaptionBlocks() {
        assertEquals("A title\n\n"+MobileShareRP140.link(item()),MobileShareRP140.text(item(overview="  ")))
    }
}
