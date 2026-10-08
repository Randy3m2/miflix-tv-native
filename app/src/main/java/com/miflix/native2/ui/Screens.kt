package com.miflix.native2.ui

import android.content.Intent
import android.net.Uri
import android.view.View
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.type
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
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
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.miflix.native2.BuildConfig
import com.miflix.native2.model.*
import kotlinx.coroutines.delay
import kotlin.math.abs

private val StreamingTiles = listOf(
    CollectionTile(
        "netflix", "Netflix",
        "https://cdn.jsdelivr.net/gh/luckynumb3rs/stremio-perfect-setup/collections/streaming/cover/netflix.png",
        "https://cdn.jsdelivr.net/gh/luckynumb3rs/stremio-perfect-setup/collections/streaming/focused/netflix.gif"
    ),
    CollectionTile(
        "disney", "Disney+",
        "https://cdn.jsdelivr.net/gh/luckynumb3rs/stremio-perfect-setup/collections/streaming/cover/disney-plus.png",
        "https://cdn.jsdelivr.net/gh/luckynumb3rs/stremio-perfect-setup/collections/streaming/focused/disney-plus.gif"
    ),
    CollectionTile(
        "prime", "Prime Video",
        "https://cdn.jsdelivr.net/gh/luckynumb3rs/stremio-perfect-setup/collections/streaming/cover/prime-video.png",
        "https://cdn.jsdelivr.net/gh/luckynumb3rs/stremio-perfect-setup/collections/streaming/focused/prime-video.gif"
    ),
    CollectionTile(
        "apple", "Apple TV+",
        "https://cdn.jsdelivr.net/gh/luckynumb3rs/stremio-perfect-setup/collections/streaming/cover/apple-tv.png",
        "https://cdn.jsdelivr.net/gh/luckynumb3rs/stremio-perfect-setup/collections/streaming/focused/apple-tv.gif"
    ),
    CollectionTile(
        "hbo", "HBO Max",
        "https://cdn.jsdelivr.net/gh/luckynumb3rs/stremio-perfect-setup/collections/streaming/cover/hbo-max.png",
        "https://cdn.jsdelivr.net/gh/luckynumb3rs/stremio-perfect-setup/collections/streaming/focused/hbo-max.gif"
    )
)

@Composable
private fun AppShell(state: AppState, screen: Screen, onNavigate: (Screen) -> Unit, content: @Composable BoxScope.() -> Unit) {
    Row(Modifier.fillMaxSize().background(Bg)) {
        Sidebar(screen, state.activeProfile.name, onNavigate, state.notifications.size)
        Box(Modifier.weight(1f).fillMaxHeight(), content = content)
    }
}

@Composable
fun HomeScreen(state: AppState, onNavigate: (Screen) -> Unit) {
    LaunchedEffect(Unit) {
        delay(550)
        state.loadHomeExtras()
    }
    val openItem: (MediaSummary) -> Unit = { item -> state.launch { state.open(item) } }
    val openCollection: (CollectionTile) -> Unit = { tile -> state.launch { state.openCollection(tile.id, tile.title) } }

    AppShell(state, Screen.HOME, onNavigate) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 64.dp)
        ) {
            item(key = "hero") { TopTenHero(state.trending.take(10), openItem) }
            if (state.continueWatching.isNotEmpty()) {
                item(key = "continue") {
                    MediaRail(
                        title = "Continue Watching",
                        rows = state.continueWatching,
                        landscape = true,
                        progressFor = { state.progress[it.cloudId]?.percent },
                        onClick = openItem
                    )
                }
            }
            item(key = "streaming") { CollectionRail("Streaming", StreamingTiles, openCollection) }
            item(key = "movies") { MediaRail("Popular · Movies", state.movies, onClick = openItem) }
            item(key = "series") { MediaRail("Popular · Series", state.series, onClick = openItem) }
            item(key = "top") { MediaRail("Top Rated", state.topRated, onClick = openItem) }
        }
    }
}

@Composable
private fun TopTenHero(rows: List<MediaSummary>, onOpen: (MediaSummary) -> Unit) {
    if (rows.isEmpty()) {
        Box(Modifier.fillMaxWidth().height(450.dp).background(Bg))
        return
    }
    var index by remember(rows.map { it.cloudId }) { mutableIntStateOf(0) }
    val safeIndex = index.coerceIn(0, rows.lastIndex)
    val hero = rows[safeIndex]

    LaunchedEffect(rows.map { it.cloudId }) {
        while (true) {
            delay(6500)
            index = (index + 1) % rows.size
        }
    }

    Box(Modifier.fillMaxWidth().height(500.dp)) {
        Crossfade(hero, animationSpec = tween(260, easing = LinearOutSlowInEasing), label = "topHero") { item ->
            AsyncImage(item.backdrop, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x08000000), Color(0x2C000000), Bg), startY = 120f)))
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xF8050505), Color(0xA0050505), Color.Transparent), endX = 860f)))
        Column(Modifier.align(Alignment.CenterStart).padding(start = 44.dp, top = 56.dp).widthIn(max = 720.dp)) {
            Text("TOP 10  ·  TRENDING THIS WEEK", color = Color(0xFFB8B8BC), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Text(hero.title, color = Color.White, fontSize = 43.sp, fontWeight = FontWeight.Black, maxLines = 2)
            Spacer(Modifier.height(10.dp))
            Text("${hero.year}  ·  ★ ${"%.1f".format(hero.rating)}", color = Color(0xFFE0E0E0), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            Text(hero.overview, color = Color(0xFFE7E7EA), fontSize = 15.sp, maxLines = 3, modifier = Modifier.widthIn(max = 690.dp))
            Spacer(Modifier.height(20.dp))
            FocusButton("View Details", primary = true) { onOpen(hero) }
        }
        Row(
            Modifier.align(Alignment.BottomEnd).padding(end = 34.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            rows.forEachIndexed { i, _ ->
                Box(
                    Modifier.width(if (i == safeIndex) 32.dp else 8.dp).height(8.dp)
                        .background(if (i == safeIndex) Color.White else Color(0xFFBDBDBD), RoundedCornerShape(8.dp))
                )
            }
        }
    }
}

@Composable
private fun MediaRail(
    title: String,
    rows: List<MediaSummary>,
    landscape: Boolean = false,
    progressFor: (MediaSummary) -> Double? = { null },
    onClick: (MediaSummary) -> Unit
) {
    if (rows.isEmpty()) return
    val h = if (landscape) 224.dp else 326.dp
    Column(Modifier.fillMaxWidth().height(h)) {
        Text(title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 34.dp, bottom = 8.dp))
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 24.dp, end = 64.dp),
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            items(rows, key = { it.cloudId }, contentType = { if (landscape) "landscape" else "poster" }) { item ->
                MediaCard(item, landscape = landscape, progressPercent = progressFor(item), onClick = { onClick(item) })
            }
        }
    }
}

