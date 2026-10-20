package com.miflix.native2.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.miflix.native2.model.MediaSummary
import com.miflix.native2.model.Profile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

/** Session cache: rotation and profile focus never poll a backend. */
private object ProfileArtCacheRP56 {
    var language = ""
    var fetchedAt = 0L
    var titles: List<MediaSummary> = emptyList()
}

@Composable
internal fun ProfilePickerRP56(
    state: AppState, picking: Boolean, onSelect: (Profile) -> Unit,
    onManage: () -> Unit, onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val first = remember { FocusRequester() }
    val locale = state.tmdb.language
    var titles by remember(locale) { mutableStateOf(
        if (ProfileArtCacheRP56.language == locale) ProfileArtCacheRP56.titles else emptyList()
    ) }
    var index by remember(locale) { mutableIntStateOf(0) }
    val active = state.appForeground
    LaunchedEffect(locale, active) {
        if (!active) return@LaunchedEffect
        val fresh = ProfileArtCacheRP56.language == locale &&
            System.currentTimeMillis() - ProfileArtCacheRP56.fetchedAt < 6 * 60 * 60_000L
        if (!fresh) {
            try {
                val loaded = state.tmdb.trending().filter { it.backdrop != null }
                    .distinctBy { it.cloudId }.take(10)
                if (loaded.isNotEmpty() && state.tmdb.language == locale) {
                    ProfileArtCacheRP56.language = locale
                    ProfileArtCacheRP56.titles = loaded
                    ProfileArtCacheRP56.fetchedAt = System.currentTimeMillis()
                    titles = loaded
                }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { /* Remain usable offline, with a black background. */ }
        }
    }
    LaunchedEffect(active, titles) {
        if (active && titles.size > 1) while (true) {
            delay(12_000)
            index = (index + 1) % titles.size
        }
    }
    LaunchedEffect(state.profiles.firstOrNull()?.id) {
        withFrameNanos { }
        runCatching { first.requestFocus() }
    }
    val current = titles.getOrNull(index % titles.size.coerceAtLeast(1))
    val drift = remember { Animatable(0f) }
    LaunchedEffect(current?.cloudId, active) {
        if (active) {
            drift.snapTo(0f)
            drift.animateTo(1f, tween(12_000, easing = LinearEasing))
        }
    }
    BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black).clipToBounds()) {
        val avatarSize = ((maxHeight.value - 166f) / state.profiles.size.coerceIn(4, 6) - 12f)
            .coerceIn(52f, 82f).dp
        Box(Modifier.align(Alignment.CenterEnd).fillMaxWidth(.76f).fillMaxHeight()) {
            Crossfade(targetState = current?.backdrop, animationSpec = tween(1100), label = "profileArtwork") { url ->
                if (url != null) AsyncImage(url, null,
                    Modifier.fillMaxSize().graphicsLayer {
                        scaleX = 1.065f; scaleY = 1.065f
                        translationX = (drift.value - .5f) * size.width * .035f
                    }, contentScale = ContentScale.Crop)
            }
        }
        // Solid left third: names and avatars remain legible on bright artwork.
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(
            0f to Color.Black, .30f to Color.Black, .48f to Color(0xEF000000),
            .68f to Color(0x68000000), 1f to Color(0x10000000))))
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
            0f to Color(0x55000000), .55f to Color.Transparent, 1f to Color(0xD9000000))))
        Column(Modifier.fillMaxHeight().fillMaxWidth(.48f).padding(start = 48.dp, top = 24.dp, bottom = 18.dp)) {
            Text("BruniO", color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.Black)
            Text(if (picking) tr("Preparando tu perfil…", "Preparing your profile…")
                else tr("Elige un perfil", "Choose a profile"), color = Color(0xFFB6B6B6), fontSize = 14.sp)
            Spacer(Modifier.height(16.dp))
            LazyColumn(Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 5.dp, horizontal = 5.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(state.profiles, key = { it.id }) { profile ->
                    var focused by remember(profile.id) { mutableStateOf(false) }
                    val scale by animateFloatAsState(if (focused) 1.07f else 1f, tween(160), label = "profileAvatar")
                    Row(Modifier.fillMaxWidth()
                        .then(if (profile.id == state.profiles.firstOrNull()?.id) Modifier.focusRequester(first) else Modifier)
                        .onFocusChanged { focused = it.isFocused }
                        .tvClick { if (!picking) onSelect(profile) }
                        .clickable { if (!picking) onSelect(profile) },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        Box(Modifier.graphicsLayer { scaleX = scale; scaleY = scale }
                            .border(if (focused) 2.dp else 0.dp, if (focused) Color.White else Color.Transparent,
                                RoundedCornerShape(14.dp)).padding(4.dp)) {
                            ProfileAvatar(profile, Modifier.size(avatarSize))
                        }
                        Text(profile.name, color = if (focused) Color.White else Color(0xFFAAAAAA),
                            fontSize = if (focused) 23.sp else 17.sp,
                            fontWeight = if (focused) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                item {
                    FocusButton(tr("＋ Agregar / administrar", "＋ Add / manage profiles")) { onManage() }
                }
            }
        }
        current?.let { title ->
            Column(Modifier.align(Alignment.BottomEnd).fillMaxWidth(.44f).padding(end = 36.dp, bottom = 32.dp),
                horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(tr("TOP 10 · TENDENCIAS", "TOP 10 · TRENDING"), color = Color(0xFFCCCCCC), fontSize = 10.sp)
                Text(title.title, color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
