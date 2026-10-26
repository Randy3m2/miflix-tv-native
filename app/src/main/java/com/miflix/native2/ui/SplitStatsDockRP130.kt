package com.miflix.native2.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.miflix.native2.data.*
import kotlinx.coroutines.*

internal fun splitStatsTitleRP130(req:PlayerRequest)=
    (if(req.liveProvider=="lidom") "LIDOM · " else "")+req.item.title

/** Data-only overlay in the actual 16:9 letterbox; never owns or resizes a player. */
@Composable
internal fun SplitStatsDockRP130(state:AppState,req:PlayerRequest,repo:SportsStatsRP95,
    game:LiveScoreRP11?,select:(LiveScoreRP11)->Unit,choose:()->Unit,close:()->Unit,
    modifier:Modifier=Modifier) {
    val status=rememberSplitStatsRP130(state,req,repo,game,select)
    val data=status.data
    val stale=status.stale
    val searching=status.searching
    Column(modifier.clip(RoundedCornerShape(10.dp))
        .background(GlassActionBrushRP108(false,true))
        .border(1.dp,Color(0x554F5E72),RoundedCornerShape(10.dp)).focusGroup().padding(7.dp),
        verticalArrangement=Arrangement.spacedBy(3.dp)) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)) {
            Text(game?.league ?: "Stats",color=Color.White,fontSize=13.sp,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f))
            if(stale) Text(tr("Sin actualizar","Last update"),color=Color(0xFFFFC28A),fontSize=9.sp)
            SplitStatsActionRP130(tr("Partido","Game"),choose)
            SplitStatsActionRP130("×",close,description=tr("Cerrar estadísticas","Close stats"))
        }
        if(game==null) {
            Box(Modifier.weight(1f).fillMaxWidth(),contentAlignment=Alignment.Center) {
                SplitStatsActionRP130(if(searching) tr("Buscando partido…","Finding game…") else tr("Seleccionar partido","Select game"),choose)
            }
        } else {
            Row(Modifier.weight(1f).fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                        StatsTeamLogoRP132(game.away,Modifier.weight(1f).height(26.dp))
                        Text(data?.teams?.getOrNull(0)?.score ?: game.away.score,color=Color.White,fontSize=21.sp,fontWeight=FontWeight.Bold)
                        Text("–",color=Muted,fontSize=13.sp)
                        Text(data?.teams?.getOrNull(1)?.score ?: game.home.score,color=Color.White,fontSize=21.sp,fontWeight=FontWeight.Bold)
                        StatsTeamLogoRP132(game.home,Modifier.weight(1f).height(26.dp))
                    }
                    Text(data?.phase ?: game.phase,color=Muted,fontSize=10.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
                }
                Column(Modifier.weight(.9f),verticalArrangement=Arrangement.spacedBy(2.dp)) {
                    val nba=game.league=="NBA"
                    MiniStatsRowRP130(tr("Equipo","Team"),if(nba) listOf("PTS","REB","AST") else listOf("R","H","E"),true)
                    listOf(game.away,game.home).forEachIndexed {index,team->
                        val current=data?.teams?.getOrNull(index)
                        MiniStatsRowRP130(team.short,listOf(current?.score ?: team.score,
                            current?.values?.get(if(nba) "totalRebounds" else "hits") ?: "—",
                            current?.values?.get(if(nba) "assists" else "errors") ?: "—"),false,team)
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniStatsRowRP130(team:String,values:List<String>,header:Boolean,logoTeam:ScoreTeamRP11?=null) {
    Row(Modifier.fillMaxWidth().height(16.dp),verticalAlignment=Alignment.CenterVertically) {
        if(logoTeam==null) Text(team,color=Muted,fontSize=10.sp,maxLines=1,modifier=Modifier.weight(1.4f))
        else StatsTeamLogoRP132(logoTeam,Modifier.weight(1.4f).height(16.dp))
        values.forEach {Text(it,color=if(header) Muted else Color.White,fontSize=10.sp,modifier=Modifier.weight(1f),maxLines=1,textAlign=TextAlign.Center)}
    }
}

@Composable
internal fun SplitStatsActionRP130(label:String,click:()->Unit,modifier:Modifier=Modifier,selected:Boolean=false,textSize:Int=11,description:String=label) {
    var focused by remember {mutableStateOf(false)}
    Box(modifier.height(24.dp).widthIn(min=26.dp).clip(RoundedCornerShape(6.dp))
        .onFocusChanged {focused=it.isFocused}.background(GlassActionBrushRP108(focused,selected))
        .border(1.dp,if(focused) Color.White else Color(0x445E6878),RoundedCornerShape(6.dp))
        .semantics {contentDescription=description}.clickable(role=Role.Button,onClick=click).padding(horizontal=7.dp),contentAlignment=Alignment.Center) {
        Text(label,color=Color.White,fontSize=textSize.sp,fontWeight=if(focused) FontWeight.Bold else FontWeight.Normal,maxLines=1)
    }
}

private data class SplitStatsStateRP130(val data:GameStatsRP95?,val stale:Boolean,val searching:Boolean)

@Composable
private fun rememberSplitStatsRP130(state:AppState,req:PlayerRequest,repo:SportsStatsRP95,game:LiveScoreRP11?,select:(LiveScoreRP11)->Unit):SplitStatsStateRP130 {
    val title=splitStatsTitleRP130(req)
    var data by remember(game?.league,game?.id) {mutableStateOf(game?.let {repo.cached(it)})}
    var stale by remember(game?.league,game?.id) {mutableStateOf(false)}
    var searching by remember {mutableStateOf(false)}
    val latestSelect by rememberUpdatedState(select)
    LaunchedEffect(req.playbackId,game==null,state.appForeground) {
        if(game!=null || !state.appForeground) return@LaunchedEffect
        searching=true
        try {
            val preferred=SplitStatsPolicyRP130.preferred(title)
            suspend fun lookup(league:String):LiveScoreRP11? = try {
                SplitStatsPolicyRP130.match(title,repo.games(league,LiveScoresRP11.today()))
            } catch(e:CancellationException) {throw e} catch(_:Exception) {null}
            val primary=lookup(preferred)
            if(primary!=null) latestSelect(primary)
            else {
                val matches=coroutineScope {
                    listOf("NBA","MLB","LIDOM").filter {it!=preferred}.map {league->async {lookup(league)}}.awaitAll().filterNotNull()
                }
                matches.singleOrNull()?.let {latestSelect(it)}
            }
        } finally {searching=false}
    }
    LaunchedEffect(game?.league,game?.id,state.appForeground) {
        val selected=game ?: return@LaunchedEffect
        if(!state.appForeground) return@LaunchedEffect
        var failures=0
        while(isActive) {
            val started=System.nanoTime()
            try {
                data=repo.stats(selected,force=true);stale=false;failures=0
                if(data?.final==true) break
            } catch(e:CancellationException) {throw e} catch(_:Exception) {stale=true;failures++}
            delay(StatsTablePolicyRP105.pollDelayMs(data?.live!=false,failures,(System.nanoTime()-started)/1_000_000L))
        }
    }
    return SplitStatsStateRP130(data,stale,searching)
}

/** Short fixed toolbar + one stable-focus lazy table: fits a quarter-screen TV cell. */
@Composable
internal fun SplitStatsFullRP130(state:AppState,req:PlayerRequest,repo:SportsStatsRP95,
    game:LiveScoreRP11?,select:(LiveScoreRP11)->Unit,choose:()->Unit,close:()->Unit,
    videoFocus:FocusRequester,modifier:Modifier=Modifier) {
    val status=rememberSplitStatsRP130(state,req,repo,game,select)
    val data=status.data
    var teamIndex by remember(game?.league,game?.id) {mutableIntStateOf(0)}
    var tab by remember(game?.league,game?.id) {mutableIntStateOf(0)}
    var page by remember(teamIndex,tab) {mutableIntStateOf(0)}
    val firstRow=remember {FocusRequester()}
    val header=remember {FocusRequester()}
    val teams=remember {FocusRequester()}
    Column(modifier.fillMaxWidth().focusGroup().padding(top=4.dp),verticalArrangement=Arrangement.spacedBy(3.dp)) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp)) {
            Text((game?.league ?: "Stats")+(if(status.stale) " · !" else ""),color=Color.White,fontSize=12.sp,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f))
            if(game!=null) Text(data?.phase ?: game.phase,color=Muted,fontSize=10.sp,maxLines=1,modifier=Modifier.weight(1f))
            SplitStatsActionRP130(tr("Partido","Game"),choose,Modifier.focusRequester(header))
            SplitStatsActionRP130("×",close,description=tr("Cerrar estadísticas","Close stats"))
        }
        if(game==null) {
            Box(Modifier.fillMaxWidth().weight(1f),contentAlignment=Alignment.Center) {
                SplitStatsActionRP130(if(status.searching) tr("Buscando partido…","Finding game…") else tr("Seleccionar partido","Select game"),choose)
            }
        } else {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                StatsTeamLogoRP132(game.away,Modifier.size(26.dp))
                Text((data?.teams?.getOrNull(0)?.score ?: game.away.score)+" – "+(data?.teams?.getOrNull(1)?.score ?: game.home.score),
                    color=Color.White,fontSize=18.sp,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f),textAlign=TextAlign.Center)
                StatsTeamLogoRP132(game.home,Modifier.size(26.dp))
            }
            val current=if(teamIndex==0) game.away else game.home
            val table=remember(data,teamIndex,tab,page) {
                val groups=if(tab==1) data?.pitching.orEmpty() else data?.players.orEmpty()
                StatsTablePolicyRP105.build(groups.filter {if(it.teamId.isNotBlank()) it.teamId==current.id else it.team==data?.teams?.getOrNull(teamIndex)?.name},game.league,page)
            }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                listOf(game.away,game.home).forEachIndexed {index,team->
                    StatsTeamButtonRP132(team,teamIndex==index,Modifier.weight(1f).then(if(index==teamIndex) Modifier.focusRequester(teams) else Modifier)
                        .focusProperties {down=firstRow}) {teamIndex=index}
                }
                if(game.league!="NBA") {
                    SplitStatsActionRP130(tr("Bateo","Batting"),{tab=0},Modifier.weight(1f).focusProperties {down=firstRow},tab==0)
                    SplitStatsActionRP130(tr("Pitcheo","Pitching"),{tab=1},Modifier.weight(1f).focusProperties {down=firstRow},tab==1)
                }
                if(table.pages>1) SplitStatsActionRP130((page+1).toString()+"/"+table.pages+" ›",{page=(page+1)%table.pages},Modifier.focusProperties {down=firstRow})
            }
            StatsTableCellsRP105(tr("Jugador","Player"),table.columns,header=true)
            StatsTableRowsRP105(table,firstRow,header,teams,videoFocus,Modifier.weight(1f),rowHeightDp=26)
        }
    }
}
