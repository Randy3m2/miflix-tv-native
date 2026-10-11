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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.tv.material3.Text
import com.miflix.native2.model.MediaSummary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

@Composable
fun SkipIntroDialog(onSkip: () -> Unit,close: () -> Unit) {
    val first=remember { FocusRequester() }
    Dialog(onDismissRequest=close,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        val view=LocalView.current
        DisposableEffect(view) {
            (view.parent as? DialogWindowProvider)?.window?.setDimAmount(0f)
            onDispose { }
        }
        LaunchedEffect(Unit) { withFrameNanos { };first.requestFocus() }
        Box(Modifier.fillMaxSize().background(Color.Transparent)) {
            FocusButton(tr("Saltar introducción","Skip Intro"),modifier=Modifier.align(Alignment.BottomEnd).padding(end=30.dp,bottom=100.dp).focusRequester(first)) { onSkip() }
        }
    }
}

@Composable
fun QueueAddDialog(state: AppState,item: MediaSummary,close: () -> Unit) {
    val first=remember { FocusRequester() }
    LaunchedEffect(Unit) { withFrameNanos { };first.requestFocus() }
    var season by remember { mutableStateOf(1) }
    var episodes by remember { mutableStateOf<List<com.miflix.native2.model.EpisodeSummary>>(emptyList()) }
    var count by remember { mutableStateOf(1) }
    LaunchedEffect(item.cloudId,season) { state.socialAction { count=state.tmdb.details(item).seasonCount;episodes=state.tmdb.season(item.id,season) } }
    Dialog(onDismissRequest=close,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Column(Modifier.width(680.dp).heightIn(max=500.dp).background(Panel,RoundedCornerShape(20.dp)).padding(24.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
            Text(tr("Añadir episodio a la cola","Add episode to queue"),color=Color.White,fontSize=24.sp)
            Text(item.title,color=Muted)
            Row(horizontalArrangement=Arrangement.spacedBy(14.dp)) {
                FocusButton("‹") { if(season>1) season-- };Text("S$season",color=Color.White)
                FocusButton("›") { if(season<count) season++ };FocusButton(tr("Cerrar","Close"),modifier=Modifier.focusRequester(first)) { close() }
            }
            LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                items(episodes,key={it.episode}) { ep -> FocusButton("E${ep.episode} · ${ep.title}") { state.launch { state.socialAction { state.enqueue(item,ep.season,ep.episode);close() } } } }
            }
        }
    }
}

@Composable
fun PlayerSocialDialog(state: AppState,onCreate: () -> Unit,close: () -> Unit) {
    val first=remember { FocusRequester() }
    LaunchedEffect(Unit) { withFrameNanos { };first.requestFocus() }
    var parties by remember { mutableStateOf(false) }
    var reactions by remember { mutableStateOf(false) }
    var calendar by remember { mutableStateOf(false) }
    Dialog(onDismissRequest=close,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Column(Modifier.width(780.dp).heightIn(max=530.dp).background(Panel,RoundedCornerShape(22.dp)).padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            Text(tr("Amigos y Party","Friends & Party"),color=Color.White,fontSize=26.sp)
            Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                FocusButton(tr("Amigos","Friends"),primary=!parties,modifier=Modifier.focusRequester(first)) { parties=false }
                FocusButton(tr("Salas","Parties"),primary=parties) { parties=true }
                FocusButton(tr("Actualizar","Refresh")) { state.launch { state.refreshFriendsDirectory() } }
                FocusButton(tr("Calendario","Calendar")) { calendar=true }
            }
            if(state.watchParty!=null) Row(horizontalArrangement=Arrangement.spacedBy(14.dp)) {
                Text("Party · ${state.watchParty?.roomCode}",color=Color.White)
                FocusButton(tr("Reacciones / Chat / QR","Reactions / Chat / QR")) { reactions=true }
                FocusButton(tr("Salir de sala","Leave Party")) { state.launch { state.leaveWatchParty();close() } }
            } else FocusButton(tr("Crear sala con este contenido","Create Party with this title")) { onCreate() }
            LazyColumn(Modifier.weight(1f,false),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                val friends=state.friendActivity.filter { !parties || it.room!=null }
                if(friends.isEmpty()) item { Text(tr("Pulsa Actualizar para ver la actividad","Select Refresh to see activity"),color=Muted) }
                items(friends,key={it.id}) { friend ->
                    Column(Modifier.fillMaxWidth().background(Color(0xFF252525),RoundedCornerShape(14.dp)).padding(14.dp)) {
                        Text("@${friend.nickname}",color=Color.White,fontSize=20.sp)
                        Text(friend.title.ifBlank { if(friend.online) tr("En línea","Online") else tr("Desconectado","Offline") },color=Muted)
                        friend.room?.let { code -> FocusButton(tr("Solicitar acceso","Request access")) { state.launch { state.socialAction { state.joinWatchParty(code) } } } }
                    }
                }
            }
            Text(state.partyStatus,color=Muted)
            FocusButton(tr("Cerrar","Close")) { close() }
        }
    }
    if(reactions) PartyActionsDialog(state) { reactions=false }
    if(calendar) CalendarDialog(state) { calendar=false }
}

