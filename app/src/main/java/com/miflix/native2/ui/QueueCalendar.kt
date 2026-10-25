@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.miflix.native2.ui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.*
import kotlinx.coroutines.launch
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
import com.miflix.native2.model.Profile
import com.miflix.native2.model.PartyRole
import com.miflix.native2.data.LiveChannel
import com.miflix.native2.data.LiveCategory
import com.miflix.native2.data.LiveRepository
import coil3.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
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
    PlayerMenuDialog(onDismissRequest=close,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Column(Modifier.width(680.dp).heightIn(max=500.dp).background(Panel,RoundedCornerShape(20.dp)).padding(24.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
            Text(tr("Añadir episodio a la cola","Add episode to queue"),color=Color.White,fontSize=24.sp)
            Text(item.title,color=Muted)
            FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
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
fun PlayerSocialDialog(state: AppState,onCreate: () -> Unit,onRefreshRequests: () -> Unit,onSwitchCurrent: () -> Unit={},onWatchParty: () -> Unit={},close: () -> Unit) {
    PartyRoomSheet(state,onCreate,onRefreshRequests,onSwitchCurrent,onWatchParty,close)
}

@Composable
fun QueueDialog(state: AppState, close: () -> Unit) {
    val first=remember { FocusRequester() }
    LaunchedEffect(Unit) { withFrameNanos { };first.requestFocus() }
    PlayerMenuDialog(onDismissRequest=close,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Column(Modifier.width(760.dp).heightIn(max=500.dp).background(Panel,RoundedCornerShape(22.dp)).padding(24.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
            Text(tr("Tu cola","Your queue"),color=Color.White,fontSize=26.sp)
            Text(tr("Se reproduce al terminar el contenido actual. La reproducción manual conserva la cola.","Plays when the current title ends. Manual playback preserves your queue."),color=Muted)
            LazyColumn(Modifier.weight(1f,false),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                if(state.queueEntries.isEmpty()) item { Text(tr("Cola vacía","Queue is empty"),color=Muted) }
                items(state.queueEntries,key={it.id}) { entry ->
                    Column(Modifier.fillMaxWidth().background(Panel,RoundedCornerShape(12.dp)).padding(12.dp)) {
                        Text(entry.title+if(entry.season>0) " · S${entry.season} E${entry.episode}" else "",color=Color.White,maxLines=2)
                        FlowRow(horizontalArrangement=Arrangement.spacedBy(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                            if(state.partyRole!=PartyRole.GUEST) FocusButton(if(state.queueStarting) tr("Cargando…","Loading…") else tr("Reproducir","Play")) {
                                val req=state.playerRequest
                                state.launch { state.socialAction { state.playQueued(req,entry);if(state.queueEntries.none { it.id==entry.id }) close() } }
                            }
                            FocusButton("↑") { state.moveQueued(entry.id,-1) }
                            FocusButton("↓") { state.moveQueued(entry.id,1) }
                            FocusButton(tr("Eliminar","Remove"),destructive=true) { state.removeQueued(entry.id) }
                        }
                    }
                }
            }
            state.error?.let { Text(it,color=Color.White,maxLines=3) }
            FlowRow(horizontalArrangement=Arrangement.spacedBy(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                FocusButton(tr("Vaciar cola","Clear queue")) { state.clearQueue() }
                FocusButton(tr("Cerrar","Close"),modifier=Modifier.focusRequester(first)) { close() }
            }
        }
    }
}

internal fun eventLocal(value: String): String = runCatching {
    val normalized=value.replace(Regex("\\.\\d+"),"").replace("+00:00","Z")
    val parser=SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX",Locale.US).apply { timeZone=TimeZone.getTimeZone("UTC") }
    SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.US).format(parser.parse(normalized)!!)
}.getOrDefault(value)

@Composable
fun CalendarDialog(state: AppState, close: () -> Unit) {
    val androidContextRP120=androidx.compose.ui.platform.LocalContext.current.applicationContext
    val first=remember { FocusRequester() }
    LaunchedEffect(Unit) { withFrameNanos { };first.requestFocus() }
    var offset by remember { mutableStateOf(0) }
    var creating by remember { mutableStateOf(false) }
    var day by remember { mutableStateOf<String?>(null) }
    val month=remember(offset) { Calendar.getInstance().apply { add(Calendar.MONTH,offset);set(Calendar.DAY_OF_MONTH,1) } }
    val prefix=SimpleDateFormat("yyyy-MM",Locale.US).format(month.time)
    val subscribedEventsRP120=rememberSportEventsRP120(state,prefix)
    val subscribedByDayRP120=remember(subscribedEventsRP120) {subscribedEventsRP120.groupBy {it.day()}}
    var subscriptionsRP120 by remember {mutableStateOf(false)}
    var eventSearchRP120 by remember {mutableStateOf("")}
    LaunchedEffect(Unit) { state.socialAction { state.refreshCalendar();state.refreshFriends() } }
    PlayerMenuDialog(onDismissRequest=close,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Column(Modifier.width(820.dp).heightIn(max=560.dp).background(Panel,RoundedCornerShape(22.dp)).padding(24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text(tr("Calendario","Calendar"),color=Color.White,fontSize=26.sp)
            Row(horizontalArrangement=Arrangement.spacedBy(14.dp)) {
                FocusButton("‹") { offset--;day=null }
                Text(prefix,color=Color.White,fontSize=22.sp)
                FocusButton("›") { offset++;day=null }
                FocusButton(tr("Suscripciones","Subscriptions")) {subscriptionsRP120=true}
                FocusButton(tr("Crear evento","Create event"),modifier=Modifier.focusRequester(first)) { creating=true }
                FocusButton(tr("Actualizar","Refresh")) { state.launch { state.socialAction { state.refreshCalendar() };withContext(Dispatchers.IO) {com.miflix.native2.data.CalendarStoreRP120.refresh(androidContextRP120,com.miflix.native2.data.CalendarStoreRP120.owner(androidContextRP120,state.activeProfile.id),true)} } }
                FocusButton(tr("Cerrar","Close")) { close() }
            }
            state.error?.let { Text(it,color=Color.White,maxLines=3) }
            val days=List((month.get(Calendar.DAY_OF_WEEK)+5)%7) { 0 }+(1..month.getActualMaximum(Calendar.DAY_OF_MONTH)).toList()
            LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                item { Text(tr("Selecciona un día para ver sus eventos","Select a day to see its events"),color=Muted) }
                item { Row { (tr("L M X J V S D","M T W T F S S").split(" ")).forEach { Text(it,color=Muted,modifier=Modifier.weight(1f)) } } }
                items(days.chunked(7)) { group ->
                    Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                        group.forEach { n ->
                            val date="$prefix-${n.toString().padStart(2,'0')}"
                            val sporting=subscribedByDayRP120[date].orEmpty()
                                val count=state.calendarEvents.count { eventLocal(it.starts).startsWith(date) }+sporting.size
                            if(n==0) Spacer(Modifier.weight(1f)) else CalendarDay(n,count,day==date,Modifier.weight(1f),height=48.dp,logosRP120=sporting.map {it.logos.firstOrNull() ?: com.miflix.native2.data.CalendarSourcesRP120.logo(it.league)}) {day=if(day==date) null else date}
                        }
                        repeat(7-group.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
                item {NativeTextField(eventSearchRP120,{eventSearchRP120=it},tr("Buscar equipo, liga o enfrentamiento","Find team, league or matchup"),modifier=Modifier.fillMaxWidth())}
                val sportRowsRP120=subscribedEventsRP120.filter {(eventSearchRP120.isNotBlank() || it.day().startsWith(day ?: prefix)) &&
                    eventSearchRP120.trim().split(" ").filter {q ->q.isNotBlank()}.all {q ->com.miflix.native2.data.IptvPlaylist.searchKey(it.league+" "+it.title).contains(com.miflix.native2.data.IptvPlaylist.searchKey(q))}}
                items(sportRowsRP120,key={"sport:"+it.key}) {event ->CalendarSportEventRP120(state,event)}
                val events=state.calendarEvents.filter { eventLocal(it.starts).startsWith(day ?: prefix) }
                if(events.isEmpty() && sportRowsRP120.isEmpty()) item { Text(tr("No hay eventos en este período","No events in this period"),color=Muted) }
                items(events,key={it.id}) { event ->
                    Column(Modifier.background(Panel,RoundedCornerShape(14.dp)).padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        Text(event.title,color=Color.White,fontSize=20.sp)
                        Text(eventLocal(event.starts)+" · "+event.kind+if(event.season>0) " · S${event.season} E${event.episode}" else "",color=Muted)
                        Text(if(event.owner==state.session?.userId) tr("Organizado por ti","Organized by you")+" · ${event.guests.size} "+tr("invitados","guests") else tr("Te han invitado","You're invited"),color=Muted)
                        Row(horizontalArrangement=Arrangement.spacedBy(14.dp)) {
                            if(event.cloud!=null || event.channelId.isNotBlank()) FocusButton(tr("Ver contenido","View title")) { state.launch { state.socialAction { state.openCalendarEvent(event);close() } } }
                            if(event.owner==state.session?.userId) FocusButton(tr("Eliminar evento","Delete event"),destructive=true) { state.launch { state.socialAction { state.deleteCalendarEvent(event.id) } } }
                        }
                    }
                }
            }
        }
    }
    if(subscriptionsRP120) CalendarSubscriptionsRP120(state) {subscriptionsRP120=false}
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
    var channel by remember { mutableStateOf<LiveChannel?>(null) }
    var channels by remember { mutableStateOf<List<LiveChannel>>(emptyList()) }
    var categories by remember { mutableStateOf<List<LiveCategory>>(emptyList()) }
    var category by remember { mutableStateOf<LiveCategory?>(null) }
    var offset by remember { mutableStateOf(0) }
    var more by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    val repo=remember { LiveRepository() }
    LaunchedEffect(kind) {
        if(kind=="live" || kind=="sports") state.socialAction {
            categories=repo.categories(state.calendarLiveManifest(kind));category=categories.firstOrNull()
        }
    }
    LaunchedEffect(kind,category,query) {
        val chosen=category
        if((kind=="live" || kind=="sports") && chosen!=null) {
            loading=true
            try { state.socialAction {
                kotlinx.coroutines.delay(300)
                channels=repo.channels(state.calendarLiveManifest(kind),chosen,search=query)
                offset=channels.size;more=chosen.paginated && channels.isNotEmpty()
            } } finally { loading=false }
        }
    }
    PlayerMenuDialog(onDismissRequest=close,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        LazyColumn(Modifier.width(760.dp).heightIn(max=550.dp).background(Panel,RoundedCornerShape(20.dp)).padding(24.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
            item { Text(tr("Crear evento","Create event"),color=Color.White,fontSize=26.sp) }
            item { Row(horizontalArrangement=Arrangement.spacedBy(14.dp)) { listOf("movie" to tr("Película","Movie"),"series" to tr("Serie","Series"),"live" to "Live TV","sports" to "Live Sports").forEach { (k,name) -> FocusButton(name,primary=kind==k,modifier=if(k=="movie") Modifier.focusRequester(first) else Modifier) { kind=k;item=null;channel=null;channels=emptyList();categories=emptyList();category=null;results=emptyList();query="" } } } }
            item { NativeTextField(title,{title=it},tr("Título del evento","Event title")) }
            item { NativeTextField(date,{date=it},"YYYY-MM-DD HH:mm") }
            if(kind=="movie" || kind=="series") {
                item { NativeTextField(query,{query=it},tr("Buscar contenido","Search titles"));FocusButton(tr("Buscar","Search")) { state.launch { state.socialAction { val requestedKind=kind;val requestedQuery=query;val found=state.tmdb.search(requestedQuery).filter { it.type==requestedKind };if(kind==requestedKind && query==requestedQuery) results=found } } } }
                items(results.chunked(3)) { row ->
                    Row(Modifier.fillMaxWidth().padding(horizontal=8.dp,vertical=12.dp),horizontalArrangement=Arrangement.spacedBy(22.dp)) {
                        row.forEach { result -> Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                            MediaCard(result,modifier=Modifier.fillMaxWidth(),showCaption=false,allowContextMenu=false) { item=result;title=result.title }
                            Text(result.title+" · "+result.year,color=if(item?.cloudId==result.cloudId) Color.White else Muted,maxLines=2)
                        } }
                        repeat(3-row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
                item { Text(tr("Contenido seleccionado","Selected title")+": "+item?.title.orEmpty(),color=Muted) }
                if(kind=="series") item { Row(horizontalArrangement=Arrangement.spacedBy(14.dp)) { NativeTextField(season,{season=it},tr("Temporada","Season"),modifier=Modifier.weight(1f));NativeTextField(episode,{episode=it},tr("Episodio","Episode"),modifier=Modifier.weight(1f)) } }
            }
            if(kind=="live" || kind=="sports") {
                item { androidx.compose.foundation.lazy.LazyRow(horizontalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(8.dp)) {
                    items(categories,key={it.type+":"+it.id+":"+it.genre}) { c -> FocusButton(c.name,primary=category==c) { channel=null;category=c } }
                } }
                item { NativeTextField(query,{query=it},tr("Buscar canal o evento","Search channel or event")) }
                if(loading) item { Text(tr("Cargando…","Loading…"),color=Muted) }
                items(channels.filter { category?.searchable==true || query.isBlank() || it.name.contains(query,true) }.distinctBy { it.id },key={it.id}) { live ->
                    Row(Modifier.fillMaxWidth().padding(8.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                        AsyncImage(model=live.poster,contentDescription=null,modifier=Modifier.size(70.dp),contentScale=ContentScale.Fit)
                        FocusButton(live.name,primary=channel?.id==live.id,modifier=Modifier.weight(1f)) { channel=live;title=live.name }
                    }
                }
                if(more) item { FocusButton(tr("Cargar más","Load more")) { if(!loading) state.launch {
                    loading=true
                    try { state.socialAction { category?.let { c ->
                        val requestedKind=kind;val requestedQuery=query;val requestedOffset=offset
                        val next=repo.channels(state.calendarLiveManifest(requestedKind),c,requestedOffset,requestedQuery)
                        if(kind==requestedKind && category==c && query==requestedQuery) { channels=(channels+next).distinctBy { it.id };offset=requestedOffset+next.size;more=next.isNotEmpty() }
                    } } } finally { loading=false }
                } } }
                item { Text(tr("Seleccionado: ","Selected: ")+channel?.name.orEmpty(),color=Muted) }
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
                        require(if(kind=="live" || kind=="sports") channel!=null else item?.type==kind) { tr("Selecciona el contenido","Select a title") }
                        val s=if(kind=="series") season.toIntOrNull() ?: 0 else 0;val e=if(kind=="series") episode.toIntOrNull() ?: 0 else 0
                        require(kind!="series" || s>0 && e>0) { tr("Temporada/episodio inválidos","Invalid season/episode") }
                        state.createCalendarEvent(title,kind,date,if(kind=="live" || kind=="sports") null else item?.cloudId,s,e,guests.toList(),channel);close()
                    } } finally { saving=false }
                } }
                FocusButton(tr("Cancelar","Cancel")) { close() }
            } }
        }
    }
}

@Composable
fun PlayerPartyChatDialog(state: AppState, close: () -> Unit) {
    val room=state.watchParty?.roomCode
    var showQr by remember(room) { mutableStateOf(false) }
    var draft by remember(room) { mutableStateOf("") }
    var sending by remember(room) { mutableStateOf(false) }
    val closeFocus=remember { FocusRequester() }
    val listState=rememberLazyListState()
    val messages=state.partyMessages.filter { it.kind=="chat" }
    LaunchedEffect(Unit) { withFrameNanos { };runCatching { closeFocus.requestFocus() } }
    LaunchedEffect(messages.lastOrNull()?.id) { if(messages.isNotEmpty()) listState.scrollToItem(messages.lastIndex) }
    PlayerMenuDialog(onDismissRequest=close,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Column(Modifier.width(850.dp).heightIn(max=570.dp).background(Panel,RoundedCornerShape(22.dp)).padding(24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Text("${tr("Chat del Party","Party chat")} · ${room.orEmpty()}",color=Color.White,fontSize=26.sp,modifier=Modifier.weight(1f))
                if(room!=null) {
                    FocusButton(tr("Chat QR","Chat QR")) { showQr=true }
                    Spacer(Modifier.width(10.dp))
                }
                FocusButton(tr("Cerrar","Close"),modifier=Modifier.focusRequester(closeFocus)) { close() }
            }
            LazyColumn(state=listState,modifier=Modifier.weight(1f,false).fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                if(messages.isEmpty()) item { Text(tr("Todavía no hay mensajes.","No messages yet."),color=Muted) }
                items(messages,key={ it.id }) { message ->
                    val author=if(message.userId==state.session?.userId) state.nickname.ifBlank { tr("Tú","You") }
                        else state.friendPeople[message.userId] ?: tr("Participante","Participant")
                    Column(Modifier.fillMaxWidth().background(Panel,RoundedCornerShape(10.dp)).padding(12.dp).focusable()) {
                        Text(author,color=Muted,fontSize=13.sp)
                        Text(message.body,color=Color.White,fontSize=18.sp)
                    }
                }
            }
            if(room==null) Text(tr("Únete a un Party para conversar.","Join a Party to chat."),color=Muted)
            Row(horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=Alignment.CenterVertically) {
                NativeTextField(draft,{ draft=it.take(500) },tr("Escribe un mensaje","Write a message"),modifier=Modifier.weight(1f))
                FocusButton(if(sending) tr("Enviando…","Sending…") else tr("Enviar","Send")) {
                    val text=draft.trim()
                    if(text.isNotBlank() && room!=null && !sending && state.watchParty?.roomCode==room) {
                        sending=true
                        state.launch {
                            try {
                                state.error=null
                                state.sendPartyEvent("chat",text)
                                if(state.error==null) draft=""
                            } finally { sending=false }
                        }
                    }
                }
            }
            state.error?.let { Text(it,color=Color.White,fontSize=13.sp,maxLines=2) }
        }
    }
    if(showQr && room!=null) PlayerMenuDialog(onDismissRequest={ showQr=false },properties=DialogProperties(usePlatformDefaultWidth=false)) {
        val qrClose=remember { FocusRequester() }
        LaunchedEffect(Unit) { withFrameNanos { };runCatching { qrClose.requestFocus() } }
        Column(Modifier.width(440.dp).background(Panel,RoundedCornerShape(20.dp)).padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp),horizontalAlignment=Alignment.CenterHorizontally) {
            Text("Chat · $room",color=Color.White,fontSize=24.sp)
            QrCode("https://randy3m2.github.io/miflix-tv-native/party/?room=$room",220.dp)
            Text(tr("Escanea para usar el chat desde tu celular.","Scan to chat from your phone."),color=Muted)
            FocusButton(tr("Volver al chat","Back to chat"),modifier=Modifier.focusRequester(qrClose)) { showQr=false }
        }
    }

}

/** Fits the Friends content viewport. Each pane scrolls within its own measured bounds. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun InlineCalendar(state: AppState,modifier: Modifier=Modifier) {
    val androidContextRP120=androidx.compose.ui.platform.LocalContext.current.applicationContext
    var offset by remember { mutableIntStateOf(0) }
    var day by remember { mutableStateOf<String?>(null) }
    var creating by remember { mutableStateOf(false) }
    val month=remember(offset) { Calendar.getInstance().apply { add(Calendar.MONTH,offset);set(Calendar.DAY_OF_MONTH,1) } }
    val prefix=SimpleDateFormat("yyyy-MM",Locale.US).format(month.time)
    val subscribedEventsRP120=rememberSportEventsRP120(state,prefix)
    val subscribedByDayRP120=remember(subscribedEventsRP120) {subscribedEventsRP120.groupBy {it.day()}}
    var subscriptionsRP120 by remember {mutableStateOf(false)}
    var eventSearchRP120 by remember {mutableStateOf("")}
    val days=List((month.get(Calendar.DAY_OF_WEEK)+5)%7) { 0 }+(1..month.getActualMaximum(Calendar.DAY_OF_MONTH)).toList()
    LaunchedEffect(state.session?.userId) { if(state.session!=null) state.socialAction { state.refreshCalendar();state.refreshFriends() } }
    BoxWithConstraints(modifier) {
    val dayHeight=((maxHeight-105.dp)/6f).coerceIn(34.dp,48.dp)
    Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            FocusButton("‹",horizontalPadding=14.dp) { offset--;day=null }
            Text(prefix,color=Color.White,fontSize=20.sp,modifier=Modifier.weight(1f))
            FocusButton("›",horizontalPadding=14.dp) { offset++;day=null }
            FocusButton(tr("Suscripciones","Subscriptions")) {subscriptionsRP120=true}
            FocusButton(tr("Crear evento","Create event")) { creating=true }
            FocusButton(tr("Actualizar","Refresh")) { state.launch { state.socialAction { state.refreshCalendar() };withContext(Dispatchers.IO) {com.miflix.native2.data.CalendarStoreRP120.refresh(androidContextRP120,com.miflix.native2.data.CalendarStoreRP120.owner(androidContextRP120,state.activeProfile.id),true)} } }
        }
        Row(Modifier.weight(1f).fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(16.dp)) {
            LazyColumn(Modifier.weight(1f).fillMaxHeight().background(Panel,RoundedCornerShape(14.dp)).padding(12.dp),
                contentPadding=PaddingValues(bottom=12.dp),verticalArrangement=Arrangement.spacedBy(5.dp)) {
                item { Row { tr("L M X J V S D","M T W T F S S").split(" ").forEach { Text(it,color=Muted,fontSize=11.sp,modifier=Modifier.weight(1f)) } } }
                items(days.chunked(7)) { week ->
                    Row(horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                        week.forEach { n ->
                            if(n==0) Spacer(Modifier.weight(1f)) else {
                                val date="$prefix-${n.toString().padStart(2,'0')}"
                                val sporting=subscribedByDayRP120[date].orEmpty()
                                val count=state.calendarEvents.count { eventLocal(it.starts).startsWith(date) }+sporting.size
                                CalendarDay(n,count,day==date,Modifier.weight(1f),height=dayHeight,logosRP120=sporting.map {it.logos.firstOrNull() ?: com.miflix.native2.data.CalendarSourcesRP120.logo(it.league)}) { day=if(day==date) null else date }
                            }
                        }
                        repeat(7-week.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
            LazyColumn(Modifier.weight(1f).fillMaxHeight(),contentPadding=PaddingValues(bottom=16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                item { Text(day ?: tr("Eventos del mes","This month's events"),color=Muted,fontSize=13.sp) }
                item {NativeTextField(eventSearchRP120,{eventSearchRP120=it},tr("Buscar equipo, liga o enfrentamiento","Find team, league or matchup"),modifier=Modifier.fillMaxWidth())}
                val sportRowsRP120=subscribedEventsRP120.filter {(eventSearchRP120.isNotBlank() || it.day().startsWith(day ?: prefix)) &&
                    eventSearchRP120.trim().split(" ").filter {q ->q.isNotBlank()}.all {q ->com.miflix.native2.data.IptvPlaylist.searchKey(it.league+" "+it.title).contains(com.miflix.native2.data.IptvPlaylist.searchKey(q))}}
                items(sportRowsRP120,key={"sport:"+it.key}) {event ->CalendarSportEventRP120(state,event)}
                val events=state.calendarEvents.filter { eventLocal(it.starts).startsWith(day ?: prefix) }
                if(events.isEmpty() && sportRowsRP120.isEmpty()) item { Text(tr("No hay eventos en este período","No events in this period"),color=Muted) }
                items(events,key={it.id}) { event ->
                    Column(Modifier.fillMaxWidth().background(Panel,RoundedCornerShape(12.dp)).padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        Text(event.title,color=Color.White,fontSize=17.sp,maxLines=2)
                        Text(eventLocal(event.starts),color=Muted,fontSize=12.sp)
                        Text(if(event.owner==state.session?.userId) tr("Organizado por ti","Organized by you") else tr("Te han invitado","You're invited"),color=Muted,fontSize=12.sp)
                        FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                            if(event.cloud!=null || event.channelId.isNotBlank()) FocusButton(tr("Ver contenido","View title")) { state.launch { state.socialAction { state.openCalendarEvent(event) } } }
                            if(event.owner==state.session?.userId) FocusButton(tr("Eliminar","Delete"),destructive=true) { state.launch { state.socialAction { state.deleteCalendarEvent(event.id) } } }
                        }
                    }
                }
            }
        }
    }
    }
    if(subscriptionsRP120) CalendarSubscriptionsRP120(state) {subscriptionsRP120=false}
    if(creating) CreateEventDialog(state,day) { creating=false }
}

@Composable
private fun CalendarDay(number: Int,count: Int,selected: Boolean,modifier: Modifier,height: androidx.compose.ui.unit.Dp=34.dp,logosRP120:List<String> = emptyList(),onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(modifier.height(height).onFocusChanged { focused=it.isFocused }.focusable().tvClick(onClick).clickable(onClick=onClick)
        .background(GlassActionBrushRP108(focused,selected),RoundedCornerShape(7.dp))
        .border(1.dp,if(focused) Accent else if(selected) PanelBorder else Color.Transparent,RoundedCornerShape(7.dp)),contentAlignment=Alignment.Center) {
        Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(2.dp)) {
            Text(number.toString()+(if(count>0) " •" else ""),color=Color.White,fontSize=12.sp,fontWeight=if(focused || selected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal,maxLines=1)
            if(logosRP120.isNotEmpty()) Row(horizontalArrangement=Arrangement.spacedBy(2.dp)) {
                logosRP120.distinct().take(2).forEach {EventLogoRP120(it,"EV",Modifier.size(14.dp))}
            }
        }
    }
}
