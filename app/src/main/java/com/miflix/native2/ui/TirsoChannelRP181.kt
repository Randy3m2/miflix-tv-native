package com.miflix.native2.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.miflix.native2.data.Tirso247ScheduleRP174
import com.miflix.native2.data.TirsoScheduleItemRP174
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Calendar

@Composable
internal fun TirsoChannelRP181(state:AppState,mobile:Boolean,modifier:Modifier=Modifier) {
    val owner=state.localExtrasScope()
    var now by remember(owner) {mutableStateOf(Tirso247ScheduleRP174.current())}
    var guide by remember {mutableStateOf(false)}
    var ready by remember(now?.id,owner) {mutableStateOf(false)}
    var checking by remember(now?.id,owner) {mutableStateOf(true)}
    var playing by remember {mutableStateOf(false)}
    var retry by remember {mutableIntStateOf(0)}
    LaunchedEffect(owner) {while(true) {now=Tirso247ScheduleRP174.current();delay(30_000)}}
    LaunchedEffect(owner,now?.id,retry) {
        checking=true
        try {ready=now?.let {withTimeoutOrNull(45_000) {state.verifyTirsoItemRP180(it)}}==true}
        finally {checking=false}
    }
    Column(modifier.fillMaxWidth().themeSurfaceRP181().padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text("TIRSO 24/7",color=Accent,fontSize=18.sp,fontWeight=FontWeight.Bold)
        val current=now
        val target=current?.let {if(ready) state.tirsoTargetRP181(it) else null}
        if(target?.item?.backdrop!=null) AsyncImage(target.item.backdrop,null,Modifier.fillMaxWidth().height(if(mobile) 100.dp else 72.dp),contentScale=ContentScale.Crop)
        Text(when {checking->tr("Comprobando el programa actual…","Checking the current program…");ready->current?.title.orEmpty();else->tr("Programa sin enlaces disponibles","Current program has no available sources")},color=Color.White,fontSize=14.sp,maxLines=2)
        if(current!=null) Text(tr("Ahora · ","Now · ")+Tirso247ScheduleRP174.clock(current.startMinute)+" – "+Tirso247ScheduleRP174.clock(current.endMinute),color=Muted,fontSize=12.sp)
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically) {
            val play:()->Unit={if(current!=null && ready && !playing) {playing=true;state.launch {
                try {Tirso247ScheduleRP174.current()?.let {state.playTirsoScheduleItemRP177(it)}} finally {playing=false}
            }};Unit}
            if(mobile) {
                MobileButtonRP133(if(playing) tr("Abriendo…","Opening…") else tr("Reproducir","Play"),"play",selected=true,enabled=ready && !playing,click=play)
                MobileButtonRP133(tr("Guía","Guide"),"calendar") {guide=true}
                if(!checking && !ready) MobileIconButtonRP133("refresh",tr("Reintentar","Retry")) {retry++}
            } else {
                if(ready) FocusButton(if(playing) tr("Abriendo…","Opening…") else tr("Reproducir","Play"),primary=true,horizontalPadding=14.dp,onClick=play)
                FocusButton(tr("Guía","Guide"),horizontalPadding=14.dp) {guide=true}
                if(!checking && !ready) FocusButton("↻",horizontalPadding=12.dp) {retry++}
            }
        }
    }
    if(guide) TirsoGuideRP181(state,mobile) {guide=false}
}

@Composable
private fun TirsoGuideRP181(state:AppState,mobile:Boolean,close:()->Unit) {
    val content:@Composable ColumnScope.()->Unit={TirsoGuideContentRP181(state,mobile)}
    if(mobile) MobileSheetRP133("Tirso 24/7 · "+tr("Guía","Guide"),close,content)
    else PlayerMenuDialog(onDismissRequest=close) {
        Column(Modifier.widthIn(max=760.dp).fillMaxHeight(.88f).background(Bg).padding(24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Text("Tirso 24/7 · "+tr("Guía","Guide"),color=Color.White,fontSize=24.sp,modifier=Modifier.weight(1f))
                FocusButton("×",onClick=close)
            }
            content()
        }
    }
}

@Composable
private fun ColumnScope.TirsoGuideContentRP181(state:AppState,mobile:Boolean) {
    val owner=state.localExtrasScope()
    val rows=remember(owner) {mutableStateListOf<TirsoScheduleItemRP174>()}
    var checking by remember {mutableStateOf(true)}
    var scanned by remember {mutableIntStateOf(0)}
    var retry by remember {mutableIntStateOf(0)}
    var calendar by remember {mutableStateOf(Calendar.getInstance())}
    LaunchedEffect(owner) {while(true) {delay(30_000);calendar=Calendar.getInstance()}}
    val dayKey="${calendar.get(Calendar.YEAR)}:${calendar.get(Calendar.DAY_OF_YEAR)}"
    val schedule=remember(dayKey) {Tirso247ScheduleRP174.today(calendar)}
    val current=Tirso247ScheduleRP174.current(calendar)
    LaunchedEffect(owner,retry,dayKey) {
        checking=true;rows.clear();scanned=0
        try {
            val ordered=schedule.sortedBy {if(it.id==current?.id) -1 else it.startMinute}
            for(entry in ordered) {
                if(withTimeoutOrNull(45_000) {state.verifyTirsoItemRP180(entry)}==true) rows.add(entry)
                scanned++
            }
        } finally {checking=false}
    }
    Text(tr("Elige un horario para ver ese programa desde el principio. Solo aparecen programas con fuentes disponibles.","Choose a time to watch that program from the beginning. Only programs with available sources appear."),color=Muted,fontSize=13.sp)
    if(checking) Text(tr("Comprobando enlaces · ","Checking sources · ")+"$scanned/${schedule.size}",color=Accent,fontSize=12.sp)
    if(!checking && rows.isEmpty()) Text(tr("No hay programas con enlaces disponibles. Revisa tus add-ons.","No programs have available sources. Check your add-ons."),color=Muted)
    LazyColumn(Modifier.weight(1f).fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        items(rows.sortedBy {it.startMinute},key={it.id}) {entry->
            val target=state.tirsoTargetRP181(entry)
            Column(Modifier.fillMaxWidth().themeSurfaceRP181(entry.id==current?.id).padding(14.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                Text(Tirso247ScheduleRP174.clock(entry.startMinute)+" – "+Tirso247ScheduleRP174.clock(entry.endMinute),color=Accent,fontSize=13.sp)
                Text(entry.title,color=Color.White,fontWeight=FontWeight.Bold,fontSize=16.sp)
                if(!target?.episodeTitle.isNullOrBlank()) Text(target?.episodeTitle.orEmpty(),color=Muted,fontSize=12.sp)
                val play:()->Unit={state.launch {state.playTirsoScheduleItemRP177(entry)};Unit}
                if(mobile) MobileButtonRP133(tr("Reproducir","Play"),"play",click=play)
                else FocusButton(tr("Reproducir","Play"),onClick=play)
            }
        }
    }
    if(!checking) {
        if(mobile) MobileButtonRP133(tr("Actualizar guía","Refresh guide"),"refresh") {retry++}
        else FocusButton(tr("Actualizar guía","Refresh guide")) {retry++}
    }
}
