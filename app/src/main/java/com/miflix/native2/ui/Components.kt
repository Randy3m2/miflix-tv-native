package com.miflix.native2.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.miflix.native2.model.MediaSummary

val Bg = Color(0xFF060608)
val Panel = Color(0xFF121218)
val Purple = Color(0xFF8B5CF6)
val Muted = Color(0xFF9A9BA5)

@Composable
fun FocusButton(text: String, modifier: Modifier = Modifier, primary: Boolean = false, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.045f else 1f, label = "buttonScale")
    Box(
        modifier.scale(scale).onFocusChanged { focused = it.isFocused }.focusable().clickable(onClick = onClick)
            .background(if (focused || primary) Purple else Color(0xCC202028), RoundedCornerShape(12.dp))
            .padding(horizontal = 22.dp, vertical = 12.dp), contentAlignment = Alignment.Center
    ) { Text(text, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }
}

@Composable
fun MediaCard(item: MediaSummary, modifier: Modifier = Modifier, onFocused: (MediaSummary) -> Unit, onClick: () -> Unit) {
    var focused by remember(item.cloudId) { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.065f else 1f, label = "cardScale")
    Box(modifier.width(252.dp).height(142.dp).scale(scale).onFocusChanged { focused = it.isFocused; if (it.isFocused) onFocused(item) }
        .focusable().clickable(onClick = onClick).clip(RoundedCornerShape(12.dp)).background(Color(0xFF1A1A20))) {
        AsyncImage(model = item.backdrop ?: item.poster, contentDescription = item.title, modifier = Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC000000)), startY = 55f)))
        Column(Modifier.align(Alignment.BottomStart).padding(12.dp)) {
            Text(item.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text("${item.year}  ★ ${"%.1f".format(item.rating)}", color = Color(0xFFDFDFE6), fontSize = 11.sp)
        }
        if (focused) Box(Modifier.matchParentSize().clip(RoundedCornerShape(12.dp)).background(Color(0x18FFFFFF)))
    }
}

@Composable
fun NativeTextField(value: String, onValueChange: (String) -> Unit, hint: String, password: Boolean = false, modifier: Modifier = Modifier) {
    var focused by remember { mutableStateOf(false) }
    BasicTextField(value, onValueChange, singleLine = true,
        textStyle = TextStyle(color = Color.White, fontSize = 16.sp), cursorBrush = SolidColor(Purple),
        visualTransformation = if (password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        modifier = modifier.onFocusChanged { focused = it.isFocused }.background(if (focused) Color(0xFF252532) else Panel, RoundedCornerShape(10.dp)).padding(15.dp),
        decorationBox = { inner -> if (value.isBlank()) Text(hint, color = Muted, fontSize = 15.sp); inner() })
}

@Composable
fun Sidebar(screen: Screen, onNavigate: (Screen) -> Unit, onAnyContentFocus: Boolean) {
    var anyFocused by remember { mutableStateOf(false) }
    LaunchedEffect(onAnyContentFocus) { if (onAnyContentFocus) anyFocused = false }
    val width by animateDpAsState(if (anyFocused) 212.dp else 82.dp, label = "sideWidth")
    val items = listOf(
        Screen.HOME to "⌂" to "Home", Screen.SEARCH to "⌕" to "Search", Screen.MOVIES to "▣" to "Movies",
        Screen.SERIES to "▤" to "Series", Screen.MY_LIST to "♡" to "My List", Screen.PROFILES to "●" to "Profiles", Screen.SETTINGS to "⚙" to "Settings"
    )
    Column(Modifier.width(width).fillMaxHeight().background(Color(0xE6101014)).padding(top = 24.dp, start = 12.dp, end = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("M", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(start = 17.dp, bottom = 20.dp))
        items.forEach { triple ->
            val destination = triple.first.first; val icon = triple.first.second; val label = triple.second
            var focused by remember { mutableStateOf(false) }
            Row(Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused; if (it.isFocused) anyFocused = true }.focusable().clickable { onNavigate(destination) }
                .background(if (focused || destination == screen) Color(0xFF252532) else Color.Transparent, RoundedCornerShape(10.dp)).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(icon, color = if (focused || destination == screen) Purple else Color.White, fontSize = 20.sp)
                if (anyFocused) { Spacer(Modifier.width(14.dp)); Text(label, color = Color.White, fontSize = 15.sp, maxLines = 1) }
            }
        }
    }
}
