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
    seekable: Boolean, party: Boolean, onParty: () -> Unit, onChat: () -> Unit, onReactions: () -> Unit, onQuickReactions: () -> Unit, onClose: () -> Unit, onMaximize: () -> Unit,
    nextAvailable: Boolean, onNext: () -> Unit, onEpisodes: () -> Unit, onQueue: () -> Unit, onInfo: () -> Unit, onPip: () -> Unit, onMultiView: () -> Unit, onMoment: () -> Unit,
    previousChannelVisible:Boolean=false,previousChannelAvailable:Boolean=false,previousChannelName:String="",onPreviousChannel:()->Unit={},onScores:()->Unit={},onStatsRP95:()->Unit={},channelFavoriteRP36: @Composable ()->Unit={},badgePlayerRP26:androidx.media3.exoplayer.ExoPlayer?=null,easyModeRP79:Boolean=false
) {
    if(LocalMobileBrunioRP133.current) {
        MobilePlayerControlsRP133(request,paused,position,duration,seekable,nextAvailable,easyModeRP79,badgePlayerRP26,
            onInteraction,onPlay,onClose,onSeek,onMenu,onLinks,
            {if(request.live) onMenu("channels") else onEpisodes()},onNext,onParty,onChat,onReactions,onMultiView,onStatsRP95,onInfo,onPip)
        return
    }
    if(easyModeRP79) {
        val easySeekFocusRP89=remember {FocusRequester()}
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent,Color(0xF0000000))))
                .padding(30.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
                Text(request.item.title,color=Color.White,fontSize=28.sp,maxLines=2)
                if(!request.live || request.sportsRecordingRP72) {
                    Text(tr("Usa ← y → en la barra para retroceder o adelantar","Use ← and → on the bar to seek"),color=Muted,fontSize=18.sp)
                    PlayerSeekBar(position,duration,seekable,easySeekFocusRP89,playFocus,onInteraction,onSeek,largeRP89=true)
                }
                Row(horizontalArrangement=Arrangement.spacedBy(18.dp),verticalAlignment=Alignment.CenterVertically) {
                    if(!request.live || request.sportsRecordingRP72) FocusButton(if(paused) tr("Reproducir","Play") else tr("Pausar","Pause"),Modifier.focusRequester(playFocus).focusProperties {if(seekable) up=easySeekFocusRP89}) {onInteraction();onPlay()}
                    else FocusButton(tr("Canales","Channels"),Modifier.focusRequester(playFocus)) {onInteraction();onMenu("channels")}
                    FocusButton(tr("Idioma de audio","Audio language")) {onInteraction();onMenu("audio")}
                    if(!request.live && duration>0) Text(playbackTime(position)+" / "+playbackTime(duration),color=Color.White,fontSize=20.sp)
                }
            }
        }
        return
    }
    val seekFocus=remember { FocusRequester() }
    var focusedLabel by remember { mutableStateOf("") }
    Box(Modifier.fillMaxSize()) {
        Row(Modifier.align(Alignment.TopEnd).padding(top=20.dp,end=24.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            PlayerIconButton("score",tr("PIN Live score","PIN Live score"),modifier=if(request.live && !request.sportsRecordingRP72) Modifier.focusRequester(playFocus) else Modifier,onInteraction=onInteraction,onClick=onScores,onLabel={focusedLabel=it})
            if(com.miflix.native2.data.SportsStatsRP95.available(request.live,request.sportsRecordingRP72,request.liveProvider,request.item.title+" "+request.item.overview)) PlayerIconButton("stats",tr("Estadísticas NBA / MLB","NBA / MLB stats"),onInteraction=onInteraction,onClick=onStatsRP95,onLabel={focusedLabel=it})
            if(!request.live) PlayerIconButton("info",tr("Información técnica","Playback info"),onInteraction=onInteraction,onClick=onInfo,onLabel={ focusedLabel=it })
            if(!request.live && seekable) PlayerIconButton("moment",tr("Grabar momento","Mark a moment"),onInteraction=onInteraction,onClick=onMoment,onLabel={ focusedLabel=it })
            PlayerIconButton("pip",tr("Ventana flotante","Picture in Picture"),onInteraction=onInteraction,onClick=onPip,onLabel={ focusedLabel=it })
            if(request.live) PlayerIconButton("grid",if(request.liveProvider=="lidom") tr("SplitScreen · LIDOM","SplitScreen · LIDOM") else tr("Hasta 4 canales","Up to 4 channels"),onInteraction=onInteraction,onClick=onMultiView,onLabel={ focusedLabel=it })
        }
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Transparent,Color(0xE6000000))))
            .padding(start=24.dp,end=24.dp,top=30.dp,bottom=16.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
            var titleFocused by remember { mutableStateOf(false) }
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            Text(request.item.title,color=if(titleFocused) Color.White else Color.White,fontSize=22.sp,lineHeight=26.sp,maxLines=1,
                modifier=Modifier.weight(1f,fill=false).onFocusChanged { titleFocused=it.isFocused; if(it.isFocused) onInteraction() }
                    .background(GlassActionBrushRP108(titleFocused),RoundedCornerShape(6.dp))
                    .semantics { contentDescription=request.item.title+" · "+tr("Opciones de contenido y Party","Content and Party options") }
                    .clickable { onInteraction(); onMenu("content") }.padding(horizontal=4.dp,vertical=2.dp))
            channelFavoriteRP36()
            }
            PlaybackBadgesRP26(badgePlayerRP26,request)
            Text(if(request.item.type=="series") "S${request.season} E${request.episode} · ${request.stream.name}" else request.stream.name,
                color=Color(0xFFBBBBBB),fontSize=12.sp,lineHeight=15.sp,maxLines=1)
            Text(focusedLabel,color=Color.White,fontSize=12.sp,lineHeight=16.sp,maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis,modifier=Modifier.fillMaxWidth().height(18.dp))
            if(!request.live || request.sportsRecordingRP72) PlayerSeekBar(position,duration,seekable,seekFocus,playFocus,onInteraction,onSeek)
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                if(request.sportsRecordingRP72) PlayerIconButton("rewind",tr("Retroceder 10 segundos","Back 10 seconds"),onInteraction=onInteraction,onClick={onSeek((position-10_000L).coerceAtLeast(0L))},onLabel={focusedLabel=it},enabled=seekable)
                if(!request.live || request.sportsRecordingRP72) PlayerIconButton(if(paused) "play" else "pause",if(paused) tr("Reproducir","Play") else tr("Pausar","Pause"),Modifier.focusRequester(playFocus).focusProperties { up=seekFocus },onInteraction,onPlay,onLabel={ focusedLabel=it })
                if(request.sportsRecordingRP72) PlayerIconButton("forward",tr("Adelantar 10 segundos","Forward 10 seconds"),onInteraction=onInteraction,onClick={onSeek((position+10_000L).coerceAtMost(duration))},onLabel={focusedLabel=it},enabled=seekable)
                if(request.item.type=="series" && !request.live) {
                    PlayerIconButton("next",tr("Siguiente episodio","Next episode"),onInteraction=onInteraction,onClick=onNext,onLabel={ focusedLabel=it },enabled=nextAvailable)
                    PlayerIconButton("episodes",tr("Episodios","Episodes"),onInteraction=onInteraction,onClick=onEpisodes,onLabel={ focusedLabel=it })
                }
                if(request.live && !request.sportsRecordingRP72 && previousChannelVisible) PlayerIconButton("previous",
                    tr("Canal anterior","Previous channel")+previousChannelName.takeIf {it.isNotBlank()}?.let {" · $it"}.orEmpty(),
                    onInteraction=onInteraction,onClick=onPreviousChannel,onLabel={focusedLabel=it},enabled=previousChannelAvailable)
                if(!request.live || request.sportsRecordingRP72) PlayerIconButton("cc",tr("Subtítulos","Subtitles"),onInteraction=onInteraction,onClick={onMenu("subtitles")},onLabel={ focusedLabel=it })
                if(!request.live || request.sportsRecordingRP72) PlayerIconButton("audio",tr("Pista de audio","Audio track"),onInteraction=onInteraction,onClick={onMenu("audio")},onLabel={ focusedLabel=it })
                if(!request.live || (request.liveProvider=="sports" || request.liveProvider==com.miflix.native2.data.NuvioTestConfigRP49.PROVIDER)) PlayerIconButton("links",if(request.liveProvider=="sports") tr("Cambiar fuente","Change source") else tr("Enlaces de reproducción","Playback links"),onInteraction=onInteraction,onClick=onLinks,onLabel={ focusedLabel=it })
                if(request.live && !request.sportsRecordingRP72) PlayerIconButton("queue",tr("Juegos · Todos los deportes","Games · All sports"),onInteraction=onInteraction,onClick={onMenu("games")},onLabel={focusedLabel=it})
                PlayerIconButton("party","Friends & Party",onInteraction=onInteraction,onClick=onParty,onLabel={ focusedLabel=it })
                PlayerIconButton("chat",tr("Chat del Party","Party chat"),onInteraction=onInteraction,onClick=onChat,onLabel={ focusedLabel=it })
                PlayerIconButton("reactions",tr("Emojis · Mantén pulsado para frases","Emojis · Hold for phrases"),onInteraction=onInteraction,onClick=onQuickReactions,onLongClick=onReactions,onLabel={ focusedLabel=it })
                if(!request.live) PlayerIconButton("queue",tr("Cola","Queue"),onInteraction=onInteraction,onClick=onQueue,onLabel={ focusedLabel=it })
                Spacer(Modifier.weight(1f))
                Text(if(duration>0) "${tr("Finaliza","Ends")} " + com.miflix.native2.player.PlaybackClockRP63.format(System.currentTimeMillis()+((duration-position).coerceAtLeast(0)/speed.coerceAtLeast(0.1f)).toLong()) else if(request.liveProvider=="lidom") tr("TRANSMISIÓN","STREAM") else if(request.sportsRecordingRP72) tr("GRABACIÓN","RECORDING") else if(request.live) tr("EN VIVO","LIVE") else playbackTime(position),
                    color=Color.White,fontSize=14.sp,lineHeight=18.sp)
            }
        }
    }
}

