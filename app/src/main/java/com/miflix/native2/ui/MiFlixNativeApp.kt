package com.miflix.native2.ui

import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import com.miflix.native2.R
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
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

@Composable
fun MiFlixNativeApp(initialPartyCode: String? = null) {
    val context = LocalContext.current.applicationContext
    val state = remember { AppState(context) }

    MaterialTheme {
        var bootstrapped by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            delay(420)
            state.bootstrap()
            bootstrapped = true
        }
        LaunchedEffect(bootstrapped, initialPartyCode) {
            if (bootstrapped && !initialPartyCode.isNullOrBlank()) {
                state.launch { state.joinWatchParty(initialPartyCode) }
            }
        }
        when (state.screen) {
            Screen.SPLASH -> SplashScreen()
            Screen.HOME -> HomeScreen(state) { state.screen = it }
            Screen.FRIENDS -> FriendsScreen(state)
            Screen.TRAKT -> TraktScreen(state)
            Screen.NOTIFICATIONS -> NotificationsScreen(state)
            Screen.LIVE_TV -> LiveScreen(state)
            Screen.SEARCH -> SearchScreen(state) { state.screen = Screen.HOME }
            Screen.MOVIES -> CatalogScreen(state, "Movies", state.movies) { state.screen = Screen.HOME }
            Screen.SERIES -> CatalogScreen(state, "Series", state.series) { state.screen = Screen.HOME }
            Screen.COLLECTIONS -> CollectionsScreen(state) { state.screen = Screen.HOME }
            Screen.COLLECTION_DETAIL -> CatalogScreen(state, state.collectionTitle, state.collectionItems) { state.screen = Screen.COLLECTIONS }
            Screen.PLATFORM_DETAIL -> PlatformScreen(state) { state.screen = Screen.COLLECTIONS }
            Screen.GENRES -> GenresScreen(state) { state.screen = Screen.HOME }
            Screen.GENRE_DETAIL -> CatalogScreen(state, state.genreTitle, state.genreItems) { state.screen = Screen.GENRES }
            Screen.YEAR_DETAIL -> CatalogScreen(state, state.yearTitle, state.yearItems) { state.screen = Screen.COLLECTIONS }
            Screen.MY_LIST -> MyListScreen(state) { state.screen = Screen.HOME }
            Screen.SETTINGS -> SettingsScreen(state) { state.screen = Screen.HOME }
            Screen.PROFILES -> ProfilesScreen(state) { state.screen = Screen.HOME }
            Screen.WATCH_PARTY -> WatchPartyScreen(state) { state.screen = if (state.selected != null) Screen.DETAILS else Screen.HOME }
            Screen.PAIR_DEVICE -> PairDeviceScreen(state) { state.screen = Screen.SETTINGS }
            Screen.DETAILS -> DetailsScreen(state) { state.screen = Screen.HOME }
            Screen.PLAYER -> state.playerRequest?.let { req -> PlayerScreen(state, req) { state.screen = if (req.live) Screen.LIVE_TV else Screen.DETAILS } }
                ?: run { state.screen = Screen.HOME }
        }

        LaunchedEffect(bootstrapped, state.session?.userId, state.activeProfile.id, state.favorites.toList()) {
            if(bootstrapped) while(true) {
                runCatching { state.checkNotifications() }
                delay(30*60*1000L)
            }
        }
        LaunchedEffect(state.session?.userId) {
            if(state.session!=null) while(true) {
                delay(30*60*1000L)
                state.refreshAccountSession()
            }
        }
        LaunchedEffect(state.session?.userId, state.watchParty?.roomCode) {
            if (state.watchParty != null) {
                var ticks = 0
                while (state.watchParty != null) {
                    state.socialAction { state.pollSocialRoom() }
                    if (++ticks % 30 == 0) state.socialAction { state.heartbeatRoom() }
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
                    Text(message, color = Color.White, fontSize = 13.sp)
                    Spacer(Modifier.width(12.dp))
                    FocusButton("Dismiss") { state.error = null }
                }
            }
        }
    }
}

@Composable
private fun SplashScreen() {
    Box(Modifier.fillMaxSize().background(Color(0xFF070B16))) {
        Image(painterResource(R.drawable.brunio_splash), "BruniO",
            modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
    }
}

@Composable
private fun MyListScreen(state: AppState, onBack: () -> Unit) {
    var catalog by remember { mutableStateOf<List<com.miflix.native2.model.MediaSummary>>(emptyList()) }
    LaunchedEffect(state.favorites.toList()) {
        catalog = state.favorites.mapNotNull { id -> runCatching { state.tmdb.byCloudId(id) }.getOrNull() }
    }
    CatalogScreen(state, "My List", catalog, onBack)
}

@Composable
private fun LoadingOverlay(text: String) {
    Box(Modifier.fillMaxSize().background(Color(0x55000000)), contentAlignment = Alignment.Center) {
        Text(
            text,
            color = Color.Black,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.background(Color.White, RoundedCornerShape(18.dp)).padding(horizontal = 28.dp, vertical = 17.dp)
        )
    }
}
