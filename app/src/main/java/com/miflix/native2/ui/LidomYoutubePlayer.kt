package com.miflix.native2.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Text
import com.miflix.native2.data.*
import kotlinx.coroutines.CancellationException

internal data class LidomYoutubeSelection(val team:String,val source:String,val video:String)

internal fun openLidomWebsite(context:android.content.Context,url:String):Boolean {
    if(!LidomRepository.httpUrl(url)) return false
    return runCatching {context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));true}.getOrDefault(false)
}

@Composable
internal fun LidomYoutubeChooser(repo:LidomRepository,source:LidomSource,onPlay:(String)->Unit,close:()->Unit) {
    val context=LocalContext.current;var url by remember {mutableStateOf(source.url.takeIf {LidomRepository.youtubeId(it)!=null} ?: repo.lastVideo(source.team)?.takeIf {repo.last(source.team)==source.id}?.let {"https://www.youtube.com/watch?v=$it"}.orEmpty())}
    var rows by remember {mutableStateOf<List<LidomVideo>>(emptyList())};var message by remember {mutableStateOf("")};var loading by remember {mutableStateOf(true)}
    LaunchedEffect(source.id) {
        try {rows=repo.videos(source)} catch(e:CancellationException) {throw e} catch(_:Exception) {message=tr("No se pudo cargar la lista. Puedes usar el enlace de un video oficial.","Could not load the video list. You can enter an official video URL.")} finally {loading=false}
    }
    PlayerSideSheet(source.name+" · YouTube",close) {
        item {Text(tr("Elige un video o pega su enlace. Un video de archivo no se identifica como un partido en vivo.","Choose a video or paste its link. Archive videos are not labeled as live games."),color=Muted,fontSize=13.sp)}
        item {NativeTextField(url,{url=it},"YouTube video URL",modifier=Modifier.fillMaxWidth())}
        item {FocusButton(tr("Reproducir en BruniO","Play in BruniO"),primary=true) {val id=LidomRepository.youtubeId(url);if(id==null) message=tr("Introduce el enlace de un video de YouTube, no el del canal.","Enter a YouTube video URL, not a channel URL.") else onPlay(id)}}
        if(loading) item {Text(tr("Cargando videos…","Loading videos…"),color=Muted)}
        if(message.isNotBlank()) item {Text(message,color=Muted)}
        items(rows,key={it.id}) {video ->FocusButton(video.title,modifier=Modifier.fillMaxWidth()) {onPlay(video.id)}}
        item {FocusButton(tr("Abrir canal fuera de BruniO (opcional)","Open channel outside BruniO (optional)")) {if(!openLidomWebsite(context,source.url.ifBlank {source.reference})) message=tr("No se pudo abrir la página.","Could not open the page.")}}
    }
}

internal fun youtubeMessageRP165(status:String):String = when(status) {
    "blocked" -> tr("Pulsa Pausa / Reproducir para iniciar el juego aquí.","Press Pause / Play to start the game here.")
    "error:101","error:150" -> tr("Este canal no permite reproducir este video dentro de otras apps. Elige otra fuente.","This channel does not allow this video inside other apps. Choose another source.")
    "error:100" -> tr("El video ya no está disponible o es privado.","This video is unavailable or private.")
    "error:153" -> tr("YouTube no pudo verificar el reproductor. Actualiza Android System WebView y reintenta.","YouTube could not verify the player. Update Android System WebView and retry.")
    else -> tr("No se pudo iniciar YouTube aquí. Reintenta o elige otra fuente.","YouTube could not start here. Retry or choose another source.")+" ($status)"
}

