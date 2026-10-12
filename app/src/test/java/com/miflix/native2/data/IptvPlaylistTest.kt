package com.miflix.native2.data

import org.junit.Assert.*
import org.junit.Test

class IptvPlaylistTest {
    @Test fun quotedCommaGroupsAndAlternativeLinks() {
        val text="""#EXTM3U
#EXTINF:-1 tvg-id="test" tvg-logo="https://example.org/logo,a.png" group-title="News;General",Canal Uno
#EXTVLCOPT:http-user-agent=ExampleTV
https://example.org/live.m3u8
#EXTINF:-1 tvg-id="test" group-title="News",Canal Uno HD
https://example.org/hd.m3u8
"""
        val rows=IptvPlaylist.parse(text)
        assertEquals(2,rows.size);assertEquals(rows[0].id,rows[1].id)
        assertEquals("Canal Uno",rows[0].name);assertEquals(setOf("News","General"),rows[0].groups)
        assertEquals("https://example.org/logo,a.png",rows[0].logo)
        assertEquals("ExampleTV",rows[0].headers["User-Agent"]);assertTrue(rows[1].headers.isEmpty())
    }
    @Test fun rejectsNonHttpAndDrmAndNeverCarriesHeadersToNextEntry() {
        val text="""#EXTM3U
#EXTINF:-1,Unsupported
plugin://not-a-stream
#EXTINF:-1,Protected
#KODIPROP:inputstream.adaptive.license_key=example
https://example.org/protected.mpd
#EXTINF:-1,Normal
https://example.org/live.m3u8
"""
        assertEquals(listOf("Normal"),IptvPlaylist.parse(text).map { it.name })
    }
    @Test fun identityDoesNotDependOnExpiringStreamUrl() {
        fun parse(url: String)=IptvPlaylist.parse("#EXTM3U\n#EXTINF:-1 tvg-id=\"test\",Channel\n$url").single()
        assertEquals(parse("https://a.test/one").id,parse("https://a.test/two").id)
        assertEquals("television",IptvPlaylist.searchKey("Televisión"))
    }
    @Test fun exactDuplicateStreamsCollapse() {
        val entry="#EXTINF:-1 tvg-id=\"test\",Channel\nhttps://example.org/live\n"
        assertEquals(1,IptvPlaylist.parse("#EXTM3U\n"+entry+entry).size)
    }
}
