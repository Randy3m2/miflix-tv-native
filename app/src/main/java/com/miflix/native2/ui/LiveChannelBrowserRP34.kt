package com.miflix.native2.ui

import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.miflix.native2.data.*
import com.miflix.native2.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

private val browserCategoryCacheRP34=PlaybackLinkCache<String,List<LiveCategory>>(12) {System.currentTimeMillis()}

internal data class BrowserCategoryRP34(val provider:String,val value:LiveCategory,val sourceLabel:String) {
    val key:String get()="$provider:${value.type}:${value.id}:${value.genre.orEmpty()}"
}
internal data class BrowserChannelRP34(val provider:String,val value:LiveChannel) {
    val key:String get()="$provider:${value.type}:${value.id}"
}

/** Shared TV remote picker for player Channels and multiview add/replace. */
@Composable
internal fun LiveChannelBrowserRP34(state:AppState,initialProvider:String,select:(PlayerRequest)->Unit,close:()->Unit,compactRP43:Boolean=false) {
    val current=state.playerRequest?.takeIf {it.live}
    val ownerRP40=remember {state.session?.userId to state.activeProfile.id}
    val restoredRP40=remember {LiveChannelNavigationRP40.matching(state,current)}
    var restorePendingRP40 by remember {mutableStateOf(restoredRP40!=null)}
    val context=LocalContext.current
    val repo=remember {LiveRepository()}
    val scope=rememberCoroutineScope()
    val permits=remember {Semaphore(4)}
    val first=remember {FocusRequester()}
    val firstChannel=remember {FocusRequester()}
    val playingChannelFocus=remember {FocusRequester()}
    val categoryListRP40=rememberLazyListState()
    val channelList=rememberLazyListState(restoredRP40?.scrollIndex ?: 0,restoredRP40?.scrollOffset ?: 0)
    var initialFocus by remember {mutableStateOf(true)}
    var categories by remember {mutableStateOf<List<BrowserCategoryRP34>>(emptyList())}
    var selected by remember {mutableStateOf("")}
    var channels by remember {mutableStateOf<List<BrowserChannelRP34>>(emptyList())}
    var search by remember {mutableStateOf(restoredRP40?.search.orEmpty())}
    var loading by remember {mutableStateOf(true)}
    var resolving by remember {mutableStateOf(false)}
    var message by remember {mutableStateOf("")}
    var more by remember {mutableStateOf(false)}
    var offset by remember {mutableIntStateOf(0)}
    var generation by remember {mutableIntStateOf(0)}
    var catalogWarningRP39 by remember {mutableStateOf("")}
    var retry by remember {mutableIntStateOf(0)}
    val pageCache=remember {mutableMapOf<String,Pair<List<BrowserChannelRP34>,Boolean>>()}
    val revision=LiveLibrary.revision
    val favorites=remember(revision,state.session?.userId,state.activeProfile.id) {LiveLibrary.favorites(context,state)}
    val group=categories.firstOrNull {it.key==selected}
    LaunchedEffect(retry) {
        loading=true;message="";catalogWarningRP39="";categories=emptyList();selected=""
        val providers=listOf(if(initialProvider=="sports") "sports" else "nauta")
        val found=coroutineScope {providers.map {provider ->async {
            try {permits.withPermit {
                val manifest=state.calendarLiveManifest(provider)
                val catalog=browserCategoryCacheRP34.get(manifest) ?: repo.categories(manifest).also {browserCategoryCacheRP34.put(manifest,it)}
                catalog.map {BrowserCategoryRP34(provider,it,if(provider=="nauta") "Nauta" else if(provider=="sports") tr("Deportes","Sports") else IptvRepository.sources[provider]?.first.orEmpty())}
            }} catch(e:CancellationException) {throw e} catch(_:Exception) {emptyList()}
        }}.awaitAll().flatten()}
        categories=(found+IptvRepository.sources.map {(provider,source) ->
            BrowserCategoryRP34(provider,LiveCategory("tv","iptv:all","IPTV · "+source.first,paginated=true,searchable=true),"")
        }).distinctBy {it.key}
        selected=if(restoredRP40?.category?.value?.id=="favorites") "favorites" else categories.firstOrNull {it.key==restoredRP40?.category?.key}?.key
            ?: categories.firstOrNull {it.provider==current?.liveProvider && current?.liveProvider in IptvRepository.sources}?.key
            ?: categories.firstOrNull {isMostViewedRP37(it.value)}?.key ?: categories.firstOrNull()?.key ?: "favorites"
        categoryListRP40.scrollToItem((categories.indexOfFirst {it.key==selected}+1).coerceAtLeast(0))
        if(!initialFocus) {withFrameNanos {};runCatching {first.requestFocus()}}
        // Keep the initial provider usable while the other catalog loads. Only metadata:
        // event lists and playback URLs are requested when the user selects them.
        val otherProvider=if(initialProvider=="sports") "nauta" else "sports"
        try {
            val manifest=state.calendarLiveManifest(otherProvider)
            val catalog=browserCategoryCacheRP34.get(manifest)
                ?: repo.categories(manifest).also {browserCategoryCacheRP34.put(manifest,it)}
            val extra=catalog.map {BrowserCategoryRP34(otherProvider,it,if(otherProvider=="sports") tr("Deportes","Sports") else "Nauta")}
            categories=(categories.filter {it.provider !in IptvRepository.sources}+extra+
                categories.filter {it.provider in IptvRepository.sources}).distinctBy {it.key}
        } catch(e:CancellationException) {throw e} catch(_:Exception) {
            catalogWarningRP39=if(otherProvider=="sports") tr("No se pudieron cargar las categorías de deportes.","Could not load sports categories.")
                else tr("No se pudieron cargar las categorías de TV.","Could not load TV categories.")
        }
    }
    LaunchedEffect(selected,search) {
        generation++;resolving=false;more=false;channels=emptyList();offset=0;message=""
        if(selected.isBlank()) return@LaunchedEffect
        if(selected=="favorites") {loading=false;return@LaunchedEffect}
        val c=group ?: return@LaunchedEffect
        loading=true
        try {
            val restored=restoredRP40
            if(restorePendingRP40 && restored!=null && c.key==restored.category.key && search==restored.search) {
                channels=restored.rows;offset=restored.offset;more=restored.more;restorePendingRP40=false
                return@LaunchedEffect
            }
            // Focusing categories switches lists; only settled focus sends a request.
            delay(if(search.isBlank()) 180 else 350)
            val cacheKey=c.key+"|"+search
            val cached=pageCache[cacheKey]
            if(cached!=null) {channels=cached.first;more=cached.second;offset=channels.size}
            else {
                val rows=repo.channels(state.calendarLiveManifest(c.provider),c.value,search=search)
                channels=rows.distinctBy {it.type+":"+it.id}.map {BrowserChannelRP34(c.provider,it)}
                offset=rows.size;more=c.value.paginated && rows.isNotEmpty()
                if(pageCache.size>=32) pageCache.remove(pageCache.keys.first())
                pageCache[cacheKey]=channels to more
            }
        } catch(e:CancellationException) {throw e} catch(_:Exception) {message=tr("No se pudo cargar esta categoría.","Could not load this category.")}
        finally {loading=false}
    }
    val playing=current?.takeIf {r -> restoredRP40==null && group?.provider==r.liveProvider && search.isBlank() && (group?.value?.genre==null || group?.value?.let {isMostViewedRP37(it)}==true)}?.let {r ->
        BrowserChannelRP34(r.liveProvider,LiveChannel(r.liveChannelId,r.liveType,r.item.title,r.item.backdrop,r.item.overview))
    }
    val displayed=(if(selected=="favorites") favorites.map {BrowserChannelRP34(it.provider,it.channel)} else listOfNotNull(playing)+channels).distinctBy {it.key}.filter {
        search.isBlank() || IptvPlaylist.searchKey(it.value.name).contains(IptvPlaylist.searchKey(search)) || it.value.number.toString()==search.trim()
    }
    LaunchedEffect(displayed.map {it.key},loading) {
        if(initialFocus && !loading) {
            val index=displayed.indexOfFirst {it.provider==current?.liveProvider && it.value.id==current?.liveChannelId}
            if(index>=0) {channelList.scrollToItem(index);withFrameNanos {};runCatching {playingChannelFocus.requestFocus()}}
            else {withFrameNanos {};runCatching {first.requestFocus()}}
            initialFocus=false
        }
    }
    val lastBrowseRP40 by rememberUpdatedState(group?.let {c ->
        LiveBrowseSnapshotRP40(c,displayed,search,offset,more,channelList.firstVisibleItemIndex,channelList.firstVisibleItemScrollOffset)
    })
    DisposableEffect(Unit) {
        onDispose {
            lastBrowseRP40?.takeIf {snap -> ownerRP40==(state.session?.userId to state.activeProfile.id) && snap.rows.any {it.provider==current?.liveProvider && it.value.id==current?.liveChannelId}}
                ?.let {LiveChannelNavigationRP40.save(state,it)}
        }
    }
    var pendingSportsRP44 by remember {mutableStateOf<BrowserChannelRP34?>(null)}
    fun selectSportsRP44(entry:BrowserChannelRP34,stream:StreamChoice) {
        val c=entry.value
        val activeCategory=group ?: BrowserCategoryRP34(entry.provider,LiveCategory(c.type,"sports","Sports"),"Sports")
        LiveChannelNavigationRP40.save(state,LiveBrowseSnapshotRP40(activeCategory,displayed,search,offset,more,channelList.firstVisibleItemIndex,channelList.firstVisibleItemScrollOffset))
        select(PlayerRequest(MediaSummary(c.id.hashCode(),"live",c.name,c.description,null,c.poster,0.0,"LIVE"),stream,0,0,0L,live=true,liveChannelId=c.id,liveProvider=entry.provider,liveType=c.type))
    }
    fun tune(entry:BrowserChannelRP34) {
        if(resolving) return
        if(entry.provider=="sports") {pendingSportsRP44=entry;return}
        val stamp=generation
        scope.launch {
            resolving=true;message=""
            try {
                val stream=repo.streams(state.calendarLiveManifest(entry.provider),entry.value).firstOrNull()
                    ?: error("No streams")
                if(stamp!=generation) return@launch
                val c=entry.value
                val activeCategory=group ?: BrowserCategoryRP34(entry.provider,LiveCategory(entry.value.type,"favorites","Favorites"),"")
                LiveChannelNavigationRP40.save(state,LiveBrowseSnapshotRP40(activeCategory,displayed,search,offset,more,channelList.firstVisibleItemIndex,channelList.firstVisibleItemScrollOffset))
                select(PlayerRequest(MediaSummary(c.id.hashCode(),"live",c.name,c.description,null,c.poster,0.0,"LIVE"),stream,0,0,0L,
                    live=true,liveChannelId=c.id,liveProvider=entry.provider,liveType=c.type))
            } catch(e:CancellationException) {throw e} catch(_:Exception) {if(stamp==generation) message=tr("No se pudo abrir el canal. Prueba otro.","Could not open the channel. Try another.")}
            finally {if(stamp==generation) resolving=false}
        }
    }
    PlayerMenuDialog(onDismissRequest=close,fullScreen=true) {
        BoxWithConstraints(Modifier.fillMaxSize().consumeOpeningPress()) {
            val width=if(compactRP43) {if(maxWidth<700.dp) .82f else .60f} else if(maxWidth<700.dp) .82f else .64f
            Row(Modifier.align(if(compactRP43) Alignment.CenterEnd else Alignment.CenterStart).fillMaxHeight(if(compactRP43) .88f else 1f).fillMaxWidth(width).widthIn(max=740.dp).clip(RoundedCornerShape(if(compactRP43) 18.dp else 0.dp)).background(Color(0xEF080808)),horizontalArrangement=Arrangement.spacedBy(1.dp)) {
                Column(Modifier.weight(.36f).fillMaxHeight().onPreviewKeyEvent {event ->
                    if(event.nativeKeyEvent.keyCode==android.view.KeyEvent.KEYCODE_DPAD_RIGHT && displayed.isNotEmpty()) {
                        if(event.type==KeyEventType.KeyDown) scope.launch {
                            channelList.scrollToItem(0);withFrameNanos {};runCatching {firstChannel.requestFocus()}
                        };true
                    } else false
                }.background(Color(0xF00C0C0C)).padding(12.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                    Text(tr("CATEGORÍAS","CATEGORIES"),color=Muted,fontSize=12.sp)
                    LazyColumn(Modifier.weight(1f),state=categoryListRP40,verticalArrangement=Arrangement.spacedBy(5.dp),contentPadding=PaddingValues(bottom=18.dp)) {
                        item(key="favorites") {BrowserCategoryRowRP34(tr("♥ Favoritos","♥ Favorites"),selected=="favorites",Modifier.then(if(selected=="favorites") Modifier.focusRequester(first) else Modifier),{selected="favorites"})}
                        items(categories,key={it.key}) {c ->
                            BrowserCategoryRowRP34(c.value.name,selected==c.key,
                                Modifier.then(if(c.key==selected) Modifier.focusRequester(first) else Modifier),
                                {selected=c.key},c.sourceLabel)
                        }
                    }
                    if(catalogWarningRP39.isNotBlank()) {
                        Text(catalogWarningRP39,color=Muted,fontSize=10.sp)
                        FocusButton(tr("Reintentar","Retry"),modifier=Modifier.fillMaxWidth()) {retry++}
                    }
                    FocusButton(tr("Cerrar","Close"),modifier=Modifier.fillMaxWidth(),onClick=close)
                }
                Column(Modifier.weight(.64f).fillMaxHeight().padding(horizontal=16.dp,vertical=14.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                    Text(if(selected=="favorites") tr("Favoritos","Favorites") else group?.value?.name ?: tr("Canales","Channels"),color=Color.White,fontSize=21.sp,fontWeight=FontWeight.Bold)
                    NativeTextField(search,{search=it},tr("Buscar canal","Search channels"),modifier=Modifier.fillMaxWidth())
                    if(loading || resolving) Text(if(resolving) tr("Abriendo canal…","Opening channel…") else tr("Cargando…","Loading…"),color=Muted,fontSize=12.sp)
                    if(message.isNotBlank()) Text(message,color=Color.White,fontSize=12.sp)
                    LazyColumn(Modifier.weight(1f),state=channelList,verticalArrangement=Arrangement.spacedBy(2.dp),contentPadding=PaddingValues(bottom=16.dp)) {
                        items(displayed,key={it.key}) {entry ->
                            val favorite=favorites.any {it.provider==entry.provider && it.channel.id==entry.value.id}
                            BrowserChannelRowRP34(entry.value,entry.value.number.takeIf {it>0} ?: (displayed.indexOf(entry)+1),favorite,
                                playing=entry.provider==current?.liveProvider && entry.value.id==current?.liveChannelId,
                                click={tune(entry)},hold={if(entry.provider=="sports") tune(entry) else LiveLibrary.toggle(context,state,entry.provider,entry.value)},
                                modifier=Modifier.then(if(entry==displayed.firstOrNull()) Modifier.focusRequester(firstChannel) else Modifier)
                                    .then(if(entry.provider==current?.liveProvider && entry.value.id==current?.liveChannelId) Modifier.focusRequester(playingChannelFocus) else Modifier).onPreviewKeyEvent {event ->
                                    if(event.nativeKeyEvent.keyCode==android.view.KeyEvent.KEYCODE_DPAD_LEFT) {if(event.type==KeyEventType.KeyDown) runCatching {first.requestFocus()};true} else false
                                })
                        }
                        if(displayed.isEmpty() && !loading) item {Text(tr("No hay canales en esta selección.","No channels in this selection."),color=Muted)}
                        if(more && !loading && selected!="favorites") item {
                            FocusButton(tr("Cargar más","Load more"),modifier=Modifier.fillMaxWidth()) {
                                val c=group
                                if(c!=null && !loading) scope.launch {
                                    val stamp=generation;val query=search;loading=true
                                    try {
                                        val added=repo.channels(state.calendarLiveManifest(c.provider),c.value,offset,query)
                                        if(stamp!=generation) return@launch
                                        val previous=channels.size
                                        channels=(channels+added.map {BrowserChannelRP34(c.provider,it)}).distinctBy {it.key}
                                        offset+=added.size;more=added.isNotEmpty() && channels.size>previous
                                        // Evict page-one cache after paging: its offset no longer describes this list.
                                        pageCache.remove(c.key+"|"+query)
                                    } catch(e:CancellationException) {throw e} catch(_:Exception) {if(stamp==generation) message=tr("No se pudieron cargar más canales.","Could not load more channels.")}
                                    finally {if(stamp==generation) loading=false}
                                }
                            }
                        }
                    }
                    Text(if(group?.provider=="sports") tr("OK / Mantén OK · Enlaces    Atrás · Cerrar","OK / Hold OK · Links    Back · Close") else tr("Mantén OK · ♥ Favorito    Atrás · Cerrar","Hold OK · ♥ Favorite    Back · Close"),color=Muted,fontSize=10.sp)
                }
            }
        }
    }
    pendingSportsRP44?.let {entry ->LiveSportsLinksRP44(state,entry.value,{pendingSportsRP44=null}) {stream ->pendingSportsRP44=null;selectSportsRP44(entry,stream)}}

}

@Composable
private fun BrowserCategoryRowRP34(title:String,selected:Boolean,modifier:Modifier,onSelect:()->Unit,source:String="",activateOnly:Boolean=false) {
    var focused by remember {mutableStateOf(false)}
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(9.dp)).background(if(focused) Color.White else if(selected) Color(0xFF292929) else Color.Transparent)
        .onFocusChanged {focused=it.isFocused;if(it.isFocused && !activateOnly) onSelect()}.focusable().tvClick(onSelect).clickable(onClick=onSelect).padding(horizontal=12.dp,vertical=12.dp)) {
        Text(title,color=if(focused) Color.Black else Color.White,fontSize=14.sp,maxLines=2)
        if(source.isNotBlank()) Text(source,color=if(focused) Color.DarkGray else Muted,fontSize=10.sp)
    }
}

