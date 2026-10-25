package com.miflix.native2.data

import org.junit.Assert.*
import org.junit.Test
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class CalendarRP120Test {
    @Test fun leagueIncludesAllTeams() {assertTrue(CalendarPolicyRP120.follows(setOf("NBA:*"),"NBA","g",listOf("1","2")))}
    @Test fun teamIncludesAwayOrHome() {for(id in listOf("1","2")) assertTrue(CalendarPolicyRP120.follows(setOf("NBA:$id"),"NBA","g",listOf("1","2")))}
    @Test fun unrelatedTeamExcluded() {assertFalse(CalendarPolicyRP120.follows(setOf("NBA:3"),"NBA","g",listOf("1","2")))}
    @Test fun leagueIdsCannotBleed() {assertFalse(CalendarPolicyRP120.follows(setOf("MLB:1"),"NBA","g",listOf("1")))}
    @Test fun individualEventOnly() {assertTrue(CalendarPolicyRP120.follows(setOf("EVENT:NBA:g"),"NBA","g",emptyList()));assertFalse(CalendarPolicyRP120.follows(setOf("EVENT:NBA:g"),"NBA","h",emptyList()))}
    @Test fun thresholdStartsNow() {assertFalse(CalendarPolicyRP120.due(1001,1000,true,false,false));assertTrue(CalendarPolicyRP120.due(1000,1000,true,false,false))}
    @Test fun noFakeMidnightAlert() {assertFalse(CalendarPolicyRP120.due(0,100,true,false,false));assertFalse(CalendarPolicyRP120.due(100,100,false,false,false))}
    @Test fun cancelledAndSentNeverAlert() {assertFalse(CalendarPolicyRP120.due(100,100,true,true,false));assertFalse(CalendarPolicyRP120.due(100,100,true,false,true))}
    @Test fun oldEventsDoNotSpam() {assertFalse(CalendarPolicyRP120.due(100,600101,true,false,false))}
    @Test fun sixHourCacheAndManualRefresh() {assertFalse(CalendarPolicyRP120.refresh(1000,2000,false));assertTrue(CalendarPolicyRP120.refresh(1000,21601000,false));assertTrue(CalendarPolicyRP120.refresh(1000,1001,true))}
    @Test fun clockRollbackAllowsRefresh() {assertTrue(CalendarPolicyRP120.refresh(2000,1000,false))}
    @Test fun rescheduledEventHasNewNoticeIdentity() {assertNotEquals(CalendarPolicyRP120.noticeKey("NBA","a",1),CalendarPolicyRP120.noticeKey("NBA","a",2))}
    @Test fun jsonRoundTrip() {
        val e=SportEventRP120("a","NBA","Away vs Home","2026-10-25",Instant.parse("2026-10-25T23:00:00Z").toEpochMilli(),listOf("1","2"),listOf("logo"))
        assertEquals(e,SportEventRP120.parse(e.json()))
    }
    @Test fun invalidDatesRejected() {assertTrue(runCatching {SportEventRP120.parse(JSONObject("""{"id":"x","league":"NBA","title":"x","date":"wrong"}"""))}.isFailure)}
    @Test fun nbaScheduleTeamsAndTba() {
        val j=JSONObject("""{"events":[{"id":"a","name":"Away at Home","date":"2026-10-25T23:00:00Z","competitions":[{"timeValid":false,"competitors":[{"team":{"id":"1","logo":"logo"}},{"team":{"id":"2"}}]}],"status":{"type":{"name":"STATUS_SCHEDULED"}}}]}""")
        val e=CalendarSourcesRP120.nba(j).single();assertEquals(listOf("1","2"),e.teams);assertFalse(e.timed);assertEquals("NBA:a",e.key)
    }
    @Test fun baseballPostponement() {
        val j=JSONObject("""{"dates":[{"games":[{"gamePk":1,"gameDate":"2026-10-25T23:00:00Z","status":{"detailedState":"Postponed"},"teams":{"away":{"team":{"id":1,"name":"A"}},"home":{"team":{"id":2,"name":"B"}}}}]}]}""")
        val e=CalendarSourcesRP120.baseball(j,"LIDOM",emptyList()).single();assertTrue(e.cancelled);assertEquals("A vs B",e.title)
    }
    @Test fun f1SessionsHaveDistinctKeys() {
        val j=JSONObject("""{"MRData":{"RaceTable":{"Races":[{"season":"2026","round":"1","raceName":"GP","date":"2026-11-01","time":"15:00:00Z","Qualifying":{"date":"2026-10-31","time":"14:00:00Z"},"FirstPractice":{"date":"2026-10-30"}}]}}}""")
        val rows=CalendarSourcesRP120.f1(j);assertEquals(3,rows.size);assertEquals(3,rows.map {it.key}.distinct().size);assertFalse(rows.first {it.title.endsWith("Practice 1")}.timed)
    }
    @Test fun nbaStandingsColumnMapping() {
        val j=JSONObject("""{"children":[{"name":"West","standings":{"entries":[{"team":{"id":"a","displayName":"A"},"stats":[{"name":"wins","displayValue":"3"},{"name":"losses","displayValue":"1"},{"name":"gamesBehind","displayValue":"0.5"},{"name":"Last Ten Games","displayValue":"3-1"}]}]}}]}""")
        assertEquals(listOf("3","1","0.5","3-1"),StandingsRP120.nba(j).single().rows.single().values)
    }
    @Test fun baseballStandingsUseRankNotArrayOrder() {
        val j=JSONObject("""{"records":[{"teamRecords":[{"team":{"id":1,"name":"B"},"leagueRank":"2"},{"team":{"id":2,"name":"A"},"leagueRank":"1"}]}]}""")
        assertEquals(listOf("2","1"),StandingsRP120.baseball(j,"LIDOM",emptyList()).single().rows.map {it.id})
    }
    @Test fun missingStandingsIsEmpty() {assertTrue(StandingsRP120.nba(JSONObject()).isEmpty());assertTrue(StandingsRP120.baseball(JSONObject(),"MLB",emptyList()).isEmpty())}
    @Test fun specialDatesDoNotInventTimes() {
        val rows=CalendarSourcesRP120.specials();assertFalse(rows.first {it.league=="GRAMMYS"}.timed);assertFalse(rows.first {it.league=="MISS_UNIVERSE"}.timed)
        val oscars=rows.first {it.league=="OSCARS"};assertEquals(19,Instant.ofEpochMilli(oscars.starts).atZone(ZoneId.of("America/New_York")).hour)
    }
    @Test fun feedAcceptsIsoStartWithoutEpochMath() {
        val e=SportEventRP120.parse(JSONObject("""{"id":"one","league":"BOXING","title":"Fight","startsAt":"2027-01-02T01:30:00Z"}"""))
        assertEquals("2027-01-02",e.date);assertTrue(e.timed);assertEquals(Instant.parse("2027-01-02T01:30:00Z").toEpochMilli(),e.starts)
    }
    @Test fun missingStartRemainsDateOnly() {
        val e=SportEventRP120.parse(JSONObject("""{"id":"one","league":"GRAMMYS","title":"Awards","date":"2027-02-07"}"""))
        assertFalse(e.timed)
    }
    @Test fun f1StandingsIncludeBothChampionships() {
        val j=JSONObject("""{"MRData":{"StandingsTable":{"StandingsLists":[{"ConstructorStandings":[{"position":"1","points":"10","wins":"1","Constructor":{"constructorId":"ferrari","name":"Ferrari"}}]}]}},"driversRP120":{"MRData":{"StandingsTable":{"StandingsLists":[{"DriverStandings":[{"position":"1","points":"10","wins":"1","Driver":{"driverId":"driver","givenName":"Test","familyName":"Driver"},"Constructors":[{"constructorId":"ferrari"}]}]}]}}}}""")
        val groups=StandingsRP120.f1(j);assertEquals(2,groups.size);assertTrue(groups.all {it.rows.single().logo.isNotBlank()})
    }

}
