package com.miflix.native2.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.miflix.native2.R
import com.miflix.native2.data.*
import com.miflix.native2.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

private data class HubEventRP41(val provider:String,val channel:LiveChannel,val category:LiveCategory) {
    val key get()=provider+":"+channel.type+":"+channel.id
}
private val hubCatalogRP41=PlaybackLinkCache<String,List<LiveCategory>>(4) {System.currentTimeMillis()}

@Composable
internal fun LiveHubRP41(state:AppState) {
    val context=LocalContext.current
    val owner="${state.session?.userId ?: "local"}:${state.activeProfile.id}"
    var section by remember(owner) {mutableStateOf("hub")}
    var busy by remember {mutableStateOf(false)}
    var error by remember {mutableStateOf("")}
    val scope=rememberCoroutineScope()
    var opening by remember {mutableStateOf<Job?>(null)}
    var configuring by remember {mutableStateOf(false)}
    var revision by remember {mutableIntStateOf(0)}
    val first=remember {FocusRequester()}
    fun back() {opening?.cancel();busy=false;error="";section="hub"}
    BackHandler {if(section!="hub" || busy) back() else state.screen=Screen.HOME}
    LaunchedEffect(section) {if(section=="hub") {withFrameNanos {};runCatching {first.requestFocus()}}}
    Column(Modifier.fillMaxSize().background(Color.Black).padding(horizontal=28.dp,vertical=18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)) {
            FocusButton("‹",horizontalPadding=14.dp) {if(section=="hub") state.screen=Screen.HOME else back()}
            Text(if(section=="hub") "BRUNIO · LIVE" else when(section) {"basketball"->"NBA · Basketball";"baseball"->"MLB · Baseball";"football"->"NFL · American Football";"soccer"->tr("Fútbol","Football");"f1"->"F1";"lidom"->"LIDOM";else->"Others"},color=Color.White,fontSize=24.sp,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f))
            if(section=="others") FocusButton(tr("Configurar deportes","Sports settings")) {configuring=true}
        }
        if(busy) Text(tr("Abriendo canal…","Opening channel…"),color=Muted)
        if(error.isNotBlank()) Text(error,color=Color.White,fontSize=12.sp)
        if(section=="hub") {
            val board=ImageBitmap.imageResource(R.drawable.live_hub_board_rp41)
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val gridWidth=minOf(maxWidth,maxHeight*1.54f)
            Column(Modifier.width(gridWidth).fillMaxHeight().align(Alignment.Center),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                repeat(3) {row ->
                    Row(Modifier.weight(1f).fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(14.dp)) {
                        repeat(3) {col ->
                            val index=row*3+col;val key=liveHubKeysRP41[index]
                            HubTileRP41(board,index,key,Modifier.weight(1f).fillMaxHeight().then(if(index==0) Modifier.focusRequester(first) else Modifier)) {
                                if(!busy) {
                                    error=""
                                    when(key) {
                                        "scores"->state.showLiveScoresRP11=true
                                        "tv"->{busy=true;opening=scope.launch {
                                            try {LiveHubPlaybackRP41.startTV(context,state)}
                                            catch(e:CancellationException) {throw e}
                                            catch(_:Exception) {error=tr("No se pudo abrir TV. Vuelve a intentarlo.","Could not open TV. Try again.")}
                                            finally {busy=false}
                                        }}
                                        else->section=key
                                    }
                                }
                            }
                        }
                    }
                }
            }
            }
        } else if(section=="lidom") HubLidomRP41(state,Modifier.weight(1f))
        else key(section,revision) {HubSportsRP41(state,section,Modifier.weight(1f))}
    }
    if(configuring) {
        var manifest by remember {mutableStateOf(state.calendarLiveManifest("sports"))}
        PlayerMenuDialog(onDismissRequest={configuring=false}) {
            Column(Modifier.widthIn(max=600.dp).background(Panel,RoundedCornerShape(18.dp)).padding(24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Text(tr("Fuente de deportes","Sports source"),color=Color.White,fontSize=22.sp)
                NativeTextField(manifest,{manifest=it},"https://…/manifest.json",modifier=Modifier.fillMaxWidth())
                FocusButton(tr("Guardar","Save")) {
                    if(manifest.startsWith("https://") && manifest.endsWith("/manifest.json")) {
                        context.getSharedPreferences("miflix_live",android.content.Context.MODE_PRIVATE).edit().putString("sports_manifest",manifest.trim()).apply()
                        revision++;configuring=false
                    }
                }
                FocusButton(tr("Cerrar","Close")) {configuring=false}
            }
        }
    }
}

