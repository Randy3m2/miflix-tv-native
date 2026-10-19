package com.miflix.native2.ui

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
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
import androidx.tv.material3.Text
import com.miflix.native2.data.LidomRepository

private class SplitYoutubeBridgeRP9(val report:(String)->Unit) {
    var active=true
    @JavascriptInterface fun error(code:Int) {
        Handler(Looper.getMainLooper()).post {if(active) report("YouTube · $code")}
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun LidomSplitYoutubeTileRP9(req:PlayerRequest,audible:Boolean,modifier:Modifier,click:()->Unit) {
    val context=LocalContext.current
    val video=LidomRepository.youtubeId(req.stream.url)
    var web by remember(req.playbackId) {mutableStateOf<WebView?>(null)}
    var focused by remember {mutableStateOf(false)}
    var message by remember(req.playbackId) {mutableStateOf("")}
    val bridge=remember(req.playbackId) {SplitYoutubeBridgeRP9 {message=it}}
    val lifecycle=context.playbackActivity()?.lifecycle
    val active=lifecycle?.currentState?.isAtLeast(Lifecycle.State.STARTED)!=false
    val latestSound by rememberUpdatedState(audible)
    DisposableEffect(web,lifecycle) {
        val view=web
        val observer=LifecycleEventObserver {_,event ->
            if(event==Lifecycle.Event.ON_STOP) {
                view?.evaluateJavascript("brunioState(false,false)",null);view?.onPause()
            } else if(event==Lifecycle.Event.ON_START) {
                view?.onResume();view?.evaluateJavascript("brunioState($latestSound,true)",null)
            }
        }
        lifecycle?.addObserver(observer)
        onDispose {lifecycle?.removeObserver(observer)}
    }
    DisposableEffect(req.playbackId) {
        onDispose {
            bridge.active=false
            web?.let {v ->
                v.evaluateJavascript("brunioState(false,false)",null)
                v.stopLoading();v.removeJavascriptInterface("BruniOSplit");v.onPause();v.destroy()
            }
            web=null
        }
    }
    Box(modifier.onFocusChanged {focused=it.hasFocus}.tvClick(click).focusable()
        .border(if(focused) 3.dp else 1.dp,if(focused) Accent else Color(0xFF444444))
        .padding(4.dp).background(Color.Black)) {
        if(video!=null) AndroidView(factory={c ->
            WebView(c).apply {
                setBackgroundColor(android.graphics.Color.BLACK)
                isFocusable=false;isFocusableInTouchMode=false
                settings.javaScriptEnabled=true;settings.domStorageEnabled=true
                settings.allowFileAccess=false;settings.allowContentAccess=false
                settings.mixedContentMode=android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
                settings.mediaPlaybackRequiresUserGesture=false
                webChromeClient=WebChromeClient()
                webViewClient=object:WebViewClient() {
                    override fun shouldOverrideUrlLoading(view:WebView,request:WebResourceRequest)=request.isForMainFrame
                }
                addJavascriptInterface(bridge,"BruniOSplit")
                val origin="https://"+c.packageName
                val html="""<!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1"><style>
                html,body,#player{margin:0;width:100%;height:100%;background:black;overflow:hidden}
                </style></head><body><div id="player"></div><script>
                var p=null,ready=false,sound=$audible,playing=$active;
                function brunioState(s,a){sound=s;playing=a;if(!ready)return;
                if(sound)p.unMute();else p.mute();if(playing)p.playVideo();else p.pauseVideo();}
                function onYouTubeIframeAPIReady(){p=new YT.Player('player',{width:'100%',height:'100%',videoId:'$video',
                playerVars:{autoplay:0,controls:1,playsinline:1,origin:'$origin'},
                events:{onReady:function(){ready=true;brunioState(sound,playing);},
                onError:function(e){BruniOSplit.error(e.data);},
                onAutoplayBlocked:function(){BruniOSplit.error(0);}}});}
                </script><script src="https://www.youtube.com/iframe_api"></script></body></html>"""
                loadDataWithBaseURL(origin+"/",html,"text/html","UTF-8",null)
                web=this
            }
        },update={it.evaluateJavascript("brunioState($audible,$active)",null)},modifier=Modifier.fillMaxSize())
        // Keep remote focus on the tile; OK opens the shared multiview controls.
        Box(Modifier.fillMaxSize().clickable(onClick=click))
        Text((if(audible) "♫  " else "")+req.item.title,color=Color.White,fontSize=15.sp,
            modifier=Modifier.align(Alignment.BottomStart).background(Color(0xB0000000)).padding(8.dp))
        if(message.isNotBlank() || video==null) Text(
            tr("No se pudo reproducir YouTube. OK para elegir otra fuente.","YouTube could not play. Press OK to choose another source.")+" "+message,
            color=Color.White,modifier=Modifier.align(Alignment.Center).background(Color.Black).padding(12.dp))
    }
}
