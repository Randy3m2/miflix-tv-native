package com.nuvio.app.features.brunio

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.nuvio.app.features.home.MetaPreview
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

private data class ProviderShelf(val title: String, val genre: String, val type: String = "movie")
private data class ProviderMood(val label: String, val genre: String, val type: String = "movie")

/** Provider-filtered TMDB shelves, never claiming that TMDB's popularity is an official platform ranking. */
private val providerShelves = listOf(
    ProviderShelf("Acción y aventura", "28"),
    ProviderShelf("Comedia", "35"),
    ProviderShelf("Drama", "18"),
    ProviderShelf("Terror", "27"),
    ProviderShelf("Ciencia ficción", "878"),
    ProviderShelf("Animación", "16"),
    ProviderShelf("Romance", "10749"),
    ProviderShelf("Documentales", "99"),
    ProviderShelf("Series para maratonear", "18", "series"),
)
private val providerMoods = listOf(
    ProviderMood("😌 Relajarme", "35"),
    ProviderMood("🔥 Adrenalina", "28"),
    ProviderMood("😱 Sentir miedo", "27"),
    ProviderMood("❤️ Algo romántico", "10749"),
    ProviderMood("🚀 Explorar otros mundos", "878"),
    ProviderMood("👨‍👩‍👧 Ver en familia", "16"),
    ProviderMood("📺 Maratón de series", "18", "series"),
)

