package com.nuvio.app.features.brunio

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nuvio.app.features.addons.httpGetTextWithHeaders
import io.ktor.http.encodeURLParameter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/** Live Sports is a separate Stremio-compatible add-on, not an IPTV category. */
internal object BrunioSportsAddon {
    data class Catalog(val id: String, val name: String, val type: String, val genre: String? = null)
    data class Event(val id: String, val name: String, val poster: String?)
    data class Source(val name: String, val url: String)
    private val json = Json { ignoreUnknownKeys = true }

    private fun JsonObject.value(key: String): String? =
        (this[key] as? JsonPrimitive)?.contentOrNull?.trim()

    private fun root(manifest: String): String {
        val trimmed = manifest.trim()
        require(trimmed.startsWith("https://") && trimmed.endsWith("/manifest.json") &&
            !trimmed.contains('?') && !trimmed.contains('#') && !trimmed.contains('@')) {
            "Se requiere un manifiesto HTTPS válido."
        }
        return trimmed.removeSuffix("/manifest.json")
    }
    private suspend fun fetch(url: String): JsonObject = withContext(Dispatchers.IO) {
        json.parseToJsonElement(httpGetTextWithHeaders(url,
            mapOf("Accept" to "application/json"))) as? JsonObject
            ?: error("El add-on devolvió una respuesta no válida.")
    }

    suspend fun catalogs(manifest: String): List<Catalog> {
        val body = fetch(root(manifest) + "/manifest.json")
        val items = body["catalogs"] as? JsonArray ?: return emptyList()
        return items.mapNotNull { item ->
            val row = item as? JsonObject ?: return@mapNotNull null
            val type = row.value("type") ?: return@mapNotNull null
            val id = row.value("id") ?: return@mapNotNull null
            // Mirror TV LiveRepository: accept published add-on catalog types,
            // not only tv/channel. Event providers may use other Stremio types.
            if (!Regex("[A-Za-z0-9_.:-]{1,120}").matches(type) ||
                !Regex("[A-Za-z0-9_.:-]{1,150}").matches(id)) return@mapNotNull null
            Catalog(id, row.value("name") ?: id, type)
        }.distinctBy { it.type + ":" + it.id }
    }
    suspend fun events(manifest: String, catalog: Catalog): List<Event> {
        val genre = catalog.genre?.let { "/genre=${it.encodeURLParameter()}" }.orEmpty()
        val body = fetch(root(manifest) + "/catalog/${catalog.type.encodeURLParameter()}/${catalog.id.encodeURLParameter()}$genre.json")
        return (body["metas"] as? JsonArray).orEmpty().mapNotNull { item ->
            val row = item as? JsonObject ?: return@mapNotNull null
            val id = row.value("id")?.takeIf { it.length <= 250 } ?: return@mapNotNull null
            Event(id, row.value("name") ?: id, row.value("poster"))
        }.take(300)
    }
    suspend fun sources(manifest: String, catalog: Catalog, event: Event): List<Source> {
        val id = event.id.encodeURLParameter()
        val body = fetch(root(manifest) + "/stream/${catalog.type.encodeURLParameter()}/$id.json")
        return (body["streams"] as? JsonArray).orEmpty().mapNotNull { item ->
            val row = item as? JsonObject ?: return@mapNotNull null
            val url = row.value("url") ?: return@mapNotNull null
            // A native MPV source must be directly playable, not an iframe page,
            // DRM-only manifest, magnet link or third-party site.
            if (!(url.startsWith("https://") || url.startsWith("http://")) ||
                url.length > 4096 || url.contains('@')) return@mapNotNull null
            Source(row.value("title") ?: row.value("name") ?: "Reproducir", url)
        }.take(20)
    }
}

@Composable
internal fun BrunioSportsAddonScreen(
    onBack: () -> Unit,
    onPlay: (String, String, String?) -> Unit,
) {
    val manifest = remember { brunioDefaultSportsManifest() }
    var catalogs by remember { mutableStateOf(emptyList<BrunioSportsAddon.Catalog>()) }
    var selected by remember { mutableStateOf<BrunioSportsAddon.Catalog?>(null) }
    var events by remember { mutableStateOf(emptyList<BrunioSportsAddon.Event>()) }
    var selectedEvent by remember { mutableStateOf<BrunioSportsAddon.Event?>(null) }
    var sources by remember { mutableStateOf(emptyList<BrunioSportsAddon.Source>()) }
    var busy by remember { mutableStateOf(false) }
    var selectedLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var refresh by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(manifest, refresh) {
        error = null
        busy = true
        try {
            catalogs = BrunioSportsAddon.catalogs(manifest)
            selected = catalogs.firstOrNull()
            if (catalogs.isEmpty()) error = "El add-on no tiene catálogos deportivos."
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (_: Exception) {
            error = "No se pudo consultar el manifiesto deportivo."
        } finally {
            busy = false
        }
    }
    LaunchedEffect(manifest, selected, refresh) {
        val catalog = selected ?: return@LaunchedEffect
        events = emptyList()
        selectedEvent = null
        sources = emptyList()
        selectedLoading = true
        try {
            events = BrunioSportsAddon.events(manifest, catalog)
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (_: Exception) {
            error = "No se pudo cargar el catálogo de eventos."
        } finally {
            selectedLoading = false
        }
    }

    BrunioReadableSurface {
        Column(modifier = Modifier.fillMaxSize().padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedButton(onClick = onBack) { Text("Volver") }
                Text("Live Sports · Add-on", style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.weight(1f))
                OutlinedButton(onClick = { refresh++ }) { Text("Actualizar") }
            }
            Text("Eventos del add-on deportivo de BruniO TV, independientes de Tirso TV.")
            if (manifest.isBlank()) Text("No hay manifiesto deportivo configurado.")
            if (error != null) Text(error.orEmpty(), color = MaterialTheme.colorScheme.error)
            if (busy || selectedLoading) Text("Cargando…")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                catalogs.forEach { catalog ->
                    FilterChip(selected = selected == catalog,
                        onClick = { selected = catalog; error = null },
                        label = { Text(catalog.name) })
                }
            }
            if (!busy && !selectedLoading && selected == null && catalogs.isNotEmpty()) Text("Selecciona un catálogo deportivo.")
            if (selectedEvent != null) {
                Text(selectedEvent!!.name, style = MaterialTheme.typography.titleLarge)
                OutlinedButton(onClick = { selectedEvent = null; sources = emptyList() }) {
                    Text("Volver a eventos")
                }
                LazyColumn {
                    items(sources) { source ->
                        Button(onClick = {
                            onPlay(selectedEvent!!.name, source.url, selectedEvent!!.poster)
                        }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Text(source.name)
                        }
                    }
                }
                if (!busy && sources.isEmpty()) Text("Sin enlaces directos compatibles con MPV.")
            } else {
                LazyColumn {
                    items(events, key = { it.id }) { event ->
                        Row(modifier = Modifier.fillMaxWidth().clickable {
                            val catalog = selected ?: return@clickable
                            selectedEvent = event
                            sources = emptyList()
                            busy = true
                            scope.launch {
                                try {
                                    sources = BrunioSportsAddon.sources(manifest, catalog, event)
                                } catch (cancel: CancellationException) {
                                    throw cancel
                                } catch (_: Exception) {
                                    error = "No se pudieron consultar las fuentes del evento."
                                } finally { busy = false }
                            }
                        }.padding(vertical = 12.dp)) {
                            Text(event.name)
                        }
                    }
                }
            }
        }
    }
}
