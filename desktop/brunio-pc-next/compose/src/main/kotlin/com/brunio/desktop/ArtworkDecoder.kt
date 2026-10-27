package com.brunio.desktop

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

/** PNG metadata may contain SVG text. Only an XML document can be an SVG image. */
internal fun isSvgArtwork(bytes:ByteArray):Boolean {
    val header=bytes.take(4096).toByteArray().toString(Charsets.UTF_8).trimStart('\uFEFF',' ','\t','\r','\n')
    return header.startsWith("<svg",ignoreCase=true) ||
        header.startsWith("<?xml",ignoreCase=true) && header.contains("<svg",ignoreCase=true)
}

internal fun decodeArtwork(bytes:ByteArray):ImageBitmap {
    if(!isSvgArtwork(bytes)) {
        // ImageIO handles PNG/JPEG/GIF; Skia supplies formats such as WebP.
        return ImageIO.read(ByteArrayInputStream(bytes))?.toComposeImageBitmap()
            ?: Image.makeFromEncoded(bytes).toComposeImageBitmap()
    }
    val data=org.jetbrains.skia.Data.makeFromBytes(bytes)
    val dom=org.jetbrains.skia.svg.SVGDOM(data)
    val surface=org.jetbrains.skia.Surface.makeRasterN32Premul(256,256)
    try {
        surface.canvas.clear(org.jetbrains.skia.Color.TRANSPARENT)
        dom.setContainerSize(256f,256f)
        dom.render(surface.canvas)
        return surface.makeImageSnapshot().toComposeImageBitmap()
    } finally {surface.close();dom.close();data.close()}
}
