package com.brunio.desktop
import kotlin.test.*
class UiStringsTest {
    @Test fun englishBundleShipsWithTheApplication(){assertEquals("Settings",UiStrings.translate("Ajustes","en"));assertEquals("Play",UiStrings.translate("Reproducir","en"))}
    @Test fun spanishDoesNotRewriteText(){assertEquals("Ajustes",UiStrings.translate("Ajustes","es"))}
    @Test fun unknownTitleIsPreserved(){assertEquals("Title 123",UiStrings.translate("Title 123","en"))}
}
