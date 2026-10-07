package com.miflix.native2.ui

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.miflix.native2.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(state: AppState, onNavigate: (Screen) -> Unit) {
    val scope = rememberCoroutineScope()
    var focusedCandidate by remember { mutableStateOf<MediaSummary?>(state.trending.firstOrNull()) }
    var hero by remember { mutableStateOf<MediaSummary?>(state.trending.firstOrNull()) }

    // A rapid D-pad sweep only moves the focus ring. The expensive hero/backdrop update
    // happens after the user settles on one card for a moment.
    LaunchedEffect(focusedCandidate?.cloudId) {
        val candidate = focusedCandidate ?: return@LaunchedEffect
        delay(420)
        hero = candidate
    }
    LaunchedEffect(Unit) { state.loadHomeExtras() }

    val openItem: (MediaSummary) -> Unit = remember(state) {
        { item -> scope.launch { state.open(item) } }
    }
    val cardFocus: (MediaSummary) -> Unit = remember {
        { item -> focusedCandidate = item }
    }

    Row(Modifier.fillMaxSize().background(Bg)) {
        Sidebar(state.screen, onNavigate)
        Box(Modifier.weight(1f).fillMaxHeight()) {
            HeroBackdrop(hero)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 44.dp)
            ) {
                item(key = "hero") {
                    HeroContent(hero = hero, onOpen = openItem)
                }
                if (state.continueWatching.isNotEmpty()) {
                    item(key = "continue") {
                        Rail(
                            title = "Continue watching",
                            rows = state.continueWatching,
                            progressFor = { state.progress[it.cloudId]?.percent },
                            onFocus = cardFocus,
                            onClick = openItem
                        )
                    }
                }
                item(key = "trending") { Rail("Trending", state.trending, onFocus = cardFocus, onClick = openItem) }
                item(key = "movies") { Rail("Popular movies", state.movies, onFocus = cardFocus, onClick = openItem) }
                item(key = "series") { Rail("Popular series", state.series, onFocus = cardFocus, onClick = openItem) }
                item(key = "top") { Rail("Top rated", state.topRated, onFocus = cardFocus, onClick = openItem) }
            }
            Text(
                state.activeProfile.name,
                color = Color.White,
                fontSize = 14.sp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(28.dp)
                    .background(Color(0xAA191920), RoundedCornerShape(18.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun HeroBackdrop(hero: MediaSummary?) {
    Crossfade(
        targetState = hero,
        animationSpec = tween(durationMillis = 180, easing = LinearOutSlowInEasing),
        label = "heroBackdrop"
    ) { item ->
        AsyncImage(
            model = item?.backdrop,
            contentDescription = null,
            modifier = Modifier.fillMaxWidth().height(420.dp),
            contentScale = ContentScale.Crop
        )
    }
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x22000000), Bg), startY = 90f)))
    Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Bg, Color.Transparent), endX = 720f)))
}

@Composable
private fun HeroContent(hero: MediaSummary?, onOpen: (MediaSummary) -> Unit) {
    Column(
        Modifier
            .height(335.dp)
            .fillMaxWidth()
            .padding(start = 42.dp, top = 78.dp, end = 52.dp)
    ) {
        Crossfade(
            targetState = hero,
            animationSpec = tween(durationMillis = 160, easing = LinearOutSlowInEasing),
            label = "heroText"
        ) { item ->
            Column {
                Text(item?.title ?: "MiFlix", color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(10.dp))
                Text(
                    item?.overview.orEmpty(),
                    color = Color(0xFFE3E3E8),
                    fontSize = 15.sp,
                    maxLines = 3,
                    modifier = Modifier.widthIn(max = 760.dp)
                )
                Spacer(Modifier.height(18.dp))
                item?.let { h -> FocusButton("▶  Details", primary = true) { onOpen(h) } }
            }
        }
    }
}

