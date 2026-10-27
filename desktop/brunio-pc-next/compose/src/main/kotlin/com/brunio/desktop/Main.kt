package com.brunio.desktop

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.delay

/** Development harness for the new navigation. Not a releasable media application. */
fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "BruniO PC · Desarrollo") {
        MaterialTheme(colorScheme = darkColorScheme(background = Color(0xFF080809))) {
            NavigationHarness()
        }
    }
}

@Composable
private fun NavigationHarness() {
    val state = remember { NavigationState() }
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    var expanded by remember { mutableStateOf(false) }
    var route by remember { mutableStateOf("Inicio") }
    fun now() = System.nanoTime() / 1_000_000
    LaunchedEffect(hovered) { state.pointer(hovered, now()) }
    LaunchedEffect(Unit) {
        while (true) {
            if (state.tick(now())) expanded = state.expanded
            delay(16)
        }
    }
    val width by animateDpAsState(if (expanded) 244.dp else 76.dp, tween(180))
    Box(Modifier.fillMaxSize().background(Color(0xFF080809))) {
        Column(Modifier.fillMaxSize().padding(start = 108.dp, top = 48.dp, end = 48.dp)) {
            Text(route, style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(24.dp))
            Text("Entorno de desarrollo de navegación. La migración de las pantallas y del reproductor aún no está lista.")
        }
        Column(Modifier.fillMaxHeight().width(width).background(Color(0xFF111114))
            .hoverable(interaction).padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (expanded) "BruniO" else "B", style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(vertical = 20.dp))
            listOf("Inicio", "Buscar", "Mi lista", "Live TV", "Amigos", "Calendario", "Descargas", "Ajustes").forEachIndexed { index, name ->
                Surface(onClick = { route = name }, shape = RoundedCornerShape(28.dp),
                    color = if (route == name) Color(0xFF303034) else Color.Transparent,
                    modifier = Modifier.fillMaxWidth()) {
                    Text(if (expanded) name else (index + 1).toString(), modifier = Modifier.padding(14.dp))
                }
            }
        }
    }
}
