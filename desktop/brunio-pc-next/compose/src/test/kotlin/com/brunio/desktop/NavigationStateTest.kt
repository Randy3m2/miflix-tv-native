package com.brunio.desktop

import kotlin.test.*

class NavigationStateTest {
    @Test fun quickPassDoesNotExpand() {
        val state = NavigationState()
        state.pointer(true, 0); state.pointer(false, 90)
        state.tick(500); assertFalse(state.expanded)
    }
    @Test fun movementBetweenChildrenDoesNotRestartTimer() {
        val state = NavigationState()
        state.pointer(true, 0); state.pointer(true, 100)
        state.tick(140); assertTrue(state.expanded)
    }
    @Test fun gapCanBeCrossedWithoutCollapse() {
        val state = NavigationState()
        state.pointer(true, 0); state.tick(200)
        state.pointer(false, 201); state.pointer(true, 400)
        state.tick(700); assertTrue(state.expanded)
    }
    @Test fun keyboardFocusKeepsMenuOpen() {
        val state = NavigationState()
        state.focus(true, 0); state.tick(200)
        state.pointer(false, 300); state.tick(1000)
        assertTrue(state.expanded)
        state.focus(false, 1001); state.tick(1261)
        assertFalse(state.expanded)
    }
    @Test fun playerHideCancelsPendingExpansion() {
        val state = NavigationState()
        state.pointer(true, 0); state.hide(); state.tick(1000)
        assertFalse(state.expanded)
    }
}
