package com.miflix.native2.data

import kotlinx.coroutines.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class StatsRefreshRP105Test {
    private fun body()=checkNotNull(javaClass.classLoader?.getResourceAsStream("sports_nba_rp95.json")).bufferedReader().use {it.readText()}
    private fun game(id:String="401909848")=LiveScoreRP11(id,"NBA","2026-10-23",ScoreTeamRP11("15","Bucks","MIL","","0"),ScoreTeamRP11("4","Bulls","CHI","","0"),"live","","",0)
    @Test fun cachedStatsAreAvailableImmediatelyWhileAnotherGameLoads()=runBlocking {
        val started=CompletableDeferred<Unit>();val release=CompletableDeferred<Unit>()
        val repo=SportsStatsRP95(fetch={url ->if(url.contains("event=1")) {started.complete(Unit);release.await()};body()})
        val old=repo.stats(game())
        val pending=launch {repo.stats(game("1"))}
        started.await()
        assertSame(old,repo.cached(game()))
        pending.cancelAndJoin()
        assertNull(repo.cached(game("1")))
    }
    @Test fun unrelatedGameDoesNotWaitForOldSlowRequest()=runBlocking {
        val started=CompletableDeferred<Unit>()
        val repo=SportsStatsRP95(fetch={url ->if(url.contains("event=1")) {started.complete(Unit);awaitCancellation()};body()})
        val pending=launch {repo.stats(game("1"))};started.await()
        val result=withTimeout(1500) {repo.stats(game())}
        assertEquals(2,result.teams.size)
        pending.cancelAndJoin()
    }
    @Test fun nbaPlayerPhotosAndNumbersStayWithTheirRows() {
        val json=JSONObject(body())
        val player=json.getJSONObject("boxscore").getJSONArray("players").getJSONObject(0).getJSONArray("statistics").getJSONObject(0).getJSONArray("athletes").getJSONObject(0)
        player.getJSONObject("athlete").put("headshot",JSONObject().put("href","https://example.test/player.png")).put("jersey","7")
        val row=SportsStatsRP95.nba(json,game()).players[0].rows[0]
        assertEquals("https://example.test/player.png",row.photo);assertEquals("7",row.number)
    }
    @Test fun openingPanelCanRefreshImmediatelyWithoutDiscardingItsCachedFrame()=runBlocking {
        var calls=0
        val repo=SportsStatsRP95(fetch={calls++;body()})
        val old=repo.stats(game());assertSame(old,repo.cached(game()))
        repo.stats(game());assertEquals(1,calls)
        repo.stats(game(),force=true);assertEquals(2,calls)
        assertNotNull(repo.cached(game()))
    }
}
