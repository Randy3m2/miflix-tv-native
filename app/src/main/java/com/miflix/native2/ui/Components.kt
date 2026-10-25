package com.miflix.native2.ui

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
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

val Bg = Color(0xFF050505)
val Panel = Color(0xFF141414)
val Muted = Color(0xFFAAAAAF)
val SoftWhite = Color(0xFFF3F3F3)
internal object FocusTheme {
    var argb by mutableIntStateOf(0xFFB792FF.toInt())
}

val Accent: Color get() = Color(FocusTheme.argb)
val FocusInk: Color get() = if(Accent.luminance()>.179f) Color.Black else Color.White
val FocusedPanel = Color(0xFF242126)
val PanelBorder = Color(0xFF303035)
internal typealias ProfileMenuAction = () -> Unit
internal val LocalProfileManage=staticCompositionLocalOf<ProfileMenuAction?> {null}
internal val LocalCastPair=staticCompositionLocalOf<ProfileMenuAction?> {null}
val BrowseTileWidth = 240.dp
val BrowseTileHeight = 136.dp
val BrowseTileSlotWidth = 254.dp
val BrowseTileSlotHeight = 152.dp

private val FastMotion = tween<Float>(durationMillis = 80, easing = LinearOutSlowInEasing)

fun Modifier.tvClick(onClick: () -> Unit): Modifier = this.onPreviewKeyEvent { event ->
    val activate = event.key == Key.DirectionCenter ||
        event.key == Key.Enter ||
        event.key == Key.NumPadEnter
    MenuActivity.touch()
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
    Box(
        modifier
            
            .onFocusChanged {
                focused = it.isFocused
                onFocused?.invoke(it.isFocused)
            }
            .focusable()
            .then(if (onLongClick != null) Modifier.tvPlaybackClick(onClick, onLongClick) else Modifier.tvClick(onClick))
            .clickable(onClick = onClick)
            .background(GlassActionBrushRP108(actionFocused,primary,destructive), RoundedCornerShape(50))
            .border(if (actionFocused) 2.dp else 1.dp, if(actionFocused) Color.White else if(destructive) Color(0xFF78404B) else if(primary) Color(0xFF74608D) else PanelBorder, RoundedCornerShape(50))
            .padding(horizontal = horizontalPadding, vertical = if(LocalSimpleHome.current) 16.dp else 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = if(actionFocused) Color.White else if(destructive) Color(0xFFFFB9C3) else Color.White,
            fontSize = if(LocalSimpleHome.current) 20.sp else 15.sp, lineHeight=if(LocalSimpleHome.current) 25.sp else 20.sp,
            fontWeight = FontWeight.Bold, maxLines=1, overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}

@Composable
fun BackIconButton(onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val actionFocused=actionFocusVisibleRP64(focused)
    Box(Modifier.size(44.dp).semantics { contentDescription=tr("Volver","Back") }.onFocusChanged { focused=it.isFocused }.focusable().tvClick(onClick)
        .clickable(onClick=onClick).background(GlassActionBrushRP108(actionFocused),RoundedCornerShape(22.dp))
        .border(if(actionFocused) 2.dp else 1.dp,if(actionFocused) Color.White else Color(0x557D8AA3),RoundedCornerShape(22.dp)),
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
                .focusable()
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
                    .focusable()
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
    Box(modifier.onFocusChanged { focused=it.isFocused }.focusable().tvClick { editing=true }
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
    fun navigateHeader(destination:Screen) {HeaderNavigationRP108.policy.navigate(owner,destination.name);onNavigate(destination)}
    val navItems=(if(kids) listOf(Screen.HOME to "Kids",Screen.SEARCH to tr("Buscar","Search"),Screen.MY_LIST to tr("Mi lista","My List")) else listOf(
        Screen.HOME to tr("Inicio","Home"),
        Screen.SEARCH to tr("Buscar","Search"),
        Screen.FRIENDS to tr("Amigos y salas","Friends & Party"),
        Screen.LIVE_TV to tr("En Vivo","Live"),
        Screen.MY_LIST to tr("Mi lista","My List")
    ))
    LaunchedEffect(current,owner) {
        if(claim) {
            val index=navItems.indexOfFirst {it.first==current}
            if(index>=0) navScroll.scrollToItem(index)
            withFrameNanos { }
            runCatching {headerTarget.requestFocus()}
        }
    }
    var headerHadFocus by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().height(76.dp).onPreviewKeyEvent { event ->
        if(event.key==Key.DirectionDown && onMoveDown!=null) {
            if(event.type==KeyEventType.KeyDown && event.nativeKeyEvent.repeatCount==0) onMoveDown()
            true
        } else false
    }.onFocusChanged {
        if(it.hasFocus && !headerHadFocus) onHeaderFocused?.invoke()
        headerHadFocus=it.hasFocus
    }.focusGroup()
        .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha=.8f),Color.Black.copy(alpha=.35f),Color.Transparent))).padding(horizontal=24.dp),
        verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(18.dp)) {
        Row(Modifier.width(136.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)) {
        Image(painterResource(R.drawable.brunio_icon),"BruniO",Modifier.size(36.dp).clip(RoundedCornerShape(9.dp)))
        var profileFocused by remember { mutableStateOf(false) }
        var profileMenu by remember { mutableStateOf(false) }
        val profileAnchor=remember { FocusRequester() }
        Box {
        Row(Modifier.focusRequester(profileAnchor).onFocusChanged { profileFocused=it.isFocused }.focusable()
            .tvClick { profileMenu=true }.clickable { profileMenu=true }
            .background(GlassActionBrushRP108(profileFocused),androidx.compose.foundation.shape.CircleShape)
            .border(1.dp,Color.White.copy(alpha=if(profileFocused) .45f else .16f),androidx.compose.foundation.shape.CircleShape).padding(4.dp),
            verticalAlignment=Alignment.CenterVertically) {
            if(profile!=null) ProfileAvatar(profile,Modifier.size(34.dp).clip(androidx.compose.foundation.shape.CircleShape))
            else Box(Modifier.size(34.dp),contentAlignment=Alignment.Center) { Text(profileName.take(1),color=Color.White) }
        }
        if(profileMenu) androidx.compose.ui.window.Popup(
            alignment=Alignment.TopStart,offset=androidx.compose.ui.unit.IntOffset(0,with(LocalDensity.current) { 58.dp.roundToPx() }),
            onDismissRequest={ profileMenu=false;runCatching { profileAnchor.requestFocus() } },
            properties=androidx.compose.ui.window.PopupProperties(focusable=true)) {
            val menuFocus=remember { FocusRequester() }
            LaunchedEffect(Unit) { withFrameNanos { };runCatching { menuFocus.requestFocus() } }
            Column(Modifier.width(290.dp).heightIn(max=420.dp).verticalScroll(rememberScrollState()).background(Color.Black,RoundedCornerShape(14.dp))
                .border(1.dp,PanelBorder,RoundedCornerShape(14.dp)).padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text(profileName,color=Muted,fontSize=13.sp,modifier=Modifier.padding(8.dp))
                                if(!kids) FocusButton(tr("Ajustes","Settings"),modifier=Modifier.fillMaxWidth().focusRequester(menuFocus)) { profileMenu=false;navigateHeader(Screen.SETTINGS) }
                if(!kids) manageProfile?.let {manage -> FocusButton(tr("Administrar perfil","Manage profile"),modifier=Modifier.fillMaxWidth()) {profileMenu=false;manage()} }
                if(!kids) pairTv?.let {pair -> FocusButton(tr("Conectar teléfono / PC","Connect phone / PC"),modifier=Modifier.fillMaxWidth()) {profileMenu=false;pair()} }
                onEditPicture?.let { edit -> FocusButton(tr("Editar foto","Edit picture"),modifier=Modifier.fillMaxWidth()) { profileMenu=false;edit() } }
                FocusButton(tr("Cambiar perfil","Switch profile"),modifier=Modifier.fillMaxWidth().then(if(kids) Modifier.focusRequester(menuFocus) else Modifier)) { profileMenu=false;navigateHeader(Screen.PROFILES) }
            }
        }
        }
        } // symmetric profile slot
        Box(Modifier.weight(1f).height(60.dp)) {
            androidx.compose.foundation.lazy.LazyRow(Modifier.fillMaxSize(),
                state=navScroll,verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp,Alignment.CenterHorizontally),
                contentPadding=PaddingValues(horizontal=4.dp,vertical=6.dp)) {
                items(navItems,key={it.first}) { (destination,label) ->
                    val activate={if(destination==Screen.LIVE_TV && current==destination && onMoveDown!=null) onMoveDown() else navigateHeader(destination)}
                    if(destination==Screen.SEARCH) SearchNavigationButton(selected=current==destination,modifier=if(current==destination) Modifier.focusRequester(headerTarget) else Modifier,onClick=activate)
                    else NavigationTab(label,selected=current==destination,modifier=if(current==destination) Modifier.focusRequester(headerTarget) else Modifier,onClick=activate)
                }
            }
        }
        Row(Modifier.width(136.dp),horizontalArrangement=Arrangement.spacedBy(10.dp,Alignment.End),verticalAlignment=Alignment.CenterVertically) {
            NotificationButton(notificationCount,if(current==Screen.NOTIFICATIONS) Modifier.focusRequester(headerTarget) else Modifier) {navigateHeader(Screen.NOTIFICATIONS)}
            if(!kids) SettingsNavigationButtonRP106(current==Screen.SETTINGS,if(current==Screen.SETTINGS) Modifier.focusRequester(headerTarget) else Modifier) {navigateHeader(Screen.SETTINGS)}
        }

    }
}