@Composable
private fun CollectionRail(title: String, rows: List<CollectionTile>, onClick: (CollectionTile) -> Unit) {
    Column(Modifier.fillMaxWidth().height(235.dp)) {
        Text(title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 34.dp, bottom = 8.dp))
        LazyRow(
            contentPadding = PaddingValues(start = 24.dp, end = 64.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(rows, key = { it.id }) { tile -> CollectionCard(tile) { onClick(tile) } }
        }
    }
}

@Composable
fun CatalogScreen(state: AppState, title: String, catalog: List<MediaSummary>, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    AppShell(state, state.screen, { state.screen = it }) {
        Column(Modifier.fillMaxSize().padding(start = 30.dp, top = 30.dp, end = 46.dp)) {
            Text(title, color = Color.White, fontSize = 31.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(18.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp), contentPadding = PaddingValues(bottom = 60.dp, end = 30.dp)) {
                val chunks = catalog.chunked(5)
                items(chunks.size, key = { it }) { idx ->
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        chunks[idx].forEach { item ->
                            MediaCard(
                                item = item,
                                progressPercent = state.progress[item.cloudId]?.percent,
                                onClick = { state.launch { state.open(item) } }
                            )
                        }
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
    BackHandler(onBack = onBack)
    LaunchedEffect(query) {
        if (query.trim().length < 2) results = emptyList()
        else {
            delay(400)
            searching = true
            results = runCatching { state.tmdb.search(query.trim()) }.getOrElse { emptyList() }
            searching = false
        }
    }
    AppShell(state, Screen.SEARCH, { state.screen = it }) {
        Column(Modifier.fillMaxSize().padding(34.dp)) {
            Text("Search", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(16.dp))
            NativeTextField(query, { query = it }, "Movies, series…", modifier = Modifier.width(620.dp))
            Spacer(Modifier.height(18.dp))
            if (searching) Text("Searching…", color = Muted, fontSize = 14.sp)
            LazyColumn(contentPadding = PaddingValues(bottom = 50.dp, end = 40.dp)) {
                val chunks = results.chunked(5)
                items(chunks.size, key = { it }) { idx ->
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        chunks[idx].forEach { item -> MediaCard(item = item, onClick = { state.launch { state.open(item) } }) }
                    }
                }
            }
        }
    }
}

@Composable
fun CollectionsScreen(state: AppState, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    LaunchedEffect(Unit) { state.loadHomeExtras() }
    val years = (2026 downTo 1990).map { year -> CollectionTile("year:$year", year.toString(), "") }
    AppShell(state, Screen.COLLECTIONS, { state.screen = it }) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 34.dp, bottom = 60.dp)) {
            item {
                Column(Modifier.padding(horizontal = 34.dp)) {
                    Text("Collections", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(5.dp))
                    Text("Streaming services, genres and movies by year.", color = Muted, fontSize = 14.sp)
                    Spacer(Modifier.height(24.dp))
                }
            }
            item { CollectionRail("Streaming", StreamingTiles) { tile -> state.launch { state.openCollection(tile.id, tile.title) } } }
            item {
                Text("Movies by Year", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 34.dp, top = 8.dp, bottom = 10.dp))
                LazyRow(contentPadding = PaddingValues(start = 30.dp, end = 64.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(years, key = { it.id }) { tile ->
                        FocusButton(tile.title, modifier = Modifier.width(106.dp)) { state.launch { state.openCollection(tile.id, tile.title) } }
                    }
                }
                Spacer(Modifier.height(28.dp))
            }
        }
    }
}

@Composable
fun GenresScreen(state: AppState, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    AppShell(state, Screen.GENRES, { state.screen = it }) {
        Column(Modifier.fillMaxSize().padding(34.dp)) {
            Text("Genres", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(6.dp))
            Text("40–50 results per genre, mixing movies and series.", color = Muted, fontSize = 14.sp)
            Spacer(Modifier.height(24.dp))
            LazyColumn(contentPadding = PaddingValues(bottom = 50.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val chunks = state.genres.chunked(4)
                items(chunks.size, key = { it }) { idx ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        chunks[idx].forEach { g -> GenreCard(g) { state.launch { state.openGenre(g) } } }
                    }
                }
            }
        }
    }
}

