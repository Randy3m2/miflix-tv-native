package com.miflix.native2.data

import org.junit.Assert.*
import org.junit.Test

class FinalPlaylistRP97Test {
    private fun seed()=checkNotNull(javaClass.classLoader?.getResourceAsStream("final_playlist_rp97.m3u")).bufferedReader().use {it.readText()}
    @Test fun fullUserPlaylistHasStableUniqueIdsAndAllTwelveGroups() {
        val rows=FinalPlaylistRP97.parse(seed())
        assertEquals(2660,rows.size);assertEquals(2660,rows.map {it.id}.toSet().size)
        assertEquals(12,rows.map {it.category}.toSet().size)
        assertEquals((1..2660).toList(),rows.map {it.number})
        assertEquals(222,rows.count {it.logo==null})
        assertTrue(rows.any {it.name=="Dragon Ball Z 24/7"})
        assertTrue(rows.first {it.name=="Pinky y el Cerebro 24/7"}.logo!!.contains("8vOh5PAM3BUSXN0ZYbavhe6U3C2"))
    }
    @Test fun changingStreamUrlKeepsChannelIdAndUpdatedPayload() {
        val prefix="#EXTM3U\n#EXTINF:-1 tvg-id=\"x\" group-title=\"Deportes\",ESPN\n"
        val a=FinalPlaylistRP97.parse(prefix+"https://a.example/old.m3u8").single()
        val b=FinalPlaylistRP97.parse(prefix+"https://a.example/new.m3u8").single()
        assertEquals(a.id,b.id);assertNotEquals(a.url,b.url)
    }
    @Test fun quotedCommaNameUnicodeAndRequiredHttpHeadersSurvive() {
        val raw="\uFEFF#EXTM3U\r\n#EXTINF:-1 tvg-logo=\"https://a.example/logo.jpg\" group-title=\"República Dominicana\",Canal, HD\r\n#EXTVLCOPT:http-user-agent=Player\r\n#EXTVLCOPT:http-referrer=https://a.example/\r\nhttps://a.example/live.m3u8\r\n"
        val row=FinalPlaylistRP97.parse(raw).single()
        assertEquals("Canal, HD",row.name);assertEquals("República Dominicana",row.category)
        assertEquals("Player",row.headers["User-Agent"]);assertEquals("https://a.example/",row.headers["Referer"])
        assertEquals("aguilascibaenas",FinalPlaylistRP97.nameKey("Águilas Cibaeñas"))
    }
    @Test fun invalidPlaylistPayloadsAreRejected() {
        for(raw in listOf("","<html>Error</html>","#EXTM3U\n#EXTINF:-1,No stream\n")) {
            assertThrows(IllegalArgumentException::class.java) {FinalPlaylistRP97.parse(raw)}
        }
    }
    @Test fun drmAndUnsafeSchemesDoNotBecomeUnplayableChannels() {
        val raw="#EXTM3U\n#EXTINF:-1,Good\nhttps://a.example/a.m3u8\n#EXTINF:-1,Bad\nfile:///etc/passwd\n#EXTINF:-1,DRM\n#KODIPROP:inputstream.adaptive.license_key=secret\nhttps://a.example/drm.mpd\n"
        assertEquals(listOf("Good"),FinalPlaylistRP97.parse(raw).map {it.name})
    }
    @Test fun lidomAssignmentsResolveByStationNameAfterCatalogReplacement() {
        val names=FinalPlaylistRP97.parse(seed()).map {it.name}
        for(label in listOf("Teleantillas","Coral 39","Tele Universo","CDN Deportes")) {
            assertTrue("Missing station $label",names.any {FinalPlaylistRP97.stationMatches(it,listOf(label))})
        }
        assertFalse(names.any {FinalPlaylistRP97.stationMatches(it,listOf("Digital 15"))})
        assertFalse(names.any {FinalPlaylistRP97.stationMatches(it,listOf("Telemedios"))})
        assertFalse(FinalPlaylistRP97.stationMatches("Telemicro Internacional",listOf("Telemedios")))
    }
    @Test fun onlyLateralLivePlayerKeysRevealControls() {
        assertTrue(LiveRemotePolicyRP97.revealControls(true,false,21,false,false,false))
        assertTrue(LiveRemotePolicyRP97.revealControls(true,false,22,false,false,false))
        assertFalse(LiveRemotePolicyRP97.revealControls(true,false,23,false,false,false))
        assertFalse(LiveRemotePolicyRP97.revealControls(false,false,22,false,false,false))
        assertFalse(LiveRemotePolicyRP97.revealControls(true,true,22,false,false,false))
        assertFalse(LiveRemotePolicyRP97.revealControls(true,false,22,true,false,false))
        assertFalse(LiveRemotePolicyRP97.revealControls(true,false,22,false,false,true))
    }
    @Test fun qrAcceptsHttpsOnlyAndRejectsCrossOriginOrWrongToken() {
        assertTrue(FinalM3UServerRP97.valid("Live TV","https://raw.githubusercontent.com/user/repo/main/list.m3u"))
        assertFalse(FinalM3UServerRP97.valid("Live TV","http://example.com/list.m3u"))
        assertFalse(FinalM3UServerRP97.valid("Live TV","https://user:pass@example.com/list.m3u"))
        assertTrue(FinalM3UServerRP97.acceptsForm("http://10.0.0.2:1234","http://10.0.0.2:1234","token","token"))
        assertFalse(FinalM3UServerRP97.acceptsForm("http://10.0.0.2:1234","https://evil.example","token","token"))
        assertFalse(FinalM3UServerRP97.acceptsForm("http://10.0.0.2:1234","http://10.0.0.2:1234","token","wrong"))
    }
    @Test fun missingLidomChannelsUseBackupsWithoutDuplicatingNewEntries() {
        val rows=FinalPlaylistRP97.parse(seed())
        val text=checkNotNull(javaClass.classLoader?.getResourceAsStream("lidom_backup_rp97.m3u")).bufferedReader().use {it.readText()}
        val backups=FinalPlaylistRP97.parse(text)
        val merged=FinalPlaylistRP97.withLidomBackups(rows,backups)
        assertEquals(2662,merged.size)
        assertEquals(2662,merged.map {it.number}.toSet().size)
        assertEquals(2662,FinalPlaylistRP97.withLidomBackups(merged,backups).size)
        val digital=backups.first {FinalPlaylistRP97.stationMatches(it.name,listOf("Digital 15"))}
        assertTrue(digital.headers.containsKey("Referer"));assertTrue(digital.headers.containsKey("User-Agent"))
        val updated=digital.copy(url="https://new.example/digital15.m3u8")
        val next=FinalPlaylistRP97.withLidomBackups(rows+updated,backups)
        assertEquals(2662,next.size)
        assertEquals("https://new.example/digital15.m3u8",next.single {FinalPlaylistRP97.stationMatches(it.name,listOf("Digital 15"))}.url)
    }
    @Test fun recordingsAreGroupedBySportWithUnknownsRetained() {
        assertEquals("basketball",RecordingSportRP97.group("NBA Finals Replay"))
        assertEquals("baseball",RecordingSportRP97.group("MLB Yankees vs Dodgers recording"))
        assertEquals("baseball",RecordingSportRP97.group("LIDOM resumen"))
        assertEquals("football",RecordingSportRP97.group("NFL Super Bowl Replay"))
        assertEquals("soccer",RecordingSportRP97.group("Fútbol Champions League"))
        assertEquals("f1",RecordingSportRP97.group("Formula 1 replay"))
        assertEquals("combat",RecordingSportRP97.group("UFC recording"))
        assertEquals("other",RecordingSportRP97.group("Unclassified replay"))
    }

    @Test fun explicitGithubRefreshUsesAFreshCacheKeyWithoutChangingSignedUrls() {
        val raw="https://raw.githubusercontent.com/user/repo/main/list.m3u"
        assertNotEquals(FinalPlaylistRP97.refreshUrl(raw,1),FinalPlaylistRP97.refreshUrl(raw,2))
        assertTrue(FinalPlaylistRP97.refreshUrl(raw+"?token=x",1).contains("?token=x&brunio_refresh=1"))
        val signed="https://cdn.example/list.m3u?signature=abc"
        assertEquals(signed,FinalPlaylistRP97.refreshUrl(signed,1))
    }

}
