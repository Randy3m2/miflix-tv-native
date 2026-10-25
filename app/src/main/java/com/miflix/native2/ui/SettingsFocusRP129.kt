@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.miflix.native2.ui

import androidx.compose.foundation.onFocusedBoundsChanged
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.miflix.native2.data.HomeStartupPolicyRP129
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal val LocalSettingsViewportRP129=staticCompositionLocalOf<Rect?> {null}

/** Reveal a clipped heading when an early control receives focus; don't pin the whole tall card. */
@Composable
internal fun Modifier.settingsCardHeaderRP129():Modifier {
    val viewport=LocalSettingsViewportRP129.current
    val requester=remember {BringIntoViewRequester()}
    val scope=rememberCoroutineScope()
    val own=remember {arrayOfNulls<LayoutCoordinates>(1)}
    val target=remember {arrayOfNulls<LayoutCoordinates>(1)}
    val work=remember {arrayOfNulls<Job>(1)}
    val margin=with(LocalDensity.current) {12.dp.toPx()}
    val firstActions=with(LocalDensity.current) {160.dp.toPx()}
    DisposableEffect(Unit) {onDispose {work[0]?.cancel()}}
    return bringIntoViewRequester(requester).onGloballyPositioned {own[0]=it}.onFocusedBoundsChanged {focused ->
        if(target[0]!==focused) {work[0]?.cancel();target[0]=focused}
        val card=own[0]
        if(focused!=null && card!=null && focused.isAttached && card.isAttached && viewport!=null && work[0]?.isActive!=true) {
            val bounds=card.boundsInWindow()
            val focus=focused.boundsInWindow()
            if(viewport.height>2*margin && bounds.width>0f && HomeStartupPolicyRP129.revealHeader(bounds.top,focus.top,viewport.top,margin,firstActions)) {
                val height=minOf(bounds.height,(focus.bottom-bounds.top+margin).coerceAtLeast(margin))
                if(height<=viewport.height-2*margin) work[0]=scope.launch {
                    if(card.isAttached && focused.isAttached) requester.bringIntoView(Rect(0f,0f,bounds.width,height))
                }
            }
        }
    }
}
