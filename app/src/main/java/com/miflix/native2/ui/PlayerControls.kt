package com.miflix.native2.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.TrackSelectionOverride
import androidx.tv.material3.Text
import com.miflix.native2.model.SubtitleChoice

private fun playbackTime(ms: Long): String {
    val seconds=ms.coerceAtLeast(0)/1000
    return if(seconds>=3600) "%d:%02d:%02d".format(seconds/3600,seconds/60%60,seconds%60)
    else "%02d:%02d".format(seconds/60,seconds%60)
}

/** Compact TV controls. Only the focused action has a white background. */
@Composable
internal fun CompactPlayerControls(
    request: PlayerRequest, paused: Boolean, position: Long, duration: Long, speed: Float,
    maximized: Boolean, playFocus: FocusRequester, onInteraction: () -> Unit, onPlay: () -> Unit,
    onResize: () -> Unit, onMenu: (String) -> Unit, onLinks: () -> Unit, onSeek: (Long) -> Unit,
    seekable: Boolean, party: Boolean, onParty: () -> Unit, onClose: () -> Unit, onMaximize: () -> Unit
) {
    val seekFocus=remember { FocusRequester() }
    var focusedLabel by remember { mutableStateOf("") }
    Box(Modifier.fillMaxSize()) {
        Row(Modifier.align(Alignment.TopEnd).padding(top=20.dp,end=24.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            PlayerIconButton("close",tr("Salir","Exit"),onInteraction=onInteraction,onClick=onClose,onLabel={ focusedLabel=it })
            PlayerIconButton("maximize",if(maximized) tr("Restaurar tamaño","Restore size") else tr("Maximizar","Maximize"),onInteraction=onInteraction,onClick=onMaximize,onLabel={ focusedLabel=it })
        }
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Transparent,Color(0xE6000000))))
            .padding(start=24.dp,end=24.dp,top=30.dp,bottom=16.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
            var titleFocused by remember { mutableStateOf(false) }
            Text(request.item.title,color=if(titleFocused) Color.Black else Color.White,fontSize=22.sp,lineHeight=26.sp,maxLines=1,
                modifier=Modifier.onFocusChanged { titleFocused=it.isFocused; if(it.isFocused) onInteraction() }
                    .background(if(titleFocused) Color.White else Color.Transparent,RoundedCornerShape(6.dp))
                    .semantics { contentDescription=request.item.title+" · "+tr("Opciones de contenido y Party","Content and Party options") }
                    .clickable { onInteraction(); onMenu("content") }.padding(horizontal=4.dp,vertical=2.dp))
            Text(if(request.item.type=="series") "S${request.season} E${request.episode} · ${request.stream.name}" else request.stream.name,
                color=Color(0xFFBBBBBB),fontSize=12.sp,lineHeight=15.sp,maxLines=1)
            Text(focusedLabel,color=Color.White,fontSize=12.sp,lineHeight=16.sp,maxLines=2,modifier=Modifier.fillMaxWidth().height(32.dp))
            PlayerSeekBar(position,duration,seekable,seekFocus,playFocus,onInteraction,onSeek)
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                PlayerIconButton(if(paused) "play" else "pause",if(paused) tr("Reproducir","Play") else tr("Pausar","Pause"),Modifier.focusRequester(playFocus).focusProperties { up=seekFocus },onInteraction,onPlay,onLabel={ focusedLabel=it })
                PlayerIconButton("resize",tr("Agrandar / encoger","Zoom / Fit"),onInteraction=onInteraction,onClick=onResize,onLabel={ focusedLabel=it })
                PlayerIconButton("speed","${tr("Velocidad","Speed")} · ${speed}×",onInteraction=onInteraction,onClick={onMenu("speed")},onLabel={ focusedLabel=it })
                PlayerIconButton("cc",tr("Subtítulos","Subtitles"),onInteraction=onInteraction,onClick={onMenu("subtitles")},onLabel={ focusedLabel=it })
                PlayerIconButton("audio",tr("Pista de audio","Audio track"),onInteraction=onInteraction,onClick={onMenu("audio")},onLabel={ focusedLabel=it })
                PlayerIconButton("links",tr("Enlaces de reproducción","Playback links"),onInteraction=onInteraction,onClick=onLinks,onLabel={ focusedLabel=it })
                PlayerIconButton("party",if(party) tr("Mi Party","My Party") else tr("Crear Party","Create Party"),onInteraction=onInteraction,onClick=onParty,onLabel={ focusedLabel=it })
                Spacer(Modifier.weight(1f))
                Text(if(duration>0) "${playbackTime(position)} / ${playbackTime(duration)}" else if(request.live) tr("EN VIVO","LIVE") else playbackTime(position),
                    color=Color.White,fontSize=14.sp,lineHeight=18.sp)
            }
        }
    }
}

