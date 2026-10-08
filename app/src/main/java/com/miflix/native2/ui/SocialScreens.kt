package com.miflix.native2.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
fun FriendsScreen(state: AppState) {
    var nick by remember { mutableStateOf(state.nickname) }
    var find by remember { mutableStateOf("") }
    var phrases by remember { mutableStateOf(state.partyPhrases.joinToString("|")) }
    LaunchedEffect(state.session?.userId) { state.refreshFriends(); nick = state.nickname }
    BackHandler { state.screen = Screen.HOME }
    Row(Modifier.fillMaxSize().background(Bg)) {
        Sidebar(Screen.FRIENDS,state.activeProfile.name,{state.screen=it},state.notifications.size)
        LazyColumn(Modifier.weight(1f).padding(28.dp), verticalArrangement=Arrangement.spacedBy(14.dp),contentPadding=PaddingValues(bottom=50.dp)) {
            item { Text("Friends & Nickname",color=Color.White,fontSize=28.sp) }
            if(state.session==null) item { FocusButton("Sign in") { state.screen=Screen.SETTINGS } }
            else {
                item {
                    Text("Your unique nickname · 3–24 lowercase letters, numbers or _",color=Muted)
                    NativeTextField(nick,{nick=it},"Nickname")
                    FocusButton("Save nickname") { state.launch { state.saveNickname(nick) } }
                }
                item {
                    NativeTextField(find,{find=it},"Friend's nickname")
                    FocusButton("Add friend") { state.launch { state.findAndAddFriend(find) } }
                }
                items(state.friendRows) { row ->
                    val uid=state.session?.userId
                    val other=if(row.sender==uid)row.receiver else row.sender
                    val name=state.friendPeople[other]?:"Friend"
                    if(row.status=="accepted") Text("✓ @$name",color=Color.White,fontSize=18.sp)
                    else if(row.receiver==uid) FocusButton("Accept @$name") { state.launch { state.addFriend(other) } }
                    else Text("Request sent to @$name",color=Muted)
                }
                item { FocusButton("Refresh") { state.launch { state.refreshFriends() } } }
            }
            item {
                Text("Quick phrases · separate with | · up to 8 phrases",color=Color.White)
                NativeTextField(phrases,{phrases=it},"Phrase one|Phrase two")
                FocusButton("Save phrases") { state.savePhrases(phrases) }
            }
            item { FocusButton("Trakt") { state.screen=Screen.TRAKT } }
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
