package com.miflix.native2.data

import kotlinx.coroutines.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class SportsStatsRP95Test {
    private fun raw(name:String)=checkNotNull(javaClass.classLoader?.getResourceAsStream("sports_${name}_rp95.json")).bufferedReader().use {it.readText()}
    private fun game(league:String="NBA",id:String=if(league=="NBA") "401909848" else "849805"):LiveScoreRP11 {
        val home=if(league=="NBA") ScoreTeamRP11("15","Milwaukee Bucks","MIL","","0") else ScoreTeamRP11("158","Milwaukee Brewers","MIL","","0")
        val away=if(league=="NBA") ScoreTeamRP11("4","Chicago Bulls","CHI","","0") else ScoreTeamRP11("147","New York Yankees","NYY","","0")
        return LiveScoreRP11(id,league,"2026-10-23",home,away,"live","","",0)
    }
    @Test fun statsAreRestrictedToLiveSportsAndExcludeVodRecaps() {
        assertTrue(SportsStatsRP95.available(true,false,"sports","Game"))
        assertTrue(SportsStatsRP95.available(true,false,"nauta","ESPN HD"))
        assertTrue(SportsStatsRP95.available(true,false,"iptv","NBA TV"))
        assertFalse(SportsStatsRP95.available(false,false,"sports","NBA"))
        assertFalse(SportsStatsRP95.available(true,true,"sports","MLB replay"))
        assertFalse(SportsStatsRP95.available(true,false,"iptv","CNN News"))
        assertFalse(SportsStatsRP95.available(true,false,"iptv","My Vod Channel"))
    }
    @Test fun matchingRequiresBothTeamsAndOneUnambiguousGame() {
        val game=game()
        assertEquals(game,SportsStatsRP95.match("Chicago Bulls at Milwaukee Bucks",listOf(game)))
        assertEquals(game,SportsStatsRP95.match("MIL vs CHI HD",listOf(game)))
        assertNull(SportsStatsRP95.match("NBA live basketball",listOf(game)))
        assertNull(SportsStatsRP95.match("Chicago Bulls",listOf(game)))
        assertNull(SportsStatsRP95.match("CHI vs MIL",listOf(game,game.copy(id="2"))))
        assertNull(SportsStatsRP95.match("CHICAGO movie MILK",listOf(game)))
    }
    @Test fun nbaParsesPlayerColumnsByProviderLabelsAndRetainsTeamIdentity() {
        val parsed=SportsStatsRP95.nba(JSONObject(raw("nba")),game())
        assertTrue(parsed.final);assertEquals("Final",parsed.phase)
        assertEquals(listOf("CHI","MIL"),parsed.teams.map {it.short})
        assertEquals("125",parsed.teams[1].score)
        assertTrue(parsed.teams[0].values.containsKey("fieldGoalPct"))
        val group=parsed.players.first {it.teamId=="4"}
        assertTrue("PTS" in group.columns && "AST" in group.columns)
        assertTrue(group.rows.isNotEmpty());assertTrue(group.rows.all {it.values.size==group.columns.size})
    }
    @Test fun nbaRejectsMismatchedGameAndSupportsPreGameEmptyTables() {
        val json=JSONObject(raw("nba"))
        assertThrows(IllegalArgumentException::class.java) {SportsStatsRP95.nba(json,game(id="1"))}
        json.remove("boxscore")
        val parsed=SportsStatsRP95.nba(json,game())
        assertTrue(parsed.players.isEmpty());assertEquals(2,parsed.teams.size)
    }
    @Test fun mlbUsesGameStatsAndSeparatesBattingFromPitching() {
        val json=JSONObject(raw("mlb"));val live=json.getJSONObject("liveData")
        val awayId=live.getJSONObject("boxscore").getJSONObject("teams").getJSONObject("away").getJSONObject("team").optString("id")
        val actual=game("MLB").let {it.copy(away=it.away.copy(id=awayId))}
        val parsed=SportsStatsRP95.baseball(json,actual)
        assertTrue(parsed.final);assertEquals("Final",parsed.phase)
        assertEquals("3",parsed.teams[1].score)
        assertEquals(2,parsed.players.size);assertEquals(2,parsed.pitching.size)
        assertTrue(parsed.players.first().columns.contains("AB"))
        assertTrue(parsed.pitching.first().columns.contains("IP"))
        assertTrue(parsed.players.any {it.rows.isNotEmpty()});assertTrue(parsed.pitching.any {it.rows.isNotEmpty()})
        assertThrows(IllegalArgumentException::class.java) {SportsStatsRP95.baseball(json,actual.copy(id="1"))}
    }
    @Test fun simultaneousRequestsShareCacheAndExpireAfterThirtySeconds() = runBlocking {
        var calls=0;var time=0L
        val body=JSONObject(raw("nba")).apply {getJSONObject("header").getJSONArray("competitions").getJSONObject(0)
            .getJSONObject("status").getJSONObject("type").put("completed",false)}.toString()
        val repo=SportsStatsRP95(fetch={calls++;delay(10);body},now={time})
        coroutineScope {listOf(async {repo.stats(game())},async {repo.stats(game())}).awaitAll()}
        assertEquals(1,calls)
        time=29_999L;repo.stats(game());assertEquals(1,calls)
        time=30_000L;repo.stats(game());assertEquals(2,calls)
    }
    @Test fun cancelledLookupDoesNotPublishACachedResult() = runBlocking {
        var calls=0
        val entered=CompletableDeferred<Unit>()
        val body=raw("nba")
        val repo=SportsStatsRP95(fetch={calls++;if(calls==1) {entered.complete(Unit);awaitCancellation()};body})
        val job=launch {repo.stats(game())}
        entered.await();job.cancelAndJoin()
        repo.stats(game());assertEquals(2,calls)
    }
    @Test fun finalResultsCacheForFiveMinutesAndFailuresCanRecover() = runBlocking {
        var calls=0;var time=0L;var fail=true
        val body=raw("nba")
        val repo=SportsStatsRP95(fetch={calls++;if(fail) error("offline");body},now={time})
        try {repo.stats(game());fail("Expected failure")} catch(_:IllegalStateException) { }
        fail=false;repo.stats(game());assertEquals(2,calls)
        time=299_999L;repo.stats(game());assertEquals(2,calls)
        time=300_000L;repo.stats(game());assertEquals(3,calls)
    }
}
