package com.miflix.native2.ui

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.miflix.native2.data.*
import kotlinx.coroutines.*
import java.time.LocalDate

/** Fixed controls and table header; only compact, constant-height table rows scroll. */
@Composable
internal fun SportsStatsPanelRP95(state:AppState,title:String,repo:SportsStatsRP95,selected:LiveScoreRP11?,
    select:(LiveScoreRP11)->Unit,onClose:()->Unit,videoFocus:FocusRequester,modifier:Modifier=Modifier,leaguesRP130:List<String> = listOf("NBA","MLB"),selectOnlyRP130:Boolean=false) {
    var choosing by remember {mutableStateOf(selectOnlyRP130 || selected==null)}
    var league by remember {mutableStateOf(selected?.league ?: if("LIDOM" in title.uppercase()) "LIDOM" else StatsTablePolicyRP105.preferredLeague(title))}
    var date by remember {mutableStateOf(selected?.date ?: LiveScoresRP11.today())}
    var games by remember(date) {mutableStateOf<List<LiveScoreRP11>>(emptyList())}
    var gamesBusy by remember {mutableStateOf(false)}
    var gamesError by remember {mutableStateOf(false)}
    var reload by remember {mutableIntStateOf(0)}
    var autoMatched by remember {mutableStateOf(selected!=null)}
    var data by remember(selected?.league,selected?.id) {mutableStateOf(selected?.let {repo.cached(it)})}
    var stale by remember(selected?.league,selected?.id) {mutableStateOf(false)}
    var loading by remember(selected?.league,selected?.id) {mutableStateOf(false)}
    var statsRetry by remember {mutableIntStateOf(0)}
    val closeFocus=remember {FocusRequester()}
    // A data refresh never requests focus. Only entering/changing the panel does.
    LaunchedEffect(choosing,selected?.league,selected?.id) {withFrameNanos {};runCatching {closeFocus.requestFocus()}}
    LaunchedEffect(date,league,reload,choosing,state.appForeground) {
        if(!choosing || !state.appForeground) return@LaunchedEffect
        gamesBusy=true;gamesError=false
        try {
            coroutineScope {
                val sports=if(autoMatched) listOf(league) else listOf(league)+leaguesRP130.filter {it!=league}
                sports.forEach {sport ->launch {
                    try {
                        val rows=repo.games(sport,date)
                        games=games.filterNot {it.league==sport}+rows
                        if(!autoMatched) SportsStatsRP95.match(title,rows)?.let {
                            autoMatched=true;select(it);choosing=false
                        }
                    } catch(e:CancellationException) {throw e} catch(_:Exception) {gamesError=true}
                }}
            }
            autoMatched=true
        } finally {gamesBusy=false}
    }
    LaunchedEffect(selected?.league,selected?.id,choosing,state.appForeground,statsRetry) {
        val game=selected ?: return@LaunchedEffect
        if(choosing || !state.appForeground) return@LaunchedEffect
        var failures=0
        while(isActive) {
            val started=System.nanoTime()
            loading=data==null
            try {
                val result=repo.stats(game,force=true)
                data=result;stale=false;failures=0
                if(result.final) break
            } catch(e:CancellationException) {throw e} catch(_:Exception) {stale=true;failures++}
            finally {loading=false}
            delay(StatsTablePolicyRP105.pollDelayMs(data?.live!=false,failures,(System.nanoTime()-started)/1_000_000L))
        }
    }
    Column(modifier.fillMaxHeight().background(Color(0xFF080808)).focusGroup().padding(horizontal=12.dp,vertical=10.dp),verticalArrangement=Arrangement.spacedBy(7.dp)) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            Text(if(choosing) tr("Seleccionar partido","Select game") else (selected?.league.orEmpty()+" · "+tr("Estadísticas","Box score")),color=Color.White,fontSize=16.sp,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f))
            StatsActionRP105("×",false,Modifier.width(32.dp).focusRequester(closeFocus).focusProperties {left=videoFocus},description=tr("Cerrar estadísticas","Close stats"),click=onClose)
        }
        if(choosing) {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                for(sport in leaguesRP130) StatsActionRP105(sport,league==sport,Modifier.weight(1f)) {autoMatched=true;league=sport}
            }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                StatsActionRP105("‹",false) {autoMatched=true;date=LocalDate.parse(date).minusDays(1).toString()}
                Text(date,color=Color.White,fontSize=13.sp)
                StatsActionRP105("›",false) {autoMatched=true;date=LocalDate.parse(date).plusDays(1).toString()}
            }
            if(gamesBusy) Text(tr("Buscando partidos…","Loading games…"),color=Muted,fontSize=11.sp)
            if(gamesError) Text(tr("No se pudieron cargar todos los partidos.","Some games could not be loaded."),color=Color(0xFFFFC28A),fontSize=11.sp)
            LazyColumn(Modifier.weight(1f).fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(4.dp),contentPadding=PaddingValues(bottom=12.dp)) {
                items(games.filter {it.league==league},key={"${it.league}:${it.id}"}) {game ->
                    StatsActionRP105("${game.away.short} ${game.away.score} – ${game.home.score} ${game.home.short} · ${game.phase}",selected?.league==game.league && selected.id==game.id,Modifier.fillMaxWidth()) {select(game);choosing=false}
                }
                if(!gamesBusy && games.none {it.league==league}) item {Text(tr("No hay partidos en esta fecha.","No games on this date."),color=Muted,fontSize=13.sp)}
                item {StatsActionRP105(tr("Actualizar lista","Refresh games"),false,Modifier.fillMaxWidth()) {reload++}}
                if(selected!=null) item {StatsActionRP105(tr("Volver a estadísticas","Back to stats"),false,Modifier.fillMaxWidth()) {choosing=false}}
            }
        } else selected?.let {game ->
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                ScoreTeamCompactRP105(game.away,data?.teams?.getOrNull(0)?.score ?: game.away.score,Modifier.weight(1f))
                Text("–",color=Muted,fontSize=16.sp)
                ScoreTeamCompactRP105(game.home,data?.teams?.getOrNull(1)?.score ?: game.home.score,Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                Text(data?.phase ?: game.phase,color=Muted,fontSize=11.sp,maxLines=1,overflow=TextOverflow.Ellipsis,modifier=Modifier.weight(1f))
                StatsActionRP105(tr("Partido","Game"),false) {choosing=true;autoMatched=true;date=game.date;league=game.league}
                if(stale) StatsActionRP105("↻",false,description=tr("Reintentar estadísticas","Retry stats")) {statsRetry++}
            }
            if(loading) Text(tr("Cargando estadísticas…","Loading stats…"),color=Muted,fontSize=11.sp)
            if(stale) Text(tr("Sin actualización; conservamos los últimos datos.","Update unavailable; showing last stats."),color=Color(0xFFFFC28A),fontSize=10.sp)
            key(game.league,game.id) {StatsBoxScoreRP105(game,data,videoFocus)}
        }
    }
}

