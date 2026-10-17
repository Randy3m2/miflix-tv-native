package com.miflix.native2.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.focusGroup
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.miflix.native2.R
import com.miflix.native2.data.FriendActivity
import com.miflix.native2.model.Profile
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Text
import kotlinx.coroutines.delay

val PartyEmojis = listOf("😂", "❤️", "🔥", "😱", "👏", "🍿")

@Composable
fun SocialBackdrop() {
    // A solid dark surface avoids decoding a full-screen bitmap and applying a color matrix.
    Box(Modifier.fillMaxSize().background(Bg))
}

@Composable
fun FriendsScreen(state: AppState) {
    var momentsOpen by remember {mutableStateOf(false)}
    var tab by remember { mutableStateOf(0) }
    var search by remember { mutableStateOf("") }
    var person by remember { mutableStateOf<FriendActivity?>(null) }
    var recommendations by remember { mutableStateOf(false) }
    var showAllFriends by remember { mutableStateOf(false) }
    var previewRevision by remember { mutableIntStateOf(0) }
    var recommendationsPreview by remember(state.session?.userId) { mutableStateOf<List<com.miflix.native2.data.FriendRecommendation>>(emptyList()) }
    var previewsLoading by remember { mutableStateOf(false) }
    var previewError by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(state.session?.userId,previewRevision) {
        val owner=state.session?.userId ?: return@LaunchedEffect
        previewsLoading=true;previewError=null
        try {
            val rows=state.friendRecommendations()
            if(state.session?.userId==owner) recommendationsPreview=rows
            state.refreshCalendar()
        } catch(e: kotlinx.coroutines.CancellationException) { throw e }
        catch(e: Exception) { previewError=e.message }
        finally { previewsLoading=false }
    }
    BackHandler { if(tab==3) tab=0 else state.screen=Screen.HOME }
    val contentFocus=remember { FocusRequester() }
    Column(Modifier.fillMaxSize()) {
        Sidebar(Screen.FRIENDS,state.activeProfile.name,{ state.screen=it },state.unreadNoticeCount,state.activeProfile,onMoveDown={ contentFocus.requestFocus(); Unit })
        BoxWithConstraints(Modifier.weight(1f).fillMaxHeight().focusRequester(contentFocus).focusGroup()) {
            val columns=if(maxWidth>=720.dp) 3 else 2
            SocialBackdrop()
            Column(Modifier.fillMaxSize().padding(horizontal=34.dp,vertical=16.dp)) {
                    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                        Text(tr("Amigos y salas","Friends & Party"),color=Color.White,fontSize=28.sp, lineHeight=34.sp,modifier=Modifier.weight(1f))
                        FocusButton(if(state.friendsTabLoading) tr("Actualizando…","Refreshing…") else tr("Actualizar","Refresh")) { state.refreshFriendsTab();previewRevision++ }
                        FocusButton(if(state.watchParty==null) tr("Crear sala","Create Party") else tr("Mi sala","My Party")) { state.screen=Screen.WATCH_PARTY }
                    }
                    Text(tr("Descubre qué están viendo tus amigos.","Discover what your friends are watching."),color=Muted,fontSize=13.sp, lineHeight=16.sp)
                    FocusButton(tr("Momentos compartidos · actualizar manualmente","Shared moments · manual refresh")) {momentsOpen=true}

                Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                        listOf(0 to tr("Amigos","Friends"),2 to tr("Solicitudes","Requests")).forEach { (index,label) -> NavigationTab(label,selected=tab==index) { tab=index } }
                        NavigationTab(tr("Calendario","Calendar"),selected=tab==3) { tab=3 }
                    }

                Spacer(Modifier.height(12.dp))
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    if(tab==3 && state.session!=null) InlineCalendar(state,Modifier.fillMaxSize())
                    else LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(bottom=32.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                if(state.session==null) item { FocusButton(tr("Iniciar sesión","Sign in")) { state.screen=Screen.SETTINGS } }
                else {
                    if(tab==0 || tab==2) items(state.partyInvitations,key={"party-invite:${it.room}"}) {invitation ->
                        Row(Modifier.fillMaxWidth().background(Panel,RoundedCornerShape(18.dp)).padding(16.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text(tr("Invitación de ","Party invitation from ")+invitation.host,color=Accent,fontSize=14.sp)
                                Text(invitation.title,color=Color.White,fontSize=18.sp,maxLines=1)
                                Text("Party · ${invitation.room}",color=Muted,fontSize=12.sp)
                            }
                            FocusButton(tr("Unirme","Join")) {state.launch {state.socialAction {state.joinWatchParty(invitation.room)}}}
                        }
                    }
                    if(tab==2) {
                        item { AccessRequests(state) }
                        items(state.friendRows.filter { it.status=="pending" }) { row ->
                            val me=state.session?.userId; val other=if(row.sender==me) row.receiver else row.sender
                            val name=state.friendPeople[other] ?: "Friend"
                            Row(Modifier.fillMaxWidth().background(Panel,RoundedCornerShape(16.dp)).padding(18.dp),horizontalArrangement=Arrangement.spacedBy(18.dp)) {
                                Text("@$name",color=Color.White,modifier=Modifier.weight(1f))
                                if(row.receiver==me) FocusButton(tr("Aceptar amistad","Accept friend request")) { state.launch { state.addFriend(other) } }
                                else Text(tr("Solicitud enviada","Request sent"),color=Muted)
                            }
                        }
                    } else {
                        item { NativeTextField(search,{ search=it },tr("Buscar amigos","Search friends"),modifier=Modifier.fillMaxWidth()) }
                        val friends=state.friendActivity.filter { (tab==0 || it.room!=null) && it.nickname.contains(search,true) }
                        if(friends.isEmpty()) item { Text(tr("No hay resultados · pulsa Refresh para cargar tus amigos","No results · Select Refresh to load your friends"),color=Muted) }
                        val displayedFriends=if(showAllFriends || tab!=0 || search.isNotBlank()) friends else friends.take(columns)
                        items(displayedFriends.chunked(columns)) { group ->
                            Row(horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                                group.forEach { friend -> FriendTile(friend,Modifier.weight(1f)) { person=friend } }
                                repeat(columns-group.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                        if(tab==0 && friends.size>columns && search.isBlank()) item { FocusButton(if(showAllFriends) tr("Mostrar menos amigos","Show fewer friends") else tr("Ver todos los amigos","View all friends")+" (${friends.size})") { showAllFriends=!showAllFriends } }
                    }
                    if(tab==0) item {
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(22.dp)) {
                            Column(Modifier.weight(.6f),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                                Text(tr("Tus amigos recomiendan","Friends recommend"),color=Color.White,fontSize=22.sp,fontWeight=androidx.compose.ui.text.font.FontWeight.Bold)
                                if(previewsLoading) Text(tr("Cargando…","Loading…"),color=Muted)
                                else if(recommendationsPreview.isEmpty()) Text(tr("Las recomendaciones de tus amigos aparecerán aquí.","Your friends' picks will appear here."),color=Muted,fontSize=13.sp)
                                Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                                    recommendationsPreview.take(3).forEach { recommendation ->
                                        FriendPreviewCard(recommendation,Modifier.weight(1f)) {
                                            state.launch { state.socialAction { state.tmdb.byCloudId(recommendation.cloud)?.let { state.open(it) } } }
                                        }
                                    }
                                }
                                FocusButton(tr("Ver todas","View all")) { recommendations=true }
                            }
                            Column(Modifier.weight(.4f),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                                Text(tr("Próximo evento","Next event"),color=Color.White,fontSize=22.sp,fontWeight=androidx.compose.ui.text.font.FontWeight.Bold)
                                val now=java.text.SimpleDateFormat("yyyy-MM-dd HH:mm",java.util.Locale.US).format(java.util.Date())
                                val next=state.calendarEvents.filter { eventLocal(it.starts)>=now }.minByOrNull { eventLocal(it.starts) }
                                Column(Modifier.fillMaxWidth().background(Panel,RoundedCornerShape(16.dp)).border(1.dp,PanelBorder,RoundedCornerShape(16.dp)).padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                                    NextEventSummary(state,next)
                                    FocusButton(tr("Abrir calendario","Open calendar")) { tab=3 }
                                }
                            }
                        }
                        previewError?.let { Text(tr("No se pudo actualizar parte de la información. Pulsa Refresh.","Some information could not refresh. Select Refresh."),color=Muted,fontSize=12.sp) }
                    }
                    if(state.partyStatus.isNotBlank()) item { Text(state.partyStatus,color=Muted) }
                }
            
                    }
                }
            }
        }
    }
    person?.let { friend -> FriendActionsDialog(state,friend,close={ person=null }) }
    if(momentsOpen) SharedMomentsInbox(state) {momentsOpen=false}
    if(recommendations) FriendRecommendationsDialog(state) { recommendations=false }
}

