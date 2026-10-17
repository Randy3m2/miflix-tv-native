package com.miflix.native2.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
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
    Dialog(onDismissRequest=close,properties=DialogProperties(usePlatformDefaultWidth=false)) {
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
                FocusButton(tr("Compartir con ","Share with ")+friend.nickname) {
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
    Dialog(onDismissRequest=close,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        LazyColumn(Modifier.width(630.dp).heightIn(max=460.dp).background(Panel,RoundedCornerShape(20.dp)).padding(24.dp),
            verticalArrangement=Arrangement.spacedBy(14.dp)) {
            item {Text(tr("Momentos compartidos","Shared moments"),color=Color.White,fontSize=26.sp)}
            item {Text(tr("Pulsa Actualizar para buscar momentos nuevos. No se revisan automáticamente.","Select Refresh to check for new moments. No automatic checks."),color=Muted)}
            item {FocusButton(if(state.sharedMomentsLoading) tr("Actualizando…","Refreshing…") else tr("Actualizar momentos","Refresh moments")) {
                if(!state.sharedMomentsLoading) state.launch {state.refreshSharedMoments()}
            }}
            if(rows.isEmpty()) item {Text(tr("No hay momentos cargados.","No loaded moments."),color=Muted)}
            items(rows,key={it.id}) { moment ->
                Column(Modifier.fillMaxWidth().background(Bg,RoundedCornerShape(14.dp)).padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                    Text(moment.title,color=Color.White,fontSize=20.sp)
                    Text("${moment.nickname} · "+(if(moment.season>0) "S${moment.season} E${moment.episode} · " else "")+
                        "${momentTime(moment.startMs)} – ${momentTime(moment.endMs)}",color=Muted)
                    Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                        FocusButton(if(state.watchParty!=null || state.pendingPartyCode!=null) tr("Salir del party y ver","Leave party and play") else tr("Ver momento","Play moment")) {
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
