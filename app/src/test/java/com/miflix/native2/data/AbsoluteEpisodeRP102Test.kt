package com.miflix.native2.data

import com.miflix.native2.model.EpisodeSummary
import org.junit.Assert.*
import org.junit.Test

class AbsoluteEpisodeRP102Test {
    private fun rows(counts:List<Int>)=counts.flatMapIndexed {season,count ->(1..count).map {episode ->EpisodeSummary(season+1,episode,"Title $season $episode","Synopsis","image",8.0,"2000-01-01")}}
    @Test fun dbzHas291ContinuousEntries() {
        val source=rows(listOf(39,35,33,32,26,29,25,34,38))
        val result=AbsoluteEpisodeRP102.flatten(12971,source.reversed())
        assertEquals((1..291).toList(),result.map {it.absoluteEpisodeRP102})
        assertEquals(source.map {it.season to it.episode},result.map {it.season to it.episode})
    }
    @Test fun narutoHas220ContinuousEntries() {
        val result=AbsoluteEpisodeRP102.flatten(46260,rows(listOf(52,52,54,62)))
        assertEquals(220,result.size);assertEquals(220,result.last().absoluteEpisodeRP102)
        assertEquals(4,result.last().season);assertEquals(62,result.last().episode)
    }
    @Test fun canonicalProgressKeysStayUnchanged() {
        val source=rows(listOf(52,52,54,62));val result=AbsoluteEpisodeRP102.flatten(46260,source)
        assertEquals(source.map {"s${it.season}e${it.episode}"},result.map {"s${it.season}e${it.episode}"})
        assertEquals(53,result.first {it.season==2 && it.episode==1}.absoluteEpisodeRP102)
    }
    @Test fun metadataAndArtStayAttachedToTheirEpisodes() {
        val source=rows(listOf(220));val result=AbsoluteEpisodeRP102.flatten(46260,source)
        assertEquals(source,result.map {it.copy(absoluteEpisodeRP102=0)})
    }
    @Test fun specialsAreNotIncluded() {
        val special=EpisodeSummary(0,1,"Special","",null,0.0,"")
        assertEquals(220,AbsoluteEpisodeRP102.flatten(46260,rows(listOf(220))+special).size)
    }
    @Test fun unrelatedShowsAndShippudenAreNotChanged() {
        val source=rows(listOf(4,5));assertSame(source,AbsoluteEpisodeRP102.flatten(31910,source))
        assertFalse(AbsoluteEpisodeRP102.enabled(31910));assertFalse(AbsoluteEpisodeRP102.enabled(1))
    }
    @Test(expected=IllegalArgumentException::class) fun incompleteMetadataIsRejected() {AbsoluteEpisodeRP102.flatten(12971,rows(listOf(290)))}
    @Test(expected=IllegalArgumentException::class) fun duplicateCoordinatesAreRejected() {
        val source=rows(listOf(220));AbsoluteEpisodeRP102.flatten(46260,source.dropLast(1)+source.first())
    }
    @Test(expected=IllegalArgumentException::class) fun missingEpisodeNumbersAreRejected() {
        AbsoluteEpisodeRP102.flatten(46260,rows(listOf(220)).map {if(it.episode==220) it.copy(episode=221) else it})
    }
    @Test fun transitionAcrossSeasonsUsesAbsoluteOrder() {
        val result=AbsoluteEpisodeRP102.flatten(46260,rows(listOf(52,52,54,62)))
        val index=result.indexOfFirst {it.season==1 && it.episode==52}
        assertEquals(2,result[index+1].season);assertEquals(1,result[index+1].episode)
        assertEquals(53,result[index+1].absoluteEpisodeRP102)
    }
}
