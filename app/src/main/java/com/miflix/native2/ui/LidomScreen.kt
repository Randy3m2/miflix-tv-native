package com.miflix.native2.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.focusable
import androidx.compose.foundation.border
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Text
import com.miflix.native2.data.*
import com.miflix.native2.model.MediaSummary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
internal fun LidomScreen(state:AppState) {
    val context=LocalContext.current
    val owner="${state.session?.userId ?: "local"}:${state.activeProfile.id}"
    val repo=remember(owner) {LidomRepository(context,owner)}
    val scope=rememberCoroutineScope()
    var revision by remember {mutableIntStateOf(0)}
    var refresh by remember {mutableIntStateOf(0)}
    var date by remember {mutableStateOf(LidomRepository.today())}
    var games by remember {mutableStateOf<List<LidomGame>>(emptyList())}
    var loading by remember {mutableStateOf(false)}
    var message by remember {mutableStateOf("")}
    var onlyFavorites by remember(owner) {mutableStateOf(false)}
    val grid=androidx.compose.foundation.lazy.grid.rememberLazyGridState()
    var configure by remember(owner) {mutableStateOf(state.lidomConfigure)}
    var team by remember(owner) {mutableStateOf(repo.team(state.lidomTeamId).takeIf {state.lidomShowSources})}
    LaunchedEffect(Unit) {state.lidomShowSources=false;state.lidomConfigure=false}
    LaunchedEffect(date,refresh) {
        loading=true;message=""
        try {games=repo.games(date)} catch(e:CancellationException) {throw e} catch(_:Exception) {games=emptyList();message=tr("Calendario no disponible. Los equipos y sus fuentes siguen disponibles.","Schedule unavailable. Teams and sources remain available.")}
        finally {loading=false}
    }
    BackHandler {state.screen=com.miflix.native2.ui.Screen.LIVE_TV}
    AppShell(state,Screen.LIVE_TV,{state.screen=it}) {
        LazyVerticalGrid(GridCells.Adaptive(240.dp),Modifier.fillMaxSize().padding(horizontal=32.dp).smoothVerticalDpad { !grid.isScrollInProgress },state=grid,contentPadding=PaddingValues(vertical=18.dp),horizontalArrangement=Arrangement.spacedBy(16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
            item(span={GridItemSpan(maxLineSpan)}) {Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text("BRUNIO SPORTS",color=Accent,fontSize=13.sp,fontWeight=FontWeight.Bold)
                Text("LIDOM 2026–27",color=Color.White,fontSize=32.sp,fontWeight=FontWeight.Black)
                Text(tr("6 equipos · Fuentes por equipo y partido","6 teams · Sources by team and game"),color=Muted,fontSize=14.sp)
                Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                    FocusButton(if(onlyFavorites) tr("Todos los equipos","All teams") else tr("★ Favoritos","★ Favorites")) {onlyFavorites=!onlyFavorites}
                    FocusButton(tr("Actualizar partidos","Refresh games")) {refresh++}
                    FocusButton("PIN Live score") {state.showLiveScoresRP11=true}
                    FocusButton("SplitScreen") {
                        state.launch {
                            state.leaveWatchParty()
                            state.miniPlayer=false;state.playerRequest=null
                            state.multiViewRequests.clear();state.screen=Screen.MULTIVIEW
                        }
                    }
                    FocusButton(tr("Configurar fuentes","Source settings")) {configure=true}
                    FocusButton(tr("Volver","Back")) {state.screen=Screen.LIVE_TV}
                }
            }}
            item(span={GridItemSpan(maxLineSpan)}) {Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text(tr("Partidos","Games")+" · "+date+" · AST",color=Color.White,fontSize=20.sp)
                Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                    FocusButton("‹ "+tr("Anterior","Previous")) {date=lidomMoveDate(date,-1)}
                    FocusButton(tr("Hoy","Today")) {date=LidomRepository.today()}
                    FocusButton(tr("Siguiente","Next")+" ›") {date=lidomMoveDate(date,1)}
                }
                if(loading) Text(tr("Cargando calendario…","Loading schedule…"),color=Muted)
                if(message.isNotBlank()) Text(message,color=Muted)
                if(!loading && message.isBlank() && games.isEmpty()) Text(tr("No hay partidos publicados para esta fecha.","No published games for this date."),color=Muted)
                Text(tr("El estado del partido no confirma qué canal lo transmite. Las fuentes iniciales corresponden al equipo local.","Game status does not confirm a broadcast. Game selection opens the home team's sources."),color=Muted,fontSize=12.sp)
            }}
            gridItems(games,key={"game:${it.id}"},span={GridItemSpan(maxLineSpan)}) {game ->
                LidomGameCard(game) {team=game.home;state.lidomTeamId=game.home.id}
            }
            val visible=revision.let {repo.teams.filter {!onlyFavorites || repo.favorite(it.id)}}
            if(visible.isEmpty()) item(span={GridItemSpan(maxLineSpan)}) {Text(tr("Agrega equipos a favoritos.","Add teams to favorites."),color=Muted)}
            gridItems(visible,key={it.id}) {club ->
                Column(Modifier.fillMaxWidth().background(Panel,RoundedCornerShape(20.dp)).padding(16.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(10.dp)) {
                    val drawable=context.resources.getIdentifier(club.logo,"drawable",context.packageName)
                    if(drawable!=0) Image(painterResource(drawable),club.name,Modifier.size(82.dp))
                    Text(club.short,color=Color.White,fontSize=23.sp,fontWeight=FontWeight.Bold)
                    FocusButton(tr("Ver fuentes","Choose source"),modifier=Modifier.fillMaxWidth(),primary=true,onLongClick={repo.toggleFavorite(club.id);revision++}) {team=club;state.lidomTeamId=club.id}
                    FocusButton(if(repo.favorite(club.id)) tr("★ Quitar favorito","★ Remove favorite") else tr("☆ Agregar favorito","☆ Add to favorites"),modifier=Modifier.fillMaxWidth()) {repo.toggleFavorite(club.id);revision++}
                }
            }
        }
    }
    key(owner) {
    team?.let {club -> LidomSourcesSheet(state,repo,club,{team=null},{team=null;configure=true})}
    if(configure) LidomSourcesSettings(repo,state.lidomTeamId,{configure=false;revision++})
    }
}

