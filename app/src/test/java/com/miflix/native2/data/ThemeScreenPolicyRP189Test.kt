package com.miflix.native2.data

import org.junit.Assert.*
import org.junit.Test

class ThemeScreenPolicyRP189Test {
    @Test fun myListKeepsOriginalStyle() {
        assertTrue(ThemeScreenPolicyRP189.usesOriginalStyle("MY_LIST"))
    }
    @Test fun leavingOriginalPagesRestoresTheSelectedTheme() {
        listOf("DETAILS","HOME","LIVE","SEARCH","SETTINGS","PLAYER").forEach {
            assertFalse(ThemeScreenPolicyRP189.usesOriginalStyle(it))
        }
    }
}
