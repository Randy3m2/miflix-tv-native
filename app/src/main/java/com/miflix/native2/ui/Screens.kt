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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
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
import kotlin.math.abs

private val StreamingTiles: List<CollectionTile> = Catalogs.providers.map {
    CollectionTile(it.id, it.title, it.coverUrl, it.focusGifUrl)
}
private val GenreTiles: List<CollectionTile> = Catalogs.genres.map { CollectionTile("genre:${it.id}", it.title) }
private val YearTiles: List<CollectionTile> = Catalogs.years.map { CollectionTile("year:$it", it.toString()) }

@Composable
private fun AppShell(state: AppState, screen: Screen, onNavigate: (Screen) -> Unit, content: @Composable BoxScope.() -> Unit) {
    Row(Modifier.fillMaxSize().background(Bg)) {
        Sidebar(screen, state.activeProfile.name, onNavigate)
        Box(Modifier.weight(1f).fillMaxHeight(), content = content)
    }
}

@Composable
fun HomeScreen(state: AppState, onNavigate: (Screen) -> Unit) {
    LaunchedEffect(Unit) { delay(450); state.loadHomeExtras() }
    val openItem: (MediaSummary) -> Unit = { item -> state.launchTask { state.open(item) } }

    AppShell(state, Screen.HOME, onNavigate) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 54.dp)) {
            item(key = "hero") { TopTenHero(state.trending.take(10), openItem) }
            if (state.continueWatching.isNotEmpty()) {
                item(key = "continue") {
                    MediaRail("Continue Watching", state.continueWatching, landscape = true,
                        progressFor = { state.progress[it.cloudId]?.percent }, onClick = openItem)
                }
            }
            item(key = "streaming") {
                CollectionRail("Streaming", StreamingTiles) { tile ->
                    Catalogs.providers.firstOrNull { it.id == tile.id }?.let { provider -> state.launchTask { state.openProvider(provider) } }
                }
            }
            item(key = "genres") {
                CollectionRail("Genres", GenreTiles) { tile ->
                    val id = tile.id.substringAfter("genre:")
                    Catalogs.genres.firstOrNull { it.id == id }?.let { genre -> state.launchTask { state.openGenre(genre) } }
                }
            }
            item(key = "movies") { MediaRail("Popular · Movies", state.movies, onClick = openItem) }
            item(key = "series") { MediaRail("Popular · Series", state.series, onClick = openItem) }
            item(key = "top") { MediaRail("Top Rated", state.topRated, onClick = openItem) }
            item(key = "years") {
                CollectionRail("Movies by Year", YearTiles) { tile -> tile.id.substringAfter("year:").toIntOrNull()?.let { y -> state.launchTask { state.openYear(y) } } }
            }
        }
    }
}

@Composable
private fun TopTenHero(rows: List<MediaSummary>, onOpen: (MediaSummary) -> Unit) {
    if (rows.isEmpty()) { Box(Modifier.fillMaxWidth().height(450.dp).background(Bg)); return }
    var index by remember(rows.map { it.cloudId }) { mutableIntStateOf(0) }
    val safeIndex = index.coerceIn(0, rows.lastIndex)
    val hero = rows[safeIndex]
    LaunchedEffect(rows.map { it.cloudId }) { while (true) { delay(6500); index = (index + 1) % rows.size } }

    Box(Modifier.fillMaxWidth().height(500.dp)) {
        Crossfade(targetState = hero, animationSpec = tween(240, easing = LinearOutSlowInEasing), label = "topHero") { item ->
            AsyncImage(model = item.backdrop, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x08000000), Color(0x2C000000), Bg), startY = 120f)))
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xF8050505), Color(0xA0050505), Color.Transparent), endX = 860f)))
        Column(Modifier.align(Alignment.CenterStart).padding(start = 38.dp, top = 54.dp).widthIn(max = 720.dp)) {
            Text("TOP 10  ·  TRENDING THIS WEEK", color = Color(0xFFB8B8BC), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp)); Text(hero.title, color = Color.White, fontSize = 43.sp, fontWeight = FontWeight.Black, maxLines = 2)
            Spacer(Modifier.height(9.dp)); Text("${hero.year}  ·  ★ ${"%.1f".format(hero.rating)}", color = Color(0xFFE0E0E0), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp)); Text(hero.overview, color = Color(0xFFE7E7EA), fontSize = 15.sp, maxLines = 3, modifier = Modifier.widthIn(max = 690.dp))
            Spacer(Modifier.height(20.dp)); FocusButton("View Details", primary = true) { onOpen(hero) }
        }
        Row(Modifier.align(Alignment.BottomEnd).padding(end = 30.dp, bottom = 22.dp), horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
            rows.forEachIndexed { i, _ -> Box(Modifier.width(if (i == safeIndex) 30.dp else 8.dp).height(8.dp).background(if (i == safeIndex) Color.White else Color(0xFFBDBDBD), RoundedCornerShape(8.dp))) }
        }
    }
}

