package com.miflix.native2.ui

import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
    val ai=remember(context) {GeminiAssistantRP150(context)}
    val scope=rememberCoroutineScope()
    val collectionPrefs=remember(context) {context.getSharedPreferences("brunio_smart_collections_rp150",android.content.Context.MODE_PRIVATE)}
    val owner=state.localExtrasScope()
    var collectionRevision by remember {mutableIntStateOf(0)}
    val saved=remember(owner,collectionRevision) {runCatching {val a=org.json.JSONArray(collectionPrefs.getString(owner,"[]"));(0 until a.length()).mapNotNull {a.optJSONObject(it)}}.getOrDefault(emptyList())}
    var query by remember(owner) {mutableStateOf("")}
    var budget by remember {mutableIntStateOf(0)}
    val useGemini=remember {context.getSharedPreferences("brunio_gemini_rp150",0).getBoolean("enabled",false)}
    var sceneFinder by remember {mutableStateOf(false)}
    var phone by remember {mutableStateOf(false)}
    val configured=ai.configured()
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
    var voiceOwner by remember {mutableStateOf<String?>(null)}
    val voice=androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()) {result ->
        if(result.resultCode==android.app.Activity.RESULT_OK && voiceOwner==owner && state.localExtrasScope()==owner) {
            result.data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.trim()?.takeIf {it.isNotBlank()}?.let {query=it.take(500);message=tr("Listo. Pulsa Buscar para descubrir tu próxima historia.","Ready. Select Search to discover your next story.")}
        }
        voiceOwner=null
    }
    fun dictate() {
        if(busy) return
        voiceOwner=owner
        try {voice.launch(android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL,android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE,state.voiceLocaleRP81)
            .putExtra(android.speech.RecognizerIntent.EXTRA_MAX_RESULTS,3)
            .putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT,tr("¿Qué te apetece ver?","What would you like to watch?")))}
        catch(_:Exception) {voiceOwner=null;phone=true;message=tr("Esta TV no ofrece dictado. Escanea el QR y usa el micrófono del celular.","This TV does not offer dictation. Scan the QR and use your phone microphone.")}
    }
    val artwork=state.moodArtworkRC102()
    val starters=(state.forYou+state.trending+state.topRated).distinctBy {it.cloudId}.filter {(!it.poster.isNullOrBlank() || !it.backdrop.isNullOrBlank()) && !state.kidsMode}.take(10)
    if(sceneFinder) {SceneFinderRP151(state) {sceneFinder=false};return}
    if(phone) AssistantTextQrRP150(state,{if(!busy) query=it},{phone=false})
    androidx.activity.compose.BackHandler(onBack=close)
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Column(Modifier.fillMaxSize().consumeOpeningPress().background(Color.Black).padding(horizontal=34.dp,vertical=18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp),verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(3.dp)) {
                    Text("✦  BruniO AI",color=Color.White,fontSize=29.sp,fontWeight=FontWeight.Bold)
                    Text(tr("Menos buscar. Más descubrir.","Less searching. More discovering."),color=Color(0xffbcb4cf),fontSize=13.sp)
                }
                Text(if(useGemini && configured) "GEMINI" else tr("EN TU TV","ON YOUR TV"),color=Color(0xffc8adff),fontSize=9.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(end=8.dp))
                if(!state.kidsMode && LocalGuestExitRP150.current==null) AssistantActionRP152(tr("Invitado","Guest"),"friends") {if(state.prepareGuestRP150()) guest() else message=tr("Cierra el reproductor y sal del Party para entrar como invitado.","Close playback and leave the Party to enter guest mode.")}
                AssistantActionRP152("Scene Finder","search") {sceneFinder=true}
                AssistantActionRP152("×",onClick=close)
            }
            LazyColumn(Modifier.weight(1f).fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(14.dp),contentPadding=PaddingValues(bottom=24.dp)) {
                item(key="search-rp152") {
                    Row(Modifier.fillMaxWidth().background(Color.White.copy(alpha=.065f),RoundedCornerShape(20.dp)).border(1.dp,Color.White.copy(alpha=.12f),RoundedCornerShape(20.dp)).padding(8.dp),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically) {
                        AssistantSearchFieldRP153(query,{if(!busy) query=it.take(500)},!busy,Modifier.weight(1f)) {submit()}
                        AssistantActionRP152(tr("Dictar","Speak"),"mic",onClick={dictate()})
                        AssistantActionRP152(if(busy) tr("Buscando…","Searching…") else tr("Buscar","Search"),"search",selected=true) {submit()}
                        AssistantActionRP152("QR") {if(!busy) phone=true}
                    }
                }
                item(key="time-rp152") {Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    Text(tr("Tu tiempo","Your time"),color=Color(0xffbcb4cf),fontSize=12.sp)
                    LazyRow(horizontalArrangement=Arrangement.spacedBy(7.dp)) {items(listOf(0,15,30,45,60,90,120)) {time ->AssistantActionRP152(if(time==0) tr("Según consulta","From request") else "$time min",selected=budget==time) {if(!busy) budget=time}}}
                }}
                if(busy || message.isNotBlank()) item(key="status-rp152") {Row(horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=Alignment.CenterVertically) {
                    Text(if(busy) tr("✦ Buscando historias para ti…","✦ Finding your next story…") else message,color=Color(0xffcdb9ef),fontSize=13.sp,modifier=Modifier.weight(1f))
                    if(busy) AssistantActionRP152(tr("Cancelar","Cancel")) {requestGeneration++;job?.cancel();busy=false}
                }}
                if(picks.isNotEmpty()) {
                    item(key="results-title-rp152") {Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {Text(tr("Hecho para tu momento","Made for your moment"),color=Color.White,fontSize=22.sp,fontWeight=FontWeight.Bold);Text(resultQuery.ifBlank {"${resultBudget} min"},color=Muted,fontSize=12.sp,maxLines=1,overflow=TextOverflow.Ellipsis)}
                        if(resultQuery.isNotBlank()) AssistantActionRP152(tr("Guardar colección","Save collection"),"list") {
                            val values=(listOf(org.json.JSONObject().put("title",resultQuery.take(65)).put("query",resultQuery).put("budget",resultBudget).put("plan",activePlan?.json()))+saved.filterNot {it.optString("query")==resultQuery}).take(10)
                            collectionPrefs.edit().putString(owner,org.json.JSONArray(values).toString()).apply();collectionRevision++
                        }
                    }}
                    items(picks.chunked(4),key={chunk ->"result:"+chunk.first().item.cloudId+":"+chunk.first().episode?.episode}) {chunk ->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                        chunk.forEach {pick ->AssistantPosterRP152(pick,Modifier.weight(1f),details={close();state.launch {state.open(pick.item)}},play=if(pick.resume || pick.episode!=null) ({close();state.launch {state.open(pick.item);if(state.selected?.cloudId==pick.item.cloudId) {val ep=pick.episode;if(ep!=null) state.playEpisode(ep) else state.playMovie()}}}) else null)}
                        repeat(4-chunk.size) {Spacer(Modifier.weight(1f))}
                    }}
                }
                if(desiredAudio.isNotBlank()) item(key="audio-rp152") {AssistantActionRP152(tr("Preferir audio: ","Prefer audio: ")+AppLanguagesRP148.label(desiredAudio)) {state.savePlaybackPreferences(desiredAudio,state.subtitleLanguage,state.autoplayNext);message=tr("Audio preferido guardado. El doblaje se confirma en las fuentes.","Audio preference saved. Dubbed audio depends on available sources.")}}
                item(key="ideas-title-rp152") {Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {Text(tr("Una idea para esta noche","A little inspiration for tonight"),color=Color.White,fontSize=20.sp,fontWeight=FontWeight.SemiBold);Text(tr("EXPLORA →","EXPLORE →"),color=Color(0xffb9a1e1),fontSize=10.sp,modifier=Modifier.padding(top=6.dp))}}
                item(key="ideas-rp152") {LazyRow(horizontalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(vertical=4.dp)) {items(AssistantIdeasRP152,key={it.id}) {idea ->AssistantIdeaCardRP152(state,idea,artwork[idea.art]) {submit(idea.query())}}}}
                if(saved.isNotEmpty()) item(key="collections-rp152") {Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
                    Text(tr("Tus colecciones","Your collections"),color=Color.White,fontSize=20.sp,fontWeight=FontWeight.SemiBold)
                    LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {items(saved) {row ->AssistantActionRP152(row.optString("title"),"list") {submit(row.optString("query"),row.optInt("budget"),runCatching {AssistantPlanRP150.json(row.getJSONObject("plan").toString())}.getOrNull())}}}
                    AssistantActionRP152(tr("Borrar colecciones","Clear collections")) {collectionPrefs.edit().remove(owner).apply();collectionRevision++}
                }}
                if(picks.isEmpty() && starters.isNotEmpty() && !busy) item(key="starter-rp152") {Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
                    Text(tr("Empieza con una buena historia","Start with a great story"),color=Color.White,fontSize=20.sp,fontWeight=FontWeight.SemiBold)
                    LazyRow(horizontalArrangement=Arrangement.spacedBy(14.dp)) {items(starters,key={it.cloudId}) {media ->AssistantPosterRP152(AssistantPickRP150(media,0),Modifier.width(136.dp),details={close();state.launch {state.open(media)}},play=null)}}
                }}
            }
        }
    }
}

