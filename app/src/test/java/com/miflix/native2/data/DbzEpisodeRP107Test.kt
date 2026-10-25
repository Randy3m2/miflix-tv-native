package com.miflix.native2.data

import com.miflix.native2.model.EpisodeSummary
import org.junit.Assert.*
import org.junit.Test

class DbzEpisodeRP107Test {
    private val rows=listOf(39,35,33,32,26,29,25,34,38).flatMapIndexed {season,count ->(1..count).map {episode ->EpisodeSummary(season+1,episode,"Episode","",null,0.0,"")}}.mapIndexed {index,row ->row.copy(absoluteEpisodeRP102=index+1)}
    @Test fun everyEpisodeQueriesSeasonOneWithItsAbsoluteNumber() {
        assertEquals(291,rows.size)
        for((index,row) in rows.withIndex()) assertEquals(1 to index+1,DbzEpisodeRP107.coordinates(row.season,row.episode,rows))
    }
    @Test fun sagaBoundaryDoesNotSendSeasonTwoToAnAddon() {assertEquals(1 to 40,DbzEpisodeRP107.coordinates(2,1,rows))}
    @Test fun finalEpisodeQueriesOneTwoNineOne() {assertEquals(1 to 291,DbzEpisodeRP107.coordinates(9,38,rows))}
    @Test fun importedAbsoluteEpisodeIsNotOffsetAgain() {assertEquals(1 to 200,DbzEpisodeRP107.coordinates(1,200,rows))}
    @Test fun overrideExcludesMoviesNarutoAndShippuden() {
        assertTrue(DbzEpisodeRP107.enabled("series",12971))
        assertFalse(DbzEpisodeRP107.enabled("movie",12971))
        assertFalse(DbzEpisodeRP107.enabled("series",46260))
        assertFalse(DbzEpisodeRP107.enabled("series",31910))
    }
    @Test(expected=IllegalArgumentException::class) fun invalidEpisodeDoesNotFallBackToEpisodeOne() {DbzEpisodeRP107.coordinates(9,100,rows)}
    @Test(expected=IllegalArgumentException::class) fun unnumberedMetadataDoesNotSendASagaCoordinate() {DbzEpisodeRP107.coordinates(2,1,rows.map {it.copy(absoluteEpisodeRP102=0)})}
}
