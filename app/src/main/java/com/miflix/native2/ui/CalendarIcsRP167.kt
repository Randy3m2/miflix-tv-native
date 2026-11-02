package com.miflix.native2.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.miflix.native2.data.*
import kotlinx.coroutines.*
import java.time.*

internal fun calendarSharedEntriesRP167(state:AppState)=state.calendarEvents.mapNotNull {e->
 val instant=runCatching {Instant.parse(e.starts)}.getOrElse {runCatching {OffsetDateTime.parse(e.starts).toInstant()}.getOrNull()} ?: return@mapNotNull null
 CalendarIcsRP167.Entry("shared:"+e.id,e.title,instant.atZone(ZoneId.systemDefault()).toLocalDate().toString(),instant.toEpochMilli(),"BruniO · "+e.kind)
}
@Composable internal fun CalendarExportRP167(state:AppState,close:()->Unit) {
 val context=LocalContext.current
 val owner="${state.session?.userId ?: "local"}:${state.activeProfile.id}"
 val scope=rememberCoroutineScope()
 var busy by remember {mutableStateOf(false)}
 var message by remember {mutableStateOf("")}
 var link by remember(owner) {mutableStateOf(CalendarFeedRP167.url(context,owner))}
 fun content():String {val shared=calendarSharedEntriesRP167(state);CalendarFeedRP167.saveShared(context,owner,shared);return CalendarIcsRP167.encode(owner,CalendarIcsRP167.localEntries(context,owner)+shared)}
 val save=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/calendar")) {uri->if(uri!=null)scope.launch {try {val text=content();withContext(Dispatchers.IO) {context.contentResolver.openOutputStream(uri)?.use {it.write(text.toByteArray(Charsets.UTF_8))} ?: error("Unable to save")};message=tr("Calendario exportado.","Calendar exported.")}catch(e:Exception){message=e.message.orEmpty()}}}
 MobileSheetRP133("BruniO · Calendar ICS",close) {
  Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
   Text(tr("BruniO sigue siendo tu calendario principal. Exporta una copia para Google Calendar, Outlook o Samsung Calendar.","BruniO remains your main calendar. Export a copy for Google Calendar, Outlook or Samsung Calendar."),color=Muted)
   MobileButtonRP133(tr("Guardar archivo .ics","Save .ics file"),"download",Modifier.fillMaxWidth()) {save.launch("BruniO-calendar.ics")}
   MobileButtonRP133(tr("Compartir / importar .ics","Share / import .ics"),"calendar",Modifier.fillMaxWidth()) {runCatching {CalendarIcsRP167.share(context,content())}.onFailure {message=it.message.orEmpty()}}
   Text(tr("Suscripción automática: requiere instalar el servicio ICS RP167 en tu backend. La URL privada permite consultar los eventos; puedes revocarla aquí.","Live subscription requires the RP167 ICS backend service. The private URL grants read access to events; you can revoke it here."),color=Muted)
   if(state.session!=null) {
    MobileButtonRP133(if(link.isBlank()) tr("Crear suscripción HTTPS","Create HTTPS subscription") else tr("Actualizar suscripción","Update subscription"),modifier=Modifier.fillMaxWidth(),enabled=!busy) {scope.launch {busy=true;try {val s=state.session ?: error("Sign in first");val ics=content();if(link.isBlank())link=CalendarFeedRP167.create(context,s,state.activeProfile.id,ics) else CalendarFeedRP167.update(context,s,state.activeProfile.id,ics);message=tr("Suscripción lista. Copia la URL y agrégala por URL en tu calendario externo.","Subscription ready. Copy the URL and add it by URL in your external calendar.")}catch(e:CancellationException){throw e}catch(_:Exception){message=tr("No se pudo publicar. Instala el SQL y la función brunio-calendar-feed incluidos en RP167; exportar .ics funciona sin eso.","Could not publish. Install the RP167 SQL and brunio-calendar-feed function; .ics export works without them.")}finally {busy=false}}}
    if(link.isNotBlank()) {
     MobileButtonRP133(tr("Copiar URL privada","Copy private URL"),modifier=Modifier.fillMaxWidth()) {(context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager).setPrimaryClip(android.content.ClipData.newPlainText("BruniO Calendar",link));message=tr("URL copiada.","URL copied.")}
     MobileButtonRP133(tr("Revocar suscripción","Revoke subscription"),modifier=Modifier.fillMaxWidth(),enabled=!busy) {scope.launch {busy=true;try {CalendarFeedRP167.revoke(context,state.session ?: error("Sign in first"),state.activeProfile.id);link="";message=tr("URL revocada.","URL revoked.")}catch(_:Exception){message=tr("No se pudo revocar. Reintenta con conexión.","Could not revoke. Retry when online.")}finally {busy=false}}}
    }
   }
   Text(tr("El archivo es una copia. La suscripción se publica al actualizar este calendario y en segundo plano cada 6 h, sujeto a Android. Google/Outlook deciden cuándo consultan la URL.","The file is a snapshot. Subscriptions publish when this calendar refreshes and in the background every 6 hours, subject to Android. Google/Outlook control when they fetch the URL."),color=Muted)
   if(message.isNotBlank())Text(message,color=MobileTintRP133)
  }
 }
}
