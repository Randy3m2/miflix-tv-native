package com.miflix.native2.data

import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class NativeSourceRepositoryTest {
    private val hash="a".repeat(40)
    private val title="Example.Show.S01.1080p"
    private fun hits()="""{"hits":[{"hash":"$hash","title":"$title"}]}"""
    private fun cached()="""{"success":true,"data":{"$hash":{"files":[{"name":"Example.Show.S01E01.mkv","size":1000},{"name":"Example.Show.S01E02.mkv","size":2000}]}}}"""
    @Test fun nativeSearchOnlyChecksCacheAndNeverSendsCredentialsToIndex() = runBlocking {
        val paths=mutableListOf<String>()
        val repo=NativeSourceRepository { url,service,headers,body,_ ->
            paths.add(url)
            if(service=="Knaben") { assertTrue(headers.isEmpty());assertFalse(body.orEmpty().contains("test-secret"));hits() }
            else { assertEquals("Bearer test-secret",headers["Authorization"]);cached() }
        }
        val rows=repo.search("account-a","test-secret",listOf("Example Show"),"series","2020",1,2,0)
        assertEquals(1,rows.size);assertEquals("Example.Show.S01E02.mkv",rows.single().filename)
        assertEquals(2000L,rows.single().videoSize);assertTrue(paths.none { it.contains("createtorrent") })
        assertTrue(repo.search("account-a","test-secret",listOf("Example Show"),"series","2020",1,2,1500).isEmpty())
    }
    @Test fun unavailableCacheIsExcluded() = runBlocking {
        val repo=NativeSourceRepository { _,service,_,_,_ -> if(service=="Knaben") hits() else """{"success":true,"data":{}}""" }
        assertTrue(repo.search("a","test-secret",listOf("Example Show"),"series","2020",1,2,0).isEmpty())
    }
    @Test fun selectedFileUsesReturnedIdAndResolvesOnlyOnPlayback() = runBlocking {
        var createCount=0
        val repo=NativeSourceRepository { url,service,_,_,_ -> when {
            service=="Knaben" -> hits()
            url.contains("checkcached") -> cached()
            url.contains("createtorrent") -> { createCount++;"""{"success":true,"data":{"torrent_id":42}}""" }
            url.contains("mylist") -> """{"success":true,"data":{"download_present":true,"files":[{"id":0,"name":"Example.Show.S01E02.mkv","size":2000}]}}"""
            url.contains("requestdl") -> { assertTrue(url.contains("file_id=0"));"""{"success":true,"data":"https://cdn.example.org/file"}""" }
            else -> error("Unexpected request")
        } }
        val choice=repo.search("a","test-secret",listOf("Example Show"),"series","2020",1,2,0).single()
        assertEquals(0,createCount)
        val resolved=repo.materialize(choice,"test-secret") { true }
        assertEquals(1,createCount);assertEquals("https://cdn.example.org/file",resolved.url);assertNull(resolved.nativeSource)
    }
    @Test fun cacheDoesNotCrossAccountsOrCredentials() = runBlocking {
        var indexCalls=0
        val repo=NativeSourceRepository { _,service,_,_,_ -> if(service=="Knaben") { indexCalls++;hits() } else cached() }
        suspend fun search(owner: String,key: String) { repo.search(owner,key,listOf("Example Show"),"series","2020",1,2,0) }
        search("a","key-a");search("a","key-a");assertEquals(1,indexCalls)
        search("b","key-b");assertEquals(2,indexCalls)
        search("b","replacement-key");assertEquals(3,indexCalls)
    }
    @Test fun stalePlaybackNeverRegistersATorrent() = runBlocking {
        var mutations=0
        val repo=NativeSourceRepository { url,service,_,_,_ ->
            if(url.contains("createtorrent")) mutations++
            if(service=="Knaben") hits() else cached()
        }
        val choice=repo.search("a","test-secret",listOf("Example Show"),"series","2020",1,2,0).single()
        assertTrue(runCatching { repo.materialize(choice,"test-secret") { false } }.isFailure)
        assertEquals(0,mutations)
    }
    @Test fun expiredCacheNeverRegistersATorrent() = runBlocking {
        var expired=false;var mutations=0
        val repo=NativeSourceRepository { url,service,_,_,_ ->
            if(url.contains("createtorrent")) mutations++
            if(service=="Knaben") hits() else if(expired) """{"success":true,"data":{}}""" else cached()
        }
        val choice=repo.search("a","test-secret",listOf("Example Show"),"series","2020",1,2,0).single()
        expired=true
        assertTrue(runCatching { repo.materialize(choice,"test-secret") { true } }.isFailure)
        assertEquals(0,mutations)
    }
    @Test fun wrongMovieYearIsRejectedBeforeCacheLookup() = runBlocking {
        var torboxCalls=0
        val repo=NativeSourceRepository { _,service,_,_,_ ->
            if(service=="Knaben") """{"hits":[{"hash":"$hash","title":"Example.Movie.1990.1080p"}]}"""
            else { torboxCalls++;cached() }
        }
        assertTrue(repo.search("a","test-secret",listOf("Example Movie"),"movie","2020",0,0,0).isEmpty())
        assertEquals(0,torboxCalls)
    }
}
