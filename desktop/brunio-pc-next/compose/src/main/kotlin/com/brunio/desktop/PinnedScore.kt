package com.brunio.desktop
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun PinnedScore(utilities:Utilities) {
    val event=utilities.pinned?:return
    var position by remember{mutableStateOf(Offset.Zero)}
    LaunchedEffect(event.optString("id"),event.optString("league")){while(utilities.pinned!=null){delay(30000);utilities.refreshPinned()}}
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val density=LocalDensity.current
        val minX=with(density){(320.dp-maxWidth).toPx().coerceAtMost(0f)}
        val maxY=with(density){(maxHeight-190.dp).toPx().coerceAtLeast(0f)}
        Surface(Modifier.align(Alignment.TopEnd).padding(top=80.dp,end=24.dp).offset{IntOffset(position.x.roundToInt(),position.y.roundToInt())}
            .width(296.dp).pointerInput(minX,maxY){detectDragGestures{change,drag->change.consume();position=Offset((position.x+drag.x).coerceIn(minX,0f),(position.y+drag.y).coerceIn(0f,maxY))}},
            shape=RoundedCornerShape(24.dp),color=Color(0xEB222228)) {
            Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                Row {Text(event.optString("league"),Modifier.weight(1f),style=MaterialTheme.typography.labelMedium);TextButton(onClick={utilities.pinned=null}){Text("Cerrar")}}
                Text(event.optString("title"),maxLines=2)
                Row(horizontalArrangement=Arrangement.spacedBy(14.dp),verticalAlignment=Alignment.CenterVertically) {
                    val logos=event.optJSONArray("logos");val scores=event.optJSONArray("scores")
                    if(logos!=null)for(i in 0 until logos.length()){RemoteArtwork(logos.optString(i),Modifier.size(32.dp));Text(scores?.optString(i)?:"")}
                }
                Text(event.optString("status"),color=Color.LightGray,style=MaterialTheme.typography.labelSmall)
            }
        }
    }
}
