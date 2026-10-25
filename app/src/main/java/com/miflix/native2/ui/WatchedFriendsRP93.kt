package com.miflix.native2.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Text
import com.miflix.native2.data.SocialPerson
import com.miflix.native2.model.MediaSummary
import com.miflix.native2.model.Profile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
internal fun WatchedFriendsBadgeRP93(state:AppState,item:MediaSummary) {
    val owner=state.watchedFriendsOwnerRP93 ?: return
    var people by remember(owner,item.cloudId) {mutableStateOf<List<SocialPerson>>(emptyList())}
    var showNames by remember(owner,item.cloudId) {mutableStateOf(false)}
    var focused by remember(owner,item.cloudId) {mutableStateOf(false)}
    val pillFocus=remember(owner,item.cloudId) {FocusRequester()}
    val scope=rememberCoroutineScope()
    LaunchedEffect(owner,item.cloudId) {people=state.friendsWhoWatchedRP93(item.cloudId)}
    if(people.isEmpty()) return
    val label=if(item.type=="series") tr("Vieron episodios","Watched episodes") else tr("Ya la vieron","Already watched")
    val ink=if(focused && !showNames) Color.White else Color.White.copy(alpha=.92f)
    val preview=people.take(3)
    Row(Modifier.widthIn(max=264.dp).height(40.dp).focusRequester(pillFocus)
        .onFocusChanged {focused=it.isFocused}.clip(CircleShape)
        .background(GlassActionBrushRP108(focused && !showNames))
        .semantics {contentDescription=label+" · ${people.size}"}
        .tvClick {showNames=true}.clickable(role=Role.Button) {showNames=true}
        .padding(horizontal=10.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        Box(Modifier.width((24+(preview.size-1)*16).dp).height(24.dp)) {
            preview.forEachIndexed {index,person ->
                ProfileAvatar(Profile(person.id,person.nickname,person.avatar),Modifier.offset(x=(index*16).dp).size(24.dp).clip(CircleShape))
            }
        }
        if(people.size>preview.size) Text("+${people.size-preview.size}",color=ink,fontSize=12.sp)
        Text(label,color=ink,fontSize=13.sp,maxLines=1,overflow=TextOverflow.Ellipsis,modifier=Modifier.weight(1f,fill=false))
        Text("›",color=ink.copy(alpha=.7f),fontSize=20.sp)
    }
    fun dismiss() {
        showNames=false
        scope.launch {withFrameNanos {};runCatching {pillFocus.requestFocus()}}
    }
    if(showNames) WatchedPeopleRP96(label,people,::dismiss)
}

@Composable
private fun WatchedPeopleRP96(label:String,people:List<SocialPerson>,close:()->Unit) {
    val firstFocus=remember {FocusRequester()}
    val closeFocus=remember {FocusRequester()}
    var closeFocused by remember {mutableStateOf(false)}
    LaunchedEffect(Unit) {withFrameNanos {};runCatching {if(people.isEmpty()) closeFocus.requestFocus() else firstFocus.requestFocus()}}
    Dialog(onDismissRequest=close,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Column(Modifier.widthIn(max=350.dp).fillMaxWidth(.86f).heightIn(max=390.dp)
            .clip(RoundedCornerShape(22.dp)).background(Color(0xF2171717)).padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(label,color=Color.White,fontSize=18.sp,fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis)
                    Text("${people.size} "+tr(if(people.size==1) "amigo" else "amigos",if(people.size==1) "friend" else "friends"),color=Muted,fontSize=12.sp)
                }
                Box(Modifier.size(32.dp).focusRequester(closeFocus).onFocusChanged {closeFocused=it.isFocused}
                    .clip(CircleShape).background(GlassActionBrushRP108(closeFocused))
                    .semantics {contentDescription=tr("Cerrar","Close")}
                    .tvClick(close).clickable(role=Role.Button,onClick=close),contentAlignment=Alignment.Center) {
                    Text("×",color=if(closeFocused) Color.White else Color.White.copy(alpha=.8f),fontSize=24.sp)
                }
            }
            LazyColumn(Modifier.fillMaxWidth().heightIn(max=276.dp),contentPadding=PaddingValues(vertical=4.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                items(people,key={it.id}) {person ->
                    var rowFocused by remember(person.id) {mutableStateOf(false)}
                    Row(Modifier.fillMaxWidth().then(if(person.id==people.firstOrNull()?.id) Modifier.focusRequester(firstFocus) else Modifier)
                        .onFocusChanged {rowFocused=it.isFocused}.clip(RoundedCornerShape(14.dp))
                        .background(GlassActionBrushRP108(rowFocused)).focusable().padding(8.dp),
                        verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                        ProfileAvatar(Profile(person.id,person.nickname,person.avatar),Modifier.size(40.dp).clip(CircleShape))
                        Column(Modifier.weight(1f)) {
                            Text(person.nickname,color=if(rowFocused) Color.White else Color.White,fontSize=16.sp,fontWeight=FontWeight.Medium,maxLines=1,overflow=TextOverflow.Ellipsis)
                            Text(tr("Amigo en BruniO","BruniO friend"),color=if(rowFocused) Color(0xFFDDDDDD) else Muted,fontSize=11.sp,maxLines=1)
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun WatchedFriendsSettingRP93(state:AppState) {
    val owner=state.watchedFriendsOwnerRP93 ?: return
    var value by remember(owner) {mutableStateOf<Boolean?>(null)}
    var busy by remember(owner) {mutableStateOf(false)}
    var failed by remember(owner) {mutableStateOf(false)}
    val scope=rememberCoroutineScope()
    LaunchedEffect(owner) {
        try {
            value=state.watchedSharingRP93()
        } catch(e:CancellationException) {
            throw e
        } catch(_:Exception) {
            failed=true
        }
    }
    SettingsSectionCard {
        SettingsTileRC103(tr("Compartir lo que ya vi","Share what I watched"),
            when {busy -> tr("Guardando…","Saving…");failed -> tr("No se pudo conectar. Pulsa para reintentar.","Could not connect. Select to retry.")
                else -> tr("Tus amigos pueden ver tu avatar en los títulos que viste. Se guarda por perfil.","Friends can see your avatar on titles you watched. Saved per profile.")},
            "friends",checked=value ?: false) {
            if(!busy) scope.launch {
                busy=true
                try {
                    value=if(value==null || failed) state.watchedSharingRP93() else state.saveWatchedSharingRP93(value!=true)
                    failed=false
                } catch(e:CancellationException) {throw e} catch(_:Exception) {failed=true}
                finally {busy=false}
            }
        }
    }
}
