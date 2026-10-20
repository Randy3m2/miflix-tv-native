package com.miflix.native2.ui

import android.annotation.SuppressLint
import android.webkit.*
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.tv.material3.Text
import androidx.compose.ui.window.DialogProperties

@Composable
internal fun HubWebCardRP41(title:String,logo:String,click:()->Unit) {
    val context=LocalContext.current
    val resource=context.resources.getIdentifier("sports_"+logo+"_rp41","drawable",context.packageName)
    var focused by remember {mutableStateOf(false)}
    Column(Modifier.fillMaxWidth().height(194.dp).background(Color(0xFF161616),RoundedCornerShape(13.dp))
        .border(if(focused) 2.dp else 1.dp,if(focused) Color.White else PanelBorder,RoundedCornerShape(13.dp))
        .onFocusChanged {focused=it.isFocused}.focusable().tvClick(click).clickable(onClick=click).padding(16.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Box(Modifier.weight(1f).fillMaxWidth().background(Color.White,RoundedCornerShape(8.dp)).padding(12.dp),contentAlignment=Alignment.Center) {
            if(resource!=0) Image(painterResource(resource),title,Modifier.fillMaxSize()) else Text(title,color=Color.Black,fontSize=26.sp)
        }
        Text(title,color=Color.White)
        Text("WEB",color=Muted,fontSize=10.sp)
    }
}

/** These are HTML pages, not media URLs. Never pass them to Media3 or infer a stream. */
@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun SportsWebRP41(url:String,close:()->Unit) {
    val context=LocalContext.current
    var message by remember {mutableStateOf("")}
    val web=remember(url) {WebView(context).apply {
        settings.javaScriptEnabled=true;settings.domStorageEnabled=true;settings.mediaPlaybackRequiresUserGesture=false
        settings.allowFileAccess=false;settings.allowContentAccess=false;settings.setSupportMultipleWindows(false)
        settings.javaScriptCanOpenWindowsAutomatically=false
        webViewClient=object:WebViewClient() {
            override fun shouldOverrideUrlLoading(view:WebView,request:WebResourceRequest):Boolean {
                if(!request.isForMainFrame) return false
                return request.url.scheme!="https" || request.url.host !in setOf("sportsbite.org","www.sportsbite.org")
            }
            override fun onReceivedHttpError(view:WebView,request:WebResourceRequest,response:WebResourceResponse) {
                if(request.isForMainFrame) message="HTTP ${response.statusCode} · "+tr("El sitio no permitió abrir la página.","The site did not allow this page to open.")
            }
        }
        webChromeClient=WebChromeClient()
        loadUrl(url)
    }}
    DisposableEffect(web) {onDispose {web.stopLoading();web.onPause();web.destroy()}}
    PlayerMenuDialog(onDismissRequest=close,fullScreen=true,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        BackHandler {close()}
        Column(Modifier.fillMaxSize().background(Color.Black)) {
            Row(Modifier.fillMaxWidth().padding(10.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                FocusButton(tr("Cerrar","Close"),onClick=close)
                Text("Sportsbite · Web",color=Color.White)
                if(message.isNotBlank()) Text(message,color=Muted,fontSize=12.sp)
            }
            AndroidView(factory={web},modifier=Modifier.weight(1f).fillMaxWidth())
        }
    }
}
