package com.miflix.native2.data
import org.junit.Assert.*
import org.junit.Test
class ChannelMetadataRP50Test {
 @Test fun extractsOriginalChannelNumberWithQuery() {
  assertEquals(43,ChannelMetadataRP50.number("https://example.org/free/account/list/43.m3u8?token=abc"))
  assertEquals(0,ChannelMetadataRP50.number("https://example.org/master.m3u8"))
 }
 @Test fun readsChannelNameFromSegmentDirectory() {
  val value=ChannelMetadataRP50.parse("https://example.org/43.m3u8","#EXTM3U\n#EXTINF:8.6,\nhttp://cdn.example.org/get_ts/account/list/Animaniacs/_576013.ts\n")
  assertEquals("Animaniacs",value.name);assertEquals(43,value.number)
 }
 @Test fun decodesNamesWithoutChangingPlusSigns() {
  val value=ChannelMetadataRP50.parse("https://example.org/42.m3u8","#EXTM3U\n/get_ts/a/b/Canal%20Espa%C3%B1ol+/_1.ts")
  assertEquals("Canal Español+",value.name)
 }
 @Test fun doesNotTreatEpisodeTitlesOrGenericDirectoriesAsChannelNames() {
  assertEquals("Canal 42",ChannelMetadataRP50.parse("https://example.org/42.m3u8","#EXTM3U\n#EXTINF:8.0,Episode 1\nhttps://cdn.example.org/live/segments/1.ts").name)
  assertEquals("Canal 42",ChannelMetadataRP50.parse("https://example.org/42.m3u8","<html>Error</html>").name)
 }
 @Test fun explicitNameDoesNotNeedNetwork() {
  val v=ChannelMetadataRP50.resolve("https://example.invalid/43.m3u8?token=abc","Mi canal")
  assertEquals("Mi canal",v.name);assertEquals(43,v.number)
 }
 @Test fun readsExplicitHlsTitle() {
  val v=ChannelMetadataRP50.parse("https://example.org/43.m3u8","#EXTM3U\n#EXT-X-SESSION-DATA:DATA-ID=\"com.apple.hls.title\",VALUE=\"Canal Uno\"\n")
  assertEquals("Canal Uno",v.name)
 }
}
