package com.miflix.native2.data

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import java.net.URI

class AddonStartupRP12Test {
 private val manifest="""{"name":"Test","resources":["stream"]}"""
 private fun stream(lang:String)= """{"streams":[{"name":"$lang","title":"1080p $lang","url":"https://video.test/$lang"}]}"""
 @Test fun metadataAndStreamStartInParallel()=runBlocking {
  val started=CompletableDeferred<Unit>()
  val repo=StreamRepository("https://fast.test/manifest.json") {url,timeout ->
   if(url.endsWith("manifest.json")) {
    assertEquals(2000,timeout)
    withTimeout(1000) {started.await()}
    manifest
   } else {assertEquals(8000,timeout);started.complete(Unit);stream("English")}
  }
  assertEquals(1,repo.resolve("tt1","movie",preferredAudio="en").size)
 }
 @Test fun fastProviderIsPublishedBeforeSlowProviderFinishes()=runBlocking {
  val releaseSlow=CompletableDeferred<Unit>()
  val batches=mutableListOf<String>()
  val repo=StreamRepository("https://slow.test/manifest.json") {url,_ ->
   when {
    url.endsWith("manifest.json") -> manifest
    url.contains("slow.test") -> {releaseSlow.await();stream("Latino")}
    else -> stream("English")
   }
  }
  repo.addManifest("https://fast.test/manifest.json")
  val rows=withTimeout(5000) {
   repo.resolve("tt1","movie",preferredAudio="es") {batch ->
    val name=batch.single().name
    batches.add(name)
    if(name=="English") releaseSlow.complete(Unit)
   }
  }
  assertEquals(listOf("English","Latino"),batches)
  assertEquals(listOf("Latino","English"),rows.map {it.name})
 }
 @Test fun allProviderResultsKeepSpanishBeforeEnglish()=runBlocking {
  val repo=StreamRepository("https://english.test/manifest.json") {url,_ ->
   if(url.endsWith("manifest.json")) manifest else if(url.contains("english")) stream("English")
   else {delay(40);stream("Latino")}
  }
  repo.addManifest("https://spanish.test/manifest.json")
  assertEquals(listOf("Latino","English"),repo.resolve("tt1","movie",preferredAudio="es").map {it.name})
 }
 @Test fun metadataFailureDoesNotHideValidDirectStreams()=runBlocking {
  val repo=StreamRepository("https://test/manifest.json") {url,_ ->
   if(url.endsWith("manifest.json")) throw java.io.IOException("Unavailable")
   stream("English")
  }
  assertEquals(1,repo.resolve("tt1","movie",preferredAudio="en").size)
 }
 @Test fun catalogOnlyIsNotQueriedAgainAfterMetadataInspection()=runBlocking {
  var streams=0
  val repo=StreamRepository("https://test/manifest.json") {url,_ ->
   if(url.endsWith("manifest.json")) """{"name":"Catalog","resources":["catalog"]}"""
   else {streams++;stream("English")}
  }
  repo.inspectSourceAddons()
  assertTrue(repo.resolve("tt1","movie").isEmpty())
  assertEquals(0,streams)
 }
 @Test fun torrentResponsesAreDiagnosedInsteadOfBeingClaimedPlayable()=runBlocking {
  val repo=StreamRepository("https://torrent.test/manifest.json") {url,_ ->
   if(url.endsWith("manifest.json")) manifest
   else """{"streams":[{"name":"Torrent","infoHash":"1234567890123456789012345678901234567890","fileIdx":0}]}"""
  }
  assertTrue(repo.resolve("tt1","movie").isEmpty())
  assertTrue(repo.hasTorrentOnlyResults())
  assertTrue(repo.status(repo.manifests().first()).contains("Torrents ignored: 1"))
 }
 @Test fun factoryListAndMigrationKeepPersonalAddonsAndRemovals() {
  val factory=BundledAddonsRP12.defaults
  assertEquals(5,factory.size)
  assertEquals(listOf("mg.homelabx.qzz.io","x.pepo.ar","addon.peerflix.mov","addonlatam.duckdns.org","torrentio.strem.fun"),factory.map {URI(it).host})
  val custom="https://my-comet.test/config/manifest.json"
  assertEquals(listOf(custom)+factory,BundledAddonsRP12.effective(listOf(custom)+BundledAddonsRP12.retired.toList()))
  assertFalse(BundledAddonsRP12.effective(emptyList(),setOf(factory[1])).contains(factory[1]))
  val old=BundledAddonsRP12.retired.first()
  assertTrue(BundledAddonsRP12.effective(listOf(old),restored=setOf(old)).contains(old))
  assertEquals(5,BundledAddonsRP12.effective(factory).size)
 }
}
