@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package com.miflix.native2.ui

import android.content.Context
import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.miflix.native2.data.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import org.json.JSONObject
import java.time.LocalDate
import kotlin.math.roundToInt

private class ScorePinStateRP11(context:Context,owner:String) {
    private val prefs=context.getSharedPreferences("brunio_score_pin_rp11",Context.MODE_PRIVATE)
    private val key=AddonPriorityRP2.id(owner)+":"
    val repo=LiveScoresRP11()
    var pinned by mutableStateOf(runCatching {LiveScoresRP11.decode(prefs.getString(key+"pin","").orEmpty())}.getOrNull())
        private set
    var enabled by mutableStateOf(prefs.getBoolean(key+"enabled",true))
        private set
    var x by mutableStateOf(ScorePolicyRP11.coordinate(prefs.getFloat(key+"x",1f)))
    var y by mutableStateOf(ScorePolicyRP11.coordinate(prefs.getFloat(key+"y",0f)))
    var stale by mutableStateOf(false)
    fun pin(game:LiveScoreRP11?) {
        pinned=game;stale=false
        prefs.edit().apply {if(game==null) remove(key+"pin") else putString(key+"pin",LiveScoresRP11.encode(game))}.apply()
    }
    fun update(game:LiveScoreRP11) {if(game.state=="final") pin(game) else {pinned=game;stale=false}}
    fun saveEnabled(value:Boolean) {enabled=value;prefs.edit().putBoolean(key+"enabled",value).apply()}
    fun savePosition() {prefs.edit().putFloat(key+"x",x).putFloat(key+"y",y).apply()}
}

private fun scorePhaseRP11(game:LiveScoreRP11):String {
    if(game.state=="scheduled" && game.starts.isNotBlank()) {
        val local=runCatching {
            java.time.OffsetDateTime.parse(game.starts).atZoneSameInstant(java.time.ZoneId.of("America/Santo_Domingo"))
                .format(java.time.format.DateTimeFormatter.ofPattern("MM/dd · h:mm a",java.util.Locale.US))+" AST"
        }.getOrNull()
        if(local!=null && game.phase.lowercase(java.util.Locale.ROOT).let {p ->listOf("postponed","delayed","suspended","cancel").none {it in p}}) return local
    }
    return when(game.phase) {
        "Scheduled","Pre-Game","Preview"->tr("Por comenzar","Scheduled")
        "In Progress"->tr("En juego","In progress")
        "Postponed"->tr("Pospuesto","Postponed")
        "Delayed"->tr("Demorado","Delayed")
        "Suspended"->tr("Suspendido","Suspended")
        else->game.phase
    }
}

