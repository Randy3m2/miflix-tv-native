package com.miflix.native2.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
    SettingsDialogRC103(tr("Haz BruniO tuyo","Make BruniO yours"),tr("Ajusta tu reproducción, tu catálogo y tus preferencias en esta TV.","Shape playback, your catalog and your preferences on this TV."),close) {
        Text(tr("Los interruptores aplican a esta TV. Tus listas, títulos ocultos y preferencias se guardan por perfil.","Switches apply to this TV. Lists, hidden titles and preferences are saved per profile."),color=Muted,fontSize=13.sp)
        SettingsHeadingRC103(tr("A tu ritmo","At your pace"),tr("Más comodidad en cada sesión.","More comfort in every session."))
        SettingsTileRC103(tr("Temporizador de reproducción","Sleep timer"),tr("Elige cuándo pausar: por minutos o al terminar episodios.","Choose when to pause: after minutes or completed episodes."),"sleep",checked=extras.enabled("sleep")) {extras.enable("sleep",!extras.enabled("sleep"))}
        if(extras.enabled("sleep")) Text(tr("En el reproductor: Funciones locales → 30/60/90 minutos o 1/2/3 episodios. Pausa al vencer; no apaga físicamente la TV.","In the player: Local features → 30/60/90 minutes or 1/2/3 episodes. Pauses playback; does not power off the TV."),color=Muted,fontSize=13.sp)
        SettingsTileRC103(tr("¿Sigues viendo?","Are you still watching?"),tr("Pregunta antes de seguir tras varios episodios sin usar el mando.","Ask before continuing after several episodes without remote interaction."),"still",checked=extras.enabled("still")) {extras.enable("still",!extras.enabled("still"))}
        if(extras.enabled("still")) {
            Text(tr("Preguntar después de:","Ask after:"),color=Muted,fontSize=13.sp)
            Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {listOf(2,3,5).forEach {n -> FocusButton("$n "+tr("episodios","episodes"),primary=extras.stillLimit==n,modifier=Modifier.settingsHelpRC103(tr("¿Sigues viendo?","Are you still watching?"),tr("Solicita confirmación tras este número de episodios consecutivos sin interacción con el mando.","Request confirmation after this many consecutive episodes without remote interaction."),"still","$n "+tr("episodios","episodes"))) {extras.saveStillLimit(n)}}}
        }
        SettingsTileRC103(tr("Recordar audio y subtítulos","Remember audio and subtitles"),tr("Recupera las pistas que elegiste para cada serie de este perfil.","Recall the tracks you chose for each series in this profile."),"tracks",checked=extras.enabled("tracks")) {extras.enable("tracks",!extras.enabled("tracks"))}
        if(extras.enabled("tracks")) SettingsTileRC103(tr("Borrar preferencias de series","Clear series preferences"),tr("Vuelve a usar tus idiomas predeterminados.","Return to your default language choices."),"tracks") {extras.clearTracks(owner);status=tr("Preferencias borradas.","Preferences cleared.")}
        SettingsHeadingRC103(tr("Un catálogo a tu manera","A catalog your way"),tr("Organiza lo que quieres encontrar.","Organize what you want to find."))
        SettingsTileRC103(tr("Listas personales locales","Local personal lists"),tr("Agrupa títulos en tus propias listas para este perfil.","Group titles in your own lists for this profile."),"lists",checked=extras.enabled("lists")) {extras.enable("lists",!extras.enabled("lists"))}
        if(extras.enabled("lists")) SettingsTileRC103(tr("Administrar mis listas","Manage my lists"),tr("Crea, renombra y organiza tus colecciones.","Create, rename and organize your collections."),"lists") {showLists=true}
        SettingsTileRC103(tr("Hoy no quiero ver esto","Not today"),tr("Oculta temporalmente títulos de las recomendaciones de Home.","Temporarily hide titles from Home recommendations."),"hide",checked=extras.enabled("hide")) {extras.enable("hide",!extras.enabled("hide"))}
        if(extras.enabled("hide")) SettingsTileRC103(tr("Restaurar títulos ocultos","Restore hidden titles"),tr("Vuelve a incluirlos en las recomendaciones de este perfil.","Include them again in this profile's recommendations."),"hide") {extras.restoreHidden(owner);status=tr("Títulos restaurados.","Titles restored.")}
        SettingsTileRC103(tr("Recordar filtros del catálogo","Remember catalog filters"),tr("Conserva tus filtros para la próxima visita.","Keep your filters for your next visit."),"filters",checked=extras.enabled("filters")) {extras.enable("filters",!extras.enabled("filters"));state.restoreCustomFilterRC100()}
        SettingsHeadingRC103(tr("Herramientas y respaldo","Tools and backup"),tr("Información útil y tus preferencias a mano.","Useful information and your preferences at hand."))
        SettingsTileRC103(tr("Panel técnico del reproductor","Player technical panel"),tr("Consulta calidad, audio y búfer durante la reproducción.","See quality, audio and buffer information during playback."),"technical",checked=extras.enabled("technical")) {extras.enable("technical",!extras.enabled("technical"))}
        SettingsTileRC103(tr("Backup local de configuración","Local settings backup"),tr("Exporta o restaura los datos locales de este perfil.","Export or restore this profile's local data."),"backup",checked=extras.enabled("backup")) {extras.enable("backup",!extras.enabled("backup"))}
        if(extras.enabled("backup")) {
            Text(tr("Solo datos locales y preferencias; sin contraseñas, sesiones ni enlaces de reproducción. Restaurar requiere el mismo perfil. Usa el selector de archivos de Android.","Local data and preferences only; no passwords, sessions or playback links. Restore requires the same profile. Uses Android's file picker."),color=Muted,fontSize=13.sp)
            if(!working) {
                SettingsTileRC103(tr("Exportar JSON","Export JSON"),tr("Guarda una copia de tus preferencias.","Save a copy of your preferences."),"backup") {runCatching {export.launch("BruniO-local-backup.json")}.onFailure {status=tr("Esta TV no tiene selector de archivos compatible.","This TV has no compatible file picker.")}}
                SettingsTileRC103(tr("Importar JSON","Import JSON"),tr("Elige un respaldo para revisar y restaurar.","Choose a backup to review and restore."),"backup") {runCatching {restore.launch(arrayOf("application/json","text/plain","application/octet-stream"))}.onFailure {status=tr("Esta TV no tiene selector de archivos compatible.","This TV has no compatible file picker.")}}
            } else Text(tr("Procesando respaldo…","Processing backup…"),color=Color.White,fontSize=14.sp)
        }
        if(status.isNotBlank()) Text(status,color=Color.White)
    }
}

