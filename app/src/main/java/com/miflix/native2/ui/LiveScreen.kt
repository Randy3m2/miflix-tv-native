package com.miflix.native2.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.focusGroup
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import java.text.Normalizer
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Text
import com.miflix.native2.data.*
import com.miflix.native2.model.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import android.content.Context

@Composable
fun LiveScreen(state: AppState) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("miflix_live", Context.MODE_PRIVATE) }
    val repo = remember { LiveRepository() }
    val scope = rememberCoroutineScope()
    var provider by remember { mutableStateOf(state.liveProviderTab) }
    var showFavorites by remember { mutableStateOf(false) }
    var showGuideSettings by remember { mutableStateOf(false) }
    val guide=rememberLiveGuide()
    val sports=provider=="sports"
    val iptv=provider.startsWith("iptv")
    var refreshVersion by remember { mutableStateOf(0) }
    var sportsManifest by remember { mutableStateOf(prefs.getString("sports_manifest", "https://sportsfree-us2.highfly.to/manifest.json").orEmpty()) }
    var editManifest by remember { mutableStateOf(false) }
    val manifest = if(sports) sportsManifest else state.calendarLiveManifest(provider)
    var categories by remember { mutableStateOf<List<LiveCategory>>(emptyList()) }
    var category by remember { mutableStateOf<LiveCategory?>(null) }
    var channels by remember { mutableStateOf<List<LiveChannel>>(emptyList()) }
    var message by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var sourceChannel by remember { mutableStateOf<LiveChannel?>(null) }
    var sources by remember { mutableStateOf<List<StreamChoice>>(emptyList()) }
    var search by remember { mutableStateOf(state.liveSearch) }
    fun channelNumber(channel: LiveChannel): Int {
        if(channel.number>0) return channel.number
        val key = "channel_${manifest}_${channel.id}"
        val existing = prefs.getInt(key, 0)
        if (existing > 0) return existing
        val counterKey = "next_number_$manifest"
        val next = prefs.getInt(counterKey, 1)
        prefs.edit().putInt(key, next).putInt(counterKey, next + 1).apply()
        return next
    }
    var offset by remember { mutableStateOf(0) }
    var canLoadMore by remember { mutableStateOf(false) }
    var generation by remember { mutableStateOf(0) }

    LaunchedEffect(manifest,refreshVersion) {
        categories = emptyList(); category = null; channels = emptyList(); message = ""; loading = true
        try {
            categories = repo.categories(manifest)
            category = categories.firstOrNull { it.id==state.liveCategoryId } ?: categories.firstOrNull()
            if (categories.isEmpty()) message = tr("Este complemento no tiene catálogos en vivo.","This add-on has no live catalogs.")
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            message = if (sports) "Sports service unavailable. Open Configure in your browser and paste its generated manifest. ${e.message}" else e.message.orEmpty()
        } finally { loading = false }
    }
    LaunchedEffect(manifest, category, search) {
        generation++;canLoadMore=false;sources=emptyList();sourceChannel=null
        val selected = category ?: return@LaunchedEffect
        loading = true; message = ""; channels = emptyList(); offset = 0
        try {
            if (search.isNotBlank()) delay(350)
            channels = repo.channels(manifest, selected, search = if (iptv || search.trim().toIntOrNull() == null) search else "")
            channels.forEach { channelNumber(it) }
            offset = channels.size
            canLoadMore = selected.paginated && channels.isNotEmpty()
            if (channels.isEmpty()) message = "No channels or events in this category right now."
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            message = e.message.orEmpty() }
        finally { loading = false }
    }
    fun play(channel: LiveChannel, stream: StreamChoice) {
        sources = emptyList(); sourceChannel = null
        val item = MediaSummary(channel.id.hashCode(), "live", channel.name, channel.description, null, channel.poster, 0.0, tr("EN VIVO","LIVE"))
        state.openLivePlayer(PlayerRequest(item, stream, 0, 0, 0L, live = true, liveChannelId = channel.id, liveProvider = provider, liveType=channel.type))
    }
    fun openChannel(channel: LiveChannel, manual: Boolean) {
        if (loading) return
        scope.launch {
            val requestedManifest=manifest;val requestedGeneration=generation
            loading = true; message = ""
            try {
                val links = repo.streams(requestedManifest, channel)
                if(requestedGeneration!=generation || requestedManifest!=manifest) return@launch
                if (links.isEmpty()) message = tr("Este canal no devolvió enlaces HTTP reproducibles.","No playable HTTP links returned for this channel.")
                else if (manual || links.size > 1) { sourceChannel = channel; sources = links }
                else play(channel, links.first())
            } catch (e: Exception) {
            if (e is CancellationException) throw e
            message = e.message.orEmpty() }
            finally { loading = false }
        }
    }
    BackHandler { state.screen = Screen.HOME }
    val contentFocus=remember { FocusRequester() }
    Column(Modifier.fillMaxSize().background(Bg)) {
        Sidebar(Screen.LIVE_TV, state.activeProfile.name, { state.screen = it },state.notifications.size,state.activeProfile,onMoveDown={ contentFocus.requestFocus(); Unit })
        Column(Modifier.weight(1f).fillMaxHeight().padding(horizontal=34.dp,vertical=16.dp).focusRequester(contentFocus).focusGroup()) {
            Text(tr("TV en vivo","Live TV"), color=Color.White,fontSize=22.sp,lineHeight=26.sp)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                item { FocusButton(tr("Canales de TV","TV Channels"),primary=provider=="nauta") { provider="nauta";state.liveProviderTab=provider;state.liveCategoryId=null } }
                item { FocusButton(tr("Deportes en vivo","Live Sports"),primary=sports) { provider="sports";state.liveProviderTab=provider;state.liveCategoryId=null } }
                item { FocusButton(tr("Canales","Channels"),primary=iptv) { provider="iptv:do";state.liveProviderTab=provider;state.liveCategoryId=null } }
                item { FocusButton(tr("★ Favoritos","★ Favorites")) { showFavorites=true } }
                item { FocusButton("EPG") { showGuideSettings=true } }
                if(iptv) item { FocusButton(tr("Actualizar","Refresh")) { if(!loading) scope.launch {
                    loading=true
                    try { IptvRepository.refresh(manifest);refreshVersion++ } catch(e: Exception) { if(e is CancellationException) throw e;message=e.message.orEmpty() } finally { loading=false }
                } } }
                if(sports) item { FocusButton(tr("Manifiesto de deportes","Sports manifest")) { editManifest=true } }
            }
            if(iptv) { Spacer(Modifier.height(8.dp));RegionTabs(provider) { provider=it;state.liveProviderTab=it;state.liveCategoryId=null } }
            Spacer(Modifier.height(8.dp))
            NativeTextField(search, { search = it; state.liveSearch=it }, tr("Buscar nombre o número de canal","Search channel name or number"), modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            if (loading) Text(tr("Cargando canales…","Loading live content…"), color = Color.White)
            if (message.isNotBlank()) Text(message, color = Color.White, fontSize = 14.sp, lineHeight=17.sp)
            LazyColumn(Modifier.weight(1f), contentPadding=PaddingValues(bottom=24.dp,end=8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if(categories.size>1) item(key="categories") {
                    LazyRow(horizontalArrangement=Arrangement.spacedBy(12.dp)) { items(categories) { c -> FocusButton(c.name,primary=c==category) { category=c;state.liveCategoryId=c.id } } }
                }
                val visible = channels.filter { channel ->
                    search.isBlank() || liveSearchKey(channel.name).contains(liveSearchKey(search)) || channelNumber(channel).toString() == search.trim()
                }
                items(visible, key = { it.id }) { channel ->
                    Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.weight(1f)) { LiveChannelRow(channel,channelNumber(channel),{ openChannel(channel,false) },{ openChannel(channel,true) }) { color -> ChannelSchedule(provider,channel,guide,color) } }
                        ChannelExtras(state,provider,channel,guide,compact=true)
                    }
                }
                if (!loading && channels.isNotEmpty() && visible.isEmpty()) item {
                    Text(tr("No hay canales coincidentes en esta categoría.","No matching channels in this category."), color = Color.White)
                }
                if (canLoadMore && !loading) item {
                    FocusButton(tr("Cargar más","Load more")) {
                        val c = category ?: return@FocusButton
                        scope.launch {
                            val requestedManifest=manifest;val requestedSearch=search;val requestedOffset=offset;val requestedGeneration=generation
                            loading = true
                            try {
                                val more = repo.channels(requestedManifest, c, requestedOffset, if (iptv || requestedSearch.trim().toIntOrNull() == null) requestedSearch else "")
                                if(generation!=requestedGeneration || manifest!=requestedManifest) return@launch
                                more.forEach { channelNumber(it) }
                                val before = channels.size
                                channels = (channels + more).distinctBy { it.id }
                                offset += more.size
                                canLoadMore = more.isNotEmpty() && channels.size > before
                            } catch (e: Exception) {
            if (e is CancellationException) throw e
            message = e.message.orEmpty() }
                            finally { loading = false }
                        }
                    }
                }
            }
        }
    }
    if(showFavorites) FavoriteChannelsDialog(state,{ showFavorites=false;state.openLivePlayer(it) }) { showFavorites=false }
    if(showGuideSettings) LiveGuideDialog { showGuideSettings=false }
    if (editManifest) Dialog(onDismissRequest = { editManifest = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        var draft by remember { mutableStateOf(sportsManifest) }
        Column(Modifier.fillMaxWidth().background(Panel).padding(30.dp)) {
            Text(tr("Pega la URL manifest.json de SportsFree Configure","Paste the manifest.json URL from SportsFree Configure"), color = Color.White)
            NativeTextField(value = draft, onValueChange = { draft = it }, hint = "https://…/manifest.json")
            FocusButton(tr("Guardar","Save")) {
                if (draft.trim().startsWith("https://") && draft.trim().endsWith("/manifest.json")) {
                    sportsManifest = draft.trim(); prefs.edit().putString("sports_manifest", sportsManifest).apply(); editManifest = false
                }
            }
            FocusButton(tr("Cancelar","Cancel")) { editManifest = false }
        }
    }
    if (sources.isNotEmpty()) Dialog(onDismissRequest = { sources = emptyList() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val first = remember { FocusRequester() }
        LaunchedEffect(Unit) { delay(100); first.requestFocus() }
        Column(Modifier.fillMaxSize().background(Bg).padding(32.dp)) {
            Text(sourceChannel?.name.orEmpty(), color = Color.White, fontSize = 24.sp, lineHeight=29.sp)
            FocusButton(tr("Cerrar","Close")) { sources = emptyList() }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(sources) { stream ->
                    FocusButton("${stream.name} · ${stream.title}", modifier = Modifier.fillMaxWidth().then(if (stream === sources.first()) Modifier.focusRequester(first) else Modifier)) {
                        sourceChannel?.let { play(it, stream) }
                    }
                }
            }
        }
    }
}

