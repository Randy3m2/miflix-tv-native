package com.miflix.native2.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.miflix.native2.data.*
import com.miflix.native2.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.time.*
import java.time.format.DateTimeFormatter

@Composable
internal fun MobileFriendsRP133(state:AppState) {
    var tab by rememberSaveable {mutableStateOf("Friends")}
    var code by rememberSaveable {mutableStateOf("")}
    var nickname by rememberSaveable {mutableStateOf("")}
    var chat by rememberSaveable {mutableStateOf("")}
    var friend by remember {mutableStateOf<FriendActivity?>(null)}
    val art=remember(state.localExtrasScope()) {mutableStateMapOf<String,MediaSummary>()}
    val permits=remember {Semaphore(2)}
    LaunchedEffect(state.localExtrasScope()) {state.refreshFriendsTab()}
    LaunchedEffect(state.friendActivity.toList()) {
        state.friendActivity.filter {it.cloudId.isNotBlank() && it.cloudId !in art}.take(20).forEach {person->launch {
            permits.withPermit {runCatching {state.tmdb.byCloudId(person.cloudId)}.getOrNull()?.let {art[person.cloudId]=it}}
        }}
    }
    friend?.let {f->MobileSheetRP133(f.nickname,{friend=null}) {
        ProfileAvatar(Profile(f.id,f.nickname,f.avatar),Modifier.size(70.dp))
        Text(f.title,color=Color.White,fontSize=21.sp,fontWeight=FontWeight.Bold)
        MobileButtonRP133(tr("Ver ahora","Watch"),"play",Modifier.fillMaxWidth()) {friend=null;state.launch {state.watchFriendIndependently(f)}}
        MobileButtonRP133(tr("Ver detalles","View details"),"more",Modifier.fillMaxWidth()) {friend=null;state.launch {state.openRecommendation(f.cloudId)}}
        if(f.room!=null) MobileButtonRP133(tr("Solicitar acceso al Party","Request Party access"),"friends",Modifier.fillMaxWidth()) {friend=null;state.launch {state.joinWatchParty(f.room)}}
    }}
    Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Row(Modifier.padding(horizontal=16.dp),verticalAlignment=Alignment.CenterVertically) {
            Text("Friends & Party",color=Color.White,fontSize=24.sp,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f))
            MobileIconButtonRP133("refresh",tr("Actualizar","Refresh")) {state.refreshFriendsTab()}
        }
        LazyRow(contentPadding=PaddingValues(horizontal=16.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            items(listOf("Friends" to tr("Amigos","Friends"),"Requests" to tr("Solicitudes","Requests"),"Calendar" to tr("Calendario","Calendar"))) {(id,label)->MobileButtonRP133(label,selected=tab==id) {tab=id}}
        }
        if(tab=="Calendar") {MobileCalendarRP133(state);return@Column}
        LazyColumn(contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
            if(state.session==null) item {MobileButtonRP133(tr("Iniciar sesión","Sign in"),"friends",Modifier.fillMaxWidth()) {state.screen=Screen.SETTINGS}}
            if(tab=="Requests") {
                item {MobileFieldRP133(nickname,{nickname=it},tr("Apodo de tu amigo","Friend nickname"))}
                item {MobileButtonRP133(tr("Agregar amigo","Add friend"),"plus",Modifier.fillMaxWidth()) {state.launch {state.findAndAddFriend(nickname)}}}
                items(state.friendRows.filter {it.status!="accepted" && it.receiver==state.socialIdentityId},key={it.sender+it.receiver}) {row->
                    MobileButtonRP133(state.friendPeople[row.sender] ?: tr("Solicitud de amistad","Friend request"),"check",Modifier.fillMaxWidth()) {state.launch {state.addFriend(row.sender)}}
                }
                items(state.partyInvitations,key={it.room}) {invite->MobileButtonRP133(invite.host+" · "+invite.title,"friends",Modifier.fillMaxWidth()) {state.launch {state.joinWatchParty(invite.room)}}}
            } else {
                items(state.friendActivity,key={it.id}) {person->
                    Box(Modifier.fillMaxWidth().height(205.dp).mobileGlassRP133().clickable {friend=person}) {
                        art[person.cloudId]?.let {AsyncImage(it.backdrop ?: it.poster,null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)}
                        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent,Color(0xF0080A0F)))))
                        Row(Modifier.align(Alignment.BottomStart).padding(14.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                            ProfileAvatar(Profile(person.id,person.nickname,person.avatar),Modifier.size(44.dp))
                            Column(Modifier.weight(1f)) {Text(person.nickname,color=Color.White,fontSize=17.sp,fontWeight=FontWeight.Bold);Text(if(person.online) person.title.ifBlank {tr("En línea","Online")} else tr("Sin conexión","Offline"),color=Muted,fontSize=13.sp,maxLines=2)}
                            MobileIconRP133("chevron")
                        }
                    }
                }
                if(state.friendActivity.isEmpty() && !state.friendsTabLoading) item {Text(tr("Tus amigos aparecerán aquí al conectarse.","Your friends will appear here when available."),color=Muted)}
            }
            item {MobileButtonRP133(tr("Crear Watch Party","Create Watch Party"),"friends",Modifier.fillMaxWidth(),selected=true) {state.launch {state.createWatchParty()}}}
            item {Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                MobileFieldRP133(code,{code=it.filter(Char::isDigit).take(6)},tr("Código Party","Party code"),modifier=Modifier.weight(1f),keyboard=androidx.compose.ui.text.input.KeyboardType.Number)
                MobileButtonRP133(tr("Unirme","Join"),enabled=code.length==6) {state.launch {state.joinWatchParty(code)}}
            }}
            state.watchParty?.let {party->
                item {Column(Modifier.fillMaxWidth().mobileGlassRP133(true).padding(14.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                    Text(tr("Tu sala · ","Your room · ")+party.roomCode,color=Color.White,fontSize=20.sp,fontWeight=FontWeight.Bold)
                    Text(party.title,color=Muted)
                    Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {state.partyMembers.take(6).forEach {person->ProfileAvatar(Profile(person.id,person.nickname,person.avatar),Modifier.size(35.dp))}}
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        MobileButtonRP133(tr("Reproducir","Play"),"play",Modifier.weight(1f)) {state.launch {state.switchToPartyState(party,true)}}
                        MobileButtonRP133(tr("Salir","Leave"),"close") {state.launch {state.leaveWatchParty()}}
                    }
                }}
                items(state.partyMessages.takeLast(25),key={it.id}) {message->Text((state.partyMembers.firstOrNull {it.id==message.userId}?.nickname ?: "")+": "+message.body,color=Color.White,fontSize=13.sp)}
                item {Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    MobileFieldRP133(chat,{chat=it},tr("Mensaje","Message"),modifier=Modifier.weight(1f))
                    MobileButtonRP133(tr("Enviar","Send"),enabled=chat.isNotBlank()) {val text=chat;chat="";state.launch {state.sendPartyEvent("chat",text)}}
                }}
            }
        }
    }
}

