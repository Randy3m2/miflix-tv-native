package com.miflix.native2.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.tv.material3.Text
import com.miflix.native2.model.MediaSummary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import org.json.JSONObject

@Composable
private fun LocalPanelRC100(title:String,close:()->Unit,content:@Composable ColumnScope.()->Unit) {
    Dialog(onDismissRequest=close) {
        val first=remember {FocusRequester()}
        LaunchedEffect(Unit) {withFrameNanos {};runCatching {first.requestFocus()}}
        Column(Modifier.width(560.dp).heightIn(max=520.dp).background(Panel,RoundedCornerShape(24.dp)).padding(22.dp).verticalScroll(rememberScrollState()).focusGroup(),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text(title,color=Color.White,fontSize=24.sp)
            FocusButton(tr("Cerrar","Close"),modifier=Modifier.focusRequester(first)) {close()}
            content()
        }
    }
}

@Composable
internal fun LocalExtrasSettingsRC100(state:AppState,close:()->Unit) {
    val extras=state.localExtras
    var showLists by remember {mutableStateOf(false)}
    var status by remember {mutableStateOf("")}
    var pendingImport by remember {mutableStateOf<JSONObject?>(null)}
    var working by remember {mutableStateOf(false)}
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    val owner=state.localExtrasScope()
    val export=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) {uri ->
        if(uri!=null && !working) {working=true;scope.launch {
            try {val text=state.exportLocalBackupRC100().toString();withContext(Dispatchers.IO) {context.contentResolver.openOutputStream(uri)?.use {it.write(text.toByteArray(Charsets.UTF_8))} ?: error("Cannot open file")};status=tr("Backup exportado.","Backup exported.")}
            catch(e:Exception) {status=tr("No se pudo exportar: ","Export failed: ")+e.message}
            finally {working=false}
        }}
    }
    val restore=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {uri ->
        if(uri!=null && !working) {working=true;scope.launch {
            try {val body=withContext(Dispatchers.IO) {context.contentResolver.openInputStream(uri)?.use {input ->
                val output=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192)
                while(true) {val n=input.read(buffer);if(n<0) break;require(output.size()+n<=2_000_000) {"Backup too large"};output.write(buffer,0,n)}
                JSONObject(output.toString("UTF-8"))
            } ?: error("Cannot open file")};require(state.localExtrasScope()==owner);state.validateLocalBackupRC100(body);pendingImport=body}
            catch(e:Exception) {status=tr("No se pudo importar: ","Import failed: ")+e.message}
            finally {working=false}
        }}
    }
    if(showLists) LocalListsRC100(state,null) {showLists=false}
    pendingImport?.let {body ->LocalPanelRC100(tr("Restaurar backup","Restore backup"),{pendingImport=null}) {
        Text(tr("Reemplaza la configuración y los datos locales de este perfil. No cambia tu cuenta ni My List.","Replaces this profile's local settings and data. Your account and My List stay unchanged."),color=Muted)
        FocusButton(tr("Restaurar","Restore")) {pendingImport=null;working=true;scope.launch {try {require(state.localExtrasScope()==owner);state.importLocalBackupRC100(body);status=tr("Backup restaurado.","Backup restored.")} catch(e:Exception) {status=e.message.orEmpty()} finally {working=false}}}
    }}
    LocalPanelRC100(tr("Funciones locales","Local features"),close) {
        Text(tr("Guardadas en esta TV. Los interruptores son del dispositivo; listas, ocultos y preferencias se separan por perfil.","Stored on this TV. Switches apply to this device; lists, hidden titles and preferences are per profile."),color=Muted)
        val names=listOf(
            "sleep" to tr("Temporizador de reproducción","Sleep timer"),
            "still" to tr("¿Sigues viendo?","Are you still watching?"),
            "lists" to tr("Listas personales locales","Local personal lists"),
            "hide" to tr("Hoy no quiero ver esto","Not today"),
            "filters" to tr("Recordar filtros del catálogo","Remember catalog filters"),
            "tracks" to tr("Recordar audio y subtítulos por serie","Remember audio and subtitles per series"),
            "technical" to tr("Panel técnico del reproductor","Player technical panel"),
            "backup" to tr("Backup local de configuración","Local settings backup")
        )
        names.forEach { (key,title) ->FocusButton(title+" · "+if(extras.enabled(key)) "ON" else "OFF") {extras.enable(key,!extras.enabled(key));if(key=="filters") state.restoreCustomFilterRC100()} }
        if(extras.enabled("still")) {
            Text(tr("Preguntar tras episodios consecutivos sin usar el mando:","Ask after consecutive episodes without remote interaction:"),color=Muted)
            Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {listOf(2,3,5).forEach {n -> FocusButton("$n",primary=extras.stillLimit==n) {extras.saveStillLimit(n)}}}
        }
        if(extras.enabled("sleep")) Text(tr("En el reproductor: Funciones locales → 30/60/90 minutos o 1/2/3 episodios. Pausa al vencer; no apaga físicamente la TV.","In the player: Local features → 30/60/90 minutes or 1/2/3 episodes. Pauses playback; does not power off the TV."),color=Muted)
        if(extras.enabled("lists")) FocusButton(tr("Administrar mis listas","Manage my lists")) {showLists=true}
        if(extras.enabled("hide")) FocusButton(tr("Restaurar títulos ocultos","Restore hidden titles")) {extras.restoreHidden(owner);status=tr("Títulos restaurados.","Titles restored.")}
        if(extras.enabled("tracks")) FocusButton(tr("Borrar preferencias de series","Clear series preferences")) {extras.clearTracks(owner);status=tr("Preferencias borradas.","Preferences cleared.")}
        if(extras.enabled("backup")) {
            Text(tr("Solo datos locales y preferencias; sin contraseñas, sesiones ni enlaces de reproducción. Restaurar requiere el mismo perfil. Usa el selector de archivos de Android.","Local data and preferences only; no passwords, sessions or playback links. Restore requires the same profile. Uses Android's file picker."),color=Muted)
            if(!working) Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                FocusButton(tr("Exportar JSON","Export JSON")) {runCatching {export.launch("BruniO-local-backup.json")}.onFailure {status=tr("Esta TV no tiene selector de archivos compatible.","This TV has no compatible file picker.")}}
                FocusButton(tr("Importar JSON","Import JSON")) {runCatching {restore.launch(arrayOf("application/json","text/plain","application/octet-stream"))}.onFailure {status=tr("Esta TV no tiene selector de archivos compatible.","This TV has no compatible file picker.")}}
            }
        }
        if(status.isNotBlank()) Text(status,color=Color.White)
    }
}

