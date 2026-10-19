package com.miflix.native2.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import androidx.media3.exoplayer.ExoPlayer
import com.miflix.native2.data.PlaybackBadgePolicyRP26
import kotlinx.coroutines.delay
import java.util.Locale

/** Small, passive pills: they never take focus away from playback controls. */
@Composable
internal fun PlaybackBadgesRP26(player:ExoPlayer?,request:PlayerRequest) {
    var input by remember(player,request.playbackId) {mutableStateOf(PlaybackBadgePolicyRP26.Input())}
    LaunchedEffect(player,request.playbackId) {
        if(player!=null) while(true) {
            val v=player.videoFormat
            val a=player.audioFormat
            input=PlaybackBadgePolicyRP26.Input(
                // The selected link only: never infer a codec from the movie title or URL.
                reported=listOf(request.stream.name,request.stream.title,request.stream.filename).joinToString(" "),
                videoKnown=v!=null,height=v?.height ?: -1,width=v?.width ?: -1,
                videoMime=v?.sampleMimeType.orEmpty(),videoCodecs=v?.codecs.orEmpty(),
                transfer=v?.colorInfo?.colorTransfer ?: -1,
                audioKnown=a!=null,audioMime=a?.sampleMimeType.orEmpty(),audioCodecs=a?.codecs.orEmpty(),
                channels=a?.channelCount ?: -1,language=a?.language.orEmpty())
            delay(750)
        }
    }
    val badges=remember(input) {PlaybackBadgePolicyRP26.read(input)}
    val locale=Locale.forLanguageTag(tr("es-419","en"))
    val language=badges.language.takeIf {it.isNotBlank()}?.let {
        Locale.forLanguageTag(it).getDisplayLanguage(locale).replaceFirstChar {c->c.titlecase(locale)}
    }.orEmpty()
    val audio=listOf(language,when(badges.channels) {"Stereo"->tr("Estéreo","Stereo");else->badges.channels})
        .filter {it.isNotBlank()}.joinToString(" ")
    if(listOf(badges.resolution,badges.hdr,badges.audio,audio).all {it.isBlank()}) return
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical=3.dp),
        horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically) {
        if(badges.resolution.isNotBlank()) PlaybackPillRP26 {Text(badges.resolution,color=Color.White,fontSize=13.sp)}
        if(badges.hdr.isNotBlank() || badges.audio.isNotBlank()) PlaybackPillRP26 {
            if(badges.hdr.isNotBlank()) {
                if(badges.hdr=="Dolby Vision") DolbyMarkRP26()
                Text(badges.hdr,color=Color.White,fontSize=13.sp)
            }
            if(badges.hdr.isNotBlank() && badges.audio.isNotBlank()) Text("·",color=Color.White,fontSize=13.sp)
            if(badges.audio.isNotBlank()) {
                if(badges.audio in listOf("Atmos","Dolby Digital","Dolby Digital+")) DolbyMarkRP26()
                Text(badges.audio,color=Color.White,fontSize=13.sp)
            }
        }
        if(audio.isNotBlank()) PlaybackPillRP26 {PlayerAudioMarkRP26();Text(audio,color=Color.White,fontSize=13.sp)}
    }
}

@Composable
private fun PlaybackPillRP26(content:@Composable RowScope.()->Unit) {
    Row(Modifier.background(Color(0x99303C46),RoundedCornerShape(50))
        .border(1.dp,Color(0xAA5BA5BC),RoundedCornerShape(50)).padding(horizontal=10.dp,vertical=4.dp),
        verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp),content=content)
}

@Composable
private fun DolbyMarkRP26() {
    Canvas(Modifier.size(19.dp,12.dp)) {
        val w=size.width;val h=size.height
        val left=Path().apply {moveTo(0f,0f);lineTo(w*.14f,0f);cubicTo(w*.59f,0f,w*.59f,h,w*.14f,h);lineTo(0f,h);close()}
        val right=Path().apply {moveTo(w,0f);lineTo(w*.86f,0f);cubicTo(w*.41f,0f,w*.41f,h,w*.86f,h);lineTo(w,h);close()}
        drawPath(left,Color.White);drawPath(right,Color.White)
    }
}

@Composable
private fun PlayerAudioMarkRP26() {
    Canvas(Modifier.size(16.dp)) {
        val w=size.width;val h=size.height
        val speaker=Path().apply {moveTo(w*.08f,h*.35f);lineTo(w*.28f,h*.35f);lineTo(w*.5f,h*.15f);lineTo(w*.5f,h*.85f);lineTo(w*.28f,h*.65f);lineTo(w*.08f,h*.65f);close()}
        drawPath(speaker,Color.White)
        drawArc(Color.White,-55f,110f,false,Offset(w*.3f,h*.25f),Size(w*.48f,h*.5f),style=Stroke(1.2.dp.toPx()))
        drawArc(Color.White,-55f,110f,false,Offset(w*.21f,h*.08f),Size(w*.76f,h*.84f),style=Stroke(1.2.dp.toPx()))
    }
}