@Composable
private fun FriendTile(friend: FriendActivity,modifier: Modifier,onClick: () -> Unit) {
    var focused by remember(friend.id) { mutableStateOf(false) }
    Row(modifier.height(112.dp).onFocusChanged { focused=it.isFocused }.focusable().tvClick(onClick).clickable(onClick=onClick)
        .background(if(focused) FocusedPanel else Panel,RoundedCornerShape(16.dp))
        .border(1.dp,if(focused) Accent else PanelBorder,RoundedCornerShape(16.dp)).padding(16.dp),
        verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)) {
        ProfileAvatar(Profile(friend.id,friend.nickname,friend.avatar),Modifier.size(48.dp).clip(androidx.compose.foundation.shape.CircleShape))
        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(6.dp)) {
            Text("@"+friend.nickname,color=Color.White,fontSize=16.sp,lineHeight=20.sp,maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            Text(if(friend.online) tr("● En línea","● Online") else tr("Desconectado","Offline"),color=if(friend.online) Color(0xFF8DE5B8) else Muted,fontSize=12.sp)
            if(friend.title.isNotBlank()) Text(tr("Viendo ","Watching ")+friend.title,color=Muted,fontSize=12.sp,lineHeight=15.sp,maxLines=2,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            if(friend.room!=null) Text(tr("Solicitar acceso","Request access"),color=Accent,fontSize=11.sp,maxLines=1)
        }
    }
}