@Composable
private fun MediaRail(
    title: String,
    rows: List<MediaSummary>,
    landscape: Boolean = false,
    ranked: Boolean = false,
    progressFor: (MediaSummary) -> Double? = { null },
    onClick: (MediaSummary) -> Unit
) {
    val h = if (landscape) 224.dp else 326.dp
    Column(Modifier.fillMaxWidth().height(h)) {
        Text(title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 28.dp, bottom = 8.dp))
        LazyRow(Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(0.dp)) {
            items(rows, key = { it.cloudId }) { item ->
                MediaCard(item, landscape = landscape, progressPercent = progressFor(item), rank = if (ranked) rows.indexOf(item) + 1 else null, onClick = { onClick(item) })
            }
        }
    }
}

@Composable
private fun CollectionRail(title: String, rows: List<CollectionTile>, onClick: (CollectionTile) -> Unit) {
    Column(Modifier.fillMaxWidth().height(235.dp)) {
        Text(title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 28.dp, bottom = 8.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(rows, key = { it.id }) { tile -> CollectionCard(tile) { onClick(tile) } }
        }
    }
}

@Composable
fun ProviderScreen(state: AppState, onBack: () -> Unit) {
    val provider = state.selectedProvider ?: return
    BackHandler(onBack = onBack)
    val openItem: (MediaSummary) -> Unit = { state.launchTask { state.open(it) } }
    AppShell(state, Screen.PROVIDER, { state.screen = it }) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 28.dp, bottom = 60.dp)) {
            item {
                Row(Modifier.padding(horizontal = 28.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    FocusButton("←", modifier = Modifier.width(64.dp), onClick = onBack)
                    Spacer(Modifier.width(16.dp)); Text(provider.title, color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.height(18.dp))
            }
            if (state.providerLoading && state.providerTop.isEmpty()) item { Text("Loading ${provider.title}…", color = Muted, modifier = Modifier.padding(28.dp)) }
            if (state.providerTop.isNotEmpty()) item { MediaRail("Top 10 on ${provider.title}", state.providerTop, ranked = true, onClick = openItem) }
            if (state.providerMovies.isNotEmpty()) item { MediaRail("Movies", state.providerMovies, onClick = openItem) }
            if (state.providerSeries.isNotEmpty()) item { MediaRail("Series", state.providerSeries, onClick = openItem) }
            if (state.providerLatest.isNotEmpty()) item { MediaRail("Latest", state.providerLatest, onClick = openItem) }
            if (state.providerTopRated.isNotEmpty()) item { MediaRail("Best Rated", state.providerTopRated, onClick = openItem) }
            item {
                CollectionRail("Genres on ${provider.title}", GenreTiles) { tile ->
                    Catalogs.genres.firstOrNull { it.id == tile.id.substringAfter("genre:") }?.let { genre ->
                        state.launchTask { state.openProviderGenre(genre) }
                    }
                }
            }
        }
    }
}

