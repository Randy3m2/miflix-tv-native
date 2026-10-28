package com.miflix.native2.ui
import org.junit.Test
import org.junit.Assert.*
class SettingsScrollRP138Test {
    @Test fun footerCanBeRevealedAndVisibleControlsDoNotJitter() {
        assertEquals(0f,SettingsBringRP138.calculateScrollDistance(40f,60f,300f),0f)
        assertTrue(SettingsBringRP138.calculateScrollDistance(270f,60f,300f)>0f)
        assertTrue(SettingsBringRP138.calculateScrollDistance(-20f,60f,300f)<0f)
    }
    @Test fun largeSectionsNeverProduceNonFiniteScrollDistances() {
        for(height in listOf(30f,150f,500f,1500f)) {
            val distance=SettingsBringRP138.calculateScrollDistance(-50f,height,300f)
            assertTrue(distance.isFinite())
        }
    }
}
