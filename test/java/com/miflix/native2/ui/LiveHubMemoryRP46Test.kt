package com.miflix.native2.ui

import org.junit.Assert.*
import org.junit.Test

class LiveHubMemoryRP46Test {
    @Test fun remembersSportsSectionAndCategoryAfterPlayerUnmount() {
        val owner="account:baseball-profile"
        LiveHubMemoryRP46.section(owner,"baseball")
        LiveHubMemoryRP46.category(owner,"baseball","sports:mlb:live")
        // A fresh hub reads the same session memory after the player closes.
        assertEquals("baseball",LiveHubMemoryRP46.section(owner))
        assertEquals("sports:mlb:live",LiveHubMemoryRP46.category(owner,"baseball"))
        LiveHubMemoryRP46.section(owner,"hub")
        assertEquals("hub",LiveHubMemoryRP46.section(owner))
    }
    @Test fun separatesProfilesAndSections() {
        val first="same-account:profile-one";val second="same-account:profile-two"
        LiveHubMemoryRP46.section(first,"tv")
        LiveHubMemoryRP46.category(first,"tv","nauta:most-viewed")
        LiveHubMemoryRP46.category(first,"baseball","mlb-games")
        assertEquals("hub",LiveHubMemoryRP46.section(second))
        assertNull(LiveHubMemoryRP46.category(second,"tv"))
        assertEquals("nauta:most-viewed",LiveHubMemoryRP46.category(first,"tv"))
        assertEquals("mlb-games",LiveHubMemoryRP46.category(first,"baseball"))
    }
    @Test fun boundsSessionMemory() {
        LiveHubMemoryRP46.section("old-profile","f1")
        for(i in 1..6) LiveHubMemoryRP46.section("recent-profile-$i","basketball")
        assertEquals("hub",LiveHubMemoryRP46.section("old-profile"))
        assertEquals("basketball",LiveHubMemoryRP46.section("recent-profile-6"))
    }
}
