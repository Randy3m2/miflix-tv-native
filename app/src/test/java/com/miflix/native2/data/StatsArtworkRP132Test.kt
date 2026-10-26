package com.miflix.native2.data

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class StatsArtworkRP132Test {
    @Test fun portraitsUseRacingIdentityRatherThanCountryFlag() {
        val raw=checkNotNull(javaClass.classLoader?.getResourceAsStream("f1_scoreboard_rp131.json")).bufferedReader().use {it.readText()}
        val drivers=F1StatsRP131.parse(JSONObject(raw)).flatMap {it.drivers}
        assertTrue(drivers.isNotEmpty())
        assertTrue(drivers.filter {it.id!="5892"}.all {it.photo=="https://a.espncdn.com/i/headshots/rpm/players/full/${it.id}.png"})
        assertTrue(drivers.none {it.photo==it.flag})
        assertTrue(drivers.first {it.id=="5892"}.photo.contains("colher01"))
    }
    @Test fun officialPortraitWinsAndInvalidIdentityDoesNotProduceAnImageUrl() {
        assertEquals("https://images.example/driver.png",F1StatsRP131.portrait("5829","https://images.example/driver.png"))
        assertEquals("",F1StatsRP131.portrait("../bad"))
        assertEquals("",F1StatsRP131.portrait("","http://images.example/driver.png"))
        assertEquals("https://a.espncdn.com/i/headshots/rpm/players/full/5829.png",F1StatsRP131.portrait("5829","file:///private.png"))
    }
}
