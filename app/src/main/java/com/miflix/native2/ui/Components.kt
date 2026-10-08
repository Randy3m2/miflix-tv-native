package com.miflix.native2.ui

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.res.painterResource
import com.miflix.native2.R
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
import androidx.compose.ui.composed
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.miflix.native2.model.CollectionTile
import com.miflix.native2.model.MediaSummary

val Bg = Color(0xFF050505)
val Panel = Color(0xFF151515)
val Muted = Color(0xFF9A9A9F)
val SoftWhite = Color(0xFFF3F3F3)

private val FastMotion = tween<Float>(durationMillis = 90, easing = LinearOutSlowInEasing)

fun Modifier.tvClick(onClick: () -> Unit): Modifier = this.onPreviewKeyEvent { event ->
    val activate = event.key == Key.DirectionCenter ||
        event.key == Key.Enter ||
        event.key == Key.NumPadEnter
    if (activate && event.type == KeyEventType.KeyUp) {
        onClick()
        true
    } else {
        false
    }
}

// Consume the full key gesture so a long press never triggers quick play on release.
fun Modifier.tvPlaybackClick(onClick: () -> Unit, onLongClick: () -> Unit): Modifier = composed {
    val scope = rememberCoroutineScope()
    val shortAction by rememberUpdatedState(onClick)
    val longAction by rememberUpdatedState(onLongClick)
    var pending by remember { mutableStateOf<Job?>(null) }
    var held by remember { mutableStateOf(false) }
    var longFired by remember { mutableStateOf(false) }
    DisposableEffect(Unit) { onDispose { pending?.cancel() } }
    this.onFocusChanged {
        if (!it.hasFocus) { pending?.cancel(); held = false }
    }.onPreviewKeyEvent { event ->
        val code = event.nativeKeyEvent.keyCode
        val activate = code in listOf(23, 66, 160, 85, 126)
        if (!activate) false else {
            if (event.type == KeyEventType.KeyDown && !held) {
                held = true
                longFired = false
                pending = scope.launch {
                    delay(1000)
                    if (held) { longFired = true; longAction() }
                }
            } else if (event.type == KeyEventType.KeyUp) {
                pending?.cancel()
                val quick = held && !longFired && !event.nativeKeyEvent.isCanceled
                held = false
                if (quick) shortAction()
            }
            true
        }
    }
}