@Composable
internal fun MobileSubscriptionsRP133(state:AppState,initialLeague:String,close:()->Unit) {
    val context=LocalContext.current.applicationContext
    val owner=CalendarStoreRP120.owner(context,state.activeProfile.id)
    var league by rememberSaveable {mutableStateOf(initialLeague)}
    var teams by remember {mutableStateOf<List<CalendarTeamRP120>>(emptyList())}
    val revision by CalendarStoreRP120.revision.collectAsState()
    val subs=remember(revision,owner) {CalendarStoreRP120.subscriptions(context,owner)}
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {teams=withContext(Dispatchers.IO) {CalendarSourcesRP120.teams(context)}}
    MobileSheetRP133(tr("Tus equipos y ligas","Your teams and leagues"),close) {
        LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {items(listOf("NBA","MLB","F1","LIDOM")) {id->MobileButtonRP133(id,selected=league==id) {league=id}}}
        MobileButtonRP133((if("$league:*" in subs) "✓ " else "+ ")+tr("Seguir liga completa","Follow entire league"),modifier=Modifier.fillMaxWidth(),selected="$league:*" in subs) {
            if(android.os.Build.VERSION.SDK_INT>=33) permission.launch("android.permission.POST_NOTIFICATIONS")
            CalendarStoreRP120.toggle(context,owner,"$league:*")
        }
        LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            items(teams.filter {it.league==league},key={it.id}) {team->
                Row(Modifier.fillMaxWidth().mobileGlassRP133("${team.league}:${team.id}" in subs).clickable {
                    if(android.os.Build.VERSION.SDK_INT>=33) permission.launch("android.permission.POST_NOTIFICATIONS")
                    CalendarStoreRP120.toggle(context,owner,"${team.league}:${team.id}")
                }.padding(12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    EventLogoRP120(team.logo,team.name,Modifier.size(38.dp))
                    Text(team.name,color=Color.White,modifier=Modifier.weight(1f))
                    MobileIconRP133(if("${team.league}:${team.id}" in subs) "check" else "plus")
                }
            }
        }
    }
}

