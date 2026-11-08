package com.miflix.native2.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.miflix.native2.data.DailyRotationPolicyRP188
import com.miflix.native2.data.TirsoScheduleItemRP174
import com.miflix.native2.model.MediaSummary
import com.miflix.native2.model.PartyRole
import kotlinx.coroutines.delay
import java.util.Calendar

@Composable
internal fun DailyRotationRowRP188(state:AppState,mobile:Boolean=false,filter:String="") {
    if(state.kidsMode || state.partyRole==PartyRole.GUEST) return
    val owner=state.localExtrasScope()
    var calendar by remember(owner) {mutableStateOf(Calendar.getInstance())}
    LaunchedEffect(owner,state.appForeground) {
        calendar=Calendar.getInstance()
        while(state.appForeground) {delay(60_000);calendar=Calendar.getInstance()}
    }
    val year=calendar.get(Calendar.YEAR);val day=calendar.get(Calendar.DAY_OF_YEAR)
    val key=listOf(owner,year,day,filter,state.interfaceLanguage,state.audioLanguage,state.streamSizeLimitGb,state.easyModeRP79,state.nativeOnlyRP75,state.nativeSpanishOnlyRP77,state.nativeSourceEnabled,state.addonManifests.toList().hashCode()).joinToString(":")
    val rows=remember(key) {mutableStateListOf<Pair<TirsoScheduleItemRP174,MediaSummary>>()}
    var loading by remember(key) {mutableStateOf(true)}
    var retry by remember(key) {mutableIntStateOf(0)}
    var opening by remember(key) {mutableStateOf(false)}
    LaunchedEffect(key,retry,opening,state.appForeground) {
        if(opening || !state.appForeground) return@LaunchedEffect
        loading=true;rows.clear()
        try {
            val candidates=DailyRotationPolicyRP188.candidates(year,day).filter {filter.isBlank() || (if(it.isMovie) "movie" else "series")==filter}
            state.dailyRotationRP188.load(key,candidates,{state.verifyTirsoItemRP180(it)}) {entry->
                val target=state.tirsoTargetRP181(entry)
                if(target!=null && rows.none {it.first.id==entry.id}) {
                    val title=target.item.title+if(target.season>0) " · S${target.season} E${target.episode}" else ""
                    rows.add(entry to target.item.copy(title=title))
                }
            }
        } finally {loading=false}
    }
    val title=tr("Rotación diaria","Daily rotation")
    val play:(MediaSummary)->Unit={item->
        val entry=rows.firstOrNull {it.second.cloudId==item.cloudId}?.first
        if(entry!=null && !opening) {opening=true;state.launch {
            try {state.socialAction {state.playTirsoScheduleItemRP177(entry,Screen.HOME)}} finally {opening=false}
        }}
    }
    if(rows.isNotEmpty()) {
        if(mobile) MobileRailRP133(state,title,rows.map {it.second},onSelectRP188=play)
        else SpotlightRail(title,rows.map {it.second},showTitle=true,onClick=play)
    } else Column(Modifier.fillMaxWidth().padding(horizontal=if(mobile) 16.dp else 40.dp,vertical=16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Text(title,color=Color.White,fontSize=if(mobile) 19.sp else 24.sp,fontWeight=FontWeight.Bold)
        Text(if(loading) tr("Preparando la selección de hoy…","Preparing today's selection…") else tr("No encontramos enlaces para la selección de hoy. Revisa tus add-ons o reintenta.","No sources were found for today's selection. Check your add-ons or retry."),color=Muted,fontSize=14.sp)
        if(!loading) {
            val again={state.dailyRotationRP188.invalidate(key);retry++;Unit}
            if(mobile) MobileButtonRP133(tr("Reintentar","Retry"),click=again)
            else FocusButton(tr("Reintentar","Retry"),onClick=again)
        }
    }
}
