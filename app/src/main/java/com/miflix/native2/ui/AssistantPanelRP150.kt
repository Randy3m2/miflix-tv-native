package com.miflix.native2.ui

import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.miflix.native2.data.*
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean

internal val LocalAssistantOpenRP150=staticCompositionLocalOf<(() -> Unit)?> {null}
internal val LocalGuestExitRP150=staticCompositionLocalOf<(() -> Unit)?> {null}

@Composable
internal fun AssistantPanelRP150(state:AppState,close:()->Unit,guest:()->Unit) {
    val context=LocalContext.current.applicationContext
    var scenePreviewsRP151 by remember {mutableStateOf(scenePreviewEnabledRP151(context))}
    val ai=remember(context) {GeminiAssistantRP150(context)}
    val scope=rememberCoroutineScope()
    val collectionPrefs=remember(context) {context.getSharedPreferences("brunio_smart_collections_rp150",android.content.Context.MODE_PRIVATE)}
    val owner=state.localExtrasScope()
    var collectionRevision by remember {mutableIntStateOf(0)}
    val saved=remember(owner,collectionRevision) {runCatching {val a=org.json.JSONArray(collectionPrefs.getString(owner,"[]"));(0 until a.length()).mapNotNull {a.optJSONObject(it)}}.getOrDefault(emptyList())}
    var query by remember(owner) {mutableStateOf("")}
    var budget by remember {mutableIntStateOf(0)}
    var useGemini by remember {mutableStateOf(false)}
    var sceneFinder by remember {mutableStateOf(false)}
    var setup by remember {mutableStateOf(false)}
    var phone by remember {mutableStateOf(false)}
    var configured by remember {mutableStateOf(ai.configured())}
    var busy by remember {mutableStateOf(false)}
    var message by remember {mutableStateOf("")}
    var picks by remember(owner) {mutableStateOf<List<AssistantPickRP150>>(emptyList())}
    var resultQuery by remember(owner) {mutableStateOf("")}
    var resultBudget by remember {mutableIntStateOf(0)}
    var activePlan by remember(owner) {mutableStateOf<AssistantPlanRP150?>(null)}
    var desiredAudio by remember {mutableStateOf("")}
    var requestGeneration by remember(owner) {mutableLongStateOf(0L)}
    var job by remember {mutableStateOf<Job?>(null)}
    val plans=remember(owner) {linkedMapOf<String,AssistantPlanRP150>()}
    DisposableEffect(owner) {onDispose {job?.cancel()}}
    fun submit(text:String=query,time:Int=budget,preset:AssistantPlanRP150?=null) {
        if(busy) return
        query=text;budget=time;resultQuery=text;resultBudget=time;activePlan=null;message="";picks=emptyList();desiredAudio="";busy=true
        val generation=++requestGeneration
        val remote=preset==null && useGemini && configured
        job=scope.launch {
            try {
                if(Regex("(?i)(en qu[eé] episodio|which episode|scene finder|escena|timestamp)").containsMatchIn(text)) {sceneFinder=true;return@launch}
                val key="$remote:$text:$time:$preset"
                var cachePlan=true
                val plan=preset ?: plans[key] ?: if(remote) try {
                    withContext(Dispatchers.IO) {ai.plan(text,time)}
                } catch(e:CancellationException) {throw e} catch(_:Exception) {cachePlan=false;if(generation==requestGeneration) message=tr("Gemini no respondió o alcanzó su límite. Usando filtros locales.","Gemini unavailable or limit reached. Using local filters.");AssistantPlanRP150.local(text,time)} else AssistantPlanRP150.local(text,time)
                if(cachePlan) {if(plans.size>=12) plans.remove(plans.keys.first());plans[key]=plan}
                val rows=state.assistantRP150.picks(owner,plan,state.continueWatching.toList(),state.progress.toMap())
                currentCoroutineContext().ensureActive()
                if(state.localExtrasScope()!=owner || generation!=requestGeneration) return@launch
                picks=if(state.kidsMode) {val allowed=state.tmdb.kidsFilter(rows.map {it.item}).map {it.cloudId}.toSet();rows.filter {it.item.cloudId in allowed}} else rows
                activePlan=plan
                desiredAudio=plan.audio
                if(picks.isEmpty()) message=tr("No encontramos coincidencias verificadas. Prueba otros filtros.","No verified matches found. Try different filters.")
            } catch(e:CancellationException) {throw e} catch(_:Exception) {if(generation==requestGeneration) message=tr("No se pudo completar la selección. Puedes reintentar.","Could not complete this selection. You can retry.")}
            finally {if(generation==requestGeneration) busy=false}
        }
    }
    if(sceneFinder) {SceneFinderRP151(state) {sceneFinder=false};return}
    if(phone) AssistantTextQrRP150(state,{if(!busy) query=it},{phone=false})
    if(setup) GeminiSetupRP150(state,ai,{configured=ai.configured();setup=false})
    PlayerMenuDialog(onDismissRequest=close,fullScreen=true) {
        Column(Modifier.fillMaxSize().consumeOpeningPress().background(Bg).padding(horizontal=34.dp,vertical=20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {Text("✦ BruniO AI",color=Color.White,fontSize=30.sp);Text(tr("Tu próxima historia, a tu manera","Your next story, your way"),color=Muted,fontSize=14.sp)}
                if(!state.kidsMode && LocalGuestExitRP150.current==null) FocusButton(tr("Modo invitado","Guest mode")) {if(state.prepareGuestRP150()) guest() else message=tr("Cierra la reproducción o sal del Watch Party antes de entrar como invitado.","Close playback or leave the Watch Party before entering guest mode.")}
                FocusButton("Scene Finder") {sceneFinder=true}
                FocusButton("×",onClick=close)
            }
            LazyColumn(Modifier.weight(1f).fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(bottom=24.dp)) {
                item(key="assistant-static-rp150-0") {
                    BasicTextField(query,{if(!busy) query=it.take(500)},textStyle=androidx.compose.ui.text.TextStyle(color=Color.White,fontSize=19.sp),cursorBrush=SolidColor(Accent),modifier=Modifier.fillMaxWidth().background(Panel,RoundedCornerShape(16.dp)).padding(18.dp),decorationBox={inner ->if(query.isBlank()) Text(tr("¿Qué te apetece ver?","What would you like to watch?"),color=Muted,fontSize=19.sp);inner()})
                    Text(tr("Puedes usar el teclado del sistema o un teclado Bluetooth. La IA solo recibe tu consulta al enviar.","Use the system keyboard or Bluetooth. AI receives only your submitted request."),color=Muted,fontSize=11.sp,modifier=Modifier.padding(top=6.dp))
                }
                item(key="preview-rp151") {
                    FocusButton(tr("Miniaturas al buscar: ","Seek thumbnails: ")+if(scenePreviewsRP151) "ON" else "OFF") {scenePreviewsRP151=!scenePreviewsRP151;context.getSharedPreferences("brunio_scene_preview_rp151",0).edit().putBoolean("enabled",scenePreviewsRP151).apply()}
                    Text(tr("Opcional: extrae una imagen al detenerte sobre la barra. Depende de la fuente; puede usar datos de vídeo. OK confirma y Atrás cancela.","Optional: extracts a frame when you stop on the seek bar. Source dependent; may use video data. OK confirms and Back cancels."),color=Muted,fontSize=11.sp)
                }
                item(key="assistant-static-rp150-1") {LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {items(listOf(0,15,30,45,60,90,120)) {time ->FocusButton(if(time==0) tr("Según consulta","From request") else "$time min",primary=budget==time) {if(!busy) budget=time}}}}
                item(key="assistant-static-rp150-2") {LazyRow(horizontalArrangement=Arrangement.spacedBy(10.dp)) {items(listOf(tr("Terror de los 90","90s horror"),tr("Películas de viajes en el tiempo","Time travel movies"),tr("Mejores finales inesperados","Thrillers with plot twists"),tr("Ciencia ficción con acción menos de 2 horas en español","Sci-fi action under 2 hours in Spanish"))) {example ->FocusButton(example) {submit(example)}}}}
                if(saved.isNotEmpty()) item(key="assistant-static-rp150-3") {Text(tr("Tus colecciones inteligentes","Your smart collections"),color=Color.White,fontSize=18.sp);LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {items(saved) {row ->FocusButton(row.optString("title")) {submit(row.optString("query"),row.optInt("budget"),runCatching {AssistantPlanRP150.json(row.getJSONObject("plan").toString())}.getOrNull())}}}}
                if(picks.isNotEmpty() && resultQuery.isNotBlank()) item(key="assistant-static-rp150-4") {Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                    FocusButton(tr("Guardar colección","Save collection")) {
                        val values=(listOf(org.json.JSONObject().put("title",resultQuery.take(65)).put("query",resultQuery).put("budget",resultBudget).put("plan",activePlan?.json()))+saved.filterNot {it.optString("query")==resultQuery}).take(10)
                        collectionPrefs.edit().putString(owner,org.json.JSONArray(values).toString()).apply();collectionRevision++
                    }
                    if(saved.isNotEmpty()) FocusButton(tr("Borrar colecciones guardadas","Clear saved collections")) {collectionPrefs.edit().remove(owner).apply();collectionRevision++}
                }}
                item(key="assistant-static-rp150-5") {Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                    FocusButton(if(busy) tr("Buscando…","Searching…") else tr("Buscar para mí","Find something for me")) {submit()}
                    FocusButton(tr("Escribir por QR","Type with phone QR")) {if(!busy) phone=true}
                    if(busy) FocusButton(tr("Cancelar","Cancel")) {requestGeneration++;job?.cancel();busy=false}
                    if(!state.kidsMode) {FocusButton(if(useGemini) "Gemini · ON" else tr("Filtros locales","Local filters"),primary=useGemini) {if(!busy) {if(configured) useGemini=!useGemini else setup=true}};FocusButton(tr("Configurar Gemini","Set up Gemini")) {setup=true}}
                }}
                if(message.isNotBlank()) item(key="assistant-static-rp150-6") {Text(message,color=Muted,fontSize=14.sp)}
                if(desiredAudio.isNotBlank()) item(key="assistant-static-rp150-7") {Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {Text(tr("El doblaje se confirma en las fuentes disponibles, no por la sinopsis.","Dubbed audio is confirmed by available sources, not by the synopsis."),color=Muted,fontSize=13.sp);FocusButton(tr("Preferir audio: ","Prefer audio: ")+AppLanguagesRP148.label(desiredAudio)) {state.savePlaybackPreferences(desiredAudio,state.subtitleLanguage,state.autoplayNext);message=tr("Preferencia de audio guardada.","Audio preference saved.")}}}
                if(picks.isNotEmpty()) item(key="assistant-static-rp150-8") {Text(tr("Resultados: ","Results: ")+resultQuery.ifBlank {"${resultBudget} min"},color=Color.White,fontSize=17.sp)}
                items(picks,key={"${it.item.cloudId}:${it.episode?.season}:${it.episode?.episode}"}) {pick ->
                    Row(Modifier.fillMaxWidth().background(Panel,RoundedCornerShape(18.dp)).padding(12.dp),horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                        AsyncImage(pick.item.backdrop ?: pick.item.poster,pick.item.title,Modifier.width(156.dp).height(88.dp),contentScale=ContentScale.Crop)
                        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                            Text(pick.item.title,color=Color.White,fontSize=19.sp,maxLines=2)
                            Text("${pick.minutes} min"+(pick.episode?.let {" · S${it.season} E${it.episode}"} ?: "")+if(pick.resume) tr(" · para terminar"," · remaining") else "",color=Accent,fontSize=13.sp)
                            Text(pick.item.overview,color=Muted,fontSize=12.sp,maxLines=2)
                        }
                        FocusButton(tr("Detalles","Details")) {close();state.launch {state.open(pick.item)}}
                        if(pick.resume && pick.episode==null) FocusButton(tr("Continuar","Continue")) {close();state.launch {state.open(pick.item);state.playMovie()}}
                        pick.episode?.let {ep ->FocusButton(if(pick.resume) tr("Continuar","Continue") else tr("Reproducir","Play")) {close();state.launch {state.open(pick.item);state.playEpisode(ep)}}}
                    }
                }
            }
        }
    }
}

