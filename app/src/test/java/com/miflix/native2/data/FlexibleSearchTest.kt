package com.miflix.native2.data

import com.miflix.native2.model.MediaSummary
import org.junit.Assert.*
import org.junit.Test

class FlexibleSearchTest {
    private fun item(id: Int,title: String)=MediaSummary(id,"movie",title,"",null,null,8.0,"")
    @Test fun accentsPunctuationAndWhitespace() {
        assertEquals("el senor de los anillos",FlexibleSearch.normalize("  EL Señor: de los ANILLOS! "))
    }
    @Test fun partialWordsAndSmallTypos() {
        assertTrue(FlexibleSearch.score("Breaking Bad","breking bad")>=650)
        assertTrue(FlexibleSearch.score("Silo","siloo")>=650)
        assertTrue(FlexibleSearch.score("Star Wars","star war")>=650)
        assertEquals(0,FlexibleSearch.score("Titanic","breaking bad"))
    }
    @Test fun exactFirstAndDuplicateTitlesByIdentity() {
        val exact=item(1,"Silo");val other=item(2,"Silo: Behind the scenes")
        assertEquals(listOf(exact,other),FlexibleSearch.rank(listOf(other,exact,exact),"silo"))
        assertTrue(FlexibleSearch.variants("breking bad").size<=2)
    }
    @Test fun transposedLettersAndMissingSpacesMatch() {
        assertTrue(FlexibleSearch.score("Avengers","Avengres")>=650)
        assertTrue(FlexibleSearch.score("John Wick","Jhon Wick")>=650)
        assertTrue(FlexibleSearch.score("Star Wars","starwars")>=900)
        assertTrue(FlexibleSearch.score("El conjuro","conjuro")>=750)
        assertTrue(FlexibleSearch.score("Batman","Batman")>FlexibleSearch.score("Batman","Batmam"))
    }
    @Test fun typoVariantsStayBounded() {
        assertTrue(FlexibleSearch.variants("the avengres endgame").size<=2)
        assertFalse(FlexibleSearch.variants("the avengres endgame").contains("the"))
    }

}