@Composable
private fun PlayerIconButton(icon: String, label: String, modifier: Modifier=Modifier, onInteraction: () -> Unit, onClick: () -> Unit, onLabel: (String) -> Unit = {}) {
    var focused by remember { mutableStateOf(false) }
    Box(modifier.size(44.dp).onFocusChanged { focused=it.isFocused; if(it.isFocused) { onInteraction(); onLabel(label) } }
        .background(if(focused) Color.White else Color.Transparent,CircleShape)
        .semantics { contentDescription=label }
        .clickable { onInteraction(); onClick() },contentAlignment=Alignment.Center) {
        PlayerGlyph(icon,if(focused) Color.Black else Color.White,Modifier.size(23.dp))

    }
}

@Composable
private fun PlayerGlyph(icon: String, color: Color, modifier: Modifier) {
    Canvas(modifier) {
        val w=size.width; val h=size.height; val stroke=Stroke(2.dp.toPx())
        fun line(x1:Float,y1:Float,x2:Float,y2:Float)=drawLine(color,Offset(w*x1,h*y1),Offset(w*x2,h*y2),2.dp.toPx())
        fun rectangle(x:Float,y:Float,a:Float,b:Float)=drawRect(color,Offset(w*x,h*y),Size(w*a,h*b),style=stroke)
        when(icon) {
            "play" -> drawPath(Path().apply { moveTo(w*.28f,h*.15f); lineTo(w*.85f,h*.5f); lineTo(w*.28f,h*.85f); close() },color)
            "pause" -> { drawRect(color,Offset(w*.22f,h*.17f),Size(w*.18f,h*.66f)); drawRect(color,Offset(w*.60f,h*.17f),Size(w*.18f,h*.66f)) }
            "close" -> { line(.2f,.2f,.8f,.8f); line(.8f,.2f,.2f,.8f) }
            "maximize" -> { line(.1f,.35f,.1f,.1f);line(.1f,.1f,.35f,.1f);line(.65f,.1f,.9f,.1f);line(.9f,.1f,.9f,.35f);line(.9f,.65f,.9f,.9f);line(.9f,.9f,.65f,.9f);line(.35f,.9f,.1f,.9f);line(.1f,.9f,.1f,.65f) }
            "resize" -> { rectangle(.1f,.2f,.8f,.6f);line(.22f,.65f,.42f,.45f);line(.22f,.65f,.22f,.46f);line(.22f,.65f,.41f,.65f);line(.78f,.35f,.58f,.55f);line(.78f,.35f,.78f,.54f);line(.78f,.35f,.59f,.35f) }
            "speed" -> { drawArc(color,180f,180f,false,Offset(w*.1f,h*.2f),Size(w*.8f,h*.65f),style=stroke);line(.15f,.8f,.85f,.8f);line(.5f,.7f,.76f,.3f); drawCircle(color,w*.06f,Offset(w*.5f,h*.7f)) }
            "cc" -> { rectangle(.04f,.2f,.92f,.6f);drawArc(color,60f,240f,false,Offset(w*.17f,h*.34f),Size(w*.26f,h*.32f),style=stroke);drawArc(color,60f,240f,false,Offset(w*.56f,h*.34f),Size(w*.26f,h*.32f),style=stroke) }
            "audio" -> { rectangle(.24f,.08f,.52f,.84f);drawCircle(color,w*.08f,Offset(w*.5f,h*.3f),style=stroke);drawCircle(color,w*.16f,Offset(w*.5f,h*.66f),style=stroke) }
            "links" -> { drawRoundRect(color,Offset(w*.04f,h*.38f),Size(w*.55f,h*.3f),cornerRadius=androidx.compose.ui.geometry.CornerRadius(w*.14f),style=stroke);drawRoundRect(color,Offset(w*.41f,h*.26f),Size(w*.55f,h*.3f),cornerRadius=androidx.compose.ui.geometry.CornerRadius(w*.14f),style=stroke);line(.35f,.55f,.65f,.4f) }
            "party" -> { drawCircle(color,w*.14f,Offset(w*.34f,h*.3f),style=stroke);drawCircle(color,w*.11f,Offset(w*.75f,h*.35f),style=stroke);drawArc(color,180f,180f,false,Offset(w*.08f,h*.52f),Size(w*.52f,h*.38f),style=stroke);drawArc(color,180f,180f,false,Offset(w*.60f,h*.6f),Size(w*.30f,h*.25f),style=stroke) }

        }
    }
}

