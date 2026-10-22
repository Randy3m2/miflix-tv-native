package com.miflix.native2.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NativeSourcesSettings(state: AppState) {
    var draft by remember(state.session?.userId,state.activeProfile.id) { mutableStateOf("") }
    var showQrRP76 by remember(state.session?.userId,state.activeProfile.id) {mutableStateOf(false)}
    Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        Text(tr("Motor nativo · Experimental y opcional","Native engine · Experimental and optional"),color=Color.White,fontSize=22.sp)
        Text(tr("Busca sin instalar add-ons. Usa Knaben y tu propia cuenta TorBox; solo muestra archivos disponibles en caché. Actívalo solo cuando desees probarlo.",
            "Search without installing add-ons. Uses Knaben and your own TorBox account; only shows cached files. Enable it only when you want to try it."),color=Muted,fontSize=14.sp)
        FocusButton(tr("Motor nativo: ","Native engine: ")+if(state.nativeSourceEnabled) tr("Encendido","On") else tr("Apagado","Off"),primary=state.nativeSourceEnabled) {
            if(!state.nativeSourceSaving) state.toggleNativeSources()
        }
        if(state.nativeSourceConfigured) FocusButton(tr("Solo motor nativo: ","Native engine only: ")+if(state.nativeOnlyRP75) tr("Sí","Yes") else tr("No","No"),primary=state.nativeOnlyRP75) {
            if(!state.nativeSourceSaving) state.setNativeOnlyRP75(!state.nativeOnlyRP75)
        }
        Text(tr("La clave se guarda cifrada para tu cuenta en esta TV. No se comparte con amigos ni se envía al buscador. Configúrala de nuevo en otros dispositivos.",
            "The key is encrypted for your account on this TV. It is not shared with friends or sent to the search index. Set it up separately on other devices."),color=Muted,fontSize=14.sp)
        NativeTextField(draft,{ draft=it },if(state.nativeSourceConfigured) tr("Nueva API key de TorBox · reemplazar","New TorBox API key · replace") else "TorBox API key",password=true,modifier=Modifier.fillMaxWidth())
        FlowRow(horizontalArrangement=Arrangement.spacedBy(16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
            FocusButton(tr("Pegar API key por QR local","Paste API key via local QR")) {
                if(!state.nativeSourceSaving && state.session!=null && !state.kidsMode && state.partyRole!=com.miflix.native2.model.PartyRole.GUEST) showQrRP76=true
            }
            FocusButton(if(state.nativeSourceSaving) tr("Verificando…","Checking…") else tr("Verificar y guardar","Verify and save")) {
                if(!state.nativeSourceSaving) state.launch { state.socialAction { state.saveNativeSourceKey(draft);draft="" } }
            }
            if(state.nativeSourceConfigured) FocusButton(tr("Eliminar clave y apagar","Remove key and disable")) { if(!state.nativeSourceSaving) state.clearNativeSourceKey() }
        }
        if(state.nativeSourceStatus.isNotBlank()) Text(state.nativeSourceStatus,color=Color.White,fontSize=14.sp)
        Text(if(state.nativeOnlyRP75 && state.nativeSourceEnabled)
            tr("Solo motor nativo activo: no se consultan add-ons para buscar enlaces VOD. Usa Knaben y TorBox; no instala P2P en esta TV.",
                "Native-only mode is active: add-ons are not queried for VOD sources. Uses Knaben and TorBox; no P2P engine is installed on this TV.")
            else tr("Al reproducir, TorBox añade el archivo elegido a tu cuenta y genera su enlace. Con el modo Solo motor nativo apagado, combina sus resultados con tus add-ons.",
                "On playback, TorBox adds the selected file to your account and generates its link. With Native engine only off, results are combined with your add-ons."),color=Muted,fontSize=14.sp)
    }
    if(showQrRP76) TorBoxKeyQrRP76(state,received={draft=it},close={showQrRP76=false})
}
