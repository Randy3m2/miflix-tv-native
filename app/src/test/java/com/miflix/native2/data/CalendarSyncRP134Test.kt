package com.miflix.native2.data

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class CalendarSyncRP134Test {
    private fun entry(enabled:Boolean,at:Long)=JSONObject().put("enabled",enabled).put("at",at)
    @Test fun mergeKeepsIndependentDeviceFollows() {
        val tv=JSONObject().put("NBA:bos",entry(true,10))
        val phone=JSONObject().put("MLB:nyy",entry(true,20))
        assertEquals(setOf("NBA:bos","MLB:nyy"),CalendarSyncRP134.enabled(CalendarSyncRP134.merge(tv,phone)))
        assertFalse(tv.has("MLB:nyy"))
    }
    @Test fun staleDeviceCannotResurrectRemovedTeam() {
        val removed=JSONObject().put("NBA:bos",entry(false,30))
        val stale=JSONObject().put("NBA:bos",entry(true,10))
        assertTrue(CalendarSyncRP134.enabled(CalendarSyncRP134.merge(removed,stale)).isEmpty())
        assertTrue(CalendarSyncRP134.enabled(CalendarSyncRP134.merge(stale,removed)).isEmpty())
    }
    @Test fun newerLocalEditSurvivesRemoteReply() {
        val local=JSONObject().put("F1:*",entry(true,40))
        val old=JSONObject().put("F1:*",entry(false,30))
        assertEquals(setOf("F1:*"),CalendarSyncRP134.enabled(CalendarSyncRP134.merge(local,old)))
    }
    @Test fun deletionWinsTimestampTieAndMergeIsIdempotent() {
        val yes=JSONObject().put("EVENT:NBA:1",entry(true,10))
        val no=JSONObject().put("EVENT:NBA:1",entry(false,10))
        val merged=CalendarSyncRP134.merge(yes,no)
        assertTrue(CalendarSyncRP134.enabled(merged).isEmpty())
        assertEquals(merged.toString(),CalendarSyncRP134.merge(merged,yes).toString())
    }
}
