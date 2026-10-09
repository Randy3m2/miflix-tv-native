package com.miflix.native2.data

import org.junit.Assert.*
import org.junit.Test

class UpdatePolicyTest {
    @Test fun releaseCandidatesCompareNumericallyAndNeverOfferDowngrades() {
        assertTrue(UpdatePolicy.newer("2.0.0-rc18","2.0.0-rc17"))
        assertFalse(UpdatePolicy.newer("2.0.0-rc9","2.0.0-rc17"))
        assertFalse(UpdatePolicy.newer("2.0.0-rc17","2.0.0-rc17"))
        assertTrue(UpdatePolicy.newer("2.0.0","2.0.0-rc17"))
        assertFalse(UpdatePolicy.newer("2.0.0-rc99","2.0.0"))
        assertTrue(UpdatePolicy.newer("2.1.0-rc1","2.0.0"))
        assertFalse(UpdatePolicy.newer("Latest","2.0.0-rc17"))
    }
    @Test fun onlyOurHttpsReleaseAssetsAreAllowed() {
        assertTrue(UpdatePolicy.releaseUrl("https://github.com/Randy3m2/miflix-tv-native/releases/download/native-v2.0.0-rc18/update.json"))
        assertFalse(UpdatePolicy.releaseUrl("http://github.com/randy3m2/miflix-tv-native/releases/download/x/a.apk"))
        assertFalse(UpdatePolicy.releaseUrl("https://github.com/other/app/releases/download/x/a.apk"))
        assertFalse(UpdatePolicy.releaseUrl("https://github.com.evil.test/randy3m2/miflix-tv-native/releases/download/x/a.apk"))
        assertFalse(UpdatePolicy.releaseUrl("https://user@github.com/randy3m2/miflix-tv-native/releases/download/x/a.apk"))
    }
    @Test fun redirectsAllowGithubAssetCdnButRejectExternalHosts() {
        assertTrue(UpdatePolicy.downloadHost("https://release-assets.githubusercontent.com/a?token=example"))
        assertFalse(UpdatePolicy.downloadHost("https://example.test/a.apk"))
        assertFalse(UpdatePolicy.downloadHost("http://objects.githubusercontent.com/a"))
    }
}
