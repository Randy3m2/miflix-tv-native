@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.miflix.native2.ui

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.miflix.native2.data.*
import com.miflix.native2.model.*
import kotlinx.coroutines.*
import java.time.LocalDate

private val mobileLeagueMemoryRP133=linkedMapOf<String,String>()

@Composable
internal fun MobileLiveRP133(state:AppState,select:((PlayerRequest)->Unit)?=null,initial:String="TV") {
    val liveOwner=state.localExtrasScope()
    var league by rememberSaveable(liveOwner) {mutableStateOf(if(initial!="TV") initial else mobileLeagueMemoryRP133[liveOwner] ?: initial)}
    val latestLeague by rememberUpdatedState(league)
    DisposableEffect(liveOwner) {onDispose {mobileLeagueMemoryRP133.remove(liveOwner);mobileLeagueMemoryRP133[liveOwner]=latestLeague;while(mobileLeagueMemoryRP133.size>6) mobileLeagueMemoryRP133.remove(mobileLeagueMemoryRP133.keys.first())}}

    Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        LazyRow(contentPadding=PaddingValues(horizontal=16.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            items(listOf("TV","NBA","MLB","F1","LIDOM","Others")) {key->MobileButtonRP133(if(key=="TV") "Live TV" else if(key=="F1") "F1 · MotoGP" else key,selected=league==key) {league=key}}
        }
        if(league=="TV") MobileChannelsRP133(state,select)
        else MobileSportsRP133(state,league,select)
    }
}

