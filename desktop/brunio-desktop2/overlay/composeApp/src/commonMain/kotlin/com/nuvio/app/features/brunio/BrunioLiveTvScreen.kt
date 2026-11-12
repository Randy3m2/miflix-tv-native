package com.nuvio.app.features.brunio

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import com.nuvio.app.features.profiles.ProfileRepository
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

internal data class BrunioLiveChannel(
    val id: String,
    val name: String,
    val url: String,
    val category: String,
    val logo: String?,
)

internal expect fun bundledBrunioLivePlaylist(): String
internal expect fun hiddenBrunioChannels(): Set<String>
internal expect fun saveHiddenBrunioChannels(ids: Set<String>)
internal expect fun favoriteBrunioChannels(profileIndex: Int): Set<String>
internal expect fun saveFavoriteBrunioChannels(profileIndex: Int, ids: Set<String>)

/**
 * TV's existing curated M3U: category -> channels -> native Nuvio/MPV player.
 * Hide preferences are local and reversible; no account data is overwritten.
 */
internal object BrunioLiveTvCatalog {
    private val attribute = Regex("""([a-zA-Z-]+)="([^"]*)"""")

    fun parse(input: String): List<BrunioLiveChannel> {
        val channels = mutableListOf<BrunioLiveChannel>()
        var name = ""
        var category = "Otros"
        var logo: String? = null
        val seen = mutableSetOf<String>()
        for (line in input.lineSequence()) {
            val text = line.trim()
            if (text.startsWith("#EXTINF:", ignoreCase = true)) {
                val properties = attribute.findAll(text).associate { it.groupValues[1] to it.groupValues[2] }
                name = text.substringAfterLast(',').trim().take(130)
                category = properties["group-title"].orEmpty().ifBlank { "Otros" }.take(65)
                logo = properties["tvg-logo"]?.takeIf { it.startsWith("https://") }
                    ?.take(2048)
            } else if (name.isNotBlank() && !text.startsWith("#") &&
                (text.startsWith("https://") || text.startsWith("http://"))
            ) {
                val stream = text.take(4096)
                val id = (name + "|" + stream).hashCode().toUInt().toString(16)
                if (seen.add(id)) channels.add(BrunioLiveChannel(
                    id = id, name = name, url = stream, category = category, logo = logo,
                ))
                name = ""
                logo = null
            }
        }
        return channels
    }
}

@Composable
internal fun BrunioLiveTvScreen(
    onBack: () -> Unit,
    onPlay: (BrunioLiveChannel) -> Unit,
    onMultiView: (List<BrunioLiveChannel>) -> Unit = {},
) {
    val all = remember { BrunioLiveTvCatalog.parse(bundledBrunioLivePlaylist()) }
    var filter by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Tirso TV") }
    var hidden by remember { mutableStateOf(hiddenBrunioChannels()) }
    var multiviewIds by remember { mutableStateOf(setOf<String>()) }
    val profileIndex = ProfileRepository.activeProfileId
    var favorites by remember(profileIndex) {
        mutableStateOf(favoriteBrunioChannels(profileIndex))
    }
    val groups = remember(all) {
        listOf("Favoritos", "Live Sports", "Tirso TV") +
            listOf("República Dominicana", "Deportes",
            "Películas & Series", "Entretenimiento", "24/7 Shows", "Noticias")
            .filter { value -> all.any { it.category == value } } +
            all.map { it.category }.distinct().filterNot {
                it in setOf("República Dominicana", "Deportes", "Películas & Series",
                    "Entretenimiento", "24/7 Shows", "Noticias")
            }.sorted()
    }
    val matching = remember(all, category, filter, hidden, favorites) {
        all.asSequence()
            .filter {
                when (category) {
                    "Tirso TV" -> true
                    "Favoritos" -> it.id in favorites
                    "Live Sports" -> it.category.contains("sport", ignoreCase = true) ||
                        it.category.contains("deport", ignoreCase = true) ||
                        it.name.contains("ESPN", ignoreCase = true) ||
                        it.name.contains("TNT Sports", ignoreCase = true)
                    else -> it.category == category
                }
            }
            .filter { it.id !in hidden }
            .filter { filter.isBlank() || it.name.contains(filter.trim(), ignoreCase = true) }
            .take(3000)
            .toList()
    }
    BrunioReadableSurface {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedButton(onClick = onBack) { Text("Volver") }
            Text("Live TV · BruniO", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.weight(1f))
            if (multiviewIds.size >= 2) {
                Button(onClick = {
                    onMultiView(all.filter { it.id in multiviewIds }.take(4))
                }) { Text("Multiview · " + multiviewIds.size + " canales") }
            }
            if (hidden.isNotEmpty()) OutlinedButton(onClick = {
                hidden = emptySet()
                saveHiddenBrunioChannels(hidden)
            }) { Text("Restaurar ocultos") }
        }
        Text("Tirso TV · " + all.size + " canales del catálogo de TV",
            style = MaterialTheme.typography.bodyMedium)
        OutlinedTextField(
            value = filter, onValueChange = { filter = it.take(100) },
            label = { Text("Buscar canal") }, singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            groups.forEach { group ->
                FilterChip(
                    selected = category == group,
                    onClick = { category = group },
                    label = { Text(group) },
                )
            }
        }
        Text(matching.size.toString() + " canales", style = MaterialTheme.typography.titleMedium)
        LazyColumn(modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(matching, key = { it.id }) { channel ->
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onPlay(channel) }
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AsyncImage(
                        model = channel.logo,
                        contentDescription = null,
                        modifier = Modifier.size(width = 76.dp, height = 44.dp),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(channel.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(channel.category, style = MaterialTheme.typography.bodySmall)
                    }
                    OutlinedButton(onClick = {
                        favorites = if (channel.id in favorites) favorites - channel.id
                            else favorites + channel.id
                        saveFavoriteBrunioChannels(profileIndex, favorites)
                    }) {
                        Text(if (channel.id in favorites) "★ Favorito" else "☆ Favorito")
                    }
                    OutlinedButton(onClick = {
                        val wasSelected = channel.id in multiviewIds
                        multiviewIds = if (wasSelected) multiviewIds - channel.id else
                            if (multiviewIds.size < 4) multiviewIds + channel.id else multiviewIds
                    }, enabled = channel.id in multiviewIds || multiviewIds.size < 4) {
                        Text(if (channel.id in multiviewIds) "✓ En multiview" else "+ Multiview")
                    }
                    OutlinedButton(onClick = {
                        hidden = hidden + channel.id
                        multiviewIds = multiviewIds - channel.id
                        saveHiddenBrunioChannels(hidden)
                    }) { Text("Ocultar") }
                    Button(onClick = { onPlay(channel) }) { Text("Ver canal") }
                }
            }
        }
    }
    }
}
