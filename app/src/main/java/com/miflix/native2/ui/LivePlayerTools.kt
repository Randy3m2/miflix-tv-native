package com.miflix.native2.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import androidx.tv.material3.Text
import com.miflix.native2.data.*
import com.miflix.native2.model.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LiveChannelPicker(state: AppState, initialProvider: String, select: (PlayerRequest) -> Unit, close: () -> Unit) {
    val context=LocalContext.current
    val prefs=remember { context.getSharedPreferences("miflix_live",android.content.Context.MODE_PRIVATE) }
    fun number(manifest: String,channel: LiveChannel): Int {
        if(channel.number>0) return channel.number
        val key="channel_${manifest}_${channel.id}"
        val known=prefs.getInt(key,0)
        if(known>0) return known
        val counter="next_number_$manifest";val n=prefs.getInt(counter,1)
        prefs.edit().putInt(key,n).putInt(counter,n+1).apply();return n
    }
    val repo=remember { LiveRepository() }
    val scope=rememberCoroutineScope()
    var provider by remember { mutableStateOf(initialProvider) }
    var showFavorites by remember { mutableStateOf(false) }
    
    val manifest=state.calendarLiveManifest(provider)
    var categories by remember { mutableStateOf<List<LiveCategory>>(emptyList()) }
    var category by remember { mutableStateOf<LiveCategory?>(null) }
    var channels by remember { mutableStateOf<List<LiveChannel>>(emptyList()) }
    var search by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var resolving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var more by remember { mutableStateOf(false) }
    var offset by remember { mutableStateOf(0) }
    var generation by remember { mutableStateOf(0) }
    val first=remember { FocusRequester() }
    LaunchedEffect(Unit) { withFrameNanos { }; first.requestFocus() }
    LaunchedEffect(manifest) {
        categories=emptyList(); category=null;channels=emptyList();message=""
        try { categories=repo.categories(manifest);category=categories.firstOrNull() }
        catch(e: Exception) { if(e is CancellationException) throw e;message=e.message.orEmpty() }
    }
    LaunchedEffect(manifest,category,search) {
        generation++
        val c=category ?: return@LaunchedEffect
        busy=true;message="";channels=emptyList();more=false
        try {
            if(search.isNotBlank()) delay(300)
            channels=repo.channels(manifest,c,search=search);offset=channels.size;more=c.paginated && channels.isNotEmpty()
        } catch(e: Exception) { if(e is CancellationException) throw e;message=e.message.orEmpty() }
        finally { busy=false }
    }
    PlayerMenuDialog(onDismissRequest={ if(!resolving) close() },properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Column(Modifier.fillMaxWidth(.9f).heightIn(max=540.dp).background(Panel,RoundedCornerShape(22.dp)).padding(24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text(tr("Elegir canal","Choose channel"),color=Color.White,fontSize=25.sp)
            Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                FocusButton("Live TV",modifier=Modifier.focusRequester(first)) { if(!resolving) provider="nauta" }
                FocusButton("Live Sports") { if(!resolving) provider="sports" }
                FocusButton(tr("Canales","Channels")) { if(!resolving) provider="iptv:do" }
                FocusButton("★") { if(!resolving) showFavorites=true }
                FocusButton(tr("Cerrar","Close")) { if(!resolving) close() }
            }
            RegionTabs(provider) { if(!resolving) provider=it }
            LazyRow(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                items(categories) { c -> FocusButton(c.name,primary=category==c) { if(!resolving) category=c } }
            }
            NativeTextField(search,{ if(!resolving) search=it },tr("Buscar canal","Search channel"),modifier=Modifier.fillMaxWidth())
            if(busy || resolving) Text(tr("Cargando…","Loading…"),color=Muted)
            if(message.isNotBlank()) Text(message,color=Color.White,maxLines=3)
            LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(6.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                val visible=channels.filter { search.isBlank() || IptvPlaylist.searchKey(it.name).contains(IptvPlaylist.searchKey(search)) || it.number.toString()==search.trim() }
                items(visible,key={it.type+":"+it.id}) { channel ->
                    Column { Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    AsyncImage(channel.poster,null,Modifier.size(48.dp),contentScale=ContentScale.Fit)
                    FocusButton("${number(manifest,channel).toString().padStart(3,'0')} · ${channel.name}",modifier=Modifier.weight(1f)) {
                        if(!busy && !resolving) scope.launch {
                            resolving=true;message=""
                            try {
                                val stream=repo.streams(manifest,channel).firstOrNull() ?: error(tr("Canal sin enlaces disponibles","No streams available"))
                                val item=MediaSummary(channel.id.hashCode(),"live",channel.name,channel.description,null,channel.poster,0.0,"LIVE")
                                select(PlayerRequest(item,stream,0,0,0,true,channel.id,provider,liveType=channel.type))
                            } catch(e: Exception) { if(e is CancellationException) throw e;message=e.message.orEmpty() }
                            finally { resolving=false }
                        }
                    }
                }
                    ChannelExtras(state,provider,channel)
                    }
                }
                if(visible.isEmpty() && !busy) item { Text(tr("No hay canales en esta búsqueda","No channels for this search"),color=Muted) }
                if(more && !busy) item { FocusButton(tr("Cargar más","Load more")) { scope.launch {
                    val c=category ?: return@launch
                    val currentManifest=manifest;val currentSearch=search;val currentGeneration=generation
                    busy=true
                    try {
                        val rows=repo.channels(currentManifest,c,offset,currentSearch)
                        if(generation==currentGeneration) {
                            val before=channels.size;channels=(channels+rows).distinctBy { it.type+":"+it.id };offset+=rows.size;more=channels.size>before
                        }
                    } catch(e: Exception) { if(e is CancellationException) throw e;message=e.message.orEmpty() }
                    finally { busy=false }
                } } }
            }
        }
    }
    if(showFavorites) FavoriteChannelsDialog(state,{ showFavorites=false;select(it) }) { showFavorites=false }

}