@Composable
fun ProviderGenreScreen(state: AppState, onBack: () -> Unit) {
    val provider = state.selectedProvider ?: return
    val genre = state.selectedProviderGenre ?: return
    BackHandler(onBack = onBack)
    val openItem: (MediaSummary) -> Unit = { state.launchTask { state.open(it) } }
    AppShell(state, Screen.PROVIDER_GENRE, { state.screen = it }) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 28.dp, bottom = 60.dp)) {
            item {
                Row(Modifier.padding(horizontal = 28.dp), verticalAlignment = Alignment.CenterVertically) {
                    FocusButton("←", modifier = Modifier.width(64.dp), onClick = onBack)
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("${genre.title} · ${provider.title}", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black)
                        Text("20+ titles per section, filtered to this platform", color = Muted, fontSize = 13.sp)
                    }
                }
                Spacer(Modifier.height(18.dp))
            }
            if (state.providerLoading && state.providerGenrePopular.isEmpty()) item { Text("Loading ${genre.title}…", color = Muted, modifier = Modifier.padding(28.dp)) }
            if (state.providerGenrePopular.isNotEmpty()) item { MediaRail("Popular ${genre.title}", state.providerGenrePopular, onClick = openItem) }
            if (state.providerGenreLatest.isNotEmpty()) item { MediaRail("Latest ${genre.title}", state.providerGenreLatest, onClick = openItem) }
            if (state.providerGenreTopRated.isNotEmpty()) item { MediaRail("Best Rated ${genre.title}", state.providerGenreTopRated, onClick = openItem) }
        }
    }
}

@Composable
fun CatalogScreen(state: AppState, title: String, catalog: List<MediaSummary>, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    AppShell(state, state.screen, { state.screen = it }) {
        Column(Modifier.fillMaxSize().padding(start = 24.dp, top = 24.dp, end = 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FocusButton("←", modifier = Modifier.width(64.dp), onClick = onBack)
                Spacer(Modifier.width(14.dp)); Text(title, color = Color.White, fontSize = 31.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.width(16.dp)); Text("${catalog.size} titles", color = Muted, fontSize = 13.sp)
            }
            Spacer(Modifier.height(12.dp))
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 184.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 48.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                gridItems(catalog, key = { it.cloudId }) { item ->
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        MediaCard(item = item, progressPercent = state.progress[item.cloudId]?.percent, onClick = { state.launchTask { state.open(item) } })
                    }
                }
            }
        }
        state.busyMessage?.let { LoadingOverlay(it) }
    }
}

@Composable
fun SearchScreen(state: AppState, onBack: () -> Unit) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<MediaSummary>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    BackHandler(onBack = onBack)
    LaunchedEffect(query) {
        if (query.trim().length < 2) results = emptyList() else {
            delay(350); searching = true; results = runCatching { state.tmdb.search(query.trim()) }.getOrElse { emptyList() }; searching = false
        }
    }
    AppShell(state, Screen.SEARCH, { state.screen = it }) {
        Column(Modifier.fillMaxSize().padding(28.dp)) {
            Text("Search", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(14.dp)); NativeTextField(query, { query = it }, "Movies, series…", modifier = Modifier.width(620.dp))
            Spacer(Modifier.height(14.dp)); if (searching) Text("Searching…", color = Muted, fontSize = 13.sp)
            LazyVerticalGrid(GridCells.Adaptive(184.dp), Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 40.dp)) {
                gridItems(results, key = { it.cloudId }) { item -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { MediaCard(item, onClick = { state.launchTask { state.open(item) } }) } }
            }
        }
    }
}

@Composable
fun CollectionsScreen(state: AppState, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    AppShell(state, Screen.COLLECTIONS, { state.screen = it }) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 28.dp, bottom = 48.dp)) {
            item {
                Column(Modifier.padding(horizontal = 28.dp)) {
                    Text("Browse", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(5.dp)); Text("Streaming platforms, genres and movies by year.", color = Muted, fontSize = 14.sp); Spacer(Modifier.height(22.dp))
                }
            }
            item { CollectionRail("Streaming", StreamingTiles) { tile -> Catalogs.providers.firstOrNull { it.id == tile.id }?.let { state.launchTask { state.openProvider(it) } } } }
            item { CollectionRail("Genres", GenreTiles) { tile -> Catalogs.genres.firstOrNull { it.id == tile.id.substringAfter("genre:") }?.let { state.launchTask { state.openGenre(it) } } } }
            item { CollectionRail("Movies by Year", YearTiles) { tile -> tile.id.substringAfter("year:").toIntOrNull()?.let { y -> state.launchTask { state.openYear(y) } } } }
        }
    }
}