@Composable
internal fun MobileChannelsRP133(state:AppState,select:((PlayerRequest)->Unit)?=null) {
    val context=LocalContext.current
    val owner=CustomChannelsRP45.owner(state)
    val revision by CustomChannelsRP45.changesRP48.collectAsState()
    val favoriteRevision=LiveLibrary.revision
    var channels by remember(owner) {mutableStateOf<List<CustomChannelRP45>>(emptyList())}
    var favorites by remember(owner) {mutableStateOf<List<FavoriteChannel>>(emptyList())}
    val saved=remember(owner) {LiveChannelNavigationRP40.matching(state,state.playerRequest)}
    var category by rememberSaveable(owner) {mutableStateOf(saved?.category?.value?.name ?: "Favorites")}
    var query by rememberSaveable(owner) {mutableStateOf(saved?.search ?: "")}
    var count by rememberSaveable(category,query) {mutableIntStateOf((saved?.offset ?: 10).coerceAtLeast(10))}
    val scroll=rememberLazyListState(saved?.scrollIndex ?: 0,saved?.scrollOffset ?: 0)
    var initialCategory by remember {mutableStateOf(true)}
    LaunchedEffect(category,query) {if(initialCategory) initialCategory=false else {count=10;scroll.scrollToItem(0)}}
    var options by remember {mutableStateOf<CustomChannelRP45?>(null)}
    LaunchedEffect(owner,revision,favoriteRevision) {
        val snapshot=withContext(Dispatchers.IO) {CustomChannelsRP45.list(context,owner,true) to LiveLibrary.favorites(context,state)}
        channels=snapshot.first;favorites=snapshot.second
    }
    val favoriteIds=remember(favorites) {favorites.map {it.channel.id}.toSet()}
    val visible=remember(channels,category,query,favoriteIds) {
        channels.filter {(category!="Favorites" || it.id in favoriteIds) &&
            (category=="Favorites" || category=="All" || it.category==category) &&
            (query.isBlank() || it.name.contains(query,true) || it.number.toString()==query.trim())}
    }
    fun tune(channel:CustomChannelRP45) {
        val request=CustomChannelsRP45.request(channel)
        val cat=LiveCategory("tv","mobile:"+category,category,genre=category)
        LiveChannelNavigationRP40.save(state,LiveBrowseSnapshotRP40(BrowserCategoryRP34(CustomChannelsRP45.provider,cat,""),
            visible.map {BrowserChannelRP34(CustomChannelsRP45.provider,LiveChannel(it.id,"tv",it.name,it.logo,it.category,it.number))},query,count,count<visible.size,scroll.firstVisibleItemIndex,scroll.firstVisibleItemScrollOffset))
        if(select!=null) select(request) else state.openLivePlayer(request)
    }
    options?.let {channel->MobileSheetRP133(channel.name,{options=null}) {
        MobileButtonRP133(tr("Reproducir","Play"),"play",Modifier.fillMaxWidth()) {options=null;tune(channel)}
        MobileButtonRP133(tr("Favorito","Favorite"),"heart",Modifier.fillMaxWidth(),selected=channel.id in favoriteIds) {LiveLibrary.toggle(context,state,CustomChannelsRP45.provider,LiveChannel(channel.id,"tv",channel.name,channel.logo,channel.category,channel.number))}
        MobileButtonRP133(tr("Eliminar canal","Remove channel"),"close",Modifier.fillMaxWidth()) {state.launch {withContext(Dispatchers.IO) {CustomChannelsRP45.remove(context,owner,channel.id)};options=null}}
    }}
    Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        MobileFieldRP133(query,{query=it},tr("Buscar canales…","Search channels…"),modifier=Modifier.padding(horizontal=16.dp))
        LazyRow(contentPadding=PaddingValues(horizontal=16.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            items(listOf("Favorites")+channels.map {it.category}.distinct()+"All") {cat->
                MobileButtonRP133(when(cat) {"Favorites"->tr("Favoritos","Favorites");"All"->tr("Todos","All");else->cat},selected=cat==category) {category=cat}
            }
        }
        LazyColumn(Modifier.weight(1f),state=scroll,contentPadding=PaddingValues(horizontal=16.dp,vertical=6.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            items(visible.take(count),key={it.id}) {channel->
                Row(Modifier.fillMaxWidth().mobileGlassRP133(state.playerRequest?.liveChannelId==channel.id)
                    .combinedClickable(onClick={tune(channel)},onLongClick={options=channel}).padding(12.dp),
                    verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(56.dp).clip(RoundedCornerShape(10.dp)).background(Color.Black.copy(alpha=.3f)),contentAlignment=Alignment.Center) {
                        if(channel.logo.isNullOrBlank()) Text(channel.name.take(3),color=Color.White,fontWeight=FontWeight.Bold)
                        else AsyncImage(channel.logo,null,Modifier.fillMaxSize().padding(3.dp),contentScale=ContentScale.Fit)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(channel.name,color=Color.White,fontSize=15.sp,fontWeight=FontWeight.Bold,maxLines=2,overflow=TextOverflow.Ellipsis)
                        Text(channel.number.toString()+" · "+channel.category,color=Muted,fontSize=11.sp,maxLines=1)
                    }
                    MobileIconButtonRP133("heart",tr("Favorito","Favorite"),selected=channel.id in favoriteIds) {
                        LiveLibrary.toggle(context,state,CustomChannelsRP45.provider,LiveChannel(channel.id,"tv",channel.name,channel.logo,channel.category,channel.number))
                    }
                }
            }
            if(visible.isEmpty()) item {Text(tr("No hay canales aquí. Selecciona otra categoría.","No channels here. Choose another category."),color=Muted,modifier=Modifier.padding(16.dp))}
            if(count<visible.size) item {MobileButtonRP133(tr("Cargar 10 más","Load 10 more"),"plus",Modifier.fillMaxWidth()) {count+=10}}
        }
    }
}

@Composable
private fun MobileSportsRP133(state:AppState,league:String,select:((PlayerRequest)->Unit)?) {
    var tab by rememberSaveable(league) {mutableStateOf("Games")}
    var subscriptions by remember {mutableStateOf(false)}
    Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        LazyRow(contentPadding=PaddingValues(horizontal=16.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            items(listOf("Games","Table","Stats","Players")) {key->MobileButtonRP133(key,selected=tab==key) {tab=key}}
            if(league!="Others") item {MobileButtonRP133(tr("Seguir","Follow"),"plus") {subscriptions=true}}
        }
        if(tab=="Games") {
            if(league=="LIDOM") MobileLidomRP133(state,select)
            else MobileSportGamesRP133(state,league,select)
        } else if(league=="Others") Text(tr("Selecciona una liga para ver sus estadísticas.","Select a league for statistics."),color=Muted,modifier=Modifier.padding(20.dp))
        else MobileLeagueDataRP133(league,tab)
    }
    if(subscriptions) MobileSubscriptionsRP133(state,league) {subscriptions=false}
}

