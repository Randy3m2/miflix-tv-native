package com.miflix.native2.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Text
import com.miflix.native2.data.*
import com.miflix.native2.model.MediaSummary
import com.miflix.native2.model.StreamChoice
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

private data class TVGroupRP32(val provider:String,val category:LiveCategory) {
    val key:String get()="$provider:${category.type}:${category.id}:${category.genre.orEmpty()}"
}
private data class TVChannelRP32(val provider:String,val channel:LiveChannel) {
    val key:String get()="$provider:${channel.type}:${channel.id}"
}

/** A single browse surface. Original provider/id survives for favorites and playback. */
@Composable
internal fun UnifiedLiveTVRP32(state:AppState,modifier:Modifier=Modifier) {
    val context=LocalContext.current
    val repo=remember {LiveRepository()}
    val scope=rememberCoroutineScope()
    val permits=remember {Semaphore(4)}
    var selected by remember {mutableStateOf("")}
    var query by remember {mutableStateOf(state.liveSearch)}
    var groups by remember {mutableStateOf<List<TVGroupRP32>>(emptyList())}
    var rows by remember {mutableStateOf<List<TVChannelRP32>>(emptyList())}
    var more by remember {mutableStateOf(false)}
    var busy by remember {mutableStateOf(false)}
    var message by remember {mutableStateOf("")}
    var generation by remember {mutableIntStateOf(0)}
    var retry by remember {mutableIntStateOf(0)}
    var chosen by remember {mutableStateOf<TVChannelRP32?>(null)}
    var links by remember {mutableStateOf<List<StreamChoice>>(emptyList())}
    val offsets=remember {mutableMapOf<String,Int>()}
    val finished=remember {mutableSetOf<String>()}
    fun manifest(provider:String)=state.calendarLiveManifest(provider)
    val active=remember(groups,selected) {groups.filter {it.key==selected}}
    suspend fun page(reset:Boolean) {
        val stamp=generation
        val requested=active
        if(reset) {offsets.clear();finished.clear();rows=emptyList()}
        busy=true;message=""
        try {
            var failed=0
            val batches=coroutineScope {requested.filter {it.key !in finished}.map {group ->async {
                try {
                    val result=permits.withPermit {repo.channels(manifest(group.provider),group.category,offsets[group.key] ?: 0,query)}
                    Triple(group,result,null as Exception?)
                } catch(e:CancellationException) {throw e} catch(e:Exception) {Triple(group,emptyList<LiveChannel>(),e)}
            }}.awaitAll()}
            if(stamp!=generation) return
            val incoming=mutableListOf<TVChannelRP32>()
            for((group,channels,error) in batches) {
                if(error!=null) {failed++;continue}
                offsets[group.key]=(offsets[group.key] ?: 0)+channels.size
                if(!group.category.paginated || channels.isEmpty()) finished.add(group.key)
                incoming.addAll(channels.map {TVChannelRP32(group.provider,it)})
            }
            val oldKeys=rows.map {it.key}.toSet()
            val next=incoming.distinctBy {it.key}.filter {it.key !in oldKeys}
            rows=rows+next
            // Do not repeat a broken/non-advancing source indefinitely.
            if(!reset && next.isEmpty()) requested.forEach {finished.add(it.key)}
            more=requested.any {it.key !in finished}
            if(failed>0) message=tr("Algunas fuentes no respondieron. Los demás canales siguen disponibles.","Some sources did not respond. Other channels remain available.")
            else if(rows.isEmpty()) message=tr("No se encontraron canales en esta selección.","No channels found in this selection.")
        } finally {if(stamp==generation) busy=false}
    }
    LaunchedEffect(retry) {
        busy=true;message="";groups=emptyList()
        val providers=listOf("nauta")
        var failures=0
        val loaded=coroutineScope {providers.map {provider ->async {
            try {permits.withPermit {repo.categories(manifest(provider)).map {TVGroupRP32(provider,it)}}}
            catch(e:CancellationException) {throw e} catch(_:Exception) {failures++;emptyList()}
        }}.awaitAll().flatten()}
        groups=loaded+IptvRepository.sources.map {(provider,source) ->
            TVGroupRP32(provider,LiveCategory("tv","iptv:all","IPTV · "+source.first,paginated=true,searchable=true))
        }
        selected=groups.firstOrNull {isMostViewedRP37(it.category)}?.key ?: groups.firstOrNull()?.key.orEmpty()
        busy=false
        if(failures>0) message=tr("Algunas fuentes no están disponibles.","Some sources are unavailable.")
    }
    LaunchedEffect(active,query) {
        generation++;chosen=null;links=emptyList()
        if(groups.isEmpty()) return@LaunchedEffect
        if(query.isNotBlank()) delay(350)
        page(true)
    }
    fun play(entry:TVChannelRP32,stream:StreamChoice) {
        active.firstOrNull()?.let {g ->LiveChannelNavigationRP40.save(state,LiveBrowseSnapshotRP40(
            BrowserCategoryRP34(g.provider,g.category,if(g.provider=="nauta") "Nauta" else "IPTV"),
            rows.filter {query.isBlank() || IptvPlaylist.searchKey(it.channel.name).contains(IptvPlaylist.searchKey(query)) || it.channel.number.toString()==query.trim()}.map {BrowserChannelRP34(it.provider,it.channel)},query,offsets[g.key] ?: rows.size,more))}
        chosen=null;links=emptyList()
        val c=entry.channel
        state.openLivePlayer(PlayerRequest(MediaSummary(c.id.hashCode(),"live",c.name,c.description,null,c.poster,0.0,tr("EN VIVO","LIVE")),stream,0,0,0L,
            live=true,liveChannelId=c.id,liveProvider=entry.provider,liveType=c.type))
    }
    fun openChannel(entry:TVChannelRP32) {
        if(busy) return
        val stamp=generation
        scope.launch {
            busy=true
            try {
                val found=repo.streams(manifest(entry.provider),entry.channel)
                if(stamp!=generation) return@launch
                when {found.isEmpty()->message=tr("El canal no devolvió enlaces.","No links returned for this channel.");found.size==1->play(entry,found.first());else->{chosen=entry;links=found}}
            } catch(e:CancellationException) {throw e} catch(_:Exception) {message=tr("No se pudo abrir el canal.","Could not open this channel.")}
            finally {if(stamp==generation) busy=false}
        }
    }
    Column(modifier.fillMaxWidth().padding(top=12.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Text(tr("CATEGORÍAS","CATEGORIES"),color=Muted)
        LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp),contentPadding=PaddingValues(horizontal=4.dp,vertical=4.dp)) {
            items(groups,key={it.key}) {g ->FocusButton(g.category.name,primary=selected==g.key) {selected=g.key}}
        }
        NativeTextField(query,{query=it;state.liveSearch=it},tr("Buscar canales","Search channels"),modifier=Modifier.fillMaxWidth())
        if(busy) Text(tr("Cargando canales…","Loading channels…"),color=Color.White)
        if(message.isNotBlank()) Text(message,color=Color.White)
        if(!busy && groups.isEmpty()) FocusButton(tr("Reintentar","Retry")) {retry++}
        val visible=rows.filter {query.isBlank() || IptvPlaylist.searchKey(it.channel.name).contains(IptvPlaylist.searchKey(query)) || it.channel.number.toString()==query.trim()}
        LazyVerticalGrid(GridCells.Adaptive(180.dp),modifier=Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(14.dp),verticalArrangement=Arrangement.spacedBy(14.dp),contentPadding=PaddingValues(start=4.dp,end=4.dp,top=6.dp,bottom=48.dp)) {
            items(visible,key={it.key}) {entry ->
                val revision=LiveLibrary.revision
                val favorite=remember(revision,state.activeProfile.id,state.session?.userId,entry.key) {LiveLibrary.favorites(context,state).any {it.provider==entry.provider && it.channel.id==entry.channel.id}}
                LiveChannelTile(entry.channel,entry.channel.number.takeIf {it>0} ?: (rows.indexOf(entry)+1),favorite,{openChannel(entry)}) {LiveLibrary.toggle(context,state,entry.provider,entry.channel)}
            }
            if(more && !busy) item(span={GridItemSpan(maxLineSpan)}) {FocusButton(tr("Cargar más canales","Load more channels")) {scope.launch {page(false)}}}
        }
    }
    if(links.isNotEmpty()) Dialog(onDismissRequest={links=emptyList();chosen=null},properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Column(Modifier.fillMaxSize().background(Bg).padding(30.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text(chosen?.channel?.name.orEmpty(),color=Color.White)
            FocusButton(tr("Cerrar","Close")) {links=emptyList();chosen=null}
            androidx.compose.foundation.lazy.LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp)) {items(links) {stream ->FocusButton(stream.name+" · "+stream.title) {chosen?.let {play(it,stream)}}}}
        }
    }
}