@Composable
internal fun LocalListsRC100(state:AppState,adding:MediaSummary?,initialListId:String?=null,close:()->Unit) {
    val extras=state.localExtras;val owner=state.localExtrasScope()
    var selected by remember(owner,initialListId) {mutableStateOf(initialListId)}
    var name by remember(owner,initialListId) {mutableStateOf(extras.lists(owner).firstOrNull {it.optString("id")==initialListId}?.optString("name").orEmpty())}
    var message by remember(owner) {mutableStateOf("")}
    var deleting by remember(owner) {mutableStateOf(false)}
    val rows=extras.lists(owner)
    var shown by remember(selected) {mutableIntStateOf(25)}
    val current=rows.firstOrNull {it.optString("id")==selected}
    val currentItems=remember(owner,selected,extras.revision) {current?.let {extras.listItems(it)} ?: emptyList()}
    LocalListsPanelRC104(if(adding!=null) tr("Añadir a una colección","Add to a collection") else current?.optString("name") ?: tr("Tus listas, tus historias","Your lists, your stories"),close) {
        if(message.isNotBlank()) Text(message,color=Color.White,fontSize=14.sp)
        if(current==null) {
            SettingsHeadingRC103(tr("Dale un nombre a tu colección","Give your collection a name"),if(adding==null) tr("Agrupa películas y series para cada ocasión.","Group movies and series for every occasion.") else tr("Crea una lista o elige una para guardar este título.","Create a list or choose one to save this title."))
            NativeTextField(name,{name=it.take(60)},tr("Ej.: Noches de suspenso","E.g. Thriller nights"),modifier=Modifier.fillMaxWidth())
            if(name.isNotBlank()) SettingsTileRC103(tr("Crear colección","Create collection"),if(adding==null) tr("Después añade títulos desde sus fichas.","Then add titles from their details.") else tr("Crea la lista y guarda este título en ella.","Create the list and save this title in it."),"lists") {
                runCatching {
                    val title=name.trim();val id=extras.createList(owner,title)
                    if(adding!=null) {extras.add(owner,id,adding);message=tr("Añadido a ","Added to ")+title;name=""}
                    else {selected=id;name=title;message=""}
                }.onFailure {message=it.message.orEmpty()}
            }
            if(rows.isEmpty()) Text(tr("Tu primera colección comienza con un nombre. Agrega películas o series desde su ficha → Añadir a lista local.","Your first collection starts with a name. Add movies or series from their details → Add to local list."),color=Muted,fontSize=14.sp)
            else {
                SettingsHeadingRC103(tr("Tus colecciones","Your collections"),if(adding==null) tr("Abre una tarjeta para ver y organizar sus títulos.","Open a card to see and organize its titles.") else tr("Pulsa una colección para añadir el título.","Select a collection to add the title."))
                val previews=remember(owner,extras.revision) {rows.map(::localListPreviewRC104)}
                LazyRow(Modifier.fillMaxWidth().focusGroup(),horizontalArrangement=Arrangement.spacedBy(16.dp),contentPadding=PaddingValues(8.dp)) {
                    items(previews,key={it.id}) {preview ->LocalListCardRC104(preview,Modifier.width(304.dp)) {
                        if(adding!=null) runCatching {extras.add(owner,preview.id,adding)}.onSuccess {message=tr("Añadido a ","Added to ")+preview.name}.onFailure {message=it.message.orEmpty()}
                        else {selected=preview.id;name=preview.name;message="";deleting=false}
                    }}
                }
            }
        } else {
            val preview=remember(owner,selected,extras.revision) {localListPreviewRC104(current)}
            LocalListCardRC104(preview,Modifier.fillMaxWidth())
            FocusButton(tr("Volver a mis colecciones","Back to my collections")) {selected=null;name="";deleting=false;message=""}
            NativeTextField(name,{name=it.take(60)},tr("Nombre de la colección","Collection name"),modifier=Modifier.fillMaxWidth())
            if(name.isNotBlank() && name.trim()!=current.optString("name")) FocusButton(tr("Guardar nombre","Save name")) {runCatching {extras.renameList(owner,current.getString("id"),name)}.onFailure {message=it.message.orEmpty()}}
            FocusButton(tr("Eliminar colección","Delete collection"),destructive=true) {deleting=true}
            if(deleting) {
                Text(tr("Se elimina esta lista; los títulos siguen disponibles en BruniO.","This list will be deleted; its titles remain available in BruniO."),color=Muted,fontSize=13.sp)
                Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                    FocusButton(tr("Confirmar eliminación","Confirm deletion"),destructive=true) {extras.deleteList(owner,current.getString("id"));selected=null;name="";deleting=false;message=""}
                    FocusButton(tr("Cancelar","Cancel")) {deleting=false}
                }
            }
            if(currentItems.isEmpty()) Text(tr("Lista vacía. Abre una película o serie y elige Añadir a lista local para comenzar.","Empty list. Open a movie or series and choose Add to local list to begin."),color=Muted,fontSize=14.sp)
            currentItems.take(shown).forEach {item ->
                key(item.cloudId) {LocalListTitleRC104(item,onOpen={close();state.launch {state.open(item)}},onRemove={extras.remove(owner,current.getString("id"),item.cloudId)})}
            }
            if(currentItems.size>shown) FocusButton(tr("Cargar 25 más","Load 25 more")) {shown+=25}
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
    LocalCatalogFiltersRP24(state,key,rows,close)
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
