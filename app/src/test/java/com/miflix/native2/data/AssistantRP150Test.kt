package com.miflix.native2.data

import com.miflix.native2.model.PlaybackProgress
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class AssistantRP150Test {
    @Test fun localSpanishRequestCombinesGenreRuntimeAndAudio() {
        val p=AssistantPlanRP150.local("Quiero ciencia ficción con acción, menos de 2 horas y doblada al español")
        assertEquals(listOf(878,28),p.genres);assertEquals(119,p.minutes);assertEquals("es",p.audio);assertTrue(p.query.isBlank())
    }
    @Test fun fractionalAndCombinedDurationsAreNotMisreadAsLongerBudgets() {
        assertEquals(90,AssistantPlanRP150.local("science fiction up to 1.5 hours").minutes)
        assertEquals(90,AssistantPlanRP150.local("hasta 1 hora y 30 minutos").minutes)
        assertEquals(89,AssistantPlanRP150.local("action less than 1.5 hours").minutes)
    }
    @Test fun aiCannotRelaxAnExplicitLocalTimeOrAudioConstraint() {
        val local=AssistantPlanRP150.local("action under 2 hours in Spanish")
        val remote=AssistantPlanRP150(listOf(878),200,audio="en")
        val merged=remote.constrainedBy(local)
        assertEquals(119,merged.minutes);assertEquals("es",merged.audio);assertTrue(merged.genres.containsAll(listOf(28,878)))
    }
    @Test fun explicitTimeButtonOverridesParsedDuration() {assertEquals(30,AssistantPlanRP150.local("action under 2 hours",30).minutes)}
    @Test fun smartCollectionFiltersAreSpecificAndSpoilerFree() {
        val horror=AssistantPlanRP150.local("Terror de los 90");assertEquals(listOf(27),horror.genres);assertEquals(1990,horror.decade)
        assertEquals("time travel",AssistantPlanRP150.local("Películas de viajes en el tiempo").keyword)
        assertEquals("plot twist",AssistantPlanRP150.local("Mejores finales inesperados").keyword)
    }
    @Test fun savedCollectionKeepsValidatedFiltersWithoutCallingAiAgain() {
        val original=AssistantPlanRP150(listOf(27),90,1990,"plot twist",audio="es")
        assertEquals(original,AssistantPlanRP150.json(original.json().toString()))
    }
    @Test fun actualPlayerDurationIsPreferredForContinueWatching() {
        assertEquals(20,AssistantTimeRP150.remaining(0,PlaybackProgress(position=600000,duration=1800000)))
        assertEquals(20,AssistantTimeRP150.remaining(90,PlaybackProgress(position=600000,duration=1800000)))
    }
    @Test fun titleSearchIsKeptAsTitleSearch() {assertEquals("Iron Man",AssistantPlanRP150.local("Iron Man").query)}
    @Test fun invalidAiOutputCannotInjectArbitraryGenresOrLanguages() {
        for(json in listOf("""{"genres":[99999]}""","""{"minutes":999}""","""{"audio":"http://example.test"}""","""{"decade":1991}""")) {
            assertThrows(IllegalArgumentException::class.java) {AssistantPlanRP150.json(json)}
        }
    }
    @Test fun remainingTimeRoundsUpAndRejectsUnknownRuntime() {
        assertEquals(0,AssistantTimeRP150.remaining(0,null));assertEquals(30,AssistantTimeRP150.remaining(40,PlaybackProgress(position=600001)))
        assertEquals(0,AssistantTimeRP150.remaining(40,PlaybackProgress(position=9999999)))
    }
    @Test fun discoveryUsesAllRequestedFiltersWithoutAssumingOriginalLanguageIsAudio()=runBlocking {
        var captured=""
        val repo=TmdbRepository("test") {url,_->captured=url;"""{"results":[]}"""}
        repo.assistantDiscoverRP150(AssistantPlanRP150(listOf(27),30,1990,audio="es"))
        assertTrue(captured.contains("/discover/movie?"));assertTrue(captured.contains("with_runtime.lte=30"));assertTrue(captured.contains("with_genres=27"))
        assertFalse(captured.contains("with_original_language"));assertTrue(captured.contains("1990-01-01"));assertTrue(captured.contains("1999-12-31"))
    }
    @Test fun missingKeywordProducesNoMisleadingGenericCollection()=runBlocking {
        val urls=mutableListOf<String>();val repo=TmdbRepository("test") {url,_->urls.add(url);"""{"results":[]}"""}
        assertTrue(repo.assistantDiscoverRP150(AssistantPlanRP150(keyword="time travel")).isEmpty());assertEquals(1,urls.size)
    }
    @Test fun movieAndEpisodeRuntimeAreVerifiedAndRepeatedQueriesAreCached()=runBlocking {
        val calls=mutableListOf<String>()
        val tmdb=TmdbRepository("test") {url,_->
            calls.add(url)
            when {
                url.contains("/discover/movie?") -> """{"results":[{"id":1,"title":"Short film","poster_path":"/fixture.jpg","release_date":"2000-01-01"}]}"""
                url.contains("/movie/1?") -> """{"id":1,"runtime":20,"genres":[],"credits":{}}"""
                url.contains("/discover/tv?") -> """{"results":[{"id":2,"name":"Series","poster_path":"/fixture-series.jpg","first_air_date":"2000-01-01"}]}"""
                url.contains("/tv/2/season/1?") -> """{"episodes":[{"episode_number":1,"name":"Pilot","runtime":25,"air_date":"2000-01-01"}]}"""
                else -> error("Unexpected metadata route")
            }
        }
        val repo=AssistantRepositoryRP150(tmdb);val plan=AssistantPlanRP150(minutes=30)
        val first=repo.picks("profile-a",plan,emptyList(),emptyMap())
        assertEquals(setOf(20,25),first.map {it.minutes}.toSet());assertEquals(1,first.count {it.episode!=null})
        val count=calls.size
        assertEquals(first,repo.picks("profile-a",plan,emptyList(),emptyMap()));assertEquals(count,calls.size)
        assertEquals(25,first.first {it.episode!=null}.episode!!.runtimeMinutesRP150)
    }
    @Test fun guestStoresAreIndependentAndDoNotAliasSets() {
        val host=MemoryPreferencesRP150();val guest=MemoryPreferencesRP150()
        host.edit().putString("history","host").putStringSet("favorites",mutableSetOf("a")).commit()
        guest.edit().putString("history","guest").putBoolean("enabled",true).commit()
        guest.edit().clear().apply()
        assertEquals("host",host.getString("history",null));assertFalse(guest.contains("history"))
        host.getStringSet("favorites",null)!!.add("b")
        assertEquals(setOf("a"),host.getStringSet("favorites",null))
    }
    @Test fun guestEditorChangesAreAtomicUntilApply() {
        val p=MemoryPreferencesRP150();val e=p.edit().putInt("x",5)
        assertEquals(0,p.getInt("x",0));e.apply();assertEquals(5,p.getInt("x",0));p.edit().remove("x").commit();assertFalse(p.contains("x"))
    }
}
