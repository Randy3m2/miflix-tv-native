package com.nuvio.app.features.brunio

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * BruniO screens must not inherit Nuvio's window chrome foreground.
 * The shell can render with a light Material palette over a black background.
 */
internal val BrunioDarkColors = darkColorScheme(
    primary = Color(0xFFB49AFF),
    onPrimary = Color(0xFF171020),
    background = Color(0xFF0B0D10),
    onBackground = Color(0xFFF7F7FA),
    surface = Color(0xFF16181E),
    onSurface = Color(0xFFF5F6FA),
    surfaceVariant = Color(0xFF24262E),
    onSurfaceVariant = Color(0xFFD0D2DB),
    outline = Color(0xFF888C9A),
    outlineVariant = Color(0xFF5C6070),
    error = Color(0xFFFF8A93),
    onError = Color(0xFF170C10),
)

@Composable
internal fun BrunioReadableSurface(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = BrunioDarkColors) {
        // MaterialTheme alone does NOT override LocalContentColor. Nuvio's
        // root ContentColor may be black even though our BruniO pages are dark.
        CompositionLocalProvider(LocalContentColor provides BrunioDarkColors.onSurface) {
            Surface(
                color = BrunioDarkColors.background,
                contentColor = BrunioDarkColors.onSurface,
                content = content,
            )
        }
    }
}
