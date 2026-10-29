package com.miflix.native2.data

import org.junit.Assert.*
import org.junit.Test

class TvInputPolicyRP143Test {
    @Test fun fireTvUsesItsSystemKeyboardInsteadOfGenericRecognitionActivity() {
        assertTrue(TvInputPolicyRP143.systemKeyboard("Amazon","Fire TV Stick"))
        assertTrue(TvInputPolicyRP143.systemKeyboard("amazon","AFTMM"))
    }
    @Test fun fireTvModelStillWorksWhenManufacturerIsReportedDifferently() {
        assertTrue(TvInputPolicyRP143.systemKeyboard("Unknown","AFTSSS"))
    }
    @Test fun otherAndroidTvsRetainTheirExistingVoiceRecognizerRoute() {
        assertFalse(TvInputPolicyRP143.systemKeyboard("Google","Chromecast"))
        assertFalse(TvInputPolicyRP143.systemKeyboard("NVIDIA","SHIELD Android TV"))
    }
}
