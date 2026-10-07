package com.miflix.native2.ui

import android.content.Intent
import android.net.Uri
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
import com.miflix.native2.BuildConfig
import com.miflix.native2.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

private val GenreTiles = listOf(
    CollectionTile("action", "Action", "https://raw.githubusercontent.com/rrevanth/nuvio-assets/main/genres/action/action-landscape.png"),
    CollectionTile("scifi", "Science Fiction", "https://raw.githubusercontent.com/rrevanth/nuvio-assets/main/genres/sci-fi/sci-fi-landscape.png")
)

@Composable
private fun AppShell(state: AppState, screen: Screen, onNavigate: (Screen) -> Unit, content: @Composable BoxScope.() -> Unit) {
    Row(Modifier.fillMaxSize().background(Bg)) {
        Sidebar(screen, state.activeProfile.name, onNavigate)
        Box(Modifier.weight(1f).fillMaxHeight(), content = content)
    }
}

@Composable
fun HomeScreen(state: AppState, onNavigate: (Screen) -> Unit) {
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        delay(700)
        state.loadHomeExtras()
    }

    val openItem: (MediaSummary) -> Unit = remember(state) { { item -> scope.launch { state.open(item) } } }
    val openCollection: (CollectionTile) -> Unit = remember(state) {
        { tile -> scope.launch { state.openCollection(tile.id, tile.title) } }
    }

    AppShell(state, Screen.HOME, onNavigate) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 54.dp)
        ) {
            item(key = "hero") {
                TopTenHero(state.trending.take(10), openItem)
            }
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
            item(key = "streaming") {
                CollectionRail("Streaming", StreamingTiles, openCollection)
            }
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
        Crossfade(
            targetState = hero,
            animationSpec = tween(260, easing = LinearOutSlowInEasing),
            label = "topHero"
        ) { item ->
            AsyncImage(
                model = item.backdrop,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x08000000), Color(0x2C000000), Bg), startY = 120f)))
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xF8050505), Color(0xA0050505), Color.Transparent), endX = 860f)))

        Column(
            Modifier.align(Alignment.CenterStart).padding(start = 44.dp, top = 56.dp).widthIn(max = 720.dp)
        ) {
            Text("TOP 10  ·  TRENDING THIS WEEK", color = Color(0xFFB8B8BC), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Text(hero.title, color = Color.White, fontSize = 43.sp, fontWeight = FontWeight.Black, maxLines = 2)
            Spacer(Modifier.height(10.dp))
            Text(
                "${hero.year}  ·  ★ ${"%.1f".format(hero.rating)}",
                color = Color(0xFFE0E0E0), fontSize = 14.sp, fontWeight = FontWeight.SemiBold
            )
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
                    Modifier
                        .width(if (i == safeIndex) 32.dp else 8.dp)
                        .height(8.dp)
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
    val h = if (landscape) 224.dp else 326.dp
    Column(Modifier.fillMaxWidth().height(h)) {
        Text(title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 34.dp, bottom = 8.dp))
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            items(rows, key = { it.cloudId }, contentType = { if (landscape) "landscape" else "poster" }) { item ->
                MediaCard(
                    item = item,
                    landscape = landscape,
                    progressPercent = progressFor(item),
                    onClick = { onClick(item) }
                )
            }
        }
    }
}

@Composable
private fun CollectionRail(title: String, rows: List<CollectionTile>, onClick: (CollectionTile) -> Unit) {
    Column(Modifier.fillMaxWidth().height(235.dp)) {
        Text(title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 34.dp, bottom = 8.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(rows, key = { it.id }) { tile -> CollectionCard(tile) { onClick(tile) } }
        }
    }
}

