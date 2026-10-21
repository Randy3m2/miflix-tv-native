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
import androidx.compose.ui.focus.onFocusChanged
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
import androidx.compose.ui.window.Dialog
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
    var interacting by remember(owner) {mutableStateOf(false)}
    var actions by remember(owner) {mutableStateOf(false)}
    val visible=state.appForeground && !state.kidsMode && state.screen !in setOf(Screen.PROFILES,Screen.SPLASH)
    LaunchedEffect(owner) {state.showLiveScoresRP11=false;moving=false}
    val current=pin.pinned
    LaunchedEffect(visible,pin.enabled,current?.id) {
        if(!visible || !pin.enabled || current==null) {actions=false;interacting=false;moving=false}
    }
    DisposableEffect(owner,visible,pin.enabled,current?.id) {
        val callback:(() -> Unit)?=if(visible && pin.enabled && current!=null) ({moving=false;interacting=true;actions=true}) else null
        state.focusScorePinRP15=callback
        onDispose {if(state.focusScorePinRP15===callback) state.focusScorePinRP15=null}
    }
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
        val width=minOf(156.dp,maxWidth-8.dp)
        val height=68.dp
        val maxX=with(density) {(maxWidth-width-8.dp).toPx()}.coerceAtLeast(0f)
        val maxY=with(density) {(maxHeight-height-8.dp).toPx()}.coerceAtLeast(0f)
        val inset=with(density) {4.dp.toPx()}
        val focus=remember {FocusRequester()}
        Popup(alignment=Alignment.TopStart,
            offset=IntOffset((inset+pin.x*maxX).roundToInt(),(inset+pin.y*maxY).roundToInt()),
            onDismissRequest={moving=false;interacting=false;pin.savePosition()},
            properties=PopupProperties(focusable=moving || interacting,dismissOnBackPress=moving || interacting)) {
            LaunchedEffect(moving,interacting,actions) {if((moving || interacting) && !actions) {withFrameNanos {};runCatching {focus.requestFocus()}}}
            Column(Modifier.consumeOpeningPress().width(width).focusRequester(focus).onPreviewKeyEvent {event ->
                if(!moving && !interacting) false else {
                    val key=event.nativeKeyEvent
                    val supported=key.keyCode in listOf(KeyEvent.KEYCODE_DPAD_LEFT,KeyEvent.KEYCODE_DPAD_RIGHT,
                        KeyEvent.KEYCODE_DPAD_UP,KeyEvent.KEYCODE_DPAD_DOWN,KeyEvent.KEYCODE_DPAD_CENTER,
                        KeyEvent.KEYCODE_ENTER,KeyEvent.KEYCODE_BACK)
                    val arrow=key.keyCode in listOf(KeyEvent.KEYCODE_DPAD_LEFT,KeyEvent.KEYCODE_DPAD_RIGHT,KeyEvent.KEYCODE_DPAD_UP,KeyEvent.KEYCODE_DPAD_DOWN)
                    if(supported && ((arrow && key.action==KeyEvent.ACTION_DOWN) || (!arrow && key.action==KeyEvent.ACTION_UP))) {
                        when(key.keyCode) {
                            KeyEvent.KEYCODE_DPAD_LEFT->if(moving) {pin.x=ScorePolicyRP11.coordinate(pin.x-.025f)}
                            KeyEvent.KEYCODE_DPAD_RIGHT->if(moving) {pin.x=ScorePolicyRP11.coordinate(pin.x+.025f)}
                            KeyEvent.KEYCODE_DPAD_UP->if(moving) {pin.y=ScorePolicyRP11.coordinate(pin.y-.025f)}
                            KeyEvent.KEYCODE_DPAD_DOWN->if(moving) {pin.y=ScorePolicyRP11.coordinate(pin.y+.025f)}
                            KeyEvent.KEYCODE_DPAD_CENTER,KeyEvent.KEYCODE_ENTER->if(moving) {moving=false;interacting=false;pin.savePosition()} else {actions=true}
                            else->{moving=false;interacting=false;pin.savePosition()}
                        }
                    }
                    supported
                }
            }.focusable(enabled=moving || interacting).pointerInput(owner,maxX,maxY) {
                detectDragGestures(onDragEnd={pin.savePosition()},onDragCancel={pin.savePosition()}) {change,drag ->
                    change.consume()
                    if(maxX>0) pin.x=ScorePolicyRP11.coordinate(pin.x+drag.x/maxX)
                    if(maxY>0) pin.y=ScorePolicyRP11.coordinate(pin.y+drag.y/maxY)
                }
            }) {
                Column(Modifier.fillMaxWidth().height(40.dp)
                    .background(Color(0xE6101010),RoundedCornerShape(36.dp))
                    .border(if(moving || interacting) 2.dp else 1.dp,if(moving || interacting) Color.White else Color(0xFF444444),RoundedCornerShape(36.dp))
                    .clickable {interacting=true;actions=true}.padding(horizontal=6.dp,vertical=3.dp),
                    horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(1.dp,Alignment.CenterVertically)) {
                    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                        ScoreTeamLogoRP11(current.away,current.league,18)
                        Text(current.away.score+" – "+current.home.score,color=Color.White,fontSize=16.sp,lineHeight=18.sp,fontWeight=FontWeight.Bold)
                        ScoreTeamLogoRP11(current.home,current.league,18)
                    }
                    val live=if(current.state=="live") tr("EN VIVO","LIVE")+" · " else ""
                    Text(current.league+" · "+live+scorePhaseRP11(current)+(if(pin.stale) tr(" · Sin actualizar"," · Update delayed") else ""),
                        color=if(pin.stale) Color(0xFFFFD287) else Color(0xFFBDBDBD),fontSize=9.sp,lineHeight=11.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
                }
                if(interacting && !moving) Text(tr("OK · Opciones    Atrás · Salir","OK · Options    Back · Exit"),color=Color.White,fontSize=10.sp,lineHeight=12.sp,
                    modifier=Modifier.background(Color(0xDC101010),RoundedCornerShape(12.dp)).padding(6.dp))
                if(moving) Text(tr("Flechas · Mover     OK / Atrás · Guardar","Arrows · Move     OK / Back · Save"),
                    color=Color.White,fontSize=10.sp,lineHeight=12.sp,modifier=Modifier.background(Color(0xDC101010),RoundedCornerShape(12.dp)).padding(6.dp))
            }
        }
    }
    if(actions && current!=null && visible) {
        val unpinFocus=remember {FocusRequester()}
        Dialog(onDismissRequest={actions=false}) {
            Column(Modifier.consumeOpeningPress().width(300.dp).background(Panel,RoundedCornerShape(22.dp)).padding(22.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Text("Live Scores",color=Color.White,fontSize=22.sp,fontWeight=FontWeight.Bold)
                FocusButton(tr("Quitar marcador · Unpin","Unpin"),modifier=Modifier.focusRequester(unpinFocus)) {pin.pin(null);actions=false;interacting=false;moving=false}
                FocusButton(tr("Mover marcador","Move score")) {actions=false;interacting=false;moving=true}
                FocusButton(tr("Ver partidos","Browse games")) {actions=false;interacting=false;state.showLiveScoresRP11=true}
                FocusButton(tr("Cerrar","Close")) {actions=false}
            }
            LaunchedEffect(Unit) {withFrameNanos {};runCatching {unpinFocus.requestFocus()}}
        }
    }
    if(state.showLiveScoresRP11 && !state.kidsMode) ScorePinManagerRP11(state,pin,onMove={
        state.showLiveScoresRP11=false;interacting=false;moving=true
    },close={state.showLiveScoresRP11=false})
}