@Composable
internal fun LocalListsRC100(state:AppState,adding:MediaSummary?,close:()->Unit) {
    val extras=state.localExtras;val owner=state.localExtrasScope()
    var selected by remember(owner) {mutableStateOf<String?>(null)}
    var name by remember(owner) {mutableStateOf("")}
    var message by remember(owner) {mutableStateOf("")}
    var deleting by remember(owner) {mutableStateOf(false)}
    val rows=extras.lists(owner)
    var shown by remember(selected) {mutableIntStateOf(25)}
    val current=rows.firstOrNull {it.optString("id")==selected}
    LocalPanelRC100(if(adding==null) tr("Mis listas locales","My local lists") else tr("Añadir a lista local","Add to local list"),close) {
        NativeTextField(name,{name=it.take(60)},tr("Nombre de la lista","List name"),modifier=Modifier.fillMaxWidth())
        if(name.isNotBlank()) FocusButton(tr("Crear lista","Create list")) {runCatching {val id=extras.createList(owner,name);if(adding!=null) {extras.add(owner,id,adding);message=tr("Añadido.","Added.")} else selected=id;name=""}.onFailure {message=it.message.orEmpty()}}
        if(message.isNotBlank()) Text(message,color=Muted)
        if(current==null) {
            if(rows.isEmpty()) Text(tr("Crea tu primera lista.","Create your first list."),color=Muted)
            rows.forEach {row ->FocusButton(row.getString("name")+" · "+extras.listItems(row).size) {
                if(adding!=null) {runCatching {extras.add(owner,row.getString("id"),adding)}.onSuccess {message=tr("Añadido a ","Added to ")+row.getString("name")}.onFailure {message=it.message.orEmpty()}}
                else {selected=row.getString("id");name=row.getString("name")}
            }}
        } else {
            Text(current.getString("name"),color=Color.White,fontSize=20.sp)
            FocusButton(tr("Volver a mis listas","Back to my lists")) {selected=null;name="";deleting=false}
            if(name.isNotBlank()) FocusButton(tr("Renombrar","Rename")) {extras.renameList(owner,current.getString("id"),name)}
            FocusButton(tr("Eliminar lista","Delete list")) {deleting=true}
            if(deleting) FocusButton(tr("Confirmar eliminación","Confirm deletion")) {extras.deleteList(owner,current.getString("id"));selected=null;name="";deleting=false}
            extras.listItems(current).take(shown).forEach {item ->
                Text(item.title,color=Color.White)
                Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                    FocusButton(tr("Ver título","View title")) {close();state.launch {state.open(item)}}
                    FocusButton(tr("Quitar","Remove")) {extras.remove(owner,current.getString("id"),item.cloudId)}
                }
            }
            if(extras.listItems(current).size>shown) FocusButton(tr("Cargar 25 más","Load 25 more")) {shown+=25}
        }
    }
}

