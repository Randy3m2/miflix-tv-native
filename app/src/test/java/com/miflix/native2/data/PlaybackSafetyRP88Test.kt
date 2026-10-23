package com.miflix.native2.data

import org.junit.Assert.*
import org.junit.Test

class PlaybackSafetyRP88Test {
    @Test fun replacementReleasesOnceBeforeBindingNextPlayer() {
        val releases=PlayerReleaseRP87()
        val events=mutableListOf<String>()
        val old=releases.bind {events.add("old:$it")}
        releases.beforeReplace()
        val next=releases.bind {events.add("next:$it")}
        releases.dispose(old)
        assertEquals(listOf("old:true"),events)
        assertFalse(next.isClosed)
        releases.dispose(next)
        releases.dispose(next)
        assertEquals(listOf("old:true","next:false"),events)
    }
    @Test fun leavingPlaybackUsesNormalCleanup() {
        val releases=PlayerReleaseRP87()
        val events=mutableListOf<Boolean>()
        val old=releases.bind {events.add(it)}
        releases.beforeReplace(false)
        releases.dispose(old)
        assertEquals(listOf(false),events)
    }
    @Test fun rapidDuplicateIsIgnoredButDifferentSourceIsAccepted() {
        val actions=RapidActionRP87()
        assertTrue(actions.accept("1080",1000))
        assertFalse(actions.accept("1080",1001))
        assertTrue(actions.accept("4k",1002))
        assertFalse(actions.accept("1080",1449))
        assertTrue(actions.accept("1080",1450))
    }
    @Test fun returnCommandRequiresHostRecipientAndFreshTimestamp() {
        val now=100000L
        assertTrue(PartyReturnPolicyRP88.accepts("host","host","return:guest","guest",now,now))
        assertFalse(PartyReturnPolicyRP88.accepts("other","host","return:guest","guest",now,now))
        assertFalse(PartyReturnPolicyRP88.accepts("host","host","return:other","guest",now,now))
        assertFalse(PartyReturnPolicyRP88.accepts("host","host","return:guest","guest",now-30001,now))
    }
    @Test fun easySpanishDefaultIgnoresNormalSeriesHistory() {
        assertEquals("es",EasySubtitleRP87.profileLanguage(true,"en",null))
        assertEquals("es",EasySubtitleRP87.seriesLanguage(true,"es","en",true))
        assertEquals("ko",EasySubtitleRP87.seriesLanguage(false,"en","ko",false))
        assertNull(EasySubtitleRP87.seriesLanguage(true,"off","es",false))
    }
}
