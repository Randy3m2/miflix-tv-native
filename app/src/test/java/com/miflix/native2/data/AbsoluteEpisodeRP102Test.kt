package com.miflix.native2.data

import com.miflix.native2.model.EpisodeSummary
import kotlinx.coroutines.runBlocking
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
    @Test fun dbzMetadataAndSpecialsRemainCorrect() {
        val source=rows(listOf(291))
        val special=EpisodeSummary(0,1,"Special","",null,0.0,"")
        assertEquals(source,AbsoluteEpisodeRP102.flatten(12971,source+special).map {it.copy(absoluteEpisodeRP102=0)})
    }
    @Test(expected=IllegalArgumentException::class) fun incompleteDbzIsStillRejected() {AbsoluteEpisodeRP102.flatten(12971,rows(listOf(290)))}
    @Test(expected=IllegalArgumentException::class) fun duplicateDbzCoordinatesAreStillRejected() {
        val source=rows(listOf(291));AbsoluteEpisodeRP102.flatten(12971,source.dropLast(1)+source.first())
    }
    @Test(expected=IllegalArgumentException::class) fun dbzGapsAreStillRejected() {
        AbsoluteEpisodeRP102.flatten(12971,rows(listOf(291)).map {if(it.episode==291) it.copy(episode=292) else it})
    }
    @Test fun narutoAndShippudenKeepOriginalCoordinatesEvenWithGaps() {
        for(id in listOf(46260,31910)) {
            assertFalse(AbsoluteEpisodeRP102.enabled(id));assertNull(AbsoluteEpisodeRP102.count(id))
            val source=rows(listOf(3,2)).filterNot {it.season==1 && it.episode==2}
            assertSame(source,AbsoluteEpisodeRP102.flatten(id,source))
            assertTrue(source.all {it.absoluteEpisodeRP102==0})
        }
    }
    @Test fun unrelatedShowsAreNotChanged() {
        val source=rows(listOf(4,5));assertSame(source,AbsoluteEpisodeRP102.flatten(1,source))
        assertFalse(AbsoluteEpisodeRP102.enabled(61709));assertFalse(AbsoluteEpisodeRP102.enabled(1))
    }
    @Test fun narutoRepositoryLoadsOnlyRequestedCanonicalSeason()=runBlocking {checkCanonicalSeason(46260)}
    @Test fun shippudenRepositoryLoadsOnlyRequestedCanonicalSeason()=runBlocking {checkCanonicalSeason(31910)}
    private suspend fun checkCanonicalSeason(id:Int) {
        val urls=mutableListOf<String>()
        val repo=TmdbRepository("test") {url,_->
            urls.add(url)
            check(url.contains("/tv/$id/season/2?")) {"Must not fetch whole-series metadata or other seasons"}
            """{"episodes":[{"episode_number":26,"name":"Original 26"},{"episode_number":28,"name":"Original 28"}]}"""
        }
        val result=repo.season(id,2)
        assertEquals(listOf(2 to 26,2 to 28),result.map {it.season to it.episode})
        assertTrue(result.all {it.absoluteEpisodeRP102==0})
        assertNull(repo.absoluteNumberRP102(id,2,26))
        assertEquals(1,urls.size)
    }
}
