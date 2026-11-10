package com.miflix.native2.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage

/** RP192: fixed-size artwork with no per-card infinite shimmer animation.
 * Hundreds of animated placeholders previously competed with scroll and focus frames.
 * Coil handles image memory/disk caching; the fixed container prevents layout shifts.
 */
@Composable
internal fun LoadingArtworkRP58(
    model: Any?, contentDescription: String?, modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    var loading by remember(model) { mutableStateOf(model != null) }
    var failed by remember(model) { mutableStateOf(model == null) }
    Box(modifier) {
        if (loading || failed) {
            Box(Modifier.matchParentSize().background(Color(0xFF252528)))
        }
        AsyncImage(
            model = model,
            contentDescription = contentDescription,
            modifier = Modifier.matchParentSize(),
            contentScale = contentScale,
            onLoading = { loading = true; failed = false },
            onSuccess = { loading = false; failed = false },
            onError = { loading = false; failed = true }
        )
    }
}