@Composable
private fun ColumnScope.StatsBoxScoreRP105(game:LiveScoreRP11,data:GameStatsRP95?,videoFocus:FocusRequester) {
    var teamIndex by remember {mutableIntStateOf(0)}
    var tab by remember {mutableIntStateOf(0)}
    var page by remember(teamIndex,tab) {mutableIntStateOf(0)}
    val teamRequests=remember {List(2) {FocusRequester()}}
    val teamFocus=teamRequests[teamIndex]
    val firstRow=remember(teamIndex,tab,page) {FocusRequester()}
    val tabs=if(game.league=="NBA") listOf(tr("Jugadores","Players"),tr("Equipos","Teams")) else listOf(tr("Bateo","Batting"),tr("Pitcheo","Pitching"),tr("Equipos","Teams"))
    val tabRequests=remember {List(tabs.size) {FocusRequester()}}
    val tabFocus=tabRequests[tab]
    val columnFocus=remember {FocusRequester()}
    val currentTeam=if(teamIndex==0) game.away else game.home
    val table=remember(data,teamIndex,tab,page) {
        if(data==null) StatsTableRP105(emptyList(),emptyList(),1)
        else if(tab==tabs.lastIndex) {
            val values=data.teams.getOrNull(teamIndex)?.values.orEmpty()
            val metrics=if(game.league=="NBA") listOf("fieldGoalPct" to "FG%","threePointFieldGoalPct" to "3P%","freeThrowPct" to "FT%","totalRebounds" to "REB","assists" to "AST","steals" to "STL","blocks" to "BLK","turnovers" to "TO")
                else listOf("hits" to "H","homeRuns" to "HR","rbi" to "RBI","baseOnBalls" to "BB","strikeOuts" to "K","errors" to "E","inningsPitched" to "IP")
            StatsTableRP105(listOf(tr("Valor","Value")),metrics.map {(key,label) ->StatsTableRowRP105(key,label,listOf(values[key] ?: "—"))},1)
        } else {
            val groups=(if(game.league!="NBA" && tab==1) data.pitching else data.players).filter {
                if(it.teamId.isNotBlank()) it.teamId==currentTeam.id else it.team==data.teams.getOrNull(teamIndex)?.name
            }
            StatsTablePolicyRP105.build(groups,game.league,page)
        }
    }
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(5.dp)) {
        listOf(game.away,game.home).forEachIndexed {index,team ->
            StatsTeamButtonRP132(team,teamIndex==index,Modifier.weight(1f)
                .focusRequester(teamRequests[index]).focusProperties {left=if(index==0) videoFocus else teamRequests[0];right=if(index==0) teamRequests[1] else FocusRequester.Cancel;down=tabFocus}) {teamIndex=index}
        }
    }
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)) {
        tabs.forEachIndexed {index,label ->StatsActionRP105(label,tab==index,Modifier.weight(1f).focusRequester(tabRequests[index]).focusProperties {up=teamFocus;down=firstRow;left=if(index>0) tabRequests[index-1] else videoFocus;right=if(index<tabs.lastIndex) tabRequests[index+1] else if(table.pages>1) columnFocus else FocusRequester.Cancel}) {tab=index}}
        if(table.pages>1) StatsActionRP105("${page.coerceAtMost(table.pages-1)+1}/${table.pages} ›",false,Modifier.focusRequester(columnFocus).focusProperties {left=tabRequests.last();up=teamFocus;down=firstRow},description=tr("Más columnas","More columns")) {page=(page+1)%table.pages}
    }
    StatsTableCellsRP105(tr("Jugador / estadística","Player / statistic"),table.columns,header=true)
    Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF343434)))
    key(teamIndex,tab,page) {StatsTableRowsRP105(table,firstRow,tabFocus,teamFocus,videoFocus,Modifier.weight(1f))}
    Text(tr("← Video  ·  → Equipos  ·  Datos pueden adelantarse al video","← Video  ·  → Teams  ·  Data may lead video"),color=Muted,fontSize=9.sp,maxLines=1)
}