@Composable
internal fun LocalNotTodayRC100(state:AppState,item:MediaSummary,close:()->Unit) {
    LocalPanelRC100(tr("Hoy no quiero ver esto","Not today"),close) {
        Text(tr("Ocultar de las recomendaciones de Home. Puedes encontrarlo en búsqueda o tus listas.","Hide from Home recommendations. You can still find it in search or your lists."),color=Muted)
        listOf(1,7,30).forEach {days ->FocusButton("$days "+tr("día(s)","day(s)")) {state.localExtras.hide(state.localExtrasScope(),item.cloudId,days);close()}}
    }
}

internal fun localCatalogRC100(state:AppState,key:String,rows:List<MediaSummary>):List<MediaSummary> {
    val extras=state.localExtras
    val f=if(extras.enabled("filters")) extras.preference(state.localExtrasScope(),"filter:$key") else JSONObject()
    val filtered=rows.filter {item ->
        (f.optString("type").isBlank() || item.type==f.optString("type")) &&
        (f.optString("year").isBlank() || item.year==f.optString("year")) &&
        (!f.optBoolean("unwatched",false) || (state.progress[item.cloudId]?.percent ?: 0.0)<90.0)
    }
    return when(f.optString("sort")) {"rating" -> filtered.sortedByDescending {it.rating};"year" -> filtered.sortedByDescending {it.year};"title" -> filtered.sortedBy {it.title.lowercase()};else -> filtered}
}

@Composable
internal fun LocalCatalogFiltersRC100(state:AppState,key:String,rows:List<MediaSummary>,close:()->Unit) {
    val extras=state.localExtras;val owner=state.localExtrasScope();val f=extras.preference(owner,"filter:$key")
    fun save(field:String,value:Any) {extras.savePreference(owner,"filter:$key",JSONObject(f.toString()).put(field,value))}
    LocalPanelRC100(tr("Filtros locales","Local filters"),close) {
        Text(tr("Aplican a los títulos cargados de este catálogo. Se recuerdan por perfil y catálogo; no realizan búsquedas nuevas.","Apply to loaded titles in this catalog. Remembered per profile and catalog; no new searches."),color=Muted)
        FocusButton(tr("Restablecer","Reset")) {extras.savePreference(owner,"filter:$key",JSONObject())}
        listOf("" to tr("Todos los tipos","All types"),"movie" to tr("Películas","Movies"),"series" to tr("Series","Series")).forEach {(v,label) ->FocusButton(label,primary=f.optString("type")==v) {save("type",v)}}
        FocusButton(tr("Ocultar vistos","Hide watched")+" · "+if(f.optBoolean("unwatched")) "ON" else "OFF") {save("unwatched",!f.optBoolean("unwatched"))}
        listOf("" to tr("Orden original","Original order"),"rating" to tr("Valoración","Rating"),"year" to tr("Más recientes","Newest year"),"title" to tr("Título A–Z","Title A–Z")).forEach {(v,label) ->FocusButton(label,primary=f.optString("sort")==v) {save("sort",v)}}
        FocusButton(tr("Todos los años","All years"),primary=f.optString("year").isBlank()) {save("year","")}
        rows.map {it.year}.filter {it.isNotBlank()}.distinct().sortedDescending().forEach {year ->FocusButton(year,primary=f.optString("year")==year) {save("year",year)}}
    }
}

