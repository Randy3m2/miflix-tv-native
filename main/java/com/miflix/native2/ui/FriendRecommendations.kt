package com.miflix.native2.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.miflix.native2.data.FriendActivity
import com.miflix.native2.data.FriendRecommendation
import com.miflix.native2.model.Profile
import com.miflix.native2.model.MediaSummary
import com.miflix.native2.model.PartyRole
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun RecommendTitleDialog(state: AppState, item: MediaSummary, close: () -> Unit) {
    val scope=rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var sent by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    val first=remember { FocusRequester() }
    LaunchedEffect(sent) { withFrameNanos { }; first.requestFocus() }
    Dialog(onDismissRequest={ if(!busy) close() }) {
        Column(Modifier.width(510.dp).background(Panel,RoundedCornerShape(22.dp)).padding(24.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
            Text(item.title,color=Color.White,fontSize=24.sp)
            Text(if(sent) tr("Recomendación guardada. Tus amigos ya pueden verla.","Recommendation saved. Your friends can now see it.")
                else tr("Se agregará a tus recomendaciones, visibles para todos tus amigos aceptados.","Add this title to your recommendations, visible to all your accepted friends."),color=Muted)
            if(message.isNotBlank()) Text(message,color=Color.White)
            if(!sent) FocusButton(if(busy) tr("Guardando…","Saving…") else tr("Recomendar a amigos","Recommend to friends"),modifier=Modifier.focusRequester(first)) {
                if(!busy) scope.launch {
                    busy=true
                    try { state.recommendToFriends(item); sent=true }
                    catch(e: Exception) { if(e is CancellationException) throw e; message=e.message.orEmpty() }
                    finally { busy=false }
                }
            }
            FocusButton(tr("Cerrar","Close"),modifier=if(sent) Modifier.focusRequester(first) else Modifier) { if(!busy) close() }
        }
    }
}

@Composable
fun FriendRecommendationsDialog(state: AppState, owner: String?=null, ownerName: String?=null, close: () -> Unit) {
    val scope=rememberCoroutineScope()
    val account=state.session?.userId
    var rows by remember(account) { mutableStateOf<List<FriendRecommendation>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var mine by remember { mutableStateOf(false) }
    val first=remember { FocusRequester() }
    suspend fun refresh() {
        if(busy) return
        busy=true; message=""
        try { rows=state.friendRecommendations() }
        catch(e: Exception) { if(e is CancellationException) throw e; message=e.message.orEmpty() }
        finally { busy=false }
    }
    LaunchedEffect(account) { refresh() }
    LaunchedEffect(Unit) { withFrameNanos { };first.requestFocus() }
    val visible=rows.filter { if(owner!=null) it.owner==owner else !mine || it.owner==account }
    Dialog(onDismissRequest=close,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Column(Modifier.fillMaxWidth(.9f).widthIn(max=850.dp).heightIn(max=540.dp).background(Panel,RoundedCornerShape(22.dp)).padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            Text(if(ownerName!=null) tr("Recomendaciones de ","Recommendations by ")+ownerName else tr("Recomendaciones de amigos","Friends' recommendations"),color=Color.White,fontSize=25.sp)
            Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                FocusButton(if(busy) tr("Cargando…","Loading…") else tr("Actualizar","Refresh"),modifier=Modifier.focusRequester(first)) { scope.launch { refresh() } }
                if(owner==null) FocusButton(if(mine) tr("Ver todas","Show all") else tr("Mis recomendaciones","My recommendations")) { mine=!mine }
                FocusButton(tr("Cerrar","Close")) { close() }
            }
            if(message.isNotBlank()) Text(message,color=Color.White,maxLines=3)
            LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(6.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
                if(!busy && visible.isEmpty()) item { Text(tr("Aún no hay recomendaciones. Puedes agregar una desde la ficha de una película o serie.","No recommendations yet. Add one from a movie or series details page."),color=Muted) }
                items(visible,key={it.owner+":"+it.cloud}) { row ->
                    Row(Modifier.fillMaxWidth().background(Color(0xFF303030),RoundedCornerShape(16.dp)).padding(16.dp),horizontalArrangement=Arrangement.spacedBy(18.dp)) {
                        AsyncImage(row.poster,null,Modifier.size(76.dp,114.dp),contentScale=ContentScale.Fit)
                        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                            Text(row.title,color=Color.White,fontSize=20.sp,maxLines=2)
                            Text(if(row.owner==account) tr("Recomendado por ti","Recommended by you") else tr("Recomendado por @","Recommended by @")+row.nickname,color=Muted)
                            Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                                FocusButton(tr("Ver contenido","View title")) { if(!busy) {
                                    busy=true
                                    state.launch {
                                        try { state.openRecommendation(row.cloud);close() }
                                        catch(e: Exception) { if(e is CancellationException) throw e;message=e.message.orEmpty() }
                                        finally { busy=false }
                                    }
                                } }
                                if(row.owner==account) FocusButton(tr("Quitar","Remove")) { if(!busy) scope.launch {
                                    busy=true
                                    try { state.removeFriendRecommendation(row.cloud);rows=rows.filterNot { it.owner==row.owner && it.cloud==row.cloud } }
                                    catch(e: Exception) { if(e is CancellationException) throw e;message=e.message.orEmpty() }
                                    finally { busy=false }
                                } }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FriendActionsDialog(state: AppState, friend: FriendActivity, close: () -> Unit, onContentOpened: () -> Unit = close) {
    var recommendations by remember { mutableStateOf(false) }
    var confirmLeave by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    val first=remember { FocusRequester() }
    LaunchedEffect(confirmLeave,recommendations) { if(!recommendations) { withFrameNanos { };first.requestFocus() } }
    val canWatch=friend.online && friend.cloudId.matches(Regex("^tmdb:(movie|series):[0-9]+$"))
    fun watch() {
        if(busy) return
        busy=true
        onContentOpened()
        state.launch {try {state.socialAction {state.watchFriendIndependently(friend)}} finally {busy=false} }
    }
    Dialog(onDismissRequest={ if(!busy) close() }) {
        LazyColumn(Modifier.width(520.dp).heightIn(max=510.dp).background(Panel,RoundedCornerShape(22.dp)).padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            item { ProfileAvatar(Profile(friend.id,friend.nickname,friend.avatar),Modifier.size(84.dp)) }
            item { Text("@${friend.nickname}",color=Color.White,fontSize=25.sp) }
            item { Text(friend.title.ifBlank { if(friend.online) tr("En línea","Online") else tr("Desconectado","Offline") }+
                (if(friend.season>0 && friend.title.isNotBlank()) " · S${friend.season} E${friend.episode}" else ""),color=Muted) }
            if(confirmLeave) {
                item { Text(if(state.partyRole==PartyRole.HOST) tr("Para ver por tu cuenta se cerrará tu party actual. ¿Continuar?","Watching independently will close your current party. Continue?")
                    else tr("Saldrás de tu party actual para ver por tu cuenta. ¿Continuar?","You will leave your current party to watch independently. Continue?"),color=Color.White) }
                item { FocusButton(tr("Cancelar","Cancel"),modifier=Modifier.focusRequester(first)) { if(!busy) confirmLeave=false } }
                item { FocusButton(if(busy) tr("Abriendo…","Opening…") else tr("Continuar","Continue")) { watch() } }
            } else {
                item { FocusButton(if(busy) tr("Abriendo…","Opening…") else tr("Ver","Watch"),modifier=Modifier.focusRequester(first)) {
                    if(!busy) {
                        if(!canWatch) state.error=tr("Este amigo no está viendo un título disponible ahora.","This friend is not watching an available title right now.")
                        else if(state.watchParty!=null || state.pendingPartyCode!=null) confirmLeave=true else watch()
                    }
                } }
                item { FocusButton(tr("Ver detalles","View Details")) {
                    if(!busy) {
                        if(!friend.cloudId.matches(Regex("^tmdb:(movie|series):[0-9]+$"))) state.error=tr("No hay detalles disponibles para esta actividad.","No title details are available for this activity.")
                        else {
                            busy=true
                            state.launch {try {state.socialAction {
                                val title=state.tmdb.byCloudId(friend.cloudId) ?: error(tr("Título no disponible","Title unavailable"))
                                state.open(title)
                                if(state.selected?.cloudId==title.cloudId && state.screen==Screen.DETAILS) onContentOpened()
                            }} finally {busy=false} }
                        }
                    }
                } }
                item { FocusButton(tr("Recomendaciones","Recommendations")) {if(!busy) recommendations=true} }
                item { FocusButton(tr("Cerrar","Close")) { if(!busy) close() } }
            }
            state.error?.let { error -> item { Text(error,color=Color.White,maxLines=3) } }
        }
    }
    if(recommendations) FriendRecommendationsDialog(state,friend.id,friend.nickname) { recommendations=false }
}
