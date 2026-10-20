package com.miflix.native2.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.miflix.native2.data.*
import com.miflix.native2.model.*
import kotlinx.coroutines.*

@Composable
internal fun HubM3URP42(state:AppState,modifier:Modifier=Modifier) {
    val context=LocalContext.current;val scope=rememberCoroutineScope()
    var entries by remember {mutableStateOf<List<IptvEntry>>(emptyList())}
    var message by remember {mutableStateOf("")};var web by remember {mutableStateOf<String?>(null)}
    LaunchedEffect(Unit) {
        try {entries=withContext(Dispatchers.IO) {context.assets.open("others_rp42.m3u").bufferedReader().use {IptvPlaylist.parse(it.readText())}}}
        catch(e:CancellationException) {throw e} catch(_:Exception) {message=tr("No se pudo cargar M3U.","Could not load M3U.")}
    }
    Column(modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Column(Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(Color(0xFF272727),Color(0xFF101010))),RoundedCornerShape(16.dp)).padding(18.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
            Text("M3U",color=Color.White,fontSize=24.sp,fontWeight=FontWeight.Bold)
            Text(tr("Tu lista · ","Your playlist · ")+entries.size+tr(" entradas"," entries"),color=Muted,fontSize=12.sp)
        }
        if(message.isNotBlank()) Text(message,color=Muted)
        LazyVerticalGrid(GridCells.Adaptive(240.dp),Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(14.dp),verticalArrangement=Arrangement.spacedBy(14.dp),contentPadding=PaddingValues(bottom=30.dp)) {
            items(entries,key={it.id+":"+it.url}) {entry ->
                HubEventCardRP41(entry.name,entry.logo,if(playlistWebRP42(entry.url)) tr("Página web","Web page") else "M3U") {
                    if(state.partyRole!=PartyRole.GUEST) {
                        if(playlistWebRP42(entry.url)) web=entry.url
                        else scope.launch {
                            state.openLivePlayer(PlayerRequest(MediaSummary(entry.id.hashCode(),"live",entry.name,"M3U",null,entry.logo,0.0,"LIVE"),
                                StreamChoice("M3U",entry.url,entry.name,requestHeaders=entry.headers),0,0,0L,live=true,liveChannelId=entry.id,liveProvider="m3u:rp42",liveType="tv",returnScreen=Screen.LIVE_TV))
                        }
                    }
                }
            }
        }
    }
    web?.let {SportsWebRP41(it) {web=null}}
}
