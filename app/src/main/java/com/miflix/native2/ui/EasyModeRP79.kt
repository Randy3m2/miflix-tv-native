package com.miflix.native2.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.miflix.native2.model.MediaSummary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

/** Only these four destinations are shown; profile recovery is a deliberate logo hold. */
@Composable
internal fun EasyNavigationRP79(state:AppState,onMoveDown:()->Unit={}) {
    var exit by remember {mutableStateOf(false)}
    Row(Modifier.fillMaxWidth().heightIn(min=88.dp).background(Color.Black).padding(horizontal=24.dp,vertical=12.dp)
        .onPreviewKeyEvent {event ->
            if(event.key==Key.DirectionDown) {
                if(event.type==KeyEventType.KeyDown) runCatching {onMoveDown()}
                true
            } else false
        },verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
        FocusButton("BruniO",onLongClick={exit=true},horizontalPadding=14.dp) {}
        listOf(Screen.HOME to "Home",Screen.SEARCH to tr("🔍 Buscar","🔍 Search"),
            Screen.MY_LIST to tr("Mi lista","My List"),Screen.LIVE_TV to tr("En vivo","Live TV")).forEach {(screen,label) ->
            FocusButton(label,primary=state.screen==screen,horizontalPadding=18.dp) {state.screen=screen}
        }
    }
    if(exit) androidx.compose.ui.window.Dialog(onDismissRequest={exit=false}) {
        val stay=remember {androidx.compose.ui.focus.FocusRequester()}
        LaunchedEffect(Unit) {withFrameNanos {};runCatching {stay.requestFocus()}}
        Column(Modifier.width(500.dp).background(Color(0xFF141414),RoundedCornerShape(22.dp)).padding(28.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
            Text(tr("¿Salir del modo fácil?","Exit Easy mode?"),color=Color.White,fontSize=28.sp)
            Text(tr("Volverás a las opciones completas de este perfil.","The full options for this profile will return."),color=Muted,fontSize=19.sp)
            FocusButton(tr("Seguir en modo fácil","Stay in Easy mode"),Modifier.focusRequester(stay)) {exit=false}
            FocusButton(tr("Desactivar modo fácil","Turn off Easy mode")) {exit=false;state.launch {state.setEasyModeRP79(false)}}
        }
    }
}

/** Inline on the Live TV screen, dialog in the player. One shared catalog and navigation. */
@Composable
internal fun EasyBrowserFrameRP79(embedded:Boolean,close:()->Unit,content:@Composable ()->Unit) {
    if(embedded) Box(Modifier.fillMaxSize()) {content()}
    else PlayerMenuDialog(onDismissRequest=close,fullScreen=true,content=content)
}

@Composable
internal fun EasyScreenRP79(state:AppState,screen:Screen) {
    val owner="${state.localExtrasScope()}:${state.interfaceLanguage}"
    BackHandler(enabled=screen!=Screen.HOME) {
        state.screen=if(screen==Screen.DETAILS && state.detailsReturnScreen in listOf(Screen.HOME,Screen.SEARCH,Screen.MY_LIST)) state.detailsReturnScreen else Screen.HOME
    }
    AppShell(state,screen,{state.screen=it}) {
        when(screen) {
            Screen.HOME -> EasyHomeRP79(state,owner)
            Screen.SEARCH -> EasySearchRP79(state,owner)
            Screen.MY_LIST -> EasyListRP79(state,owner)
            Screen.LIVE_TV -> LiveChannelBrowserRP34(state,"nauta",select={request ->
                state.openLivePlayer(request.copy(returnScreen=Screen.LIVE_TV))
            },close={state.screen=Screen.HOME},embeddedRP79=true)
            Screen.DETAILS -> EasyDetailsRP79(state)
            else -> Unit
        }
    }
}

@Composable
private fun EasyHomeRP79(state:AppState,owner:String) {
    var loading by remember(owner) {mutableStateOf(true)}
    LaunchedEffect(owner) {try {state.loadEasyHomeRP79()} finally {loading=false}}
    LazyColumn(Modifier.fillMaxSize().padding(horizontal=30.dp),verticalArrangement=Arrangement.spacedBy(22.dp),contentPadding=PaddingValues(top=16.dp,bottom=32.dp)) {
        item {Text(tr("¿Qué quieres ver?","What would you like to watch?"),color=Color.White,fontSize=32.sp,fontWeight=FontWeight.Bold)}
        if(state.continueWatching.isNotEmpty()) item {
            EasyShelfRP79(tr("Seguir viendo","Continue Watching"),state.continueWatching.toList()) {item ->state.launch {state.easyResumeRP79(item)}}
        }
        item {EasyShelfRP79(tr("Películas","Movies"),state.movies.toList()) {item ->state.launch {state.open(item)}}}
        item {EasyShelfRP79(tr("Series","Series"),state.series.toList()) {item ->state.launch {state.open(item)}}}
        if(loading) item {Text(tr("Cargando…","Loading…"),color=Muted,fontSize=22.sp)}
        if(!loading && state.movies.isEmpty() && state.series.isEmpty()) item {
            FocusButton(tr("Volver a cargar","Try again")) {state.launch {state.loadEasyHomeRP79()}}
        }
    }
}

@Composable
private fun EasyShelfRP79(title:String,rows:List<MediaSummary>,onOpen:(MediaSummary)->Unit) {
    Column(verticalArrangement=Arrangement.spacedBy(14.dp)) {
        Text(title,color=Color.White,fontSize=26.sp,fontWeight=FontWeight.Bold)
        LazyRow(horizontalArrangement=Arrangement.spacedBy(18.dp),contentPadding=PaddingValues(4.dp)) {
            items(rows,key={it.cloudId}) {item ->EasyCardRP79(item,Modifier.width(180.dp)) {onOpen(item)}}
        }
    }
}

@Composable
private fun EasyCardRP79(item:MediaSummary,modifier:Modifier=Modifier,onOpen:()->Unit) {
    var focused by remember(item.cloudId) {mutableStateOf(false)}
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(if(focused) Color.White else Color(0xFF191919))
        .border(if(focused) 3.dp else 1.dp,if(focused) Color.White else Color(0xFF333333),RoundedCornerShape(16.dp))
        .onFocusChanged {focused=it.isFocused}.focusable().tvClick(onOpen).clickable(onClick=onOpen)) {
        AsyncImage(item.poster,null,Modifier.fillMaxWidth().height(220.dp).background(Color(0xFF222222)),contentScale=ContentScale.Crop)
        Text(item.title,color=if(focused) Color.Black else Color.White,fontSize=20.sp,lineHeight=26.sp,maxLines=2,
            overflow=TextOverflow.Ellipsis,modifier=Modifier.fillMaxWidth().heightIn(min=68.dp).padding(10.dp))
    }
}

