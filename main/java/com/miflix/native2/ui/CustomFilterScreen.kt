package com.miflix.native2.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.miflix.native2.data.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

@Composable
fun CustomFilterScreen(state: AppState) {
    var edit by remember { mutableStateOf(state.appliedCustomFilter==null) }
    val grid=rememberLazyGridState(state.customFilterScrollIndex,state.customFilterScrollOffset)
    var previous by remember { mutableStateOf(state.appliedCustomFilter) }
    LaunchedEffect(state.appliedCustomFilter) {
        if(previous!=state.appliedCustomFilter) { grid.scrollToItem(0);previous=state.appliedCustomFilter }
    }
    DisposableEffect(grid) { onDispose { state.customFilterScrollIndex=grid.firstVisibleItemIndex;state.customFilterScrollOffset=grid.firstVisibleItemScrollOffset } }
    BackHandler { state.screen=Screen.COLLECTIONS }
    AppShell(state,Screen.CUSTOM_FILTER,{ state.screen=it }) {
        Column(Modifier.fillMaxSize().padding(horizontal=34.dp,vertical=24.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
            Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                Text(tr("Filtro personalizado","Custom filter"),color=Color.White,fontSize=27.sp,modifier=Modifier.weight(1f))
                FocusButton(tr("Editar filtros","Edit filters")) { if(!state.customFilterLoading) edit=true }
                FocusButton(tr("Limpiar","Clear")) { if(!state.customFilterLoading) state.launch { state.applyCustomFilter(CustomCatalogFilter()) } }
                FocusButton(tr("Atrás","Back")) { state.screen=Screen.COLLECTIONS }
            }
            val applied=state.appliedCustomFilter
            if(applied!=null) Text(filterSummary(applied),color=Muted,fontSize=14.sp,maxLines=3)
            if(state.customFilterLoading) Text(tr("Buscando…","Searching…"),color=Color.White)
            if(state.customFilterError.isNotBlank()) Text(state.customFilterError,color=Color.White,maxLines=3)
            LazyVerticalGrid(columns=GridCells.Adaptive(150.dp),state=grid,modifier=Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement=Arrangement.spacedBy(18.dp),verticalArrangement=Arrangement.spacedBy(18.dp),contentPadding=PaddingValues(top=8.dp,bottom=40.dp)) {
                gridItems(state.customFilterResults,key={ it.cloudId }) { item -> MediaCard(item,modifier=Modifier.fillMaxWidth(),compact=true) { state.launch { state.open(item) } } }
                if(!state.customFilterLoading && state.customFilterResults.isEmpty()) item(span={ GridItemSpan(maxLineSpan) }) {
                    Text(if(applied==null) tr("Elige filtros para descubrir contenido.","Choose filters to discover titles.") else tr("No hay coincidencias. Prueba quitando algún filtro.","No matches. Try removing a filter."),color=Muted)
                }
                if(state.customFilterMore && !state.customFilterLoading && applied!=null) item(span={ GridItemSpan(maxLineSpan) }) {
                    FocusButton(tr("Cargar más","Load more")) { state.launch { state.applyCustomFilter(applied,true) } }
                }
            }
        }
    }
    if(edit) CustomFilterEditor(state,apply={ value ->
        state.customFilter=value;edit=false;state.launch { state.applyCustomFilter(value) }
    },close={ edit=false })
}

