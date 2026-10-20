package com.miflix.native2.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
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
    var section by remember(owner) {mutableStateOf(LiveHubMemoryRP46.section(owner))}
    var customChannelsRP45 by remember(owner) {mutableStateOf(false)}
    var configuring by remember {mutableStateOf(false)}
    var revision by remember {mutableIntStateOf(0)}
    val first=remember {FocusRequester()}
    val contentFocusRP44=remember {FocusRequester()}
    fun changeSectionRP46(value:String) {LiveHubMemoryRP46.section(owner,value);section=value}
    fun back() {changeSectionRP46("hub")}
    BackHandler {if(section!="hub") back() else state.screen=Screen.HOME}
    LaunchedEffect(section) {if(section=="hub") {withFrameNanos {};runCatching {first.requestFocus()}}}
    Column(Modifier.fillMaxSize().background(Color.Black).padding(horizontal=14.dp,vertical=10.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Sidebar(Screen.LIVE_TV,state.activeProfile.name,{state.screen=it},state.unreadNoticeCount,state.activeProfile,
            onMoveDown={runCatching {contentFocusRP44.requestFocus()};Unit},onEditPicture={state.profilePhotoEditor=state.activeProfile},kids=state.kidsMode)
        Column(Modifier.weight(1f).fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)) {
            FocusButton("‹",horizontalPadding=14.dp) {if(section=="hub") state.screen=Screen.HOME else back()}
            Text(if(section=="hub") tr("En Vivo","Live") else when(section) {"basketball"->"NBA · Basketball";"baseball"->"MLB · Baseball";"football"->"NFL · American Football";"soccer"->tr("Fútbol","Football");"f1"->"F1";"lidom"->"LIDOM";"tv"->tr("Canales de TV","TV Channels");else->"Others"},color=Color.White,fontSize=24.sp,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f))
            if(section=="others") {
                FocusButton(tr("Mis canales","My channels")) {customChannelsRP45=true}
                FocusButton(tr("Configurar deportes","Sports settings")) {configuring=true}
            }
        }
        if(section=="hub") {
            val board=ImageBitmap.imageResource(R.drawable.live_hub_board_rp41)
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().focusRequester(contentFocusRP44).focusGroup()) {
            val gapRP46=10.dp
            val tileWidthRP46=(maxWidth-32.dp-gapRP46*2)/3
            val tileHeightRP46=(maxHeight-8.dp-gapRP46*2)/3
            Column(Modifier.width(tileWidthRP46*3+gapRP46*2).height(tileHeightRP46*3+gapRP46*2).align(Alignment.Center),verticalArrangement=Arrangement.spacedBy(gapRP46)) {
                repeat(3) {row ->
                    Row(Modifier.height(tileHeightRP46).fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(gapRP46)) {
                        repeat(3) {col ->
                            val index=row*3+col;val key=liveHubKeysRP41[index]
                            HubTileRP41(board,index,key,Modifier.weight(1f).fillMaxHeight().then(if(index==0) Modifier.focusRequester(first) else Modifier)) {
                                when(key) {
                                    "scores"->state.showLiveScoresRP11=true
                                    else->changeSectionRP46(key)
                                }
                            }
                        }
                    }
                }
            }
            }
        } else if(section=="tv") key(owner) {UnifiedLiveTVRP32(state,Modifier.weight(1f).focusRequester(contentFocusRP44).focusGroup())}
        else if(section=="lidom") HubLidomRP41(state,Modifier.weight(1f).focusRequester(contentFocusRP44).focusGroup())
        else key(section,revision) {HubSportsRP41(state,section,Modifier.weight(1f).focusRequester(contentFocusRP44).focusGroup())}
    }
    }
    if(customChannelsRP45) CustomChannelsPanelRP45(state) {customChannelsRP45=false}
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
    val scale by animateFloatAsState(if(focused) 1.015f else 1f,label="Live tile focus")
    val xs=listOf(40,534,1029);val ys=listOf(24,355,672)
    Box(modifier.graphicsLayer {scaleX=scale;scaleY=scale;shadowElevation=if(focused) 10.dp.toPx() else 0f;shape=RoundedCornerShape(13.dp)}
        .clip(RoundedCornerShape(13.dp)).border(if(focused) 2.dp else 1.dp,if(focused) Color.White else Color(0xFF263245),RoundedCornerShape(13.dp))
        .semantics {contentDescription=label}.onFocusChanged {focused=it.isFocused}.focusable().tvClick(click).clickable(onClick=click)) {
        Canvas(Modifier.fillMaxSize()) {
            val sourceY=ys[index/3]+if(index/3==2) 0 else 8
            drawRect(Color(0xFF080B13))
            // Cover the wider card with dim stadium art; fit the main artwork without stretching.
            val backgroundHeight=(467f*size.height/size.width).toInt().coerceIn(1,292)
            drawImage(board,srcOffset=IntOffset(xs[index%3],sourceY+(292-backgroundHeight)/2),srcSize=IntSize(467,backgroundHeight),dstSize=IntSize(size.width.toInt(),size.height.toInt()),alpha=.24f)
            val fit=minOf(size.width/467f,size.height/292f)
            val artWidth=(467*fit).toInt();val artHeight=(292*fit).toInt()
            drawImage(board,srcOffset=IntOffset(xs[index%3],sourceY),srcSize=IntSize(467,292),dstOffset=IntOffset((size.width.toInt()-artWidth)/2,(size.height.toInt()-artHeight)/2),dstSize=IntSize(artWidth,artHeight))
            if(focused) drawRect(Color.White.copy(alpha=.07f))
        }
    }
}