@Composable
internal fun LocalPlayerToolsRC100(state:AppState,request:PlayerRequest,player:Player,visible:Boolean,onPause:()->Unit) {
    val extras=state.localExtras
    var show by remember(request.playbackId) {mutableStateOf(false)}
    var tech by remember(request.playbackId) {mutableStateOf(false)}
    var clock by remember {mutableStateOf(android.os.SystemClock.elapsedRealtime())}
    LaunchedEffect(request.playbackId) {extras.playbackStarted(state.localExtrasScope())}
    LaunchedEffect(player,extras.enabled("sleep"),extras.timerDeadline) {
        while(extras.enabled("sleep") && extras.timerDeadline>0) {delay(1000);clock=android.os.SystemClock.elapsedRealtime();if(extras.tick()) onPause()}
    }
    if(visible && (extras.enabled("sleep") || extras.enabled("technical"))) Box(Modifier.fillMaxSize(),contentAlignment=Alignment.TopEnd) {
        FocusButton(tr("Funciones locales","Local features"),modifier=Modifier.padding(top=22.dp,end=30.dp)) {show=true}
    }
    if(show) LocalPanelRC100(tr("Funciones locales","Local features"),{show=false}) {
        if(extras.enabled("sleep")) {
            if(extras.timerDeadline>0) Text(tr("Restante: ","Remaining: ")+((extras.timerDeadline-clock).coerceAtLeast(0)/60_000+1)+tr(" min"," min"),color=Muted)
            if(extras.timerEpisodes>0) Text(tr("Episodios restantes: ","Episodes remaining: ")+extras.timerEpisodes,color=Muted)
            listOf(30,60,90).forEach {n ->FocusButton("$n min") {extras.startMinutes(n);show=false}}
            if(!request.live && request.momentEndMs<=0) listOf(1,2,3).forEach {n ->FocusButton(tr("Al terminar ","After ")+n+tr(" episodio(s)/título(s)"," episode(s)/title(s)")) {extras.startEpisodes(n);show=false}}
            FocusButton(tr("Cancelar temporizador","Cancel timer")) {extras.clearTimer();show=false}
        }
        if(extras.enabled("technical")) FocusButton(tr("Panel técnico","Technical panel")) {tech=true;show=false}
    }
    if(tech && extras.enabled("technical")) {
        var report by remember {mutableStateOf("")}
        LaunchedEffect(player) {while(true) {
            report=buildString {
                appendLine("Buffer: ${player.totalBufferedDuration.coerceAtLeast(0)/1000}s")
                appendLine("${tr("Estado","State")}: ${player.playbackState} · ${tr("Velocidad","Speed")}: ${player.playbackParameters.speed}×")
                for(g in player.currentTracks.groups) for(i in 0 until g.length) if(g.isTrackSelected(i)) {
                    val f=g.getTrackFormat(i)
                    appendLine(listOfNotNull(f.sampleMimeType,f.codecs,f.language,f.label).joinToString(" · "))
                    if(g.type==C.TRACK_TYPE_VIDEO) appendLine("${f.width} × ${f.height} · ${f.frameRate} fps")
                    if(g.type==C.TRACK_TYPE_AUDIO) appendLine("${f.channelCount} ch · ${f.sampleRate} Hz")
                    if(f.bitrate>0) appendLine("${f.bitrate/1000} kbps")
                }
            };delay(1000)
        }}
        LocalPanelRC100(tr("Panel técnico","Technical panel"),{tech=false}) {Text(report,color=Color.White);Text(tr("Solo datos del reproductor. Valores desconocidos pueden aparecer como −1. No ejecuta tests de velocidad.","Player data only. Unknown values may appear as −1. Does not run speed tests."),color=Muted)}
    }
    if(extras.stillPrompt && state.watchParty==null) {
        LaunchedEffect(Unit) {delay(60_000);extras.stopSafety();onPause()}
        LocalPanelRC100(tr("¿Sigues viendo?","Are you still watching?"),{extras.stopSafety();onPause()}) {
            Text(tr("La reproducción automática espera tu respuesta. Sin respuesta se detiene.","Autoplay is waiting for your answer. Without a response, playback stops."),color=Muted)
            FocusButton(tr("Sí, continuar","Yes, continue")) {extras.acknowledge()}
            FocusButton(tr("Detener","Stop")) {extras.stopSafety();onPause()}
        }
    }
}
