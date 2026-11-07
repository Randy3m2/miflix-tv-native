package com.miflix.native2.data

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.pow

class ThemePaletteRP185Test {
    @Test fun onlyTheCuratedThemesRemainAndSavedColorsAreUnambiguous() {
        val presets=ThemeStudioRP173.presets
        assertEquals(setOf("default","fogata","sand","christmas","halloween","summer","storm","nature","sea","nba_gsw"),presets.map {it.id}.toSet())
        assertEquals(presets.size,presets.map {it.focus}.distinct().size)
        assertEquals("default",ThemeStudioRP173.byId("venom").id)
    }
    @Test fun retiredProfilesReturnToDefaultWhileCustomAccentIsPreserved() {
        assertEquals(ThemeStudioRP173.byId("default").focus.toInt(),ThemeStudioRP173.normalizeFocus(0xFFFF3344.toInt()))
        assertEquals(0xFF65BFFF.toInt(),ThemeStudioRP173.normalizeFocus(0xFF65BFFF.toInt()))
        for(preset in ThemeStudioRP173.presets) assertEquals(preset.focus.toInt(),ThemeStudioRP173.normalizeFocus(preset.focus.toInt()))
    }
    @Test fun smallLabelsStayReadableOnEveryPanelAndSelectedSurface() {
        for(preset in ThemeStudioRP173.presets) {
            assertTrue(preset.id+" secondary text",contrast(preset.secondary,preset.surface)>=4.5)
            assertTrue(preset.id+" white text",contrast(0xFFFFFFFF,preset.surface)>=7.0)
            assertTrue(preset.id+" selected text",contrast(0xFFFFFFFF,mix(preset.surface,preset.focus,.24))>=4.5)
            assertTrue(preset.id+" visible focus",contrast(preset.focus,preset.background)>=3.0)
            assertTrue(ThemeStudioRP173.cornerRadius(preset.id) in 10..24)
        }
    }
    private fun mix(a:Long,b:Long,f:Double):Long {
        var result=0xFF000000L
        for(shift in listOf(0,8,16)) result=result or ((((a shr shift and 255)*(1-f)+(b shr shift and 255)*f).toLong()) shl shift)
        return result
    }
    private fun luminance(value:Long):Double {
        fun channel(shift:Int):Double {val v=(value shr shift and 255)/255.0;return if(v<=.04045) v/12.92 else ((v+.055)/1.055).pow(2.4)}
        return .2126*channel(16)+.7152*channel(8)+.0722*channel(0)
    }
    private fun contrast(a:Long,b:Long):Double {val x=luminance(a);val y=luminance(b);return (maxOf(x,y)+.05)/(minOf(x,y)+.05)}
}
