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

/** Basic navigation only; profile recovery requires the fifteen-second logo gesture. */
@Composable
internal fun EasyNavigationRP79(state:AppState,onMoveDown:()->Unit={}) {
    var exit by remember(state.localExtrasScope()) {mutableStateOf(false)}
    var exitReady by remember(state.localExtrasScope()) {mutableStateOf(false)}
    var remaining by remember(state.localExtrasScope()) {mutableIntStateOf(15)}
    LaunchedEffect(exit,state.appForeground) {
        if(!state.appForeground) {exit=false;exitReady=false;return@LaunchedEffect}
        if(exit && !exitReady) {
            val deadline=android.os.SystemClock.elapsedRealtime()+15_000L
            while(exit && !exitReady) {
                remaining=((deadline-android.os.SystemClock.elapsedRealtime()+999L)/1000L).coerceIn(0L,15L).toInt()
                if(remaining==0) exitReady=true else kotlinx.coroutines.delay(100)
            }
        }
    }
    Row(Modifier.fillMaxWidth().heightIn(min=88.dp).background(Color.Black).padding(horizontal=24.dp,vertical=12.dp)
        .onPreviewKeyEvent {event ->
            if(event.key==Key.DirectionDown) {
                if(event.type==KeyEventType.KeyDown) runCatching {onMoveDown()}
                true
            } else false
        },verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
        EasyExitLogoRP80(state,onCountdown={exitReady=false;remaining=15;exit=true}) {exitReady=true;remaining=0;exit=true}
        listOf(Screen.HOME to "Home",Screen.SEARCH to tr("🔍 Buscar","🔍 Search"),
            Screen.MY_LIST to tr("Mi lista","My List")).forEach {(screen,label) ->
            FocusButton(label,primary=state.screen==screen,horizontalPadding=16.dp) {state.screen=screen}
        }
        FocusButton("Live TV",primary=state.screen==Screen.LIVE_TV && !state.easyMlbRP80,horizontalPadding=16.dp) {
            state.easyMlbRP80=false;state.screen=Screen.LIVE_TV
        }
        FocusButton("MLB",primary=state.screen==Screen.LIVE_TV && state.easyMlbRP80,horizontalPadding=16.dp) {
            state.easyMlbRP80=true;state.screen=Screen.LIVE_TV
        }
    }
    if(exit) androidx.compose.ui.window.Dialog(onDismissRequest={exit=false}) {
        val stay=remember {androidx.compose.ui.focus.FocusRequester()}
        LaunchedEffect(Unit) {withFrameNanos {};runCatching {stay.requestFocus()}}
        Column(Modifier.width(500.dp).background(Color(0xFF141414),RoundedCornerShape(22.dp)).padding(28.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
            Text(tr("¿Salir del modo fácil?","Exit Easy mode?"),color=Color.White,fontSize=28.sp)
            Text(tr("Volverás a las opciones completas de este perfil.","The full options for this profile will return."),color=Muted,fontSize=19.sp)
            FocusButton(tr("Seguir en modo fácil","Stay in Easy mode"),Modifier.focusRequester(stay)) {exit=false}
            if(exitReady) FocusButton(tr("Desactivar modo fácil","Turn off Easy mode")) {exit=false;state.launch {state.setEasyModeRP79(false)}}
            else Text(tr("Podrás salir en ","You can exit in ")+"$remaining s",color=Color.White,fontSize=23.sp)
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
    if(screen==Screen.SEARCH) {
        CompositionLocalProvider(LocalEasySearchRP80 provides true) {
            SearchScreen(state) {state.screen=Screen.HOME}
        }
        return
    }
    AppShell(state,screen,{state.screen=it}) {
        when(screen) {
            Screen.HOME -> EasyHomeRP79(state,owner)
            Screen.MY_LIST -> EasyListRP79(state,owner)
            Screen.LIVE_TV -> if(state.easyMlbRP80) EasyMlbRP80(state,Modifier.fillMaxSize().padding(horizontal=24.dp,vertical=12.dp))
            else LiveAutoStartRP83(state) { LiveChannelBrowserRP34(state,"nauta",select={request ->
                state.openLivePlayer(request.copy(returnScreen=Screen.LIVE_TV))
            },close={state.screen=Screen.HOME},embeddedRP79=true) }
            Screen.DETAILS -> EasyDetailsRP79(state)
            else -> Unit
        }
    }
}

@Composable
private fun EasyHomeRP79(state:AppState,owner:String) {
    var loading by remember(owner) {mutableStateOf(true)}
    LaunchedEffect(owner) {try {state.loadEasyHomeRP79()} finally {loading=false}}
    LaunchedEffect(owner) {state.loadEasyRowsRP80()}
    LazyColumn(Modifier.fillMaxSize().padding(horizontal=30.dp),verticalArrangement=Arrangement.spacedBy(22.dp),contentPadding=PaddingValues(top=16.dp,bottom=32.dp)) {
        item(key="easy-heading") {Text(tr("¿Qué quieres ver?","What would you like to watch?"),color=Color.White,fontSize=32.sp,fontWeight=FontWeight.Bold)}
        if(state.continueWatching.isNotEmpty()) item(key="easy-continue") {
            EasyShelfRP79(tr("Seguir viendo","Continue Watching"),state.continueWatching.toList()) {item ->state.launch {state.open(item)}}
        }
        item(key="easy-movies") {EasyShelfRP79(tr("Películas populares","Popular movies"),state.movies.toList()) {item ->state.launch {state.open(item)}}}
        item(key="easy-series") {EasyShelfRP79(tr("Series populares","Popular series"),state.series.toList()) {item ->state.launch {state.open(item)}}}
        if(state.easyRowsMatchRP80) EasyHomeRowsRP80.rows.forEach {row ->
            val titles=state.easyRowsRP80[row.id].orEmpty()
            if(titles.isNotEmpty()) item(key="easy-extra:${row.id}") {
                EasyShelfRP79(tr(row.spanish,row.english),titles) {item ->state.launch {state.open(item)}}
            }
        }
        if(loading || state.easyRowsLoadingRP80) item(key="easy-loading") {Text(tr("Preparando más opciones para ti…","Preparing more choices for you…"),color=Muted,fontSize=22.sp)}
        if(state.easyRowsMatchRP80 && !state.easyRowsLoadingRP80 && state.easyRowsMessageRP80.isNotBlank()) item(key="easy-retry-rows") {
            Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Text(state.easyRowsMessageRP80,color=Muted,fontSize=20.sp)
                FocusButton(tr("Cargar las filas pendientes","Load remaining rows")) {state.launch {state.loadEasyRowsRP80(retry=true)}}
            }
        }
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
    val details=state.details?.takeIf {it.summary.cloudId==item.cloudId}
    val summary=details?.summary ?: item
    val saved=state.progress[item.cloudId]
    LazyColumn(Modifier.fillMaxSize().padding(horizontal=32.dp),verticalArrangement=Arrangement.spacedBy(18.dp),contentPadding=PaddingValues(top=20.dp,bottom=32.dp)) {
        item(key="easy-artwork:${item.cloudId}") {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(28.dp),verticalAlignment=Alignment.Top) {
                AsyncImage(
                    model=summary.poster ?: item.poster,
                    contentDescription=tr("Carátula de ","Poster for ")+item.title,
                    modifier=Modifier.width(180.dp).height(270.dp).clip(RoundedCornerShape(18.dp)).background(Color(0xFF222222)),
                    contentScale=ContentScale.Fit
                )
                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(16.dp)) {
                    Text(summary.title,color=Color.White,fontSize=32.sp,lineHeight=38.sp,fontWeight=FontWeight.Bold)
                    val metadata=listOfNotNull(summary.year.takeIf {it.isNotBlank()},details?.runtimeText?.takeIf {it.isNotBlank()}).joinToString(" · ")
                    if(metadata.isNotBlank()) Text(metadata,color=Muted,fontSize=21.sp)
                    Text(summary.overview.ifBlank {item.overview}.ifBlank {tr("Sinopsis no disponible.","Synopsis unavailable.")},
                        color=Color(0xFFE4E4E4),fontSize=23.sp,lineHeight=32.sp)
                }
            }
        }
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
                if(saved!=null && saved.episode>0) item(key="easy-resume-position") {
                    Text(tr("Te quedaste en el episodio ","You left off at episode ")+saved.episode+" · "+tr("Temporada ","Season ")+saved.season,
                        color=Color.White,fontSize=26.sp,lineHeight=32.sp,fontWeight=FontWeight.SemiBold)
                }
                item {
                    Row(horizontalArrangement=Arrangement.spacedBy(18.dp),verticalAlignment=Alignment.CenterVertically) {
                        if(state.currentSeason>1) FocusButton("‹") {state.launch {state.loadSeason(state.currentSeason-1)}}
                        Text(tr("Temporada ","Season ")+state.currentSeason,color=Color.White,fontSize=26.sp)
                        if(state.currentSeason<details.seasonCount) FocusButton("›") {state.launch {state.loadSeason(state.currentSeason+1)}}
                    }
                }
                items(state.episodes.toList(),key={"${it.season}:${it.episode}"}) {episode ->
                    FocusButton((if(state.episodeWatched(item,episode)) "✓ " else "")+"${episode.episode}. ${episode.title}",modifier=Modifier.fillMaxWidth().heightIn(min=64.dp)) {state.launch {state.playEpisode(episode)}}
                }
            }
        }
    }
}
