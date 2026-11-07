package com.miflix.native2.data

import org.junit.Assert.*
import org.junit.Test

class ThemeDetailsRP182Test {
    @Test fun everyPresetHasItsOwnEmblemAndUnambiguousSavedColor() {
        val presets=ThemeStudioRP173.presets
        assertEquals(presets.map {it.id}.toSet(),ThemeDetailsRP182.details.keys)
        assertEquals(presets.size,presets.map {it.focus}.distinct().size)
        assertEquals(presets.size,ThemeDetailsRP182.details.values.map {it.path}.distinct().size)
        assertTrue(ThemeDetailsRP182.details.values.all {it.symbol.isNotBlank() && it.path.startsWith("M")})
    }
    @Test fun seasonalSportsAndCharacterThemesAreAvailable() {
        val ids=ThemeStudioRP173.presets.map {it.id}.toSet()
        assertTrue(ids.containsAll(ThemeDetailsRP182.seasonal+ThemeDetailsRP182.sports+"spider_man"))
        assertEquals("Shuriken",ThemeDetailsRP182.forId("naruto")!!.symbol)
        assertEquals("Leviathan axe",ThemeDetailsRP182.forId("god_of_war")!!.symbol)
    }
}
