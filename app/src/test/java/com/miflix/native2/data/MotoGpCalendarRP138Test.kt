package com.miflix.native2.data

import org.json.JSONArray
import org.junit.Test
import org.junit.Assert.*
import java.time.Instant

class MotoGpCalendarRP138Test {
    private fun event(extra:String="",broadcasts:String="[]")=JSONArray("""[{"id":"gp-1","name":"Sepang","kind":"GP","season":{"year":2026},"business_unit":{"acronym":"MGP"},"date_end":"2026-11-01T18:00:00+08:00","broadcasts":$broadcasts $extra}]""")
    @Test fun normalizesLocalOffsetAndExcludesMoto2() {
        val sessions="""[{"id":"sprint","name":"Sprint","date_start":"2026-10-31T15:00:00+0800","category":{"acronym":"MGP"}},{"id":"moto2","name":"Race","date_start":"2026-11-01T14:00:00+0800","category":{"acronym":"MT2"}}]"""
        val rows=MotoGpCalendarRP138.parse(event(broadcasts=sessions),2026)
        assertEquals(1,rows.size);assertEquals("MOTOGP",rows.single().league)
        assertEquals(Instant.parse("2026-10-31T07:00:00Z").toEpochMilli(),rows.single().starts)
        assertTrue(rows.single().timed);assertTrue(rows.single().logos.single().contains("motogp.png"))
    }
    @Test fun unknownSessionTimeIsDateOnlyWithoutAnInventedAlarm() {
        val row=MotoGpCalendarRP138.parse(event(),2026).single()
        assertFalse(row.timed);assertEquals(0L,row.starts);assertEquals("2026-11-01",row.date)
        assertFalse(CalendarPolicyRP120.due(row.starts,Long.MAX_VALUE,row.timed,false,false))
    }
    @Test fun hiddenEventsAndDifferentSeasonsAreNotPublished() {
        assertTrue(MotoGpCalendarRP138.parse(event(extra=",\"show_in_calendar\":false"),2026).isEmpty())
        assertTrue(MotoGpCalendarRP138.parse(event(),2027).isEmpty())
    }
    @Test fun cancelledRaceRetainsCancellationForExistingCalendarFiltering() {
        for(status in listOf("CANCELLED","POSTPONED")) {
            val rows=MotoGpCalendarRP138.parse(event(extra=",\"status\":\"$status\""),2026)
            assertTrue(rows.single().cancelled)
        }
    }
    @Test fun liveF1IncludesMotoGpAndKeepsUnrelatedRacingOut() {
        assertTrue(SportsBrowsePolicyRP121.leagueEvent("f1","MotoGP Malaysia"))
        assertTrue(SportsBrowsePolicyRP121.leagueEvent("f1","Formula 1 Mexico"))
        assertFalse(SportsBrowsePolicyRP121.leagueEvent("f1","NASCAR Cup Series"))
        assertFalse(SportsBrowsePolicyRP121.leagueEvent("f1","Moto2 Malaysia"))
        assertTrue(MotoGpCalendarRP138.matches("Moto GP · Race"));assertFalse(MotoGpCalendarRP138.matches("Formula One"))
        assertTrue(CalendarSourcesRP120.leagues.contains("MOTOGP"))
        assertTrue(CalendarPolicyRP120.follows(setOf("MOTOGP:*"),"MOTOGP","gp-1",emptyList()))
    }
}