@Composable
internal fun LiveScoreOverlayRP11(state:AppState) {
    val context=LocalContext.current
    val owner="${state.session?.userId ?: "local"}:${state.activeProfile.id}"
    val pin=remember(owner) {ScorePinStateRP11(context,owner)}
    var moving by remember(owner) {mutableStateOf(false)}
    val visible=state.appForeground && !state.kidsMode && state.screen !in setOf(Screen.PROFILES,Screen.SPLASH)
    LaunchedEffect(owner) {state.showLiveScoresRP11=false;moving=false}
    val current=pin.pinned
    LaunchedEffect(owner,pin.enabled,current?.id,current?.league,current?.date,visible) {
        if(!visible || !pin.enabled || current==null || current.state=="final") return@LaunchedEffect
        var failures=0
        while(true) {
            try {
                val game=pin.repo.games(current.league,current.date).firstOrNull {it.id==current.id}
                if(game!=null && pin.pinned?.id==current.id) {pin.update(game);failures=0}
                else {pin.stale=true;failures++}
            } catch(e:CancellationException) {throw e} catch(_:Exception) {pin.stale=true;failures++}
            val wait=ScorePolicyRP11.delayMs(pin.pinned?.state ?: "final",failures)
            if(wait==0L) break
            delay(wait)
        }
    }
    if(visible && pin.enabled && current!=null) BoxWithConstraints(Modifier.fillMaxSize()) {
        val density=LocalDensity.current
        val width=minOf(340.dp,maxWidth-16.dp)
        val height=106.dp
        val maxX=with(density) {(maxWidth-width-16.dp).toPx()}.coerceAtLeast(0f)
        val maxY=with(density) {(maxHeight-height-16.dp).toPx()}.coerceAtLeast(0f)
        val inset=with(density) {8.dp.toPx()}
        val focus=remember {FocusRequester()}
        Popup(alignment=Alignment.TopStart,
            offset=IntOffset((inset+pin.x*maxX).roundToInt(),(inset+pin.y*maxY).roundToInt()),
            onDismissRequest={moving=false;pin.savePosition()},
            properties=PopupProperties(focusable=moving,dismissOnBackPress=moving)) {
            LaunchedEffect(moving) {if(moving) {withFrameNanos {};runCatching {focus.requestFocus()}}}
            Column(Modifier.width(width).focusRequester(focus).onPreviewKeyEvent {event ->
                if(!moving) false else {
                    val key=event.nativeKeyEvent
                    val supported=key.keyCode in listOf(KeyEvent.KEYCODE_DPAD_LEFT,KeyEvent.KEYCODE_DPAD_RIGHT,
                        KeyEvent.KEYCODE_DPAD_UP,KeyEvent.KEYCODE_DPAD_DOWN,KeyEvent.KEYCODE_DPAD_CENTER,
                        KeyEvent.KEYCODE_ENTER,KeyEvent.KEYCODE_BACK)
                    if(supported && key.action==KeyEvent.ACTION_DOWN) {
                        when(key.keyCode) {
                            KeyEvent.KEYCODE_DPAD_LEFT->pin.x=ScorePolicyRP11.coordinate(pin.x-.025f)
                            KeyEvent.KEYCODE_DPAD_RIGHT->pin.x=ScorePolicyRP11.coordinate(pin.x+.025f)
                            KeyEvent.KEYCODE_DPAD_UP->pin.y=ScorePolicyRP11.coordinate(pin.y-.025f)
                            KeyEvent.KEYCODE_DPAD_DOWN->pin.y=ScorePolicyRP11.coordinate(pin.y+.025f)
                            else->{moving=false;pin.savePosition()}
                        }
                    }
                    supported
                }
            }.focusable(enabled=moving).pointerInput(owner,maxX,maxY) {
                detectDragGestures(onDragEnd={pin.savePosition()},onDragCancel={pin.savePosition()}) {change,drag ->
                    change.consume()
                    if(maxX>0) pin.x=ScorePolicyRP11.coordinate(pin.x+drag.x/maxX)
                    if(maxY>0) pin.y=ScorePolicyRP11.coordinate(pin.y+drag.y/maxY)
                }
            }) {
                Column(Modifier.fillMaxWidth().height(70.dp)
                    .background(Color(0xE6101010),RoundedCornerShape(36.dp))
                    .border(if(moving) 2.dp else 1.dp,if(moving) Color.White else Color(0xFF444444),RoundedCornerShape(36.dp))
                    .clickable {state.showLiveScoresRP11=true}.padding(horizontal=16.dp,vertical=8.dp),
                    horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(3.dp)) {
                    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        ScoreTeamLogoRP11(current.away,current.league)
                        Text(current.away.short,color=Color.White,fontSize=13.sp,fontWeight=FontWeight.Bold,maxLines=1)
                        Text(current.away.score+" – "+current.home.score,color=Color.White,fontSize=20.sp,fontWeight=FontWeight.Bold)
                        Text(current.home.short,color=Color.White,fontSize=13.sp,fontWeight=FontWeight.Bold,maxLines=1)
                        ScoreTeamLogoRP11(current.home,current.league)
                    }
                    val live=if(current.state=="live") tr("EN VIVO","LIVE")+" · " else ""
                    Text(current.league+" · "+live+scorePhaseRP11(current)+(if(pin.stale) tr(" · Sin actualizar"," · Update delayed") else ""),
                        color=if(pin.stale) Color(0xFFFFD287) else Color(0xFFBDBDBD),fontSize=10.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
                }
                if(moving) Text(tr("Flechas · Mover     OK / Atrás · Guardar","Arrows · Move     OK / Back · Save"),
                    color=Color.White,fontSize=11.sp,modifier=Modifier.background(Color(0xDC101010),RoundedCornerShape(12.dp)).padding(8.dp))
            }
        }
    }
    if(state.showLiveScoresRP11 && !state.kidsMode) ScorePinManagerRP11(state,pin,onMove={
        state.showLiveScoresRP11=false;moving=true
    },close={state.showLiveScoresRP11=false})
}

