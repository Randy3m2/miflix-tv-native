package com.miflix.native2.data

import org.junit.Assert.assertEquals
import org.junit.Test

class SearchHistoryTest {
    @Test fun newestSearchMovesToFrontWithoutCaseDuplicates() {
        assertEquals(listOf("SILO","Dune"),SearchHistory.remember(listOf("Dune","Silo"),"  SILO  "))
    }
    @Test fun historyNeverExceedsTenAndEvictsOldest() {
        assertEquals(listOf("New title")+(1..9).map { "Title $it" },SearchHistory.remember((1..10).map { "Title $it" },"New title"))
    }
    @Test fun blankOrSingleCharacterDoesNotPolluteHistory() {
        assertEquals(listOf("Dune"),SearchHistory.remember(listOf("Dune"),"  "))
        assertEquals(listOf("Dune"),SearchHistory.remember(listOf("Dune"),"a"))
    }
    @Test fun trimsAndCollapsesWhitespace() {
        assertEquals(listOf("The Last of Us"),SearchHistory.remember(emptyList(),"  The   Last\tof Us  "))
    }
}
