package com.miflix.native2.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
    LaunchedEffect(Unit) {withFrameNanos {};runCatching {first.requestFocus()} }
    PlayerMenuDialog(onDismissRequest=close) {
        Column(Modifier.consumeOpeningPress().widthIn(max=510.dp).background(Panel,RoundedCornerShape(24.dp)).padding(24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text("S${ep.season} E${ep.episode}",color=Color.White,fontSize=24.sp)
            FocusButton(tr("Elegir enlace","Play manually"),modifier=Modifier.focusRequester(first)) {close();beforePlayback();state.launch {state.socialAction {state.chooseEpisodeSources(ep)} } }
            FocusButton(tr("Desde el principio","Play from beginning")) {close();beforePlayback();state.launch {state.socialAction {state.stopRandomEpisodes();state.playEpisode(ep,true)} } }
            FocusButton(tr("Marcar como visto","Mark as watched")) {state.markWatched(item,ep.season,ep.episode);close()}
            FocusButton(tr("Cerrar","Close"),onClick=close)
        }
    }
}
