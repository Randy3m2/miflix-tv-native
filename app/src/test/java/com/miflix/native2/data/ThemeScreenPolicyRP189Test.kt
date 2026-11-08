package com.miflix.native2.data

import org.junit.Assert.*
import org.junit.Test

class ThemeScreenPolicyRP189Test {
    @Test fun originalPagesExcludeEveryThemeLayer() {
        assertTrue(ThemeScreenPolicyRP189.usesOriginalStyle("MY_LIST"))
        assertTrue(ThemeScreenPolicyRP189.usesOriginalStyle("DETAILS"))
    }
    @Test fun leavingOriginalPagesRestoresTheSelectedTheme() {
        listOf("HOME","LIVE","SEARCH","SETTINGS","PLAYER").forEach {
            assertFalse(ThemeScreenPolicyRP189.usesOriginalStyle(it))
        }
    }
}
