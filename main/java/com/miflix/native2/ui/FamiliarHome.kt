package com.miflix.native2.ui

import androidx.compose.runtime.staticCompositionLocalOf

/** Optional Home-only presentation. Other screens retain their existing dimensions. */
internal val LocalSimpleHome=staticCompositionLocalOf {false}
