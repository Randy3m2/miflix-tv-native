package com.miflix.native2.data
import org.junit.Assert.*
import org.junit.Test
class FactoryDeepflixRP21Test {
    private val deepflix="https://mg.homelabx.qzz.io/manifest.json"
    @Test fun guestAndEmptyProfileIncludeDeepflixWithoutRetiredOptIn() {
        assertTrue(BundledAddonsRP12.effective(emptyList()).contains(deepflix))
        assertFalse(BundledAddonsRP12.retired.contains(deepflix))
        assertEquals(1,BundledAddonsRP12.effective(listOf(deepflix,deepflix)).count {it==deepflix})
    }
    @Test fun removalIsRespectedAndOtherDefaultsKeepTheirOrder() {
        assertFalse(BundledAddonsRP12.effective(emptyList(),setOf(deepflix)).contains(deepflix))
        assertEquals(listOf("addon.peerflix.mov","addonlatam.duckdns.org","torrentio.strem.fun"),
            BundledAddonsRP12.defaults.filter {it!=deepflix}.map {java.net.URI(it).host})
    }
}