@Composable
private fun BrowserChannelRowRP34(channel:LiveChannel,number:Int,favorite:Boolean,playing:Boolean,click:()->Unit,hold:()->Unit,modifier:Modifier=Modifier) {
    var focused by remember(channel.id) {mutableStateOf(false)}
    Row(modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(if(focused) Color(0xFF292929) else Color.Transparent)
        .border(if(focused) 1.dp else 0.dp,if(focused) Color.White else Color.Transparent,RoundedCornerShape(8.dp))
        .onFocusChanged {focused=it.isFocused}.focusable().tvPlaybackClick(click,hold).clickable(onClick=click).padding(horizontal=10.dp,vertical=9.dp),
        verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(40.dp),contentAlignment=Alignment.Center) {
            if(channel.poster!=null) AsyncImage(channel.poster,null,Modifier.fillMaxSize(),contentScale=ContentScale.Fit)
            else Text(channel.name.take(2),color=Color.White,fontSize=16.sp)
        }
        Text(number.toString(),color=Muted,fontSize=12.sp)
        Column(Modifier.weight(1f)) {
            Text(channel.name,color=Color.White,fontWeight=FontWeight.Bold,fontSize=14.sp,maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            Text(if(playing) tr("▶ Reproduciendo","▶ Now playing") else tr("EN VIVO","LIVE"),color=if(playing) Color.White else Muted,fontSize=10.sp)
        }
        ChannelHeartRP38(favorite)
    }
}

