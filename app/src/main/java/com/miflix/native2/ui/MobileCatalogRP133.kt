@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.miflix.native2.ui

import androidx.activity.compose.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.miflix.native2.model.*
import kotlinx.coroutines.*

@Composable
internal fun MobileRailRP133(state:AppState,title:String,rows:List<MediaSummary>,landscape:Boolean=false,topTen:Boolean=false) {
    if(rows.isEmpty()) return
    Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Text(title,color=Color.White,fontSize=19.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(horizontal=16.dp))
        LazyRow(contentPadding=PaddingValues(horizontal=16.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            itemsIndexed(if(topTen) rows.take(10) else rows,key={_,it->it.cloudId}) {index,item->
                MobilePosterRP133(state,item,landscape=landscape,rank=if(topTen) index+1 else null)
            }
        }
    }
}

@Composable
internal fun MobileHomeRP133(state:AppState) {
    var filter by rememberSaveable {mutableStateOf("")}
    var more by rememberSaveable {mutableIntStateOf(15)}
    LaunchedEffect(state.localExtrasScope(),state.interfaceLanguage,state.kidsMode,state.easyModeRP79) {
        if(state.kidsMode) {state.loadKidsHome();return@LaunchedEffect}
        if(state.easyModeRP79) {state.loadEasyHomeRP79();state.loadEasyRowsRP80();return@LaunchedEffect}
        supervisorScope {
            launch {runCatching {state.loadHomeExtras()}}
            launch {runCatching {state.loadFamiliarHome()}}
            launch {runCatching {state.loadBecauseWatched()}}
            launch {runCatching {state.loadSeasonalProgramming()}}
            launch {runCatching {state.loadNovelasHome()}}
        }
    }
    val nowPlayingRP162=rememberMobileNowPlayingRP162(state)
    val kids=state.kidsMode
    val rows=if(kids) listOf(tr("Películas","Movies") to state.kidsMovies.toList(),tr("Series","Series") to state.kidsSeries.toList())
        else if(state.easyModeRP79) listOf(tr("Películas populares","Popular movies") to state.movies.toList(),tr("Series populares","Popular series") to state.series.toList())+EasyHomeRowsRP80.rows.map {tr(it.spanish,it.english) to state.easyRowsRP80[it.id].orEmpty()}
        else listOf(tr("Películas populares","Popular movies") to state.movies.toList(),tr("Series populares","Popular series") to state.series.toList(),
            tr("Top 10 · Películas","Top 10 · Movies") to state.topRated.toList(),tr("Top 10 · Series","Top 10 · Series") to state.bestSeries.toList(),
            tr("Para ti","For you") to state.forYou.toList(),tr("Novelas que atrapan","Telenovelas") to state.novelasPicks.toList(),
            tr("Clásicos de siempre","Timeless classics") to state.classicPicks.toList(),tr("Acción","Action") to state.action.toList(),
            tr("Ciencia ficción","Sci-fi") to state.sciFi.toList(),tr("Comedia","Comedy") to state.comedy.toList(),
            tr("Suspenso","Thrillers") to state.thrillers.toList(),tr("Porque viste…","Because you watched…") to state.becauseWatchedPicks.toList(),
            tr("Nuevas películas","New movies") to state.newMovies.toList(),tr("Romance","Romance") to state.romancePicks.toList(),
            tr("Terror","Horror") to state.horrorPicks.toList(),tr("Selección de temporada","Seasonal picks") to state.seasonalItems.toList(),
            "Netflix" to state.netflix.toList(),"Disney+" to state.disney.toList(),"Prime Video" to state.prime.toList(),"Max" to state.hbo.toList())
    fun filtered(items:List<MediaSummary>)=state.localRecommendations(items).filter {filter.isEmpty() || it.type==filter}
    val continueRows=filtered(if(kids) state.kidsContinue.toList() else state.continueWatching.toList())
    val hero=filtered(state.trending.toList()+state.movies.toList()).firstOrNull()
    LazyColumn(verticalArrangement=Arrangement.spacedBy(22.dp),contentPadding=PaddingValues(bottom=24.dp)) {
        item("filters") {LazyRow(contentPadding=PaddingValues(horizontal=16.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            items(listOf("" to tr("Todo","All"),"movie" to tr("Películas","Movies"),"series" to tr("Series","Series"))) {(key,label)->
                MobileButtonRP133(label,selected=filter==key) {filter=key}
            }
        }}
        if(nowPlayingRP162.cards.any {filter.isBlank() || it.media?.type==filter}) item("now-playing-rp162") {MobileNowPlayingRowRP162(state,nowPlayingRP162,filter)}
        // Reserved first shelf: late metadata cannot anchor the list below Continue Watching.
        item("continue") {
            if(continueRows.isNotEmpty()) MobileRailRP133(state,tr("Seguir viendo","Continue Watching"),continueRows,true)
            else if(!state.continueHomeReadyRP129 && !kids) Text(tr("Preparando Seguir viendo…","Loading Continue Watching…"),color=Muted,modifier=Modifier.padding(horizontal=16.dp))
        }
        if(hero!=null) item("hero") {
            Box(Modifier.padding(horizontal=16.dp).fillMaxWidth().height(280.dp).clip(RoundedCornerShape(18.dp)).background(MobilePanelRP133)) {
                AsyncImage(hero.backdrop ?: hero.poster,null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent,Color(0xF0080A0F)))))
                Column(Modifier.align(Alignment.BottomStart).padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Text(hero.title,color=Color.White,fontSize=28.sp,lineHeight=31.sp,fontWeight=FontWeight.Bold,maxLines=2)
                    Text(hero.year+" · ★ "+String.format("%.1f",hero.rating),color=Muted,fontSize=12.sp)
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        MobileButtonRP133(tr("Ver","Watch"),"play",Modifier.weight(1f),selected=true) {state.launch {state.open(hero)}}
                        MobileIconButtonRP133("plus",tr("Mi lista","My list"),selected=hero.cloudId in state.favorites) {state.toggleFavorite(hero)}
                    }
                }
            }
        }
        if(!kids && !state.easyModeRP79) item("platforms") {
            Column(verticalArrangement=Arrangement.spacedBy(9.dp)) {
                Text(tr("Tus plataformas","Your platforms"),color=Color.White,fontSize=19.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(horizontal=16.dp))
                LazyRow(contentPadding=PaddingValues(horizontal=16.dp),horizontalArrangement=Arrangement.spacedBy(9.dp)) {
                    items(listOf("netflix" to "Netflix","disney" to "Disney+","prime" to "Prime Video","hbo" to "Max","apple" to "Apple TV+","hulu" to "Hulu","paramount" to "Paramount+","amcplus" to "AMC+")) {(id,title)->
                        val tile=StreamingTiles.firstOrNull {it.id==id}
                        Box(Modifier.width(112.dp).height(70.dp).mobileGlassRP133().clickable {state.launch {state.openPlatform(id,title)}},contentAlignment=Alignment.Center) {
                            if(id=="amcplus") Image(androidx.compose.ui.res.painterResource(com.miflix.native2.R.drawable.streaming_amcplus_rp48),title,Modifier.fillMaxSize().padding(12.dp),contentScale=ContentScale.Fit)
                            else if(tile!=null && tile.coverUrl.isNotBlank()) AsyncImage(tile.coverUrl,title,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
                            else Text(title,color=Color.White,fontWeight=FontWeight.Bold)
                        }
                    }
                }
            }
        }
        items(rows.take(more).filter {filtered(it.second).isNotEmpty()},key={it.first}) {(title,items)->
            MobileRailRP133(state,title,filtered(items),topTen=title.startsWith("Top 10"))
        }
        if(!kids && !state.easyModeRP79) item("franchises") {
            Column(Modifier.padding(horizontal=16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                Text(tr("Franquicias","Franchises"),color=Color.White,fontSize=19.sp,fontWeight=FontWeight.Bold)
                LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    items(com.miflix.native2.data.FranchiseCatalogRP29.brands) {(id,title)->
                        MobileButtonRP133(title) {state.launch {state.openFranchise(id)}}
                    }
                }
            }
        }
        if(more<rows.size) item("more") {MobileButtonRP133(tr("Cargar más","Load more"),"plus",Modifier.padding(horizontal=16.dp).fillMaxWidth()) {more+=5}}
    }
}