@Composable
private fun PlayerIconButton(icon: String, label: String, modifier: Modifier=Modifier, onInteraction: () -> Unit, onClick: () -> Unit, onLabel: (String) -> Unit = {}, enabled: Boolean=true, onLongClick: (() -> Unit)?=null) {
    var focused by remember { mutableStateOf(false) }
    Box(modifier.size(44.dp).onFocusChanged { focused=it.isFocused; if(it.isFocused) { onInteraction(); onLabel(label) } }
        .then(if(onLongClick!=null && enabled) Modifier.tvPlaybackClick({ onInteraction();onClick() },{ onInteraction();onLongClick() }) else Modifier)
        .background(GlassActionBrushRP108(focused),CircleShape)
        .semantics { contentDescription=label }
        .clickable(enabled=enabled) { onInteraction(); onClick() },contentAlignment=Alignment.Center) {
        PlayerGlyph(icon,if(!enabled) Color(0xFF666666) else if(focused) Color.White else Color.White,Modifier.size(23.dp))

    }
}

@Composable
private fun PlayerGlyph(icon: String, color: Color, modifier: Modifier) {
    Canvas(modifier) {
        val w=size.width; val h=size.height; val stroke=Stroke(2.dp.toPx())
        fun line(x1:Float,y1:Float,x2:Float,y2:Float)=drawLine(color,Offset(w*x1,h*y1),Offset(w*x2,h*y2),2.dp.toPx())
        fun rectangle(x:Float,y:Float,a:Float,b:Float)=drawRect(color,Offset(w*x,h*y),Size(w*a,h*b),style=stroke)
        when(icon) {
            "stats" -> {rectangle(.14f,.55f,.16f,.30f);rectangle(.42f,.33f,.16f,.52f);rectangle(.70f,.12f,.16f,.73f)}
            "play" -> drawPath(Path().apply { moveTo(w*.28f,h*.15f); lineTo(w*.85f,h*.5f); lineTo(w*.28f,h*.85f); close() },color)
            "pause" -> { drawRect(color,Offset(w*.22f,h*.17f),Size(w*.18f,h*.66f)); drawRect(color,Offset(w*.60f,h*.17f),Size(w*.18f,h*.66f)) }
            "previous" -> { drawPath(Path().apply {moveTo(w*.84f,h*.15f);lineTo(w*.30f,h*.5f);lineTo(w*.84f,h*.85f);close()},color);drawRect(color,Offset(w*.14f,h*.15f),Size(w*.10f,h*.70f)) }
            "rewind" -> {drawPath(Path().apply {moveTo(w*.48f,h*.2f);lineTo(w*.12f,h*.5f);lineTo(w*.48f,h*.8f);close()},color);drawPath(Path().apply {moveTo(w*.88f,h*.2f);lineTo(w*.52f,h*.5f);lineTo(w*.88f,h*.8f);close()},color)}
            "forward" -> {drawPath(Path().apply {moveTo(w*.12f,h*.2f);lineTo(w*.48f,h*.5f);lineTo(w*.12f,h*.8f);close()},color);drawPath(Path().apply {moveTo(w*.52f,h*.2f);lineTo(w*.88f,h*.5f);lineTo(w*.52f,h*.8f);close()},color)}
            "next" -> { drawPath(Path().apply { moveTo(w*.16f,h*.15f);lineTo(w*.70f,h*.5f);lineTo(w*.16f,h*.85f);close() },color);drawRect(color,Offset(w*.76f,h*.15f),Size(w*.10f,h*.70f)) }
            "queue" -> { line(.12f,.2f,.88f,.2f);line(.12f,.5f,.70f,.5f);line(.12f,.8f,.55f,.8f);line(.78f,.64f,.78f,.94f);line(.64f,.79f,.92f,.79f) }
            "episodes" -> { rectangle(.08f,.12f,.24f,.20f);rectangle(.08f,.42f,.24f,.20f);rectangle(.08f,.72f,.24f,.20f);line(.46f,.22f,.92f,.22f);line(.46f,.52f,.92f,.52f);line(.46f,.82f,.92f,.82f) }
            "moment" -> { rectangle(.12f,.22f,.76f,.56f);line(.3f,.12f,.3f,.88f);line(.7f,.12f,.7f,.88f) }
            "info" -> { drawCircle(color,w*.4f,Offset(w*.5f,h*.5f),style=stroke);line(.5f,.45f,.5f,.75f);drawCircle(color,w*.04f,Offset(w*.5f,h*.27f)) }
            "pip" -> { rectangle(.05f,.15f,.9f,.7f);drawRect(color,Offset(w*.52f,h*.48f),Size(w*.33f,h*.26f)) }
            "score" -> {rectangle(.05f,.18f,.9f,.64f);line(.5f,.18f,.5f,.82f);line(.17f,.38f,.35f,.38f);line(.17f,.58f,.35f,.58f);line(.65f,.38f,.83f,.38f);line(.65f,.58f,.83f,.58f)}
            "grid" -> { rectangle(.08f,.08f,.35f,.35f);rectangle(.57f,.08f,.35f,.35f);rectangle(.08f,.57f,.35f,.35f);rectangle(.57f,.57f,.35f,.35f) }
            "close" -> { line(.2f,.2f,.8f,.8f); line(.8f,.2f,.2f,.8f) }
            "maximize" -> { line(.1f,.35f,.1f,.1f);line(.1f,.1f,.35f,.1f);line(.65f,.1f,.9f,.1f);line(.9f,.1f,.9f,.35f);line(.9f,.65f,.9f,.9f);line(.9f,.9f,.65f,.9f);line(.35f,.9f,.1f,.9f);line(.1f,.9f,.1f,.65f) }
            "resize" -> { rectangle(.1f,.2f,.8f,.6f);line(.22f,.65f,.42f,.45f);line(.22f,.65f,.22f,.46f);line(.22f,.65f,.41f,.65f);line(.78f,.35f,.58f,.55f);line(.78f,.35f,.78f,.54f);line(.78f,.35f,.59f,.35f) }
            "speed" -> { drawArc(color,180f,180f,false,Offset(w*.1f,h*.2f),Size(w*.8f,h*.65f),style=stroke);line(.15f,.8f,.85f,.8f);line(.5f,.7f,.76f,.3f); drawCircle(color,w*.06f,Offset(w*.5f,h*.7f)) }
            "cc" -> { rectangle(.04f,.2f,.92f,.6f);drawArc(color,60f,240f,false,Offset(w*.17f,h*.34f),Size(w*.26f,h*.32f),style=stroke);drawArc(color,60f,240f,false,Offset(w*.56f,h*.34f),Size(w*.26f,h*.32f),style=stroke) }
            "audio" -> { rectangle(.24f,.08f,.52f,.84f);drawCircle(color,w*.08f,Offset(w*.5f,h*.3f),style=stroke);drawCircle(color,w*.16f,Offset(w*.5f,h*.66f),style=stroke) }
            "links" -> { drawRoundRect(color,Offset(w*.04f,h*.38f),Size(w*.55f,h*.3f),cornerRadius=androidx.compose.ui.geometry.CornerRadius(w*.14f),style=stroke);drawRoundRect(color,Offset(w*.41f,h*.26f),Size(w*.55f,h*.3f),cornerRadius=androidx.compose.ui.geometry.CornerRadius(w*.14f),style=stroke);line(.35f,.55f,.65f,.4f) }
            "chat" -> { rectangle(.08f,.12f,.84f,.62f);line(.22f,.74f,.22f,.94f);line(.22f,.94f,.44f,.74f);line(.24f,.34f,.76f,.34f);line(.24f,.52f,.60f,.52f) }
            "reactions" -> { drawCircle(color,w*.42f,Offset(w*.5f,h*.5f),style=stroke);drawCircle(color,w*.035f,Offset(w*.35f,h*.38f));drawCircle(color,w*.035f,Offset(w*.65f,h*.38f));drawArc(color,0f,180f,false,Offset(w*.27f,h*.42f),Size(w*.46f,h*.30f),style=stroke) }
            "party" -> { drawCircle(color,w*.14f,Offset(w*.34f,h*.3f),style=stroke);drawCircle(color,w*.11f,Offset(w*.75f,h*.35f),style=stroke);drawArc(color,180f,180f,false,Offset(w*.08f,h*.52f),Size(w*.52f,h*.38f),style=stroke);drawArc(color,180f,180f,false,Offset(w*.60f,h*.6f),Size(w*.30f,h*.25f),style=stroke) }

        }
    }
}

