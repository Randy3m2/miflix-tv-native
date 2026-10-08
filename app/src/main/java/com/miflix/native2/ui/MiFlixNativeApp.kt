package com.miflix.native2.ui

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
            Screen.PLAYER -> state.playerRequest?.let { req -> PlayerScreen(state, req) { state.screen = Screen.DETAILS } }
                ?: run { state.screen = Screen.HOME }
        }

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
    Box(Modifier.fillMaxSize().background(Bg), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(106.dp).background(Color.White, RoundedCornerShape(28.dp)), contentAlignment = Alignment.Center) {
                Text("M", color = Color.Black, fontSize = 46.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.height(17.dp))
            Text("MiFlix", color = Color.White, fontSize = 31.sp, fontWeight = FontWeight.Black)
            Text("Native TV", color = Muted, fontSize = 13.sp)
        }
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