@Composable
private fun MobileSportGamesRP133(state:AppState,league:String,select:((PlayerRequest)->Unit)?) {
    val repo=remember {LiveRepository()}
    var categories by remember(league) {mutableStateOf<List<LiveCategory>>(emptyList())}
    var category by remember(league) {mutableStateOf<LiveCategory?>(null)}
    var games by remember(league) {mutableStateOf<List<LiveChannel>>(emptyList())}
    var busy by remember {mutableStateOf(false)}
    var message by remember {mutableStateOf("")}
    var skip by remember(league) {mutableIntStateOf(0)}
    var nextSkip by remember(league) {mutableIntStateOf(0)}
    var more by remember(league) {mutableStateOf(false)}
    var resolving by remember {mutableStateOf(false)}
    val owner=state.localExtrasScope()
    var links by remember {mutableStateOf<Pair<LiveChannel,List<StreamChoice>>?>(null)}
    val manifest=state.calendarLiveManifest("sports")
    val section=when(league) {"NBA"->"basketball";"MLB"->"baseball";"F1"->"f1";else->"others"}
    LaunchedEffect(manifest,league) {
        busy=true;message=""
        try {categories=repo.categories(manifest).filter {
            if(section=="others") (SportsRecordingPolicyRP72.recording(it.name,it.id,it.genre) || liveSportGroupRP41(it) in listOf("others","soccer","football")) && !liveAggregateRP41(it)
            else liveSportGroupRP41(it)==section && SportsBrowsePolicyRP121.leagueEvent(section,it.name+" "+it.genre.orEmpty())
        };category=categories.firstOrNull()}
        catch(e:CancellationException) {throw e} catch(e:Exception) {message=e.message.orEmpty()}
        finally {busy=false}
    }
    LaunchedEffect(category,skip,manifest) {
        val cat=category ?: return@LaunchedEffect
        busy=true;message=""
        try {
            val page=repo.channels(manifest,cat,skip)
            val allowed=page.filter {SportsBrowsePolicyRP121.leagueEvent(section,it.name+" "+it.description+" "+cat.name+" "+cat.genre.orEmpty())}
            games=(if(skip==0) allowed else games+allowed).distinctBy {it.id}
            nextSkip=skip+page.size;more=cat.paginated && page.isNotEmpty()
        }
        catch(e:CancellationException) {throw e} catch(e:Exception) {message=e.message.orEmpty()}
        finally {busy=false}
    }
    fun play(channel:LiveChannel,stream:StreamChoice,all:List<StreamChoice>) {
        if(owner!=state.localExtrasScope()) return
        val req=PlayerRequest(MediaSummary(channel.id.hashCode(),"live",channel.name,channel.description,channel.poster,channel.poster,0.0,"LIVE"),
            stream,0,0,0L,live=true,liveChannelId=channel.id,liveProvider="sports",liveType=channel.type,returnScreen=Screen.LIVE_TV,
            sportsRecordingRP72=SportsRecordingPolicyRP72.recording(category?.name.orEmpty(),category?.id.orEmpty(),category?.genre,channel.name,channel.description))
        if(select!=null) select(req) else state.openLivePlayer(if(all.isEmpty()) req else state.sportsRequestRP71(req,all.take(5)))
    }
    links?.let {(channel,streams)->MobileSheetRP133(channel.name,{links=null}) {
        LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp)) {itemsIndexed(streams) {_,stream->MobileButtonRP133(stream.name+" · "+stream.title,"play",Modifier.fillMaxWidth()) {links=null;play(channel,stream,emptyList())}}}
    }}
    var recordingFilter by rememberSaveable(league,category?.id,category?.genre) {mutableStateOf("all")}
    val recording=remember(league,category,games) {MobileLatestRP139.recordingMode(league,category,games)}
    val recordingKeys=remember(games,category) {games.associate {it.id to MobileLatestRP139.recordingGroup(it,category)}}
    val recordingGroups=remember(recordingKeys) {recordingKeys.values.distinct()}
    val shown=remember(games,recording,recordingFilter,recordingKeys) {if(recording && recordingFilter!="all") games.filter {recordingKeys[it.id]==recordingFilter} else games}
    LazyVerticalGrid(GridCells.Adaptive(160.dp),Modifier.fillMaxSize(),contentPadding=PaddingValues(16.dp),horizontalArrangement=Arrangement.spacedBy(12.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        item(span={GridItemSpan(maxLineSpan)},key="sport-controls") {
        Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        if(categories.size>1) LazyRow(contentPadding=PaddingValues(horizontal=16.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {items(categories,key={it.id+it.genre}) {cat->MobileButtonRP133(cat.genre ?: cat.name,selected=cat==category) {skip=0;games=emptyList();category=cat}}}
        if(busy) Text(tr("Cargando eventos…","Loading events…"),color=MobileTintRP133,modifier=Modifier.padding(horizontal=16.dp))
        if(message.isNotBlank()) Text(message,color=Muted,modifier=Modifier.padding(horizontal=16.dp))
        if(recording) {
            Text(tr("Grabaciones por deporte","Recordings by sport"),color=Muted,fontSize=13.sp)
            LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                items(listOf("all")+recordingGroups,key={it}) {sport->MobileButtonRP133(when(sport) {
                    "all"->tr("Todos","All");"basketball"->"NBA";"baseball"->tr("Béisbol","Baseball");"football"->"NFL";"soccer"->tr("Fútbol","Soccer");"f1"->"F1 / MotoGP";"combat"->tr("Combate","Combat");"tennis"->tr("Tenis","Tennis");"hockey"->"Hockey";else->tr("Otros","Other")
                },selected=recordingFilter==sport) {recordingFilter=sport}}
            }
        }
        }
        }
            gridItems(shown,key={it.id}) {game->Column(Modifier.mobileGlassRP133().combinedClickable(onClick={state.launch {
                if(!resolving) {resolving=true;try {
                    val streams=repo.streams(manifest,game);require(streams.isNotEmpty()) {tr("Sin fuentes disponibles","No sources available")};play(game,streams.first(),streams)
                } finally {resolving=false}}
            }},onLongClick={state.launch {links=game to repo.streams(manifest,game)}})) {
                Box(Modifier.fillMaxWidth().aspectRatio(16f/9f)) {
                    AsyncImage(game.poster,game.name,Modifier.fillMaxSize().clip(RoundedCornerShape(topStart=16.dp,topEnd=16.dp)),contentScale=ContentScale.Crop)
                    if(MotoGpCalendarRP138.matches(game.name+" "+category?.name.orEmpty()+" "+category?.genre.orEmpty())) EventLogoRP120(CalendarSourcesRP120.logo("MOTOGP"),"MotoGP",Modifier.align(Alignment.TopStart).padding(6.dp).size(32.dp))
                }
                Text(game.name,color=Color.White,fontSize=14.sp,fontWeight=FontWeight.Bold,maxLines=3,modifier=Modifier.padding(10.dp))
                Text(SportsBrowsePolicyRP121.startLabel(game.startsAtRP121),color=Muted,fontSize=11.sp,modifier=Modifier.padding(horizontal=10.dp,vertical=6.dp))
            }}
            if(more && !busy) item(span={GridItemSpan(maxLineSpan)}) {MobileButtonRP133(tr("Cargar más juegos","Load more games"),"plus",Modifier.fillMaxWidth()) {skip=nextSkip}}
            if(!busy && shown.isEmpty()) item(span={GridItemSpan(maxLineSpan)}) {Text(tr("La fuente aún no ha publicado eventos.","No events published by this source yet."),color=Muted)}
    }
}

