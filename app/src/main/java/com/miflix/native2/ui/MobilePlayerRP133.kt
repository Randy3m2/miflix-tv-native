package com.miflix.native2.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.media3.common.*
import androidx.lifecycle.lifecycleScope
import androidx.media3.exoplayer.ExoPlayer
import androidx.tv.material3.Text
import com.miflix.native2.model.*
import com.miflix.native2.data.SpanishAudioRP83
import kotlinx.coroutines.*
import java.util.Locale

private fun mobileTimeRP133(value:Long):String {
    val seconds=value.coerceAtLeast(0)/1000
    return if(seconds>=3600) String.format(Locale.US,"%d:%02d:%02d",seconds/3600,seconds/60%60,seconds%60)
        else String.format(Locale.US,"%d:%02d",seconds/60,seconds%60)
}

@Composable
internal fun MobilePlayerControlsRP133(request:PlayerRequest,paused:Boolean,position:Long,duration:Long,
    seekable:Boolean,nextAvailable:Boolean,easy:Boolean,player:ExoPlayer?,onInteraction:()->Unit,onPlay:()->Unit,
    onClose:()->Unit,onSeek:(Long)->Unit,onMenu:(String)->Unit,onLinks:()->Unit,onEpisodes:()->Unit,
    onNext:()->Unit,onParty:()->Unit,onChat:()->Unit,onReactions:()->Unit,onMultiView:()->Unit,
    onStats:()->Unit,onInfo:()->Unit,onPip:()->Unit,onResize:()->Unit) {
    val context=LocalContext.current
    var more by remember {mutableStateOf(false)}
    var muted by remember(player) {mutableStateOf(player?.volume==0f)}
    LaunchedEffect(more) {while(more) {onInteraction();delay(1000)}}
    fun action(block:()->Unit) {onInteraction();block()}
    if(more) MobileSheetRP133(tr("Opciones del reproductor","Player options"),{more=false}) {
        LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            item {MobileButtonRP133(tr("Audio","Audio"),"audio",Modifier.fillMaxWidth()) {more=false;action {onMenu("audio")}}}
            item {MobileButtonRP133(tr("Subtítulos","Subtitles"),"cc",Modifier.fillMaxWidth()) {more=false;action {onMenu("subtitles")}}}
            item {MobileButtonRP133(if(request.live) tr("Canales / juegos","Channels / games") else tr("Episodios","Episodes"),"grid",Modifier.fillMaxWidth()) {more=false;action(onEpisodes)}}
            if(nextAvailable) item {MobileButtonRP133(tr("Siguiente episodio","Next episode"),"next",Modifier.fillMaxWidth()) {more=false;action(onNext)}}
            if(!easy) {
                item {MobileButtonRP133(tr("Cambiar fuente","Change source"),"link",Modifier.fillMaxWidth()) {more=false;action(onLinks)}}
                if(request.live) {
                    item {MobileButtonRP133("SplitScreen","grid",Modifier.fillMaxWidth()) {more=false;action(onMultiView)}}
                    item {MobileButtonRP133("Stats","stats",Modifier.fillMaxWidth()) {more=false;action(onStats)}}
                }
                if(!request.live && MobileDownloadRP133.direct(request.stream)) item {MobileButtonRP133(tr("Descargar","Download"),"download",Modifier.fillMaxWidth()) {more=false;action {
                    context.playbackActivity()?.lifecycleScope?.launch(Dispatchers.IO) {runCatching {MobileDownloadsRP134.enqueue(context,request.item,request.stream,request.season,request.episode)}}
                }}}
                item {MobileButtonRP133(tr("Transmitir pantalla","Cast screen"),"cast",Modifier.fillMaxWidth()) {more=false;action {runCatching {context.startActivity(android.content.Intent(android.provider.Settings.ACTION_CAST_SETTINGS))}}}}
                item {MobileButtonRP133("Watch Party","friends",Modifier.fillMaxWidth()) {more=false;action(onParty)}}
                item {MobileButtonRP133("Chat","friends",Modifier.fillMaxWidth()) {more=false;action(onChat)}}
                item {MobileButtonRP133(tr("Reacciones","Reactions"),"heart",Modifier.fillMaxWidth()) {more=false;action(onReactions)}}
                item {MobileButtonRP133("PiP","full",Modifier.fillMaxWidth()) {more=false;action(onPip)}}
                item {MobileButtonRP133("Info","more",Modifier.fillMaxWidth()) {more=false;action(onInfo)}}
            }
        }
    }
    Box(Modifier.fillMaxSize()) {
        Row(Modifier.align(Alignment.TopCenter).fillMaxWidth()
            .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color.Black.copy(alpha=.65f),Color.Transparent)))
            .padding(horizontal=12.dp,vertical=6.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            MobileIconButtonRP133("back",tr("Volver","Back")) {action(onClose)}
            Text(request.item.title,color=Color.White,fontSize=16.sp,fontWeight=FontWeight.SemiBold,maxLines=1,
                overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis,modifier=Modifier.weight(1f))
            MobileIconButtonRP133(if(muted) "mute" else "volume",tr("Silenciar / activar sonido","Mute / unmute")) {action {muted=!muted;player?.volume=if(muted) 0f else 1f}}
            MobileIconButtonRP133("more",tr("Más opciones","More options")) {action {more=true}}
        }
        Row(Modifier.align(Alignment.Center),horizontalArrangement=Arrangement.spacedBy(32.dp),verticalAlignment=Alignment.CenterVertically) {
            if(seekable) MobileSeekButtonRP134("−10",tr("Retroceder 10 segundos","Back 10 seconds")) {action {onSeek((position-10_000).coerceAtLeast(0))}}
            MobileIconButtonRP133(if(paused) "play" else "pause",tr("Reproducir / pausar","Play / pause"),Modifier.size(56.dp)) {action(onPlay)}
            if(seekable) MobileSeekButtonRP134("+10",tr("Adelantar 10 segundos","Forward 10 seconds")) {action {onSeek((position+10_000).coerceAtMost(duration))}}
        }
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth()
            .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color.Transparent,Color.Black.copy(alpha=.75f))))
            .padding(horizontal=16.dp,vertical=6.dp)) {
            if(seekable) MobileSeekRP133(position,duration,onInteraction,onSeek)
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Text(if(seekable) mobileTimeRP133(position)+" / "+mobileTimeRP133(duration) else tr("● EN VIVO","● LIVE"),color=Color.White,fontSize=12.sp,modifier=Modifier.weight(1f))
                if(!easy) MobileIconButtonRP133("link",tr("Cambiar fuente","Change source")) {action(onLinks)}
                MobileIconButtonRP133("audio",tr("Audio y subtítulos","Audio & subtitles")) {action {onMenu("audio")}}
                MobileIconButtonRP133("full",tr("Ajustar / llenar pantalla","Fit / fill screen")) {action(onResize)}
            }
        }
    }
}

