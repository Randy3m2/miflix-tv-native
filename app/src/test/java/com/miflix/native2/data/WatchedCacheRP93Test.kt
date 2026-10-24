package com.miflix.native2.data

import org.junit.Assert.*
import org.junit.Test

class WatchedCacheRP93Test {
    @Test fun cachesEmptyAndIsolatesProfilesAndTitles() {
        val cache=WatchedCacheRP93<List<String>>()
        cache.put("account:a:tmdb:movie:1",emptyList(),100L,true)
        assertEquals(emptyList<String>(),cache.get("account:a:tmdb:movie:1",101L))
        assertNull(cache.get("account:b:tmdb:movie:1",101L))
        assertNull(cache.get("other:a:tmdb:movie:1",101L))
        assertNull(cache.get("account:a:tmdb:movie:2",101L))
    }
    @Test fun expirationAndFailureBackoffAreBounded() {
        val cache=WatchedCacheRP93<String>()
        cache.put("ok","friend",0L,true)
        assertEquals("friend",cache.get("ok",899999L))
        assertNull(cache.get("ok",900000L))
        cache.put("failed","empty",0L,false)
        assertEquals("empty",cache.get("failed",59999L))
        assertNull(cache.get("failed",60000L))
        cache.put("clock","value",100L,true)
        assertNull(cache.get("clock",99L))
    }
    @Test fun evictsLeastRecentlyUsedAndClearsOnPrivacyChange() {
        val cache=WatchedCacheRP93<String>()
        repeat(64) {cache.put("$it","$it",0L,true)}
        assertEquals("0",cache.get("0",1L))
        cache.put("64","64",1L,true)
        assertNull(cache.get("1",2L))
        assertEquals("0",cache.get("0",2L))
        cache.clear()
        assertNull(cache.get("0",2L))
        assertNull(cache.get("64",2L))
    }
}
