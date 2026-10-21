package com.miflix.native2.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import coil3.compose.AsyncImage
import com.miflix.native2.model.MediaSummary
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text

@Composable
internal fun ModernMyListScreenRP63(state:AppState,onBack:()->Unit) {
    BackHandler(onBack=onBack)
    val owner="${state.session?.userId}:${state.activeProfile.id}"
    LaunchedEffect(owner) {runCatching {state.pushCloud()}}
    var type by rememberSaveable(owner) {mutableStateOf("all")}
    var order by rememberSaveable(owner) {mutableStateOf("last_added")}
    val filteredIds=state.favorites.filter {it.matches(Regex("^tmdb:(movie|series):[0-9]+$")) && (type=="all" || it.startsWith("tmdb:$type:"))}
    val ids=if(order=="oldest") filteredIds.reversed() else filteredIds
    LaunchedEffect(owner,ids,state.myListLimit) {state.loadMyListPage(owner,ids)}
    val cached=state.myListTitles.associateBy {it.cloudId}
    val titles=ids.take(state.myListLimit).mapNotNull {cached[it]}
    val filterKey=state.localCatalogKeyRC100(tr("Mi lista","My List"))
    var filters by remember(filterKey) {mutableStateOf(false)}
    if(filters) LocalCatalogFiltersRC100(state,filterKey,titles) {filters=false}
    val visible=localCatalogRC100(state,filterKey,titles)
    val grid=rememberLazyGridState()
    LaunchedEffect(owner,type,order) {grid.scrollToItem(0)}
    BrowseReturnScope("mylist:$owner:$type:$order") {
        AppShell(state,Screen.MY_LIST,{state.screen=it}) {
            BrowsingViewport(state,visible.firstOrNull(),heroHeight=80.dp) {
                Column(Modifier.fillMaxSize().padding(horizontal=16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(tr("Mi lista","My List"),color=Color.White,fontSize=29.sp,fontWeight=FontWeight.Bold)
                            Text(state.activeProfile.name+" · ${visible.size} / ${ids.size} "+tr("títulos","titles"),color=Muted,fontSize=12.sp)
                        }
                        if(state.localExtras.enabled("filters")) ModernButtonRP63(tr("Filtros","Filters"),action={filters=true})
                    }
                    FlowRow(horizontalArrangement=Arrangement.spacedBy(10.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        listOf("all" to tr("Todo","All"),"movie" to tr("Películas","Movies"),"series" to tr("Series","Series")).forEach {(id,label) ->
                            ModernButtonRP63(label,primary=type==id,action={state.myListLimit=15;type=id})
                        }
                        Spacer(Modifier.width(10.dp))
                        ModernButtonRP63(tr("Últimos añadidos","Last added"),primary=order=="last_added",action={state.myListLimit=15;order="last_added"})
                        ModernButtonRP63(tr("Más antiguos","Oldest added"),primary=order=="oldest",action={state.myListLimit=15;order="oldest"})
                    }
                    BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(),contentAlignment=Alignment.TopCenter) {
                        val cellWidth=((maxWidth-56.dp)/5).coerceAtLeast(1.dp)
                        val captionHeight=(82f*LocalDensity.current.fontScale).dp.coerceAtLeast(82.dp)
                        val posterHeight=minOf(cellWidth*1.5f,(maxHeight-captionHeight-20.dp).coerceAtLeast(48.dp))
                        val initialLoading=state.myListLoading || (ids.isNotEmpty() && titles.isEmpty() && state.myListMessage.isBlank())
                        LazyVerticalGrid(GridCells.Fixed(5),state=grid,modifier=Modifier.fillMaxSize().smoothVerticalDpad {!grid.isScrollInProgress},
                            horizontalArrangement=Arrangement.spacedBy(12.dp),verticalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(start=4.dp,end=4.dp,top=6.dp,bottom=14.dp)) {
                            items(visible,key={it.cloudId}) {item ->
                                MyListCardRP64(item,posterHeight,captionHeight,Modifier.fillMaxWidth().then(homeReturnFocus("mylist:$owner:${item.cloudId}")),
                                    state.progress[item.cloudId]?.percent,onLongClick={state.titleActions(item)}) {state.launch {state.open(item)}}
                            }
                            if(titles.isEmpty() && initialLoading) items(minOf(15,ids.size),key={"loading:$it"}) {
                                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0xFF15161B))) {
                                    Box(Modifier.fillMaxWidth().height(posterHeight).background(Color(0xFF202127)))
                                    Column(Modifier.fillMaxWidth().height(captionHeight).padding(10.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                                        Box(Modifier.fillMaxWidth(.85f).height(9.dp).background(Color(0xFF2A2C33)))
                                        Box(Modifier.fillMaxWidth(.55f).height(8.dp).background(Color(0xFF22242A)))
                                    }
                                }
                            }
                            item(span={GridItemSpan(maxLineSpan)}) {
                                Column(Modifier.fillMaxWidth().padding(vertical=12.dp),verticalArrangement=Arrangement.spacedBy(12.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                                    if(state.myListLoading) Text(tr("Cargando tu lista…","Loading your list…"),color=Muted)
                                    else {
                                        if(visible.isEmpty()) Text(if(ids.isEmpty()) tr("Guarda tus próximos favoritos aquí.","Save your next favorites here.") else tr("No hay títulos con estos filtros.","No titles match these filters."),color=Muted)
                                        if(ids.size>state.myListLimit) ModernButtonRP63(tr("Cargar 15 más","Load 15 more"),Modifier.widthIn(max=340.dp).fillMaxWidth(),primary=true,action={state.myListLimit+=15})
                                        if(state.myListMessage.isNotBlank()) {
                                            Text(state.myListMessage,color=Muted,fontSize=12.sp)
                                            ModernButtonRP63(tr("Reintentar","Retry"),action={state.launch {state.loadMyListPage(owner,ids)}})
                                        }
                                    }
                                    ModernButtonRP63(tr("Actualizar lista","Refresh list"),action={state.launch {runCatching {state.pushCloud()}.onFailure {state.myListMessage=it.message.orEmpty()}}})
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** The entire card owns focus, so automatic scrolling includes its title and metadata. */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun MyListCardRP64(item:MediaSummary,posterHeight:androidx.compose.ui.unit.Dp,captionHeight:androidx.compose.ui.unit.Dp,
    modifier:Modifier=Modifier,progress:Double?,onLongClick:()->Unit,onClick:()->Unit) {
    var focused by remember(item.cloudId) {mutableStateOf(false)}
    val preview=LocalBrowsePreview.current
    val shape=RoundedCornerShape(14.dp)
    Column(modifier.onFocusChanged {focused=it.isFocused;if(it.isFocused) preview?.invoke(item)}.focusable()
        .tvPlaybackClick(onClick,onLongClick).clickable(onClick=onClick)
        .clip(shape).background(Color(0xFF111216)).border(if(focused) 2.dp else 1.dp,if(focused) Color.White else ModernBorderRP63,shape)) {
        Box(Modifier.fillMaxWidth().height(posterHeight).background(Color(0xFF0B0C10))) {
            LoadingArtworkRP58(item.poster ?: item.backdrop,item.title,Modifier.fillMaxSize(),contentScale=ContentScale.Fit)
            observeCardFreshness(item)?.let {CardFreshnessTag(it,Modifier.align(Alignment.TopEnd).padding(5.dp).widthIn(max=130.dp))}
            progress?.takeIf {it>0.0 && it<100.0}?.let {value ->
                Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(3.dp).background(Color(0x66555555))) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth((value/100.0).toFloat().coerceIn(0f,1f)).background(Color.White))
                }
            }
        }
        Column(Modifier.fillMaxWidth().height(captionHeight).padding(horizontal=9.dp,vertical=8.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
            Box(Modifier.weight(1f).fillMaxWidth(),contentAlignment=Alignment.CenterStart) {
                Text(item.title,color=Color.White,fontSize=13.sp,lineHeight=16.sp,fontWeight=FontWeight.SemiBold,
                    maxLines=if(focused) 1 else 3,softWrap=!focused,overflow=TextOverflow.Ellipsis,
                    modifier=Modifier.fillMaxWidth().then(if(focused) Modifier.basicMarquee(iterations=Int.MAX_VALUE) else Modifier))
            }
            Text(listOf(item.year,if(item.type=="series") tr("Serie","Series") else tr("Película","Movie")).filter {it.isNotBlank()}.joinToString(" · "),
                color=Muted,fontSize=10.sp,lineHeight=12.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
        }
    }
}
