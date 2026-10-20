package com.miflix.native2.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.Text
import com.miflix.native2.data.*
import com.miflix.native2.model.MediaSummary
import com.miflix.native2.model.PartyRole
import com.miflix.native2.model.StreamChoice
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private data class NuvioLocationRP49(var folder:String="",var catalog:String="")
private val nuvioLocationsRP49=linkedMapOf<String,NuvioLocationRP49>()
private data class NuvioPageRP49(val time:Long,val rows:List<LiveChannel>)
private val nuvioPagesRP49=linkedMapOf<String,NuvioPageRP49>()

@Composable
internal fun NuvioTestScreenRP49(state:AppState,modifier:Modifier) {
    val context=LocalContext.current
    val owner="${state.session?.userId ?: "local"}:${state.activeProfile.id}"
    val folders=remember {runCatching {NuvioTestConfigRP49.folders(context)}.getOrDefault(emptyList())}
    val saved=remember(owner) {
        val value=nuvioLocationsRP49.remove(owner) ?: NuvioLocationRP49()
        nuvioLocationsRP49[owner]=value
        while(nuvioLocationsRP49.size>6) nuvioLocationsRP49.remove(nuvioLocationsRP49.keys.first())
        value
    }
    var folderId by remember(owner) {mutableStateOf(saved.folder)}
    var catalogId by remember(owner) {mutableStateOf(saved.catalog)}
    val folder=folders.firstOrNull {it.id==folderId}
    val catalog=folder?.catalogs?.firstOrNull {it.id==catalogId} ?: folder?.catalogs?.firstOrNull()
    val repo=remember {LiveRepository()}
    val scope=rememberCoroutineScope()
    var rows by remember {mutableStateOf<List<LiveChannel>>(emptyList())}
    var loading by remember {mutableStateOf(false)}
    var message by remember {mutableStateOf("")}
    var more by remember {mutableStateOf(false)}
    var offset by remember {mutableIntStateOf(0)}
    var generation by remember {mutableIntStateOf(0)}
    var retry by remember {mutableIntStateOf(0)}
    var pending by remember {mutableStateOf<LiveChannel?>(null)}
    fun backToSports() {saved.folder="";saved.catalog="";folderId="";catalogId=""}
    BackHandler(enabled=folder!=null) {backToSports()}
    LaunchedEffect(folderId,catalogId,retry) {
        generation++;val stamp=generation;pending=null;rows=emptyList();more=false;message=""
        val c=catalog ?: return@LaunchedEffect
        saved.folder=folderId;saved.catalog=c.id
        loading=true
        try {
            val key=c.type+":"+c.id
            val cache=nuvioPagesRP49[key]?.takeIf {retry==0 && System.currentTimeMillis()-it.time<300000L}
            rows=cache?.rows ?: repo.channels(NuvioTestConfigRP49.MANIFEST,c).distinctBy {it.type+":"+it.id}.also {
                nuvioPagesRP49.remove(key);nuvioPagesRP49[key]=NuvioPageRP49(System.currentTimeMillis(),it)
                while(nuvioPagesRP49.size>12) nuvioPagesRP49.remove(nuvioPagesRP49.keys.first())
            }
            offset=rows.size;more=rows.isNotEmpty()
        } catch(e:CancellationException) {throw e}
        catch(e:Exception) {
            message=if(e is HttpFailure && e.status==403) tr("La fuente rechazó la solicitud (HTTP 403). Puedes reintentar más tarde.","The source rejected the request (HTTP 403). You can retry later.")
                else tr("No se pudo cargar esta categoría. Reintenta.","Could not load this category. Try again.")
        } finally {if(stamp==generation) loading=false}
    }
    fun play(channel:LiveChannel,stream:StreamChoice) {
        if(state.partyRole==PartyRole.GUEST || state.kidsMode) return
        val c=catalog ?: return
        LiveChannelNavigationRP40.save(state,LiveBrowseSnapshotRP40(BrowserCategoryRP34(NuvioTestConfigRP49.PROVIDER,c,"Nuvio Test"),rows.map {BrowserChannelRP34(NuvioTestConfigRP49.PROVIDER,it)},offset=offset,more=more))
        state.openLivePlayer(PlayerRequest(MediaSummary(channel.id.hashCode(),"live",channel.name,channel.description,null,channel.poster,0.0,"REPLAY"),stream,0,0,0L,
            live=true,liveChannelId=channel.id,liveProvider=NuvioTestConfigRP49.PROVIDER,liveType=channel.type,returnScreen=Screen.LIVE_TV))
    }
    Column(modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Text(tr("REPETICIONES DEPORTIVAS","SPORTS REPLAYS"),color=Muted,fontSize=12.sp)
        if(folder==null) {
            LazyVerticalGrid(GridCells.Adaptive(230.dp),Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(14.dp),verticalArrangement=Arrangement.spacedBy(14.dp),contentPadding=PaddingValues(6.dp,6.dp,6.dp,24.dp)) {
                items(folders,key={it.id}) {f ->HubEventCardRP41(f.title,f.poster,"${f.catalogs.size} · "+tr("categorías","categories")) {
                    folderId=f.id;catalogId=f.catalogs.firstOrNull()?.id.orEmpty();saved.folder=folderId;saved.catalog=catalogId
                }}
                if(folders.isEmpty()) item(span={GridItemSpan(maxLineSpan)}) {Text(tr("No se pudo leer la colección importada.","Could not read the imported collection."),color=Color.White)}
            }
        } else {
            Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                FocusButton("‹ "+tr("Deportes","Sports")) {backToSports()}
                Text(folder.title,color=Color.White,fontSize=21.sp)
            }
            LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp),contentPadding=PaddingValues(4.dp)) {
                items(folder.catalogs,key={it.type+":"+it.id}) {c ->HubCategoryChipRP42(c.name,c.id==catalog?.id) {catalogId=c.id}}
            }
            if(loading) Text(tr("Cargando…","Loading…"),color=Muted)
            if(message.isNotBlank()) {Text(message,color=Muted,fontSize=13.sp);FocusButton(tr("Reintentar","Retry")) {retry++}}
            LazyVerticalGrid(GridCells.Adaptive(240.dp),Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(14.dp),verticalArrangement=Arrangement.spacedBy(14.dp),contentPadding=PaddingValues(6.dp,6.dp,6.dp,24.dp)) {
                items(rows,key={it.type+":"+it.id}) {channel ->HubEventCardRP41(channel.name,channel.poster,channel.description) {
                    if(state.partyRole==PartyRole.GUEST) message=tr("El host elige el contenido.","The host chooses the content.")
                    else if(!state.kidsMode) pending=channel
                }}
                if(!loading && rows.isEmpty() && message.isBlank()) item(span={GridItemSpan(maxLineSpan)}) {Text(tr("No hay repeticiones en esta categoría.","No replays in this category."),color=Muted)}
                if(more && !loading) item(span={GridItemSpan(maxLineSpan)}) {FocusButton(tr("Cargar más","Load more")) {
                    val c=catalog
                    if(c!=null) scope.launch {
                        val stamp=generation;loading=true
                        try {
                            val incoming=repo.channels(NuvioTestConfigRP49.MANIFEST,c,skip=offset)
                            if(stamp==generation) {
                                val old=rows.map {it.type+":"+it.id}.toSet()
                                val added=incoming.filter {it.type+":"+it.id !in old}.distinctBy {it.type+":"+it.id}
                                rows=rows+added;offset+=incoming.size;more=added.isNotEmpty()
                            }
                        } catch(e:CancellationException) {throw e}
                        catch(_:Exception) {if(stamp==generation) message=tr("No se pudo cargar más contenido.","Could not load more content.")}
                        finally {if(stamp==generation) loading=false}
                    }
                }}
            }
        }
    }
    pending?.let {channel ->LiveSportsLinksRP44(state,channel,{pending=null},manifestOverrideRP49=NuvioTestConfigRP49.MANIFEST) {stream ->pending=null;play(channel,stream)}}
}
