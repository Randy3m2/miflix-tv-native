package com.miflix.native2.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.miflix.native2.data.FriendActivity
import com.miflix.native2.model.MediaSummary
import com.miflix.native2.model.PartyRole
import com.miflix.native2.model.Profile
import kotlinx.coroutines.CancellationException

internal val ModernSurfaceRP63=Color(0xFF101114)
internal val ModernBorderRP63=Color(0xFF35373E)

@Composable
internal fun ModernButtonRP63(text:String,modifier:Modifier=Modifier,primary:Boolean=false,enabled:Boolean=true,destructive:Boolean=false,action:()->Unit) {
    var focused by remember {mutableStateOf(false)}
    val active=actionFocusVisibleRP64(focused && enabled)
    val shape=RoundedCornerShape(12.dp)
    Box(modifier.onFocusChanged {focused=it.isFocused}.focusable(enabled)
        .tvClick {if(enabled) action()}.clickable(enabled=enabled,onClick=action)
        .background(if(active) Color.White else if(primary) Color(0xFF211F28) else Color(0xFF17181C),shape)
        .border(if(active) 2.dp else 1.dp,if(active) Color.White else if(primary) Accent.copy(alpha=.55f) else ModernBorderRP63,shape)
        .padding(horizontal=18.dp,vertical=13.dp),contentAlignment=Alignment.Center) {
        Text(text,color=if(!enabled) Color(0xFF666871) else if(active) Color.Black else if(destructive) Color(0xFFFF858D) else Color.White,fontSize=15.sp,fontWeight=FontWeight.SemiBold)
    }
}

@Composable
internal fun ModernDialogRP63(close:()->Unit,width:Float=.78f,content:@Composable ColumnScope.()->Unit) {
    Dialog(onDismissRequest=close,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        BoxWithConstraints(Modifier.fillMaxSize().padding(22.dp),contentAlignment=Alignment.Center) {
            Column(Modifier.consumeOpeningPress().width((maxWidth*width).coerceAtMost(840.dp)).heightIn(max=maxHeight)
                .clip(RoundedCornerShape(22.dp)).background(ModernSurfaceRP63)
                .border(1.dp,ModernBorderRP63,RoundedCornerShape(22.dp)).verticalScroll(rememberScrollState()),content=content)
        }
    }
}

