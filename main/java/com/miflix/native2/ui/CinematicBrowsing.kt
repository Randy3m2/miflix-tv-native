package com.miflix.native2.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
    onClick: (MediaSummary) -> Unit
) {
    if(rows.isEmpty()) return
    var selectedId by rememberSaveable(title) { mutableStateOf(rows.first().cloudId) }
    var active by remember { mutableStateOf(false) }
    var settledId by remember { mutableStateOf(selectedId) }
    val bring=remember { BringIntoViewRequester() }
    LaunchedEffect(selectedId,active) { if(active) { delay(60); settledId=selectedId } }
    LaunchedEffect(active) { if(active) { delay(50); bring.bringIntoView() } }
    val infoVisibility by animateFloatAsState(if(active) 1f else 0f,tween(150),label="railInfo")
    val selected=rows.firstOrNull { it.cloudId==settledId } ?: rows.first()
    Column(Modifier.fillMaxWidth().height(if(eyebrow==null) 302.dp else 323.dp)
        .bringIntoViewRequester(bring).onFocusChanged { active=it.hasFocus }.focusGroup()) {
        eyebrow?.let { Text(it,color=Accent,fontSize=11.sp,fontWeight=FontWeight.Bold,
            modifier=Modifier.padding(start=40.dp,bottom=5.dp)) }
        Text(title,color=Color.White,fontSize=22.sp,lineHeight=27.sp,fontWeight=FontWeight.Bold,
            modifier=Modifier.padding(start=40.dp,end=40.dp,bottom=8.dp))
        LazyRow(Modifier.fillMaxWidth().height(188.dp),contentPadding=PaddingValues(horizontal=34.dp,vertical=6.dp)) {
            items(rows,key={it.cloudId},contentType={"spotlight"}) { item ->
                SpotlightCard(item,expanded=active && selectedId==item.cloudId,landscape=landscape,
                    progress=progressFor(item),onFocused={selectedId=item.cloudId},onClick={onClick(item)})
            }
        }
        // Reserved space prevents neighboring rails from jumping when focus changes.
        Column(Modifier.padding(start=40.dp,end=40.dp,top=4.dp).fillMaxWidth().height(72.dp)
            .graphicsLayer { alpha=infoVisibility;translationY=(1f-infoVisibility)*6.dp.toPx() }) {
            if(active) {
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
private fun SpotlightCard(item: MediaSummary,expanded: Boolean,landscape: Boolean,progress: Double?,onFocused: () -> Unit,onClick: () -> Unit) {
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
    Box(Modifier.width(width.dp).fillMaxHeight().padding(horizontal=6.dp).bringIntoViewRequester(cardBring)
        .onFocusChanged { focused=it.isFocused; if(it.isFocused) onFocused() }.focusable()
        .tvClick(onClick).clickable(onClick=onClick)
        .border(if(focused) 2.dp else 1.dp,if(focused) Color.White else PanelBorder,RoundedCornerShape(9.dp))
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
    var query by rememberSaveable(owner) { mutableStateOf(state.searchQuery) }
    var results by remember { mutableStateOf<List<MediaSummary>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf(false) }
    var history by remember(owner) { mutableStateOf(state.searchHistory()) }
    val keyboardFocus=remember { FocusRequester() }
    val resultsFocus=remember { FocusRequester() }
    BackHandler(onBack=onBack)
    fun update(value: String) { query=value.take(100);state.searchQuery=query }
    fun submit() { state.rememberSearch(query);history=state.searchHistory();runCatching { resultsFocus.requestFocus() } }
    LaunchedEffect(Unit) { delay(150);runCatching { keyboardFocus.requestFocus() } }
    LaunchedEffect(query,state.interfaceLanguage) {
        searchError=false
        if(query.trim().length<2) { results=emptyList();searching=false;return@LaunchedEffect }
        results=emptyList();searching=true
        try { delay(450);results=state.searchFlexible(query.trim()) }
        catch(e: CancellationException) { throw e }
        catch(_: Exception) { searchError=true }
        finally { searching=false }
    }
    val suggestions=(state.seasonalItems+state.trending+state.movies+state.series).distinctBy { it.cloudId }.take(30)
    val display=if(query.trim().length<2) suggestions else results
    AppShell(state,Screen.SEARCH,{state.screen=it}) {
        Row(Modifier.fillMaxSize().padding(start=34.dp,end=28.dp,top=20.dp),horizontalArrangement=Arrangement.spacedBy(26.dp)) {
            LazyColumn(Modifier.width(262.dp).fillMaxHeight(),verticalArrangement=Arrangement.spacedBy(12.dp),
                contentPadding=PaddingValues(bottom=24.dp)) {
                item { Text(tr("Buscar","Search"),color=Color.White,fontSize=30.sp,fontWeight=FontWeight.Black) }
                item { NativeTextField(query,{update(it)},tr("Películas, series…","Movies, series…"),
                    modifier=Modifier.fillMaxWidth(),onSubmit={submit()}) }
                item {
                    Column(verticalArrangement=Arrangement.spacedBy(5.dp)) {
                        "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".chunked(6).forEachIndexed { row,letters ->
                            Row(horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                                letters.forEachIndexed { col,letter ->
                                    SearchKey(letter.toString(),Modifier.weight(1f).then(if(row==0 && col==0) Modifier.focusRequester(keyboardFocus) else Modifier)) { update(query+letter.lowercaseChar()) }
                                }
                            }
                        }
                        Row(horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                            SearchKey(tr("Espacio","Space"),Modifier.weight(1f)) { update(query+" ") }
                            SearchKey("⌫",Modifier.weight(1f)) { update(query.dropLast(1)) }
                            SearchKey(tr("Borrar","Clear"),Modifier.weight(1f)) { update("") }
                        }
                        SearchKey(tr("Ver resultados →","View results →"),Modifier.fillMaxWidth()) { submit() }
                    }
                }
                if(history.isNotEmpty()) item {
                    Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {
                        Text(tr("Recientes","Recent searches"),color=Muted,fontSize=12.sp)
                        history.take(5).forEach { previous -> SearchKey(previous,Modifier.fillMaxWidth()) {update(previous)} }
                    }
                }
            }
            Column(Modifier.weight(1f).fillMaxHeight()) {
                Text(when {
                    searching -> tr("Buscando…","Searching…")
                    searchError -> tr("No pudimos buscar. Intenta de nuevo.","Search unavailable. Try again.")
                    query.trim().length<2 -> tr("Descubre tu próxima historia","Find your next story")
                    else -> tr("Resultados para","Results for")+" “${query.trim()}”"
                },color=Color.White,fontSize=19.sp,lineHeight=24.sp,fontWeight=FontWeight.Bold,maxLines=2,
                    modifier=Modifier.padding(bottom=12.dp))
                if(!searching && query.trim().length>=2 && display.isEmpty()) {
                    Text(tr("Prueba otro título o revisa la escritura.","Try another title or check the spelling."),color=Muted,fontSize=14.sp)
                }
                LazyVerticalGrid(GridCells.Adaptive(142.dp),modifier=Modifier.weight(1f).fillMaxWidth()
                    .focusRequester(resultsFocus).focusGroup(),horizontalArrangement=Arrangement.spacedBy(8.dp),
                    verticalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(top=4.dp,bottom=32.dp)) {
                    gridItems(display,key={it.cloudId}) { item -> MediaCard(item,compact=true) {
                        state.rememberSearch(query);state.launch { state.open(item) }
                    } }
                }
            }
        }
    }
}

@Composable
private fun SearchKey(label: String,modifier: Modifier=Modifier,onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(modifier.height(34.dp).onFocusChanged {focused=it.isFocused}.focusable().tvClick(onClick).clickable(onClick=onClick)
        .background(if(focused) Color.White else Panel,RoundedCornerShape(6.dp))
        .border(1.dp,if(focused) Color.White else PanelBorder,RoundedCornerShape(6.dp)).padding(horizontal=4.dp),
        contentAlignment=Alignment.Center) {
        Text(label,color=if(focused) Color.Black else Color.White,fontSize=13.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
    }
}
