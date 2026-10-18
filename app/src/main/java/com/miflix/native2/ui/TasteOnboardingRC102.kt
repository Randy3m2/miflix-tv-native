package com.miflix.native2.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.miflix.native2.data.DirectorPerson
import com.miflix.native2.model.MediaSummary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
internal fun TasteOnboardingRC102(state:AppState,close:()->Unit) {
    val owner=state.localExtrasScope()
    val titles=remember(owner) {mutableStateMapOf<String,MediaSummary>().apply {state.tasteProfile.seeds(owner).forEach {put(it.cloudId,it)}}}
    val people=remember(owner) {mutableStateMapOf<Int,DirectorPerson>().apply {state.tasteProfile.actors(owner).forEach {put(it.id,it)}}}
    var genres by remember(owner) {mutableStateOf(state.tasteProfile.genres(owner))}
    var tab by remember(owner) {mutableStateOf("movie")}
    var query by remember(owner) {mutableStateOf("")}
    var searchRows by remember(owner) {mutableStateOf<List<MediaSummary>?>(null)}
    var actors by remember(owner) {mutableStateOf<List<DirectorPerson>>(emptyList())}
    var loading by remember(owner) {mutableStateOf(false)}
    var message by remember(owner) {mutableStateOf("")}
    var visibleCount by remember(owner,tab) {mutableIntStateOf(30)}
    val scope=rememberCoroutineScope()
    fun skip() {if(owner==state.localExtrasScope()) state.tasteProfile.skip(owner);close()}
    LaunchedEffect(owner) {if(state.movies.isEmpty() && state.series.isEmpty()) runCatching {state.loadHome()}}
    LaunchedEffect(owner,tab,state.interfaceLanguage) {
        query="";searchRows=null;message=""
        if(tab=="actors" || tab=="novelas") {
            loading=true
            try {if(tab=="actors") actors=state.onboardingActors() else state.loadNovelasHome()}
            catch(e:CancellationException) {throw e} catch(_:Exception) {message=tr("No se pudo cargar esta selección. Puedes reintentar o seguir con las otras.","This selection could not load. Retry or continue with the others.")}
            finally {loading=false}
        }
    }
    val movies=(state.movies+state.topRated+state.trending+titles.values).filter {it.type=="movie"}.distinctBy {it.cloudId}
    val series=(state.series+state.bestSeries+state.trending+titles.values).filter {it.type=="series"}.distinctBy {it.cloudId}
    val rows=searchRows ?: when(tab) {"movie"->movies;"series"->series;"novelas"->if(state.novelasMatch) state.novelasPicks.toList() else emptyList();else ->emptyList()}
    Dialog(onDismissRequest={skip()},properties=DialogProperties(usePlatformDefaultWidth=false,decorFitsSystemWindows=false)) {
        val first=remember {FocusRequester()}
        LaunchedEffect(Unit) {withFrameNanos {};runCatching {first.requestFocus()}}
        Column(Modifier.fillMaxSize().consumeOpeningPress().background(Brush.linearGradient(listOf(Color(0xff141426),Color(0xff08090e),Color(0xff111b23)))).padding(horizontal=34.dp,vertical=22.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                ProfileAvatar(state.activeProfile,Modifier.size(54.dp))
                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    Text("BRUNIO · "+tr("HECHO PARA TI","MADE FOR YOU"),color=Accent,fontSize=11.sp,fontWeight=FontWeight.Bold)
                    Text(tr("¿Qué te encanta ver?","What do you love watching?"),color=Color.White,fontSize=30.sp,fontWeight=FontWeight.Black)
                    Text(tr("Elige títulos y personas que te gusten. Tu Home empieza contigo.","Pick titles and people you love. Your Home starts with you."),color=Muted,fontSize=13.sp)
                }
                Column(horizontalAlignment=Alignment.End) {
                    Text(state.activeProfile.name,color=Color.White,fontSize=16.sp,fontWeight=FontWeight.Bold)
                    Text("${titles.size+people.size+genres.size} "+tr("elegidos","selected"),color=Accent,fontSize=14.sp)
                }
            }
            Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                listOf("movie" to tr("Películas","Movies"),"series" to tr("Series","Series"),"novelas" to tr("Novelas","Telenovelas"),"actors" to tr("Actores","Actors"),"genres" to tr("Géneros","Genres")).forEachIndexed {index,(id,label) ->
                    FocusButton(label,primary=tab==id,modifier=if(index==0) Modifier.focusRequester(first) else Modifier) {tab=id}
                }
            }
            if(tab!="genres") Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                NativeTextField(query,{query=it.take(80)},if(tab=="actors") tr("Busca un actor o actriz","Find an actor") else tr("Busca un título que te guste","Find a title you love"),modifier=Modifier.weight(1f))
                if(!loading) FocusButton(tr("Buscar","Search")) {
                    val text=query.trim();val currentTab=tab
                    if(text.length>=2) scope.launch {
                        loading=true;message=""
                        try {if(currentTab=="actors") {val result=state.onboardingActors(text);if(currentTab==tab) actors=result}
                            else {val result=state.tmdb.search(text).filter {it.type==if(currentTab=="movie") "movie" else "series"};if(currentTab==tab) searchRows=result}}
                        catch(e:CancellationException) {throw e} catch(_:Exception) {message=tr("No se pudo buscar. Reintenta.","Search failed. Try again.")}
                        finally {loading=false}
                    }
                }
                if(searchRows!=null || query.isNotBlank()) FocusButton(tr("Limpiar","Clear")) {query="";searchRows=null;if(tab=="actors") scope.launch {runCatching {state.onboardingActors()}.onSuccess {actors=it}}}
            }
            if(loading) Text(tr("Preparando tu selección…","Preparing your selection…"),color=Accent,fontSize=13.sp)
            if(message.isNotBlank()) Text(message,color=Muted,fontSize=13.sp)
            LazyVerticalGrid(columns=GridCells.Adaptive(if(tab=="genres") 200.dp else 126.dp),modifier=Modifier.weight(1f).fillMaxWidth(),
                contentPadding=PaddingValues(top=8.dp,bottom=16.dp,start=5.dp,end=5.dp),horizontalArrangement=Arrangement.spacedBy(16.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
                if(tab=="actors") items(actors.take(visibleCount),key={it.id}) {actor ->
                    TasteChoiceCardRC102(actor.name,actor.photo,tr("ACTOR / ACTRIZ","ACTOR"),actor.id in people) {
                        if(actor.id in people) people.remove(actor.id) else if(people.size<12) people[actor.id]=actor else message=tr("Puedes elegir hasta 12 actores.","You can choose up to 12 actors.")
                    }
                } else if(tab=="genres") items(TasteGenresRC102,key={it.key}) {genre ->
                    val selected=genre.key in genres
                    val image=state.genreCovers[genre.key] ?: (if(genre.key=="soap") state.novelasPicks.firstOrNull()?.poster else movies.firstOrNull()?.backdrop)
                    TasteChoiceCardRC102(tr(genre.es,genre.en),image,tr("GÉNERO","GENRE"),selected,landscape=true) {genres=if(selected) genres-genre.key else genres+genre.key}
                } else items(rows.take(visibleCount),key={it.cloudId}) {item ->
                    TasteChoiceCardRC102(item.title,item.poster ?: item.backdrop,item.year,item.cloudId in titles) {
                        if(item.cloudId in titles) titles.remove(item.cloudId) else if(titles.size<30) titles[item.cloudId]=item else message=tr("Puedes elegir hasta 30 títulos.","You can choose up to 30 titles.")
                    }
                }
                if((if(tab=="actors") actors.size else rows.size)>visibleCount) item {FocusButton(tr("Ver más","See more")) {visibleCount+=30}}
                if(!loading && tab!="genres" && (if(tab=="actors") actors.isEmpty() else rows.isEmpty())) item {
                    Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
                        Text(tr("Prueba una búsqueda o elige otra categoría.","Try a search or choose another category."),color=Muted)
                        if(tab=="novelas") FocusButton(tr("Reintentar","Retry")) {scope.launch {state.loadNovelasHome()}}
                        if(tab=="actors") FocusButton(tr("Reintentar","Retry")) {scope.launch {runCatching {state.onboardingActors()}.onSuccess {actors=it}}}
                    }
                }
            }
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)) {
                Text(tr("Puedes omitirlo y editar tus gustos después en Settings.","You can skip this and edit your tastes later in Settings."),color=Muted,fontSize=12.sp,modifier=Modifier.weight(1f))
                FocusButton(tr("Omitir por ahora","Skip for now")) {skip()}
                FocusButton(tr("Crear mi selección","Make it mine"),primary=true) {
                    if(owner==state.localExtrasScope()) state.tasteProfile.configure(owner,titles.values.toList(),people.values.toList(),genres)
                    close()
                }
            }
        }
    }
}

