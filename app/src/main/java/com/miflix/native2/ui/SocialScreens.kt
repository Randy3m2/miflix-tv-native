package com.miflix.native2.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
    Image(painterResource(R.drawable.brunio_social_background),null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x33030912),Color(0xAA030712)))))
}

@Composable
fun FriendsScreen(state: AppState) {
    var tab by remember { mutableStateOf(0) }
    var search by remember { mutableStateOf("") }
    var person by remember { mutableStateOf<FriendActivity?>(null) }
    var manage by remember { mutableStateOf(false) }
    var refreshing by remember { mutableStateOf(false) }
    BackHandler { state.screen=Screen.HOME }
    Row(Modifier.fillMaxSize()) {
        Sidebar(Screen.FRIENDS,state.activeProfile.name,{ state.screen=it },state.notifications.size)
        BoxWithConstraints(Modifier.weight(1f).fillMaxHeight()) {
            val columns=if(maxWidth>=720.dp) 4 else 3
            SocialBackdrop()
            LazyColumn(Modifier.fillMaxSize().padding(horizontal=28.dp),contentPadding=PaddingValues(vertical=28.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
                item {
                    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                        Text("Friends & Party",color=Color.White,fontSize=28.sp,modifier=Modifier.weight(1f))
                        FocusButton(if(refreshing) "Actualizando…" else "Refresh") { if(!refreshing) state.launch {
                            refreshing=true
                            try { state.socialAction { state.refreshFriendsDirectory() } } finally { refreshing=false }
                        } }
                        FocusButton("Create Party") { state.screen=Screen.WATCH_PARTY }
                    }
                    Text("Actualiza cuando quieras ver la actividad reciente de tus amigos",color=Muted,fontSize=13.sp)
                }
                item {
                    Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                        listOf("Friends","Parties","Requests").forEachIndexed { index,label -> FocusButton(label,primary=tab==index) { tab=index } }
                        FocusButton("Mi nickname / frases") { manage=true }
                    }
                }
                if(state.session==null) item { FocusButton("Sign in") { state.screen=Screen.SETTINGS } }
                else {
                    if(tab==2) {
                        item { AccessRequests(state) }
                        items(state.friendRows.filter { it.status=="pending" }) { row ->
                            val me=state.session?.userId; val other=if(row.sender==me) row.receiver else row.sender
                            val name=state.friendPeople[other] ?: "Friend"
                            Row(Modifier.fillMaxWidth().background(Color(0x99172032),RoundedCornerShape(16.dp)).padding(18.dp),horizontalArrangement=Arrangement.spacedBy(18.dp)) {
                                Text("@$name",color=Color.White,modifier=Modifier.weight(1f))
                                if(row.receiver==me) FocusButton("Aceptar amistad") { state.launch { state.addFriend(other) } }
                                else Text("Solicitud enviada",color=Muted)
                            }
                        }
                    } else {
                        item { NativeTextField(search,{ search=it },"Buscar amigos",modifier=Modifier.width(320.dp)) }
                        val friends=state.friendActivity.filter { (tab==0 || it.room!=null) && it.nickname.contains(search,true) }
                        if(friends.isEmpty()) item { Text("No hay resultados · pulsa Refresh para cargar tus amigos",color=Muted) }
                        items(friends.chunked(columns)) { group ->
                            Row(horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                                group.forEach { friend -> FriendTile(friend,Modifier.weight(1f)) { person=friend } }
                                repeat(columns-group.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                    if(state.partyStatus.isNotBlank()) item { Text(state.partyStatus,color=Muted) }
                }
            }
        }
    }
    person?.let { friend ->
        Dialog(onDismissRequest={ person=null }) {
            Column(Modifier.width(430.dp).background(Panel,RoundedCornerShape(22.dp)).padding(24.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
                ProfileAvatar(Profile(friend.id,friend.nickname,friend.avatar),Modifier.size(100.dp))
                Text("@${friend.nickname}",color=Color.White,fontSize=25.sp)
                Text(if(friend.title.isBlank()) if(friend.online) "Online" else "Offline" else "Watching ${friend.title}",color=Muted)
                friend.room?.let { code ->
                    FocusButton(if(state.pendingPartyCode==code) "Esperando aprobación" else "Request access",primary=true) {
                        state.launch { state.socialAction { state.joinWatchParty(code); person=null } }
                    }
                }
                FocusButton("Cerrar") { person=null }
            }
        }
    }
    if(manage) SocialSettingsDialog(state) { manage=false }
}

@Composable
private fun FriendTile(friend: FriendActivity,modifier: Modifier,onClick: () -> Unit) {
    var focused by remember(friend.id) { mutableStateOf(false) }
    Column(modifier.height(225.dp).onFocusChanged { focused=it.isFocused }.focusable().tvClick(onClick).clickable(onClick=onClick)
        .background(if(focused) Color(0xDD27364E) else Color(0x99131C2C),RoundedCornerShape(18.dp))
        .border(if(focused) 2.dp else 1.dp,if(focused) Color.White else Color(0x334F6078),RoundedCornerShape(18.dp)).padding(16.dp),
        horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(8.dp)) {
        ProfileAvatar(Profile(friend.id,friend.nickname,friend.avatar),Modifier.size(84.dp))
        Text(friend.nickname,color=Color.White,fontSize=18.sp,maxLines=1)
        Text(if(friend.title.isNotBlank()) "Watching ${friend.title}" else if(friend.online) "Online" else "Offline",color=if(friend.online) Color(0xFF8DE5B8) else Muted,fontSize=13.sp,maxLines=2)
        if(friend.room!=null) Text("Party · Request access",color=Color(0xFF9DD4FF),fontSize=12.sp)
    }
}

@Composable
private fun SocialSettingsDialog(state: AppState,close: () -> Unit) {
    var nick by remember { mutableStateOf(state.nickname) }
    var find by remember { mutableStateOf("") }
    var phrases by remember { mutableStateOf(state.partyPhrases.joinToString("|")) }
    Dialog(onDismissRequest=close,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        LazyColumn(Modifier.width(640.dp).heightIn(max=460.dp).background(Panel,RoundedCornerShape(22.dp)).padding(24.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
            item { Text("Mi perfil social",color=Color.White,fontSize=25.sp) }
            item { NativeTextField(nick,{ nick=it },"Nickname"); FocusButton("Guardar nickname") { state.launch { state.saveNickname(nick) } } }
            item { NativeTextField(find,{ find=it },"Nickname de tu amigo"); FocusButton("Agregar amigo") { state.launch { state.findAndAddFriend(find) } } }
            item { NativeTextField(phrases,{ phrases=it },"Frases separadas por |"); FocusButton("Guardar frases") { state.savePhrases(phrases) } }
            item { FocusButton("Cerrar") { close() } }
        }
    }
}

@Composable
fun AccessRequests(state: AppState) {
    if(state.accessRequests.isNotEmpty()) Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Text("Solicitudes de acceso · ${state.accessRequests.size}",color=Color.White,fontSize=22.sp)
        state.accessRequests.toList().forEach { request ->
            Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                Text("@${request.nickname}",color=Color.White)
                FocusButton("Aceptar") { state.launch { state.decideAccess(request,true) } }
                FocusButton("Rechazar") { state.launch { state.decideAccess(request,false) } }
            }
        }
    }
}

@Composable
fun PartyActionsDialog(state: AppState, close: () -> Unit) {
    val first=remember { FocusRequester() }
    LaunchedEffect(Unit) { delay(100); first.requestFocus() }
    Dialog(onDismissRequest=close,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Column(Modifier.fillMaxWidth().background(Panel).padding(28.dp)) {
            Text("React to the party",color=Color.White,fontSize=24.sp)
            LazyRow(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                items(PartyEmojis) { emoji -> FocusButton(emoji, modifier=if(emoji==PartyEmojis.first()) Modifier.focusRequester(first) else Modifier) { state.launch { state.sendPartyEvent("emoji",emoji) }; close() } }
            }
            Spacer(Modifier.height(12.dp))
            LazyColumn(Modifier.heightIn(max=240.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                items(state.partyPhrases) { phrase -> FocusButton(phrase,modifier=Modifier.fillMaxWidth()) { state.launch { state.sendPartyEvent("phrase",phrase) }; close() } }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                FocusButton("Room / mobile chat QR") { close(); state.screen=Screen.WATCH_PARTY }
                FocusButton("Close") { close() }
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
                state.traktStatus="Code expired. Connect again."; state.traktDevice=null
            } catch(e: Exception) {
                if(e is kotlinx.coroutines.CancellationException)throw e
                state.traktStatus=e.message.orEmpty(); state.traktDevice=null
            }
        }
    }
    BackHandler { state.screen=Screen.SETTINGS }
    LazyColumn(Modifier.fillMaxSize().background(Bg).padding(35.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
        item { Text("Trakt",color=Color.White,fontSize=30.sp); Text(state.traktStatus,color=Muted) }
        item {
            Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                if(!state.traktConnected) FocusButton("Connect Trakt") { state.launch { state.traktStart() } }
                else {
                    FocusButton("Import Watchlist") { state.launch { state.traktImport() } }
                    FocusButton("Recent History") { state.launch { state.socialAction {
                        val s=state.session?:return@socialAction
                        val rows=state.trakt.call(s,"history").optJSONArray("items")
                        history=if(rows==null)emptyList() else (0 until rows.length()).map { i ->
                            val row=rows.getJSONObject(i); val media=row.optJSONObject("movie")?:row.optJSONObject("show")
                            val ep=row.optJSONObject("episode")
                            media?.optString("title").orEmpty()+(if(ep!=null) " · S${ep.optInt("season")} E${ep.optInt("number")}" else "")
                        }
                    } } }
                    FocusButton("Disconnect") { state.launch { state.traktDisconnect() } }
                }
                FocusButton("Back") { state.screen=Screen.SETTINGS }
            }
        }
        if(device!=null) item {
            val verification=device.optString("verification_url","https://trakt.tv/activate")
            Row(horizontalArrangement=Arrangement.spacedBy(24.dp)) {
                QrCode(verification,210.dp)
                Column { Text(device.optString("user_code"),color=Color.White,fontSize=35.sp); Text(verification,color=Color.White) }
            }
        }
        item { Text("Watched movies and episodes sync after 80% playback. Live TV is excluded.",color=Muted) }
        items(history) { Text(it,color=Color.White,fontSize=17.sp) }
    }
}

@Composable
fun NotificationsScreen(state: AppState) {
    BackHandler { state.screen = Screen.HOME }
    Row(Modifier.fillMaxSize().background(Bg)) {
        Sidebar(Screen.NOTIFICATIONS,state.activeProfile.name,{state.screen=it},state.notifications.size)
        LazyColumn(Modifier.weight(1f).padding(30.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            item {
                Text("Notificaciones · ${state.notifications.size}",color=Color.White,fontSize=30.sp)
                Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    FocusButton("Revisar ahora") { state.launch { runCatching { state.checkNotifications() }.onFailure { state.error=it.message } } }
                    FocusButton("Limpiar notificaciones") { state.clearNotifications() }
                }
                Text("Nuevos estrenos de tus favoritos. Se revisan al abrir MiFlix y cada 30 minutos mientras está abierta.",color=Muted)
            }
            if(state.notifications.isEmpty()) item { Text("No hay notificaciones nuevas",color=Color.White) }
            items(state.notifications.toList(),key={it.id}) { notice ->
                Column {
                    Text(notice.item.title,color=Color.White,fontSize=22.sp)
                    Text(notice.message,color=Muted)
                    FocusButton("Ver contenido") { state.launch { state.open(notice.item) } }
                }
            }
        }
    }
}
