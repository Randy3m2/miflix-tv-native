package com.brunio.desktop

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.text.input.PasswordVisualTransformation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image as SkiaImage
import java.net.URI

/** Presentation models are independent from credentials and the legacy widget classes. */
enum class DesktopTab(val title: String, val glyph: String) {
    Home("Inicio", "play"), Search("Buscar", "search"), Library("Mi lista", "queue"),
    Live("Live TV", "grid"), Sports("Deportes", "score"), Friends("Amigos & Party", "party"),
    Calendar("Calendario", "calendar"), Downloads("Descargas", "download"),
    Notifications("Notificaciones", "notifications"), Settings("Ajustes", "settings")
}
data class MediaTile(val id: String, val title: String, val image: String = "", val subtitle: String = "", val progress: Float? = null)
data class ContentRow(val title: String, val tiles: List<MediaTile>, val wide: Boolean = false)
data class FeedEntry(val id: String, val title: String, val detail: String, val action: String? = null)
data class DesktopScreenState(
    val busy: Boolean = false, val error: String? = null,
    val calendarMonth: String = java.time.YearMonth.now().toString(),
    val hero: MediaTile? = null, val rows: List<ContentRow> = emptyList(),
    val tiles: List<MediaTile> = emptyList(), val entries: List<FeedEntry> = emptyList(),
)

@Composable
fun RemoteArtwork(url: String, modifier: Modifier = Modifier, description: String? = null) {
    val bitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, url) {
        value = null
        if (url.isNotBlank()) value = withContext(Dispatchers.IO) {
            runCatching {
                val uri = URI(url)
                val stream = if (url.startsWith("cover:")) {
                    val hash=url.removePrefix("cover:")
                    require(hash.matches(Regex("[a-f0-9]{64}")))
                    val base=java.nio.file.Path.of(System.getenv("LOCALAPPDATA") ?: System.getProperty("user.home"),"BruniO","PCNext","Downloads")
                    java.nio.file.Files.newInputStream(base.resolve("cover-$hash.img"))
                } else if (url.startsWith("asset:")) {
                    val name = url.removePrefix("asset:")
                    require(name.matches(Regex("[A-Za-z0-9_.-]+")))
                    val roots = listOf(java.nio.file.Path.of("..", "..", "brunio-pc", "assets"), java.nio.file.Path.of("desktop", "brunio-pc", "assets"))
                    val path = roots.map { it.resolve(name) }.first { java.nio.file.Files.isRegularFile(it) }
                    java.nio.file.Files.newInputStream(path)
                } else {
                    require(uri.scheme == "https" && uri.userInfo == null)
                    val connection = uri.toURL().openConnection()
                    connection.connectTimeout = 8000; connection.readTimeout = 8000
                    connection.getInputStream()
                }
                stream.use { input ->
                    val bytes = input.readNBytes(12 * 1024 * 1024 + 1)
                    require(bytes.size <= 12 * 1024 * 1024)
                    val svg=bytes.take(1024).toByteArray().toString(Charsets.UTF_8).contains("<svg",ignoreCase=true)
                    if(svg) {
                        val data=org.jetbrains.skia.Data.makeFromBytes(bytes)
                        val dom=org.jetbrains.skia.svg.SVGDOM(data)
                        val surface=org.jetbrains.skia.Surface.makeRasterN32Premul(256,256)
                        try {dom.setContainerSize(256f,256f);dom.render(surface.canvas);surface.makeImageSnapshot().toComposeImageBitmap()}
                        finally {surface.close();dom.close();data.close()}
                    } else SkiaImage.makeFromEncoded(bytes).toComposeImageBitmap()
                }
            }.getOrNull()
        }
    }
    Box(modifier.background(Color(0xFF1B1B20))) {
        bitmap?.let { Image(it, description, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
    }
}

@Composable
private fun MediaCard(tile: MediaTile, wide: Boolean, onOpen: (String) -> Unit) {
    Column(Modifier.width(if (wide) 300.dp else 174.dp).clickable { onOpen(tile.id) }, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box {
            RemoteArtwork(tile.image, Modifier.fillMaxWidth().height(if (wide) 169.dp else 252.dp).clip(RoundedCornerShape(12.dp)), tile.title)
            tile.progress?.let { progress -> LinearProgressIndicator(progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(3.dp), color = Color.White) }
        }
        Text(tile.title, maxLines = 1, color = Color.White)
        if (tile.subtitle.isNotBlank()) Text(tile.subtitle, color = Color(0xFF9B9BA5), fontSize = 12.sp, maxLines = 1)
    }
}

@Composable
fun DesktopScreen(tab: DesktopTab, state: DesktopScreenState, onAction: (String) -> Unit, onSearch: (String) -> Unit) {
    Column(Modifier.fillMaxSize().background(Color(0xFF080809))) {
        if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Color.White)
        state.error?.let { message -> Surface(color = Color(0xFF302024), modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(message, modifier = Modifier.weight(1f)); TextButton(onClick = { onAction("retry") }) { Text("Reintentar") }
            }
        } }
        when (tab) {
            DesktopTab.Home -> HomeScreen(state, onAction)
            DesktopTab.Search -> SearchScreen(state, onAction, onSearch)
            DesktopTab.Library, DesktopTab.Live, DesktopTab.Sports -> CatalogScreen(tab, state, onAction)
            DesktopTab.Calendar -> CalendarScreen(state, onAction)
            DesktopTab.Settings -> SettingsScreen(onAction)
            else -> FeedScreen(tab, state, onAction)
        }
    }
}

