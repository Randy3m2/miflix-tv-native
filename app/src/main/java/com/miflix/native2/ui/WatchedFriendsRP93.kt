package com.miflix.native2.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
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
    LaunchedEffect(owner,item.cloudId) {people=state.friendsWhoWatchedRP93(item.cloudId)}
    if(people.isEmpty()) return
    val label=if(item.type=="series") tr("Vieron episodios","Watched episodes") else tr("Ya la vieron","Already watched")
    Box(Modifier.width(232.dp).height(64.dp)) {
        FocusButton("",modifier=Modifier.fillMaxSize().semantics {contentDescription=label+": "+people.joinToString {it.nickname}},
            horizontalPadding=0.dp,onFocused={focused=it}) {showNames=true}
        Row(Modifier.align(Alignment.Center).padding(horizontal=10.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)) {
            people.take(4).forEach {person ->
                ProfileAvatar(Profile(person.id,person.nickname,person.avatar),Modifier.size(26.dp).clip(CircleShape)
                    .border(1.dp,if(focused) Color.Black.copy(alpha=.4f) else Color.White.copy(alpha=.65f),CircleShape))
            }
            Column {
                if(people.size>4) Text("+${people.size-4}",color=if(focused) Color.Black else Color.White,fontSize=12.sp)
                Text(label,color=if(focused) Color.Black else Color.White,fontSize=11.sp,maxLines=2)
            }
        }
    }
    if(showNames) Dialog(onDismissRequest={showNames=false}) {
        Column(Modifier.width(410.dp).background(Color(0xFF111111),RoundedCornerShape(22.dp))
            .border(1.dp,Color.White.copy(alpha=.15f),RoundedCornerShape(22.dp)).padding(24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text(label,color=Color.White,fontSize=23.sp)
            androidx.compose.foundation.lazy.LazyColumn(Modifier.heightIn(max=280.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                items(people.size,key={people[it].id}) {index ->
                    val person=people[index]
                    var rowFocus by remember(person.id) {mutableStateOf(false)}
                    Row(Modifier.fillMaxWidth().onFocusChanged {rowFocus=it.isFocused}
                        .background(if(rowFocus) Color(0xFF333333) else Color.Transparent,RoundedCornerShape(10.dp))
                        .focusable().padding(6.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                        ProfileAvatar(Profile(person.id,person.nickname,person.avatar),Modifier.size(34.dp).clip(CircleShape))
                        Text(person.nickname,color=Color.White,fontSize=17.sp)
                    }
                }
            }
            FocusButton(tr("Cerrar","Close"),modifier=Modifier.fillMaxWidth()) {showNames=false}
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
