package com.miflix.native2.data
import org.junit.Assert.*
import org.junit.Test
class LiveManifestUrlRP49Test {
    @Test fun keepsConfiguredPathAndRawQueryForCatalogAndStream() {
        val manifest="https://example.com/config-token/manifest.json?lg=English%2C+Spanish&token=a%2Bb%26c"
        assertEquals("https://example.com/config-token/catalog/tv/baseball.json?lg=English%2C+Spanish&token=a%2Bb%26c",LiveManifestUrlRP49.resource(manifest,"catalog/tv/baseball.json"))
        assertEquals("https://example.com/config-token/stream/tv/game%3A1.json?lg=English%2C+Spanish&token=a%2Bb%26c",LiveManifestUrlRP49.resource(manifest,"stream/tv/game%3A1.json"))
    }
    @Test fun leavesUnconfiguredAddonRequestsUnchanged() {
        assertEquals("https://example.com/catalog/tv/list.json",LiveManifestUrlRP49.resource("https://example.com/manifest.json","catalog/tv/list.json"))
        assertEquals("https://example.com/config/stream/tv/id.json",LiveManifestUrlRP49.resource("https://example.com/config/manifest.json","stream/tv/id.json"))
    }
    @Test fun preservesExtraPathBeforeQueryAndDropsFragment() {
        assertEquals("https://example.com/catalog/tv/list/skip=25.json?lg=es",LiveManifestUrlRP49.resource("https://example.com/manifest.json?lg=es#fragment","/catalog/tv/list/skip=25.json"))
    }
}
