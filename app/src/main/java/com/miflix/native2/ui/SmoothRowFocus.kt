package com.miflix.native2.ui

import androidx.compose.foundation.focusGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.focusProperties
import kotlinx.coroutines.launch
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.focus.onFocusChanged
import com.miflix.native2.data.HorizontalRepeatGate

internal fun Modifier.smoothHorizontalDpad(canRepeat: () -> Boolean = {true}): Modifier = composed {
    val gate=remember {HorizontalRepeatGate()}
    this.onFocusChanged {if(!it.hasFocus) gate.reset()}.onPreviewKeyEvent {event ->
        gate.consume(event.nativeKeyEvent.keyCode,event.type==KeyEventType.KeyDown,event.type==KeyEventType.KeyUp,
            event.nativeKeyEvent.repeatCount,event.nativeKeyEvent.eventTime,!canRepeat())
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

/** Down always enters at item zero, regardless of the expanded card's center above. */
internal fun Modifier.firstCardOnDown(
    row: androidx.compose.foundation.lazy.LazyListState,
    first: androidx.compose.ui.focus.FocusRequester
): Modifier = composed {
    val scope=androidx.compose.runtime.rememberCoroutineScope()
    var pending by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<kotlinx.coroutines.Job?>(null) }
    this.focusProperties {
        onEnter = {
            if(requestedFocusDirection==androidx.compose.ui.focus.FocusDirection.Down) {
                if(row.firstVisibleItemIndex==0 && row.firstVisibleItemScrollOffset==0) {
                    first.requestFocus()
                } else {
                    cancelFocusChange()
                    if(pending?.isActive!=true) pending=scope.launch {
                        row.scrollToItem(0)
                        androidx.compose.runtime.withFrameNanos { }
                        runCatching { first.requestFocus() }
                    }
                }
            }
        }
    }
}
