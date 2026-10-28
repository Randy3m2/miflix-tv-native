package com.miflix.native2.data

import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class PersonFilmographyRP141Test {
    @Test fun differentPeopleUseTheirExactIdsWithoutSearchingByName()=runBlocking {
        val urls=mutableListOf<String>()
        val repo=TmdbRepository("test") {url,_->
            urls.add(url)
            val person=if(url.contains("/person/10/")) 10 else 20
            """{"id":$person,"cast":[{"id":${person+100},"media_type":"movie","title":"Actor $person movie","character":"Detective"}]}"""
        }
        assertEquals(listOf(110),repo.actorCredits(10).map {it.id})
        assertEquals(listOf(120),repo.actorCredits(20).map {it.id})
        assertTrue(urls[0].contains("/person/10/combined_credits?"))
        assertTrue(urls[1].contains("/person/20/combined_credits?"))
        assertFalse(urls.any {it.contains("/search/") || it.contains("/discover/")})
    }
    @Test fun selfArchiveAndTalkShowsDoNotReplaceActingCredits() {
        val json=JSONObject("""{"id":10,"cast":[
          {"id":1,"media_type":"movie","character":"Detective"},
          {"id":2,"media_type":"movie","character":"Self"},
          {"id":3,"media_type":"movie","character":"Himself (archive footage)"},
          {"id":4,"media_type":"tv","character":"Guest","genre_ids":[10767]},
          {"id":5,"media_type":"tv","character":"Hero (voice)"},
          {"id":6,"media_type":"movie","character":"Self - Interviewee"},
          {"id":7,"media_type":"movie","character":"Ella misma"}
        ]}""")
        assertEquals(listOf(1,5),PersonFilmographyRP141.rows(json,10,"cast").map {it.getInt("id")})
    }
    @Test fun movieAndSeriesWithTheSameNumericIdRemainDistinct()=runBlocking {
        val repo=TmdbRepository("test") {_,_->"""{"id":10,"cast":[{"id":42,"media_type":"movie","title":"Movie"},{"id":42,"media_type":"tv","name":"Series"},{"id":42,"media_type":"movie","title":"Duplicate"}]}"""}
        assertEquals(setOf("tmdb:movie:42","tmdb:series:42"),repo.actorCredits(10).map {it.cloudId}.toSet())
        assertEquals(2,repo.actorCredits(10).size)
    }
    @Test fun directorsDoNotReceiveActingOrProducingCredits() {
        val json=JSONObject("""{"id":10,"cast":[{"id":1,"media_type":"movie"}],"crew":[{"id":2,"media_type":"movie","job":"Director"},{"id":3,"media_type":"movie","job":"Producer"},{"id":4,"media_type":"tv","job":"Director"}]}""")
        assertEquals(listOf(2,4),PersonFilmographyRP141.rows(json,10,"director").map {it.getInt("id")})
    }
    @Test fun unrelatedResponseCannotBeDisplayedForSelectedPerson() {
        assertThrows(IllegalArgumentException::class.java) {PersonFilmographyRP141.rows(JSONObject("""{"id":20,"cast":[]}"""),10,"cast")}
    }
    @Test fun localeIsSentForEachRequestWithoutReusingPreviousResults()=runBlocking {
        val urls=mutableListOf<String>()
        val repo=TmdbRepository("test") {url,_->urls.add(url);"""{"id":10,"cast":[]}"""}
        repo.language="es-MX";repo.actorCredits(10)
        repo.language="en-US";repo.actorCredits(10)
        assertTrue(urls[0].contains("language=es-MX"));assertTrue(urls[1].contains("language=en-US"))
    }
    @Test fun adultInvalidAndUnsupportedCreditsStayOutOfFilmography() {
        val json=JSONObject("""{"id":10,"cast":[{"id":1,"media_type":"movie","adult":true},{"id":0,"media_type":"movie"},{"id":3,"media_type":"person"},{"id":4,"media_type":"tv","genre_ids":[10763]}]}""")
        assertTrue(PersonFilmographyRP141.rows(json,10,"cast").isEmpty())
    }
}
