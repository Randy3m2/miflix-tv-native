package com.miflix.native2.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalWindowInfo

/** Focus is owned by Compose; a dialog must not leave its background action filled. */
@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
internal fun actionFocusVisibleRP64(focused:Boolean):Boolean = focused && LocalWindowInfo.current.isWindowFocused