@Composable
private fun HubSportsRP41(state:AppState,section:String,modifier:Modifier) {
    val repo=remember {LiveRepository()};val scope=rememberCoroutineScope()
    val ownerRP46="${state.session?.userId ?: "local"}:${state.activeProfile.id}"
    val contextRP48=LocalContext.current
    val customRevisionRP48 by CustomChannelsRP45.changesRP48.collectAsState()
    val customRowsRP48=remember(ownerRP46,customRevisionRP48,section) {if(section=="others") CustomChannelsRP45.list(contextRP48,ownerRP46) else emptyList()}
    var customManageRP48 by remember(ownerRP46) {mutableStateOf<CustomChannelRP45?>(null)}
    val manifest=state.calendarLiveManifest("sports")
    var categories by remember {mutableStateOf<List<LiveCategory>>(emptyList())}
    var selected by remember {mutableStateOf<LiveCategory?>(null)}
    var events by remember {mutableStateOf<List<HubEventRP41>>(emptyList())}
    var loading by remember {mutableStateOf(true)};var resolving by remember {mutableStateOf(false)}
    var message by remember {mutableStateOf("")};var retry by remember {mutableIntStateOf(0)}
    var offset by remember {mutableIntStateOf(0)};var more by remember {mutableStateOf(false)}
    var generation by remember {mutableIntStateOf(0)}
    var nautaF1RP42 by remember {mutableStateOf<List<HubEventRP41>>(emptyList())}
    var nautaWarningRP42 by remember {mutableStateOf("")}
    var iptvF1RP43 by remember {mutableStateOf<List<HubEventRP41>>(emptyList())}
    var iptvWarningRP43 by remember {mutableStateOf("")}
    val displayedRP42=events+nautaF1RP42+iptvF1RP43
    val offsetsRP41=remember {mutableMapOf<LiveCategory,Int>()}
    val pendingRP41=remember {mutableSetOf<LiveCategory>()}
    val permitsRP41=remember {Semaphore(3)}
    LaunchedEffect(section,retry) {
        if(section!="f1") return@LaunchedEffect
        nautaWarningRP42=""
        try {
            val source=state.calendarLiveManifest("nauta")
            val catalog=hubCatalogRP41.get(source) ?: repo.categories(source).also {hubCatalogRP41.put(source,it)}
            val category=catalog.firstOrNull {it.searchable} ?: catalog.firstOrNull() ?: error("No Nauta catalogs")
            val rows=if(category.searchable) coroutineScope {
                listOf("F1","Formula 1","Movistar","DAZN").map {query ->async {
                    try {permitsRP41.withPermit {repo.channels(source,category,search=query)}}
                    catch(e:CancellationException) {throw e} catch(_:Exception) {emptyList()}
                }}.awaitAll().flatten()
            } else repo.channels(source,category)
            nautaF1RP42=rows.filter {requestedF1StationRP43(it.name)}.distinctBy {it.type+":"+it.id}.map {HubEventRP41("nauta",it,category)}
        } catch(e:CancellationException) {throw e} catch(_:Exception) {nautaWarningRP42=tr("Nauta no respondió. Los eventos de Live Sports siguen disponibles.","Nauta did not respond. Live Sports events remain available.")}
    }
    LaunchedEffect(section,retry) {
        if(section!="f1") return@LaunchedEffect
        iptvWarningRP43=""
        val limits=Semaphore(2)
        val providers=IptvRepository.sources.keys.sortedBy {if(it=="iptv:es") 0 else 1}
        var failures=0
        val rows=coroutineScope {providers.map {provider ->async {
            try {limits.withPermit {
                val category=LiveCategory("tv","iptv:all","IPTV · "+IptvRepository.sources[provider]?.first.orEmpty(),paginated=true,searchable=true)
                val manifest=state.calendarLiveManifest(provider)
                val candidates=mutableListOf<LiveChannel>()
                for(query in listOf("F1","Formula","Movistar","DAZN")) candidates+=repo.channels(manifest,category,search=query)
                candidates.filter {requestedF1StationRP43(it.name)}.distinctBy {it.type+":"+it.id}.map {HubEventRP41(provider,it,category)}
            }} catch(e:CancellationException) {throw e} catch(_:Exception) {failures++;emptyList()}
        }}.awaitAll().flatten()}
        iptvF1RP43=rows
        if(failures>0) iptvWarningRP43=tr("Algunas listas IPTV no respondieron.","Some IPTV playlists did not respond.")
    }
    LaunchedEffect(manifest,retry) {
        loading=true;message=""
        try {
            val all=hubCatalogRP41.get(manifest) ?: repo.categories(manifest).also {hubCatalogRP41.put(manifest,it)}
            categories=all.filter {liveSportGroupRP41(it)==section && (section!="others" || !liveAggregateRP41(it))}
            val savedRP46=LiveHubMemoryRP46.category(ownerRP46,section)
            selected=categories.firstOrNull {it.type+":"+it.id+":"+it.genre.orEmpty()==savedRP46} ?: categories.firstOrNull()
            if(categories.isEmpty()) message=tr("No hay categorías publicadas para este deporte.","No categories published for this sport.")
        } catch(e:CancellationException) {throw e} catch(_:Exception) {message=tr("La fuente de deportes no respondió.","Sports source did not respond.")}
        finally {loading=false}
    }
    LaunchedEffect(selected) {
        generation++;val c=selected ?: return@LaunchedEffect
        LiveHubMemoryRP46.category(ownerRP46,section,c.type+":"+c.id+":"+c.genre.orEmpty())
        loading=true;events=emptyList();message="";more=false;offset=0
        try {
            offsetsRP41.clear();pendingRP41.clear()
            val requested=listOf(c)
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
    var pendingRP44 by remember {mutableStateOf<HubEventRP41?>(null)}
    fun playRP44(event:HubEventRP41,stream:StreamChoice) {
        val c=event.channel
        LiveChannelNavigationRP40.save(state,LiveBrowseSnapshotRP40(BrowserCategoryRP34(event.provider,event.category,"Sports"),displayedRP42.filter {it.provider==event.provider && it.category==event.category}.map {BrowserChannelRP34(it.provider,it.channel)},offset=offsetsRP41[event.category] ?: offset,more=event.category in pendingRP41))
        state.openLivePlayer(PlayerRequest(MediaSummary(c.id.hashCode(),"live",c.name,c.description,null,c.poster,0.0,"LIVE"),stream,0,0,0L,live=true,liveChannelId=c.id,liveProvider=event.provider,liveType=c.type,returnScreen=Screen.LIVE_TV))
    }
    pendingRP44?.let {event ->LiveSportsLinksRP44(state,event.channel,{pendingRP44=null}) {stream ->pendingRP44=null;playRP44(event,stream)}}
    fun tune(event:HubEventRP41) {
        if(resolving) return
        if(state.partyRole==PartyRole.GUEST) {message=tr("El anfitrión elige el contenido.","The host chooses the content.");return}
        if(event.provider=="sports") {pendingRP44=event;return}
        val stamp=generation;val profile=state.session?.userId to state.activeProfile.id
        scope.launch {
            resolving=true;message=""
            try {
                val stream=repo.streams(state.calendarLiveManifest(event.provider),event.channel).firstOrNull() ?: error("No links")
                if(stamp!=generation || profile!=(state.session?.userId to state.activeProfile.id) || state.screen!=Screen.LIVE_TV || state.partyRole==PartyRole.GUEST) return@launch
                val c=event.channel
                LiveChannelNavigationRP40.save(state,LiveBrowseSnapshotRP40(BrowserCategoryRP34(event.provider,event.category,if(event.provider=="nauta") "Nauta" else "Sports"),displayedRP42.filter {it.provider==event.provider && it.category==event.category}.map {BrowserChannelRP34(it.provider,it.channel)},offset=if(event.provider=="sports") offsetsRP41[event.category] ?: offset else displayedRP42.count {it.provider==event.provider && it.category==event.category},more=event.provider=="sports" && event.category in pendingRP41))
                state.openLivePlayer(PlayerRequest(MediaSummary(c.id.hashCode(),"live",c.name,c.description,null,c.poster,0.0,"LIVE"),stream,0,0,0L,live=true,liveChannelId=c.id,liveProvider=event.provider,liveType=c.type,returnScreen=Screen.LIVE_TV))
            } catch(e:CancellationException) {throw e} catch(_:Exception) {message=tr("Este evento no devolvió un enlace reproducible.","This event returned no playable link.")}
            finally {resolving=false}
        }
    }
    Column(modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        // Only Others has category tabs. Dedicated sports present event cards only.
        if(categories.isNotEmpty()) {
            Text(tr("EXPLORA MÁS","EXPLORE MORE"),color=Muted,fontSize=11.sp,fontWeight=FontWeight.Bold)
            LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp),contentPadding=PaddingValues(4.dp)) {
                items(categories,key={it.type+":"+it.id+":"+it.genre}) {c ->HubCategoryChipRP42(c.name,c==selected) {selected=c}}
            }
        }
        if(loading || resolving) Text(if(resolving) tr("Abriendo evento…","Opening event…") else tr("Cargando eventos…","Loading events…"),color=Muted)
        if(iptvWarningRP43.isNotBlank()) Text(iptvWarningRP43,color=Muted,fontSize=12.sp)
        if(nautaWarningRP42.isNotBlank()) Text(nautaWarningRP42,color=Muted,fontSize=12.sp)
        if(message.isNotBlank()) {Text(message,color=Muted);FocusButton(tr("Reintentar","Retry")) {retry++}}
        LazyVerticalGrid(GridCells.Adaptive(240.dp),Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(14.dp),verticalArrangement=Arrangement.spacedBy(14.dp),contentPadding=PaddingValues(6.dp,6.dp,6.dp,30.dp)) {
            items(customRowsRP48,key={"custom:"+it.id}) {channel ->
                HubEventCardRP41(channel.name,null,tr("CANAL PERSONAL · Mantén OK: opciones","PERSONAL CHANNEL · Hold OK: options"),onHoldRP48={customManageRP48=channel}) {
                    if(state.partyRole!=PartyRole.GUEST && !state.kidsMode) state.openLivePlayer(CustomChannelsRP45.request(channel))
                }
            }
            items(displayedRP42,key={it.key}) {event ->HubEventCardRP41(event.channel.name,event.channel.poster,when {event.provider=="nauta"->if(f1ChannelRP42(event.channel.name)) "Nauta · F1" else "Nauta · DAZN";event.provider in IptvRepository.sources->"IPTV · "+IptvRepository.sources[event.provider]?.first.orEmpty();else->event.channel.description}) {tune(event)}}
            if(!loading && displayedRP42.isEmpty() && customRowsRP48.isEmpty() && message.isBlank()) item(span={GridItemSpan(maxLineSpan)}) {Text(tr("No hay eventos publicados ahora.","No events published right now."),color=Muted)}
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
    customManageRP48?.let {channel ->CustomChannelManageRP48(state,channel) {customManageRP48=null}}

}

@Composable
internal fun HubEventCardRP41(title:String,poster:String?,subtitle:String,onHoldRP48:(()->Unit)?=null,click:()->Unit) {
    var focused by remember(title) {mutableStateOf(false)}
    val scale by animateFloatAsState(if(focused) 1.015f else 1f,label="Event focus")
    Column(Modifier.fillMaxWidth().height(194.dp).graphicsLayer {scaleX=scale;scaleY=scale}
        .clip(RoundedCornerShape(13.dp)).background(Brush.verticalGradient(listOf(Color(0xFF242424),Color(0xFF101010))))
        .border(if(focused) 2.dp else 1.dp,if(focused) Color.White else PanelBorder,RoundedCornerShape(13.dp))
        .onFocusChanged {focused=it.isFocused}.focusable().tvPlaybackClick(click,onHoldRP48 ?: click).clickable(onClick=click)) {
        Box(Modifier.fillMaxWidth().weight(1f).background(Brush.verticalGradient(listOf(Color(0xFF17283C),Color.Black))),contentAlignment=Alignment.Center) {
            if(!poster.isNullOrBlank()) AsyncImage(poster,null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
            else Text(title,color=Color.White,fontSize=22.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(14.dp),maxLines=2)
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent,Color(0x80000000)))))
        }
        Text(title,color=Color.White,fontSize=14.sp,fontWeight=FontWeight.Bold,maxLines=2,overflow=TextOverflow.Ellipsis,modifier=Modifier.padding(horizontal=12.dp,vertical=6.dp))
        if(subtitle.isNotBlank()) Text(subtitle,color=Muted,fontSize=11.sp,maxLines=1,overflow=TextOverflow.Ellipsis,modifier=Modifier.padding(start=12.dp,end=12.dp,bottom=8.dp))
    }
}

