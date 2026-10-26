package com.miflix.native2.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.miflix.native2.data.*
import kotlinx.coroutines.*

@Composable
internal fun F1StatsPanelRP131(state:AppState,repo:F1StatsRP131,compact:Boolean,close:()->Unit,
    videoFocus:FocusRequester,modifier:Modifier=Modifier) {
    var snapshot by remember {mutableStateOf(repo.cached())}
    var selected by remember {mutableStateOf<String?>(null)}
    var choosing by remember {mutableStateOf(false)}
    var stale by remember {mutableStateOf(false)}
    val sessions=snapshot?.sessions.orEmpty()
    val session=if(selected==null) F1StatsRP131.automatic(sessions) else sessions.firstOrNull {it.id==selected}
    val header=remember {FocusRequester()}
    val firstRow=remember {FocusRequester()}
    val context=LocalContext.current
    val logo=remember(context) {context.resources.getIdentifier("calendar_f1_official_rp123","drawable",context.packageName)}
    LaunchedEffect(state.appForeground) {
        if(!state.appForeground) return@LaunchedEffect
        var failures=0
        while(isActive) {
            try {snapshot=repo.load();stale=false;failures=0}
            catch(e:CancellationException) {throw e} catch(_:Exception) {stale=true;failures++}
            val live=snapshot?.sessions?.any {it.live}==true
            delay(if(failures>0) (15_000L*failures.coerceAtMost(4)) else if(live) 15_000L else 30_000L)
        }
    }
    val phase=when {
        stale->tr("Sin actualizar","Update unavailable")
        session==null->tr("Sin sesión","No session")
        session.live->tr("En vivo","Live")+(if(session.lap>0) " · "+tr("Vuelta ","Lap ")+session.lap else "")
        session.final->"Final · "+session.date.take(10)
        else->tr("Programada","Scheduled")+" · "+session.date.take(10)
    }
    Column(modifier.clip(RoundedCornerShape(10.dp)).background(GlassActionBrushRP108(false,true))
        .border(1.dp,Color(0x554F5E72),RoundedCornerShape(10.dp)).focusGroup().padding(6.dp),
        verticalArrangement=Arrangement.spacedBy(2.dp)) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)) {
            if(logo!=0) AsyncImage(logo,"F1",Modifier.width(31.dp).height(15.dp))
            else Text("F1",color=Color.White,fontSize=13.sp,fontWeight=FontWeight.Bold)
            Text((session?.type.orEmpty()+" · "+phase).trim(' ','·'),color=if(stale) Color(0xFFFFC28A) else Color.White,
                fontSize=10.sp,maxLines=1,overflow=TextOverflow.Ellipsis,modifier=Modifier.weight(1f))
            SplitStatsActionRP130(tr("Sesión","Session"),{choosing=!choosing},Modifier.focusRequester(header),selected=choosing)
            SplitStatsActionRP130("×",close,description=tr("Cerrar estadísticas","Close stats"))
        }
        if(choosing) {
            LazyRow(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                item {SplitStatsActionRP130("Auto",{selected=null;choosing=false;runCatching {header.requestFocus()}},selected=selected==null)}
                items(sessions,key={it.id}) {row->
                    SplitStatsActionRP130(row.type+" · "+row.date.take(10),
                        {selected=row.id;choosing=false;runCatching {header.requestFocus()}},selected=selected==row.id)
                }
            }
        }
        if(!compact) Text(session?.event ?: tr("Fórmula 1","Formula 1"),color=Muted,fontSize=10.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
        if(session==null || session.drivers.isEmpty()) {
            Text(if(stale) tr("No se pudo consultar F1. Reintentaremos.","Could not load F1. Retrying.")
                else if(selected!=null && session==null) tr("Sesión fuera del listado actual; selecciona Auto.","Session no longer listed; choose Auto.")
                else tr("Esperando datos publicados por ESPN…","Waiting for published ESPN data…"),
                color=Muted,fontSize=11.sp,maxLines=2,modifier=Modifier.weight(1f))
        } else {
            val table=remember(session,compact) {StatsTableRP105(if(compact) listOf("POS","GAP") else listOf("POS","LAP","GAP"),
                session.drivers.map {StatsTableRowRP105(it.id,it.name,if(compact) listOf(it.position,it.gap) else listOf(it.position,it.lap,it.gap),photo=it.photo)},1)}
            StatsTableCellsRP105(tr("Piloto","Driver"),table.columns,header=true)
            StatsTableRowsRP105(table,firstRow,header,header,videoFocus,Modifier.weight(1f),rowHeightDp=if(compact) 25 else 30)
            if(!compact) Text(tr("ESPN · datos publicados; — = no disponible","ESPN · published data; — = unavailable"),color=Muted,fontSize=9.sp,maxLines=1)
        }
    }
}