@Composable
private fun GenreCard(g: GenreDefinition, onClick: () -> Unit) {
    var focused by remember(g.id) { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.02f else 1f, tween(90), label = "genreScale")
    Box(
        Modifier.width(280.dp).height(150.dp).graphicsLayer { scaleX = scale; scaleY = scale }
            .onFocusChanged { focused = it.isFocused }.focusable().tvClick(onClick).clickable(onClick = onClick)
            .clip(RoundedCornerShape(14.dp)).background(if (focused) Color.White else Color(0xFF171717))
            .border(if (focused) 2.dp else 1.dp, if (focused) Color.White else Color(0xFF292929), RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (g.coverUrl.isNotBlank()) {
            AsyncImage(g.coverUrl, g.title, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(Modifier.fillMaxSize().background(Color(0x66000000)))
        }
        Text(g.title, color = if (focused && g.coverUrl.isBlank()) Color.Black else Color.White, fontSize = 21.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
fun PlatformScreen(state: AppState, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    AppShell(state, Screen.PLATFORM_DETAIL, { state.screen = it }) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 32.dp, bottom = 64.dp)) {
            item {
                Column(Modifier.padding(horizontal = 34.dp)) {
                    Text(state.platformTitle, color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(4.dp))
                    Text("Top 10, movies, series, latest releases and best-rated picks by genre.", color = Muted, fontSize = 14.sp)
                    Spacer(Modifier.height(24.dp))
                }
            }
            items(state.platformSections, key = { it.id }) { section ->
                MediaRail(section.title, section.items, landscape = section.landscape, onClick = { state.launch { state.open(it) } })
            }
        }
    }
}

@Composable
fun DetailsScreen(state: AppState, onBack: () -> Unit) {
    val item = state.selected ?: return
    val details = state.details
    val saved = state.progress[item.cloudId]
    BackHandler(onBack = onBack)

    Box(Modifier.fillMaxSize().background(Bg)) {
        AsyncImage(item.backdrop, null, Modifier.fillMaxWidth().height(590.dp).align(Alignment.TopCenter), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x22000000), Color(0x66000000), Bg), startY = 150f)))
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xF7050505), Color(0xA8050505), Color.Transparent), endX = 980f)))

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 38.dp, end = 56.dp, top = 28.dp, bottom = 70.dp)
        ) {
            item(key = "hero") {
                Column(Modifier.fillMaxWidth().heightIn(min = 500.dp)) {
                    FocusButton("←", modifier = Modifier.width(66.dp), onClick = onBack)
                    Spacer(Modifier.height(58.dp))
                    Text(item.title, color = Color.White, fontSize = 46.sp, fontWeight = FontWeight.Black, maxLines = 2)
                    Spacer(Modifier.height(8.dp))
                    Text("${item.year}   ★ ${"%.1f".format(item.rating)}   ${details?.runtimeText.orEmpty()}", color = Color(0xFFD6D6D8), fontSize = 15.sp)
                    if (!details?.genres.isNullOrEmpty()) {
                        Spacer(Modifier.height(7.dp))
                        Text(details?.genres?.take(4)?.joinToString("  ·  ").orEmpty(), color = Color(0xFFB9B9BD), fontSize = 13.sp)
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(item.overview, color = Color(0xFFE8E8EB), fontSize = 16.sp, maxLines = 4, modifier = Modifier.widthIn(max = 860.dp))
                    Spacer(Modifier.height(24.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (item.type == "movie") {
                            val label = if ((saved?.percent ?: 0.0) in 1.0..95.0) "▶  Resume ${formatPosition(saved?.position ?: 0)}" else "▶  Play"
                            FocusButton(label, onLongClick = { state.launch { state.chooseCurrentSources() } }, primary = true, modifier = Modifier.widthIn(min = 330.dp)) {
                                state.launch { runCatching { state.playMovie() }.onFailure { state.error = it.message } }
                            }
                        } else {
                            val canResume = (saved?.season ?: 0) > 0 && (saved?.episode ?: 0) > 0 && (saved?.percent ?: 0.0) < 96.0
                            FocusButton(if (canResume) "▶  Continue S${saved?.season} E${saved?.episode}" else "▶  Play S1 E1", onLongClick = { state.launch { state.chooseCurrentSources() } }, primary = true, modifier = Modifier.widthIn(min = 330.dp)) {
                                state.launch { runCatching { if (canResume) state.resumeSeries() else state.playSeriesFromStart() }.onFailure { state.error = it.message } }
                            }
                        }
                        FocusButton(if (state.favorites.contains(item.cloudId)) "✓ My List" else "+ My List") {
                            state.toggleFavorite(item); state.launch { runCatching { state.pushCloud() } }
                        }
                        FocusButton("Watch Party") { state.screen = Screen.WATCH_PARTY }
                    }
                    Spacer(Modifier.height(38.dp))
                }
            }

            if (item.type == "series") {
                item(key = "episodes") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Season ${state.currentSeason}", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(16.dp))
                        if ((details?.seasonCount ?: 0) > 1) {
                            FocusButton("‹") { if (state.currentSeason > 1) state.launch { state.loadSeason(state.currentSeason - 1) } }
                            Spacer(Modifier.width(7.dp))
                            FocusButton("›") { if (state.currentSeason < (details?.seasonCount ?: 1)) state.launch { state.loadSeason(state.currentSeason + 1) } }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    LazyRow(modifier = Modifier.fillMaxWidth().height(236.dp), contentPadding = PaddingValues(end = 54.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(state.episodes, key = { "${it.season}:${it.episode}" }) { ep ->
                            EpisodeCard(ep, onLongClick = { state.launch { state.chooseEpisodeSources(ep) } }) { state.launch { runCatching { state.playEpisode(ep) }.onFailure { state.error = it.message } } }
                        }
                    }
                    Spacer(Modifier.height(28.dp))
                }
            }

            if (!details?.cast.isNullOrEmpty()) {
                item(key = "cast") {
                    Text("Cast", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(14.dp))
                    LazyRow(contentPadding = PaddingValues(end = 50.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        items(details?.cast.orEmpty(), key = { it.name }) { CastCard(it) }
                    }
                    Spacer(Modifier.height(28.dp))
                }
            }

            if (!details?.trailers.isNullOrEmpty()) {
                item(key = "trailers") {
                    Text("Trailers", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(14.dp))
                    LazyRow(contentPadding = PaddingValues(end = 50.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(details?.trailers.orEmpty(), key = { it.key }) { trailer ->
                            val context = LocalContext.current
                            TrailerCard(trailer) {
                                trailer.watchUrl?.let { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it))) } }
                            }
                        }
                    }
                    Spacer(Modifier.height(28.dp))
                }
            }

            item(key = "info") {
                Text(if (item.type == "movie") "Movie Details" else "Series Details", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(14.dp))
                InfoLine("Release Info", details?.releaseInfo.orEmpty())
                InfoLine("Runtime", details?.runtimeText.orEmpty())
                InfoLine("Origin Country", details?.originCountry.orEmpty())
                InfoLine("Original Language", details?.originalLanguage.orEmpty())
            }
        }
    }
}

