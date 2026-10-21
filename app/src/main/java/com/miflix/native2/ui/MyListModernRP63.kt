package com.miflix.native2.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
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
    val filteredIds=state.favorites.filter {type=="all" || it.startsWith("tmdb:$type:")}
    val ids=if(order=="oldest") filteredIds.reversed() else filteredIds
    LaunchedEffect(owner,ids,state.myListLimit) {state.loadMyListPage(owner,ids)}
    val titles=state.myListTitles.filter {type=="all" || it.type==type}
    val filterKey=state.localCatalogKeyRC100(tr("Mi lista","My List"))
    var filters by remember(filterKey) {mutableStateOf(false)}
    if(filters) LocalCatalogFiltersRC100(state,filterKey,titles) {filters=false}
    val visible=localCatalogRC100(state,filterKey,titles)
    val grid=rememberLazyGridState()
    LaunchedEffect(owner,type,order) {grid.scrollToItem(0)}
    BrowseReturnScope("mylist:$owner:$type:$order") {
        AppShell(state,Screen.MY_LIST,{state.screen=it}) {
            BrowsingViewport(state,visible.firstOrNull(),heroHeight=108.dp) {
                Column(Modifier.fillMaxSize().padding(horizontal=30.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(tr("Mi lista","My List"),color=Color.White,fontSize=29.sp,fontWeight=FontWeight.Bold)
                            Text(state.activeProfile.name+" · ${visible.size} / ${ids.size} "+tr("títulos","titles"),color=Muted,fontSize=12.sp)
                        }
                        if(state.localExtras.enabled("filters")) ModernButtonRP63(tr("Filtros","Filters"),action={filters=true})
                    }
                    FlowRow(horizontalArrangement=Arrangement.spacedBy(10.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        listOf("all" to tr("Todo","All"),"movie" to tr("Películas","Movies"),"series" to tr("Series","Series")).forEach {(id,label) ->
                            ModernButtonRP63(label,primary=type==id,action={state.myListLimit=16;type=id})
                        }
                        Spacer(Modifier.width(10.dp))
                        ModernButtonRP63(tr("Últimos añadidos","Last added"),primary=order=="last_added",action={state.myListLimit=16;order="last_added"})
                        ModernButtonRP63(tr("Más antiguos","Oldest added"),primary=order=="oldest",action={state.myListLimit=16;order="oldest"})
                    }
                    Box(Modifier.weight(1f).fillMaxWidth(),contentAlignment=Alignment.TopCenter) {
                        LazyVerticalGrid(GridCells.Fixed(4),state=grid,modifier=Modifier.fillMaxHeight().widthIn(max=732.dp).fillMaxWidth().smoothVerticalDpad {!grid.isScrollInProgress},
                            horizontalArrangement=Arrangement.spacedBy(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp),contentPadding=PaddingValues(top=8.dp,bottom=36.dp)) {
                            items(visible,key={it.cloudId}) {item ->
                                MediaCard(item,modifier=Modifier.fillMaxWidth().then(homeReturnFocus("mylist:$owner:${item.cloudId}")),
                                    progressPercent=state.progress[item.cloudId]?.percent,onLongClick={state.titleActions(item)}) {state.launch {state.open(item)}}
                            }
                            item(span={GridItemSpan(maxLineSpan)}) {
                                Column(Modifier.fillMaxWidth().padding(vertical=12.dp),verticalArrangement=Arrangement.spacedBy(12.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                                    if(state.myListLoading) Text(tr("Cargando tu lista…","Loading your list…"),color=Muted)
                                    else {
                                        if(visible.isEmpty()) Text(if(ids.isEmpty()) tr("Guarda tus próximos favoritos aquí.","Save your next favorites here.") else tr("No hay títulos con estos filtros.","No titles match these filters."),color=Muted)
                                        if(ids.size>state.myListLimit) ModernButtonRP63(tr("Cargar 16 más","Load 16 more"),Modifier.widthIn(max=340.dp).fillMaxWidth(),primary=true,action={state.myListLimit+=16})
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
