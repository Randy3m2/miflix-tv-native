package com.miflix.native2.data

import org.junit.Assert.*
import org.junit.Test

class DiscoveryRP167Test {
 @Test fun prebufferReleasesConstructedResourceOnAllocationFailure() = kotlinx.coroutines.runBlocking {
  val engine=WarmConstructionRP92(kotlinx.coroutines.Dispatchers.Default,kotlinx.coroutines.Dispatchers.Unconfined)
  val resource=Any();var releases=0
  try {engine.run(build={resource to Unit},valid={true},configure={_,_->throw OutOfMemoryError("simulated allocation pressure")},release={assertSame(resource,it);releases++});fail("Expected allocation failure")}catch(_:OutOfMemoryError){}
  assertEquals(1,releases)
 }

 @Test fun previewDoesNotCutOffEpisodeOrAppearOnShortClips(){assertFalse(NextPreviewPolicyRP167.visible(90000,120000,-1,false));assertFalse(NextPreviewPolicyRP167.visible(1200000,1200000,-1,true));assertTrue(NextPreviewPolicyRP167.visible(1110000,1200000,-1,false));assertFalse(NextPreviewPolicyRP167.visible(1109999,1200000,-1,false))}
 @Test fun creditsTriggerIsBoundedToLastThreeMinutes(){assertTrue(NextPreviewPolicyRP167.visible(1020000,1200000,600000,false));assertFalse(NextPreviewPolicyRP167.visible(1019999,1200000,600000,false));assertEquals(1L,NextPreviewPolicyRP167.secondsRemaining(1199999,1200000))}
 @Test fun remoteRejectsPublicHostsCredentialsAndUnsupportedCommands(){for(url in listOf("8.8.8.8","https://192.168.1.2","http://evil.example","http://u:p@192.168.1.2","192.168.1.2:9999","192.168.1.2?x=1"))assertThrows(IllegalArgumentException::class.java){RemotePolicyRP167.endpoint(url)};assertEquals("http://192.168.1.2:8765",RemotePolicyRP167.endpoint("192.168.1.2"));assertFalse(RemotePolicyRP167.valid("shell","x"));assertFalse(RemotePolicyRP167.valid("text","x".repeat(513)));assertTrue(RemotePolicyRP167.valid("subtitles",""))}
 @Test fun widgetLinksRecognizeDatesWithoutTreatingOtherContentAsCalendar(){assertTrue(CalendarLinkRP167.matches("brunio://calendar?date=2026-11-02"));assertEquals("2026-11-02",CalendarLinkRP167.date("brunio://calendar?date=2026-11-02"));assertNull(CalendarLinkRP167.date("brunio://calendar?date=2026-02-30"));assertFalse(CalendarLinkRP167.matches("https://calendar"));assertFalse(CalendarLinkRP167.matches("brunio://content/movie/1"))}
 @Test fun icsEscapesInputAndUsesExclusiveAllDayEnd(){val e=CalendarIcsRP167.Entry("x","Title, semi; colon\nBEGIN:VEVENT","2026-11-02",null);val text=CalendarIcsRP167.encode("u:p",listOf(e),0);assertTrue(text.contains("DTEND;VALUE=DATE:20261103\r\n"));assertTrue(text.contains("SUMMARY:Title\\, semi\\; colon\\nBEGIN:VEVENT"));assertEquals(1,Regex("(?m)^BEGIN:VEVENT\\r?$").findAll(text).count())}
 @Test fun icsHasStableIdentityUtcTimesAndUtf8SafeFolding(){val e=CalendarIcsRP167.Entry("x","⚾Película".repeat(25),"2026-11-02",0);val a=CalendarIcsRP167.encode("u:p",listOf(e),0);val b=CalendarIcsRP167.encode("u:p",listOf(e),60000);assertEquals(a.lineSequence().first {it.startsWith("UID:")},b.lineSequence().first {it.startsWith("UID:")});assertTrue(a.contains("DTSTART:19700101T000000Z"));a.split("\r\n").forEach {assertTrue(it.toByteArray(Charsets.UTF_8).size<=75)};assertEquals(a,a.toByteArray(Charsets.UTF_8).toString(Charsets.UTF_8))}
 @Test fun remoteAuthenticationRequiresPairingAndDeduplicatesBySession(){var now=0L;val access=LocalCastAccess("123456"){now};assertNull(access.connect("000000","","192.168.1.3"));val token=access.connect("123456","","192.168.1.3");assertNotNull(token);assertFalse(access.authenticated("bad",""));assertTrue(access.authenticated(token,""));assertFalse(access.duplicate(token,"request-123"));assertTrue(access.duplicate(token,"request-123"));now=1800001;assertFalse(access.authenticated(token,""))}
}
