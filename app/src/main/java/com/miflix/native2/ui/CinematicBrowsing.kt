package com.miflix.native2.ui

import androidx.compose.animation.animateColorAsState

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
    showTitle: Boolean = false,
    eyebrow: String? = null,
    ranked: Boolean = false,
    awardLabels: Boolean = false,
    awardYear: Int? = null,
    onLongClick: ((MediaSummary) -> Unit)? = null,
    onClick: (MediaSummary) -> Unit
) {
    val simple=LocalSimpleHome.current
    val hold=onLongClick ?: LocalTitleActions.current
    val publishPreview=LocalBrowsePreview.current
    val pinned=publishPreview!=null
    val homeShelf=LocalHomeShelfPaging.current
    val more=LocalHomeShelfMore.current
    val shelfKey=LocalHomeShelfKey.current
    val cardRowHeight=if(homeShelf) LocalHomeCardRowHeight.current else 188.dp
    if(rows.isEmpty()) return
    var selectedId by rememberSaveable(title) { mutableStateOf(rows.first().cloudId) }
    var active by remember { mutableStateOf(false) }
    var settledId by remember { mutableStateOf<String?>(null) }
    val bring=remember { BringIntoViewRequester() }
    val railScroll=androidx.compose.foundation.lazy.rememberLazyListState()
    val firstCard=remember {FocusRequester()}
    val cardFocusRP61=remember {mutableMapOf<String,FocusRequester>()}
    LaunchedEffect(selectedId,active) { settledId=null;if(active && !simple) { delay(3000);settledId=selectedId } }
    LaunchedEffect(active) { if(active && !homeShelf) { delay(50); bring.bringIntoView() } }
    val infoVisibility by animateFloatAsState(if(active && settledId==selectedId) 1f else 0f,tween(150),label="railInfo")
    val horizontalAnchor=remember {com.miflix.native2.data.HorizontalFocusAnchor()}
    val horizontalSpec=remember {object: BringIntoViewSpec {
        override fun calculateScrollDistance(offset: Float,size: Float,containerSize: Float): Float =
            horizontalAnchor.distance(offset,size,containerSize,android.os.SystemClock.uptimeMillis())
    } }
    val selected=rows.firstOrNull { it.cloudId==selectedId } ?: rows.first()
    Column(Modifier.fillMaxWidth().height(if(homeShelf) cardRowHeight+(if(eyebrow==null) 40.dp else 60.dp) else if(pinned) {if(eyebrow==null) 232.dp else 252.dp} else if(eyebrow==null) 302.dp else 323.dp)
        .bringIntoViewRequester(bring).onFocusChanged { active=it.hasFocus }.focusGroup()) {
        eyebrow?.let { Text(it,color=Accent,fontSize=11.sp,fontWeight=FontWeight.Bold,
            modifier=Modifier.padding(start=40.dp,bottom=5.dp)) }
        Text(title,color=Color.White,fontSize=if(simple) 27.sp else 22.sp,lineHeight=if(simple) 32.sp else 27.sp,fontWeight=FontWeight.Bold,
            maxLines=1,overflow=TextOverflow.Ellipsis,
            modifier=Modifier.padding(start=40.dp,end=40.dp,bottom=8.dp))
        CompositionLocalProvider(LocalBringIntoViewSpec provides horizontalSpec,LocalHorizontalFocusAnchor provides horizontalAnchor) {
        LazyRow(Modifier.fillMaxWidth().height(cardRowHeight).smoothHorizontalDpad { !railScroll.isScrollInProgress }.firstCardOnDown(railScroll,firstCard,restoreIndex={rows.indexOfFirst {it.cloudId==selectedId}.coerceAtLeast(0)},restoreFocus={index -> rows.getOrNull(index)?.let {cardFocusRP61[it.cloudId]}}),state=railScroll,contentPadding=PaddingValues(horizontal=34.dp,vertical=6.dp)) {
            items(rows,key={it.cloudId},contentType={"spotlight"}) { item ->
                val cardFocus=remember(item.cloudId) {FocusRequester()}
                DisposableEffect(item.cloudId,cardFocus) {
                    cardFocusRP61[item.cloudId]=cardFocus
                    onDispose {if(cardFocusRP61[item.cloudId]===cardFocus) cardFocusRP61.remove(item.cloudId)}
                }
                SpotlightCard(item,expanded=active && selectedId==item.cloudId && settledId==item.cloudId,landscape=landscape,showTitle=showTitle,
                    progress=progressFor(item),rank=if(ranked) rows.indexOf(item)+1 else null,
                    awardLabel=if(awardLabels) com.miflix.native2.data.AwardSelections.label(item.id,item.type,tr("es","en")=="es",awardYear) else null,modifier=Modifier.focusRequester(cardFocus).then(homeReturnFocus("media:$title:${item.cloudId}")),
                    onFocused={selectedId=item.cloudId;publishPreview?.invoke(item)},onLongClick=hold?.let {action -> {action(item)}},onClick={onClick(item)})
            }
            if(homeShelf && more!=null) item(key="shelf_more") {
                LoadMoreTile(modifier=Modifier.width(150.dp).height(cardRowHeight-12.dp)
                    .then(homeReturnFocus("more:$shelfKey"))) {more(shelfKey,title,rows)}
            }
        }
        }
        // Reserved space prevents neighboring rails from jumping when focus changes.
        if(!pinned) Column(Modifier.padding(start=40.dp,end=40.dp,top=4.dp).fillMaxWidth().height(72.dp)
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
private fun SpotlightCard(item: MediaSummary,expanded: Boolean,landscape: Boolean,showTitle: Boolean,progress: Double?,rank:Int?=null,awardLabel:String?=null,modifier: Modifier=Modifier,onFocused: () -> Unit,onLongClick: (() -> Unit)? = null,onClick: () -> Unit) {
    val freshness=observeCardFreshness(item)
    val simple=LocalSimpleHome.current
    var focused by remember(item.cloudId) { mutableStateOf(false) }
    val focusInk by animateColorAsState(if(focused) Accent else Color.Transparent,tween(120),label="posterFocusInk")
    val focusScale by animateFloatAsState(if(focused) 1.012f else 1f,tween(120),label="posterFocusScale")
    val cardBring=remember { BringIntoViewRequester() }
    
    // The measured slot expands with its artwork, so neighboring cards are pushed, never covered.
    val baseWidth=if(landscape) 312f else if(simple) 172f else 130f
    val slotWidth by animateFloatAsState(if(expanded) 334f else baseWidth,
        tween(if(expanded) 220 else 160),label="spotlightSlotWidth")
    LaunchedEffect(expanded) {if(expanded) {delay(230);cardBring.bringIntoView()}}
    var showBackdrop by remember(item.cloudId) { mutableStateOf(landscape) }
    LaunchedEffect(expanded,landscape) {
        if(expanded) { delay(60); showBackdrop=true } else showBackdrop=landscape
    }
    Box(modifier.width(slotWidth.dp).fillMaxHeight().padding(horizontal=6.dp)
        .bringIntoViewRequester(cardBring)
        .onFocusChanged { focused=it.isFocused; if(it.isFocused) onFocused() }.focusable()
        .then(if(onLongClick==null) Modifier.tvClick(onClick) else Modifier.tvPlaybackClick(onClick,onLongClick)).clickable(onClick=onClick)) {
        Box(Modifier.fillMaxSize().graphicsLayer {scaleX=focusScale;scaleY=focusScale}
        .border(if(simple) 3.dp else 2.dp,focusInk,RoundedCornerShape(18.dp))
        .padding(3.dp).clip(RoundedCornerShape(16.dp)).background(Panel)) {
        LoadingArtworkRP58(if(showBackdrop) item.backdrop ?: item.poster else item.poster ?: item.backdrop,
            item.title,Modifier.fillMaxSize(),contentScale=if(simple && !landscape && !expanded) ContentScale.Fit else ContentScale.Crop)
        if(expanded || simple || showTitle || rank!=null || awardLabel!=null) Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent,Color(0xD9050505)))))
        if(expanded || simple || showTitle) Text(item.title,color=Color.White,fontSize=if(simple) 18.sp else if(expanded) 18.sp else 12.sp,lineHeight=if(simple || expanded) 22.sp else 15.sp,
            fontWeight=FontWeight.Bold,maxLines=2,overflow=TextOverflow.Ellipsis,
            modifier=Modifier.align(Alignment.BottomStart).padding(start=10.dp,end=10.dp,top=10.dp,bottom=if(awardLabel!=null && freshness!=null) 68.dp else if(awardLabel!=null || freshness!=null) 38.dp else 10.dp))
        rank?.let {position ->
            // Draw inside the existing clipped artwork: no separate rank tile or changing focus geometry.
            Box(Modifier.align(Alignment.TopStart).size(width=86.dp,height=80.dp)
                .background(Brush.radialGradient(listOf(Color(0xA6000000),Color.Transparent))))
            Text(position.toString(),color=Color.White,fontSize=if(position==10) 46.sp else 56.sp,
                lineHeight=60.sp,fontWeight=FontWeight.Black,
                modifier=Modifier.align(Alignment.TopStart).padding(start=10.dp,top=4.dp))
        }
        awardLabel?.let {label ->
            Text(label,color=Color(0xFFFFDEA0),fontSize=10.sp,lineHeight=13.sp,fontWeight=FontWeight.Bold,
                maxLines=2,overflow=TextOverflow.Ellipsis,
                modifier=Modifier.align(Alignment.BottomStart).fillMaxWidth()
                    .background(Color(0xD90B0B10)).padding(horizontal=8.dp,vertical=7.dp))
        }
        if(freshness!=null) CardFreshnessTag(freshness,
            Modifier.align(Alignment.BottomCenter).padding(start=6.dp,end=6.dp,top=7.dp,bottom=if(awardLabel!=null) 40.dp else 7.dp).widthIn(max=150.dp))
        progress?.takeIf { it>0.0 && it<100.0 }?.let { value ->
            Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(3.dp).background(Color(0x88555555))) {
                Box(Modifier.fillMaxHeight().fillMaxWidth((value/100.0).toFloat().coerceIn(0f,1f)).background(Color.White))
            }
        }
        }
    }
}

