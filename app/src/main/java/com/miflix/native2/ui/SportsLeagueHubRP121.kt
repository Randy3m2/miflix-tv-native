package com.miflix.native2.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.miflix.native2.data.*
import kotlinx.coroutines.*
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Public sports browsing stays outside the player. No playback or account state in caches. */
@Composable
internal fun SportsLeagueHubRP121(state:AppState,league:String,modifier:Modifier,games:@Composable (String)->Unit) {
    val owner="${state.session?.userId ?: "local"}:${state.activeProfile.id}"
    val saved=remember(owner,league) {SportsBrowseMemoryRP121.get(owner,league)}
    var tab by rememberSaveable(owner,league) {mutableStateOf(saved.first)}
    var day by rememberSaveable(owner,league) {mutableStateOf(saved.second)}
    val latest by rememberUpdatedState(tab to day)
    DisposableEffect(owner,league) {onDispose {SportsBrowseMemoryRP121.put(owner,league,latest)}}
    val tabs=rememberSaveableStateHolder()
    val context=LocalContext.current.applicationContext
    val revision by CalendarStoreRP120.revision.collectAsState()
    val followed=remember(revision,owner,league) {"$league:*" in CalendarStoreRP120.subscriptions(context,owner)}
    var subscriptions by remember {mutableStateOf(false)}
    if(subscriptions) CalendarSubscriptionsRP120(state,initialLeagueRP121=league) {subscriptions=false}
    Column(modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp),verticalAlignment=Alignment.CenterVertically) {
            listOf("games" to tr("Juegos","Games"),"table" to tr("Posiciones","Table"),"stats" to tr("Estadísticas","Stats"),"players" to tr(if(league=="F1") "Pilotos" else "Jugadores",if(league=="F1") "Drivers" else "Players")).forEach {(key,label) ->
                FocusButton(label,primary=tab==key) {tab=key}
            }
            Spacer(Modifier.weight(1f))
            FocusButton(if(followed) tr("✓ Siguiendo","✓ Following") else tr("+ Seguir","+ Follow"),primary=followed) {subscriptions=true}
        }
        if(tab=="games" && league!="LIDOM") Row(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically) {
            FocusButton("‹",horizontalPadding=14.dp) {day=LocalDate.parse(day).minusDays(1).toString()}
            FocusButton(tr("Hoy","Today"),primary=day==LocalDate.now().toString()) {day=LocalDate.now().toString()}
            FocusButton("›",horizontalPadding=14.dp) {day=LocalDate.parse(day).plusDays(1).toString()}
            Text(day,color=Color.White,fontSize=14.sp)
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            tabs.SaveableStateProvider(tab) {when(tab) {
                "games"->games(day)
                "table"->StandingsScreenRP120(close={},initialLeagueRP121=league,embeddedRP121=true)
                "stats"->SportsSeasonStatsRP121(league)
                "players"->SportsPlayersRP121(league)
            }}
        }
    }
}

@Composable
private fun SportsPortraitRP121(person:SportsPersonRP121,size:androidx.compose.ui.unit.Dp=38.dp) {
    val context=LocalContext.current
    var failed by remember(person.photo) {mutableStateOf(false)}
    val pixels=with(androidx.compose.ui.platform.LocalDensity.current) {size.roundToPx()}.coerceIn(96,384)
    val model=remember(person.photo,pixels) {ImageRequest.Builder(context).data(person.photo).size(pixels,pixels).build()}
    if(person.photo.isBlank() || failed) EventLogoRP120(person.logo.ifBlank {CalendarSourcesRP120.logo(person.league)},person.name,Modifier.size(size))
    else AsyncImage(model,person.name,Modifier.size(size).clip(CircleShape).background(Color.White.copy(alpha=.08f)),contentScale=ContentScale.Crop,onError={failed=true})
}

