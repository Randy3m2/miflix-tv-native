package com.miflix.native2.data

import com.miflix.native2.model.StreamChoice
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class NativeVodRP77Test {
    private fun row(id:String,label:String,size:Long=1000)=StreamChoice("BruniO Native · TorBox","brunio-native://$id/file",label,
        filename=label,videoSize=size,addonName="BruniO Native",nativeSource=NativeSource(id,"file",size))
    @Test fun latinoBeatsHigherQualitySpainAndNativeFallbackKeepsThatOrder() {
        val latino=row("lat","Film Latino 1080p",2000)
        val spain=row("es","Film es-ES 2160p",9000)
        val cast=row("cast","Film Castellano 2160p",9000)
        assertEquals(listOf(latino,spain,cast),StreamOrdering.sorted(listOf(cast,spain,latino),"es",nativeSpanishOnly=true))
        assertEquals(latino,StreamOrdering.best(listOf(spain,latino),"es",nativeSpanishOnly=true))
        assertEquals(listOf(latino,spain),StreamOrdering.fallbackCandidates(listOf(spain,latino),"es",nativeSpanishOnly=true))
    }
    @Test fun qualityPrecedesSizeAndNativeSizeCanBeSmallOrLargeFirst() {
        val fourK=row("4k","Film Latino 2160p",9000)
        val small=row("small","Film Latino 1080p",1000)
        val large=row("large","Film Latino 1080p",5000)
        val input=listOf(large,small,fourK)
        assertEquals(listOf(fourK,small,large),StreamOrdering.sorted(input,"es",nativeSizeAscending=true,nativeSpanishOnly=true))
        assertEquals(listOf(fourK,large,small),StreamOrdering.sorted(input,"es",nativeSizeAscending=false,nativeSpanishOnly=true))
    }
    @Test fun strictSpanishRejectsEnglishUnknownAndSubtitleOnlyButDoesNotFilterOtherAddons() {
        val english=row("eng","Film English 1080p")
        val unknown=row("unknown","Film MULTI 1080p")
        val subtitles=row("subs","Film English 1080p\nSubtitles: Spanish")
        val latino=row("lat","Film Latino 720p")
        val other=StreamChoice("Other","https://example.org/video","Unknown",addonName="Other")
        assertEquals(listOf(latino,other),StreamOrdering.sorted(listOf(english,unknown,subtitles,other,latino),"es",nativeSpanishOnly=true))
        assertEquals(4,StreamOrdering.sorted(listOf(english,unknown,subtitles,latino),"es",nativeSpanishOnly=false).size)
        assertTrue(StreamOrdering.sorted(listOf(english),"en",nativeSpanishOnly=true).isNotEmpty())
        assertTrue(StreamOrdering.sorted(listOf(latino.copy(audioLanguages=listOf("eng"))),"es",nativeSpanishOnly=true).isEmpty())
    }
    @Test fun materializedNativeSourceKeepsFilterIdentity() {
        val direct=row("eng","Film English").copy(url="https://cdn.example.org/video",sourceUrl="brunio-native://eng/file",nativeSource=null)
        assertTrue(StreamOrdering.sorted(listOf(direct),"es",nativeSpanishOnly=true).isEmpty())
    }
    @Test fun fileLevelLatinoWinsWithinAPackWhoseTitleHasNoAudioLabel() = runBlocking {
        val hash="a".repeat(40)
        val repo=NativeSourceRepository { _,service,_,_,_ ->
            if(service=="Knaben") """{"hits":[{"hash":"$hash","title":"Example.Show.S01.1080p"}]}"""
            else """{"success":true,"data":{"$hash":{"files":[{"name":"Example.Show.S01E01.es-ES.2160p.mkv","size":9000},{"name":"Example.Show.S01E01.Latino.1080p.mkv","size":2000}]}}}"""
        }
        val picked=repo.search("a","key",listOf("Example Show"),"series","2020",1,1,0,"es",spanishOnly=true).single()
        assertTrue(picked.filename.contains("Latino"))
    }
    @Test fun nativeResultCacheSeparatesStrictAndNonStrictRequests() = runBlocking {
        val hash="a".repeat(40)
        val repo=NativeSourceRepository { _,service,_,_,_ ->
            if(service=="Knaben") """{"hits":[{"hash":"$hash","title":"Example.Show.S01.English.1080p"}]}"""
            else """{"success":true,"data":{"$hash":{"files":[{"name":"Example.Show.S01E01.English.mkv","size":1000}]}}}"""
        }
        assertEquals(1,repo.search("a","key",listOf("Example Show"),"series","2020",1,1,0,"es",spanishOnly=false).size)
        assertTrue(repo.search("a","key",listOf("Example Show"),"series","2020",1,1,0,"es",spanishOnly=true).isEmpty())
    }
}