@Composable
fun SettingsScreen(state: AppState, onBack: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var addonUrl by remember { mutableStateOf("") }
    val context = LocalContext.current
    BackHandler(onBack = onBack)

    AppShell(state, Screen.SETTINGS, { state.screen = it }) {
        LazyColumn(Modifier.fillMaxSize().padding(40.dp), contentPadding = PaddingValues(bottom = 60.dp)) {
            item {
                Text("Settings", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(8.dp))
                Text("MiFlix Native ${BuildConfig.VERSION_NAME} · Compose TV + Media3", color = Muted, fontSize = 14.sp)
                Spacer(Modifier.height(30.dp))

                if (state.session == null) {
                    Text("Account & Sync", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("Use email/password or scan the secure pairing QR with your phone.", color = Muted, fontSize = 14.sp, modifier = Modifier.width(760.dp))
                    Spacer(Modifier.height(16.dp))
                    NativeTextField(email, { email = it }, "Email", modifier = Modifier.width(520.dp))
                    Spacer(Modifier.height(10.dp))
                    NativeTextField(password, { password = it }, "Password", true, Modifier.width(520.dp))
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FocusButton("Sign In", primary = true) { state.launch { state.login(email.trim(), password) } }
                        FocusButton("QR Sign-in") { state.screen = Screen.PAIR_DEVICE }
                    }
                } else {
                    Text("Account & Sync", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("Signed in as ${state.session?.email}", color = Color(0xFFE4E4E6), fontSize = 16.sp)
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FocusButton("Sync Now", primary = true) { state.launch { state.syncFromCloud(); state.pushCloud(); if (state.tmdb.token.isNotBlank()) state.loadHome() } }
                        FocusButton("Pair / Add Add-on") { state.screen = Screen.PAIR_DEVICE }
                        FocusButton("Sign Out") { state.signOut() }
                    }
                }

                Spacer(Modifier.height(28.dp))
                Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    FocusButton("Friends / Nickname / Phrases") { state.screen=Screen.FRIENDS }
                    FocusButton("Trakt") { state.screen=Screen.TRAKT }
                }
                Spacer(Modifier.height(18.dp))
                Text("Playback preferences",color=Color.White,fontSize=22.sp)
                Text("Audio: ${state.audioLanguage.ifBlank { "Automatic" }}",color=Muted)
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    listOf("" to "Auto", "es" to "Español", "en" to "English", "pt" to "Português", "fr" to "Français").forEach { (code,label) ->
                        FocusButton(label,primary=state.audioLanguage==code) { state.savePlaybackPreferences(code,state.subtitleLanguage,state.autoplayNext) }
                    }
                }
                Text("Subtitles: ${state.subtitleLanguage}",color=Muted)
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    listOf("off" to "Off", "es" to "Español", "en" to "English", "pt" to "Português", "fr" to "Français").forEach { (code,label) ->
                        FocusButton(label,primary=state.subtitleLanguage==code) { state.savePlaybackPreferences(state.audioLanguage,code,state.autoplayNext) }
                    }
                }
                FocusButton("Autoplay next episode: ${if(state.autoplayNext) "ON" else "OFF"}") { state.savePlaybackPreferences(state.audioLanguage,state.subtitleLanguage,!state.autoplayNext) }
                Spacer(Modifier.height(24.dp))
                Text("Stream Add-ons", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("Torrentio and Comet manifests are account-level, so every profile on this account shares them.", color = Muted, fontSize = 14.sp, modifier = Modifier.width(840.dp))
                Spacer(Modifier.height(12.dp))
                state.streamRepo.manifests().forEach { manifest ->
                    Text("• ${runCatching { Uri.parse(manifest).host }.getOrNull() ?: "Configured add-on"} · configured", color = Color(0xFFE4E4E6), fontSize = 12.sp, maxLines = 1)
                }
                Spacer(Modifier.height(12.dp))
                NativeTextField(addonUrl, { addonUrl = it }, "Torrentio / Comet manifest URL", modifier = Modifier.width(760.dp))
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FocusButton("Add Manifest") { state.launch { state.addAddonManifest(addonUrl); addonUrl = "" } }
                    FocusButton("Use QR") { state.screen = Screen.PAIR_DEVICE }
                }

                Spacer(Modifier.height(34.dp))
                Text("Updates", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("The latest channel keeps the same APK URL, so your Downloader code stays permanent.", color = Muted, fontSize = 14.sp, modifier = Modifier.width(760.dp))
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FocusButton(if (state.updateChecking) "Checking…" else "Check for Updates", primary = true) {
                        if (!state.updateChecking) state.launch { state.checkForUpdates() }
                    }
                    state.updateInfo?.takeIf { it.isNewer }?.let { info ->
                        FocusButton("Open ${info.version}") { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(info.downloadUrl))) } }
                    }
                }
                state.updateInfo?.let { info ->
                    Spacer(Modifier.height(9.dp))
                    Text(if (info.isNewer) "Update available: ${info.version}" else "You're on the latest build.", color = Color(0xFFE7E7E9), fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun PairDeviceScreen(state: AppState, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    LaunchedEffect(Unit) { if (state.pairingRequest == null) state.startPairing() }
    LaunchedEffect(state.pairingRequest?.code) {
        while (state.pairingRequest != null) {
            delay(1800)
            if (state.pollPairing()) break
        }
    }
    Box(Modifier.fillMaxSize().background(Bg), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.widthIn(max = 900.dp)) {
            Text("Pair MiFlix", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(10.dp))
            Text("Scan this QR on your phone to sign in and/or send a Torrentio or Comet manifest without typing it on the TV.", color = Muted, fontSize = 15.sp)
            Spacer(Modifier.height(24.dp))
            state.pairingRequest?.let { req ->
                QrCode(req.url, 260.dp)
                Spacer(Modifier.height(16.dp))
                Text(state.pairingStatus, color = Color.White, fontSize = 15.sp)
                Text("Expires in about 10 minutes", color = Muted, fontSize = 12.sp)
            } ?: Text(state.pairingStatus.ifBlank { "Preparing…" }, color = Color.White, fontSize = 16.sp)
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FocusButton("Back", primary = true) { onBack() }
                FocusButton("New QR") { state.pairingRequest = null; state.launch { state.startPairing() } }
            }
        }
    }
}

