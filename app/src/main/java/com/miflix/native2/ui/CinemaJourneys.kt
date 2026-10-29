package com.miflix.native2.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.miflix.native2.model.MediaSummary
import com.miflix.native2.model.EpisodeSummary
import kotlinx.coroutines.CancellationException

internal data class JourneyEntry(val id:Int,val type:String,val title:String,val year:String,val story:Int,val note:String="") {
    val cloud:String get()="tmdb:$type:$id"
    fun summary()=MediaSummary(id,type,title,"",null,null,0.0,year)
}
private fun film(id:Int,title:String,year:String,story:Int,note:String="")=JourneyEntry(id,"movie",title,year,story,note)
private fun show(id:Int,title:String,year:String,story:Int,note:String="")=JourneyEntry(id,"series",title,year,story,note)
internal val CinemaJourneys=mapOf(
    "Marvel · Infinity Saga" to listOf(
        film(1726,"Iron Man","2008",3,"Post-credits scene"),film(1724,"The Incredible Hulk","2008",5),film(10138,"Iron Man 2","2010",4),film(10195,"Thor","2011",6),
        film(1771,"Captain America: The First Avenger","2011",1),film(24428,"The Avengers","2012",7,"Mid-credits and post-credits scenes"),film(68721,"Iron Man 3","2013",8),film(76338,"Thor: The Dark World","2013",9),
        film(100402,"Captain America: The Winter Soldier","2014",10),film(118340,"Guardians of the Galaxy","2014",11),film(99861,"Avengers: Age of Ultron","2015",13),film(102899,"Ant-Man","2015",14),
        film(271110,"Captain America: Civil War","2016",15),film(284052,"Doctor Strange","2016",18),film(283995,"Guardians of the Galaxy Vol. 2","2017",12),film(315635,"Spider-Man: Homecoming","2017",17),
        film(284053,"Thor: Ragnarok","2017",19),film(284054,"Black Panther","2018",16),film(299536,"Avengers: Infinity War","2018",21),film(363088,"Ant-Man and the Wasp","2018",20,"Credits connect to Infinity War"),
        film(299537,"Captain Marvel","2019",2,"Credits connect to Endgame"),film(299534,"Avengers: Endgame","2019",22),film(429617,"Spider-Man: Far from Home","2019",23)
    ),
    "Star Wars · Core journey" to listOf(
        film(11,"A New Hope","1977",8),film(1891,"The Empire Strikes Back","1980",9),film(1892,"Return of the Jedi","1983",10),film(1893,"The Phantom Menace","1999",1),film(1894,"Attack of the Clones","2002",2),film(1895,"Revenge of the Sith","2005",3),
        film(140607,"The Force Awakens","2015",14),film(330459,"Rogue One","2016",7),film(181808,"The Last Jedi","2017",15),film(348350,"Solo","2018",4),film(181812,"The Rise of Skywalker","2019",16),
        show(82856,"The Mandalorian","2019",11),show(115036,"The Book of Boba Fett","2021",12),show(92830,"Obi-Wan Kenobi","2022",5),show(83867,"Andor","2022",6),show(114461,"Ahsoka","2023",13,"Stories in this era overlap")
    ),
    "DC · DCEU" to listOf(
        film(49521,"Man of Steel","2013",3),film(209112,"Batman v Superman: Dawn of Justice","2016",4),film(297761,"Suicide Squad","2016",5),film(297762,"Wonder Woman","2017",1),
        film(791373,"Zack Snyder's Justice League","2021",6,"Alternate cut; does not create a second story step"),film(297802,"Aquaman","2018",7),film(287947,"Shazam!","2019",8),film(495764,"Birds of Prey","2020",9),film(464052,"Wonder Woman 1984","2020",2),film(436969,"The Suicide Squad","2021",10),show(110492,"Peacemaker","2022",11,"Later continuity differs; this itinerary starts with season 1"),film(298618,"The Flash","2023",12,"Multiverse branch")
    ),
    "Game of Thrones" to listOf(show(1399,"Game of Thrones","2011",2),show(94997,"House of the Dragon","2022",1,"Prequel; series episode order stays in release order"))
)