@Composable
private fun HubLidomRP41(state:AppState,modifier:Modifier) {
    val context=LocalContext.current
    val owner="${state.session?.userId ?: "local"}:${state.activeProfile.id}"
    val repo=remember(owner) {LidomRepository(context,owner)}
    var date by remember(owner) {mutableStateOf(LidomRepository.today())}
    var games by remember {mutableStateOf<List<LidomGame>>(emptyList())}
    var loading by remember {mutableStateOf(true)}
    var message by remember {mutableStateOf("")};var retry by remember {mutableIntStateOf(0)}
    var team by remember(owner) {mutableStateOf<LidomTeam?>(null)}
    var configure by remember {mutableStateOf(false)}
    LaunchedEffect(date,retry) {
        loading=true;message=""
        try {games=repo.games(date)} catch(e:CancellationException) {throw e} catch(_:Exception) {games=emptyList();message=tr("No se pudo cargar el calendario.","Could not load schedule.")}
        finally {loading=false}
    }
    Column(modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            FocusButton("‹",horizontalPadding=16.dp) {date=moveLidomDayRP42(date,-1)}
            FocusButton(tr("Hoy","Today")) {date=LidomRepository.today()}
            FocusButton("›",horizontalPadding=16.dp) {date=moveLidomDayRP42(date,1)}
            Text(date+" · AST",color=Color.White,fontSize=16.sp,modifier=Modifier.weight(1f))
            FocusButton(tr("Ajustes","Settings")) {configure=true}
        }
        if(loading) Text(tr("Cargando partidos…","Loading games…"),color=Muted)
        if(message.isNotBlank()) {Text(message,color=Muted);FocusButton(tr("Reintentar","Retry")) {retry++}}
        LazyVerticalGrid(GridCells.Adaptive(260.dp),Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(14.dp),verticalArrangement=Arrangement.spacedBy(14.dp),contentPadding=PaddingValues(4.dp,4.dp,4.dp,30.dp)) {
            items(games,key={"game:${it.id}"}) {game ->
                var focused by remember {mutableStateOf(false)}
                val choose={team=game.home;state.lidomTeamId=game.home.id}
                Column(Modifier.fillMaxWidth().height(190.dp).clip(RoundedCornerShape(14.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFF202020),Color(0xFF0D0D0D))))
                    .border(if(focused) 2.dp else 1.dp,if(focused) Color.White else PanelBorder,RoundedCornerShape(14.dp))
                    .onFocusChanged {focused=it.isFocused}.focusable().tvClick(choose).clickable(onClick=choose).padding(14.dp),
                    horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.weight(1f),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)) {
                        val away=context.resources.getIdentifier(game.away.logo,"drawable",context.packageName)
                        val home=context.resources.getIdentifier(game.home.logo,"drawable",context.packageName)
                        if(away!=0) Image(painterResource(away),game.away.name,Modifier.size(58.dp))
                        Text("VS",color=Muted,fontWeight=FontWeight.Bold)
                        if(home!=0) Image(painterResource(home),game.home.name,Modifier.size(58.dp))
                    }
                    Text(game.away.short+" vs "+game.home.short,color=Color.White,fontWeight=FontWeight.Bold)
                    Text(game.time+" · "+game.status,color=Muted,fontSize=12.sp,maxLines=1)
                }
            }
            if(!loading && games.isEmpty()) item(span={GridItemSpan(maxLineSpan)}) {Text(tr("No hay partidos para esta fecha. Puedes abrir los canales de cada equipo.","No games for this date. You can open each team's channels."),color=Muted)}
            item(span={GridItemSpan(maxLineSpan)}) {Text(tr("EQUIPOS Y CANALES","TEAMS & CHANNELS"),color=Muted,fontSize=12.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=12.dp,bottom=4.dp))}
            items(repo.teams,key={"team:${it.id}"}) {club ->
                var focused by remember {mutableStateOf(false)}
                val choose={team=club;state.lidomTeamId=club.id}
                Row(Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(14.dp))
                    .background(if(focused) Color(0xFF303030) else Color(0xFF171717))
                    .border(if(focused) 2.dp else 1.dp,if(focused) Color.White else PanelBorder,RoundedCornerShape(14.dp))
                    .onFocusChanged {focused=it.isFocused}.focusable().tvClick(choose).clickable(onClick=choose).padding(14.dp),
                    verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)) {
                    val logo=context.resources.getIdentifier(club.logo,"drawable",context.packageName)
                    if(logo!=0) Image(painterResource(logo),club.name,Modifier.size(56.dp))
                    Column {Text(club.short,color=Color.White,fontWeight=FontWeight.Bold);Text(tr("Ver canales","View channels"),color=Muted,fontSize=12.sp)}
                }
            }
        }
    }
    team?.let {club ->LidomSourcesSheet(state,repo,club,{team=null},{team=null;configure=true})}
    if(configure) LidomSourcesSettings(repo,state.lidomTeamId,{configure=false})
}

