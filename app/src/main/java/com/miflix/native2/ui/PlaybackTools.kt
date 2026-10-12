package com.miflix.native2.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.exoplayer.ExoPlayer
import androidx.tv.material3.Text
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun SubtitleTimingDialog(value: Long, apply: (Long) -> Unit, close: () -> Unit) {
    var draft by remember { mutableStateOf(value) }
    val first=remember { FocusRequester() }
    LaunchedEffect(Unit) { withFrameNanos { };first.requestFocus() }
    Dialog(onDismissRequest=close) {
        Column(Modifier.width(540.dp).background(Panel,RoundedCornerShape(22.dp)).padding(24.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
            Text(tr("Sincronizar subtítulos","Subtitle timing"),color=Color.White,fontSize=24.sp)
            Text(String.format(Locale.US,"%+.2f s",draft/1000.0),color=Color.White,fontSize=32.sp)
            Text(tr("Negativo: aparecen antes. Positivo: aparecen después. Se aplica a este contenido.","Negative: show earlier. Positive: show later. Applies to this playback."),color=Muted)
            Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                FocusButton("−1 s",modifier=Modifier.focusRequester(first)) { draft=(draft-1000).coerceAtLeast(-30000) }
                FocusButton("−0.25 s") { draft=(draft-250).coerceAtLeast(-30000) }
                FocusButton("+0.25 s") { draft=(draft+250).coerceAtMost(30000) }
                FocusButton("+1 s") { draft=(draft+1000).coerceAtMost(30000) }
            }
            Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                FocusButton(tr("Restablecer","Reset")) { draft=0 }
                FocusButton(tr("Aplicar","Apply")) { apply(draft);close() }
                FocusButton(tr("Cancelar","Cancel")) { close() }
            }
        }
    }
}

@Composable
fun PlaybackInfoDialog(player: ExoPlayer, request: PlayerRequest, close: () -> Unit) {
    var video by remember { mutableStateOf<Format?>(player.videoFormat) }
    var audio by remember { mutableStateOf<Format?>(player.audioFormat) }
    var buffer by remember { mutableStateOf(0L) }
    LaunchedEffect(player) { while(true) { video=player.videoFormat;audio=player.audioFormat;buffer=player.totalBufferedDuration;delay(1000) } }
    val first=remember { FocusRequester() }
    LaunchedEffect(Unit) { withFrameNanos { };first.requestFocus() }
    fun rate(f: Format?): String {
        val bitrate=f?.averageBitrate?.takeIf { it>0 } ?: f?.peakBitrate?.takeIf { it>0 }
        return bitrate?.let { String.format(Locale.US,"%.2f Mbps",it/1_000_000.0) } ?: tr("No informado","Not reported")
    }
    Dialog(onDismissRequest=close,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Column(Modifier.width(690.dp).heightIn(max=510.dp).background(Panel,RoundedCornerShape(22.dp)).padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            Text(tr("Información de reproducción","Playback information"),color=Color.White,fontSize=25.sp)
            LazyColumn(Modifier.weight(1f,false),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                item { Text(request.item.title,color=Color.White,fontSize=21.sp) }
                item { InfoLine(tr("Resolución","Resolution"),video?.takeIf { it.width>0 && it.height>0 }?.let { "${it.width} × ${it.height}" } ?: "—") }
                item { InfoLine("Video",listOfNotNull(video?.sampleMimeType,video?.codecs,video?.frameRate?.takeIf { it>0 }?.let { "$it fps" }).joinToString(" · ").ifBlank { "—" }) }
                item { InfoLine(tr("Bitrate de vídeo (declarado)","Video bitrate (declared)"),rate(video)) }
                item { InfoLine(tr("Formato de audio","Audio format"),listOfNotNull(audio?.sampleMimeType,audio?.codecs,audio?.channelCount?.takeIf { it>0 }?.let { "$it ch" },audio?.sampleRate?.takeIf { it>0 }?.let { "$it Hz" },audio?.language).joinToString(" · ").ifBlank { "—" }) }
                item { InfoLine(tr("Bitrate de audio (declarado)","Audio bitrate (declared)"),rate(audio)) }
                item { InfoLine(tr("Nombre del archivo","File name"),request.stream.filename.ifBlank { tr("No informado por el proveedor","Not supplied by provider") }) }
                item { InfoLine(tr("Fuente","Source"),request.stream.name) }
                item { InfoLine("Buffer",String.format(Locale.US,"%.1f s",buffer/1000.0)) }
            }
            FocusButton(tr("Cerrar","Close"),modifier=Modifier.focusRequester(first)) { close() }
        }
    }
}
@Composable
private fun InfoLine(label: String,value: String) { Column { Text(label,color=Muted,fontSize=14.sp);Text(value,color=Color.White,fontSize=18.sp) } }

internal fun android.content.Context.playbackActivity(): androidx.activity.ComponentActivity? {
    var current: android.content.Context=this
    while(current is android.content.ContextWrapper) {
        if(current is androidx.activity.ComponentActivity) return current
        val next=current.baseContext
        if(next===current) break
        current=next
    }
    return current as? androidx.activity.ComponentActivity
}
