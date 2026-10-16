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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.onSizeChanged
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
import kotlinx.coroutines.ensureActive

private val StreamingTiles = listOf(
    CollectionTile("natgeo", "National Geographic", ""),
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
    ),
    CollectionTile("hulu", "Hulu",
        "https://cdn.jsdelivr.net/gh/luckynumb3rs/stremio-perfect-setup/collections/streaming/cover/hulu.png",
        "https://cdn.jsdelivr.net/gh/luckynumb3rs/stremio-perfect-setup/collections/streaming/focused/hulu.gif"),
    CollectionTile("crunchyroll", "Crunchyroll",
        "https://cdn.jsdelivr.net/gh/luckynumb3rs/stremio-perfect-setup/collections/streaming/cover/crunchyroll.png",
        "https://cdn.jsdelivr.net/gh/luckynumb3rs/stremio-perfect-setup/collections/streaming/focused/crunchyroll.gif")
)

@Composable
internal fun AppShell(state: AppState, screen: Screen, onNavigate: (Screen) -> Unit, content: @Composable BoxScope.() -> Unit) {
    val contentFocus=remember { FocusRequester() }
    Column(Modifier.fillMaxSize().background(Bg)) {
        Sidebar(screen, state.activeProfile.name, onNavigate, state.unreadNoticeCount, state.activeProfile,
            onMoveDown={ contentFocus.requestFocus(); Unit },onEditPicture={ state.profilePhotoEditor=state.activeProfile },kids=state.kidsMode)
        Box(Modifier.weight(1f).fillMaxWidth().padding(bottom=24.dp).focusRequester(contentFocus).focusGroup(), content = content)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(state: AppState,onNavigate: (Screen) -> Unit) {
    val genreScroll=rememberLazyListState()
    val firstGenre=remember {FocusRequester()}
    val scroll=rememberLazyListState()
    LaunchedEffect(state.activeProfile.id,state.interfaceLanguage) {if(state.trending.isEmpty()) state.loadHome()}
    val heroFocus=remember { FocusRequester() }
    val contentFocus=remember { FocusRequester() }
    val scope=rememberCoroutineScope()
    val density=androidx.compose.ui.platform.LocalDensity.current
    val railThreshold=with(density) { 180.dp.toPx() }
    val focusInset=with(density) { 12.dp.toPx() }
    val headerShade by remember { derivedStateOf { if(scroll.firstVisibleItemIndex>0) .88f else (scroll.firstVisibleItemScrollOffset/320f).coerceIn(0f,.88f) } }
    LaunchedEffect(state.interfaceLanguage) { delay(550); state.loadHomeExtras(); runCatching { state.loadComingSoon() } }
    LaunchedEffect(Unit) { state.loadDiscoveryArt() }
    // Refresh the calendar policy on foreground entry; no periodic catalog polling.
    LaunchedEffect(state.appForeground,state.interfaceLanguage) {
        if(state.appForeground) state.loadSeasonalProgramming()
    }
    val openItem: (MediaSummary) -> Unit={ item -> state.launch { state.open(item) } }
    val openCollection: (CollectionTile) -> Unit={ tile -> state.launch { state.openCollection(tile.id,tile.title) } }
    val returnToTop: () -> Unit={ scope.launch { scroll.animateScrollToItem(0) }; Unit }
    Box(Modifier.fillMaxSize().background(Bg)) {
        BrowsingViewport(state,(state.trending+state.movies).firstOrNull {it.type=="movie"},heroHeight=312.dp,headerInset=84.dp,heroFocus=heroFocus,ranked=(state.trending+state.movies).filter {it.type=="movie"}.distinctBy {it.cloudId}.take(10),
            onRandom={if(!state.randomChoosing) state.launch {state.playRandomMovie()}}) {
        CompositionLocalProvider(LocalBringIntoViewSpec provides object : BringIntoViewSpec {
            override fun calculateScrollDistance(offset: Float,size: Float,containerSize: Float): Float = when {
                size>=railThreshold && size<=containerSize-2*focusInset -> offset-focusInset
                offset<0 -> offset
                offset+size>containerSize -> offset+size-containerSize
                else -> 0f
            }
        }) {
            LazyColumn(Modifier.fillMaxSize().padding(bottom=12.dp).smoothVerticalDpad { !scroll.isScrollInProgress }.focusRequester(contentFocus).focusGroup(),state=scroll,contentPadding=PaddingValues(bottom=32.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                if(state.continueWatching.isNotEmpty()) item(key="continue") {
                    SpotlightRail(title=tr("Seguir viendo","Continue Watching"),rows=state.continueWatching,landscape=true,
                        progressFor={ state.progress[it.cloudId]?.percent },onLongClick={state.titleActions(it,true)},onClick=openItem)
                }
                item(key="streaming") { CollectionRail(tr("Plataformas","Streaming"),StreamingTiles,openCollection) }
                item(key="genres_home") { Column {
                    Text(tr("Explora por género","Browse by genre"),color=Color.White,fontSize=24.sp,lineHeight=29.sp,fontWeight=FontWeight.Bold,
                        modifier=Modifier.padding(start=40.dp,end=40.dp,top=16.dp,bottom=16.dp))
                    LazyRow(modifier=Modifier.fillMaxWidth().padding(horizontal=32.dp).clipToBounds().smoothHorizontalDpad().firstCardOnDown(genreScroll,firstGenre),state=genreScroll,contentPadding=PaddingValues(horizontal=8.dp,vertical=8.dp),horizontalArrangement=Arrangement.spacedBy(18.dp)) {
                        items(state.genres,key={it.id}) { genre -> GenreCard(genre.copy(coverUrl=state.genreCovers[genre.id] ?: genre.coverUrl),modifier=if(genre.id==state.genres.firstOrNull()?.id) Modifier.focusRequester(firstGenre) else Modifier) { state.launch { state.openGenre(genre) } } }
                    }
                    FocusButton(tr("Todas las colecciones →","All collections →"),modifier=Modifier.padding(start=40.dp,top=12.dp)) { onNavigate(Screen.COLLECTIONS) }
                    Spacer(Modifier.height(24.dp))
                } }
                if(state.forYou.isNotEmpty()) item(key="for_you") { SpotlightRail(title=tr("Para ti · tus puntuaciones","For you · Your ratings"),rows=state.forYou,onClick=openItem) }
                item(key="franchises_home") {
                    CollectionRail(tr("Franquicias","Franchises"),listOf(CollectionTile("franchise:marvel","Marvel",""),CollectionTile("franchise:dc","DC",""))) { tile ->
                        state.launch { state.openFranchise(tile.id.substringAfter(":")) }
                    }
                }
                if(state.seasonalItems.isNotEmpty()) item(key="seasonal") {
                    SpotlightRail(seasonalTitle(state.seasonalTheme),state.seasonalItems,onClick=openItem,
                        eyebrow=tr("SELECCIÓN DE TEMPORADA","SEASONAL SPOTLIGHT"))
                }
                item(key="movies") { SpotlightRail(title=tr("Películas populares","Popular · Movies"),rows=state.movies,onClick=openItem) }
                item(key="series") { SpotlightRail(title=tr("Series populares","Popular · Series"),rows=state.series,onClick=openItem) }
                item(key="top") { SpotlightRail(title=tr("Mejor valorados","Top Rated"),rows=state.topRated,onClick=openItem) }
                item(key="now_playing") { SpotlightRail(tr("Estrenos en cartelera","Now playing"),state.newMovies,onClick=openItem) }
                item(key="best_series") { SpotlightRail(tr("Series mejor valoradas","Top rated series"),state.bestSeries,onClick=openItem) }
                item(key="action") { SpotlightRail(tr("Acción para esta noche","Action tonight"),state.action,onClick=openItem) }
                item(key="scifi") { SpotlightRail(tr("Ciencia ficción","Science fiction"),state.sciFi,onClick=openItem) }
                item(key="comedy") { SpotlightRail(tr("Algo para reír","Time for a comedy"),state.comedy,onClick=openItem) }
                item(key="thrillers") { SpotlightRail(tr("Suspenso y misterio","Thrillers & mystery"),state.thrillers,onClick=openItem) }
                if(state.comingMovies.isNotEmpty() || state.comingSeries.isNotEmpty()) item(key="coming_soon_home") {
                    SpotlightRail(tr("Próximamente","Coming soon"),(state.comingMovies+state.comingSeries).distinctBy { it.cloudId }.take(30),onClick={ state.launch { state.open(it,upcoming=true) } })
                }
            }
        }
        } // fixed browse viewport
        Sidebar(Screen.HOME,state.activeProfile.name,onNavigate,state.unreadNoticeCount,state.activeProfile,
            backgroundAlpha=headerShade,onHeaderFocused=returnToTop,onEditPicture={ state.profilePhotoEditor=state.activeProfile },kids=state.kidsMode,
            onMoveDown=({ scope.launch {
                scroll.scrollToItem(0)
                androidx.compose.runtime.withFrameNanos { }
                if(state.trending.isNotEmpty()) heroFocus.requestFocus() else contentFocus.requestFocus()
            }; Unit }))
    }
}

@Composable
private fun DirectorRail(state: AppState) {
    Column {
    Text(tr("Directores","Directors"),color=Color.White,fontSize=24.sp,lineHeight=29.sp,fontWeight=FontWeight.Bold,
        modifier=Modifier.padding(start=34.dp,top=12.dp,bottom=16.dp))
    LazyRow(modifier=Modifier.fillMaxWidth().padding(horizontal=26.dp),contentPadding=PaddingValues(horizontal=8.dp,vertical=8.dp),horizontalArrangement=Arrangement.spacedBy(18.dp)) {
        items(state.directors,key={it.key}) { group ->
            FocusButton(if(group.key=="russo") tr("Hermanos Russo","Russo Brothers") else group.title) {
                state.launch { state.socialAction { state.openDirector(group) } }
            }
        }
    }
    Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun TopTenHero(rows: List<MediaSummary>, onOpen: (MediaSummary) -> Unit, onFocused: (Boolean) -> Unit = {}, onBackdrop: (String?) -> Unit = {},buttonModifier: Modifier = Modifier,randomChoosing: Boolean=false,onRandom: (() -> Unit)?=null) {
    if (rows.isEmpty()) {
        Box(Modifier.fillMaxWidth().height(160.dp).padding(horizontal=40.dp),contentAlignment=Alignment.CenterStart) {
            onRandom?.let { play -> FocusButton(tr("Reproducir aleatorio","Play random"),modifier=buttonModifier) { play() } }
        }
        return
    }
    var index by remember(rows.map { it.cloudId }) { mutableIntStateOf(0) }
    var heroFocused by remember { mutableStateOf(false) }
    val safeIndex = index.coerceIn(0, rows.lastIndex)
    val hero = rows[safeIndex]
    LaunchedEffect(hero.backdrop) { onBackdrop(hero.backdrop) }

    LaunchedEffect(rows.map { it.cloudId }) {
        while (true) {
            delay(6500)
            if(!heroFocused) index = (index + 1) % rows.size
        }
    }

    Box(Modifier.fillMaxWidth().height(290.dp)) {
        Crossfade(hero, animationSpec = tween(260, easing = LinearOutSlowInEasing), label = "topHero") { item ->
            AsyncImage(item.backdrop, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x08000000), Color(0x2C000000), Bg), startY = 120f)))
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xF8050505), Color(0xB0050505), Color.Transparent), endX = 1000f)))
        Column(Modifier.align(Alignment.CenterStart).padding(start=34.dp,end=24.dp).fillMaxWidth(.64f)) {
            Text(tr("TOP 10 · TENDENCIAS DE LA SEMANA","TOP 10  ·  TRENDING THIS WEEK"), color = Accent, fontSize = 12.sp, lineHeight=15.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Text(hero.title, color = Color.White, fontSize = 30.sp, lineHeight=35.sp, fontWeight = FontWeight.Black, maxLines = 2)
            Spacer(Modifier.height(10.dp))
            Text("${hero.year}  ·  ★ ${"%.1f".format(hero.rating)}", color = Color(0xFFE0E0E0), fontSize = 14.sp, lineHeight=17.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            Text(hero.overview, color = Color(0xFFE7E7EA), fontSize = 15.sp, lineHeight=18.sp, maxLines = 2, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(20.dp))
            FlowRow(horizontalArrangement=Arrangement.spacedBy(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                FocusButton(tr("Ver detalles","View Details"),primary=true,onFocused={ heroFocused=it; onFocused(it) },modifier=buttonModifier) { onOpen(hero) }
                onRandom?.let { play -> FocusButton(if(randomChoosing) tr("Eligiendo…","Choosing…") else tr("Reproducir aleatorio","Play random"),onFocused={ heroFocused=it; onFocused(it) }) { play() } }
            }
        }
        Text("#${safeIndex+1}",color=Accent.copy(alpha=.8f),fontSize=104.sp,fontWeight=FontWeight.Black,modifier=Modifier.align(Alignment.CenterEnd).padding(end=42.dp))
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
    compact: Boolean = false,
    progressFor: (MediaSummary) -> Double? = { null },
    onLongClick: ((MediaSummary) -> Unit)? = null,
    onClick: (MediaSummary) -> Unit
) {
    SpotlightRail(title,rows,landscape,progressFor,onLongClick=onLongClick,onClick=onClick)
}

@Composable
private fun CollectionRail(title: String, rows: List<CollectionTile>, onClick: (CollectionTile) -> Unit) {
    val railScroll=rememberLazyListState()
    val firstCard=remember {FocusRequester()}
    Column(Modifier.fillMaxWidth().height(228.dp)) {
        Text(title, color = Color.White, fontSize = 22.sp, lineHeight=26.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 40.dp,end=40.dp, bottom = 8.dp))
        LazyRow(
            modifier=Modifier.fillMaxWidth().padding(horizontal=32.dp).clipToBounds().smoothHorizontalDpad().firstCardOnDown(railScroll,firstCard),
            state=railScroll,
            contentPadding = PaddingValues(horizontal=8.dp,vertical=8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(rows, key = { it.id }) { tile ->
                Box(if(tile.id==rows.firstOrNull()?.id) Modifier.focusRequester(firstCard) else Modifier) {
                    CollectionCard(tile) { onClick(tile) }
                }
            }
        }
    }
}

@Composable
fun CatalogScreen(state: AppState, title: String, catalog: List<MediaSummary>, onBack: () -> Unit, footer: (@Composable () -> Unit)? = null, header: (@Composable () -> Unit)? = null) {
    BackHandler(onBack = onBack)
    val catalogScroll=androidx.compose.runtime.saveable.rememberSaveable(title,saver=androidx.compose.foundation.lazy.grid.LazyGridState.Saver) { androidx.compose.foundation.lazy.grid.LazyGridState() }
    var previousCount by remember(title) {mutableIntStateOf(catalog.size)}
    var newBatchStart by remember(title) {mutableIntStateOf(-1)}
    val newBatchFocus=remember {FocusRequester()}
    LaunchedEffect(catalog.size,title) {
        val before=previousCount
        previousCount=catalog.size
        if(state.screen==Screen.GENRE_DETAIL && before>0 && catalog.size>before) {
            newBatchStart=before
            catalogScroll.scrollToItem(before)
            withFrameNanos { }
            runCatching {newBatchFocus.requestFocus()}
        }
    }
    AppShell(state, state.screen, { state.screen = it }) {
        BrowsingViewport(state,catalog.firstOrNull(),heroHeight=152.dp) {
        Column(Modifier.fillMaxSize().padding(start = 34.dp, top = 8.dp, end = 34.dp)) {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Text(title,color=Color.White,fontSize=24.sp,lineHeight=29.sp,fontWeight=FontWeight.Black,modifier=Modifier.weight(1f))
                Text("${catalog.size} "+tr("cargados","loaded"),color=Muted,fontSize=12.sp)
            }
            header?.invoke()
            Spacer(Modifier.height(8.dp))
            LazyVerticalGrid(columns=GridCells.Adaptive(220.dp),state=catalogScroll,modifier=Modifier.weight(1f).fillMaxWidth().smoothVerticalDpad(),
                horizontalArrangement=Arrangement.spacedBy(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(bottom=48.dp,top=8.dp)) {
                gridItems(catalog,key={it.cloudId}) { item -> MediaCard(item,landscape=true,compact=true,modifier=Modifier.fillMaxWidth()
                    .then(if(item.cloudId==catalog.getOrNull(newBatchStart)?.cloudId) Modifier.focusRequester(newBatchFocus) else Modifier),progressPercent=state.progress[item.cloudId]?.percent,onLongClick={state.titleActions(item)}) { state.launch { state.open(item) } } }
                if(footer!=null) item(span={ androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) { footer() }
            }
        }
        } // fixed browse viewport
    }
}

@Composable
fun SearchScreen(state: AppState, onBack: () -> Unit) = CinematicSearchScreen(state,onBack)

@Composable
fun CollectionsScreen(state: AppState, onBack: () -> Unit) {
    val genreScroll=rememberLazyListState()
    val firstGenre=remember {FocusRequester()}
    BackHandler(onBack = onBack)
    LaunchedEffect(Unit) { state.loadDiscoveryArt(); state.loadHomeExtras(); runCatching { state.loadComingSoon() }.onFailure { state.error=it.message } }
    val years = (java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) downTo 1990).map { year -> CollectionTile("year:$year", year.toString(), "") }
    AppShell(state, Screen.COLLECTIONS, { state.screen = it }) {
        BrowsingViewport(state,state.comingMovies.firstOrNull() ?: state.movies.firstOrNull(),heroHeight=190.dp) {
        LazyColumn(Modifier.fillMaxSize().smoothVerticalDpad(), contentPadding = PaddingValues(top = 34.dp, bottom = 60.dp)) {
            item {
                Column(Modifier.padding(horizontal = 34.dp)) {
                    Text(tr("Colecciones","Collections"), color = Color.White, fontSize = 32.sp, lineHeight=38.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(5.dp))
                    Text(tr("Plataformas, géneros y películas por año.","Streaming services, genres and movies by year."), color = Muted, fontSize = 14.sp, lineHeight=17.sp)
                    Spacer(Modifier.height(16.dp))
                    FocusButton(tr("Filtro personalizado","Custom filter")) { state.screen=Screen.CUSTOM_FILTER }
                    Spacer(Modifier.height(24.dp))
                }
            }
            item {
                Text(tr("Franquicias","Franchises"),color=Color.White,fontSize=24.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(start=34.dp,bottom=12.dp))
                LazyRow(Modifier.fillMaxWidth(),contentPadding=PaddingValues(horizontal=28.dp,vertical=8.dp),horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                    items(listOf("marvel" to "Marvel","dc" to "DC")) { (brand,label) ->
                        CollectionCard(CollectionTile("franchise:$brand",label,"")) { state.launch { state.openFranchise(brand) } }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
            item { Column {
                Text(tr("Géneros","Genres"),color=Color.White,fontSize=24.sp,lineHeight=29.sp,fontWeight=FontWeight.Bold,
                    modifier=Modifier.padding(start=34.dp,bottom=16.dp))
                LazyRow(modifier=Modifier.fillMaxWidth().padding(horizontal=26.dp).firstCardOnDown(genreScroll,firstGenre),state=genreScroll,contentPadding=PaddingValues(horizontal=8.dp,vertical=8.dp),horizontalArrangement=Arrangement.spacedBy(18.dp)) {
                    items(state.genres,key={it.id}) { genre -> GenreCard(genre.copy(coverUrl=state.genreCovers[genre.id] ?: genre.coverUrl),modifier=if(genre.id==state.genres.firstOrNull()?.id) Modifier.focusRequester(firstGenre) else Modifier) { state.launch { state.openGenre(genre) } } }
                }
                Spacer(Modifier.height(24.dp))
            } }
            if(state.comingMovies.isNotEmpty()) item { MediaRail(tr("Próximamente · Películas","Coming Soon · Movies"),state.comingMovies,onClick={ state.launch { state.open(it,upcoming=true) } }) }
            if(state.comingSeries.isNotEmpty()) item { MediaRail(tr("Próximamente · Series","Coming Soon · Series"),state.comingSeries,onClick={ state.launch { state.open(it,upcoming=true) } }) }
            item { DirectorRail(state) }
            item { CollectionRail(tr("Plataformas","Streaming"), StreamingTiles) { tile -> state.launch { state.openCollection(tile.id, tile.title) } } }
            listOf("series" to tr("Series por año","Series by Year"),"documentary" to tr("Documentales por año","Documentaries by Year")).forEach { (category,label) ->
                item {
                    Text(label,color=Color.White,fontSize=22.sp,modifier=Modifier.padding(start=34.dp,top=8.dp,bottom=10.dp))
                    LazyRow(Modifier.fillMaxWidth().padding(horizontal=26.dp),contentPadding=PaddingValues(horizontal=8.dp,vertical=8.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                        items(years,key={it.id}) { tile -> FocusButton(tile.title,modifier=Modifier.width(106.dp)) { state.launch { state.openYear(tile.title.toInt(),category) } } }
                    }
                    Spacer(Modifier.height(28.dp))
                }
            }
            item {
                Text(tr("Películas por año","Movies by Year"), color = Color.White, fontSize = 22.sp, lineHeight=26.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 34.dp, top = 8.dp, bottom = 10.dp))
                LazyRow(modifier=Modifier.fillMaxWidth().padding(horizontal=26.dp),contentPadding = PaddingValues(horizontal=8.dp,vertical=8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(years, key = { it.id }) { tile ->
                        FocusButton(tile.title, modifier = Modifier.width(106.dp)) { state.launch { state.openCollection(tile.id, tile.title) } }
                    }
                }
                Spacer(Modifier.height(28.dp))
            }
        }
        } // fixed browse viewport
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
private fun GenreCard(g: GenreDefinition,modifier: Modifier=Modifier,onClick: () -> Unit) {
    var focused by remember(g.id) { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.012f else 1f, tween(80), label = "genreScale")
    Column(modifier.width(BrowseTileSlotWidth)) {
        Box(Modifier.width(BrowseTileSlotWidth).height(BrowseTileSlotHeight),contentAlignment=Alignment.Center) {
            Box(Modifier.width(BrowseTileWidth).height(BrowseTileHeight).graphicsLayer { scaleX=scale;scaleY=scale }
                .onFocusChanged { focused=it.isFocused }.focusable().tvClick(onClick).clickable(onClick=onClick)
                .clip(RoundedCornerShape(14.dp)).background(Panel)
                .border(if(focused) 2.dp else 1.dp,if(focused) Accent else PanelBorder,RoundedCornerShape(14.dp))) {
                if(g.coverUrl.isNotBlank()) AsyncImage(g.coverUrl,g.title,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
                else Text(g.title,color=Color.White,fontSize=21.sp,fontWeight=FontWeight.Bold,modifier=Modifier.align(Alignment.Center))
            }
        }
        Text(g.title,color=Color.White,fontSize=14.sp,lineHeight=17.sp,fontWeight=FontWeight.Medium,modifier=Modifier.padding(start=8.dp,top=2.dp))
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
    var showMore by remember(item.cloudId) { mutableStateOf(false) }
    var showRecommend by remember(item.cloudId) { mutableStateOf(false) }
    var showRating by remember(item.cloudId) { mutableStateOf(false) }
    var episodeChoice by remember(item.cloudId) { mutableStateOf<EpisodeSummary?>(null) }
    var showQueueAdd by remember(item.cloudId) { mutableStateOf(false) }
    var showQueue by remember { mutableStateOf(false) }
    val detailScroll=rememberLazyListState()
    val playFocus = remember(item.cloudId) { FocusRequester() }
    val entrance=remember(item.cloudId) { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(item.cloudId) {
        withFrameNanos { }
        runCatching { playFocus.requestFocus() }
        detailScroll.scrollToItem(0)
        entrance.animateTo(1f,tween(120,easing=LinearOutSlowInEasing))
    }
    BackHandler(onBack = onBack)
    if(showRecommend) RecommendTitleDialog(state,item) { showRecommend=false }
    if(showRating) RatingDialog(state,item) { showRating=false }
    episodeChoice?.let { ep ->
        Dialog(onDismissRequest={ episodeChoice=null }) {
            val first=remember {FocusRequester()}
            LaunchedEffect(Unit) {withFrameNanos {};runCatching {first.requestFocus()} }
            Column(Modifier.consumeOpeningPress().focusRequester(first).focusGroup().width(530.dp).heightIn(max=470.dp).background(Panel,RoundedCornerShape(24.dp)).padding(24.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(16.dp)) {
                var reveal by remember(item.cloudId,ep.season,ep.episode) { mutableStateOf(false) }
                val hidden=!state.episodeWatched(item,ep) && !reveal
                Text("S${ep.season} E${ep.episode} · ${if(hidden) tr("No visto","Not watched") else ep.title}",color=Color.White,fontSize=22.sp)
                if(hidden) FocusButton(tr("Mostrar detalles (spoilers)","Reveal details (spoilers)")) { reveal=true }
                if(reveal && ep.overview.isNotBlank()) Text(ep.overview,color=Muted,fontSize=15.sp,maxLines=5)
                val resume=state.progress["${item.cloudId}:s${ep.season}e${ep.episode}"]
                if((resume?.position ?: 0L)>0 && (resume?.percent ?: 0.0)<96) FocusButton(tr("Continuar viendo","Continue watching")) { episodeChoice=null;state.launch {state.socialAction {state.playEpisode(ep)} } }
                FocusButton(tr("Desde el principio","Play from beginning")) { episodeChoice=null;state.launch {state.socialAction {state.playEpisode(ep,true)} } }
                FocusButton(tr("Añadir a cola","Add to queue")) { state.enqueue(item,ep.season,ep.episode);episodeChoice=null }
                FocusButton(tr("Elegir enlace","Play manually")) { episodeChoice=null;state.launch {state.chooseEpisodeSources(ep)} }
                FocusButton(tr("Marcar como visto","Mark as watched")) { state.markWatched(item,ep.season,ep.episode);episodeChoice=null }
                FocusButton(tr("Cancelar","Cancel")) { episodeChoice=null }
            }
        }
    }
    if(showQueueAdd) QueueAddDialog(state,item) { showQueueAdd=false }
    if(showQueue) QueueDialog(state) { showQueue=false }

    if(showMore) Dialog(onDismissRequest={ showMore=false }) {
        val moreFocus=remember { FocusRequester() }
        LaunchedEffect(Unit) { withFrameNanos { };runCatching { moreFocus.requestFocus() } }
        Column(Modifier.consumeOpeningPress().widthIn(max=520.dp).heightIn(max=480.dp).background(Panel,RoundedCornerShape(24.dp)).padding(24.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text(tr("Más opciones","More options"),color=Color.White,fontSize=24.sp)
            if(!state.selectedUpcoming) FocusButton(tr("Desde el principio","Play from beginning"),modifier=Modifier.focusRequester(moreFocus)) { showMore=false;state.launch { if(item.type=="movie") state.playMovie(true) else state.playSeriesFromStart() } }
            FocusButton(tr("Añadir a cola","Add to queue"),modifier=if(state.selectedUpcoming) Modifier.focusRequester(moreFocus) else Modifier) { showMore=false;if(item.type=="series") showQueueAdd=true else state.enqueue(item) }
            FocusButton(tr("Ver cola","View queue")+" · ${state.queueEntries.size}") { showMore=false;showQueue=true }
            FocusButton(tr("Recomendar a amigos","Recommend to friends")) { showMore=false;showRecommend=true }
            if(!state.selectedUpcoming) {
                FocusButton(tr("Elegir enlace","Play manually")) { showMore=false;state.launch {state.chooseCurrentSources()} }
                FocusButton(tr("Marcar como visto","Mark as watched")) { showMore=false;state.markWatched(item) }
            }
            FocusButton(tr("Cerrar","Close")) { showMore=false }
        }
    }

    AppShell(state,state.detailsReturnScreen,{ state.screen=it }) {
    Box(Modifier.fillMaxSize().background(Bg).clipToBounds().graphicsLayer {
        alpha=.65f+.35f*entrance.value
        translationY=(1f-entrance.value)*6.dp.toPx()
    }) {
        AsyncImage(item.backdrop, null, Modifier.fillMaxWidth().height(430.dp).align(Alignment.TopCenter), contentScale = ContentScale.Crop)
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
            contentPadding = PaddingValues(start = 34.dp, end = 34.dp, top = 28.dp, bottom = 70.dp)
        ) {
            item(key = "hero") {
                Column(Modifier.fillMaxWidth().heightIn(min = 300.dp)) {
                    BackIconButton(onClick = onBack)
                    Spacer(Modifier.height(14.dp))
                    Text(item.title, color = Color.White, fontSize = 40.sp, lineHeight=46.sp, fontWeight = FontWeight.Black, maxLines = 2, overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    Spacer(Modifier.height(8.dp))
                    Text("${item.year}   ★ ${"%.1f".format(item.rating)}   ${details?.runtimeText.orEmpty()}", color = Color(0xFFD6D6D8), fontSize = 15.sp, lineHeight=18.sp)
                    if (!details?.genres.isNullOrEmpty()) {
                        Spacer(Modifier.height(7.dp))
                        Text(details?.genres?.take(4)?.joinToString("  ·  ").orEmpty(), color = Color(0xFFB9B9BD), fontSize = 13.sp, lineHeight=16.sp)
                    }
                    if(state.selectedUpcoming && !details?.releaseDate.isNullOrBlank()) {
                        Text(tr("Próximamente","Coming soon")+" · ${details?.releaseDate}",color=Accent,fontSize=14.sp,modifier=Modifier.padding(top=8.dp))
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(item.overview, color = Color(0xFFE8E8EB), fontSize = 16.sp, lineHeight=22.sp, maxLines = 3, modifier = Modifier.fillMaxWidth(.72f))
                    Spacer(Modifier.height(24.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement=Arrangement.spacedBy(12.dp)) {
                        if(state.selectedUpcoming) {
                            val watching=state.favorites.contains(item.cloudId)
                            FocusButton(if(watching) tr("✓ Te avisaremos","✓ Reminder set") else tr("Avisarme","Notify me"),primary=true,
                                modifier=Modifier.focusRequester(playFocus).width(220.dp)) {
                                state.remindMe(item);state.launch { runCatching { state.pushCloud() } }
                            }
                        } else if (item.type == "movie") {
                            val label = if ((saved?.percent ?: 0.0) in 1.0..95.0) "▶  ${tr("Reanudar","Resume")} ${formatPosition(saved?.position ?: 0)}" else tr("▶  Reproducir","▶  Play")
                            FocusButton(label, onLongClick = { showMore=true }, primary = true, modifier = Modifier.focusRequester(playFocus).width(220.dp)) {
                                state.launch { runCatching { state.playMovie() }.onFailure { state.error = it.message } }
                            }
                        } else {
                            val canResume = (saved?.season ?: 0) > 0 && (saved?.episode ?: 0) > 0 && (saved?.percent ?: 0.0) < 96.0
                            FocusButton(if (canResume) "▶  ${tr("Continuar","Continue")} S${saved?.season} E${saved?.episode}" else tr("▶  Reproducir S1 E1","▶  Play S1 E1"), onLongClick = { showMore=true }, primary = true, modifier = Modifier.focusRequester(playFocus).width(220.dp)) {
                                state.launch { runCatching { if (canResume) state.resumeSeries() else state.playSeriesFromStart() }.onFailure { state.error = it.message } }
                            }
                        }
                        FocusButton(if (state.favorites.contains(item.cloudId)) tr("✓ Mi lista","✓ My List") else tr("+ Mi lista","+ My List")) {
                            state.toggleFavorite(item); state.launch { runCatching { state.pushCloud() } }
                        }
                        if(item.type=="series" && !state.selectedUpcoming) FocusButton(if(state.randomEpisodeBusy) tr("Eligiendo…","Choosing…") else tr("🎲 Episodio aleatorio","🎲 Random episode")) {
                            state.launch {state.playRandomEpisode(item)}
                        }
                        if(!state.kidsMode) FocusButton(tr("Sala compartida","Watch Party")) { state.screen = Screen.WATCH_PARTY }
                        if(!state.kidsMode) FocusButton("★ "+(state.ratings[item.cloudId]?.let {"$it/10"} ?: tr("Puntuar","Rate"))) {showRating=true}
                    }
                    Spacer(Modifier.height(38.dp))
                }
            }

            if (item.type == "series") {
                item(key = "episodes") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(tr("Episodios","Episodes"), color = Color.White, fontSize = 25.sp, lineHeight=30.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(16.dp))
                        if ((details?.seasonCount ?: 0) > 1) {
                            SeasonSelector(state, details?.seasonCount ?: 0)

                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    LazyRow(modifier = Modifier.fillMaxWidth().height(236.dp), contentPadding = PaddingValues(end = 54.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(state.episodes, key = { "${it.season}:${it.episode}" }) { ep ->
                            EpisodeCard(ep, hidden=!state.episodeWatched(item,ep), onLongClick = { episodeChoice=ep }) { episodeChoice=ep }
                        }
                    }
                    Spacer(Modifier.height(28.dp))
                }
            }

            if(!state.kidsMode) item(key="cast") {
                Text(tr("Reparto","Cast"),color=Color.White,fontSize=24.sp,fontWeight=FontWeight.Bold)
                Spacer(Modifier.height(14.dp))
                LazyRow(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(16.dp),contentPadding=PaddingValues(end=24.dp)) {
                    items(details?.cast.orEmpty(),key={"${it.id}:${it.name}"}) { CastCard(it,state) }
                }
                Spacer(Modifier.height(28.dp))
            }
            if(!state.kidsMode) item(key="reviews") { TraktCommentsSection(state,item);Spacer(Modifier.height(28.dp)) }

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
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SettingsScreen(state: AppState, onBack: () -> Unit) {
    var category by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("account") }
    var helpTitle by remember(category) { mutableStateOf(SettingsCategories.first { it.id==category }.english) }
    var helpDescription by remember(category) { mutableStateOf("Select a setting to see its details. Changes are saved on this device.") }
    var helpIndex by remember(category) { mutableStateOf(-1) }
    val settingsScroll=rememberLazyListState()
    LaunchedEffect(category) { settingsScroll.scrollToItem(0) }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var addonUrl by remember { mutableStateOf("") }
    var showUpdates by remember { mutableStateOf(false) }
    if(showUpdates) AppUpdateDialog(state) { showUpdates=false }
    val context = LocalContext.current
    var showUsage by remember { mutableStateOf(false) }
    var usageMs by remember { mutableStateOf(com.miflix.native2.data.AppUsageTracker.totalMs(context)) }
    LaunchedEffect(context) {
        while(true) {
            usageMs = com.miflix.native2.data.AppUsageTracker.totalMs(context)
            delay(5000)
        }
    }
    val usageHours = usageMs / 3_600_000L
    val usageMinutes = (usageMs / 60_000L) % 60L
    val usageLabel = "$usageHours h $usageMinutes min"
    if(showUsage) {
        val closeFocus = remember { FocusRequester() }
        LaunchedEffect(Unit) { withFrameNanos { }; runCatching { closeFocus.requestFocus() } }
        Dialog(onDismissRequest={ showUsage=false }) {
            Column(Modifier.widthIn(max=560.dp).background(Panel,RoundedCornerShape(20.dp)).padding(26.dp),
                verticalArrangement=Arrangement.spacedBy(16.dp)) {
                Text(tr("Tiempo de uso de BruniO", "BruniO usage time"),color=Color.White,fontSize=24.sp)
                Text(usageLabel,color=Color.White,fontSize=34.sp,fontWeight=FontWeight.Bold)
                Text(tr("Total en este dispositivo, sumando todos los perfiles. Incluye navegación y reproducción mientras la app está visible.",
                    "Total on this device, across all profiles. Includes browsing and playback while the app is visible."),color=Muted,fontSize=16.sp)
                Text(tr("Se cuenta desde esta actualización. Se conserva al cerrar y actualizar la app.",
                    "Tracking starts with this update. Saved when closing and updating the app."),color=Muted,fontSize=14.sp)
                FocusButton(tr("Cerrar", "Close"),modifier=Modifier.focusRequester(closeFocus)) { showUsage=false }
            }
        }
    }
    BackHandler(onBack = onBack)

    val helpValue = when(helpIndex) {
        0 -> "BruniO ${BuildConfig.VERSION_NAME}"
        1 -> if(state.interfaceLanguage=="en") "English" else "Español"
        2 -> state.session?.email ?: tr("Sin sesión","Not signed in")
        3 -> state.audioLanguage.ifBlank { tr("Automático","Automatic") }
        4 -> if(state.streamSizeLimitGb==0.0) tr("Sin límite","Unlimited") else "${state.streamSizeLimitGb} GB"
        5 -> state.subtitleLanguage
        6 -> "${(state.subtitleScale*100).toInt()}%"
        7 -> if(state.autoplayNext) "ON" else "OFF"
        8 -> "${state.addonManifests.size} "+tr("configurados","configured")
        9 -> tr("Configuración de cuenta","Account configuration")
        10 -> usageLabel
        11 -> BuildConfig.VERSION_NAME
        else -> ""
    }
    AppShell(state, Screen.SETTINGS, { state.screen = it }) {
        SocialBackdrop()
        CompositionLocalProvider(LocalBringIntoViewSpec provides object : BringIntoViewSpec {
            override fun calculateScrollDistance(offset: Float,size: Float,containerSize: Float): Float = when {
                size>containerSize-48 -> offset-24
                offset<24 -> offset-24
                offset+size>containerSize-24 -> offset+size-containerSize+24
                else -> 0f
            }
        }) {
        Column(Modifier.fillMaxSize().padding(horizontal=34.dp)) {
            Text(tr("Ajustes","Settings"),color=Color.White,fontSize=34.sp,lineHeight=41.sp,fontWeight=FontWeight.Black,
                modifier=Modifier.padding(top=24.dp))
            Text(tr("Tu experiencia, a tu manera.","Make BruniO yours."),color=Muted,fontSize=15.sp,modifier=Modifier.padding(top=4.dp,bottom=18.dp))
            LazyRow(horizontalArrangement=Arrangement.spacedBy(10.dp),contentPadding=PaddingValues(vertical=4.dp)) {
                items(SettingsCategories,key={it.id}) { tab ->
                    FocusButton(tr(tab.spanish,tab.english),primary=category==tab.id) { category=tab.id }
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(Modifier.weight(1f).fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(22.dp)) {
                LazyColumn(Modifier.weight(.7f).fillMaxHeight(),state=settingsScroll,
                    contentPadding=PaddingValues(top=4.dp,bottom=42.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
                    if(category=="system") item(key="setting-0") {
                        SettingsSectionCard(onFocused={ helpTitle="About BruniO";helpDescription="Usage on this device, installed version and updates.";helpIndex=0 }) {
                Text(tr("Acerca de BruniO","About BruniO"),color=Color.White,fontSize=22.sp,fontWeight=FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("BruniO ${BuildConfig.VERSION_NAME}", color = Muted, fontSize = 14.sp, lineHeight=17.sp)
                Spacer(Modifier.height(12.dp))
                FocusButton("Changelog / Details") {state.changelogOpen=true}
                Spacer(Modifier.height(10.dp))
                FocusButton("${tr("Tiempo de uso", "Usage time")} · $usageLabel") {
                    usageMs=com.miflix.native2.data.AppUsageTracker.totalMs(context)
                    showUsage=true
                }
                Spacer(Modifier.height(30.dp))


                        }
                    }
                    if(category=="language") item(key="setting-1") {
                        SettingsSectionCard(onFocused={ helpTitle="App language";helpDescription="Choose English or Spanish for menus and catalog information.";helpIndex=1 }) {
                Text(tr("Idioma de la app","App language"),color=Color.White,fontSize=22.sp, lineHeight=26.sp)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement=Arrangement.spacedBy(14.dp)) {
                    FocusButton("Español",primary=state.interfaceLanguage=="es") { state.launch { state.setInterfaceLanguage("es") } }
                    FocusButton("English",primary=state.interfaceLanguage=="en") { state.launch { state.setInterfaceLanguage("en") } }
                }

                        }
                    }
                    if(category=="account") item(key="setting-2") {
                        SettingsSectionCard(onFocused={ helpTitle="Account & Sync";helpDescription="Manage your account, device pairing and shared favorites.";helpIndex=2 }) {
                if (state.session == null) {
                    Text(tr("Cuenta y sincronización","Account & Sync"), color = Color.White, fontSize = 22.sp, lineHeight=26.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(tr("Usa tu correo y contraseña o escanea el QR seguro con tu celular.","Use email/password or scan the secure pairing QR with your phone."), color = Muted, fontSize = 14.sp, lineHeight=17.sp, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(16.dp))
                    NativeTextField(email, { email = it }, tr("Correo","Email"), modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(10.dp))
                    NativeTextField(password, { password = it }, tr("Contraseña","Password"), true, Modifier.fillMaxWidth())
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
                    }
                    if(category=="language") item(key="setting-3") {
                        SettingsSectionCard(onFocused={ helpTitle="Audio language";helpDescription="Preferred audio and playback links. Available tracks can also be changed in the player.";helpIndex=3 }) {
                Text(tr("Preferencias de reproducción","Playback preferences"),color=Color.White,fontSize=22.sp, lineHeight=26.sp)
                Text(tr("Auriculares: ajuste estéreo automático al conectarlos a la TV. La sincronización depende también de la TV y los auriculares.","Headphones: automatic stereo adjustment when connected to the TV. Sync also depends on your TV and headphones."),color=Muted,fontSize=14.sp)
                Text("Audio: ${state.audioLanguage.ifBlank { tr("Automático","Automatic") }}",color=Muted)
                if(com.miflix.native2.data.StreamLanguagePriority.prefersSpanish(state.audioLanguage)) {
                    Text(tr("Enlaces: español latino y de España primero; inglés como alternativa.",
                        "Links: Latin American and Spain Spanish first; English as fallback."),color=Muted,fontSize=14.sp)
                }
                LazyRow(horizontalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(vertical=8.dp)) { item { FlowRow(horizontalArrangement=Arrangement.spacedBy(18.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
                    listOf("" to tr("Automático","Auto"), "es" to "Español", "en" to "English", "pt" to "Português", "fr" to "Français").forEach { (code,label) ->
                        FocusButton(label,primary=state.audioLanguage==code) { state.savePlaybackPreferences(code,state.subtitleLanguage,state.autoplayNext) }
                    }
                } } }
                Spacer(Modifier.height(18.dp))

                        }
                    }
                    if(category=="playback") item(key="setting-4") {
                        SettingsSectionCard(onFocused={ helpTitle="Playback link size limit";helpDescription="Choose a maximum file size. A cap also hides links with unknown size.";helpIndex=4 }) {
                var customLimit by remember(state.streamSizeLimitGb) { mutableStateOf(if(state.streamSizeLimitGb>0) state.streamSizeLimitGb.toString() else "") }
                Text(tr("Límite de tamaño de enlaces","Playback link size limit"),color=Color.White,fontSize=22.sp,lineHeight=26.sp)
                Text(if(state.streamSizeLimitGb==0.0) tr("Sin límite","Unlimited") else "${tr("Máximo","Maximum")}: ${state.streamSizeLimitGb} GB",color=Muted)
                Spacer(Modifier.height(12.dp))
                FlowRow(horizontalArrangement=Arrangement.spacedBy(12.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    listOf(0.0,5.0,10.0,20.0,40.0,80.0).forEach { limit ->
                        FocusButton(if(limit==0.0) tr("Sin límite","Unlimited") else "${limit.toInt()} GB",primary=state.streamSizeLimitGb==limit) { state.saveStreamSizeLimit(limit) }
                    }
                }
                Spacer(Modifier.height(12.dp))
                FlowRow(horizontalArrangement=Arrangement.spacedBy(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    NativeTextField(customLimit,{ customLimit=it },tr("Límite personalizado en GB","Custom limit in GB"),modifier=Modifier.width(300.dp))
                    FocusButton(tr("Aplicar límite","Apply limit")) {
                        val number=customLimit.trim().replace(',','.').toDoubleOrNull()
                        if(number==null || !number.isFinite() || number !in 0.0..500.0) state.error=tr("Escribe un número entre 0 y 500 GB.","Enter a number from 0 to 500 GB.")
                        else state.saveStreamSizeLimit(number)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(tr("Con un límite activo se ocultan los enlaces sin tamaño conocido. Se aplica también a la reproducción rápida.","An active cap hides links with unknown size. It also applies to quick playback."),color=Muted,fontSize=14.sp,lineHeight=18.sp)

                        }
                    }
                    if(category=="language") item(key="setting-5") {
                        SettingsSectionCard(onFocused={ helpTitle="Subtitles";helpDescription="Choose the subtitle language used for playback.";helpIndex=5 }) {
                Text("${tr("Subtítulos","Subtitles")}: ${state.subtitleLanguage}",color=Muted)
                LazyRow(horizontalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(vertical=8.dp)) { item { FlowRow(horizontalArrangement=Arrangement.spacedBy(18.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
                    listOf("off" to tr("Desactivado","Off"), "es" to "Español", "en" to "English", "pt" to "Português", "fr" to "Français").forEach { (code,label) ->
                        FocusButton(label,primary=state.subtitleLanguage==code) { state.savePlaybackPreferences(state.audioLanguage,code,state.autoplayNext) }
                    }
                } } }
                Spacer(Modifier.height(18.dp))

                        }
                    }
                    if(category=="appearance") item(key="focus-theme") {
                        SettingsSectionCard {
                            Text(tr("Colores de la app","Color themes"),color=Color.White,fontSize=22.sp,fontWeight=FontWeight.Bold)
                            Text(tr("Elige el color del selector y los contornos.","Choose your focus and outline color."),color=Muted)
                            var customColor by remember {mutableStateOf("#B792FF")}
                            NativeTextField(customColor,{customColor=it.take(7)},tr("Color personalizado · #RRGGBB","Custom color · #RRGGBB"))
                            FocusButton(tr("Aplicar color","Apply color")) {
                                if(customColor.matches(Regex("#[0-9a-fA-F]{6}"))) state.saveFocusColor(android.graphics.Color.parseColor(customColor))
                                else state.error=tr("Usa # y seis dígitos hexadecimales.","Use # followed by six hexadecimal digits.")
                            }
                            Spacer(Modifier.height(12.dp))
                            FlowRow(horizontalArrangement=Arrangement.spacedBy(10.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                                listOf(0xFFFFFFFF.toInt() to "White",0xFFB792FF.toInt() to "Purple",0xFF65BFFF.toInt() to "Blue",0xFF66E6A0.toInt() to "Green",0xFFFFBB66.toInt() to "Amber",0xFFFF8FB6.toInt() to "Rose").forEach {(color,label) ->
                                    FocusButton("● $label",primary=state.focusColor==color) {state.saveFocusColor(color)}
                                }
                            }
                        }
                    }
                    if(category=="account") item(key="remember-profile") {
                        SettingsSectionCard {
                            FocusButton(tr("Recordar último perfil","Remember last profile")+": "+if(state.rememberLastProfile) "ON" else "OFF") {state.setRememberProfile(!state.rememberLastProfile)}
                        }
                    }
                    if(category=="appearance") item(key="setting-6") {
                        SettingsSectionCard(onFocused={ helpTitle="Subtitle style";helpDescription="Set subtitle color and size. Preview the result before you play.";helpIndex=6 }) {
                Text(tr("Estilo de subtítulos","Subtitle style"),color=Color.White,fontSize=22.sp,lineHeight=26.sp,fontWeight=FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Text(tr("Sin fondo negro. El contorno mantiene el texto legible.","No black background. An outline keeps text readable."),color=Muted)
                Spacer(Modifier.height(14.dp))
                Text(tr("Color","Color"),color=Muted)
                FlowRow(horizontalArrangement=Arrangement.spacedBy(14.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    listOf(android.graphics.Color.WHITE to tr("Blanco","White"),android.graphics.Color.YELLOW to tr("Amarillo","Yellow"),android.graphics.Color.CYAN to tr("Cian","Cyan"),0xFFFFDAB9.toInt() to tr("Crema","Cream"),0xFF90EE90.toInt() to tr("Verde","Green")).forEach { (color,name) ->
                        FocusButton(name,primary=state.subtitleColor==color) { state.saveSubtitleStyle(color,state.subtitleScale) }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(tr("Tamaño","Size"),color=Muted)
                FlowRow(horizontalArrangement=Arrangement.spacedBy(14.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    listOf(0.8f to tr("Pequeño","Small"),1f to tr("Normal","Normal"),1.2f to tr("Grande","Large"),1.4f to tr("Muy grande","Extra large")).forEach { (scale,name) ->
                        FocusButton(name,primary=state.subtitleScale==scale) { state.saveSubtitleStyle(state.subtitleColor,scale) }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text(tr("Así se verán tus subtítulos","Your subtitles will look like this"),color=Color(state.subtitleColor),fontSize=(24*state.subtitleScale).sp,lineHeight=(30*state.subtitleScale).sp)
                Spacer(Modifier.height(20.dp))
                FocusButton(tr("Restaurar estilo","Reset style")) { state.saveSubtitleStyle(android.graphics.Color.WHITE,1f) }

                        }
                    }
                    if(category=="playback") item(key="setting-7") {
                        SettingsSectionCard(onFocused={ helpTitle="Autoplay next episode";helpDescription="Continue automatically when the current episode finishes.";helpIndex=7 }) {
                FocusButton("${tr("Siguiente episodio automático","Autoplay next episode")}: ${if(state.autoplayNext) tr("Activado","ON") else tr("Desactivado","OFF")}") { state.savePlaybackPreferences(state.audioLanguage,state.subtitleLanguage,!state.autoplayNext) }
                Spacer(Modifier.height(24.dp))

                        }
                    }
                    if(category=="sources") item(key="setting-8") {
                        SettingsSectionCard(onFocused={ helpTitle="Stream Add-ons";helpDescription="Manage the stream manifests shared by every profile on your account.";helpIndex=8 }) {
                Text(tr("Complementos de reproducción","Stream Add-ons"), color = Color.White, fontSize = 22.sp, lineHeight=26.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(tr("Los manifiestos de Torrentio y Comet se comparten entre los perfiles de esta cuenta.","Torrentio and Comet manifests are account-level, so every profile on this account shares them."), color = Muted, fontSize = 14.sp, lineHeight=17.sp, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                state.addonManifests.toList().forEach { manifest ->
                    Row(Modifier.fillMaxWidth().padding(vertical=6.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                        Text("• ${runCatching { Uri.parse(manifest).host }.getOrNull() ?: tr("Complemento configurado","Configured add-on")}",color=Color.White,fontSize=14.sp,modifier=Modifier.weight(1f))
                        FocusButton(tr("Eliminar","Remove"),destructive=true) { state.launch { state.socialAction { state.removeAddonManifest(manifest) } } }
                    }
                }
                Spacer(Modifier.height(12.dp))
                NativeTextField(addonUrl, { addonUrl = it }, tr("URL del manifiesto Torrentio / Comet","Torrentio / Comet manifest URL"), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(18.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
                    FocusButton(tr("Añadir manifiesto","Add Manifest")) { state.launch { state.socialAction { state.addAddonManifest(addonUrl); addonUrl = "" } } }
                    FocusButton(tr("Usar QR","Use QR")) { state.screen = Screen.PAIR_DEVICE }
                }

                Spacer(Modifier.height(34.dp))

                        }
                    }
                    if(category=="sources") item(key="setting-9") {
                        SettingsSectionCard(onFocused={ helpTitle="Native Sources";helpDescription="Configure the native sources available for your account.";helpIndex=9 }) {
                NativeSourcesSettings(state)

                        }
                    }
                    if(category=="system") item(key="setting-10") {
                        SettingsSectionCard(onFocused={ helpTitle="Storage";helpDescription="Clear temporary artwork without changing your account or favorites.";helpIndex=10 }) {
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
                    }
                    if(category=="system") item(key="setting-11") {
                        SettingsSectionCard(onFocused={ helpTitle="Updates";helpDescription="Check for a newer BruniO version.";helpIndex=11 }) {
                Text(tr("Actualizaciones","Updates"),color=Color.White,fontSize=22.sp)
                Spacer(Modifier.height(12.dp))
                Text("BruniO ${BuildConfig.VERSION_NAME}",color=Muted)
                Spacer(Modifier.height(12.dp))
                FocusButton(tr("Buscar actualizaciones","Check for Updates")) { showUpdates=true }

                        }
                    }
                }
                Column(Modifier.weight(.3f).fillMaxHeight().background(Panel,RoundedCornerShape(16.dp)).border(1.dp,PanelBorder,RoundedCornerShape(16.dp)).padding(16.dp),
                    verticalArrangement=Arrangement.spacedBy(10.dp)) {
                    Text(helpTitle,color=Color.White,fontSize=20.sp,lineHeight=24.sp,fontWeight=FontWeight.Bold,maxLines=2,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    Text(helpDescription,color=Muted,fontSize=14.sp,lineHeight=20.sp,maxLines=4,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    Spacer(Modifier.weight(1f))
                    Text(tr("Valor actual","Current setting"),color=Muted,fontSize=12.sp)
                    Text(helpValue,color=Accent,fontSize=16.sp,lineHeight=20.sp,maxLines=2,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
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
    var managing by remember { mutableStateOf(false) }
    var picking by remember { mutableStateOf(false) }
    val firstProfile=remember { FocusRequester() }
    LaunchedEffect(Unit) { delay(150); runCatching { firstProfile.requestFocus() } }
    var newName by remember { mutableStateOf("") }
    var newAvatar by remember { mutableStateOf("ai:astronaut") }
    var editingAvatar by remember { mutableStateOf<Profile?>(null) }
    var deletingProfile by remember { mutableStateOf<Profile?>(null) }
    var choosingNewAvatar by remember { mutableStateOf(false) }
    var renamingProfile by remember { mutableStateOf<Profile?>(null) }
    var profileName by remember { mutableStateOf("") }
    BackHandler { if(managing) managing=false else onBack() }
    Box(Modifier.fillMaxSize().background(Bg)) {
    ProfileLandscape(state.appForeground)
    LazyColumn(Modifier.fillMaxSize().padding(horizontal=50.dp),contentPadding=PaddingValues(vertical=32.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
        item { Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text(tr("¿Quién está viendo?","Who's watching?"),color=Color.White,fontSize=38.sp, lineHeight=45.sp,fontWeight=FontWeight.Black)
            Text(if(picking) tr("Preparando tu perfil…","Preparing your profile…") else tr("Tu próxima historia empieza aquí.","Your next story starts here."),color=Muted,fontSize=16.sp,lineHeight=20.sp)
            FocusButton(if(managing) tr("Listo","Done") else tr("Administrar perfiles","Manage profiles")) { managing=!managing }
        } }
        item {
            LazyRow(horizontalArrangement=Arrangement.spacedBy(20.dp),contentPadding=PaddingValues(8.dp)) {
                items(state.profiles,key={it.id}) { profile ->
                    var focused by remember { mutableStateOf(false) }
                    val avatarScale by animateFloatAsState(if(focused) 1.05f else 1f,tween(160),label="profileFocus")
                    Column(Modifier.width(180.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(14.dp)) {
                        Column(Modifier.then(if(profile.id==state.profiles.firstOrNull()?.id) Modifier.focusRequester(firstProfile) else Modifier)
                            .onFocusChanged { focused=it.isFocused }.focusable()
                            .tvClick { if(!picking) { picking=true; state.launch { try { state.selectProfile(profile) } finally { picking=false } } } }
                            .clickable { if(!picking) { picking=true; state.launch { try { state.selectProfile(profile) } finally { picking=false } } } },horizontalAlignment=Alignment.CenterHorizontally) {
                            Box(Modifier.graphicsLayer { scaleX=avatarScale;scaleY=avatarScale }
                                .border(if(focused) 3.dp else 0.dp,Color.White,RoundedCornerShape(22.dp)).padding(5.dp)) {
                                ProfileAvatar(profile,Modifier.size(120.dp))
                            }
                            Text(profile.name,color=Color.White,fontSize=17.sp, lineHeight=21.sp)
                        }
                        if(managing) {
                        FocusButton(tr("Cambiar nombre","Rename")) { profileName=profile.name; renamingProfile=profile }
                        FocusButton(tr("Editar foto","Edit picture")) { editingAvatar=profile }
                        FocusButton(tr("Subir desde celular","Upload from phone")) { state.launch { state.socialAction { state.startAvatarUpload(profile) } } }
                        if(state.profiles.size>1 && !profile.isKids && profile.id!=com.miflix.native2.data.KidsContentPolicy.PROFILE_ID) FocusButton(tr("Eliminar perfil","Delete profile")) { deletingProfile=profile }
                        }
                    }
                }
            }
        }
        if(managing) item { Column {
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
    renamingProfile?.let { profile ->
        Dialog(onDismissRequest={ renamingProfile=null }) {
            Column(Modifier.width(460.dp).background(Panel,RoundedCornerShape(24.dp)).padding(24.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
                Text(tr("Nombre del perfil","Profile name"),color=Color.White,fontSize=26.sp,lineHeight=31.sp)
                NativeTextField(profileName,{profileName=it},tr("Nombre","Name"),modifier=Modifier.fillMaxWidth())
                Row(horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                    FocusButton(tr("Guardar","Save")) { state.launch { state.socialAction { state.renameProfile(profile,profileName); renamingProfile=null } } }
                    FocusButton(tr("Cancelar","Cancel")) { renamingProfile=null }
                }
            }
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
        },close={ editingAvatar=null },customUpload={ editingAvatar=null;state.launch { state.socialAction { state.startAvatarUpload(profile) } } })
    }
}

@Composable
fun WatchPartyScreen(state: AppState,onBack: () -> Unit) = PartyLobby(state,onBack)

@Composable
fun PlayerScreen(state: AppState, request: PlayerRequest, onClose: () -> Unit) {
    val context = LocalContext.current
    val mini=state.miniPlayer && state.screen!=Screen.PLAYER
    val timing=remember(request.playbackId) { com.miflix.native2.player.SubtitleTiming() }
    var timingMs by remember(request.playbackId) { mutableStateOf(0L) }
    var confirmMulti by remember { mutableStateOf(false) }
    val playbackView = LocalView.current
    var quickReactions by remember(request.playbackId) { mutableStateOf(false) }
    var firstFrame by remember(request.playbackId) {mutableStateOf(false)}
    var hadPlaybackError by remember(request.playbackId) {mutableStateOf(false)}
    var controlsVisible by remember(request.playbackId) { mutableStateOf(true) }
    var controlInteraction by remember { mutableStateOf(0) }
    val surfaceFocus = remember { FocusRequester() }
    LaunchedEffect(firstFrame) {if(firstFrame) {withFrameNanos {};runCatching {surfaceFocus.requestFocus()} }}
    val playFocus = remember { FocusRequester() }
    var playerMenu by remember(request.playbackId) { mutableStateOf<String?>(null) }
    var resizeMode by remember { mutableStateOf(androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    var maximized by remember { mutableStateOf(true) }
    var speed by remember(request.playbackId) { mutableStateOf(1f) }
    var playbackError by remember(request.playbackId) { mutableStateOf<String?>(null) }
    LaunchedEffect(playbackError) {if(playbackError!=null) {hadPlaybackError=true;state.finishPlaybackLoading(request.playbackId)}}
    var externalSubtitles by remember(request.playbackId) { mutableStateOf<List<SubtitleChoice>>(emptyList()) }
    var subtitleLoading by remember(request.playbackId) { mutableStateOf(false) }
    var pendingSubtitleLabel by remember(request.playbackId) { mutableStateOf<String?>(null) }
    var surfaceGeneration by remember(request.playbackId) { mutableStateOf(0) }
    var tracks by remember(request.playbackId) { mutableStateOf(androidx.media3.common.Tracks.EMPTY) }
    var showPartyActions by remember { mutableStateOf(false) }
    var showQueue by remember { mutableStateOf(false) }
    var creatingParty by remember { mutableStateOf(false) }
    var introDismissed by remember(request.playbackId) { mutableStateOf(false) }
    var showAccessRequests by remember { mutableStateOf(false) }
    var showEpisodes by remember(request.playbackId) { mutableStateOf(false) }
    var episodesResumeAfterClose by remember(request.playbackId) { mutableStateOf(false) }
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
    var guestIntentSequence by remember(request.playbackId) { mutableStateOf(0L) }
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
        val renderers = if(request.live) androidx.media3.exoplayer.DefaultRenderersFactory(context).setEnableDecoderFallback(true)
            else com.miflix.native2.player.TimedTextRenderers(context,timing).setEnableDecoderFallback(true)
        val httpFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setDefaultRequestProperties(request.stream.requestHeaders)
        val dataFactory = DefaultDataSource.Factory(context, httpFactory)
        ExoPlayer.Builder(context, renderers)
            .apply {if(!request.live) setAudioAttributes(androidx.media3.common.AudioAttributes.Builder().setUsage(androidx.media3.common.C.USAGE_MEDIA).setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MOVIE).build(),true)}
            .setHandleAudioBecomingNoisy(true)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataFactory))
            .setLoadControl(androidx.media3.exoplayer.DefaultLoadControl.Builder()
                .setBufferDurationsMs(15000,50000,1000,1500)
                .setPrioritizeTimeOverSizeThresholds(true).build())
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
            val trackBuilder = TrackSelectionParameters.Builder(context)
            if(com.miflix.native2.data.StreamLanguagePriority.prefersSpanish(state.audioLanguage)) {
                trackBuilder.setPreferredAudioLanguages("es", "en")
            } else trackBuilder.setPreferredAudioLanguage(state.audioLanguage.takeIf { it.isNotBlank() })
            if (preferredText != null) trackBuilder.setPreferredTextLanguage(preferredText)
            trackBuilder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT,state.subtitleLanguage == "off")
            trackSelectionParameters = trackBuilder.build()
            videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
            prepare()
            playWhenReady = if (state.partyRole == PartyRole.GUEST) state.watchParty?.playing ?: true else true
        }
    }

    fun userPlayback(playing: Boolean) {
        if(player.playWhenReady==playing) return
        player.playWhenReady=playing
        if(state.watchParty!=null) state.launch {
            if(state.partyRole==PartyRole.HOST) state.hostPartyUpdate(request,player.currentPosition,playing)
            state.publishPartyPlaybackNotice(playing)
        }
    }
    fun openPlayerParty() { showPartyActions=true }
    fun createPlayerParty() {
        if(!creatingParty) {
            creatingParty=true
            state.launch { try { if(state.createWatchParty(true)!=null) state.hostPartyUpdate(request,player.currentPosition,player.playWhenReady) } finally { creatingParty=false } }
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
    HeadphoneAudioRouting(player) { playbackError=null }
    LaunchedEffect(player) {
        try {
            while(true) {
                positionMs=player.currentPosition; durationMs=player.duration.coerceAtLeast(0)
                // Snapshot contains catalog metadata only, never stream URLs or headers.
                state.localNowPlaying=org.json.JSONObject().put("active",true)
                    .put("cloud",request.item.cloudId).put("title",request.item.title)
                    .put("poster",request.item.poster ?: org.json.JSONObject.NULL)
                    .put("season",request.season).put("episode",request.episode)
                    .put("position",positionMs).put("duration",durationMs)
                    .put("playing",player.isPlaying).put("loading",!firstFrame)
                    .put("updatedAt",System.currentTimeMillis()).toString()
                delay(250)
            }
        } finally {state.localNowPlaying="{\"active\":false}"}
    }
    LaunchedEffect(paused) {
        if(paused) while(true) { clockNow = System.currentTimeMillis(); delay(1000) }
    }
    LaunchedEffect(paused,request.playbackId) {
        pauseInfoDismissed = false
        pauseSynopsis = false
        if(paused) { delay(5000); if(!pauseInfoDismissed) pauseSynopsis = true }
    }
    val randomMode=state.randomEpisodeCloud==request.item.cloudId && request.item.type=="series"
    val queueNext=state.queueEntries.firstOrNull()
    var queueSynopsis by remember(queueNext?.id) { mutableStateOf("") }
    LaunchedEffect(queueNext?.id) {
        queueNext?.let { entry -> queueSynopsis=runCatching {
            val nextItem=state.tmdb.byCloudId(entry.cloud) ?: return@runCatching ""
            if(entry.season>0) state.tmdb.episodeInfo(nextItem,entry.season,entry.episode)?.overview.orEmpty() else nextItem.overview
        }.getOrDefault("") }
    }
    LaunchedEffect(ended,upcoming,queueNext?.id,cancelAutoplay,state.autoplayNext,rateAtEnd,randomMode) {
        if(ended && !rateAtEnd && !cancelAutoplay && state.partyRole!=PartyRole.GUEST && (queueNext!=null || (randomMode || upcoming!=null) && state.autoplayNext)) {
            countdown=10
            while(countdown>0) { delay(1000);countdown-- }
            cancelAutoplay=true
            state.launch { runCatching { if(queueNext!=null) state.playQueued(request,queueNext) else if(randomMode) state.playRandomEpisode(request.item) else upcoming?.let { state.playNext(request,it) } }.onFailure { state.error=it.message } }
        }
    }
    LaunchedEffect(state.playbackLoading,player) {
        val pending=state.playbackLoading
        if(pending!=null && pending.playbackId!=request.playbackId) player.pause()
    }
    LaunchedEffect(player) { showEpisodes = false }
    LaunchedEffect(playerMenu,request.playbackId) {
        if(playerMenu=="subtitles" && externalSubtitles.isEmpty()) {
            subtitleLoading=true
            try { externalSubtitles=runCatching { state.playbackSubtitles(request) }.getOrDefault(emptyList()) }
            finally { subtitleLoading=false }
        }
    }

    DisposableEffect(player, playbackView) {
        val previousKeepScreenOn = playbackView.keepScreenOn
        fun updateScreenAwake() {
            playbackView.keepScreenOn = previousKeepScreenOn || (
                player.playWhenReady &&
                    (player.playbackState == Player.STATE_READY || player.playbackState == Player.STATE_BUFFERING)
                )
        }
        var triedLiveHls=false
        val listener = object : Player.Listener {
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                // Some IPTV endpoints have no extension and return generic content types.
                // Retry HLS once only when automatic container detection failed, never on HTTP/auth errors.
                if(request.live && !triedLiveHls && error.errorCode==androidx.media3.common.PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED) {
                    triedLiveHls=true
                    player.setMediaItem(MediaItem.Builder().setUri(request.stream.url).setMimeType(MimeTypes.APPLICATION_M3U8).build())
                    player.prepare()
                    return
                }
                playbackError=playerFailureDetails(error)
            }
            override fun onTracksChanged(value: androidx.media3.common.Tracks) {
                tracks = value
                pendingSubtitleLabel?.let { label ->
                    val group=value.groups.firstOrNull { it.type==C.TRACK_TYPE_TEXT && (0 until it.length).any { index -> it.getTrackFormat(index).label==label && it.isTrackSupported(index) } }
                    if(group!=null) {
                        val index=(0 until group.length).first { group.getTrackFormat(it).label==label && group.isTrackSupported(it) }
                        pendingSubtitleLabel=null
                        player.trackSelectionParameters=player.trackSelectionParameters.buildUpon().setTrackTypeDisabled(C.TRACK_TYPE_TEXT,false).setOverrideForType(androidx.media3.common.TrackSelectionOverride(group.mediaTrackGroup,index)).build()
                    }
                }
            }
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                paused = !playWhenReady && player.playbackState != Player.STATE_ENDED
                if(paused) { controlsVisible=true; controlInteraction++ }
                if(state.partyRole == PartyRole.GUEST && !applyingRemote && reason == Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST) {
                    if(state.watchParty?.hostOnlyControls==true) state.partyFollowHost=playWhenReady
                    guestPlaybackIntent=playWhenReady; guestIntentUntil=System.currentTimeMillis()+10000
                    val sequence=++guestIntentSequence
                    state.launch { runCatching { state.requestPartyPlayback(request,playWhenReady) }.onFailure {
                        if(it is kotlinx.coroutines.CancellationException) throw it
                        if(sequence==guestIntentSequence) { guestPlaybackIntent=null; state.error=it.message }
                    } }
                }
            }
            override fun onEvents(player: Player, events: Player.Events) {
                updateScreenAwake()
            }

            override fun onRenderedFirstFrame() {firstFrame=true;state.finishPlaybackLoading(request.playbackId)}
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

    LaunchedEffect(player, state.partyRole, state.watchParty?.roomCode, request.stream.url,state.appForeground) {
        var previousRemotePlaying: Boolean?=null
        while (state.watchParty != null && state.appForeground) {
            delay(500)
            if(state.sessionNeedsSignIn) continue
            when (state.partyRole) {
                PartyRole.HOST -> {
                    val command=try { state.takePartyPlayback() } catch(e: kotlinx.coroutines.CancellationException) { throw e } catch(e: Exception) { state.error=e.message;null }
                    kotlinx.coroutines.currentCoroutineContext().ensureActive()
                    if(state.playerRequest?.playbackId!=request.playbackId) break
                    command?.let { player.playWhenReady=it }
                    runCatching { state.hostPartyUpdate(request, player.currentPosition, player.isPlaying) }
                }
                PartyRole.GUEST -> {
                    val remote = try { state.refreshParty() } catch(e: kotlinx.coroutines.CancellationException) { throw e } catch(_: Exception) { null } ?: continue
                    if (remote.cloudId != request.item.cloudId || remote.season != request.season || remote.episode != request.episode) {
                        try { state.switchToPartyState(remote) } catch(e: kotlinx.coroutines.CancellationException) { throw e } catch(e: Exception) { state.error=e.message }
                        if(state.playerRequest?.playbackId!=request.playbackId) break
                        continue
                    }
                    kotlinx.coroutines.currentCoroutineContext().ensureActive()
                    if(state.playerRequest?.playbackId!=request.playbackId) break
                    if(!state.partyFollowHost && remote.hostOnlyControls) continue
                    if(guestPlaybackIntent==remote.playing || System.currentTimeMillis()>=guestIntentUntil) guestPlaybackIntent=null
                    // Preserve the paused decoder frame. Do not seek to stale host snapshots while
                    // a guest pause/resume request is awaiting acknowledgement, or while paused.
                    // Reconcile position on resume / during playback before applying remote play.
                    if(guestPlaybackIntent==null && remote.playing && !request.live && player.isCurrentMediaItemSeekable && abs(partyTargetPosition(remote)-player.currentPosition)>900) {
                        player.seekTo(partyTargetPosition(remote))
                    }
                    if(guestPlaybackIntent==null && !request.live && player.isCurrentMediaItemSeekable) {
                        val drift=partyTargetPosition(remote)-player.currentPosition
                        val speed=if(!remote.playing || abs(drift)<120) 1f else if(drift>0) 1.02f else .98f
                        if(player.playbackParameters.speed!=speed) player.setPlaybackSpeed(speed)
                    }
                    if(guestPlaybackIntent==null && !request.live && !remote.playing && previousRemotePlaying!=false && player.isCurrentMediaItemSeekable && abs(remote.positionMs-player.currentPosition)>120) {
                        player.seekTo(remote.positionMs) // Align the paused frame once, never on every paused poll.
                    }
                    if(guestPlaybackIntent==null) previousRemotePlaying=remote.playing
                    if (guestPlaybackIntent==null && remote.playing != player.playWhenReady) {
                        applyingRemote = true; player.playWhenReady = remote.playing; applyingRemote = false
                    }
                }
                null -> Unit
            }
        }
    }

    LaunchedEffect(state.watchParty?.roomCode,state.partyFollowHost,state.partyRole) {
        if(state.watchParty==null || !state.partyFollowHost || state.partyRole!=PartyRole.GUEST) player.setPlaybackSpeed(1f)
    }

    // Explicit timer also covers controls holding focus on TV remotes.
    LaunchedEffect(player, controlInteraction, showEpisodes, showQueue, showPartyActions, showAccessRequests, state.sourceSelection, playerMenu, paused, ended, playbackError, mini) {
        if (!mini && playbackError==null && !paused && !ended && playerMenu == null && !quickReactions && !showEpisodes && !showQueue && !showPartyActions && !showAccessRequests && state.sourceSelection == null) {
            delay(3000)
            controlsVisible = false
            runCatching { surfaceFocus.requestFocus() }
        }
    }

    LaunchedEffect(firstFrame,controlsVisible, showEpisodes, showQueue, showPartyActions, showAccessRequests, playerMenu, state.sourceSelection, pauseSynopsis, pauseInfoDismissed, ended, playbackError, mini) {
        if(firstFrame && !mini && playbackError==null && controlsVisible && (!pauseSynopsis || pauseInfoDismissed) && !ended && !showEpisodes && !showQueue && !showPartyActions && !showAccessRequests && playerMenu == null && !quickReactions && state.sourceSelection == null) {
            withFrameNanos { }; runCatching { playFocus.requestFocus() }
        }
    }
    LaunchedEffect(pauseSynopsis, pauseInfoDismissed, mini) {
        if(!mini && pauseSynopsis && !pauseInfoDismissed) { withFrameNanos { }; runCatching { surfaceFocus.requestFocus() } }
    }
    var exitArmedAt by remember(request.playbackId) { mutableStateOf(0L) }
    fun requestExit() {
        val now=android.os.SystemClock.elapsedRealtime()
        if(exitArmedAt!=0L && now-exitArmedAt<=2200L) { exitArmedAt=0L;onClose() }
        else exitArmedAt=now
    }
    LaunchedEffect(exitArmedAt) { if(exitArmedAt!=0L) { delay(2200);exitArmedAt=0L } }
    BackHandler(enabled=!mini) {
        if (quickReactions) { quickReactions=false;controlsVisible=true;controlInteraction++ }
        else if (playerMenu != null) playerMenu = null
        else if (showEpisodes) { showEpisodes=false; if(episodesResumeAfterClose) player.play() }
        else if (paused && !pauseInfoDismissed) {
            pauseSynopsis = false; pauseInfoDismissed = true
            controlsVisible = true; controlInteraction++;requestExit()
        } else requestExit()
    }

    Box(Modifier.fillMaxSize().consumeOpeningPress().background(Color.Black).focusRequester(surfaceFocus).onPreviewKeyEvent { event ->
        if(mini || quickReactions) return@onPreviewKeyEvent false
        val code = event.nativeKeyEvent.keyCode
        if(code!=android.view.KeyEvent.KEYCODE_BACK && event.type==KeyEventType.KeyDown && code !in listOf(android.view.KeyEvent.KEYCODE_DPAD_CENTER,android.view.KeyEvent.KEYCODE_ENTER)) exitArmedAt=0L
        if(code==android.view.KeyEvent.KEYCODE_BACK) false
        else if(code in listOf(android.view.KeyEvent.KEYCODE_VOLUME_UP,android.view.KeyEvent.KEYCODE_VOLUME_DOWN,android.view.KeyEvent.KEYCODE_VOLUME_MUTE)) false
        else if (dismissKey == code) {
            if (event.type == KeyEventType.KeyUp) dismissKey = null
            true
        } else if (event.type == KeyEventType.KeyDown && !showEpisodes && !showQueue && !showPartyActions && !showAccessRequests && playerMenu == null && !quickReactions && state.sourceSelection == null) {
            val wasHidden = !controlsVisible
            controlsVisible = true
            controlInteraction++
            if (paused && !pauseInfoDismissed) {
                pauseInfoDismissed = true; pauseSynopsis = false; dismissKey = code
                true
            } else if(code == android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE || code == android.view.KeyEvent.KEYCODE_MEDIA_PLAY || code == android.view.KeyEvent.KEYCODE_MEDIA_PAUSE) {
                userPlayback(when(code) { android.view.KeyEvent.KEYCODE_MEDIA_PLAY -> true; android.view.KeyEvent.KEYCODE_MEDIA_PAUSE -> false; else -> !player.playWhenReady })
                dismissKey = code; true
            } else if(wasHidden) {
                if(code in listOf(android.view.KeyEvent.KEYCODE_DPAD_CENTER,android.view.KeyEvent.KEYCODE_ENTER,android.view.KeyEvent.KEYCODE_NUMPAD_ENTER)) {
                    userPlayback(!player.playWhenReady)
                }
                dismissKey = code; true
            } else false
        } else false
    }.focusable(enabled=!mini)) {
        key(player,surfaceGeneration) { AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    // RC5 used a fresh SurfaceView; synchronize it with Compose, without reusing
                    // a previous content's surface. Do not relayout it for every progress tick.
                    setEnableComposeSurfaceSyncWorkaround(true)
                    setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                    setKeepContentOnPlayerReset(false)
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
                if(view.resizeMode!=resizeMode) view.resizeMode = resizeMode
                view.subtitleView?.apply {
                    val styleKey=state.subtitleColor to state.subtitleScale
                    if(tag!=styleKey) {
                        setApplyEmbeddedStyles(false)
                        setApplyEmbeddedFontSizes(false)
                        setStyle(androidx.media3.ui.CaptionStyleCompat(state.subtitleColor,android.graphics.Color.TRANSPARENT,android.graphics.Color.TRANSPARENT,androidx.media3.ui.CaptionStyleCompat.EDGE_TYPE_OUTLINE,android.graphics.Color.BLACK,null))
                        setFractionalTextSize(0.0533f*state.subtitleScale)
                        tag=styleKey
                    }
                }
            },
            modifier = if(maximized) Modifier.fillMaxSize() else Modifier.fillMaxSize().padding(horizontal=40.dp,vertical=24.dp)
        ) }
        if(!firstFrame && playbackError==null && state.playbackLoading==null) PlaybackArtwork(state,request)
        if(mini) {
            Row(Modifier.align(Alignment.BottomCenter).background(Color(0xEE171717)).padding(6.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                FocusButton(if(paused) "▶" else "Ⅱ",horizontalPadding=12.dp) { userPlayback(!player.playWhenReady) }
                FocusButton(tr("Ampliar","Expand"),horizontalPadding=14.dp) { state.launch {
                    if(!request.live) { state.open(request.item);if(request.season>1) state.loadSeason(request.season) }
                    if(state.playerRequest?.playbackId==request.playbackId) {
                        state.selected=request.item;state.miniPlayer=false;state.screen=Screen.PLAYER;controlsVisible=true;controlInteraction++
                    }
                } }
                FocusButton(tr("Cerrar","Close"),horizontalPadding=14.dp) { state.miniPlayer=false;state.playerRequest=null }
            }
        } else if(firstFrame || hadPlaybackError || playbackError!=null) {
        if(playbackError!=null) {
            val errorMessage=playbackError
            LaunchedEffect(errorMessage) {
                delay(5000)
                if(playbackError==errorMessage) { playbackError=null;controlsVisible=true;controlInteraction++ }
            }
            Dialog(onDismissRequest={ playbackError=null;controlsVisible=true },properties=DialogProperties(usePlatformDefaultWidth=false)) {
            val retryFocus=remember { FocusRequester() }
            LaunchedEffect(Unit) { withFrameNanos { };runCatching { retryFocus.requestFocus() } }
            Column(Modifier.width(600.dp).background(Color(0xEE171717),RoundedCornerShape(18.dp)).padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
                Text(tr("No se pudo reproducir este enlace","This link could not be played"),color=Color.White,fontSize=22.sp,lineHeight=27.sp)
                Text(playbackError.orEmpty(),color=Muted)
                Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    FocusButton(tr("Reintentar","Retry"),modifier=Modifier.focusRequester(retryFocus)) { playbackError=null; surfaceGeneration++; player.prepare() }
                    FocusButton(tr("Otro enlace","Another link")) { playbackError=null; state.launch { state.playerSources(request,player.currentPosition) } }
                    FocusButton(tr("Salir","Exit")) { onClose() }
                }
            }
            }
        }

        if(playerMenu in listOf("content","speed","audio","subtitles")) PlayerOptionsDialog(playerMenu!!, player, tracks, speed,
            onSpeed={ speed=it; player.setPlaybackSpeed(it) },
            externalSubtitles=externalSubtitles.filter { extra -> request.stream.subtitles.none { it.url==extra.url } }, subtitleLoading=subtitleLoading,
            onExternalSubtitle={ selected ->
                player.currentMediaItem?.let { media ->
                    val label="BruniO · ${selected.label}"
                    pendingSubtitleLabel=label
                    val subtitle=MediaItem.SubtitleConfiguration.Builder(Uri.parse(selected.url)).setLabel(label)
                        .setLanguage(normalizeMediaLanguage(selected.lang)).setMimeType(subtitleMime(selected.url)).setSelectionFlags(C.SELECTION_FLAG_DEFAULT).build()
                    player.trackSelectionParameters=player.trackSelectionParameters.buildUpon().clearOverridesOfType(C.TRACK_TYPE_TEXT).setTrackTypeDisabled(C.TRACK_TYPE_TEXT,false).build()
                    // Explicit external subtitle selection requires preparing its sidecar source;
                    // it never runs automatically when a slow subtitle lookup finishes.
                    player.setMediaItem(media.buildUpon().setSubtitleConfigurations(listOf(subtitle)).build(),player.currentPosition)
                    player.prepare()
                }
            },
            series=request.item.type=="series", party=state.watchParty!=null,
            onEpisodes={ episodesResumeAfterClose=player.playWhenReady; player.pause(); showEpisodes=true },
            onParty={ openPlayerParty() },
            onTiming={ playerMenu="timing" },
            onClose={ playerMenu=null; controlInteraction++ })
        if(playerMenu=="timing") SubtitleTimingDialog(timingMs,apply={ value ->
            timingMs=value;timing.delayMs=value
            // Explicit user adjustment, not a recovery loop. Re-read subtitle cues at this position.
            if(player.isCurrentMediaItemSeekable) player.seekTo(player.currentPosition)
        },close={ playerMenu=null;controlInteraction++ })
        if(playerMenu=="info") PlaybackInfoDialog(player,request) { playerMenu=null;controlInteraction++ }
        if(playerMenu=="channel_permission") Dialog(onDismissRequest={ playerMenu=null }) {
            val first=remember { FocusRequester() }
            LaunchedEffect(Unit) { withFrameNanos { };first.requestFocus() }
            Column(Modifier.width(460.dp).background(Panel,RoundedCornerShape(20.dp)).padding(24.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
                Text(tr("El host cambia el canal del party. Sal de la sala si quieres ver otro por tu cuenta.","The host changes the party channel. Leave the party to watch a different channel independently."),color=Color.White)
                FocusButton("OK",modifier=Modifier.focusRequester(first)) { playerMenu=null;controlInteraction++ }
            }
        }
        if(playerMenu=="channels") LiveChannelPicker(state,request.liveProvider,select={ req ->
            if(state.partyRole==PartyRole.GUEST) state.error=tr("El host cambia el canal del party","The host changes the party channel")
            else { playerMenu=null;state.openLivePlayer(req) }
        },close={ playerMenu=null;controlInteraction++ })
        if(confirmMulti) Dialog(onDismissRequest={ confirmMulti=false }) {
            val first=remember { FocusRequester() }
            LaunchedEffect(Unit) { withFrameNanos { };first.requestFocus() }
            Column(Modifier.width(520.dp).background(Panel,RoundedCornerShape(20.dp)).padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
                Text(if(state.partyRole==PartyRole.HOST) tr("Multivista cerrará tu party actual. ¿Continuar?","Multiview will close your current party. Continue?") else tr("Multivista saldrá de tu party actual. ¿Continuar?","Multiview will leave your current party. Continue?"),color=Color.White)
                FocusButton(tr("Cancelar","Cancel"),modifier=Modifier.focusRequester(first)) { confirmMulti=false }
                FocusButton(tr("Continuar","Continue")) { state.launch { state.leaveWatchParty();state.beginMultiView(request) } }
            }
        }
        if((firstFrame || hadPlaybackError) && controlsVisible && (!pauseSynopsis || pauseInfoDismissed) && !showEpisodes && !showPartyActions && !ended) {
            CompactPlayerControls(request, paused, positionMs, durationMs, speed, maximized, playFocus,
                onInteraction={ controlInteraction++ },
                onPlay={ userPlayback(!player.playWhenReady) },
                onResize={ resizeMode=if(resizeMode == androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT) androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM else androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT },
                onMenu={ playerMenu=it }, onLinks={ state.launch { state.playerSources(request,player.currentPosition) } },
                onSeek={ if(player.isCurrentMediaItemSeekable) { if(state.partyRole==PartyRole.GUEST && state.watchParty?.hostOnlyControls==true) state.partyFollowHost=false;positionMs=it; player.seekTo(it) } }, seekable=player.isCurrentMediaItemSeekable && durationMs>0,
                party=state.watchParty!=null,onParty={ openPlayerParty() },
                onChat={ if(state.watchParty==null) openPlayerParty() else playerMenu="party-chat" },
                onReactions={ if(state.watchParty==null) openPlayerParty() else playerMenu="party-reactions" },
                onQuickReactions={ if(state.watchParty==null) openPlayerParty() else { quickReactions=true;controlsVisible=false;controlInteraction++ } },onClose={ requestExit() }, onMaximize={ maximized=!maximized },
                nextAvailable=(randomMode || upcoming!=null) && state.partyRole!=PartyRole.GUEST,
                onNext={cancelAutoplay=true;state.launch {state.socialAction {if(randomMode) state.playRandomEpisode(request.item) else upcoming?.let {state.playNext(request,it)} } }},
                onEpisodes={ if(request.live) playerMenu=if(state.partyRole==PartyRole.GUEST) "channel_permission" else "channels" else { episodesResumeAfterClose=false;showEpisodes=true } },onQueue={ showQueue=true },
                onInfo={ playerMenu="info" },onPip={ playerMenu=null;controlsVisible=false;state.miniPlayer=true;state.screen=Screen.HOME },
                onMultiView={ if(state.watchParty!=null || state.pendingPartyCode!=null) confirmMulti=true else state.beginMultiView(request) })
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
        if(firstFrame && (controlsVisible || state.partyPlaybackNotice!=null) && !mini && state.watchParty!=null && playerMenu==null && !showPartyActions && !showEpisodes) {
            PartyMembersOverlay(state,Modifier.align(Alignment.TopCenter).padding(top=20.dp),playing=player.playWhenReady) {openPlayerParty()}
        }
        if(controlsVisible && state.accessRequests.isNotEmpty() && !showEpisodes && !showPartyActions) {
            Box(Modifier.align(Alignment.CenterEnd).padding(26.dp)) {
                FocusButton("${state.accessRequests.size} ${tr("solicitudes · Sala","requests · Party")}") { showAccessRequests=true }
            }
        }
        if(pauseSynopsis && !pauseInfoDismissed && !showEpisodes && !showPartyActions && playerMenu == null && !quickReactions && state.sourceSelection == null && !ended) {
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
        if(paused && !pauseInfoDismissed && !ended && !showEpisodes && !showPartyActions && playerMenu == null && !quickReactions && state.sourceSelection == null) {
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
                Text(if(request.live || durationMs <= 0) remainingText else "${tr("Termina a las","Ends at")} · "+android.text.format.DateFormat.getTimeFormat(context).format(java.util.Date(clockNow+((durationMs-positionMs).coerceAtLeast(0)/speed.coerceAtLeast(0.1f)).toLong())),color=Color(0xFFE0E0E0),fontSize=19.sp, lineHeight=23.sp)
            }
        }
        if(!ended && !showEpisodes && !showQueue && !showPartyActions && !showAccessRequests && playerMenu==null && !quickReactions && state.sourceSelection==null && state.partyRole != PartyRole.GUEST && durationMs > 0) {
            val intro = segments.intro?.takeIf { it.endMs <= durationMs && positionMs >= it.startMs && positionMs < it.endMs }
            val credits = segments.outro?.takeIf { it.startMs < durationMs && positionMs >= it.startMs && positionMs < it.endMs }
            if(intro != null || credits != null) Box(Modifier.align(Alignment.BottomEnd).padding(end=30.dp,bottom=100.dp)) {
                if(intro != null && !introDismissed) SkipIntroDialog({ introDismissed=true;player.seekTo(intro.endMs) }) { introDismissed=true }
                else if(credits != null) FocusButton(tr("Saltar créditos","Skip Credits"),primary=true) {
                    val post = segments.postCredits?.startMs?.takeIf { it > positionMs && it < durationMs }
                    player.seekTo(post ?: credits.endMs.coerceAtMost(durationMs))
                }
            }
        }
        if(rateAtEnd) RatingDialog(state,request.item) { rateAtEnd=false; state.launch { recommendations=state.recommendationsFor(request.item) } }
        if(ended && state.playbackLoading==null && state.partyRole != PartyRole.GUEST && !rateAtEnd && !showQueue && !showPartyActions) {
            Dialog(onDismissRequest={ cancelAutoplay=true; onClose() },properties=DialogProperties(usePlatformDefaultWidth=false)) {
                Column(Modifier.width(850.dp).heightIn(max=600.dp).background(Color(0xF00B0B0B),RoundedCornerShape(20.dp)).padding(25.dp)) {
                    Text("${tr("Terminaste","You finished")} ${request.item.title}",color=Color.White,fontSize=26.sp, lineHeight=31.sp)
                    queueNext?.let { entry ->
                        Text(tr("Siguiente en tu cola","Next in your queue")+": "+entry.title+if(entry.season>0) " · S${entry.season} E${entry.episode}" else "",color=Color.White,fontSize=20.sp)
                        Text(queueSynopsis,color=Color.White,fontSize=15.sp,lineHeight=20.sp,maxLines=4)
                        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                            FocusButton(if(!cancelAutoplay) "${tr("Reproducir cola","Play queue")} · ${countdown}s" else tr("Reproducir cola","Play queue")) { cancelAutoplay=true;state.launch { state.socialAction { state.playQueued(request,entry) } } }
                            FocusButton(tr("Cancelar","Cancel")) { cancelAutoplay=true }
                            FocusButton(tr("Editar cola","Edit queue")) { cancelAutoplay=true;showQueue=true }
                        }
                    }
                    if(queueNext==null && randomMode) {
                        Text(tr("Siguiente episodio aleatorio","Next random episode"),color=Muted,fontSize=20.sp)
                        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                            FocusButton(tr("Reproducir aleatorio","Play random")+if(!cancelAutoplay && state.autoplayNext) " · ${countdown}s" else "") {cancelAutoplay=true;state.launch {state.playRandomEpisode(request.item)}}
                            FocusButton(tr("Detener aleatorio","Stop shuffle")) {cancelAutoplay=true;state.stopRandomEpisodes()}
                        }
                    }
                    if(queueNext==null && !randomMode) upcoming?.let { next ->
                        val hidden=!state.episodeWatched(request.item,next)
                        Text("${tr("Siguiente","Next")}: S${next.season} E${next.episode} · ${if(hidden) tr("No visto","Not watched") else next.title}",color=Muted)
                        Text(if(hidden) tr("Sinopsis oculta para evitar spoilers","Synopsis hidden to avoid spoilers") else next.overview,color=Color.White,fontSize=15.sp,lineHeight=20.sp,maxLines=4)
                        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                            FocusButton(if(state.autoplayNext && !cancelAutoplay) "${tr("Siguiente episodio","Next episode")} · ${countdown}s" else tr("Siguiente episodio","Next episode"),primary=true) { cancelAutoplay=true; state.launch { state.playNext(request,next) } }
                            FocusButton(tr("Cancelar reproducción automática","Cancel autoplay")) { cancelAutoplay=true }
                        }
                    }
                    Text(tr("También te puede gustar","You may also like"),color=Color.White,fontSize=22.sp, lineHeight=26.sp)
                    LazyRow(horizontalArrangement=Arrangement.spacedBy(12.dp),modifier=Modifier.weight(1f,false)) {
                        items(recommendations,key={it.cloudId}) { item ->
                            MediaCard(item,allowContextMenu=false) { cancelAutoplay=true; state.launch { state.open(item) } }
                        }
                    }
                    Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                        FocusButton(tr("★ Puntuar","★ Rate")) { cancelAutoplay=true; rateAtEnd=true }
                        FocusButton(tr("Volver a detalles","Back to details")) { cancelAutoplay=true; onClose() }
                    }
                }
            }
        }
        if(playerMenu=="party-chat") PlayerPartyChatDialog(state) { playerMenu=null;controlInteraction++ }
        if(playerMenu=="party-reactions") PartyActionsDialog(state) { playerMenu=null;controlInteraction++ }
        if(quickReactions && state.watchParty!=null && !mini) QuickReactionBar(
            onSend={ emoji -> state.launch { state.socialAction { state.sendPartyEvent("emoji",emoji) } } },
            onClose={ quickReactions=false;controlsVisible=true;controlInteraction++ }
        )
        LaunchedEffect(state.watchParty?.roomCode,mini) { if(state.watchParty==null || mini) quickReactions=false }

        if (showPartyActions) PlayerSocialDialog(state,onCreate={ createPlayerParty() },onRefreshRequests={
            state.launch { state.socialAction { state.refreshPartyRequests() };showPartyActions=false;showAccessRequests=true }
        },onSwitchCurrent={
            state.launch { state.socialAction { state.hostPartyUpdate(request,player.currentPosition,player.playWhenReady) } }
        },onWatchParty={
            state.launch { state.socialAction {
                val remote=state.refreshParty()
                if(remote!=null) {
                    state.partyFollowHost=true
                    if(remote.cloudId==request.item.cloudId && remote.season==request.season && remote.episode==request.episode) {
                        applyingRemote=true
                        if(!request.live && player.isCurrentMediaItemSeekable) player.seekTo(remote.positionMs)
                        player.playWhenReady=remote.playing;applyingRemote=false
                    } else state.switchToPartyState(remote)
                    showPartyActions=false
                }
            } }
        }) { showPartyActions=false }
        if(showQueue) QueueDialog(state) { showQueue=false }
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
                controlsVisible=true; controlInteraction++
                if(episodesResumeAfterClose) player.play()
            }
        }


        } // full-size controls
        if(!mini && exitArmedAt!=0L) Text(tr("Pulsa dos veces para salir","Double tap to exit"),color=Color.White,fontSize=18.sp,
            modifier=Modifier.align(Alignment.TopCenter).padding(top=32.dp).background(Color(0xE6222222),RoundedCornerShape(16.dp)).padding(horizontal=24.dp,vertical=14.dp))
    }
}

@Composable
private fun EpisodePickerOverlay(state: AppState, request: PlayerRequest, onClose: () -> Unit) {
    val details = state.details
    var episodeActions by remember {mutableStateOf<EpisodeSummary?>(null)}
    val closeFocus=remember { FocusRequester() }
    val episodeFocus=remember { FocusRequester() }
    val listState=rememberLazyListState()
    val focusIndex=state.episodes.indexOfFirst { it.season==request.season && it.episode==request.episode }.coerceAtLeast(0)
    var entered by remember {mutableStateOf(false)}
    LaunchedEffect(state.currentSeason,state.episodes.map { it.season to it.episode }) {
        if(state.episodes.isNotEmpty()) {
            listState.scrollToItem(focusIndex)
            withFrameNanos { }
            if(!entered) {episodeFocus.requestFocus();entered=true}
        } else if(!entered) { withFrameNanos { }; closeFocus.requestFocus() }
    }
    Dialog(onDismissRequest=onClose,properties=DialogProperties(usePlatformDefaultWidth=false)) {
    Box(Modifier.fillMaxSize().background(Color(0xB8000000))) {
        Column(
            Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(620.dp).background(Color(0xFF0B0B0B)).padding(26.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(tr("Episodios","Episodes"), color = Color.White, fontSize = 29.sp, lineHeight=35.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.weight(1f))
                if(state.partyRole!=PartyRole.GUEST) FocusButton(tr("🎲 Aleatorio","🎲 Random")) {onClose();state.launch {state.playRandomEpisode(request.item)}}
                FocusButton(tr("Cerrar","Close"),modifier=Modifier.focusRequester(closeFocus)) { onClose() }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                SeasonSelector(state, details?.seasonCount ?: 0)
            }
            Spacer(Modifier.height(16.dp))
            LazyColumn(state=listState,modifier=Modifier.weight(1f),contentPadding = PaddingValues(bottom = 30.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(state.episodes, key = { "${it.season}:${it.episode}" }) { ep ->
                    val active = ep.season == request.season && ep.episode == request.episode
                    EpisodeListRow(ep, active, hidden=!state.episodeWatched(request.item,ep), modifier=if(state.episodes.indexOf(ep)==focusIndex) Modifier.focusRequester(episodeFocus) else Modifier, onLongClick = { episodeActions=ep }) {
                        onClose()
                        state.launch {runCatching {state.stopRandomEpisodes();state.playEpisode(ep)}.onFailure {state.error=it.message}}
                    }
                }
            }
        }
    }
    }
    episodeActions?.let {ep -> EpisodeActionsDialog(state,request.item,ep,beforePlayback=onClose) {episodeActions=null} }
}

@Composable
private fun EpisodeListRow(ep: EpisodeSummary, active: Boolean, hidden: Boolean, modifier: Modifier=Modifier, onLongClick: () -> Unit, onClick: () -> Unit) {
    var focused by remember(ep.season, ep.episode) { mutableStateOf(false) }
    Row(
        modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused }.tvPlaybackClick(onClick, onLongClick).clickable(onClick = onClick)
            .background(if (focused) Accent else if (active) Color(0xFF242424) else Color(0xFF161616), RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        EpisodeArtwork(ep,hidden,Modifier.width(150.dp).height(84.dp).clip(RoundedCornerShape(8.dp)))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("S${ep.season} E${ep.episode} · ${if(hidden) tr("No visto","Not watched") else ep.title}", color = if (focused) FocusInk else Color.White, fontSize = 14.sp, lineHeight=17.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(if(hidden) tr("Sinopsis oculta para evitar spoilers","Synopsis hidden to avoid spoilers") else ep.overview, color = if (focused) FocusInk.copy(alpha=.8f) else Muted, fontSize = 11.sp, lineHeight=13.sp, maxLines = 2)
        }
        if (active) Text(tr("ACTUAL","NOW"), color = if (focused) FocusInk else Color.White, fontSize = 10.sp, lineHeight=12.sp, fontWeight = FontWeight.Black)
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
private fun CastCard(person: CastMember, state: AppState,compact: Boolean=false) {
    var show by remember(person.id) { mutableStateOf(false) }
    var focused by remember { mutableStateOf(false) }
    if(show) ActorCreditsDialog(person, state) { show=false }
    val click = { if(person.id>0) show=true }
    Column(Modifier.width(if(compact) 92.dp else 138.dp).border(if(focused) 2.dp else 0.dp, Accent, RoundedCornerShape(12.dp)).onFocusChanged { focused=it.isFocused }.focusable().tvClick(click).clickable(onClick=click), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(if(compact) 72.dp else 106.dp).clip(RoundedCornerShape(53.dp)).background(Color(0xFF252525)), contentAlignment = Alignment.Center) {
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
    val scale by animateFloatAsState(if (focused) 1.012f else 1f, tween(80, easing = LinearOutSlowInEasing), label = "trailerScale")
    Column(Modifier.width(330.dp).graphicsLayer { scaleX = scale; scaleY = scale }.onFocusChanged { focused = it.isFocused }.focusable().tvClick(onClick).clickable(onClick = onClick)) {
        Box(Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF1A1A1A)).border(if (focused) 2.dp else 0.dp, Accent, RoundedCornerShape(12.dp))) {
            AsyncImage(trailer.thumbnail, trailer.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(Modifier.fillMaxSize().background(Color(0x33000000)), contentAlignment = Alignment.Center) { Text("▶", color = Color.White, fontSize = 31.sp, lineHeight=37.sp) }
        }
        Spacer(Modifier.height(7.dp))
        Text(trailer.name, color = Color.White, fontSize = 13.sp, lineHeight=16.sp, maxLines = 1)
    }
}

@Composable
private fun EpisodeCard(ep: EpisodeSummary, hidden: Boolean, onLongClick: () -> Unit, onClick: () -> Unit) {
    var focused by remember(ep.season, ep.episode) { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.012f else 1f, tween(80, easing = LinearOutSlowInEasing), label = "episodeScale")
    Box(Modifier.width(328.dp).height(225.dp), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.width(316.dp).graphicsLayer { scaleX = scale; scaleY = scale }.onFocusChanged { focused = it.isFocused }.focusable().tvPlaybackClick(onClick, onLongClick).clickable(onClick = onClick)) {
            Box(Modifier.fillMaxWidth().height(176.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF1B1B1B)).border(if (focused) 2.dp else 0.dp, Accent, RoundedCornerShape(12.dp))) {
                EpisodeArtwork(ep,hidden,Modifier.fillMaxSize())
                if (focused) Box(Modifier.fillMaxSize().background(Color(0x26000000)), contentAlignment = Alignment.Center) { Text("▶", color = Color.White, fontSize = 32.sp, lineHeight=38.sp) }
            }
            Spacer(Modifier.height(7.dp))
            Text("S${ep.season} E${ep.episode} · ${if(hidden) tr("No visto","Not watched") else ep.title}", color = Color.White, fontSize = 13.sp, lineHeight=16.sp, fontWeight = FontWeight.Bold, maxLines = 1)
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
                    FocusButton(tr("Actualizar enlaces","Refresh links")) { state.launch { state.refreshPlaybackLinks(selection) } }
                    Spacer(Modifier.width(12.dp))
                    FocusButton(tr("Cerrar","Close")) { state.sourceSelection = null }
                }
                Text(tr("Enlaces guardados hasta 3 horas. Usa Actualizar enlaces si alguno dejó de funcionar.",
                    "Links cached for up to 3 hours. Refresh links if one stops working."),color=Muted,fontSize=14.sp)
                if(state.partyRole==PartyRole.GUEST && state.watchParty?.sourceName?.isNotBlank()==true) {
                    Text(tr("Fuente del host: ","Host source: ")+state.watchParty?.sourceName.orEmpty(),color=Accent,fontSize=14.sp)
                    if(state.watchParty?.shareStream==true) FocusButton(tr("Usar fuente del party","Use party source")) {state.launch {state.socialAction {state.usePartySource()}}}
                }
                if(state.partyRole==PartyRole.GUEST) Text(tr("Este enlace se cambia solo para ti · sigues sincronizado con el host","This link changes only for you · You stay synced with the host"),color=Muted)
                Spacer(Modifier.height(18.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 30.dp)) {
                    items(selection.streams) { stream ->
                        Column {
                        if(state.partyRole==PartyRole.GUEST && com.miflix.native2.data.PartySourcePolicy.key(stream)==state.watchParty?.sourceKey) Text(tr("FUENTE DEL PARTY","PARTY SOURCE"),color=Accent,fontSize=12.sp)
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
}

@Composable
private fun SourceLinkCard(stream: com.miflix.native2.model.StreamChoice,modifier: Modifier,onClick: () -> Unit) {
    var focused by remember(stream) {mutableStateOf(false)}
    var rawDetails by remember(stream) {mutableStateOf(false)}
    val info=remember(stream) {com.miflix.native2.data.StreamPresentation.parse(stream.addonName,stream.name,stream.title,stream.filename,stream.videoSize)}
    val ink=Color.White
    if(rawDetails) Dialog(onDismissRequest={rawDetails=false}) {
        val first=remember {FocusRequester()}
        LaunchedEffect(Unit) {withFrameNanos {};runCatching {first.requestFocus()} }
        Column(Modifier.consumeOpeningPress().widthIn(max=650.dp).heightIn(max=460.dp).background(Panel,RoundedCornerShape(24.dp)).padding(24.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text(info.heading,color=Color.White,fontSize=23.sp)
            Text(listOf(stream.title,stream.filename).filter {it.isNotBlank()}.distinct().joinToString("\n\n"),color=Muted,fontSize=14.sp,modifier=Modifier.contentFocusBorder().focusable().padding(6.dp))
            FocusButton(tr("Cerrar","Close"),modifier=Modifier.focusRequester(first)) {rawDetails=false}
        }
    }
    Column(modifier.onFocusChanged {focused=it.isFocused}.focusable().tvPlaybackClick(onClick,{rawDetails=true}).clickable(onClick=onClick)
        .background(if(focused) FocusedPanel else Color(0xFF252528),RoundedCornerShape(18.dp))
        .border(if(focused) 2.dp else 1.dp,if(focused) Accent else PanelBorder,RoundedCornerShape(18.dp)).padding(20.dp),
        verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text(info.heading,color=ink,fontSize=15.sp,lineHeight=20.sp,fontWeight=FontWeight.Bold,maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        if(info.technologies.isNotEmpty()) FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
            info.technologies.forEach {TechnologyBadge(it,ink)}
        }
        val specs=listOf(info.video,info.audio).filter {it.isNotBlank()}.joinToString("  |  ")
        if(specs.isNotBlank()) Text(specs,color=Color(0xFFE0DDE6),fontSize=13.sp,lineHeight=18.sp,maxLines=2,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        val languages=stream.audioLanguages.map {it.uppercase(java.util.Locale.US)}.distinct().take(5).joinToString(" / ")
        val source=listOf(info.source,languages).filter {it.isNotBlank()}.joinToString(" · ")
        if(source.isNotBlank()) Text(source,color=Muted,fontSize=12.sp,lineHeight=17.sp,maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        if(info.size.isNotBlank()) Text("${tr("Peso","Size")}  ${info.size}",color=ink,fontSize=11.sp,lineHeight=15.sp,fontWeight=FontWeight.Bold,
            modifier=Modifier.background(Color(0x80000000),RoundedCornerShape(4.dp)).padding(horizontal=7.dp,vertical=3.dp))
        if(specs.isBlank() && info.technologies.isEmpty() && source.isBlank()) {
            val fallback=stream.title.lineSequence().firstOrNull {it.isNotBlank() && !it.equals(stream.name,true) && it!=stream.filename}.orEmpty()
            if(fallback.isNotBlank()) Text(fallback,color=Muted,fontSize=12.sp,maxLines=2,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun SeasonSelector(state: AppState, count: Int) {
    val rail=rememberLazyListState(initialFirstVisibleItemIndex=(state.currentSeason-1).coerceAtLeast(0))
    val scope=rememberCoroutineScope()
    var pending by remember {mutableStateOf<kotlinx.coroutines.Job?>(null)}
    DisposableEffect(Unit) {onDispose {pending?.cancel()} }
    LazyRow(Modifier.widthIn(max=680.dp).smoothHorizontalDpad(),state=rail,horizontalArrangement=Arrangement.spacedBy(10.dp),contentPadding=PaddingValues(4.dp)) {
        items((1..count).toList(),key={it}) { season ->
            FocusButton("${tr("Temporada","Season")} $season",primary=state.currentSeason==season,
                onFocused={ focused ->
                    if(focused) {pending?.cancel();pending=scope.launch {delay(240);if(state.currentSeason!=season) state.loadSeason(season)}}
                    else pending?.cancel()
                }) {pending?.cancel();state.launch {state.loadSeason(season)} }
        }
    }
}

@Composable
private fun ActorCreditsDialog(person: CastMember, state: AppState, close: () -> Unit) {
    var credits by remember(person.id) { mutableStateOf<List<MediaSummary>?>(null) }
    var failure by remember(person.id) { mutableStateOf<String?>(null) }
    LaunchedEffect(person.id) {
        try { credits=state.tmdb.actorCredits(person.id) }
        catch(e: kotlinx.coroutines.CancellationException) { throw e }
        catch(e: Exception) { failure=e.message ?: tr("Error al cargar", "Unable to load") }
    }
    Dialog(onDismissRequest=close, properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Column(Modifier.width(850.dp).heightIn(max=580.dp).background(Panel,RoundedCornerShape(20.dp)).padding(24.dp)) {
            Text(person.name, color=Color.White, fontSize=26.sp)
            FocusButton(tr("Cerrar", "Close")) { close() }
            if(credits==null) Text(failure ?: tr("Cargando…", "Loading…"),color=Color.White)
            if(credits?.isEmpty()==true) Text(tr("Sin títulos disponibles", "No titles available"),color=Muted)
            LazyVerticalGrid(columns=GridCells.Adaptive(140.dp),modifier=Modifier.weight(1f,false),
                horizontalArrangement=Arrangement.spacedBy(14.dp),verticalArrangement=Arrangement.spacedBy(14.dp),contentPadding=PaddingValues(bottom=20.dp)) {
                gridItems(credits.orEmpty(),key={it.cloudId}) { title ->
                    MediaCard(title,modifier=Modifier.fillMaxWidth(),showCaption=true,allowContextMenu=false) {close();state.launch {state.open(title)} }
                }
            }
        }
    }
}

@Composable
private fun EpisodeArtwork(ep: EpisodeSummary, hidden: Boolean, modifier: Modifier) {
    Box(modifier.background(Color(0xFF171B24)),contentAlignment=Alignment.Center) {
        if(!hidden) {
            AsyncImage(ep.still,ep.title,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
        } else {
            // Older Android TVs ignore Modifier.blur: never load the spoiler image there.
            if(android.os.Build.VERSION.SDK_INT>=31) {
                AsyncImage(ep.still,null,Modifier.fillMaxSize().blur(32.dp),contentScale=ContentScale.Crop)
            }
            Box(Modifier.fillMaxSize().background(Color(0xDB10131A)))
            Text(tr("No visto","Not watched"),color=Color(0xFFCED1D8),fontSize=14.sp)
        }
    }
}
