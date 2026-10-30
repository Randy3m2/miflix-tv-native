package com.miflix.native2.ui

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.miflix.native2.BuildConfig
import com.miflix.native2.data.*
import com.miflix.native2.model.MediaSummary
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

internal data class MobileNowPlayingCardRP162(val presence:NowPlayingEntryRP162,val media:MediaSummary?)
internal class MobileNowPlayingStateRP162 {
    var cards by mutableStateOf<List<MobileNowPlayingCardRP162>>(emptyList())
    var loading by mutableStateOf(false)
    var failed by mutableStateOf(false)
}

/** A read on Home entry/resume or explicit refresh. No backend polling or new TV heartbeats. */
@Composable
internal fun rememberMobileNowPlayingRP162(state:AppState):MobileNowPlayingStateRP162 {
    val account=state.session?.userId
    val profile=state.activeProfile.id
    val enabled=account!=null && !state.kidsMode && !state.easyModeRP79 && !state.sessionNeedsSignIn
    val result=remember(account,profile,enabled,state.interfaceLanguage) {MobileNowPlayingStateRP162()}
    val repository=remember {NowPlayingRepositoryRP162(BuildConfig.SUPABASE_URL,BuildConfig.SUPABASE_KEY)}
    suspend fun load() {
        val session=state.session ?: return
        if(!enabled || !state.appForeground || result.loading) return
        result.loading=true;result.failed=false
        try {
            val local=(state.continueWatching+state.trending+state.movies+state.series).associateBy {it.cloudId}
            val cards=withContext(Dispatchers.IO) {
                val entries=repository.read(session,SystemClock::elapsedRealtime)
                val gate=Semaphore(3)
                coroutineScope {entries.map {entry ->async {gate.withPermit {
                    val media=local[entry.cloud] ?: try {state.tmdb.byCloudId(entry.cloud)} catch(e:CancellationException) {throw e} catch(_:Exception) {null}
                    MobileNowPlayingCardRP162(entry,media)
                }}}.awaitAll()}
            }
            ensureActiveNowPlayingRP162()
            if(state.session?.userId==session.userId && state.activeProfile.id==profile && !state.kidsMode && !state.easyModeRP79)
                result.cards=cards.filter {it.presence.validUntil>SystemClock.elapsedRealtime()}
        } catch(e:CancellationException) {throw e}
          catch(_:Exception) {result.failed=true;result.cards=emptyList()}
        finally {result.loading=false}
    }
    LaunchedEffect(result,state.appForeground,state.mobileHomeEntryRP162) {
        if(enabled && state.appForeground) load() else result.cards=emptyList()
    }
    // A local expiry timer prevents expired sessions lingering in an open Home screen.
    LaunchedEffect(result.cards) {
        result.cards.minOfOrNull {it.presence.validUntil}?.let {expiry ->
            delay((expiry-SystemClock.elapsedRealtime()).coerceAtLeast(1))
            result.cards=result.cards.filter {it.presence.validUntil>SystemClock.elapsedRealtime()}
        }
    }
    return result
}
private suspend fun ensureActiveNowPlayingRP162() {currentCoroutineContext().ensureActive()}

@Composable
internal fun MobileNowPlayingRowRP162(state:AppState,value:MobileNowPlayingStateRP162,filter:String) {
    // Live TV remains visible in All; Movies/Series narrow the shelf like Continue Watching.
    val cards=value.cards.filter {filter.isBlank() || it.media?.type==filter}
    if(cards.isEmpty()) return
    Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal=16.dp),verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(2.dp)) {
                Text(tr("Reproduciendo ahora","Now Playing"),color=Color.White,fontSize=19.sp,fontWeight=FontWeight.Bold)
                Text(tr("En tu TV · misma cuenta","On your TV · same account"),color=Muted,fontSize=11.sp)
            }
        }
        LazyRow(contentPadding=PaddingValues(horizontal=16.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            items(cards,key={it.presence.profile}) {card ->
                val entry=card.presence
                val profileName=state.profiles.firstOrNull {it.id==entry.profile}?.name.orEmpty()
                val title=card.media?.title ?: entry.title
                val caption=listOfNotNull(profileName.takeIf {it.isNotBlank()},if(entry.episode>0) "${tr("T","S")}${entry.season} · E${entry.episode}" else null).joinToString(" · ")
                val description=title+" · "+tr("En tu TV","On your TV")+if(caption.isBlank()) "" else " · $caption"
                Column(Modifier.width(210.dp).semantics {contentDescription=description}.clickable(enabled=card.media!=null) {
                    card.media?.let {media->state.launch {state.open(media)}}
                }) {
                    Box(Modifier.fillMaxWidth().aspectRatio(16f/9f).clip(RoundedCornerShape(12.dp)).background(MobilePanelRP133)) {
                        if(card.media!=null) LoadingArtworkRP58(card.media.backdrop ?: card.media.poster,title,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
                        else MobileIconRP133("cast",modifier=Modifier.align(Alignment.Center).size(36.dp),tint=MobileTintRP133)
                        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent,Color.Black.copy(alpha=.36f)))))
                        Row(Modifier.align(Alignment.TopStart).padding(8.dp).background(Color(0xE6101118),RoundedCornerShape(20.dp)).padding(horizontal=8.dp,vertical=5.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                            MobileIconRP133("cast",Modifier.size(13.dp),tint=MobileTintRP133)
                            Text("TV",color=Color.White,fontSize=10.sp,fontWeight=FontWeight.Bold)
                        }
                    }
                    Text(title,color=Color.White,fontSize=12.sp,maxLines=2,overflow=TextOverflow.Ellipsis,modifier=Modifier.padding(top=5.dp))
                    if(caption.isNotBlank()) Text(caption,color=Muted,fontSize=11.sp,maxLines=2,modifier=Modifier.padding(top=2.dp))
                }
            }
        }
    }
}
