package com.miflix.native2.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.type
import com.miflix.native2.data.SearchTextEditor
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.miflix.native2.model.MediaSummary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

internal fun seasonalTitle(theme: String): String = when(theme) {
    "halloween" -> tr("Halloween · noches de miedo","Halloween · Fright nights")
    "christmas" -> tr("Navidad · historias para compartir","Christmas · Home for the holidays")
    "romance" -> tr("Febrero · historias de amor","February · Love stories")
    "adventure" -> tr("Verano · una gran aventura","Summer · A great escape")
    else -> tr("Descubre algo diferente","Discover something different")
}

/** Only the active row expands. Row height stays fixed, including its synopsis. */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
internal fun SpotlightRail(
    title: String,
    rows: List<MediaSummary>,
    landscape: Boolean = false,
    progressFor: (MediaSummary) -> Double? = { null },
    eyebrow: String? = null,
    onLongClick: ((MediaSummary) -> Unit)? = null,
    onClick: (MediaSummary) -> Unit
) {
    val hold=onLongClick ?: LocalTitleActions.current
    if(rows.isEmpty()) return
    var selectedId by rememberSaveable(title) { mutableStateOf(rows.first().cloudId) }
    var active by remember { mutableStateOf(false) }
    var settledId by remember { mutableStateOf<String?>(null) }
    val bring=remember { BringIntoViewRequester() }
    val railScroll=androidx.compose.foundation.lazy.rememberLazyListState()
    val firstCard=remember {FocusRequester()}
    LaunchedEffect(selectedId,active) { settledId=null;if(active) { delay(3000);settledId=selectedId } }
    LaunchedEffect(active) { if(active) { delay(50); bring.bringIntoView() } }
    val infoVisibility by animateFloatAsState(if(active && settledId==selectedId) 1f else 0f,tween(150),label="railInfo")
    val horizontalSpec=remember {object: BringIntoViewSpec {
        override fun calculateScrollDistance(offset: Float,size: Float,containerSize: Float): Float=when {
            offset<0 -> offset
            offset+size>containerSize -> offset+size-containerSize
            else -> 0f
        }
    } }
    val selected=rows.firstOrNull { it.cloudId==selectedId } ?: rows.first()
    Column(Modifier.fillMaxWidth().height(if(eyebrow==null) 302.dp else 323.dp)
        .bringIntoViewRequester(bring).onFocusChanged { active=it.hasFocus }.focusGroup()) {
        eyebrow?.let { Text(it,color=Accent,fontSize=11.sp,fontWeight=FontWeight.Bold,
            modifier=Modifier.padding(start=40.dp,bottom=5.dp)) }
        Text(title,color=Color.White,fontSize=22.sp,lineHeight=27.sp,fontWeight=FontWeight.Bold,
            modifier=Modifier.padding(start=40.dp,end=40.dp,bottom=8.dp))
        CompositionLocalProvider(LocalBringIntoViewSpec provides horizontalSpec) {
        LazyRow(Modifier.fillMaxWidth().height(188.dp).smoothHorizontalDpad().firstCardOnDown(railScroll,firstCard),state=railScroll,contentPadding=PaddingValues(horizontal=34.dp,vertical=6.dp)) {
            items(rows,key={it.cloudId},contentType={"spotlight"}) { item ->
                SpotlightCard(item,expanded=active && selectedId==item.cloudId && settledId==item.cloudId,landscape=landscape,
                    progress=progressFor(item),modifier=if(item.cloudId==rows.first().cloudId) Modifier.focusRequester(firstCard) else Modifier,
                    onFocused={selectedId=item.cloudId},onLongClick=hold?.let {action -> {action(item)}},onClick={onClick(item)})
            }
        }
        }
        // Reserved space prevents neighboring rails from jumping when focus changes.
        Column(Modifier.padding(start=40.dp,end=40.dp,top=4.dp).fillMaxWidth().height(72.dp)
            .graphicsLayer { alpha=infoVisibility;translationY=(1f-infoVisibility)*6.dp.toPx() }) {
            if(active && settledId==selectedId) {
                Text("${selected.title}  ·  ${selected.year}  ·  ★ ${"%.1f".format(selected.rating)}",
                    color=Color.White,fontSize=14.sp,lineHeight=18.sp,fontWeight=FontWeight.SemiBold,maxLines=1,
                    overflow=TextOverflow.Ellipsis)
                Text(selected.overview,color=Muted,fontSize=13.sp,lineHeight=17.sp,maxLines=2,
                    overflow=TextOverflow.Ellipsis,modifier=Modifier.widthIn(max=740.dp).padding(top=4.dp))
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun SpotlightCard(item: MediaSummary,expanded: Boolean,landscape: Boolean,progress: Double?,modifier: Modifier=Modifier,onFocused: () -> Unit,onLongClick: (() -> Unit)? = null,onClick: () -> Unit) {
    var focused by remember(item.cloudId) { mutableStateOf(false) }
    val cardBring=remember { BringIntoViewRequester() }
    LaunchedEffect(expanded) { if(expanded) { delay(170);cardBring.bringIntoView() } }
    // Width animates only inside this bounded LazyRow, never the whole screen.
    val width by animateFloatAsState(if(expanded) 322f else if(landscape) 240f else 132f,
        tween(150,easing=LinearOutSlowInEasing),label="spotlightWidth")
    var showBackdrop by remember(item.cloudId) { mutableStateOf(landscape) }
    LaunchedEffect(expanded,landscape) {
        if(expanded) { delay(60); showBackdrop=true } else showBackdrop=landscape
    }
    Box(modifier.width(width.dp).fillMaxHeight().padding(horizontal=6.dp).bringIntoViewRequester(cardBring)
        .onFocusChanged { focused=it.isFocused; if(it.isFocused) onFocused() }.focusable()
        .then(if(onLongClick==null) Modifier.tvClick(onClick) else Modifier.tvPlaybackClick(onClick,onLongClick)).clickable(onClick=onClick)
        .border(if(focused) 2.dp else 1.dp,if(focused) Accent else PanelBorder,RoundedCornerShape(9.dp))
        .padding(3.dp).clip(RoundedCornerShape(7.dp)).background(Panel)) {
        AsyncImage(if(showBackdrop) item.backdrop ?: item.poster else item.poster ?: item.backdrop,
            item.title,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent,Color(0xD9050505)))))
        Text(item.title,color=Color.White,fontSize=if(expanded) 18.sp else 12.sp,lineHeight=if(expanded) 22.sp else 15.sp,
            fontWeight=FontWeight.Bold,maxLines=2,overflow=TextOverflow.Ellipsis,
            modifier=Modifier.align(Alignment.BottomStart).padding(10.dp))
        progress?.takeIf { it>0.0 && it<100.0 }?.let { value ->
            Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(3.dp).background(Color(0x88555555))) {
                Box(Modifier.fillMaxHeight().fillMaxWidth((value/100.0).toFloat().coerceIn(0f,1f)).background(Color.White))
            }
        }
    }
}