@Composable
internal fun ModernFriendActionsRP63(state:AppState,friend:FriendActivity,close:()->Unit,onContentOpened:()->Unit) {
    var recommendations by remember(friend.id) {mutableStateOf(false)}
    var confirmLeave by remember(friend.id) {mutableStateOf(false)}
    var busy by remember(friend.id) {mutableStateOf(false)}
    var item by remember(friend.id,friend.cloudId) {mutableStateOf<MediaSummary?>(null)}
    val owner=state.socialIdentityId
    LaunchedEffect(friend.id,friend.cloudId,owner) {
        if(friend.cloudId.matches(Regex("^tmdb:(movie|series):[0-9]+$"))) {
            try {val result=state.tmdb.byCloudId(friend.cloudId);if(owner==state.socialIdentityId) item=result}
            catch(e:CancellationException) {throw e} catch(_:Exception) { }
        }
    }
    val first=remember {FocusRequester()}
    LaunchedEffect(confirmLeave,recommendations) {if(!recommendations) {withFrameNanos {};runCatching {first.requestFocus()}}}
    val canWatch=friend.online && friend.cloudId.matches(Regex("^tmdb:(movie|series):[0-9]+$"))
    fun watch() {
        if(busy) return
        busy=true;onContentOpened()
        state.launch {try {state.socialAction {state.watchFriendIndependently(friend)}} finally {busy=false}}
    }
    ModernDialogRP63(close={if(!busy) close()},width=.63f) {
        Box(Modifier.fillMaxWidth().height(166.dp).background(Color(0xFF1A1B20))) {
            item?.let {AsyncImage(it.backdrop ?: it.poster,null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)}
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent,ModernSurfaceRP63))))
            ModernButtonRP63("×",Modifier.align(Alignment.TopEnd).padding(12.dp),enabled=!busy,action=close)
            Row(Modifier.align(Alignment.BottomStart).padding(horizontal=24.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)) {
                ProfileAvatar(Profile(friend.id,friend.nickname,friend.avatar),Modifier.size(62.dp).clip(CircleShape))
                Column {
                    Text("@${friend.nickname}",color=Color.White,fontSize=23.sp,fontWeight=FontWeight.Bold)
                    Text(if(friend.online && friend.title.isNotBlank()) tr("Viendo ahora","Watching now") else if(friend.online) tr("En línea","Online") else tr("Desconectado","Offline"),color=Muted,fontSize=12.sp)
                }
            }
        }
        Column(Modifier.padding(24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text(friend.title.ifBlank {tr("Actividad del amigo","Friend activity")},color=Color.White,fontSize=29.sp,fontWeight=FontWeight.Bold)
            if(friend.season>0 && friend.title.isNotBlank()) Text(tr("Temporada ${friend.season} · Episodio ${friend.episode}","Season ${friend.season} · Episode ${friend.episode}"),color=Muted,fontSize=15.sp)
            if(confirmLeave) {
                Text(if(state.partyRole==PartyRole.HOST) tr("Se cerrará tu sala actual para ver por tu cuenta. ¿Continuar?","Your current party will close to watch independently. Continue?") else tr("Saldrás de tu sala para ver por tu cuenta. ¿Continuar?","You will leave your party to watch independently. Continue?"),color=Color.White)
                Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    ModernButtonRP63(tr("Cancelar","Cancel"),Modifier.weight(1f).focusRequester(first),action={confirmLeave=false})
                    ModernButtonRP63(tr("Continuar","Continue"),Modifier.weight(1f),primary=true,enabled=!busy,action=::watch)
                }
            } else {
                Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    ModernButtonRP63(if(busy) tr("Abriendo…","Opening…") else "▶  "+tr("Ver","Watch"),Modifier.weight(1f).focusRequester(first),primary=true,enabled=!busy,action={
                        if(!canWatch) state.error=tr("No hay un título disponible ahora.","No title is available right now.")
                        else if(state.watchParty!=null || state.pendingPartyCode!=null) confirmLeave=true else watch()
                    })
                    val pending=friend.room!=null && state.pendingPartyCode==friend.room
                    ModernButtonRP63(if(pending) tr("Solicitud enviada","Request sent") else tr("Solicitar acceso","Request Access"),Modifier.weight(1f),enabled=!busy && !pending && friend.room!=null && !state.kidsMode,action={
                        friend.room?.let {room ->busy=true;state.launch {try {state.socialAction {state.joinWatchParty(room)}} finally {busy=false}}}
                    })
                }
                if(friend.room==null) Text(tr("Acceso disponible cuando tu amigo tenga una sala activa.","Access is available when your friend has an active party."),color=Muted,fontSize=11.sp)
                Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    ModernButtonRP63(tr("Ver detalles","View details"),Modifier.weight(1f),enabled=!busy,action={
                        if(!friend.cloudId.matches(Regex("^tmdb:(movie|series):[0-9]+$"))) state.error=tr("No hay detalles para esta actividad.","No details are available for this activity.")
                        else {busy=true;state.launch {try {state.socialAction {
                            val title=item ?: state.tmdb.byCloudId(friend.cloudId) ?: error(tr("Título no disponible","Title unavailable"))
                            state.open(title);if(state.selected?.cloudId==title.cloudId && state.screen==Screen.DETAILS) onContentOpened()
                        }} finally {busy=false}}}
                    })
                    ModernButtonRP63(tr("Recomendaciones","Recommendations"),Modifier.weight(1f),enabled=!busy,action={recommendations=true})
                }
            }
            if(state.partyStatus.isNotBlank()) Text(state.partyStatus,color=Muted,fontSize=12.sp)
            state.error?.let {Text(it,color=Color(0xFFFFA1A7),fontSize=12.sp,maxLines=3)}
        }
    }
    if(recommendations) FriendRecommendationsDialog(state,friend.id,friend.nickname) {recommendations=false}
}

