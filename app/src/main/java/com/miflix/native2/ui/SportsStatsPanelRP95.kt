package com.miflix.native2.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.miflix.native2.data.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.isActive
import java.time.LocalDate

/** Composed only when open; no jobs or polling survive closing/backgrounding the panel. */
@Composable
internal fun SportsStatsPanelRP95(state:AppState,title:String,repo:SportsStatsRP95,selected:LiveScoreRP11?,
    select:(LiveScoreRP11)->Unit,onClose:()->Unit,videoFocus:FocusRequester,modifier:Modifier=Modifier) {
    var choosing by remember {mutableStateOf(selected==null)}
    var league by remember {mutableStateOf(selected?.league ?: "NBA")}
    var date by remember {mutableStateOf(selected?.date ?: LiveScoresRP11.today())}
    var games by remember {mutableStateOf<List<LiveScoreRP11>>(emptyList())}
    var gamesBusy by remember {mutableStateOf(false)}
    var gamesError by remember {mutableStateOf(false)}
    var reload by remember {mutableIntStateOf(0)}
    var autoMatched by remember {mutableStateOf(selected!=null)}
    var data by remember(selected?.league,selected?.id) {mutableStateOf<GameStatsRP95?>(null)}
    var stale by remember(selected?.league,selected?.id) {mutableStateOf(false)}
    var loading by remember(selected?.league,selected?.id) {mutableStateOf(false)}
    var tab by remember(selected?.league,selected?.id) {mutableIntStateOf(0)}
    var teamIndex by remember(selected?.league,selected?.id) {mutableIntStateOf(0)}
    val closeFocus=remember {FocusRequester()}
    LaunchedEffect(choosing,selected?.league,selected?.id) {withFrameNanos {};runCatching {closeFocus.requestFocus()}}
    LaunchedEffect(date,reload,choosing,state.appForeground) {
        if(!choosing || !state.appForeground) return@LaunchedEffect
        gamesBusy=true;gamesError=false
        try {
            val result=coroutineScope {
                listOf("NBA","MLB").map {sport ->async {
                    try {repo.games(sport,date)} catch(e:CancellationException) {throw e} catch(_:Exception) {gamesError=true;emptyList()}
                }}.flatMap {it.await()}
            }
            games=result
            if(!autoMatched) {
                autoMatched=true
                withContext(Dispatchers.Default) {SportsStatsRP95.match(title,result)}?.let {select(it);league=it.league;choosing=false}
            }
        } finally {gamesBusy=false}
    }
    LaunchedEffect(selected?.league,selected?.id,choosing,state.appForeground) {
        val game=selected ?: return@LaunchedEffect
        if(choosing || !state.appForeground) return@LaunchedEffect
        var failures=0
        while(isActive) {
            loading=data==null
            try {
                val result=repo.stats(game)
                data=result;stale=false;failures=0
                if(result.final) break
            } catch(e:CancellationException) {throw e} catch(_:Exception) {stale=true;failures++}
            finally {loading=false}
            delay(if(failures>0) (60_000L*failures.coerceAtMost(4)) else if(data?.live==false) 60_000L else 30_000L)
        }
    }
    Column(modifier.fillMaxHeight().background(Color(0xFF101010)).focusGroup().padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text(tr("Estadísticas","Live stats"),color=Color.White,fontSize=23.sp,fontWeight=FontWeight.Bold)
                Text(if(choosing) tr("Selecciona un partido","Select a game") else selected?.league.orEmpty(),color=Muted,fontSize=12.sp)
            }
            FocusButton("×",modifier=Modifier.focusRequester(closeFocus).focusProperties {left=videoFocus},horizontalPadding=14.dp,onClick=onClose)
        }
        if(choosing) {
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                for(sport in listOf("NBA","MLB")) StatsTabRP95(sport,league==sport) {league=sport}
            }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                FocusButton("‹",horizontalPadding=12.dp) {date=LocalDate.parse(date).minusDays(1).toString();autoMatched=true}
                Text(date,color=Color.White,fontSize=14.sp)
                FocusButton("›",horizontalPadding=12.dp) {date=LocalDate.parse(date).plusDays(1).toString();autoMatched=true}
            }
            if(gamesBusy) Text(tr("Buscando partidos…","Loading games…"),color=Muted,fontSize=13.sp)
            if(gamesError) Text(tr("No se pudieron cargar todos los partidos.","Some games could not be loaded."),color=Color(0xFFFFC28A),fontSize=12.sp)
            LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(top=6.dp,bottom=18.dp,start=3.dp,end=3.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                items(games.filter {it.league==league},key={"${it.league}:${it.id}"}) {game ->
                    StatsGameCardRP95(game,selected?.league==game.league && selected.id==game.id) {select(game);choosing=false}
                }
                if(!gamesBusy && games.none {it.league==league}) item {Text(tr("No hay partidos disponibles en esta fecha.","No games available for this date."),color=Muted,fontSize=14.sp)}
                item {FocusButton(tr("Actualizar lista","Refresh games"),modifier=Modifier.fillMaxWidth()) {reload++}}
                if(selected!=null) item {FocusButton(tr("Volver a estadísticas","Back to stats"),modifier=Modifier.fillMaxWidth()) {choosing=false}}
            }
        } else {
            val game=selected
            if(game!=null) {
                val current=data
                val tabs=if(game.league=="NBA") listOf(tr("Jugadores","Players"),tr("Equipos","Teams")) else listOf(tr("Bateo","Batting"),tr("Pitcheo","Pitching"),tr("Equipos","Teams"))
                // One scrolling column: score/header/rows cannot fight nested scrolling or clip on return.
                LazyColumn(Modifier.weight(1f).fillMaxWidth(),contentPadding=PaddingValues(top=6.dp,bottom=22.dp,start=3.dp,end=3.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                    item(key="score") {
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly,verticalAlignment=Alignment.CenterVertically) {
                            for((index,team) in listOf(game.away,game.home).withIndex()) Column(horizontalAlignment=Alignment.CenterHorizontally) {
                                AsyncImage(team.logo,null,Modifier.size(34.dp))
                                Text(team.short,color=Muted,fontSize=12.sp)
                                Text(current?.teams?.getOrNull(index)?.score ?: team.score,color=Color.White,fontSize=27.sp,fontWeight=FontWeight.Bold)
                            }
                        }
                    }
                    item(key="phase") {Text(current?.phase ?: game.phase,color=Color.White,fontSize=14.sp)}
                    item(key="game") {FocusButton(tr("Cambiar partido","Change game"),modifier=Modifier.fillMaxWidth()) {choosing=true;autoMatched=true;date=game.date;league=game.league}}
                    item(key="team-selector") {
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                            for((index,team) in listOf(game.away,game.home).withIndex()) Box(Modifier.weight(1f)) {
                                StatsTabRP95(team.short,teamIndex==index) {teamIndex=index}
                            }
                        }
                    }
                    item(key="tabs") {
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                            tabs.forEachIndexed {index,label ->Box(Modifier.weight(1f)) {StatsTabRP95(label,tab==index) {tab=index}}}
                        }
                    }
                    if(loading) item(key="loading") {Text(tr("Cargando estadísticas…","Loading stats…"),color=Muted,fontSize=13.sp)}
                    if(stale) item(key="stale") {Text(tr("Sin actualización. Reintentaremos automáticamente.","Update unavailable. Retrying automatically."),color=Color(0xFFFFC28A),fontSize=12.sp)}
                    if(current!=null) {
                        val teamTab=tab==tabs.lastIndex
                        val allGroups=if(game.league=="MLB" && tab==1) current.pitching else current.players
                        val teamName=current.teams.getOrNull(teamIndex)?.name.orEmpty()
                        val teamId=if(teamIndex==0) game.away.id else game.home.id
                        val groups=allGroups.filter {if(it.teamId.isNotBlank()) it.teamId==teamId else it.team==teamName}
                        if(teamTab) {
                            val keys=if(game.league=="NBA") listOf("fieldGoalPct" to "FG%","threePointFieldGoalPct" to "3P%","freeThrowPct" to "FT%","totalRebounds" to "REB","assists" to "AST","steals" to "STL","blocks" to "BLK","turnovers" to "TO")
                                else listOf("hits" to "H","homeRuns" to "HR","rbi" to "RBI","baseOnBalls" to "BB","strikeOuts" to "K","errors" to "E","inningsPitched" to "IP")
                            item(key="team-heading") {Text(teamName,color=Color.White,fontSize=16.sp,fontWeight=FontWeight.Bold)}
                            items(keys,key={it.first}) {(metric,label) ->StatsTeamMetricRP95(label,current.teams.getOrNull(teamIndex)?.values?.get(metric) ?: "—")}
                        } else {
                            for((groupIndex,group) in groups.withIndex()) {
                                item(key="team:$groupIndex") {Text(group.team,color=Color.White,fontSize=16.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=8.dp))}
                                items(group.rows,key={"$groupIndex:${it.id}"}) {row ->StatsPlayerRowRP95(group.columns,row)}
                            }
                            if(groups.all {it.rows.isEmpty()}) item(key="empty") {Text(tr("Las estadísticas aparecerán cuando estén disponibles.","Stats will appear when available."),color=Muted,fontSize=14.sp)}
                        }
                        item(key="disclaimer") {Text(tr("Datos en vivo; pueden adelantarse al vídeo.","Live data may be ahead of the video."),color=Muted,fontSize=10.sp)}
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsTabRP95(label:String,selected:Boolean,click:()->Unit) {
    FocusButton((if(selected) "• " else "")+label,horizontalPadding=10.dp,onClick=click)
}

@Composable
private fun StatsGameCardRP95(game:LiveScoreRP11,selected:Boolean,click:()->Unit) {
    var focused by remember {mutableStateOf(false)}
    Column(Modifier.fillMaxWidth().onFocusChanged {focused=it.isFocused}
        .background(if(focused) Color.White else Color(0xFF1B1B1B),RoundedCornerShape(12.dp))
        .clickable(onClick=click).padding(12.dp),verticalArrangement=Arrangement.spacedBy(5.dp)) {
        Text((if(selected) "✓ " else "")+game.away.short+" @ "+game.home.short,color=if(focused) Color.Black else Color.White,fontSize=17.sp,fontWeight=FontWeight.Bold)
        Text("${game.away.score} – ${game.home.score} · ${game.phase}",color=if(focused) Color.DarkGray else Muted,fontSize=13.sp)
    }
}

@Composable
private fun StatsPlayerRowRP95(columns:List<String>,row:StatsRowRP95) {
    var focused by remember(row.id) {mutableStateOf(false)}
    var expanded by remember(row.id) {mutableStateOf(false)}
    val indexes=if(expanded) columns.indices.toList() else {
        val preferred=if("PTS" in columns) listOf("MIN","PTS","REB","AST") else columns.take(4)
        preferred.mapNotNull {label ->columns.indexOf(label).takeIf {it>=0}}
    }
    Column(Modifier.fillMaxWidth().onFocusChanged {focused=it.isFocused}
        .background(if(focused) Color.White else Color(0xFF1B1B1B),RoundedCornerShape(10.dp))
        .clickable {expanded=!expanded}.padding(10.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text(row.name,color=if(focused) Color.Black else Color.White,fontSize=15.sp,maxLines=1,overflow=TextOverflow.Ellipsis,fontWeight=FontWeight.Bold)
        for(chunk in indexes.chunked(4)) Row(Modifier.fillMaxWidth()) {
            for(index in chunk) Column(Modifier.weight(1f)) {
                Text(columns[index],color=if(focused) Color.DarkGray else Muted,fontSize=10.sp)
                Text(row.values.getOrElse(index) {"—"},color=if(focused) Color.Black else Color.White,fontSize=15.sp)
            }
        }
    }
}

@Composable
private fun StatsTeamMetricRP95(label:String,value:String) {
    var focused by remember {mutableStateOf(false)}
    Row(Modifier.fillMaxWidth().onFocusChanged {focused=it.isFocused}.focusable()
        .background(if(focused) Color.White else Color(0xFF1B1B1B),RoundedCornerShape(10.dp)).padding(12.dp),horizontalArrangement=Arrangement.SpaceBetween) {
        val color=if(focused) Color.Black else Color.White
        Text(label,color=if(focused) Color.DarkGray else Muted,fontSize=13.sp);Text(value,color=color,fontSize=17.sp)
    }
}
