package com.miflix.native2.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.miflix.native2.model.MediaSummary
import kotlinx.coroutines.delay

/**
 * RP199: opt-in cinematic ambient gallery. No video decoder, background network loop,
 * or screen remeasurement. Only the selected image is composed at a time.
 */
@Composable
internal fun AmbientCinemaRP199(
    titles: List<MediaSummary>,
    onClose: () -> Unit,
    onDetails: (MediaSummary) -> Unit,
) {
    val films=remember(titles) {
        titles.distinctBy {it.cloudId}
            .filter {!(it.backdrop ?: it.poster).isNullOrBlank()}
            .take(30)
    }
    if(films.isEmpty()) {
        androidx.compose.runtime.LaunchedEffect(Unit) {onClose()}
        return
    }
    var current by remember {mutableIntStateOf(0)}
    var paused by remember {mutableStateOf(false)}
    val item=films[current.coerceIn(0,films.lastIndex)]
    LaunchedEffect(current,paused) {
        if(!paused && films.size>1) {
            delay(12_000)
            current=(current+1)%films.size
        }
    }
    BackHandler {onClose()}
    Dialog(onDismissRequest=onClose,
        properties=DialogProperties(usePlatformDefaultWidth=false,decorFitsSystemWindows=false)) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            Crossfade(targetState=item,animationSpec=tween(500),label="ambientCinema") { film ->
                AsyncImage(
                    model=film.backdrop ?: film.poster,
                    contentDescription=film.title,
                    modifier=Modifier.fillMaxSize(),
                    contentScale=ContentScale.Crop
                )
            }
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
                listOf(Color.Black.copy(alpha=.42f),Color.Transparent,Color.Black.copy(alpha=.88f))
            )))
            Column(Modifier.align(Alignment.BottomStart).fillMaxWidth(.78f)
                .padding(start=48.dp,bottom=46.dp,end=20.dp),verticalArrangement=Arrangement.spacedBy(9.dp)) {
                Text(tr("BRUNIO · CINE AMBIENTAL","BRUNIO · AMBIENT CINEMA"),
                    color=Color(0xFFECECEC),fontSize=12.sp,fontWeight=FontWeight.Bold)
                Text(item.title,color=Color.White,fontSize=30.sp,fontWeight=FontWeight.Bold,
                    maxLines=2,overflow=TextOverflow.Ellipsis)
                Text(listOf(item.year, if(item.type=="series") tr("Serie","Series") else tr("Película","Movie"))
                    .filter {it.isNotBlank()}.joinToString(" · "),
                    color=Color(0xFFD5D5DA),fontSize=14.sp)
                Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    FocusButton(if(paused) tr("▶ Continuar","▶ Resume") else tr("Ⅱ Pausar","Ⅱ Pause"),
                        horizontalPadding=18.dp) {paused=!paused}
                    FocusButton(tr("Siguiente →","Next →"),horizontalPadding=18.dp) {
                        current=(current+1)%films.size
                    }
                    FocusButton(tr("Detalles","Details"),horizontalPadding=18.dp) {
                        onDetails(item)
                    }
                    FocusButton(tr("Cerrar","Close"),horizontalPadding=18.dp,onClick=onClose)
                }
            }
        }
    }
}