@Composable
fun DetailsScreen(state: AppState, onBack: () -> Unit) {
    val item = state.selected ?: return
    val details = state.details
    val context = LocalContext.current
    val saved = state.progress[item.cloudId]
    BackHandler(onBack = onBack)

    Box(Modifier.fillMaxSize().background(Bg)) {
        AsyncImage(model = item.backdrop, contentDescription = null, modifier = Modifier.fillMaxWidth().height(590.dp).align(Alignment.TopCenter), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x22000000), Color(0x66000000), Bg), startY = 150f)))
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xF7050505), Color(0xA8050505), Color.Transparent), endX = 980f)))

        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 34.dp, end = 50.dp, top = 26.dp, bottom = 70.dp)) {
            item(key = "hero") {
                Column(Modifier.fillMaxWidth().heightIn(min = 500.dp)) {
                    FocusButton("←", modifier = Modifier.width(64.dp), onClick = onBack)
                    Spacer(Modifier.height(54.dp)); Text(item.title, color = Color.White, fontSize = 46.sp, fontWeight = FontWeight.Black, maxLines = 2)
                    Spacer(Modifier.height(8.dp)); Text("${item.year}   ★ ${"%.1f".format(item.rating)}   ${details?.runtimeText.orEmpty()}", color = Color(0xFFD6D6D8), fontSize = 15.sp)
                    if (!details?.genres.isNullOrEmpty()) { Spacer(Modifier.height(7.dp)); Text(details?.genres?.take(4)?.joinToString("  ·  ").orEmpty(), color = Color(0xFFB9B9BD), fontSize = 13.sp) }
                    Spacer(Modifier.height(14.dp)); Text(item.overview, color = Color(0xFFE8E8EB), fontSize = 16.sp, maxLines = 4, modifier = Modifier.widthIn(max = 860.dp))
                    Spacer(Modifier.height(24.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (item.type == "movie") {
                            val label = if ((saved?.percent ?: 0.0) in 1.0..95.0) "▶  Resume ${formatPosition(saved?.position ?: 0)}" else "▶  Play"
                            FocusButton(label, primary = true, modifier = Modifier.widthIn(min = 300.dp)) { state.launchTask { state.playMovie() } }
                        } else {
                            val canResume = (saved?.season ?: 0) > 0 && (saved?.episode ?: 0) > 0 && (saved?.percent ?: 0.0) < 96.0
                            FocusButton(if (canResume) "▶  Continue S${saved?.season} E${saved?.episode}" else "▶  Play S1 E1", primary = true, modifier = Modifier.widthIn(min = 300.dp)) {
                                state.launchTask { if (canResume) state.resumeSeries() else state.playSeriesFromStart() }
                            }
                        }
                        FocusButton(if (state.favorites.contains(item.cloudId)) "✓ My List" else "+ My List") { state.toggleFavorite(item); state.pushCloudAsync() }
                        FocusButton("◉ Watch Party") { state.launchTask { state.createWatchParty() } }
                    }
                    Spacer(Modifier.height(36.dp))
                }
            }

            if (item.type == "series") {
                item(key = "episodes") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Season ${state.currentSeason}", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(14.dp)); FocusButton("‹") { if (state.currentSeason > 1) state.launchTask { state.loadSeason(state.currentSeason - 1) } }
                        Spacer(Modifier.width(7.dp)); FocusButton("›") { if (state.currentSeason < (details?.seasonCount ?: 1)) state.launchTask { state.loadSeason(state.currentSeason + 1) } }
                    }
                    Spacer(Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(end = 30.dp)) {
                        items(state.episodes, key = { "${it.season}:${it.episode}" }) { ep -> EpisodeCard(ep) { state.launchTask { state.playEpisode(ep) } } }
                    }
                    Spacer(Modifier.height(34.dp))
                }
            }

            if (!details?.cast.isNullOrEmpty()) {
                item(key = "cast") {
                    Text("Cast", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Black); Spacer(Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(18.dp)) { items(details!!.cast, key = { it.name + it.character }) { CastCard(it) } }
                    Spacer(Modifier.height(32.dp))
                }
            }
            if (!details?.trailers.isNullOrEmpty()) {
                item(key = "trailers") {
                    Text("Trailers", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Black); Spacer(Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(details!!.trailers, key = { it.key }) { trailer ->
                            TrailerCard(trailer) { trailer.watchUrl?.let { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it))) } } }
                        }
                    }
                    Spacer(Modifier.height(32.dp))
                }
            }
            item(key = "details") {
                Text(if (item.type == "movie") "Movie Details" else "Series Details", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(14.dp)); DetailLine("Release Info", details?.releaseInfo.orEmpty())
                DetailLine("Runtime", details?.runtimeText.orEmpty()); DetailLine("Origin Country", details?.originCountry.orEmpty()); DetailLine("Original Language", details?.originalLanguage.orEmpty())
            }
        }
        state.busyMessage?.let { LoadingOverlay(it) }
    }
}

