package com.miflix.native2.data

import org.junit.Test
import org.junit.Assert.*

class MobileLatestRP139Test {
    private val recaps=LiveCategory("tv","recaps","Sport Recaps")
    @Test fun optionalFeedAcceptsHttpsAndRejectsLocalOrCredentialUrls() {
        assertTrue(MobileLatestRP139.validFeed(""));assertTrue(MobileLatestRP139.validFeed("https://example.com/events.json"))
        assertFalse(MobileLatestRP139.validFeed("http://example.com/events.json"));assertFalse(MobileLatestRP139.validFeed("file:///tmp/events.json"))
        assertFalse(MobileLatestRP139.validFeed("https://user:secret@example.com/events.json"))
    }
    @Test fun recordingsAreClassifiedLocallyAndUnknownOnesRemainAccessible() {
        val nba=LiveChannel("1","tv","Lakers vs Celtics replay",null,"")
        val mlb=LiveChannel("2","tv","Dodgers recap",null,"")
        val unknown=LiveChannel("3","tv","Archive game",null,"")
        assertTrue(MobileLatestRP139.recordingMode("Others",recaps,listOf(nba,mlb)))
        assertEquals("basketball",MobileLatestRP139.recordingGroup(nba,recaps))
        assertEquals("baseball",MobileLatestRP139.recordingGroup(mlb,recaps))
        assertEquals("other",MobileLatestRP139.recordingGroup(unknown,recaps))
        assertFalse(MobileLatestRP139.recordingMode("NBA",recaps,listOf(nba)))
    }
    @Test fun titleTakesPriorityOverCategoryFallback() {
        val motor=LiveCategory("tv","motor","Motorsport recordings")
        assertEquals("basketball",MobileLatestRP139.recordingGroup(LiveChannel("1","tv","NBA highlights",null,""),motor))
        assertEquals("f1",MobileLatestRP139.recordingGroup(LiveChannel("2","tv","Race replay",null,""),motor))
    }
    @Test fun calendarDisplaysMultipleLeaguesWithoutDuplicatingSessionLogos() {
        val rows=listOf(
            SportEventRP120("1","MOTOGP","Sprint","2026-11-01",0,timed=false),
            SportEventRP120("2","MOTOGP","Race","2026-11-01",0,timed=false),
            SportEventRP120("3","NBA","Game","2026-11-01",0,timed=false),
            SportEventRP120("4","MLB","Tomorrow","2026-11-02",0,timed=false))
        val logos=MobileLatestRP139.dailyLogos(rows,"2026-11-01")
        assertEquals(2,logos.size)
        assertTrue(logos.contains(CalendarSourcesRP120.logo("MOTOGP")))
        assertTrue(logos.contains(CalendarSourcesRP120.logo("NBA")))
        assertFalse(logos.contains(CalendarSourcesRP120.logo("MLB")))
    }
    @Test fun mobileLeagueNamesAreReadableAndPreserveSubscriptionKeys() {
        assertEquals("MotoGP",MobileLatestRP139.leagueName("MOTOGP"))
        assertEquals("Miss Universe",MobileLatestRP139.leagueName("MISS_UNIVERSE"))
        assertEquals("Boxeo",MobileLatestRP139.leagueName("BOXING",true))
        assertTrue(CalendarSourcesRP120.leagues.contains("MOTOGP"))
        assertEquals(9,CalendarSourcesRP120.leagues.distinct().size)
    }
}
