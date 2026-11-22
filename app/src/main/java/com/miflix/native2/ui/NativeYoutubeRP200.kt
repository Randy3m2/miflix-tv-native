package com.miflix.native2.ui

import android.view.KeyEvent
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.ui.PlayerView
import androidx.tv.material3.Text
import com.miflix.native2.data.rp200.InAppYouTubeExtractor
import com.miflix.native2.data.rp200.TrailerPlaybackSource
import com.miflix.native2.data.rp200.YoutubeChunkedDataSourceFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

/** RP200: use one native video surface and BruniO remote controls. Never create an iframe. */
internal object NativeTrailerResolverRP200 {
    val extractor = InAppYouTubeExtractor()
    private val cache=mutableMapOf<String,TrailerPlaybackSource>()
    suspend fun get(id:String,live:Boolean=false):TrailerPlaybackSource? {
        if(live) return extractor.extractLivePlaybackSource("https://www.youtube.com/watch?v=$id")
        synchronized(cache) {cache[id]}?.let {return it}
        val result=extractor.extractPlaybackSource("https://www.youtube.com/watch?v=$id",maxVideoHeight=1080)
        if(result!=null) synchronized(cache) {if(cache.size>12) cache.clear();cache[id]=result}
        return result
    }
}

@OptIn(UnstableApi::class)
internal fun ExoPlayer.setNativeYoutubeSource(source:TrailerPlaybackSource) {
    val dataSource=if(source.isHls) androidx.media3.datasource.DefaultHttpDataSource.Factory()
        .setConnectTimeoutMs(8000).setReadTimeoutMs(8000)
    else YoutubeChunkedDataSourceFactory()
    val factory=DefaultMediaSourceFactory(dataSource)
    val item=MediaItem.Builder().setUri(source.videoUrl)
    if(source.isHls) item.setMimeType(androidx.media3.common.MimeTypes.APPLICATION_M3U8)
    val video=factory.createMediaSource(item.build())
    val audio=source.audioUrl
    if(audio.isNullOrBlank()) setMediaSource(video)
    else setMediaSource(MergingMediaSource(video,factory.createMediaSource(MediaItem.fromUri(audio))))
    prepare()
}

internal suspend fun awaitYoutubeLiveStall(player:Player) {
    val pollMs=500L
    val stallMs=8_000L
    var position=player.currentPosition
    var stalledSince=android.os.SystemClock.elapsedRealtime()
    while(true) {
        delay(pollMs)
        val now=android.os.SystemClock.elapsedRealtime()
        val next=player.currentPosition
        if(!player.playWhenReady || next!=position) stalledSince=now
        if(player.playWhenReady && (player.playerError!=null ||
                player.playbackState==Player.STATE_ENDED || now-stalledSince>=stallMs)) return
        position=next
    }
}

