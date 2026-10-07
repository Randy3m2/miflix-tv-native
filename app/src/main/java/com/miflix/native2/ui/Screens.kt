package com.miflix.native2.ui

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.miflix.native2.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

@Composable
fun HomeScreen(state: AppState, onNavigate: (Screen) -> Unit) {
    val scope = rememberCoroutineScope()
    var focusedCandidate by remember { mutableStateOf<MediaSummary?>(state.trending.firstOrNull()) }
    var hero by remember { mutableStateOf<MediaSummary?>(state.trending.firstOrNull()) }
    var contentFocused by remember { mutableStateOf(false) }
    LaunchedEffect(focusedCandidate?.cloudId) { delay(180); hero = focusedCandidate }

    Row(Modifier.fillMaxSize().background(Bg)) {
        Sidebar(state.screen, onNavigate, contentFocused)
        Box(Modifier.weight(1f).fillMaxHeight()) {
            AsyncImage(model = hero?.backdrop, contentDescription = null, modifier = Modifier.fillMaxWidth().height(420.dp), contentScale = ContentScale.Crop)
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x22000000), Bg), startY = 90f)))
            Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Bg, Color.Transparent), endX = 700f)))
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 48.dp)) {
                item {
                    Column(Modifier.height(335.dp).fillMaxWidth().padding(start = 42.dp, top = 78.dp, end = 52.dp)) {
                        Text(hero?.title ?: "MiFlix", color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(10.dp))
                        Text(hero?.overview.orEmpty(), color = Color(0xFFE3E3E8), fontSize = 15.sp, maxLines = 3, modifier = Modifier.widthIn(max = 760.dp))
                        Spacer(Modifier.height(18.dp))
                        hero?.let { h -> FocusButton("▶  Play / Details", primary = true, onClick = { scope.launch { state.open(h) } }) }
                    }
                }
                item { Rail("Trending", state.trending, { focusedCandidate = it; contentFocused = true }) { scope.launch { state.open(it) } } }
                item { Rail("Popular movies", state.movies, { focusedCandidate = it; contentFocused = true }) { scope.launch { state.open(it) } } }
                item { Rail("Popular series", state.series, { focusedCandidate = it; contentFocused = true }) { scope.launch { state.open(it) } } }
                item { Rail("Top rated", state.topRated, { focusedCandidate = it; contentFocused = true }) { scope.launch { state.open(it) } } }
            }
            Text(state.activeProfile.name, color = Color.White, fontSize = 14.sp, modifier = Modifier.align(Alignment.TopEnd).padding(28.dp).background(Color(0xAA191920), RoundedCornerShape(18.dp)).padding(horizontal = 16.dp, vertical = 8.dp))
        }
    }
}

@Composable
private fun Rail(title: String, rows: List<MediaSummary>, onFocus: (MediaSummary) -> Unit, onClick: (MediaSummary) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(bottom = 28.dp)) {
        Text(title, color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 42.dp, bottom = 12.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 42.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            items(rows, key = { it.cloudId }) { MediaCard(it, onFocused = onFocus, onClick = { onClick(it) }) }
        }
    }
}

@Composable
fun CatalogScreen(state: AppState, title: String, items: List<MediaSummary>, onBack: () -> Unit) {
    val scope = rememberCoroutineScope(); BackHandler(onBack = onBack)
    Column(Modifier.fillMaxSize().background(Bg).padding(44.dp)) {
        Text("←  $title", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(28.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            val chunks = items.chunked(4)
            items(chunks.size) { idx ->
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) { chunks[idx].forEach { item -> MediaCard(item, onFocused = {}, onClick = { scope.launch { state.open(item) } }) } }
            }
        }
    }
}

@Composable
fun SearchScreen(state: AppState, onBack: () -> Unit) {
    var query by remember { mutableStateOf("") }; var results by remember { mutableStateOf<List<MediaSummary>>(emptyList()) }; val scope = rememberCoroutineScope(); BackHandler(onBack = onBack)
    Column(Modifier.fillMaxSize().background(Bg).padding(52.dp)) {
        Text("Search", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            NativeTextField(query, { query = it }, "Movie or series…", modifier = Modifier.width(520.dp))
            Spacer(Modifier.width(14.dp)); FocusButton("Search", primary = true) { if (query.isNotBlank()) scope.launch { results = runCatching { state.tmdb.search(query) }.getOrElse { emptyList() } } }
        }
        Spacer(Modifier.height(30.dp)); LazyColumn(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            items(results.chunked(4).size) { idx -> Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) { results.chunked(4)[idx].forEach { item -> MediaCard(item, onFocused = {}, onClick = { scope.launch { state.open(item) } }) } } }
        }
    }
}

