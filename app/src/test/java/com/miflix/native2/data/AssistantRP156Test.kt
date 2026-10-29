package com.miflix.native2.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class AssistantRP156Test {
    @Test fun moreThanIsMinimumAndUnderIsMaximum() {
        val more=AssistantPlanRP150.local("Busca películas que duren más de 2 horas")
        assertEquals(121,more.minMinutes);assertEquals(0,more.minutes);assertEquals("",more.query)
        assertEquals(120,AssistantPlanRP150.local("movies at least 2 hours").minMinutes)
        assertEquals(119,AssistantPlanRP150.local("menos de 2 horas").minutes)
    }
    @Test fun modelCannotTurnMinimumIntoMaximum() {
        val p=AssistantPlanRP150(minutes=120).constrainedBy(AssistantPlanRP150.local("more than 2 hours"))
        assertEquals(0,p.minutes);assertEquals(121,p.minMinutes)
        assertEquals(p,AssistantPlanRP150.json(p.json().toString()))
    }
    @Test fun minimumReachesDiscoveryQuery()=runBlocking {
        var request=""
        val tmdb=TmdbRepository("test") {url,_->request=url;"""{"results":[]}"""}
        tmdb.assistantDiscoverRP150(AssistantPlanRP150.local("más de 2 horas"))
        assertTrue(request.contains("with_runtime.gte=121"));assertFalse(request.contains("with_runtime.lte"))
    }
    @Test fun episodeDescriptionsRouteToIntegratedSceneSearch() {
        val request="Episodio de Naruto donde Gai dice esta noqueado"
        assertTrue(AssistantSceneRP156.detects(request));assertEquals("Naruto",AssistantSceneRP156.local(request).series)
        assertFalse(AssistantSceneRP156.detects("siguiente episodio"))
        assertFalse(AssistantSceneRP156.detects("películas de terror"))
    }
    @Test fun aModelEpisodeNumberAloneIsNotEnoughToClaimAMatch()=runBlocking {
        val tmdb=TmdbRepository("test") {url,_-> when {
            url.contains("/search/")->"""{"results":[{"id":1,"media_type":"tv","name":"Test Series","first_air_date":"2000-01-01"}]}"""
            url.contains("/season/")->"""{"episodes":[{"season_number":1,"episode_number":2,"name":"An unrelated episode","overview":"A picnic","air_date":"2000-01-01"}]}"""
            else->"""{"id":1,"name":"Test Series","number_of_seasons":1,"genres":[],"credits":{}}"""
        }}
        assertTrue(AssistantSceneRP156.find(tmdb,AssistantSceneHintRP156("Test Series","a missing dialogue",1,2,"Invented title")).isEmpty())
    }

}