@Composable
private fun LidomGameCard(game:LidomGame,onClick:()->Unit) {
    var focused by remember(game.id) {mutableStateOf(false)}
    Column(Modifier.fillMaxWidth().onFocusChanged {focused=it.isFocused}.focusable().tvClick(onClick)
        .background(if(focused) FocusedPanel else Panel,RoundedCornerShape(20.dp))
        .border(if(focused) 2.dp else 1.dp,if(focused) Accent else PanelBorder,RoundedCornerShape(20.dp))
        .padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(18.dp)) {
            LidomGameTeam(game.away,Modifier.weight(1f))
            Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(4.dp)) {
                Text(game.time.ifBlank {tr("Hora por confirmar","Time TBD")},color=Color.White,fontSize=22.sp,fontWeight=FontWeight.Bold)
                Text("AST · UTC−4",color=Muted,fontSize=12.sp)
                Text("VS",color=Accent,fontSize=14.sp,fontWeight=FontWeight.Bold)
            }
            LidomGameTeam(game.home,Modifier.weight(1f))
        }
        Text(tr("Transmisión anunciada: ","Listed broadcast: ")+
            game.broadcasts.joinToString(" · ").ifBlank {tr("Por confirmar","To be confirmed")},color=Color.White,fontSize=15.sp)
        Text(game.status+" · "+tr("Seleccionar fuentes del equipo local","Select home team sources"),color=Muted,fontSize=12.sp)
        if(game.broadcasts.any {it.contains("MLB.TV",ignoreCase=true)})
            Text(tr("MLB.TV puede requerir suscripción y abrirse en su plataforma oficial.","MLB.TV may require a subscription and playback on its official platform."),color=Muted,fontSize=12.sp)
    }
}

@Composable
private fun LidomGameTeam(team:LidomTeam,modifier:Modifier) {
    val context=LocalContext.current
    val drawable=remember(team.logo) {context.resources.getIdentifier(team.logo,"drawable",context.packageName)}
    Column(modifier,horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(6.dp)) {
        if(drawable!=0) Image(painterResource(drawable),team.name,Modifier.size(64.dp))
        Text(team.short,color=Color.White,fontSize=20.sp,fontWeight=FontWeight.Bold)
    }
}

private fun lidomMoveDate(value:String,days:Int):String {
    val f=java.text.SimpleDateFormat("yyyy-MM-dd",java.util.Locale.US).apply {timeZone=java.util.TimeZone.getTimeZone("America/Santo_Domingo")}
    val c=java.util.Calendar.getInstance(f.timeZone).apply {time=f.parse(value) ?: java.util.Date();add(java.util.Calendar.DATE,days)}
    return f.format(c.time)
}

