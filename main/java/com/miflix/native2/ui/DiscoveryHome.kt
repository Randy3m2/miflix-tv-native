package com.miflix.native2.ui

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
private fun moodGlyph(mood:String)=when(mood) {"christmas"->"❄";"happy"->"☀";"sad"->"☂";"documentary"->"◉";"marathon"->"∞";"twist"->"↝";"romantic"->"♡";"adrenaline"->"ϟ";"mindbending"->"✧";else->"◈"}

@Composable
private fun MoodTile(mood:String,selected:Boolean,modifier:Modifier=Modifier,onClick:()->Unit) {
    var focused by remember {mutableStateOf(false)}
    val hue=when(mood) {
        "christmas" ->Color(0xff51baa0);"happy","cozy" ->Color(0xffd8ac68);"sad" ->Color(0xff7c9cdb)
        "romantic" ->Color(0xffd482a0);"adrenaline","halloween" ->Color(0xffd68b65)
        else ->Color(0xffac92d9)
    }
    Row(modifier.height(62.dp).width(220.dp).onFocusChanged {focused=it.isFocused}.focusable().tvClick(onClick).clickable(onClick=onClick)
        .background(Brush.horizontalGradient(listOf(hue.copy(alpha=if(selected || focused) .34f else .16f),Color(0xff17151f))),RoundedCornerShape(18.dp))
        .border(if(focused) 2.dp else 1.dp,if(focused) Accent else if(selected) hue else hue.copy(alpha=.25f),RoundedCornerShape(18.dp))
        .padding(horizontal=13.dp),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
        Text(moodGlyph(mood),color=hue,fontSize=26.sp)
        Column(Modifier.weight(1f)) {
            Text(moodTitle(mood),color=Color.White,fontSize=14.sp,maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            Text(if(selected) tr("SELECCIONADO","SELECTED") else tr("EXPLORAR","EXPLORE"),color=if(selected) hue else Muted,fontSize=9.sp)
        }
    }
}

@Composable
internal fun TrailerActionBar(previous:(()->Unit)?,next:(()->Unit)?,details:(()->Unit)?,watch:(()->Unit)?,swipe:Modifier) {
    Row(Modifier.fillMaxWidth().then(swipe).background(Color(0xff111017)).padding(horizontal=20.dp,vertical=14.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
        listOf(Triple("←","Previous",previous),Triple("→","Next",next),Triple("↑","Details",details),Triple("↓","Watch Title",watch)).forEach { (hint,label,action) ->
            androidx.compose.foundation.layout.Box(Modifier.weight(1f).height(48.dp)
                .background(if(label=="Watch Title") Accent.copy(alpha=.2f) else Color(0xff24212c),androidx.compose.foundation.shape.RoundedCornerShape(24.dp))
                .then(if(action!=null) Modifier.clickable(onClick=action) else Modifier).padding(horizontal=14.dp),contentAlignment=androidx.compose.ui.Alignment.Center) {
                Text("$hint  $label",color=if(action!=null) Color.White else Muted,fontSize=15.sp)
            }
        }
    }
}

@Composable
internal fun DiscoveryControls(mood:String,loading:Boolean,onMood:(String)->Unit,onTrailers:()->Unit,onSurprise:()->Unit,onTools:()->Unit) {
    CompositionLocalProvider(LocalSimpleHome provides false) {
        Column(Modifier.fillMaxWidth().padding(horizontal=40.dp,vertical=4.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                Text(tr("Elige tu vibe","Pick your vibe"),color=Color.White,fontSize=23.sp)
                Text(tr("TU NOCHE. TU ESTILO.","YOUR NIGHT. YOUR VIBE."),color=Accent,fontSize=11.sp)
            }
            HomeHorizontalRail {
                LazyRow(Modifier.fillMaxWidth().smoothHorizontalDpad(),horizontalArrangement=Arrangement.spacedBy(10.dp),contentPadding=PaddingValues(vertical=4.dp)) {
                    items(DiscoveryMoods,key={it.ifEmpty {"for-me"}}) {value ->
                        MoodTile(value,mood==value,homeReturnFocus("mood:"+value.ifEmpty {"default"})) {onMood(value)}
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
