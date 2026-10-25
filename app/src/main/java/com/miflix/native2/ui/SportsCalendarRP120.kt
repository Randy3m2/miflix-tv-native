package com.miflix.native2.ui

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.miflix.native2.data.*
import kotlinx.coroutines.*
import java.time.*
import java.time.format.DateTimeFormatter

@Composable
internal fun CalendarRuntimeRP120(owner:String,name:String,foreground:Boolean=true) {
    val context=LocalContext.current.applicationContext
    val starting=tr("Comienza ahora","Starting now")
    LaunchedEffect(owner,name,starting,foreground) {
        if(owner.isBlank()) return@LaunchedEffect
        withContext(Dispatchers.IO) {CalendarStoreRP120.activate(context,owner,name,starting)}
        if(!foreground) return@LaunchedEffect
        withContext(Dispatchers.IO) {
            try {CalendarStoreRP120.refresh(context,owner)} catch(e:CancellationException) {throw e} catch(_:Exception) { }
        }
        while(isActive) {
            val next=withContext(Dispatchers.IO) {
                try {CalendarStoreRP120.refresh(context,owner)} catch(e:CancellationException) {throw e} catch(_:Exception) { }
                CalendarStoreRP120.tick(context,owner)
                CalendarStoreRP120.events(context,owner).filter {it.timed && it.starts>System.currentTimeMillis()}.minOfOrNull {it.starts}
            }
            delay(((next ?: (System.currentTimeMillis()+60000))-System.currentTimeMillis()).coerceIn(250,60000))
        }
    }
}

@Composable
internal fun rememberSportEventsRP120(state:AppState,month:String):List<SportEventRP120> {
    val context=LocalContext.current.applicationContext;val owner=CalendarStoreRP120.owner(context,state.activeProfile.id)
    val revision by CalendarStoreRP120.revision.collectAsState()
    val rows by produceState<List<SportEventRP120>>(emptyList(),owner,revision) {
        value=withContext(Dispatchers.IO) {CalendarStoreRP120.events(context,owner)}
    }
    val subscriptions=remember(revision,owner) {CalendarStoreRP120.subscriptions(context,owner)}
    LaunchedEffect(owner,month,subscriptions) {withContext(Dispatchers.IO) {
        runCatching {CalendarStoreRP120.refresh(context,owner,month=YearMonth.parse(month))}
    }}
    return rows
}

@Composable
internal fun EventLogoRP120(logo:String,caption:String,modifier:Modifier=Modifier) {
    val context=LocalContext.current
    var imageFailed by remember(logo) {mutableStateOf(false)}
    val model=remember(logo) {if(logo.startsWith("drawable:")) context.resources.getIdentifier(logo.substringAfter(":"),"drawable",context.packageName) else logo}
    Box(modifier.background(Color.White.copy(alpha=.06f),RoundedCornerShape(8.dp)),contentAlignment=Alignment.Center) {
        if(logo.isBlank() || imageFailed) Text(caption.take(3),color=Color.White,fontSize=10.sp,fontWeight=FontWeight.Bold)
        else AsyncImage(ImageRequest.Builder(context).data(model).size(128,128).build(),caption,Modifier.fillMaxSize().padding(3.dp),contentScale=ContentScale.Fit,onError={imageFailed=true})
    }
}

@Composable
internal fun CalendarSportEventRP120(state:AppState,event:SportEventRP120) {
    val context=LocalContext.current;val owner=CalendarStoreRP120.owner(context,state.activeProfile.id)
    var details by remember(event.key) {mutableStateOf(false)}
    Row(Modifier.fillMaxWidth().background(GlassActionBrushRP108(false,true),RoundedCornerShape(14.dp))
        .padding(12.dp),horizontalArrangement=Arrangement.spacedBy(10.dp),verticalAlignment=Alignment.CenterVertically) {
        EventLogoRP120(event.logos.firstOrNull() ?: CalendarSourcesRP120.logo(event.league),event.league,Modifier.size(38.dp))
        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
            Text(event.title,color=Color.White,fontSize=15.sp,fontWeight=FontWeight.Bold,maxLines=3)
            Text(event.league+" · "+event.day()+" · "+eventTimeRP120(event),color=Muted,fontSize=11.sp)
        }
        FocusButton(tr("Ver","View"),horizontalPadding=12.dp) {details=true}
    }
    if(details) PlayerSideSheet(event.league,{details=false}) {
        item {Text(event.title,color=Color.White,fontSize=22.sp,fontWeight=FontWeight.Bold)}
        item {Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            event.logos.forEach {EventLogoRP120(it,event.league,Modifier.size(56.dp))}
        }}
        item {Text(event.day()+" · "+eventTimeRP120(event),color=Color.White)}
        item {Text(if(event.timed) tr("Recibirás un aviso al comenzar si sigues este evento o sus equipos.","Get a start alert by following this event or its teams.") else tr("Hora por confirmar. No se programa un aviso hasta tener una hora confirmada.","Time to be confirmed. No start alert until a confirmed time is available."),color=Muted)}
        item {FocusButton(tr("Seguir / dejar de seguir este evento","Follow / unfollow this event")) {CalendarStoreRP120.toggle(context,owner,"EVENT:"+event.key)}}
    }
}
internal fun eventTimeRP120(event:SportEventRP120)=if(event.timed) DateTimeFormatter.ofPattern("h:mm a").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(event.starts)) else tr("Hora por confirmar","Time TBA")

