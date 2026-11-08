package com.miflix.native2.data

import org.junit.Assert.*
import org.junit.Test

class ThemeMotifsRP186Test {
    @Test fun backgroundsHaveNoDefaultMarkAndOnlyUseCuratedThemes() {
        val expected=ThemeStudioRP173.presets.map {it.id}.filter {it!="default" && it!="nba_gsw"}.toSet()
        assertEquals(expected,ThemeMotifsRP186.paths.keys)
        assertTrue(ThemeMotifsRP186.paths.values.all {it.startsWith("M")})
        assertTrue(ThemeMotifsRP186.silhouetteAlpha in 0f..0.1f)
        assertTrue(ThemeMotifsRP186.logoAlpha in 0f..0.1f)
    }
    @Test fun marksFitInsidePortraitLandscapeAndSmallViewports() {
        for((w,h) in listOf(1920f to 1080f,3840f to 2160f,360f to 800f,800f to 360f,240f to 240f,0f to 0f)) {
            val p=ThemeMotifsRP186.placement(w,h)
            assertTrue(p.x>=0f && p.y>=0f && p.side>=0f)
            assertTrue(p.x+p.side<=w+.01f && p.y+p.side<=h+.01f)
            assertTrue(p.side<=w*.72f+.01f && p.side<=h*.58f+.01f)
        }
    }
}
