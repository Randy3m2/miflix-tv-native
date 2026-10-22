package com.miflix.native2.data

import org.junit.Assert.*
import org.junit.Test

class EasyModeRP79Test {
    @Test fun preferencesDoNotCrossAccountsOrProfiles() {
        assertNotEquals(EasyModePolicyRP79.key("account-a","main"),EasyModePolicyRP79.key("account-a","grandpa"))
        assertNotEquals(EasyModePolicyRP79.key("account-a","main"),EasyModePolicyRP79.key("account-b","main"))
        assertNotEquals(EasyModePolicyRP79.key(null,"main"),EasyModePolicyRP79.key("account-a","main"))
        assertNotEquals(EasyModePolicyRP79.key("a:b","c"),EasyModePolicyRP79.key("a","b:c"))
    }
    @Test fun onlyBasicRoutesAndTheirPlaybackAreAllowed() {
        listOf("HOME","SEARCH","MY_LIST","LIVE_TV","DETAILS","PLAYER").forEach {assertTrue(EasyModePolicyRP79.allowsScreen(it))}
        listOf("FRIENDS","WATCH_PARTY","SETTINGS","PROFILES","MULTIVIEW","NOTIFICATIONS","PAIR_DEVICE","COLLECTIONS","LIDOM").forEach {assertFalse(EasyModePolicyRP79.allowsScreen(it))}
    }
    @Test fun playerCannotOpenAdvancedMenus() {
        listOf(null,"audio","channels").forEach {assertTrue(EasyModePolicyRP79.allowsPlayerMenu(it))}
        listOf("content","speed","subtitles","info","party-chat","sports_links","timing").forEach {assertFalse(EasyModePolicyRP79.allowsPlayerMenu(it))}
    }
    @Test fun easySourcesAreIndependentOfTheNormalAddonConfiguration() {
        assertEquals(listOf("https://mg.homelabx.qzz.io/manifest.json",BundledAddonsRP12.mandanguita),EasyModePolicyRP79.sourceManifests())
    }
}