@Composable
internal fun CalendarSubscriptionsRP120(state:AppState,initialLeagueRP121:String?=null,close:()->Unit) {
    val context=LocalContext.current;val owner=CalendarStoreRP120.owner(context,state.activeProfile.id);val scope=rememberCoroutineScope()
    val revision by CalendarStoreRP120.revision.collectAsState()
    val subs=remember(revision,owner) {CalendarStoreRP120.subscriptions(context,owner)}
    val teams by produceState<List<CalendarTeamRP120>>(emptyList()) {value=withContext(Dispatchers.IO) {CalendarSourcesRP120.teams(context)}}
    var league by remember(initialLeagueRP121) {mutableStateOf<String?>(initialLeagueRP121)}
    var query by remember {mutableStateOf("")}
    var feed by remember(owner) {mutableStateOf(CalendarStoreRP120.feed(context,owner))}
    var busy by remember {mutableStateOf(false)}
    var status by remember {mutableStateOf("")}
    var pendingRefresh by remember {mutableStateOf(false)}
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    fun refresh(force:Boolean=false) {
        if(busy) {pendingRefresh=true;return}
        busy=true
        scope.launch {
            try {
                do {
                    pendingRefresh=false
                    withContext(Dispatchers.IO) {CalendarStoreRP120.refresh(context,owner,force)}
                    status=CalendarStoreRP120.error(context,owner).takeIf {it.isNotBlank()}?.let {tr("No se actualizaron: ","Could not update: ")+it} ?: tr("Calendario actualizado","Calendar updated")
                } while(pendingRefresh)
            } catch(e:CancellationException) {throw e} catch(_:Exception) {status=tr("Sin conexión. Conservamos el calendario guardado.","Offline. Keeping the saved calendar.")}
            finally {busy=false}
        }
    }
    fun follow(key:String) {
        CalendarStoreRP120.toggle(context,owner,key)
        if(Build.VERSION.SDK_INT>=33 && context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED) permission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        status=tr("Selección guardada. Los horarios se actualizarán en segundo plano.","Selection saved. Schedules will update in the background.")
    }
    PlayerSideSheet(tr("Suscripciones","Subscriptions"),close,league) {
        item {Text(tr("Tus equipos. Tus fechas.","Your teams. Your dates."),color=Color.White,fontSize=23.sp,fontWeight=FontWeight.Bold)}
        item {Text(tr("Calendario por perfil · horarios locales · avisos al iniciar","Per-profile calendar · local times · start alerts"),color=Muted,fontSize=12.sp)}
        if(league==null) {
            items(CalendarSourcesRP120.leagues) {id ->
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    EventLogoRP120(CalendarSourcesRP120.logo(id),id,Modifier.size(44.dp))
                    FocusButton(id+(if(subs.any {it.startsWith("$id:")}) "  ✓" else ""),Modifier.weight(1f)) {league=id;query=""}
                }
            }
        } else {
            val id=league!!
            item {FocusButton(tr("‹ Todas las ligas y eventos","‹ All leagues and events")) {league=null}}
            item {FocusButton((if("$id:*" in subs) "✓ " else "+ ")+tr("Seguir ","Follow ")+id,Modifier.fillMaxWidth(),primary="$id:*" in subs) {follow("$id:*")}}
            val choices=teams.filter {it.league==id}
            if(choices.isNotEmpty()) item {NativeTextField(query,{query=it},tr("Buscar equipo","Find team"),modifier=Modifier.fillMaxWidth())}
            items(choices.filter {IptvPlaylist.searchKey(it.name).contains(IptvPlaylist.searchKey(query))},key={it.id}) {team ->
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                    EventLogoRP120(team.logo,team.name,Modifier.size(46.dp))
                    FocusButton((if("$id:${team.id}" in subs) "✓ " else "+ ")+team.name,Modifier.weight(1f),primary="$id:${team.id}" in subs) {follow("$id:${team.id}")}
                }
            }
            if(id in listOf("BOXING","GRAMMYS","OSCARS","MISS_UNIVERSE")) item {
                Text(tr("Fechas confirmadas y catálogo editorial. Las nuevas peleas y ceremonias pueden actualizarse con el JSON de GitHub.","Confirmed dates and curated catalog. New fights and ceremonies can be updated using a GitHub JSON feed."),color=Muted,fontSize=12.sp)
            }
        }
        item {FocusButton(tr("Actualizar horarios","Refresh schedules")) {refresh(true)}}
        item {NativeTextField(feed,{feed=it},tr("URL HTTPS de eventos especiales (opcional)","Special events HTTPS URL (optional)"),modifier=Modifier.fillMaxWidth())}
        item {FocusButton(tr("Guardar fuente de eventos","Save events feed")) {
            if(feed.isBlank() || runCatching {val uri=java.net.URI(feed.trim());uri.scheme=="https" && !uri.host.isNullOrBlank() && uri.userInfo==null}.getOrDefault(false)) {
                CalendarStoreRP120.setFeed(context,owner,feed);refresh(true)
            } else status=tr("Usa una URL HTTPS válida","Use a valid HTTPS URL")
        }}
        if(Build.VERSION.SDK_INT>=31) item {FocusButton(tr("Permitir avisos puntuales","Allow precise alerts")) {
            if(CalendarAlarmsRP120.precise(context)) status=tr("Los avisos puntuales ya están permitidos","Precise alerts are already allowed")
            else runCatching {context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,android.net.Uri.parse("package:"+context.packageName)))}
                .onFailure {status=tr("Esta TV no ofrece ese ajuste. Los avisos pueden retrasarse.","This TV does not expose that setting. Alerts may be delayed.")}
        }}
        if(busy || status.isNotBlank()) item {Text(if(busy) tr("Actualizando…","Updating…") else status,color=Color.White)}
    }
}
