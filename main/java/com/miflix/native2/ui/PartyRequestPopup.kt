package com.miflix.native2.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Text

/** A separate modal window, above the player and its controls; OK accepts the focused action. */
@Composable
internal fun PartyRequestPopup(state: AppState) {
    if(state.partyRole!=com.miflix.native2.model.PartyRole.HOST) return
    val room=state.watchParty?.roomCode
    val handled=remember(room,state.session?.userId) { mutableStateListOf<String>() }
    val request=state.accessRequests.firstOrNull { it.room==room && it.id !in handled } ?: return
    key(room,request.id) {
        val accept=remember { FocusRequester() }
        var processing by remember { mutableStateOf(false) }
        var failure by remember { mutableStateOf<String?>(null) }
        fun decide(approve: Boolean) {
            if(processing) return
            processing=true; failure=null
            state.launch {
                try {
                    state.decideAccess(request,approve)
                    if(state.accessRequests.none { it.id==request.id && it.room==request.room }) handled.add(request.id)
                    else failure=state.error ?: tr("No se pudo responder. Intenta de nuevo.","Could not respond. Try again.")
                } finally { processing=false }
            }
        }
        Dialog(onDismissRequest={ if(!processing) handled.add(request.id) },properties=DialogProperties(usePlatformDefaultWidth=false)) {
            LaunchedEffect(request.id) { withFrameNanos { }; accept.requestFocus() }
            Column(Modifier.width(570.dp).background(Color(0xF51E1E1E),RoundedCornerShape(20.dp)).padding(28.dp)
                .onPreviewKeyEvent { processing },verticalArrangement=Arrangement.spacedBy(18.dp)) {
                Text(tr("Solicitud para entrar al Party","Request to join your Party"),color=Color.White,fontSize=24.sp,lineHeight=29.sp)
                Text("@${request.nickname} ${tr("quiere unirse a tu sesión","wants to join your session")}",color=Color.White,fontSize=18.sp,lineHeight=22.sp)
                if(processing) Text(tr("Respondiendo…","Responding…"),color=Muted)
                failure?.let { Text(it,color=Color(0xFFFFA5A5),maxLines=3) }
                Row(horizontalArrangement=Arrangement.spacedBy(18.dp)) {
                    FocusButton(tr("Aceptar","Accept"),modifier=Modifier.focusRequester(accept)) { decide(true) }
                    FocusButton(tr("Rechazar","Decline")) { decide(false) }
                }
                Text(tr("OK confirma el botón seleccionado · Atrás lo deja para después","OK confirms the selected button · Back postpones it"),color=Muted,fontSize=12.sp,lineHeight=15.sp)
            }
        }
    }
}