@Composable
private fun MobileLidomRP133(state:AppState,select:((PlayerRequest)->Unit)?) {
    val context=LocalContext.current
    val repo=remember(state.localExtrasScope()) {LidomRepository(context,state.localExtrasScope())}
    var day by rememberSaveable {mutableStateOf(LocalDate.now().toString())}
    var games by remember {mutableStateOf<List<LidomGame>>(emptyList())}
    var team by remember {mutableStateOf<String?>(null)}
    LaunchedEffect(day) {try {games=repo.games(day)} catch(e:CancellationException) {throw e} catch(e:Exception) {state.error=e.message}}
    team?.let {id->LidomMultiViewPickerRP9(state,id,select={req->team=null;if(select!=null) select(req) else state.openLivePlayer(req)},close={team=null})}
    LazyColumn(contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item {Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically) {
            MobileIconButtonRP133("back",tr("Día anterior","Previous day")) {day=LocalDate.parse(day).minusDays(1).toString()}
            Text(day,color=Color.White,modifier=Modifier.weight(1f))
            MobileIconButtonRP133("chevron",tr("Día siguiente","Next day")) {day=LocalDate.parse(day).plusDays(1).toString()}
        }}
        items(games,key={it.id}) {game->Column(Modifier.fillMaxWidth().mobileGlassRP133().padding(14.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Text(game.time+" · "+game.status,color=Muted,fontSize=12.sp)
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceEvenly) {
                AsyncImage(game.away.logo,game.away.name,Modifier.size(48.dp).clickable {team=game.away.id})
                Text("vs",color=Color.White)
                AsyncImage(game.home.logo,game.home.name,Modifier.size(48.dp).clickable {team=game.home.id})
            }
            Text(game.away.name+" vs "+game.home.name,color=Color.White,fontSize=14.sp)
            MobileButtonRP133(tr("Elegir transmisión","Choose broadcast"),"play",Modifier.fillMaxWidth()) {team=game.home.id}
        }}
        item {Text(tr("Canales de equipos","Team channels"),color=Color.White,fontSize=19.sp,fontWeight=FontWeight.Bold)}
        items(repo.teams,key={it.id}) {t->MobileButtonRP133(t.name,"play",Modifier.fillMaxWidth()) {team=t.id}}
    }
}