/** Remote keyboard and results share one viewport; no navigation to a second page. */
@Composable
internal fun CinematicSearchScreen(state: AppState,onBack: () -> Unit) {
    val owner=state.searchHistoryOwner()
    var query by rememberSaveable(owner) {mutableStateOf(state.searchQuery)}
    var cursor by rememberSaveable(owner) {mutableIntStateOf(query.length)}
    var results by remember {mutableStateOf<List<MediaSummary>>(emptyList())}
    var searching by remember {mutableStateOf(false)}
    var searchError by remember {mutableStateOf(false)}
    var history by remember(owner) {mutableStateOf(state.searchHistory())}
    val keyboardFocus=remember {FocusRequester()}
    val resultsFocus=remember {FocusRequester()}
    BackHandler(onBack=onBack)
    fun apply(edit: SearchTextEditor.Edit) {query=edit.text;cursor=edit.cursor;state.searchQuery=query}
    fun replace(value: String) {apply(SearchTextEditor.replace(value))}
    fun insert(value: String) {apply(SearchTextEditor.insert(query,cursor,value))}
    fun submit() {state.rememberSearch(query);history=state.searchHistory();runCatching {resultsFocus.requestFocus()}}
    val voice=androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()) { result ->
        if(result.resultCode==android.app.Activity.RESULT_OK) {
            result.data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.takeIf {it.isNotBlank()}?.let {replace(it)}
        }
    }
    fun dictate() {
        val intent=android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL,android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE,if(state.interfaceLanguage=="es") "es-DO" else "en-US")
            .putExtra(android.speech.RecognizerIntent.EXTRA_MAX_RESULTS,1)
            .putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT,tr("Di el título","Say the title"))
        try {voice.launch(intent)}
        catch(_: android.content.ActivityNotFoundException) {state.error=tr("Esta TV no tiene un servicio de dictado disponible.","No voice input service is available on this TV.")}
        catch(_: Exception) {state.error=tr("No se pudo iniciar el micrófono.","Could not start voice input.")}
    }
    LaunchedEffect(Unit) {withFrameNanos { };runCatching {keyboardFocus.requestFocus()}}
    LaunchedEffect(query,state.interfaceLanguage) {
        searchError=false
        if(query.trim().length<2) {results=emptyList();searching=false;return@LaunchedEffect}
        results=state.localSearch(query);searching=true
        try {delay(250);results=state.searchFlexible(query.trim())}
        catch(e: CancellationException) {throw e}
        catch(_: Exception) {searchError=true}
        finally {searching=false}
    }
    val suggestions=(if(state.kidsMode) state.kidsMovies+state.kidsSeries else state.seasonalItems+state.trending+state.movies+state.series).distinctBy {it.cloudId}.take(30)
    val display=if(query.trim().length<2) suggestions else results
    AppShell(state,Screen.SEARCH,{state.screen=it}) {
        Column(Modifier.fillMaxSize().padding(start=34.dp,end=28.dp,top=12.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            // This text box uses the in-app keyboard; focusing it never invokes a covering IME.
            var textFocused by remember {mutableStateOf(false)}
            val textScroll=rememberScrollState()
            val density=androidx.compose.ui.platform.LocalDensity.current
            var fieldWidth by remember {mutableIntStateOf(0)}
            LaunchedEffect(cursor,query,fieldWidth) {
                val caret=with(density) {(cursor*10).dp.toPx()}
                textScroll.scrollTo((caret-fieldWidth*.8f).toInt().coerceAtLeast(0))
            }
            Box(Modifier.fillMaxWidth().height(46.dp).onFocusChanged {textFocused=it.isFocused}
                .onPreviewKeyEvent {event ->
                    val code=event.nativeKeyEvent.keyCode
                    if(code==21 || code==22 || code==67) {
                        if(event.type==KeyEventType.KeyDown) {
                            if(code==67) apply(SearchTextEditor.backspace(query,cursor)) else cursor=SearchTextEditor.move(query,cursor,if(code==21) -1 else 1)
                        };true
                    } else if(event.type==KeyEventType.KeyDown && event.nativeKeyEvent.unicodeChar>=32 && !event.nativeKeyEvent.isCtrlPressed) {
                        insert(String(Character.toChars(event.nativeKeyEvent.unicodeChar)));true
                    } else false
                }.focusable().tvClick {keyboardFocus.requestFocus()}.clickable {keyboardFocus.requestFocus()}
                .background(Panel,RoundedCornerShape(8.dp)).border(if(textFocused) 2.dp else 1.dp,if(textFocused) Accent else PanelBorder,RoundedCornerShape(8.dp))
                .padding(horizontal=14.dp),contentAlignment=Alignment.CenterStart) {
                Text(androidx.compose.ui.text.buildAnnotatedString {
                    if(query.isEmpty()) append(tr("Películas, series…","Movies, series…"))
                    else {
                        val safe=cursor.coerceIn(0,query.length)
                        append(query.substring(0,safe))
                        pushStyle(androidx.compose.ui.text.SpanStyle(color=Accent));append("│");pop()
                        append(query.substring(safe))
                    }
                },color=if(query.isEmpty()) Muted else Color.White,fontSize=19.sp,maxLines=1,
                    modifier=Modifier.fillMaxWidth().onSizeChanged {fieldWidth=it.width}.horizontalScroll(textScroll))
            }
            Row(Modifier.weight(1f).fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(24.dp)) {
                LazyColumn(Modifier.width(220.dp).fillMaxHeight(),verticalArrangement=Arrangement.spacedBy(10.dp),contentPadding=PaddingValues(bottom=20.dp)) {
                    item {
                        Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
                            "abcdefghijklmnopqrstuvwxyz1234567890".chunked(6).forEachIndexed { row,letters ->
                                Row(horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                                    letters.forEachIndexed { col,letter -> SearchKey(letter.toString(),Modifier.weight(1f)
                                        .then(if(row==0 && col==0) Modifier.focusRequester(keyboardFocus) else Modifier)) {insert(letter.toString())} }
                                }
                            }
                            Row(horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                                SearchKey(tr("Espacio","Space"),Modifier.weight(2f)) {insert(" ")}
                                SearchKey("⌫",Modifier.weight(1f)) {apply(SearchTextEditor.backspace(query,cursor))}
                                SearchKey("←",Modifier.weight(1f)) {cursor=SearchTextEditor.move(query,cursor,-1)}
                                SearchKey("→",Modifier.weight(1f)) {cursor=SearchTextEditor.move(query,cursor,1)}
                            }
                            Row(horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                                SearchKey(tr("Voz","Voice"),Modifier.weight(1f)) {dictate()}
                                SearchKey(tr("Borrar","Clear"),Modifier.weight(1f)) {replace("")}
                                SearchKey(tr("Ver →","View →"),Modifier.weight(1f)) {submit()}
                            }
                        }
                    }
                    if(history.isNotEmpty()) item {
                        Column(verticalArrangement=Arrangement.spacedBy(5.dp)) {
                            Text(tr("Recientes","Recent searches"),color=Muted,fontSize=12.sp)
                            history.take(5).forEach {previous -> SearchKey(previous,Modifier.fillMaxWidth()) {replace(previous)} }
                        }
                    } else if(state.kidsMode) item {
                        Column(verticalArrangement=Arrangement.spacedBy(5.dp)) {
                            Text(tr("Explora Kids","Explore Kids"),color=Muted,fontSize=12.sp)
                            state.kidsMovies.take(3).forEach {item -> SearchKey(item.title,Modifier.fillMaxWidth()) {replace(item.title)} }
                        }
                    } else item {
                        Column(verticalArrangement=Arrangement.spacedBy(5.dp)) {
                            SearchKey(seasonalTitle(state.seasonalTheme),Modifier.fillMaxWidth()) {
                                replace(when(state.seasonalTheme) {"halloween"->"Halloween";"christmas"->"Christmas";else->"Adventure"})
                            }
                            state.genres.take(5).forEach {genre -> SearchKey(genre.title,Modifier.fillMaxWidth()) {state.launch {state.openGenre(genre)}} }
                        }
                    }
                }
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    Text(if(query.trim().length<2) tr("Tus recomendaciones de búsqueda","Your Search Recommendations") else tr("Resultados para","Results for")+" “${query.trim()}”",
                        color=Color.White,fontSize=20.sp,lineHeight=24.sp,fontWeight=FontWeight.Bold,maxLines=2,modifier=Modifier.padding(bottom=6.dp))
                    if(searching || searchError) Text(if(searching) tr("Buscando más…","Finding more…") else tr("No pudimos completar la búsqueda.","Could not complete the search."),color=Muted,fontSize=12.sp)
                    if(!searching && !searchError && query.trim().length>=2 && display.isEmpty()) Text(tr("Prueba otro título.","Try another title."),color=Muted,fontSize=14.sp)
                    LazyVerticalGrid(GridCells.Adaptive(142.dp),modifier=Modifier.weight(1f).fillMaxWidth().focusRequester(resultsFocus).focusGroup(),
                        horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(top=4.dp,bottom=32.dp)) {
                        gridItems(display,key={it.cloudId}) {item -> MediaCard(item,compact=true) {state.rememberSearch(query);state.launch {state.open(item)}} }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchKey(label: String,modifier: Modifier=Modifier,onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(modifier.height(28.dp).onFocusChanged {focused=it.isFocused}.focusable().tvClick(onClick).clickable(onClick=onClick)
        .background(if(focused) Accent else Panel,RoundedCornerShape(6.dp))
        .border(1.dp,if(focused) Accent else PanelBorder,RoundedCornerShape(6.dp)).padding(horizontal=4.dp),
        contentAlignment=Alignment.Center) {
        Text(label,color=if(focused) FocusInk else Color.White,fontSize=13.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
    }
}
