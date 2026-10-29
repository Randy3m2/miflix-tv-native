package com.miflix.native2.ui

import org.junit.Assert.*
import org.junit.Test

class AppLanguagesRP148Test {
    @Test fun localeAndNativeLabelsCoverAllSelectableInterfaces() {
        assertEquals(setOf("en","es","fr","ko"),AppLanguagesRP148.codes.toSet())
        assertEquals(AppLanguagesRP148.codes.toSet(),AppLanguagesRP148.choices.map {it.first}.toSet())
        assertEquals("fr-FR",AppLanguagesRP148.locale("fr"));assertEquals("ko-KR",AppLanguagesRP148.locale("ko"))
        assertEquals("Français",AppLanguagesRP148.label("fr"));assertEquals("한국어",AppLanguagesRP148.label("ko"))
    }
    @Test fun coreNavigationHasBundledTranslations() {
        for(label in listOf("Home","Search","Settings","Friends & Party","My List","Continue Watching","Play","Audio","Subtitles","App language")) {
            if(label!="Audio") assertNotEquals(label,AppLanguagesRP148.translate("fr",label))
            else assertEquals("Audio",AppLanguagesRP148.translate("fr",label))
            assertNotEquals(label,AppLanguagesRP148.translate("ko",label))
        }
    }
    @Test fun unknownAndDynamicProviderTextIsPreserved() {
        val text="Private provider title · 123"
        assertEquals(text,AppLanguagesRP148.translate("fr",text));assertEquals(text,AppLanguagesRP148.translate("ko",text))
        assertEquals("Play",AppLanguagesRP148.translate("unknown","Play"))
    }
    @Test fun concatenatedLabelsKeepTheirTrailingSeparator() {
        assertTrue(AppLanguagesRP148.translate("fr","Season ").endsWith(" "))
        assertTrue(AppLanguagesRP148.translate("ko","QR valid for ").endsWith(" "))
    }
}
