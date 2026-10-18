package com.miflix.native2.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import com.miflix.native2.model.Profile
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import coil3.compose.AsyncImage
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Text
import com.miflix.native2.data.SocialPerson

internal fun momentTime(ms: Long): String {
    val total=ms.coerceAtLeast(0)/1000
    return if(total>=3600) "%d:%02d:%02d".format(total/3600,total/60%60,total%60) else "%02d:%02d".format(total/60,total%60)
}

@Composable
internal fun MomentComposer(state: AppState,req: PlayerRequest,start: Long?,end: Long?,
    position: () -> Long,onStart: (Long) -> Unit,onEnd: (Long) -> Unit,close: () -> Unit) {
    val owner=state.session?.userId
    var friends by remember(owner) {mutableStateOf<List<SocialPerson>>(emptyList())}
    var busy by remember(owner) {mutableStateOf(false)}
    var note by remember(owner) {mutableStateOf("")}
    PlayerMenuDialog(onDismissRequest=close,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        LazyColumn(Modifier.width(470.dp).heightIn(max=430.dp).background(Panel,RoundedCornerShape(20.dp)).padding(22.dp),
            verticalArrangement=Arrangement.spacedBy(12.dp)) {
            item {Text(tr("Grabar momento","Mark a moment"),color=Color.White,fontSize=25.sp)}
            item {Text(tr("Solo guarda los tiempos; no graba ni sube video.","Saves timestamps only; no video recording or upload."),color=Muted)}
            item {Text("${tr("Inicio","Start")}: ${start?.let(::momentTime) ?: "—"}   ${tr("Final","End")}: ${end?.let(::momentTime) ?: "—"}",color=Color.White)}
            item {FocusButton(tr("Marcar inicio aquí","Mark start here")) {onStart(position().coerceAtLeast(0));close()}}
            if(start!=null) item {FocusButton(tr("Marcar final aquí","Mark end here")) {
                val now=position()
                if(now<=start) note=tr("El final debe estar después del inicio.","End must be after start.")
                else {onEnd(now);note=""}
            }}
            if(start!=null && end!=null && end>start) item {
                FocusButton(if(busy) tr("Espera…","Please wait…") else tr("Elegir amigo / Actualizar amigos","Choose friend / Refresh friends")) {
                    if(!busy) {busy=true;state.launch {
                        try {friends=state.momentRecipients();note=if(friends.isEmpty()) tr("No hay amigos aceptados.","No accepted friends.") else ""}
                        finally {busy=false}
                    }}
                }
            }
            items(friends,key={it.id}) { friend ->
                MomentFriendCard(friend,enabled=!busy) {
                    if(!busy && start!=null && end!=null && state.session?.userId==owner) {
                        busy=true;state.launch {
                            try {state.sendMoment(req,friend.id,start,end);note=tr("Momento compartido.","Moment shared.")}
                            finally {busy=false}
                        }
                    }
                }
            }
            if(note.isNotBlank()) item {Text(note,color=Accent)}
            item {FocusButton(tr("Cerrar","Close")) {close()}}
        }
    }
}

