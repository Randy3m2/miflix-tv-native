package com.miflix.native2.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.miflix.native2.data.EasyExitHoldRP80
import kotlinx.coroutines.delay

/** A continuous sixty-second hold, followed by release, opens the existing confirmation. */
@Composable
internal fun EasyExitLogoRP80(state:AppState,unlock:()->Unit) {
    val owner=state.localExtrasScope()
    val gate=remember(owner) {EasyExitHoldRP80()}
    val focus=remember {FocusRequester()}
    val latestUnlock by rememberUpdatedState(unlock)
    var focused by remember(owner) {mutableStateOf(false)}
    var held by remember(owner) {mutableStateOf(false)}
    var remaining by remember(owner) {mutableLongStateOf(60L)}
    fun cancel() {gate.cancel();held=false;remaining=60L}
    DisposableEffect(gate) {onDispose {gate.cancel()}}
    LaunchedEffect(state.appForeground) {if(!state.appForeground) cancel()}
    LaunchedEffect(held) {
        while(held) {
            remaining=gate.secondsLeft(android.os.SystemClock.uptimeMillis())
            delay(250)
        }
    }
    Column(Modifier.focusRequester(focus).onFocusChanged {
        focused=it.isFocused
        if(!it.isFocused) cancel()
    }.onPreviewKeyEvent {event ->
        val key=event.nativeKeyEvent
        val confirm=key.keyCode in setOf(android.view.KeyEvent.KEYCODE_DPAD_CENTER,
            android.view.KeyEvent.KEYCODE_ENTER,android.view.KeyEvent.KEYCODE_NUMPAD_ENTER)
        if(!confirm) {
            if(key.action==android.view.KeyEvent.ACTION_DOWN) cancel()
            false
        } else {
            if(!state.appForeground || key.isCanceled) cancel()
            else if(key.action==android.view.KeyEvent.ACTION_DOWN) {
                if(gate.down(key.keyCode,key.eventTime,key.repeatCount)) {held=true;remaining=60L}
            } else if(key.action==android.view.KeyEvent.ACTION_UP) {
                val done=gate.up(key.keyCode,key.eventTime)
                held=false;remaining=60L
                if(done) latestUnlock()
            }
            true
        }
    }.focusable().pointerInput(owner,state.appForeground) {
        if(!state.appForeground) return@pointerInput
        awaitEachGesture {
            val down=awaitFirstDown()
            down.consume()
            focus.requestFocus()
            gate.down(0,android.os.SystemClock.uptimeMillis(),0);held=true;remaining=60L
            val up=waitForUpOrCancellation()
            val done=if(up!=null) gate.up(0,android.os.SystemClock.uptimeMillis()) else false
            up?.consume();cancel()
            if(done) latestUnlock()
        }
    }.semantics {contentDescription=tr("BruniO · Mantén OK 60 segundos y suelta para salir del modo fácil",
        "BruniO · Hold OK for 60 seconds, then release to exit Easy mode")}
        .background(if(focused) Color.White else Panel,RoundedCornerShape(30.dp))
        .border(1.dp,if(focused) Color.White else PanelBorder,RoundedCornerShape(30.dp))
        .padding(horizontal=14.dp,vertical=10.dp),horizontalAlignment=Alignment.CenterHorizontally) {
        Text("BruniO",color=if(focused) Color.Black else Color.White,fontSize=20.sp,fontWeight=FontWeight.Bold)
        if(held) Text(if(remaining==0L) tr("Suelta","Release") else "$remaining s",
            color=if(focused) Color.Black else Color.White,fontSize=12.sp)
    }
}
