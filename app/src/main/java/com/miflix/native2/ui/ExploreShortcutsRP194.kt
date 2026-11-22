package com.miflix.native2.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.miflix.native2.R

/** RP194: visually consistent Home rail. All five original actions are preserved. */
internal enum class ExploreDestinationRP194 { TRAILERS, SURPRISE, CINEMA, COLLECTIONS, OSCARS, AMBIENT, DAILY_ROTATION }
private data class ExploreArtworkRP194(
    val destination: ExploreDestinationRP194,
    val title: String,
    val imageRes: Int
)

@Composable
internal fun ExploreShortcutsRP194(
    loading: Boolean,
    onTrailers: () -> Unit,
    onSurprise: () -> Unit,
    onCinema: () -> Unit,
    onCollections: () -> Unit,
    onOscars: () -> Unit,
    onAmbient: () -> Unit,
    onDailyRotation: () -> Unit
) {
    val entries=listOf(
        ExploreArtworkRP194(ExploreDestinationRP194.TRAILERS,tr("Tráilers","Trailers"),R.drawable.brunio_profile_landscape),
        ExploreArtworkRP194(ExploreDestinationRP194.SURPRISE,if(loading) tr("Preparando…","Preparing…") else tr("Sorpréndeme","Surprise me"),R.drawable.theme_halloween_rp188),
        ExploreArtworkRP194(ExploreDestinationRP194.CINEMA,tr("Mi universo de cine","My cinema hub"),R.drawable.theme_storm_rp188),
        ExploreArtworkRP194(ExploreDestinationRP194.COLLECTIONS,tr("Colecciones","Collections"),R.drawable.franchise_marvel),
        ExploreArtworkRP194(ExploreDestinationRP194.OSCARS,"Oscars",R.drawable.theme_fogata_rp188),
        ExploreArtworkRP194(ExploreDestinationRP194.AMBIENT,tr("Cine ambiental","Ambient Cinema"),R.drawable.theme_sea_rp188),
        ExploreArtworkRP194(ExploreDestinationRP194.DAILY_ROTATION,tr("Rotación diaria","Daily Rotation"),R.drawable.theme_nature_rp188)
    )
    val rowHeight=if(LocalHomeShelfPaging.current) LocalHomeCardRowHeight.current+40.dp else 228.dp
    Column(Modifier.fillMaxWidth().height(rowHeight)) {
        Text(tr("Explorar","Explore"),color=Color.White,fontSize=22.sp,lineHeight=27.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(start=40.dp,bottom=8.dp))
        HomeShortcutRail("explore-shortcuts",entries,{it.destination.name}) {entry,modifier ->
            val action=when(entry.destination) {
                ExploreDestinationRP194.TRAILERS -> onTrailers
                ExploreDestinationRP194.SURPRISE -> onSurprise
                ExploreDestinationRP194.CINEMA -> onCinema
                ExploreDestinationRP194.COLLECTIONS -> onCollections
                ExploreDestinationRP194.OSCARS -> onOscars
                ExploreDestinationRP194.AMBIENT -> onAmbient
                ExploreDestinationRP194.DAILY_ROTATION -> onDailyRotation
            }
            ExploreShortcutCardRP194(entry,modifier.then(homeReturnFocus("explore:"+entry.destination.name)),action)
        }
    }
}

@Composable
private fun ExploreShortcutCardRP194(entry:ExploreArtworkRP194,modifier:Modifier,onClick:()->Unit) {
    var focused by remember {mutableStateOf(false)}
    val scale by animateFloatAsState(if(focused) 1.025f else 1f,tween(120),label="exploreScale")
    val stroke by animateColorAsState(if(focused) Accent else Color.White.copy(alpha=.12f),tween(120),label="exploreStroke")
    val shape=RoundedCornerShape(15.dp)
    Box(
        modifier.width(homePosterSlotWidth()).height(LocalHomeCardRowHeight.current-12.dp).padding(horizontal=6.dp)
            .graphicsLayer { scaleX=scale;scaleY=scale }
            .onFocusChanged {focused=it.isFocused}
            .tvClick(onClick).clickable(onClick=onClick)
            .clip(shape)
            .background(Color(0xFF18191E))
            .border(if(focused) 2.dp else 1.dp,stroke,shape)
    ) {
        Image(painterResource(entry.imageRes),contentDescription=null,modifier=Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
            listOf(Color.Black.copy(alpha=.16f),Color.Black.copy(alpha=.45f),Color.Black.copy(alpha=.93f))
        )))
        if(entry.destination==ExploreDestinationRP194.OSCARS) {
            AsyncImage("file:///android_asset/league_logos_rp138/oscars.png",null,
                Modifier.align(Alignment.TopEnd).padding(10.dp).size(42.dp),contentScale=ContentScale.Fit)
        }
        Text(entry.title,color=Color.White,fontSize=13.sp,lineHeight=17.sp,fontWeight=FontWeight.Bold,
            maxLines=2,overflow=TextOverflow.Ellipsis,modifier=Modifier.align(Alignment.BottomStart).padding(10.dp))
    }
}