/** Left/right seeks ten seconds while this bar has focus; up/down uses normal TV navigation. */
@Composable
private fun PlayerSeekBar(position: Long, duration: Long, enabled: Boolean, seekFocus: FocusRequester, playFocus: FocusRequester, onInteraction: () -> Unit, onSeek: (Long) -> Unit,largeRP89:Boolean=false) {
    var focused by remember {mutableStateOf(false)}
    var dragging by remember {mutableStateOf(false)}
    var preview by remember {mutableStateOf<Long?>(null)}
    val latestSeek by rememberUpdatedState(onSeek)
    val interaction by rememberUpdatedState(onInteraction)
    val shown=(preview ?: position).coerceIn(0L,duration.coerceAtLeast(0L))
    val fraction=if(duration>0) (shown.toFloat()/duration).coerceIn(0f,1f) else 0f
    LaunchedEffect(position,dragging) {if(!dragging && preview?.let {kotlin.math.abs(position-it)<1000L}==true) preview=null}
    LaunchedEffect(preview,dragging) {if(preview!=null && !dragging) {kotlinx.coroutines.delay(1500);preview=null} }
    Box(Modifier.fillMaxWidth().height(if(largeRP89) 52.dp else 30.dp).focusRequester(seekFocus).focusProperties {down=playFocus}
        .onFocusChanged {focused=it.isFocused;if(it.isFocused) interaction()}
        .semantics {contentDescription=playbackTime(shown)+" / "+playbackTime(duration)}
        .onPreviewKeyEvent {event ->
            val code=event.nativeKeyEvent.keyCode
            if(code==android.view.KeyEvent.KEYCODE_DPAD_LEFT || code==android.view.KeyEvent.KEYCODE_DPAD_RIGHT) {
                if(event.type==KeyEventType.KeyDown && enabled) {
                    interaction();val target=com.miflix.native2.data.EasyPlaybackRP89.seekTarget(preview ?: position,duration,if(code==android.view.KeyEvent.KEYCODE_DPAD_LEFT) -1 else 1)
                    preview=target;latestSeek(target)
                };true
            } else false
        }.focusable()
        .pointerInput(duration,enabled) {detectTapGestures {if(enabled && size.width>0) {interaction();val target=(it.x/size.width*duration).toLong().coerceIn(0L,duration);preview=target;latestSeek(target)} } }
        .pointerInput(duration,enabled) {
            detectDragGestures(
                onDragStart={dragging=true;interaction()},
                onDragCancel={dragging=false;preview=null},
                onDragEnd={if(enabled) preview?.let {latestSeek(it)};dragging=false},
                onDrag={change,_ -> if(enabled && size.width>0) {change.consume();interaction();preview=(change.position.x/size.width*duration).toLong().coerceIn(0L,duration)}}
            )
        }) {
        Text(playbackTime(shown)+" / "+playbackTime(duration),color=Color.White,fontSize=if(largeRP89) 19.sp else 12.sp,lineHeight=if(largeRP89) 24.sp else 16.sp,
            modifier=Modifier.align(Alignment.TopEnd).background(Color(0xA0000000),RoundedCornerShape(5.dp)).padding(horizontal=7.dp,vertical=2.dp))
        Canvas(Modifier.align(Alignment.BottomCenter).padding(bottom=2.dp).fillMaxWidth().height(if(largeRP89) {if(focused || dragging) 12.dp else 8.dp} else if(focused || dragging) 8.dp else 5.dp)) {
            drawRoundRect(Color(0xFF555555),cornerRadius=androidx.compose.ui.geometry.CornerRadius(size.height))
            if(fraction>0) drawRoundRect(Color.White,size=Size(size.width*fraction,size.height),cornerRadius=androidx.compose.ui.geometry.CornerRadius(size.height))
            if((focused || dragging) && enabled) drawCircle(Color.White,7.dp.toPx(),Offset(size.width*fraction,size.height/2))
        }
    }
}