@Composable
internal fun JourneyPanel(state:AppState,exit:()->Unit,back:()->Unit) {
    var saga by remember {mutableStateOf(state.savedCinemaJourney().takeIf {it in CinemaJourneys} ?: CinemaJourneys.keys.first())}
    var chronological by remember {mutableStateOf(state.cinemaJourneyChronological())}
    var episodeGuide by remember {mutableStateOf<JourneyEntry?>(null)}
    val entry=episodeGuide
    if(entry!=null) {JourneyEpisodes(state,entry,exit) {episodeGuide=null};return}
    val rows=CinemaJourneys.getValue(saga).let {if(chronological) it.sortedBy {v->v.story} else it.sortedWith(compareBy<JourneyEntry> {v->v.year}.thenBy {v->CinemaJourneys.getValue(saga).indexOf(v)})}
    PlayerSideSheet(tr("Itinerarios de sagas","Saga journeys"),back) {
        item {CinemaBanner("BRUNIO · JOURNEYS",tr("Cada historia, en su lugar","Every story in its place"),tr("Tu siguiente paso, con lo visto siempre a mano.","Your next chapter, with your progress alongside."))}
        item {Text(tr("Itinerarios curados: Marvel cubre Infinity Saga; DC usa DCEU, sin mezclar universos. No es una cronología completa de todos los estrenos actuales. Las escenas poscréditos son notas, sin saltos de vídeo.","Curated itineraries: Marvel covers Infinity Saga; DC uses DCEU without mixing universes. This is not a complete timeline of every current release. Post-credit scenes are notes, not video jumps."),color=Muted,fontSize=12.sp)}
        items(CinemaJourneys.keys.toList()) {name ->FocusButton(name,primary=name==saga,modifier=Modifier.fillMaxWidth()) {saga=name;state.saveCinemaJourney(name)}}
        item {Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {FocusButton(tr("Estreno","Release"),primary=!chronological) {chronological=false;state.saveCinemaJourneyChronological(false)};FocusButton(tr("Historia","Story"),primary=chronological) {chronological=true;state.saveCinemaJourneyChronological(true)}}}
        items(rows,key={it.cloud}) {row ->
            val p=state.progress[row.cloud]
            val status=if(row.type=="movie" && (p?.percent ?: 0.0)>=90) tr("✓ Vista","✓ Watched") else if(row.type=="series" && p!=null) "S${p.season} E${p.episode}" else tr("Pendiente","Pending")
            JourneyCard(state,row,rows.indexOf(row)+1,status,onDetails={exit();state.launch {val media=state.tmdb.byCloudId(row.cloud) ?: row.summary();state.open(media)}},onEpisodes=if(row.type=="series") ({episodeGuide=row}) else null)

        }
    }
}

@Composable
private fun JourneyCard(state:AppState,row:JourneyEntry,rank:Int,status:String,onDetails:()->Unit,onEpisodes:(()->Unit)?) {
    var media by remember(row.cloud) {mutableStateOf<MediaSummary?>(null)}
    LaunchedEffect(row.cloud) {try {media=state.tmdb.byCloudId(row.cloud)} catch(e:CancellationException) {throw e} catch(_:Exception) { }}
    Row(Modifier.fillMaxWidth().background(Color(0xff211e2a),RoundedCornerShape(18.dp)).padding(12.dp),horizontalArrangement=Arrangement.spacedBy(13.dp)) {
        Box(Modifier.width(74.dp).height(110.dp).background(Color(0xff31273d),RoundedCornerShape(10.dp))) {
            media?.let {AsyncImage(it.poster ?: it.backdrop,null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)}
            Text(rank.toString(),color=Color.White,fontSize=26.sp,modifier=Modifier.padding(6.dp))
        }
        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(6.dp)) {
            Text(row.title,color=Color.White,fontSize=17.sp,maxLines=2)
            Text("${row.year} · $status",color=Accent,fontSize=12.sp)
            if(row.note.isNotBlank()) Text(row.note,color=Muted,fontSize=11.sp,maxLines=3)
            Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                FocusButton(tr("Detalles","Details"),horizontalPadding=14.dp,onClick=onDetails)
                onEpisodes?.let {FocusButton(tr("Episodios","Episodes"),horizontalPadding=14.dp,onClick=it)}
            }
        }
    }
}

@Composable
private fun JourneyEpisodes(state:AppState,entry:JourneyEntry,exit:()->Unit,back:()->Unit) {
    var season by remember(entry.cloud) {mutableIntStateOf(1)}
    var retry by remember {mutableIntStateOf(0)}
    var seasons by remember(entry.cloud) {mutableIntStateOf(1)}
    var rows by remember(entry.cloud) {mutableStateOf<List<EpisodeSummary>>(emptyList())}
    var loading by remember {mutableStateOf(true)};var message by remember {mutableStateOf("")}
    LaunchedEffect(entry.cloud,season,retry) {
        loading=true;message=""
        try {val detail=state.tmdb.details(entry.summary());seasons=detail.seasonCount.coerceAtLeast(1);rows=state.tmdb.season(entry.id,season)}
        catch(e:CancellationException) {throw e} catch(_:Exception) {message=tr("No se pudieron cargar episodios.","Could not load episodes.")}
        loading=false
    }
    PlayerSideSheet(entry.title,back) {
        item {Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {if(season>1) FocusButton("←") {season--};Text("Season $season",color=Color.White,modifier=Modifier.padding(top=12.dp));if(season<seasons) FocusButton("→") {season++}}}
        if(loading) item {Text(tr("Cargando…","Loading…"),color=Muted)}
        if(message.isNotBlank()) item {Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {Text(message,color=Accent);FocusButton(tr("Reintentar","Retry")) {retry++}}}
        if(!loading) items(rows,key={"${it.season}:${it.episode}"}) {ep ->
            val watched=state.episodeWatched(entry.summary(),ep)
            Row(Modifier.fillMaxWidth().background(Panel,RoundedCornerShape(16.dp)).padding(10.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                AsyncImage(ep.still,null,Modifier.width(94.dp).height(58.dp),contentScale=ContentScale.Crop)
                FocusButton((if(watched) "✓ " else "○ ")+"S${ep.season} E${ep.episode} · ${ep.title}",modifier=Modifier.weight(1f)) {
                val today=java.text.SimpleDateFormat("yyyy-MM-dd",java.util.Locale.US).format(java.util.Date())
                if(ep.airDate.isBlank() || ep.airDate>today) state.error=tr("Este episodio todavía no está disponible.","This episode is not available yet.") else {
                exit();state.launch {val media=state.tmdb.byCloudId(entry.cloud) ?: entry.summary();state.open(media);if(state.selected?.cloudId==entry.cloud) state.playEpisode(ep)}
                }
                }
            }
        }
    }
}