private fun filterSummary(f: CustomCatalogFilter): String = buildList {
    add(if(f.type=="series") tr("Series","Series") else tr("Películas","Movies"))
    f.actor?.let { add(tr("Actor: ","Actor: ")+it.name) }
    f.director?.let { add(tr("Director: ","Director: ")+it.name) }
    f.genre?.let { add(it.name) }
    if(f.fromYear.isNotBlank() || f.toYear.isNotBlank()) add("${f.fromYear.ifBlank { "…" }}–${f.toYear.ifBlank { "…" }}")
    if(f.minimumRating>0) add("★ ≥ ${f.minimumRating}")
    add(when(f.sort) { "rating" -> tr("Mejor puntuadas","Highest rated");"newest" -> tr("Más recientes","Newest");else -> tr("Popularidad","Popularity") })
}.joinToString(" · ")

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CustomFilterEditor(state: AppState,apply: (CustomCatalogFilter) -> Unit,close: () -> Unit) {
    var draft by remember { mutableStateOf(state.customFilter) }
    var personRole by remember { mutableStateOf<String?>(null) }
    var genresOpen by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    val first=remember { FocusRequester() }
    LaunchedEffect(Unit) { withFrameNanos { };first.requestFocus() }
    Dialog(onDismissRequest=close,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Column(Modifier.fillMaxWidth(.9f).heightIn(max=540.dp).background(Panel,RoundedCornerShape(22.dp)).padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            Text(tr("Combina tus filtros","Combine your filters"),color=Color.White,fontSize=26.sp)
            LazyColumn(Modifier.weight(1f,false),verticalArrangement=Arrangement.spacedBy(18.dp),contentPadding=PaddingValues(6.dp)) {
                item { Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    FocusButton(tr("Películas","Movies"),primary=draft.type=="movie",modifier=Modifier.focusRequester(first)) { if(draft.type!="movie") draft=draft.copy(type="movie",genre=null) }
                    FocusButton(tr("Series","Series"),primary=draft.type=="series") { if(draft.type!="series") draft=draft.copy(type="series",genre=null) }
                } }
                item { FlowRow(horizontalArrangement=Arrangement.spacedBy(12.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    FocusButton(tr("Actor: ","Actor: ")+(draft.actor?.name ?: tr("Cualquiera","Any"))) { personRole="actor" }
                    if(draft.actor!=null) FocusButton(tr("Quitar actor","Clear actor")) { draft=draft.copy(actor=null) }
                    FocusButton(tr("Director: ","Director: ")+(draft.director?.name ?: tr("Cualquiera","Any"))) { personRole="director" }
                    if(draft.director!=null) FocusButton(tr("Quitar director","Clear director")) { draft=draft.copy(director=null) }
                } }
                item { FocusButton(tr("Género: ","Genre: ")+(draft.genre?.name ?: tr("Todos","All"))) { genresOpen=true } }
                item { Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
                    Text(tr("Año de estreno · para un año exacto, repítelo en ambas casillas","Release year · for one exact year, enter it in both fields"),color=Muted)
                    Row(horizontalArrangement=Arrangement.spacedBy(18.dp)) {
                        NativeTextField(draft.fromYear,{ draft=draft.copy(fromYear=it.filter(Char::isDigit).take(4)) },tr("Desde · opcional","From · optional"),modifier=Modifier.weight(1f))
                        NativeTextField(draft.toYear,{ draft=draft.copy(toYear=it.filter(Char::isDigit).take(4)) },tr("Hasta · opcional","To · optional"),modifier=Modifier.weight(1f))
                    }
                } }
                item { Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    Text(tr("Puntuación mínima: ","Minimum rating: ")+(if(draft.minimumRating==0) tr("Cualquiera","Any") else "${draft.minimumRating}/10"),color=Color.White,modifier=Modifier.weight(1f))
                    FocusButton("−") { draft=draft.copy(minimumRating=(draft.minimumRating-1).coerceAtLeast(0)) }
                    FocusButton("+") { draft=draft.copy(minimumRating=(draft.minimumRating+1).coerceAtMost(10)) }
                } }
                item { FlowRow(horizontalArrangement=Arrangement.spacedBy(12.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    listOf("popular" to tr("Popularidad","Popularity"),"rating" to tr("Mejor puntuadas","Highest rated"),"newest" to tr("Más recientes","Newest")).forEach { (key,label) ->
                        FocusButton(label,primary=draft.sort==key) { draft=draft.copy(sort=key) }
                    }
                } }
                if(error.isNotBlank()) item { Text(error,color=Color.White) }
            }
            Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                FocusButton(tr("Ver resultados","View results")) { try { draft.validate();apply(draft) } catch(e: IllegalArgumentException) { error=e.message.orEmpty() } }
                FocusButton(tr("Restablecer","Reset")) { draft=CustomCatalogFilter();error="" }
                FocusButton(tr("Cancelar","Cancel")) { close() }
            }
        }
    }
    personRole?.let { role -> CatalogPersonPicker(state,role,select={ person ->
        draft=if(role=="actor") draft.copy(actor=person) else draft.copy(director=person);personRole=null
    },close={ personRole=null }) }
    if(genresOpen) CatalogGenrePicker(state,draft.type,select={ draft=draft.copy(genre=it);genresOpen=false },close={ genresOpen=false })
}