private class LidomYoutubeBridge(val callback:(String)->Unit) {
    var active=true
    private val handler=Handler(Looper.getMainLooper())
    @JavascriptInterface fun status(value:String) {if(value in setOf("ready","playing","blocked","ended") || value.matches(Regex("error:[0-9]{1,3}"))) handler.post {if(active) callback(value)}}
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun LidomYoutubePlayer(video:String,onWorking:()->Unit,close:()->Unit,
    preview:Boolean=false,embedded:Boolean=false,caption:String="",previous:(()->Unit)?=null,next:(()->Unit)?=null,
    onEnded:(()->Unit)?=null,details:(()->Unit)?=null,watch:(()->Unit)?=null,swipe:Modifier=Modifier) {
    val context=LocalContext.current
    val previewFocus=remember(video) {FocusRequester()}
    LaunchedEffect(video,preview) {if(preview) {withFrameNanos {};runCatching {previewFocus.requestFocus()}}}
    var status by remember(video) {mutableStateOf("")}
    var view by remember(video) {mutableStateOf<WebView?>(null)}
    val latestWorking by rememberUpdatedState(onWorking)
    val latestEnded by rememberUpdatedState(onEnded)
    var recorded by remember(video) {mutableStateOf(false)}
    var fullscreenRP165 by remember(video) {mutableStateOf(false)}
    var retryRP165 by remember(video) {mutableIntStateOf(0)}
    androidx.activity.compose.BackHandler(enabled=fullscreenRP165 && !preview) {fullscreenRP165=false}
    LaunchedEffect(video,retryRP165) {
        kotlinx.coroutines.delay(30000)
        if(!recorded && status!="blocked" && !status.startsWith("error:")) status="error:0"
    }
    val bridge=remember(video) {LidomYoutubeBridge {value ->status=value;if(value=="playing" && !recorded) {recorded=true;latestWorking()};if(value=="ended") latestEnded?.invoke()}}
    val lifecycle=context.playbackActivity()?.lifecycle
    DisposableEffect(view,lifecycle) {
        val web=view
        val observer=androidx.lifecycle.LifecycleEventObserver {_,event ->if(event==androidx.lifecycle.Lifecycle.Event.ON_STOP) {web?.evaluateJavascript("brunioPause()",null);web?.onPause()} else if(event==androidx.lifecycle.Lifecycle.Event.ON_START) web?.onResume()}
        lifecycle?.addObserver(observer)
        onDispose {lifecycle?.removeObserver(observer)}
    }
    DisposableEffect(video) {onDispose {bridge.active=false;view?.let {it.evaluateJavascript("brunioPause()",null);it.stopLoading();it.removeJavascriptInterface("BruniO");it.onPause();it.destroy()};view=null}}
    val remoteController=Modifier.onPreviewKeyEvent {event ->
        val key=event.nativeKeyEvent
        if(preview && TrailerRemotePolicy.handles(key.keyCode)) {
            when(TrailerRemotePolicy.action(key.keyCode,key.action==android.view.KeyEvent.ACTION_DOWN,key.repeatCount)) {
                "previous" ->previous?.invoke()
                "next" ->next?.invoke()
                "details" ->details?.invoke()
                "watch" ->watch?.invoke()
                "pause" ->view?.evaluateJavascript("brunioPlayPause()",null)
            }
            true
        } else if(!preview && key.keyCode in setOf(android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,android.view.KeyEvent.KEYCODE_MEDIA_PLAY,android.view.KeyEvent.KEYCODE_MEDIA_PAUSE)) {
            if(key.action==android.view.KeyEvent.ACTION_DOWN && key.repeatCount==0) view?.evaluateJavascript("brunioPlayPause()",null)
            true
        } else false
    }
    val playerContent: @Composable () -> Unit = {
        RemoteDialogInputRP167()
        Column(Modifier.fillMaxSize().background(Color.Black).then(remoteController)) {
            if(preview && !embedded) {
                Text(caption,color=Color.White,fontSize=20.sp,maxLines=1,overflow=TextOverflow.Ellipsis,modifier=Modifier.fillMaxWidth().padding(start=24.dp,end=24.dp,top=10.dp,bottom=4.dp))
            } else if(!fullscreenRP165) Row(Modifier.fillMaxWidth().padding(10.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                FocusButton(tr("Volver a fuentes","Back to sources"),onClick=close)
                FocusButton(tr("Pausa / Reproducir","Pause / Play")) {view?.evaluateJavascript("brunioPlayPause()",null)}
                FocusButton("−10s") {view?.evaluateJavascript("brunioSeek(-10)",null)}
                FocusButton("+10s") {view?.evaluateJavascript("brunioSeek(10)",null)}
                FocusButton(tr("Pantalla completa","Full screen")) {fullscreenRP165=true}
                FocusButton("YouTube") {openLidomWebsite(context,"https://www.youtube.com/watch?v=$video")}
            }
            if(status.startsWith("error:") || status=="blocked") Row(Modifier.padding(10.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                Text(youtubeMessageRP165(status),color=Color.White,modifier=Modifier.weight(1f))
                if(!preview) FocusButton(tr("Reintentar","Retry")) {status="";recorded=false;retryRP165++;view?.reload()}
                if(!preview) FocusButton(tr("Probar otra fuente","Try another source"),onClick=close)
            }
            AndroidView(modifier=Modifier.weight(1f).fillMaxWidth().then(if(preview) Modifier.focusRequester(previewFocus).focusable() else Modifier),factory={c ->
                val frame=android.widget.FrameLayout(c)
                val web=WebView(c).apply {
                    layoutParams=android.widget.FrameLayout.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT,android.view.ViewGroup.LayoutParams.MATCH_PARENT)
                    setLayerType(android.view.View.LAYER_TYPE_HARDWARE,null)
                    settings.offscreenPreRaster=true
                    keepScreenOn=true
                    setBackgroundColor(android.graphics.Color.BLACK)
                    settings.javaScriptEnabled=true;settings.domStorageEnabled=true
                    settings.allowFileAccess=false;settings.allowContentAccess=false
                    settings.mixedContentMode=android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
                    settings.mediaPlaybackRequiresUserGesture=false
                    android.webkit.CookieManager.getInstance().setAcceptThirdPartyCookies(this,true)
                    webChromeClient=WebChromeClient()
                    webViewClient=object:WebViewClient() {
                        override fun shouldOverrideUrlLoading(v:WebView,request:WebResourceRequest):Boolean {
                            return request.isForMainFrame
                        }
                    }
                    addJavascriptInterface(bridge,"BruniO")
                    val origin="https://"+c.packageName.lowercase(java.util.Locale.ROOT)
                    val html=c.assets.open(if(preview) "discovery_youtube.html" else "lidom_youtube.html").bufferedReader().use {it.readText()}.replace("__VIDEO_ID__",video).replace("__ORIGIN__",origin)
                    loadDataWithBaseURL(origin+"/",html,"text/html","UTF-8",null)
                    view=this
                }
                frame.addView(web)
                frame
            })
        // RP198: immersive Discover Trailers uses remote shortcuts without a permanent action strip.
        // Fullscreen/standalone previews retain the visible controls for accessibility.
        if(preview && !embedded) TrailerActionBar(previous,next,details,watch,swipe)

    }
}
    if(embedded) playerContent() else Dialog(onDismissRequest=close,properties=DialogProperties(usePlatformDefaultWidth=false,decorFitsSystemWindows=false)) {playerContent()}
}
