package com.miflix.native2.ui

import androidx.annotation.OptIn
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.ui.PlayerView
import androidx.tv.material3.Text
import com.miflix.native2.data.LidomRepository
import com.miflix.native2.data.rp200.YoutubeChunkedDataSourceFactory
import kotlinx.coroutines.CancellationException

/** RP200: native LIDOM Multiview tile; no embedded YouTube iframe. */
@OptIn(UnstableApi::class)
@Composable
internal fun LidomSplitYoutubeTileRP9(req:PlayerRequest,audible:Boolean,modifier:Modifier,click:()->Unit) {
    val context=LocalContext.current
    val video=LidomRepository.youtubeId(req.stream.url)
    var focused by remember(req.playbackId) {mutableStateOf(false)}
    var message by remember(req.playbackId) {mutableStateOf("")}
    var ready by remember(req.playbackId) {mutableStateOf(false)}
    val player=remember(req.playbackId) {ExoPlayer.Builder(context.applicationContext).build()}
    val lifecycle=context.playbackActivity()?.lifecycle
    val running=lifecycle?.currentState?.isAtLeast(Lifecycle.State.STARTED)!=false
    LaunchedEffect(video) {
        if(video==null) {message=tr("Enlace de YouTube inválido","Invalid YouTube URL");return@LaunchedEffect}
        message="";ready=false
        try {
            val source=NativeTrailerResolverRP200.get(video)
            if(source==null) {message=tr("No se pudo resolver el directo","Live stream unavailable");return@LaunchedEffect}
            val factory=DefaultMediaSourceFactory(YoutubeChunkedDataSourceFactory())
            if(!source.audioUrl.isNullOrBlank()) {
                player.setMediaSource(MergingMediaSource(
                    factory.createMediaSource(MediaItem.fromUri(source.videoUrl)),
                    factory.createMediaSource(MediaItem.fromUri(source.audioUrl))
                ))
            } else player.setMediaSource(factory.createMediaSource(MediaItem.fromUri(source.videoUrl)))
            player.prepare();ready=true
        } catch(e:CancellationException) {throw e}
          catch(_:Exception) {message=tr("No se pudo reproducir el directo","Could not play live stream")}
    }
    LaunchedEffect(audible,running,ready) {
        player.volume=if(audible) 1f else 0f
        player.playWhenReady=running && ready
    }
    DisposableEffect(lifecycle,player) {
        val observer=LifecycleEventObserver {_,event ->
            if(event==Lifecycle.Event.ON_STOP) player.playWhenReady=false
            else if(event==Lifecycle.Event.ON_START && ready) player.playWhenReady=true
        }
        lifecycle?.addObserver(observer)
        onDispose {lifecycle?.removeObserver(observer);player.release()}
    }
    Box(modifier.onFocusChanged {focused=it.hasFocus}.tvClick(click).focusable()
        .border(if(focused) 3.dp else 1.dp,if(focused) Accent else Color(0xFF444444))
        .padding(4.dp).background(Color.Black)) {
        AndroidView(
            factory={c->PlayerView(c).apply {useController=false;this.player=player;resizeMode=androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT}},
            modifier=Modifier.fillMaxSize()
        )
        Box(Modifier.fillMaxSize().clickable(onClick=click))
        Text((if(audible) "♫  " else "")+req.item.title,color=Color.White,fontSize=15.sp,
            modifier=Modifier.align(Alignment.BottomStart).background(Color(0xB0000000)).padding(8.dp))
        if(message.isNotBlank()) Text(message+" · "+tr("OK para elegir otra fuente","OK to choose another source"),
            color=Color.White,modifier=Modifier.align(Alignment.Center).background(Color.Black).padding(12.dp))
    }
}