@Composable
fun ProfilesScreen(state: AppState, onBack: () -> Unit) {
    var newName by remember { mutableStateOf("") }
    BackHandler(onBack = onBack)
    Column(Modifier.fillMaxSize().background(Bg).padding(64.dp)) {
        Text("Who's watching?", color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(8.dp))
        Text("TMDB, add-ons, collections and app settings are shared. My List and progress stay separate per profile.", color = Muted, fontSize = 14.sp)
        Spacer(Modifier.height(30.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            state.profiles.forEach { p ->
                var focused by remember(p.id) { mutableStateOf(false) }
                val scale by animateFloatAsState(if (focused) 1.025f else 1f, tween(90, easing = LinearOutSlowInEasing), label = "profileScale")
                Column(
                    Modifier.width(150.dp).graphicsLayer { scaleX = scale; scaleY = scale }
                        .onFocusChanged { focused = it.isFocused }.focusable()
                        .tvClick { state.launch { state.selectProfile(p) } }
                        .clickable { state.launch { state.selectProfile(p) } },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        Modifier.size(110.dp).clip(RoundedCornerShape(55.dp)).background(if (focused) Color.White else Color(0xFF242424))
                            .border(if (focused) 2.dp else 0.dp, Color.White, RoundedCornerShape(55.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(p.name.take(1).uppercase(), color = if (focused) Color.Black else Color.White, fontSize = 38.sp, fontWeight = FontWeight.Black)
                    }
                    Spacer(Modifier.height(9.dp))
                    Text(p.name, color = Color.White, fontSize = 14.sp)
                }
            }
        }
        Spacer(Modifier.height(36.dp))
        Text("Create profile", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            NativeTextField(newName, { newName = it }, "Profile name", modifier = Modifier.width(360.dp))
            FocusButton("Add Profile", primary = true) { state.launch { state.createProfile(newName); newName = "" } }
        }
    }
}

@Composable
fun WatchPartyScreen(state: AppState, onBack: () -> Unit) {
    var joinCode by remember { mutableStateOf("") }
    BackHandler(onBack = onBack)
    AppShell(state, Screen.WATCH_PARTY, { state.screen=it }) {
        LazyColumn(Modifier.fillMaxSize().padding(28.dp),verticalArrangement=Arrangement.spacedBy(16.dp),contentPadding=PaddingValues(bottom=40.dp)) {
            item {
                Text("Watch Party",color=Color.White,fontSize=32.sp,fontWeight=FontWeight.Bold)
                Text("One room and QR for all content. Scan on your phone for live chat.",color=Muted)
                Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                    FocusButton("Friends / Nickname") { state.screen=Screen.FRIENDS }
                    FocusButton("Back") { onBack() }
                }
            }
            val party=state.watchParty
            if(party==null) {
                item {
                    FocusButton("Create Room",primary=true) { state.launch { state.socialAction { state.createWatchParty() } } }
                    if(state.selected!=null || state.playerRequest!=null) FocusButton("Create with current title") { state.launch { state.socialAction { state.createWatchParty(true) } } }
                    NativeTextField(joinCode,{joinCode=it.filter(Char::isDigit).take(6)},"6-digit room code")
                    FocusButton("Join") { state.launch { state.socialAction { state.joinWatchParty(joinCode) } } }
                }
            } else {
                item {
                    Row(horizontalArrangement=Arrangement.spacedBy(25.dp)) {
                        QrCode("https://randy3m2.github.io/miflix-tv-native/party/?room=${party.roomCode}",210.dp)
                        Column {
                            Text(party.roomCode,color=Color.White,fontSize=42.sp,fontWeight=FontWeight.Bold)
                            Text(if(state.partyRole==PartyRole.HOST) "Host · same code when changing content" else "Guest · follows the host",color=Muted)
                            FocusButton("Choose content") { state.screen=Screen.HOME }
                            if(state.playerRequest!=null) FocusButton("Return to playback") { state.screen=Screen.PLAYER }
                            FocusButton("Leave / close room") { state.launch { state.leaveWatchParty(); onBack() } }
                        }
                    }
                }
                item { LazyRow(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                    items(PartyEmojis) { emoji -> FocusButton(emoji) { state.launch { state.sendPartyEvent("emoji",emoji) } } }
                } }
                item { Text("Quick phrases",color=Color.White,fontSize=20.sp) }
                items(state.partyPhrases) { phrase -> FocusButton(phrase) { state.launch { state.sendPartyEvent("phrase",phrase) } } }
                item { Text("Participants · add as friend",color=Color.White,fontSize=20.sp) }
                items(state.partyMembers,key={it.id}) { person ->
                    if(person.id!=state.session?.userId) FocusButton("@${person.nickname} · Add friend") { state.launch { state.addFriend(person.id) } }
                    else Text("@${state.nickname.ifBlank { "You" }}",color=Muted)
                }
                item { Text("Recent chat",color=Color.White,fontSize=20.sp) }
                items(state.partyMessages.takeLast(10),key={it.id}) { event ->
                    val name=state.partyMembers.firstOrNull { it.id==event.userId }?.nickname?:"Guest"
                    Text("@$name · ${event.body}",color=Color.White,fontSize=15.sp)
                }
            }
            if(state.partyStatus.isNotBlank()) item { Text(state.partyStatus,color=Muted) }
        }
    }
}

@Composable
fun PlayerScreen(state: AppState, request: PlayerRequest, onClose: () -> Unit) {
    val context = LocalContext.current
    val playbackView = LocalView.current
    var controlsVisible by remember(request.playbackId) { mutableStateOf(true) }
    var controlInteraction by remember { mutableStateOf(0) }
    var nativePlayerView by remember { mutableStateOf<PlayerView?>(null) }
    var showPartyActions by remember { mutableStateOf(false) }
    var showEpisodes by remember(request.playbackId) { mutableStateOf(false) }
    val subtitles = request.stream.subtitles
    val preferredText = state.subtitleLanguage.takeUnless { it == "off" }
    var paused by remember(request.playbackId) { mutableStateOf(false) }
    var pauseSynopsis by remember(request.playbackId) { mutableStateOf(false) }
    var clockNow by remember { mutableStateOf(System.currentTimeMillis()) }
    var applyingRemote by remember { mutableStateOf(false) }
    var positionMs by remember(request.playbackId) { mutableStateOf(request.resumeMs) }
    var durationMs by remember(request.playbackId) { mutableStateOf(0L) }
    var segments by remember(request.playbackId) { mutableStateOf(PlaybackSegments()) }
    var episodeInfo by remember(request.playbackId) { mutableStateOf<EpisodeSummary?>(null) }
    var upcoming by remember(request.playbackId) { mutableStateOf<EpisodeSummary?>(null) }
    var recommendations by remember(request.playbackId) { mutableStateOf<List<MediaSummary>>(emptyList()) }
    var ended by remember(request.playbackId) { mutableStateOf(false) }
    var countdown by remember(request.playbackId) { mutableStateOf(10) }
    var cancelAutoplay by remember(request.playbackId) { mutableStateOf(false) }

    val player = remember(request.playbackId) {
        val renderers = DefaultRenderersFactory(context).setEnableDecoderFallback(true)
        val httpFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setDefaultRequestProperties(request.stream.requestHeaders)
        val dataFactory = DefaultDataSource.Factory(context, httpFactory)
        ExoPlayer.Builder(context, renderers)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataFactory))
            .build().apply {
            setSeekBackIncrementMs(10_000)
            setSeekForwardIncrementMs(10_000)
            val builder = MediaItem.Builder().setUri(request.stream.url)
            if (subtitles.isNotEmpty()) {
                builder.setSubtitleConfigurations(
                    subtitles.sortedBy { if(normalizeMediaLanguage(it.lang) == preferredText) 0 else 1 }.take(60).map { s ->
                        MediaItem.SubtitleConfiguration.Builder(Uri.parse(s.url)).apply {
                            if (s.lang.isNotBlank()) setLanguage(normalizeMediaLanguage(s.lang))
                            setLabel(s.label.ifBlank { s.lang.ifBlank { "Subtitle" } })
                            setMimeType(subtitleMime(s.url))
                        }.build()
                    }
                )
            }
            setMediaItem(builder.build(), request.resumeMs)
            val trackBuilder = TrackSelectionParameters.Builder(context).setPreferredAudioLanguage(state.audioLanguage.takeIf { it.isNotBlank() })
            if (preferredText != null) trackBuilder.setPreferredTextLanguage(preferredText)
            trackBuilder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT,state.subtitleLanguage == "off")
            trackSelectionParameters = trackBuilder.build()
            videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
            prepare()
            playWhenReady = if (state.partyRole == PartyRole.GUEST) state.watchParty?.playing ?: true else true
        }
    }

    LaunchedEffect(request.playbackId) {
        if(!request.live) {
            state.launch { segments = state.playbackSegments(request) }
            state.launch { recommendations = runCatching { state.tmdb.recommendations(request.item) }.getOrDefault(emptyList()) }
            if(request.item.type == "series") {
                state.launch { episodeInfo = runCatching { state.tmdb.episodeInfo(request.item,request.season,request.episode) }.getOrNull() }
                state.launch { upcoming = runCatching { state.nextEpisode(request) }.getOrNull() }
            }
        }
    }
    LaunchedEffect(player) {
        while(true) { positionMs = player.currentPosition; durationMs = player.duration.coerceAtLeast(0); delay(250) }
    }
    LaunchedEffect(paused) {
        if(paused) while(true) { clockNow = System.currentTimeMillis(); delay(1000) }
    }
    LaunchedEffect(paused,request.playbackId) {
        pauseSynopsis = false
        if(paused) { delay(5000); pauseSynopsis = true }
    }
    LaunchedEffect(ended,upcoming,cancelAutoplay,state.autoplayNext) {
        if(ended && upcoming != null && state.autoplayNext && !cancelAutoplay && state.partyRole != PartyRole.GUEST) {
            countdown = 10
            while(countdown > 0) { delay(1000); countdown-- }
            state.launch { runCatching { state.playNext(request,upcoming!!) }.onFailure { state.error=it.message } }
        }
    }
    LaunchedEffect(player) { showEpisodes = false }

    DisposableEffect(player, playbackView) {
        val previousKeepScreenOn = playbackView.keepScreenOn
        fun updateScreenAwake() {
            playbackView.keepScreenOn = previousKeepScreenOn || (
                player.playWhenReady &&
                    (player.playbackState == Player.STATE_READY || player.playbackState == Player.STATE_BUFFERING)
                )
        }
        val listener = object : Player.Listener {
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                paused = !playWhenReady && player.playbackState != Player.STATE_ENDED
                if(state.partyRole == PartyRole.GUEST && !applyingRemote && reason == Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST) {
                    if(!playWhenReady && state.watchParty?.playing == true) state.launch { runCatching { state.requestPartyPause(request) }.onFailure { state.error=it.message } }
                    else if(state.watchParty?.playing == false) {
                        applyingRemote = true; player.pause(); applyingRemote = false
                    }
                }
            }
            override fun onEvents(player: Player, events: Player.Events) {
                updateScreenAwake()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    ended = true; paused = false
                    val duration = player.duration.coerceAtLeast(0)
                    if (duration > 0) state.updateProgress(request, duration, duration, ended = true)
                    state.launch { state.traktMarkWatched(request,duration,duration) }
                    state.launch { runCatching { state.pushCloud() } }
                }
            }
        }
        player.addListener(listener)
        paused = !player.playWhenReady
        updateScreenAwake()
        onDispose {
            playbackView.keepScreenOn = previousKeepScreenOn
            player.removeListener(listener)
            val lastPosition=player.currentPosition
            val lastDuration=player.duration.coerceAtLeast(0)
            state.updateProgress(request, lastPosition, lastDuration)
            state.launch { state.traktMarkWatched(request,lastPosition,lastDuration) }
            state.launch { runCatching { state.pushCloud() } }
            player.release()
        }
    }

    LaunchedEffect(player, request.stream.url) {
        while (true) {
            delay(4000)
            val d = player.duration
            if (d > 0) state.updateProgress(request, player.currentPosition, d)
            if (d > 0 && player.currentPosition.toDouble()/d>=0.8) state.launch { state.traktMarkWatched(request,player.currentPosition,d) }
        }
    }

    LaunchedEffect(player, state.partyRole, state.watchParty?.roomCode, request.stream.url) {
        while (state.watchParty != null) {
            delay(1000)
            when (state.partyRole) {
                PartyRole.HOST -> {
                    if(runCatching { state.takePartyPause() }.getOrDefault(false)) player.pause()
                    state.hostPartyUpdate(request, player.currentPosition, player.playWhenReady && player.playbackState != Player.STATE_ENDED)
                }
                PartyRole.GUEST -> {
                    val remote = state.refreshParty() ?: continue
                    if (remote.cloudId != request.item.cloudId || remote.season != request.season || remote.episode != request.episode) {
                        state.switchToPartyState(remote)
                        break
                    }
                    if (!request.live && abs(remote.positionMs - player.currentPosition) > 1800) player.seekTo(remote.positionMs)
                    if (remote.playing != player.playWhenReady) {
                        applyingRemote = true; player.playWhenReady = remote.playing; applyingRemote = false
                    }
                }
                null -> Unit
            }
        }
    }

    // Explicit timer also covers controls holding focus on TV remotes.
    LaunchedEffect(player, controlInteraction, showEpisodes, showPartyActions, state.sourceSelection) {
        if (!showEpisodes && !showPartyActions && state.sourceSelection == null) {
            delay(2000)
            controlsVisible = false
            nativePlayerView?.hideController()
            nativePlayerView?.requestFocus()
        }
    }

    BackHandler {
        if (showEpisodes) showEpisodes = false else onClose()
    }

    Box(Modifier.fillMaxSize().background(Color.Black).onPreviewKeyEvent { event ->
        if (event.type == KeyEventType.KeyDown && !showEpisodes && state.sourceSelection == null) {
            controlsVisible = true
            controlInteraction++
        }
        false
    }) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    nativePlayerView = this
                    useController = true
                    setControllerVisibilityListener(PlayerView.ControllerVisibilityListener { visibility ->
                        if (visibility != View.VISIBLE) controlsVisible = false
                    })
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
            update = { view ->
                if (view.player !== player) view.player = player
            },
            modifier = Modifier.fillMaxSize()
        )

        if(controlsVisible && !pauseSynopsis) {
            Column(Modifier.align(Alignment.TopStart).padding(26.dp).widthIn(max=600.dp).background(Color(0x99000000),RoundedCornerShape(12.dp)).padding(12.dp)) {
                Text(request.item.title,color=Color.White,fontSize=22.sp,fontWeight=FontWeight.Bold,maxLines=1)
                if(request.item.type == "series") Text("S${request.season} E${request.episode} · ${episodeInfo?.title.orEmpty()}",color=Muted,fontSize=14.sp)
            }
        }
        if(pauseSynopsis && !showEpisodes && !showPartyActions && state.sourceSelection == null && !ended) {
            Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xC9000000),Color(0x22000000))))) {
                Column(Modifier.align(Alignment.CenterStart).padding(60.dp).widthIn(max=680.dp)) {
                    Text("Estás viendo",color=Muted,fontSize=18.sp)
                    Text(request.item.title,color=Color.White,fontSize=38.sp,fontWeight=FontWeight.Bold)
                    if(request.item.type == "series") Text("S${request.season} E${request.episode} · ${episodeInfo?.title.orEmpty()}",color=Color.White,fontSize=22.sp)
                    Spacer(Modifier.height(14.dp))
                    Text(episodeInfo?.overview?.takeIf { it.isNotBlank() } ?: request.item.overview,color=Color(0xFFE0E0E0),fontSize=19.sp,maxLines=5)
                }

            }
        }
        if(paused && !ended && !showEpisodes && !showPartyActions && state.sourceSelection == null) {
            val clock = android.text.format.DateFormat.getTimeFormat(context).format(java.util.Date(clockNow))
            val remaining = (durationMs - positionMs).coerceAtLeast(0L) / 1000
            val remainingText = when {
                request.live -> "En vivo"
                durationMs <= 0 -> "Tiempo restante no disponible"
                remaining >= 3600 -> "%d:%02d:%02d".format(remaining / 3600, remaining / 60 % 60, remaining % 60)
                else -> "%d:%02d".format(remaining / 60, remaining % 60)
            }
            Column(Modifier.align(Alignment.BottomEnd).padding(end=35.dp,bottom=100.dp)
                .background(Color(0xAA000000),RoundedCornerShape(12.dp)).padding(14.dp)) {
                Text("Hora · $clock",color=Color.White,fontSize=20.sp)
                Text(if(request.live || durationMs <= 0) remainingText else "Restante · $remainingText",color=Color.White,fontSize=17.sp)
                Text("Pausado",color=Muted,fontSize=14.sp)
            }
        }
        if(!request.live && controlsVisible && !showEpisodes && !showPartyActions) {
            Box(Modifier.align(Alignment.BottomStart).padding(start=26.dp,bottom=110.dp)) {
                FocusButton("Playback links") { state.launch { state.playerSources(request,player.currentPosition) } }
            }
        }
        if(!ended && !showEpisodes && state.partyRole != PartyRole.GUEST && durationMs > 0) {
            val intro = segments.intro?.takeIf { it.endMs <= durationMs && positionMs >= it.startMs && positionMs < it.endMs }
            val credits = segments.outro?.takeIf { it.startMs < durationMs && positionMs >= it.startMs && positionMs < it.endMs }
            if(intro != null || credits != null) Box(Modifier.align(Alignment.BottomEnd).padding(end=30.dp,bottom=100.dp)) {
                if(intro != null) FocusButton("Skip Intro",primary=true) { player.seekTo(intro.endMs) }
                else if(credits != null) FocusButton("Skip Credits",primary=true) {
                    val post = segments.postCredits?.startMs?.takeIf { it > positionMs && it < durationMs }
                    player.seekTo(post ?: credits.endMs.coerceAtMost(durationMs))
                }
            }
        }
        if(ended && state.partyRole != PartyRole.GUEST) {
            Dialog(onDismissRequest={ cancelAutoplay=true; onClose() },properties=DialogProperties(usePlatformDefaultWidth=false)) {
                Column(Modifier.width(850.dp).heightIn(max=600.dp).background(Color(0xF00B0B0B),RoundedCornerShape(20.dp)).padding(25.dp)) {
                    Text("Terminaste ${request.item.title}",color=Color.White,fontSize=26.sp)
                    upcoming?.let { next ->
                        Text("Siguiente: S${next.season} E${next.episode} · ${next.title}",color=Muted)
                        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                            FocusButton(if(state.autoplayNext && !cancelAutoplay) "Next episode · ${countdown}s" else "Next episode",primary=true) { cancelAutoplay=true; state.launch { state.playNext(request,next) } }
                            FocusButton("Cancel autoplay") { cancelAutoplay=true }
                        }
                    }
                    Text("También te puede gustar",color=Color.White,fontSize=22.sp)
                    LazyRow(horizontalArrangement=Arrangement.spacedBy(12.dp),modifier=Modifier.weight(1f,false)) {
                        items(recommendations,key={it.cloudId}) { item ->
                            Column(Modifier.width(170.dp)) {
                                AsyncImage(item.poster,item.title,Modifier.height(190.dp).fillMaxWidth(),contentScale=ContentScale.Crop)
                                FocusButton(item.title) { cancelAutoplay=true; state.launch { state.open(item) } }
                            }
                        }
                    }
                    FocusButton("Back to details") { cancelAutoplay=true; onClose() }
                }
            }
        }
        if (controlsVisible && !showEpisodes && !showPartyActions && request.item.type == "series") {
            Row(
                Modifier.align(Alignment.TopEnd).padding(top = 24.dp, end = 26.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FocusButton("Episodes") { player.pause(); showEpisodes = true }
                if (state.watchParty != null) FocusButton("React / Chat") { showPartyActions = true }
                else FocusButton("Create Party") { state.screen=Screen.WATCH_PARTY }
            }
        } else if (controlsVisible && !showEpisodes && !showPartyActions) {
            Box(Modifier.align(Alignment.TopEnd).padding(top = 24.dp, end = 26.dp)) {
                if(state.watchParty!=null) FocusButton("React / Chat") { showPartyActions = true }
                else FocusButton("Create Party") { state.screen=Screen.WATCH_PARTY }
            }
        }

        if (showPartyActions) PartyActionsDialog(state) { showPartyActions = false }
        if (state.watchParty != null && state.floatingEvents.isNotEmpty()) {
            Column(Modifier.align(Alignment.CenterEnd).padding(30.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                state.floatingEvents.toList().forEach { event ->
                    val name=state.partyMembers.firstOrNull { it.id==event.userId }?.nickname ?: "Guest"
                    Text("@$name  ${event.body}",color=Color.White,maxLines=3,fontSize=if(event.kind=="emoji") 30.sp else 18.sp,
                        modifier=Modifier.widthIn(max=450.dp).background(Color(0xCC181818),RoundedCornerShape(16.dp)).padding(16.dp))
                }
            }
        }
        if (showEpisodes) {
            EpisodePickerOverlay(state, request) {
                showEpisodes = false
                player.play()
            }
        }

        if (controlsVisible && !showEpisodes && subtitles.isNotEmpty()) {
            Text(
                "CC ${subtitles.count { it.lang == "es" || it.lang == "en" }} EN/ES · ${subtitles.size} total",
                color = Color(0xFFDDDDDF), fontSize = 11.sp,
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 24.dp, bottom = 18.dp).background(Color(0x99000000), RoundedCornerShape(8.dp)).padding(8.dp)
            )
        }
    }
}

