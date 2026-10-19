package com.miflix.native2.data
import org.junit.Assert.*
import org.junit.Test
import org.json.JSONObject
import kotlinx.coroutines.runBlocking
class LiveScoresRP11Test {
 private fun fixture(name:String)=javaClass.getResource("/live_scores_rp11/$name.json")!!.readText()

 @Test fun parsesActualNbaFeedAndPreservesLogos() {
  val rows=LiveScoresRP11.nba(JSONObject(fixture("nba")),"2026-10-18")
  assertTrue(rows.isNotEmpty())
  val g=rows.first()
  assertEquals("NBA",g.league)
  assertTrue(g.home.logo.startsWith("https://"))
  assertTrue(g.home.name.isNotBlank())
  assertEquals("—",g.home.score)
 }
 @Test fun parsesLidomScoresWithoutDependingOnGoogle() {
  val rows=LiveScoresRP11.baseball(JSONObject(fixture("lidom")),"LIDOM","2026-10-18")
  assertEquals(3,rows.size)
  val toros=rows.flatMap {listOf(it.home,it.away)}.first {it.id=="668"}
  assertEquals("6",toros.score)
  assertEquals("Toros del Este",toros.name)
 }
 @Test fun parsesMlbFeedAndRoundTripsLocalPin() {
  val rows=LiveScoresRP11.baseball(JSONObject(fixture("mlb")),"MLB","2026-10-18")
  assertTrue(rows.isNotEmpty())
  assertEquals(rows.first(),LiveScoresRP11.decode(LiveScoresRP11.encode(rows.first())))
  assertTrue(rows.first().home.logo.startsWith("https://"))
 }
 @Test fun pollingIsBoundedStopsAtFinalAndCoordinatesStayOnScreen() {
  assertEquals(30000L,ScorePolicyRP11.delayMs("live"))
  assertEquals(120000L,ScorePolicyRP11.delayMs("scheduled"))
  assertEquals(0L,ScorePolicyRP11.delayMs("final"))
  assertEquals(60000L,ScorePolicyRP11.delayMs("live",1))
  assertEquals(300000L,ScorePolicyRP11.delayMs("live",100))
  assertEquals(0f,ScorePolicyRP11.coordinate(-1f))
  assertEquals(1f,ScorePolicyRP11.coordinate(2f))
  assertEquals(0f,ScorePolicyRP11.coordinate(Float.NaN))
 }
 @Test fun sharedCacheAvoidsDuplicateRequestsAndSeparatesLeaguesAndDates()=runBlocking {
  var requests=0
  val repo=LiveScoresRP11 {url ->
   requests++
   fixture(if(url.contains("espn.com")) "nba" else if(url.contains("leagueId=131")) "lidom" else "mlb")
  }
  repo.games("NBA","2026-10-18")
  repo.games("NBA","2026-10-18")
  assertEquals(1,requests)
  repo.games("LIDOM","2026-10-18")
  assertEquals(2,requests)
  repo.games("NBA","2026-10-19")
  assertEquals(3,requests)
 }
 @Test fun explicitLiveAndUnknownScoresAreNotFabricated() {
  val j=JSONObject("""{"events":[{"id":"1","date":"2026-10-18T22:00Z","status":{"period":3,"displayClock":"5:12","type":{"state":"in"}},"competitions":[{"competitors":[{"homeAway":"home","score":"82","team":{"id":"1","displayName":"Home","abbreviation":"H"}},{"homeAway":"away","score":"80","team":{"id":"2","displayName":"Away","abbreviation":"A"}}]}]}]}""")
  val g=LiveScoresRP11.nba(j,"2026-10-18").single()
  assertEquals("live",g.state)
  assertEquals("82",g.home.score)
  assertEquals("Q3 · 5:12",g.phase)
 }
}
