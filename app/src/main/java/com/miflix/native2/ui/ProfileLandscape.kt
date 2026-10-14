package com.miflix.native2.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.miflix.native2.R

/** One local texture, transformed in the graphics layer; no video decoder or GIF frames. */
@Composable
internal fun ProfileLandscape(active: Boolean) {
    val drift=remember { Animatable(0f) }
    LaunchedEffect(active) {
        if(active) while(true) {
            drift.animateTo(1f,tween(26000,easing=LinearEasing))
            drift.animateTo(0f,tween(26000,easing=LinearEasing))
        }
    }
    Box(Modifier.fillMaxSize().clipToBounds()) {
        Image(painterResource(R.drawable.brunio_profile_landscape),null,
            Modifier.fillMaxSize().graphicsLayer {
                scaleX=1.06f;scaleY=1.06f
                translationX=(drift.value-.5f)*size.width*.035f
                translationY=(drift.value-.5f)*size.height*.018f
            },contentScale=ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xA6050505),Color(0x30050505)))))
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x50050505),Color(0x08050505),Color(0xC0050505)))))
    }
}