@Composable
private fun TasteChoiceCardRC102(title:String,image:String?,subtitle:String,selected:Boolean,landscape:Boolean=false,onClick:()->Unit) {
    var focused by remember {mutableStateOf(false)}
    val scale by animateFloatAsState(if(focused) 1.035f else 1f,tween(150),label="taste-card-scale")
    val shape=RoundedCornerShape(16.dp)
    Box(Modifier.fillMaxWidth().aspectRatio(if(landscape) 1.65f else 2f/3f).graphicsLayer {scaleX=scale;scaleY=scale}
        .onFocusChanged {focused=it.isFocused}.clip(shape).background(Brush.linearGradient(listOf(Color(0xff26394d),Color(0xff281d35))))
        .border(if(focused || selected) 3.dp else 1.dp,if(focused) Color.White else if(selected) Accent else Color.White.copy(alpha=.12f),shape)
        .clickable(onClick=onClick)) {
        if(image!=null) AsyncImage(image,contentDescription=title,contentScale=ContentScale.Crop,modifier=Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent,Color.Black.copy(alpha=.1f),Color.Black.copy(alpha=.95f)))))
        if(selected) Box(Modifier.align(Alignment.TopEnd).padding(9.dp).size(27.dp).background(Accent,CircleShape),contentAlignment=Alignment.Center) {
            Text("✓",color=Color.Black,fontSize=18.sp,fontWeight=FontWeight.Bold)
        }
        Column(Modifier.align(Alignment.BottomStart).padding(12.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
            Text(title,color=Color.White,fontSize=14.sp,fontWeight=FontWeight.Bold,maxLines=2,overflow=TextOverflow.Ellipsis)
            Text(subtitle,color=if(selected) Accent else Color(0xffd4d7e2),fontSize=10.sp,maxLines=1)
        }
    }
}
