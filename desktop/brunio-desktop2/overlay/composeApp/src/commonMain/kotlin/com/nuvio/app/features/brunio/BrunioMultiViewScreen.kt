package com.nuvio.app.features.brunio

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nuvio.app.features.player.PlatformPlayerSurface
import com.nuvio.app.features.player.PlayerEngineController

/**
 * Four independent native player instances. Exactly one channel carries audio.
 * VOD URLs, stream credentials and Watch Party state are not forwarded to other
 * channels. Any mix of sports and general live M3U channels is supported.
 */
@Composable
internal fun BrunioMultiViewScreen(
    channels: List<BrunioLiveChannel>,
    onBack: () -> Unit,
    onOpenFullPlayer: (BrunioLiveChannel) -> Unit,
) {
    var focusedId by remember(channels) {
        mutableStateOf(channels.firstOrNull()?.id.orEmpty())
    }
    var selected by remember(channels) { mutableStateOf(channels.take(4)) }
    BrunioReadableSurface {
        Column(
            modifier = Modifier.fillMaxSize().background(BrunioDarkColors.background)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onBack) { Text("Volver a Live TV") }
                Text("BruniO Multiview · " + selected.size + " canales",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f))
                Text("Audio: una señal a la vez",
                    style = MaterialTheme.typography.bodySmall,
                    color = BrunioDarkColors.onSurfaceVariant)
            }
            if (selected.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Agrega canales desde Live TV para continuar.")
                }
            } else {
                val first = selected.take(2)
                val second = selected.drop(2).take(2)
                Row(modifier = Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    first.forEach { channel ->
                        key(channel.id) {
                            BrunioMultiViewTile(
                                channel = channel,
                                focused = focusedId == channel.id,
                                modifier = Modifier.weight(1f).fillMaxSize(),
                                onAudioFocus = { focusedId = channel.id },
                                onRemove = {
                                    selected = selected.filterNot { it.id == channel.id }
                                    if (focusedId == channel.id)
                                        focusedId = selected.firstOrNull()?.id.orEmpty()
                                },
                                onFullPlayer = { onOpenFullPlayer(channel) },
                            )
                        }
                    }
                }
                if (second.isNotEmpty()) {
                    Row(modifier = Modifier.fillMaxWidth().weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        second.forEach { channel ->
                            key(channel.id) {
                                BrunioMultiViewTile(
                                    channel = channel,
                                    focused = focusedId == channel.id,
                                    modifier = Modifier.weight(1f).fillMaxSize(),
                                    onAudioFocus = { focusedId = channel.id },
                                    onRemove = {
                                        selected = selected.filterNot { it.id == channel.id }
                                        if (focusedId == channel.id)
                                            focusedId = selected.firstOrNull()?.id.orEmpty()
                                    },
                                    onFullPlayer = { onOpenFullPlayer(channel) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BrunioMultiViewTile(
    channel: BrunioLiveChannel,
    focused: Boolean,
    modifier: Modifier = Modifier,
    onAudioFocus: () -> Unit,
    onRemove: () -> Unit,
    onFullPlayer: () -> Unit,
) {
    var controller by remember(channel.id) { mutableStateOf<PlayerEngineController?>(null) }
    var error by remember(channel.id) { mutableStateOf<String?>(null) }
    LaunchedEffect(controller, focused) {
        controller?.setMuted(!focused)
    }
    Box(
        modifier = modifier.border(
            if (focused) 2.dp else 1.dp,
            if (focused) BrunioDarkColors.primary else BrunioDarkColors.outlineVariant,
            RoundedCornerShape(12.dp),
        ).background(Color.Black),
    ) {
        PlatformPlayerSurface(
            sourceUrl = channel.url,
            streamType = "hls",
            modifier = Modifier.fillMaxSize(),
            playWhenReady = true,
            onControllerReady = { ready ->
                controller = ready
                ready.setMuted(!focused)
            },
            onSnapshot = {},
            onError = { message -> error = message },
        )
        Column(
            modifier = Modifier.fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(Color(0xCC09090D))
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(channel.name,
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = onAudioFocus) {
                    Text(if (focused) "🔊 Audio activo" else "🔇 Escuchar")
                }
                OutlinedButton(onClick = onFullPlayer) { Text("Pantalla completa / PiP") }
                OutlinedButton(onClick = onRemove) { Text("×") }
            }
            if (error != null) Text("Señal no disponible",
                style = MaterialTheme.typography.bodySmall,
                color = BrunioDarkColors.error)
        }
    }
}
