package com.miflix.native2.ui

import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.CoroutineScope

internal val LocalHomeShelfMotion=staticCompositionLocalOf<HomeShelfMotion?> {null}

/** RP60: compatibility with header reset callers. Compose now owns vertical scrolling. */
internal class HomeShelfMotion(@Suppress("UNUSED_PARAMETER") scope:CoroutineScope) {
    fun reset() = Unit
}