@Composable
internal fun BrunioProviderScreen(
    provider: BrunioHomeRow,
    onBack: () -> Unit,
    onOpenMedia: (MetaPreview) -> Unit,
) {
    var topTen by remember(provider.id) { mutableStateOf(emptyList<MetaPreview>()) }
    var shelves by remember(provider.id) { mutableStateOf(emptyMap<String, List<MetaPreview>>()) }
    var mood by remember(provider.id) { mutableStateOf<ProviderMood?>(null) }
    var moodTitles by remember(provider.id) { mutableStateOf(emptyList<MetaPreview>()) }
    var busy by remember(provider.id) { mutableStateOf(true) }
    var moodBusy by remember(provider.id) { mutableStateOf(false) }
    var error by remember(provider.id) { mutableStateOf<String?>(null) }
    var revision by remember(provider.id) { mutableStateOf(0) }

    LaunchedEffect(provider.id, revision) {
        busy = true
        error = null
        topTen = emptyList()
        shelves = emptyMap()
        try {
            val base = provider.query + mapOf("sort_by" to "popularity.desc")
            val throttle = Semaphore(4)
            coroutineScope {
                val popularMovies = async { BrunioTmdb.catalog("discover/movie", "movie", base) }
                val popularShows = async { BrunioTmdb.catalog("discover/tv", "series", base) }
                val genreShelves = providerShelves.map { row ->
                    async {
                        throttle.withPermit {
                            try {
                                val endpoint = if (row.type == "series") "discover/tv" else "discover/movie"
                                row.title to BrunioTmdb.catalog(endpoint, row.type,
                                    base + mapOf("with_genres" to row.genre)).take(18)
                            } catch (cancel: CancellationException) { throw cancel }
                            catch (_: Exception) { row.title to emptyList<MetaPreview>() }
                        }
                    }
                }
                topTen = (popularMovies.await() + popularShows.await())
                    .distinctBy { it.id }.take(10)
                shelves = genreShelves.awaitAll().toMap()
            }
        } catch (cancel: CancellationException) { throw cancel }
        catch (_: Exception) { error = "No se pudo cargar el catálogo. Comprueba tu conexión y TMDB." }
        finally { busy = false }
    }

    LaunchedEffect(provider.id, mood) {
        val selected = mood ?: return@LaunchedEffect
        moodBusy = true
        moodTitles = emptyList()
        try {
            val endpoint = if (selected.type == "series") "discover/tv" else "discover/movie"
            moodTitles = BrunioTmdb.catalog(endpoint, selected.type,
                provider.query + mapOf("with_genres" to selected.genre,
                    "sort_by" to "popularity.desc")).take(18)
        } catch (cancel: CancellationException) { throw cancel }
        catch (_: Exception) { moodTitles = emptyList() }
        finally { moodBusy = false }
    }

    // Full-window discovery experience; never the small provider list AlertDialog.
    Dialog(onDismissRequest = onBack,
        properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.fillMaxSize().background(Color(0xFF090B10))) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 34.dp, vertical = 22.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                OutlinedButton(onClick = onBack) { Text("Volver") }
                Column(Modifier.weight(1f)) {
                    Text(provider.label, style = MaterialTheme.typography.headlineLarge,
                        color = Color.White, fontWeight = FontWeight.Bold)
                    Text("Explorar películas y series de la plataforma",
                        color = Color.LightGray)
                }
                OutlinedButton(onClick = { revision++ }) { Text("Actualizar") }
            }
            LazyColumn(Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(24.dp)) {
                if (busy) item { Text("Cargando contenido…",
                    Modifier.padding(horizontal = 34.dp), color = Color.White) }
                if (error != null) item {
                    Text(error.orEmpty(), Modifier.padding(horizontal = 34.dp),
                        color = Color(0xFFFF848E))
                }
                if (!busy && error == null) {
                    item {
                        Text("Top 10 · Más populares en ${provider.label}",
                            Modifier.padding(horizontal = 34.dp), color = Color.White,
                            style = MaterialTheme.typography.headlineSmall)
                        Text("Selección basada en la popularidad de TMDB; no es un ranking oficial.",
                            Modifier.padding(horizontal = 34.dp, vertical = 4.dp),
                            color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
                        ProviderPosters(topTen, onOpenMedia, numbered = true)
                    }
                    item {
                        Text("¿Qué tienes ganas de ver?",
                            Modifier.padding(horizontal = 34.dp), color = Color.White,
                            style = MaterialTheme.typography.headlineSmall)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(9.dp),
                            modifier = Modifier.padding(top = 10.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 34.dp)) {
                            items(providerMoods) { option ->
                                AssistChip(onClick = { mood = option }, label = { Text(option.label) })
                            }
                        }
                    }
                    if (mood != null) item {
                        Text("Para tu mood: ${mood?.label.orEmpty()}",
                            Modifier.padding(horizontal = 34.dp), color = Color.White,
                            style = MaterialTheme.typography.titleLarge)
                        if (moodBusy) Text("Buscando títulos…",
                            Modifier.padding(horizontal = 34.dp), color = Color.LightGray)
                        else ProviderPosters(moodTitles, onOpenMedia)
                    }
                    items(providerShelves) { genre ->
                        val titles = shelves[genre.title].orEmpty()
                        if (titles.isNotEmpty()) {
                            Text(genre.title, Modifier.padding(horizontal = 34.dp),
                                color = Color.White, style = MaterialTheme.typography.titleLarge)
                            Spacer(Modifier.height(10.dp))
                            ProviderPosters(titles, onOpenMedia)
                        }
                    }
                    item { Spacer(Modifier.height(35.dp)) }
                }
            }
        }
    }
}

@Composable
private fun ProviderPosters(
    titles: List<MetaPreview>,
    onOpen: (MetaPreview) -> Unit,
    numbered: Boolean = false,
) {
    if (titles.isEmpty()) {
        Text("No se encontraron títulos disponibles.",
            Modifier.padding(horizontal = 34.dp, vertical = 12.dp),
            color = Color.LightGray)
        return
    }
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 34.dp),
    ) {
        items(titles) { title ->
            Column(Modifier.width(154.dp).clickable { onOpen(title) },
                verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Box {
                    AsyncImage(model = title.poster, contentDescription = title.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.width(154.dp).height(220.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF20222B)))
                    if (numbered) {
                        val rank = titles.indexOf(title) + 1
                        Box(Modifier.align(Alignment.BottomStart)
                            .background(Color(0xDA08090C), RoundedCornerShape(topEnd = 7.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)) {
                            Text(rank.toString(), color = Color.White,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Text(title.name, color = Color.White, maxLines = 2,
                    overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