@Composable
internal fun HubCategoryChipRP42(label:String,selected:Boolean,click:()->Unit) {
    var focused by remember {mutableStateOf(false)}
    Row(Modifier.clip(RoundedCornerShape(18.dp)).background(if(selected) Color(0xFF303030) else Color(0xFF161616))
        .border(if(focused) 2.dp else 1.dp,if(focused) Color.White else if(selected) Color(0xFF727272) else Color(0xFF2A2A2A),RoundedCornerShape(18.dp))
        .onFocusChanged {focused=it.isFocused}.focusable().tvClick(click).clickable(onClick=click).padding(horizontal=16.dp,vertical=12.dp),
        verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(9.dp)) {
        Canvas(Modifier.size(16.dp)) {
            if(label=="M3U") {
                for(i in 0..2) drawLine(Color.White,androidx.compose.ui.geometry.Offset(0f,size.height*(.2f+i*.3f)),androidx.compose.ui.geometry.Offset(size.width,size.height*(.2f+i*.3f)),strokeWidth=2.dp.toPx())
            } else drawCircle(if(selected) Color.White else Color(0xFF888888),radius=size.minDimension*.28f)
        }
        Text(label,color=Color.White,fontSize=13.sp,fontWeight=if(selected) FontWeight.Bold else FontWeight.Normal)
    }
}
