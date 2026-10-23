package com.miflix.native2.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import kotlinx.coroutines.launch

@Composable
internal fun EasySynopsisRP85(title:String,synopsis:String,close:()->Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest=close) {
        val scroll=rememberScrollState()
        val focus=remember {FocusRequester()}
        val scope=rememberCoroutineScope()
        LaunchedEffect(Unit) {withFrameNanos {};runCatching {focus.requestFocus()}}
        Column(Modifier.width(680.dp).heightIn(max=450.dp).background(Color(0xFF141414),RoundedCornerShape(22.dp)).padding(24.dp),
            verticalArrangement=Arrangement.spacedBy(16.dp)) {
            Text(title,color=Color.White,fontSize=27.sp,maxLines=2)
            Box(Modifier.weight(1f,false).fillMaxWidth().focusRequester(focus).onPreviewKeyEvent {event ->
                val direction=when(event.key) {Key.DirectionDown->1;Key.DirectionUp->-1;else->0}
                val canScroll=if(direction>0) scroll.value<scroll.maxValue else scroll.value>0
                if(direction!=0 && canScroll) {
                    if(event.type==KeyEventType.KeyDown) scope.launch {scroll.animateScrollTo((scroll.value+direction*110).coerceIn(0,scroll.maxValue))}
                    true
                } else false
            }.focusable().verticalScroll(scroll)) {
                Text(synopsis,color=Color.White,fontSize=23.sp,lineHeight=32.sp)
            }
            FocusButton(tr("Cerrar","Close"),Modifier.fillMaxWidth(),onClick=close)
        }
    }
}