/** Left/right seeks ten seconds while this bar has focus; up/down uses normal TV navigation. */
@Composable
private fun PlayerSeekBar(position: Long, duration: Long, enabled: Boolean, seekFocus: FocusRequester, playFocus: FocusRequester, onInteraction: () -> Unit, onSeek: (Long) -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val fraction=if(duration>0) (position.toFloat()/duration).coerceIn(0f,1f) else 0f
    Box(Modifier.fillMaxWidth().height(30.dp).focusRequester(seekFocus).focusProperties { down=playFocus }.onFocusChanged { focused=it.isFocused; if(it.isFocused) onInteraction() }
        .semantics { contentDescription=tr("Barra de reproducción: izquierda o derecha para avanzar diez segundos","Playback bar: left or right to seek ten seconds") }
        .onPreviewKeyEvent { event ->
            val code=event.nativeKeyEvent.keyCode
            if(code==android.view.KeyEvent.KEYCODE_DPAD_LEFT || code==android.view.KeyEvent.KEYCODE_DPAD_RIGHT) {
                if(event.type==KeyEventType.KeyDown) { onInteraction(); if(enabled) onSeek((position+if(code==android.view.KeyEvent.KEYCODE_DPAD_LEFT) -10000 else 10000).coerceIn(0,duration)) }; true
            } else false
        }.focusable()
        .pointerInput(duration,enabled) { detectTapGestures { if(enabled && size.width>0) { onInteraction();onSeek((it.x/size.width*duration).toLong().coerceIn(0,duration)) } } }
        .pointerInput(duration,enabled) { detectDragGestures { change,_ -> if(enabled && size.width>0) { change.consume();onInteraction();onSeek((change.position.x/size.width*duration).toLong().coerceIn(0,duration)) } } },contentAlignment=Alignment.Center) {
        Canvas(Modifier.fillMaxWidth().height(if(focused) 8.dp else 5.dp)) {
            drawRoundRect(Color(0xFF555555),cornerRadius=androidx.compose.ui.geometry.CornerRadius(size.height))
            if(fraction>0) drawRoundRect(if(focused) Color.White else Color(0xFFE50914),size=Size(size.width*fraction,size.height),cornerRadius=androidx.compose.ui.geometry.CornerRadius(size.height))
            if(focused && enabled) drawCircle(Color.White,7.dp.toPx(),Offset(size.width*fraction,size.height/2))
        }
    }
}

