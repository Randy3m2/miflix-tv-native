package com.miflix.native2.data
import org.junit.Assert.*
import org.junit.Test
class EpgParserTest {
    private val xml="""<tv><channel id="News.ar"><display-name>News Argentina</display-name></channel><programme channel="News.ar" start="20261012090000 -0300" stop="20261012100000 -0300"><title>Morning &amp; News</title><desc>Details</desc></programme><programme channel="News.ar" start="20261012100000 -0300" stop="20261012110000 -0300"><title>Next show</title></programme></tv>"""
    @Test fun timezoneAndBoundaryAreCorrect() {
        val now=EpgParser.time("20261012123000 +0000")!!
        val guide=EpgParser.parse(xml,now)
        val (current,next)=guide.nowNext("News.ar",now)
        assertEquals("Morning & News",current!!.title);assertEquals("Next show",next!!.title)
        assertEquals("Next show",guide.nowNext("News.ar",current.stop).first!!.title)
        assertNull(guide.nowNext("News.ar",next.stop).first)
    }
    @Test fun wrongCountryIsNotAutomaticallyMatched() {
        val guide=EpgParser.parse(xml,EpgParser.time("20261012123000 +0000")!!)
        val channel=LiveChannel("iptv:hash","tv","News",null,"",tvgId="News.us")
        assertNull(guide.match(channel,null));assertEquals("News.ar",guide.match(channel,"News.ar"))
        assertEquals("News.ar",guide.match(channel.copy(tvgId="News.ar@HD"),null))
    }
    @Test fun externalEntityInputIsRejected() {
        try { EpgParser.parse("""<!DOCTYPE tv [<!ENTITY x SYSTEM "file:///etc/passwd">]><tv/>""");fail() } catch(_:IllegalArgumentException) { }
    }
    @Test fun invalidDatesRejected() { assertNull(EpgParser.time("20261399120000 +0000")) }
}