@Composable
internal fun LidomSourcesSheet(state:AppState,repo:LidomRepository,team:LidomTeam,close:()->Unit,settings:()->Unit,onSplitSelect:((PlayerRequest)->Unit)?=null) {
    var splitMode by remember {mutableStateOf(false)}
    val scope=rememberCoroutineScope();var busy by remember {mutableStateOf(false)};var error by remember {mutableStateOf("")}
    var selected by remember {mutableStateOf<LidomSource?>(null)};var candidates by remember {mutableStateOf<List<com.miflix.native2.model.StreamChoice>>(emptyList())}
    var youtube by remember {mutableStateOf<LidomSource?>(null)}
    var video by remember {mutableStateOf<LidomYoutubeSelection?>(null)}
    fun play(source:LidomSource,stream:com.miflix.native2.model.StreamChoice) {
        state.lidomTeamId=team.id;state.lidomShowSources=false
        val item=MediaSummary(team.mlb,"live",team.name+" · "+source.name,tr("Transmisión seleccionada por el usuario. La programación puede variar.","User-selected broadcast. Programming may vary."),null,null,0.0,"LIDOM")
        val request=PlayerRequest(item,stream,0,0,0,true,"lidom:${team.id}:${source.id}","lidom",liveType=if(source.kind=="YOUTUBE") "youtube" else "tv",liveMime=if(source.kind=="DASH") "application/dash+xml" else if(source.kind=="HLS") "application/x-mpegURL" else "",lidomTeam=team.id,lidomSource=source.id,returnScreen=Screen.LIDOM)
        if(onSplitSelect!=null) onSplitSelect(request)
        else if(splitMode) state.launch {state.leaveWatchParty();state.beginMultiView(request)}
        else state.openLivePlayer(request)
        close()
    }
    PlayerSideSheet(team.name+" · "+tr("Fuentes","Sources"),close) {
        item {Text(tr("YouTube primero, luego televisión. Ninguna fuente se presenta como un partido en vivo sin comprobarlo.","YouTube first, then TV. A channel listing does not confirm a live game broadcast."),color=Muted,fontSize=13.sp)}
        if(onSplitSelect==null) item {
            FocusButton(if(splitMode) tr("SplitScreen activado · elige una fuente","SplitScreen on · choose a source") else tr("Abrir en SplitScreen","Open in SplitScreen"),primary=splitMode) {splitMode=!splitMode}
        }
        val sources=repo.sources(team.id)
        if(sources.isEmpty()) item {Text(tr("No hay fuentes. Agrega una en configuración.","No sources. Add one in settings."),color=Muted)}
        items(sources,key={it.id}) {source ->
            FocusButton(source.name+(if(repo.last(team.id)==source.id) " · ✓ "+tr("Última funcional","Last working") else ""),modifier=Modifier.fillMaxWidth(),) {
                if(!busy) {
                    error="";candidates=emptyList();selected=source
                    if(source.kind=="YOUTUBE") youtube=source
                    else scope.launch {busy=true;try {
                        candidates=repo.resolve(source,state.calendarLiveManifest("nauta"))
                        if(candidates.isEmpty()) error=tr("No se encontró una señal compatible en tus canales. Agrega una URL autorizada en configuración o elige otra fuente.","No compatible signal found in your channels. Add an authorized URL in settings or try another source.")
                    } catch(e:CancellationException) {throw e} catch(_:Exception) {error=tr("No se pudo cargar. Prueba otra fuente.","Could not load. Try another source.")} finally {busy=false}}
                }
            }
            Text(source.note,color=Muted,fontSize=11.sp)
        }
        if(busy) item {Text(tr("Comprobando opciones…","Checking options…"),color=Muted)}
        if(error.isNotBlank()) item {Text(error,color=Color(0xFFFFC6C6));FocusButton(tr("Probar otra fuente","Try another source")) {error="";selected=null;candidates=emptyList()}}
        items(candidates.indices.toList(),key={"choice:$it"}) {index -> val stream=candidates[index];FocusButton("▶ ${index+1} · "+stream.name+" · "+stream.title,modifier=Modifier.fillMaxWidth()) {selected?.let {play(it,stream)}}}
        item {FocusButton(tr("Agregar / editar fuentes","Add / edit sources"),modifier=Modifier.fillMaxWidth(),onClick=settings)}
    }
    youtube?.let {source -> LidomYoutubeChooser(repo,source,onPlay={id ->
        youtube=null
        if(onSplitSelect!=null || splitMode) play(source,com.miflix.native2.model.StreamChoice("YouTube","https://www.youtube.com/watch?v=$id",source.name,addonName="LIDOM"))
        else video=LidomYoutubeSelection(team.id,source.id,id)
    },close={youtube=null})}
    video?.let {playing -> LidomYoutubePlayer(playing.video,onWorking={repo.working(team.id,playing.source,playing.video)},close={video=null})}
}