@Composable
private fun MobileSeekButtonRP134(text:String,label:String,click:()->Unit) {
    Box(Modifier.size(48.dp).background(Color.Black.copy(alpha=.35f),androidx.compose.foundation.shape.CircleShape)
        .semantics {contentDescription=label;role=androidx.compose.ui.semantics.Role.Button}.clickable(onClick=click),contentAlignment=Alignment.Center) {Text(text,color=Color.White,fontSize=16.sp,fontWeight=FontWeight.SemiBold)}
}

@Composable
private fun MobileSeekRP133(position:Long,duration:Long,interact:()->Unit,seek:(Long)->Unit) {
    var width by remember {mutableIntStateOf(1)}
    var drag by remember {mutableStateOf<Long?>(null)}
    val latestSeek by rememberUpdatedState(seek)
    val latestInteract by rememberUpdatedState(interact)
    fun at(x:Float)=((x/width.coerceAtLeast(1)).coerceIn(0f,1f)*duration).toLong()
    Canvas(Modifier.fillMaxWidth().height(38.dp).onSizeChanged {width=it.width}
        .pointerInput(duration) {detectTapGestures {latestInteract();latestSeek(at(it.x))}}
        .pointerInput(duration) {detectDragGestures(onDragStart={latestInteract();drag=at(it.x)},onDragCancel={drag=null},
            onDragEnd={drag?.let(latestSeek);drag=null}) {change,_->change.consume();drag=at(change.position.x)}}) {
        val value=if(duration>0) ((drag ?: position).toFloat()/duration).coerceIn(0f,1f) else 0f
        drawLine(Color(0xFF464955),Offset(0f,size.height/2),Offset(size.width,size.height/2),4.dp.toPx())
        drawLine(Color.White,Offset(0f,size.height/2),Offset(size.width*value,size.height/2),4.dp.toPx())
        drawCircle(Color.White,6.dp.toPx(),Offset(size.width*value,size.height/2))
    }
}

