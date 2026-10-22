package com.miflix.native2.data

import com.miflix.native2.model.StreamChoice
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class KoreanPlaybackRP78Test {
    private fun row(id:String,label:String,provider:String="Torrentio")=StreamChoice(provider,"https://example.org/$id",label,addonName=provider)
    @Test fun generalAddonResultsPreferKoreanBeforeQuality() {
        val en=row("en","Film English 2160p","Comet")
        val ko=row("ko","Film Korean 1080p")
        assertEquals(ko,StreamOrdering.best(listOf(en,ko),"ko"))
        assertEquals(ko,StreamOrdering.fallbackCandidates(listOf(en,ko),"ko").first())
        assertEquals(en,StreamOrdering.best(listOf(en,ko),"en"))
    }
    @Test fun declaredAudioAndSecondaryKoreanAreUsed() {
        val declared=row("decl","Film 1080p").copy(audioLanguages=listOf("kor"))
        val subs=row("subs","Film English 2160p\nSubtitles: Korean")
        assertEquals(declared,StreamOrdering.best(listOf(subs,declared),"ko"))
        assertEquals(declared,StreamOrdering.best(listOf(declared),"es",secondaryAudio="ko"))
        assertFalse(StreamLanguagePriority.hasAudio("eng","Film Korean","ko"))
    }
    @Test fun spanishOnlyNativeOptionDoesNotFilterKoreanPlayback() {
        val ko=row("ko","Film Korean 1080p","BruniO Native").copy(url="brunio-native://hash/file",nativeSource=NativeSource("hash","file",1000))
        assertEquals(ko,StreamOrdering.best(listOf(ko),"ko",nativeSpanishOnly=true))
    }
    @Test fun subtitleFallbackSearchesForKoreanEvenIfEnglishAndSpanishAreAlreadyPresent() = runBlocking {
        var calls=0
        val repo=SubtitleRepository("https://primary.example","https://fallback.example") {url ->
            calls++
            if(url.startsWith("https://primary")) """{"subtitles":[{"url":"https://subs/es","lang":"spa"},{"url":"https://subs/en","lang":"eng"}]}"""
            else """{"subtitles":[{"url":"https://subs/ko","lang":"kor"}]}"""
        }
        val subs=repo.resolve("tt1234567","movie",preferredLanguage="ko")
        assertEquals(2,calls);assertEquals("ko",subs.first().lang);assertEquals("https://subs/ko",subs.first().url)
    }
    @Test fun existingPreferredSubtitlesDoNotNeedAnExtraFallbackRequest() = runBlocking {
        var calls=0
        val repo=SubtitleRepository("https://primary.example","https://fallback.example") { _ ->
            calls++;"""{"subtitles":[{"url":"https://subs/ko","lang":"ko-KR"}]}"""
        }
        assertEquals("ko",repo.resolve("tt1234567","movie",preferredLanguage="ko").first().lang)
        assertEquals(1,calls)
    }
}
