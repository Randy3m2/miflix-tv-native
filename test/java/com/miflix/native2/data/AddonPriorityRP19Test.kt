package com.miflix.native2.data
import org.junit.Assert.*
import org.junit.Test
class AddonPriorityRP19Test {
    private val addons=listOf("Comet","Torrentio","Nuve+","Umbra","Progreso Latino","Deepflix","Addon Latam","LAT-Add").map {InstalledSourceAddonRP2(it,it,"test")}
    @Test fun spanishDefaultsPutDeepflixFirstAndPreserveOtherRanks() {
        assertEquals(listOf("Deepflix","LAT-Add","Addon Latam","Progreso Latino","Umbra","Nuve+","Torrentio","Comet"),AddonPriorityRP2.ordered(addons,emptyList(),"es-419").map {it.name})
    }
    @Test fun savedOrderStillWins() {
        assertEquals(listOf("Comet","Addon Latam"),AddonPriorityRP2.ordered(addons,listOf("Comet","Addon Latam"),"es").take(2).map {it.name})
    }
    @Test fun englishDefaultsAreUnchangedAndHostRecognizesDeepflix() {
        assertEquals(listOf("Comet","Torrentio"),AddonPriorityRP2.ordered(addons,emptyList(),"en").take(2).map {it.name})
        assertEquals(-1,AddonPriorityRP2.defaultRank(InstalledSourceAddonRP2("mg","MG","mg.homelabx.qzz.io"),"es"))
    }
}
