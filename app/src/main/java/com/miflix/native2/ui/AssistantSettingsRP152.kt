package com.miflix.native2.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.miflix.native2.data.GeminiAssistantRP150

@Composable
internal fun AssistantSettingsRP152(state:AppState) {
    val context=LocalContext.current.applicationContext
    val ai=remember(context) {GeminiAssistantRP150(context)}
    val prefs=remember(context) {context.getSharedPreferences("brunio_gemini_rp150",0)}
    var enabled by remember {mutableStateOf(prefs.getBoolean("enabled",false))}
    var configured by remember {mutableStateOf(ai.configured())}
    var setup by remember {mutableStateOf(false)}
    if(setup) GeminiSetupRP150(state,ai) {configured=ai.configured();if(!configured) {enabled=false;prefs.edit().putBoolean("enabled",false).apply()};setup=false}
    Column(verticalArrangement=Arrangement.spacedBy(14.dp)) {
        Text("✦  BruniO Assistant",color=Color.White,fontSize=25.sp,fontWeight=FontWeight.Bold)
        AssistantActionRP152(if(state.assistantEnabledRP155) "BruniO Assistant · ON" else "BruniO Assistant · OFF",selected=state.assistantEnabledRP155) {state.saveAssistantEnabledRP155(!state.assistantEnabledRP155)}
        Text(tr("Al activarlo aparece el icono AI junto a Settings. Comandos de canales, juegos, posiciones, recordatorios y ayuda de la app.","Enable to show the AI icon beside Settings. Channels, games, standings, reminders and app help."),color=Muted,fontSize=13.sp)
        Text(tr("Descubre a tu manera","Discover your way"),color=Color(0xffc3adeb),fontSize=16.sp)
        Text(tr("Los filtros locales siempre están disponibles. Añade Gemini si quieres interpretar consultas más libres.","Local discovery is always available. Add Gemini for more flexible requests."),color=Muted,fontSize=14.sp)
        Text(if(configured) tr("✓ Clave guardada en esta TV","✓ Key saved on this TV") else tr("Gemini aún no está configurado","Gemini is not configured yet"),color=if(configured) Color(0xff9fdac6) else Muted,fontSize=14.sp)
        FlowRow(horizontalArrangement=Arrangement.spacedBy(10.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            AssistantActionRP152(if(enabled && configured) "Gemini · ON" else "Gemini · OFF",selected=enabled && configured) {
                if(!configured) setup=true else {enabled=!enabled;prefs.edit().putBoolean("enabled",enabled).apply()}
            }
            AssistantActionRP152(tr("Configurar Gemini","Set up Gemini"),"gear") {setup=true}
        }
        Text(tr("Clave personal cifrada en este dispositivo. Las consultas completas pueden enviarse después de una pausa al usar Gemini. Si Gemini falla o alcanza su cuota, BruniO usa los filtros locales.","Your personal key is encrypted on this device. Completed requests may be sent after a pause when Gemini is enabled. If Gemini fails or reaches its quota, BruniO uses local discovery."),color=Muted,fontSize=13.sp)
        Text(tr("Usa un proyecto Free Tier sin facturación si deseas evitar cargos. Las cuotas de Google son independientes de BruniO.","Use a Free Tier project without billing to avoid charges. Google quotas are independent of BruniO."),color=Muted,fontSize=13.sp)
        Spacer(Modifier.height(4.dp))
        Text(tr("Miniaturas del reproductor · ON","Player scene previews · ON"),color=Color.White,fontSize=16.sp,fontWeight=FontWeight.SemiBold)
        Text(tr("Activadas automáticamente. La disponibilidad de imágenes depende de la fuente; OK confirma el salto y Atrás cancela.","Enabled automatically. Image availability depends on the source; OK confirms the seek and Back cancels."),color=Muted,fontSize=13.sp)
    }
}