@Composable
private fun HubTileRP41(board:ImageBitmap,index:Int,label:String,modifier:Modifier,click:()->Unit) {
    var focused by remember {mutableStateOf(false)}
    val scale by animateFloatAsState(if(focused) 1.025f else 1f,label="Live tile focus")
    val xs=listOf(40,534,1029);val ys=listOf(24,355,672)
    Box(modifier.graphicsLayer {scaleX=scale;scaleY=scale;shadowElevation=if(focused) 10.dp.toPx() else 0f;shape=RoundedCornerShape(13.dp)}
        .clip(RoundedCornerShape(13.dp)).border(if(focused) 2.dp else 1.dp,if(focused) Color.White else Color(0xFF263245),RoundedCornerShape(13.dp))
        .semantics {contentDescription=label}.onFocusChanged {focused=it.isFocused}.focusable().tvClick(click).clickable(onClick=click)) {
        Canvas(Modifier.fillMaxSize()) {
            drawImage(board,srcOffset=IntOffset(xs[index%3],ys[index/3]),srcSize=IntSize(467,if(index/3==2) 292 else 308),dstSize=IntSize(size.width.toInt(),size.height.toInt()))
            if(focused) drawRect(Color.White.copy(alpha=.07f))
        }
    }
}

@Composable
private fun HubSportsRP41(state:AppState,section:String,modifier:Modifier) {
    val repo=remember {LiveRepository()};val scope=rememberCoroutineScope()
    val manifest=state.calendarLiveManifest("sports")
    var categories by remember {mutableStateOf<List<LiveCategory>>(emptyList())}
    var selected by remember {mutableStateOf<LiveCategory?>(null)}
    var events by remember {mutableStateOf<List<HubEventRP41>>(emptyList())}
    var loading by remember {mutableStateOf(true)};var resolving by remember {mutableStateOf(false)}
    var message by remember {mutableStateOf("")};var retry by remember {mutableIntStateOf(0)}
    var offset by remember {mutableIntStateOf(0)};var more by remember {mutableStateOf(false)}
    var generation by remember {mutableIntStateOf(0)}
    var web by remember {mutableStateOf<String?>(null)}
    val offsetsRP41=remember {mutableMapOf<LiveCategory,Int>()}
    val pendingRP41=remember {mutableSetOf<LiveCategory>()}
    val permitsRP41=remember {Semaphore(3)}
    LaunchedEffect(manifest,retry) {
        loading=true;message=""
        try {
            val all=hubCatalogRP41.get(manifest) ?: repo.categories(manifest).also {hubCatalogRP41.put(manifest,it)}
            categories=all.filter {liveSportGroupRP41(it)==section && (section!="others" || !liveAggregateRP41(it))}
            selected=categories.firstOrNull()
            if(categories.isEmpty()) message=tr("No hay categorías publicadas para este deporte.","No categories published for this sport.")
        } catch(e:CancellationException) {throw e} catch(_:Exception) {message=tr("La fuente de deportes no respondió.","Sports source did not respond.")}
        finally {loading=false}
    }
    LaunchedEffect(selected) {
        generation++;val c=selected ?: return@LaunchedEffect
        loading=true;events=emptyList();message="";more=false;offset=0
        try {
            offsetsRP41.clear();pendingRP41.clear()
            val requested=if(section=="others") listOf(c) else categories.distinctBy {it.type+":"+it.id}
            val batches=coroutineScope {requested.map {cat -> async {
                val rows=permitsRP41.withPermit {repo.channels(manifest,cat)}
                cat to rows
            }}.awaitAll()}
            events=batches.flatMap {(cat,rows) ->
                offsetsRP41[cat]=rows.size;if(cat.paginated && rows.isNotEmpty()) pendingRP41.add(cat)
                rows.map {HubEventRP41("sports",it,cat)}
            }.distinctBy {it.key}
            offset=batches.firstOrNull()?.second?.size ?: 0;more=pendingRP41.isNotEmpty()
        } catch(e:CancellationException) {throw e} catch(_:Exception) {message=tr("No se pudieron cargar los eventos.","Could not load events.")}
        finally {loading=false}
    }
    fun tune(event:HubEventRP41) {
        if(resolving) return
        if(state.partyRole==PartyRole.GUEST) {message=tr("El anfitrión elige el contenido.","The host chooses the content.");return}
        val stamp=generation;val profile=state.session?.userId to state.activeProfile.id
        scope.launch {
            resolving=true;message=""
            try {
                val stream=repo.streams(manifest,event.channel).firstOrNull() ?: error("No links")
                if(stamp!=generation || profile!=(state.session?.userId to state.activeProfile.id) || state.screen!=Screen.LIVE_TV || state.partyRole==PartyRole.GUEST) return@launch
                val c=event.channel
                LiveChannelNavigationRP40.save(state,LiveBrowseSnapshotRP40(BrowserCategoryRP34("sports",event.category,"Sports"),events.filter {it.category==event.category}.map {BrowserChannelRP34(it.provider,it.channel)},offset=offsetsRP41[event.category] ?: offset,more=event.category in pendingRP41))
                state.openLivePlayer(PlayerRequest(MediaSummary(c.id.hashCode(),"live",c.name,c.description,null,c.poster,0.0,"LIVE"),stream,0,0,0L,live=true,liveChannelId=c.id,liveProvider="sports",liveType=c.type,returnScreen=Screen.LIVE_TV))
            } catch(e:CancellationException) {throw e} catch(_:Exception) {message=tr("Este evento no devolvió un enlace reproducible.","This event returned no playable link.")}
            finally {resolving=false}
        }
    }
    Column(modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        // Only Others has category tabs. Dedicated sports present event cards only.
        if(section=="others" && categories.isNotEmpty()) LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp),contentPadding=PaddingValues(4.dp)) {
            items(categories,key={it.type+":"+it.id+":"+it.genre}) {c ->FocusButton(c.name,primary=c==selected) {selected=c}}
        }
        if(loading || resolving) Text(if(resolving) tr("Abriendo evento…","Opening event…") else tr("Cargando eventos…","Loading events…"),color=Muted)
        if(message.isNotBlank()) {Text(message,color=Muted);FocusButton(tr("Reintentar","Retry")) {retry++}}
        LazyVerticalGrid(GridCells.Adaptive(240.dp),Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(14.dp),verticalArrangement=Arrangement.spacedBy(14.dp),contentPadding=PaddingValues(6.dp,6.dp,6.dp,30.dp)) {
            if(section=="others") {
                item(key="tnt-web") {HubWebCardRP41("TNT Sports","tnt",{web="https://sportsbite.org/247/tnt1uk"})}
                item(key="tbs-web") {HubWebCardRP41("TBS Sports","tbs",{web="https://sportsbite.org/t247/tbs"})}
            }
            items(events,key={it.key}) {event ->HubEventCardRP41(event.channel.name,event.channel.poster,event.channel.description) {tune(event)}}
            if(!loading && events.isEmpty() && message.isBlank()) item(span={GridItemSpan(maxLineSpan)}) {Text(tr("No hay eventos publicados ahora.","No events published right now."),color=Muted)}
            if(more && !loading) item(span={GridItemSpan(maxLineSpan)}) {FocusButton(tr("Cargar más","Load more")) {
                val c=selected
                if(c!=null) scope.launch {
                    val stamp=generation;loading=true
                    try {
                        val batches=coroutineScope {pendingRP41.toList().map {cat ->async {
                            cat to permitsRP41.withPermit {repo.channels(manifest,cat,offsetsRP41[cat] ?: 0)}
                        }}.awaitAll()}
                        if(stamp==generation) {
                            val oldKeys=events.map {it.key}.toSet()
                            for((cat,rows) in batches) {
                                offsetsRP41[cat]=(offsetsRP41[cat] ?: 0)+rows.size
                                if(rows.isEmpty() || rows.all {"sports:${it.type}:${it.id}" in oldKeys}) pendingRP41.remove(cat)
                            }
                            events=(events+batches.flatMap {(cat,rows) ->rows.map {HubEventRP41("sports",it,cat)}}).distinctBy {it.key}
                            offset=offsetsRP41[c] ?: offset;more=pendingRP41.isNotEmpty()
                        }
                    } catch(e:CancellationException) {throw e} catch(_:Exception) {message=tr("No se pudieron cargar más eventos.","Could not load more events.")}
                    finally {if(stamp==generation) loading=false}
                }
            }}
        }
    }
    web?.let {SportsWebRP41(it) {web=null}}
}

