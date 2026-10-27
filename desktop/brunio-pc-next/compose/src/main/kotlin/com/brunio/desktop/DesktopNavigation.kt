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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** Overlay rail: content always reserves 76 dp; hovering never changes its width. */
@Composable
fun DesktopNavigation(selected: DesktopTab, onSelect: (DesktopTab) -> Unit, onProfiles: () -> Unit) {
    val state = remember { NavigationState() }
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    var expanded by remember { mutableStateOf(false) }
    fun now() = System.nanoTime() / 1_000_000
    LaunchedEffect(hovered) { state.pointer(hovered, now()) }
    LaunchedEffect(Unit) { while (true) { if (state.tick(now())) expanded = state.expanded; delay(16) } }
    val width by animateDpAsState(if (expanded) 244.dp else 76.dp, tween(180))
    Box(Modifier.fillMaxHeight().width(width).clipToBounds().background(Color(0xFF111114))
        .hoverable(interaction)) {
        Column(Modifier.fillMaxWidth().fillMaxHeight().padding(12.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(ui("B"), Modifier.padding(12.dp, 20.dp), style = MaterialTheme.typography.headlineMedium)
            DesktopTab.entries.forEach { route ->
                Surface(onClick = { onSelect(route) }, shape = RoundedCornerShape(28.dp),
                    color = if (selected == route) Color(0xFF34343A) else Color.Transparent,
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = route.title }) {
                    Row(Modifier.height(48.dp).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        PlayerGlyph(route.glyph, Color.White, Modifier.size(24.dp))
                        if(expanded)Text(ui(route.title), maxLines = 1)
                    }
                }
            }
            Surface(onClick = onProfiles, shape = RoundedCornerShape(28.dp), color = Color(0xFF202025), modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Cambiar perfil" }) {
                Row(Modifier.height(48.dp).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    PlayerGlyph("party", Color.White, Modifier.size(24.dp)); if(expanded)Text(ui("Cambiar perfil"), maxLines = 1)
                }
            }
        }
    }
}
