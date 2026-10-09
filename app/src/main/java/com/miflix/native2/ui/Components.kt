package com.miflix.native2.ui

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
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
    horizontalPadding: androidx.compose.ui.unit.Dp = 24.dp,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.018f else 1f,
        animationSpec = FastMotion,
        label = "buttonScale"
    )
    val white = focused
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
            .border(if (focused) 1.5.dp else if(primary) 1.dp else 0.dp, if(focused) Color.White else Color(0xFF686868), RoundedCornerShape(24.dp))
            .padding(horizontal = horizontalPadding, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = if (white) Color.Black else Color.White,
            fontSize = 15.sp, lineHeight=18.sp,
            fontWeight = FontWeight.Bold, maxLines=1, overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}

@Composable
fun BackIconButton(onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(Modifier.size(44.dp).semantics { contentDescription=tr("Buscar","Search") }.onFocusChanged { focused=it.isFocused }.focusable().tvClick(onClick)
        .clickable(onClick=onClick).background(if(focused) SoftWhite else Color(0x990C111B),RoundedCornerShape(22.dp))
        .border(if(focused) 2.dp else 1.dp,if(focused) Color.White else Color(0x557D8AA3),RoundedCornerShape(22.dp)),
        contentAlignment=Alignment.Center) {
        Text("‹",color=if(focused) Color.Black else Color.White,fontSize=32.sp, lineHeight=38.sp)
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
                    Text(item.title, color = Color.White, fontSize = 14.sp, lineHeight=17.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text("${item.year}  ★ ${"%.1f".format(item.rating)}", color = Color(0xFFD5D5D5), fontSize = 11.sp, lineHeight=13.sp)
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
        Text(tile.title, color = Color.White, fontSize = 14.sp, lineHeight=17.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 8.dp, top = 2.dp))
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
            color=if(value.isBlank()) Muted else Color.White,fontSize=16.sp, lineHeight=19.sp,maxLines=1)
    }
    if(editing) {
        val editorFocus=remember { FocusRequester() }
        DisposableEffect(Unit) { onDispose { keyboard?.hide() } }
        fun close() { keyboard?.hide(); editing=false }
        Dialog(onDismissRequest={ close() },properties=DialogProperties(usePlatformDefaultWidth=false)) {
            Column(Modifier.width(560.dp).background(Panel,RoundedCornerShape(20.dp)).padding(24.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
                Text(hint,color=Color.White,fontSize=22.sp, lineHeight=26.sp)
                BasicTextField(value=value,onValueChange=onValueChange,singleLine=true,
                    textStyle=TextStyle(color=Color.White,fontSize=19.sp, lineHeight=23.sp),cursorBrush=SolidColor(Color.White),
                    visualTransformation=if(password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
                    modifier=Modifier.fillMaxWidth().focusRequester(editorFocus).background(Color(0xFF202A3B),RoundedCornerShape(12.dp)).padding(16.dp))
                FocusButton(tr("Listo","Done"),primary=true) { close() }
            }
            LaunchedEffect(Unit) { delay(150); editorFocus.requestFocus(); keyboard?.show() }
        }
    }
}

/** Compact top navigation. White fill belongs only to the control with remote focus. */
@Composable
fun Sidebar(screen: Screen, profileName: String, onNavigate: (Screen) -> Unit) {
    Sidebar(screen,profileName,onNavigate,0)
}

@Composable
fun Sidebar(screen: Screen,profileName: String,onNavigate: (Screen) -> Unit,notificationCount: Int,
            profile: com.miflix.native2.model.Profile? = null, backgroundAlpha: Float = 0f, onHeaderFocused: (() -> Unit)? = null, onMoveDown: (() -> Unit)? = null) {
    val current=when(screen) {
        Screen.WATCH_PARTY -> Screen.FRIENDS
        Screen.GENRES,Screen.GENRE_DETAIL,Screen.YEAR_DETAIL,Screen.PLATFORM_DETAIL,Screen.COLLECTION_DETAIL -> Screen.COLLECTIONS
        else -> screen
    }
    val navItems=listOf(
        Screen.HOME to tr("Inicio","Home"),
        Screen.SEARCH to tr("Buscar","Search"),
        Screen.COLLECTIONS to tr("Colecciones","Collections"),
        Screen.FRIENDS to tr("Amigos y salas","Friends & Party"),
        Screen.LIVE_TV to tr("TV en vivo","Live TV"),
        Screen.MY_LIST to tr("Mi lista","My List"),
        Screen.SETTINGS to tr("Ajustes","Settings")
    )
    Row(Modifier.fillMaxWidth().height(76.dp).onPreviewKeyEvent { event ->
        if(event.key==Key.DirectionDown && onMoveDown!=null) {
            if(event.type==KeyEventType.KeyDown) onMoveDown()
            true
        } else false
    }.onFocusChanged { if(it.hasFocus) onHeaderFocused?.invoke() }.focusGroup()
        .background(Color.Black.copy(alpha=backgroundAlpha.coerceIn(0f,1f))).padding(horizontal=24.dp),
        verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(18.dp)) {
        var profileFocused by remember { mutableStateOf(false) }
        Row(Modifier.onFocusChanged { profileFocused=it.isFocused }.focusable()
            .tvClick { onNavigate(Screen.PROFILES) }.clickable { onNavigate(Screen.PROFILES) }
            .background(if(profileFocused) Color.White else Color(0xFF242426),RoundedCornerShape(14.dp)).padding(7.dp),
            verticalAlignment=Alignment.CenterVertically) {
            if(profile!=null) ProfileAvatar(profile,Modifier.size(34.dp))
            else Box(Modifier.size(34.dp),contentAlignment=Alignment.Center) { Text(profileName.take(1),color=if(profileFocused) Color.Black else Color.White) }
            Text("⌄",color=if(profileFocused) Color.Black else Color.White,modifier=Modifier.padding(start=6.dp),fontSize=16.sp)
        }
        var navOrigin by remember { mutableStateOf(0f) }
        val bounds=remember { mutableStateMapOf<Screen,Pair<Float,Float>>() }
        var target by remember { mutableStateOf<Screen?>(null) }
        var navFocused by remember { mutableStateOf(false) }
        val x by animateFloatAsState(target?.let { bounds[it] }?.first ?: 0f,tween(150),label="navFocusX")
        val width by animateFloatAsState(target?.let { bounds[it] }?.second ?: 0f,tween(150),label="navFocusWidth")
        val density=LocalDensity.current
        Box(Modifier.weight(1f).height(60.dp).clipToBounds().onGloballyPositioned { navOrigin=it.positionInRoot().x }) {
            androidx.compose.foundation.lazy.LazyRow(Modifier.fillMaxSize().onFocusChanged { navFocused=it.hasFocus },
                verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp),
                contentPadding=PaddingValues(horizontal=4.dp,vertical=6.dp)) {
                items(navItems,key={it.first}) { (destination,label) ->
                    var focused by remember { mutableStateOf(false) }
                    val lift by animateFloatAsState(if(focused) -2f else 0f,tween(150),label="navLift")
                    Box(Modifier.onGloballyPositioned { bounds[destination]=Pair(it.positionInRoot().x-navOrigin,it.size.width.toFloat()) }
                        .graphicsLayer { translationY=lift }) {
                        if(destination==Screen.SEARCH) SearchNavigationButton(onFocused={ focused=it; if(it) target=destination }) { onNavigate(destination) }
                        else FocusButton(label,primary=current==destination,horizontalPadding=14.dp,onFocused={ focused=it; if(it) target=destination }) { onNavigate(destination) }
                    }
                }
            }
            if(navFocused && width>0) Box(Modifier.align(Alignment.BottomStart)
                .graphicsLayer { translationX=x }.width(with(density) { width.toDp() }).height(2.dp)
                .background(Color.White,RoundedCornerShape(2.dp)))
        }
        NotificationButton(notificationCount) { onNavigate(Screen.NOTIFICATIONS) }
    }
}