@Composable
private fun MobileCalendarRP133(state:AppState) {
    val context=LocalContext.current.applicationContext
    val owner=CalendarStoreRP120.owner(context,state.activeProfile.id)
    var month by rememberSaveable {mutableStateOf(YearMonth.now().toString())}
    var day by rememberSaveable {mutableStateOf(LocalDate.now().toString())}
    var events by remember {mutableStateOf<List<SportEventRP120>>(emptyList())}
    var subscriptions by remember {mutableStateOf(false)}
    val revision by CalendarStoreRP120.revision.collectAsState()
    LaunchedEffect(owner,month,revision) {events=withContext(Dispatchers.IO) {CalendarStoreRP120.events(context,owner)}}
    LaunchedEffect(owner,month) {
        withContext(Dispatchers.IO) {CalendarStoreRP120.activate(context,owner,state.activeProfile.name);CalendarStoreRP120.refresh(context,owner,month=YearMonth.parse(month))}
    }
    if(subscriptions) MobileSubscriptionsRP133(state,"NBA") {subscriptions=false}
    val ym=YearMonth.parse(month);val start=ym.atDay(1).dayOfWeek.value-1
    LazyColumn(contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item {Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            MobileIconButtonRP133("back",tr("Mes anterior","Previous month")) {month=ym.minusMonths(1).toString()}
            Text(month,color=Color.White,fontSize=18.sp,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f))
            MobileIconButtonRP133("chevron",tr("Mes siguiente","Next month")) {month=ym.plusMonths(1).toString()}
        }}
        item {Row {(if(state.interfaceLanguage=="es") listOf("L","M","M","J","V","S","D") else listOf("M","T","W","T","F","S","S")).forEach {Box(Modifier.weight(1f),contentAlignment=Alignment.Center) {Text(it,color=Muted,fontSize=12.sp)}}}}
        item {Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
            repeat((start+ym.lengthOfMonth()+6)/7) {week->Row(horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                repeat(7) {column->
                    val number=week*7+column-start+1
                    val date=if(number in 1..ym.lengthOfMonth()) ym.atDay(number).toString() else ""
                    Box(Modifier.weight(1f).height(53.dp).then(if(date.isNotEmpty()) Modifier.mobileGlassRP133(date==day).clickable {day=date} else Modifier),contentAlignment=Alignment.Center) {
                        if(date.isNotEmpty()) Column(horizontalAlignment=Alignment.CenterHorizontally) {
                            Text(number.toString(),color=Color.White,fontSize=13.sp)
                            events.firstOrNull {it.day()==date}?.let {event->EventLogoRP120(event.logos.firstOrNull() ?: CalendarSourcesRP120.logo(event.league),event.league,Modifier.size(20.dp))}
                        }
                    }
                }
            }}
        }}
        item {MobileButtonRP133(tr("Suscripciones","Subscriptions"),"plus",Modifier.fillMaxWidth()) {subscriptions=true}}
        items(events.filter {it.day()==day},key={it.key}) {event->Row(Modifier.fillMaxWidth().mobileGlassRP133().padding(12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            EventLogoRP120(event.logos.firstOrNull() ?: CalendarSourcesRP120.logo(event.league),event.league,Modifier.size(40.dp))
            Column {Text(event.title,color=Color.White,fontSize=14.sp,fontWeight=FontWeight.Bold);Text(event.league+" · "+if(event.timed) DateTimeFormatter.ofPattern("h:mm a").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(event.starts)) else day,color=Muted,fontSize=12.sp)}
        }}
        if(events.none {it.day()==day}) item {Text(tr("Sin eventos seguidos en esta fecha.","No followed events on this date."),color=Muted)}
    }
}

@Composable
internal fun MobileNotificationsRP133(state:AppState) {
    LaunchedEffect(Unit) {state.markNoticesRead()}
    LazyColumn(contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item {Text(tr("Notificaciones","Notifications"),color=Color.White,fontSize=24.sp,fontWeight=FontWeight.Bold)}
        item {MobileButtonRP133(tr("Novedades de BruniO","What's new in BruniO"),"bell",Modifier.fillMaxWidth()) {state.changelogOpen=true}}
        items(state.notifications,key={it.id}) {notice->Row(Modifier.fillMaxWidth().mobileGlassRP133().clickable {state.launch {state.open(notice.item)}}.padding(12.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            AsyncImage(notice.item.poster,notice.item.title,Modifier.width(46.dp).height(67.dp).clip(RoundedCornerShape(7.dp)),contentScale=ContentScale.Crop)
            Column {Text(notice.item.title,color=Color.White,fontWeight=FontWeight.Bold);Text(notice.message,color=Muted,fontSize=13.sp);Text(notice.date,color=Muted,fontSize=11.sp)}
        }}
    }
}