@Composable
private fun GeminiSetupRP150(state:AppState,ai:GeminiAssistantRP150,close:()->Unit) {
    var qr by remember {mutableStateOf("")};var status by remember {mutableStateOf("")};val context=LocalContext.current.applicationContext
    val main=remember {Handler(Looper.getMainLooper())}
    val alive=remember(state.appForeground) {AtomicBoolean(true)}
    val server=remember {java.util.concurrent.atomic.AtomicReference<SearchKeyboardServerRP136?>(null)}
    DisposableEffect(alive) {onDispose {alive.set(false);server.getAndSet(null)?.close()}}
    LaunchedEffect(alive) {
        qr=""
        if(!state.appForeground) return@LaunchedEffect
        try {val created=withContext(Dispatchers.IO) {
            val endpoint=localQrEndpointsRP145(context).firstOrNull() ?: error("LAN unavailable")
            SearchKeyboardServerRP136(endpoint.address,endpoint.prefixLength,{key ->
                if(!alive.get()) false else try {synchronized(ai) {if(!alive.get()) return@SearchKeyboardServerRP136 false;ai.save(key.trim())};main.post {if(alive.get()) status=tr("Clave guardada en esta TV. Ya puedes cerrar.","Key saved on this TV. You can close this panel.")};true} catch(_:Exception) {false}
            },true).also {server.set(it);if(!alive.get()) it.close()}
        };qr=created.url;delay(900000);created.close();qr="";status=tr("QR vencido.","QR expired.")}
        catch(e:CancellationException) {throw e} catch(_:Exception) {status=tr("No se pudo abrir el QR local. Revisa la red.","Could not open local QR. Check your network.")}
    }
    PlayerMenuDialog(onDismissRequest=close,fullScreen=true) {
        LazyColumn(Modifier.fillMaxSize().consumeOpeningPress().background(Bg).padding(28.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
            item(key="assistant-static-rp150-9") {Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)) {Text("Gemini · Personal API key",color=Color.White,fontSize=26.sp,modifier=Modifier.weight(1f));FocusButton("×",onClick=close)}}
            item(key="assistant-static-rp150-10") {Text(tr("Opcional. Escanea el QR y pega tu propia clave de Google AI Studio. Usa un proyecto Free Tier sin facturación: BruniO no puede convertir una clave de pago en gratuita. La clave se cifra en este dispositivo y no se sincroniza.","Optional. Scan and paste your own Google AI Studio key. Use a Free Tier project without billing: BruniO cannot make a paid key free. The key is encrypted on this device and never synced."),color=Muted,fontSize=15.sp)}
            item(key="assistant-static-rp150-11") {Text(tr("Solo se envía la consulta que escribas. Google puede usar solicitudes del nivel gratuito para mejorar sus productos. Límite local: 20 consultas al día, una cada 20 segundos; las cuotas de Google son independientes.","Only your submitted request is sent. Google may use free-tier requests to improve products. Local cap: 20 requests/day, one per 20 seconds; Google quotas are separate."),color=Muted,fontSize=13.sp)}
            if(qr.isNotBlank()) item(key="assistant-static-rp150-12") {QrCode(qr,190.dp)}
            if(status.isNotBlank()) item(key="assistant-static-rp150-13") {Text(status,color=Color.White)}
            item(key="assistant-static-rp150-14") {FocusButton(tr("Eliminar clave","Remove key")) {alive.set(false);server.getAndSet(null)?.close();qr="";synchronized(ai) {ai.clear()};status=tr("Clave eliminada.","Key removed.")};FocusButton(tr("Cerrar","Close"),onClick=close)}
        }
    }
}

