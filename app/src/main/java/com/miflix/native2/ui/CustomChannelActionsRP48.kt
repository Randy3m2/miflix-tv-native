package com.miflix.native2.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.miflix.native2.model.PartyRole
import com.miflix.native2.data.LiveChannel

@Composable
internal fun CustomChannelManageRP48(state:AppState,channel:CustomChannelRP45,close:()->Unit) {
    var editing by remember(channel.id) {mutableStateOf(false)}
    if(editing) CustomChannelsPanelRP45(state,initialEditRP48=channel,close=close)
    else CustomChannelActionsRP48(state,channel,{editing=true},close)
}

@Composable
internal fun CustomChannelActionsRP48(state:AppState,channel:CustomChannelRP45,edit:()->Unit,close:()->Unit) {
    val context=LocalContext.current
    val owner=CustomChannelsRP45.owner(state)
    val originalOwner=remember {owner}
    val first=remember {FocusRequester()}
    val favoritesRevision=LiveLibrary.revision
    val isFavorite=remember(favoritesRevision,owner,channel.id) {
        LiveLibrary.favorites(context,state).any { it.provider==CustomChannelsRP45.provider && it.channel.id==channel.id }
    }
    val favoriteChannel=LiveChannel(channel.id,"custom",channel.name,channel.logo,null)

    LaunchedEffect(owner) {if(owner!=originalOwner) close()}
    PlayerMenuDialog(onDismissRequest=close,fullScreen=true) {
        BoxWithConstraints(Modifier.fillMaxSize().consumeOpeningPress()) {
            Column(Modifier.align(Alignment.CenterEnd).fillMaxWidth(if(maxWidth<700.dp) .8f else .4f)
                .background(Color(0xFC101010),RoundedCornerShape(18.dp)).padding(24.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
                Text(channel.name,color=Color.White,fontSize=23.sp)
                Text(tr("CANAL PERSONAL","PERSONAL CHANNEL"),color=Muted,fontSize=12.sp)
                if(state.partyRole!=PartyRole.GUEST && !state.kidsMode) {
                    FocusButton(
                        tr(if(isFavorite) "Quitar de favoritos" else "Agregar a favoritos",
                           if(isFavorite) "Remove from Favorites" else "Add to Favorites"),
                        modifier=Modifier.fillMaxWidth().focusRequester(first)
                    ) {
                        LiveLibrary.toggle(context,state,CustomChannelsRP45.provider,favoriteChannel)
                    }
                    FocusButton(tr("Editar nombre o URL por QR","Edit name or URL by QR"),modifier=Modifier.fillMaxWidth()) {edit()}
                    FocusButton(tr("Eliminar canal","Delete channel"),modifier=Modifier.fillMaxWidth(),destructive=true) {
                        if(originalOwner==CustomChannelsRP45.owner(state)) CustomChannelsRP45.remove(context,owner,channel.id)
                        close()
                    }
                    Text(tr("Se elimina de Others, Live TV y del selector de SplitScreen en este perfil.","Removes it from Others, Live TV and the SplitScreen picker in this profile."),color=Muted,fontSize=12.sp)
                }
                FocusButton(tr("Cerrar","Close")) {close()}
                LaunchedEffect(Unit) {withFrameNanos {};runCatching {first.requestFocus()}}
            }
        }
    }
}
