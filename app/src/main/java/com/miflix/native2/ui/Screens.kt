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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.ExperimentalFoundationApi
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
import kotlinx.coroutines.launch
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
    Column(Modifier.fillMaxSize().background(Bg)) {
        Sidebar(screen, state.activeProfile.name, onNavigate, state.notifications.size, state.activeProfile)
        Box(Modifier.weight(1f).fillMaxWidth(), content = content)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(state: AppState,onNavigate: (Screen) -> Unit) {
    val scroll=rememberLazyListState()
    val heroFocus=remember { FocusRequester() }
    val scope=rememberCoroutineScope()
    var heroBackdrop by remember { mutableStateOf(state.trending.firstOrNull()?.backdrop) }
    val headerShade by remember { derivedStateOf { if(scroll.firstVisibleItemIndex>0) .88f else (scroll.firstVisibleItemScrollOffset/320f).coerceIn(0f,.88f) } }
    LaunchedEffect(Unit) { delay(550); state.loadHomeExtras() }
    LaunchedEffect(Unit) { state.loadDiscoveryArt() }
    val openItem: (MediaSummary) -> Unit={ item -> state.launch { state.open(item) } }
    val openCollection: (CollectionTile) -> Unit={ tile -> state.launch { state.openCollection(tile.id,tile.title) } }
    val returnToTop: () -> Unit={ scope.launch { scroll.scrollToItem(0) }; Unit }
    Box(Modifier.fillMaxSize().background(Bg)) {
        AsyncImage(heroBackdrop,null,Modifier.fillMaxWidth().height(420.dp),contentScale=ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x44000000),Bg),endY=650f)))
        CompositionLocalProvider(LocalBringIntoViewSpec provides object : BringIntoViewSpec {
            override fun calculateScrollDistance(offset: Float,size: Float,containerSize: Float): Float = when {
                offset<0 -> offset
                offset+size>containerSize -> offset+size-containerSize
                else -> 0f
            }
        }) {
            LazyColumn(Modifier.fillMaxSize().padding(top=76.dp),state=scroll,contentPadding=PaddingValues(bottom=64.dp)) {
                item(key="hero") { TopTenHero(state.trending.take(10),openItem,onFocused={ if(it) returnToTop() },onBackdrop={ heroBackdrop=it },buttonModifier=Modifier.focusRequester(heroFocus)) }
                if(state.continueWatching.isNotEmpty()) item(key="continue") {
                    MediaRail(tr("Seguir viendo","Continue Watching"),state.continueWatching,landscape=true,
                        progressFor={ state.progress[it.cloudId]?.percent },onClick=openItem)
                }
                if(state.forYou.isNotEmpty()) item(key="for_you") { MediaRail(tr("Para ti · tus puntuaciones","For you · Your ratings"),state.forYou,onClick=openItem) }
                item(key="genres_home") { Column {
                    Text(tr("Explora por género","Browse by genre"),color=Color.White,fontSize=24.sp,lineHeight=29.sp,fontWeight=FontWeight.Bold,
                        modifier=Modifier.padding(start=34.dp,top=16.dp,bottom=16.dp))
                    LazyRow(contentPadding=PaddingValues(horizontal=34.dp,vertical=8.dp),horizontalArrangement=Arrangement.spacedBy(18.dp)) {
                        items(state.genres,key={it.id}) { genre -> GenreCard(genre.copy(coverUrl=state.genreCovers[genre.id] ?: genre.coverUrl)) { state.launch { state.openGenre(genre) } } }
                    }
                    Spacer(Modifier.height(24.dp))
                } }
                item(key="directors_home") { DirectorRail(state) }
                item(key="streaming") { CollectionRail(tr("Plataformas","Streaming"),StreamingTiles,openCollection) }
                item(key="movies") { MediaRail(tr("Películas populares","Popular · Movies"),state.movies,onClick=openItem) }
                item(key="series") { MediaRail(tr("Series populares","Popular · Series"),state.series,onClick=openItem) }
                item(key="top") { MediaRail(tr("Mejor valorados","Top Rated"),state.topRated,onClick=openItem) }
            }
        }
        Sidebar(Screen.HOME,state.activeProfile.name,onNavigate,state.notifications.size,state.activeProfile,
            backgroundAlpha=headerShade,onHeaderFocused=returnToTop,
            onMoveDown=if(state.trending.isEmpty()) null else ({ scope.launch {
                scroll.scrollToItem(0)
                androidx.compose.runtime.withFrameNanos { }
                heroFocus.requestFocus()
            }; Unit }))
    }
}