@Composable
fun QueueDialog(state: AppState, close: () -> Unit) {
    val first=remember { FocusRequester() }
    LaunchedEffect(Unit) { withFrameNanos { };first.requestFocus() }
    Dialog(onDismissRequest=close,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Column(Modifier.width(760.dp).heightIn(max=500.dp).background(Panel,RoundedCornerShape(22.dp)).padding(24.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
            Text(tr("Tu cola","Your queue"),color=Color.White,fontSize=26.sp)
            Text(tr("Se reproduce al terminar el contenido actual. La reproducción manual conserva la cola.","Plays when the current title ends. Manual playback preserves your queue."),color=Muted)
            LazyColumn(Modifier.weight(1f,false),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                if(state.queueEntries.isEmpty()) item { Text(tr("Cola vacía","Queue is empty"),color=Muted) }
                items(state.queueEntries,key={it.id}) { entry ->
                    Column(Modifier.fillMaxWidth().background(Color(0xFF232323),RoundedCornerShape(12.dp)).padding(12.dp)) {
                        Text(entry.title+if(entry.season>0) " · S${entry.season} E${entry.episode}" else "",color=Color.White,maxLines=2)
                        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                            FocusButton("↑") { state.moveQueued(entry.id,-1) }
                            FocusButton("↓") { state.moveQueued(entry.id,1) }
                            FocusButton(tr("Eliminar","Remove")) { state.removeQueued(entry.id) }
                        }
                    }
                }
            }
            Row(horizontalArrangement=Arrangement.spacedBy(14.dp)) {
                FocusButton(tr("Vaciar cola","Clear queue")) { state.clearQueue() }
                FocusButton(tr("Cerrar","Close"),modifier=Modifier.focusRequester(first)) { close() }
            }
        }
    }
}

private fun eventLocal(value: String): String = runCatching {
    val normalized=value.replace(Regex("\\.\\d+"),"").replace("+00:00","Z")
    val parser=SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX",Locale.US).apply { timeZone=TimeZone.getTimeZone("UTC") }
    SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.US).format(parser.parse(normalized)!!)
}.getOrDefault(value)