@Composable
fun FocusButton(
    text: String,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    onFocused: ((Boolean) -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.018f else 1f,
        animationSpec = FastMotion,
        label = "buttonScale"
    )
    val white = focused || primary
    Box(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .onFocusChanged {
                focused = it.isFocused
                onFocused?.invoke(it.isFocused)
            }
            .focusable()
            .then(if (onLongClick != null) Modifier.tvPlaybackClick(onClick, onLongClick) else Modifier.tvClick(onClick))
            .clickable(onClick = onClick)
            .background(if (white) SoftWhite else Color(0xE6222222), RoundedCornerShape(24.dp))
            .border(if (focused && !primary) 1.5.dp else 0.dp, Color.White, RoundedCornerShape(24.dp))
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = if (white) Color.Black else Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun BackIconButton(onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(Modifier.size(44.dp).onFocusChanged { focused=it.isFocused }.focusable().tvClick(onClick)
        .clickable(onClick=onClick).background(if(focused) SoftWhite else Color(0x990C111B),RoundedCornerShape(22.dp))
        .border(if(focused) 2.dp else 1.dp,if(focused) Color.White else Color(0x557D8AA3),RoundedCornerShape(22.dp)),
        contentAlignment=Alignment.Center) {
        Text("‹",color=if(focused) Color.Black else Color.White,fontSize=32.sp)
    }
}

@Composable
fun MediaCard(
    item: MediaSummary,
    modifier: Modifier = Modifier,
    landscape: Boolean = false,
    progressPercent: Double? = null,
    onFocused: (MediaSummary) -> Unit = {},
    onClick: () -> Unit
) {
    var focused by remember(item.cloudId) { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.025f else 1f,
        animationSpec = FastMotion,
        label = "cardScale"
    )
    val outerW = if (landscape) 286.dp else 184.dp
    val outerH = if (landscape) 174.dp else 274.dp
    val innerW = if (landscape) 272.dp else 170.dp
    val innerH = if (landscape) 154.dp else 252.dp

    Box(modifier.width(outerW).height(outerH), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .width(innerW)
                .height(innerH)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    shadowElevation = if (focused) 16f else 0f
                }
                .onFocusChanged {
                    focused = it.isFocused
                    if (it.isFocused) onFocused(item)
                }
                .focusable()
                .tvClick(onClick)
                .clickable(onClick = onClick)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1A1A1A))
                .border(if (focused) 2.dp else 0.dp, Color.White, RoundedCornerShape(12.dp))
        ) {
            AsyncImage(
                model = if (landscape) item.backdrop ?: item.poster else item.poster ?: item.backdrop,
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            if (landscape) {
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(listOf(Color.Transparent, Color(0xD9000000)), startY = 60f)
                    )
                )
                Column(Modifier.align(Alignment.BottomStart).padding(12.dp)) {
                    Text(item.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text("${item.year}  ★ ${"%.1f".format(item.rating)}", color = Color(0xFFD5D5D5), fontSize = 11.sp)
                }
            }
            progressPercent?.takeIf { it > 0.0 && it < 100.0 }?.let { p ->
                Box(
                    Modifier.align(Alignment.BottomStart).fillMaxWidth().height(4.dp).background(Color(0x66000000))
                ) {
                    Box(
                        Modifier.fillMaxHeight()
                            .fillMaxWidth((p / 100.0).toFloat().coerceIn(0f, 1f))
                            .background(Color.White)
                    )
                }
            }
            if (focused) Box(Modifier.matchParentSize().background(Color(0x0AFFFFFF)))
        }
    }
}

