package com.miflix.native2.data

import com.miflix.native2.model.StreamChoice
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class MandanguitaRP62Test {
    private val url=BundledAddonsRP12.mandanguita
    private fun row(id:String,audio:String)=StreamChoice("Test","https://video.test/$id","1080p",audioLanguages=listOf(audio))
    @Test fun installedByDefaultAndRemovableWithoutLosingPersonalAddons() {
        val personal="https://my-comet.test/manifest.json"
        val effective=BundledAddonsRP12.effective(listOf(personal,url,url))
        assertEquals(1,effective.count {it==url});assertTrue(effective.contains(personal))
        assertFalse(BundledAddonsRP12.effective(listOf(personal,url),setOf(url)).contains(url))
    }
    @Test fun spanishFirstEnglishSecondAndManualOrderStillWins() {
        val mand=InstalledSourceAddonRP2(AddonPriorityRP2.id(url),"Mandanguita TV","x.pepo.ar")
        val deep=InstalledSourceAddonRP2("deep","Deepflix","mg.homelabx.qzz.io")
        val comet=InstalledSourceAddonRP2("comet","Comet","comet.test")
        val rows=listOf(comet,deep,mand)
        assertEquals(mand,AddonPriorityRP2.ordered(rows,emptyList(),"es")[0])
        assertEquals(mand,AddonPriorityRP2.ordered(rows,emptyList(),"en")[1])
        assertEquals(listOf(comet,deep,mand),AddonPriorityRP2.ordered(rows,listOf("comet","deep",mand.id),"es"))
    }
    @Test fun primaryThenSecondaryThenUnknownAndOther() {
        val en=row("en","en");val es=row("es","es-419");val fr=row("fr","fr");val unknown=row("u","und")
        assertEquals(listOf(en,es,unknown,fr),StreamOrdering.sorted(listOf(fr,unknown,es,en),"en",secondaryAudio="es"))
        assertEquals(listOf(es,unknown,fr),StreamOrdering.sorted(listOf(fr,unknown,es),"en",secondaryAudio="es"))
        assertEquals(listOf(fr,en,es),StreamOrdering.sorted(listOf(es,en,fr),"fr",secondaryAudio="en"))
        assertTrue(StreamLanguagePriority.languageMatches("es-MX","es"))
        assertFalse(StreamLanguagePriority.languageMatches("en","es"))
    }
    @Test fun subtitleLabelsDoNotInventSecondaryAudio() {
        assertFalse(StreamLanguagePriority.hasAudio("en","Spanish subtitles","es"))
        assertFalse(StreamLanguagePriority.hasAudio("","French subtitles","fr"))
    }
    @Test fun directVodWithUnknownSizeUsesImdbMovieAndEpisodeRoutes()=runBlocking {
        val requested=mutableListOf<String>()
        val repo=StreamRepository(url) {request,_ ->
            requested.add(request)
            if(request.endsWith("manifest.json")) """{"name":"RenTV","resources":[{"name":"stream","types":["movie","series"]}]}"""
            else """{"streams":[{"name":"RenTV","url":"https://video.test/vod/title.ts?token=example","behaviorHints":{"notWebReady":true,"proxyHeaders":{"request":{"Referer":"https://example.test/"}}}}]}"""
        }
        val movies=repo.resolve("tt0468569","movie",maxBytes=1000)
        assertEquals(1,movies.size);assertEquals("Mandanguita TV",movies.single().addonName)
        assertEquals("https://example.test/",movies.single().requestHeaders["Referer"])
        assertEquals("Mandanguita TV",repo.sourceAddonNames().single())
        assertTrue(StreamFilters.allowed(movies.single(),"movie",null,1000))
        assertEquals(1,repo.resolve("tt0944947","series",2,3).size)
        assertTrue(requested.any {it.endsWith("/stream/movie/tt0468569.json")})
        assertTrue(requested.any {it.endsWith("/stream/series/tt0944947:2:3.json")})
        assertFalse(requested.any {"/catalog/" in it})
    }
}
