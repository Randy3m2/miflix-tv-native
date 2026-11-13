package com.nuvio.app.features.brunio

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nuvio.app.features.player.PlayerPanelHeader
import com.nuvio.app.features.player.PlayerSidePanel
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * In-player selection shares only channel identifiers, never URLs between
 * clients. Live sports channels and normal TV channels can share one grid.
 */
/**
 * Session-only live sources. No private URLs are stored on disk, in navigation
 * arguments or in Watch Party rooms. They are discarded when BruniO exits.
 */
internal object BrunioLiveSessionRegistry {
    private val entries = linkedMapOf<String, BrunioLiveChannel>()
    fun register(name: String, url: String, logo: String?, category: String): String {
        require(url.startsWith("https://") || url.startsWith("http://"))
        val id = "session-" + (name + "|" + url).hashCode().toUInt().toString(16)
        entries[id] = BrunioLiveChannel(id, name, url, category, logo)
        while (entries.size > 30) entries.remove(entries.keys.first())
        return id
    }
    fun find(id: String): BrunioLiveChannel? = entries[id]
    fun available(): List<BrunioLiveChannel> = entries.values.toList()
}

internal object BrunioMultiViewRequests {
    val requests = MutableSharedFlow<List<String>>(extraBufferCapacity = 1)
    fun launch(ids: List<String>) {
        val chosen = ids.distinct().take(4)
        if (chosen.size >= 2) requests.tryEmit(chosen)
    }
}

@Composable
internal fun BrunioMultiViewPickerPanel(
    visible: Boolean,
    modal: Boolean,
    width: Dp,
    currentLiveChannelId: String,
    onDismiss: () -> Unit,
) {
    val all = remember { BrunioLiveTvCatalog.parse(bundledBrunioLivePlaylist()) }
    val registered = BrunioLiveSessionRegistry.available()
    val combined = (registered + all).distinctBy { it.id }
    var search by remember { mutableStateOf("") }
    var sportsOnly by remember { mutableStateOf(false) }
    var selected by remember(currentLiveChannelId) {
        mutableStateOf(setOf(currentLiveChannelId).filter(String::isNotEmpty).toSet())
    }
    val visibleChannels = remember(combined, search, sportsOnly) {
        combined.asSequence().filter {
            (!sportsOnly || isBrunioSportsChannel(it)) &&
                (search.isEmpty() || it.name.contains(search, ignoreCase = true))
        }.take(500).toList()
    }

    PlayerSidePanel(
        visible = visible,
        onDismiss = onDismiss,
        width = width,
        modal = modal,
    ) {
        BrunioReadableSurface {
            Column(
                modifier = Modifier.fillMaxSize().padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                PlayerPanelHeader(title = "BruniO · Multiview") {
                    OutlinedButton(onClick = onDismiss) { Text("Cerrar") }
                }
                Text("Selecciona entre 2 y 4 señales. Solo una tendrá audio.")
                Text("Seleccionados: " + selected.size + "/4",
                    style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { sportsOnly = false }) {
                        Text(if (!sportsOnly) "✓ Todos" else "TV + Sports")
                    }
                    OutlinedButton(onClick = { sportsOnly = true }) {
                        Text(if (sportsOnly) "✓ Live Sports" else "Live Sports")
                    }
                }
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it.take(90) },
                    label = { Text("Buscar canal o deporte") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    visibleChannels.forEach { channel ->
                        val checked = channel.id in selected
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                selected = if (checked) selected - channel.id
                                else if (selected.size < 4) selected + channel.id
                                else selected
                            }.padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(if (checked) "✓" else "+",
                                color = BrunioDarkColors.primary)
                            Text(channel.name, modifier = Modifier.weight(1f),
                                maxLines = 2)
                            if (isBrunioSportsChannel(channel)) Text(
                                "SPORTS", color = BrunioDarkColors.primary,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                }
                Button(
                    onClick = {
                        BrunioMultiViewRequests.launch(selected.toList())
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = selected.size in 2..4,
                ) {
                    Text(if (selected.size < 2) "Añade otro canal" else
                        "Abrir Multiview (" + selected.size + ")")
                }
            }
        }
    }
}

internal fun isBrunioSportsChannel(channel: BrunioLiveChannel): Boolean =
    channel.category.contains("LIDOM", ignoreCase = true) ||
        channel.category.contains("deport", ignoreCase = true) ||
        channel.category.contains("sport", ignoreCase = true) ||
        channel.name.contains("ESPN", ignoreCase = true) ||
        channel.name.contains("TNT Sports", ignoreCase = true) ||
        channel.name.contains("FOX Sports", ignoreCase = true)