@Composable
private fun NotificationButton(count: Int,modifier:Modifier=Modifier,onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(modifier.size(46.dp).semantics { contentDescription="${tr("Avisos","Alerts")}: $count" }.onFocusChanged { focused=it.isFocused }.focusable().tvClick(onClick).clickable(onClick=onClick)
        .background(GlassNavBrushRP106(focused,false),RoundedCornerShape(50))
        .border(1.dp,Color.White.copy(alpha=if(focused) .38f else .10f),RoundedCornerShape(50)),contentAlignment=Alignment.Center) {
        BellIcon(Color.White,Modifier.size(25.dp))
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
private fun SearchNavigationButton(onFocused: (Boolean) -> Unit = {},selected:Boolean=false,modifier:Modifier=Modifier,onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(modifier.size(44.dp).semantics { contentDescription=tr("Buscar","Search") }.onFocusChanged { focused=it.isFocused; onFocused(it.isFocused) }.focusable().tvClick(onClick).clickable(onClick=onClick)
        .background(GlassNavBrushRP106(focused,selected),RoundedCornerShape(50)).border(1.dp,Color.White.copy(alpha=if(focused) .36f else .10f),RoundedCornerShape(50)),contentAlignment=Alignment.Center) {
        Canvas(Modifier.size(24.dp)) {
            val ink=Color.White
            val center=androidx.compose.ui.geometry.Offset(size.width*.4f,size.height*.4f)
            drawCircle(ink,size.width*.29f,center,style=androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
            drawLine(ink,androidx.compose.ui.geometry.Offset(size.width*.62f,size.height*.62f),
                androidx.compose.ui.geometry.Offset(size.width*.94f,size.height*.94f),2.dp.toPx(),androidx.compose.ui.graphics.StrokeCap.Round)
        }
    }
}

@Composable
internal fun NavigationTab(text: String,selected: Boolean,onFocused: (Boolean) -> Unit = {},modifier:Modifier=Modifier,onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(modifier.height(42.dp).onFocusChanged {focused=it.isFocused;onFocused(it.isFocused)}
        .focusable().tvClick(onClick).clickable(onClick=onClick)
        .background(GlassNavBrushRP106(focused,selected),RoundedCornerShape(50))
        .border(1.dp,Color.White.copy(alpha=if(focused) .38f else if(selected) .18f else .06f),RoundedCornerShape(50))
        .padding(horizontal=14.dp),contentAlignment=Alignment.Center) {
        Text(text,color=Color.White.copy(alpha=if(focused || selected) 1f else .76f),fontSize=14.sp,lineHeight=18.sp,fontWeight=if(focused || selected) FontWeight.Bold else FontWeight.Medium,maxLines=1)
    }

}

private fun GlassNavBrushRP106(focused:Boolean,selected:Boolean):Brush {
    val alpha=if(focused) .30f else if(selected) .19f else .045f
    return Brush.verticalGradient(listOf(Color.White.copy(alpha=alpha),Color.White.copy(alpha=alpha*.40f)))
}

@Composable
private fun SettingsNavigationButtonRP106(selected:Boolean,modifier:Modifier=Modifier,onClick:()->Unit) {
    var focused by remember {mutableStateOf(false)}
    Box(modifier.size(46.dp).semantics {contentDescription=tr("Ajustes","Settings")}
        .onFocusChanged {focused=it.isFocused}.focusable().tvClick(onClick).clickable(onClick=onClick)
        .background(GlassNavBrushRP106(focused,selected),RoundedCornerShape(50))
        .border(1.dp,Color.White.copy(alpha=if(focused) .38f else .10f),RoundedCornerShape(50)),contentAlignment=Alignment.Center) {
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
            drawPath(gear,Color.White,style=androidx.compose.ui.graphics.drawscope.Stroke(1.6.dp.toPx()))
            drawCircle(Color.White,size.minDimension*.13f,center,style=androidx.compose.ui.graphics.drawscope.Stroke(1.6.dp.toPx()))
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
