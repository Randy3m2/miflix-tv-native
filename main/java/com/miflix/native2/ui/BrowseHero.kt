package com.miflix.native2.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.miflix.native2.model.MediaSummary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

internal val LocalBrowsePreview=staticCompositionLocalOf<((MediaSummary) -> Unit)?> {null}

/** Header and rails have separate viewports. No hero height/layout changes on focus. */
@Composable
internal fun BrowsingViewport(state: AppState,fallback: MediaSummary?,modifier: Modifier=Modifier,
    heroHeight: Dp=228.dp,heroFocus: FocusRequester?=null,ranked: List<MediaSummary> = emptyList(),onRandom: (() -> Unit)?=null,content: @Composable () -> Unit) {
    val pending=remember {mutableStateOf<MediaSummary?>(null)}
    val publish=remember { {item: MediaSummary -> pending.value=item} }
    CompositionLocalProvider(LocalBrowsePreview provides publish) {
        Column(modifier.fillMaxSize()) {
            FocusedBrowseHero(state,pending,fallback,heroHeight,heroFocus,ranked,onRandom)
            Box(Modifier.weight(1f).fillMaxWidth()) {content()}
        }
    }
}

@Composable
private fun FocusedBrowseHero(state: AppState,pending: State<MediaSummary?>,fallback: MediaSummary?,height: Dp,
    heroFocus: FocusRequester?,ranked: List<MediaSummary>,onRandom: (() -> Unit)?) {
    var shown by remember {mutableStateOf(fallback)}
    var logo by remember {mutableStateOf<String?>(null)}
    var rankIndex by remember(ranked.map {it.cloudId}) {mutableIntStateOf(0)}
    var heroFocused by remember {mutableStateOf(false)}
    LaunchedEffect(ranked.map {it.cloudId}) {
        while(ranked.isNotEmpty()) {delay(6500);if(pending.value==null && !heroFocused) rankIndex=(rankIndex+1)%ranked.size}
    }
    val candidate=pending.value ?: ranked.getOrNull(rankIndex) ?: fallback
    LaunchedEffect(candidate?.cloudId,state.interfaceLanguage) {
        // Coalesce fast focus movements before decoding artwork or requesting logos.
        delay(140)
        shown=candidate;logo=null
        if(candidate!=null) {
            delay(650)
            try {logo=state.tmdb.browseLogo(candidate)} catch(e: CancellationException) {throw e} catch(_: Exception) { }
        }
    }
    val item=shown
    Box(Modifier.fillMaxWidth().height(height).background(Bg)) {
        if(item!=null) {
            Crossfade(item.backdrop ?: item.poster,modifier=Modifier.align(Alignment.CenterEnd).fillMaxHeight().fillMaxWidth(.75f),animationSpec=tween(140),label="browseBackdrop") {image ->
                AsyncImage(image,null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
            }
            Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Bg,Bg.copy(alpha=.92f),Color.Transparent))))
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent,Bg))))
            Column(Modifier.align(Alignment.TopStart).fillMaxHeight().padding(start=40.dp,end=22.dp,top=8.dp,bottom=12.dp).fillMaxWidth(.65f),verticalArrangement=Arrangement.spacedBy(7.dp)) {
                Box(Modifier.fillMaxWidth().height(if(height>=200.dp) 64.dp else 44.dp),contentAlignment=Alignment.CenterStart) {
                    if(logo!=null) AsyncImage(logo,item.title,Modifier.widthIn(max=280.dp).fillMaxHeight(),contentScale=ContentScale.Fit)
                    else Text(item.title,color=Color.White,fontSize=if(height>=200.dp) 28.sp else 23.sp,lineHeight=if(height>=200.dp) 30.sp else 23.sp,fontWeight=FontWeight.Black,maxLines=2,overflow=TextOverflow.Ellipsis)
                }
                Text(listOf(if(item.type=="series") tr("Serie","Series") else tr("Película","Movie"),item.year,"★ ${"%.1f".format(item.rating)}").filter {it.isNotBlank()}.joinToString(" · "),color=Muted,fontSize=12.sp)
                Text(item.overview,color=Color(0xFFD4D1DA),fontSize=13.sp,lineHeight=17.sp,maxLines=if(height>=200.dp) 3 else 2,overflow=TextOverflow.Ellipsis)
                if(heroFocus!=null) Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    FocusButton(tr("Ver detalles","View details"),primary=true,onFocused={heroFocused=it},modifier=Modifier.focusRequester(heroFocus)) {state.launch {state.open(item)}}
                    onRandom?.let {action -> FocusButton(tr("Aleatorio","Random"),onClick=action)}
                }
            }
            if(pending.value==null && heroFocus!=null) Text("#${rankIndex+1}  "+tr("TOP 10 · ESTA SEMANA","TOP 10 · THIS WEEK"),color=Accent,fontSize=11.sp,
                modifier=Modifier.align(Alignment.TopEnd).padding(end=34.dp,top=12.dp))
        }
    }
}