@Composable
private fun EpisodePickerOverlay(state: AppState, request: PlayerRequest, onClose: () -> Unit) {
    val details = state.details
    Box(Modifier.fillMaxSize().background(Color(0xB8000000))) {
        Column(
            Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(620.dp).background(Color(0xFF0B0B0B)).padding(26.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Episodes", color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.weight(1f))
                FocusButton("Close") { onClose() }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                FocusButton("‹") { if (state.currentSeason > 1) state.launch { state.loadSeason(state.currentSeason - 1) } }
                Text("Season ${state.currentSeason}", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                FocusButton("›") { if (state.currentSeason < (details?.seasonCount ?: 1)) state.launch { state.loadSeason(state.currentSeason + 1) } }
            }
            Spacer(Modifier.height(16.dp))
            LazyColumn(contentPadding = PaddingValues(bottom = 30.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(state.episodes, key = { "${it.season}:${it.episode}" }) { ep ->
                    val active = ep.season == request.season && ep.episode == request.episode
                    EpisodeListRow(ep, active, onLongClick = { state.launch { state.chooseEpisodeSources(ep) } }) {
                        state.launch {
                            runCatching { state.playEpisode(ep) }.onFailure { state.error = it.message }
                            onClose()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EpisodeListRow(ep: EpisodeSummary, active: Boolean, onLongClick: () -> Unit, onClick: () -> Unit) {
    var focused by remember(ep.season, ep.episode) { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused }.focusable().tvPlaybackClick(onClick, onLongClick).clickable(onClick = onClick)
            .background(if (focused) Color.White else if (active) Color(0xFF242424) else Color(0xFF161616), RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(ep.still, ep.title, Modifier.width(150.dp).height(84.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("S${ep.season} E${ep.episode} · ${ep.title}", color = if (focused) Color.Black else Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(ep.overview, color = if (focused) Color(0xFF333333) else Muted, fontSize = 11.sp, maxLines = 2)
        }
        if (active) Text("NOW", color = if (focused) Color.Black else Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    if (value.isBlank()) return
    Row(Modifier.widthIn(max = 900.dp).fillMaxWidth().border(0.5.dp, Color(0x332F2F2F)).padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color(0xFFCACACD), fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(240.dp))
        Text(value, color = Color.White, fontSize = 15.sp)
    }
}

@Composable
private fun CastCard(person: CastMember) {
    Column(Modifier.width(138.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(106.dp).clip(RoundedCornerShape(53.dp)).background(Color(0xFF252525)), contentAlignment = Alignment.Center) {
            if (!person.profile.isNullOrBlank()) AsyncImage(person.profile, person.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else Text(person.name.split(" ").take(2).mapNotNull { it.firstOrNull()?.toString() }.joinToString(""), color = Color(0xFFAAAAAF), fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        Text(person.name, color = Color.White, fontSize = 13.sp, maxLines = 1)
        if (person.character.isNotBlank()) Text(person.character, color = Muted, fontSize = 10.sp, maxLines = 1)
    }
}

@Composable
private fun TrailerCard(trailer: TrailerSummary, onClick: () -> Unit) {
    var focused by remember(trailer.key) { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.02f else 1f, tween(90, easing = LinearOutSlowInEasing), label = "trailerScale")
    Column(Modifier.width(330.dp).graphicsLayer { scaleX = scale; scaleY = scale }.onFocusChanged { focused = it.isFocused }.focusable().tvClick(onClick).clickable(onClick = onClick)) {
        Box(Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF1A1A1A)).border(if (focused) 2.dp else 0.dp, Color.White, RoundedCornerShape(12.dp))) {
            AsyncImage(trailer.thumbnail, trailer.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(Modifier.fillMaxSize().background(Color(0x33000000)), contentAlignment = Alignment.Center) { Text("▶", color = Color.White, fontSize = 31.sp) }
        }
        Spacer(Modifier.height(7.dp))
        Text(trailer.name, color = Color.White, fontSize = 13.sp, maxLines = 1)
    }
}

@Composable
private fun EpisodeCard(ep: EpisodeSummary, onLongClick: () -> Unit, onClick: () -> Unit) {
    var focused by remember(ep.season, ep.episode) { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.02f else 1f, tween(90, easing = LinearOutSlowInEasing), label = "episodeScale")
    Box(Modifier.width(328.dp).height(225.dp), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.width(316.dp).graphicsLayer { scaleX = scale; scaleY = scale }.onFocusChanged { focused = it.isFocused }.focusable().tvPlaybackClick(onClick, onLongClick).clickable(onClick = onClick)) {
            Box(Modifier.fillMaxWidth().height(176.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF1B1B1B)).border(if (focused) 2.dp else 0.dp, Color.White, RoundedCornerShape(12.dp))) {
                AsyncImage(ep.still, ep.title, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                if (focused) Box(Modifier.fillMaxSize().background(Color(0x26000000)), contentAlignment = Alignment.Center) { Text("▶", color = Color.White, fontSize = 32.sp) }
            }
            Spacer(Modifier.height(7.dp))
            Text("S${ep.season} E${ep.episode} · ${ep.title}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            if (ep.airDate.isNotBlank()) Text(ep.airDate, color = Muted, fontSize = 10.sp, maxLines = 1)
        }
    }
}

private fun normalizeMediaLanguage(raw: String): String = when (raw.lowercase()) {
    "spa", "es-es", "spanish" -> "es"
    "eng", "en-us", "english" -> "en"
    "por", "pt-br", "pt-pt", "portuguese" -> "pt"
    "fre", "fra", "fr-fr", "french" -> "fr"
    else -> raw
}

private fun subtitleMime(url: String): String {
    val x = url.substringBefore('?').lowercase()
    return when {
        x.endsWith(".vtt") -> MimeTypes.TEXT_VTT
        else -> MimeTypes.APPLICATION_SUBRIP
    }
}

private fun formatPosition(ms: Long): String {
    val totalMinutes = (ms / 60_000L).coerceAtLeast(0)
    return if (totalMinutes >= 60) "${totalMinutes / 60}h ${totalMinutes % 60}m" else "${totalMinutes}m"
}

@Composable
fun SourcePickerOverlay(state: AppState, selection: SourceSelection) {
    val firstLink = remember(selection) { FocusRequester() }
    LaunchedEffect(selection) { delay(100); firstLink.requestFocus() }
    Dialog(
        onDismissRequest = { state.sourceSelection = null },
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)
    ) {
        Box(Modifier.fillMaxSize().background(Color(0xFF050505)).padding(40.dp)) {
            Column {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Playback links · ${selection.streams.size}", color = Color.White, fontSize = 26.sp,
                        fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    FocusButton("Close") { state.sourceSelection = null }
                }
                Spacer(Modifier.height(18.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 30.dp)) {
                    items(selection.streams) { stream ->
                        FocusButton(listOf(stream.addonName, stream.name, stream.title).filter { it.isNotBlank() }.joinToString(" · "),
                            modifier = Modifier.fillMaxWidth().then(
                                if (stream === selection.streams.first()) Modifier.focusRequester(firstLink) else Modifier
                            )) {
                            state.launch { state.playSelectedSource(selection, stream) }
                        }
                    }
                }
            }
        }
    }
}