@Composable
internal fun SharedMomentsInbox(state: AppState,close: () -> Unit) {
    var busy by remember {mutableStateOf(false)}
    val owner=state.session?.userId
    val rows=if(state.sharedMomentsOwner==owner) state.sharedMoments.toList() else emptyList()
    // Deliberately no LaunchedEffect/network request on open. Only the button loads the inbox.
    PlayerMenuDialog(onDismissRequest=close,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        LazyColumn(Modifier.width(630.dp).heightIn(max=460.dp).background(Panel,RoundedCornerShape(20.dp)).padding(24.dp),
            verticalArrangement=Arrangement.spacedBy(14.dp)) {
            item {Column(verticalArrangement=Arrangement.spacedBy(5.dp)) {
                Text(tr("COMPARTIDO CONTIGO","SHARED WITH YOU"),color=Accent,fontSize=11.sp,fontWeight=FontWeight.Bold)
                Text(tr("Momentos compartidos","Shared moments"),color=Color.White,fontSize=27.sp,fontWeight=FontWeight.Bold)
            }}
            item {Text(tr("Pulsa Actualizar para buscar momentos nuevos. No se revisan automáticamente.","Select Refresh to check for new moments. No automatic checks."),color=Muted)}
            item {FeatureActionCard(if(state.sharedMomentsLoading) tr("Actualizando…","Refreshing…") else tr("Actualizar momentos","Refresh moments"),tr("Tú eliges cuándo buscar novedades","You choose when to check for new moments"),glyph="↻",modifier=Modifier.fillMaxWidth()) {
                if(!state.sharedMomentsLoading) state.launch {state.refreshSharedMoments()}
            }}
            if(rows.isEmpty()) item {Text(tr("No hay momentos cargados.","No loaded moments."),color=Muted)}
            items(rows,key={it.id}) { moment ->
                val sender=state.friendActivity.firstOrNull {it.id==moment.sender}
                val known=(state.movies+state.series+state.continueWatching+state.collectionItems).firstOrNull {it.cloudId==moment.cloud}
                Column(Modifier.fillMaxWidth().background(Bg,RoundedCornerShape(20.dp))
                    .border(1.dp,Accent.copy(alpha=.23f),RoundedCornerShape(20.dp)).padding(16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
                    Row(horizontalArrangement=Arrangement.spacedBy(16.dp),verticalAlignment=Alignment.CenterVertically) {
                        Box(Modifier.size(76.dp,112.dp).clip(RoundedCornerShape(12.dp)).background(Accent.copy(alpha=.12f)),contentAlignment=Alignment.Center) {
                            if(known?.poster!=null) AsyncImage(known.poster,null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
                            else Text("▶",color=Accent,fontSize=30.sp)
                        }
                        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                            Row(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically) {
                                ProfileAvatar(Profile(moment.sender,moment.nickname,sender?.avatar),Modifier.size(28.dp).clip(CircleShape))
                                Text(moment.nickname,color=Muted,fontSize=12.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
                            }
                            Text(moment.title,color=Color.White,fontSize=19.sp,fontWeight=FontWeight.Bold,maxLines=2,overflow=TextOverflow.Ellipsis)
                            if(moment.season>0) Text("S${moment.season} · E${moment.episode}",color=Muted,fontSize=12.sp)
                            Text("${momentTime(moment.startMs)} → ${momentTime(moment.endMs)}  ·  ${momentTime(moment.endMs-moment.startMs)}",
                                color=Accent,fontSize=12.sp,fontWeight=FontWeight.Bold,
                                modifier=Modifier.background(Accent.copy(alpha=.1f),RoundedCornerShape(8.dp)).padding(horizontal=10.dp,vertical=6.dp))
                        }
                    }
                    Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                        FocusButton(if(state.watchParty!=null || state.pendingPartyCode!=null) tr("Salir del party y ver","Leave party and play") else tr("Ver momento","Play moment"),primary=true) {
                            if(!busy) {busy=true;state.launch {
                                try {state.playSharedMoment(moment);if(state.screen==Screen.PLAYER) close()}
                                finally {busy=false}
                            }}
                        }
                        FocusButton(tr("Eliminar","Remove")) {if(!busy) {busy=true;state.launch {try {state.removeSharedMoment(moment)} finally {busy=false}}}}
                    }
                }
            }
            item {FocusButton(tr("Cerrar","Close")) {close()}}
        }
    }
}


@Composable
private fun MomentFriendCard(friend: SocialPerson,enabled: Boolean,onClick: () -> Unit) {
    var focused by remember(friend.id) {mutableStateOf(false)}
    val activate: () -> Unit={if(enabled) onClick()}
    Row(Modifier.fillMaxWidth().onFocusChanged {focused=it.isFocused}.focusable()
        .tvClick(activate).clickable(onClick=activate)
        .background(if(focused) FocusedPanel else Bg,RoundedCornerShape(18.dp))
        .border(if(focused) 2.dp else 1.dp,if(focused) Accent else PanelBorder,RoundedCornerShape(18.dp))
        .padding(14.dp),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)) {
        ProfileAvatar(Profile(friend.id,friend.nickname,friend.avatar),Modifier.size(48.dp).clip(CircleShape))
        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
            Text(friend.nickname,color=Color.White,fontSize=20.sp,fontWeight=FontWeight.Bold,maxLines=1)
            Text(tr("Compartir momento","Share moment"),color=Muted,fontSize=13.sp)
        }
        Text("›",color=if(focused) Accent else Muted,fontSize=28.sp)
    }
}
