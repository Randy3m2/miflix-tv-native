package com.brunio.desktop

import kotlin.test.*
import java.nio.file.Files
import java.nio.file.Path

class ArtworkDecoderTest {
    @Test fun pngWithEmbeddedSvgMetadataRemainsRaster() {
        val file=Path.of("../../brunio-pc/assets/brunio_splash.png")
        val bytes=Files.readAllBytes(file)
        assertTrue(bytes.take(1024).toByteArray().toString(Charsets.UTF_8).contains("<svg"))
        assertFalse(isSvgArtwork(bytes))
        val image=decodeArtwork(bytes)
        assertEquals(1672,image.width);assertEquals(941,image.height)
    }
    @Test fun xmlAndStandaloneSvgAreRecognized() {
        assertTrue(isSvgArtwork("\uFEFF  <svg xmlns=\"http://www.w3.org/2000/svg\"/>".toByteArray()))
        assertTrue(isSvgArtwork("<?xml version=\"1.0\"?><svg/>".toByteArray()))
        assertFalse(isSvgArtwork(byteArrayOf(0x52,0x49,0x46,0x46)))
    }
}
