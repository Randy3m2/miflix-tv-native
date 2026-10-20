package com.miflix.native2.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay

/** Fixed artwork slot. Decode completion never changes focus, measures, or scroll position. */
@Composable
internal fun LoadingArtworkRP58(
    model: Any?, contentDescription: String?, modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    var loading by remember(model) { mutableStateOf(model != null) }
    var failed by remember(model) { mutableStateOf(model == null) }
    var shimmer by remember(model) { mutableStateOf(false) }
    LaunchedEffect(loading, model) {
        shimmer = false
        if (loading) { delay(160); shimmer = true }
    }
    Box(modifier) {
        if (loading || failed) Box(Modifier.matchParentSize().background(Color(0xFF252528)))
        AsyncImage(model, contentDescription, Modifier.matchParentSize(), contentScale = contentScale,
            onLoading = { loading = true; failed = false },
            onSuccess = { loading = false; failed = false },
            onError = { loading = false; failed = true })
        if (loading && shimmer) ArtworkShimmerRP58(Modifier.matchParentSize())
    }
}

@Composable
private fun ArtworkShimmerRP58(modifier: Modifier) {
    val clock = rememberInfiniteTransition(label = "artworkLoading")
    val phase = clock.animateFloat(0f, 1f,
        infiniteRepeatable(tween(1250, easing = LinearEasing), RepeatMode.Restart), label = "artworkSweep")
    // Read animation state during drawing only, never during measurement or composition.
    Box(modifier.drawWithContent {
        drawContent()
        val width = size.width * .65f
        val x = -width + phase.value * (size.width + width * 2)
        drawRect(Brush.linearGradient(
            listOf(Color.Transparent, Color(0x22FFFFFF), Color.Transparent),
            Offset(x - width, 0f), Offset(x + width, size.height)))
    })
}