@Composable
private fun HomeScreen(state: DesktopScreenState, onAction: (String) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(28.dp), contentPadding = PaddingValues(bottom = 36.dp)) {
        item {
            val hero = state.hero
            if (hero != null) Box(Modifier.fillMaxWidth().height(510.dp)) {
                RemoteArtwork(hero.image, Modifier.fillMaxSize())
                Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xF5080809), Color.Transparent))))
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xFF080809)))))
                Column(Modifier.align(Alignment.BottomStart).padding(36.dp).widthIn(max = 600.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Text(hero.title, style = MaterialTheme.typography.displayMedium, color = Color.White)
                    Text(hero.subtitle, maxLines = 3, color = Color(0xFFC7C7CF))
                    Button(onClick = { onAction(hero.id) }, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black), shape = RoundedCornerShape(28.dp)) { Text("Ver detalles", Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }
                }
            } else EmptyState("Tu cine, en un solo lugar", "El contenido aparecerá cuando se cargue tu catálogo.")
        }
        items(state.rows) { row ->
            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Text(row.title, fontSize = 23.sp, color = Color.White, modifier = Modifier.padding(horizontal = 36.dp))
                LazyRow(contentPadding = PaddingValues(horizontal = 36.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(row.tiles, key = { it.id }) { tile -> MediaCard(tile, row.wide, onAction) }
                }
            }
        }
    }
}

@Composable
private fun SearchScreen(state: DesktopScreenState, onAction: (String) -> Unit, onSearch: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(36.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Text("Encuentra tu próxima historia", style = MaterialTheme.typography.headlineLarge)
        OutlinedTextField(query, { query = it; onSearch(it) }, placeholder = { Text("Películas, series, actores o directores") }, singleLine = true,
            shape = RoundedCornerShape(30.dp), modifier = Modifier.fillMaxWidth())
        TileGrid(state.tiles, false, onAction)
    }
}

