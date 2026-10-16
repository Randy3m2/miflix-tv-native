package com.miflix.native2.data

import org.junit.Assert.*
import org.junit.Test

class SocialPublishGateTest {
    @Test fun changedContentStillWaitsFiveMinutes() {
        val gate=SocialPublishGate(300_000L,false)
        assertTrue(gate.due("user","Movie A",0))
        gate.published("user","Movie A",0)
        assertFalse(gate.due("user","Movie B",299_999))
        assertTrue(gate.due("user","Movie B",300_000))
    }
    @Test fun presenceStillHeartbeatsWhenUnchanged() {
        val gate=SocialPublishGate(300_000L,false)
        gate.published("user","same",10)
        assertTrue(gate.due("user","same",300_010))
    }
    @Test fun avatarChangesWaitFourMinutesAndUnchangedAvatarDoesNotWrite() {
        val gate=SocialPublishGate(240_000L,true)
        gate.published("user","avatar1",0)
        assertFalse(gate.due("user","avatar2",239_999))
        assertTrue(gate.due("user","avatar2",240_000))
        assertFalse(gate.due("user","avatar1",999_999))
    }
    @Test fun accountChangePublishesImmediatelyAndFailureDoesNotAdvanceGate() {
        val gate=SocialPublishGate(300_000L,false)
        assertTrue(gate.due("one","same",0))
        assertTrue(gate.due("one","same",1)) // failed network call never invokes published
        gate.published("one","same",1)
        assertTrue(gate.due("two","same",2))
        assertFalse(gate.due("one","changed",2))
    }
}
