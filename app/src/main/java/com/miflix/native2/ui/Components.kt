package com.miflix.native2.ui

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.miflix.native2.model.MediaSummary
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val Bg = Color(0xFF060608)
val Panel = Color(0xFF121218)
val Purple = Color(0xFF8B5CF6)
val Muted = Color(0xFF9A9BA5)

private val MotionSpec = tween<Float>(durationMillis = 100, easing = LinearOutSlowInEasing)

@Composable
fun FocusButton(
    text: String,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.025f else 1f,
        animationSpec = MotionSpec,
        label = "buttonScale"
    )
    Box(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick)
            .background(if (focused || primary) Purple else Color(0xE0202028), RoundedCornerShape(12.dp))
            .border(if (focused) 1.5.dp else 0.dp, Color(0xE6FFFFFF), RoundedCornerShape(12.dp))
            .padding(horizontal = 22.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun MediaCard(
    item: MediaSummary,
    modifier: Modifier = Modifier,
    progressPercent: Double? = null,
    onFocused: (MediaSummary) -> Unit,
    onClick: () -> Unit
) {
    var focused by remember(item.cloudId) { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.035f else 1f,
        animationSpec = MotionSpec,
        label = "cardScale"
    )

    // The outer box never changes size. The artwork grows only inside this reserved
    // footprint, so focus animation cannot push a LazyRow/LazyColumn by a few pixels.
    Box(
        modifier = modifier.width(268.dp).height(158.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .width(252.dp)
                .height(142.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    shadowElevation = if (focused) 14f else 0f
                }
                .onFocusChanged {
                    focused = it.isFocused
                    if (it.isFocused) onFocused(item)
                }
                .focusable()
                .clickable(onClick = onClick)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1A1A20))
                .border(if (focused) 2.dp else 0.dp, Color.White, RoundedCornerShape(12.dp))
        ) {
            AsyncImage(
                model = item.backdrop ?: item.poster,
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color(0xD6000000)),
                        startY = 54f
                    )
                )
            )
            Column(Modifier.align(Alignment.BottomStart).padding(12.dp)) {
                Text(item.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text("${item.year}  ★ ${"%.1f".format(item.rating)}", color = Color(0xFFDFDFE6), fontSize = 11.sp)
            }
            progressPercent?.takeIf { it > 0.0 && it < 100.0 }?.let { p ->
                Box(
                    Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(Color(0x55000000))
                ) {
                    Box(
                        Modifier
                            .fillMaxHeight()
                            .fillMaxWidth((p / 100.0).toFloat().coerceIn(0f, 1f))
                            .background(Purple)
                    )
                }
            }
            if (focused) Box(Modifier.matchParentSize().background(Color(0x0FFFFFFF)))
        }
    }
}

@Composable
fun NativeTextField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    password: Boolean = false,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
        cursorBrush = SolidColor(Purple),
        visualTransformation = if (password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        modifier = modifier
            .onFocusChanged { focused = it.isFocused }
            .background(if (focused) Color(0xFF252532) else Panel, RoundedCornerShape(10.dp))
            .border(if (focused) 1.5.dp else 0.dp, Color.White, RoundedCornerShape(10.dp))
            .padding(15.dp),
        decorationBox = { inner ->
            Box {
                if (value.isBlank()) Text(hint, color = Muted, fontSize = 15.sp)
                inner()
            }
        }
    )
}

@Composable
fun Sidebar(screen: Screen, onNavigate: (Screen) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var collapseJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()
    val width by animateDpAsState(
        targetValue = if (expanded) 214.dp else 82.dp,
        animationSpec = tween(120, easing = LinearOutSlowInEasing),
        label = "sideWidth"
    )
    val items = listOf(
        Triple(Screen.HOME, "⌂", "Home"),
        Triple(Screen.SEARCH, "⌕", "Search"),
        Triple(Screen.MOVIES, "▣", "Movies"),
        Triple(Screen.SERIES, "▤", "Series"),
        Triple(Screen.COLLECTIONS, "◇", "Collections"),
        Triple(Screen.MY_LIST, "♡", "My List"),
        Triple(Screen.PROFILES, "●", "Profiles"),
        Triple(Screen.SETTINGS, "⚙", "Settings")
    )

    // This host always occupies only 82dp. The expanded menu is drawn over the content
    // instead of resizing the Home layout, preventing a full-screen horizontal jump.
    Box(Modifier.width(82.dp).fillMaxHeight().zIndex(20f)) {
        Column(
            Modifier
                .width(width)
                .fillMaxHeight()
                .background(Color(0xF0101014))
                .padding(top = 24.dp, start = 12.dp, end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "M",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(start = 17.dp, bottom = 20.dp)
            )
            items.forEach { (destination, icon, label) ->
                var focused by remember(destination) { mutableStateOf(false) }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .onFocusChanged {
                            focused = it.isFocused
                            if (it.isFocused) {
                                collapseJob?.cancel()
                                expanded = true
                            } else {
                                collapseJob?.cancel()
                                collapseJob = scope.launch {
                                    delay(90)
                                    expanded = false
                                }
                            }
                        }
                        .focusable()
                        .clickable { onNavigate(destination) }
                        .background(
                            if (focused || destination == screen) Color(0xFF252532) else Color.Transparent,
                            RoundedCornerShape(10.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.width(30.dp), contentAlignment = Alignment.CenterStart) {
                        Text(icon, color = if (focused || destination == screen) Purple else Color.White, fontSize = 20.sp)
                    }
                    if (expanded) {
                        Spacer(Modifier.width(8.dp))
                        Text(label, color = Color.White, fontSize = 15.sp, maxLines = 1)
                    }
                }
            }
        }
    }
}
