@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
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
    if(LocalMobileBrunioRP133.current) {
        MobileSheetRP133(tr("Canales y juegos","Channels and games"),close) {MobileLiveRP133(state,{select(it);close()})}
    } else LiveChannelBrowserRP34(state,initialProvider,select,close)
}

@Composable
fun MultiViewScreen(state: AppState) {
    val customContextRP48=LocalContext.current
    val players=remember { MultiPlayerPool() }
    DisposableEffect(players) { onDispose { players.releaseAll() } }
    var selected by remember { mutableStateOf<String?>(null) }
    var replacing by remember { mutableStateOf<String?>(null) }
    var sourceTarget by remember {mutableStateOf<PlayerRequest?>(null)}
    var picker by remember { mutableStateOf(false) }
    var lidomPicker by remember {mutableStateOf(state.multiViewRequests.isEmpty())}
    var lidomInitialTeam by remember {mutableStateOf("")}
    var youtubeFull by remember {mutableStateOf<PlayerRequest?>(null)}
    var sound by remember { mutableStateOf(state.multiViewRequests.firstOrNull()?.playbackId) }
    var hd by remember { mutableStateOf(false) }
    var panelRP43 by remember {mutableStateOf(true)}
    val statsRepoRP130=remember { SportsStatsRP95() }
    val f1RepoRP131=remember { F1StatsRP131() }
    val f1OverrideRP131=remember {mutableStateMapOf<String,Boolean>()}
    fun isF1RP131(req:PlayerRequest)=f1OverrideRP131[req.playbackId] ?: F1StatsRP131.channel(req.liveProvider,req.item.title+" "+req.item.overview)
    val statsGamesRP130=remember { mutableStateMapOf<String,LiveScoreRP11>() }
    var teamLogosRP130 by remember {mutableStateOf<Map<String,String>>(emptyMap())}
    LaunchedEffect(customContextRP48) {
        teamLogosRP130=kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching {
                val rows=org.json.JSONObject(customContextRP48.assets.open("lidom_teams.json").bufferedReader().use {it.readText()}).getJSONArray("teams")
                (0 until rows.length()).associate {index->
                    val team=rows.getJSONObject(index);team.getInt("mlb").toString() to team.optString("logo")
                }
            }.getOrDefault(emptyMap())
        }
    }
    fun assignStatsRP130(id:String,game:LiveScoreRP11) {
        if(state.multiViewRequests.none {it.playbackId==id}) return
        fun logo(team:ScoreTeamRP11)=if(game.league=="LIDOM" && team.logo.isBlank()) team.copy(logo=teamLogosRP130[team.id].orEmpty()) else team
        statsGamesRP130[id]=game.copy(away=logo(game.away),home=logo(game.home))
    }
    val statsOpenRP130=remember { mutableStateMapOf<String,Boolean>() }
    var statsSourceRP130 by remember { mutableStateOf<String?>(null) }
    var chooseStatsRP130 by remember { mutableStateOf<String?>(null) }
    var fourthStatsRP130 by remember { mutableStateOf(false) }
    val first=remember { FocusRequester() }
    val slotIdsRP130=state.multiViewRequests.map {it.playbackId}
    LaunchedEffect(slotIdsRP130) {
        statsGamesRP130.keys.toList().filter {it !in slotIdsRP130}.forEach {statsGamesRP130.remove(it)}
        f1OverrideRP131.keys.toList().filter {it !in slotIdsRP130}.forEach {f1OverrideRP131.remove(it)}
        statsOpenRP130.keys.toList().filter {it !in slotIdsRP130}.forEach {statsOpenRP130.remove(it)}
        if(statsSourceRP130 !in slotIdsRP130) statsSourceRP130=slotIdsRP130.firstOrNull()
        if(chooseStatsRP130 !in slotIdsRP130) chooseStatsRP130=null
    }
    LaunchedEffect(panelRP43) { withFrameNanos { };runCatching {first.requestFocus()} }
    val view=LocalView.current
    DisposableEffect(view) { val old=view.keepScreenOn;view.keepScreenOn=true;onDispose { view.keepScreenOn=old } }
    fun exit() {players.releaseAll();state.multiViewRequests.clear();state.playerRequest=null;state.miniPlayer=false;state.screen=Screen.LIVE_TV}
    BackHandler {
        when {
            chooseStatsRP130!=null ->chooseStatsRP130=null
            panelRP43 ->panelRP43=false
            state.multiViewRequests.size==3 && fourthStatsRP130 ->{fourthStatsRP130=false;runCatching {first.requestFocus()}}
            state.multiViewRequests.size==2 && statsOpenRP130.values.any {it} ->{statsOpenRP130.clear();runCatching {first.requestFocus()}}
            else ->exit()
        }
    }
    BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black).padding(6.dp)) {
        val slots=youtubeFull?.let {listOf(it)} ?: state.multiViewRequests.toList()
        BoxWithConstraints(Modifier.fillMaxSize().padding(bottom=52.dp)) {
            val columns=if(slots.size<=1) 1 else 2
            val rows=if(slots.size>2) 2 else 1
            val tileWidth=(maxWidth-12.dp*(columns-1))/columns
            val tileHeight=(maxHeight-12.dp*(rows-1))/rows
            slots.forEachIndexed { index,req ->
                key(req.playbackId) {
                    Box(Modifier.offset(x=(tileWidth+12.dp)*(index%columns),y=(tileHeight+12.dp)*(index/columns)).width(tileWidth).height(tileHeight)) {
                        MultiViewTile(req,if(youtubeFull!=null) req.playbackId==youtubeFull?.playbackId else req.playbackId==sound,hd,players,
                            Modifier.fillMaxSize(),index,statsFooterRP130=youtubeFull==null && slots.size in 2..3) {selected=req.playbackId}
                        if(youtubeFull==null && SplitStatsPolicyRP130.mode(slots.size)!=0) {
                            SplitStatsActionRP130("▥ Stats",modifier=Modifier.align(Alignment.BottomEnd).padding(8.dp).height(32.dp),selected=if(slots.size==3) fourthStatsRP130 && statsSourceRP130==req.playbackId else statsOpenRP130[req.playbackId]==true,click={
                                if(slots.size==3) {statsSourceRP130=req.playbackId;fourthStatsRP130=true}
                                else statsOpenRP130[req.playbackId]=statsOpenRP130[req.playbackId]!=true
                                panelRP43=false
                            })
                            if(slots.size==2 && statsOpenRP130[req.playbackId]==true) {
                                val band=((tileHeight-tileWidth*9f/16f)/2f-5.dp).coerceAtLeast(0.dp)
                                if(band>=96.dp) {
                                    if(isF1RP131(req)) F1StatsPanelRP131(state,f1RepoRP131,true,
                                        {statsOpenRP130[req.playbackId]=false;runCatching {first.requestFocus()}},first,
                                        Modifier.align(Alignment.TopCenter).padding(6.dp).fillMaxWidth().height(band))
                                    else SplitStatsDockRP130(state,req,statsRepoRP130,statsGamesRP130[req.playbackId],
                                    {assignStatsRP130(req.playbackId,it)},{chooseStatsRP130=req.playbackId},
                                    {statsOpenRP130[req.playbackId]=false;runCatching {first.requestFocus()}},
                                    Modifier.align(Alignment.TopCenter).padding(6.dp).fillMaxWidth().height(band))
                                }
                                else FocusButton(tr("Ver estadísticas","View stats"),modifier=Modifier.align(Alignment.TopStart).padding(8.dp)) {chooseStatsRP130=req.playbackId}
                            }
                        }
                    }
                }
            }
        }
        if(youtubeFull==null && slots.size==3) {
            val statsReq=slots.firstOrNull {it.playbackId==statsSourceRP130} ?: slots.first()
            // Fourth cell is data only: never allocates another decoder.
            Column(Modifier.align(Alignment.BottomEnd).padding(bottom=52.dp)
                .width((maxWidth-12.dp)/2f).height((maxHeight-64.dp)/2f)
                .background(Color(0xEA101318),RoundedCornerShape(12.dp))
                .border(1.dp,Color(0xFF444B55),RoundedCornerShape(12.dp)).padding(6.dp)) {
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                    slots.forEachIndexed {slotIndex,slot ->
                        SplitStatsActionRP130((slotIndex+1).toString()+" · "+slot.item.title,modifier=Modifier.weight(1f),selected=slot.playbackId==statsReq.playbackId,click={
                            statsSourceRP130=slot.playbackId;fourthStatsRP130=true;panelRP43=false
                        })
                    }
                }
                if(fourthStatsRP130) key(statsReq.playbackId) {
                    if(isF1RP131(statsReq)) F1StatsPanelRP131(state,f1RepoRP131,false,
                        {fourthStatsRP130=false;runCatching {first.requestFocus()}},first,Modifier.weight(1f))
                    else SplitStatsFullRP130(state,statsReq,statsRepoRP130,statsGamesRP130[statsReq.playbackId],
                        {assignStatsRP130(statsReq.playbackId,it)},{chooseStatsRP130=statsReq.playbackId},
                        {fourthStatsRP130=false;runCatching {first.requestFocus()}},first,Modifier.weight(1f))
                } else Box(Modifier.weight(1f).fillMaxWidth(),contentAlignment=Alignment.Center) {
                    FocusButton(tr("▥ Stats · elige una pantalla","▥ Stats · choose a screen")) {fourthStatsRP130=true;panelRP43=false}
                }
            }
        }
        if(panelRP43) {
            val panelWidth=if(maxWidth<700.dp) maxWidth*.52f else maxWidth*.36f
            Column(Modifier.align(Alignment.CenterEnd).width(panelWidth).fillMaxHeight(.86f)
                .background(Color(0xF20C0C0C),RoundedCornerShape(20.dp)).border(1.dp,Color(0xFF404040),RoundedCornerShape(20.dp))
                .padding(18.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                Text("SPLITSCREEN",color=Muted,fontSize=11.sp)
                Text("${state.multiViewRequests.size} / 4 · "+tr("pantallas","screens"),color=Color.White,fontSize=22.sp)
                FocusButton(tr("Añadir canal","Add channel"),modifier=Modifier.fillMaxWidth().focusRequester(first),primary=true) {
                    if(state.multiViewRequests.size<4) {replacing=null;picker=true}
                }
                FocusButton(tr("Añadir LIDOM","Add LIDOM"),modifier=Modifier.fillMaxWidth()) {
                    if(state.multiViewRequests.size<4) {replacing=null;lidomInitialTeam="";lidomPicker=true}
                }
                Text(tr("PANTALLAS ACTIVAS","ACTIVE SCREENS"),color=Muted,fontSize=11.sp,modifier=Modifier.padding(top=6.dp))
                state.multiViewRequests.forEachIndexed {index,req ->
                    FocusButton("${index+1} · "+req.item.title+(if(req.playbackId==sound) " · ♪" else ""),modifier=Modifier.fillMaxWidth()) {selected=req.playbackId}
                }
                Text(tr("AUDIO Y OPCIONES","AUDIO & OPTIONS"),color=Muted,fontSize=11.sp,modifier=Modifier.padding(top=6.dp))
                FocusButton(if(hd) "HD · 720p" else tr("Fluido · 540p","Smooth · 540p"),modifier=Modifier.fillMaxWidth()) {hd=!hd}
                FocusButton("PIN Live score",modifier=Modifier.fillMaxWidth()) {state.showLiveScoresRP11=true}
                if(youtubeFull!=null) FocusButton(tr("Volver al mosaico","Back to grid"),modifier=Modifier.fillMaxWidth()) {youtubeFull=null}
                FocusButton(tr("Ocultar panel","Hide panel"),modifier=Modifier.fillMaxWidth()) {panelRP43=false}
                FocusButton(tr("Salir de SplitScreen","Exit SplitScreen"),modifier=Modifier.fillMaxWidth()) {exit()}
            }
        } else SplitStatsActionRP130("SplitScreen · ☷",{panelRP43=true},Modifier.align(Alignment.BottomEnd).padding(horizontal=10.dp,vertical=2.dp).height(40.dp).focusRequester(first),textSize=14)
    }

    selected?.let { id ->
        val req=state.multiViewRequests.firstOrNull { it.playbackId==id }
        if(req!=null) PlayerMenuDialog(onDismissRequest={ selected=null },fullScreen=true) {
            Box(Modifier.fillMaxSize(),contentAlignment=Alignment.CenterEnd) {
            val focus=remember { FocusRequester() }
            LaunchedEffect(Unit) { withFrameNanos { };focus.requestFocus() }
            Column(Modifier.fillMaxWidth(.40f).fillMaxHeight(.86f).padding(end=12.dp).verticalScroll(rememberScrollState()).background(Color(0xF20C0C0C),RoundedCornerShape(20.dp)).padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
                Text(req.item.title,color=Color.White,fontSize=23.sp)
                FocusButton(tr("Reintentar","Retry"),modifier=Modifier.focusRequester(focus)) {
                    youtubeFull=null
                    val replacement=req.copy(playbackId=java.util.UUID.randomUUID().toString())
                    val i=state.multiViewRequests.indexOfFirst { it.playbackId==id }
                    players.release(id)
                    if(i>=0) state.multiViewRequests[i]=replacement
                    if(sound==id) sound=replacement.playbackId
                    selected=null
                }
                FocusButton(tr("Escuchar este canal","Listen to this channel")) { sound=id;selected=null }
                if(state.multiViewRequests.size in 2..3) {
                    FocusButton("Stats · F1") {
                        f1OverrideRP131[id]=true;statsSourceRP130=id;fourthStatsRP130=true;statsOpenRP130[id]=true
                        selected=null;panelRP43=false
                    }
                    FocusButton("Stats · NBA / MLB / LIDOM") {
                        f1OverrideRP131[id]=false;statsSourceRP130=id;fourthStatsRP130=true;statsOpenRP130[id]=true
                        selected=null;panelRP43=false
                    }
                }
                FocusButton(tr("Cambiar fuente","Change source")) {youtubeFull=null;sourceTarget=req;selected=null}
                FocusButton(tr("Cambiar canal","Change channel")) {
                    youtubeFull=null;replacing=id;selected=null
                    if(req.liveProvider=="lidom") {lidomInitialTeam="";lidomPicker=true} else picker=true
                }
                FocusButton(tr("Pantalla completa","Full screen")) {
                    selected=null
                    if(req.liveType=="youtube") youtubeFull=req
                    else {players.releaseAll();state.multiViewRequests.clear();state.openLivePlayer(req.copy(playbackId=java.util.UUID.randomUUID().toString()))}
                }
                FocusButton(tr("Quitar","Remove")) {
                    youtubeFull=null
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
    }
    chooseStatsRP130?.let {id ->
        state.multiViewRequests.firstOrNull {it.playbackId==id}?.let {req ->
            PlayerMenuDialog(onDismissRequest={chooseStatsRP130=null},fullScreen=true) {
                Box(Modifier.fillMaxSize(),contentAlignment=Alignment.CenterEnd) {
                    if(isF1RP131(req)) F1StatsPanelRP131(state,f1RepoRP131,false,
                        {chooseStatsRP130=null;runCatching {first.requestFocus()}},first,
                        Modifier.fillMaxWidth(.48f).fillMaxHeight().padding(8.dp))
                    else SportsStatsPanelRP95(state,splitStatsTitleRP130(req),statsRepoRP130,statsGamesRP130[id],
                        {assignStatsRP130(id,it);chooseStatsRP130=null},
                        {chooseStatsRP130=null;runCatching {first.requestFocus()}},first,
                        Modifier.fillMaxWidth(.48f).fillMaxHeight().padding(8.dp),leaguesRP130=listOf("NBA","MLB","LIDOM"),selectOnlyRP130=true)
                }
            }
        }
    }
    sourceTarget?.let { target ->
        if(target.liveProvider=="lidom") LidomMultiViewPickerRP9(state,target.lidomTeam,select={req ->
            val i=state.multiViewRequests.indexOfFirst {it.playbackId==target.playbackId}
            if(i>=0) {
                players.release(target.playbackId)
                state.multiViewRequests[i]=req
                if(sound==target.playbackId) sound=req.playbackId
            }
            sourceTarget=null
        },close={sourceTarget=null})
        else MultiViewSourceDialog(state,target,choose={ stream ->
            val i=state.multiViewRequests.indexOfFirst {it.playbackId==target.playbackId}
            if(i>=0) {
                val replacement=if(target.liveProvider==CustomChannelsRP45.provider) {
                    CustomChannelsRP45.findRP48(customContextRP48,CustomChannelsRP45.owner(state),target.liveChannelId)?.let {CustomChannelsRP45.request(it)}
                        ?: target.copy(stream=stream,playbackId=java.util.UUID.randomUUID().toString())
                } else target.copy(stream=stream,playbackId=java.util.UUID.randomUUID().toString())
                players.release(target.playbackId)
                state.multiViewRequests[i]=replacement
                if(sound==target.playbackId) sound=replacement.playbackId
            }
            sourceTarget=null
        },close={sourceTarget=null})
    }
    if(lidomPicker) LidomMultiViewPickerRP9(state,lidomInitialTeam,select={req ->
        val i=state.multiViewRequests.indexOfFirst {it.playbackId==replacing}
        if(i>=0) {
            replacing?.let {players.release(it)}
            state.multiViewRequests[i]=req
            if(sound==replacing) sound=req.playbackId
        } else if(state.multiViewRequests.size<4) {
            state.multiViewRequests.add(req)
            if(sound==null) sound=req.playbackId
        }
        lidomPicker=false
    },close={lidomPicker=false})
    if(picker) LiveChannelBrowserRP34(state,state.liveProviderTab,compactRP43=true,select={ req ->
        val i=state.multiViewRequests.indexOfFirst { it.playbackId==replacing }
        if(i>=0) { replacing?.let { players.release(it) };if(sound==replacing) sound=req.playbackId;state.multiViewRequests[i]=req }
        else if(state.multiViewRequests.size<4) { state.multiViewRequests.add(req);if(sound==null) sound=req.playbackId }
        picker=false
    },close={ picker=false })
}

@Composable
private fun MultiViewSourceDialog(state: AppState,req: PlayerRequest,choose: (com.miflix.native2.model.StreamChoice) -> Unit,close: () -> Unit) {
    val repo=remember {LiveRepository()}
    val customContextRP48=LocalContext.current
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
            rows=if(provider==CustomChannelsRP45.provider) {
                listOfNotNull(CustomChannelsRP45.findRP48(customContextRP48,CustomChannelsRP45.owner(state),req.liveChannelId)?.let {CustomChannelsRP45.request(it).stream})
            } else repo.streams(state.calendarLiveManifest(provider),LiveChannel(req.liveChannelId,req.liveType.ifBlank {"tv"},req.item.title,req.item.poster,req.item.overview))
            if(rows.isEmpty()) message=tr("El proveedor no devolvió fuentes alternativas.","The provider returned no alternative sources.")
        } catch(e: CancellationException) {throw e} catch(e: Exception) {message=e.message ?: tr("No se pudieron cargar las fuentes.","Could not load sources.")}
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
internal fun MultiViewTile(req: PlayerRequest, audible: Boolean, hd: Boolean, pool: MultiPlayerPool, modifier: Modifier, order: Int, statsFooterRP130:Boolean=false, click: () -> Unit) {
    if(req.liveType=="youtube") {
        LidomSplitYoutubeTileRP9(req,audible,modifier,click)
        return
    }
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
            p.setMediaItem(MediaItem.Builder().setUri(req.stream.url).apply {if(req.liveMime.isNotBlank()) setMimeType(req.liveMime)}.build());p.prepare();p.playWhenReady=true;player=p
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
        Text((if(audible) "♫  " else "")+req.item.title,color=Color.White,fontSize=15.sp,modifier=Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(end=if(statsFooterRP130) 110.dp else 0.dp).background(Color(0xB0000000)).padding(8.dp))
        if(error!=null) Text(tr("No se pudo abrir. OK para cambiar o reintentar.","Unable to play. OK to change or retry.")+"\n"+error,color=Color.White,modifier=Modifier.align(Alignment.Center).background(Color.Black).padding(14.dp))
        else if(buffering) Text(tr("Cargando…","Loading…"),color=Color.White,modifier=Modifier.align(Alignment.Center))
    }
}

/** Main-thread ownership ensures a fifth decoder is never started before old tiles release. */
internal class MultiPlayerPool {
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
