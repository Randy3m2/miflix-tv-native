package com.miflix.native2.ui

import android.view.View
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal class SmoothFocusState(val rootView: View) {
    var owner: Any?=null
    var rectangle by mutableStateOf<Rect?>(null)
    var radius by mutableStateOf(12f)
    var visible by mutableStateOf(false)
    fun select(id: Any,bounds: Rect,corners: Float) {
        owner=id; rectangle=bounds; radius=corners; visible=true
    }
    fun release(id: Any) { if(owner===id) { owner=null; visible=false } }
}
internal val LocalSmoothFocus=staticCompositionLocalOf<SmoothFocusState?> { null }

@Composable
internal fun usesSmoothFocus(): Boolean = LocalSmoothFocus.current?.rootView===LocalView.current.rootView

/** One animated outline per screen, drawn without participating in focus or video layout. */
@Composable
internal fun SmoothFocusHost(content: @Composable BoxScope.() -> Unit) {
    val root=LocalView.current.rootView
    val state=remember(root) { SmoothFocusState(root) }
    var origin by remember { mutableStateOf(Offset.Zero) }
    Box(Modifier.fillMaxSize().clipToBounds().onGloballyPositioned { origin=it.boundsInRoot().topLeft }) {
        CompositionLocalProvider(LocalSmoothFocus provides state) { content() }
        val bounds=state.rectangle
        if(bounds!=null) {
            val x by animateFloatAsState(bounds.left-origin.x,tween(150),label="contentFocusX")
            val y by animateFloatAsState(bounds.top-origin.y,tween(150),label="contentFocusY")
            val w by animateFloatAsState(bounds.width,tween(150),label="contentFocusWidth")
            val h by animateFloatAsState(bounds.height,tween(150),label="contentFocusHeight")
            val radius by animateFloatAsState(state.radius,tween(150),label="contentFocusCorners")
            Canvas(Modifier.matchParentSize()) {
                if(state.visible && w>0 && h>0) {
                    val inset=2.dp.toPx()
                    drawRoundRect(Color.White,Offset(x-inset,y-inset),Size(w+2*inset,h+2*inset),
                        CornerRadius(radius.dp.toPx()),style=Stroke(2.dp.toPx()))
                }
            }
        }
    }
}

internal fun Modifier.smoothFocusFrame(radius: Dp=12.dp): Modifier = composed {
    val host=LocalSmoothFocus.current
    // Dialogs use a different root. Their normal local focus border stays active.
    if(host==null || host.rootView!==LocalView.current.rootView) this
    else {
        val id=remember { Any() }
        var focused by remember { mutableStateOf(false) }
        var bounds by remember { mutableStateOf<Rect?>(null) }
        DisposableEffect(host,id) { onDispose { host.release(id) } }
        this.onGloballyPositioned {
            bounds=it.boundsInRoot()
            if(focused) bounds?.let { rectangle -> host.select(id,rectangle,radius.value) }
        }.onFocusChanged {
            focused=it.isFocused
            if(focused) bounds?.let { rectangle -> host.select(id,rectangle,radius.value) }
            else host.release(id)
        }
    }
}
