package com.miflix.native2.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.miflix.native2.data.*
import kotlinx.coroutines.*
import java.time.*

@Composable
internal fun AssistantCommandPanelRP155(state:AppState,command:AssistantCommandRP155,close:()->Unit) {
    val context=LocalContext.current.applicationContext
    val scope=rememberCoroutineScope()
    val owner=state.localExtrasScope()
    var rows by remember(command) {mutableStateOf<List<AssistantLiveRP155>>(emptyList())}
    var message by remember(command) {mutableStateOf("")}
    var busy by remember(command) {mutableStateOf(false)}
    var calendar by remember {mutableStateOf(false)}
    var queue by remember {mutableStateOf(false)}
    var day by remember(command) {mutableStateOf(command.date)}
    var time by remember(command) {mutableStateOf(command.time)}
    var title by remember(command) {mutableStateOf(command.target)}
    var saved by remember(command) {mutableStateOf(false)}
    fun runAction(block:suspend ()->Unit) {if(busy) return;busy=true;scope.launch {try {check(state.localExtrasScope()==owner);block()} catch(e:CancellationException) {throw e} catch(_:Exception) {message=tr("No se pudo completar esta acción. Puedes reintentar.","Could not complete this action. You can retry.")} finally {busy=false}}}
    LaunchedEffect(command,owner) {
        if(command.raw==state.assistantQueryRP156 && AssistantDirectRP156.run(state,command.raw)) {state.assistantQueryRP156=null;close();return@LaunchedEffect}
        if(command.action !in listOf(AssistantActionRP155.CHANNEL,AssistantActionRP155.GAME)) return@LaunchedEffect
        if(command.target.length<2) {message=tr("Dime el nombre del canal o equipo.","Tell me the channel or team name.");return@LaunchedEffect}
        busy=true
        try {
            val answer=withTimeout(30_000) {if(command.action==AssistantActionRP155.GAME) AssistantLiveCatalogRP155.games(state,command.target) else AssistantLiveCatalogRP155.channels(context,state,command.target) to ""}
            ensureActive();if(state.localExtrasScope()!=owner) return@LaunchedEffect
            rows=answer.first
            if(command.raw==state.assistantQueryRP156 && rows.size==1) {
                AssistantLiveCatalogRP155.play(state,rows.single());state.assistantQueryRP156=null;close();return@LaunchedEffect
            }
            message=answer.second.ifBlank {if(rows.isEmpty()) tr("No encontré ese canal en tus fuentes configuradas.","That channel was not found in your configured sources.") else tr("Selecciona la opción que quieres reproducir.","Select the option you want to play.")}
        } catch(e:CancellationException) {throw e} catch(_:Exception) {message=tr("No se pudo consultar la fuente. Revisa Live TV.","Could not query the source. Check Live TV.")} finally {busy=false}
    }
    if(calendar) CalendarDialog(state) {calendar=false}
    if(queue) QueueDialog(state) {queue=false}
    Column(Modifier.fillMaxWidth().background(Color(0xff111115),RoundedCornerShape(16.dp)).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text(tr("BruniO puede ayudarte","BruniO can help"),color=Color.White,fontSize=19.sp)
        if(busy) Text(tr("Consultando…","Checking…"),color=Muted)
        if(message.isNotBlank()) Text(message,color=Muted,fontSize=13.sp)
        when(command.action) {
            AssistantActionRP155.HELP -> {
                Text(tr("Puedo abrir las secciones de la app, buscar canales y juegos en vivo, mostrar posiciones, guardar recordatorios, recomendar algo parecido, afinar resultados y comparar sin spoilers.","I can open app sections, find channels and live games, show standings, save reminders, find similar titles, refine results and compare without spoilers."),color=Muted,fontSize=14.sp)
                listOf("Pon el juego de Lakers que está now","Abre la tabla de posiciones de LIDOM","Pon un recordatorio para Vision Quest el viernes","Algo como Interstellar, pero más corto","Esas no, algo más reciente","Compara estas tres sin spoilers","Abre mi lista","Pon subtítulos en español").forEach {Text("• $it",color=Color.White,fontSize=13.sp)}
            }
            AssistantActionRP155.CHANNEL,AssistantActionRP155.GAME -> {
                rows.forEach {row ->AssistantActionRP152(tr("Reproducir: ","Play: ")+row.channel.name,"play",modifier=Modifier.fillMaxWidth()) {runAction {AssistantLiveCatalogRP155.play(state,row);close()}}}
                AssistantActionRP152(tr("Ver Live TV","Open Live TV")) {state.screen=Screen.LIVE_TV;close()}
            }
            AssistantActionRP155.STANDINGS -> {
                AssistantActionRP152(tr("Abrir posiciones · ","Open standings · ")+command.target) {state.assistantLeagueRP156=command.target;state.normalLiveEntryRP85=false;state.screen=Screen.LIVE_TV;close()}
            }
            AssistantActionRP155.NAVIGATE -> {
                AssistantActionRP152(tr("Abrir: ","Open: ")+command.target) {
                    when(command.target) {
                        "calendar"->calendar=true;"queue"->queue=true
                        "addons","language","playback","appearance" -> {state.assistantSettingsCategoryRP155=if(command.target=="addons") "sources" else command.target;state.screen=Screen.SETTINGS;close()}
                        else->{val next=mapOf("home" to Screen.HOME,"search" to Screen.SEARCH,"live" to Screen.LIVE_TV,"favorites" to Screen.MY_LIST,"notifications" to Screen.NOTIFICATIONS,"settings" to Screen.SETTINGS,"profiles" to Screen.PROFILES,"friends" to Screen.FRIENDS,"franchises" to Screen.COLLECTIONS,"downloads" to Screen.DOWNLOADS)[command.target];if(next!=null) {state.screen=next;close()}}
                    }
                }
            }
            AssistantActionRP155.REMINDER -> {
                Text(tr("Recordatorio en esta TV","Reminder on this TV"),color=Color.White,fontSize=17.sp)
                NativeTextField(title,{title=it.take(120)},tr("Título","Title"),modifier=Modifier.fillMaxWidth())
                NativeTextField(day,{day=it.take(30)},tr("Fecha o día: viernes / 2026-10-30","Date or day: friday / 2026-10-30"),modifier=Modifier.fillMaxWidth())
                NativeTextField(time,{time=it.take(12)},tr("¿A qué hora? Ejemplo: 20:00 o 8 pm","What time? Example: 20:00 or 8 pm"),modifier=Modifier.fillMaxWidth())
                val at=runCatching {AssistantCommandsRP155.reminderAt(day,time,ZonedDateTime.now())}.getOrNull()
                Text(at?.let {"${it.toLocalDate()} · ${it.toLocalTime()} · ${it.zone.id}"} ?: tr("Indica un día y una hora futuros para continuar.","Enter a future date and time to continue."),color=Muted,fontSize=13.sp)
                AssistantActionRP152(if(saved) tr("✓ Recordatorio guardado","✓ Reminder saved") else tr("Guardar recordatorio","Save reminder"),"bell") {
                    if(at!=null && title.isNotBlank() && !saved) runAction {withContext(Dispatchers.IO) {CalendarStoreRP120.addPersonalRP155(context,CalendarStoreRP120.owner(context,state.activeProfile.id),title,at.toInstant().toEpochMilli())};saved=true;message=tr("Guardado en el calendario de este perfil en esta TV.","Saved in this profile's calendar on this TV.")}
                }
            }
            AssistantActionRP155.PLAYBACK -> {
                AssistantActionRP152(tr("Aplicar: ","Apply: ")+command.target) {message=if(state.assistantPlaybackRP155?.invoke(command.target)==true) tr("Acción enviada al reproductor.","Action sent to the player.") else tr("No hay un reproductor activo compatible. Abre un contenido primero.","No compatible active player. Open a title first.")}
            }
            else ->Text(tr("Usa una petición de descubrimiento para esta función.","Use a discovery request for this feature."),color=Muted)
        }
    }
}