@Composable
fun MultiViewScreen(state: AppState) {
    val players=remember { MultiPlayerPool() }
    DisposableEffect(players) { onDispose { players.releaseAll() } }
    var selected by remember { mutableStateOf<String?>(null) }
    var replacing by remember { mutableStateOf<String?>(null) }
    var sourceTarget by remember {mutableStateOf<PlayerRequest?>(null)}
    var picker by remember { mutableStateOf(false) }
    var sound by remember { mutableStateOf(state.multiViewRequests.firstOrNull()?.playbackId) }
    var hd by remember { mutableStateOf(false) }
    val first=remember { FocusRequester() }
    LaunchedEffect(Unit) { withFrameNanos { };first.requestFocus() }
    val view=LocalView.current
    DisposableEffect(view) { val old=view.keepScreenOn;view.keepScreenOn=true;onDispose { view.keepScreenOn=old } }
    fun exit() { players.releaseAll();state.multiViewRequests.clear();state.screen=Screen.LIVE_TV }
    BackHandler { exit() }
    Column(Modifier.fillMaxSize().background(Color.Black).padding(22.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            Text(tr("Multivista","Multiview")+" · ${state.multiViewRequests.size}/4",color=Color.White,fontSize=23.sp,modifier=Modifier.weight(1f))
            FocusButton(tr("Añadir canal","Add channel"),modifier=Modifier.focusRequester(first)) { if(state.multiViewRequests.size<4) { replacing=null;picker=true } }
            FocusButton(if(hd) "HD · 720p" else tr("Fluido · 540p","Smooth · 540p")) { hd=!hd }
            FocusButton(tr("Salir","Exit")) { exit() }
        }
        Text(tr("OK en un canal: elegir audio, cambiar, ampliar o quitar.","OK on a channel: choose audio, change, expand or remove."),color=Muted,fontSize=13.sp)
        val slots=state.multiViewRequests.toList()
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val columns=if(slots.size<=1) 1 else 2
            val rows=if(slots.size>2) 2 else 1
            val tileWidth=(maxWidth-12.dp*(columns-1))/columns
            val tileHeight=(maxHeight-12.dp*(rows-1))/rows
            slots.forEachIndexed { index,req ->
                key(req.playbackId) {
                    MultiViewTile(req,req.playbackId==sound,hd,players,
                        Modifier.offset(x=(tileWidth+12.dp)*(index%columns),y=(tileHeight+12.dp)*(index/columns)).width(tileWidth).height(tileHeight),index) {
                        selected=req.playbackId
                    }
                }
            }
        }
    }

    selected?.let { id ->
        val req=state.multiViewRequests.firstOrNull { it.playbackId==id }
        if(req!=null) PlayerMenuDialog(onDismissRequest={ selected=null }) {
            val focus=remember { FocusRequester() }
            LaunchedEffect(Unit) { withFrameNanos { };focus.requestFocus() }
            Column(Modifier.width(440.dp).heightIn(max=440.dp).verticalScroll(rememberScrollState()).background(Panel,RoundedCornerShape(20.dp)).padding(24.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
                Text(req.item.title,color=Color.White,fontSize=23.sp)
                FocusButton(tr("Reintentar","Retry"),modifier=Modifier.focusRequester(focus)) {
                    val replacement=req.copy(playbackId=java.util.UUID.randomUUID().toString())
                    val i=state.multiViewRequests.indexOfFirst { it.playbackId==id }
                    players.release(id)
                    if(i>=0) state.multiViewRequests[i]=replacement
                    if(sound==id) sound=replacement.playbackId
                    selected=null
                }
                FocusButton(tr("Escuchar este canal","Listen to this channel")) { sound=id;selected=null }
                FocusButton(tr("Cambiar fuente","Change source")) {sourceTarget=req;selected=null}
                FocusButton(tr("Cambiar canal","Change channel")) { replacing=id;selected=null;picker=true }
                FocusButton(tr("Pantalla completa","Full screen")) { players.releaseAll();state.multiViewRequests.clear();state.openLivePlayer(req.copy(playbackId=java.util.UUID.randomUUID().toString())) }
                FocusButton(tr("Quitar","Remove")) {
                    players.release(id)
                    state.multiViewRequests.removeAll { it.playbackId==id }
                    if(sound==id) sound=state.multiViewRequests.firstOrNull()?.playbackId
                    selected=null
                    if(state.multiViewRequests.isEmpty()) exit()
                }
                FocusButton(tr("Cerrar","Close")) { selected=null }
            }
        }
    }
    sourceTarget?.let { target ->
        MultiViewSourceDialog(state,target,choose={ stream ->
            val i=state.multiViewRequests.indexOfFirst {it.playbackId==target.playbackId}
            if(i>=0) {
                val replacement=target.copy(stream=stream,playbackId=java.util.UUID.randomUUID().toString())
                players.release(target.playbackId)
                state.multiViewRequests[i]=replacement
                if(sound==target.playbackId) sound=replacement.playbackId
            }
            sourceTarget=null
        },close={sourceTarget=null})
    }
    if(picker) LiveChannelPicker(state,state.liveProviderTab,select={ req ->
        val i=state.multiViewRequests.indexOfFirst { it.playbackId==replacing }
        if(i>=0) { replacing?.let { players.release(it) };if(sound==replacing) sound=req.playbackId;state.multiViewRequests[i]=req }
        else if(state.multiViewRequests.size<4) { state.multiViewRequests.add(req);if(sound==null) sound=req.playbackId }
        picker=false
    },close={ picker=false })
}

