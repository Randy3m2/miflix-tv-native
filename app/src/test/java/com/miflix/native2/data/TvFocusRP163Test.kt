package com.miflix.native2.data

import org.junit.Assert.*
import org.junit.Test

class TvFocusRP163Test {
    @Test fun returningToSearchRestoresLInsteadOfTheDefaultA() {
        val saved=TvFocusRestoreRP163("keyboard:L")
        saved.focused("keyboard:A") // destination's initial-focus effect
        assertEquals("keyboard:L",saved.pending)
        saved.focused("keyboard:L");saved.restored()
        assertEquals("keyboard:L",saved.last);assertNull(saved.pending)
    }
    @Test fun userNavigationCancelsDelayedRestoration() {
        val saved=TvFocusRestoreRP163("keyboard:L");val before=saved.intent
        saved.interacted();saved.focused("keyboard:M")
        assertTrue(saved.intent>before);assertNull(saved.pending);assertEquals("keyboard:M",saved.last)
    }
    @Test fun closingADialogReturnsToTheActionThatOpenedIt() {
        val saved=TvFocusRestoreRP163(null)
        saved.focused("audio");saved.windowLost();saved.focused("default-play")
        assertEquals("audio",saved.pending)
    }
    @Test fun holdingOkDoesNotRunBothFoundationAndCustomClicks() {
        val press=TvPressRP163();press.down(0);repeat(20) {press.down(it+1)}
        assertTrue(press.up(false));assertFalse(press.up(false))
    }
    @Test fun cancelledPressAndReleaseAfterFocusLeavesNeverActivateTheNewButton() {
        val press=TvPressRP163();press.down(0);assertFalse(press.up(true))
        press.down(0);press.reset();assertFalse(press.up(false))
        press.down(4);assertFalse(press.up(false))
    }
}