@Composable
private fun EasySearchRP79(state:AppState,owner:String) {
    var query by rememberSaveable(owner) {mutableStateOf("")}
    var results by remember(owner) {mutableStateOf<List<MediaSummary>>(emptyList())}
    var loading by remember(owner) {mutableStateOf(false)}
    var message by remember(owner) {mutableStateOf("")}
    LaunchedEffect(owner,query) {
        results=emptyList();message="";loading=false
        if(query.trim().length<2) return@LaunchedEffect
        delay(450);loading=true
        try {
            results=state.searchFlexible(query).filter {it.type=="movie" || it.type=="series"}
        } catch(e: CancellationException) {
            throw e
        } catch(e: Exception) {
            message=tr("No se pudo buscar. Inténtalo de nuevo.","Search failed. Please try again.")
        } finally {loading=false}
    }
    Column(Modifier.fillMaxSize().padding(30.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
        Text(tr("Buscar película o serie","Search movies or series"),color=Color.White,fontSize=30.sp,fontWeight=FontWeight.Bold)
        NativeTextField(query,{query=it},tr("Escribe el nombre…","Type a title…"),modifier=Modifier.fillMaxWidth())
        if(loading) Text(tr("Buscando…","Searching…"),color=Muted,fontSize=22.sp)
        if(message.isNotBlank()) Text(message,color=Muted,fontSize=20.sp)
        if(!loading && query.trim().length>=2 && results.isEmpty() && message.isBlank()) Text(tr("Sin resultados","No results"),color=Muted,fontSize=22.sp)
        EasyGridRP79(results,Modifier.weight(1f)) {item ->state.launch {state.open(item)}}
    }
}

@Composable
private fun EasyListRP79(state:AppState,owner:String) {
    var limit by rememberSaveable(owner) {mutableIntStateOf(15)}
    val ids=state.favorites.toList()
    LaunchedEffect(owner,ids,limit) {state.myListLimit=limit;state.loadMyListPage("${state.session?.userId}:${state.activeProfile.id}",ids)}
    val cached=state.myListTitles.associateBy {it.cloudId}
    Column(Modifier.fillMaxSize().padding(30.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
        Text(tr("Mi lista","My List"),color=Color.White,fontSize=30.sp,fontWeight=FontWeight.Bold)
        EasyGridRP79(ids.take(limit).mapNotNull {cached[it]},Modifier.weight(1f)) {item ->state.launch {state.open(item)}}
        if(state.myListLoading) Text(tr("Cargando…","Loading…"),color=Muted,fontSize=22.sp)
        else if(ids.isEmpty()) Text(tr("Aquí aparecerán tus películas y series guardadas.","Your saved movies and series will appear here."),color=Muted,fontSize=22.sp)
        if(ids.size>limit) FocusButton(tr("Ver más","Show more")) {limit+=15}
    }
}

@Composable
private fun EasyGridRP79(rows:List<MediaSummary>,modifier:Modifier=Modifier,onOpen:(MediaSummary)->Unit) {
    LazyColumn(modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(18.dp),contentPadding=PaddingValues(4.dp)) {
        items(rows.chunked(4),key={it.first().cloudId}) {group ->
            Row(horizontalArrangement=Arrangement.spacedBy(18.dp)) {
                group.forEach {item ->EasyCardRP79(item,Modifier.weight(1f)) {onOpen(item)}}
                repeat(4-group.size) {Spacer(Modifier.weight(1f))}
            }
        }
    }
}

@Composable
private fun EasyDetailsRP79(state:AppState) {
    val item=state.selected ?: return
    val details=state.details
    LazyColumn(Modifier.fillMaxSize().padding(horizontal=32.dp),verticalArrangement=Arrangement.spacedBy(18.dp),contentPadding=PaddingValues(top=20.dp,bottom=32.dp)) {
        item {Text(item.title,color=Color.White,fontSize=32.sp,fontWeight=FontWeight.Bold)}
        if(details==null) item {Text(tr("Cargando…","Loading…"),color=Muted,fontSize=22.sp)}
        else {
            item {
                Row(horizontalArrangement=Arrangement.spacedBy(18.dp)) {
                    if(!state.selectedUpcoming) FocusButton(tr("Reproducir","Play")) {state.launch {state.easyResumeRP79(item)}}
                    FocusButton(if(item.cloudId in state.favorites) tr("Quitar de Mi lista","Remove from My List") else tr("Guardar en Mi lista","Save to My List")) {
                        state.toggleFavorite(item);state.launch {state.pushCloud()}
                    }
                }
            }
            if(item.type=="series") {
                item {
                    Row(horizontalArrangement=Arrangement.spacedBy(18.dp),verticalAlignment=Alignment.CenterVertically) {
                        if(state.currentSeason>1) FocusButton("‹") {state.launch {state.loadSeason(state.currentSeason-1)}}
                        Text(tr("Temporada ","Season ")+state.currentSeason,color=Color.White,fontSize=26.sp)
                        if(state.currentSeason<details.seasonCount) FocusButton("›") {state.launch {state.loadSeason(state.currentSeason+1)}}
                    }
                }
                items(state.episodes.toList(),key={"${it.season}:${it.episode}"}) {episode ->
                    FocusButton("${episode.episode}. ${episode.title}",modifier=Modifier.fillMaxWidth()) {state.launch {state.playEpisode(episode)}}
                }
            }
        }
    }
}
