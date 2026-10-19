package com.miflix.native2.ui

import org.junit.Assert.*
import org.junit.Test

class ScorePinShortcutRP15Test {
    @Test fun threeSeparatePressesOpenActionsOnce() {
        val s=ScorePinShortcutRP15()
        assertFalse(s.press(true,0,100))
        assertFalse(s.press(true,0,350))
        assertTrue(s.press(true,0,600))
        assertFalse(s.press(true,0,700))
    }
    @Test fun holdingUpDoesNotCountAsThreeClicks() {
        val s=ScorePinShortcutRP15()
        assertFalse(s.press(true,0,100))
        for(i in 1..10) assertFalse(s.press(true,i,100+i*50L))
    }
    @Test fun anotherDirectionResetsGesture() {
        val s=ScorePinShortcutRP15()
        s.press(true,0,100);s.press(true,0,200)
        assertFalse(s.press(false,0,250))
        assertFalse(s.press(true,0,300))
    }
    @Test fun slowPressesDoNotOpenActions() {
        val s=ScorePinShortcutRP15()
        assertFalse(s.press(true,0,100))
        assertFalse(s.press(true,0,900))
        assertFalse(s.press(true,0,1200))
    }
    @Test fun resetPreventsGestureCrossingScreenOrProfile() {
        val s=ScorePinShortcutRP15()
        s.press(true,0,100);s.press(true,0,200);s.reset()
        assertFalse(s.press(true,0,300))
    }
}
