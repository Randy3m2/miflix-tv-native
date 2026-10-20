package com.miflix.native2.ui

import com.miflix.native2.data.LiveCategory
import org.junit.Assert.*
import org.junit.Test

class LiveHubPolicyRP41Test {
    private fun group(name:String)=liveSportGroupRP41(LiveCategory("tv",name.lowercase(),name))
    @Test fun basketballIncludesCollege() {assertEquals("basketball",group("NCAA Basketball"))}
    @Test fun baseballIncludesInternational() {assertEquals("baseball",group("Béisbol internacional"))}
    @Test fun americanFootballDoesNotBecomeSoccer() {assertEquals("football",group("American Football"));assertEquals("football",group("American-Football"))}
    @Test fun nflIsAmericanFootball() {assertEquals("football",group("NFL"))}
    @Test fun soccerIncludesSpanishNames() {assertEquals("soccer",group("Fútbol"))}
    @Test fun formulaOneIsRacing() {assertEquals("f1",group("Formula 1"))}
    @Test fun unassignedSportsStayInOthers() {assertEquals("others",group("Boxing"));assertEquals("others",group("Tennis"))}
    @Test fun mixedFeedsAreNotDedicatedOtherSports() {
        assertTrue(liveAggregateRP41(LiveCategory("tv","live","Live Now")))
        assertFalse(liveAggregateRP41(LiveCategory("tv","ufc","UFC")))
    }
}
