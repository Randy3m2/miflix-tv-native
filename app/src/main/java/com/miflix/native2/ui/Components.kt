package com.miflix.native2.ui

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import androidx.compose.ui.graphics.luminance
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

internal object ThemeRuntimeRP177 {
    // Read the current route without overwriting the saved theme or changing it on Back.
    var originalPageRP189:()->Boolean={false}
    private var savedId by mutableStateOf("")
    var id:String
        get()=if(originalPageRP189()) "" else savedId
        set(value) {savedId=value}
    private var savedSecondary by mutableIntStateOf(0xFF9F82F5.toInt())
    var secondary:Int
        get()=if(originalPageRP189()) 0xFFB792FF.toInt() else savedSecondary
        set(value) {savedSecondary=value}
    private var savedBackground by mutableIntStateOf(0xFF050505.toInt())
    var background:Int
        get()=if(originalPageRP189()) 0xFF050505.toInt() else savedBackground
        set(value) {savedBackground=value}
    private var savedPanel by mutableIntStateOf(0xFF141414.toInt())
    var panel:Int
        get()=if(originalPageRP189()) 0xFF141414.toInt() else savedPanel
        set(value) {savedPanel=value}
    private var savedMuted by mutableIntStateOf(0xFFAAAAAF.toInt())
    var muted:Int
        get()=if(originalPageRP189()) 0xFFAAAAAF.toInt() else savedMuted
        set(value) {savedMuted=value}
    private var savedFocusedPanel by mutableIntStateOf(0xFF242126.toInt())
    var focusedPanel:Int
        get()=if(originalPageRP189()) 0xFF242126.toInt() else savedFocusedPanel
        set(value) {savedFocusedPanel=value}
    private var savedBorder by mutableIntStateOf(0xFF303035.toInt())
    var border:Int
        get()=if(originalPageRP189()) 0xFF303035.toInt() else savedBorder
        set(value) {savedBorder=value}
    private var savedRadius by mutableIntStateOf(50)
    var radius:Int
        get()=if(originalPageRP189()) 50 else savedRadius
        set(value) {savedRadius=value}
    fun restore(value:Int) {
        val color=com.miflix.native2.data.ThemeStudioRP173.normalizeFocus(value)
        val preset=com.miflix.native2.data.ThemeStudioRP173.presets.firstOrNull {it.focus.toInt()==color}
        if(preset!=null && preset.id!="default") apply(preset) else {
            id="";background=0xFF050505.toInt();panel=0xFF141414.toInt()
            muted=0xFFAAAAAF.toInt();focusedPanel=0xFF242126.toInt();border=0xFF303035.toInt()
            radius=50;secondary=color;FocusTheme.argb=color
        }
    }
    fun apply(preset: com.miflix.native2.data.ThemeStudioPresetRP173) {
        if(preset.id=="default") {restore(preset.focus.toInt());return}
        id=preset.id; secondary=preset.secondary.toInt()
        background=preset.background.toInt(); panel=preset.surface.toInt(); muted=preset.secondary.toInt()
        focusedPanel=preset.surface.toInt(); border=preset.primary.toInt()
        radius=com.miflix.native2.data.ThemeStudioRP173.cornerRadius(preset.id)
        FocusTheme.argb=preset.focus.toInt()
    }
}
val Bg: Color get() = Color(ThemeRuntimeRP177.background)
val Panel: Color get() = Color(ThemeRuntimeRP177.panel)
val Muted: Color get() = Color(ThemeRuntimeRP177.muted)
val SoftWhite = Color(0xFFF3F3F3)
internal object FocusTheme {
    private var savedArgb by mutableIntStateOf(0xFFB792FF.toInt())
    var argb:Int
        get()=if(ThemeRuntimeRP177.originalPageRP189()) 0xFFB792FF.toInt() else savedArgb
        set(value) {savedArgb=value}
}
val Accent: Color get() = Color(FocusTheme.argb)
val FocusInk: Color get() = if(Accent.luminance()>.179f) Color.Black else Color.White
val FocusedPanel: Color get() = Color(ThemeRuntimeRP177.focusedPanel)
val PanelBorder: Color get() = Color(ThemeRuntimeRP177.border)
internal typealias ProfileMenuAction = () -> Unit
internal val LocalProfileManage=staticCompositionLocalOf<ProfileMenuAction?> {null}
internal val LocalCastPair=staticCompositionLocalOf<ProfileMenuAction?> {null}
val BrowseTileWidth = 240.dp
val BrowseTileHeight = 136.dp
val BrowseTileSlotWidth = 254.dp
val BrowseTileSlotHeight = 152.dp

private val FastMotion = tween<Float>(durationMillis = 80, easing = LinearOutSlowInEasing)

