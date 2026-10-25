package com.miflix.native2.data
import org.junit.Test
import org.junit.Assert.*
import org.json.JSONObject
import java.time.LocalDate

class NbaCalendarRP123Test {
    @Test fun oneMonthUsesSupportedQuery() {
        val urls=NbaCalendarRequestsRP123.months(LocalDate.parse("2026-10-24"),LocalDate.parse("2026-10-31"))
        assertEquals(1,urls.size)
        assertTrue(urls.single().contains("dates=202610&"))
        assertFalse(urls.single().contains("20261024-"))
    }
    @Test fun rangeCrossingYearRequestsEachMonthExactlyOnce() {
        val urls=NbaCalendarRequestsRP123.months(LocalDate.parse("2026-12-31"),LocalDate.parse("2027-02-01"))
        assertEquals(listOf("202612","202701","202702"),urls.map {it.substringAfter("dates=").substringBefore("&")})
    }
    @Test fun leapDayWorks() {
        assertTrue(NbaCalendarRequestsRP123.months(LocalDate.parse("2028-02-29"),LocalDate.parse("2028-02-29")).single().contains("dates=202802&"))
    }
    @Test fun reversedRangeRejected() {
        try {NbaCalendarRequestsRP123.months(LocalDate.parse("2026-11-01"),LocalDate.parse("2026-10-01"));fail("Expected invalid range")}
        catch(_:IllegalArgumentException) { }
    }
    @Test fun nbaParserKeepsBothTeamIdsAndLogos() {
        val rows=CalendarSourcesRP120.nba(JSONObject("""{"events":[{"id":"game1","name":"Lakers at Warriors","date":"2026-10-25T23:00Z","competitions":[{"competitors":[{"team":{"id":"13","logo":"https://example.com/lakers.png"}},{"team":{"id":"9","logo":"https://example.com/warriors.png"}}]}]}]}"""))
        assertEquals(1,rows.size)
        assertEquals(listOf("13","9"),rows.single().teams)
        assertEquals(2,rows.single().logos.size)
        assertEquals("NBA",rows.single().league)
        assertTrue(rows.single().timed)
    }
    @Test fun badEventDoesNotHideOtherGames() {
        val rows=CalendarSourcesRP120.nba(JSONObject("""{"events":[{"id":"broken"},{"id":"game2","name":"Team A vs Team B","date":"2026-10-25T23:00Z","competitions":[{"competitors":[]}]}]}"""))
        assertEquals(listOf("game2"),rows.map {it.id})
    }
    @Test fun postponedGameNotShownAsActive() {
        val rows=CalendarSourcesRP120.nba(JSONObject("""{"events":[{"id":"p","name":"Postponed","date":"2026-10-25T23:00Z","status":{"type":{"name":"STATUS_POSTPONED"}},"competitions":[{"competitors":[]}]}]}"""))
        assertTrue(rows.single().cancelled)
    }
}
