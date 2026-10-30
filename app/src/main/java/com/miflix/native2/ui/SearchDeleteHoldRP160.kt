package com.miflix.native2.ui

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.semantics.*
import kotlinx.coroutines.*

internal fun Modifier.searchDeleteHoldRP160(focused:Boolean,delete:()->Unit,clear:()->Unit):Modifier=composed {
    val remove by rememberUpdatedState(delete)
    val removeAll by rememberUpdatedState(clear)
    val scope=rememberCoroutineScope()
    var held by remember {mutableStateOf(false)}
    var cleared by remember {mutableStateOf(false)}
    var timer by remember {mutableStateOf<Job?>(null)}
    fun cancel() {timer?.cancel();timer=null;held=false}
    DisposableEffect(focused) {if(!focused) cancel();onDispose {cancel()}}
    this.onPreviewKeyEvent {event ->
        val key=event.nativeKeyEvent
        if(key.keyCode !in setOf(android.view.KeyEvent.KEYCODE_DPAD_CENTER,android.view.KeyEvent.KEYCODE_ENTER,android.view.KeyEvent.KEYCODE_NUMPAD_ENTER,android.view.KeyEvent.KEYCODE_SPACE)) false
        else {
            if(key.action==android.view.KeyEvent.ACTION_DOWN && !held) {
                held=true;cleared=false
                timer=scope.launch {delay(3000);if(held) {cleared=true;removeAll()}}
            } else if(key.action==android.view.KeyEvent.ACTION_UP) {
                val single=held && !cleared && !key.isCanceled;cancel();if(single) remove()
            }
            true
        }
    }.pointerInput(Unit) {
        detectTapGestures(onPress={
            coroutineScope {
                var all=false
                val job=launch {delay(3000);all=true;removeAll()}
                val released=tryAwaitRelease();job.cancel()
                if(released && !all) remove()
            }
        })
    }.semantics {
        onClick {remove();true}
        onLongClick(label=tr("Borrar todo","Clear all")) {removeAll();true}
    }
}
