package com.miflix.native2.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class SplitStatsRP130Test {
    private fun game(id:String="1",league:String="LIDOM")=LiveScoreRP11(id,league,"2026-10-25",
        ScoreTeamRP11("203","Tigres del Licey","L","logo","1"),
        ScoreTeamRP11("201","Águilas Cibaeñas","AC","logo","0"),"live","3","")
    @Test fun onlyTwoOrThreeStreamsAllocateStatsLayout() {
        assertEquals(listOf(0,0,2,3,0,0),(0..5).map {SplitStatsPolicyRP130.mode(it)})
        assertEquals(0,SplitStatsPolicyRP130.mode(2,true))
        assertEquals(0,SplitStatsPolicyRP130.mode(3,true))
    }
    @Test fun lidomChannelMatchesOneFixtureWithoutGuessingDoubleHeaders() {
        val g=game()
        assertEquals(g,SplitStatsPolicyRP130.match("Tigres del Licey · Digital 15",listOf(g)))
        assertNull(SplitStatsPolicyRP130.match("Licey",listOf(g,g.copy(id="2"))))
        assertNull(SplitStatsPolicyRP130.match("LIDOM",listOf(g)))
    }
    @Test fun nbaAndMlbStillRequireBothTeams() {
        val g=game(league="NBA").copy(home=ScoreTeamRP11("1","Los Angeles Lakers","LAL","","0"),away=ScoreTeamRP11("2","Golden State Warriors","GSW","","0"))
        assertNull(SplitStatsPolicyRP130.match("Lakers",listOf(g)))
        assertEquals(g,SplitStatsPolicyRP130.match("Lakers vs Warriors",listOf(g)))
        assertNull(SplitStatsPolicyRP130.match("Lakers vs Warriors",listOf(g,g.copy(id="2"))))
    }
    @Test fun lidomUsesWinterScheduleAndTheSameBaseballBoxScoreRoute() = runBlocking {
        val urls=mutableListOf<String>()
        val repo=SportsStatsRP95(fetch={url->
            urls.add(url)
            if("schedule?" in url) """{"dates":[]}"""
            else """{"gamePk":1,"gameData":{"status":{"abstractGameState":"Live"}},"liveData":{"linescore":{"currentInning":3,"inningHalf":"Top","outs":1},"boxscore":{"teams":{}}}}"""
        })
        assertTrue(repo.games("LIDOM","2026-10-25").isEmpty())
        assertTrue(urls.single().contains("sportId=17&leagueId=131"))
        val stats=repo.stats(game())
        assertEquals(2,stats.teams.size)
        assertTrue(stats.phase.contains("3"))
        assertTrue(urls.last().contains("/game/1/feed/live"))
    }
}