@Composable
private fun MobileLeagueDataRP133(league:String,tab:String) {
    val context=LocalContext.current.applicationContext
    var groups by remember(league,tab) {mutableStateOf<List<StandingGroupRP120>>(emptyList())}
    var leaders by remember(league,tab) {mutableStateOf<List<SportsLeadersRP121>>(emptyList())}
    var players by remember(league,tab) {mutableStateOf<List<SportsPersonRP121>>(emptyList())}
    var busy by remember {mutableStateOf(true)}
    var message by remember {mutableStateOf("")}
    LaunchedEffect(league,tab) {
        busy=true
        try {when(tab) {
            "Table"->groups=StandingsRP120.load(context,league,SportsBrowseRepositoryRP121.season(league)).first
            "Stats"->leaders=SportsBrowseRepositoryRP121.leaders(context,league).rows
            else->players=SportsBrowseRepositoryRP121.players(context,league).rows
        }} catch(e:CancellationException) {throw e} catch(e:Exception) {message=e.message.orEmpty()}
        finally {busy=false}
    }
    LazyColumn(contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        if(busy) item {Text(tr("Cargando…","Loading…"),color=MobileTintRP133)}
        if(message.isNotEmpty()) item {Text(message,color=Muted)}
        groups.forEach {group->
            item {Text(group.title,color=Color.White,fontWeight=FontWeight.Bold,fontSize=19.sp)}
            item {Row(Modifier.fillMaxWidth()) {Text(tr("Equipo","Team"),color=Muted,fontSize=11.sp,modifier=Modifier.weight(2f));group.columns.forEach {Text(it,color=Muted,fontSize=11.sp,modifier=Modifier.weight(.6f))}}}
            items(group.rows,key={group.title+it.id}) {row->Row(Modifier.fillMaxWidth().mobileGlassRP133().padding(10.dp),verticalAlignment=Alignment.CenterVertically) {
                Row(Modifier.weight(2f),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)) {Text(row.rank,color=Muted,fontSize=12.sp);AsyncImage(row.logo,row.name,Modifier.size(28.dp));Text(row.name,color=Color.White,fontSize=12.sp,maxLines=2,modifier=Modifier.weight(1f))}
                row.values.forEach {Text(it,color=Color.White,fontSize=12.sp,modifier=Modifier.weight(.6f))}
            }}
        }
        leaders.forEach {leader->
            item {Text(leader.title,color=Color.White,fontSize=19.sp,fontWeight=FontWeight.Bold)}
            items(leader.rows,key={leader.key+it.person.id}) {row->MobileSportPersonRP133(row.person,row.value)}
        }
        items(players,key={it.id}) {person->MobileSportPersonRP133(person,person.position)}
    }
}

@Composable
private fun MobileSportPersonRP133(person:SportsPersonRP121,value:String) {
    Row(Modifier.fillMaxWidth().mobileGlassRP133().padding(10.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
        AsyncImage(person.photo.ifBlank {person.logo},person.name,Modifier.size(42.dp).clip(RoundedCornerShape(21.dp)),contentScale=ContentScale.Crop)
        Column(Modifier.weight(1f)) {Text(person.name,color=Color.White,fontSize=14.sp);Text(person.team,color=Muted,fontSize=11.sp)}
        Text(value,color=Color.White,fontWeight=FontWeight.Bold,fontSize=17.sp)
    }
}