@Composable private fun DetailLine(label: String, value: String) {
    Row(Modifier.width(760.dp).padding(vertical = 9.dp)) { Text(label, color = Color(0xFFCBCBCD), fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(210.dp)); Text(value.ifBlank { "—" }, color = Color.White, fontSize = 14.sp) }
    Box(Modifier.width(760.dp).height(1.dp).background(Color(0xFF292929)))
}

@Composable private fun CastCard(person: CastMember) {
    Column(Modifier.width(112.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(86.dp).clip(RoundedCornerShape(43.dp)).background(Color(0xFF222222)), contentAlignment = Alignment.Center) {
            if (!person.profile.isNullOrBlank()) AsyncImage(model = person.profile, contentDescription = person.name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else Text(person.name.split(" ").take(2).mapNotNull { it.firstOrNull()?.toString() }.joinToString(""), color = Color(0xFFAAAAAF), fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp)); Text(person.name, color = Color.White, fontSize = 13.sp, maxLines = 1)
        if (person.character.isNotBlank()) Text(person.character, color = Muted, fontSize = 10.sp, maxLines = 1)
    }
}

@Composable private fun TrailerCard(trailer: TrailerSummary, onClick: () -> Unit) {
    var focused by remember(trailer.key) { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.02f else 1f, tween(90, easing = LinearOutSlowInEasing), label = "trailerScale")
    Column(Modifier.width(330.dp).graphicsLayer { scaleX = scale; scaleY = scale }.onFocusChanged { focused = it.isFocused }.focusable().tvClick(onClick).clickable(onClick = onClick)) {
        Box(Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF1A1A1A)).border(if (focused) 2.dp else 0.dp, Color.White, RoundedCornerShape(12.dp))) {
            AsyncImage(model = trailer.thumbnail, contentDescription = trailer.name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(Modifier.align(Alignment.Center).size(48.dp).background(Color(0xDFFFFFFF), RoundedCornerShape(24.dp)), contentAlignment = Alignment.Center) { Text("▶", color = Color.Black, fontSize = 20.sp) }
        }
        Spacer(Modifier.height(8.dp)); Text(trailer.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable private fun EpisodeCard(ep: EpisodeSummary, onClick: () -> Unit) {
    var focused by remember(ep.season, ep.episode) { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.02f else 1f, tween(90, easing = LinearOutSlowInEasing), label = "epScale")
    Box(Modifier.width(328.dp).height(225.dp), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.width(312.dp).graphicsLayer { scaleX = scale; scaleY = scale }.onFocusChanged { focused = it.isFocused }.focusable().tvClick(onClick).clickable(onClick = onClick)) {
            Box(Modifier.fillMaxWidth().height(170.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF1B1B1B)).border(if (focused) 2.dp else 0.dp, Color.White, RoundedCornerShape(12.dp))) {
                AsyncImage(model = ep.still, contentDescription = ep.title, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Text("S${ep.season} E${ep.episode}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.TopStart).padding(8.dp).background(Color(0xCC000000), RoundedCornerShape(6.dp)).padding(horizontal = 7.dp, vertical = 4.dp))
            }
            Spacer(Modifier.height(7.dp)); Text("S${ep.season} E${ep.episode} · ${ep.title}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(ep.airDate, color = Muted, fontSize = 10.sp, maxLines = 1)
        }
    }
}

@Composable
fun SettingsScreen(state: AppState, onBack: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val context = LocalContext.current
    BackHandler(onBack = onBack)
    AppShell(state, Screen.SETTINGS, { state.screen = it }) {
        LazyColumn(Modifier.fillMaxSize().padding(30.dp), contentPadding = PaddingValues(bottom = 50.dp)) {
            item {
                Text("Settings", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(8.dp)); Text("MiFlix Native ${BuildConfig.VERSION_NAME} · Compose TV + Media3", color = Muted, fontSize = 14.sp)
                Spacer(Modifier.height(28.dp))

                Text("Account & Sync", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(8.dp))
                if (state.session == null) {
                    Text("Use your remote, or scan a QR code and type on your phone.", color = Muted, fontSize = 14.sp)
                    Spacer(Modifier.height(14.dp)); NativeTextField(email, { email = it }, "Email", modifier = Modifier.width(520.dp)); Spacer(Modifier.height(9.dp))
                    NativeTextField(password, { password = it }, "Password", true, Modifier.width(520.dp)); Spacer(Modifier.height(13.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FocusButton("Sign In", primary = true) { state.launchTask { state.login(email.trim(), password) } }
                        FocusButton("QR / Phone Pairing") { state.startPairing(); state.screen = Screen.PAIRING }
                    }
                } else {
                    Text("Signed in as ${state.session?.email}", color = Color(0xFFE4E4E6), fontSize = 16.sp); Spacer(Modifier.height(13.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FocusButton("Sync Now", primary = true) { state.launchTask { state.syncFromCloud(); state.pushCloud(); if (state.tmdb.token.isNotBlank()) state.loadHome() } }
                        FocusButton("Pair Phone / Add Add-on") { state.startPairing(); state.screen = Screen.PAIRING }
                        FocusButton("Profiles") { state.screen = Screen.PROFILES }
                        FocusButton("Sign Out") { state.signOut() }
                    }
                    Spacer(Modifier.height(18.dp)); Text("Streaming add-ons", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text(if (state.addons.isEmpty()) "None configured" else state.addons.joinToString("  ·  ") { "${it.name} ✓" }, color = Muted, fontSize = 13.sp)
                }

                Spacer(Modifier.height(30.dp)); Text("Watch Party", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(10.dp))
                FocusButton("Create / Join Watch Party") { state.screen = Screen.WATCH_PARTY }

                Spacer(Modifier.height(30.dp)); Text("Updates", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(8.dp))
                Text("The latest channel keeps the same APK URL, so one Downloader code can stay permanent.", color = Muted, fontSize = 14.sp, modifier = Modifier.width(760.dp)); Spacer(Modifier.height(13.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FocusButton(if (state.updateChecking) "Checking…" else "Check for Updates", primary = true) { if (!state.updateChecking) state.launchTask { state.checkForUpdates() } }
                    state.updateInfo?.takeIf { it.isNewer }?.let { info -> FocusButton("Open ${info.version}") { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(info.downloadUrl))) } } }
                }
                state.updateInfo?.let { info -> Spacer(Modifier.height(9.dp)); Text(if (info.isNewer) "Update available: ${info.version}" else "You're on the latest build.", color = Color(0xFFE7E7E9), fontSize = 13.sp) }
                state.busyMessage?.let { Spacer(Modifier.height(14.dp)); Text(it, color = Color.White, fontSize = 13.sp) }
            }
        }
    }
}

@Composable
fun ProfilesScreen(state: AppState, onBack: () -> Unit) {
    var newName by remember { mutableStateOf("") }
    BackHandler(onBack = onBack)
    Column(Modifier.fillMaxSize().background(Bg).padding(54.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { FocusButton("←", modifier = Modifier.width(64.dp), onClick = onBack); Spacer(Modifier.width(15.dp)); Text("Who's watching?", color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.Black) }
        Spacer(Modifier.height(30.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
            state.profiles.forEach { p ->
                var focused by remember(p.id) { mutableStateOf(false) }
                val scale by animateFloatAsState(if (focused) 1.025f else 1f, tween(90, easing = LinearOutSlowInEasing), label = "profileScale")
                Column(Modifier.width(140.dp).graphicsLayer { scaleX = scale; scaleY = scale }.onFocusChanged { focused = it.isFocused }.focusable()
                    .tvClick { state.launchTask { state.selectProfile(p) } }.clickable { state.launchTask { state.selectProfile(p) } }, horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(104.dp).clip(RoundedCornerShape(52.dp)).background(if (focused) Color.White else Color(0xFF242424)).border(if (focused) 2.dp else 0.dp, Color.White, RoundedCornerShape(52.dp)), contentAlignment = Alignment.Center) {
                        Text(p.name.take(1).uppercase(), color = if (focused) Color.Black else Color.White, fontSize = 36.sp, fontWeight = FontWeight.Black)
                    }
                    Spacer(Modifier.height(8.dp)); Text(p.name, color = Color.White, fontSize = 13.sp)
                }
            }
        }
        if (state.session != null && state.profiles.size < 6) {
            Spacer(Modifier.height(38.dp)); Text("Add profile", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp)); Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                NativeTextField(newName, { newName = it }, "Profile name", modifier = Modifier.width(360.dp))
                FocusButton("Create", primary = true) { val name = newName; if (name.isNotBlank()) { state.launchTask { state.createProfile(name) }; newName = "" } }
            }
            Spacer(Modifier.height(8.dp)); Text("New profiles share TMDB, add-ons and account setup with Profile 1; My List and watch progress stay separate.", color = Muted, fontSize = 12.sp)
        }
    }
}

@Composable
fun PairingScreen(state: AppState, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    DisposableEffect(Unit) { if (state.pairingInfo == null) state.startPairing(); onDispose { state.stopPairing() } }
    val info = state.pairingInfo
    Row(Modifier.fillMaxSize().background(Bg).padding(44.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(42.dp)) {
        Column(Modifier.width(420.dp)) {
            FocusButton("← Back", onClick = onBack); Spacer(Modifier.height(28.dp))
            Text("Pair your phone", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Black); Spacer(Modifier.height(10.dp))
            Text("Scan the QR code while your phone and TV are on the same Wi‑Fi. From your phone you can sign in or paste a configured Torrentio, Comet or other Stremio manifest URL.", color = Color(0xFFD9D9DC), fontSize = 15.sp)
            Spacer(Modifier.height(18.dp)); Text("One-time pairing code", color = Muted, fontSize = 12.sp)
            Text(info?.code.orEmpty(), color = Color.White, fontSize = 31.sp, fontWeight = FontWeight.Black, letterSpacing = 5.sp)
            if (state.pairingStatus.isNotBlank()) { Spacer(Modifier.height(16.dp)); Text(state.pairingStatus, color = Color.White, fontSize = 14.sp) }
            Spacer(Modifier.height(18.dp)); Text("Configured add-ons: ${if (state.addons.isEmpty()) "None" else state.addons.joinToString { it.name }}", color = Muted, fontSize = 12.sp)
        }
        if (info != null) QrCode(info.url, 250.dp)
    }
}

@Composable
fun WatchPartyScreen(state: AppState, onBack: () -> Unit) {
    var joinCode by remember { mutableStateOf("") }
    BackHandler(onBack = onBack)
    val party = state.watchParty
    val room = state.watchPartyRoom
    Row(Modifier.fillMaxSize().background(Bg).padding(44.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(44.dp)) {
        Column(Modifier.width(590.dp)) {
            FocusButton("← Back", onClick = onBack); Spacer(Modifier.height(24.dp))
            Text("Watch Party", color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.Black); Spacer(Modifier.height(8.dp))
            Text("Every device resolves its own Torrentio/Comet stream. MiFlix only syncs the title, episode, play/pause and position.", color = Muted, fontSize = 14.sp)
            Spacer(Modifier.height(24.dp))
            if (party == null) {
                if (state.selected != null) {
                    FocusButton("Create room for ${state.selected?.title}", primary = true) { state.launchTask { state.createWatchParty() } }
                    Spacer(Modifier.height(22.dp))
                }
                Text("Join a room", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    NativeTextField(joinCode, { joinCode = it.uppercase().take(6) }, "6-character code", modifier = Modifier.width(280.dp))
                    FocusButton("Join", primary = true) { state.launchTask { state.joinWatchParty(joinCode) } }
                }
                Spacer(Modifier.height(8.dp)); Text("You can also scan a MiFlix Watch Party QR code on a device that supports the MiFlix deep link.", color = Muted, fontSize = 12.sp)
            } else if (room != null) {
                Text(if (party.isHost) "Room ready" else "Joined room", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(7.dp)); Text(room.roomCode, color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Black, letterSpacing = 7.sp)
                Spacer(Modifier.height(9.dp)); Text(state.selected?.title ?: room.cloudId, color = Color(0xFFE5E5E7), fontSize = 16.sp)
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (party.isHost) FocusButton("▶ Start Party", primary = true) { state.launchTask { state.startHostPartyPlayback() } }
                    FocusButton("Leave Party") { state.leaveWatchParty(); state.screen = Screen.HOME }
                }
            }
            if (state.partyBusy) { Spacer(Modifier.height(14.dp)); Text("Connecting…", color = Color.White, fontSize = 13.sp) }
        }
        if (room != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                QrCode("miflix://watch-party?code=${room.roomCode}", 250.dp)
                Spacer(Modifier.height(12.dp)); Text("Scan to join", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun PlayerScreen(state: AppState, request: PlayerRequest, onClose: () -> Unit) {
    val context = LocalContext.current
    val player = remember(request.stream.url) {
        val renderers = DefaultRenderersFactory(context).setEnableDecoderFallback(true)
        ExoPlayer.Builder(context, renderers).build().apply {
            setSeekBackIncrementMs(10_000); setSeekForwardIncrementMs(10_000)
            val builder = MediaItem.Builder().setUri(request.stream.url)
            if (request.stream.subtitles.isNotEmpty()) {
                builder.setSubtitleConfigurations(request.stream.subtitles.take(20).map { s ->
                    MediaItem.SubtitleConfiguration.Builder(Uri.parse(s.url)).apply {
                        if (s.lang.isNotBlank()) setLanguage(s.lang); setLabel(s.label.ifBlank { s.lang.ifBlank { "Subtitle" } })
                        setMimeType(if (s.url.lowercase().contains(".srt")) MimeTypes.APPLICATION_SUBRIP else MimeTypes.TEXT_VTT)
                    }.build()
                })
            }
            setMediaItem(builder.build(), request.resumeMs)
            trackSelectionParameters = TrackSelectionParameters.Builder(context).setPreferredAudioLanguage("es").setPreferredTextLanguage("es").build()
            videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT; prepare(); playWhenReady = true
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    val duration = player.duration.coerceAtLeast(0); if (duration > 0) state.updateProgress(request, duration, duration, ended = true); state.pushCloudAsync()
                }
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener); state.updateProgress(request, player.currentPosition, player.duration.coerceAtLeast(0)); state.pushCloudAsync(); player.release()
        }
    }

    LaunchedEffect(player) {
        while (true) { delay(5000); val d = player.duration; if (d > 0) state.updateProgress(request, player.currentPosition, d) }
    }

    LaunchedEffect(player, state.watchParty?.roomCode, request.item.cloudId, request.season, request.episode) {
        while (state.watchParty != null) {
            delay(1000)
            if (state.watchParty?.isHost == true) {
                runCatching { state.hostPartyTick(request, player.currentPosition, player.isPlaying) }
            } else {
                val remote = runCatching { state.guestPartySnapshot() }.getOrNull() ?: continue
                val sameMedia = remote.cloudId == request.item.cloudId && remote.season == request.season && remote.episode == request.episode
                if (!sameMedia) { runCatching { state.switchGuestToRemote(remote) }; break }
                if (abs(remote.positionMs - player.currentPosition) > 1800) player.seekTo(remote.positionMs)
                if (remote.isPlaying && !player.isPlaying) player.play()
                if (!remote.isPlaying && player.isPlaying) player.pause()
            }
        }
    }

    BackHandler { onClose() }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(factory = { ctx ->
            PlayerView(ctx).apply {
                useController = true; controllerShowTimeoutMs = 2000; controllerAutoShow = true; controllerHideOnTouch = true
                setShowSubtitleButton(true); setShowRewindButton(true); setShowFastForwardButton(true); this.player = player
                layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            }
        }, modifier = Modifier.fillMaxSize())
        state.watchParty?.let { p ->
            Text("PARTY ${p.roomCode} · ${if (p.isHost) "HOST" else "SYNCED"}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.TopEnd).padding(18.dp).background(Color(0x99000000), RoundedCornerShape(12.dp)).padding(horizontal = 10.dp, vertical = 6.dp))
        }
    }
}

@Composable private fun LoadingOverlay(text: String) {
    Box(Modifier.fillMaxSize().background(Color(0x99000000)), contentAlignment = Alignment.Center) {
        Text(text, color = Color.Black, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.background(Color.White, RoundedCornerShape(18.dp)).padding(horizontal = 28.dp, vertical = 17.dp))
    }
}

private fun formatPosition(ms: Long): String {
    val totalMinutes = (ms / 60_000L).coerceAtLeast(0)
    return if (totalMinutes >= 60) "${totalMinutes / 60}h ${totalMinutes % 60}m" else "${totalMinutes}m"
}
