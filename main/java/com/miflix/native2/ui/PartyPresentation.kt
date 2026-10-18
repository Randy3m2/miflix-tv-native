package com.miflix.native2.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.tv.material3.Text
import com.miflix.native2.model.PartyRole
import com.miflix.native2.model.Profile
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

internal fun partyJoinUrl(code: String) = "https://randy3m2.github.io/miflix-tv-native/party/?room=$code"

/** The window itself never dims the video; only the right panel draws a scrim. */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
internal fun PlayerSideSheet(title: String,close: () -> Unit,resetKey:Any?=null,content: LazyListScope.() -> Unit) {
    val first=remember {FocusRequester()}
    val sheetScroll=rememberLazyListState()
    val sheetScope=rememberCoroutineScope()
    val bring=remember {object:BringIntoViewSpec {
        override fun calculateScrollDistance(offset:Float,size:Float,containerSize:Float):Float = when {
            size>=containerSize ->offset
            offset<8f ->offset-8f
            offset+size>containerSize-8f ->offset+size-containerSize+8f
            else ->0f
        }
    }}
    Dialog(onDismissRequest=close,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        val view=LocalView.current
        DisposableEffect(view) {
            (view.parent as? DialogWindowProvider)?.window?.setDimAmount(0f)
            onDispose { }
        }
        LaunchedEffect(resetKey) {sheetScroll.scrollToItem(0);withFrameNanos { };runCatching {first.requestFocus()} }
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val panelWidth=(maxWidth*.52f).coerceIn(360.dp,560.dp).coerceAtMost(maxWidth)
            Column(Modifier.align(Alignment.CenterEnd).padding(14.dp).width(panelWidth).fillMaxHeight()
                .background(Color(0xF01A1A23),RoundedCornerShape(24.dp)).padding(22.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    Text(title,color=Color.White,fontSize=26.sp,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f))
                    FocusButton(tr("Cerrar","Close"),modifier=Modifier.focusRequester(first).onFocusChanged {if(it.isFocused) sheetScope.launch {sheetScroll.scrollToItem(0)}},onClick=close)
                }
                CompositionLocalProvider(LocalBringIntoViewSpec provides bring) {
                    LazyColumn(Modifier.weight(1f).fillMaxWidth(),state=sheetScroll,contentPadding=PaddingValues(top=8.dp,bottom=20.dp),verticalArrangement=Arrangement.spacedBy(12.dp),content=content)
                }
            }
        }
    }
}

@Composable
internal fun PartyCode(code: String) {
    Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text(tr("Código del party","Party code"),color=Muted,fontSize=13.sp)
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            code.take(6).forEach {digit ->
                Column(Modifier.weight(1f),horizontalAlignment=Alignment.CenterHorizontally) {
                    Text(digit.toString(),color=Color.White,fontSize=30.sp,fontWeight=FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Box(Modifier.fillMaxWidth().height(2.dp).background(Color(0xFF85858E)))
                }
            }
        }
    }
}

@Composable
private fun PartyToggle(title: String,subtitle: String,checked: Boolean,enabled: Boolean,onToggle: () -> Unit) {
    var focused by remember {mutableStateOf(false)}
    val activate: () -> Unit={if(enabled) onToggle()}
    Row(Modifier.fillMaxWidth().onFocusChanged {focused=it.isFocused}.focusable()
        .tvClick(activate).clickable(onClick=activate)
        .background(Color(0xFF0A0A10),RoundedCornerShape(28.dp))
        .border(2.dp,if(focused) Accent else Color.Transparent,RoundedCornerShape(28.dp)).padding(18.dp),
        verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)) {
        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
            Text(title,color=Color.White,fontSize=17.sp,fontWeight=FontWeight.SemiBold)
            Text(subtitle,color=Muted,fontSize=12.sp,lineHeight=16.sp)
        }
        Box(Modifier.size(42.dp,24.dp).background(if(checked) Accent else Color(0xFF555562),CircleShape).padding(3.dp)) {
            Box(Modifier.align(if(checked) Alignment.CenterEnd else Alignment.CenterStart).size(18.dp).background(Color.White,CircleShape))
        }
    }
}