@Composable
internal fun GeminiSetupRP150(state:AppState,ai:GeminiAssistantRP150,close:()->Unit) {
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
                if(!alive.get()) false else try {
                    synchronized(ai) {if(!alive.get()) return@SearchKeyboardServerRP136 false;ai.save(key)}
                    main.post {if(alive.get()) status=tr("Clave guardada en esta TV. Ya puedes cerrar.","Key saved on this TV. You can close this panel.")};true
                } catch(e:SearchKeyboardServerRP136.SaveFailure) {
                    main.post {if(alive.get()) status=when(e.reason) {
                        SearchKeyboardServerRP136.SaveError.INVALID_KEY ->tr("Formato de clave no válido. Copia la clave completa, sin código ni comillas.","Invalid key format. Copy the full key without code or quotes.")
                        SearchKeyboardServerRP136.SaveError.STORAGE ->tr("La TV no pudo guardar la clave. Revisa el espacio disponible.","The TV could not persist the key. Check available storage.")
                        else ->tr("La TV no pudo cifrar la clave. Reinicia BruniO y reintenta.","The TV could not encrypt the key. Restart BruniO and retry.")
                    }}
                    throw e
                }
            },true).also {server.set(it);if(!alive.get()) it.close()}
        };qr=created.url;delay(900000);created.close();qr="";status=tr("QR vencido.","QR expired.")}
        catch(e:CancellationException) {throw e} catch(_:Exception) {status=tr("No se pudo abrir el QR local. Revisa la red.","Could not open local QR. Check your network.")}
    }
    PlayerMenuDialog(onDismissRequest=close,fullScreen=true) {
        LazyColumn(Modifier.fillMaxSize().consumeOpeningPress().background(Color.Black).padding(28.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
            item(key="assistant-static-rp150-9") {Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)) {Text("Gemini · Personal API key",color=Color.White,fontSize=26.sp,modifier=Modifier.weight(1f));FocusButton("×",onClick=close)}}
            item(key="assistant-static-rp150-10") {Text(tr("Opcional. Escanea el QR y pega tu propia clave de Google AI Studio. Usa un proyecto Free Tier sin facturación: BruniO no puede convertir una clave de pago en gratuita. La clave se cifra en este dispositivo y no se sincroniza.","Optional. Scan and paste your own Google AI Studio key. Use a Free Tier project without billing: BruniO cannot make a paid key free. The key is encrypted on this device and never synced."),color=Muted,fontSize=15.sp)}
            item(key="assistant-static-rp150-11") {Text(tr("Solo se envía la consulta que escribas. Google puede usar solicitudes del nivel gratuito para mejorar sus productos. Límite local: 20 consultas al día, una cada 20 segundos; las cuotas de Google son independientes.","Only your submitted request is sent. Google may use free-tier requests to improve products. Local cap: 20 requests/day, one per 20 seconds; Google quotas are separate."),color=Muted,fontSize=13.sp)}
            if(qr.isNotBlank()) item(key="assistant-static-rp150-12") {QrCode(qr,190.dp)}
            if(status.isNotBlank()) item(key="assistant-static-rp150-13") {Text(status,color=Color.White)}
            item(key="assistant-static-rp150-14") {FlowRow(horizontalArrangement=Arrangement.spacedBy(16.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
                FocusButton(tr("Eliminar clave","Remove key")) {alive.set(false);server.getAndSet(null)?.close();qr="";synchronized(ai) {ai.clear()};status=tr("Clave eliminada.","Key removed.")}
                FocusButton(tr("Cerrar","Close"),onClick=close)
            }}
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
        LazyColumn(Modifier.fillMaxSize().consumeOpeningPress().background(Color.Black).padding(30.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            item(key="assistant-static-rp150-15") {Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)) {Text(tr("Escribe o dicta desde tu celular","Type or dictate from your phone"),color=Color.White,fontSize=26.sp,modifier=Modifier.weight(1f));FocusButton("×",onClick=close)}}
            item(key="assistant-static-rp150-16") {Text(tr("Escanea el QR. Puedes usar el micrófono del teclado del celular. El texto llegará al campo de BruniO AI; después cierra este panel y pulsa Buscar. No se consulta Gemini mientras escribes.","Scan the QR and use your phone keyboard microphone if desired. Text fills BruniO AI; then close this panel and select Find. Typing never calls Gemini."),color=Muted,fontSize=15.sp)}
            if(url.isNotBlank()) item(key="assistant-static-rp150-17") {QrCode(url,190.dp)}
            if(status.isNotBlank()) item(key="assistant-static-rp150-18") {Text(status,color=Muted)}
            item(key="assistant-static-rp150-19") {FocusButton(tr("Listo · volver a mi consulta","Done · back to my request"),onClick=close)}
        }
    }
}
