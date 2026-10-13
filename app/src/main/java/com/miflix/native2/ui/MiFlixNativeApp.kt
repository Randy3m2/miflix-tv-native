package com.miflix.native2.ui

import androidx.compose.foundation.Image
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
        }
        lifecycle?.addObserver(observer)
        onDispose { lifecycle?.removeObserver(observer);state.appForeground=false }
    }
    LaunchedEffect(state.session?.refreshToken) { state.sessionNeedsSignIn=false }


    val localizedContext=remember(baseContext,state.interfaceLanguage) {
        val config=android.content.res.Configuration(context.resources.configuration)
        config.setLocale(java.util.Locale.forLanguageTag(state.interfaceLanguage))
        android.view.ContextThemeWrapper(baseContext,0).apply { applyOverrideConfiguration(config) }
    }
    CompositionLocalProvider(LocalContext provides localizedContext) {
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
        LaunchedEffect(bootstrapped, initialPartyCode) {
            if (bootstrapped && !initialPartyCode.isNullOrBlank()) {
                state.launch { state.joinWatchParty(initialPartyCode) }
            }
        }
        LaunchedEffect(state.screen) { if(state.screen==Screen.PLAYER) state.miniPlayer=false }
        LaunchedEffect(state.activeProfile.id,state.session?.userId) {
            if(state.miniPlayer) { state.miniPlayer=false;state.playerRequest=null }
        }
        Box(Modifier.fillMaxSize()) {
        key(state.screen) {
        when (state.screen) {
            Screen.SPLASH -> BruniOSplashScreen()
            Screen.HOME -> HomeScreen(state) { state.screen = it }
            Screen.FRIENDS -> FriendsScreen(state)
            Screen.TRAKT -> TraktScreen(state)
            Screen.NOTIFICATIONS -> NotificationsScreen(state)
            Screen.LIVE_TV -> LiveScreen(state)
            Screen.SEARCH -> SearchScreen(state) { state.screen = Screen.HOME }
            Screen.MOVIES -> CatalogScreen(state, tr("Películas","Movies"), state.movies, onBack = { state.screen = Screen.HOME })
            Screen.SERIES -> CatalogScreen(state, tr("Series","Series"), state.series, onBack = { state.screen = Screen.HOME })
            Screen.FRANCHISE -> FranchiseScreen(state)
            Screen.CUSTOM_FILTER -> CustomFilterScreen(state)
            Screen.COLLECTIONS -> CollectionsScreen(state) { state.screen = Screen.HOME }
            Screen.COLLECTION_DETAIL -> CatalogScreen(state, state.collectionTitle, state.collectionItems, onBack = { state.screen = Screen.COLLECTIONS })
            Screen.PLATFORM_DETAIL -> PlatformScreen(state) { state.screen = Screen.COLLECTIONS }
            Screen.GENRES -> CollectionsScreen(state) { state.screen = Screen.HOME }
            Screen.GENRE_DETAIL -> CatalogScreen(state, state.genreTitle, state.genreItems, onBack = { state.screen = Screen.COLLECTIONS })
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
                    PlayerScreen(state,req) { state.miniPlayer=false;state.screen=if(req.live) Screen.LIVE_TV else Screen.DETAILS }
                }
            }
        }
        }
        LaunchedEffect(bootstrapped,state.session?.userId,state.activeProfile.id) {
            if(bootstrapped) runCatching { state.loadRatings() }.onFailure { state.error=it.message }
        }
        LaunchedEffect(bootstrapped, state.session?.userId, state.activeProfile.id, state.favorites.toList()) {
            if(bootstrapped) while(true) {
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
        LaunchedEffect(bootstrapped,state.session?.userId,state.appForeground,
            state.activeProfile.avatarValue,state.playerRequest?.item?.cloudId,state.playerRequest?.season,
            state.playerRequest?.episode,state.screen,state.watchParty?.roomCode,state.pendingPartyCode) {
            if(bootstrapped && state.session!=null && state.appForeground) while(true) {
                if(!state.sessionNeedsSignIn) {
                    try { state.refreshSocialActivity() }
                    catch(e: kotlinx.coroutines.CancellationException) { throw e }
                    catch(_: Exception) { }
                }
                delay(5000) // Local check; presence writes are throttled to 60 seconds.
            }
        }
        LaunchedEffect(state.session?.userId, state.watchParty?.roomCode,state.appForeground) {
            if (state.watchParty != null && state.appForeground) {
                var ticks = 0
                while (state.watchParty != null && state.appForeground) {
                    if(!state.sessionNeedsSignIn) {
                        state.socialAction { state.pollSocialRoom() }
                        if (++ticks % 30 == 0) state.socialAction { state.heartbeatRoom() }
                    }
                    delay(2000)
                }
            }
        }
        state.sourceSelection?.let { SourcePickerOverlay(state, it) }

        state.busyMessage?.let { LoadingOverlay(it) }

        state.error?.takeIf { it.isNotBlank() && !it.contains("rememberCoroutineScope", ignoreCase = true) }?.let { message ->
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                Row(
                    Modifier.padding(bottom = 28.dp).background(Color(0xEE6E202A), RoundedCornerShape(14.dp)).padding(horizontal = 20.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(message, color = Color.White, fontSize = 13.sp, lineHeight=16.sp)
                    Spacer(Modifier.width(12.dp))
                    FocusButton(tr("Cerrar","Dismiss")) { state.error = null }
                }
            }
        }
        state.partyJoinedNotice?.let { notice ->
            Box(Modifier.fillMaxSize(),contentAlignment=Alignment.TopEnd) {
                Text(notice,color=Color.White,fontSize=18.sp,modifier=Modifier.padding(32.dp).background(Color(0xEE272727),RoundedCornerShape(16.dp)).padding(20.dp))
            }
        }
        AvatarUploadDialog(state)
        PartyRequestPopup(state)
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
    }
}
}

@Composable
private fun MyListScreen(state: AppState, onBack: () -> Unit) {
    val owner="${state.session?.userId}:${state.activeProfile.id}"
    val ids=state.favorites.toList()
    LaunchedEffect(owner,ids,state.myListLimit) { state.loadMyListPage(owner,ids) }
    CatalogScreen(state,tr("Mi lista","My List"),state.myListTitles,onBack,footer={
        Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
        if(state.myListLoading) Text(tr("Cargando…","Loading…"),color=Color.White)
        else {
            if(state.myListMessage.isNotBlank()) Text(state.myListMessage,color=Color.White)
            if(ids.size>state.myListLimit) FocusButton(tr("Cargar más","Load more")) { state.myListLimit+=20 }
            if(state.myListMessage.isNotBlank()) FocusButton(tr("Reintentar","Retry")) { state.launch { state.loadMyListPage(owner,ids) } }
        }
        }
    })
}

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