@Composable
internal fun PartyMembersOverlay(state: AppState,modifier: Modifier=Modifier,playing: Boolean?=null,onOpen: () -> Unit) {
    val members=state.partyMembers.sortedWith(compareBy<com.miflix.native2.data.SocialPerson> {it.id!=state.watchParty?.hostUserId}.thenBy {it.nickname}).ifEmpty {
        listOf(com.miflix.native2.data.SocialPerson(state.session?.userId.orEmpty(),state.nickname.ifBlank {state.activeProfile.name},state.activeProfile.avatarValue))
    }
    Row(modifier.semantics {contentDescription=tr("Participantes del party","Party members")}
        .contentFocusBorder(24.dp).focusable().tvClick(onOpen).clickable(onClick=onOpen).padding(5.dp),
        horizontalArrangement=Arrangement.spacedBy((-6).dp),verticalAlignment=Alignment.CenterVertically) {
        members.take(5).forEach {member ->
            val avatar=if(member.id==state.session?.userId) state.activeProfile.avatarValue else member.avatar
            ProfileAvatar(Profile(member.id,member.nickname,avatar),Modifier.size(32.dp).clip(CircleShape).border(1.dp,Color.White.copy(alpha=.7f),CircleShape))
        }
        if(members.size>5) Text("+${members.size-5}",color=Color.White,fontSize=12.sp,modifier=Modifier.padding(start=10.dp))
        state.partyPlaybackNotice?.takeIf {playing==null || it.resumed==playing}?.let {notice ->
            Text(notice.actor+(if(notice.resumed) tr(" reanudó"," resumed") else tr(" pausó"," paused"))+
                (if(notice.localOnly) tr(" (local)"," (locally)") else ""),
                color=Color.White,fontSize=14.sp,maxLines=1,overflow=TextOverflow.Ellipsis,
                modifier=Modifier.padding(start=14.dp).background(Color(0xD916161F),RoundedCornerShape(18.dp)).padding(horizontal=14.dp,vertical=8.dp))
        }
    }
}

