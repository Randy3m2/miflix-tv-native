package com.miflix.native2.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.miflix.native2.R

/** Same artwork and reveal timing as the approved preview, without its loop fade. */
@Composable
fun BruniOSplashScreen() {
    val resources = LocalContext.current.resources
    val artwork = remember(resources) {
        val source = BitmapFactory.decodeResource(resources, R.drawable.brunio_splash)
        val pixels = IntArray(source.width * source.height)
        source.getPixels(pixels, 0, source.width, 0, 0, source.width, source.height)
        for (i in pixels.indices) {
            val c = pixels[i]
            val brightness = maxOf((c ushr 16) and 255, (c ushr 8) and 255, c and 255)
            val alpha = ((brightness - 24) * 255 / 24).coerceIn(0, 255)
            pixels[i] = (alpha shl 24) or (c and 0x00FFFFFF)
        }
        val result = Bitmap.createBitmap(pixels, source.width, source.height, Bitmap.Config.ARGB_8888)
        source.recycle()
        result.asImageBitmap()
    }
    var elapsed by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        while (true) withFrameNanos { elapsed = (it - start) / 1_000_000_000f }
    }
    fun smooth(value: Float): Float {
        val p = value.coerceIn(0f, 1f)
        return p * p * (3f - 2f * p)
    }
    Canvas(Modifier.fillMaxSize().background(Color(0xFF050914))
        .semantics { contentDescription = "BruniO" }) {
        val scale = minOf(size.width / 1672f, size.height / 941f)
        withTransform({
            translate((size.width - 1672f * scale) / 2f, (size.height - 941f * scale) / 2f)
            scale(scale, scale, Offset.Zero)
        }) {
            clipRect(616f, 197f, 1070f, 554f) {
                drawImage(artwork, alpha = smooth((elapsed - .18f) / .75f))
            }
            val edges = listOf(426f, 601f, 697f, 846f, 993f, 1048f, 1252f)
            for (i in 0..5) {
                val p = smooth((elapsed - .65f - i * .24f) / .4f)
                clipRect(edges[i], 752f - 200f * p, edges[i + 1], 755f) {
                    drawImage(artwork, alpha = p)
                }
            }
            val alpha = smooth((elapsed - 1.1f) / .45f)
            val center = Offset(836f, 829.5f)
            drawCircle(Color(0xFF2B3144), 21f, center, alpha = alpha, style = Stroke(3.5f))
            for (i in 0..59) {
                val p = i / 59f
                drawArc(Color(25 / 255f + 180 / 255f * p, 105 / 255f + 120 / 255f * p,
                    178 / 255f + 70 / 255f * p, alpha * (.1f + .9f * p)),
                    elapsed * 270f + i * 3.6f, 5f, false,
                    Offset(center.x - 21f, center.y - 21f), Size(42f, 42f), style = Stroke(5.2f))
            }
        }
    }
}
