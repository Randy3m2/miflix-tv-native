package com.miflix.native2.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

internal val QuickPartyEmojis = listOf("😂","🔥","😱","🍿","⚰️")

/** A focusable popup occupies only the top strip; playback continues behind it. */
@Composable
internal fun QuickReactionBar(onSend: (String) -> Unit,onClose: () -> Unit) {
    val first=remember { FocusRequester() }
    val topInset=with(LocalDensity.current) { 28.dp.roundToPx() }
    val openingGesture=remember { com.miflix.native2.data.QuickReactionGestureGate() }
    Popup(alignment=Alignment.TopCenter,offset=IntOffset(0,topInset),onDismissRequest=onClose,
        properties=PopupProperties(focusable=true,dismissOnBackPress=true,dismissOnClickOutside=true)) {
        Row(Modifier.onPreviewKeyEvent { event ->
            openingGesture.consume(event.nativeKeyEvent.keyCode,
                event.type==KeyEventType.KeyDown,event.type==KeyEventType.KeyUp,
                event.nativeKeyEvent.repeatCount)
        }.focusGroup().background(Panel,RoundedCornerShape(16.dp))
            .border(1.dp,PanelBorder,RoundedCornerShape(16.dp)).padding(12.dp),
            horizontalArrangement=Arrangement.spacedBy(10.dp),verticalAlignment=Alignment.CenterVertically) {
            QuickPartyEmojis.forEachIndexed { index,emoji ->
                FocusButton(emoji,modifier=if(index==0) Modifier.focusRequester(first) else Modifier,
                    horizontalPadding=18.dp) { onSend(emoji);onClose() }
            }
            FocusButton("×",horizontalPadding=16.dp) { onClose() }
        }
        LaunchedEffect(Unit) { withFrameNanos { };runCatching { first.requestFocus() } }
    }
}