@Composable
fun DetailsScreen(state: AppState, onBack: () -> Unit) {
    val item = state.selected ?: return; val details = state.details; val scope = rememberCoroutineScope(); BackHandler(onBack = onBack)
    Box(Modifier.fillMaxSize().background(Bg)) {
        AsyncImage(model = item.backdrop, contentDescription = null, modifier = Modifier.fillMaxWidth().height(500.dp), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x33000000), Bg), startY = 120f)))
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 64.dp, end = 64.dp, top = 55.dp, bottom = 60.dp)) {
            item {
                Text("← Back", color = Color(0xFFE9E9F0), fontSize = 16.sp, modifier = Modifier.padding(bottom = 65.dp))
                Text(item.title, color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(8.dp)); Text("${item.year}   ★ ${"%.1f".format(item.rating)}   ${details?.runtimeText.orEmpty()}", color = Color(0xFFD0D0D8), fontSize = 15.sp)
                Spacer(Modifier.height(14.dp)); Text(item.overview, color = Color(0xFFE2E2E8), fontSize = 16.sp, maxLines = 4, modifier = Modifier.widthIn(max = 850.dp))
                Spacer(Modifier.height(22.dp)); Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    if (item.type == "movie") FocusButton("▶  Play", primary = true) { scope.launch { runCatching { state.playMovie() }.onFailure { state.error = it.message } } }
                    FocusButton(if (state.favorites.contains(item.cloudId)) "✓ My List" else "+ My List") { state.toggleFavorite(item); scope.launch { runCatching { state.pushCloud() } } }
                }
                Spacer(Modifier.height(38.dp))
            }
            if (item.type == "series") {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Season ${state.currentSeason}", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(18.dp))
                        if ((details?.seasonCount ?: 0) > 1) {
                            FocusButton("Previous") { if (state.currentSeason > 1) scope.launch { state.loadSeason(state.currentSeason - 1) } }
                            Spacer(Modifier.width(8.dp)); FocusButton("Next") { if (state.currentSeason < (details?.seasonCount ?: 1)) scope.launch { state.loadSeason(state.currentSeason + 1) } }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(state.episodes, key = { "${it.season}:${it.episode}" }) { ep -> EpisodeCard(ep) { scope.launch { runCatching { state.playEpisode(ep) }.onFailure { state.error = it.message } } } }
                    }
                    Spacer(Modifier.height(50.dp))
                }
            }
        }
        state.busyMessage?.let { LoadingOverlay(it) }
    }
}

