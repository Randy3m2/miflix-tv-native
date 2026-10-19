package com.miflix.native2.data

import com.miflix.native2.model.StreamChoice
import org.junit.Assert.*
import org.junit.Test

class UnknownSizeExceptionRP8Test {
    private fun stream(host:String,size:Long=0)=StreamChoice("Source","https://video.test/file","Latino",
        videoSize=size,sourceAddonHost=host)
    @Test fun onlyApprovedManifestHostsAllowUnknownSize() {
        for(host in listOf("addonlatam.duckdns.org","plus.nuvehub.app","mg.homelabx.qzz.io"))
            assertTrue(StreamFilters.allowed(stream(host),"movie",null,1000))
        for(host in listOf("","mg.homelabx.qzz.io.evil.test","progreso-latino.xyz","comet.test","plus.nuvehub.app.evil.test"))
            assertFalse(StreamFilters.allowed(stream(host),"movie",null,1000))
    }
    @Test fun knownSizesAlwaysRespectCap() {
        for(host in listOf("addonlatam.duckdns.org","plus.nuvehub.app","mg.homelabx.qzz.io","other.test")) {
            assertTrue(StreamFilters.allowed(stream(host,1000),"movie",null,1000))
            assertFalse(StreamFilters.allowed(stream(host,1001),"movie",null,1000))
        }
    }
    @Test fun displayNameCannotCreateExceptionAndCopiesPreserveOrigin() {
        assertFalse(StreamFilters.allowed(stream("other.test").copy(addonName="Nuve+"),"movie",null,1000))
        val original=stream("plus.nuvehub.app")
        assertTrue(StreamFilters.allowed(original.copy(url="https://resolved.test/video"),"movie",null,1000))
    }
    @Test fun exceptionDoesNotBypassYearOrUncachedDownloadFilters() {
        assertFalse(StreamFilters.allowed(stream("plus.nuvehub.app").copy(filename="Matrix.2003.1080p"),"movie","1999",1000,"Matrix"))
        assertFalse(StreamFilters.allowed(stream("addonlatam.duckdns.org").copy(title="[TB Download] Latino"),"movie",null,1000))
    }
}
