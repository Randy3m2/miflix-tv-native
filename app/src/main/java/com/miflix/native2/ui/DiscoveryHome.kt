package com.miflix.native2.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.shape.RoundedCornerShape
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

internal val DiscoveryMoods=listOf("","christmas","happy","sad","documentary","marathon","background","twist","cozy","romantic","adrenaline","mindbending","family","nostalgia","quick","meaningful","halloween")
internal fun moodTitle(mood:String):String=when(mood) {
    "" -> tr("Para mí","For me")
    "christmas" -> tr("Navideño","Christmas")
    "happy","comfort" -> tr("Feliz","Feel good")
    "sad" -> tr("Una buena llorada","A good cry")
    "documentary" -> tr("Documentales","Documentaries")
    "marathon" -> tr("Modo maratón","Marathon mode")
    "background" -> tr("Ruido de fondo","Background noise")
    "twist" -> "Plot twist"
    "cozy" -> tr("Noche acogedora","Cozy night")
    "romantic" -> tr("Noche romántica","Date night")
    "adrenaline" -> tr("Adrenalina","Adrenaline rush")
    "mindbending" -> tr("Rompe la mente","Mind-bending")
    "family" -> tr("En familia","Family time")
    "nostalgia" -> tr("Nostalgia","Nostalgia")
    "quick" -> tr("Hasta 30 min","Under 30 min")
    "halloween" -> "Halloween"
    else -> tr("Algo con sentido","Something meaningful")
}
private fun moodGlyph(mood:String)=when(mood) {
    "christmas"->"❄";"happy"->"☀";"sad"->"☂";"documentary"->"◎";"marathon"->"∞";"background"->"♫"
    "twist"->"↯";"cozy"->"☕";"romantic"->"♡";"adrenaline"->"ϟ";"mindbending"->"✧";"family"->"⌂"
    "nostalgia"->"◷";"quick"->"◴";"meaningful"->"✦";"halloween"->"☾";else->"✧"
}

@Composable
private fun MoodTile(mood:String,selected:Boolean,modifier:Modifier=Modifier,image:String?=null,onClick:()->Unit) {
    var focused by remember {mutableStateOf(false)}
    val scale by animateFloatAsState(if(focused) 1.025f else 1f,tween(150),label="mood-photo-focus")
    val hue=when(mood) {
        "christmas" ->Color(0xff62dbc0);"happy","cozy" ->Color(0xffffd28a);"sad" ->Color(0xff9ebdff)
        "romantic" ->Color(0xffffb3d1);"adrenaline","halloween" ->Color(0xffffb484)
        else ->Color(0xffcab2ff)
    }
    val shape=RoundedCornerShape(19.dp)
    Box(modifier.height(100.dp).width(224.dp).graphicsLayer {scaleX=scale;scaleY=scale}
        .onFocusChanged {focused=it.isFocused}.clip(shape)
        .background(Brush.linearGradient(listOf(hue.copy(alpha=.3f),Color(0xff121521))))
        .border(if(focused || selected) 2.dp else 1.dp,if(focused) Color.White else if(selected) hue else Color.White.copy(alpha=.14f),shape)
        .clickable(onClick=onClick)) {
        if(image!=null) AsyncImage(image,contentDescription=moodTitle(mood),contentScale=ContentScale.Crop,modifier=Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color.Black.copy(alpha=.88f),Color.Black.copy(alpha=.30f)))))
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent,Color.Black.copy(alpha=.45f)))))
        Row(Modifier.fillMaxSize().padding(14.dp),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(40.dp).background(hue.copy(alpha=.16f),RoundedCornerShape(13.dp)).border(1.dp,hue.copy(alpha=.5f),RoundedCornerShape(13.dp)),contentAlignment=androidx.compose.ui.Alignment.Center) {
                Text(if(selected) "✓" else moodGlyph(mood),color=hue,fontSize=25.sp)
            }
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)) {
                Text(moodTitle(mood),color=Color.White,fontSize=15.sp,fontWeight=androidx.compose.ui.text.font.FontWeight.Bold,maxLines=2,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                Text(if(selected) tr("TU VIBE","YOUR VIBE") else tr("EXPLORAR","EXPLORE"),color=if(selected) hue else Color(0xffd2d5df),fontSize=9.sp)
            }
        }
    }
}