@Composable
fun CatalogScreen(state: AppState, title: String, catalog: List<MediaSummary>, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    BackHandler(onBack = onBack)
    AppShell(state, state.screen, { state.screen = it }) {
        Column(Modifier.fillMaxSize().padding(start = 30.dp, top = 30.dp, end = 30.dp)) {
            Text(title, color = Color.White, fontSize = 31.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(18.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp), contentPadding = PaddingValues(bottom = 48.dp)) {
                val chunks = catalog.chunked(6)
                items(chunks.size, key = { it }) { idx ->
                    Row {
                        chunks[idx].forEach { item ->
                            MediaCard(
                                item = item,
                                progressPercent = state.progress[item.cloudId]?.percent,
                                onClick = { scope.launch { state.open(item) } }
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
    val scope = rememberCoroutineScope()
    BackHandler(onBack = onBack)

    LaunchedEffect(query) {
        if (query.trim().length < 2) {
            results = emptyList()
        } else {
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
            LazyColumn(contentPadding = PaddingValues(bottom = 40.dp)) {
                val chunks = results.chunked(6)
                items(chunks.size, key = { it }) { idx ->
                    Row { chunks[idx].forEach { item -> MediaCard(item = item, onClick = { scope.launch { state.open(item) } }) } }
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
    AppShell(state, Screen.COLLECTIONS, { state.screen = it }) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 34.dp, bottom = 48.dp)) {
            item {
                Column(Modifier.padding(horizontal = 34.dp)) {
                    Text("Collections", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(5.dp))
                    Text("Streaming services and the collections you actually use.", color = Muted, fontSize = 14.sp)
                    Spacer(Modifier.height(24.dp))
                }
            }
            item { CollectionRail("Streaming", StreamingTiles) { tile -> scope.launch { state.openCollection(tile.id, tile.title) } } }
            item { CollectionRail("Discover", GenreTiles) { tile -> scope.launch { state.openCollection(tile.id, tile.title) } } }
        }
    }
}

@Composable
fun DetailsScreen(state: AppState, onBack: () -> Unit) {
    val item = state.selected ?: return
    val details = state.details
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val saved = state.progress[item.cloudId]
    BackHandler(onBack = onBack)

    Box(Modifier.fillMaxSize().background(Bg)) {
        AsyncImage(
            model = item.backdrop,
            contentDescription = null,
            modifier = Modifier.fillMaxWidth().height(590.dp).align(Alignment.TopCenter),
            contentScale = ContentScale.Crop
        )
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
                    Text(
                        "${item.year}   ★ ${"%.1f".format(item.rating)}   ${details?.runtimeText.orEmpty()}",
                        color = Color(0xFFD6D6D8), fontSize = 15.sp
                    )
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
                            FocusButton(label, primary = true, modifier = Modifier.widthIn(min = 330.dp)) {
                                scope.launch { runCatching { state.playMovie() }.onFailure { state.error = it.message } }
                            }
                        } else {
                            val canResume = (saved?.season ?: 0) > 0 && (saved?.episode ?: 0) > 0 && (saved?.percent ?: 0.0) < 96.0
                            FocusButton(
                                if (canResume) "▶  Continue S${saved?.season} E${saved?.episode}" else "▶  Play S1 E1",
                                primary = true,
                                modifier = Modifier.widthIn(min = 330.dp)
                            ) {
                                scope.launch {
                                    runCatching { if (canResume) state.resumeSeries() else state.playSeriesFromStart() }
                                        .onFailure { state.error = it.message }
                                }
                            }
                        }
                        FocusButton(if (state.favorites.contains(item.cloudId)) "✓ My List" else "+ My List") {
                            state.toggleFavorite(item)
                            scope.launch { runCatching { state.pushCloud() } }
                        }
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
                            FocusButton("‹") { if (state.currentSeason > 1) scope.launch { state.loadSeason(state.currentSeason - 1) } }
                            Spacer(Modifier.width(7.dp))
                            FocusButton("›") { if (state.currentSeason < (details?.seasonCount ?: 1)) scope.launch { state.loadSeason(state.currentSeason + 1) } }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    LazyRow(modifier = Modifier.fillMaxWidth().height(236.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(state.episodes, key = { "${it.season}:${it.episode}" }) { ep ->
                            EpisodeCard(ep) { scope.launch { runCatching { state.playEpisode(ep) }.onFailure { state.error = it.message } } }
                        }
                    }
                    Spacer(Modifier.height(28.dp))
                }
            }

            if (!details?.cast.isNullOrEmpty()) {
                item(key = "cast") {
                    Text("Cast", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(20.dp), modifier = Modifier.height(176.dp)) {
                        items(details?.cast.orEmpty(), key = { it.name }) { person -> CastCard(person) }
                    }
                    Spacer(Modifier.height(18.dp))
                }
            }

            if (!details?.trailers.isNullOrEmpty()) {
                item(key = "trailers") {
                    Text("Trailers", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.height(225.dp)) {
                        items(details?.trailers.orEmpty(), key = { it.key }) { trailer ->
                            TrailerCard(trailer) {
                                trailer.watchUrl?.let { url ->
                                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                }
            }

            item(key = "details") {
                Text(if (item.type == "movie") "Movie Details" else "Series Details", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(14.dp))
                DetailLine("Release Info", details?.releaseInfo?.takeIf { it.isNotBlank() } ?: item.year)
                DetailLine("Runtime", details?.runtimeText.orEmpty())
                DetailLine("Origin Country", details?.originCountry.orEmpty())
                DetailLine("Original Language", details?.originalLanguage.orEmpty())
            }
        }
        state.busyMessage?.let { LoadingOverlay(it) }
    }
}


@Composable
private fun DetailLine(label: String, value: String) {
    if (value.isBlank()) return
    Row(
        Modifier.widthIn(max = 900.dp).fillMaxWidth().border(0.5.dp, Color(0x332F2F2F)).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color(0xFFCACACD), fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(240.dp))
        Text(value, color = Color.White, fontSize = 15.sp)
    }
}

@Composable
private fun CastCard(person: CastMember) {
    Column(Modifier.width(138.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(106.dp).clip(RoundedCornerShape(53.dp)).background(Color(0xFF252525)), contentAlignment = Alignment.Center) {
            if (!person.profile.isNullOrBlank()) {
                AsyncImage(model = person.profile, contentDescription = person.name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Text(person.name.split(" ").take(2).mapNotNull { it.firstOrNull()?.toString() }.joinToString(""), color = Color(0xFFAAAAAF), fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
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
    Column(
        Modifier.width(330.dp).graphicsLayer { scaleX = scale; scaleY = scale }
            .onFocusChanged { focused = it.isFocused }.focusable().tvClick(onClick).clickable(onClick = onClick)
    ) {
        Box(
            Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF1A1A1A))
                .border(if (focused) 2.dp else 0.dp, Color.White, RoundedCornerShape(12.dp))
        ) {
            AsyncImage(model = trailer.thumbnail, contentDescription = trailer.name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(Modifier.fillMaxSize().background(Color(0x33000000)), contentAlignment = Alignment.Center) {
                Text("▶", color = Color.White, fontSize = 31.sp)
            }
        }
        Spacer(Modifier.height(7.dp))
        Text(trailer.name, color = Color.White, fontSize = 13.sp, maxLines = 1)
    }
}

@Composable
private fun EpisodeCard(ep: EpisodeSummary, onClick: () -> Unit) {
    var focused by remember(ep.season, ep.episode) { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.02f else 1f, tween(90, easing = LinearOutSlowInEasing), label = "episodeScale")
    Box(Modifier.width(328.dp).height(225.dp), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.width(316.dp).graphicsLayer { scaleX = scale; scaleY = scale }
                .onFocusChanged { focused = it.isFocused }.focusable().tvClick(onClick).clickable(onClick = onClick)
        ) {
            Box(
                Modifier.fillMaxWidth().height(176.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF1B1B1B))
                    .border(if (focused) 2.dp else 0.dp, Color.White, RoundedCornerShape(12.dp))
            ) {
                AsyncImage(model = ep.still, contentDescription = ep.title, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                if (focused) Box(Modifier.fillMaxSize().background(Color(0x26000000)), contentAlignment = Alignment.Center) {
                    Text("▶", color = Color.White, fontSize = 32.sp)
                }
            }
            Spacer(Modifier.height(7.dp))
            Text("S${ep.season} E${ep.episode} · ${ep.title}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            if (ep.airDate.isNotBlank()) Text(ep.airDate, color = Muted, fontSize = 10.sp, maxLines = 1)
        }
    }
}

@Composable
fun SettingsScreen(state: AppState, onBack: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    BackHandler(onBack = onBack)

    AppShell(state, Screen.SETTINGS, { state.screen = it }) {
        LazyColumn(Modifier.fillMaxSize().padding(40.dp), contentPadding = PaddingValues(bottom = 50.dp)) {
            item {
                Text("Settings", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(8.dp))
                Text("MiFlix Native ${BuildConfig.VERSION_NAME} · Compose TV + Media3", color = Muted, fontSize = 14.sp)
                Spacer(Modifier.height(30.dp))

                if (state.session == null) {
                    Text("Account & Sync", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("Sign in once. Profiles, progress, TMDB and your private Torrentio/TorBox setup restore automatically.", color = Muted, fontSize = 14.sp, modifier = Modifier.width(760.dp))
                    Spacer(Modifier.height(16.dp))
                    NativeTextField(email, { email = it }, "Email", modifier = Modifier.width(520.dp))
                    Spacer(Modifier.height(10.dp))
                    NativeTextField(password, { password = it }, "Password", true, Modifier.width(520.dp))
                    Spacer(Modifier.height(14.dp))
                    FocusButton("Sign In", primary = true) { scope.launch { state.login(email.trim(), password) } }
                } else {
                    Text("Account & Sync", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("Signed in as ${state.session?.email}", color = Color(0xFFE4E4E6), fontSize = 16.sp)
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FocusButton("Sync Now", primary = true) {
                            scope.launch {
                                state.syncFromCloud(); state.pushCloud(); if (state.tmdb.token.isNotBlank()) state.loadHome()
                            }
                        }
                        FocusButton("Sign Out") { state.signOut() }
                    }
                    Spacer(Modifier.height(20.dp))
                    Text("TMDB: ${if (state.tmdb.token.isNotBlank()) "Ready" else "Missing"}  ·  Torrentio/TorBox: ${if (state.streamRepo.manifestUrl.isNotBlank()) "Ready" else "Missing"}", color = Muted, fontSize = 13.sp)
                }

                Spacer(Modifier.height(34.dp))
                Text("Updates", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("The latest channel always uses the same APK URL, so one Downloader code can stay permanent.", color = Muted, fontSize = 14.sp, modifier = Modifier.width(760.dp))
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FocusButton(if (state.updateChecking) "Checking…" else "Check for Updates", primary = true) {
                        if (!state.updateChecking) scope.launch { state.checkForUpdates() }
                    }
                    state.updateInfo?.takeIf { it.isNewer }?.let { info ->
                        FocusButton("Open ${info.version}") {
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(info.downloadUrl))) }
                        }
                    }
                }
                state.updateInfo?.let { info ->
                    Spacer(Modifier.height(9.dp))
                    Text(
                        if (info.isNewer) "Update available: ${info.version}" else "You're on the latest build.",
                        color = Color(0xFFE7E7E9), fontSize = 13.sp
                    )
                }

                state.error?.let { Spacer(Modifier.height(20.dp)); Text(it, color = Color(0xFFFF8585), fontSize = 14.sp) }
                state.busyMessage?.let { Spacer(Modifier.height(16.dp)); Text(it, color = Color.White, fontSize = 14.sp) }
            }
        }
    }
}

@Composable
fun ProfilesScreen(state: AppState, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    BackHandler(onBack = onBack)
    Column(Modifier.fillMaxSize().background(Bg).padding(64.dp)) {
        Text("Who's watching?", color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(34.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            state.profiles.forEach { p ->
                var focused by remember(p.id) { mutableStateOf(false) }
                val scale by animateFloatAsState(if (focused) 1.025f else 1f, tween(90, easing = LinearOutSlowInEasing), label = "profileScale")
                Column(
                    Modifier.width(150.dp).graphicsLayer { scaleX = scale; scaleY = scale }
                        .onFocusChanged { focused = it.isFocused }.focusable()
                        .tvClick { scope.launch { state.selectProfile(p) } }
                        .clickable { scope.launch { state.selectProfile(p) } },
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
                        MediaItem.SubtitleConfiguration.Builder(Uri.parse(s.url)).apply {
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
        Text(text, color = Color.Black, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.background(Color.White, RoundedCornerShape(18.dp)).padding(horizontal = 28.dp, vertical = 17.dp))
    }
}

private fun formatPosition(ms: Long): String {
    val totalMinutes = (ms / 60_000L).coerceAtLeast(0)
    return if (totalMinutes >= 60) "${totalMinutes / 60}h ${totalMinutes % 60}m" else "${totalMinutes}m"
}