@Composable
private fun CatalogPersonPicker(state: AppState,role: String,select: (CatalogPerson) -> Unit,close: () -> Unit) {
    var query by remember { mutableStateOf("") }
    var rows by remember { mutableStateOf<List<CatalogPerson>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    val first=remember { FocusRequester() }
    LaunchedEffect(Unit) { withFrameNanos { };first.requestFocus() }
    LaunchedEffect(query) {
        rows=emptyList();error="";loading=false
        if(query.trim().length>=2) {
            delay(350);loading=true
            try { rows=state.tmdb.filterPeople(query) }
            catch(e: Exception) { if(e is CancellationException) throw e;error=e.message.orEmpty() }
            finally { loading=false }
        }
    }
    Dialog(onDismissRequest=close,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Column(Modifier.width(720.dp).heightIn(max=530.dp).background(Panel,RoundedCornerShape(22.dp)).padding(24.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
            Text(if(role=="actor") tr("Seleccionar actor","Choose actor") else tr("Seleccionar director","Choose director"),color=Color.White,fontSize=25.sp)
            NativeTextField(query,{ query=it },tr("Buscar por nombre","Search by name"),modifier=Modifier.fillMaxWidth().focusRequester(first))
            if(loading) Text(tr("Buscando…","Searching…"),color=Muted)
            if(error.isNotBlank()) Text(error,color=Color.White)
            LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(6.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
                items(rows,key={it.id}) { person -> Row(horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                    AsyncImage(person.photo,null,Modifier.size(58.dp,80.dp),contentScale=ContentScale.Crop)
                    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                        FocusButton(person.name,modifier=Modifier.fillMaxWidth()) { select(person) }
                        Text(person.knownFor,color=Muted,fontSize=13.sp,maxLines=2)
                    }
                } }
                if(!loading && rows.isEmpty() && query.trim().length>=2) item { Text(tr("Sin coincidencias; prueba otro nombre.","No matches; try another name."),color=Muted) }
            }
            FocusButton(tr("Cerrar","Close")) { close() }
        }
    }
}

@Composable
private fun CatalogGenrePicker(state: AppState,type: String,select: (CatalogGenre?) -> Unit,close: () -> Unit) {
    var rows by remember { mutableStateOf<List<CatalogGenre>>(emptyList()) }
    var error by remember { mutableStateOf("") }
    val first=remember { FocusRequester() }
    LaunchedEffect(Unit) {
        withFrameNanos { };first.requestFocus()
        try { rows=state.tmdb.filterGenres(type) }
        catch(e: Exception) { if(e is CancellationException) throw e;error=e.message.orEmpty() }
    }
    Dialog(onDismissRequest=close) {
        Column(Modifier.width(470.dp).heightIn(max=510.dp).background(Panel,RoundedCornerShape(22.dp)).padding(24.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
            Text(tr("Género","Genre"),color=Color.White,fontSize=25.sp)
            if(error.isNotBlank()) Text(error,color=Color.White)
            LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(10.dp),contentPadding=PaddingValues(6.dp)) {
                item { FocusButton(tr("Todos","All"),modifier=Modifier.focusRequester(first)) { select(null) } }
                items(rows,key={it.id}) { genre -> FocusButton(genre.name,modifier=Modifier.fillMaxWidth()) { select(genre) } }
            }
            FocusButton(tr("Cerrar","Close")) { close() }
        }
    }
}

@Composable
fun FranchiseScreen(state: AppState) {
    val grid=rememberLazyGridState(state.franchiseScrollIndex,state.franchiseScrollOffset)
    var previous by remember { mutableStateOf(state.franchiseType) }
    LaunchedEffect(state.franchiseType) { if(previous!=state.franchiseType) { grid.scrollToItem(0);previous=state.franchiseType } }
    DisposableEffect(grid) { onDispose { state.franchiseScrollIndex=grid.firstVisibleItemIndex;state.franchiseScrollOffset=grid.firstVisibleItemScrollOffset } }
    BackHandler { state.screen=Screen.COLLECTIONS }
    AppShell(state,Screen.FRANCHISE,{ state.screen=it }) {
        Column(Modifier.fillMaxSize().padding(horizontal=34.dp,vertical=24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                Text(com.miflix.native2.data.FranchiseCatalogRP29.title(state.franchiseBrand),color=Color.White,fontSize=30.sp,modifier=Modifier.weight(1f))
                if(com.miflix.native2.data.FranchiseCatalogRP29.supportsMovies(state.franchiseBrand)) FocusButton(tr("Películas","Movies"),primary=state.franchiseType=="movie") { state.launch { state.loadFranchise("movie") } }
                FocusButton(tr("Series","Series"),primary=state.franchiseType=="series") { state.launch { state.loadFranchise("series") } }
                FocusButton(tr("Atrás","Back")) { state.screen=Screen.COLLECTIONS }
            }
            if(state.franchiseLoading) Text(tr("Cargando…","Loading…"),color=Muted)
            if(state.franchiseError.isNotBlank()) Text(state.franchiseError,color=Color.White,maxLines=3)
            LazyVerticalGrid(columns=GridCells.Adaptive(150.dp),state=grid,modifier=Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement=Arrangement.spacedBy(18.dp),verticalArrangement=Arrangement.spacedBy(18.dp),contentPadding=PaddingValues(top=8.dp,bottom=40.dp)) {
                gridItems(state.franchiseItems,key={it.cloudId}) { item -> MediaCard(item,modifier=Modifier.fillMaxWidth(),compact=true) { state.launch { state.open(item) } } }
                if(!state.franchiseLoading && state.franchiseItems.isEmpty()) item(span={ GridItemSpan(maxLineSpan) }) { Text(tr("No hay resultados disponibles. Pulsa Películas o Series para reintentar.","No results available. Select Movies or Series to retry."),color=Muted) }
                if(state.franchiseMore && !state.franchiseLoading) item(span={ GridItemSpan(maxLineSpan) }) {
                    FocusButton(tr("Cargar más","Load more")) { state.launch { state.loadFranchise(state.franchiseType,true) } }
                }
            }
        }
    }
}
