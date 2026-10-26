package com.miflix.native2.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider

internal val LocalPlayerMenuSide=staticCompositionLocalOf {false}

/** A single right-side surface; the rest of the video is transparent, with no blur or dim. */
@Composable
internal fun PlayerMenuDialog(onDismissRequest: () -> Unit,properties: DialogProperties=DialogProperties(),
    forceSide: Boolean=false,fullScreen:Boolean=false,content: @Composable () -> Unit) {
    if(LocalMobileBrunioRP133.current) {
        Dialog(onDismissRequest=onDismissRequest,properties=DialogProperties(usePlatformDefaultWidth=false,decorFitsSystemWindows=false)) {
            Box(Modifier.fillMaxSize().safeDrawingPadding().imePadding().background(Color(0xF0080A0F)).padding(12.dp)) {
                CompositionLocalProvider(LocalPlayerMenuSide provides false) {content()}
            }
        }
        return
    }
    if(fullScreen) {
        Dialog(onDismissRequest=onDismissRequest,properties=DialogProperties(
            dismissOnBackPress=properties.dismissOnBackPress,dismissOnClickOutside=properties.dismissOnClickOutside,
            securePolicy=properties.securePolicy,usePlatformDefaultWidth=false,
            decorFitsSystemWindows=properties.decorFitsSystemWindows)) {
            val view=LocalView.current
            DisposableEffect(view) {
                (view.parent as? DialogWindowProvider)?.window?.let {window ->
                    window.setDimAmount(0f)
                    window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
                }
                onDispose { }
            }
            Box(Modifier.fillMaxSize()) {CompositionLocalProvider(LocalPlayerMenuSide provides false) {content()}}
        }
        return
    }
    val side=forceSide || LocalPlayerMenuSide.current
    if(!side) {
        Dialog(onDismissRequest=onDismissRequest,properties=properties,content=content)
        return
    }
    Dialog(onDismissRequest=onDismissRequest,properties=DialogProperties(
        dismissOnBackPress=properties.dismissOnBackPress,dismissOnClickOutside=properties.dismissOnClickOutside,
        securePolicy=properties.securePolicy,usePlatformDefaultWidth=false,
        decorFitsSystemWindows=properties.decorFitsSystemWindows)) {
        val view=LocalView.current
        DisposableEffect(view) {
            (view.parent as? DialogWindowProvider)?.window?.setDimAmount(0f)
            onDispose { }
        }
        val reveal=remember {Animatable(0f)}
        LaunchedEffect(Unit) {reveal.animateTo(1f,tween(160))}
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val width=(maxWidth*.52f).coerceIn(360.dp,560.dp).coerceAtMost(maxWidth)
            Box(Modifier.align(Alignment.CenterEnd).width(width).fillMaxHeight().padding(14.dp)
                .graphicsLayer {translationX=(1f-reveal.value)*size.width}
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xF014141E),RoundedCornerShape(24.dp))
                .border(1.dp,Color(0x405F5A76),RoundedCornerShape(24.dp)),contentAlignment=Alignment.TopStart) {
                CompositionLocalProvider(LocalPlayerMenuSide provides true) {content()}
            }
        }
    }
}
