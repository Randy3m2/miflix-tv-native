package com.miflix.native2.data

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.CancellationException
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class UpdateMetadataRP187Test {
    private fun metadata(app:String="com.brunio.mobile",code:Int=22326)=JSONObject()
        .put("applicationId",app).put("version","1.0.8-m9-rp187").put("versionCode",code)
        .put("downloadUrl",if(app=="com.brunio.mobile") "https://github.com/Randy3m2/miflix-tv-native/releases/download/mobile-v1.0.8-m9-rp187/BruniO-Mobile.apk" else "https://github.com/Randy3m2/miflix-tv-native/releases/download/native-v2.0.0-rp187/MiFlix-TV-Native.apk")
        .put("sha256","a".repeat(64)).put("sizeBytes",1024)
    private val direct="https://github.com/Randy3m2/miflix-tv-native/releases/download/mobile-latest/update.json"
    private fun release(url:String=direct)="""{"assets":[{"name":"update.json","browser_download_url":"$url"}]}"""

    @Test fun directMetadataNeedsNoReleaseApiAndKeepsVerifiedFields()=runBlocking {
        val calls=mutableListOf<String>()
        val repo=UpdateMetadataRP187("com.brunio.mobile",{calls.add(it);metadata().toString()},{})
        val info=repo.check(22324)
        assertEquals(listOf(direct),calls);assertTrue(info.isNewer);assertEquals(22326,info.versionCode)
        assertEquals("a".repeat(64),info.sha256);assertEquals(1024L,info.sizeBytes)
    }
    @Test fun unavailableDirectEndpointFallsBackToDiscoveryAndOneMetadataRetry()=runBlocking {
        val calls=mutableListOf<String>();var paused=0
        val repo=UpdateMetadataRP187("com.brunio.mobile",{url->
            calls.add(url)
            when(calls.size) {1->throw IllegalStateException("HTTP 503: <!DOCTYPE html> private diagnostic");2->release();else->metadata().toString()}
        },{paused++})
        assertTrue(repo.check(22324).isNewer);assertEquals(3,calls.size);assertTrue(calls[1].startsWith("https://api.github.com/"));assertEquals(1,paused)
    }
    @Test fun failuresNeverExposeHtmlOrMasqueradeAsUpToDate()=runBlocking {
        val repo=UpdateMetadataRP187("com.brunio.mobile",{throw IllegalStateException("HTTP 503: <!DOCTYPE html> secret");},{})
        try {repo.check(22324);fail("Must report unavailable")} catch(e:UpdateCheckUnavailableRP187) {
            assertFalse(e.message.orEmpty().contains("DOCTYPE"));assertFalse(e.message.orEmpty().contains("secret"));assertNull(e.cause)
        }
    }
    @Test fun wrongPackageBadHashAndUntrustedDownloadsAreRejected()=runBlocking {
        val bad=listOf(metadata("com.miflix.native2"),metadata().put("sha256","bad"),metadata().put("downloadUrl","https://evil.example/app.apk"),metadata().put("sizeBytes",UpdatePolicy.MAX_BYTES+1))
        for(body in bad) {
            val repo=UpdateMetadataRP187("com.brunio.mobile",{if(it.startsWith("https://api.github.com"))release() else body.toString()},{})
            try {repo.check(22324);fail("Must reject unverified metadata")} catch(_:UpdateCheckUnavailableRP187) {}
        }
    }
    @Test fun cancellationDoesNotTriggerFallbackRequests()=runBlocking {
        var calls=0
        val repo=UpdateMetadataRP187("com.brunio.mobile",{calls++;throw CancellationException()},{})
        try {repo.check(22324);fail("Must cancel")} catch(_:CancellationException) {}
        assertEquals(1,calls)
    }
    @Test fun tvAndMobileChannelsAreDistinctAndEqualCodesAreNotUpdates()=runBlocking {
        var seen=""
        val repo=UpdateMetadataRP187("com.miflix.native2",{seen=it;metadata("com.miflix.native2").toString()},{})
        assertFalse(repo.check(22326).isNewer);assertTrue(seen.contains("/native-latest/"))
        assertFalse(UpdatePolicy.metadataApiUrl("https://api.github.com/repos/another/repo/releases/latest"))
        assertFalse(UpdatePolicy.metadataApiUrl("http://api.github.com/repos/Randy3m2/miflix-tv-native/releases/tags/native-latest"))
    }
    @Test fun legacyTvMetadataWithoutApplicationIdIsAcceptedOnlyOnTvChannel()=runBlocking {
        val legacy=metadata("com.miflix.native2").apply {remove("applicationId")}.toString()
        val tv=UpdateMetadataRP187("com.miflix.native2",{legacy},{})
        assertTrue(tv.check(22324).isNewer)
        val mobile=UpdateMetadataRP187("com.brunio.mobile",{if(it.startsWith("https://api.github.com"))release() else legacy},{})
        try {mobile.check(22324);fail("TV metadata must not be accepted by mobile")} catch(_:UpdateCheckUnavailableRP187) {}
    }
}