@Composable
private fun ScoreTeamLogoRP11(team:ScoreTeamRP11,league:String,size:Int=24) {
    val context=LocalContext.current
    val local=remember(team.id,league) {
        if(league!="LIDOM") 0 else runCatching {
            val teams=JSONObject(context.assets.open("lidom_teams.json").bufferedReader().use {it.readText()}).getJSONArray("teams")
            val item=(0 until teams.length()).map {teams.getJSONObject(it)}.firstOrNull {it.optInt("mlb").toString()==team.id}
            context.resources.getIdentifier(item?.optString("logo").orEmpty(),"drawable",context.packageName)
        }.getOrDefault(0)
    }
    if(local!=0) Image(painterResource(local),team.name,Modifier.size(size.dp))
    else if(team.logo.isNotBlank()) AsyncImage(team.logo,team.name,Modifier.size(size.dp))
    else Text(team.short.take(2),color=Color.White,fontSize=11.sp,modifier=Modifier.width(size.dp))
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
    PlayerSideSheet("Live Scores",close) {
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
                listOf("NBA","MLB","LIDOM").forEach {l ->ScoreLeagueButtonRP17(l,league==l) {league=l}}
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
                val isPinned=pin.pinned?.let {it.id==game.id && it.league==game.league}==true
                FocusButton(if(isPinned) tr("Quitar marcador · Unpin","Unpin") else tr("Fijar marcador · Pin","Pin live score"),primary=!isPinned) {
                    if(isPinned) pin.pin(null) else {pin.saveEnabled(true);pin.pin(game)}
                    close()
                }
            }
        }
    }
}

@Composable
private fun ScoreLeagueButtonRP17(league:String,selected:Boolean,onClick:()->Unit) {
    var focused by remember {mutableStateOf(false)}
    val active=actionFocusVisibleRP64(focused)
    val logo=when(league) {
        "NBA"->com.miflix.native2.R.drawable.score_league_nba_rp17
        "MLB"->com.miflix.native2.R.drawable.score_league_mlb_rp17
        else->com.miflix.native2.R.drawable.score_league_lidom_rp17
    }
    Column(Modifier.onFocusChanged {focused=it.isFocused}.focusable().tvClick(onClick).clickable(onClick=onClick)
        .width(88.dp).height(80.dp).background(if(active) Color.White else Color(0xFF141414),RoundedCornerShape(16.dp))
        .border(if(focused) 2.dp else 1.dp,if(focused) Color.White else if(selected) Color(0xFFAAAAAA) else Color(0xFF343434),RoundedCornerShape(16.dp))
        .padding(8.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(6.dp,Alignment.CenterVertically)) {
        Image(painterResource(logo),league,Modifier.width(64.dp).height(36.dp),contentScale=androidx.compose.ui.layout.ContentScale.Fit)
        Text(league,color=if(active) Color.Black else if(selected) Color.White else Muted,fontSize=14.sp,lineHeight=16.sp,fontWeight=FontWeight.Bold)
    }
}
