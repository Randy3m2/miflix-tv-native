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
    var draft by remember(state.session?.userId) { mutableStateOf("") }
    Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        Text(tr("Motor nativo de enlaces","Native source engine"),color=Color.White,fontSize=22.sp)
        Text(tr("Busca sin instalar add-ons. Usa Knaben y tu propia cuenta TorBox; solo muestra archivos disponibles en caché. Apagado por defecto.",
            "Search without installing add-ons. Uses Knaben and your own TorBox account; only shows cached files. Off by default."),color=Muted,fontSize=14.sp)
        FocusButton(tr("Motor nativo: ","Native engine: ")+if(state.nativeSourceEnabled) tr("Encendido","On") else tr("Apagado","Off"),primary=state.nativeSourceEnabled) {
            if(!state.nativeSourceSaving) state.toggleNativeSources()
        }
        Text(tr("La clave se guarda cifrada para tu cuenta en esta TV. No se comparte con amigos ni se envía al buscador. Configúrala de nuevo en otros dispositivos.",
            "The key is encrypted for your account on this TV. It is not shared with friends or sent to the search index. Set it up separately on other devices."),color=Muted,fontSize=14.sp)
        NativeTextField(draft,{ draft=it },if(state.nativeSourceConfigured) tr("Nueva API key de TorBox · reemplazar","New TorBox API key · replace") else "TorBox API key",password=true,modifier=Modifier.fillMaxWidth())
        FlowRow(horizontalArrangement=Arrangement.spacedBy(16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
            FocusButton(if(state.nativeSourceSaving) tr("Verificando…","Checking…") else tr("Verificar y guardar","Verify and save")) {
                if(!state.nativeSourceSaving) state.launch { state.socialAction { state.saveNativeSourceKey(draft);draft="" } }
            }
            if(state.nativeSourceConfigured) FocusButton(tr("Eliminar clave y apagar","Remove key and disable")) { if(!state.nativeSourceSaving) state.clearNativeSourceKey() }
        }
        if(state.nativeSourceStatus.isNotBlank()) Text(state.nativeSourceStatus,color=Color.White,fontSize=14.sp)
        Text(tr("Al reproducir, TorBox añade el archivo elegido a tu cuenta y genera su enlace. Tus add-ons siguen funcionando; el motor complementa sus resultados.",
            "On playback, TorBox adds the selected file to your account and generates its link. Your add-ons keep working; the engine supplements their results."),color=Muted,fontSize=14.sp)
    }
}
