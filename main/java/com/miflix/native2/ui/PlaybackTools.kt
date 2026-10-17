@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.miflix.native2.ui

import androidx.compose.foundation.focusable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
    PlayerMenuDialog(onDismissRequest=close) {
        Column(Modifier.width(540.dp).heightIn(max=460.dp).verticalScroll(rememberScrollState()).background(Panel,RoundedCornerShape(22.dp)).padding(24.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
            Text(tr("Sincronizar subtítulos","Subtitle timing"),color=Color.White,fontSize=24.sp)
            Text(String.format(Locale.US,"%+.2f s",draft/1000.0),color=Color.White,fontSize=32.sp)
            Text(tr("Negativo: aparecen antes. Positivo: aparecen después. Se aplica a este contenido.","Negative: show earlier. Positive: show later. Applies to this playback."),color=Muted)
            FlowRow(horizontalArrangement=Arrangement.spacedBy(12.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                FocusButton("−1 s",modifier=Modifier.focusRequester(first)) { draft=(draft-1000).coerceAtLeast(-30000) }
                FocusButton("−0.25 s") { draft=(draft-250).coerceAtLeast(-30000) }
                FocusButton("+0.25 s") { draft=(draft+250).coerceAtMost(30000) }
                FocusButton("+1 s") { draft=(draft+1000).coerceAtMost(30000) }
            }
            FlowRow(horizontalArrangement=Arrangement.spacedBy(12.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
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
    var subtitle by remember {mutableStateOf<Format?>(null)}
    LaunchedEffect(player) { while(true) {
        video=player.videoFormat;audio=player.audioFormat;buffer=player.totalBufferedDuration
        subtitle=player.currentTracks.groups.filter {it.type==C.TRACK_TYPE_TEXT}.firstNotNullOfOrNull {group ->
            (0 until group.length).firstOrNull {group.isTrackSelected(it)}?.let {group.getTrackFormat(it)}
        }
        delay(1000)
    } }
    val first=remember { FocusRequester() }
    val presentation=remember(request.stream) {com.miflix.native2.data.StreamPresentation.parse(request.stream.addonName,request.stream.name,request.stream.title,request.stream.filename,request.stream.videoSize)}
    fun rate(f: Format?): String {
        val bitrate=f?.averageBitrate?.takeIf { it>0 } ?: f?.peakBitrate?.takeIf { it>0 }
        return bitrate?.let { String.format(Locale.US,"%.2f Mbps",it/1_000_000.0) } ?: "—"
    }
    PlayerMenuDialog(onDismissRequest=close,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        val view=androidx.compose.ui.platform.LocalView.current
        DisposableEffect(view) {
            (view.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window?.setDimAmount(0f)
            onDispose { }
        }
        LaunchedEffect(Unit) {withFrameNanos { };runCatching {first.requestFocus()} }
        Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Brush.horizontalGradient(
            listOf(Color(0xEC08080E),Color(0xC408080E),Color(0x6008080E)))).padding(horizontal=40.dp,vertical=26.dp)) {
            Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(14.dp)) {
                Row(Modifier.fillMaxWidth(),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                    Text(tr("Información de reproducción","Playback information"),color=Color.White,fontSize=25.sp,modifier=Modifier.weight(1f))
                    FocusButton(tr("Cerrar","Close"),modifier=Modifier.focusRequester(first),onClick=close)
                }
                LazyColumn(Modifier.weight(1f).fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(18.dp),contentPadding=PaddingValues(bottom=24.dp)) {
                    item {InfoLine(tr("ORIGEN","SOURCE"),presentation.heading)}
                    item {Text(request.item.title+if(request.season>0) " · S${request.season} E${request.episode}" else "",color=Color.White,fontSize=20.sp)}
                    item {InfoLine(tr("Motor de reproducción","Playback engine"),"Media3 · ExoPlayer")}
                    item {Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(24.dp)) {
                        Box(Modifier.weight(1f)) {InfoLine(tr("Nombre del archivo","File name"),com.miflix.native2.data.PartySourcePolicy.label(request.stream.filename).ifBlank {"—"})}
                        Box(Modifier.width(160.dp)) {InfoLine(tr("Tamaño del archivo","File Size"),request.stream.videoSize.takeIf {it>0}?.let {String.format(Locale.US,"%.2f GiB",it/1073741824.0)} ?: tr("No informado","Not reported"))}
                    }}
                    item {Text("VIDEO",color=Muted,fontSize=12.sp)}
                    item {Row(horizontalArrangement=Arrangement.spacedBy(26.dp)) {
                        Box(Modifier.weight(1f)) {InfoLine(tr("Códec","Codec"),listOfNotNull(video?.sampleMimeType,video?.codecs).joinToString(" · ").ifBlank {"—"})}
                        Box(Modifier.weight(1f)) {InfoLine(tr("Resolución","Resolution"),video?.takeIf {it.width>0 && it.height>0}?.let {"${it.width} × ${it.height}"} ?: "—")}
                        Box(Modifier.weight(1f)) {InfoLine("Bitrate",rate(video))}
                    }}
                    item {Text("AUDIO",color=Muted,fontSize=12.sp)}
                    item {Row(horizontalArrangement=Arrangement.spacedBy(26.dp)) {
                        Box(Modifier.weight(1f)) {InfoLine(tr("Códec","Codec"),audio?.sampleMimeType ?: "—")}
                        Box(Modifier.weight(1f)) {InfoLine(tr("Canales","Channels"),audio?.channelCount?.takeIf {it>0}?.toString() ?: "—")}
                        Box(Modifier.weight(1f)) {InfoLine(tr("Frecuencia","Sample rate"),audio?.sampleRate?.takeIf {it>0}?.let {"${it/1000.0} kHz"} ?: "—")}
                        Box(Modifier.weight(1f)) {InfoLine(tr("Idioma","Language"),audio?.language ?: "—")}
                    }}
                    item {Row(horizontalArrangement=Arrangement.spacedBy(26.dp)) {
                        Box(Modifier.weight(1f)) {InfoLine(tr("Subtítulos","Subtitles"),listOfNotNull(subtitle?.label,subtitle?.language).joinToString(" · ").ifBlank {tr("Desactivados","Off")})}
                        Box(Modifier.weight(1f)) {InfoLine("Buffer",String.format(Locale.US,"%.1f s",buffer/1000.0))}
                    }}
                }
            }
        }
    }
}
@Composable
private fun InfoLine(label: String,value: String) { Column(Modifier.fillMaxWidth().contentFocusBorder(8.dp).focusable().padding(6.dp)) { Text(label,color=Muted,fontSize=14.sp);Text(value,color=Color.White,fontSize=18.sp) } }

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