@Composable
private fun SportsPersonDetailsRP121(person:SportsPersonRP121,close:()->Unit) {
    PlayerSideSheet(person.league,close) {
        item {Row(horizontalArrangement=Arrangement.spacedBy(16.dp),verticalAlignment=Alignment.CenterVertically) {
            SportsPortraitRP121(person,80.dp)
            Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {
                Text(person.name,color=Color.White,fontSize=23.sp,fontWeight=FontWeight.Bold)
                Text(person.team.ifBlank {person.league},color=Muted,fontSize=14.sp)
                if(person.position.isNotBlank()) Text(person.position,color=Color.White,fontSize=13.sp)
            }
        }}
        item {Text(tr("Información publicada por la fuente deportiva.","Information published by the sports data source."),color=Muted,fontSize=12.sp)}
    }
}

@Composable
private fun SportsSeasonStatsRP121(league:String) {
    val context=LocalContext.current.applicationContext
    var data by remember(league) {mutableStateOf<SportsSnapshotRP121<List<SportsLeadersRP121>>?>(null)}
    var retry by remember(league) {mutableIntStateOf(0)}
    var loading by remember {mutableStateOf(true)}
    var error by remember {mutableStateOf(false)}
    var person by remember {mutableStateOf<SportsPersonRP121?>(null)}
    LaunchedEffect(league,retry) {
        loading=true;error=false
        try {data=withContext(Dispatchers.IO) {SportsBrowseRepositoryRP121.leaders(context,league,retry>0)}}
        catch(e:CancellationException) {throw e} catch(_:Exception) {error=true}
        finally {loading=false}
    }
    Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(tr("Líderes de temporada","Season leaders"),color=Color.White,fontSize=20.sp,fontWeight=FontWeight.Bold)
                Text(seasonLabelRP121(league),color=Muted,fontSize=12.sp)
            }
            FocusButton(tr("Actualizar","Refresh")) {if(!loading) retry++}
        }
        SportsDataStatusRP121(loading,error,data?.stale==true,data?.updated)
        val rows=data?.rows.orEmpty()
        if(!loading && !error && rows.isEmpty()) Text(tr("La fuente aún no publica líderes para esta temporada.","The source has not published season leaders yet."),color=Muted)
        LazyVerticalGrid(GridCells.Adaptive(290.dp),Modifier.weight(1f).fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(14.dp),verticalArrangement=Arrangement.spacedBy(14.dp),contentPadding=PaddingValues(4.dp,4.dp,4.dp,24.dp)) {
            gridItems(rows,key={it.key}) {group ->
                Column(Modifier.fillMaxWidth().focusGroup().background(Color.White.copy(alpha=.035f),RoundedCornerShape(16.dp)).border(1.dp,Color.White.copy(alpha=.10f),RoundedCornerShape(16.dp)).padding(10.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    Row(Modifier.fillMaxWidth().padding(horizontal=7.dp,vertical=7.dp)) {
                        Text(statTitleRP121(group.key,group.title),color=Color.White,fontSize=16.sp,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f),maxLines=2)
                        Text(group.unit,color=Muted,fontSize=11.sp)
                    }
                    group.rows.forEach {row ->
                        var focused by remember(row.person.id) {mutableStateOf(false)}
                        Row(Modifier.fillMaxWidth().heightIn(min=48.dp).clip(RoundedCornerShape(10.dp)).background(GlassActionBrushRP108(focused))
                            .onFocusChanged {focused=it.isFocused}.focusable().tvClick {person=row.person}.clickable {person=row.person}.padding(7.dp),
                            horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically) {
                            Text(row.rank,color=Muted,fontSize=11.sp,modifier=Modifier.width(15.dp))
                            SportsPortraitRP121(row.person,34.dp)
                            Column(Modifier.weight(1f)) {
                                Text(row.person.name,color=Color.White,fontSize=13.sp,fontWeight=FontWeight.Bold,maxLines=1,overflow=TextOverflow.Ellipsis)
                                Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                                    if(row.person.logo.isNotBlank()) EventLogoRP120(row.person.logo,row.person.team,Modifier.size(12.dp))
                                    Text(row.person.team,color=Muted,fontSize=10.sp,maxLines=1,overflow=TextOverflow.Ellipsis,modifier=Modifier.weight(1f))
                                }
                            }
                            Text(row.value,color=Color.White,fontSize=15.sp,fontWeight=FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
    person?.let {p ->SportsPersonDetailsRP121(p) {person=null}}
}

@Composable
private fun SportsPlayersRP121(league:String) {
    val context=LocalContext.current.applicationContext
    var teamId by rememberSaveable(league) {mutableStateOf("")}
    var query by rememberSaveable(league) {mutableStateOf("")}
    var count by remember(league) {mutableIntStateOf(40)}
    var choosing by remember {mutableStateOf(false)}
    var data by remember(league) {mutableStateOf<SportsSnapshotRP121<List<SportsPersonRP121>>?>(null)}
    var visible by remember {mutableStateOf<List<SportsPersonRP121>>(emptyList())}
    var visibleFor by remember {mutableStateOf<SportsSnapshotRP121<List<SportsPersonRP121>>?>(null)}
    var publishedQuery by remember {mutableStateOf("")}
    var loading by remember {mutableStateOf(true)}
    var error by remember {mutableStateOf(false)}
    var retry by remember {mutableIntStateOf(0)}
    var person by remember {mutableStateOf<SportsPersonRP121?>(null)}
    val teams by produceState<List<CalendarTeamRP120>>(emptyList(),league) {value=withContext(Dispatchers.IO) {CalendarSourcesRP120.teams(context).filter {it.league==league}}}
    LaunchedEffect(league,teamId,retry) {
        loading=true;error=false
        try {data=withContext(Dispatchers.IO) {SportsBrowseRepositoryRP121.players(context,league,teamId,retry>0)}}
        catch(e:CancellationException) {throw e} catch(_:Exception) {error=true;data=null}
        finally {loading=false}
    }
    val index by produceState<SportsPlayerIndexRP121?>(null,data) {
        val source=data
        value=if(source==null) null else withContext(Dispatchers.Default) {SportsPlayerIndexRP121(source,source.rows.map {it to SportsBrowsePolicyRP121.key(it.name+" "+it.team)})}
    }
    LaunchedEffect(index,query,teamId) {
        val source=index ?: return@LaunchedEffect
        val term=query
        delay(100)
        visible=withContext(Dispatchers.Default) {val words=SportsBrowsePolicyRP121.key(term).split(" ").filter {it.isNotBlank()};source.rows.filter {(_,name) ->words.all {it in name}}.map {it.first}}
        visibleFor=source.source;publishedQuery=term;count=40
    }
    val displayReady=visibleFor===data && publishedQuery==query
    Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp),verticalAlignment=Alignment.CenterVertically) {
            Text(tr(if(league=="F1") "Pilotos" else "Jugadores activos",if(league=="F1") "Drivers" else "Active players"),color=Color.White,fontSize=20.sp,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f))
            FocusButton(teams.firstOrNull {it.id==teamId}?.name ?: tr("Todos los equipos","All teams")) {choosing=true}
            FocusButton(tr("Actualizar","Refresh")) {if(!loading) retry++}
        }
        NativeTextField(query,{query=it},tr("Buscar jugador","Find player"),modifier=Modifier.fillMaxWidth())
        Text(if(league=="F1") tr("Última parrilla publicada · destacados primero","Last published race grid · featured first") else tr("Destacados primero · luego orden alfabético","Featured first · then alphabetical order"),color=Muted,fontSize=11.sp)
        SportsDataStatusRP121(loading,error,data?.stale==true,data?.updated)
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if(!loading && !error && displayReady) LazyVerticalGrid(GridCells.Adaptive(155.dp),Modifier.fillMaxSize(),horizontalArrangement=Arrangement.spacedBy(12.dp),verticalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(4.dp,4.dp,4.dp,24.dp)) {
                gridItems(visible.take(count),key={it.id}) {p ->
                    var focused by remember(p.id) {mutableStateOf(false)}
                    Column(Modifier.fillMaxWidth().height(174.dp).clip(RoundedCornerShape(15.dp)).background(GlassActionBrushRP108(focused))
                        .border(if(focused) 2.dp else 1.dp,Color.White.copy(alpha=if(focused) .8f else .12f),RoundedCornerShape(15.dp))
                        .onFocusChanged {focused=it.isFocused}.focusable().tvClick {person=p}.clickable {person=p}.padding(12.dp),
                        horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(6.dp)) {
                        SportsPortraitRP121(p,80.dp)
                        Text(p.name,color=Color.White,fontSize=14.sp,fontWeight=FontWeight.Bold,maxLines=2,overflow=TextOverflow.Ellipsis)
                        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                            EventLogoRP120(p.logo.ifBlank {CalendarSourcesRP120.logo(p.league)},p.team.ifBlank {p.league},Modifier.size(12.dp))
                            Text(p.team.ifBlank {p.league},color=Muted,fontSize=10.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
                        }
                    }
                }
                if(visible.size>count) item(span={GridItemSpan(maxLineSpan)}) {FocusButton(tr("Cargar 40 más","Load 40 more")) {count+=40}}
            }
            if(!loading && !error && !displayReady) Text(tr("Filtrando…","Filtering…"),color=Muted)
            if(!loading && !error && displayReady && visible.isEmpty()) Text(tr("No hay jugadores publicados para este filtro.","No published players for this filter."),color=Muted)
        }
    }
    if(choosing) PlayerSideSheet(tr("Equipos","Teams"),{choosing=false}) {
        item {FocusButton(tr("Todos","All")) {teamId="";retry=0;choosing=false}}
        items(teams,key={it.id}) {team ->Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            EventLogoRP120(team.logo,team.name,Modifier.size(38.dp))
            FocusButton(team.name,Modifier.weight(1f),primary=team.id==teamId) {teamId=team.id;retry=0;choosing=false}
        }}
    }
    person?.let {p ->SportsPersonDetailsRP121(p) {person=null}}
}