@Composable
internal fun LiveFavoriteRP36(state:AppState,request:PlayerRequest,onInteraction:()->Unit) {
    val context=LocalContext.current
    val revision=LiveLibrary.revision
    val favorite=remember(revision,state.activeProfile.id,state.session?.userId,request.liveProvider,request.liveChannelId) {
        LiveLibrary.favorites(context,state).any {it.provider==request.liveProvider && it.channel.id==request.liveChannelId}
    }
    var focused by remember {mutableStateOf(false)}
    val action=tr(if(favorite) "Quitar de favoritos" else "Agregar a favoritos",if(favorite) "Remove from favorites" else "Add to favorites")
    val toggle={
        onInteraction()
        LiveLibrary.toggle(context,state,request.liveProvider,LiveChannel(request.liveChannelId,request.liveType,request.item.title,request.item.backdrop,request.item.overview))
    }
    Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
        .background(if(focused) Color(0xFF303030) else Color.Transparent)
        .border(if(focused) 1.dp else 0.dp,if(focused) Color.White else Color.Transparent,RoundedCornerShape(12.dp))
        .semantics {contentDescription=action;stateDescription=if(favorite) "Favorito" else "Sin marcar"}
        .onFocusChanged {focused=it.isFocused;if(it.isFocused) onInteraction()}
        .focusable().tvClick(toggle).clickable(onClick=toggle),contentAlignment=Alignment.Center) {
        ChannelHeartRP38(favorite)
    }
}

/** Match the provider category without assuming accent/case/spelling of its id. */
internal fun isMostViewedRP37(category:LiveCategory):Boolean {
    val title=java.text.Normalizer.normalize(category.name,java.text.Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"),"").lowercase(java.util.Locale.ROOT)
    return title.contains("mas vistos") || title.contains("most viewed") || title.contains("most watched")
}

/** Drawn icon: independent of TV fonts, emoji substitution, and button text ellipsis. */
@Composable
private fun ChannelHeartRP38(favorite:Boolean) {
    val heart=remember {Path().apply {
        moveTo(12f,21f)
        cubicTo(10f,19.2f,2f,13.5f,2f,7.5f)
        cubicTo(2f,2f,8.8f,0.8f,12f,5.3f)
        cubicTo(15.2f,0.8f,22f,2f,22f,7.5f)
        cubicTo(22f,13.5f,14f,19.2f,12f,21f)
        close()
    }}
    Canvas(Modifier.size(24.dp)) {
        withTransform({scale(size.width/24f,size.height/24f,pivot=Offset.Zero)}) {
            drawPath(heart,if(favorite) Color(0xFFFF3040) else Color.White)
        }
    }
}