@Composable
private fun MultiViewSourceDialog(state: AppState,req: PlayerRequest,choose: (com.miflix.native2.model.StreamChoice) -> Unit,close: () -> Unit) {
    val repo=remember {LiveRepository()}
    var rows by remember(req.playbackId) {mutableStateOf<List<com.miflix.native2.model.StreamChoice>>(emptyList())}
    var loading by remember {mutableStateOf(true)}
    var message by remember {mutableStateOf("")}
    var revision by remember {mutableIntStateOf(0)}
    val first=remember {FocusRequester()}
    val firstLink=remember {FocusRequester()}
    LaunchedEffect(rows) {if(rows.isNotEmpty()) {withFrameNanos { };runCatching {firstLink.requestFocus()}}}
    LaunchedEffect(req.playbackId,revision) {
        loading=true;message=""
        try {
            val provider=req.liveProvider.ifBlank {state.liveProviderTab}
            rows=repo.streams(state.calendarLiveManifest(provider),LiveChannel(req.liveChannelId,req.liveType.ifBlank {"tv"},req.item.title,req.item.poster,req.item.overview))
            if(rows.isEmpty()) message=tr("El proveedor no devolvió fuentes alternativas.","The provider returned no alternative sources.")
        } catch(e: CancellationException) {throw e}
        catch(e: Exception) {message=e.message ?: tr("No se pudieron cargar las fuentes.","Could not load sources.")}
        finally {loading=false}
    }
    PlayerMenuDialog(onDismissRequest=close,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Column(Modifier.widthIn(max=650.dp).fillMaxWidth(.85f).heightIn(max=430.dp).background(Panel,RoundedCornerShape(20.dp)).padding(24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text(tr("Fuente del canal","Channel source")+" · "+req.item.title,color=Color.White,fontSize=22.sp,maxLines=2)
            if(loading) Text(tr("Buscando fuentes…","Finding sources…"),color=Muted)
            if(message.isNotBlank()) Text(message,color=Muted,fontSize=14.sp,maxLines=3)
            LazyColumn(Modifier.weight(1f,false),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                items(rows,key={it.url}) {stream ->
                    FocusButton((if(stream.url==req.stream.url) "✓ " else "")+stream.name+" · "+stream.title,modifier=Modifier.fillMaxWidth().then(if(stream.url==rows.firstOrNull()?.url) Modifier.focusRequester(firstLink) else Modifier)) {choose(stream)}
                }
            }
            Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                FocusButton(tr("Actualizar fuentes","Refresh sources")) {if(!loading) revision++}
                FocusButton(tr("Cerrar","Close"),modifier=Modifier.focusRequester(first)) {close()}
            }
        }
        LaunchedEffect(Unit) {withFrameNanos { };runCatching {first.requestFocus()}}
    }
}

