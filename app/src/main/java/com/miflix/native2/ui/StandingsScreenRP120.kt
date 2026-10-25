package com.miflix.native2.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.miflix.native2.data.*
import kotlinx.coroutines.*
import java.time.LocalDate

@Composable
internal fun StandingsScreenRP120(initialLeagueRP121:String="NBA",embeddedRP121:Boolean=false,close:()->Unit) {
    val context=LocalContext.current.applicationContext
    var league by remember(initialLeagueRP121) {mutableStateOf(initialLeagueRP121)}
    val today=LocalDate.now()
    fun currentSeason(id:String)=when(id) {"NBA"->today.year+if(today.monthValue>=7) 1 else 0;"LIDOM"->today.year-if(today.monthValue<7) 1 else 0;else->today.year}
    var season by remember {mutableIntStateOf(currentSeason(league))}
    var groups by remember {mutableStateOf<List<StandingGroupRP120>>(emptyList())}
    var groupIndex by remember {mutableIntStateOf(0)}
    var loading by remember {mutableStateOf(false)}
    var message by remember {mutableStateOf("")}
    var refresh by remember {mutableIntStateOf(0)}
    val scroll=rememberLazyListState()
    LaunchedEffect(league,season,refresh) {
        loading=true;message="";groups=emptyList();groupIndex=0
        try {
            val result=withContext(Dispatchers.IO) {StandingsRP120.load(context,league,season,refresh>0)}
            groups=result.first
            if(result.second) message=tr("Sin conexión · última tabla guardada","Offline · last saved table")
        } catch(e:CancellationException) {throw e} catch(_:Exception) {message=tr("No se pudo cargar. Reintenta.","Could not load. Try again.")}
        finally {loading=false}
    }
    LaunchedEffect(groupIndex,league,season) {scroll.scrollToItem(0)}
    val contentRP121:@Composable ()->Unit = {
        Column(Modifier.fillMaxSize().background(Bg).padding(if(embeddedRP121) 0.dp else 28.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                Text(tr("Tabla de posiciones","Standings"),color=Color.White,fontSize=26.sp,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f))
                if(!embeddedRP121) FocusButton("×",horizontalPadding=16.dp, onClick=close)
            }
            Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                if(!embeddedRP121) listOf("NBA","MLB","F1","LIDOM").forEach {id ->FocusButton(id,primary=id==league) {league=id;season=currentSeason(id);refresh=0}}
                FocusButton("‹",horizontalPadding=14.dp) {if(season>currentSeason(league)-5) {season--;refresh=0}}
                Text(if(league in listOf("NBA","LIDOM")) {
                    if(league=="NBA") "${season-1}–$season" else "$season–${season+1}"
                } else season.toString(),color=Color.White,modifier=Modifier.padding(top=12.dp))
                FocusButton("›",horizontalPadding=14.dp) {if(season<currentSeason(league)) {season++;refresh=0}}
                FocusButton(tr("Actualizar","Refresh")) {refresh++}
            }
            if(groups.isNotEmpty()) Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                groups.forEachIndexed {i,g ->FocusButton(when(g.title) {"Eastern Conference"->tr("Conferencia Este","Eastern Conference");"Western Conference"->tr("Conferencia Oeste","Western Conference");"Drivers"->tr("Pilotos","Drivers");"Constructors"->tr("Constructores","Constructors");else->g.title},primary=i==groupIndex,horizontalPadding=12.dp) {groupIndex=i}}
            }
            if(loading || message.isNotBlank()) Text(if(loading) tr("Cargando…","Loading…") else message,color=Muted)
            val group=groups.getOrNull(groupIndex)
            if(group!=null) {
                Row(Modifier.fillMaxWidth().padding(horizontal=14.dp,vertical=8.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    Text("#",color=Muted,modifier=Modifier.width(32.dp))
                    Text(tr("Equipo","Team"),color=Muted,modifier=Modifier.weight(1f))
                    group.columns.forEach {Text(it,color=Color.White,fontWeight=FontWeight.Bold,modifier=Modifier.width(70.dp))}
                }
                LazyColumn(Modifier.weight(1f).fillMaxWidth(),state=scroll,contentPadding=PaddingValues(bottom=24.dp),verticalArrangement=Arrangement.spacedBy(3.dp)) {
                    itemsIndexed(group.rows,key={_,row ->row.id}) {_,row ->
                        var focused by remember(row.id) {mutableStateOf(false)}
                        Row(Modifier.fillMaxWidth().heightIn(min=54.dp).onFocusChanged {focused=it.isFocused}.focusable()
                            .background(GlassActionBrushRP108(focused),RoundedCornerShape(10.dp)).padding(horizontal=14.dp,vertical=9.dp),
                            verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                            Text(row.rank,color=Color.White,modifier=Modifier.width(32.dp))
                            EventLogoRP120(row.logo,row.name,Modifier.size(34.dp))
                            Text(row.name,color=Color.White,fontSize=16.sp,fontWeight=if(focused) FontWeight.Bold else FontWeight.Medium,modifier=Modifier.weight(1f),maxLines=2)
                            row.values.forEach {Text(it,color=Color.White,fontSize=15.sp,modifier=Modifier.width(70.dp))}
                        }
                    }
                }
                if(group.rows.isEmpty()) Text(tr("Todavía no hay posiciones para esta temporada","No standings available for this season yet"),color=Muted)
            }
            Text(tr("NBA: ESPN · MLB/LIDOM: MLB · F1: Jolpica. Caché 6 h.","NBA: ESPN · MLB/LIDOM: MLB · F1: Jolpica. 6-hour cache."),color=Muted,fontSize=10.sp)
        }
    }
    if(embeddedRP121) contentRP121() else PlayerMenuDialog(onDismissRequest=close,fullScreen=true) {contentRP121()}
}