@Composable
internal fun PlayerOptionsDialog(menu: String, player: Player, tracks: Tracks, speed: Float,
    onSpeed: (Float) -> Unit, externalSubtitles: List<SubtitleChoice>, subtitleLoading: Boolean, onExternalSubtitle: (SubtitleChoice) -> Unit, series: Boolean, party: Boolean, onEpisodes: () -> Unit, onParty: () -> Unit, onTiming: () -> Unit, spanishOnlyRP83:Boolean=false, onClose: () -> Unit) {
    if(LocalMobileBrunioRP133.current) {
        MobileTracksRP133(menu,player,tracks,speed,onSpeed,externalSubtitles,subtitleLoading,onExternalSubtitle,spanishOnlyRP83,series,party,onEpisodes,onParty,onTiming,onClose)
        return
    }
    var audioStatus by remember(player,menu) { mutableStateOf("") }
    var pendingAudio by remember(player,menu) { mutableStateOf<TrackSelectionOverride?>(null) }
    LaunchedEffect(player,pendingAudio) {
        val target=pendingAudio ?: return@LaunchedEffect
        var applied=false
        for(attempt in 0 until 80) {
            val current=player.currentTracks.groups.firstOrNull { it.mediaTrackGroup==target.mediaTrackGroup }
            if(current!=null && target.trackIndices.all { it in 0 until current.length && current.isTrackSelected(it) }) { applied=true;break }
            kotlinx.coroutines.delay(100)
        }
        audioStatus=if(applied) tr("Pista de audio seleccionada ✓","Audio track selected ✓") else tr("El reproductor no confirmó el cambio. Prueba otra pista o enlace.","The player did not confirm the change. Try another track or source.")
        pendingAudio=null
    }
    val first=remember(menu) { FocusRequester() }
    LaunchedEffect(menu) { withFrameNanos { }; runCatching {first.requestFocus()} }
    val title=when(menu) { "content" -> tr("Contenido / Party","Content / Party"); "speed" -> tr("Velocidad","Speed"); "audio" -> tr("Pista de audio","Audio track"); else -> tr("Subtítulos","Subtitles") }
    PlayerMenuDialog(onDismissRequest=onClose,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Column(Modifier.width(550.dp).heightIn(max=460.dp).background(Color(0xF0181818),RoundedCornerShape(18.dp)).padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            Text(title,color=Color.White,fontSize=24.sp,lineHeight=29.sp)
            if(menu=="audio" && audioStatus.isNotBlank()) Text(audioStatus,color=Color.White,fontSize=14.sp)
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
                            val auto=player.trackSelectionParameters.buildUpon().clearOverridesOfType(type).setTrackTypeDisabled(type,type==C.TRACK_TYPE_TEXT)
                            if(type==C.TRACK_TYPE_AUDIO && spanishOnlyRP83) auto.setPreferredAudioLanguages("es-419","es-MX","es-ES","es")
                            player.trackSelectionParameters=auto.build();onClose()
                        } }
                        if(type==C.TRACK_TYPE_TEXT) item { FocusButton(tr("Sincronizar subtítulos","Subtitle timing")) { onTiming() } }
                        var count=0
                        tracks.groups.filter { it.type==type }.forEach { group ->
                            for(index in 0 until group.length) {
                                val format=group.getTrackFormat(index)
                                if(type==C.TRACK_TYPE_AUDIO && spanishOnlyRP83 && com.miflix.native2.data.SpanishAudioRP83.rank(format.language,format.label)<0) continue
                                count++
                                val supported=group.isTrackSupported(index,true)
                                val limited=supported && !group.isTrackSupported(index)
                                val label=listOfNotNull(format.label?.takeIf { it.isNotBlank() },format.language?.takeIf { it.isNotBlank() },format.codecs?.takeIf { it.isNotBlank() },if(format.channelCount>0) "${format.channelCount} ch" else null).distinct().joinToString(" · ").ifBlank { "${tr("Pista","Track")} $count" }
                                item { FocusButton("${if(group.isTrackSelected(index)) "✓ " else ""}$count · $label${if(limited) tr(" · Compatibilidad limitada"," · Limited compatibility") else ""}${if(!supported) tr(" · No compatible con esta TV"," · Unsupported on this TV") else ""}",modifier=Modifier.fillMaxWidth()) {
                                    val current=player.currentTracks.groups.firstOrNull { it.mediaTrackGroup==group.mediaTrackGroup }
                                    if(current==null || index !in 0 until current.length) {
                                        audioStatus=tr("La lista cambió. Cierra y vuelve a abrir este menú.","The track list changed. Close and reopen this menu.")
                                    } else if(!current.isTrackSupported(index,true)) {
                                        audioStatus=tr("La TV no puede decodificar esta pista. Elige otra pista o un enlace con otro formato de audio.","This TV cannot decode this track. Choose another track or a source with a different audio format.")
                                    } else {
                                        val override=TrackSelectionOverride(current.mediaTrackGroup,index)
                                        val params=player.trackSelectionParameters.buildUpon().clearOverridesOfType(type).setTrackTypeDisabled(type,false)
                                        if(type==C.TRACK_TYPE_AUDIO) params.setPreferredAudioLanguage(null)
                                        player.trackSelectionParameters=params.setOverrideForType(override).build()
                                        if(type==C.TRACK_TYPE_AUDIO) {
                                            audioStatus=tr("Cambiando pista de audio…","Switching audio track…")
                                            pendingAudio=override
                                        } else onClose()
                                    }
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
