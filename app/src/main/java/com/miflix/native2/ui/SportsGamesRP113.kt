package com.miflix.native2.ui

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.miflix.native2.data.*
import com.miflix.native2.model.*
import kotlinx.coroutines.*

/** All provider sports categories, loaded on demand. No polling or backend writes. */
@Composable
internal fun SportsGamesRP113(state:AppState,request:PlayerRequest,close:()->Unit) {
    val repo=remember {LiveRepository()}
    val scope=rememberCoroutineScope()
    val owner=state.session?.userId to state.activeProfile.id
    val manifest=state.calendarLiveManifest("sports")
    var categories by remember {mutableStateOf<List<LiveCategory>>(emptyList())}
    var category by remember {mutableStateOf<LiveCategory?>(null)}
    var rows by remember {mutableStateOf<List<LiveChannel>>(emptyList())}
    var loading by remember {mutableStateOf(true)}
    var resolving by remember {mutableStateOf(false)}
    var message by remember {mutableStateOf("")}
    var retry by remember {mutableIntStateOf(0)}
    var skip by remember {mutableIntStateOf(0)}
    var more by remember {mutableStateOf(false)}
    LaunchedEffect(manifest,retry) {
        try {categories=repo.categories(manifest);message=""}
        catch(e:CancellationException) {throw e}
        catch(_:Exception) {message=tr("No se pudieron cargar los deportes","Could not load sports")}
        finally {loading=false}
    }
    LaunchedEffect(category,skip,retry) {
        val selected=category ?: return@LaunchedEffect
        loading=true;message=""
        if(skip==0) rows=emptyList()
        try {
            val page=repo.channels(manifest,selected,skip)
            rows=(rows+page).distinctBy {it.id}
            more=selected.paginated && page.isNotEmpty()
        } catch(e:CancellationException) {throw e}
        catch(_:Exception) {message=tr("No se pudieron cargar los juegos","Could not load games")}
        finally {loading=false}
    }
    PlayerSideSheet(tr("Juegos · Todos los deportes","Games · All sports"),close,category?.id) {
        if(category==null) {
            items(categories,key={"category:${it.type}:${it.id}"}) {entry ->
                FocusButton(entry.name,Modifier.fillMaxWidth()) {skip=0;category=entry}
            }
        } else {
            item {FocusButton(tr("‹ Todos los deportes","‹ All sports"),Modifier.fillMaxWidth()) {category=null;rows=emptyList();skip=0}}
            items(rows,key={"game:${it.id}"}) {game ->
                val playing=request.liveProvider=="sports" && request.liveChannelId==game.id
                SportsGameCardRP114(game,playing,resolving) {
                    if(!resolving && state.partyRole!=PartyRole.GUEST) scope.launch {
                        resolving=true;message=""
                        val selected=category ?: run {resolving=false;return@launch}
                        try {
                            val links=repo.streams(manifest,game)
                            if(owner!=(state.session?.userId to state.activeProfile.id) || state.partyRole==PartyRole.GUEST || state.playerRequest?.playbackId!=request.playbackId) return@launch
                            val first=links.firstOrNull() ?: error("No sources")
                            val next=PlayerRequest(MediaSummary(game.id.hashCode(),"live",game.name,game.description,null,game.poster,0.0,"LIVE"),first,0,0,0L,
                                live=true,liveChannelId=game.id,liveProvider="sports",liveType=game.type,
                                sportsRecordingRP72=SportsRecordingPolicyRP72.recording(selected.name,selected.id,selected.genre,game.name,game.description))
                            LiveChannelNavigationRP40.save(state,LiveBrowseSnapshotRP40(BrowserCategoryRP34("sports",selected,"Sports"),rows.map {BrowserChannelRP34("sports",it)}))
                            state.openLivePlayer(state.sportsRequestRP71(next,links));close()
                        } catch(e:CancellationException) {throw e}
                        catch(_:Exception) {message=tr("No se pudo abrir. Elige otro juego.","Could not open. Choose another game.")}
                        finally {resolving=false}
                    }
                }
            }
            if(more && !loading) item {FocusButton(tr("Cargar más","Load more"),Modifier.fillMaxWidth()) {skip=rows.size}}
        }
        if(loading || resolving) item {Text(tr("Cargando…","Loading…"),color=Color.White)}
        if(message.isNotBlank()) item {
            Text(message,color=Color.White)
            FocusButton(tr("Reintentar","Retry")) {retry++}
        }
    }
}

/** Fixed-size artwork keeps remote navigation stable while images arrive. */
@Composable
private fun SportsGameCardRP114(game:LiveChannel,playing:Boolean,busy:Boolean,onClick:()->Unit) {
    var focused by remember {mutableStateOf(false)}
    val active=actionFocusVisibleRP64(focused)
    val shape=RoundedCornerShape(16.dp)
    val context=LocalContext.current
    val artwork=remember(context,game.poster) {
        ImageRequest.Builder(context).data(game.poster).size(480,270).build()
    }
    Row(Modifier.fillMaxWidth().height(116.dp).clip(shape)
        .background(GlassActionBrushRP108(active,playing))
        .border(if(active) 2.dp else 1.dp,Color.White.copy(alpha=if(active) .85f else if(playing) .42f else .14f),shape)
        .onFocusChanged {focused=it.isFocused}.focusable()
        .tvClick {if(!busy) onClick()}.clickable {if(!busy) onClick()}
        .padding(10.dp),verticalAlignment=Alignment.CenterVertically,
        horizontalArrangement=Arrangement.spacedBy(14.dp)) {
        Box(Modifier.width(150.dp).height(86.dp).clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha=.07f)),contentAlignment=Alignment.Center) {
            // Title is also shown beside the image if the provider has no artwork.
            Text("▶",color=Color.White.copy(alpha=.5f),fontSize=24.sp)
            AsyncImage(artwork,null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
        }
        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(7.dp)) {
            Text(game.name,color=Color.White,fontSize=16.sp,lineHeight=21.sp,
                fontWeight=if(active || playing) FontWeight.Bold else FontWeight.SemiBold,
                maxLines=3,overflow=TextOverflow.Ellipsis)
            if(playing) Text(tr("▶ VIENDO AHORA","▶ NOW PLAYING"),color=Color.White,
                fontSize=10.sp,fontWeight=FontWeight.Bold)
            else if(active) Text(tr("OK · Reproducir","OK · Play"),color=Color.White,
                fontSize=11.sp,fontWeight=FontWeight.Bold)
        }
    }
}