@Composable
internal fun PlayerOptionsDialog(menu: String, player: Player, tracks: Tracks, speed: Float,
    onSpeed: (Float) -> Unit, externalSubtitles: List<SubtitleChoice>, subtitleLoading: Boolean, onExternalSubtitle: (SubtitleChoice) -> Unit, series: Boolean, party: Boolean, onEpisodes: () -> Unit, onParty: () -> Unit, onClose: () -> Unit) {
    val first=remember(menu) { FocusRequester() }
    LaunchedEffect(menu) { withFrameNanos { }; first.requestFocus() }
    val title=when(menu) { "content" -> tr("Contenido / Party","Content / Party"); "speed" -> tr("Velocidad","Speed"); "audio" -> tr("Pista de audio","Audio track"); else -> tr("Subtítulos","Subtitles") }
    Dialog(onDismissRequest=onClose,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Column(Modifier.width(550.dp).heightIn(max=460.dp).background(Color(0xF0181818),RoundedCornerShape(18.dp)).padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            Text(title,color=Color.White,fontSize=24.sp,lineHeight=29.sp)
            LazyColumn(Modifier.weight(1f,false),verticalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(4.dp)) {
                when(menu) {
                    "content" -> {
                        if(series) item { FocusButton(tr("Episodios","Episodes"),modifier=Modifier.focusRequester(first)) { onClose();onEpisodes() } }
                        item { FocusButton(if(party) tr("Reacciones / Chat","React / Chat") else tr("Crear sala","Create Party"),modifier=if(series) Modifier else Modifier.focusRequester(first)) { onClose();onParty() } }
                    }
                    "speed" -> listOf(.5f,.75f,1f,1.25f,1.5f,1.75f,2f).forEachIndexed { index,value -> item { FocusButton("${if(speed==value) "✓ " else ""}${value}×",modifier=if(index==0) Modifier.focusRequester(first) else Modifier) { onSpeed(value);onClose() } } }
                    else -> {
                        val type=if(menu=="audio") C.TRACK_TYPE_AUDIO else C.TRACK_TYPE_TEXT
                        item { FocusButton(if(type==C.TRACK_TYPE_TEXT) tr("Desactivados","Off") else tr("Automático","Automatic"),modifier=Modifier.focusRequester(first)) {
                            player.trackSelectionParameters=player.trackSelectionParameters.buildUpon().clearOverridesOfType(type).setTrackTypeDisabled(type,type==C.TRACK_TYPE_TEXT).build();onClose()
                        } }
                        var count=0
                        tracks.groups.filter { it.type==type }.forEach { group ->
                            for(index in 0 until group.length) if(group.isTrackSupported(index)) {
                                count++
                                val format=group.getTrackFormat(index)
                                val label=listOfNotNull(format.label?.takeIf { it.isNotBlank() },format.language?.takeIf { it.isNotBlank() },format.codecs?.takeIf { it.isNotBlank() },if(format.channelCount>0) "${format.channelCount} ch" else null).distinct().joinToString(" · ").ifBlank { "${tr("Pista","Track")} $count" }
                                item { FocusButton("${if(group.isTrackSelected(index)) "✓ " else ""}$label",modifier=Modifier.fillMaxWidth()) {
                                    player.trackSelectionParameters=player.trackSelectionParameters.buildUpon().setTrackTypeDisabled(type,false).setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup,index)).build();onClose()
                                } }
                            }
                        }
                        if(type==C.TRACK_TYPE_TEXT) {
                            if(subtitleLoading) item { Text(tr("Buscando subtítulos externos…","Finding external subtitles…"),color=Color(0xFFBBBBBB)) }
                            externalSubtitles.forEach { subtitle -> item { FocusButton(subtitle.label,modifier=Modifier.fillMaxWidth()) { onExternalSubtitle(subtitle);onClose() } } }
                        }
                        if(count==0) item { Text(tr("No hay pistas disponibles en este enlace todavía.","No tracks available in this link yet."),color=Color(0xFFBBBBBB)) }
                    }
                }
            }
            FocusButton(tr("Cerrar","Close")) { onClose() }
        }
    }
}
