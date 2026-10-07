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
import kotlinx.coroutines.launch

@Composable
fun MiFlixNativeApp() {
    val context = LocalContext.current.applicationContext
    val state = remember { AppState(context) }
    val scope = rememberCoroutineScope()

    MaterialTheme {
        LaunchedEffect(Unit) { delay(650); state.bootstrap() }
        when (state.screen) {
            Screen.SPLASH -> SplashScreen()
            Screen.HOME -> HomeScreen(state) { dest -> state.screen = dest }
            Screen.SEARCH -> SearchScreen(state) { state.screen = Screen.HOME }
            Screen.MOVIES -> CatalogScreen(state, "Movies", state.movies, { state.screen = Screen.HOME })
            Screen.SERIES -> CatalogScreen(state, "Series", state.series, { state.screen = Screen.HOME })
            Screen.COLLECTIONS -> CollectionsScreen(state) { state.screen = Screen.HOME }
            Screen.MY_LIST -> MyListScreen(state) { state.screen = Screen.HOME }
            Screen.SETTINGS -> SettingsScreen(state) { state.screen = Screen.HOME }
            Screen.PROFILES -> ProfilesScreen(state) { state.screen = Screen.HOME }
            Screen.DETAILS -> DetailsScreen(state) { state.screen = Screen.HOME }
            Screen.PLAYER -> state.playerRequest?.let { req -> PlayerScreen(state, req) { state.screen = Screen.DETAILS } } ?: run { state.screen = Screen.HOME }
        }
        state.error?.let { message ->
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                Row(Modifier.padding(bottom = 30.dp).background(Color(0xEE6E202A), RoundedCornerShape(12.dp)).padding(horizontal = 22.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(message, color = Color.White, fontSize = 14.sp); Spacer(Modifier.width(14.dp)); FocusButton("Dismiss") { state.error = null }
                }
            }
        }
    }
}

@Composable
private fun SplashScreen() {
    Box(Modifier.fillMaxSize().background(Bg), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(112.dp).background(Purple, RoundedCornerShape(30.dp)), contentAlignment = Alignment.Center) { Text("M", color = Color.White, fontSize = 46.sp, fontWeight = FontWeight.Black) }
            Spacer(Modifier.height(18.dp)); Text("MiFlix", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Black); Text("Native TV · Alpha 2", color = Muted, fontSize = 14.sp)
        }
    }
}

@Composable
private fun MyListScreen(state: AppState, onBack: () -> Unit) {
    val scope = rememberCoroutineScope(); var items by remember { mutableStateOf<List<com.miflix.native2.model.MediaSummary>>(emptyList()) }
    LaunchedEffect(state.favorites.toList()) {
        items = state.favorites.mapNotNull { id -> runCatching { state.tmdb.byCloudId(id) }.getOrNull() }
    }
    CatalogScreen(state, "My List", items, onBack)
}
