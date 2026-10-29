package com.miflix.native2.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.miflix.native2.model.*
import com.miflix.native2.data.DiscoveryMood
import kotlinx.coroutines.*

/** Searches actual episode metadata. No generated episode numbers or fabricated timecodes. */
@Composable
internal fun SceneFinderRP151(state:AppState,close:()->Unit) {
    val scope=rememberCoroutineScope()
    var series by remember {mutableStateOf("Naruto Shippuden")}
    var clue by remember {mutableStateOf("Pain")}
    var season by remember {mutableIntStateOf(8)}
    var busy by remember {mutableStateOf(false)}
    var message by remember {mutableStateOf("")}
    var shows by remember {mutableStateOf<List<MediaSummary>>(emptyList())}
    var selected by remember {mutableStateOf<MediaSummary?>(null)}
    var results by remember {mutableStateOf<List<EpisodeSummary>>(emptyList())}
    var job by remember {mutableStateOf<Job?>(null)}
    DisposableEffect(Unit) {onDispose {job?.cancel()}}
    fun searchEpisodes(show:MediaSummary) {
        if(busy) return
        selected=show;results=emptyList();busy=true;message=""
        val wanted=DiscoveryMood.normalize(clue).split(" ").filter {it.length>=3}.distinct()
        val start=season
        job=scope.launch {
            try {
                val detail=state.tmdb.details(show)
                val found=mutableListOf<EpisodeSummary>()
                for(s in start..minOf(start+2,detail.seasonCount)) {
                    ensureActive()
                    found+=state.tmdb.season(show.id,s).filter {ep ->
                        val text=DiscoveryMood.normalize(ep.title+" "+ep.overview)
                        wanted.isNotEmpty() && wanted.all {text.contains(it)}
                    }
                }
                results=found.filter {it.airDate.isNotBlank() && it.airDate<=java.time.LocalDate.now().toString()}
                message=if(results.isEmpty()) tr("Sin coincidencias en estas 3 temporadas. Cambia la temporada o usa una pista más corta.","No matches in these 3 seasons. Change the season or use a shorter clue.") else tr("Coincidencias en título o sinopsis; confirma el episodio antes de reproducir. Sin minuto exacto verificado.","Title or synopsis matches; check the episode before playing. No verified exact timestamp.")
            } catch(e:CancellationException) {throw e} catch(_:Exception) {message=tr("No se pudieron consultar los episodios. Reintenta.","Could not load episodes. Retry.")} finally {busy=false}
        }
    }
    PlayerMenuDialog(onDismissRequest=close,fullScreen=true) {
        Column(Modifier.fillMaxSize().background(Bg).padding(28.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {Text("✦ Scene Finder",fontSize=28.sp,color=Color.White);FocusButton("×",onClick=close)}
            Text(tr("Encuentra episodios por sus títulos y sinopsis. No analiza el vídeo ni inventa minutos.","Find episodes by their titles and synopses. Does not analyze video or invent timestamps."),color=Muted,fontSize=13.sp)
            Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                BasicTextField(series,{if(!busy) {series=it.take(120);selected=null;shows=emptyList();results=emptyList()}},textStyle=androidx.compose.ui.text.TextStyle(color=Color.White,fontSize=18.sp),cursorBrush=SolidColor(Accent),modifier=Modifier.weight(1f).background(Panel,RoundedCornerShape(12.dp)).padding(14.dp))
                BasicTextField(clue,{if(!busy) clue=it.take(120)},textStyle=androidx.compose.ui.text.TextStyle(color=Color.White,fontSize=18.sp),cursorBrush=SolidColor(Accent),modifier=Modifier.weight(1f).background(Panel,RoundedCornerShape(12.dp)).padding(14.dp))
            }
            Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                FocusButton("−") {if(!busy) season=(season-1).coerceAtLeast(1)}
                Text(tr("Desde temporada ","From season ")+season,color=Color.White,modifier=Modifier.padding(top=12.dp))
                FocusButton("+") {if(!busy) season=(season+1).coerceAtMost(100)}
                FocusButton(if(busy) tr("Buscando…","Searching…") else tr("Buscar","Search")) {
                    if(!busy && series.isNotBlank() && clue.isNotBlank()) {
                        val chosen=selected
                        if(chosen!=null) searchEpisodes(chosen) else {
                            busy=true;message="";results=emptyList()
                            job=scope.launch {try {val candidates=state.tmdb.search(series).filter {it.type=="series"};shows=if(state.kidsMode) state.tmdb.kidsFilter(candidates) else candidates;if(shows.isEmpty()) message=tr("Serie no encontrada.","Series not found.")} catch(e:CancellationException) {throw e} catch(_:Exception) {message=tr("Error de búsqueda.","Search failed.")} finally {busy=false}}
                        }
                    }
                }
                if(busy) FocusButton(tr("Cancelar","Cancel")) {job?.cancel();busy=false}
            }
            if(message.isNotBlank()) Text(message,color=Accent,fontSize=13.sp)
            LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                if(selected==null) items(shows,key={it.cloudId}) {show ->FocusButton(show.title+" · "+show.year,modifier=Modifier.fillMaxWidth()) {searchEpisodes(show)}}
                items(results,key={"${it.season}:${it.episode}"}) {ep ->
                    Column(Modifier.fillMaxWidth().background(Panel,RoundedCornerShape(16.dp)).padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        Text("S${ep.season} E${ep.episode} · ${ep.title}",color=Color.White,fontSize=19.sp)
                        Text(ep.overview,color=Muted,fontSize=13.sp,maxLines=3)
                        FocusButton(tr("Ver episodio","Watch episode")) {val show=selected;if(show!=null) {close();state.launch {state.open(show);if(state.selected?.cloudId==show.cloudId) state.playEpisode(ep)}}}
                    }
                }
            }
        }
    }
}
