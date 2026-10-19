package com.miflix.native2.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Text
import com.miflix.native2.model.MediaSummary
import com.miflix.native2.model.EpisodeSummary

internal val LocalTitleActions=staticCompositionLocalOf<((MediaSummary)->Unit)?> {null}

@Composable
internal fun TitleActionsDialog(state: AppState,item: MediaSummary,continuing: Boolean,close: ()->Unit) {
    val first=remember {FocusRequester()}
    LaunchedEffect(Unit) {withFrameNanos {};runCatching {first.requestFocus()} }
    suspend fun prepare() {
        state.open(item)
        check(state.selected?.cloudId==item.cloudId && state.details?.summary?.cloudId==item.cloudId) {tr("Título no disponible","Title unavailable")}
    }
    fun action(block: suspend ()->Unit) {close();state.launch {state.socialAction {block()} } }
    PlayerMenuDialog(onDismissRequest=close) {
        Column(Modifier.consumeOpeningPress().widthIn(max=520.dp).background(Panel,RoundedCornerShape(24.dp)).padding(24.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Text(item.title,color=Color.White,fontSize=24.sp)
            FocusButton(tr("Ir a detalles","Go to details"),modifier=Modifier.focusRequester(first)) {action {state.open(item)} }
            FocusButton(tr("Elegir enlace","Play manually")) {action {prepare();if(!state.selectedUpcoming) state.chooseCurrentSources()} }
            FocusButton(tr("Desde el principio","Start from beginning")) {action {prepare();if(!state.selectedUpcoming) {if(item.type=="movie") state.playMovie(true) else state.playSeriesFromStart()} } }
            if(continuing) FocusButton(tr("Quitar","Remove"),destructive=true) {state.removeContinue(item);close()}
            else FocusButton(tr("Marcar como visto","Mark as watched")) {state.markWatched(item);close()}
            FocusButton(tr("Cerrar","Close"),onClick=close)
        }
    }
}

@Composable
internal fun EpisodeActionsDialog(state: AppState,item: MediaSummary,ep: EpisodeSummary,beforePlayback: ()->Unit = {},close: ()->Unit) {
    val first=remember {FocusRequester()}
    var reveal by remember(item.cloudId,ep.season,ep.episode) {mutableStateOf(false)}
    val hidden=state.hideEpisodeSpoilersRP20(item,ep) && !reveal
    LaunchedEffect(Unit) {withFrameNanos {};runCatching {first.requestFocus()} }
    fun play(block:suspend ()->Unit) {close();beforePlayback();state.launch {state.socialAction {state.stopRandomEpisodes();block()}}}
    PlayerMenuDialog(onDismissRequest=close,fullScreen=true) {
        BoxWithConstraints(Modifier.fillMaxSize().consumeOpeningPress().background(Color.Black)) {
            val wide=maxWidth>=700.dp
            Box(Modifier.fillMaxSize().blur(26.dp)) {
                coil3.compose.AsyncImage(if(hidden) item.backdrop ?: item.poster else ep.still ?: item.backdrop,null,Modifier.fillMaxSize(),contentScale=androidx.compose.ui.layout.ContentScale.Crop)
            }
            Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(Color(0xDC050505),Color(0xA0050505),Color(0xBB050505)))))
            val info:@Composable ()->Unit={
                Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(14.dp)) {
                    Text(item.title,color=Color(0xFFBBBBBB),fontSize=15.sp)
                    Text("S${ep.season} E${ep.episode}"+(if(!hidden && ep.rating>0) "  ·  ★ %.1f".format(ep.rating) else ""),color=Color.White,fontSize=14.sp)
                    Text(if(hidden) tr("No visto","Not watched") else ep.title,color=Color.White,fontSize=34.sp,lineHeight=39.sp,fontWeight=androidx.compose.ui.text.font.FontWeight.Bold)
                    Text(if(hidden) tr("Los detalles de este episodio están ocultos para evitar spoilers.","Episode details are hidden to avoid spoilers.") else ep.overview.ifBlank {tr("Sin sinopsis disponible.","Synopsis unavailable.")},color=Color(0xFFE2E2E2),fontSize=18.sp,lineHeight=25.sp)
                    if(ep.airDate.isNotBlank()) Text(ep.airDate,color=Color(0xFFBBBBBB),fontSize=13.sp)
                }
            }
            val actions:@Composable ()->Unit={
                Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(9.dp)) {
                    FocusButton(tr("Marcar como visto","Mark as watched"),modifier=Modifier.fillMaxWidth().focusRequester(first)) {state.markWatched(item,ep.season,ep.episode);close()}
                    FocusButton(tr("Marcar temporada como vista","Mark season as watched"),modifier=Modifier.fillMaxWidth()) {close();state.launch {state.socialAction {state.markSeasonWatched(item,ep.season)}}}
                    FocusButton(tr("▶  Reproducir","▶  Play"),modifier=Modifier.fillMaxWidth()) {play {state.playEpisode(ep)}}
                    FocusButton(tr("Elegir enlace","Play manually"),modifier=Modifier.fillMaxWidth()) {play {state.chooseEpisodeSources(ep)}}
                    FocusButton(tr("Desde el principio","Start from beginning"),modifier=Modifier.fillMaxWidth()) {play {state.playEpisode(ep,true)}}
                    FocusButton(tr("Añadir a cola","Add to queue"),modifier=Modifier.fillMaxWidth()) {state.enqueue(item,ep.season,ep.episode);close()}
                    if(hidden) FocusButton(tr("Mostrar detalles (spoilers)","Reveal details (spoilers)"),modifier=Modifier.fillMaxWidth()) {reveal=true}
                    FocusButton(tr("Cerrar","Close"),modifier=Modifier.fillMaxWidth(),onClick=close)
                }
            }
            if(wide) Row(Modifier.fillMaxSize().padding(horizontal=44.dp,vertical=30.dp),horizontalArrangement=Arrangement.spacedBy(42.dp),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                Box(Modifier.weight(1.2f).fillMaxHeight().verticalScroll(rememberScrollState()),contentAlignment=androidx.compose.ui.Alignment.CenterStart) {info()}
                Box(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),contentAlignment=androidx.compose.ui.Alignment.Center) {actions()}
            } else Column(Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(24.dp)) {info();actions()}
        }
    }
}
