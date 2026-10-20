package com.miflix.native2.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import kotlinx.coroutines.CancellationException

private val directorPhotosRP58 = mutableStateMapOf<String, String>()

@Composable
internal fun DirectorRailRP58(state: AppState) {
    var selected by remember { mutableStateOf(state.directors.firstOrNull()?.key) }
    val title = state.directors.firstOrNull { it.key == selected }?.let {
        if (it.key == "russo") tr("Hermanos Russo", "Russo Brothers") else it.title
    }.orEmpty()
    Column(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(122.dp).clipToBounds()) {
            AsyncImage(selected?.let { directorPhotosRP58[it] }, null,
                Modifier.align(Alignment.CenterEnd).width(250.dp).fillMaxHeight(), contentScale = ContentScale.Crop,
                colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }))
            Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(
                0f to Bg, .58f to Bg, .82f to Color(0xB0050505), 1f to Color.Transparent)))
            Text(title, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterStart).padding(start = 34.dp), maxLines = 2)
        }
        Text(tr("Directores", "Directors"), color = Muted, fontSize = 17.sp,
            modifier = Modifier.padding(start = 34.dp, bottom = 10.dp))
        HomeHorizontalRail {
            LazyRow(Modifier.fillMaxWidth().smoothHorizontalDpad(),
                contentPadding = PaddingValues(horizontal = 28.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                items(state.directors, key = { it.key }) { group ->
                    var photo by remember(group.key) { mutableStateOf(
                        group.people.firstOrNull()?.photo ?: directorPhotosRP58[group.key]) }
                    LaunchedEffect(group.key) {
                        if (photo == null) try {
                            photo = state.tmdb.directorPerson(group.names.first())?.photo
                            photo?.let { directorPhotosRP58[group.key] = it }
                        } catch (e: CancellationException) { throw e }
                        catch (_: Exception) { /* Name remains usable offline. */ }
                    }
                    var focused by remember { mutableStateOf(false) }
                    val scale by animateFloatAsState(if (focused) 1.025f else 1f, tween(140), label = "directorFocus")
                    val label = if (group.key == "russo") tr("Hermanos Russo", "Russo Brothers") else group.title
                    val open = { state.launch { state.socialAction { state.openDirector(group) } }; Unit }
                    Box(Modifier.width(250.dp).height(148.dp)
                        .then(homeReturnFocus("director:${group.key}"))
                        .onFocusChanged { focused = it.isFocused; if (it.isFocused) selected = group.key }
                        .tvClick(open).clickable(onClick = open)
                        .graphicsLayer { scaleX = scale; scaleY = scale }
                        .border(if (focused) 2.dp else 1.dp, if (focused) Accent else Color(0xFF343434), RoundedCornerShape(15.dp))
                        .padding(3.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF222225))) {
                        AsyncImage(photo, null, Modifier.align(Alignment.CenterEnd).fillMaxHeight().fillMaxWidth(.68f),
                            contentScale = ContentScale.Crop,
                            colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }))
                        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(
                            listOf(Color(0xE5222225), Color.Transparent))))
                        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
                            listOf(Color.Transparent, Color(0xE9000000)))))
                        Text(label, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold,
                            maxLines = 2, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.align(Alignment.BottomStart).padding(12.dp))
                    }
                }
            }
        }
        Spacer(Modifier.height(18.dp))
    }
}
