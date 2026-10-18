package com.miflix.native2.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Text
import com.miflix.native2.model.MediaSummary
import com.miflix.native2.model.TrailerSummary
import kotlinx.coroutines.CancellationException

internal fun moodTitle(mood:String):String=when(mood) {
    "quick" -> tr("Ver rápido · hasta 30 min","Quick watch · up to 30 min")
    "marathon" -> tr("Modo maratón","Marathon mode")
    "meaningful" -> tr("Algo con sentido","Something meaningful")
    "background" -> tr("Ruido de fondo","Background noise")
    "christmas" -> tr("Una noche de Navidad","Christmas stories")
    "halloween" -> tr("Temporada de Halloween","Halloween season")
    else -> tr("Algo para sentirte bien","Feel-good picks")
}

@Composable
internal fun DiscoveryControls(mood:String,loading:Boolean,onMood:(String)->Unit,onTrailers:()->Unit,onSurprise:()->Unit) {
    CompositionLocalProvider(LocalSimpleHome provides false) {
    Column(Modifier.fillMaxWidth().padding(horizontal=40.dp,vertical=4.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text(tr("¿Qué te apetece ver?","What are you in the mood for?"),color=Color.White,fontSize=22.sp)
        HomeHorizontalRail {
            LazyRow(Modifier.fillMaxWidth().smoothHorizontalDpad(),horizontalArrangement=Arrangement.spacedBy(10.dp),contentPadding=PaddingValues(vertical=2.dp)) {
                item {FocusButton(tr("Para mí","For me"),primary=mood.isEmpty(),modifier=homeReturnFocus("mood:default")) {onMood("")}}
                items(listOf("quick","marathon","meaningful","background")) {value ->
                    FocusButton(moodTitle(value),primary=mood==value,modifier=homeReturnFocus("mood:$value")) {onMood(value)}
                }
            }
        }
        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            FocusButton(tr("Deslúmbrame · Tráileres →","Dazzle me · Trailers →"),modifier=homeReturnFocus("discovery:trailers"),onClick=onTrailers)
            FocusButton(if(loading) tr("Preparando selección…","Preparing picks…") else tr("Sorpréndeme","Surprise me"),modifier=homeReturnFocus("discovery:surprise"),onClick=onSurprise)
        }
        Text(tr("También puedes buscar: algo para sentirme bien · un thriller con final inesperado","Try searching: something to feel good · a thriller with an unexpected ending"),color=Muted,fontSize=12.sp,maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
    }
    }
}

/** Touch drags and remote arrows advance horizontally; only the current YouTube player exists. */
@Composable
internal fun DiscoveryTrailers(state:AppState,candidates:List<MediaSummary>,close:()->Unit) {
    DisposableEffect(state) {
        state.discoveryPreviewActive=true
        onDispose {state.discoveryPreviewActive=false;MenuActivity.touch()}
    }
    val rows=remember(candidates) {
        val today=java.text.SimpleDateFormat("yyyy-MM-dd",java.util.Locale.US).format(java.util.Date())
        candidates.distinctBy {it.cloudId}.filter {it.releaseDate.isBlank() || it.releaseDate<=today}.take(40)
    }
    var index by remember {mutableIntStateOf(0)}
    var trailer by remember {mutableStateOf<TrailerSummary?>(null)}
    var loadedCloud by remember {mutableStateOf<String?>(null)}
    var message by remember {mutableStateOf("")}
    var loading by remember {mutableStateOf(true)}
    var retry by remember {mutableIntStateOf(0)}
    var drag by remember {mutableFloatStateOf(0f)}
    val item=rows.getOrNull(index)
    fun next() {if(index<rows.lastIndex) index++ else close()}
    LaunchedEffect(item?.cloudId,retry,state.interfaceLanguage,state.activeProfile.id) {
        trailer=null;loadedCloud=null;message="";loading=true
        try {
            if(item!=null && (!state.kidsMode || state.tmdb.kidsAllowed(item))) trailer=state.tmdb.previewTrailers(item).firstOrNull()
            loadedCloud=item?.cloudId
            if(trailer==null) message=tr("No hay un tráiler disponible para este título.","No trailer is available for this title.")
        } catch(e:CancellationException) {throw e} catch(_:Exception) {message=tr("No se pudo cargar el tráiler. Puedes reintentar o avanzar.","Could not load the trailer. Retry or move to the next title.")} finally {loading=false}
    }
    // Warm only the next title's metadata. No second WebView or video download.
    LaunchedEffect(loadedCloud,index) {
        if(loadedCloud==item?.cloudId && item!=null && index<rows.lastIndex) {
            try {state.tmdb.previewTrailers(rows[index+1])} catch(e:CancellationException) {throw e} catch(_:Exception) { }
        }
    }
    val swipe=Modifier.pointerInput(index) {
        detectHorizontalDragGestures(onDragStart={drag=0f},onDragCancel={drag=0f},onDragEnd={
            if(drag < -80f) next() else if(drag > 80f && index>0) index--
            drag=0f
        }) {change,amount ->change.consume();drag+=amount}
    }
    val playing=trailer.takeIf {loadedCloud==item?.cloudId}
    if(item!=null && playing!=null) key(item.cloudId,playing.key) {
        LidomYoutubePlayer(playing.key,onWorking={},close=close,preview=true,
            caption="${index+1}/${rows.size} · ${item.title}",
            previous=if(index>0) ({index--; Unit}) else null,next={next()},onEnded={next()},
            details={close();state.launch {state.open(item)}},
            watch={close();state.launch {state.surpriseDiscovery(listOf(item))}},swipe=swipe)
    } else Dialog(onDismissRequest=close,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Column(Modifier.fillMaxWidth(.8f).background(Color(0xff101016)).padding(28.dp).then(swipe),verticalArrangement=Arrangement.spacedBy(18.dp)) {
            Text(item?.title ?: tr("Tráileres","Trailers"),color=Color.White,fontSize=24.sp)
            Text(if(loading) tr("Cargando tráiler…","Loading trailer…") else message,color=Muted)
            Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                if(index>0) FocusButton("←") {index--}
                if(!loading) FocusButton(tr("Reintentar","Retry")) {retry++}
                if(index<rows.lastIndex) FocusButton(tr("Siguiente →","Next →")) {next()}
                FocusButton(tr("Cerrar","Close"),onClick=close)
            }
        }
    }
}