@Composable
internal fun MobileSearchRP133(state:AppState) {
    var query by rememberSaveable {mutableStateOf(state.searchQuery)}
    var items by remember {mutableStateOf<List<MediaSummary>>(emptyList())}
    var busy by remember {mutableStateOf(false)}
    var filter by rememberSaveable {mutableStateOf("")}
    val context=LocalContext.current
    var history by remember(state.searchHistoryOwner()) {mutableStateOf(state.searchHistory())}
    LaunchedEffect(query,state.searchHistoryOwner()) {
        if(query.trim().length>=2) {delay(1200);state.rememberSearch(query.trim());history=state.searchHistory()}
    }
    val voice=rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {result->
        result.data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let {query=it}
    }
    LaunchedEffect(query,state.localExtrasScope(),state.interfaceLanguage) {
        state.searchQuery=query
        if(query.trim().length<2) {items=emptyList();busy=false;return@LaunchedEffect}
        busy=true
        try {delay(350);items=state.searchFlexible(query.trim())}
        catch(e:CancellationException) {throw e} catch(e:Exception) {state.error=e.message}
        finally {busy=false}
    }
    Column(Modifier.fillMaxSize().imePadding(),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Row(Modifier.padding(horizontal=16.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            MobileFieldRP133(query,{query=it},tr("Títulos, actores o directores","Titles, actors or directors"),modifier=Modifier.weight(1f))
            MobileIconButtonRP133("mic",tr("Buscar con voz","Voice search")) {
                runCatching {voice.launch(android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                    .putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL,android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    .putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE,if(state.interfaceLanguage=="es") "es-US" else "en-US")
                    .putExtra("android.speech.extra.ENABLE_LANGUAGE_DETECTION",true)
                    .putExtra("android.speech.extra.LANGUAGE_DETECTION_ALLOWED_LANGUAGES",arrayListOf("es-US","en-US")))}.onFailure {state.error=tr("No hay reconocimiento de voz disponible.","Voice recognition unavailable.")}
            }
        }
        LazyRow(contentPadding=PaddingValues(horizontal=16.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            items(listOf("" to tr("Todo","All"),"movie" to tr("Películas","Movies"),"series" to tr("Series","Series"))) {(id,title)->MobileButtonRP133(title,selected=filter==id) {filter=id}}
        }
        if(query.isBlank() && history.isNotEmpty()) LazyRow(contentPadding=PaddingValues(horizontal=16.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            items(history) {term->MobileButtonRP133(term,"search") {query=term}}
        }
        if(busy) Text(tr("Buscando…","Searching…"),color=MobileTintRP133,modifier=Modifier.padding(horizontal=16.dp))
        val visible=items.filter {filter.isBlank() || it.type==filter}
        if(query.length<2) Text(tr("Busca en español o inglés. También por actor o director.","Search in Spanish or English, including actors and directors."),color=Muted,modifier=Modifier.padding(20.dp))
        else if(!busy && visible.isEmpty()) Text(tr("No encontramos resultados.","No results found."),color=Muted,modifier=Modifier.padding(20.dp))
        LazyVerticalGrid(GridCells.Adaptive(110.dp),Modifier.weight(1f),contentPadding=PaddingValues(16.dp),horizontalArrangement=Arrangement.spacedBy(10.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
            gridItems(visible,key={it.cloudId}) {MobilePosterRP133(state,it,Modifier.fillMaxWidth())}
        }
    }
}

@Composable
internal fun MobileDetailsRP133(state:AppState) {
    val item=state.selected ?: return
    val detail=state.details
    val absolute=com.miflix.native2.data.AbsoluteEpisodeRP102.enabled(item.id)
    val context=LocalContext.current
    var friends by remember(item.cloudId) {mutableStateOf<List<com.miflix.native2.data.SocialPerson>>(emptyList())}
    var friendsOpen by remember {mutableStateOf(false)}
    var episodeMenu by remember {mutableStateOf<EpisodeSummary?>(null)}
    var castPersonRP141 by remember(item.cloudId) {mutableStateOf<CastMember?>(null)}
    castPersonRP141?.let {person->MobileFilmographyRP141(state,person) {castPersonRP141=null}}
    LaunchedEffect(item.cloudId,state.watchedFriendsOwnerRP93) {friends=runCatching {state.friendsWhoWatchedRP93(item.cloudId)}.getOrDefault(emptyList())}
    val warmup=state.detailsWarmupKeyRP90(item)
    LaunchedEffect(warmup) {if(warmup!=null) state.prefetchDetailsRP90(item,warmup)}
    if(friendsOpen) MobileSheetRP133(tr("Amigos que ya la vieron","Friends who watched"),{friendsOpen=false}) {
        LazyColumn {items(friends,key={it.id}) {person->Row(Modifier.padding(vertical=8.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            ProfileAvatar(Profile(person.id,person.nickname,person.avatar),Modifier.size(42.dp));Text(person.nickname,color=Color.White)
        }}}
    }
    episodeMenu?.let {ep->MobileSheetRP133(ep.title,{episodeMenu=null}) {
        MobileButtonRP133(tr("Reproducir","Play"),"play",Modifier.fillMaxWidth()) {episodeMenu=null;state.launch {state.playEpisode(ep)}}
        MobileButtonRP133(tr("Elegir fuente","Choose source"),"link",Modifier.fillMaxWidth()) {episodeMenu=null;state.launch {state.chooseEpisodeSources(ep)}}
        MobileButtonRP133(tr("Marcar visto","Mark watched"),"check",Modifier.fillMaxWidth()) {state.markWatched(item,ep.season,ep.episode);episodeMenu=null}
    }}
    LazyColumn(contentPadding=PaddingValues(bottom=24.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
        item("art") {Box(Modifier.fillMaxWidth().height(330.dp)) {
            AsyncImage(item.backdrop ?: item.poster,null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent,MobileBgRP133))))
            MobileIconButtonRP133("back",tr("Volver","Back"),Modifier.align(Alignment.TopStart).padding(16.dp)) {state.screen=state.detailsReturnScreen}
            if(friends.isNotEmpty()) MobileButtonRP133(friends.size.toString()+" "+tr("ya la vieron","watched"),"friends",Modifier.align(Alignment.TopEnd).padding(16.dp)) {friendsOpen=true}
            Text(item.title,color=Color.White,fontSize=32.sp,lineHeight=36.sp,fontWeight=FontWeight.Bold,modifier=Modifier.align(Alignment.BottomStart).padding(horizontal=16.dp),maxLines=3)
        }}
        item("about") {Column(Modifier.padding(horizontal=16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text(listOf(item.year,"★ "+String.format("%.1f",item.rating),detail?.runtimeText.orEmpty()).filter {it.isNotBlank()}.joinToString(" · "),color=Muted,fontSize=13.sp)
            Text(item.overview,color=Color.White,fontSize=15.sp,lineHeight=22.sp)
            val resume=state.progress[item.cloudId]
            if(item.type=="series" && (resume?.episode ?: 0)>0) Text(tr("Te quedaste en el episodio ","You left off at episode ")+resume?.episode,color=MobileTintRP133,fontSize=13.sp)
            MobileButtonRP133(tr("Reproducir","Play"),"play",Modifier.fillMaxWidth(),selected=true,enabled=!state.selectedUpcoming) {state.launch {
                if(item.type=="series") state.episodes.firstOrNull {it.season==(resume?.season ?: 1) && it.episode==(resume?.episode ?: 1)}?.let {state.playEpisode(it)} ?: state.playSeriesFromStart() else state.playMovie()
            }}
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly) {
                MobileDetailActionRP140(if(item.cloudId in state.favorites) "check" else "plus",tr("Mi lista","My list"),selected=item.cloudId in state.favorites) {state.toggleFavorite(item)}
                MobileDetailActionRP140("share",tr("Compartir","Share")) {shareMobileRP140(context,item,detail?.imdbId)}
                MobileDetailActionRP140("play",tr("Tráiler","Trailer")) {state.launch {
                    val trailer=detail?.trailers?.firstOrNull() ?: state.tmdb.previewTrailers(item).firstOrNull()
                    if(trailer==null) state.error=tr("No hay un tráiler disponible para este contenido.","No trailer is available for this title.")
                    else runCatching {context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW,android.net.Uri.parse(trailer.watchUrl)))}.onFailure {state.error=tr("No se pudo abrir el tráiler.","Unable to open the trailer.")}
                }}
                if(!state.kidsMode && !state.easyModeRP79) MobileDetailActionRP140("friends","Party") {state.launch {if(state.createWatchParty(true)!=null) state.screen=Screen.WATCH_PARTY}}
                if(!state.easyModeRP79) MobileDetailActionRP140("link",tr("Fuentes","Sources")) {state.launch {if(item.type=="series") state.episodes.firstOrNull()?.let {state.chooseEpisodeSources(it)} else state.chooseCurrentSources()}}
            }
        }}
        if(!state.kidsMode) item("download") {
            Column(Modifier.padding(horizontal=16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                MobileButtonRP133(if(item.type=="series") tr("Descargar temporada seleccionada","Download selected season") else tr("Descargar película","Download movie"),"download",Modifier.fillMaxWidth(),enabled=!state.mobileDownloadBusyRP134 && !state.selectedUpcoming) {
                    state.launch {if(item.type=="series") state.downloadMobileSeasonRP134() else state.downloadMobileItemRP134(item)}
                }
                if(state.mobileDownloadStatusRP134.isNotBlank()) Text(state.mobileDownloadStatusRP134,color=Muted,fontSize=12.sp)
            }
        }
        if(item.type=="series") {
            item("seasons") {LazyRow(contentPadding=PaddingValues(horizontal=16.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                if(absolute) item {Text(tr("Serie completa","Complete series"),color=Color.White,fontSize=18.sp)} else items((1..(detail?.seasonCount ?: 1).coerceAtLeast(1)).toList()) {season->MobileButtonRP133(tr("Temporada ","Season ")+season,selected=season==state.currentSeason) {state.launch {state.loadSeason(season)}}}
            }}
            item("episodes") {LazyRow(contentPadding=PaddingValues(horizontal=16.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                items(state.episodes,key={"${it.season}:${it.episode}"}) {ep->Column(Modifier.width(245.dp).combinedClickable(onClick={state.launch {state.playEpisode(ep)}},onLongClick={episodeMenu=ep})) {
                    Box(Modifier.fillMaxWidth().aspectRatio(16f/9f).clip(RoundedCornerShape(12.dp)).background(MobilePanelRP133)) {
                        AsyncImage(ep.still,ep.title,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
                        if(state.episodeWatched(item,ep)) Box(Modifier.align(Alignment.TopEnd).padding(8.dp).background(MobileTintRP133,RoundedCornerShape(20.dp)).padding(4.dp)) {MobileIconRP133("check",Modifier.size(18.dp))}
                    }
                    Text("${ep.absoluteEpisodeRP102.takeIf {it>0} ?: ep.episode}. ${ep.title}",color=Color.White,fontSize=14.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=7.dp),maxLines=2)
                    Text(ep.overview,color=Muted,fontSize=12.sp,maxLines=3,overflow=TextOverflow.Ellipsis,modifier=Modifier.padding(top=4.dp))
                }}
            }}
        }
        if(!detail?.cast.isNullOrEmpty()) item("cast") {Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Text(tr("Reparto","Cast"),color=Color.White,fontSize=19.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(horizontal=16.dp))
            LazyRow(contentPadding=PaddingValues(horizontal=16.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                items(detail?.cast.orEmpty(),key={it.name+it.id}) {person->Column(Modifier.width(92.dp).clickable(enabled=person.id>0) {castPersonRP141=person}) {
                    AsyncImage(person.profile,person.name,Modifier.size(80.dp).clip(RoundedCornerShape(40.dp)).background(MobilePanelRP133),contentScale=ContentScale.Crop)
                    Text(person.name,color=Color.White,fontSize=12.sp,maxLines=2,modifier=Modifier.padding(top=6.dp))
                }}
            }
        }}
    }
}

@Composable
internal fun MobileCatalogGridRP133(state:AppState,title:String,rows:List<MediaSummary>,more:(()->Unit)?=null) {
    LazyVerticalGrid(GridCells.Adaptive(110.dp),contentPadding=PaddingValues(16.dp),horizontalArrangement=Arrangement.spacedBy(10.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        item(span={GridItemSpan(maxLineSpan)}) {Text(title,color=Color.White,fontSize=23.sp,fontWeight=FontWeight.Bold)}
        gridItems(rows,key={it.cloudId}) {MobilePosterRP133(state,it,Modifier.fillMaxWidth())}
        if(more!=null) item(span={GridItemSpan(maxLineSpan)}) {MobileButtonRP133(tr("Cargar más","Load more"),"plus",Modifier.fillMaxWidth(),click=more)}
    }
}