@Composable
internal fun StatsTableRowsRP105(table:StatsTableRP105,first:FocusRequester,header:FocusRequester,teams:FocusRequester,video:FocusRequester,modifier:Modifier,rowHeightDp:Int=39) {
    val list=rememberLazyListState()
    var tableFocused by remember {mutableStateOf(false)}
    var selectedKey by remember {mutableStateOf(table.rows.firstOrNull()?.key)}
    val keys=remember(table.rows) {table.rows.map {it.key}}
    val cursor=keys.indexOf(selectedKey).coerceAtLeast(0)
    LaunchedEffect(keys) {if(selectedKey !in keys) selectedKey=keys.firstOrNull()}
    // A single focus target survives lazy-row recycling and every live-data refresh.
    LaunchedEffect(cursor,tableFocused) {
        if(!tableFocused || table.rows.isEmpty()) return@LaunchedEffect
        withFrameNanos { }
        val layout=list.layoutInfo
        val visible=layout.visibleItemsInfo
        val target=visible.firstOrNull {it.index==cursor}
        if(target!=null) {
            val overflow=when {
                target.offset<layout.viewportStartOffset ->target.offset-layout.viewportStartOffset
                target.offset+target.size>layout.viewportEndOffset ->target.offset+target.size-layout.viewportEndOffset
                else ->0
            }
            if(overflow!=0) list.scrollBy(overflow.toFloat())
        } else {
            val start=StatsViewportRP106.firstFor(cursor,list.firstVisibleItemIndex,visible.lastOrNull()?.index ?: list.firstVisibleItemIndex)
            list.scrollToItem(start)
        }
    }
    LazyColumn(modifier.fillMaxWidth().focusRequester(first)
        .onFocusChanged {tableFocused=it.isFocused}
        .semantics {contentDescription=table.rows.getOrNull(cursor)?.let {it.name+" · "+it.values.joinToString(" · ")} ?: "Stats"}
        .onPreviewKeyEvent {event ->
            when(event.nativeKeyEvent.keyCode) {
                19,20 -> {
                    if(event.type==KeyEventType.KeyDown) {
                        val index=keys.indexOf(selectedKey).coerceAtLeast(0)
                        val next=StatsTablePolicyRP105.target(index,if(event.nativeKeyEvent.keyCode==19) -1 else 1,keys.size)
                        if(next<0) runCatching {header.requestFocus()} else selectedKey=keys.getOrNull(next)
                    }
                    true
                }
                21 -> {if(event.type==KeyEventType.KeyDown) runCatching {video.requestFocus()};true}
                22 -> {if(event.type==KeyEventType.KeyDown) runCatching {teams.requestFocus()};true}
                else ->false
            }
        }.focusable(),state=list,contentPadding=PaddingValues(bottom=8.dp)) {
        itemsIndexed(table.rows,key={_,row ->row.key}) {index,row ->
            Column(Modifier.fillMaxWidth().height(rowHeightDp.dp)
                .background(if(tableFocused && index==cursor) Color(0xFF292929) else Color.Transparent)
                ) {
                StatsTableCellsRP105(row.name,row.values,photo=row.photo,number=row.number,modifier=Modifier.weight(1f))
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF222222)))
            }
        }
        if(table.rows.isEmpty()) item {Text(tr("Las estadísticas aparecerán cuando estén disponibles.","Stats will appear when available."),color=Muted,fontSize=12.sp,modifier=Modifier.padding(vertical=12.dp))}
    }
}

