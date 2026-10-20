package com.miflix.native2.ui

import com.miflix.native2.data.IptvPlaylist
import org.junit.Assert.*
import org.junit.Test

class LiveHubPolicyRP42Test {
    @Test fun findsF1Stations() {assertTrue(f1ChannelRP42("Sky Sports F1 HD"));assertTrue(f1ChannelRP42("DAZN F1"))}
    @Test fun findsFormulaAliases() {assertTrue(f1ChannelRP42("Fórmula-1"));assertTrue(f1ChannelRP42("Formula One"));assertTrue(f1ChannelRP42("F1TV Pro"))}
    @Test fun rejectsLookalikes() {assertFalse(f1ChannelRP42("F10 Movies"));assertFalse(f1ChannelRP42("Formula 10"));assertFalse(f1ChannelRP42("ESPN"))}
    @Test fun distinguishesPageFromMedia() {assertTrue(playlistWebRP42("https://sportsbite.org/247/tnt1uk"));assertFalse(playlistWebRP42("https://video.example/live/master.m3u8"));assertFalse(playlistWebRP42("https://video.example/live/123"))}
    @Test fun m3uDoesNotTurnHtmlIntoMedia() {
        val rows=IptvPlaylist.parse("#EXTM3U\n#EXTINF:-1,Stream 1\nhttps://sportsbite.org/247/tnt1uk\n")
        assertEquals(1,rows.size);assertTrue(playlistWebRP42(rows.first().url))
    }
    @Test fun movesAcrossMonth() {assertEquals("2026-11-01",moveLidomDayRP42("2026-10-31",1));assertEquals("2026-09-30",moveLidomDayRP42("2026-10-01",-1))}
    @Test fun handlesLeapYear() {assertEquals("2028-02-29",moveLidomDayRP42("2028-03-01",-1))}
    @Test fun movesAcrossYear() {assertEquals("2027-01-01",moveLidomDayRP42("2026-12-31",1))}
}