@Composable
internal fun SocialSettingsDialog(state: AppState,close: () -> Unit) {
    var nick by remember { mutableStateOf(state.nickname) }
    var find by remember { mutableStateOf("") }
    var phrases by remember { mutableStateOf(state.partyPhrases.joinToString("|")) }
    PlayerMenuDialog(onDismissRequest=close,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        LazyColumn(Modifier.width(640.dp).heightIn(max=460.dp).background(Panel,RoundedCornerShape(22.dp)).padding(24.dp),verticalArrangement=Arrangement.spacedBy(24.dp)) {
            item { Text(tr("Mi perfil social","My social profile"),color=Color.White,fontSize=25.sp, lineHeight=30.sp) }
            item { Column(verticalArrangement=Arrangement.spacedBy(16.dp)) { NativeTextField(nick,{ nick=it },tr("Apodo","Nickname")); FocusButton(tr("Guardar nickname","Save nickname")) { state.launch { state.saveNickname(nick) } } } }
            item { Column(verticalArrangement=Arrangement.spacedBy(16.dp)) { NativeTextField(find,{ find=it },tr("Apodo de tu amigo","Your friend's nickname")); FocusButton(tr("Agregar amigo","Add friend")) { state.launch { state.findAndAddFriend(find) } } } }
            item { Column(verticalArrangement=Arrangement.spacedBy(16.dp)) { NativeTextField(phrases,{ phrases=it },tr("Frases separadas por |","Phrases separated by |")); FocusButton(tr("Guardar frases","Save phrases")) { state.savePhrases(phrases) } } }
            item { FocusButton(tr("Cerrar","Close")) { close() } }
        }
    }
}