@Composable
private fun CatalogScreen(tab: DesktopTab, state: DesktopScreenState, onAction: (String) -> Unit) {
    var filter by remember(tab) { mutableStateOf("Todos") }
    Column(Modifier.fillMaxSize().padding(36.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Text(tab.title, style = MaterialTheme.typography.headlineLarge)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(if ((tab == DesktopTab.Live || tab == DesktopTab.Sports)) listOf("Todos") + state.tiles.map { it.subtitle }.filter { it.isNotBlank() }.distinct().sorted() else listOf("Todos", "Películas", "Series")) { title ->
                FilterChip(selected = filter == title, onClick = { filter = title; onAction("filter:$title") }, label = { Text(title) }, shape = RoundedCornerShape(24.dp))
            }
        }
        TileGrid(state.tiles, (tab == DesktopTab.Live || tab == DesktopTab.Sports), onAction)
    }
}

@Composable
private fun TileGrid(tiles: List<MediaTile>, wide: Boolean, onOpen: (String) -> Unit) {
    if (tiles.isEmpty()) EmptyState("No hay contenido para mostrar", "Los resultados y tus selecciones aparecerán aquí.")
    else LazyVerticalGrid(columns = GridCells.Adaptive(if (wide) 300.dp else 174.dp), horizontalArrangement = Arrangement.spacedBy(18.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        items(tiles, key = { it.id }) { MediaCard(it, wide, onOpen) }
    }
}

@Composable
private fun FeedScreen(tab: DesktopTab, state: DesktopScreenState, onAction: (String) -> Unit) {
    Column(Modifier.fillMaxSize().padding(36.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(tab.title, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.weight(1f))
            if (tab == DesktopTab.Friends) { OutlinedButton(onClick = { onAction("party:open") }) { Text("Watch Party") }; OutlinedButton(onClick = { onAction("friends:add") }) { Text("Agregar amigo") } }
            if (tab == DesktopTab.Notifications) {TextButton(onClick={onAction("notifications:refresh")}){Text("Actualizar")};TextButton(onClick={onAction("notifications:clear")}){Text("Borrar")}}
            if (tab == DesktopTab.Calendar) OutlinedButton(onClick = { onAction("calendar:subscriptions") }) { Text("Suscripciones") }
        }
        if (state.entries.isEmpty()) EmptyState(when(tab) {
            DesktopTab.Friends -> "Tus amigos y tu Watch Party"
            DesktopTab.Calendar -> "Tu agenda de estrenos y eventos"
            DesktopTab.Downloads -> "Tu biblioteca sin conexión"
            else -> "Estás al día"
        }, when(tab) {
            DesktopTab.Downloads -> "Las descargas se agruparán por película, serie y temporada."
            else -> "No hay elementos para mostrar."
        })
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(state.entries, key = { it.id }) { entry ->
                Surface(color = Color(0xFF151518), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(22.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) { Text(entry.title); Text(entry.detail, color = Color.Gray) }
                        entry.action?.let { action -> OutlinedButton(onClick = { onAction(entry.id) }) { Text(action) } }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(onAction: (String) -> Unit) {
    val groups = listOf("Cuenta y perfiles", "Reproducción", "Complementos", "Audio y subtítulos", "Descargas", "Apariencia", "Actualizaciones")
    var group by remember { mutableStateOf(groups.first()) }
    Row(Modifier.fillMaxSize().padding(36.dp), horizontalArrangement = Arrangement.spacedBy(36.dp)) {
        Column(Modifier.width(220.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Ajustes", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(16.dp))
            groups.forEach { item -> FilterChip(selected = group == item, onClick = { group = item }, label = { Text(item) }, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(22.dp)) {
            Text(group, style = MaterialTheme.typography.headlineMedium)
            val fields = when (group) {
                "Cuenta y perfiles" -> listOf("Administrar perfiles", "Conectar cuenta", "Cerrar sesión")
                "Reproducción" -> listOf("Autoplay", "Reintentar otra fuente", "Límite de tamaño para PC")
                "Complementos" -> listOf("Administrar complementos", "Prioridad de complementos", "Límite de resultados")
                "Audio y subtítulos" -> listOf("Idioma de audio", "Idioma de subtítulos", "Tamaño de subtítulos")
                "Descargas" -> listOf("Carpeta de descargas", "Límite de tamaño de descarga")
                "Apariencia" -> listOf("Idioma de la interfaz", "Limpiar caché de imágenes")
                else -> listOf("Buscar actualización")
            }
            fields.forEach { field ->
                Surface(onClick = { onAction("settings:$field") }, shape = RoundedCornerShape(22.dp), color = Color(0xFF17171B), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(24.dp)) { Text(field, Modifier.weight(1f)); Text("›", color = Color.Gray) }
                }
            }
        }
    }
}

@Composable
fun EmptyState(title: String, detail: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 44.dp, horizontal = 36.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall, color = Color.White)
        Text(detail, color = Color(0xFF95959E))
    }
}

@Composable
fun ProfilePicker(profiles: List<MediaTile>, onSelect: (String) -> Unit, onManage: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color(0xFF080809)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(40.dp)) {
            Text("¿Quién está viendo?", style = MaterialTheme.typography.displaySmall)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                items(profiles, key = { it.id }) { profile ->
                    Column(Modifier.width(132.dp).clickable { onSelect(profile.id) }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        RemoteArtwork(profile.image, Modifier.size(132.dp).clip(RoundedCornerShape(66.dp)), profile.title)
                        Text(profile.title)
                    }
                }
            }
            OutlinedButton(onClick = onManage, shape = RoundedCornerShape(24.dp)) { Text("Administrar perfiles") }
        }
    }
}

@Composable
private fun CalendarScreen(state: DesktopScreenState, onAction: (String) -> Unit) {
    val month = java.time.YearMonth.parse(state.calendarMonth)
    var selected by remember(month) { mutableStateOf(if (java.time.YearMonth.now() == month) java.time.LocalDate.now().dayOfMonth else 1) }
    fun day(entry: FeedEntry): Int? = runCatching {
        java.time.OffsetDateTime.parse(entry.detail).atZoneSameInstant(java.time.ZoneId.systemDefault()).dayOfMonth
    }.getOrNull()
    Row(Modifier.fillMaxSize().padding(36.dp), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
        Column(Modifier.weight(1.4f), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Text("Tu calendario", style = MaterialTheme.typography.headlineLarge)
            Text("Estrenos y eventos compartidos con tu TV", color = Color.Gray)
            Row(Modifier.fillMaxWidth()) { listOf("L", "M", "X", "J", "V", "S", "D").forEach { Text(it, Modifier.weight(1f).padding(14.dp), color = Color.Gray) } }
            val offset = month.atDay(1).dayOfWeek.value - 1
            val count = month.lengthOfMonth()
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat((offset + count + 6) / 7) { week ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(7) { column ->
                            val number = week * 7 + column - offset + 1
                            if (number !in 1..count) Spacer(Modifier.weight(1f).height(66.dp))
                            else Surface(onClick = { selected = number }, shape = RoundedCornerShape(18.dp),
                                color = if (selected == number) Color.White else Color(0xFF17171C),
                                contentColor = if (selected == number) Color.Black else Color.White,
                                modifier = Modifier.weight(1f).height(66.dp)) {
                                Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(number.toString())
                                    if (state.entries.any { day(it) == number }) Text("•")
                                }
                            }
                        }
                    }
                }
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text("${selected} · ${month.month.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.forLanguageTag("es"))}", style = MaterialTheme.typography.headlineSmall)
            val events = state.entries.filter { day(it) == selected }
            if (events.isEmpty()) Text("Sin eventos para este día", color = Color.Gray)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(events) { event ->
                    Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFF17171C)) {
                        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text(event.title, style = MaterialTheme.typography.titleMedium)
                            Text(event.detail, color = Color.Gray, fontSize = 12.sp)
                            if (event.action != null) TextButton(onClick = { onAction(event.id) }) { Text(event.action) }
                        }
                    }
                }
            }
        }
    }
}
