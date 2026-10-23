package com.miflix.native2.ui

import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.foundation.Image
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import com.miflix.native2.R
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MiFlixNativeApp(initialPartyCode: String? = null) {
    val baseContext=LocalContext.current
    val context = baseContext.applicationContext
    val state = remember { AppState(context) }
    DisposableEffect(baseContext) {
        val lifecycle=(baseContext as? androidx.lifecycle.LifecycleOwner)?.lifecycle
        state.appForeground=lifecycle?.currentState?.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)==true
        val observer=androidx.lifecycle.LifecycleEventObserver { _, _ ->
            state.appForeground=lifecycle?.currentState?.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)==true
            state.publishSocialPresenceEvent()
            if(!state.appForeground) state.launch {state.flushSettingsRP83()}
        }
        lifecycle?.addObserver(observer)
        onDispose { lifecycle?.removeObserver(observer);state.appForeground=false;state.publishSocialPresenceEvent() }
    }
    LaunchedEffect(state.session?.refreshToken) { state.sessionNeedsSignIn=false }


    val localizedContext=remember(baseContext,state.interfaceLanguage) {
        val config=android.content.res.Configuration(context.resources.configuration)
        config.setLocale(java.util.Locale.forLanguageTag(if(state.interfaceLanguage=="es") "es-MX" else "en-US"))
        android.view.ContextThemeWrapper(baseContext,0).apply { applyOverrideConfiguration(config) }
    }
    CompositionLocalProvider(LocalCardFreshnessLoad provides {item: com.miflix.native2.model.MediaSummary -> state.requestCardFreshness(item)},LocalCardFreshness provides {item: com.miflix.native2.model.MediaSummary -> state.cardFreshness(item)},LocalPlayerMenuSide provides (state.screen==Screen.PLAYER || state.screen==Screen.MULTIVIEW),LocalContext provides localizedContext,LocalProfileManage provides {state.socialProfileEditor=true},LocalCastPair provides {state.localCastPairing=true},LocalTitleActions provides {item: com.miflix.native2.model.MediaSummary -> state.titleActions(item)}) {
    val originalDensity=androidx.compose.ui.platform.LocalDensity.current
    CompositionLocalProvider(LocalSimpleHome provides state.easyModeRP79,androidx.compose.ui.platform.LocalDensity provides
        androidx.compose.ui.unit.Density(originalDensity.density,if(state.easyModeRP79) maxOf(originalDensity.fontScale,1.2f) else originalDensity.fontScale)) {
    MaterialTheme {
        var bootstrapped by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            val animation = launch { delay(2400) }
            state.bootstrap()
            animation.join()
            bootstrapped = true
        }
        if (!bootstrapped) {
            BruniOSplashScreen()
            return@MaterialTheme
        }
        LaunchedEffect(bootstrapped, initialPartyCode,state.easyModeRP79) {
            if (bootstrapped && !state.easyModeRP79 && !initialPartyCode.isNullOrBlank()) {
                state.launch { state.joinWatchParty(initialPartyCode) }
            }
        }
        LaunchedEffect(state.screen) { if(state.screen==Screen.PLAYER) state.miniPlayer=false }
        LaunchedEffect(state.activeProfile.id,state.session?.userId) {
            if(state.miniPlayer) { state.miniPlayer=false;state.playerRequest=null }
        }
        var homeExitArmedAt by remember {mutableStateOf(0L)}
        LaunchedEffect(state.screen,state.appForeground) {
            if(state.screen!=Screen.HOME || !state.appForeground) homeExitArmedAt=0L
        }
        LaunchedEffect(homeExitArmedAt) {if(homeExitArmedAt!=0L) {delay(2200);homeExitArmedAt=0L}}
        // Registered before content: dialogs, popups and screensavers get Back first.
        androidx.activity.compose.BackHandler(enabled=state.screen==Screen.HOME) {
            val now=android.os.SystemClock.elapsedRealtime()
            if(homeExitArmedAt!=0L && now-homeExitArmedAt<=2200L) {
                homeExitArmedAt=0L
                baseContext.playbackActivity()?.finish()
            } else homeExitArmedAt=now
        }
        val scoreShortcut=remember(state.activeProfile.id,state.session?.userId,state.screen,state.appForeground) {ScorePinShortcutRP15()}
        val navigationState=androidx.compose.runtime.saveable.rememberSaveableStateHolder()
        Box(Modifier.fillMaxSize().onPreviewKeyEvent {
            MenuActivity.touch()
            if(it.nativeKeyEvent.action==android.view.KeyEvent.ACTION_DOWN && it.nativeKeyEvent.keyCode!=android.view.KeyEvent.KEYCODE_BACK) homeExitArmedAt=0L
            val key=it.nativeKeyEvent
            val shortcut=state.focusScorePinRP15
            if(shortcut==null) {scoreShortcut.reset();false}
            else if(key.action==android.view.KeyEvent.ACTION_DOWN &&
                scoreShortcut.press(key.keyCode==android.view.KeyEvent.KEYCODE_DPAD_UP,key.repeatCount,key.eventTime)) {
                shortcut();true
            } else false
        }.pointerInput(Unit) {awaitPointerEventScope {while(true) {awaitPointerEvent(PointerEventPass.Initial);MenuActivity.touch()} }}) {
        navigationState.SaveableStateProvider("${state.session?.userId}:${state.activeProfile.id}:${state.screen.name}") {
        val destination=if(state.easyModeRP79 && !com.miflix.native2.data.EasyModePolicyRP79.allowsScreen(state.screen.name)) Screen.HOME else if(state.kidsMode && state.screen !in setOf(Screen.HOME,Screen.SEARCH,Screen.MY_LIST,Screen.DETAILS,Screen.PROFILES,Screen.NOTIFICATIONS,Screen.PLAYER)) Screen.HOME else state.screen
        LaunchedEffect(destination,state.screen) {if(destination!=state.screen) state.screen=destination}
        if(state.easyModeRP79 && destination!=Screen.PLAYER) EasyScreenRP79(state,destination)
        else when (destination) {
            Screen.SPLASH -> BruniOSplashScreen()
            Screen.HOME -> if(state.kidsMode) KidsHomeScreen(state) else HomeScreen(state) { state.screen = it }
            Screen.FRIENDS -> FriendsScreen(state)
            Screen.TRAKT -> TraktScreen(state)
            Screen.NOTIFICATIONS -> NotificationsScreen(state)
            Screen.LIVE_TV -> LiveScreen(state)
            Screen.LIDOM -> LidomScreen(state)
            Screen.SEARCH -> SearchScreen(state) { state.screen = Screen.HOME }
            Screen.MOVIES -> CatalogScreen(state, tr("Películas","Movies"), state.movies, onBack = { state.screen = Screen.HOME })
            Screen.SERIES -> CatalogScreen(state, tr("Series","Series"), state.series, onBack = { state.screen = Screen.HOME })
            Screen.FRANCHISE -> FranchiseScreen(state)
            Screen.CUSTOM_FILTER -> CustomFilterScreen(state)
            Screen.COLLECTIONS -> CollectionsScreen(state) { state.screen = Screen.HOME }
            Screen.OSCARS -> OscarsScreen(state)
            Screen.COLLECTION_DETAIL -> CatalogScreen(state, state.collectionTitle, state.collectionItems, onBack = { state.screen = Screen.COLLECTIONS })
            Screen.PLATFORM_DETAIL -> PlatformScreen(state) { state.screen = Screen.COLLECTIONS }
            Screen.GENRES -> CollectionsScreen(state) { state.screen = Screen.HOME }
            Screen.GENRE_DETAIL -> CatalogScreen(state,state.genreTitle,state.genreItems,onBack={state.screen=state.genreReturnScreen},footer={
                Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
                    if(state.genreMessage.isNotBlank()) Text(state.genreMessage,color=Muted)
                    if(state.genreLoading || state.genreHasMore) FeatureActionCard(if(state.genreLoading) tr("Cargando…","Loading…") else tr("Cargar más","Load more"),tr("25 títulos más","25 more titles"),modifier=Modifier.widthIn(max=400.dp).fillMaxWidth()) {
                        if(!state.genreLoading) state.launch {state.loadMoreGenre()}
                    } else Text(tr("Llegaste al final de esta selección.","You reached the end of this selection."),color=Muted)
                }
            })
            Screen.DIRECTOR_DETAIL -> CatalogScreen(state,state.directorTitle,state.directorItems, onBack = { state.screen=Screen.HOME })
            Screen.YEAR_DETAIL -> CatalogScreen(state, state.yearTitle, state.yearItems, onBack = { state.screen = Screen.COLLECTIONS })
            Screen.MY_LIST -> MyListScreen(state) { state.screen = Screen.HOME }
            Screen.SETTINGS -> SettingsScreen(state) { state.screen = Screen.HOME }
            Screen.PROFILES -> ProfilesScreen(state) { state.screen = Screen.HOME }
            Screen.WATCH_PARTY -> WatchPartyScreen(state) { state.screen = Screen.FRIENDS }
            Screen.PAIR_DEVICE -> PairDeviceScreen(state) { state.screen = Screen.SETTINGS }
            Screen.DETAILS -> DetailsScreen(state) { state.screen = state.detailsReturnScreen }
            Screen.MULTIVIEW -> MultiViewScreen(state)
            Screen.PLAYER -> if(state.playerRequest==null) state.screen=Screen.HOME

        }

        }
        state.playerRequest?.takeIf { state.screen==Screen.PLAYER || state.miniPlayer }?.let { req ->
            key(req.playbackId) {
                Box(if(state.miniPlayer && state.screen!=Screen.PLAYER) Modifier.align(Alignment.BottomEnd).padding(20.dp).width(320.dp).height(220.dp) else Modifier.fillMaxSize()) {
                    CompositionLocalProvider(LocalPlayerMenuSide provides true) {
                    PlayerScreen(state,req) { state.miniPlayer=false;state.screen=req.returnScreen ?: if(req.live) Screen.LIVE_TV else Screen.DETAILS }
                    }
                }
            }
        }
        if(!state.easyModeRP79) LiveScoreOverlayRP11(state)
        }
        LaunchedEffect(bootstrapped,state.session?.userId,state.activeProfile.id,state.easyModeRP79) {
            if(bootstrapped && !state.easyModeRP79) runCatching { state.loadRatings() }.onFailure { state.error=it.message }
        }
        LaunchedEffect(bootstrapped, state.session?.userId, state.activeProfile.id, state.favorites.toList(),state.easyModeRP79) {
            if(bootstrapped && !state.easyModeRP79) while(true) {
                runCatching { state.checkNotifications() }
                delay(30*60*1000L)
            }
        }
        LaunchedEffect(state.session?.userId,state.appForeground) {
            if(state.session!=null && state.appForeground) while(true) {
                delay(30*60*1000L)
                if(!state.sessionNeedsSignIn) state.refreshAccountSession()
            }
        }
        // Event presence has no polling loop. App background/exit records a durable stop.
        LaunchedEffect(bootstrapped,state.session?.userId,state.appForeground,state.screen,state.miniPlayer,
            state.playerRequest?.playbackId,state.multiViewRequests.firstOrNull()?.playbackId,state.socialReadyPlaybackId,state.socialEndedPlaybackId,
            state.watchParty?.roomCode,state.easyModeRP79) {
            if(bootstrapped && !state.easyModeRP79 && !state.sessionNeedsSignIn) state.publishSocialPresenceEvent()
        }
        if(!state.easyModeRP79) PresenceNetworkRecovery(state)
        LaunchedEffect(bootstrapped,state.session?.userId,state.appForeground,
            state.activeProfile.avatarValue,state.pendingPartyCode,state.easyModeRP79) {
            if(bootstrapped && !state.easyModeRP79 && state.session!=null && state.appForeground) while(true) {
                if(!state.sessionNeedsSignIn) {
                    try { state.refreshSocialActivity() }
                    catch(e: kotlinx.coroutines.CancellationException) { throw e }
                    catch(_: Exception) { }
                }
                // Avatar gate / pending admission only. Never publishes social presence.
                delay(5000)
            }
        }
        LaunchedEffect(state.session?.userId, state.watchParty?.roomCode,state.appForeground,state.easyModeRP79) {
            if (!state.easyModeRP79 && state.watchParty != null && state.appForeground) {
                var ticks = 0
                while (state.watchParty != null && state.appForeground) {
                    if(!state.sessionNeedsSignIn) {
                        val watching=state.screen==Screen.PLAYER || state.miniPlayer
                        state.socialAction { state.pollSocialRoom(refreshMembers=!watching || ticks % 8 == 0) }
                        if (++ticks % 120 == 0) state.socialAction { state.heartbeatRoom() }
                    }
                    delay(if(state.screen==Screen.PLAYER || state.miniPlayer) 500L else 2000L)
                }
            }
        }
        if(homeExitArmedAt!=0L && state.screen==Screen.HOME) Box(Modifier.fillMaxSize(),contentAlignment=Alignment.BottomCenter) {
            Text(tr("Pulsa Atrás otra vez para salir","Press Back again to exit"),color=Color.White,fontSize=16.sp,
                modifier=Modifier.padding(bottom=32.dp).background(Color(0xE6222222),RoundedCornerShape(16.dp)).padding(horizontal=24.dp,vertical=14.dp))
        }
        if(!state.easyModeRP79 && state.changelogOpen) ChangelogDialog(state) {state.changelogOpen=false}
        MenuScreensaver(state)
        if(!state.easyModeRP79 && state.socialProfileEditor) SocialSettingsDialog(state) {state.socialProfileEditor=false}
        if(!state.easyModeRP79) LocalCastHost(state)
        state.contextTitle?.takeUnless {state.easyModeRP79}?.let {title -> TitleActionsDialog(state,title,state.contextContinue) {state.contextTitle=null} }
        state.sourceSelection?.takeUnless {state.easyModeRP79}?.let { SourcePickerOverlay(state, it) }

        if(state.playbackLoading==null) state.busyMessage?.let {LoadingOverlay(it)}
        LaunchedEffect(state.error) {if(state.error!=null) state.abortPlaybackLoading()}
        state.playbackLoading?.let {PlaybackLoadingCover(it,onCancel={state.cancelPlaybackStart()})}

        state.partyJoinedNotice?.let { notice ->
            Box(Modifier.fillMaxSize(),contentAlignment=Alignment.TopEnd) {
                Text(notice,color=Color.White,fontSize=18.sp,modifier=Modifier.padding(32.dp).background(Color(0xEE272727),RoundedCornerShape(16.dp)).padding(20.dp))
            }
        }
        state.profilePhotoEditor?.let { profile ->
            AvatarPickerDialog(profile.avatarValue,choose={ avatar ->
                state.launch { state.socialAction { state.setProfileAvatar(profile,avatar);state.profilePhotoEditor=null } }
            },close={state.profilePhotoEditor=null},hidden=state.hiddenAvatars,remove=state::hideAvatar,restore=state::restoreAvatars,
                customUpload={ state.profilePhotoEditor=null;state.launch { state.socialAction { state.startAvatarUpload(profile) } } })
        }
        if(!state.easyModeRP79) {
            AvatarUploadDialog(state)
            PartyRequestPopup(state)
        }
        if(state.partyClosedNotice) {
            Dialog(onDismissRequest={ state.partyClosedNotice=false }) {
                val confirm=remember { FocusRequester() }
                LaunchedEffect(Unit) { withFrameNanos { };confirm.requestFocus() }
                Column(Modifier.width(480.dp).background(Color(0xFF252525),RoundedCornerShape(20.dp)).padding(28.dp),
                    verticalArrangement=Arrangement.spacedBy(22.dp)) {
                    Text(tr("El party fue cerrado","The party was closed"),color=Color.White,fontSize=24.sp)
                    FocusButton("OK",modifier=Modifier.focusRequester(confirm)) { state.partyClosedNotice=false }
                }
            }
        }
        state.error?.takeIf { it.isNotBlank() }?.let { message ->
            key(message) {
                LaunchedEffect(message) {
                    delay(5000)
                    if(state.error==message) state.error=null
                }
                Dialog(onDismissRequest={ if(state.error==message) state.error=null },
                    properties=androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth=false)) {
                    val dismissFocus=remember { FocusRequester() }
                    LaunchedEffect(Unit) { withFrameNanos { };runCatching { dismissFocus.requestFocus() } }
                    Column(Modifier.widthIn(max=620.dp).fillMaxWidth(.85f)
                        .background(Color(0xFF381B22),RoundedCornerShape(18.dp)).padding(24.dp),
                        verticalArrangement=Arrangement.spacedBy(16.dp)) {
                        Text(tr("No se pudo completar","Could not complete"),color=Color.White,fontSize=20.sp,fontWeight=FontWeight.Bold)
                        Box(Modifier.heightIn(max=220.dp).then(Modifier
                            .verticalScroll(androidx.compose.foundation.rememberScrollState()))) {
                            Text(message,color=Color.White,fontSize=15.sp,lineHeight=20.sp)
                        }
                        Text(tr("Se cierra automáticamente en 5 segundos.","Closes automatically after 5 seconds."),color=Color(0xFFDEBAC3),fontSize=12.sp)
                        FocusButton(tr("Cerrar","Dismiss"),modifier=Modifier.focusRequester(dismissFocus)) {
                            if(state.error==message) state.error=null
                        }
                    }
                }
            }
        }

    }
}
}

}

@Composable
private fun MyListScreen(state:AppState,onBack:()->Unit) = ModernMyListScreenRP63(state,onBack)

@Composable
private fun LoadingOverlay(text: String) {
    Box(Modifier.fillMaxSize().background(Color(0x55000000)), contentAlignment = Alignment.Center) {
        Text(
            text,
            color = Color.Black,
            fontSize = 17.sp, lineHeight=21.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.background(Color.White, RoundedCornerShape(18.dp)).padding(horizontal = 28.dp, vertical = 17.dp)
        )
    }
}
