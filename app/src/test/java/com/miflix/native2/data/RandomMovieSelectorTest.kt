package com.miflix.native2.data

import com.miflix.native2.model.MediaSummary
import org.junit.Assert.*
import org.junit.Test

class RandomMovieSelectorTest {
    private fun movie(id: Int,rating: Double,type: String="movie")=MediaSummary(id,type,"Title $id","",null,null,rating,"2025")
    @Test fun tasteScoreBandExcludesSeenDislikedAndSeries() {
        val a=movie(1,7.4);val b=movie(2,4.0);val c=movie(3,7.8);val d=movie(4,7.6,"series")
        assertEquals(listOf(a),RandomMovieSelector.pool(listOf(a,b,c,d),setOf(a.cloudId,b.cloudId,c.cloudId),7.5,setOf(c.cloudId),emptySet()))
    }
    @Test fun recentPicksAreAvoidedWhenThereAreOtherMovies() {
        val a=movie(1,7.4);val b=movie(2,7.5)
        assertEquals(listOf(b),RandomMovieSelector.pool(listOf(a,b),emptySet(),7.5,emptySet(),setOf(a.cloudId)))
    }
    @Test fun coldStartPrefersWellRatedMoviesAndDeduplicates() {
        val a=movie(1,7.0);val b=movie(2,4.0)
        assertEquals(listOf(a),RandomMovieSelector.pool(listOf(a,a,b),emptySet(),null,emptySet(),emptySet()))
    }
    @Test fun noCandidateDoesNotReintroduceFinishedContent() {
        val a=movie(1,7.0)
        assertTrue(RandomMovieSelector.pool(listOf(a),emptySet(),7.0,setOf(a.cloudId),emptySet()).isEmpty())
    }
    @Test fun ifNoScoreFallsInBandUseClosestRelatedCandidates() {
        val a=movie(1,4.0);val b=movie(2,9.5)
        assertEquals(listOf(b,a),RandomMovieSelector.pool(listOf(a,b),setOf(a.cloudId,b.cloudId),7.5,emptySet(),emptySet()))
    }
}