@Composable
private fun MultiViewTile(req: PlayerRequest, audible: Boolean, hd: Boolean, pool: MultiPlayerPool, modifier: Modifier, order: Int, click: () -> Unit) {
    val context=LocalContext.current
    var player by remember(req.playbackId) { mutableStateOf<ExoPlayer?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var buffering by remember { mutableStateOf(true) }
    var focused by remember { mutableStateOf(false) }
    val lifecycle=context.playbackActivity()?.lifecycle
    DisposableEffect(player,lifecycle) {
        val p=player
        val observer=LifecycleEventObserver { _,event ->
            if(event==Lifecycle.Event.ON_STOP) p?.pause()
            else if(event==Lifecycle.Event.ON_START && error==null) p?.play()
        }
        lifecycle?.addObserver(observer)
        if(lifecycle?.currentState?.isAtLeast(Lifecycle.State.STARTED)==false) p?.pause()
        onDispose { lifecycle?.removeObserver(observer) }
    }
    val latestAudible by rememberUpdatedState(audible)
    val latestHd by rememberUpdatedState(hd)
    LaunchedEffect(req.playbackId) {
        // Stagger decoder creation; the single player is disposed before four tiles start.
        delay(250L+order*180L)
        try {
            if(pool.closed) return@LaunchedEffect
            val http=DefaultHttpDataSource.Factory().setAllowCrossProtocolRedirects(true).setDefaultRequestProperties(req.stream.requestHeaders)
            val p=ExoPlayer.Builder(context,DefaultRenderersFactory(context).setEnableDecoderFallback(true))
                .setMediaSourceFactory(DefaultMediaSourceFactory(DefaultDataSource.Factory(context,http)))
                .setLoadControl(DefaultLoadControl.Builder().setBufferDurationsMs(6000,16000,800,1500).setTargetBufferBytes(12*1024*1024).build()).build()
            if(!pool.add(req.playbackId,p)) return@LaunchedEffect
            p.trackSelectionParameters=p.trackSelectionParameters.buildUpon().setMaxVideoSize(if(latestHd) 1280 else 960,if(latestHd) 720 else 540)
                .setMaxVideoBitrate(if(latestHd) 3000000 else 1800000).setMaxVideoFrameRate(30).setTrackTypeDisabled(C.TRACK_TYPE_TEXT,true)
                .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO,!latestAudible).build()
            p.volume=if(latestAudible) 1f else 0f
            var triedLiveHls=false
            p.addListener(object: Player.Listener {
                override fun onPlaybackStateChanged(state: Int) { buffering=state==Player.STATE_BUFFERING }
                override fun onPlayerError(e: androidx.media3.common.PlaybackException) {
                    if(!triedLiveHls && e.errorCode==androidx.media3.common.PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED) {
                        triedLiveHls=true
                        p.setMediaItem(MediaItem.Builder().setUri(req.stream.url).setMimeType(androidx.media3.common.MimeTypes.APPLICATION_M3U8).build())
                        p.prepare();return
                    }
                    error=playerFailureDetails(e)
                    p.stop()
                }
            })
            p.setMediaItem(MediaItem.fromUri(req.stream.url));p.prepare();p.playWhenReady=true;player=p
            kotlinx.coroutines.awaitCancellation()
        } catch(e: CancellationException) { throw e } catch(e: Exception) { error=e.message }
        finally { player=null;pool.release(req.playbackId) }
    }
    LaunchedEffect(player,audible,hd) {
        player?.let { p ->
            p.volume=if(audible) 1f else 0f
            p.trackSelectionParameters=p.trackSelectionParameters.buildUpon().setTrackTypeDisabled(C.TRACK_TYPE_AUDIO,!audible)
                .setMaxVideoSize(if(hd) 1280 else 960,if(hd) 720 else 540).setMaxVideoBitrate(if(hd) 3000000 else 1800000).build()
        }
    }
    Box(modifier.onFocusChanged { focused=it.hasFocus }.tvClick(click).focusable().clickable(onClick=click)
        .border(if(focused) 3.dp else 1.dp,if(focused) Accent else Color(0xFF444444)).padding(4.dp).background(Color.Black)) {
        player?.let { p -> AndroidView(factory={ ctx -> PlayerView(ctx).apply { setEnableComposeSurfaceSyncWorkaround(true);useController=false;isFocusable=false;descendantFocusability=android.view.ViewGroup.FOCUS_BLOCK_DESCENDANTS;player=p } },update={ it.player=p },modifier=Modifier.fillMaxSize()) }
        Text((if(audible) "♫  " else "")+req.item.title,color=Color.White,fontSize=15.sp,modifier=Modifier.align(Alignment.BottomStart).background(Color(0xB0000000)).padding(8.dp))
        if(error!=null) Text(tr("No se pudo abrir. OK para cambiar o reintentar.","Unable to play. OK to change or retry.")+"\n"+error,color=Color.White,modifier=Modifier.align(Alignment.Center).background(Color.Black).padding(14.dp))
        else if(buffering) Text(tr("Cargando…","Loading…"),color=Color.White,modifier=Modifier.align(Alignment.Center))
    }
}

/** Main-thread ownership ensures a fifth decoder is never started before old tiles release. */
private class MultiPlayerPool {
    private val players=mutableMapOf<String,ExoPlayer>()
    var closed=false
        private set
    fun add(id: String,player: ExoPlayer): Boolean {
        if(closed) { player.release();return false }
        release(id);players[id]=player;return true
    }
    fun release(id: String) { players.remove(id)?.release() }
    fun releaseAll() { closed=true;val old=players.values.toList();players.clear();old.forEach { it.release() } }
}
