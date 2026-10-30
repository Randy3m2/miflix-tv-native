package com.miflix.native2.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class AssistantRP157Test {
    @Test fun actorAndDirectorRequestsDoNotNeedGemini() {
        assertEquals(AssistantPersonQueryRP157("Tom Holland"),AssistantCatalogRP157.person("Películas donde sale Tom Holland"))
        assertEquals(AssistantPersonQueryRP157("Christopher Nolan","director"),AssistantCatalogRP157.person("Películas dirigidas por Christopher Nolan"))
        assertEquals("series",AssistantCatalogRP157.person("Series con Pedro Pascal")!!.type)
        assertEquals("Tom Holland",AssistantCatalogRP157.person("movies starring Tom Holland under 2 hours")!!.name)
        assertNull(AssistantCatalogRP157.person("Película donde un hombre revive el mismo día"))
    }
    @Test fun narrativeRequestsAreIdentifications() {
        assertTrue(AssistantCatalogRP157.description("Película donde un hombre revive el mismo día"))
        assertFalse(AssistantCatalogRP157.description("Películas donde sale Tom Holland"))
        assertFalse(AssistantCatalogRP157.description("Películas de terror de los 90"))
        assertNull(AssistantCatalogRP157.person("Películas de terror de los 90"))
        assertTrue(AssistantCatalogRP157.description("Película de un hombre que revive el mismo día"))
    }
    @Test fun modelSuggestionsCannotInjectUrlsOrFakeMediaTypes() {
        assertTrue(AssistantCatalogRP157.guesses("""{"titles":[{"title":"https://bad.test","type":"movie"},{"title":"A","type":"channel"}]}""").isEmpty())
        assertEquals(AssistantTitleGuessRP157("Groundhog Day","movie",1993),AssistantCatalogRP157.guesses("""{"titles":[{"title":"Groundhog Day","type":"movie","year":1993}]}""").single())
    }
    @Test fun descriptionDoesNotBlindlyTakeFirstSearchResult()=runBlocking {
        val tmdb=TmdbRepository("test") {url,_->if(url.contains("/search/")) """{"results":[{"id":1,"media_type":"movie","title":"Another Movie","poster_path":"/fixture.jpg","release_date":"1993-01-01"}]}""" else """{"id":1,"title":"Another Movie","original_title":"Another Movie","runtime":100,"genres":[],"credits":{}}"""}
        assertTrue(AssistantCatalogRP157.identify(tmdb,listOf(AssistantTitleGuessRP157("Groundhog Day","movie",1993))).isEmpty())
    }
    @Test fun sceneHintPreservesFranchiseAlternativesWithoutInventedIds() {
        val hint=AssistantSceneRP156.hint("""{"series":"Dragon Ball Z","clue":"Vegeta nightmare","title":"A real candidate title","alternateSeries":["Dragon Ball","https://bad.test"],"absoluteEpisode":153}""")
        assertEquals("Dragon Ball Z",hint.series);assertEquals(listOf("Dragon Ball"),hint.alternateSeries)
    }
    @Test fun realCreditsAreCachedAndDoNotCallGeminiOrSupabase()=runBlocking {
        val calls=mutableListOf<String>()
        val tmdb=TmdbRepository("test") {url,_->calls.add(url);when {
            url.contains("/search/person")->"""{"results":[{"id":9,"name":"Tom Holland","known_for":[]}]}"""
            url.contains("/person/9/")->"""{"id":9,"cast":[{"id":19,"media_type":"movie","title":"Example film","character":"Peter","release_date":"2020-01-01"}]}"""
            else->"""{"id":19,"title":"Example film","runtime":125,"genres":[],"credits":{}}"""
        }}
        val query=AssistantPersonQueryRP157("Tom Holland")
        assertEquals(listOf(19),AssistantCatalogRP157.filmography(tmdb,query,AssistantPlanRP150()).first.map {it.item.id})
        val count=calls.size
        assertEquals(1,AssistantCatalogRP157.filmography(tmdb,query,AssistantPlanRP150()).first.size)
        assertEquals(count,calls.size);assertTrue(calls.all {it.contains("api.themoviedb.org")})
    }
    @Test fun englishEpisodeTitleResolvesToCanonicalLocalizedSeasonWithoutTrustingModelNumbers()=runBlocking {
        val tmdb=TmdbRepository("test") {url,_->when {
            url.contains("/search/")->"""{"results":[{"id":99157,"media_type":"tv","name":"Example Series","poster_path":"/fixture.jpg","first_air_date":"2000-01-01"}]}"""
            url.contains("/season/2") && url.contains("en-US")->"""{"episodes":[{"episode_number":4,"name":"The Nightmare","overview":"A battle","air_date":"2000-01-01"}]}"""
            url.contains("/season/2")->"""{"episodes":[{"episode_number":4,"name":"La pesadilla","overview":"Una batalla","air_date":"2000-01-01"}]}"""
            url.contains("/season/")->"""{"episodes":[]}"""
            else->"""{"id":99157,"name":"Example Series","number_of_seasons":2,"genres":[],"credits":{}}"""
        }}
        tmdb.language="es-ES"
        val hit=AssistantSceneRP156.find(tmdb,AssistantSceneHintRP156("Example Series","missing clue",1,2,"The Nightmare")).single()
        assertEquals(2,hit.episode.season);assertEquals(4,hit.episode.episode);assertEquals("La pesadilla",hit.episode.title);assertTrue(hit.suggested)
    }

}