fun Modifier.tvClick(onClick: () -> Unit): Modifier = composed {
    val action by rememberUpdatedState(onClick)
    val press=remember {com.miflix.native2.data.TvPressRP163()}
    this.tvFocusAnchorRP163().onFocusChanged {if(!it.hasFocus) press.reset()}.onPreviewKeyEvent {event ->
        MenuActivity.touch()
        val activate=event.key==Key.DirectionCenter || event.key==Key.Enter || event.key==Key.NumPadEnter || event.key==Key.Spacebar
        if(!activate) false else {
            if(event.type==KeyEventType.KeyDown) press.down(event.nativeKeyEvent.repeatCount)
            else if(event.type==KeyEventType.KeyUp && press.up(event.nativeKeyEvent.isCanceled)) action()
            true
        }
    }
}

// Consume the full key gesture so a long press never triggers quick play on release.
fun Modifier.tvPlaybackClick(onClick: () -> Unit, onLongClick: () -> Unit,focusKeyRP163:String?=null): Modifier = composed {
    val scope = rememberCoroutineScope()
    val shortAction by rememberUpdatedState(onClick)
    val longAction by rememberUpdatedState(onLongClick)
    var pending by remember { mutableStateOf<Job?>(null) }
    var held by remember { mutableStateOf(false) }
    var longFired by remember { mutableStateOf(false) }
    DisposableEffect(Unit) { onDispose { pending?.cancel() } }
    this.tvFocusAnchorRP163(focusKeyRP163).onFocusChanged {
        if (!it.hasFocus) { pending?.cancel(); held = false }
    }.onPreviewKeyEvent { event ->
        MenuActivity.touch()
        val code = event.nativeKeyEvent.keyCode
        val activate = code in listOf(23, 66, 160, 85, 126)
        if (!activate) false else {
            if (event.type == KeyEventType.KeyDown && !held) {
                held = true
                longFired = false
                pending = scope.launch {
                    delay(750)
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
    destructive: Boolean = false,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val actionFocused=actionFocusVisibleRP64(focused)
    val actionShape=themeShapeRP181(if(ThemeRuntimeRP177.id.isBlank()) 50.dp else 16.dp)
    val focusScale by animateFloatAsState(if(actionFocused) 1.024f else 1f,tween(115),label="actionFocusScaleRP193")
    val borderTone by animateColorAsState(if(actionFocused) Color.White else if(destructive) Color(0xFF78404B) else if(primary) Accent.copy(alpha=.55f) else PanelBorder,tween(115),label="actionBorderRP193")
    val focusFillRP197 by animateColorAsState(if(actionFocused) Color(0xFFF5F5F5) else Color.Transparent,tween(125),label="actionWhiteFillRP197")
    val textToneRP197 by animateColorAsState(if(actionFocused) Color(0xFF161616) else if(destructive) Color(0xFFFFB9C3) else Color.White,tween(125),label="actionTextRP197")
    Box(
        modifier
            .graphicsLayer { scaleX=focusScale; scaleY=focusScale }
            .onFocusChanged {
                focused = it.isFocused
                onFocused?.invoke(it.isFocused)
            }

            .then(if (onLongClick != null) Modifier.tvPlaybackClick(onClick, onLongClick) else Modifier.tvClick(onClick))
            .clickable(onClick = onClick)
            .background(GlassActionBrushRP108(false,primary,destructive),actionShape)
            .background(focusFillRP197,actionShape)
            .border(if (actionFocused) 2.dp else 1.dp,borderTone,actionShape)
            .then(if(actionFocused) Modifier else Modifier.themeOrnamentRP181())
            .padding(horizontal = horizontalPadding, vertical = if(LocalSimpleHome.current) 16.dp else 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(9.dp)) {
        Text(
            text,
            color = textToneRP197,
            fontSize = if(LocalSimpleHome.current) 20.sp else 15.sp, lineHeight=if(LocalSimpleHome.current) 25.sp else 20.sp,
            fontWeight = FontWeight.Bold, maxLines=1, overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
        }
    }
}

@Composable
fun BackIconButton(onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val actionFocused=actionFocusVisibleRP64(focused)
    Box(Modifier.size(44.dp).semantics { contentDescription=tr("Volver","Back") }.onFocusChanged { focused=it.isFocused }.tvClick(onClick)
        .clickable(onClick=onClick).background(GlassActionBrushRP108(actionFocused),RoundedCornerShape(22.dp))
        .border(if(actionFocused) 2.dp else 1.dp,if(actionFocused) Accent else Color(0x557D8AA3),RoundedCornerShape(22.dp)),
        contentAlignment=Alignment.Center) {
        Text("‹",color=if(actionFocused) Color.White else Color.White,fontSize=32.sp, lineHeight=38.sp)
    }
}

@Composable
fun MediaCard(
    item: MediaSummary,
    modifier: Modifier = Modifier,
    landscape: Boolean = false,
    compact: Boolean = false,
    progressPercent: Double? = null,
    onFocused: (MediaSummary) -> Unit = {},
    showCaption: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    allowContextMenu: Boolean = true,
    onClick: () -> Unit
) {
    val publishPreview=LocalBrowsePreview.current
    val titleActions=LocalTitleActions.current
    val hold=onLongClick ?: titleActions?.takeIf {allowContextMenu}?.let {action -> {action(item)}}
    var focused by remember(item.cloudId) { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.012f else 1f,
        animationSpec = FastMotion,
        label = "cardScale"
    )
    val outerW = if(compact) { if(landscape) 230.dp else 144.dp } else if (landscape) 260.dp else 168.dp
    val captionHeight=if(landscape || !showCaption) 0.dp else 42.dp
    val outerH = if(compact) { if(landscape) 146.dp else 220.dp } else if (landscape) 160.dp else 252.dp
    val innerW = outerW-14.dp
    val innerH = if(compact) { if(landscape) 126.dp else 198.dp } else if (landscape) 140.dp else 230.dp

    Box(modifier.width(outerW).height(outerH+captionHeight), contentAlignment = Alignment.TopCenter) {
        Box(
            Modifier
                .padding(top=7.dp)
                .width(innerW)
                .height(innerH)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    shadowElevation = 0f
                }
                .contentFocusBorder(18.dp)

                .onFocusChanged {
                    focused = it.isFocused
                    if (it.isFocused) { onFocused(item);publishPreview?.invoke(item) }
                }

                .then(if(hold==null) Modifier.tvClick(onClick) else Modifier.tvPlaybackClick(onClick,hold))
                .clickable(onClick = onClick)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF1A1A1A))
        ) {
            LoadingArtworkRP58(
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
            observeCardFreshness(item)?.let {label ->
                CardFreshnessTag(label,Modifier.align(Alignment.TopEnd).padding(7.dp).widthIn(max=150.dp))
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
        if(!landscape && showCaption) Column(Modifier.align(Alignment.BottomCenter).width(innerW).padding(bottom=4.dp),verticalArrangement=Arrangement.spacedBy(3.dp)) {
            Text(item.title,color=Color.White,fontSize=12.sp,lineHeight=15.sp,fontWeight=FontWeight.SemiBold,maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            Text(listOf(item.year,if(item.type=="series") tr("Serie","Series") else tr("Película","Movie")).filter { it.isNotBlank() }.joinToString(" · "),color=Muted,fontSize=10.sp,lineHeight=12.sp,maxLines=1)
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
        targetValue = if (focused) 1.012f else 1f,
        animationSpec = FastMotion,
        label = "collectionScale"
    )
    var animationReady by remember(tile.id,focused) { mutableStateOf(false) }
    var animationFailed by remember(tile.id) { mutableStateOf(false) }
    var coverFailed by remember(tile.id) { mutableStateOf(false) }
    LaunchedEffect(tile.id,focused) {
        if(focused && !tile.focusGifUrl.isNullOrBlank()) { delay(420);animationReady=true }
    }
    val model = if (focused && animationReady && !animationFailed && !tile.focusGifUrl.isNullOrBlank()) tile.focusGifUrl else tile.coverUrl

    Column(Modifier.width(BrowseTileSlotWidth)) {
        Box(Modifier.width(BrowseTileSlotWidth).height(BrowseTileSlotHeight), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .width(BrowseTileWidth)
                    .height(BrowseTileHeight)
                    .graphicsLayer { scaleX = scale; scaleY = scale; shadowElevation = 0f }
                    .contentFocusBorder(14.dp)

                    .onFocusChanged { focused = it.isFocused }

                    .tvClick(onClick)
                    .clickable(onClick = onClick)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF191919))
            ) {
                val franchisePhoto=when(tile.id) {
                    "franchise:star_wars" -> R.drawable.franchise_star_wars
                    "franchise:xmen" -> R.drawable.franchise_xmen
                    "franchise:walking_dead" -> R.drawable.franchise_walking_dead
                    "franchise:game_of_thrones" -> R.drawable.franchise_game_of_thrones
                    else -> null
                }
                if(franchisePhoto!=null) Box(Modifier.fillMaxSize()) {
                    Image(painterResource(franchisePhoto),tile.title,modifier=Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
                    Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(Color.Transparent,Color(0x33000000),Color(0xE6000000)))))
                    Text(tile.title,color=Color.White,fontSize=23.sp,lineHeight=26.sp,fontWeight=FontWeight.Bold,
                        maxLines=2,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier=Modifier.align(Alignment.BottomStart).padding(16.dp))
                } else if(tile.id.startsWith("franchise:")) Box(Modifier.fillMaxSize().background(if(tile.id.endsWith("marvel")) Color(0xFF24080C) else Color(0xFF081B32)),contentAlignment=Alignment.Center) {
                    Image(painterResource(if(tile.id.endsWith("marvel")) R.drawable.franchise_marvel else R.drawable.franchise_dc),tile.title,
                        modifier=Modifier.fillMaxSize().padding(if(tile.id.endsWith("marvel")) 24.dp else 10.dp),contentScale=ContentScale.Fit)
                } else if(tile.id=="amcplus") Box(Modifier.fillMaxSize().background(Color(0xFF081716)),contentAlignment=Alignment.Center) {
                    Image(painterResource(R.drawable.streaming_amcplus_rp48),tile.title,Modifier.fillMaxSize().padding(32.dp),contentScale=ContentScale.Fit)
                } else if(tile.id=="natgeo") Row(Modifier.fillMaxSize().padding(28.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                    Box(Modifier.width(44.dp).height(66.dp).border(5.dp,Color(0xFFFFCC00)))
                    Text("NATIONAL\nGEOGRAPHIC",color=Color.White,fontSize=19.sp,lineHeight=23.sp,fontWeight=FontWeight.Bold)
                } else {
                if(coverFailed) Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) {
                    Text(tile.title,color=Color.White,fontSize=25.sp,fontWeight=FontWeight.Bold)
                }
                AsyncImage(
                    model = model,
                    contentDescription = tile.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    onError={ if(model==tile.focusGifUrl) animationFailed=true else coverFailed=true },
                    onSuccess={ coverFailed=false }
                )
                }
                if (focused) Box(Modifier.fillMaxSize().background(Color(0x0DFFFFFF)))
            }
        }

    }
}

@Composable
fun NativeTextField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    password: Boolean = false,
    modifier: Modifier = Modifier,
    onSubmit: (() -> Unit)? = null
) {
    var focused by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    val keyboard=LocalSoftwareKeyboardController.current
    Box(modifier.onFocusChanged { focused=it.isFocused }.tvClick { editing=true }
        .clickable { editing=true }.background(GlassActionBrushRP108(focused),RoundedCornerShape(12.dp))
        .border(if(focused) 2.dp else 1.dp,if(focused) Accent else PanelBorder,RoundedCornerShape(12.dp)).padding(15.dp)) {
        Text(if(value.isBlank()) hint else if(password) "•".repeat(value.length.coerceAtMost(24)) else value,
            color=if(value.isBlank()) Muted else Color.White,fontSize=16.sp, lineHeight=19.sp,maxLines=1)
    }
    if(editing) {
        val editorFocus=remember { FocusRequester() }
        DisposableEffect(Unit) { onDispose { keyboard?.hide() } }
        fun close() { keyboard?.hide(); editing=false; onSubmit?.invoke() }
        Dialog(onDismissRequest={ close() },properties=DialogProperties(usePlatformDefaultWidth=false)) {
            Column(Modifier.width(560.dp).background(Panel,RoundedCornerShape(20.dp)).padding(24.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
                Text(hint,color=Color.White,fontSize=22.sp, lineHeight=26.sp)
                BasicTextField(value=value,onValueChange=onValueChange,singleLine=true,
                    keyboardOptions=androidx.compose.foundation.text.KeyboardOptions(imeAction=androidx.compose.ui.text.input.ImeAction.Done),
                    keyboardActions=androidx.compose.foundation.text.KeyboardActions(onDone={ close() }),
                    textStyle=TextStyle(color=Color.White,fontSize=19.sp, lineHeight=23.sp),cursorBrush=SolidColor(Color.White),
                    visualTransformation=if(password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
                    modifier=Modifier.fillMaxWidth().focusRequester(editorFocus).background(Panel,RoundedCornerShape(12.dp)).padding(16.dp))
                FocusButton(tr("Listo","Done"),primary=true) { close() }
            }
            LaunchedEffect(Unit) { delay(150); editorFocus.requestFocus(); keyboard?.show() }
        }
    }
}

/** Keep the Netflix-style top navigation; focus moves without changing layout bounds. */
internal object HeaderNavigationRP108 {
    val policy=com.miflix.native2.data.HeaderFocusRP108()
    fun hasPending()=policy.hasPending()
}

@Composable
fun Sidebar(screen: Screen, profileName: String, onNavigate: (Screen) -> Unit) {
    Sidebar(screen,profileName,onNavigate,0)
}

@Composable
fun Sidebar(screen: Screen,profileName: String,onNavigate: (Screen) -> Unit,notificationCount: Int,
            profile: com.miflix.native2.model.Profile? = null, backgroundAlpha: Float = 0f, onHeaderFocused: (() -> Unit)? = null, onMoveDown: (() -> Unit)? = null,onEditPicture: (() -> Unit)? = null,kids: Boolean=false) {
    val assistantOpenRP150=LocalAssistantOpenRP150.current
    val dismissAssistantRP160=LocalAssistantDismissNavigationRP160.current
    val guestExitRP150=LocalGuestExitRP150.current
    val manageProfile=LocalProfileManage.current
    val pairTv=LocalCastPair.current
    val current=when(screen) {
        Screen.WATCH_PARTY -> Screen.FRIENDS
        Screen.COLLECTIONS,Screen.FRANCHISE,Screen.CUSTOM_FILTER,Screen.GENRES,Screen.GENRE_DETAIL,Screen.YEAR_DETAIL,Screen.PLATFORM_DETAIL,Screen.COLLECTION_DETAIL -> Screen.HOME
        else -> screen
    }
    val owner=profile?.id ?: profileName
    val headerTarget=remember(current,owner) {FocusRequester()}
    val claim=remember(current,owner) {HeaderNavigationRP108.policy.claim(owner,current.name)}
    val navScroll=androidx.compose.foundation.lazy.rememberLazyListState()
    var navFocusedRP195 by remember { mutableStateOf<Screen?>(null) }
    var navOriginRP195 by remember { mutableFloatStateOf(0f) }
    val navBoundsRP195=remember { mutableStateMapOf<Screen,Pair<Float,Float>>() }
    // RP198: never fall back to Home when the user moves focus to another header control.
    // The last focused target remains selected when focus leaves the header.
    val highlightedRP195=navFocusedRP195 ?: current
    val targetRP195=navBoundsRP195[highlightedRP195]
    val iconTargetRP198=highlightedRP195==Screen.NOTIFICATIONS || highlightedRP195==Screen.SETTINGS
    val targetXRP195=(targetRP195?.first ?: navOriginRP195)-navOriginRP195
    val targetWRP195=if(iconTargetRP198) 0f else targetRP195?.second ?: 0f
    val pillXRP195=animateFloatAsState(targetXRP195,tween(175,easing=LinearOutSlowInEasing),label="navPillXRP195")
    val pillWRP195=animateFloatAsState(targetWRP195,tween(175,easing=LinearOutSlowInEasing),label="navPillWidthRP195")
    fun navigateHeader(destination:Screen) {dismissAssistantRP160();HeaderNavigationRP108.policy.navigate(owner,destination.name);onNavigate(destination)}
    val navItems=(if(kids) listOf(Screen.HOME to "Kids",Screen.SEARCH to tr("Buscar","Search"),Screen.MY_LIST to tr("Mi lista","My List")) else listOf(
        Screen.HOME to tr("Inicio","Home"),
        Screen.SEARCH to tr("Buscar","Search"),
        Screen.FRIENDS to tr("Amigos y salas","Friends & Party"),
        Screen.LIVE_TV to tr("En Vivo","Live"),
        Screen.MY_LIST to tr("Mi lista","My List")
    )).filter {guestExitRP150==null || it.first!=Screen.FRIENDS}
    val homeRestoringRP165=LocalHomeReturnFocus.current?.pending!=null
    LaunchedEffect(current,owner) {
        if(claim && !homeRestoringRP165) {
            val index=navItems.indexOfFirst {it.first==current}
            if(index>=0) navScroll.scrollToItem(index)
            withFrameNanos { }
            runCatching {headerTarget.requestFocus()}
        }
    }
    var headerHadFocus by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().height(66.dp).onPreviewKeyEvent { event ->
        if(event.key==Key.DirectionDown && onMoveDown!=null) {
            if(event.type==KeyEventType.KeyDown && event.nativeKeyEvent.repeatCount==0) onMoveDown()
            true
        } else false
    }.onFocusChanged {
        if(it.hasFocus && !headerHadFocus) onHeaderFocused?.invoke()
        headerHadFocus=it.hasFocus
    }.focusGroup()
        .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha=.8f),Color.Black.copy(alpha=.35f),Color.Transparent))).padding(horizontal=24.dp),
        verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
        Row(Modifier.width(116.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
        Image(painterResource(R.drawable.brunio_icon),"BruniO",Modifier.size(33.dp).clip(RoundedCornerShape(9.dp)))
        var profileFocused by remember { mutableStateOf(false) }
        var profileMenu by remember { mutableStateOf(false) }
        val profileAnchor=remember { FocusRequester() }
        Box {
        Row(Modifier.focusRequester(profileAnchor).onFocusChanged { profileFocused=it.isFocused }.tvClick { profileMenu=true }.clickable { profileMenu=true }
            .background(GlassActionBrushRP108(profileFocused),androidx.compose.foundation.shape.CircleShape)
            .border(1.dp,Accent.copy(alpha=if(profileFocused) 1f else .16f),androidx.compose.foundation.shape.CircleShape).padding(4.dp),
            verticalAlignment=Alignment.CenterVertically) {
            if(profile!=null) ProfileAvatar(profile,Modifier.size(34.dp).clip(androidx.compose.foundation.shape.CircleShape))
            else Box(Modifier.size(34.dp),contentAlignment=Alignment.Center) { Text(profileName.take(1),color=Color.White) }
        }
        if(profileMenu) androidx.compose.ui.window.Popup(
            alignment=Alignment.TopStart,offset=androidx.compose.ui.unit.IntOffset(0,with(LocalDensity.current) { 58.dp.roundToPx() }),
            onDismissRequest={ profileMenu=false;runCatching { profileAnchor.requestFocus() } },
            properties=androidx.compose.ui.window.PopupProperties(focusable=true)) {
            val menuFocus=remember { FocusRequester() }
            val firstProfileAction=when {
                !kids && manageProfile!=null -> "manage"
                !kids && pairTv!=null -> "pair"
                onEditPicture!=null -> "picture"
                else -> "switch"
            }
            fun profileActionModifier(action:String)=Modifier.fillMaxWidth().then(if(firstProfileAction==action) Modifier.focusRequester(menuFocus) else Modifier)
            LaunchedEffect(Unit) { withFrameNanos { };runCatching { menuFocus.requestFocus() } }
            Column(Modifier.width(290.dp).heightIn(max=420.dp).verticalScroll(rememberScrollState()).background(Color.Black,RoundedCornerShape(14.dp))
                .border(1.dp,PanelBorder,RoundedCornerShape(14.dp)).padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text(profileName,color=Muted,fontSize=13.sp,modifier=Modifier.padding(8.dp))
                if(!kids) manageProfile?.let {manage -> FocusButton(tr("Administrar perfil","Manage profile"),modifier=profileActionModifier("manage")) {profileMenu=false;manage()} }
                if(!kids) pairTv?.let {pair -> FocusButton(tr("Conectar teléfono / PC","Connect phone / PC"),modifier=profileActionModifier("pair")) {profileMenu=false;pair()} }
                onEditPicture?.let { edit -> FocusButton(tr("Editar foto","Edit picture"),modifier=profileActionModifier("picture")) { profileMenu=false;edit() } }
                FocusButton(tr("Cambiar perfil","Switch profile"),modifier=profileActionModifier("switch")) { profileMenu=false;navigateHeader(Screen.PROFILES) }
            }
        }
        }
        } // symmetric profile slot
        // RP196: paint the sliding indicator within the existing 52dp nav slot.
        // Draw phase animation cannot change measurement, parent height or Home layout.
        Box(Modifier.weight(1f).height(52.dp)
            .onGloballyPositioned {navOriginRP195=it.positionInRoot().x}
            .drawBehind {
                val x=pillXRP195.value
                val w=pillWRP195.value
                if(w>1f) drawRoundRect(
                    color=Color(0xFFF3F3F3),
                    topLeft=Offset(x,(size.height-38.dp.toPx())/2f),
                    size=Size(w,38.dp.toPx()),
                    cornerRadius=androidx.compose.ui.geometry.CornerRadius(19.dp.toPx())
                )
            }) {
            androidx.compose.foundation.lazy.LazyRow(Modifier.fillMaxSize(),
                state=navScroll,verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(7.dp,Alignment.CenterHorizontally),
                contentPadding=PaddingValues(horizontal=4.dp,vertical=6.dp)) {
                items(navItems,key={it.first}) { (destination,label) ->
                    val activate={if(destination==Screen.LIVE_TV && current==destination && onMoveDown!=null) {dismissAssistantRP160();onMoveDown()} else navigateHeader(destination)}
                    val loc=Modifier.onGloballyPositioned {
                        navBoundsRP195[destination]=it.positionInRoot().x to it.size.width.toFloat()
                    }.then(if(current==destination) Modifier.focusRequester(headerTarget) else Modifier)
                    val focusChange:(Boolean)->Unit={ focused ->
                        if(focused) navFocusedRP195=destination
                    }
                    if(destination==Screen.SEARCH) SearchNavigationButton(
                        onFocused=focusChange,selected=highlightedRP195==destination,modifier=loc,onClick=activate,sharedHighlightRP195=true)
                    else NavigationTab(
                        label,selected=highlightedRP195==destination,onFocused=focusChange,modifier=loc,onClick=activate,sharedHighlightRP195=true)
                }
                if(guestExitRP150!=null) item(key="guest-exit-rp150") {FocusButton(tr("Salir invitado","Exit guest"),onClick=guestExitRP150)}
            }
        }
        Row(Modifier.width(if(assistantOpenRP150!=null) 164.dp else 120.dp),horizontalArrangement=Arrangement.spacedBy(9.dp,Alignment.End),verticalAlignment=Alignment.CenterVertically) {
            val calendarContextRP120=androidx.compose.ui.platform.LocalContext.current.applicationContext
        val calendarOwnerRP120=if(profile==null) "" else com.miflix.native2.data.CalendarStoreRP120.owner(calendarContextRP120,profile.id)
        val calendarRevisionRP120 by com.miflix.native2.data.CalendarStoreRP120.revision.collectAsState()
        val sportUnreadRP120 by produceState(0,calendarOwnerRP120,calendarRevisionRP120) {
            value=kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {com.miflix.native2.data.CalendarStoreRP120.unread(calendarContextRP120,calendarOwnerRP120)}
        }
        if(guestExitRP150==null) NotificationButton(notificationCount+sportUnreadRP120,
            (if(current==Screen.NOTIFICATIONS) Modifier.focusRequester(headerTarget) else Modifier)
                .onGloballyPositioned {navBoundsRP195[Screen.NOTIFICATIONS]=it.positionInRoot().x to it.size.width.toFloat()},
            onFocused={if(it) navFocusedRP195=Screen.NOTIFICATIONS}) {navigateHeader(Screen.NOTIFICATIONS)}
            if(!kids && assistantOpenRP150!=null) AssistantOrbRP155(onClick=assistantOpenRP150)
            if(!kids && guestExitRP150==null) SettingsNavigationButtonRP106(current==Screen.SETTINGS,
                (if(current==Screen.SETTINGS) Modifier.focusRequester(headerTarget) else Modifier)
                    .onGloballyPositioned {navBoundsRP195[Screen.SETTINGS]=it.positionInRoot().x to it.size.width.toFloat()},
                onFocused={if(it) navFocusedRP195=Screen.SETTINGS}) {navigateHeader(Screen.SETTINGS)}
        }

    }
}