@Composable
private fun Rail(
    title: String,
    rows: List<MediaSummary>,
    progressFor: (MediaSummary) -> Double? = { null },
    onFocus: (MediaSummary) -> Unit,
    onClick: (MediaSummary) -> Unit
) {
    // Fixed-height rail + fixed-footprint MediaCard prevents parent vertical correction
    // when the selected card scales up.
    Column(Modifier.fillMaxWidth().height(205.dp)) {
        Text(
            title,
            color = Color.White,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 42.dp, bottom = 8.dp)
        )
        LazyRow(
            modifier = Modifier.fillMaxWidth().height(164.dp),
            contentPadding = PaddingValues(horizontal = 34.dp),
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            items(rows, key = { it.cloudId }, contentType = { "media" }) { item ->
                MediaCard(
                    item = item,
                    progressPercent = progressFor(item),
                    onFocused = onFocus,
                    onClick = { onClick(item) }
                )
            }
        }
    }
}

@Composable
fun CatalogScreen(state: AppState, title: String, items: List<MediaSummary>, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    BackHandler(onBack = onBack)
    Column(Modifier.fillMaxSize().background(Bg).padding(start = 44.dp, top = 36.dp, end = 44.dp)) {
        Text("←  $title", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(22.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(bottom = 40.dp)) {
            val chunks = items.chunked(4)
            items(chunks.size, key = { it }) { idx ->
                Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                    chunks[idx].forEach { item ->
                        MediaCard(
                            item = item,
                            progressPercent = state.progress[item.cloudId]?.percent,
                            onFocused = {},
                            onClick = { scope.launch { state.open(item) } }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SearchScreen(state: AppState, onBack: () -> Unit) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<MediaSummary>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    BackHandler(onBack = onBack)

    suspend fun runSearch() {
        if (query.trim().length < 2) {
            results = emptyList(); return
        }
        searching = true
        results = runCatching { state.tmdb.search(query.trim()) }.getOrElse { emptyList() }
        searching = false
    }

    LaunchedEffect(query) {
        if (query.trim().length < 2) {
            results = emptyList()
        } else {
            delay(420)
            runSearch()
        }
    }

    Column(Modifier.fillMaxSize().background(Bg).padding(52.dp)) {
        Text("Search", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            NativeTextField(query, { query = it }, "Movie or series…", modifier = Modifier.width(520.dp))
            Spacer(Modifier.width(14.dp))
            FocusButton("Search", primary = true) { scope.launch { runSearch() } }
            if (searching) {
                Spacer(Modifier.width(18.dp)); Text("Searching…", color = Muted, fontSize = 14.sp)
            }
        }
        Spacer(Modifier.height(22.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(bottom = 40.dp)) {
            val chunks = results.chunked(4)
            items(chunks.size, key = { it }) { idx ->
                Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                    chunks[idx].forEach { item ->
                        MediaCard(item, onFocused = {}, onClick = { scope.launch { state.open(item) } })
                    }
                }
            }
        }
    }
}

@Composable
fun CollectionsScreen(state: AppState, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    BackHandler(onBack = onBack)
    LaunchedEffect(Unit) { state.loadHomeExtras() }
    val openItem: (MediaSummary) -> Unit = remember(state) { { item -> scope.launch { state.open(item) } } }

    Box(Modifier.fillMaxSize().background(Bg)) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 38.dp, bottom = 44.dp)) {
            item(key = "head") {
                Column(Modifier.padding(horizontal = 44.dp)) {
                    Text("←  Collections", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(8.dp))
                    Text("Fast native shelves · genres and streaming platforms", color = Muted, fontSize = 14.sp)
                    Spacer(Modifier.height(26.dp))
                }
            }
            if (state.action.isNotEmpty()) item(key = "action") { Rail("Action", state.action, onFocus = {}, onClick = openItem) }
            if (state.sciFi.isNotEmpty()) item(key = "scifi") { Rail("Science fiction", state.sciFi, onFocus = {}, onClick = openItem) }
            if (state.netflix.isNotEmpty()) item(key = "netflix") { Rail("Netflix", state.netflix, onFocus = {}, onClick = openItem) }
            if (state.disney.isNotEmpty()) item(key = "disney") { Rail("Disney+", state.disney, onFocus = {}, onClick = openItem) }
            if (state.extrasLoading && state.action.isEmpty() && state.netflix.isEmpty()) {
                item { Text("Loading collections…", color = Muted, fontSize = 15.sp, modifier = Modifier.padding(horizontal = 44.dp, vertical = 22.dp)) }
            }
        }
    }
}

@Composable
fun DetailsScreen(state: AppState, onBack: () -> Unit) {
    val item = state.selected ?: return
    val details = state.details
    val scope = rememberCoroutineScope()
    val saved = state.progress[item.cloudId]
    BackHandler(onBack = onBack)

    Box(Modifier.fillMaxSize().background(Bg)) {
        AsyncImage(
            model = item.backdrop,
            contentDescription = null,
            modifier = Modifier.fillMaxWidth().height(500.dp),
            contentScale = ContentScale.Crop
        )
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x33000000), Bg), startY = 120f)))
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Bg, Color.Transparent), endX = 780f)))

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 64.dp, end = 64.dp, top = 46.dp, bottom = 60.dp)
        ) {
            item(key = "meta") {
                Text("← Back", color = Color(0xFFE9E9F0), fontSize = 16.sp, modifier = Modifier.padding(bottom = 58.dp))
                Text(item.title, color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(8.dp))
                Text(
                    "${item.year}   ★ ${"%.1f".format(item.rating)}   ${details?.runtimeText.orEmpty()}",
                    color = Color(0xFFD0D0D8),
                    fontSize = 15.sp
                )
                if (!details?.genres.isNullOrEmpty()) {
                    Spacer(Modifier.height(7.dp))
                    Text(details?.genres?.take(4)?.joinToString("  ·  ").orEmpty(), color = Muted, fontSize = 13.sp)
                }
                Spacer(Modifier.height(14.dp))
                Text(item.overview, color = Color(0xFFE2E2E8), fontSize = 16.sp, maxLines = 4, modifier = Modifier.widthIn(max = 850.dp))
                Spacer(Modifier.height(22.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    if (item.type == "movie") {
                        val label = if ((saved?.percent ?: 0.0) in 1.0..95.0) "▶  Resume ${formatPosition(saved?.position ?: 0)}" else "▶  Play"
                        FocusButton(label, primary = true) {
                            scope.launch { runCatching { state.playMovie() }.onFailure { state.error = it.message } }
                        }
                    } else if ((saved?.season ?: 0) > 0 && (saved?.episode ?: 0) > 0 && (saved?.percent ?: 0.0) < 96.0) {
                        FocusButton("▶  Continue S${saved?.season} E${saved?.episode}", primary = true) {
                            scope.launch { runCatching { state.resumeSeries() }.onFailure { state.error = it.message } }
                        }
                    }
                    FocusButton(if (state.favorites.contains(item.cloudId)) "✓ My List" else "+ My List") {
                        state.toggleFavorite(item)
                        scope.launch { runCatching { state.pushCloud() } }
                    }
                }
                Spacer(Modifier.height(34.dp))
            }

            if (item.type == "series") {
                item(key = "episodes") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Season ${state.currentSeason}", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(18.dp))
                        if ((details?.seasonCount ?: 0) > 1) {
                            FocusButton("Previous") {
                                if (state.currentSeason > 1) scope.launch { state.loadSeason(state.currentSeason - 1) }
                            }
                            Spacer(Modifier.width(8.dp))
                            FocusButton("Next") {
                                if (state.currentSeason < (details?.seasonCount ?: 1)) scope.launch { state.loadSeason(state.currentSeason + 1) }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    LazyRow(
                        modifier = Modifier.fillMaxWidth().height(235.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.episodes, key = { "${it.season}:${it.episode}" }) { ep ->
                            EpisodeCard(ep) {
                                scope.launch { runCatching { state.playEpisode(ep) }.onFailure { state.error = it.message } }
                            }
                        }
                    }
                    Spacer(Modifier.height(36.dp))
                }
            }
        }
        state.busyMessage?.let { LoadingOverlay(it) }
    }
}