@Composable
private fun DirectorRail(state: AppState) {
    Column {
    Text(tr("Directores","Directors"),color=Color.White,fontSize=24.sp,lineHeight=29.sp,fontWeight=FontWeight.Bold,
        modifier=Modifier.padding(start=34.dp,top=12.dp,bottom=16.dp))
    LazyRow(contentPadding=PaddingValues(horizontal=34.dp,vertical=8.dp),horizontalArrangement=Arrangement.spacedBy(18.dp)) {
        items(state.directors,key={it.key}) { group ->
            var focused by remember { mutableStateOf(false) }
            Column(Modifier.width(196.dp).onFocusChanged { focused=it.isFocused }.focusable()
                .tvClick { state.launch { state.socialAction { state.openDirector(group) } } }
                .clickable { state.launch { state.socialAction { state.openDirector(group) } } }
                .background(if(focused) Color.White else Panel,RoundedCornerShape(18.dp)).padding(10.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth().height(128.dp).clip(RoundedCornerShape(12.dp))) {
                    val photos=group.people.mapNotNull { it.photo }
                    if(photos.isEmpty()) Box(Modifier.fillMaxSize().background(Color(0xFF343434)),contentAlignment=Alignment.Center) {
                        Text(group.title.take(1),color=Color.White,fontSize=40.sp,lineHeight=48.sp)
                    } else photos.forEach { photo -> AsyncImage(photo,group.title,Modifier.weight(1f).fillMaxHeight(),contentScale=ContentScale.Crop) }
                }
                Text(if(group.key=="russo") tr("Hermanos Russo","Russo Brothers") else group.title,color=if(focused) Color.Black else Color.White,fontSize=14.sp,lineHeight=18.sp,maxLines=2)
            }
        }
    }
    Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun TopTenHero(rows: List<MediaSummary>, onOpen: (MediaSummary) -> Unit, onFocused: (Boolean) -> Unit = {}, onBackdrop: (String?) -> Unit = {},buttonModifier: Modifier = Modifier) {
    if (rows.isEmpty()) {
        Box(Modifier.fillMaxWidth().height(450.dp).background(Bg))
        return
    }
    var index by remember(rows.map { it.cloudId }) { mutableIntStateOf(0) }
    val safeIndex = index.coerceIn(0, rows.lastIndex)
    val hero = rows[safeIndex]
    LaunchedEffect(hero.backdrop) { onBackdrop(hero.backdrop) }

    LaunchedEffect(rows.map { it.cloudId }) {
        while (true) {
            delay(6500)
            index = (index + 1) % rows.size
        }
    }

    Box(Modifier.fillMaxWidth().padding(horizontal=24.dp).height(350.dp).clip(RoundedCornerShape(22.dp))) {
        Crossfade(hero, animationSpec = tween(260, easing = LinearOutSlowInEasing), label = "topHero") { item ->
            AsyncImage(item.backdrop, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x08000000), Color(0x2C000000), Bg), startY = 120f)))
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xF8050505), Color(0xA0050505), Color.Transparent), endX = 860f)))
        Column(Modifier.align(Alignment.CenterStart).padding(start = 32.dp, top = 18.dp).widthIn(max = 720.dp)) {
            Text(tr("TOP 10 · TENDENCIAS DE LA SEMANA","TOP 10  ·  TRENDING THIS WEEK"), color = Color(0xFFB8B8BC), fontSize = 12.sp, lineHeight=15.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Text(hero.title, color = Color.White, fontSize = 35.sp, lineHeight=42.sp, fontWeight = FontWeight.Black, maxLines = 2)
            Spacer(Modifier.height(10.dp))
            Text("${hero.year}  ·  ★ ${"%.1f".format(hero.rating)}", color = Color(0xFFE0E0E0), fontSize = 14.sp, lineHeight=17.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            Text(hero.overview, color = Color(0xFFE7E7EA), fontSize = 15.sp, lineHeight=18.sp, maxLines = 3, modifier = Modifier.widthIn(max = 690.dp))
            Spacer(Modifier.height(20.dp))
            FocusButton(tr("Ver detalles","View Details"), primary = true,onFocused=onFocused,modifier=buttonModifier) { onOpen(hero) }
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
        Text(title, color = Color.White, fontSize = 22.sp, lineHeight=26.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 34.dp, bottom = 8.dp))
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
        Text(title, color = Color.White, fontSize = 22.sp, lineHeight=26.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 34.dp, bottom = 8.dp))
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
            Text(title, color = Color.White, fontSize = 31.sp, lineHeight=37.sp, fontWeight = FontWeight.Black)
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
            Text(tr("Buscar","Search"), color = Color.White, fontSize = 32.sp, lineHeight=38.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(16.dp))
            NativeTextField(query, { query = it }, tr("Películas, series…","Movies, series…"), modifier = Modifier.width(620.dp))
            Spacer(Modifier.height(18.dp))
            if (searching) Text(tr("Buscando…","Searching…"), color = Muted, fontSize = 14.sp, lineHeight=17.sp)
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
    LaunchedEffect(Unit) { state.loadDiscoveryArt(); state.loadHomeExtras(); runCatching { state.loadComingSoon() }.onFailure { state.error=it.message } }
    val years = (2026 downTo 1990).map { year -> CollectionTile("year:$year", year.toString(), "") }
    AppShell(state, Screen.COLLECTIONS, { state.screen = it }) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 34.dp, bottom = 60.dp)) {
            item {
                Column(Modifier.padding(horizontal = 34.dp)) {
                    Text(tr("Colecciones","Collections"), color = Color.White, fontSize = 32.sp, lineHeight=38.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(5.dp))
                    Text(tr("Plataformas, géneros y películas por año.","Streaming services, genres and movies by year."), color = Muted, fontSize = 14.sp, lineHeight=17.sp)
                    Spacer(Modifier.height(24.dp))
                }
            }
            item { Column {
                Text(tr("Géneros","Genres"),color=Color.White,fontSize=24.sp,lineHeight=29.sp,fontWeight=FontWeight.Bold,
                    modifier=Modifier.padding(start=34.dp,bottom=16.dp))
                LazyRow(contentPadding=PaddingValues(horizontal=34.dp,vertical=6.dp),horizontalArrangement=Arrangement.spacedBy(18.dp)) {
                    items(state.genres,key={it.id}) { genre -> GenreCard(genre.copy(coverUrl=state.genreCovers[genre.id] ?: genre.coverUrl)) { state.launch { state.openGenre(genre) } } }
                }
                Spacer(Modifier.height(24.dp))
            } }
            if(state.comingMovies.isNotEmpty()) item { MediaRail(tr("Próximamente · Películas","Coming Soon · Movies"),state.comingMovies,onClick={ state.launch { state.open(it) } }) }
            if(state.comingSeries.isNotEmpty()) item { MediaRail(tr("Próximamente · Series","Coming Soon · Series"),state.comingSeries,onClick={ state.launch { state.open(it) } }) }
            item { CollectionRail(tr("Plataformas","Streaming"), StreamingTiles) { tile -> state.launch { state.openCollection(tile.id, tile.title) } } }
            item {
                Text(tr("Películas por año","Movies by Year"), color = Color.White, fontSize = 22.sp, lineHeight=26.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 34.dp, top = 8.dp, bottom = 10.dp))
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
            Text(tr("Géneros","Genres"), color = Color.White, fontSize = 32.sp, lineHeight=38.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(6.dp))
            Text(tr("40–50 resultados por género, entre películas y series.","40–50 results per genre, mixing movies and series."), color = Muted, fontSize = 14.sp, lineHeight=17.sp)
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
        Modifier.width(220.dp).height(104.dp).graphicsLayer { scaleX = scale; scaleY = scale }
            .onFocusChanged { focused = it.isFocused }.focusable().tvClick(onClick).clickable(onClick = onClick)
            .clip(RoundedCornerShape(14.dp)).background(if (focused) Color.White else Color(0xFF171717))
            .border(if (focused) 2.dp else 1.dp, if (focused) Color.White else Color(0xFF292929), RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (g.coverUrl.isNotBlank()) {
            AsyncImage(g.coverUrl, g.title, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(Modifier.fillMaxSize().background(Color(0x66000000)))
        }
        Text(g.title, color = if (focused && g.coverUrl.isBlank()) Color.Black else Color.White, fontSize = 21.sp, lineHeight=25.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
fun PlatformScreen(state: AppState, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    AppShell(state, Screen.PLATFORM_DETAIL, { state.screen = it }) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 32.dp, bottom = 64.dp)) {
            item {
                Column(Modifier.padding(horizontal = 34.dp)) {
                    Text(state.platformTitle, color = Color.White, fontSize = 34.sp, lineHeight=41.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(4.dp))
                    Text(tr("Top 10, películas, series, estrenos y mejores títulos por género.","Top 10, movies, series, latest releases and best-rated picks by genre."), color = Muted, fontSize = 14.sp, lineHeight=17.sp)
                    Spacer(Modifier.height(24.dp))
                }
            }
            items(state.platformSections, key = { it.id }) { section ->
                MediaRail(section.title, section.items, landscape = section.landscape, onClick = { state.launch { state.open(it) } })
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DetailsScreen(state: AppState, onBack: () -> Unit) {
    val item = state.selected ?: return
    val details = state.details
    val saved = state.progress[item.cloudId]
    var showRating by remember(item.cloudId) { mutableStateOf(false) }
    val detailScroll=rememberLazyListState()
    val playFocus = remember(item.cloudId) { FocusRequester() }
    LaunchedEffect(item.cloudId) { delay(150); playFocus.requestFocus(); detailScroll.scrollToItem(0) }
    BackHandler(onBack = onBack)
    if(showRating) RatingDialog(state,item) { showRating=false }

    Box(Modifier.fillMaxSize().background(Bg)) {
        AsyncImage(item.backdrop, null, Modifier.fillMaxWidth().height(590.dp).align(Alignment.TopCenter), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x22000000), Color(0x66000000), Bg), startY = 150f)))
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xF7050505), Color(0xA8050505), Color.Transparent), endX = 980f)))

        CompositionLocalProvider(LocalBringIntoViewSpec provides object : BringIntoViewSpec {
            override fun calculateScrollDistance(offset: Float,size: Float,containerSize: Float): Float = when {
                offset<0 -> offset
                offset+size>containerSize -> offset+size-containerSize
                else -> 0f
            }
        }) {
        LazyColumn(
            Modifier.fillMaxSize(), state=detailScroll,
            contentPadding = PaddingValues(start = 38.dp, end = 56.dp, top = 28.dp, bottom = 70.dp)
        ) {
            item(key = "hero") {
                Column(Modifier.fillMaxWidth().heightIn(min = 500.dp)) {
                    BackIconButton(onClick = onBack)
                    Spacer(Modifier.height(26.dp))
                    Text(item.title, color = Color.White, fontSize = 40.sp, lineHeight=46.sp, fontWeight = FontWeight.Black, maxLines = 2, overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    Spacer(Modifier.height(8.dp))
                    Text("${item.year}   ★ ${"%.1f".format(item.rating)}   ${details?.runtimeText.orEmpty()}", color = Color(0xFFD6D6D8), fontSize = 15.sp, lineHeight=18.sp)
                    if (!details?.genres.isNullOrEmpty()) {
                        Spacer(Modifier.height(7.dp))
                        Text(details?.genres?.take(4)?.joinToString("  ·  ").orEmpty(), color = Color(0xFFB9B9BD), fontSize = 13.sp, lineHeight=16.sp)
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(item.overview, color = Color(0xFFE8E8EB), fontSize = 16.sp, lineHeight=22.sp, maxLines = 3, modifier = Modifier.widthIn(max = 860.dp))
                    Spacer(Modifier.height(24.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement=Arrangement.spacedBy(12.dp)) {
                        if (item.type == "movie") {
                            val label = if ((saved?.percent ?: 0.0) in 1.0..95.0) "▶  ${tr("Reanudar","Resume")} ${formatPosition(saved?.position ?: 0)}" else tr("▶  Reproducir","▶  Play")
                            FocusButton(label, onLongClick = { state.launch { state.chooseCurrentSources() } }, primary = true, modifier = Modifier.focusRequester(playFocus).width(280.dp)) {
                                state.launch { runCatching { state.playMovie() }.onFailure { state.error = it.message } }
                            }
                        } else {
                            val canResume = (saved?.season ?: 0) > 0 && (saved?.episode ?: 0) > 0 && (saved?.percent ?: 0.0) < 96.0
                            FocusButton(if (canResume) "▶  ${tr("Continuar","Continue")} S${saved?.season} E${saved?.episode}" else tr("▶  Reproducir S1 E1","▶  Play S1 E1"), onLongClick = { state.launch { state.chooseCurrentSources() } }, primary = true, modifier = Modifier.focusRequester(playFocus).width(280.dp)) {
                                state.launch { runCatching { if (canResume) state.resumeSeries() else state.playSeriesFromStart() }.onFailure { state.error = it.message } }
                            }
                        }
                        FocusButton(if (state.favorites.contains(item.cloudId)) tr("✓ Mi lista","✓ My List") else tr("+ Mi lista","+ My List")) {
                            state.toggleFavorite(item); state.launch { runCatching { state.pushCloud() } }
                        }
                        FocusButton("★ ${state.ratings[item.cloudId]?.let { "$it/10" } ?: tr("Puntuar","Rate")}") { showRating=true }
                        FocusButton(tr("Sala compartida","Watch Party")) { state.screen = Screen.WATCH_PARTY }
                    }
                    Spacer(Modifier.height(38.dp))
                }
            }

            if (item.type == "series") {
                item(key = "episodes") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${tr("Temporada","Season")} ${state.currentSeason}", color = Color.White, fontSize = 25.sp, lineHeight=30.sp, fontWeight = FontWeight.Bold)
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
                    Text(tr("Reparto","Cast"), color = Color.White, fontSize = 27.sp, lineHeight=32.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(14.dp))
                    LazyRow(contentPadding = PaddingValues(end = 50.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        items(details?.cast.orEmpty(), key = { it.name }) { CastCard(it) }
                    }
                    Spacer(Modifier.height(28.dp))
                }
            }

            if (!details?.trailers.isNullOrEmpty()) {
                item(key = "trailers") {
                    Text(tr("Tráilers","Trailers"), color = Color.White, fontSize = 27.sp, lineHeight=32.sp, fontWeight = FontWeight.Black)
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
                Text(if (item.type == "movie") tr("Detalles de la película","Movie Details") else tr("Detalles de la serie","Series Details"), color = Color.White, fontSize = 27.sp, lineHeight=32.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(14.dp))
                InfoLine(tr("Estreno","Release Info"), details?.releaseInfo.orEmpty())
                InfoLine(tr("Duración","Runtime"), details?.runtimeText.orEmpty())
                InfoLine(tr("País de origen","Origin Country"), details?.originCountry.orEmpty())
                InfoLine(tr("Idioma original","Original Language"), details?.originalLanguage.orEmpty())
            }
        }
    }
}
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SettingsScreen(state: AppState, onBack: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var addonUrl by remember { mutableStateOf("") }
    val context = LocalContext.current
    BackHandler(onBack = onBack)

    AppShell(state, Screen.SETTINGS, { state.screen = it }) {
        SocialBackdrop()
        CompositionLocalProvider(LocalBringIntoViewSpec provides object : BringIntoViewSpec {
            override fun calculateScrollDistance(offset: Float,size: Float,containerSize: Float): Float = when {
                offset<24 -> offset-24
                offset+size>containerSize-24 -> offset+size-containerSize+24
                else -> 0f
            }
        }) {
        LazyColumn(Modifier.fillMaxSize().padding(horizontal=28.dp), contentPadding = PaddingValues(top=32.dp,bottom=60.dp), verticalArrangement=Arrangement.spacedBy(20.dp)) {
            item {
                Text(tr("Ajustes","Settings"), color = Color.White, fontSize = 34.sp, lineHeight=41.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(8.dp))
                Text("BruniO ${BuildConfig.VERSION_NAME}", color = Muted, fontSize = 14.sp, lineHeight=17.sp)
                Spacer(Modifier.height(30.dp))

            }
            item {
                Text(tr("Idioma de la app","App language"),color=Color.White,fontSize=22.sp, lineHeight=26.sp)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement=Arrangement.spacedBy(14.dp)) {
                    FocusButton("Español",primary=state.interfaceLanguage=="es") { state.launch { state.setInterfaceLanguage("es") } }
                    FocusButton("English",primary=state.interfaceLanguage=="en") { state.launch { state.setInterfaceLanguage("en") } }
                }
            }
            item {
                if (state.session == null) {
                    Text(tr("Cuenta y sincronización","Account & Sync"), color = Color.White, fontSize = 22.sp, lineHeight=26.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(tr("Usa tu correo y contraseña o escanea el QR seguro con tu celular.","Use email/password or scan the secure pairing QR with your phone."), color = Muted, fontSize = 14.sp, lineHeight=17.sp, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(16.dp))
                    NativeTextField(email, { email = it }, tr("Correo","Email"), modifier = Modifier.width(520.dp))
                    Spacer(Modifier.height(10.dp))
                    NativeTextField(password, { password = it }, tr("Contraseña","Password"), true, Modifier.width(520.dp))
                    Spacer(Modifier.height(14.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(18.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
                        FocusButton(tr("Iniciar sesión","Sign In"), primary = true) { state.launch { state.login(email.trim(), password) } }
                        FocusButton(tr("Acceder con QR","QR Sign-in")) { state.screen = Screen.PAIR_DEVICE }
                    }
                } else {
                    Text(tr("Cuenta y sincronización","Account & Sync"), color = Color.White, fontSize = 22.sp, lineHeight=26.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("${tr("Sesión de","Signed in as")} ${state.session?.email}", color = Color(0xFFE4E4E6), fontSize = 16.sp, lineHeight=19.sp)
                    Spacer(Modifier.height(14.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(18.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
                        FocusButton(tr("Sincronizar","Sync Now"), primary = true) { state.launch { state.syncFromCloud(); state.pushCloud(); if (state.tmdb.token.isNotBlank()) state.loadHome() } }
                        FocusButton(tr("Vincular / Complementos","Pair / Add Add-on")) { state.screen = Screen.PAIR_DEVICE }
                        FocusButton(tr("Cerrar sesión","Sign Out")) { state.signOut() }
                    }
                }

                Spacer(Modifier.height(28.dp))
                FlowRow(horizontalArrangement=Arrangement.spacedBy(18.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
                    FocusButton(tr("Amigos / Apodo / Frases","Friends / Nickname / Phrases")) { state.screen=Screen.FRIENDS }
                    FocusButton("Trakt") { state.screen=Screen.TRAKT }
                }
                Spacer(Modifier.height(18.dp))
            }
            item {
                Text(tr("Preferencias de reproducción","Playback preferences"),color=Color.White,fontSize=22.sp, lineHeight=26.sp)
                Text("Audio: ${state.audioLanguage.ifBlank { tr("Automático","Automatic") }}",color=Muted)
                LazyRow(horizontalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(vertical=8.dp)) { item { FlowRow(horizontalArrangement=Arrangement.spacedBy(18.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
                    listOf("" to tr("Automático","Auto"), "es" to "Español", "en" to "English", "pt" to "Português", "fr" to "Français").forEach { (code,label) ->
                        FocusButton(label,primary=state.audioLanguage==code) { state.savePlaybackPreferences(code,state.subtitleLanguage,state.autoplayNext) }
                    }
                } } }
                Spacer(Modifier.height(18.dp))
            }
            item {
                Text("${tr("Subtítulos","Subtitles")}: ${state.subtitleLanguage}",color=Muted)
                LazyRow(horizontalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(vertical=8.dp)) { item { FlowRow(horizontalArrangement=Arrangement.spacedBy(18.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
                    listOf("off" to tr("Desactivado","Off"), "es" to "Español", "en" to "English", "pt" to "Português", "fr" to "Français").forEach { (code,label) ->
                        FocusButton(label,primary=state.subtitleLanguage==code) { state.savePlaybackPreferences(state.audioLanguage,code,state.autoplayNext) }
                    }
                } } }
                Spacer(Modifier.height(18.dp))
            }
            item {
                FocusButton("${tr("Siguiente episodio automático","Autoplay next episode")}: ${if(state.autoplayNext) tr("Activado","ON") else tr("Desactivado","OFF")}") { state.savePlaybackPreferences(state.audioLanguage,state.subtitleLanguage,!state.autoplayNext) }
                Spacer(Modifier.height(24.dp))
            }
            item {
                Text(tr("Complementos de reproducción","Stream Add-ons"), color = Color.White, fontSize = 22.sp, lineHeight=26.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(tr("Los manifiestos de Torrentio y Comet se comparten entre los perfiles de esta cuenta.","Torrentio and Comet manifests are account-level, so every profile on this account shares them."), color = Muted, fontSize = 14.sp, lineHeight=17.sp, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                state.streamRepo.manifests().forEach { manifest ->
                    Text("• ${runCatching { Uri.parse(manifest).host }.getOrNull() ?: tr("Complemento configurado","Configured add-on")} · ${tr("configurado","configured")}", color = Color(0xFFE4E4E6), fontSize = 12.sp, lineHeight=15.sp, maxLines = 1)
                }
                Spacer(Modifier.height(12.dp))
                NativeTextField(addonUrl, { addonUrl = it }, tr("URL del manifiesto Torrentio / Comet","Torrentio / Comet manifest URL"), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(18.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
                    FocusButton(tr("Añadir manifiesto","Add Manifest")) { state.launch { state.addAddonManifest(addonUrl); addonUrl = "" } }
                    FocusButton(tr("Usar QR","Use QR")) { state.screen = Screen.PAIR_DEVICE }
                }

                Spacer(Modifier.height(34.dp))
            }
            item {
                Text(tr("Almacenamiento","Storage"),color=Color.White,fontSize=22.sp,lineHeight=26.sp)
                Spacer(Modifier.height(10.dp))
                Text(tr("Limpia las imágenes temporales. Conserva tu cuenta, perfiles, favoritos y ajustes.",
                    "Clear temporary images. Keep your account, profiles, favorites and settings."),color=Muted,fontSize=14.sp,lineHeight=18.sp)
                Spacer(Modifier.height(14.dp))
                FocusButton(if(state.clearingCache) tr("Limpiando…","Clearing…") else tr("Eliminar caché","Clear cache")) {
                    if(!state.clearingCache) state.launch { state.socialAction { state.clearCache() } }
                }
                if(state.cacheCleared) {
                    Spacer(Modifier.height(10.dp))
                    Text(tr("Caché eliminada. Las imágenes se descargarán cuando las necesites.",
                        "Cache cleared. Images will download again when needed."),color=Color(0xFFB6DDC0),fontSize=14.sp,lineHeight=18.sp)
                }
            }
            item {
                Text(tr("Actualizaciones","Updates"), color = Color.White, fontSize = 22.sp, lineHeight=26.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(tr("La URL del APK se conserva para mantener tu código de Downloader.","The latest channel keeps the same APK URL, so your Downloader code stays permanent."), color = Muted, fontSize = 14.sp, lineHeight=17.sp, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(14.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(18.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
                    FocusButton(if (state.updateChecking) tr("Comprobando…","Checking…") else tr("Buscar actualizaciones","Check for Updates"), primary = true) {
                        if (!state.updateChecking) state.launch { state.checkForUpdates() }
                    }
                    state.updateInfo?.takeIf { it.isNewer }?.let { info ->
                        FocusButton("${tr("Abrir","Open")} ${info.version}") { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(info.downloadUrl))) } }
                    }
                }
                state.updateInfo?.let { info ->
                    Spacer(Modifier.height(9.dp))
                    Text(if (info.isNewer) "${tr("Actualización disponible","Update available")}: ${info.version}" else tr("Tienes la versión más reciente.","You're on the latest build."), color = Color(0xFFE7E7E9), fontSize = 13.sp, lineHeight=16.sp)
                }
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
            Text(tr("Vincular BruniO","Pair MiFlix"), color = Color.White, fontSize = 36.sp, lineHeight=43.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(10.dp))
            Text(tr("Escanea el QR con tu celular para iniciar sesión o enviar un manifiesto de Torrentio o Comet.","Scan this QR on your phone to sign in and/or send a Torrentio or Comet manifest without typing it on the TV."), color = Muted, fontSize = 15.sp, lineHeight=18.sp)
            Spacer(Modifier.height(24.dp))
            state.pairingRequest?.let { req ->
                QrCode(req.url, 260.dp)
                Spacer(Modifier.height(16.dp))
                Text(state.pairingStatus, color = Color.White, fontSize = 15.sp, lineHeight=18.sp)
                Text(tr("Caduca en unos 10 minutos","Expires in about 10 minutes"), color = Muted, fontSize = 12.sp, lineHeight=15.sp)
            } ?: Text(state.pairingStatus.ifBlank { tr("Preparando…","Preparing…") }, color = Color.White, fontSize = 16.sp, lineHeight=19.sp)
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                FocusButton(tr("Volver","Back"), primary = true) { onBack() }
                FocusButton(tr("Nuevo QR","New QR")) { state.pairingRequest = null; state.launch { state.startPairing() } }
            }
        }
    }
}

@Composable
fun ProfilesScreen(state: AppState, onBack: () -> Unit) {
    var newName by remember { mutableStateOf("") }
    var newAvatar by remember { mutableStateOf("ai:astronaut") }
    var editingAvatar by remember { mutableStateOf<Profile?>(null) }
    var deletingProfile by remember { mutableStateOf<Profile?>(null) }
    var choosingNewAvatar by remember { mutableStateOf(false) }
    BackHandler(onBack = onBack)
    Box(Modifier.fillMaxSize()) {
    SocialBackdrop()
    LazyColumn(Modifier.fillMaxSize().padding(horizontal=50.dp),contentPadding=PaddingValues(vertical=32.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
        item { Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text(tr("¿Quién está viendo?","Who's watching?"),color=Color.White,fontSize=38.sp, lineHeight=45.sp,fontWeight=FontWeight.Black)
            Text(tr("Los avatares se guardan con tu perfil. Favoritos y progreso siguen separados.","Avatars are saved with your profile. Favorites and progress stay separate."),color=Muted,fontSize=14.sp, lineHeight=17.sp)
        } }
        item {
            LazyRow(horizontalArrangement=Arrangement.spacedBy(20.dp),contentPadding=PaddingValues(8.dp)) {
                items(state.profiles,key={it.id}) { profile ->
                    var focused by remember { mutableStateOf(false) }
                    Column(Modifier.width(180.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(14.dp)) {
                        Column(Modifier.onFocusChanged { focused=it.isFocused }.focusable()
                            .tvClick { state.launch { state.selectProfile(profile) } }
                            .clickable { state.launch { state.selectProfile(profile) } },horizontalAlignment=Alignment.CenterHorizontally) {
                            Box(Modifier.border(if(focused) 3.dp else 0.dp,Color.White,RoundedCornerShape(22.dp)).padding(5.dp)) {
                                ProfileAvatar(profile,Modifier.size(120.dp))
                            }
                            Text(profile.name,color=Color.White,fontSize=17.sp, lineHeight=21.sp)
                        }
                        FocusButton("Avatar") { editingAvatar=profile }
                        if(state.profiles.size>1) FocusButton(tr("Eliminar perfil","Delete profile")) { deletingProfile=profile }
                    }
                }
            }
        }
        item { Column {
            Text(tr("Crear perfil","Create profile"),color=Color.White,fontSize=22.sp, lineHeight=26.sp,fontWeight=FontWeight.Bold)
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement=Arrangement.spacedBy(22.dp),verticalAlignment=Alignment.CenterVertically) {
                ProfileAvatar(Profile("new",newName,avatarValue=newAvatar),Modifier.size(72.dp))
                NativeTextField(newName,{newName=it},tr("Nombre del perfil","Profile name"),modifier=Modifier.width(340.dp))
            }
            Spacer(Modifier.height(22.dp))
            FlowRow(horizontalArrangement=Arrangement.spacedBy(20.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
                FocusButton(tr("Elegir avatar","Choose avatar")) { choosingNewAvatar=true }
                FocusButton(tr("Añadir perfil","Add Profile"),primary=true) { state.launch { state.socialAction { state.createProfile(newName,newAvatar); newName="" } } }
            }
            Spacer(Modifier.height(26.dp))
            FocusButton(tr("Volver","Back")) { onBack() }
        } }
    }
    }
    deletingProfile?.let { profile ->
        Dialog(onDismissRequest={ deletingProfile=null }) {
            Column(Modifier.width(420.dp).background(Panel,RoundedCornerShape(24.dp)).padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
                Text(tr("Eliminar perfil","Delete profile"),color=Color.White,fontSize=26.sp, lineHeight=31.sp)
                Text(profile.name,color=Color.White,fontSize=20.sp, lineHeight=24.sp)
                Text(tr("Se eliminarán este perfil, sus favoritos, progreso y puntuaciones. Los demás perfiles se conservan.","This deletes this profile, its favorites, progress and ratings. Other profiles stay unchanged."),color=Muted)
                FocusButton(tr("Cancelar","Cancel"),primary=true) { deletingProfile=null }
                FocusButton(tr("Eliminar perfil","Delete profile")) { state.launch { state.socialAction { state.deleteProfile(profile); deletingProfile=null } } }
            }
        }
    }
    if(choosingNewAvatar) AvatarPickerDialog(newAvatar,hidden=state.hiddenAvatars,remove=state::hideAvatar,restore=state::restoreAvatars,choose={ newAvatar=it; choosingNewAvatar=false },close={ choosingNewAvatar=false })
    editingAvatar?.let { profile ->
        AvatarPickerDialog(profile.avatarValue,hidden=state.hiddenAvatars,remove=state::hideAvatar,restore=state::restoreAvatars,choose={ avatar ->
            state.launch { state.socialAction { state.setProfileAvatar(profile,avatar); editingAvatar=null } }
        },close={ editingAvatar=null })
    }
}

@Composable
fun WatchPartyScreen(state: AppState,onBack: () -> Unit) {
    var joinCode by remember { mutableStateOf("") }
    BackHandler(onBack=onBack)
    AppShell(state,Screen.WATCH_PARTY,{ state.screen=it }) {
        SocialBackdrop()
        LazyColumn(Modifier.fillMaxSize().padding(horizontal=28.dp),contentPadding=PaddingValues(vertical=28.dp),verticalArrangement=Arrangement.spacedBy(24.dp)) {
            item {
                Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(tr("Sala compartida","Watch Party"),color=Color.White,fontSize=32.sp, lineHeight=38.sp,fontWeight=FontWeight.Bold)
                        Text(tr("Una sala para todas tus películas y series","One room for all your movies and series"),color=Muted,fontSize=13.sp, lineHeight=16.sp)
                    }
                    BackIconButton(onBack)
                }
            }
            item {
                Row(horizontalArrangement=Arrangement.spacedBy(18.dp)) {
                    Column(Modifier.weight(1f).background(Color(0xAA242424),RoundedCornerShape(20.dp)).padding(22.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
                        Text(tr("Tu sesión","Your session"),color=Color.White,fontSize=23.sp, lineHeight=28.sp)
                        Text(tr("Invita a tus amigos. Tú apruebas quién entra.","Invite your friends. You approve who joins."),color=Muted,fontSize=14.sp, lineHeight=17.sp)
                        if(state.watchParty==null) {
                            FocusButton(tr("Crear sala","Create Room"),primary=true,modifier=Modifier.fillMaxWidth()) { state.launch { state.socialAction { state.createWatchParty() } } }
                            if(state.selected!=null || state.playerRequest!=null) FocusButton(tr("Con el contenido actual","With the current content"),modifier=Modifier.fillMaxWidth()) { state.launch { state.socialAction { state.createWatchParty(true) } } }
                        } else {
                            val party=state.watchParty!!
                            Text("${tr("Sala","Room")} ${party.roomCode}",color=Color.White,fontSize=26.sp, lineHeight=31.sp)
                            Text(if(state.partyRole==PartyRole.HOST) tr("Anfitrión","Host") else tr("Invitado","Guest"),color=Muted)
                            if(state.playerRequest!=null) FocusButton(tr("Volver al reproductor","Return to player")) { state.screen=Screen.PLAYER }
                            FocusButton(tr("Salir / cerrar sala","Leave / Close room")) { state.launch { state.leaveWatchParty() } }
                        }
                    }
                    Column(Modifier.weight(1f).background(Color(0xAA242424),RoundedCornerShape(20.dp)).padding(22.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
                        if(state.watchParty==null) {
                            Text(tr("Unirte a una sesión","Join a session"),color=Color.White,fontSize=23.sp, lineHeight=28.sp)
                            Text(tr("Escribe el código y espera aprobación del host.","Enter the code and wait for host approval."),color=Muted,fontSize=14.sp, lineHeight=17.sp)
                            NativeTextField(joinCode,{ joinCode=it.filter(Char::isDigit).take(6) },tr("Código de 6 dígitos","6-digit code"),modifier=Modifier.fillMaxWidth())
                            FocusButton(tr("Solicitar acceso","Request access"),modifier=Modifier.fillMaxWidth()) { state.launch { state.socialAction { state.joinWatchParty(joinCode) } } }
                        } else {
                            val party=state.watchParty!!
                            Text(tr("Chat desde tu celular","Chat from your phone"),color=Color.White,fontSize=23.sp, lineHeight=28.sp)
                            QrCode("https://randy3m2.github.io/miflix-tv-native/party/?room=${party.roomCode}",150.dp)
                            if(state.partyRole==PartyRole.HOST) FocusButton(tr("Elegir contenido","Choose content")) { state.screen=Screen.HOME }
                        }
                    }
                }
            }
            item {
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Text(tr("Solicitudes de acceso","Access requests"),color=Color.White,fontSize=22.sp, lineHeight=26.sp,modifier=Modifier.weight(1f))
                    FocusButton(tr("Actualizar","Refresh")) { state.launch { state.socialAction { state.refreshFriendsDirectory() } } }
                }
                AccessRequests(state)
                if(state.accessRequests.isEmpty()) Text(tr("Sin solicitudes pendientes","No pending requests"),color=Muted)
            }
            if(state.watchParty!=null) {
                item {
                    Text(tr("Reacciones","Reactions"),color=Color.White,fontSize=22.sp, lineHeight=26.sp)
                    LazyRow(horizontalArrangement=Arrangement.spacedBy(12.dp)) { items(PartyEmojis) { emoji -> FocusButton(emoji) { state.launch { state.sendPartyEvent("emoji",emoji) } } } }
                }
                item { Text(tr("Participantes","Participants"),color=Color.White,fontSize=22.sp, lineHeight=26.sp) }
                items(state.partyMembers,key={it.id}) { person ->
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                        Text("@${person.nickname}",color=Color.White,modifier=Modifier.weight(1f))
                        if(person.id!=state.session?.userId) FocusButton(tr("Agregar amigo","Add friend")) { state.launch { state.addFriend(person.id) } }
                    }
                }
                item { Text(tr("Chat reciente","Recent chat"),color=Color.White,fontSize=22.sp, lineHeight=26.sp) }
                items(state.partyMessages.takeLast(8),key={it.id}) { event -> Text(event.body,color=Muted) }
            }
            if(state.partyStatus.isNotBlank()) item { Text(state.partyStatus,color=Color(0xFF9DD4FF)) }
        }
    }
}

@Composable
fun PlayerScreen(state: AppState, request: PlayerRequest, onClose: () -> Unit) {
    val context = LocalContext.current
    val playbackView = LocalView.current
    var controlsVisible by remember(request.playbackId) { mutableStateOf(true) }
    var controlInteraction by remember { mutableStateOf(0) }
    val surfaceFocus = remember { FocusRequester() }
    val playFocus = remember { FocusRequester() }
    var playerMenu by remember(request.playbackId) { mutableStateOf<String?>(null) }
    var resizeMode by remember { mutableStateOf(androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    var maximized by remember { mutableStateOf(true) }
    var speed by remember(request.playbackId) { mutableStateOf(1f) }
    var volume by remember(request.playbackId) { mutableStateOf(1f) }
    var tracks by remember(request.playbackId) { mutableStateOf(androidx.media3.common.Tracks.EMPTY) }
    var showPartyActions by remember { mutableStateOf(false) }
    var showAccessRequests by remember { mutableStateOf(false) }
    var showEpisodes by remember(request.playbackId) { mutableStateOf(false) }
    val subtitles = request.stream.subtitles
    val preferredText = state.subtitleLanguage.takeUnless { it == "off" }
    var paused by remember(request.playbackId) { mutableStateOf(false) }
    var pauseSynopsis by remember(request.playbackId) { mutableStateOf(false) }
    var pauseInfoDismissed by remember(request.playbackId) { mutableStateOf(false) }
    var dismissKey by remember { mutableStateOf<Int?>(null) }
    var clockNow by remember { mutableStateOf(System.currentTimeMillis()) }
    var applyingRemote by remember { mutableStateOf(false) }
    var guestPlaybackIntent by remember(request.playbackId) { mutableStateOf<Boolean?>(null) }
    var guestIntentUntil by remember(request.playbackId) { mutableStateOf(0L) }
    var positionMs by remember(request.playbackId) { mutableStateOf(request.resumeMs) }
    var durationMs by remember(request.playbackId) { mutableStateOf(0L) }
    var segments by remember(request.playbackId) { mutableStateOf(PlaybackSegments()) }
    var episodeInfo by remember(request.playbackId) { mutableStateOf<EpisodeSummary?>(null) }
    var upcoming by remember(request.playbackId) { mutableStateOf<EpisodeSummary?>(null) }
    var recommendations by remember(request.playbackId) { mutableStateOf<List<MediaSummary>>(emptyList()) }
    var rateAtEnd by remember(request.playbackId) { mutableStateOf(false) }
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
                            setLabel(s.label.ifBlank { s.lang.ifBlank { tr("Subtítulo","Subtitle") } })
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
            state.launch { recommendations = runCatching { state.recommendationsFor(request.item) }.getOrDefault(emptyList()) }
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
        pauseInfoDismissed = false
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
            override fun onTracksChanged(value: androidx.media3.common.Tracks) { tracks = value }
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                paused = !playWhenReady && player.playbackState != Player.STATE_ENDED
                if(state.partyRole == PartyRole.GUEST && !applyingRemote && reason == Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST) {
                    if(playWhenReady != state.watchParty?.playing) {
                        guestPlaybackIntent=playWhenReady; guestIntentUntil=System.currentTimeMillis()+5000
                        state.launch { runCatching { state.requestPartyPlayback(request,playWhenReady) }.onFailure {
                            guestPlaybackIntent=null; state.error=it.message
                        } }
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
        tracks = player.currentTracks
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
                    runCatching { state.takePartyPlayback() }.getOrNull()?.let { playing -> player.playWhenReady=playing }
                    state.hostPartyUpdate(request, player.currentPosition, player.playWhenReady && player.playbackState != Player.STATE_ENDED)
                }
                PartyRole.GUEST -> {
                    val remote = state.refreshParty() ?: continue
                    if (remote.cloudId != request.item.cloudId || remote.season != request.season || remote.episode != request.episode) {
                        state.switchToPartyState(remote)
                        break
                    }
                    if(guestPlaybackIntent==remote.playing || System.currentTimeMillis()>=guestIntentUntil) guestPlaybackIntent=null
                    // Preserve the paused decoder frame. Do not seek to stale host snapshots while
                    // a guest pause/resume request is awaiting acknowledgement, or while paused.
                    // Reconcile position on resume / during playback before applying remote play.
                    if(guestPlaybackIntent==null && remote.playing && !request.live && player.isCurrentMediaItemSeekable && abs(remote.positionMs-player.currentPosition)>1800) {
                        player.seekTo(remote.positionMs)
                    }
                    if (guestPlaybackIntent==null && remote.playing != player.playWhenReady) {
                        applyingRemote = true; player.playWhenReady = remote.playing; applyingRemote = false
                    }
                }
                null -> Unit
            }
        }
    }

    // Explicit timer also covers controls holding focus on TV remotes.
    LaunchedEffect(player, controlInteraction, showEpisodes, showPartyActions, showAccessRequests, state.sourceSelection, playerMenu, paused, ended) {
        if (!ended && playerMenu == null && !showEpisodes && !showPartyActions && !showAccessRequests && state.sourceSelection == null) {
            delay(3000)
            controlsVisible = false
            surfaceFocus.requestFocus()
        }
    }

    LaunchedEffect(controlsVisible, showEpisodes, showPartyActions, showAccessRequests, playerMenu, state.sourceSelection, pauseSynopsis, ended) {
        if(controlsVisible && !pauseSynopsis && !ended && !showEpisodes && !showPartyActions && !showAccessRequests && playerMenu == null && state.sourceSelection == null) {
            withFrameNanos { }; playFocus.requestFocus()
        }
    }
    BackHandler {
        if (playerMenu != null) playerMenu = null
        else if (showEpisodes) { showEpisodes=false; player.play() }
        else if (paused && !pauseInfoDismissed) {
            pauseSynopsis = false; pauseInfoDismissed = true
            controlsVisible = true; controlInteraction++
        } else onClose()
    }

    Box(Modifier.fillMaxSize().background(Color.Black).focusRequester(surfaceFocus).onPreviewKeyEvent { event ->
        val code = event.nativeKeyEvent.keyCode
        if (dismissKey == code) {
            if (event.type == KeyEventType.KeyUp) dismissKey = null
            true
        } else if (event.type == KeyEventType.KeyDown && !showEpisodes && !showPartyActions && !showAccessRequests && playerMenu == null && state.sourceSelection == null) {
            val wasHidden = !controlsVisible
            controlsVisible = true
            controlInteraction++
            if (paused && !pauseInfoDismissed) {
                pauseInfoDismissed = true; pauseSynopsis = false; dismissKey = code
                true
            } else if(code == android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE || code == android.view.KeyEvent.KEYCODE_MEDIA_PLAY || code == android.view.KeyEvent.KEYCODE_MEDIA_PAUSE) {
                player.playWhenReady = when(code) { android.view.KeyEvent.KEYCODE_MEDIA_PLAY -> true; android.view.KeyEvent.KEYCODE_MEDIA_PAUSE -> false; else -> !player.playWhenReady }
                dismissKey = code; true
            } else if(wasHidden) { dismissKey = code; true } else false
        } else false
    }.focusable()) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    setKeepContentOnPlayerReset(true)
                    useController = false
                    isFocusable = false
                    setOnTouchListener { _, event ->
                        if(event.action==android.view.MotionEvent.ACTION_UP) {
                            pauseInfoDismissed=true; pauseSynopsis=false; controlsVisible=true; controlInteraction++
                        }
                        true
                    }
                    this.player = player
                    layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                }
            },
            update = { view ->
                if (view.player !== player) view.player = player
                view.resizeMode = resizeMode
            },
            modifier = if(maximized) Modifier.fillMaxSize() else Modifier.fillMaxSize().padding(horizontal=40.dp,vertical=24.dp)
        )

        if(playerMenu != null) PlayerOptionsDialog(playerMenu!!, player, tracks, speed, volume,
            onSpeed={ speed=it; player.setPlaybackSpeed(it) }, onVolume={ volume=it; player.volume=it },
            series=request.item.type=="series", party=state.watchParty!=null,
            onEpisodes={ player.pause(); showEpisodes=true },
            onParty={ if(state.watchParty!=null) showPartyActions=true else state.screen=Screen.WATCH_PARTY },
            onClose={ playerMenu=null; controlInteraction++ })
        if(controlsVisible && !pauseSynopsis && !showEpisodes && !showPartyActions && !ended) {
            CompactPlayerControls(request, paused, positionMs, durationMs, speed, volume, maximized, playFocus,
                onInteraction={ controlInteraction++ },
                onPlay={ player.playWhenReady=!player.playWhenReady },
                onResize={ resizeMode=if(resizeMode == androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT) androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM else androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT },
                onMenu={ playerMenu=it }, onLinks={ state.launch { state.playerSources(request,player.currentPosition) } },
                onSeek={ if(player.isCurrentMediaItemSeekable) { positionMs=it; player.seekTo(it) } }, seekable=player.isCurrentMediaItemSeekable && durationMs>0,
                onClose=onClose, onMaximize={ maximized=!maximized })
        }
        if(showAccessRequests) {
            Dialog(onDismissRequest={ showAccessRequests=false },properties=DialogProperties(usePlatformDefaultWidth=false)) {
                Column(Modifier.width(650.dp).background(Panel,RoundedCornerShape(20.dp)).padding(24.dp)) {
                    AccessRequests(state)
                    if(state.accessRequests.isEmpty()) Text(tr("No hay solicitudes pendientes","No pending requests"),color=Muted)
                    FocusButton(tr("Cerrar","Close")) { showAccessRequests=false }
                }
            }
        }
        if(controlsVisible && state.accessRequests.isNotEmpty() && !showEpisodes && !showPartyActions) {
            Box(Modifier.align(Alignment.CenterEnd).padding(26.dp)) {
                FocusButton("${state.accessRequests.size} ${tr("solicitudes · Sala","requests · Party")}") { showAccessRequests=true }
            }
        }
        if(pauseSynopsis && !pauseInfoDismissed && !showEpisodes && !showPartyActions && playerMenu == null && state.sourceSelection == null && !ended) {
            Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xC9000000),Color(0x22000000))))) {
                Column(Modifier.align(Alignment.CenterStart).padding(60.dp).widthIn(max=680.dp)) {
                    Text(tr("Estás viendo","You're watching"),color=Muted,fontSize=18.sp, lineHeight=22.sp)
                    Text(request.item.title,color=Color.White,fontSize=38.sp, lineHeight=45.sp,fontWeight=FontWeight.Bold)
                    if(request.item.type == "series") Text("S${request.season} E${request.episode} · ${episodeInfo?.title.orEmpty()}",color=Color.White,fontSize=22.sp, lineHeight=26.sp)
                    Spacer(Modifier.height(14.dp))
                    Text(episodeInfo?.overview?.takeIf { it.isNotBlank() } ?: request.item.overview,color=Color(0xFFE0E0E0),fontSize=19.sp, lineHeight=23.sp,maxLines=5)
                }

            }
        }
        if(paused && !pauseInfoDismissed && !ended && !showEpisodes && !showPartyActions && playerMenu == null && state.sourceSelection == null) {
            val clock = android.text.format.DateFormat.getTimeFormat(context).format(java.util.Date(clockNow))
            val remaining = (durationMs - positionMs).coerceAtLeast(0L) / 1000
            val remainingText = when {
                request.live -> tr("En vivo","Live")
                durationMs <= 0 -> "Tiempo restante no disponible"
                remaining >= 3600 -> "%d:%02d:%02d".format(remaining / 3600, remaining / 60 % 60, remaining % 60)
                else -> "%d:%02d".format(remaining / 60, remaining % 60)
            }
            Column(Modifier.align(Alignment.BottomEnd).padding(end=35.dp,bottom=100.dp)
                .background(Color(0xAA000000),RoundedCornerShape(12.dp)).padding(14.dp)) {
                Text("${tr("Hora","Time")} · $clock",color=Color(0xFFE0E0E0),fontSize=19.sp, lineHeight=23.sp)
                Text(if(request.live || durationMs <= 0) remainingText else "${tr("Restante","Remaining")} · $remainingText",color=Color(0xFFE0E0E0),fontSize=19.sp, lineHeight=23.sp)
            }
        }
        if(!ended && !showEpisodes && state.partyRole != PartyRole.GUEST && durationMs > 0) {
            val intro = segments.intro?.takeIf { it.endMs <= durationMs && positionMs >= it.startMs && positionMs < it.endMs }
            val credits = segments.outro?.takeIf { it.startMs < durationMs && positionMs >= it.startMs && positionMs < it.endMs }
            if(intro != null || credits != null) Box(Modifier.align(Alignment.BottomEnd).padding(end=30.dp,bottom=100.dp)) {
                if(intro != null) FocusButton(tr("Saltar introducción","Skip Intro"),primary=true) { player.seekTo(intro.endMs) }
                else if(credits != null) FocusButton(tr("Saltar créditos","Skip Credits"),primary=true) {
                    val post = segments.postCredits?.startMs?.takeIf { it > positionMs && it < durationMs }
                    player.seekTo(post ?: credits.endMs.coerceAtMost(durationMs))
                }
            }
        }
        if(rateAtEnd) RatingDialog(state,request.item) { rateAtEnd=false; state.launch { recommendations=state.recommendationsFor(request.item) } }
        if(ended && state.partyRole != PartyRole.GUEST && !rateAtEnd) {
            Dialog(onDismissRequest={ cancelAutoplay=true; onClose() },properties=DialogProperties(usePlatformDefaultWidth=false)) {
                Column(Modifier.width(850.dp).heightIn(max=600.dp).background(Color(0xF00B0B0B),RoundedCornerShape(20.dp)).padding(25.dp)) {
                    Text("${tr("Terminaste","You finished")} ${request.item.title}",color=Color.White,fontSize=26.sp, lineHeight=31.sp)
                    upcoming?.let { next ->
                        Text("${tr("Siguiente","Next")}: S${next.season} E${next.episode} · ${next.title}",color=Muted)
                        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                            FocusButton(if(state.autoplayNext && !cancelAutoplay) "${tr("Siguiente episodio","Next episode")} · ${countdown}s" else tr("Siguiente episodio","Next episode"),primary=true) { cancelAutoplay=true; state.launch { state.playNext(request,next) } }
                            FocusButton(tr("Cancelar reproducción automática","Cancel autoplay")) { cancelAutoplay=true }
                        }
                    }
                    Text(tr("También te puede gustar","You may also like"),color=Color.White,fontSize=22.sp, lineHeight=26.sp)
                    LazyRow(horizontalArrangement=Arrangement.spacedBy(12.dp),modifier=Modifier.weight(1f,false)) {
                        items(recommendations,key={it.cloudId}) { item ->
                            MediaCard(item) { cancelAutoplay=true; state.launch { state.open(item) } }
                        }
                    }
                    Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                        FocusButton(tr("★ Puntuar","★ Rate")) { cancelAutoplay=true; rateAtEnd=true }
                        FocusButton(tr("Volver a detalles","Back to details")) { cancelAutoplay=true; onClose() }
                    }
                }
            }
        }
        if (showPartyActions) PartyActionsDialog(state) { showPartyActions = false }
        if (state.watchParty != null && state.floatingEvents.isNotEmpty()) {
            Column(Modifier.align(Alignment.CenterEnd).padding(30.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                state.floatingEvents.toList().forEach { event ->
                    val name=state.partyMembers.firstOrNull { it.id==event.userId }?.nickname ?: tr("Invitado","Guest")
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
                Text(tr("Episodios","Episodes"), color = Color.White, fontSize = 29.sp, lineHeight=35.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.weight(1f))
                FocusButton(tr("Cerrar","Close")) { onClose() }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                FocusButton("‹") { if (state.currentSeason > 1) state.launch { state.loadSeason(state.currentSeason - 1) } }
                Text("${tr("Temporada","Season")} ${state.currentSeason}", color = Color.White, fontSize = 18.sp, lineHeight=22.sp, fontWeight = FontWeight.Bold)
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
            Text("S${ep.season} E${ep.episode} · ${ep.title}", color = if (focused) Color.Black else Color.White, fontSize = 14.sp, lineHeight=17.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(ep.overview, color = if (focused) Color(0xFF333333) else Muted, fontSize = 11.sp, lineHeight=13.sp, maxLines = 2)
        }
        if (active) Text(tr("ACTUAL","NOW"), color = if (focused) Color.Black else Color.White, fontSize = 10.sp, lineHeight=12.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    if (value.isBlank()) return
    Row(Modifier.widthIn(max = 900.dp).fillMaxWidth().border(0.5.dp, Color(0x332F2F2F)).padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color(0xFFCACACD), fontSize = 14.sp, lineHeight=17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(240.dp))
        Text(value, color = Color.White, fontSize = 15.sp, lineHeight=18.sp)
    }
}

@Composable
private fun CastCard(person: CastMember) {
    Column(Modifier.width(138.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(106.dp).clip(RoundedCornerShape(53.dp)).background(Color(0xFF252525)), contentAlignment = Alignment.Center) {
            if (!person.profile.isNullOrBlank()) AsyncImage(person.profile, person.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else Text(person.name.split(" ").take(2).mapNotNull { it.firstOrNull()?.toString() }.joinToString(""), color = Color(0xFFAAAAAF), fontSize = 22.sp, lineHeight=26.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        Text(person.name, color = Color.White, fontSize = 13.sp, lineHeight=16.sp, maxLines = 1)
        if (person.character.isNotBlank()) Text(person.character, color = Muted, fontSize = 10.sp, lineHeight=12.sp, maxLines = 1)
    }
}

@Composable
private fun TrailerCard(trailer: TrailerSummary, onClick: () -> Unit) {
    var focused by remember(trailer.key) { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.02f else 1f, tween(90, easing = LinearOutSlowInEasing), label = "trailerScale")
    Column(Modifier.width(330.dp).graphicsLayer { scaleX = scale; scaleY = scale }.onFocusChanged { focused = it.isFocused }.focusable().tvClick(onClick).clickable(onClick = onClick)) {
        Box(Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF1A1A1A)).border(if (focused) 2.dp else 0.dp, Color.White, RoundedCornerShape(12.dp))) {
            AsyncImage(trailer.thumbnail, trailer.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(Modifier.fillMaxSize().background(Color(0x33000000)), contentAlignment = Alignment.Center) { Text("▶", color = Color.White, fontSize = 31.sp, lineHeight=37.sp) }
        }
        Spacer(Modifier.height(7.dp))
        Text(trailer.name, color = Color.White, fontSize = 13.sp, lineHeight=16.sp, maxLines = 1)
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
                if (focused) Box(Modifier.fillMaxSize().background(Color(0x26000000)), contentAlignment = Alignment.Center) { Text("▶", color = Color.White, fontSize = 32.sp, lineHeight=38.sp) }
            }
            Spacer(Modifier.height(7.dp))
            Text("S${ep.season} E${ep.episode} · ${ep.title}", color = Color.White, fontSize = 13.sp, lineHeight=16.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            if (ep.airDate.isNotBlank()) Text(ep.airDate, color = Muted, fontSize = 10.sp, lineHeight=12.sp, maxLines = 1)
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
                    Text("${tr("Enlaces de reproducción","Playback links")} · ${selection.streams.size}", color = Color.White, fontSize = 26.sp, lineHeight=31.sp,
                        fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    FocusButton(tr("Cerrar","Close")) { state.sourceSelection = null }
                }
                if(state.partyRole==PartyRole.GUEST) Text(tr("Este enlace se cambia solo para ti · sigues sincronizado con el host","This link changes only for you · You stay synced with the host"),color=Muted)
                Spacer(Modifier.height(18.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 30.dp)) {
                    items(selection.streams) { stream ->
                        SourceLinkCard(stream,Modifier.fillMaxWidth().then(
                            if(stream===selection.streams.first()) Modifier.focusRequester(firstLink) else Modifier)) {
                            state.launch { state.playSelectedSource(selection,stream) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceLinkCard(stream: com.miflix.native2.model.StreamChoice,modifier: Modifier,onClick: () -> Unit) {
    var focused by remember(stream) { mutableStateOf(false) }
    val ink=if(focused) Color.Black else Color.White
    Column(modifier.onFocusChanged { focused=it.isFocused }.focusable().tvClick(onClick).clickable(onClick=onClick)
        .background(if(focused) Color.White else Color(0xFF252528),RoundedCornerShape(18.dp)).padding(20.dp),
        verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text(listOf(stream.addonName,stream.name).filter { it.isNotBlank() }.distinct().joinToString(" · "),
            color=ink,fontSize=15.sp,lineHeight=20.sp,fontWeight=FontWeight.Bold)
        if(stream.title.isNotBlank()) Text(stream.title,color=ink,fontSize=14.sp,lineHeight=20.sp,maxLines=if(focused) Int.MAX_VALUE else 5,
            overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        if(stream.videoSize>0) {
            val gib=stream.videoSize/1073741824.0
            val weight=if(gib>=1) "%.2f GiB".format(java.util.Locale.US,gib) else "%.1f MiB".format(java.util.Locale.US,stream.videoSize/1048576.0)
            Text("${tr("Peso","Size")}: $weight",color=ink,fontSize=14.sp,lineHeight=19.sp)
        }
        if(stream.filename.isNotBlank() && !stream.title.contains(stream.filename)) Text(stream.filename,color=ink,fontSize=12.sp,lineHeight=18.sp,
            maxLines=if(focused) Int.MAX_VALUE else 2,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
    }
}
