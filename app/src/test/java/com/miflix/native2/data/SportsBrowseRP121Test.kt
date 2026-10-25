package com.miflix.native2.data
import org.junit.Assert.*
import org.junit.Test
import org.json.JSONObject
import java.time.ZoneId
import java.time.LocalDate

class SportsBrowseRP121Test {
    @Test fun undatedOnlyToday() {
        assertTrue(SportsBrowsePolicyRP121.visibleDay("A vs B","","2026-10-25","2026-10-25"))
        assertFalse(SportsBrowsePolicyRP121.visibleDay("A vs B","","2026-10-26","2026-10-25"))
    }
    @Test fun publishedDateRespected() {
        assertTrue(SportsBrowsePolicyRP121.visibleDay("Game","2026-10-26","2026-10-26","2026-10-25"))
        assertFalse(SportsBrowsePolicyRP121.visibleDay("Game","2026-10-26","2026-10-25","2026-10-25"))
    }
    @Test fun isoTimeUsesDeviceZone() {
        assertTrue(SportsBrowsePolicyRP121.visibleDay("Game","2026-10-26T02:00:00Z","2026-10-25","2026-10-25",zone=ZoneId.of("America/Santo_Domingo")))
    }
    @Test fun invalidDateNeverInvented() {assertFalse(SportsBrowsePolicyRP121.visibleDay("2026-02-30","","2026-10-26","2026-10-25"))}
    @Test fun continuousF1ChannelsRemainAvailable() {assertTrue(SportsBrowsePolicyRP121.visibleDay("F1 TV","","2026-10-26","2026-10-25",true))}
    @Test fun lidomRequiresLeagueOrBothClubs() {
        assertTrue(SportsBrowsePolicyRP121.lidom("LIDOM: Toros vs Licey"));assertTrue(SportsBrowsePolicyRP121.lidom("Gigantes vs Águilas"))
        assertFalse(SportsBrowsePolicyRP121.lidom("San Francisco Giants vs Tigers"));assertFalse(SportsBrowsePolicyRP121.lidom("Toros de Tijuana"))
    }
    @Test fun separateSportsDoNotMix() {
        assertFalse(SportsBrowsePolicyRP121.leagueEvent("basketball","WNBA finals"));assertFalse(SportsBrowsePolicyRP121.leagueEvent("f1","MotoGP"))
        assertFalse(SportsBrowsePolicyRP121.leagueEvent("baseball","LIDOM: Licey vs Aguilas"))
        assertTrue(SportsBrowsePolicyRP121.leagueEvent("baseball","Dodgers vs Yankees"))
    }
    @Test fun spanishSearchIgnoresAccents() {assertEquals("jose ramirez",SportsBrowsePolicyRP121.key("José Ramírez"))}
    @Test fun seasonBoundaries() {
        assertEquals(2027,SportsBrowseRepositoryRP121.season("NBA",LocalDate.of(2026,10,25)))
        assertEquals(2026,SportsBrowseRepositoryRP121.season("LIDOM",LocalDate.of(2027,1,20)))
        assertEquals(2027,SportsBrowseRepositoryRP121.season("MLB",LocalDate.of(2027,4,1)))
    }
    @Test fun activeFilterIsExplicitNotAssumedFromRequest() {
        val rows=SportsParsersRP121.nbaActive(JSONObject("""{"items":[{"id":"1","fullName":"Active","active":true},{"id":"2","fullName":"Retired","active":false},{"id":"3","fullName":"Unknown"}]}"""))
        assertEquals(listOf("1"),rows.map {it.id})
    }
    @Test fun nbaRosterTeamIsConfirmedByRoster() {
        val team=CalendarTeamRP120("NBA","13","Team","logo")
        val rows=SportsParsersRP121.nbaRoster(JSONObject("""{"athletes":[{"id":"1","displayName":"A","status":{"type":"active"}},{"id":"2","displayName":"B","status":{"type":"inactive"}}]}"""),team)
        assertEquals(1,rows.size);assertEquals("13",rows.single().teamId)
    }
    @Test fun mlbRestrictsToCurrentMajorLeagueClubs() {
        val teams=listOf(CalendarTeamRP120("MLB","147","Yankees","logo"))
        val rows=SportsParsersRP121.mlbActive(JSONObject("""{"people":[{"id":1,"fullName":"A","active":true,"currentTeam":{"id":147}},{"id":2,"fullName":"B","active":true,"currentTeam":{"id":999}},{"id":3,"fullName":"C","active":false,"currentTeam":{"id":147}}]}"""),teams)
        assertEquals(listOf("1"),rows.map {it.id})
    }
    @Test fun winterRosterDoesNotUseThePersonsMlbClub() {
        val team=CalendarTeamRP120("LIDOM","672","Licey","logo")
        val rows=SportsParsersRP121.baseballRoster(JSONObject("""{"roster":[{"status":{"code":"A"},"person":{"id":1,"fullName":"A","currentTeam":{"id":147}}},{"status":{"code":"I"},"person":{"id":2,"fullName":"Inactive"}}]}"""),team)
        assertEquals(1,rows.size);assertEquals("672",rows.single().teamId)
    }
    @Test fun battingAndPitchingAreNeverConflated() {
        val j=JSONObject("""{"leagueLeaders":[{"leaderCategory":"battingAverage","statGroup":"pitching","leaders":[{"rank":1,"value":".100","person":{"id":1,"fullName":"Pitcher"}}]},{"leaderCategory":"battingAverage","statGroup":"hitting","leaders":[{"rank":1,"value":".330","person":{"id":2,"fullName":"Batter"}}]}]}""")
        val groups=SportsParsersRP121.baseballLeaders(j,"MLB","hitting",emptyList());assertEquals(".330",groups.single().rows.single().value)
    }
    @Test fun duplicateLeadersDoNotBecomeDuplicateCards() {
        val j=JSONObject("""{"leagueLeaders":[{"leaderCategory":"homeRuns","statGroup":"hitting","leaders":[{"rank":1,"value":"5","person":{"id":1,"fullName":"A"}},{"rank":1,"value":"5","person":{"id":1,"fullName":"A"}}]}]}""")
        assertEquals(1,SportsParsersRP121.baseballLeaders(j,"LIDOM","hitting",emptyList()).single().rows.size)
    }
    @Test fun nbaTiesRetainSharedRank() {
        val j=JSONObject("""{"leaders":{"categories":[{"name":"pointsPerGame","displayName":"Points","leaders":[{"displayValue":"30.0","athlete":{"id":"1","displayName":"A"}},{"displayValue":"30","athlete":{"id":"2","displayName":"B"}},{"displayValue":"20","athlete":{"id":"3","displayName":"C"}}]}]}}""")
        assertEquals(listOf("1","1","3"),SportsParsersRP121.nbaLeaders(j).single().rows.map {it.rank})
    }
    @Test fun nbaUnknownCategoriesNotMislabelled() {assertTrue(SportsParsersRP121.nbaLeaders(JSONObject("""{"leaders":{"categories":[{"name":"madeUp","leaders":[]}]}}""")).isEmpty())}
    @Test fun compactPlayerCacheRoundTrip() {
        val p=SportsPersonRP121("1","Player","NBA","photo","13","Team","logo","G");assertEquals(p,SportsPersonRP121.parse(p.json()))
    }
    @Test fun featuredPeopleMustStillComeFromTheActiveList() {
        val rows=SportsParsersRP121.sorted(listOf(SportsPersonRP121("999","A","NBA"),SportsPersonRP121("3975","Stephen Curry","NBA")))
        assertEquals("3975",rows.first().id);assertEquals(2,rows.size)
    }
    @Test fun f1GridContainsOnlyPublishedStarters() {
        val j=JSONObject("""{"MRData":{"RaceTable":{"Races":[{"Results":[{"Driver":{"driverId":"hamilton","givenName":"Lewis","familyName":"Hamilton"},"Constructor":{"constructorId":"ferrari","name":"Ferrari"}}]}]}}}""")
        val people=SportsParsersRP121.f1People(j,JSONObject());assertEquals(listOf("hamilton"),people.map {it.id});assertEquals("ferrari",people.single().teamId)
    }
    @Test fun absentSeasonIsEmptyNotPreviousSeason() {assertTrue(SportsParsersRP121.f1People(JSONObject(),JSONObject()).isEmpty())}
    @Test fun addonReleasedDateIsNotLost() {assertEquals("2026-10-26T02:00:00Z",SportsMetaPolicyRP121.start(JSONObject("""{"released":"2026-10-25T22:00:00-04:00"}""")))}
    @Test fun addonDateOnlyRemainsDateOnly() {assertEquals("2026-10-25",SportsMetaPolicyRP121.start(JSONObject("""{"date":"2026-10-25"}""")))}
    @Test fun addonUnixStartAcceptedButNotYearNumbers() {
        assertTrue(SportsMetaPolicyRP121.start(JSONObject().put("startTimestamp",1792976400L)).startsWith("2026-"))
        assertEquals("",SportsMetaPolicyRP121.start(JSONObject().put("releaseInfo",2026)))
    }

    @Test fun previousSeasonNeverRelabelledAsCurrent() {
        assertTrue(SportsParsersRP121.nbaLeaders(JSONObject("""{"requestedSeason":{"year":2026}}"""),2027).isEmpty())
        assertTrue(SportsParsersRP121.f1People(JSONObject("""{"MRData":{"RaceTable":{"season":"2025"}}}"""),JSONObject(),2026).isEmpty())
    }

    @Test fun f1UnpublishedStandingsDoNotProduceEmptyLeaderCards() {
        assertTrue(SportsParsersRP121.f1Leaders(JSONObject("""{"MRData":{"StandingsTable":{"season":"2026","StandingsLists":[]}}}"""),emptyList(),2026).isEmpty())
    }

}