@Composable
private fun EpisodeCard(ep: EpisodeSummary, onClick: () -> Unit) {
    var focused by remember(ep.season, ep.episode) { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.03f else 1f,
        animationSpec = tween(100, easing = LinearOutSlowInEasing),
        label = "episodeScale"
    )
    Box(Modifier.width(332.dp).height(225.dp), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier
                .width(320.dp)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .onFocusChanged { focused = it.isFocused }
                .focusable()
                .clickable(onClick = onClick)
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1B1B21))
                    .border(if (focused) 2.dp else 0.dp, Color.White, RoundedCornerShape(12.dp))
            ) {
                AsyncImage(model = ep.still, contentDescription = ep.title, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                if (focused) Box(Modifier.fillMaxSize().background(Color(0x26000000)), contentAlignment = Alignment.Center) {
                    Text("▶", color = Color.White, fontSize = 34.sp)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("S${ep.season} E${ep.episode} · ${ep.title}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            if (ep.airDate.isNotBlank()) Text(ep.airDate, color = Muted, fontSize = 11.sp, maxLines = 1)
        }
    }
}

@Composable
fun SettingsScreen(state: AppState, onBack: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    BackHandler(onBack = onBack)

    Column(Modifier.fillMaxSize().background(Bg).padding(60.dp)) {
        Text("Settings", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(18.dp))
        Text("MiFlix Native 2.0 Alpha 2 · Compose TV + Media3", color = Muted, fontSize = 15.sp)
        Spacer(Modifier.height(30.dp))

        if (state.session == null) {
            Text("Account & sync", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Text(
                "Sign in with the same MiFlix account used on PC. Your private TMDB/Torrentio setup, profiles and progress are restored automatically.",
                color = Muted,
                fontSize = 14.sp,
                modifier = Modifier.width(760.dp)
            )
            Spacer(Modifier.height(16.dp))
            NativeTextField(email, { email = it }, "Email", modifier = Modifier.width(520.dp))
            Spacer(Modifier.height(10.dp))
            NativeTextField(password, { password = it }, "Password", true, Modifier.width(520.dp))
            Spacer(Modifier.height(14.dp))
            FocusButton("Sign in", primary = true) { scope.launch { state.login(email.trim(), password) } }
        } else {
            Text("Signed in as ${state.session?.email}", color = Color.White, fontSize = 18.sp)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FocusButton("Sync now", primary = true) {
                    scope.launch {
                        state.syncFromCloud()
                        state.pushCloud()
                        if (state.tmdb.token.isNotBlank()) state.loadHome()
                    }
                }
                FocusButton("Sign out") { state.signOut() }
            }
            Spacer(Modifier.height(32.dp))
            Text("Profile: ${state.activeProfile.name}", color = Color.White, fontSize = 18.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                "TMDB: ${if (state.tmdb.token.isNotBlank()) "Ready" else "Missing"}  ·  Torrentio/TorBox: ${if (state.streamRepo.manifestUrl.isNotBlank()) "Ready" else "Missing"}",
                color = Muted,
                fontSize = 14.sp
            )
            Text("Private setup is restored after login; no long TV URLs to type.", color = Muted, fontSize = 13.sp)
        }
        state.error?.let { Spacer(Modifier.height(20.dp)); Text(it, color = Color(0xFFFF8585), fontSize = 14.sp) }
        state.busyMessage?.let { Spacer(Modifier.height(16.dp)); Text(it, color = Color.White, fontSize = 14.sp) }
    }
}

@Composable
fun ProfilesScreen(state: AppState, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    BackHandler(onBack = onBack)
    Column(Modifier.fillMaxSize().background(Bg).padding(64.dp)) {
        Text("Who's watching?", color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(35.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            state.profiles.forEach { p ->
                var focused by remember(p.id) { mutableStateOf(false) }
                val scale by animateFloatAsState(
                    targetValue = if (focused) 1.035f else 1f,
                    animationSpec = tween(100, easing = LinearOutSlowInEasing),
                    label = "profileScale"
                )
                Column(
                    Modifier
                        .width(150.dp)
                        .graphicsLayer { scaleX = scale; scaleY = scale }
                        .onFocusChanged { focused = it.isFocused }
                        .focusable()
                        .clickable { scope.launch { state.selectProfile(p) } },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        Modifier
                            .size(110.dp)
                            .clip(RoundedCornerShape(55.dp))
                            .background(if (focused) Purple else Color(0xFF24242C))
                            .border(if (focused) 2.dp else 0.dp, Color.White, RoundedCornerShape(55.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(p.name.take(1).uppercase(), color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.Black)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(p.name, color = Color.White, fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
fun PlayerScreen(state: AppState, request: PlayerRequest, onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val player = remember(request.stream.url) {
        val renderers = DefaultRenderersFactory(context).setEnableDecoderFallback(true)
        ExoPlayer.Builder(context, renderers).build().apply {
            setSeekBackIncrementMs(10_000)
            setSeekForwardIncrementMs(10_000)
            val builder = MediaItem.Builder().setUri(request.stream.url)
            if (request.stream.subtitles.isNotEmpty()) {
                builder.setSubtitleConfigurations(
                    request.stream.subtitles.take(20).map { s ->
                        MediaItem.SubtitleConfiguration.Builder(android.net.Uri.parse(s.url)).apply {
                            if (s.lang.isNotBlank()) setLanguage(s.lang)
                            setLabel(s.label.ifBlank { s.lang.ifBlank { "Subtitle" } })
                            setMimeType(if (s.url.lowercase().contains(".srt")) MimeTypes.APPLICATION_SUBRIP else MimeTypes.TEXT_VTT)
                        }.build()
                    }
                )
            }
            setMediaItem(builder.build(), request.resumeMs)
            trackSelectionParameters = TrackSelectionParameters.Builder(context)
                .setPreferredAudioLanguage("es")
                .setPreferredTextLanguage("es")
                .build()
            videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    val duration = player.duration.coerceAtLeast(0)
                    if (duration > 0) state.updateProgress(request, duration, duration, ended = true)
                    scope.launch { runCatching { state.pushCloud() } }
                }
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            state.updateProgress(request, player.currentPosition, player.duration.coerceAtLeast(0))
            scope.launch { runCatching { state.pushCloud() } }
            player.release()
        }
    }
    LaunchedEffect(player) {
        while (true) {
            delay(5000)
            val d = player.duration
            if (d > 0) state.updateProgress(request, player.currentPosition, d)
        }
    }
    BackHandler { onClose() }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                useController = true
                controllerShowTimeoutMs = 2000
                controllerAutoShow = true
                controllerHideOnTouch = true
                setShowSubtitleButton(true)
                setShowRewindButton(true)
                setShowFastForwardButton(true)
                this.player = player
                layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            }
        },
        modifier = Modifier.fillMaxSize().background(Color.Black)
    )
}

@Composable
private fun LoadingOverlay(text: String) {
    Box(Modifier.fillMaxSize().background(Color(0x99000000)), contentAlignment = Alignment.Center) {
        Text(
            text,
            color = Color.White,
            fontSize = 18.sp,
            modifier = Modifier
                .background(Color(0xEE17171D), RoundedCornerShape(14.dp))
                .padding(horizontal = 26.dp, vertical = 18.dp)
        )
    }
}

private fun formatPosition(ms: Long): String {
    val totalMinutes = (ms / 60_000L).coerceAtLeast(0)
    return if (totalMinutes >= 60) "${totalMinutes / 60}h ${totalMinutes % 60}m" else "${totalMinutes}m"
}