@Composable
internal fun PartyRoomSheet(state: AppState,onCreate: () -> Unit,onRefreshRequests: () -> Unit,onSwitchCurrent: () -> Unit,onWatchParty: () -> Unit,close: () -> Unit) {
    var selectedFriend by remember {mutableStateOf<com.miflix.native2.data.FriendActivity?>(null)}
    var invite by remember {mutableStateOf(false)}
    var qr by remember {mutableStateOf(false)}
    var confirmShare by remember {mutableStateOf(false)}
    var chat by remember {mutableStateOf(false)}
    var reactions by remember {mutableStateOf(false)}
    val sent=remember(state.watchParty?.roomCode) {mutableStateListOf<String>()}
    LaunchedEffect(state.session?.userId) {state.refreshFriendsTab()}
    val room=state.watchParty
    val host=state.partyRole==PartyRole.HOST
    PlayerSideSheet(tr("Amigos y Party","Friends & Party"),close) {
        if(room==null) {
            item {Text(tr("Crea un party con lo que estás viendo.","Start a party with what you are watching."),color=Muted)}
            item {FocusButton(tr("Crear party","Create party"),primary=true,modifier=Modifier.fillMaxWidth(),onClick=onCreate)}
        } else {
            item {PartyCode(room.roomCode)}
            item {Text(listOf(room.title,if(room.season>0) "S${room.season} E${room.episode}" else "").filter {it.isNotBlank()}.joinToString(" · "),color=Color.White,fontSize=15.sp,maxLines=2)}
            val current=state.playerRequest
            if(current!=null && state.screen!=Screen.PLAYER) item {
                FocusButton(tr("Volver al reproductor","Return to player"),primary=true,modifier=Modifier.fillMaxWidth()) {
                    state.returnToPlayer();close()
                }
            }
            if(current!=null && host && state.screen==Screen.PLAYER && (current.item.cloudId!=room.cloudId || current.season!=room.season || current.episode!=room.episode)) item {
                val label=current.item.title+if(current.season>0) " · S${current.season} E${current.episode}" else ""
                FocusButton(tr("Cambiar el party a ","Switch party to ")+label,primary=true,modifier=Modifier.fillMaxWidth(),onClick=onSwitchCurrent)
            }
            if(current!=null && (!host || current.item.cloudId!=room.cloudId || current.season!=room.season || current.episode!=room.episode)) item {FocusButton(tr("Ver con el party","Watch with the party"),primary=true,modifier=Modifier.fillMaxWidth(),onClick=onWatchParty)}
            item {Text("${state.partyMembers.size.coerceAtLeast(1)} "+tr("participantes","members"),color=Muted,fontSize=12.sp)}
            items(state.partyMembers.sortedWith(compareBy<com.miflix.native2.data.SocialPerson> {it.id!=room.hostUserId}.thenBy {it.nickname}),key={it.id}) {member ->
                Row(Modifier.fillMaxWidth().background(Color(0xFF0A0A10),RoundedCornerShape(28.dp)).padding(12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    ProfileAvatar(Profile(member.id,member.nickname,if(member.id==state.session?.userId) state.activeProfile.avatarValue else member.avatar),Modifier.size(36.dp))
                    Column {
                        Text(member.nickname+if(member.id==state.session?.userId) tr(" (Tú)"," (You)") else "",color=Color.White,fontSize=16.sp)
                        Text(if(member.id==room.hostUserId) tr("Anfitrión","Host") else tr("Miembro","Member"),color=Muted,fontSize=12.sp)
                    }
                }
            }
            item {FocusButton(tr("QR y enlace de invitación","QR & invite link"),modifier=Modifier.fillMaxWidth()) {qr=true}}
            if(host) item {FocusButton(tr("+ Invitar un amigo","+ Invite a friend"),modifier=Modifier.fillMaxWidth()) {invite=true}}
            if(host) {
                item {PartyToggle(tr("Solo yo controlo el party","Only I control party playback"),tr("Los invitados pueden pausar localmente y volver a sincronizarse.","Guests can pause locally and rejoin the host timeline."),room.hostOnlyControls,!state.partyOptionsBusy) {
                    state.launch {state.socialAction {state.configureParty(hostOnly=!room.hostOnlyControls)}}
                }}
                item {PartyToggle(tr("Compartir mi enlace","Share my stream"),tr("Tus invitados pueden usar tu fuente en vez de elegir otra.","Guests can use your source instead of choosing their own."),room.shareStream,!state.partyOptionsBusy) {
                    if(room.shareStream) state.launch {state.socialAction {state.configureParty(share=false)}} else confirmShare=true
                }}
            }
            if(room.sourceName.isNotBlank()) item {
                Text(tr("Fuente del host","Host source"),color=Muted,fontSize=12.sp)
                Text(room.sourceName,color=Color.White,fontSize=15.sp,maxLines=2)
                if(room.sourceSize>0) Text(String.format(java.util.Locale.US,"%.2f GiB",room.sourceSize/1073741824.0),color=Muted,fontSize=12.sp)
            }
            if(!host && room.shareStream) item {FocusButton(tr("Usar fuente del party","Use party source"),primary=true,modifier=Modifier.fillMaxWidth()) {state.launch {state.socialAction {state.usePartySource();close()}}}}
            item {Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                FocusButton(tr("Chat","Chat")) {chat=true};FocusButton(tr("Reacciones","Reactions")) {reactions=true}
            }}
            item {FocusButton(if(host) tr("Terminar para todos","End for everyone") else tr("Salir del party","Leave party"),destructive=true,modifier=Modifier.fillMaxWidth()) {state.launch {state.leaveWatchParty();close()}}}
        }
        item {FocusButton(if(state.accessRequestsLoading) tr("Consultando…","Checking…") else tr("Actualizar solicitudes","Refresh requests"),modifier=Modifier.fillMaxWidth()) {if(!state.accessRequestsLoading) onRefreshRequests()}}
        item {Text(tr("Amigos","Friends"),color=Color.White,fontSize=19.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=6.dp))}
        val friends=state.friendActivity.distinctBy {it.id}.sortedWith(compareByDescending<com.miflix.native2.data.FriendActivity> {it.online}.thenBy {it.nickname.lowercase()})
        if(state.session==null) item {Text(tr("Inicia sesión para ver tus amigos.","Sign in to see your friends."),color=Muted,fontSize=13.sp)}
        else if(friends.isEmpty()) item {Text(if(state.friendsTabLoading) tr("Cargando amigos…","Loading friends…") else tr("Aún no tienes amigos agregados.","No friends added yet."),color=Muted,fontSize=13.sp)}
        items(friends,key={"player-friend:${it.id}"}) {friend ->
            Row(Modifier.fillMaxWidth().contentFocusBorder(16.dp).focusable().tvClick {selectedFriend=friend}.clickable {selectedFriend=friend}
                .background(Color(0xFF0A0A10),RoundedCornerShape(16.dp)).padding(12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                ProfileAvatar(Profile(friend.id,friend.nickname,friend.avatar),Modifier.size(38.dp))
                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(3.dp)) {
                    Text(friend.nickname,color=Color.White,fontSize=15.sp,fontWeight=FontWeight.Bold,maxLines=1,overflow=TextOverflow.Ellipsis)
                    Text(if(friend.online) tr("En línea","Online") else tr("Desconectado","Offline"),color=if(friend.online) Color(0xFF80D99A) else Muted,fontSize=11.sp)
                    if(friend.title.isNotBlank()) Text(friend.title+(if(friend.season>0) " · S${friend.season} E${friend.episode}" else ""),color=Muted,fontSize=12.sp,maxLines=2,overflow=TextOverflow.Ellipsis)
                }
            }
        }
        if(state.partyStatus.isNotBlank()) item {Text(state.partyStatus,color=Muted,fontSize=12.sp)}
        state.error?.let {message -> item {Text(message,color=Color(0xFFFFC6C6),fontSize=12.sp)}}
    }
    selectedFriend?.let {friend -> FriendActionsDialog(state,friend,close={selectedFriend=null},onContentOpened={selectedFriend=null;close()})}
    if(qr && room!=null) PlayerSideSheet(tr("Invitar al party","Invite to party"),{qr=false}) {
        item {PartyCode(room.roomCode)}
        item {Box(Modifier.fillMaxWidth(),contentAlignment=Alignment.Center) {QrCode(partyJoinUrl(room.roomCode),190.dp)}}
        item {Text(tr("Escanea para ver el contenido y solicitar acceso. El host aprueba las nuevas solicitudes.","Scan to see the title and request access. The host approves new requests."),color=Muted,fontSize=14.sp)}
        item {Text(partyJoinUrl(room.roomCode),color=Color.White,fontSize=12.sp)}
    }
    if(invite && room!=null) PlayerSideSheet(tr("Invitar un amigo","Invite a friend"),{invite=false}) {
        val friends=state.friendActivity.distinctBy {it.id}
        if(friends.isEmpty()) item {Text(tr("No hay amigos disponibles.","No friends available."),color=Muted)}
        items(friends,key={it.id}) {friend ->
            FocusButton(friend.nickname+if(friend.id in sent) " ✓" else "",modifier=Modifier.fillMaxWidth()) {
                if(friend.id !in sent) state.launch {state.socialAction {state.inviteToParty(friend.id);sent.add(friend.id)}}
            }
        }
        item {Text(tr("La invitación aparece cuando tu amigo actualiza Friends. Invitarlo autoriza su entrada.","Your friend sees the invitation when they refresh Friends. Inviting them approves their entry."),color=Muted,fontSize=12.sp)}
    }
    if(confirmShare) PlayerSideSheet(tr("Compartir mi enlace","Share my stream"),{confirmShare=false}) {
        item {Text(tr("El enlace puede incluir acceso de tu proveedor/debrid. Solo los miembros admitidos lo recibirán. Podrían copiarlo mientras sea válido.","The link may include access to your provider/debrid account. Only admitted members can receive it. They could copy it while it remains valid."),color=Color.White,fontSize=17.sp)}
        item {FocusButton(tr("Compartir con este party","Share with this party"),primary=true,modifier=Modifier.fillMaxWidth()) {confirmShare=false;state.launch {state.socialAction {state.configureParty(share=true)}}}}
    }
    if(chat) PlayerPartyChatDialog(state) {chat=false}
    if(reactions) PartyActionsDialog(state) {reactions=false}
}