@Composable
private fun AssistantTextQrRP150(state:AppState,received:(String)->Unit,close:()->Unit) {
    val context=LocalContext.current.applicationContext
    val callback by rememberUpdatedState(received)
    val owner=state.localExtrasScope()
    val alive=remember(owner,state.appForeground) {AtomicBoolean(true)}
    val server=remember(alive) {java.util.concurrent.atomic.AtomicReference<SearchKeyboardServerRP136?>(null)}
    val main=remember {Handler(Looper.getMainLooper())}
    var url by remember {mutableStateOf("")};var status by remember {mutableStateOf("")}
    DisposableEffect(alive) {onDispose {alive.set(false);server.getAndSet(null)?.close()}}
    LaunchedEffect(alive) {
        url=""
        if(!state.appForeground) return@LaunchedEffect
        try {val created=withContext(Dispatchers.IO) {
            val endpoint=localQrEndpointsRP145(context).firstOrNull() ?: error("LAN unavailable")
            SearchKeyboardServerRP136(endpoint.address,endpoint.prefixLength) {text ->
                if(!alive.get()) false else {main.post {if(alive.get() && state.appForeground && state.localExtrasScope()==owner) callback(text)};true}
            }.also {server.set(it);if(!alive.get()) it.close()}
        };url=created.url;delay(900000);created.close();url="";status=tr("QR vencido.","QR expired.")}
        catch(e:CancellationException) {throw e} catch(_:Exception) {status=tr("No se pudo abrir el QR local. Usa el teclado de TV o Bluetooth.","Could not open local QR. Use the TV keyboard or Bluetooth.")}
    }
    PlayerMenuDialog(onDismissRequest=close,fullScreen=true) {
        LazyColumn(Modifier.fillMaxSize().consumeOpeningPress().background(Bg).padding(30.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            item(key="assistant-static-rp150-15") {Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)) {Text(tr("Escribe o dicta desde tu celular","Type or dictate from your phone"),color=Color.White,fontSize=26.sp,modifier=Modifier.weight(1f));FocusButton("×",onClick=close)}}
            item(key="assistant-static-rp150-16") {Text(tr("Escanea el QR. Puedes usar el micrófono del teclado del celular. El texto llegará al campo de BruniO AI; después cierra este panel y pulsa Buscar. No se consulta Gemini mientras escribes.","Scan the QR and use your phone keyboard microphone if desired. Text fills BruniO AI; then close this panel and select Find. Typing never calls Gemini."),color=Muted,fontSize=15.sp)}
            if(url.isNotBlank()) item(key="assistant-static-rp150-17") {QrCode(url,190.dp)}
            if(status.isNotBlank()) item(key="assistant-static-rp150-18") {Text(status,color=Muted)}
            item(key="assistant-static-rp150-19") {FocusButton(tr("Listo · volver a mi consulta","Done · back to my request"),onClick=close)}
        }
    }
}
