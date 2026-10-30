package com.miflix.native2.data

import com.miflix.native2.model.CloudSession
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class NowPlayingRP162Test {
    @Test fun deviceTagsSeparateTVAndPhoneWithoutChangingDeviceIdentity() {
        assertEquals("tv:device-a",NowPlayingRP162.deviceTag("device-a",true))
        assertEquals("mobile:device-a",NowPlayingRP162.deviceTag("device-a",false))
    }
    @Test fun expiryUsesServerRemainingTimeAndLocalMonotonicClock() {
        val rows=NowPlayingRP162.decode("""[{"profile_id":"main","cloud_id":"tmdb:series:10","title":"Friends","season":3,"episode":7,"valid_for_ms":9000}]""",1000)
        assertEquals(10000L,rows.single().validUntil)
        assertEquals(1,NowPlayingRP162.active(rows,9999).size)
        assertTrue(NowPlayingRP162.active(rows,10000).isEmpty())
        assertEquals(3,rows.single().season);assertEquals(7,rows.single().episode)
    }
    @Test fun rejectsExpiredEmptyAndURLRowsButKeepsLiveTitles() {
        val rows=NowPlayingRP162.decode("""[
            {"cloud_id":"tmdb:movie:10","title":"old","valid_for_ms":0},
            {"cloud_id":"https://bad.test/stream","title":"bad","valid_for_ms":1000},
            {"cloud_id":"tmdb:movie:11","title":"","valid_for_ms":1000},
            {"profile_id":"main","cloud_id":"tmdb:channel:12","title":"Documentaries","valid_for_ms":1000}
        ]""",0)
        assertEquals(listOf("Documentaries"),rows.map {it.title})
    }
    @Test fun multipleProfilesStaySeparateAndDuplicateProfileRowsAreBounded() {
        val rows=NowPlayingRP162.decode("""[
            {"profile_id":"main","cloud_id":"tmdb:movie:10","title":"A","valid_for_ms":1000},
            {"profile_id":"second","cloud_id":"tmdb:series:20","title":"B","valid_for_ms":1000},
            {"profile_id":"main","cloud_id":"tmdb:movie:30","title":"C","valid_for_ms":1000}
        ]""",0)
        assertEquals(listOf("A","B"),rows.map {it.title})
    }
    @Test fun accountSessionReadDoesNotAcceptAForeignAccountOrProfileParameter()=runBlocking {
        var called=false
        val repo=NowPlayingRepositoryRP162("https://test.supabase.co/","public") {url,headers ->
            called=true;assertEquals("https://test.supabase.co/rest/v1/rpc/brunio_now_playing_rp162",url)
            assertEquals("Bearer account-token",headers["Authorization"])
            assertFalse(headers.containsKey("x-brunio-profile"))
            """[{"profile_id":"main","cloud_id":"tmdb:movie:10","title":"A","valid_for_ms":1000}]"""
        }
        assertEquals(1,repo.read(CloudSession("account-token","refresh","account","email")) {500}.size)
        assertTrue(called)
    }
}