@Composable
private fun EpisodeCard(ep: EpisodeSummary, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val scale by androidx.compose.animation.core.animateFloatAsState(if (focused) 1.055f else 1f, label = "epScale")
    Column(Modifier.width(320.dp).graphicsLayer { scaleX = scale; scaleY = scale }.onFocusChanged { focused = it.isFocused }.focusable().clickable(onClick = onClick)) {
        Box(Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF1B1B21))) {
            AsyncImage(model = ep.still, contentDescription = ep.title, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            if (focused) Box(Modifier.fillMaxSize().background(Color(0x22000000)), contentAlignment = Alignment.Center) { Text("▶", color = Color.White, fontSize = 34.sp) }
        }
        Spacer(Modifier.height(8.dp)); Text("S${ep.season} E${ep.episode} · ${ep.title}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
fun SettingsScreen(state: AppState, onBack: () -> Unit) {
    var email by remember { mutableStateOf("") }; var password by remember { mutableStateOf("") }; val scope = rememberCoroutineScope(); BackHandler(onBack = onBack)
    Column(Modifier.fillMaxSize().background(Bg).padding(60.dp)) {
        Text("Settings", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(26.dp)); Text("MiFlix Native 2.0 · Compose for TV + Media3", color = Muted, fontSize = 15.sp)
        Spacer(Modifier.height(38.dp))
        if (state.session == null) {
            Text("Account & sync", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(12.dp))
            Text("Sign in with the same MiFlix account used on PC. TMDB, Torrentio, profiles and progress are pulled from your private Supabase state.", color = Muted, fontSize = 14.sp, modifier = Modifier.width(760.dp))
            Spacer(Modifier.height(18.dp)); NativeTextField(email, { email = it }, "Email", modifier = Modifier.width(520.dp)); Spacer(Modifier.height(12.dp)); NativeTextField(password, { password = it }, "Password", true, Modifier.width(520.dp)); Spacer(Modifier.height(16.dp))
            FocusButton("Sign in", primary = true) { scope.launch { state.login(email.trim(), password) } }
        } else {
            Text("Signed in as ${state.session?.email}", color = Color.White, fontSize = 18.sp); Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { FocusButton("Sync now", primary = true) { scope.launch { state.syncFromCloud(); state.pushCloud(); if (state.tmdb.token.isNotBlank()) state.loadHome() } }; FocusButton("Sign out") { state.signOut() } }
            Spacer(Modifier.height(38.dp)); Text("Profile: ${state.activeProfile.name}", color = Color.White, fontSize = 18.sp)
            Text("TMDB: ${if (state.tmdb.token.isNotBlank()) "Ready" else "Missing"} · Torrentio/TorBox: ${if (state.streamRepo.manifestUrl.isNotBlank()) "Ready" else "Missing"}", color = Muted, fontSize = 14.sp)
        }
        state.error?.let { Spacer(Modifier.height(22.dp)); Text(it, color = Color(0xFFFF8585), fontSize = 14.sp) }
        state.busyMessage?.let { Spacer(Modifier.height(18.dp)); Text(it, color = Color.White, fontSize = 14.sp) }
    }
}

@Composable
fun ProfilesScreen(state: AppState, onBack: () -> Unit) {
    val scope = rememberCoroutineScope(); BackHandler(onBack = onBack)
    Column(Modifier.fillMaxSize().background(Bg).padding(64.dp)) {
        Text("Who's watching?", color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.Black); Spacer(Modifier.height(35.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            state.profiles.forEach { p ->
                var focused by remember { mutableStateOf(false) }
                Column(Modifier.width(150.dp).onFocusChanged { focused = it.isFocused }.focusable().clickable { scope.launch { state.selectProfile(p) } }, horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(110.dp).clip(RoundedCornerShape(55.dp)).background(if (focused) Purple else Color(0xFF24242C)), contentAlignment = Alignment.Center) { Text(p.name.take(1).uppercase(), color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.Black) }
                    Spacer(Modifier.height(10.dp)); Text(p.name, color = Color.White, fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
fun PlayerScreen(state: AppState, request: PlayerRequest, onClose: () -> Unit) {
    val context = LocalContext.current; val scope = rememberCoroutineScope()
    val player = remember(request.stream.url) {
        val rf = DefaultRenderersFactory(context).setEnableDecoderFallback(true)
        ExoPlayer.Builder(context, rf).build().apply {
            setSeekBackIncrementMs(10_000); setSeekForwardIncrementMs(10_000)
            val builder = MediaItem.Builder().setUri(request.stream.url)
            if (request.stream.subtitles.isNotEmpty()) {
                builder.setSubtitleConfigurations(request.stream.subtitles.take(20).map { s ->
                    MediaItem.SubtitleConfiguration.Builder(android.net.Uri.parse(s.url)).apply {
                        if (s.lang.isNotBlank()) setLanguage(s.lang)
                        setLabel(s.label.ifBlank { s.lang.ifBlank { "Subtitle" } })
                        setMimeType(if (s.url.lowercase().contains(".srt")) MimeTypes.APPLICATION_SUBRIP else MimeTypes.TEXT_VTT)
                    }.build()
                })
            }
            setMediaItem(builder.build(), request.resumeMs)
            trackSelectionParameters = TrackSelectionParameters.Builder(context).setPreferredAudioLanguage("es").setPreferredTextLanguage("es").build()
            videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
            prepare(); playWhenReady = true
        }
    }
    DisposableEffect(player) { onDispose { state.updateProgress(request, player.currentPosition, player.duration.coerceAtLeast(0)); scope.launch { runCatching { state.pushCloud() } }; player.release() } }
    LaunchedEffect(player) { while (true) { delay(5000); val d = player.duration; if (d > 0) state.updateProgress(request, player.currentPosition, d) } }
    BackHandler { onClose() }
    AndroidView(factory = { ctx -> PlayerView(ctx).apply { useController = true; controllerShowTimeoutMs = 2500; controllerAutoShow = true; setShowSubtitleButton(true); setShowRewindButton(true); setShowFastForwardButton(true); this.player = player; layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT) } }, modifier = Modifier.fillMaxSize().background(Color.Black))
}

@Composable
private fun LoadingOverlay(text: String) {
    Box(Modifier.fillMaxSize().background(Color(0x99000000)), contentAlignment = Alignment.Center) { Text(text, color = Color.White, fontSize = 18.sp, modifier = Modifier.background(Color(0xEE17171D), RoundedCornerShape(14.dp)).padding(horizontal = 26.dp, vertical = 18.dp)) }
}
