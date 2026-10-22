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

private object VerticalFocusIntentRP67 { var revision=0L }

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
        val vertical=event.nativeKeyEvent.keyCode==19 || event.nativeKeyEvent.keyCode==20
        val consumed=if(vertical && event.type==KeyEventType.KeyDown && !canRepeat()) true
        else gate.consume(event.nativeKeyEvent.keyCode,event.type==KeyEventType.KeyDown,event.type==KeyEventType.KeyUp,
            event.nativeKeyEvent.repeatCount,event.nativeKeyEvent.eventTime,false)
        if(vertical && event.type==KeyEventType.KeyDown && !consumed) VerticalFocusIntentRP67.revision++
        consumed
    }
}

/** Snapshot the saved card before native vertical focus search can choose a neighbour. */
internal fun Modifier.firstCardOnDown(
    row: androidx.compose.foundation.lazy.LazyListState,
    first: androidx.compose.ui.focus.FocusRequester,
    restoreIndex: () -> Int = {0},
    restoreFocus: (Int) -> androidx.compose.ui.focus.FocusRequester? = {if(it==0) first else null}
): Modifier = composed {
    var intentRevision by remember {androidx.compose.runtime.mutableLongStateOf(0L)}
    var entrySerial by remember {androidx.compose.runtime.mutableIntStateOf(0)}
    var entryIndex by remember {androidx.compose.runtime.mutableIntStateOf(0)}
    val currentIndex by androidx.compose.runtime.rememberUpdatedState(restoreIndex)
    val currentFocus by androidx.compose.runtime.rememberUpdatedState(restoreFocus)
    androidx.compose.runtime.LaunchedEffect(entrySerial) {
        if(entrySerial==0 || intentRevision!=VerticalFocusIntentRP67.revision) return@LaunchedEffect
        val index=entryIndex.coerceIn(0,(row.layoutInfo.totalItemsCount-1).coerceAtLeast(0))
        if(index !in row.layoutInfo.visibleItemsInfo.map {it.index}) row.scrollToItem(index)
        for(attempt in 0 until 8) {
            androidx.compose.runtime.withFrameNanos { }
            if(intentRevision!=VerticalFocusIntentRP67.revision) return@LaunchedEffect
            if(runCatching {currentFocus(index)?.requestFocus() ?: false}.getOrDefault(false)) break
        }
    }
    this.focusProperties {
        onEnter = {
            val vertical=requestedFocusDirection==androidx.compose.ui.focus.FocusDirection.Down ||
                requestedFocusDirection==androidx.compose.ui.focus.FocusDirection.Up
            if(vertical) {
                // Cancel native spatial selection before any neighbour can overwrite row memory.
                entryIndex=currentIndex().coerceAtLeast(0)
                cancelFocusChange()
                intentRevision=VerticalFocusIntentRP67.revision
                entrySerial++
            }
        }
    }.focusGroup()
}
