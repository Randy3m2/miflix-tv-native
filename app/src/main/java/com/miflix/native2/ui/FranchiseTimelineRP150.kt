package com.miflix.native2.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.ui.focus.onFocusChanged
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.miflix.native2.model.MediaSummary
import kotlinx.coroutines.CancellationException

internal fun timelineSagaRP150(brand:String)=when(brand) {"marvel"->"Marvel · Infinity Saga";"star_wars"->"Star Wars · Core journey";"game_of_thrones"->"Game of Thrones";"dc"->"DC · DCEU";else->null}
internal fun timelineRankRP150(brand:String,title:String):Int?=when(brand) {
    "walking_dead" ->when(title) {"Fear the Walking Dead"->1;"The Walking Dead"->2;"Tales of the Walking Dead"->3;"The Walking Dead: World Beyond"->4;"The Walking Dead: The Ones Who Live"->5;"The Walking Dead: Daryl Dixon"->6;"The Walking Dead: Dead City"->7;else->null}
    "game_of_thrones" ->when(title) {"House of the Dragon"->1;"A Knight of the Seven Kingdoms"->2;"Game of Thrones"->3;else->null}
    else ->null
}

@Composable
internal fun FranchiseTimelineRP150(state:AppState,catalog:()->Unit) {
    val brand=state.franchiseBrand
    var chronological by remember(brand) {mutableStateOf(state.cinemaJourneyChronological())}
    val base=remember(brand,state.franchiseItems.toList()) {
        val curated=timelineSagaRP150(brand)?.let {CinemaJourneys[it]}.orEmpty().map {entry ->state.franchiseItems.firstOrNull {it.cloudId==entry.cloud} ?: entry.summary()}
        (curated+state.franchiseItems).distinctBy {it.cloudId}
    }
    val ranks=remember(brand,base) {
        val known=timelineSagaRP150(brand)?.let {CinemaJourneys[it]}.orEmpty().associate {it.cloud to it.story}
        base.associate {it.cloudId to ((if(brand=="game_of_thrones" && it.id==1399) 3 else if(brand=="game_of_thrones" && it.id==94997) 1 else timelineRankRP150(brand,it.title)) ?: known[it.cloudId])}
    }
    val story=chronological && ranks.values.any {it!=null}
    val rows=remember(base,story) {if(story) base.sortedWith(compareBy<MediaSummary> {ranks[it.cloudId] ?: Int.MAX_VALUE}.thenBy {it.year}) else base.sortedWith(compareBy<MediaSummary> {it.releaseDate.ifBlank {it.year.ifBlank {"9999"}}}.thenBy {it.id})}
    val list=key(brand) {rememberLazyListState()}
    val scope=rememberCoroutineScope()
    var spotlight by remember(brand) {mutableStateOf<MediaSummary?>(null)}
    BackHandler {state.screen=Screen.COLLECTIONS}
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xff17263c),Bg))).padding(horizontal=32.dp,vertical=12.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f)) {Text(com.miflix.native2.data.FranchiseCatalogRP29.title(brand),fontSize=32.sp,color=Color.White);Text(tr("Cada historia, conectada","Every story, connected"),fontSize=14.sp,color=Muted)}
                if(ranks.values.any {it!=null}) FocusButton(tr("Cronología","Story order"),primary=story) {if(!story) scope.launch {list.scrollToItem(0)};chronological=true;state.saveCinemaJourneyChronological(true)}
                FocusButton(tr("Estreno","Release order"),primary=!story) {if(story) scope.launch {list.scrollToItem(0)};chronological=false;state.saveCinemaJourneyChronological(false)}
                FocusButton(tr("Catálogo","Catalog"),onClick=catalog)
                FocusButton("×") {state.screen=Screen.COLLECTIONS}
            }
            Text(if(story) tr("Orden curado por inicio de la historia. Las series pueden solaparse; las entradas sin posición verificada quedan al final.","Curated by story beginning. Series can overlap; entries without a verified position appear last.") else tr("Películas y series por fecha de estreno.","Movies and series by release date."),color=Muted,fontSize=12.sp)
            if(state.franchiseLoading) Text(tr("Cargando…","Loading…"),color=Muted)
            LazyRow(state=list,modifier=Modifier.weight(1f).fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(16.dp),contentPadding=PaddingValues(vertical=12.dp,horizontal=6.dp)) {
                itemsIndexed(rows,key={_,item->item.cloudId}) {index,item ->
                    TimelineCardRP150(state,item,index+1,if(story) ranks[item.cloudId] else index+1) {spotlight=it}
                }
                if(state.franchiseMore && !state.franchiseLoading) item {FocusButton(tr("Cargar más","Load more")) {state.launch {state.loadFranchise(state.franchiseType,true)}}}
            }
            val featured=spotlight ?: rows.firstOrNull()
            if(featured!=null) Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                Text(featured.title,color=Color.White,fontSize=20.sp,maxLines=1)
                Text(featured.overview.ifBlank {tr("Selecciona una carátula para ver detalles y reproducir.","Select a poster for details and playback.")},color=Muted,fontSize=12.sp,maxLines=1)
            }
        }
    }
}

@Composable
private fun TimelineCardRP150(state:AppState,item:MediaSummary,index:Int,position:Int?,onSpotlight:(MediaSummary)->Unit) {
    var art by remember(item.cloudId) {mutableStateOf(item)}
    LaunchedEffect(item.cloudId) {
        if(art.poster==null) try {state.tmdb.byCloudId(item.cloudId)?.let {art=it}} catch(e:CancellationException) {throw e} catch(_:Exception) {}
    }
    val progress=state.progress[item.cloudId]
    var focused by remember {mutableStateOf(false)}
    val open={state.launch {state.open(art)};Unit}
    Column(Modifier.width(164.dp).fillMaxHeight(),verticalArrangement=Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            Box(Modifier.weight(1f).height(2.dp).background(Accent.copy(alpha=.5f)))
            Box(Modifier.size(28.dp).background(Accent.copy(alpha=.22f),RoundedCornerShape(50)).border(1.dp,Accent,RoundedCornerShape(50)),contentAlignment=Alignment.Center) {Text(if(position==null) "?" else index.toString(),color=Color.White,fontSize=14.sp)}
            Box(Modifier.weight(1f).height(2.dp).background(Accent.copy(alpha=.5f)))
        }
        Box(Modifier.fillMaxWidth().weight(1f).heightIn(max=240.dp).onFocusChanged {focused=it.isFocused;if(it.isFocused) onSpotlight(art)}.focusable().tvClick(open).clickable(onClick=open).background(Panel,RoundedCornerShape(16.dp)).clip(RoundedCornerShape(16.dp)).border(if(focused) 3.dp else 1.dp,if(focused) Color.White else Color.White.copy(alpha=.1f),RoundedCornerShape(16.dp))) {
            AsyncImage(art.poster ?: art.backdrop,item.title,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
        }
        Text(art.title,color=Color.White,fontSize=14.sp,maxLines=2,minLines=2)
        Text(item.year+" · "+if(position==null) tr("Sin posición verificada","Position unverified") else if((progress?.percent ?: 0.0)>=90 && item.type=="movie") tr("✓ Vista","✓ Watched") else if(progress!=null) tr("En progreso","In progress") else tr("Pendiente","Pending"),fontSize=11.sp,color=Muted,maxLines=1)
        if(progress!=null) Box(Modifier.fillMaxWidth().height(3.dp).background(Color.White.copy(alpha=.12f))) {Box(Modifier.fillMaxWidth((progress.percent/100).toFloat().coerceIn(0f,1f)).fillMaxHeight().background(Accent))}
    }
}
