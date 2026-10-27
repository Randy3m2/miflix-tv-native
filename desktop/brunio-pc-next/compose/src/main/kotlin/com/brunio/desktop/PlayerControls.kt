package com.brunio.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Milliseconds throughout the controller boundary, matching the TV playback protocol. */
data class PlaybackUi(
    val title: String,
    val source: String = "",
    val live: Boolean = false,
    val series: Boolean = false,
    val paused: Boolean = true,
    val position: Long = 0,
    val duration: Long = 0,
    val seekable: Boolean = false,
    val nextAvailable: Boolean = false,
    val season: Int = 0,
    val episode: Int = 0,
    val volume: Float = 1f,
    val error: String? = null,
    val buffering: Boolean = false,
)

data class PlaybackAction(val id: String, val glyph: String, val label: String, val enabled: Boolean = true)
fun playbackActions(state: PlaybackUi): List<PlaybackAction> = buildList {
    add(PlaybackAction("toggle", if (state.paused) "play" else "pause", if (state.paused) "Reproducir" else "Pausar"))
    if (state.series && !state.live) {
        add(PlaybackAction("next", "next", "Siguiente episodio", state.nextAvailable))
        add(PlaybackAction("episodes", "episodes", "Episodios"))
    }
    if (state.live) add(PlaybackAction("channels", "episodes", "Canales"))
    add(PlaybackAction("subtitles", "cc", "Subtítulos"))
    add(PlaybackAction("audio", "audio", "Pista de audio"))
    add(PlaybackAction("sources", "links", "Cambiar fuente"))
    add(PlaybackAction("party", "party", "Friends & Party"))
    add(PlaybackAction("chat", "chat", "Chat del Party"))
    add(PlaybackAction("reactions", "reactions", "Reacciones"))
    if (!state.live) add(PlaybackAction("queue", "queue", "Cola"))
}

internal fun playbackTime(ms: Long): String {
    val s = ms.coerceAtLeast(0) / 1000
    return if (s >= 3600) "%d:%02d:%02d".format(s / 3600, s / 60 % 60, s % 60)
    else "%02d:%02d".format(s / 60, s % 60)
}

/** An overlay only. The native video surface and transport are owned by the host. */
@Composable
fun TvPlayerControls(
    state: PlaybackUi,
    onAction: (String) -> Unit,
    onSeek: (Long) -> Unit,
    onVolume: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    var label by remember { mutableStateOf("") }
    var preview by remember(state.title) { mutableStateOf<Float?>(null) }
    Box(modifier.fillMaxSize()) {
        Row(Modifier.align(Alignment.TopStart).padding(24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PlayerAction(PlaybackAction("close", "close", "Volver"), { onAction("close") }, { label = it })
        }
        Row(Modifier.align(Alignment.TopEnd).padding(24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (state.live) {
                PlayerAction(PlaybackAction("scores", "score", "PIN Live score"), { onAction("scores") }, { label = it })
                PlayerAction(PlaybackAction("multiview", "grid", "Multiview"), { onAction("multiview") }, { label = it })
            }
            PlayerAction(PlaybackAction("info", "info", "Información técnica"), { onAction("info") }, { label = it })
            PlayerAction(PlaybackAction("pip", "pip", "Ventana flotante"), { onAction("pip") }, { label = it })
            PlayerAction(PlaybackAction("fullscreen", "maximize", "Pantalla completa"), { onAction("fullscreen") }, { label = it })
        }
        if (state.buffering) CircularProgressIndicator(Modifier.align(Alignment.Center), color = Color.White)
        state.error?.let { message ->
            Surface(Modifier.align(Alignment.Center).widthIn(max = 480.dp), color = Color(0xEF19191C), shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("No se pudo reproducir esta fuente", style = MaterialTheme.typography.titleLarge)
                    Text(message)
                    Button(onClick = { onAction("retry") }) { Text("Probar otra fuente") }
                }
            }
        }
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xF0000000))))
            .padding(start = 28.dp, end = 28.dp, top = 60.dp, bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(state.title, color = Color.White, fontSize = 22.sp, maxLines = 1)
            Text((if (state.series) "S${state.season} E${state.episode} · " else "") + state.source,
                color = Color(0xFFBBBBBB), fontSize = 12.sp, maxLines = 1)
            Text(label, color = Color.White, fontSize = 12.sp, modifier = Modifier.height(18.dp))
            if (!state.live) {
                val end = state.duration.coerceAtLeast(1)
                Slider(value = preview ?: (state.position.toFloat() / end).coerceIn(0f, 1f),
                    onValueChange = { preview = it },
                    onValueChangeFinished = { preview?.let { onSeek((it * end).toLong()) }; preview = null },
                    enabled = state.seekable && state.duration > 0,
                    colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White))
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                playbackActions(state).forEach { action -> PlayerAction(action, { onAction(action.id) }, { label = it }) }
                Spacer(Modifier.width(18.dp))
                Slider(value = state.volume.coerceIn(0f, 1f), onValueChange = onVolume,
                    modifier = Modifier.width(90.dp).semantics { contentDescription = "Volumen" },
                    colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White))
                Text(if (state.live) "EN VIVO" else playbackTime(state.position) + " / " + playbackTime(state.duration),
                    color = Color.White, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun PlayerAction(action: PlaybackAction, onClick: () -> Unit, onLabel: (String) -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    var focused by remember { mutableStateOf(false) }
    LaunchedEffect(hovered) { if (hovered) onLabel(action.label) }
    val active = focused || hovered
    Surface(onClick = onClick, enabled = action.enabled, shape = CircleShape,
        color = if (active) Color(0x99494950) else Color(0x66303036),
        modifier = Modifier.size(44.dp).hoverable(interaction)
            .onFocusChanged { focused = it.isFocused; if (it.isFocused) onLabel(action.label) }
            .border(1.dp, Color.White.copy(alpha = if (active) .3f else .09f), CircleShape)
            .semantics { contentDescription = action.label }) {
        Box(contentAlignment = Alignment.Center) {
            PlayerGlyph(action.glyph, if (action.enabled) Color.White else Color.Gray, Modifier.size(23.dp))
        }
    }
}