private fun liveSearchKey(value: String): String = Normalizer.normalize(value.trim().lowercase(), Normalizer.Form.NFD)
    .replace(Regex("\\p{M}+"), "")

@Composable
private fun LiveChannelRow(channel: LiveChannel, number: Int, onClick: () -> Unit, onLongClick: () -> Unit,schedule: @Composable (Color)->Unit = {}) {
    var focused by remember(channel.id) { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().onFocusChanged { focused = it.hasFocus }
        .tvPlaybackClick(onClick, onLongClick).focusable().clickable(onClick = onClick)
        .background(if (focused) Color.White else Panel, RoundedCornerShape(14.dp)).padding(12.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Text(number.toString().padStart(3, '0'), color = if (focused) Color.Black else Color.White,
            fontSize = 18.sp, lineHeight=22.sp, modifier = Modifier.width(58.dp))
        Box(Modifier.size(62.dp).clip(RoundedCornerShape(9.dp)).background(Color(0xFF303030)),
            contentAlignment = androidx.compose.ui.Alignment.Center) {
            Text(channel.name.take(1), color = Color.White, fontSize = 24.sp, lineHeight=29.sp)
            channel.poster?.let { AsyncImage(it, channel.name, Modifier.fillMaxSize(), contentScale = ContentScale.Fit) }
        }
        Spacer(Modifier.width(18.dp))
        Column(Modifier.weight(1f)) {
            Text(channel.name,color=if(focused) Color.Black else Color.White,fontSize=16.sp,lineHeight=19.sp,maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            schedule(if(focused) Color.DarkGray else Muted)
        }
        Text(tr("EN VIVO","LIVE"), color = if (focused) Color.Black else Color.White, fontSize = 12.sp, lineHeight=15.sp)
    }
}
