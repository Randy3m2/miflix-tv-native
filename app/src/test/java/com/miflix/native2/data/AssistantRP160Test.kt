package com.miflix.native2.data

import org.junit.Assert.*
import org.junit.Test

class AssistantRP160Test {
    @Test fun aNewDictationIsNotLostWhileTheSharedKeyboardMirrorCatchesUp() {
        val dictated="can you find for me Jason Statham movie"
        assertFalse(AssistantInputPolicyRP160.discardPending(dictated,"old query",dictated))
        assertFalse(AssistantInputPolicyRP160.discardPending(dictated,dictated,dictated))
        assertTrue(AssistantInputPolicyRP160.discardPending(dictated,"new manual query","new manual query"))
    }
    @Test fun independentPlayerInputDiscardsOnlyAnActualUserEdit() {
        assertFalse(AssistantInputPolicyRP160.discardPending("movies starring Tom Holland","movies starring Tom Holland",null))
        assertTrue(AssistantInputPolicyRP160.discardPending("old dictation","edited query",null))
        assertFalse(AssistantInputPolicyRP160.discardPending(null,"anything",null))
    }
    @Test fun naturalActorDictationIsNotATitleSearchForTheFullSentence() {
        assertEquals(AssistantPersonQueryRP157("Jason Statham"),AssistantCatalogRP157.person("can you find for me Jason Statham movie"))
        assertEquals(AssistantPersonQueryRP157("Tom Holland"),AssistantCatalogRP157.person("show me Tom Holland movies"))
        assertEquals(AssistantPersonQueryRP157("Tom Holland"),AssistantCatalogRP157.person("Tom Holland movies"))
        assertNull(AssistantCatalogRP157.person("science fiction movies"))
        assertNull(AssistantCatalogRP157.person("action movies"))
    }
    @Test fun actorAndDirectorWithTheSameNameDoNotProduceEmptyAmbiguousResults()=kotlinx.coroutines.runBlocking {
        val calls=mutableListOf<String>()
        val tmdb=TmdbRepository("test") {url,_->calls.add(url);when {
            url.contains("/search/person")->"""{"results":[{"id":10,"name":"Tom Holland","known_for_department":"Directing","known_for":[]},{"id":20,"name":"Tom Holland","known_for_department":"Acting","known_for":[]}]}"""
            url.contains("/person/20/")->"""{"id":20,"cast":[{"id":100,"media_type":"movie","title":"Actor's film","character":"Peter","release_date":"2020-01-01"}]}"""
            url.contains("/person/10/")->"""{"id":10,"crew":[{"id":200,"media_type":"movie","title":"Director's film","job":"Director","release_date":"1985-01-01"}]}"""
            else->"""{"runtime":120,"genres":[],"credits":{}}"""
        }}
        val actor=AssistantCatalogRP157.filmography(tmdb,AssistantPersonQueryRP157("Tom Holland"),AssistantPlanRP150())
        assertEquals(listOf(100),actor.first.map {it.item.id});assertTrue(actor.second.isEmpty())
        assertFalse(calls.any {it.contains("/person/10/")})
        val director=AssistantCatalogRP157.filmography(tmdb,AssistantPersonQueryRP157("Tom Holland","director"),AssistantPlanRP150())
        assertEquals(listOf(200),director.first.map {it.item.id})
    }

}
