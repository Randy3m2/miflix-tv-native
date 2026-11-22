package com.miflix.native2.ui

import androidx.compose.foundation.layout.*
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
internal fun DailyRotationScreenRP213(state:AppState) {
    BackHandler {state.screen=Screen.HOME}
    AppShell(state,Screen.DAILY_ROTATION,{state.screen=it}) {
        DailyRotationContent(state)
    }
}

@Composable
private fun DailyRotationContent(state:AppState) {
    val filter=""
    if(state.kidsMode || state.partyRole==PartyRole.GUEST) return
    val owner=state.localExtrasScope()
    val context=LocalContext.current
    val rotationPrefs=remember(context) {context.getSharedPreferences("rp203_daily_rotation_history",android.content.Context.MODE_PRIVATE)}
    var calendar by remember(owner) {mutableStateOf(Calendar.getInstance())}
    LaunchedEffect(owner,state.appForeground) {
        calendar=Calendar.getInstance()
        while(state.appForeground) {delay(60_000);calendar=Calendar.getInstance()}
    }
    val year=calendar.get(Calendar.YEAR);val day=calendar.get(Calendar.DAY_OF_YEAR)
    val historyKey=remember(owner,filter) {"used:"+owner+":"+filter}
    val todayKey=remember(owner,year,day,filter) {"today:"+owner+":"+year+":"+day+":"+filter}
    // The same day's selection can be reopened; exclude only previous days' picks.
    val seenPrior=remember(owner,year,day,filter) {
        val all=rotationPrefs.getStringSet(historyKey,emptySet()).orEmpty()
        val todays=rotationPrefs.getStringSet(todayKey,emptySet()).orEmpty()
        all-todays
    }
    val key=listOf(owner,year,day,filter,state.interfaceLanguage,state.audioLanguage,state.streamSizeLimitGb,state.easyModeRP79,state.nativeOnlyRP75,state.nativeSpanishOnlyRP77,state.nativeSourceEnabled,state.addonManifests.toList().hashCode()).joinToString(":")
    val rows=remember(key) {mutableStateListOf<Pair<TirsoScheduleItemRP174,MediaSummary>>()}
    var loading by remember(key) {mutableStateOf(true)}
    var retry by remember(key) {mutableIntStateOf(0)}
    var opening by remember(key) {mutableStateOf(false)}
    LaunchedEffect(key,retry,state.appForeground) {
        if(!state.appForeground) return@LaunchedEffect
        loading=true
        try {
            val candidates=DailyRotationPolicyRP188.unseenCandidates(year,day,seenPrior,filter).take(36)
            state.dailyRotationRP188.load(key,candidates,{state.verifyTirsoItemRP180(it)}) {entry->
                val target=state.tirsoTargetRP181(entry)
                if(target!=null && rows.none {DailyRotationPolicyRP188.identity(it.first)==DailyRotationPolicyRP188.identity(entry)}) {
                    val title=target.item.title+if(target.season>0) " · S${target.season} E${target.episode}" else ""
                    rows.add(entry to target.item.copy(title=title))
                    val identity=DailyRotationPolicyRP188.identity(entry)
                    rotationPrefs.edit()
                        .putStringSet(historyKey,rotationPrefs.getStringSet(historyKey,emptySet()).orEmpty()+identity)
                        .putStringSet(todayKey,rotationPrefs.getStringSet(todayKey,emptySet()).orEmpty()+identity)
                        .apply()
                }
            }
        } finally {loading=false}
    }
    val title=tr("Rotación diaria","Daily rotation")
    val play:(MediaSummary)->Unit={item->
        val entry=rows.firstOrNull {it.second.cloudId==item.cloudId}?.first
        if(entry!=null && !opening) {opening=true;state.launch {
            try {state.socialAction {state.playTirsoScheduleItemRP177(entry,Screen.DAILY_ROTATION)}} finally {opening=false}
        }}
    }
    val entryFocus=remember(key) {FocusRequester()}
    var placeholderFocused by remember(key) {mutableStateOf(false)}
    LaunchedEffect(rows.isNotEmpty()) {
        if(rows.isNotEmpty() && placeholderFocused) {
            withFrameNanos { };runCatching {entryFocus.requestFocus()}
        }
    }
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(top=24.dp,bottom=48.dp)) {
        item(key="heading") {
            Column(Modifier.fillMaxWidth().padding(start=40.dp,end=40.dp,bottom=20.dp),
                verticalArrangement=Arrangement.spacedBy(10.dp)) {
                Text(title,color=Color.White,fontSize=32.sp,fontWeight=FontWeight.Bold)
                if(loading || opening) Text(if(opening) tr("Abriendo título…","Opening title…")
                    else tr("Preparando la selección de hoy…","Preparing today's selection…"),color=Muted,fontSize=14.sp)
                if(rows.isEmpty()) {
                    if(loading) FocusButton(tr("Comprobando enlaces…","Checking sources…"),
                        modifier=Modifier.onFocusChanged {if(it.isFocused || rows.isEmpty()) placeholderFocused=it.isFocused}) {}
                    else {
                        Text(tr("No encontramos enlaces para la selección de hoy.","No sources were found for today's selection."),color=Muted,fontSize=14.sp)
                        FocusButton(tr("Reintentar","Retry"),modifier=Modifier.onFocusChanged {if(it.isFocused || rows.isEmpty()) placeholderFocused=it.isFocused}) {
                            state.dailyRotationRP188.invalidate(key);retry++
                        }
                    }
                }
            }
        }
        val films=rows.map {it.second}.filter {it.type=="movie"}.distinctBy {it.cloudId}
        val series=rows.map {it.second}.filter {it.type=="series"}.distinctBy {it.cloudId}
        if(films.isNotEmpty()) item(key="films") {
            Box(Modifier.focusRequester(entryFocus)) {
                SpotlightRail(tr("Películas","Movies"),films,showTitle=true,onClick=play)
            }
        }
        if(series.isNotEmpty()) item(key="series") {
            Box(if(films.isEmpty()) Modifier.focusRequester(entryFocus) else Modifier) {
                SpotlightRail(tr("Series","Series"),series,showTitle=true,onClick=play)
            }
        }
    }
}
