@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package com.miflix.native2.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text

@Composable
internal fun AddonLimitControlRP7(state:AppState,manifest:String) {
    var showLimits by remember(manifest,state.activeProfile.id) {mutableStateOf(false)}
    val limit=state.addonLimitRP7(manifest)
    FocusButton(tr("Enlaces: ","Links: ")+(if(limit==0) tr("Todos","All") else limit.toString())) {showLimits=true}
    if(showLimits) {
        var custom by remember {mutableStateOf(if(limit>0) limit.toString() else "")}
        var error by remember {mutableStateOf("")}
        SettingsDialogRC103(tr("Límite de enlaces","Link limit"),state.streamRepo.addonLabel(manifest),{showLimits=false}) {
            Text(tr("Se guarda por perfil en esta TV. Conserva primero los enlaces del idioma preferido y después el orden de calidad.","Saved per profile on this TV. Keeps preferred-audio links first, then quality order."),color=Muted)
            FlowRow(horizontalArrangement=Arrangement.spacedBy(12.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                listOf(0,5,10,15).forEach {n ->
                    FocusButton(if(n==0) tr("Todos","All") else n.toString(),primary=limit==n) {state.saveAddonLimitRP7(manifest,n);showLimits=false}
                }
            }
            NativeTextField(custom,{custom=it.filter {c ->c.isDigit()}.take(4)},tr("Personalizado · 1 a 1000","Custom · 1 to 1000"),modifier=Modifier.fillMaxWidth())
            if(error.isNotBlank()) Text(error,color=Color.White)
            FocusButton(tr("Aplicar","Apply")) {
                val n=custom.toIntOrNull()
                if(n==null || n !in 1..1000) error=tr("Escribe un número entre 1 y 1000.","Enter a number from 1 to 1000.")
                else {state.saveAddonLimitRP7(manifest,n);showLimits=false}
            }
        }
    }
}
