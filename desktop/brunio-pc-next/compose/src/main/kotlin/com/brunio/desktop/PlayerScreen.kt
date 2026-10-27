package com.brunio.desktop

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.json.JSONObject

/** The only media URLs in the UI belong to an explicit playback session. They are never displayed. */
data class PlaybackSession(val item: JSONObject, val streams: List<JSONObject>, val owner: String = "",
    val season: Int = 0, val episode: Int = 0, val audio: String = "es", val subtitles: String = "es",
    val startMs: Long = 0, val retry: Boolean = true, val autoplay: Boolean = true, val nextAvailable: Boolean = false)

@Composable
fun PlayerScreen(session: PlaybackSession, onClose: () -> Unit, onFullscreen: () -> Unit,
                 onProgress: (Long, Long) -> Unit, onNext: () -> Unit, onUnavailable: (String) -> Unit) {
    val engineResult = remember(session) { runCatching { VlcEngine() } }
    val engine = engineResult.getOrNull()
    var source by remember(session) { mutableStateOf(0) }
    var attempt by remember(session) { mutableStateOf(0) }
    var resume by remember(session) { mutableStateOf(session.startMs) }
    var bitmap by remember(session) { mutableStateOf<ImageBitmap?>(null) }
    var menu by remember(session) { mutableStateOf<String?>(null) }
    var volume by remember { mutableStateOf(1f) }
    val live = session.item.optString("type") == "live"
    var ui by remember(session) { mutableStateOf(PlaybackUi(session.item.optString("title"), live = live,
        series = session.item.optString("type") == "series", nextAvailable = session.nextAvailable, season = session.season, episode = session.episode,
        error = if(engine == null) "No se pudo iniciar VLC. En desarrollo, instala VLC x64 o configura BRUNIO_VLC_HOME." else null)) }
    val latestProgress by rememberUpdatedState(onProgress)
    DisposableEffect(engine) {
        onDispose {
            if(engine != null) {
                val position = engine.player.status().time(); val duration = engine.player.status().length()
                if(!live && duration > 0) latestProgress(position, duration)
                engine.close()
            }
        }
    }
    LaunchedEffect(source, attempt, session) {
        if(engine == null) return@LaunchedEffect
        val stream = session.streams.getOrNull(source)
        if(stream == null) { ui = ui.copy(error = "No hay fuentes disponibles", buffering = false); return@LaunchedEffect }
        val options = mutableListOf(":audio-language=${session.audio}", ":sub-language=${if(session.subtitles == "off") "none" else session.subtitles}")
        if (resume > 0 && !live) options.add(":start-time=${resume / 1000.0}")
        val headers = stream.optJSONObject("behaviorHints")?.optJSONObject("proxyHeaders")?.optJSONObject("request")
        listOf("User-Agent" to "http-user-agent", "Referer" to "http-referrer").forEach { (header, option) ->
            headers?.optString(header)?.takeIf { it.isNotBlank() && !it.contains('\n') && !it.contains('\r') }?.let { options.add(":$option=$it") }
        }
        ui = ui.copy(source = stream.optString("name", "Fuente ${source+1}"), error = null, buffering = true)
        engine.play(stream.getString("url"), options)
        var lastProgress = 0L
        var started = false
        val began = System.nanoTime()
        while(true) {
            delay(33)
            engine.frame.getAndSet(null)?.let { bitmap = it.image().toComposeImageBitmap() }
            val position = engine.player.status().time().coerceAtLeast(0)
            val duration = engine.player.status().length().coerceAtLeast(0)
            val playing = engine.player.status().isPlaying
            if (playing) started = true
            ui = ui.copy(position = position, duration = duration, paused = !playing,
                seekable = engine.player.status().isSeekable, buffering = !started && !engine.failed.get())
            val elapsed = (System.nanoTime() - began) / 1_000_000
            if(engine.failed.get() || !started && elapsed > 30000) {
                if(session.retry && source + 1 < session.streams.size) { resume = position; source++; break }
                ui = ui.copy(error = "La fuente no respondió. Puedes seleccionar otra.", buffering = false)
                break
            }
            if(engine.finished.get()) { if(!live && duration > 0) latestProgress(position, duration); ui = ui.copy(paused = true); if(session.autoplay) onNext(); break }
            if(!live && duration > 0 && position - lastProgress >= 15000) { latestProgress(position, duration); lastProgress = position }
        }
    }
    fun action(id: String) {
        when(id) {
            "close" -> onClose()
            "fullscreen" -> onFullscreen()
            "toggle" -> engine?.toggle()
            "sources", "audio", "subtitles" -> menu = id
            "next" -> onNext()
            "retry" -> if(source + 1 < session.streams.size) { resume = ui.position; source++ } else menu = "sources"
            else -> onUnavailable(id)
        }
    }
    Box(Modifier.fillMaxSize().background(Color.Black).onPreviewKeyEvent {
        if(it.type != KeyEventType.KeyDown) false else when(it.key) {
            Key.Spacebar -> { engine?.toggle(); true }
            Key.Escape -> { onClose(); true }
            Key.DirectionLeft -> { if(!live && ui.seekable) engine?.seek((ui.position-10000).coerceAtLeast(0)); true }
            Key.DirectionRight -> { if(!live && ui.seekable) engine?.seek((ui.position+10000).coerceAtMost(ui.duration)); true }
            else -> false
        }
    }) {
        bitmap?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit) }
        TvPlayerControls(ui, ::action, { engine?.seek(it) }, { volume = it; engine?.volume(it); ui = ui.copy(volume = it) })
    }
    menu?.let { kind ->
        AlertDialog(onDismissRequest = { menu = null }, title = { Text(when(kind) { "sources" -> "Fuentes"; "audio" -> "Audio"; else -> "Subtítulos" }) },
            text = { LazyColumn(Modifier.heightIn(max = 420.dp)) {
                if(kind == "sources") items(session.streams.indices.toList()) { index ->
                    TextButton(onClick = { resume = ui.position; source = index; attempt++; menu = null }) { Text(session.streams[index].optString("name", "Fuente ${index+1}")+" · "+session.streams[index].optString("title").take(100)) }
                } else if(kind == "audio") {
                    items(engine?.player?.audio()?.trackDescriptions().orEmpty()) { track -> TextButton(onClick = { engine?.player?.audio()?.setTrack(track.id()); menu = null }) { Text(track.description()) } }
                } else {
                    item { TextButton(onClick = { engine?.player?.subpictures()?.setTrack(-1); menu = null }) { Text("Desactivar") } }
                    items(engine?.player?.subpictures()?.trackDescriptions().orEmpty()) { track -> TextButton(onClick = { engine?.player?.subpictures()?.setTrack(track.id()); menu = null }) { Text(track.description()) } }
                }
            } }, confirmButton = { TextButton(onClick = { menu = null }) { Text("Cerrar") } })
    }
}
