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
        if(event.type==KeyEventType.KeyDown && event.nativeKeyEvent.repeatCount>0 &&
            event.nativeKeyEvent.keyCode in 21..22 && !canRepeat()) true
        else gate.consume(event.nativeKeyEvent.keyCode,event.type==KeyEventType.KeyDown,event.type==KeyEventType.KeyUp,
            event.nativeKeyEvent.repeatCount,event.nativeKeyEvent.eventTime)
    }.focusGroup()
}

/** Fresh presses remain immediate; held vertical repeats cannot queue overlapping scrolls. */
internal fun Modifier.smoothVerticalDpad(canRepeat: () -> Boolean = {true}): Modifier = composed {
    var direction by remember {androidx.compose.runtime.mutableIntStateOf(0)}
    var acceptedAt by remember {androidx.compose.runtime.mutableStateOf<Long?>(null)}
    this.onPreviewKeyEvent {event ->
        val code=event.nativeKeyEvent.keyCode
        if(code!=19 && code!=20) false
        else if(event.type==KeyEventType.KeyUp) {direction=0;acceptedAt=null;false}
        else if(event.type!=KeyEventType.KeyDown) false
        else if(event.nativeKeyEvent.repeatCount>0 && !canRepeat()) true
        else {
            val time=event.nativeKeyEvent.eventTime
            val previous=acceptedAt
            if(event.nativeKeyEvent.repeatCount==0 || direction!=code || previous==null || time<previous || time-previous>=220L) {
                direction=code;acceptedAt=time;false
            } else true
        }
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
