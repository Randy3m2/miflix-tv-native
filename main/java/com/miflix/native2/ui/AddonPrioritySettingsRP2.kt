package com.miflix.native2.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.miflix.native2.data.AddonPriorityRP2

@Composable
internal fun AddonPrioritySettingsRP2(state:AppState,close:()->Unit) {
    val owner="${state.session?.userId}:${state.activeProfile.id}"
    var language by remember(owner) {mutableStateOf(AddonPriorityRP2.language(state.audioLanguage))}
    var status by remember(owner) {mutableStateOf("")}
    var moved by remember(owner) {mutableStateOf<Pair<String,Int>?>(null)}
    LaunchedEffect(owner,state.addonManifests.toList()) {
        status=""
        try {state.inspectAddonOrderRP2()} catch(e:kotlinx.coroutines.CancellationException) {throw e} catch(_:Exception) {status=tr("No se pudieron comprobar todos los complementos. Vuelve a abrir este panel para reintentar.","Could not check every add-on. Reopen this panel to retry.")}
    }
    val rows=state.orderedSourceAddonsRP2(language)
    SettingsDialogRC103(tr("Tus fuentes, en tu orden","Your sources, your order"),tr("Solo add-ons instalados que ofrecen enlaces. Se guarda por perfil en esta TV.","Only installed add-ons that provide streams. Saved per profile on this TV."),close) {
        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            FocusButton(tr("Español","Spanish"),primary=language=="es",modifier=Modifier.settingsHelpRC103(tr("Orden para español","Spanish order"),tr("Edita la prioridad usada cuando el audio preferido es español. No cambia tu idioma de reproducción.","Edit the priority used for Spanish audio. This does not change your playback language."),"tracks",tr("Español","Spanish"))) {language="es";moved=null}
            FocusButton(tr("Inglés","English"),primary=language=="en",modifier=Modifier.settingsHelpRC103(tr("Orden para inglés","English order"),tr("Edita la prioridad usada cuando el audio preferido es inglés. No cambia tu idioma de reproducción.","Edit the priority used for English audio. This does not change your playback language."),"tracks",tr("Inglés","English"))) {language="en";moved=null}
        }
        Text(if(language=="es") tr("Audio español primero. Después se aplica este orden entre complementos; inglés queda como alternativa.","Spanish audio comes first. This add-on order applies within that group; English remains a fallback.") else tr("Audio inglés primero. Comet y Torrentio tienen prioridad inicial; puedes cambiarla aquí.","English audio comes first. Comet and Torrentio lead by default; you can change that here."),color=Muted,fontSize=14.sp,lineHeight=20.sp)
        if(status.isNotBlank()) Text(status,color=Muted,fontSize=13.sp)
        if(state.addonOrderLoading) Text(tr("Comprobando tus complementos…","Checking your add-ons…"),color=Color.White,fontSize=15.sp)
        else {
            if(rows.isEmpty()) SettingsHeadingRC103(tr("Aún no hay fuentes","No sources yet"),tr("Añade un manifiesto en Complementos de reproducción. Los add-ons de catálogo no aparecen aquí.","Add a manifest in Stream Add-ons. Catalog-only add-ons do not appear here."))
            rows.forEachIndexed {index,addon ->key(addon.id) {
                val up=remember {FocusRequester()};val down=remember {FocusRequester()}
                LaunchedEffect(moved,rows.map {it.id}) {
                    if(moved?.first==addon.id) {
                        withFrameNanos { }
                        runCatching {if(index>0 && (moved?.second==-1 || index==rows.lastIndex)) up.requestFocus() else if(index<rows.lastIndex) down.requestFocus()}
                    }
                }
                val hint=tr("Mueve este complemento para cambiar su prioridad. El número 1 se intenta antes dentro del grupo de audio preferido.","Move this add-on to change its priority. Number 1 is tried first within the preferred-audio group.")
                Row(Modifier.fillMaxWidth().background(Panel,RoundedCornerShape(18.dp)).border(1.dp,PanelBorder,RoundedCornerShape(18.dp)).padding(14.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    Text("${index+1}".padStart(2,'0'),color=Accent,fontSize=22.sp,fontWeight=FontWeight.Bold)
                    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                        Text(addon.name,color=Color.White,fontSize=16.sp,fontWeight=FontWeight.SemiBold,maxLines=2,overflow=TextOverflow.Ellipsis)
                        Text(if(index==0) tr("Primera prioridad","First priority") else tr("Prioridad ","Priority ")+(index+1),color=Muted,fontSize=12.sp)
                    }
                    if(index>0) FocusButton("↑",horizontalPadding=14.dp,modifier=Modifier.focusRequester(up).settingsHelpRC103(addon.name,hint,"lists","${index+1} / ${rows.size}")) {state.moveSourceAddonRP2(addon.id,-1,language);moved=addon.id to -1}
                    if(index<rows.lastIndex) FocusButton("↓",horizontalPadding=14.dp,modifier=Modifier.focusRequester(down).settingsHelpRC103(addon.name,hint,"lists","${index+1} / ${rows.size}")) {state.moveSourceAddonRP2(addon.id,1,language);moved=addon.id to 1}
                }
            }}
            if(rows.isNotEmpty()) SettingsTileRC103(tr("Restablecer prioridad automática","Reset automatic priority"),if(language=="es") tr("Add-ons latinos primero; después Torrentio y Comet. Solo se incluyen los instalados.","Latin-audio add-ons first, then Torrentio and Comet. Only installed add-ons are included.") else tr("Comet primero, después Torrentio y los demás instalados.","Comet first, then Torrentio and other installed add-ons."),"lists") {moved=null;state.resetSourceAddonOrderRP2(language)}
        }
    }
}
