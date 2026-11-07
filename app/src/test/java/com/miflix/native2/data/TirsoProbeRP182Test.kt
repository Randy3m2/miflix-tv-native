package com.miflix.native2.data

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import java.net.URI
import java.util.Collections

class TirsoProbeRP182Test {
    private val manifest="""{"name":"Fixture","resources":["stream"]}"""
    private val playable="""{"streams":[{"name":"Movie","url":"https://video.test/first","title":"1080p"},{"name":"Other","url":"https://video.test/second","title":"720p"}]}"""
    @Test fun firstAcceptedLinkCancelsTheSlowProvider()=runBlocking {
        val started=CompletableDeferred<Unit>()
        val cancelled=CompletableDeferred<Unit>()
        val repo=StreamRepository("https://fast.test/manifest.json") {url,timeout->
            if(url.endsWith("manifest.json")) manifest
            else if(URI(url).host=="slow.test") {
                started.complete(Unit)
                try {awaitCancellation()} finally {cancelled.complete(Unit)}
            } else {assertEquals(4000,timeout);started.await();playable}
        }
        repo.addManifest("https://slow.test/manifest.json")
        val result=withTimeout(1500) {repo.firstSourceRP182("tt1","movie")}
        assertEquals(listOf("https://video.test/first"),result.map {it.url})
        assertTrue(cancelled.isCompleted)
    }
    @Test fun probeMovesToTheNextPairOnlyWhenTheFirstPairHasNoSources()=runBlocking {
        val hosts=Collections.synchronizedSet(mutableSetOf<String>())
        val repo=StreamRepository("https://empty.test/manifest.json") {url,_->
            if(url.endsWith("manifest.json")) manifest else {
                hosts.add(URI(url).host)
                if(URI(url).host=="third.test") playable else """{"streams":[]}"""
            }
        }
        repo.addManifest("https://also-empty.test/manifest.json")
        repo.addManifest("https://third.test/manifest.json")
        assertEquals(1,repo.firstSourceRP182("tt1","movie").size)
        assertEquals(setOf("empty.test","also-empty.test","third.test"),hosts)
    }
    @Test fun unavailableAndOversizedEntriesAreSkippedBeforeAcceptingOne()=runBlocking {
        val repo=StreamRepository("https://test/manifest.json") {url,_->
            if(url.endsWith("manifest.json")) manifest else """{"streams":[
                {"infoHash":"not-a-direct-url"},
                {"url":"https://video.test/large","behaviorHints":{"videoSize":2000}},
                {"url":"https://video.test/accepted","behaviorHints":{"videoSize":500}},
                {"url":"https://video.test/extra","behaviorHints":{"videoSize":400}}
            ]}"""
        }
        assertEquals(listOf("https://video.test/accepted"),repo.firstSourceRP182("tt1","movie",maxBytes=1000).map {it.url})
    }
}
