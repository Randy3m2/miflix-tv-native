package com.miflix.native2.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** The outline belongs to its tile, so scrolling cannot leave a trailing screen overlay. */
internal fun Modifier.contentFocusBorder(radius: Dp=12.dp): Modifier = composed {
    var focused by remember { mutableStateOf(false) }
    val width by animateFloatAsState(if(focused) 2f else 0f,tween(80),label="tileOutline")
    this.onFocusChanged { focused=it.isFocused }.drawWithContent {
        drawContent()
        // Remove the previous tile's highlight immediately; only the focused tile animates in.
        if(focused && width>0f) {
            val inset=2.dp.toPx()
            drawRoundRect(Color.White,Offset(-inset,-inset),Size(size.width+2*inset,size.height+2*inset),
                CornerRadius(radius.toPx()),style=Stroke(width.dp.toPx()))
        }
    }
}
