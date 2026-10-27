package com.brunio.desktop

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.delay

/** Explicit development gallery. No sample title or fake playback is presented as real content. */
fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "BruniO PC · Galería de desarrollo") {
        MaterialTheme(colorScheme = darkColorScheme(background = Color(0xFF080809), surface = Color(0xFF151518), primary = Color.White, onPrimary = Color.Black)) {
            DesktopGallery()
        }
    }
}

@Composable
private fun DesktopGallery() {
    val navigation = remember { NavigationState() }
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    var expanded by remember { mutableStateOf(false) }
    var tab by remember { mutableStateOf(DesktopTab.Home) }
    var player by remember { mutableStateOf(false) }
    var live by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    fun now() = System.nanoTime() / 1_000_000
    LaunchedEffect(hovered) { navigation.pointer(hovered, now()) }
    LaunchedEffect(player) { if (player) { navigation.hide(); expanded = false } }
    LaunchedEffect(Unit) {
        while (true) {
            if (navigation.tick(now())) expanded = navigation.expanded
            delay(16)
        }
    }
    val width by animateDpAsState(if (expanded) 244.dp else 76.dp, tween(180))
    Column(Modifier.fillMaxSize().background(Color(0xFF080809))) {
        Row(Modifier.fillMaxWidth().background(Color(0xFF202024)).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("DESARROLLO · Presentación sin motor de reproducción conectado", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
            TextButton(onClick = { player = !player }) { Text(if (player) "Ver pantallas" else "Ver controles de TV") }
            if (player) TextButton(onClick = { live = !live }) { Text(if (live) "Modo VOD" else "Modo Live") }
        }
        Box(Modifier.fillMaxSize()) {
            if (player) TvPlayerControls(
                PlaybackUi("Vista de controles de TV", live = live, series = !live, source = "Sin fuente de vídeo conectada"),
                onAction = { if (it == "close") player = false else notice = "Acción $it: el controlador aún no está conectado" },
                onSeek = { notice = "El motor de vídeo aún no está conectado" },
                onVolume = { notice = "El motor de vídeo aún no está conectado" })
            else {
                Box(Modifier.fillMaxSize().padding(start = 76.dp)) {
                    DesktopScreen(tab, DesktopScreenState(),
                        onAction = { notice = "Esta presentación todavía no tiene conectado el controlador de $it" },
                        onSearch = { notice = "La búsqueda todavía no está conectada al puente de datos" })
                }
                Box(Modifier.fillMaxHeight().width(width).clipToBounds().background(Color(0xFF111114))
                    .hoverable(interaction).onFocusChanged { navigation.focus(it.hasFocus, now()) }) {
                    Column(Modifier.width(244.dp).fillMaxHeight().padding(12.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("B", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(12.dp, 20.dp))
                        DesktopTab.entries.forEach { route ->
                            Surface(onClick = { tab = route }, shape = RoundedCornerShape(28.dp),
                                color = if (tab == route) Color(0xFF303034) else Color.Transparent,
                                modifier = Modifier.fillMaxWidth()) {
                                Row(Modifier.height(48.dp).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                                    PlayerGlyph(route.glyph, Color.White, Modifier.size(24.dp))
                                    Text(route.title, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }
            notice?.let { message ->
                Surface(Modifier.align(Alignment.BottomCenter).padding(24.dp).widthIn(max = 680.dp), color = Color(0xFF303036), shape = RoundedCornerShape(20.dp)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(message, Modifier.weight(1f)); TextButton(onClick = { notice = null }) { Text("Cerrar") }
                    }
                }
            }
        }
    }
}