@Composable
fun CollectionCard(
    tile: CollectionTile,
    onClick: () -> Unit
) {
    var focused by remember(tile.id) { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.022f else 1f,
        animationSpec = FastMotion,
        label = "collectionScale"
    )
    val model = if (focused && !tile.focusGifUrl.isNullOrBlank()) tile.focusGifUrl else tile.coverUrl

    Column(Modifier.width(300.dp)) {
        Box(Modifier.width(300.dp).height(178.dp), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .width(286.dp)
                    .height(162.dp)
                    .graphicsLayer { scaleX = scale; scaleY = scale; shadowElevation = if (focused) 14f else 0f }
                    .onFocusChanged { focused = it.isFocused }
                    .focusable()
                    .tvClick(onClick)
                    .clickable(onClick = onClick)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF191919))
                    .border(if (focused) 2.dp else 0.dp, Color.White, RoundedCornerShape(14.dp))
            ) {
                AsyncImage(
                    model = model,
                    contentDescription = tile.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                if (focused) Box(Modifier.fillMaxSize().background(Color(0x0DFFFFFF)))
            }
        }
        Text(tile.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 8.dp, top = 2.dp))
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
    var editing by remember { mutableStateOf(false) }
    val keyboard=LocalSoftwareKeyboardController.current
    Box(modifier.onFocusChanged { focused=it.isFocused }.focusable().tvClick { editing=true }
        .clickable { editing=true }.background(if(focused) Color(0xFF263047) else Panel,RoundedCornerShape(12.dp))
        .border(if(focused) 2.dp else 1.dp,if(focused) Color.White else Color(0xFF343C50),RoundedCornerShape(12.dp)).padding(15.dp)) {
        Text(if(value.isBlank()) hint else if(password) "•".repeat(value.length.coerceAtMost(24)) else value,
            color=if(value.isBlank()) Muted else Color.White,fontSize=16.sp,maxLines=1)
    }
    if(editing) {
        val editorFocus=remember { FocusRequester() }
        DisposableEffect(Unit) { onDispose { keyboard?.hide() } }
        fun close() { keyboard?.hide(); editing=false }
        Dialog(onDismissRequest={ close() },properties=DialogProperties(usePlatformDefaultWidth=false)) {
            Column(Modifier.width(560.dp).background(Panel,RoundedCornerShape(20.dp)).padding(24.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
                Text(hint,color=Color.White,fontSize=22.sp)
                BasicTextField(value=value,onValueChange=onValueChange,singleLine=true,
                    textStyle=TextStyle(color=Color.White,fontSize=19.sp),cursorBrush=SolidColor(Color.White),
                    visualTransformation=if(password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
                    modifier=Modifier.fillMaxWidth().focusRequester(editorFocus).background(Color(0xFF202A3B),RoundedCornerShape(12.dp)).padding(16.dp))
                FocusButton("Listo",primary=true) { close() }
            }
            LaunchedEffect(Unit) { delay(150); editorFocus.requestFocus(); keyboard?.show() }
        }
    }
}

@Composable
fun Sidebar(screen: Screen, profileName: String, onNavigate: (Screen) -> Unit) {
    Sidebar(screen, profileName, onNavigate, 0)
}

@Composable
fun Sidebar(screen: Screen, profileName: String, onNavigate: (Screen) -> Unit, notificationCount: Int) {
    val navItems = listOf(
        Triple(Screen.HOME, "⌂", "Home"),
        Triple(Screen.SEARCH, "⌕", "Search"),
        Triple(Screen.COLLECTIONS, "◇", "Collections"),
        Triple(Screen.WATCH_PARTY, "◉", "Party"),
        Triple(Screen.FRIENDS, "♧", "Friends"),
        Triple(Screen.LIVE_TV, "●", "Live TV"),
        Triple(Screen.GENRES, "▦", "Genres"),
        Triple(Screen.MY_LIST, "♡", "My List"),
        Triple(Screen.NOTIFICATIONS, "🔔", if(notificationCount > 0) "Alerts ${notificationCount}+" else "Alerts"),
        Triple(Screen.SETTINGS, "⚙", "Settings")
    )

    Column(
        Modifier
            .width(154.dp)
            .fillMaxHeight()
            .background(Color(0xFF090909))
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)) {
                Box(Modifier.size(34.dp).background(Color.White, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                    Image(painterResource(R.drawable.brunio_icon), "BruniO", Modifier.fillMaxSize())
                }
                Spacer(Modifier.width(9.dp))
                Text("BruniO", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
            }
        LazyColumn(Modifier.weight(1f).fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(3.dp)) {
            items(navItems,key={it.first}) { (destination, icon, label) ->
                var focused by remember(destination) { mutableStateOf(false) }
                val active = destination == screen
                Row(
                    Modifier
                        .fillMaxWidth().height(36.dp)
                        .onFocusChanged { focused = it.isFocused }
                        .focusable()
                        .tvClick { onNavigate(destination) }
                        .clickable { onNavigate(destination) }
                        .background(if (focused || active) Color(0xFFF2F2F2) else Color.Transparent, RoundedCornerShape(12.dp))
                        .padding(horizontal = 11.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(icon, color = if (focused || active) Color.Black else Color.White, fontSize = 19.sp, modifier = Modifier.width(28.dp))
                    Text(label, color = if (focused || active) Color.Black else Color(0xFFE6E6E6), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                }
            }
        }

        var profileFocused by remember { mutableStateOf(false) }
        Row(
            Modifier
                .fillMaxWidth()
                .onFocusChanged { profileFocused = it.isFocused }
                .focusable()
                .tvClick { onNavigate(Screen.PROFILES) }
                .clickable { onNavigate(Screen.PROFILES) }
                .background(if (profileFocused) Color.White else Color.Transparent, RoundedCornerShape(12.dp))
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(32.dp).background(if (profileFocused) Color.Black else Color(0xFF292929), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                Text(profileName.take(1).uppercase(), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(9.dp))
            Text(profileName, color = if (profileFocused) Color.Black else Color.White, fontSize = 12.sp, maxLines = 1)
        }
    }
}
