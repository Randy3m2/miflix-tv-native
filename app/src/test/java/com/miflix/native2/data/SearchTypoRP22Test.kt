package com.miflix.native2.data
import org.junit.Assert.*
import org.junit.Test
class SearchTypoRP22Test {
    @Test fun doubledLetterRetrievesCorrectRemoteQueryFirst() {
        assertEquals("tremors",FlexibleSearch.variants("Tremoors").first())
        assertTrue(FlexibleSearch.score("Tremors","Tremoors")>=650)
        assertTrue(FlexibleSearch.score("Tremors 2: Aftershocks","Tremoors")>=650)
    }
    @Test fun swappedAndMissingLettersStillMatchLocally() {
        assertTrue(FlexibleSearch.score("Tremors","Tremros")>=650)
        assertTrue(FlexibleSearch.score("Tremors","Tremrs")>=650)
        assertEquals(0,FlexibleSearch.score("Titanic","Tremoors"))
    }
    @Test fun correctionsStayBoundedAndDoNotReplaceExactSearch() {
        for(q in listOf("Tremoors","Harry Potter","The Avengers","Halloween")) {
            assertTrue(FlexibleSearch.variants(q).size<=2)
            assertFalse(FlexibleSearch.variants(q).contains(FlexibleSearch.normalize(q)))
        }
        assertEquals(1000,FlexibleSearch.score("Halloween","Halloween"))
    }
}