@Composable
private fun NotificationButton(count: Int,onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(Modifier.size(46.dp).semantics { contentDescription="${tr("Avisos","Alerts")}: $count" }.onFocusChanged { focused=it.isFocused }.focusable().tvClick(onClick).clickable(onClick=onClick)
        .background(if(focused) Color.White else Color(0xFF242426),RoundedCornerShape(16.dp)),contentAlignment=Alignment.Center) {
        BellIcon(if(focused) Color.Black else Color.White,Modifier.size(25.dp))
        if(count>0) Text(if(count>9) "9+" else count.toString(),color=if(focused) Color.White else Color.Black,fontSize=10.sp,lineHeight=12.sp,
            modifier=Modifier.align(Alignment.TopEnd).background(if(focused) Color(0xFF292929) else Color(0xFFCCCCCC),RoundedCornerShape(8.dp)).padding(horizontal=4.dp,vertical=2.dp))
    }
}

@Composable
private fun BellIcon(color: Color,modifier: Modifier) {
    Canvas(modifier) {
        val w=size.width; val h=size.height
        val bell=androidx.compose.ui.graphics.Path().apply {
            moveTo(w*.23f,h*.73f); lineTo(w*.3f,h*.6f); lineTo(w*.3f,h*.4f)
            cubicTo(w*.3f,h*.08f,w*.7f,h*.08f,w*.7f,h*.4f)
            lineTo(w*.7f,h*.6f); lineTo(w*.77f,h*.73f); close()
        }
        drawPath(bell,color,style=androidx.compose.ui.graphics.drawscope.Stroke(width=1.5.dp.toPx()))
        drawCircle(color,1.7.dp.toPx(),androidx.compose.ui.geometry.Offset(w*.5f,h*.88f))
    }
}

@Composable
private fun SearchNavigationButton(onFocused: (Boolean) -> Unit = {},onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(Modifier.size(44.dp).semantics { contentDescription=tr("Buscar","Search") }.onFocusChanged { focused=it.isFocused; onFocused(it.isFocused) }.focusable().tvClick(onClick).clickable(onClick=onClick)
        .background(if(focused) Color.White else Color.Transparent,RoundedCornerShape(16.dp)),contentAlignment=Alignment.Center) {
        Canvas(Modifier.size(24.dp)) {
            val ink=if(focused) Color.Black else Color.White
            val center=androidx.compose.ui.geometry.Offset(size.width*.4f,size.height*.4f)
            drawCircle(ink,size.width*.29f,center,style=androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
            drawLine(ink,androidx.compose.ui.geometry.Offset(size.width*.62f,size.height*.62f),
                androidx.compose.ui.geometry.Offset(size.width*.94f,size.height*.94f),2.dp.toPx(),androidx.compose.ui.graphics.StrokeCap.Round)
        }
    }
}