@Composable
private fun HubEventCardRP41(title:String,poster:String?,subtitle:String,click:()->Unit) {
    var focused by remember(title) {mutableStateOf(false)}
    Column(Modifier.fillMaxWidth().height(194.dp).clip(RoundedCornerShape(13.dp)).background(Color(0xFF151515))
        .border(if(focused) 2.dp else 1.dp,if(focused) Color.White else PanelBorder,RoundedCornerShape(13.dp))
        .onFocusChanged {focused=it.isFocused}.focusable().tvClick(click).clickable(onClick=click)) {
        Box(Modifier.fillMaxWidth().weight(1f).background(Brush.verticalGradient(listOf(Color(0xFF17283C),Color.Black))),contentAlignment=Alignment.Center) {
            if(!poster.isNullOrBlank()) AsyncImage(poster,null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
            else Text(title,color=Color.White,fontSize=22.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(14.dp),maxLines=2)
        }
        Text(title,color=Color.White,fontSize=14.sp,fontWeight=FontWeight.Bold,maxLines=2,overflow=TextOverflow.Ellipsis,modifier=Modifier.padding(horizontal=12.dp,vertical=6.dp))
        if(subtitle.isNotBlank()) Text(subtitle,color=Muted,fontSize=11.sp,maxLines=1,overflow=TextOverflow.Ellipsis,modifier=Modifier.padding(start=12.dp,end=12.dp,bottom=8.dp))
    }
}

@Composable
private fun HubLidomRP41(state:AppState,modifier:Modifier) {
    val context=LocalContext.current;val scope=rememberCoroutineScope()
    val owner="${state.session?.userId ?: "local"}:${state.activeProfile.id}"
    val repo=remember(owner) {LidomRepository(context,owner)}
    var games by remember {mutableStateOf<List<LidomGame>>(emptyList())};var loading by remember {mutableStateOf(true)}
    var message by remember {mutableStateOf("")};var retry by remember {mutableIntStateOf(0)}
    var video by remember {mutableStateOf<String?>(null)}
    LaunchedEffect(retry) {loading=true;try {games=repo.games(LidomRepository.today())} catch(e:CancellationException) {throw e} catch(_:Exception) {message=tr("No se pudo cargar el calendario.","Could not load schedule.")} finally {loading=false}}
    Column(modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        if(loading) Text(tr("Cargando…","Loading…"),color=Muted)
        if(message.isNotBlank()) Text(message,color=Muted)
        Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            FocusButton(tr("Actualizar","Refresh")) {message="";retry++}
            FocusButton(tr("Configurar fuentes","Source settings")) {state.lidomConfigure=true;state.screen=Screen.LIDOM}
        }
        LazyVerticalGrid(GridCells.Adaptive(260.dp),Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(14.dp),verticalArrangement=Arrangement.spacedBy(14.dp),contentPadding=PaddingValues(bottom=30.dp)) {
            items(games,key={it.id}) {game ->
                var focused by remember {mutableStateOf(false)}
                val play={
                    if(!loading && state.partyRole!=PartyRole.GUEST) scope.launch {
                        loading=true;message=""
                        try {
                            val sources=repo.sources(game.home.id).sortedBy {if(it.id==repo.last(game.home.id)) 0 else 1}
                            var opened=false
                            for(source in sources) {
                                if(owner!="${state.session?.userId ?: "local"}:${state.activeProfile.id}" || state.screen!=Screen.LIVE_TV) break
                                if(source.kind=="YOUTUBE") {
                                    val id=LidomRepository.youtubeId(source.url)
                                    if(id!=null) {video=id;opened=true;break} // Never label an arbitrary old channel upload as this game.
                                } else {
                                    val stream=try {repo.resolve(source,state.calendarLiveManifest("nauta")).firstOrNull()} catch(e:CancellationException) {throw e} catch(_:Exception) {null}
                                    if(stream!=null && owner=="${state.session?.userId ?: "local"}:${state.activeProfile.id}" && state.screen==Screen.LIVE_TV) {
                                        val title=game.away.short+" vs "+game.home.short
                                        state.openLivePlayer(PlayerRequest(MediaSummary(game.id,"live",title,game.status,null,null,0.0,"LIDOM"),stream,0,0,0L,live=true,liveChannelId="lidom:${game.home.id}:${source.id}",liveProvider="lidom",liveType="tv",lidomTeam=game.home.id,lidomSource=source.id,returnScreen=Screen.LIVE_TV))
                                        opened=true;break
                                    }
                                }
                            }
                            if(!opened) message=tr("No hay una fuente directa disponible para este equipo. Revisa Configurar fuentes.","No direct source available for this team. Check Source settings.")
                        } finally {loading=false}
                    };Unit
                }
                Column(Modifier.fillMaxWidth().height(194.dp).clip(RoundedCornerShape(14.dp)).background(Panel).border(if(focused) 2.dp else 1.dp,if(focused) Color.White else PanelBorder,RoundedCornerShape(14.dp))
                    .onFocusChanged {focused=it.isFocused}.focusable().tvClick(play).clickable(onClick=play).padding(14.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.weight(1f),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(18.dp)) {
                        for(team in listOf(game.away,game.home)) {
                            val id=context.resources.getIdentifier(team.logo,"drawable",context.packageName)
                            if(id!=0) Image(painterResource(id),team.name,Modifier.size(64.dp))
                        }
                    }
                    Text(game.away.short+" vs "+game.home.short,color=Color.White,fontWeight=FontWeight.Bold)
                    Text(game.time+" · "+game.status,color=Muted,fontSize=12.sp,maxLines=1)
                }
            }
            if(!loading && games.isEmpty()) item(span={GridItemSpan(maxLineSpan)}) {Text(tr("No hay partidos publicados para hoy.","No games published for today."),color=Muted)}
        }
    }
    video?.let {LidomYoutubePlayer(it,{},close={video=null})}
}