@Composable
private fun ScoreTeamLogoRP11(team:ScoreTeamRP11,league:String) {
    val context=LocalContext.current
    val local=remember(team.id,league) {
        if(league!="LIDOM") 0 else runCatching {
            val teams=JSONObject(context.assets.open("lidom_teams.json").bufferedReader().use {it.readText()}).getJSONArray("teams")
            val item=(0 until teams.length()).map {teams.getJSONObject(it)}.firstOrNull {it.optInt("mlb").toString()==team.id}
            context.resources.getIdentifier(item?.optString("logo").orEmpty(),"drawable",context.packageName)
        }.getOrDefault(0)
    }
    if(local!=0) Image(painterResource(local),team.name,Modifier.size(24.dp))
    else if(team.logo.isNotBlank()) AsyncImage(team.logo,team.name,Modifier.size(24.dp))
    else Text(team.short.take(2),color=Color.White,fontSize=11.sp,modifier=Modifier.width(24.dp))
}

@Composable
private fun ScorePinManagerRP11(state:AppState,pin:ScorePinStateRP11,onMove:()->Unit,close:()->Unit) {
    var league by remember {mutableStateOf(pin.pinned?.league ?: if(state.screen==Screen.LIDOM) "LIDOM" else "NBA")}
    var date by remember {mutableStateOf(LiveScoresRP11.today())}
    var games by remember {mutableStateOf<List<LiveScoreRP11>>(emptyList())}
    var loading by remember {mutableStateOf(true)}
    var message by remember {mutableStateOf("")}
    var revision by remember {mutableIntStateOf(0)}
    LaunchedEffect(league,date,revision,state.appForeground) {
        if(!state.appForeground) return@LaunchedEffect
        loading=true;message="";games=emptyList()
        try {games=pin.repo.games(league,date)}
        catch(e:CancellationException) {throw e} catch(_:Exception) {message=tr("No se pudo consultar el proveedor. Reintenta en unos momentos.","Could not reach the provider. Try again shortly.")}
        finally {loading=false}
    }
    PlayerSideSheet(tr("PIN Live score","PIN Live score"),close) {
        item {
            Text(tr("Marcador flotante dentro de BruniO. Actualiza cada 30 segundos durante el juego; sin consultas a Supabase.",
                "Floating score inside BruniO. Updates every 30 seconds during games; no Supabase requests."),color=Muted,fontSize=13.sp)
            FocusButton(tr("Marcador: ","Score pin: ")+if(pin.enabled) tr("Activado","On") else tr("Desactivado","Off")) {pin.saveEnabled(!pin.enabled)}
        }
        pin.pinned?.let {game ->
            item {
                Text(game.away.name+" · "+game.home.name,color=Color.White,fontSize=16.sp)
                FlowRow(horizontalArrangement=Arrangement.spacedBy(10.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    FocusButton(tr("Mover píldora","Move pill")) {pin.saveEnabled(true);onMove()}
                    FocusButton(tr("Quitar PIN","Unpin")) {pin.pin(null);close()}
                }
            }
        }
        item {
            FlowRow(horizontalArrangement=Arrangement.spacedBy(10.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                listOf("NBA","MLB","LIDOM").forEach {l ->FocusButton(l,primary=league==l) {league=l}}
            }
            Text(date,color=Color.White,fontSize=14.sp)
            FlowRow(horizontalArrangement=Arrangement.spacedBy(10.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                FocusButton("‹") {date=LocalDate.parse(date).minusDays(1).toString()}
                FocusButton(tr("Hoy","Today")) {date=LiveScoresRP11.today()}
                FocusButton("›") {date=LocalDate.parse(date).plusDays(1).toString()}
                FocusButton(tr("Actualizar","Refresh")) {if(!loading) revision++}
            }
            Text(if(league=="NBA") "NBA · ESPN" else "$league · MLB Stats API",color=Muted,fontSize=12.sp)
        }
        if(loading) item {Text(tr("Cargando marcadores…","Loading scores…"),color=Muted)}
        if(message.isNotBlank()) item {Text(message,color=Muted)}
        if(!loading && message.isBlank() && games.isEmpty()) item {Text(tr("No hay partidos publicados para esta fecha.","No published games for this date."),color=Muted)}
        items(games,key={it.league+":"+it.id}) {game ->
            Column(Modifier.fillMaxWidth().background(Panel,RoundedCornerShape(18.dp)).padding(12.dp),
                verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                    ScoreTeamLogoRP11(game.away,game.league)
                    Text(game.away.short+" "+game.away.score+" – "+game.home.score+" "+game.home.short,color=Color.White,fontSize=18.sp)
                    ScoreTeamLogoRP11(game.home,game.league)
                }
                Text(game.away.name+" · "+game.home.name,color=Muted,fontSize=12.sp)
                Text(scorePhaseRP11(game),color=Muted,fontSize=12.sp)
                FocusButton(tr("Fijar marcador","Pin live score"),primary=true) {pin.saveEnabled(true);pin.pin(game);close()}
            }
        }
    }
}
