package com.miflix.native2.ui

import androidx.compose.foundation.focusGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.focusProperties
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.focus.onFocusChanged
import com.miflix.native2.data.HorizontalRepeatGate

internal val LocalHorizontalFocusAnchor=androidx.compose.runtime.staticCompositionLocalOf<com.miflix.native2.data.HorizontalFocusAnchor?> {null}

internal fun Modifier.smoothHorizontalDpad(canRepeat: () -> Boolean = {true}): Modifier = composed {
    val gate=remember {HorizontalRepeatGate()}
    val anchor=LocalHorizontalFocusAnchor.current
    this.onFocusChanged {if(!it.hasFocus) {gate.reset();anchor?.reset()}}.onPreviewKeyEvent {event ->
        val consumed=gate.consume(event.nativeKeyEvent.keyCode,event.type==KeyEventType.KeyDown,event.type==KeyEventType.KeyUp,
            event.nativeKeyEvent.repeatCount,event.nativeKeyEvent.eventTime,!canRepeat())
        if(!consumed && event.type==KeyEventType.KeyDown) anchor?.pressed(event.nativeKeyEvent.keyCode,event.nativeKeyEvent.eventTime)
        consumed
    }.focusGroup()
}

/** Fresh presses stay immediate; held repeats retarget at a bounded cadence without a key queue. */
internal fun Modifier.smoothVerticalDpad(canRepeat: () -> Boolean = {true}): Modifier = composed {
    val gate=remember {com.miflix.native2.data.VerticalRepeatGate()}
    this.onFocusChanged {if(!it.hasFocus) gate.reset()}.onPreviewKeyEvent {event ->
        gate.consume(event.nativeKeyEvent.keyCode,event.type==KeyEventType.KeyDown,event.type==KeyEventType.KeyUp,
            event.nativeKeyEvent.repeatCount,event.nativeKeyEvent.eventTime,!canRepeat())
    }
}

/** Commit the row's focus before scrolling its lazy children. Never cancel off-screen entry. */
internal fun Modifier.firstCardOnDown(
    row: androidx.compose.foundation.lazy.LazyListState,
    first: androidx.compose.ui.focus.FocusRequester
): Modifier = composed {
    var active by remember {androidx.compose.runtime.mutableStateOf(false)}
    var enteredDown by remember {androidx.compose.runtime.mutableStateOf(false)}
    androidx.compose.runtime.LaunchedEffect(active,enteredDown) {
        if(!active || !enteredDown) return@LaunchedEffect
        try {
            if(row.firstVisibleItemIndex!=0 || row.firstVisibleItemScrollOffset!=0) row.scrollToItem(0)
            // The focused row is now retained by LazyColumn. Wait for its first card to attach.
            for(attempt in 0 until 3) {
                androidx.compose.runtime.withFrameNanos { }
                if(!active || !enteredDown) break
                if(runCatching {first.requestFocus()}.getOrDefault(false)) break
            }
        } finally {enteredDown=false}
    }
    this.onFocusChanged {active=it.hasFocus}.focusProperties {
        onEnter = {
            enteredDown=requestedFocusDirection==androidx.compose.ui.focus.FocusDirection.Down
            // Let focus search finish, including beyond-bounds layout. Cancelling here can
            // dispose this row before a scroll/requestFocus coroutine ever gets to run.
        }
    }.focusGroup()
}
