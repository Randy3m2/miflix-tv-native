package com.miflix.native2.ui

import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay

internal object MenuActivity {
    var lastInput by mutableLongStateOf(SystemClock.elapsedRealtime())
    fun touch() {lastInput=SystemClock.elapsedRealtime()}
}

@Composable
internal fun PlaybackArtwork(state: AppState,request: PlayerRequest) {
    val details=state.details?.takeIf {it.summary.cloudId==request.item.cloudId}
    Box(Modifier.fillMaxSize().background(Bg),contentAlignment=Alignment.Center) {
        AsyncImage(request.item.backdrop ?: request.item.poster,null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x77000000),Color(0xC0000000)))))
        Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(18.dp)) {
            if(details?.logo!=null) AsyncImage(details.logo,request.item.title,Modifier.width(380.dp).height(150.dp),contentScale=ContentScale.Fit)
            else Text(request.item.title,color=Color.White,fontSize=32.sp,fontWeight=FontWeight.Black)
            Text(if(request.item.type=="series") "S${request.season} E${request.episode}" else "BruniO",color=Color.White,fontSize=16.sp)
            Text(tr("Preparando reproducción…","Preparing playback…"),color=Color(0xFFC5C5CA),fontSize=13.sp)
        }
    }
}

@Composable
internal fun MenuScreensaver(state: AppState) {
    var visible by remember {mutableStateOf(false)}
    val eligible=state.appForeground && state.screen !in setOf(Screen.PLAYER,Screen.MULTIVIEW,Screen.SPLASH,Screen.PAIR_DEVICE) && !state.miniPlayer && state.busyMessage==null && state.sourceSelection==null && !state.localCastPairing
    val rows=(if(state.kidsMode) state.kidsMovies else (state.trending+state.movies).filter {it.type=="movie"}.distinctBy {it.cloudId}).filter {it.backdrop!=null}.take(10)
    LaunchedEffect(eligible,state.screen,MenuActivity.lastInput,rows.map {it.cloudId}) {
        visible=false
        if(eligible && rows.isNotEmpty()) {
            delay((300000L-(SystemClock.elapsedRealtime()-MenuActivity.lastInput)).coerceAtLeast(0L))
            visible=true
        }
    }
    if(!visible || !eligible || rows.isEmpty()) return
    var index by remember {mutableIntStateOf(0)}
    LaunchedEffect(rows.map {it.cloudId}) {while(true) {delay(14000);index=(index+1)%rows.size} }
    fun dismiss() {visible=false;MenuActivity.touch()}
    Dialog(onDismissRequest={dismiss()},properties=DialogProperties(usePlatformDefaultWidth=false)) {
        val focus=remember {FocusRequester()}
        LaunchedEffect(Unit) {withFrameNanos {};focus.requestFocus()}
        BackHandler {dismiss()}
        Box(Modifier.fillMaxSize().background(Color.Black).focusRequester(focus).onPreviewKeyEvent {
            // Consume both halves of the wake gesture so it cannot activate a movie.
            if(it.type==KeyEventType.KeyUp) dismiss()
            true
        }.focusable().clickable {dismiss()}) {
            androidx.compose.foundation.Image(androidx.compose.ui.res.painterResource(com.miflix.native2.R.drawable.brunio_profile_landscape),null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
            Crossfade(rows[index.coerceIn(0,rows.lastIndex)],animationSpec=tween(700),label="screensaver") {title ->
                AsyncImage(title.backdrop?.replace("/w780/","/w1280/"),null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
            }
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent,Color(0xD0000000)))))
            Column(Modifier.align(Alignment.BottomStart).padding(42.dp)) {
                Text("BruniO · TOP 10",color=Color.White,fontSize=14.sp)
                Text(rows[index.coerceIn(0,rows.lastIndex)].title,color=Color.White,fontSize=30.sp,fontWeight=FontWeight.Bold)
                Text(tr("Pulsa cualquier botón para volver","Press any button to return"),color=Color(0xFFCCCCCC),fontSize=13.sp)
            }
        }
    }
}
