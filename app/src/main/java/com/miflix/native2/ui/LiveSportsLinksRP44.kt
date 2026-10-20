package com.miflix.native2.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.miflix.native2.data.LiveChannel
import com.miflix.native2.data.LiveRepository
import com.miflix.native2.model.PartyRole
import com.miflix.native2.model.StreamChoice
import kotlinx.coroutines.CancellationException

/** Manual selection only for Live Sports. Fetches once per opened event, never polls. */
@Composable
internal fun LiveSportsLinksRP44(state:AppState,channel:LiveChannel,close:()->Unit,select:(StreamChoice)->Unit) {
    val owner=state.session?.userId to state.activeProfile.id
    val originalOwner=remember {owner}
    var rows by remember(channel.id,channel.type) {mutableStateOf<List<StreamChoice>>(emptyList())}
    var loading by remember {mutableStateOf(true)}
    var failed by remember {mutableStateOf(false)}
    var retry by remember {mutableIntStateOf(0)}
    val first=remember {FocusRequester()}
    LaunchedEffect(owner) {if(owner!=originalOwner) close()}
    LaunchedEffect(channel.id,channel.type,retry) {
        loading=true;failed=false
        try {rows=LiveRepository().streams(state.calendarLiveManifest("sports"),channel)}
        catch(e:CancellationException) {throw e} catch(_:Exception) {failed=true;rows=emptyList()}
        finally {loading=false}
    }
    LaunchedEffect(loading,rows) {if(!loading && rows.isNotEmpty()) {withFrameNanos {};runCatching {first.requestFocus()}}}
    PlayerMenuDialog(onDismissRequest=close,fullScreen=true) {
        BoxWithConstraints(Modifier.fillMaxSize().consumeOpeningPress()) {
            Column(Modifier.align(Alignment.CenterEnd).fillMaxWidth(if(maxWidth<700.dp) .78f else .43f).fillMaxHeight(.9f)
                .background(Color(0xFC101010),RoundedCornerShape(18.dp)).padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Text(tr("ENLACES · LIVE SPORTS","LINKS · LIVE SPORTS"),color=Muted,fontSize=12.sp)
                Text(channel.name,color=Color.White,fontSize=22.sp)
                Text(tr("Elige la fuente que quieres reproducir.","Choose a source to play."),color=Muted,fontSize=13.sp)
                if(loading) Text(tr("Cargando enlaces…","Loading links…"),color=Color.White)
                if(!loading && rows.isEmpty()) {
                    Text(if(failed) tr("No se pudieron obtener los enlaces.","Could not load links.") else tr("No hay enlaces disponibles.","No links available."),color=Muted)
                    FocusButton(tr("Reintentar","Retry")) {retry++}
                }
                LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp),contentPadding=PaddingValues(3.dp)) {
                    itemsIndexed(rows) {index,stream ->
                        var focused by remember {mutableStateOf(false)}
                        val choose={if(originalOwner==(state.session?.userId to state.activeProfile.id) && state.partyRole!=PartyRole.GUEST) select(stream)}
                        Column(Modifier.fillMaxWidth().then(if(index==0) Modifier.focusRequester(first) else Modifier)
                            .onFocusChanged {focused=it.isFocused}.focusable().tvClick(choose).clickable(onClick=choose)
                            .background(if(focused) Color(0xFF292929) else Color(0xFF181818),RoundedCornerShape(12.dp))
                            .border(if(focused) 2.dp else 1.dp,if(focused) Color.White else PanelBorder,RoundedCornerShape(12.dp))
                            .padding(14.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                            Text("${index+1}. ${stream.name.ifBlank {tr("Fuente","Source")}}",color=Color.White,fontSize=15.sp,fontWeight=FontWeight.Bold)
                            if(stream.title.isNotBlank()) Text(stream.title,color=Muted,fontSize=12.sp,maxLines=4,overflow=TextOverflow.Ellipsis)
                        }
                    }
                }
                FocusButton(tr("Cerrar","Close")) {close()}
            }
        }
    }
}