@Composable
fun AccessRequests(state: AppState) {
    if(state.accessRequests.isNotEmpty()) Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Text("${tr("Solicitudes de acceso","Access requests")} · ${state.accessRequests.size}",color=Color.White,fontSize=22.sp, lineHeight=26.sp)
        state.accessRequests.toList().forEach { request ->
            Row(horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                Text("@${request.nickname}",color=Color.White)
                FocusButton(tr("Aceptar","Accept")) { state.launch { state.decideAccess(request,true) } }
                FocusButton(tr("Rechazar","Reject")) { state.launch { state.decideAccess(request,false) } }
            }
        }
    }
}

@Composable
fun PartyActionsDialog(state: AppState, close: () -> Unit) {
    val first=remember { FocusRequester() }
    LaunchedEffect(Unit) { delay(100); first.requestFocus() }
    PlayerMenuDialog(onDismissRequest=close,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Column(Modifier.consumeOpeningPress().widthIn(max=530.dp).background(Panel, RoundedCornerShape(24.dp)).padding(24.dp)) {
            Text(tr("Frases del Party","Party phrases"),color=Color.White,fontSize=24.sp, lineHeight=29.sp)
            Spacer(Modifier.height(12.dp))
            LazyColumn(Modifier.heightIn(max=240.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                items(state.partyPhrases) { phrase -> FocusButton(phrase,modifier=Modifier.fillMaxWidth().then(if(phrase==state.partyPhrases.firstOrNull()) Modifier.focusRequester(first) else Modifier)) { state.launch { state.sendPartyEvent("phrase",phrase) }; close() } }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                FocusButton(tr("Cerrar","Close")) { close() }
            }
        }
    }
}

@Composable
fun TraktScreen(state: AppState) {
    var history by remember { mutableStateOf<List<String>>(emptyList()) }
    LaunchedEffect(state.session?.userId) { state.traktRefresh() }
    val device=state.traktDevice
    LaunchedEffect(device) {
        if(device!=null) {
            val end=System.currentTimeMillis()+device.optLong("expires_in",600)*1000
            var interval=device.optLong("interval",5).coerceAtLeast(5)
            try {
                while(System.currentTimeMillis()<end) {
                    delay(interval*1000)
                    if(state.traktPoll())return@LaunchedEffect
                    interval=(state.traktDevice?.optLong("interval",interval)?:interval).coerceAtMost(30)
                }
                state.traktStatus=tr("El código caducó. Conecta de nuevo.","Code expired. Connect again."); state.traktDevice=null
            } catch(e: Exception) {
                if(e is kotlinx.coroutines.CancellationException)throw e
                state.traktStatus=e.message.orEmpty(); state.traktDevice=null
            }
        }
    }
    BackHandler { state.screen=Screen.SETTINGS }
    LazyColumn(Modifier.fillMaxSize().background(Bg).padding(35.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
        item { Text("Trakt",color=Color.White,fontSize=30.sp, lineHeight=36.sp); Text(state.traktStatus,color=Muted) }
        item {
            Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                if(!state.traktConnected) FocusButton(tr("Conectar Trakt","Connect Trakt")) { state.launch { state.traktStart() } }
                else {
                    FocusButton(tr("Importar lista de Trakt","Import Watchlist")) { state.launch { state.traktImport() } }
                    FocusButton(tr("Historial reciente","Recent History")) { state.launch { state.socialAction {
                        val s=state.session?:return@socialAction
                        val rows=state.trakt.call(s,"history").optJSONArray("items")
                        history=if(rows==null)emptyList() else (0 until rows.length()).map { i ->
                            val row=rows.getJSONObject(i); val media=row.optJSONObject("movie")?:row.optJSONObject("show")
                            val ep=row.optJSONObject("episode")
                            media?.optString("title").orEmpty()+(if(ep!=null) " · S${ep.optInt("season")} E${ep.optInt("number")}" else "")
                        }
                    } } }
                    FocusButton(tr("Desconectar","Disconnect")) { state.launch { state.traktDisconnect() } }
                }
                FocusButton(tr("Volver","Back")) { state.screen=Screen.SETTINGS }
            }
        }
        if(device!=null) item {
            val verification=device.optString("verification_url","https://trakt.tv/activate")
            Row(horizontalArrangement=Arrangement.spacedBy(24.dp)) {
                QrCode(verification,210.dp)
                Column { Text(device.optString("user_code"),color=Color.White,fontSize=35.sp, lineHeight=42.sp); Text(verification,color=Color.White) }
            }
        }
        item { Text(tr("Las películas y episodios se sincronizan al superar el 80 %. La TV en vivo queda excluida.","Watched movies and episodes sync after 80% playback. Live TV is excluded."),color=Muted) }
        items(history) { Text(it,color=Color.White,fontSize=17.sp, lineHeight=21.sp) }
    }
}

@Composable
fun NotificationsScreen(state: AppState) {
    BackHandler { state.screen = Screen.HOME }
    val contentFocus=remember { FocusRequester() }
    Column(Modifier.fillMaxSize().background(Bg)) {
        Sidebar(Screen.NOTIFICATIONS,state.activeProfile.name,{state.screen=it},state.unreadNoticeCount,state.activeProfile,onMoveDown={ contentFocus.requestFocus(); Unit })
        LazyColumn(Modifier.weight(1f).padding(horizontal=34.dp).focusRequester(contentFocus).focusGroup(),contentPadding=PaddingValues(top=24.dp,bottom=40.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            item {
                Text("${tr("Notificaciones","Notifications")} · ${state.unreadNoticeCount}",color=Color.White,fontSize=30.sp, lineHeight=36.sp)
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    FocusButton(tr("Revisar ahora","Check now")) { state.launch { runCatching { state.checkNotifications() }.onFailure { state.error=it.message } } }
                    FocusButton(tr("Marcar como leídas","Mark as read")) {state.markNoticesRead()}
                    FocusButton(tr("Limpiar notificaciones","Clear notifications")) { state.clearNotifications() }
                }
                Spacer(Modifier.height(12.dp))
                Text(tr("Nuevos episodios y estrenos de tus favoritos.","New episodes and releases from your favorites."),color=Muted,fontSize=14.sp)
            }
            item(key=UpdateNotes.id) {
                Column(Modifier.fillMaxWidth().background(Panel,RoundedCornerShape(20.dp)).border(1.dp,if(state.noticeRead(UpdateNotes.id)) PanelBorder else Accent,RoundedCornerShape(20.dp)).padding(22.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    Text("What's new · BruniO ${com.miflix.native2.BuildConfig.VERSION_NAME}",color=Color.White,fontSize=24.sp)
                    FocusButton(tr("Ver todos los cambios","View all changes")) {state.changelogOpen=true}
                    UpdateNotes.changes.take(3).forEach {(es,en) -> Text("• "+tr(es,en),color=Muted,fontSize=15.sp) }
                    if(!state.noticeRead(UpdateNotes.id)) FocusButton(tr("Marcar como leído","Mark as read")) {state.markNoticesRead(UpdateNotes.id)}
                    else Text(tr("Leído","Read"),color=Muted)
                }
            }
            if(state.notifications.isEmpty() && state.noticeRead(UpdateNotes.id)) item { Text(tr("No hay notificaciones nuevas","No new notifications"),color=Color.White) }
            items(state.notifications.toList(),key={it.id}) { notice ->
                Row(Modifier.fillMaxWidth().background(Panel,RoundedCornerShape(16.dp)).border(1.dp,PanelBorder,RoundedCornerShape(16.dp)).padding(18.dp),
                    horizontalArrangement=Arrangement.spacedBy(20.dp),verticalAlignment=Alignment.CenterVertically) {
                    coil3.compose.AsyncImage(notice.item.poster ?: notice.item.backdrop,notice.item.title,Modifier.size(72.dp,108.dp).clip(RoundedCornerShape(10.dp)),contentScale=ContentScale.Crop)
                    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        Text(if(state.noticeRead(notice.id)) tr("LEÍDA","READ") else tr("NUEVO EN TU LISTA","NEW IN YOUR LIST"),color=Accent,fontSize=10.sp)
                        Text(notice.item.title,color=Color.White,fontSize=22.sp,lineHeight=26.sp,fontWeight=androidx.compose.ui.text.font.FontWeight.Bold)
                        Text(notice.message,color=Muted,fontSize=14.sp,maxLines=2)
                    }
                    FocusButton(tr("Ver contenido","View content")) { state.launch { state.markNoticesRead(notice.id);state.open(notice.item) } }
                }
            }
        }
    }
}

@Composable
private fun FriendPreviewCard(item: com.miflix.native2.data.FriendRecommendation,modifier: Modifier,onClick: () -> Unit) {
    var focused by remember(item.cloud) { mutableStateOf(false) }
    Column(modifier.onFocusChanged { focused=it.isFocused }.focusable().tvClick(onClick).clickable(onClick=onClick),verticalArrangement=Arrangement.spacedBy(6.dp)) {
        Box(Modifier.fillMaxWidth().aspectRatio(2f/3f).clip(RoundedCornerShape(12.dp)).background(Panel).border(1.dp,if(focused) Accent else PanelBorder,RoundedCornerShape(12.dp))) {
            coil3.compose.AsyncImage(item.poster,item.title,Modifier.fillMaxSize(),contentScale=ContentScale.Fit)
        }
        Text(item.title,color=Color.White,fontSize=12.sp,maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        Text("@"+item.nickname,color=Muted,fontSize=11.sp,maxLines=1)
    }
}

@Composable
private fun NextEventSummary(state: AppState,event: com.miflix.native2.data.CalendarEvent?) {
    var poster by remember(event?.id) { mutableStateOf<String?>(null) }
    LaunchedEffect(event?.id,state.session?.userId) {
        val id=event?.cloud
        if(id!=null) try { poster=state.tmdb.byCloudId(id)?.poster }
        catch(e: kotlinx.coroutines.CancellationException) { throw e }
        catch(_: Exception) { poster=null }
    }
    if(event==null) Text(tr("Sin eventos próximos","No upcoming events"),color=Color.White,fontSize=18.sp)
    else {
        Row(horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=Alignment.CenterVertically) {
            if(poster!=null) coil3.compose.AsyncImage(poster,event.title,Modifier.size(56.dp,84.dp).clip(RoundedCornerShape(8.dp)),contentScale=ContentScale.Crop)
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                Text(event.title,color=Color.White,fontSize=17.sp,maxLines=2,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                Text(eventLocal(event.starts),color=Muted,fontSize=12.sp)
                Text("${event.guests.size} "+tr("amigos invitados","friends invited"),color=Muted,fontSize=11.sp)
            }
        }
        Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            event.guests.take(3).forEach { id ->
                state.friendActivity.firstOrNull { it.id==id }?.let { friend ->
                    ProfileAvatar(Profile(friend.id,friend.nickname,friend.avatar),Modifier.size(26.dp).clip(androidx.compose.foundation.shape.CircleShape))
                }
            }
        }
    }
}