@Composable
fun CalendarDialog(state: AppState, close: () -> Unit) {
    val first=remember { FocusRequester() }
    LaunchedEffect(Unit) { withFrameNanos { };first.requestFocus() }
    var offset by remember { mutableStateOf(0) }
    var creating by remember { mutableStateOf(false) }
    var day by remember { mutableStateOf<String?>(null) }
    val month=remember(offset) { Calendar.getInstance().apply { add(Calendar.MONTH,offset);set(Calendar.DAY_OF_MONTH,1) } }
    val prefix=SimpleDateFormat("yyyy-MM",Locale.US).format(month.time)
    LaunchedEffect(Unit) { state.socialAction { state.refreshCalendar();state.refreshFriends() } }
    Dialog(onDismissRequest=close,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Column(Modifier.width(820.dp).heightIn(max=560.dp).background(Panel,RoundedCornerShape(22.dp)).padding(24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text(tr("Calendario","Calendar"),color=Color.White,fontSize=26.sp)
            Row(horizontalArrangement=Arrangement.spacedBy(14.dp)) {
                FocusButton("‹") { offset--;day=null }
                Text(prefix,color=Color.White,fontSize=22.sp)
                FocusButton("›") { offset++;day=null }
                FocusButton(tr("Crear evento","Create event"),modifier=Modifier.focusRequester(first)) { creating=true }
                FocusButton(tr("Actualizar","Refresh")) { state.launch { state.socialAction { state.refreshCalendar() } } }
                FocusButton(tr("Cerrar","Close")) { close() }
            }
            val days=List((month.get(Calendar.DAY_OF_WEEK)+5)%7) { 0 }+(1..month.getActualMaximum(Calendar.DAY_OF_MONTH)).toList()
            LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                item { Text(tr("Selecciona un día para ver sus eventos","Select a day to see its events"),color=Muted) }
                item { Row { (tr("L M X J V S D","M T W T F S S").split(" ")).forEach { Text(it,color=Muted,modifier=Modifier.weight(1f)) } } }
                items(days.chunked(7)) { group ->
                    Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                        group.forEach { n ->
                            val date="$prefix-${n.toString().padStart(2,'0')}"
                            val count=state.calendarEvents.count { eventLocal(it.starts).startsWith(date) }
                            if(n==0) Spacer(Modifier.weight(1f)) else FocusButton("$n"+if(count>0) " •" else "",modifier=Modifier.weight(1f),primary=day==date) { day=if(day==date) null else date }
                        }
                        repeat(7-group.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
                val events=state.calendarEvents.filter { eventLocal(it.starts).startsWith(day ?: prefix) }
                if(events.isEmpty()) item { Text(tr("No hay eventos en este período","No events in this period"),color=Muted) }
                items(events,key={it.id}) { event ->
                    Column(Modifier.background(Color(0xFF252525),RoundedCornerShape(14.dp)).padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        Text(event.title,color=Color.White,fontSize=20.sp)
                        Text(eventLocal(event.starts)+" · "+event.kind+if(event.season>0) " · S${event.season} E${event.episode}" else "",color=Muted)
                        Text(if(event.owner==state.session?.userId) tr("Organizado por ti","Organized by you")+" · ${event.guests.size} "+tr("invitados","guests") else tr("Te han invitado","You're invited"),color=Muted)
                        Row(horizontalArrangement=Arrangement.spacedBy(14.dp)) {
                            event.cloud?.let { cloud -> FocusButton(tr("Ver contenido","View title")) { state.launch { state.socialAction { state.open(state.tmdb.byCloudId(cloud));close() } } } }
                            if(event.owner==state.session?.userId) FocusButton(tr("Eliminar evento","Delete event")) { state.launch { state.socialAction { state.deleteCalendarEvent(event.id) } } }
                        }
                    }
                }
            }
        }
    }
    if(creating) CreateEventDialog(state,day) { creating=false }
}

@Composable
private fun CreateEventDialog(state: AppState, day: String?, close: () -> Unit) {
    var title by remember { mutableStateOf(state.selected?.title.orEmpty()) }
    var item by remember { mutableStateOf<MediaSummary?>(state.selected) }
    val first=remember { FocusRequester() }
    LaunchedEffect(Unit) { withFrameNanos { };first.requestFocus() }
    var kind by remember { mutableStateOf(state.selected?.type?.takeIf { it=="movie" || it=="series" } ?: "movie") }
    var date by remember { mutableStateOf((day ?: SimpleDateFormat("yyyy-MM-dd",Locale.US).format(java.util.Date()))+" 20:00") }
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<MediaSummary>>(emptyList()) }
    var season by remember { mutableStateOf("1") };var episode by remember { mutableStateOf("1") }
    val guests=remember { mutableStateListOf<String>() }
    var saving by remember { mutableStateOf(false) }
    Dialog(onDismissRequest=close,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        LazyColumn(Modifier.width(760.dp).heightIn(max=550.dp).background(Panel,RoundedCornerShape(20.dp)).padding(24.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
            item { Text(tr("Crear evento","Create event"),color=Color.White,fontSize=26.sp) }
            item { Row(horizontalArrangement=Arrangement.spacedBy(14.dp)) { listOf("movie" to tr("Película","Movie"),"series" to tr("Serie","Series"),"game" to tr("Juego","Game")).forEach { (k,name) -> FocusButton(name,primary=kind==k,modifier=if(k=="movie") Modifier.focusRequester(first) else Modifier) { kind=k;item=null } } } }
            item { NativeTextField(title,{title=it},tr("Título del evento","Event title")) }
            item { NativeTextField(date,{date=it},"YYYY-MM-DD HH:mm") }
            if(kind!="game") {
                item { NativeTextField(query,{query=it},tr("Buscar contenido","Search titles"));FocusButton(tr("Buscar","Search")) { state.launch { state.socialAction { results=state.tmdb.search(query).filter { it.type==kind } } } } }
                items(results.take(8),key={it.cloudId}) { result -> FocusButton(result.title,primary=item?.cloudId==result.cloudId) { item=result;title=result.title } }
                item { Text(tr("Contenido seleccionado","Selected title")+": "+item?.title.orEmpty(),color=Muted) }
                if(kind=="series") item { Row(horizontalArrangement=Arrangement.spacedBy(14.dp)) { NativeTextField(season,{season=it},tr("Temporada","Season"),modifier=Modifier.weight(1f));NativeTextField(episode,{episode=it},tr("Episodio","Episode"),modifier=Modifier.weight(1f)) } }
            }
            item { Text(state.error.orEmpty(),color=Color.White) }
            item { Text(tr("Invitar amigos (máximo 20)","Invite friends (up to 20)"),color=Color.White) }
            val me=state.session?.userId
            items(state.friendRows.filter { it.status=="accepted" }) { friend ->
                val id=if(friend.sender==me) friend.receiver else friend.sender
                FocusButton((if(id in guests) "✓ " else "+ ")+(state.friendPeople[id] ?: id)) { if(id in guests) guests.remove(id) else if(guests.size<20) guests.add(id) }
            }
            item { Row(horizontalArrangement=Arrangement.spacedBy(14.dp)) {
                FocusButton(if(saving) tr("Guardando…","Saving…") else tr("Guardar evento","Save event")) { if(!saving) state.launch {
                    saving=true
                    try { state.socialAction {
                        require(kind=="game" || item?.type==kind) { tr("Selecciona el contenido","Select a title") }
                        val s=if(kind=="series") season.toIntOrNull() ?: 0 else 0;val e=if(kind=="series") episode.toIntOrNull() ?: 0 else 0
                        require(kind!="series" || s>0 && e>0) { tr("Temporada/episodio inválidos","Invalid season/episode") }
                        state.createCalendarEvent(title,kind,date,if(kind=="game") null else item?.cloudId,s,e,guests.toList());close()
                    } } finally { saving=false }
                } }
                FocusButton(tr("Cancelar","Cancel")) { close() }
            } }
        }
    }
}