@Composable
internal fun ModernSocialSettingsRP63(state:AppState,close:()->Unit) {
    var nick by remember(state.socialIdentityId,state.nickname) {mutableStateOf(state.nickname)}
    var find by remember(state.socialIdentityId) {mutableStateOf("")}
    val phrases=remember(state.socialIdentityId) {mutableStateListOf<String>().apply {addAll(state.partyPhrases);while(size<3) add("")}}
    var busy by remember {mutableStateOf(false)}
    val first=remember {FocusRequester()}
    LaunchedEffect(Unit) {withFrameNanos {};runCatching {first.requestFocus()}}
    ModernDialogRP63(close,width=.90f) {
        Row(Modifier.fillMaxWidth().padding(24.dp),verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(tr("Perfil social","Social profile"),color=Color.White,fontSize=29.sp,fontWeight=FontWeight.Bold)
                Text(state.activeProfile.name,color=Muted,fontSize=16.sp)
            }
            ModernButtonRP63("×",action=close)
        }
        Row(Modifier.padding(horizontal=24.dp),horizontalArrangement=Arrangement.spacedBy(26.dp)) {
            Column(Modifier.weight(.30f).padding(vertical=14.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(14.dp)) {
                ProfileAvatar(state.activeProfile,Modifier.size(112.dp).clip(CircleShape))
                Text(state.activeProfile.name,color=Color.White,fontSize=24.sp,fontWeight=FontWeight.Bold)
                Text("@${state.nickname}",color=Muted,fontSize=17.sp)
                Text(tr("Tu identidad social para este perfil.","Your social identity for this profile."),color=Muted,fontSize=13.sp)
            }
            Column(Modifier.weight(.70f),verticalArrangement=Arrangement.spacedBy(14.dp)) {
                ModernSettingsGroupRP63(tr("Apodo","Nickname")) {
                    Row(horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=Alignment.CenterVertically) {
                        NativeTextField(nick,{nick=it},tr("Tu apodo","Your nickname"),modifier=Modifier.weight(1f).focusRequester(first))
                        ModernButtonRP63(tr("Guardar apodo","Save nickname"),enabled=!busy,action={busy=true;state.launch {try {state.socialAction {state.saveNickname(nick)}} finally {busy=false}}})
                    }
                }
                ModernSettingsGroupRP63(tr("Agregar amigo","Add a friend")) {
                    Row(horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=Alignment.CenterVertically) {
                        NativeTextField(find,{find=it},tr("Apodo de tu amigo","Your friend's nickname"),modifier=Modifier.weight(1f))
                        ModernButtonRP63(tr("Agregar","Add friend"),enabled=!busy && find.isNotBlank(),action={busy=true;state.launch {try {state.socialAction {state.findAndAddFriend(find)}} finally {busy=false}}})
                    }
                }
                ModernSettingsGroupRP63(tr("Frases rápidas","Quick phrases")) {
                    phrases.indices.forEach {i ->NativeTextField(phrases[i],{phrases[i]=it},tr("Frase ${i+1}","Phrase ${i+1}"),modifier=Modifier.fillMaxWidth())}
                    if(phrases.size<8) ModernButtonRP63(tr("+ Agregar frase","+ Add phrase"),action={phrases.add("")})
                    ModernButtonRP63(tr("Guardar frases","Save phrases"),Modifier.align(Alignment.End),action={state.savePhrases(phrases.joinToString("|"))})
                }
            }
        }
        state.error?.let {Text(it,color=Color(0xFFFFA1A7),modifier=Modifier.padding(horizontal=24.dp),fontSize=12.sp)}
        ModernButtonRP63(tr("Listo","Done"),Modifier.fillMaxWidth().padding(24.dp),action=close)
    }
}

@Composable
internal fun ModernSettingsGroupRP63(title:String,content:@Composable ColumnScope.()->Unit) {
    Column(Modifier.fillMaxWidth().background(Color(0xFF141519),RoundedCornerShape(16.dp)).border(1.dp,ModernBorderRP63,RoundedCornerShape(16.dp)).padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Text(title,color=Color.White,fontSize=17.sp,fontWeight=FontWeight.SemiBold)
        content()
    }
}