@Composable
internal fun MobileTracksRP133(menu:String,player:Player,tracks:Tracks,speed:Float,onSpeed:(Float)->Unit,
    external:List<SubtitleChoice>,loading:Boolean,onExternal:(SubtitleChoice)->Unit,spanishOnly:Boolean,
    series:Boolean,party:Boolean,onEpisodes:()->Unit,onParty:()->Unit,onTiming:()->Unit,close:()->Unit) {
    var tab by remember {mutableStateOf(if(menu=="subtitles") "subtitles" else if(menu=="audio") "audio" else "content")}
    MobileSheetRP133(tr("Audio y subtítulos","Audio & subtitles"),close) {
        LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            items(listOf("audio" to tr("Audio","Audio"),"subtitles" to tr("Subtítulos","Subtitles"),"content" to tr("Opciones","Options"))) {(id,label)->MobileButtonRP133(label,selected=tab==id) {tab=id}}
        }
        LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            if(tab=="content") {
                item {Text(tr("Velocidad","Playback speed"),color=Color.White,fontSize=19.sp,fontWeight=FontWeight.Bold)}
                items(listOf(.75f,1f,1.25f,1.5f,2f)) {value->MobileButtonRP133(value.toString()+"×",modifier=Modifier.fillMaxWidth(),selected=speed==value) {onSpeed(value)}}
                if(series) item {MobileButtonRP133(tr("Episodios","Episodes"),"grid",Modifier.fillMaxWidth()) {close();onEpisodes()}}
                item {MobileButtonRP133("Watch Party","friends",Modifier.fillMaxWidth()) {close();onParty()}}
                item {MobileButtonRP133(tr("Ajustar subtítulos","Subtitle timing"),"cc",Modifier.fillMaxWidth()) {close();onTiming()}}
            } else {
                val type=if(tab=="audio") C.TRACK_TYPE_AUDIO else C.TRACK_TYPE_TEXT
                if(type==C.TRACK_TYPE_TEXT) item {MobileButtonRP133(tr("Desactivados","Off"),modifier=Modifier.fillMaxWidth()) {player.trackSelectionParameters=player.trackSelectionParameters.buildUpon().setTrackTypeDisabled(type,true).build();close()}}
                tracks.groups.filter {it.type==type}.forEach {group->
                    for(index in 0 until group.length) {
                        val format=group.getTrackFormat(index)
                        if(type==C.TRACK_TYPE_AUDIO && spanishOnly && SpanishAudioRP83.rank(format.language,format.label)<0) continue
                        item {
                            val label=listOfNotNull(format.label,format.language,if(format.channelCount>0) "${format.channelCount} ch" else null).distinct().joinToString(" · ").ifBlank {tr("Pista ","Track ")+(index+1)}
                            MobileButtonRP133(label,modifier=Modifier.fillMaxWidth(),selected=group.isTrackSelected(index),enabled=group.isTrackSupported(index,true)) {
                                val current=player.currentTracks.groups.firstOrNull {it.mediaTrackGroup==group.mediaTrackGroup}
                                if(current!=null && index<current.length) player.trackSelectionParameters=player.trackSelectionParameters.buildUpon()
                                    .clearOverridesOfType(type).setTrackTypeDisabled(type,false).setOverrideForType(TrackSelectionOverride(current.mediaTrackGroup,index)).build()
                            }
                        }
                    }
                }
                if(type==C.TRACK_TYPE_TEXT) {
                    if(loading) item {Text(tr("Buscando subtítulos…","Loading subtitles…"),color=Muted)}
                    items(external,key={it.url}) {sub->MobileButtonRP133(sub.label,modifier=Modifier.fillMaxWidth()) {onExternal(sub);close()}}
                }
            }
        }
    }
}

@Composable
internal fun MobileSourcesRP133(state:AppState,selection:SourceSelection) {
    val context=LocalContext.current
    val owner=state.localExtrasScope()
    MobileSheetRP133(tr("Cambiar fuente","Change source"),{state.sourceSelection=null}) {
        Text(selection.item.title,color=Color.White,fontSize=17.sp,fontWeight=FontWeight.Bold)
        LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            itemsIndexed(selection.streams,key={index,it->index.toString()+it.url}) {_,stream->
                Column(Modifier.fillMaxWidth().mobileGlassRP133().padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Text(stream.name,color=Color.White,fontWeight=FontWeight.Bold)
                    Text(stream.title,color=Muted,fontSize=12.sp,maxLines=4)
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        MobileButtonRP133(tr("Reproducir","Play"),"play",Modifier.weight(1f)) {state.launch {if(owner==state.localExtrasScope()) state.playSelectedSource(selection,stream)}}
                        if(!selection.item.type.equals("tv",true) && MobileDownloadRP133.direct(stream)) MobileIconButtonRP133("download",tr("Descargar archivo","Download file")) {
                            state.launch {withContext(Dispatchers.IO) {MobileDownloadsRP134.enqueue(context,selection.item,stream,selection.season,selection.episode)}}
                        }
                    }
                }
            }
        }
    }
}
