package com.brunio.desktop

import kotlin.test.*
import org.junit.Assume.assumeTrue

class VlcSmokeTest {
    @Test fun decodesOriginalVideoAndReleasesPlayer() {
        val media=System.getenv("BRUNIO_SMOKE_MEDIA")
        assumeTrue("Native test requires its generated video fixture", !media.isNullOrBlank())
        val engine=VlcEngine()
        try {
            engine.play(media!!)
            val deadline=System.nanoTime()+12_000_000_000L
            var frame:VlcEngine.Frame?=null
            while(System.nanoTime()<deadline && frame==null && !engine.failed.get()) {
                frame=engine.frame.getAndSet(null)
                if(frame==null)Thread.sleep(20)
            }
            assertFalse(engine.failed.get(),"VLC signaled a decode failure")
            assertNotNull(frame,"Native decoder did not deliver a frame")
            assertEquals(320,frame.width);assertEquals(180,frame.height)
            assertEquals(320*180,frame.pixels.size)
            assertTrue(frame.pixels.distinct().size>10,"The test pattern should contain multiple colors")
        } finally { engine.close() }
    }
}