@Composable
internal fun TrailerActionBar(previous:(()->Unit)?,next:(()->Unit)?,details:(()->Unit)?,watch:(()->Unit)?,swipe:Modifier) {
    Row(Modifier.fillMaxWidth().then(swipe).padding(horizontal=24.dp,vertical=8.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
        listOf(Triple("←","Previous",previous),Triple("→","Next",next),Triple("↑","Details",details),Triple("↓","Watch Title",watch)).forEach { (hint,label,action) ->
            Row(Modifier.height(38.dp).background(Color.White.copy(alpha=if(label=="Watch Title") .14f else .07f),RoundedCornerShape(19.dp))
                .border(1.dp,Color.White.copy(alpha=.14f),RoundedCornerShape(19.dp))
                .then(if(action!=null) Modifier.clickable(onClick=action) else Modifier).padding(horizontal=14.dp),
                verticalAlignment=androidx.compose.ui.Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Text(hint,color=if(action!=null) Accent else Muted,fontSize=24.sp,fontWeight=androidx.compose.ui.text.font.FontWeight.Bold)
                Text(label,color=if(action!=null) Color.White else Muted,fontSize=12.sp)
            }
        }
    }
}

@Composable
internal fun DiscoveryControls(mood:String,loading:Boolean,onMood:(String)->Unit,onTrailers:()->Unit,onSurprise:()->Unit,onTools:()->Unit,art:Map<String,String> = emptyMap()) {
    CompositionLocalProvider(LocalSimpleHome provides false) {
        Column(Modifier.fillMaxWidth().padding(horizontal=40.dp,vertical=4.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                Text(tr("Elige tu vibe","Pick your vibe"),color=Color.White,fontSize=23.sp)
                Text(tr("TU NOCHE. TU ESTILO.","YOUR NIGHT. YOUR VIBE."),color=Accent,fontSize=11.sp)
            }
            HomeHorizontalRail {
                LazyRow(Modifier.fillMaxWidth().smoothHorizontalDpad(),horizontalArrangement=Arrangement.spacedBy(10.dp),contentPadding=PaddingValues(vertical=4.dp)) {
                    items(DiscoveryMoods,key={it.ifEmpty {"for-me"}}) {value ->
                        MoodTile(value,mood==value,homeReturnFocus("mood:"+value.ifEmpty {"default"}),image=art[value]) {onMood(value)}
                    }
                }
            }
            Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                FocusButton("▶  "+tr("Deslúmbrame","Dazzle me"),modifier=homeReturnFocus("discovery:trailers"),onClick=onTrailers)
                FocusButton(if(loading) tr("Preparando…","Preparing…") else "✧  "+tr("Sorpréndeme","Surprise me"),modifier=homeReturnFocus("discovery:surprise"),onClick=onSurprise)
                FocusButton(tr("Mi universo de cine","My cinema hub"),onClick=onTools)
            }
        }
    }
}

/** Touch drags and remote arrows advance horizontally; only the current YouTube player exists. */
@Composable
internal fun DiscoveryTrailers(state:AppState,candidates:List<MediaSummary>,mood:String="",close:()->Unit) {
    DisposableEffect(state) {
        state.discoveryPreviewActive=true
        onDispose {state.discoveryPreviewActive=false;MenuActivity.touch()}
    }
    var rows by remember {mutableStateOf<List<MediaSummary>>(emptyList())}
    var queueLoading by remember {mutableStateOf(true)}
    var queueError by remember {mutableStateOf(false)}
    var queueRetry by remember {mutableIntStateOf(0)}
    LaunchedEffect(mood,queueRetry) {
        queueLoading=true;queueError=false
        try {
            val pool=if(mood.isBlank()) candidates else state.discoveryRun(mood)
            val today=java.text.SimpleDateFormat("yyyy-MM-dd",java.util.Locale.US).format(java.util.Date())
            rows=state.discoveryOrder("trailers:$mood",pool.distinctBy {it.cloudId}.filter {it.releaseDate.isBlank() || it.releaseDate<=today}).take(40)
        } catch(e:CancellationException) {throw e} catch(_:Exception) {queueError=true} finally {queueLoading=false}
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
    Dialog(onDismissRequest=close,properties=DialogProperties(usePlatformDefaultWidth=false,decorFitsSystemWindows=false)) {
    if(item!=null && playing!=null) key(item.cloudId,playing.key) {
        LidomYoutubePlayer(playing.key,onWorking={state.recordDiscovery("trailers:$mood",item)},close=close,preview=true,embedded=true,
            caption="${index+1}/${rows.size} · ${item.title}",
            previous=if(index>0) ({index--; Unit}) else null,next={next()},onEnded={next()},
            details={close();state.launch {state.open(item)}},
            watch={close();state.launch {state.surpriseDiscovery(listOf(item))}},swipe=swipe)
    } else {
        Column(Modifier.fillMaxSize().background(Color.Black).padding(28.dp).then(swipe),verticalArrangement=Arrangement.spacedBy(18.dp)) {
            Text(item?.title ?: tr("Tráileres","Trailers"),color=Color.White,fontSize=24.sp)
            Text(if(queueLoading || loading && item!=null) tr("Cargando tráiler…","Loading trailer…") else if(queueError) tr("No se pudo cargar esta selección.","Could not load this selection.") else if(rows.isEmpty()) tr("No hay tráileres disponibles para esta selección.","No trailers available for this selection.") else message,color=Muted)
            Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                if(index>0) FocusButton("←") {index--}
                if(!queueLoading && (!loading || item==null)) FocusButton(tr("Reintentar","Retry")) {if(item==null) queueRetry++ else retry++}
                if(index<rows.lastIndex) FocusButton(tr("Siguiente →","Next →")) {next()}
                FocusButton(tr("Cerrar","Close"),onClick=close)
            }
        }
    }
}
}
