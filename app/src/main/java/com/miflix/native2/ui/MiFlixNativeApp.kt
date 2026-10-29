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
fun MiFlixNativeApp(initialPartyCode:String?=null,initialContentLinkRP140:String?=null,onContentConsumedRP140:()->Unit={}) {
    MiFlixNativeAppImplRP150(initialPartyCode,initialContentLinkRP140,onContentConsumedRP140)
}

@Composable
private fun MiFlixNativeAppImplRP150(initialPartyCode: String? = null,initialContentLinkRP140:String?=null,onContentConsumedRP140:()->Unit={},guestRP150:Boolean=false,exitGuestRP150:()->Unit={},guestProvidersRP150:List<String> = emptyList(),guestAudioRP150:String="",guestSubtitlesRP150:String="off") {
    val baseContext=LocalContext.current
    val context = baseContext.applicationContext
    val state = remember { AppState(context) }
    var showAssistantRP150 by remember {mutableStateOf(false)}
    var assistantSequenceRP156 by remember {mutableIntStateOf(0)}
    var assistantEntryRP156 by remember {mutableStateOf(false)}
    LaunchedEffect(state.screen,state.activeProfile.id,state.session?.userId,state.assistantEnabledRP155) {showAssistantRP150=false;assistantEntryRP156=false}
    var showGuestRP150 by remember {mutableStateOf(false)}
    var consumedPartyRP150 by remember {mutableStateOf<String?>(null)}
    if(showGuestRP150 && !guestRP150) {
        val guestContext=remember {com.miflix.native2.data.GuestContextRP150(baseContext,state.interfaceLanguage)}
        CompositionLocalProvider(LocalContext provides guestContext) {
            key(guestContext) {MiFlixNativeAppImplRP150(guestRP150=true,guestProvidersRP150=state.streamRepo.manifests(),guestAudioRP150=state.audioLanguage,guestSubtitlesRP150=state.subtitleLanguage,exitGuestRP150={UiLanguage.code=state.interfaceLanguage;showGuestRP150=false})}
        }
        return
    }
    DisposableEffect(state) {onDispose {if(guestRP150) state.disposeGuestRP150()}}

    val mobileRP133=remember { (baseContext.getSystemService(android.content.Context.UI_MODE_SERVICE) as? android.app.UiModeManager)?.currentModeType!=android.content.res.Configuration.UI_MODE_TYPE_TELEVISION }
    val systemDensityRP133=androidx.compose.ui.platform.LocalDensity.current
    LaunchedEffect(mobileRP133,state.localExtrasScope()) {if(mobileRP133) MobileUiPrefsRP133.load(context,state.localExtrasScope())}
    DisposableEffect(baseContext) {
        val lifecycle=generateSequence(baseContext as android.content.Context) {(it as? android.content.ContextWrapper)?.baseContext}.take(8).filterIsInstance<androidx.lifecycle.LifecycleOwner>().firstOrNull()?.lifecycle
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
        config.setLocale(java.util.Locale.forLanguageTag(AppLanguagesRP148.locale(state.interfaceLanguage)))
        android.view.ContextThemeWrapper(baseContext,0).apply { applyOverrideConfiguration(config) }
    }
    CompositionLocalProvider(LocalGuestOpenRP154 provides (if(guestRP150 || state.kidsMode) null else ({showAssistantRP150=false;showGuestRP150=true})),LocalAssistantOpenRP150 provides (if(guestRP150 || state.kidsMode || !state.assistantEnabledRP155) null else ({assistantEntryRP156=true})),LocalGuestExitRP150 provides (if(guestRP150) exitGuestRP150 else null),LocalMobileBrunioRP133 provides mobileRP133,androidx.compose.ui.platform.LocalDensity provides (if(mobileRP133) androidx.compose.ui.unit.Density(systemDensityRP133.density,systemDensityRP133.fontScale*MobileUiPrefsRP133.scale) else systemDensityRP133),LocalCardFreshnessLoad provides {item: com.miflix.native2.model.MediaSummary -> state.requestCardFreshness(item)},LocalCardFreshness provides {item: com.miflix.native2.model.MediaSummary -> state.cardFreshness(item)},LocalPlayerMenuSide provides (state.screen==Screen.PLAYER || state.screen==Screen.MULTIVIEW),LocalContext provides localizedContext,LocalProfileManage provides (if(guestRP150) null else ({state.socialProfileEditor=true})),LocalCastPair provides (if(guestRP150) null else ({state.localCastPairing=true})),LocalTitleActions provides {item: com.miflix.native2.model.MediaSummary -> state.titleActions(item)}) {
    val originalDensity=androidx.compose.ui.platform.LocalDensity.current
    CompositionLocalProvider(LocalSimpleHome provides state.easyModeRP79,androidx.compose.ui.platform.LocalDensity provides
        androidx.compose.ui.unit.Density(originalDensity.density,if(state.easyModeRP79) maxOf(originalDensity.fontScale,1.2f) else originalDensity.fontScale)) {
    MaterialTheme {
        var bootstrapped by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            val animation = launch { delay(2400) }
            state.bootstrap()
            if(guestRP150) {
                if(guestProvidersRP150.isNotEmpty()) state.streamRepo.setManifests(guestProvidersRP150)
                state.savePlaybackPreferences(guestAudioRP150,guestSubtitlesRP150,true)
            }
            animation.join()
            bootstrapped = true
        }
        if (!bootstrapped) {
            BruniOSplashScreen()
            return@MaterialTheme
        }
        if(guestRP150) androidx.activity.compose.BackHandler(enabled=state.screen==Screen.HOME) {exitGuestRP150()}
        MobilePlayerEnvironmentRP133(mobileRP133 && state.screen in listOf(Screen.PLAYER,Screen.MULTIVIEW))
        val calendarRevisionRP134 by com.miflix.native2.data.CalendarStoreRP120.revision.collectAsState()
        val calendarOwnerRP134="${state.session?.userId ?: "local"}:${state.activeProfile.id}"
        val calendarSubscriptionsRP134=remember(calendarOwnerRP134,calendarRevisionRP134) {com.miflix.native2.data.CalendarStoreRP120.subscriptions(context,calendarOwnerRP134)}
        LaunchedEffect(state.session?.userId,state.activeProfile.id,state.appForeground,calendarSubscriptionsRP134) {
            if(state.appForeground) try {state.syncCalendarSubscriptionsRP134()} catch(e:kotlinx.coroutines.CancellationException) {throw e} catch(_:Exception) { }
        }
        CalendarRuntimeRP120("${state.session?.userId ?: "local"}:${state.activeProfile.id}",state.activeProfile.name,state.appForeground)
        LaunchedEffect(bootstrapped, initialPartyCode,state.easyModeRP79) {
            if (bootstrapped && !state.easyModeRP79 && !initialPartyCode.isNullOrBlank() && consumedPartyRP150!=initialPartyCode) {
                consumedPartyRP150=initialPartyCode
                state.launch { state.joinWatchParty(initialPartyCode) }
            }
        }
        val contentReadyRP140=mobileRP133 && state.screen !in listOf(Screen.SPLASH,Screen.PROFILES,Screen.SETTINGS)
        LaunchedEffect(initialContentLinkRP140,contentReadyRP140) {
            if(contentReadyRP140) com.miflix.native2.data.MobileShareRP140.cloudId(initialContentLinkRP140)?.let {id->
                try {state.playerRequest=null;state.openRecommendation(id)}
                catch(e:kotlinx.coroutines.CancellationException) {throw e}
                catch(e:Exception) {state.error=e.message ?: tr("No se pudo abrir el contenido compartido.","Unable to open shared content.")}
                onContentConsumedRP140()
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
        navigationState.SaveableStateProvider("${state.session?.userId}:${state.activeProfile.id}:${if(showAssistantRP150 && !guestRP150 && !mobileRP133 && !state.kidsMode) "ASSISTANT:$assistantSequenceRP156" else state.screen.name}") {
        val destination=if(guestRP150 && state.screen in setOf(Screen.SETTINGS,Screen.PROFILES,Screen.FRIENDS,Screen.WATCH_PARTY,Screen.NOTIFICATIONS,Screen.DOWNLOADS,Screen.TRAKT)) Screen.HOME else if(state.easyModeRP79 && !(mobileRP133 && state.screen==Screen.DOWNLOADS) && !com.miflix.native2.data.EasyModePolicyRP79.allowsScreen(state.screen.name)) Screen.HOME else if(state.kidsMode && state.screen !in setOf(Screen.HOME,Screen.SEARCH,Screen.MY_LIST,Screen.DETAILS,Screen.PROFILES,Screen.NOTIFICATIONS,Screen.PLAYER)) Screen.HOME else state.screen
        LaunchedEffect(destination,state.screen) {if(destination!=state.screen) state.screen=destination}
        if(showAssistantRP150 && !guestRP150 && !mobileRP133 && !state.kidsMode) CinematicSearchScreen(state,{showAssistantRP150=false},initialAi=true)
        else if(mobileRP133) MobileAppRP133(state,destination)
        else if(state.easyModeRP79 && destination!=Screen.PLAYER) EasyScreenRP79(state,destination)
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
            Screen.DOWNLOADS -> Unit
            Screen.MULTIVIEW -> MultiViewScreen(state)
            Screen.PLAYER -> if(state.playerRequest==null) state.screen=Screen.HOME

        }

        }
        state.playerRequest?.takeIf { state.screen==Screen.PLAYER || state.miniPlayer }?.let { req ->
            key(req.playbackId) {
                Box(if(state.miniPlayer && state.screen!=Screen.PLAYER) Modifier.align(Alignment.BottomEnd).padding(20.dp).width(320.dp).height(220.dp) else Modifier.fillMaxSize()) {
                    CompositionLocalProvider(LocalPlayerMenuSide provides true) {
                    Box(Modifier.fillMaxSize().then(if(mobileRP133) Modifier.safeDrawingPadding() else Modifier)) {

                    PlayerScreen(state,req) { state.miniPlayer=false;state.screen=req.returnScreen ?: if(req.live) Screen.LIVE_TV else Screen.DETAILS }
                    }
                    }
                }
            }
        }
        if(!state.easyModeRP79) LiveScoreOverlayRP11(state)
        }
        LaunchedEffect(bootstrapped,state.session?.userId,state.activeProfile.id,state.easyModeRP79) {
            if(bootstrapped && !state.easyModeRP79) runCatching { state.loadRatings() }.onFailure { state.error=it.message }
        }
        if(assistantEntryRP156 && state.assistantEnabledRP155 && !guestRP150 && !state.kidsMode) AssistantEntryRP156(state,{assistantEntryRP156=false}) {request ->
            assistantEntryRP156=false
            if(!AssistantDirectRP156.run(state,request)) {
                navigationState.removeState("${state.session?.userId}:${state.activeProfile.id}:ASSISTANT:$assistantSequenceRP156");assistantSequenceRP156++;state.assistantQueryRP156=request;state.searchQuery=request;showAssistantRP150=true
            }
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
        if(!mobileRP133) MenuScreensaver(state)
        if(!state.easyModeRP79 && state.socialProfileEditor) SocialSettingsDialog(state) {state.socialProfileEditor=false}
        if(!state.easyModeRP79) LocalCastHost(state)
        state.contextTitle?.takeUnless {state.easyModeRP79}?.let {title -> if(mobileRP133) MobileTitleActionsRP133(state,title,state.contextContinue) {state.contextTitle=null} else TitleActionsDialog(state,title,state.contextContinue) {state.contextTitle=null} }
        state.sourceSelection?.takeUnless {state.easyModeRP79}?.let { if(mobileRP133) MobileSourcesRP133(state,it) else SourcePickerOverlay(state, it) }

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
        var heldErrorRP143 by remember {mutableStateOf<String?>(null)}
        val incomingErrorRP143=state.error
        LaunchedEffect(incomingErrorRP143) {if(!mobileRP133 && !incomingErrorRP143.isNullOrBlank()) heldErrorRP143=incomingErrorRP143}
        fun dismissErrorRP143(message:String) {if(heldErrorRP143==message) heldErrorRP143=null;if(state.error==message) state.error=null}
        (if(mobileRP133) state.error else heldErrorRP143)?.takeIf { it.isNotBlank() }?.let { message ->
            key(message) {
                LaunchedEffect(message) {
                    delay(if(mobileRP133) 5000 else com.miflix.native2.data.TvInputPolicyRP143.ERROR_VISIBLE_MS)
                    dismissErrorRP143(message)
                }
                Dialog(onDismissRequest={ dismissErrorRP143(message) },
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
                        Text(if(mobileRP133) tr("Se cierra automáticamente en 5 segundos.","Closes automatically after 5 seconds.") else tr("Se cierra automáticamente en 8 segundos.","Closes automatically after 8 seconds."),color=Color(0xFFDEBAC3),fontSize=12.sp)
                        FocusButton(tr("Cerrar","Dismiss"),modifier=Modifier.focusRequester(dismissFocus)) {
                            dismissErrorRP143(message)
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