@Composable
private fun SportsDataStatusRP121(loading:Boolean,error:Boolean,stale:Boolean,updated:Long?) {
    if(loading || error || stale) Text(when {loading->tr("Cargando…","Loading…");error->tr("No se pudo cargar. Usa Actualizar para reintentar.","Could not load. Use Refresh to try again.");else->tr("Sin conexión · última versión guardada","Offline · last saved version")},color=Muted,fontSize=12.sp)
    else if(updated!=null) Text(tr("Actualizado ","Updated ")+DateTimeFormatter.ofPattern("h:mm a").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(updated)),color=Muted,fontSize=10.sp)
}
private fun seasonLabelRP121(league:String):String {val year=SportsBrowseRepositoryRP121.season(league);return when(league) {"NBA"->"${year-1}–$year";"LIDOM"->"$year–${year+1}";else->year.toString()}}
private fun statTitleRP121(key:String,fallback:String)=when(key) {
    "pointsPerGame"->tr("Puntos por partido","Points per game");"reboundsPerGame"->tr("Rebotes por partido","Rebounds per game");"assistsPerGame"->tr("Asistencias por partido","Assists per game")
    "fieldGoalPercentage"->tr("Porcentaje de campo","Field goal percentage");"3PointsMadePerGame"->tr("Triples por partido","Threes per game");"3PointPct"->tr("Porcentaje de triples","Three-point percentage")
    "blocksPerGame"->tr("Tapones por partido","Blocks per game");"stealsPerGame"->tr("Robos por partido","Steals per game")
    "hitting:battingAverage"->tr("Promedio de bateo","Batting average");"hitting:homeRuns"->tr("Jonrones","Home runs");"hitting:runsBattedIn"->tr("Carreras impulsadas","Runs batted in");"hitting:stolenBases"->tr("Bases robadas","Stolen bases");"hitting:hits"->"Hits"
    "pitching:earnedRunAverage"->tr("Efectividad","Earned run average");"pitching:strikeouts"->tr("Ponches","Strikeouts");"pitching:wins"->tr("Victorias","Wins");"pitching:whip"->"WHIP"
    "f1:points"->tr("Puntos de pilotos","Driver points");"f1:wins"->tr("Victorias de pilotos","Driver wins");else->fallback
}

private object SportsBrowseMemoryRP121 {
    private val rows=linkedMapOf<String,Pair<String,String>>()
    fun get(owner:String,league:String)=rows["$owner:$league"] ?: ("games" to LocalDate.now().toString())
    fun put(owner:String,league:String,value:Pair<String,String>) {
        val key="$owner:$league";rows.remove(key);rows[key]=value
        while(rows.size>24) rows.remove(rows.keys.first())
    }
}

private data class SportsPlayerIndexRP121(val source:SportsSnapshotRP121<List<SportsPersonRP121>>,val rows:List<Pair<SportsPersonRP121,String>>)
