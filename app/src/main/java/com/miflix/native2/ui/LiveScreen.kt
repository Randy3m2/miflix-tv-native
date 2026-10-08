package com.miflix.native2.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import kotlinx.coroutines.delay
import android.content.Context

@Composable
fun LiveScreen(state: AppState) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("miflix_live", Context.MODE_PRIVATE) }
    val repo = remember { LiveRepository() }
    val scope = rememberCoroutineScope()
    var sports by remember { mutableStateOf(false) }
    var sportsManifest by remember { mutableStateOf(prefs.getString("sports_manifest", "https://sportsfree-us2.highfly.to/manifest.json").orEmpty()) }
    var editManifest by remember { mutableStateOf(false) }
    val manifest = if (sports) sportsManifest else "https://stremio-addon-wheat.vercel.app/manifest.json"
    var categories by remember { mutableStateOf<List<LiveCategory>>(emptyList()) }
    var category by remember { mutableStateOf<LiveCategory?>(null) }
    var channels by remember { mutableStateOf<List<LiveChannel>>(emptyList()) }
    var message by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var sourceChannel by remember { mutableStateOf<LiveChannel?>(null) }
    var sources by remember { mutableStateOf<List<StreamChoice>>(emptyList()) }
    var offset by remember { mutableStateOf(0) }
    var canLoadMore by remember { mutableStateOf(false) }

    LaunchedEffect(manifest) {
        categories = emptyList(); category = null; channels = emptyList(); message = ""; loading = true
        try {
            categories = repo.categories(manifest)
            category = categories.firstOrNull()
            if (categories.isEmpty()) message = "This add-on has no live catalogs."
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            message = if (sports) "Sports service unavailable. Open Configure in your browser and paste its generated manifest. ${e.message}" else e.message.orEmpty()
        } finally { loading = false }
    }
    LaunchedEffect(manifest, category) {
        val selected = category ?: return@LaunchedEffect
        loading = true; message = ""; channels = emptyList(); offset = 0
        try {
            channels = repo.channels(manifest, selected)
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
        val item = MediaSummary(channel.id.hashCode(), "live", channel.name, channel.description, null, channel.poster, 0.0, "LIVE")
        state.playerRequest = PlayerRequest(item, stream, 0, 0, 0L, live = true)
        state.screen = Screen.PLAYER
    }
    fun open(channel: LiveChannel, manual: Boolean) {
        if (loading) return
        scope.launch {
            loading = true; message = ""
            try {
                val links = repo.streams(manifest, channel)
                if (links.isEmpty()) message = "No playable HTTP links returned for this channel."
                else if (manual || links.size > 1) { sourceChannel = channel; sources = links }
                else play(channel, links.first())
            } catch (e: Exception) {
            if (e is CancellationException) throw e
            message = e.message.orEmpty() }
            finally { loading = false }
        }
    }
    BackHandler { state.screen = Screen.HOME }
    Row(Modifier.fillMaxSize().background(Bg)) {
        Sidebar(Screen.LIVE_TV, state.activeProfile.name) { state.screen = it }
        Column(Modifier.weight(1f).fillMaxHeight().padding(24.dp)) {
            Text("Live TV", color = Color.White, fontSize = 28.sp)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FocusButton("TV Channels", primary = !sports) { sports = false }
                FocusButton("Live Sports", primary = sports) { sports = true }
                if (sports) FocusButton("Sports manifest") { editManifest = true }
            }
            Spacer(Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(categories) { c -> FocusButton(c.name, primary = c == category) { category = c } }
            }
            Spacer(Modifier.height(14.dp))
            if (loading) Text("Loading live content…", color = Color.White)
            if (message.isNotBlank()) Text(message, color = Color.White, fontSize = 14.sp)
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(channels, key = { it.id }) { channel ->
                    FocusButton("● ${channel.name}", modifier = Modifier.fillMaxWidth(), onLongClick = { open(channel, true) }) { open(channel, false) }
                }
                if (canLoadMore && !loading) item {
                    FocusButton("Load more") {
                        val c = category ?: return@FocusButton
                        scope.launch {
                            loading = true
                            try {
                                val more = repo.channels(manifest, c, offset)
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
    if (editManifest) Dialog(onDismissRequest = { editManifest = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        var draft by remember { mutableStateOf(sportsManifest) }
        Column(Modifier.fillMaxWidth().background(Panel).padding(30.dp)) {
            Text("Paste the manifest.json URL from SportsFree Configure", color = Color.White)
            NativeTextField(value = draft, onValueChange = { draft = it }, hint = "https://…/manifest.json")
            FocusButton("Save") {
                if (draft.trim().startsWith("https://") && draft.trim().endsWith("/manifest.json")) {
                    sportsManifest = draft.trim(); prefs.edit().putString("sports_manifest", sportsManifest).apply(); editManifest = false
                }
            }
            FocusButton("Cancel") { editManifest = false }
        }
    }
    if (sources.isNotEmpty()) Dialog(onDismissRequest = { sources = emptyList() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val first = remember { FocusRequester() }
        LaunchedEffect(Unit) { delay(100); first.requestFocus() }
        Column(Modifier.fillMaxSize().background(Bg).padding(32.dp)) {
            Text(sourceChannel?.name.orEmpty(), color = Color.White, fontSize = 24.sp)
            FocusButton("Close") { sources = emptyList() }
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