@Composable
internal fun LidomSourcesSettings(repo:LidomRepository,initialTeam:String,close:()->Unit) {
    var team by remember {mutableStateOf(repo.team(initialTeam) ?: repo.teams.first())};var revision by remember {mutableIntStateOf(0)}
    var editing by remember {mutableStateOf<LidomSource?>(null)}
    PlayerSideSheet(tr("LIDOM · Configuración","LIDOM · Source settings"),close) {
        item {Text(tr("URLs guardadas solo para este perfil en esta TV. Agrega señales que estés autorizado a reproducir.","URLs are stored for this profile on this TV. Add streams you are authorized to play."),color=Muted,fontSize=13.sp)}
        items(repo.teams,key={"team:${it.id}"}) {club ->FocusButton((if(team.id==club.id) "✓ " else "")+club.short) {team=club}}
        item {Text(team.name,color=Color.White,fontSize=21.sp);FocusButton(tr("+ Agregar fuente","+ Add source")) {editing=LidomSource(UUID.randomUUID().toString(),team.id,"","HLS","")}}
        items(repo.sources(team.id),key={it.id+revision}) {source ->
            Text(source.name+" · "+source.kind,color=Color.White,fontSize=16.sp)
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                FocusButton(tr("Editar","Edit")) {editing=source}
                FocusButton(tr("Eliminar","Delete"),destructive=true) {repo.delete(source);revision++}
            }
        }
    }
    editing?.let {source -> LidomSourceEditor(repo,source,close={editing=null;revision++})}
}

@Composable
private fun LidomSourceEditor(repo:LidomRepository,source:LidomSource,close:()->Unit) {
    var name by remember {mutableStateOf(source.name)};var url by remember {mutableStateOf(source.url)};var kind by remember {mutableStateOf(source.kind)};var error by remember {mutableStateOf("")}
    Dialog(onDismissRequest=close,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        LazyColumn(Modifier.consumeOpeningPress().widthIn(max=620.dp).fillMaxWidth(.8f).heightIn(max=490.dp).background(Panel,RoundedCornerShape(20.dp)).padding(22.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            item {Text(tr("Fuente autorizada","Authorized source"),color=Color.White,fontSize=22.sp)}
            item {NativeTextField(name,{name=it},tr("Nombre","Name"),modifier=Modifier.fillMaxWidth())}
            item {Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {(listOf("YOUTUBE","HLS","DASH")+if(source.aliases.isNotEmpty()) listOf("TV") else emptyList()).forEach {type ->FocusButton(type,primary=kind==type) {kind=type}}}}
            item {NativeTextField(url,{url=it},"URL",modifier=Modifier.fillMaxWidth())}
            if(error.isNotBlank()) item {Text(error,color=Color(0xFFFFC6C6))}
            item {FocusButton(tr("Guardar","Save")) {try {repo.save(source.copy(name=name.trim(),url=url.trim(),kind=kind));close()} catch(e:Exception) {error=e.message.orEmpty()}}}
            item {FocusButton(tr("Cancelar","Cancel"),onClick=close)}
        }
    }
}


@Composable
internal fun LidomMultiViewPickerRP9(state:AppState,initialTeam:String="",select:(PlayerRequest)->Unit,close:()->Unit) {
    val context=LocalContext.current
    val owner="${state.session?.userId ?: "local"}:${state.activeProfile.id}"
    val repo=remember(owner) {LidomRepository(context,owner)}
    var team by remember(owner,initialTeam) {mutableStateOf(repo.team(initialTeam))}
    var configuring by remember {mutableStateOf(false)}
    if(configuring) LidomSourcesSettings(repo,team?.id.orEmpty(),{configuring=false})
    else if(team==null) PlayerSideSheet(tr("SplitScreen · equipos LIDOM","SplitScreen · LIDOM teams"),close) {
        item {Text(tr("Elige el equipo y después la transmisión que quieres añadir.","Choose a team, then the broadcast to add."),color=Muted)}
        items(repo.teams,key={it.id}) {club ->FocusButton(club.name,modifier=Modifier.fillMaxWidth()) {team=club}}
    }
    else LidomSourcesSheet(state,repo,team!!,close,{configuring=true},onSplitSelect=select)
}