/** Remote keyboard and results share one viewport; no navigation to a second page. */
@Composable
internal fun CinematicSearchScreen(state: AppState,onBack: () -> Unit) {
    val easySearchRP80=LocalEasySearchRP80.current
    val owner=state.searchHistoryOwner()
    var query by rememberSaveable(owner) {mutableStateOf(state.searchQuery)}
    var cursor by rememberSaveable(owner) {mutableIntStateOf(query.length)}
    var results by remember {mutableStateOf<List<MediaSummary>>(emptyList())}
    var searching by remember {mutableStateOf(false)}
    var searchError by remember {mutableStateOf(false)}
    val keyboardFocus=remember {FocusRequester()}
    val resultsFocus=remember {FocusRequester()}
    BackHandler(onBack=onBack)
    fun apply(edit: SearchTextEditor.Edit) {query=edit.text;cursor=edit.cursor;state.searchQuery=query}
    fun replace(value: String) {apply(SearchTextEditor.replace(value))}
    fun insert(value: String) {apply(SearchTextEditor.insert(query,cursor,value))}
    fun submit() {state.rememberSearch(query);runCatching {resultsFocus.requestFocus()}}
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
        try {
            voice.launch(intent)
        } catch(e: android.content.ActivityNotFoundException) {
            state.error=tr("Esta TV no tiene un servicio de dictado disponible.","No voice input service is available on this TV.")
        } catch(e: Exception) {
            state.error=tr("No se pudo iniciar el micrófono.","Could not start voice input.")
        }
    }
    LaunchedEffect(Unit) {withFrameNanos { };runCatching {keyboardFocus.requestFocus()}}
    LaunchedEffect(query,state.interfaceLanguage,owner) {
        searchError=false
        if(query.isBlank()) {results=emptyList();searching=false;return@LaunchedEffect}
        results=state.localSearch(query)
        if(query.trim().length<2) {searching=false;return@LaunchedEffect}
        searching=true
        try {
            delay(if(com.miflix.native2.data.TasteQuery.parse(query)!=null) 650 else 350)
            results=state.searchFlexible(query.trim())
        } catch(e: CancellationException) {
            throw e
        } catch(e: Exception) {
            searchError=true
        } finally {searching=false}
    }
    val suggestions=(if(state.kidsMode) state.kidsMovies+state.kidsSeries else state.seasonalItems+state.trending+state.movies+state.series).distinctBy {it.cloudId}.take(30)
    val display=if(query.isBlank()) suggestions else results
    AppShell(state,Screen.SEARCH,{state.screen=it}) {
        Column(Modifier.fillMaxSize().padding(start=34.dp,end=28.dp,top=12.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            // This text box uses the in-app keyboard; focusing it never invokes a covering IME.
            var textFocused by remember {mutableStateOf(false)}
            val textScroll=rememberScrollState()
            var fieldWidth by remember {mutableIntStateOf(0)}
            var searchLayoutRP35 by remember {mutableStateOf<androidx.compose.ui.text.TextLayoutResult?>(null)}
            LaunchedEffect(cursor,query,fieldWidth,searchLayoutRP35) {
                val layout=searchLayoutRP35?.takeIf {it.layoutInput.text.text==query} ?: return@LaunchedEffect
                val caret=layout.getCursorRect(cursor.coerceIn(0,query.length)).left
                textScroll.scrollTo((caret-fieldWidth*.8f).toInt().coerceAtLeast(0))
            }
            Box(Modifier.fillMaxWidth().height(if(easySearchRP80) 56.dp else 46.dp).onFocusChanged {textFocused=it.isFocused}
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
                Text(if(query.isEmpty()) tr("Películas, series…","Movies, series…") else query,
                    color=if(query.isEmpty()) Muted else Color.White,fontSize=if(easySearchRP80) 22.sp else 19.sp,maxLines=1,
                    onTextLayout={searchLayoutRP35=it},
                    modifier=Modifier.fillMaxWidth().onSizeChanged {fieldWidth=it.width}.horizontalScroll(textScroll))
            }
            Row(Modifier.weight(1f).fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(24.dp)) {
                LazyColumn(Modifier.width(if(easySearchRP80) 250.dp else 220.dp).fillMaxHeight(),verticalArrangement=Arrangement.spacedBy(10.dp),contentPadding=PaddingValues(bottom=20.dp)) {
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
                    if(display.isNotEmpty()) item {
                        Column(verticalArrangement=Arrangement.spacedBy(5.dp)) {
                            Text(tr("Sugerencias de títulos","Title suggestions"),color=Muted,fontSize=12.sp)
                            display.take(5).forEach {title ->
                                SearchKey(title.title,Modifier.fillMaxWidth()) {
                                    state.rememberSearch(query);state.launch {state.open(title)}
                                }
                            }
                        }
                    }
                }
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    Text(if(query.isBlank()) tr("Tus recomendaciones de búsqueda","Your Search Recommendations") else tr("Resultados para","Results for")+" “${query.trim()}”",
                        color=Color.White,fontSize=20.sp,lineHeight=24.sp,fontWeight=FontWeight.Bold,maxLines=2,modifier=Modifier.padding(bottom=6.dp))
                    if(!state.easyModeRP79 && com.miflix.native2.data.TasteQuery.parse(query)!=null) Text(tr("Coincidencias por temas, reparto y fotografía disponibles","Matches based on available themes, cast and cinematography"),color=Muted,fontSize=12.sp)
                    if(!state.easyModeRP79 && com.miflix.native2.data.DiscoveryMood.intent(query).isNotBlank()) Text(tr("Selección por ánimo · basada en géneros y tus preferencias","Mood picks · based on genres and your preferences"),color=Muted,fontSize=12.sp)
                    if(searching || searchError) Text(if(searching) tr("Buscando más…","Finding more…") else tr("No pudimos completar la búsqueda.","Could not complete the search."),color=Muted,fontSize=12.sp)
                    if(!searching && !searchError && query.isNotBlank() && display.isEmpty()) Text(tr("Prueba otro título.","Try another title."),color=Muted,fontSize=14.sp)
                    LazyVerticalGrid(GridCells.Adaptive(142.dp),modifier=Modifier.weight(1f).fillMaxWidth().focusRequester(resultsFocus).focusGroup(),
                        horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(top=4.dp,bottom=32.dp)) {
                        gridItems(display,key={it.cloudId}) {item -> MediaCard(item,compact=true,allowContextMenu=!state.easyModeRP79) {state.rememberSearch(query);state.launch {state.open(item)}} }
                    }
                }
            }
        }
    }
}

internal val LocalEasySearchRP80=staticCompositionLocalOf {false}

@Composable
private fun SearchKey(label: String,modifier: Modifier=Modifier,onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val largeRP80=LocalEasySearchRP80.current
    Box(modifier.height(if(largeRP80) 34.dp else 28.dp).onFocusChanged {focused=it.isFocused}.focusable().tvClick(onClick).clickable(onClick=onClick)
        .background(if(focused) Accent else Panel,RoundedCornerShape(6.dp))
        .border(1.dp,if(focused) Accent else PanelBorder,RoundedCornerShape(6.dp)).padding(horizontal=4.dp),
        contentAlignment=Alignment.Center) {
        Text(label,color=if(focused) FocusInk else Color.White,fontSize=if(largeRP80) 16.sp else 13.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
    }
}