@Composable
private fun NotificationButton(count: Int,modifier:Modifier=Modifier,onFocused:(Boolean)->Unit={},onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val whiteFillRP198 by animateColorAsState(if(focused) Color(0xFFF5F5F5) else Color.Transparent,tween(120),label="alertFillRP198")
    val focusScaleRP194 by animateFloatAsState(if(focused) 1.055f else 1f,tween(110),label="alertNavScale")
    Box(modifier.size(40.dp).graphicsLayer { scaleX=focusScaleRP194;scaleY=focusScaleRP194 }.semantics { contentDescription="${tr("Avisos","Alerts")}: $count" }.onFocusChanged { focused=it.isFocused;onFocused(it.isFocused) }.tvClick(onClick).clickable(onClick=onClick)
        .background(GlassNavBrushRP106(false,false),RoundedCornerShape(50))
        .background(whiteFillRP198,RoundedCornerShape(50))
        .border(1.dp,Color.White.copy(alpha=if(focused) .9f else .10f),RoundedCornerShape(50)),contentAlignment=Alignment.Center) {
        BellIcon(if(focused) Color(0xFF151515) else Color.White,Modifier.size(25.dp))
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
private fun SearchNavigationButton(onFocused: (Boolean) -> Unit = {},selected:Boolean=false,modifier:Modifier=Modifier,sharedHighlightRP195:Boolean=false,onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val focusScaleRP194 by animateFloatAsState(if(focused) 1.055f else 1f,tween(110),label="searchNavScale")
    Box(modifier.size(40.dp).graphicsLayer { val scale=if(sharedHighlightRP195) 1f else focusScaleRP194;scaleX=scale;scaleY=scale }.semantics { contentDescription=tr("Buscar","Search") }.onFocusChanged { focused=it.isFocused; onFocused(it.isFocused) }.tvClick(onClick).clickable(onClick=onClick)
        .background(if(sharedHighlightRP195) Brush.verticalGradient(listOf(Color.Transparent,Color.Transparent)) else GlassNavBrushRP106(focused,selected),RoundedCornerShape(50))
        .border(1.dp,if(sharedHighlightRP195) Color.Transparent else Accent.copy(alpha=if(focused) 1f else .10f),RoundedCornerShape(50)),contentAlignment=Alignment.Center) {
        Canvas(Modifier.size(24.dp)) {
            val ink=if(sharedHighlightRP195 && selected) Color(0xFF171717) else Color.White
            val center=androidx.compose.ui.geometry.Offset(size.width*.4f,size.height*.4f)
            drawCircle(ink,size.width*.29f,center,style=androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
            drawLine(ink,androidx.compose.ui.geometry.Offset(size.width*.62f,size.height*.62f),
                androidx.compose.ui.geometry.Offset(size.width*.94f,size.height*.94f),2.dp.toPx(),androidx.compose.ui.graphics.StrokeCap.Round)
        }
    }
}

@Composable
internal fun NavigationTab(text: String,selected: Boolean,onFocused: (Boolean) -> Unit = {},modifier:Modifier=Modifier,sharedHighlightRP195:Boolean=false,onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val focusScaleRP194 by animateFloatAsState(if(focused) 1.04f else 1f,tween(115),label="tabNavScale")
    val borderRP194 by animateColorAsState(if(focused) Accent.copy(alpha=.78f) else Color.White.copy(alpha=if(selected) .17f else .055f),tween(115),label="tabNavBorder")
    val localFillRP198 by animateColorAsState(if(focused && !sharedHighlightRP195) Color(0xFFF5F5F5) else Color.Transparent,tween(120),label="subTabWhiteFillRP198")
    Box(modifier.height(38.dp).graphicsLayer { val scale=if(sharedHighlightRP195) 1f else focusScaleRP194;scaleX=scale;scaleY=scale }.onFocusChanged {focused=it.isFocused;onFocused(it.isFocused)}
        .tvClick(onClick).clickable(onClick=onClick)
        .background(if(sharedHighlightRP195) Brush.verticalGradient(listOf(Color.Transparent,Color.Transparent)) else GlassNavBrushRP106(false,selected),RoundedCornerShape(50))
        .background(localFillRP198,RoundedCornerShape(50))
        .border(1.dp,if(sharedHighlightRP195 || focused) Color.Transparent else borderRP194,RoundedCornerShape(50))
        .padding(horizontal=13.dp),contentAlignment=Alignment.Center) {
        Text(text,color=if((sharedHighlightRP195 && selected) || (focused && !sharedHighlightRP195)) Color(0xFF171717) else Color.White.copy(alpha=if(focused || selected) 1f else .83f),fontSize=14.sp,lineHeight=18.sp,fontWeight=FontWeight.Medium,maxLines=1)
    }

}

private fun GlassNavBrushRP106(focused:Boolean,selected:Boolean):Brush {
    val alpha=if(focused) .25f else if(selected) .16f else .02f
    return Brush.verticalGradient(listOf(Color.White.copy(alpha=alpha),Color.White.copy(alpha=alpha*.40f)))
}

@Composable
private fun SettingsNavigationButtonRP106(selected:Boolean,modifier:Modifier=Modifier,onFocused:(Boolean)->Unit={},onClick:()->Unit) {
    var focused by remember {mutableStateOf(false)}
    val whiteFillRP198 by animateColorAsState(if(focused) Color(0xFFF5F5F5) else Color.Transparent,tween(120),label="settingsFillRP198")
    val focusScaleRP194 by animateFloatAsState(if(focused) 1.055f else 1f,tween(110),label="settingsNavScale")
    Box(modifier.size(40.dp).graphicsLayer { scaleX=focusScaleRP194;scaleY=focusScaleRP194 }.semantics {contentDescription=tr("Ajustes","Settings")}
        .onFocusChanged {focused=it.isFocused;onFocused(it.isFocused)}.tvClick(onClick).clickable(onClick=onClick)
        .background(GlassNavBrushRP106(false,selected),RoundedCornerShape(50))
        .background(whiteFillRP198,RoundedCornerShape(50))
        .border(1.dp,Color.White.copy(alpha=if(focused) .9f else .10f),RoundedCornerShape(50)),contentAlignment=Alignment.Center) {
        Canvas(Modifier.size(24.dp)) {
            val center=androidx.compose.ui.geometry.Offset(size.width/2f,size.height/2f)
            val gear=androidx.compose.ui.graphics.Path()
            for(index in 0 until 32) {
                val angle=index*Math.PI/16-Math.PI/2
                val radius=size.minDimension*(if(index%4==1 || index%4==2) .44f else .34f)
                val x=center.x+kotlin.math.cos(angle).toFloat()*radius
                val y=center.y+kotlin.math.sin(angle).toFloat()*radius
                if(index==0) gear.moveTo(x,y) else gear.lineTo(x,y)
            }
            gear.close()
            val ink=if(focused) Color(0xFF151515) else Color.White
            drawPath(gear,ink,style=androidx.compose.ui.graphics.drawscope.Stroke(1.6.dp.toPx()))
            drawCircle(ink,size.minDimension*.13f,center,style=androidx.compose.ui.graphics.drawscope.Stroke(1.6.dp.toPx()))
        }
    }
}

/** A newly opened modal consumes the key-up/repeats that opened it. */
internal fun Modifier.consumeOpeningPress(): Modifier = composed {
    val gate=remember {com.miflix.native2.data.QuickReactionGestureGate()}
    onPreviewKeyEvent {event ->
        MenuActivity.touch()
        gate.consume(event.nativeKeyEvent.keyCode,event.type==KeyEventType.KeyDown,event.type==KeyEventType.KeyUp,event.nativeKeyEvent.repeatCount)
    }
}