/** One lazily retained decoder/player, analogous to Nuvio's shared trailer pool. */
private object NativeTrailerPlayerPoolRP200 {
    private var pooled:ExoPlayer?=null
    @Synchronized fun acquire(context:android.content.Context):ExoPlayer {
        return pooled ?: ExoPlayer.Builder(context.applicationContext).build().also {pooled=it}
    }
    @Synchronized fun park(player:ExoPlayer) {
        if(pooled===player) {
            player.playWhenReady=false
            player.stop()
            player.clearMediaItems()
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
internal fun NativeYoutubeRP200(
    video:String, caption:String, close:()->Unit, onWorking:()->Unit={},
    preview:Boolean=true, embedded:Boolean=false,
    previous:(()->Unit)?=null,next:(()->Unit)?=null,onEnded:(()->Unit)?=null,
    details:(()->Unit)?=null,watch:(()->Unit)?=null,swipe:Modifier=Modifier
) {
    val context=LocalContext.current
    val focus=remember(video) {FocusRequester()}
    val player=remember(video) {NativeTrailerPlayerPoolRP200.acquire(context)}
    var source by remember(video) {mutableStateOf<TrailerPlaybackSource?>(null)}
    var loading by remember(video) {mutableStateOf(true)}
    var error by remember(video) {mutableStateOf(false)}
    var firstFrame by remember(video) {mutableStateOf(false)}
    var controls by remember(video) {mutableStateOf(true)}
    var paused by remember(video) {mutableStateOf(false)}
    var retry by remember(video) {mutableIntStateOf(0)}
    var liveRecoveries by remember(video) {mutableIntStateOf(0)}
    var position by remember(video) {mutableLongStateOf(0L)}
    var duration by remember(video) {mutableLongStateOf(0L)}
    val lifecycle=context.playbackActivity()?.lifecycle
    var foreground by remember(lifecycle) {
        mutableStateOf(lifecycle?.currentState?.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)!=false)
    }
    DisposableEffect(lifecycle) {
        val observer=androidx.lifecycle.LifecycleEventObserver {_,event ->
            when(event) {
                androidx.lifecycle.Lifecycle.Event.ON_START -> foreground=true
                androidx.lifecycle.Lifecycle.Event.ON_STOP -> foreground=false
                else -> Unit
            }
        }
        lifecycle?.addObserver(observer)
        onDispose { lifecycle?.removeObserver(observer) }
    }
    val workCallback by rememberUpdatedState(onWorking)
    val endCallback by rememberUpdatedState(onEnded)
    DisposableEffect(player) {
        val listener=object:Player.Listener {
            override fun onRenderedFirstFrame() {firstFrame=true;workCallback()}
            override fun onPlaybackStateChanged(state:Int) {
                if(state==Player.STATE_ENDED) endCallback?.invoke()
            }
            override fun onPlayerError(cause:androidx.media3.common.PlaybackException) {if(preview) error=true}
        }
        player.addListener(listener)
        onDispose {player.removeListener(listener);NativeTrailerPlayerPoolRP200.park(player)}
    }
    LaunchedEffect(video,retry) {
        loading=true;error=false;firstFrame=false;source=null
        try {source=NativeTrailerResolverRP200.get(video,live=!preview);if(source==null) error=true}
        catch(e:CancellationException) {throw e}
        catch(_:Exception) {error=true}
        finally {loading=false}
    }
    LaunchedEffect(source) {
        val ready=source ?: return@LaunchedEffect
        try {
            player.setNativeYoutubeSource(ready)
            player.playWhenReady=!paused && (preview || foreground)
        } catch(e:CancellationException) {
            throw e
        } catch(_:Exception) {
            error=true
            return@LaunchedEffect
        }
        if(!preview) {
            awaitYoutubeLiveStall(player)
            if(liveRecoveries<2) {
                liveRecoveries++
                player.stop()
                retry++
            } else {
                player.stop()
                error=true
            }
        }
    }
    LaunchedEffect(paused,foreground) {player.playWhenReady=!paused && source!=null && (preview || foreground)}
    LaunchedEffect(controls,firstFrame) {
        if(firstFrame && controls) {delay(3200);controls=false}
    }
    LaunchedEffect(player,source) {
        while(source!=null) {
            position=player.currentPosition.coerceAtLeast(0L)
            duration=player.duration.coerceAtLeast(0L)
            delay(250)
        }
    }
    LaunchedEffect(video) {withFrameNanos {};runCatching {focus.requestFocus()}}
    val videoAlpha by animateFloatAsState(if(firstFrame) 1f else 0f,animationSpec=tween(280),label="rp200NativeFirstFrame")
    val keyHandling=Modifier.onPreviewKeyEvent {event ->
        val k=event.nativeKeyEvent
        if(k.action!=KeyEvent.ACTION_DOWN || k.repeatCount!=0) false
        else when(k.keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER,KeyEvent.KEYCODE_ENTER,KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                paused=!paused;controls=true;true
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                if(preview && previous!=null) previous()
                else player.seekTo((player.currentPosition-10000).coerceAtLeast(0))
                controls=true;true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if(preview && next!=null) next()
                else player.seekTo((player.currentPosition+10000).coerceAtMost(player.duration.coerceAtLeast(0)))
                controls=true;true
            }
            KeyEvent.KEYCODE_DPAD_UP -> {if(preview) details?.invoke() else controls=true;true}
            KeyEvent.KEYCODE_DPAD_DOWN -> {if(preview) watch?.invoke() else controls=true;true}
            KeyEvent.KEYCODE_BACK,KeyEvent.KEYCODE_ESCAPE -> {close();true}
            else -> false
        }
    }
    val playerContent: @Composable () -> Unit = {
    Box(Modifier.fillMaxSize().background(Color.Black).then(swipe).focusRequester(focus).then(keyHandling).focusable()) {
        AndroidView(factory={ctx->PlayerView(ctx).apply {
            useController=false
            resizeMode=androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
            this.player=player
            setShutterBackgroundColor(android.graphics.Color.BLACK)
        }},modifier=Modifier.fillMaxSize().graphicsLayer {alpha=videoAlpha})
        if(!firstFrame) Box(Modifier.fillMaxSize().background(Color.Black),contentAlignment=Alignment.Center) {
            Text(if(error) if(preview) tr("No se pudo cargar el tráiler nativo","Native video unavailable") else tr("No se pudo cargar el directo","Live stream unavailable")
                else if(!preview && liveRecoveries>0) tr("Reconectando el directo…","Reconnecting live stream…")
                else tr("Preparando reproducción…","Preparing playback…"),color=Color.White,fontSize=17.sp)
        }
        if(controls && firstFrame) Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth()
            .background(Color.Black.copy(alpha=.68f),RoundedCornerShape(topStart=16.dp,topEnd=16.dp))
            .padding(18.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text(caption,color=Color.White,fontSize=19.sp)
            if(duration>0) {
                Box(Modifier.fillMaxWidth().height(3.dp).background(Color.White.copy(alpha=.25f))) {
                    Box(Modifier.fillMaxWidth((position.toFloat()/duration.toFloat()).coerceIn(0f,1f)).fillMaxHeight().background(Color.White))
                }
                Text("${position/1000}s / ${duration/1000}s",color=Color.LightGray,fontSize=12.sp)
            }
            Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                FocusButton(if(paused) "▶" else "Ⅱ") {paused=!paused;controls=true}
                FocusButton("−10s") {player.seekTo((player.currentPosition-10000).coerceAtLeast(0));controls=true}
                FocusButton("+10s") {player.seekTo((player.currentPosition+10000).coerceAtMost(player.duration.coerceAtLeast(0)));controls=true}
                FocusButton(tr("Cerrar","Close"),onClick=close)
            }
        }
        if(error) Row(Modifier.align(Alignment.BottomCenter).padding(20.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            FocusButton(tr("Reintentar","Retry")) {liveRecoveries=0;retry++}
            FocusButton(tr("Cerrar","Close"),onClick=close)
        }
    }
    }
    if(embedded) playerContent()
    else Dialog(onDismissRequest=close,properties=DialogProperties(usePlatformDefaultWidth=false,decorFitsSystemWindows=false)) {playerContent()}
}