@Composable
internal fun PartyLobby(state: AppState,onBack: () -> Unit) {
    var code by remember {mutableStateOf("")}
    var preview by remember {mutableStateOf<org.json.JSONObject?>(null)}
    var sheet by remember {mutableStateOf(state.watchParty!=null)}
    var pending by remember {mutableStateOf(false)}
    androidx.activity.compose.BackHandler(onBack=onBack)
    LaunchedEffect(code) {
        preview=null
        if(code.length==6) {
            delay(450)
            try {preview=state.previewParty(code)} catch(e: CancellationException) {throw e} catch(_: Exception) { }
        }
    }
    AppShell(state,Screen.WATCH_PARTY,{state.screen=it}) {
        Row(Modifier.fillMaxSize().padding(30.dp),horizontalArrangement=Arrangement.spacedBy(26.dp)) {
            Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(16.dp)) {
                Text("Watch party",color=Color.White,fontSize=32.sp,fontWeight=FontWeight.Black)
                Text(tr("Un código. Tus amigos. La misma historia.","One code. Your friends. The same story."),color=Muted,fontSize=15.sp)
                FocusButton(if(state.watchParty==null) tr("Crear party","Create party") else tr("Abrir mi party","Open my party"),primary=true,modifier=Modifier.fillMaxWidth()) {
                    if(state.watchParty!=null) sheet=true
                    else if(!pending) state.launch {pending=true;try {state.socialAction {if(state.createWatchParty(state.playerRequest!=null)!=null) sheet=true}} finally {pending=false}}
                }
                NativeTextField(code,{code=it.filter(Char::isDigit).take(6)},tr("Código de 6 dígitos","6-digit code"),modifier=Modifier.fillMaxWidth())
                if(code.length==6) PartyCode(code)
                FocusButton(tr("Unirme al party","Join party"),modifier=Modifier.fillMaxWidth()) {
                    if(!pending) state.launch {pending=true;try {state.socialAction {state.joinWatchParty(code)}} finally {pending=false}}
                }
                if(state.pendingPartyCode!=null) Text(tr("Solicitud enviada. Esperando al host.","Request sent. Waiting for the host."),color=Accent)
                FocusButton(if(state.accessRequestsLoading) tr("Consultando…","Checking…") else tr("Actualizar solicitudes","Refresh requests")) {if(!state.accessRequestsLoading) state.launch {state.socialAction {state.refreshPartyRequests()}}}
                AccessRequests(state)
                Text(state.partyStatus,color=Muted,fontSize=13.sp)
                state.error?.let {Text(it,color=Color(0xFFFFC6C6),fontSize=13.sp)}
                FocusButton(tr("Volver","Back"),onClick=onBack)
            }
            Column(Modifier.weight(1f).fillMaxHeight().background(Panel,RoundedCornerShape(24.dp)).padding(24.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
                val data=preview
                val poster=data?.optString("poster")?.takeIf {it.startsWith("https://image.tmdb.org/")}
                if(poster!=null) coil3.compose.AsyncImage(poster,null,Modifier.fillMaxWidth().weight(1f),contentScale=androidx.compose.ui.layout.ContentScale.Fit)
                Text(data?.optString("title")?.takeIf {it.isNotBlank()} ?: tr("Tu próxima sesión juntos","Your next night together"),color=Color.White,fontSize=24.sp,fontWeight=FontWeight.Bold,maxLines=2)
                data?.let {
                    Text(if(it.optInt("season")>0) "S${it.optInt("season")} E${it.optInt("episode")}" else "",color=Accent,fontSize=14.sp)
                    Text(it.optString("overview"),color=Muted,fontSize=14.sp,maxLines=3,overflow=TextOverflow.Ellipsis)
                    Text(tr("Organizado por ","Hosted by ")+it.optString("host"),color=Color.White,fontSize=14.sp)
                }
            }
        }
    }
    if(sheet && state.watchParty!=null) PartyRoomSheet(state,onCreate={},onRefreshRequests={
        state.launch {state.socialAction {state.refreshPartyRequests()}}
    },onSwitchCurrent={state.returnToPlayer();sheet=false},
        onWatchParty={state.launch {state.socialAction {state.refreshParty()?.let {state.switchToPartyState(it,openPlayer=true)};sheet=false}}},close={sheet=false})
}
