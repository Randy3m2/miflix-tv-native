package com.miflix.native2.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.focusGroup
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
internal fun DailyRotationRowRP188(state:AppState,mobile:Boolean=false,filter:String="") {
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
    LaunchedEffect(key,retry,opening,state.appForeground) {
        if(opening || !state.appForeground) return@LaunchedEffect
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
            try {state.socialAction {state.playTirsoScheduleItemRP177(entry,Screen.HOME)}} finally {opening=false}
        }}
    }
    val entryFocus=remember(key) {FocusRequester()}
    var placeholderFocused by remember(key) {mutableStateOf(false)}
    LaunchedEffect(rows.isNotEmpty()) {
        if(rows.isNotEmpty() && placeholderFocused && !mobile) {
            withFrameNanos { };runCatching {entryFocus.requestFocus()}
        }
    }
    val railHeight=if(mobile) 240.dp else if(LocalHomeShelfPaging.current) LocalHomeCardRowHeight.current+40.dp else 302.dp
    Box(Modifier.fillMaxWidth().height(railHeight).focusRequester(entryFocus).focusGroup()) {
    if(rows.isNotEmpty()) {
        if(mobile) MobileRailRP133(state,title,rows.map {it.second},onSelectRP188=play)
        else SpotlightRail(title,rows.map {it.second},showTitle=true,onClick=play)
    } else Column(Modifier.fillMaxWidth().padding(horizontal=if(mobile) 16.dp else 40.dp,vertical=16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Text(title,color=Color.White,fontSize=if(mobile) 19.sp else 24.sp,fontWeight=FontWeight.Bold)
        Text(if(loading) tr("Preparando la selección de hoy…","Preparing today's selection…") else tr("No encontramos enlaces para la selección de hoy. Revisa tus add-ons o reintenta.","No sources were found for today's selection. Check your add-ons or retry."),color=Muted,fontSize=14.sp)
        if(loading && !mobile) FocusButton(tr("Comprobando enlaces…","Checking sources…"),modifier=Modifier.onFocusChanged {if(it.isFocused || rows.isEmpty()) placeholderFocused=it.isFocused}) { }
        if(!loading) {
            val again={state.dailyRotationRP188.invalidate(key);rows.clear();retry++;Unit}
            if(mobile) MobileButtonRP133(tr("Reintentar","Retry"),click=again)
            else FocusButton(tr("Reintentar","Retry"),modifier=Modifier.onFocusChanged {if(it.isFocused || rows.isEmpty()) placeholderFocused=it.isFocused},onClick=again)
        }
    }
}

}
