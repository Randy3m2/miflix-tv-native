package com.miflix.native2.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.miflix.native2.data.*

/** One decoder pool, at most four streams, and exactly one audible tile. */
@Composable
internal fun MobileMultiViewRP133(state:AppState) {
    val pool=remember(state.localExtrasScope()) {MultiPlayerPool()}
    DisposableEffect(pool) {onDispose {pool.releaseAll()}}
    val portrait=LocalConfiguration.current.orientation==android.content.res.Configuration.ORIENTATION_PORTRAIT
    var sound by remember {mutableStateOf(state.multiViewRequests.firstOrNull()?.playbackId)}
    var selected by remember {mutableStateOf<PlayerRequest?>(null)}
    var adding by remember {mutableStateOf(false)}
    var replacing by remember {mutableStateOf<String?>(null)}
    var stats by remember {mutableStateOf<String?>(null)}
    var choosing by remember {mutableStateOf(false)}
    var forceF1 by remember {mutableStateOf(false)}
    var game by remember {mutableStateOf<LiveScoreRP11?>(null)}
    var hd by remember {mutableStateOf(false)}
    val repo=remember {SportsStatsRP95()}
    val f1=remember {F1StatsRP131()}
    val videoFocus=remember {FocusRequester()}
    val target=state.multiViewRequests.firstOrNull {it.playbackId==stats}
    fun finish() {pool.releaseAll();state.multiViewRequests.clear();state.playerRequest=null;state.screen=Screen.LIVE_TV}
    BackHandler {finish()}
    LaunchedEffect(state.multiViewRequests.map {it.playbackId}) {
        if(state.multiViewRequests.none {it.playbackId==sound}) sound=state.multiViewRequests.firstOrNull()?.playbackId
        if(state.multiViewRequests.size>=4 || state.multiViewRequests.none {it.playbackId==stats}) stats=null
    }
    if(adding) MobileSheetRP133(tr("Agregar canal o juego","Add channel or game"),{adding=false;replacing=null}) {
        MobileLiveRP133(state,{picked->
            val request=picked.copy(playbackId=java.util.UUID.randomUUID().toString())
            val old=replacing
            if(old!=null) {
                val index=state.multiViewRequests.indexOfFirst {it.playbackId==old}
                if(index>=0) {pool.release(old);state.multiViewRequests[index]=request;if(sound==old) sound=request.playbackId}
            } else if(state.multiViewRequests.size<4) state.multiViewRequests.add(request)
            adding=false;replacing=null
        })
    }
    selected?.let {req->MobileSheetRP133(req.item.title,{selected=null}) {
        MobileButtonRP133(tr("Escuchar","Listen"),"volume",Modifier.fillMaxWidth(),selected=sound==req.playbackId) {sound=req.playbackId;selected=null}
        MobileButtonRP133("Stats","stats",Modifier.fillMaxWidth()) {stats=req.playbackId;game=null;forceF1=false;selected=null}
        MobileButtonRP133(tr("Reintentar señal","Retry stream"),"refresh",Modifier.fillMaxWidth()) {
            val index=state.multiViewRequests.indexOfFirst {it.playbackId==req.playbackId}
            if(index>=0) {
                val fresh=req.copy(playbackId=java.util.UUID.randomUUID().toString())
                state.multiViewRequests[index]=fresh
                if(sound==req.playbackId) sound=fresh.playbackId
            }
            selected=null
        }
        MobileButtonRP133(tr("Cambiar canal","Change channel"),"link",Modifier.fillMaxWidth()) {replacing=req.playbackId;selected=null;adding=true}
        MobileButtonRP133(tr("Pantalla completa","Full screen"),"full",Modifier.fillMaxWidth()) {pool.releaseAll();state.multiViewRequests.clear();selected=null;state.openLivePlayer(req)}
        MobileButtonRP133(tr("Quitar pantalla","Remove screen"),"close",Modifier.fillMaxWidth()) {pool.release(req.playbackId);state.multiViewRequests.removeAll {it.playbackId==req.playbackId};selected=null;if(state.multiViewRequests.isEmpty()) finish()}
    }}
    if(choosing && target!=null) MobileSheetRP133(tr("Seleccionar partido","Select game"),{choosing=false}) {
        MobileButtonRP133("F1",modifier=Modifier.fillMaxWidth()) {forceF1=true;choosing=false}
        SportsStatsPanelRP95(state,target.item.title,repo,game,{game=it;forceF1=false;choosing=false},{choosing=false},videoFocus,Modifier.weight(1f),listOf("NBA","MLB","LIDOM"),true)
    }
    Column(Modifier.fillMaxSize().background(Color.Black)) {
        Row(Modifier.fillMaxWidth().padding(8.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            MobileIconButtonRP133("back",tr("Salir de SplitScreen","Exit SplitScreen")) {finish()}
            LazyRow(Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                if(state.multiViewRequests.size<4) item {MobileButtonRP133(tr("Agregar","Add"),"plus") {adding=true}}
                item {MobileButtonRP133("HD",selected=hd) {hd=!hd}}
                item {MobileButtonRP133("Stats","stats",selected=target!=null,enabled=state.multiViewRequests.size<4) {if(target!=null) stats=null else {stats=state.multiViewRequests.firstOrNull()?.playbackId;game=null}}}
            }
        }
        val requests=state.multiViewRequests.toList()
        if(portrait && requests.size<=2) {
            requests.forEachIndexed {index,req->key(req.playbackId) {MultiViewTile(req,sound==req.playbackId,hd,pool,Modifier.fillMaxWidth().aspectRatio(16f/9f),index) {selected=req}}}
            if(target!=null) MobileSplitStatsRP133(state,target,repo,f1,game,{game=it},{choosing=true},{stats=null},videoFocus,forceF1,Modifier.weight(1f))
            else Spacer(Modifier.weight(1f))
        } else if(!portrait && requests.size<=2 && target!=null) {
            Row(Modifier.weight(1f).fillMaxWidth()) {
                Row(Modifier.weight(2f).fillMaxHeight()) {
                    requests.forEachIndexed {index,req->key(req.playbackId) {MultiViewTile(req,sound==req.playbackId,hd,pool,Modifier.weight(1f).fillMaxHeight(),index) {selected=req}}}
                }
                MobileSplitStatsRP133(state,target,repo,f1,game,{game=it},{choosing=true},{stats=null},videoFocus,forceF1,Modifier.weight(1f).fillMaxHeight())
            }
        } else {
            Column(Modifier.weight(1f)) {
                requests.chunked(2).forEachIndexed {row,entries->
                    Row(Modifier.weight(1f)) {
                        entries.forEachIndexed {col,req->key(req.playbackId) {MultiViewTile(req,sound==req.playbackId,hd,pool,Modifier.weight(1f).fillMaxHeight(),row*2+col) {selected=req}}}
                        if(entries.size==1 && requests.size==3 && target!=null) MobileSplitStatsRP133(state,target,repo,f1,game,{game=it},{choosing=true},{stats=null},videoFocus,forceF1,Modifier.weight(1f).fillMaxHeight())
                    }
                }
            }
            if(target!=null && requests.size!=3 && requests.size<4) MobileSplitStatsRP133(state,target,repo,f1,game,{game=it},{choosing=true},{stats=null},videoFocus,forceF1,Modifier.fillMaxWidth().height(220.dp))
        }
    }
}

@Composable
private fun MobileSplitStatsRP133(state:AppState,req:PlayerRequest,repo:SportsStatsRP95,f1:F1StatsRP131,game:LiveScoreRP11?,select:(LiveScoreRP11)->Unit,choose:()->Unit,close:()->Unit,focus:FocusRequester,forceF1:Boolean,modifier:Modifier) {
    if(forceF1 || F1StatsRP131.channel(req.liveProvider,req.item.title+" "+req.item.overview)) F1StatsPanelRP131(state,f1,true,close,focus,modifier)
    else SplitStatsFullRP130(state,req,repo,game,select,choose,close,focus,modifier)
}
