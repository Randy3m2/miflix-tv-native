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
    Image(painterResource(R.drawable.brunio_social_background),null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop,
        colorFilter=androidx.compose.ui.graphics.ColorFilter.colorMatrix(
            androidx.compose.ui.graphics.ColorMatrix().apply { setToSaturation(0f) }))
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x55414141),Color(0xAA161616)))))
}

@Composable
fun FriendsScreen(state: AppState) {
    var tab by remember { mutableStateOf(0) }
    var search by remember { mutableStateOf("") }
    var person by remember { mutableStateOf<FriendActivity?>(null) }
    var manage by remember { mutableStateOf(false) }
    var refreshing by remember { mutableStateOf(false) }
    BackHandler { state.screen=Screen.HOME }
    Column(Modifier.fillMaxSize()) {
        Sidebar(Screen.FRIENDS,state.activeProfile.name,{ state.screen=it },state.notifications.size,state.activeProfile)
        BoxWithConstraints(Modifier.weight(1f).fillMaxHeight()) {
            val columns=if(maxWidth>=720.dp) 4 else 3
            SocialBackdrop()
            LazyColumn(Modifier.fillMaxSize().padding(horizontal=28.dp),contentPadding=PaddingValues(vertical=28.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
                item {
                    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                        Text(tr("Amigos y salas","Friends & Party"),color=Color.White,fontSize=28.sp, lineHeight=34.sp,modifier=Modifier.weight(1f))
                        FocusButton(if(refreshing) tr("Actualizando…","Refreshing…") else tr("Actualizar","Refresh")) { if(!refreshing) state.launch {
                            refreshing=true
                            try { state.socialAction { state.refreshFriendsDirectory() } } finally { refreshing=false }
                        } }
                        FocusButton(if(state.watchParty==null) tr("Crear sala","Create Party") else tr("Mi sala","My Party")) { state.screen=Screen.WATCH_PARTY }
                    }
                    Text(tr("Actualiza cuando quieras ver la actividad reciente de tus amigos","Refresh to see your friends' recent activity"),color=Muted,fontSize=13.sp, lineHeight=16.sp)
                }
                item {
                    Row(horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                        listOf(tr("Amigos","Friends"),tr("Salas","Parties"),tr("Solicitudes","Requests")).forEachIndexed { index,label -> FocusButton(label,primary=tab==index) { tab=index } }
                        FocusButton(tr("Mi nickname / frases","My nickname / Phrases")) { manage=true }
                    }
                }
                if(state.session==null) item { FocusButton(tr("Iniciar sesión","Sign in")) { state.screen=Screen.SETTINGS } }
                else {
                    if(tab==2) {
                        item { AccessRequests(state) }
                        items(state.friendRows.filter { it.status=="pending" }) { row ->
                            val me=state.session?.userId; val other=if(row.sender==me) row.receiver else row.sender
                            val name=state.friendPeople[other] ?: "Friend"
                            Row(Modifier.fillMaxWidth().background(Color(0x99172032),RoundedCornerShape(16.dp)).padding(18.dp),horizontalArrangement=Arrangement.spacedBy(18.dp)) {
                                Text("@$name",color=Color.White,modifier=Modifier.weight(1f))
                                if(row.receiver==me) FocusButton(tr("Aceptar amistad","Accept friend request")) { state.launch { state.addFriend(other) } }
                                else Text(tr("Solicitud enviada","Request sent"),color=Muted)
                            }
                        }
                    } else {
                        item { NativeTextField(search,{ search=it },tr("Buscar amigos","Search friends"),modifier=Modifier.width(320.dp)) }
                        val friends=state.friendActivity.filter { (tab==0 || it.room!=null) && it.nickname.contains(search,true) }
                        if(friends.isEmpty()) item { Text(tr("No hay resultados · pulsa Refresh para cargar tus amigos","No results · Select Refresh to load your friends"),color=Muted) }
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
                Text("@${friend.nickname}",color=Color.White,fontSize=25.sp, lineHeight=30.sp)
                Text(if(friend.title.isBlank()) if(friend.online) tr("En línea","Online") else tr("Desconectado","Offline") else "${tr("Viendo","Watching")} ${friend.title}",color=Muted)
                friend.room?.let { code ->
                    FocusButton(if(state.pendingPartyCode==code) tr("Esperando aprobación","Waiting for approval") else tr("Solicitar acceso","Request access"),primary=true) {
                        state.launch { state.socialAction { state.joinWatchParty(code); person=null } }
                    }
                }
                FocusButton(tr("Cerrar","Close")) { person=null }
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
        .border(if(focused) 2.dp else 1.dp,if(focused) Color.White else Color(0x335F5F5F),RoundedCornerShape(18.dp)).padding(16.dp),
        horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(8.dp)) {
        ProfileAvatar(Profile(friend.id,friend.nickname,friend.avatar),Modifier.size(84.dp))
        Text(friend.nickname,color=Color.White,fontSize=18.sp, lineHeight=22.sp,maxLines=1)
        Text(if(friend.title.isNotBlank()) "${tr("Viendo","Watching")} ${friend.title}" else if(friend.online) tr("En línea","Online") else tr("Desconectado","Offline"),color=if(friend.online) Color(0xFF8DE5B8) else Muted,fontSize=13.sp, lineHeight=16.sp,maxLines=2)
        if(friend.room!=null) Text(tr("Sala · Solicitar acceso","Party · Request access"),color=Color(0xFF9DD4FF),fontSize=12.sp, lineHeight=15.sp)
    }
}

@Composable
private fun SocialSettingsDialog(state: AppState,close: () -> Unit) {
    var nick by remember { mutableStateOf(state.nickname) }
    var find by remember { mutableStateOf("") }
    var phrases by remember { mutableStateOf(state.partyPhrases.joinToString("|")) }
    Dialog(onDismissRequest=close,properties=DialogProperties(usePlatformDefaultWidth=false)) {
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
    Dialog(onDismissRequest=close,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Column(Modifier.fillMaxWidth().background(Panel).padding(28.dp)) {
            Text(tr("Reaccionar en la sala","React to the party"),color=Color.White,fontSize=24.sp, lineHeight=29.sp)
            LazyRow(horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                items(PartyEmojis) { emoji -> FocusButton(emoji, modifier=if(emoji==PartyEmojis.first()) Modifier.focusRequester(first) else Modifier) { state.launch { state.sendPartyEvent("emoji",emoji) }; close() } }
            }
            Spacer(Modifier.height(12.dp))
            LazyColumn(Modifier.heightIn(max=240.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                items(state.partyPhrases) { phrase -> FocusButton(phrase,modifier=Modifier.fillMaxWidth()) { state.launch { state.sendPartyEvent("phrase",phrase) }; close() } }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                FocusButton(tr("QR de la sala / Chat móvil","Room / mobile chat QR")) { close(); state.screen=Screen.WATCH_PARTY }
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
    Column(Modifier.fillMaxSize().background(Bg)) {
        Sidebar(Screen.NOTIFICATIONS,state.activeProfile.name,{state.screen=it},state.notifications.size,state.activeProfile)
        LazyColumn(Modifier.weight(1f).padding(30.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            item {
                Text("${tr("Notificaciones","Notifications")} · ${state.notifications.size}",color=Color.White,fontSize=30.sp, lineHeight=36.sp)
                Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    FocusButton(tr("Revisar ahora","Check now")) { state.launch { runCatching { state.checkNotifications() }.onFailure { state.error=it.message } } }
                    FocusButton(tr("Limpiar notificaciones","Clear notifications")) { state.clearNotifications() }
                }
                Text(tr("Nuevos estrenos de tus favoritos. Se revisan al abrir MiFlix y cada 30 minutos mientras está abierta.","New releases from your favorites. Checked when you open BruniO and every 30 minutes while it's open."),color=Muted)
            }
            if(state.notifications.isEmpty()) item { Text(tr("No hay notificaciones nuevas","No new notifications"),color=Color.White) }
            items(state.notifications.toList(),key={it.id}) { notice ->
                Column {
                    Text(notice.item.title,color=Color.White,fontSize=22.sp, lineHeight=26.sp)
                    Text(notice.message,color=Muted)
                    FocusButton(tr("Ver contenido","View content")) { state.launch { state.open(notice.item) } }
                }
            }
        }
    }
}