@Composable
internal fun StatsTableCellsRP105(name:String,values:List<String>,header:Boolean=false,photo:String="",number:String="",modifier:Modifier=Modifier) {
    val context=LocalContext.current
    val image=remember(photo,context) {ImageRequest.Builder(context).data(photo).size(64,64).build()}
    Row(modifier.fillMaxWidth().heightIn(min=23.dp).padding(horizontal=4.dp),verticalAlignment=Alignment.CenterVertically) {
        Row(Modifier.weight(.52f),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp)) {
            if(!header && photo.isNotBlank()) {
                var failed by remember(photo) {mutableStateOf(false)}
                Box(Modifier.size(25.dp).clip(CircleShape).background(Color(0xFF242424)),contentAlignment=Alignment.Center) {
                    Text(name.take(1),color=Muted,fontSize=10.sp)
                    if(!failed) AsyncImage(image,null,Modifier.fillMaxSize(),onError={failed=true})
                }
            }
            if(!header && number.isNotBlank()) Text(number,color=Muted,fontSize=10.sp,maxLines=1)
            Text(name,color=if(header) Muted else Color.White,fontSize=if(header) 10.sp else 12.sp,maxLines=1,overflow=TextOverflow.Ellipsis,fontWeight=if(header) FontWeight.Bold else FontWeight.Normal)
        }
        // Always reserve four numeric slots so headers, partial pages and values align.
        for(index in 0 until 4) Text(values.getOrElse(index) {""},modifier=Modifier.weight(.12f),color=if(header) Muted else Color.White,fontSize=if(header) 10.sp else 12.sp,maxLines=1,textAlign=TextAlign.Center)
    }
}

@Composable
private fun ScoreTeamCompactRP105(team:ScoreTeamRP11,score:String,modifier:Modifier) {
    Row(modifier,verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)) {
        StatsTeamLogoRP132(team,Modifier.size(30.dp))
        Spacer(Modifier.weight(1f))
        Text(score,color=Color.White,fontSize=22.sp,fontWeight=FontWeight.Bold)
    }
}

@Composable
private fun StatsActionRP105(label:String,selected:Boolean,modifier:Modifier=Modifier,description:String=label,click:()->Unit) {
    var focused by remember {mutableStateOf(false)}
    Box(modifier.height(30.dp).clip(RoundedCornerShape(5.dp))
        .semantics {contentDescription=description}.onFocusChanged {focused=it.isFocused}
        .background(GlassActionBrushRP108(focused,selected))
        .onPreviewKeyEvent {event ->
            if(event.nativeKeyEvent.keyCode in listOf(23,66,160)) {
                if(event.type==KeyEventType.KeyUp && !event.nativeKeyEvent.isCanceled) click()
                true
            } else false
        }.clickable(onClick=click).padding(horizontal=8.dp),contentAlignment=Alignment.Center) {
        Text(label,color=if(focused) Color.White else Color.White,fontSize=12.sp,fontWeight=if(focused || selected) FontWeight.Bold else FontWeight.Normal,maxLines=1,overflow=TextOverflow.Ellipsis)
    }
}
