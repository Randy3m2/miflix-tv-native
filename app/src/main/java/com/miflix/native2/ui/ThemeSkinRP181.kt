package com.miflix.native2.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Color and soft geometry only: no symbols, textures, timers or overlay ornaments. */
internal fun themeShapeRP181(radius:Dp=16.dp):Shape {
    if(ThemeRuntimeRP177.id.isBlank() || radius>=40.dp) return RoundedCornerShape(radius)
    return RoundedCornerShape(ThemeRuntimeRP177.radius.dp)
}

internal fun Modifier.themeSurfaceRP181(selected:Boolean=false,radius:Dp=16.dp):Modifier {
    if(ThemeRuntimeRP177.id.isBlank()) return clip(RoundedCornerShape(radius)).background(Brush.verticalGradient(listOf(
        if(selected) Color(0xFF51436F) else Color(0xFF222733),if(selected) Color(0xFF242039) else Color(0xFF10141B))))
        .border(1.dp,if(selected) MobileTintRP133 else Color(0xFF353C49),RoundedCornerShape(radius))
    val shape=themeShapeRP181(radius)
    val panel=Panel
    val top=lerp(panel,Accent,if(selected) .24f else .055f)
    return clip(shape).background(Brush.verticalGradient(listOf(top,panel)))
        .border(if(selected) 2.dp else 1.dp,if(selected) Accent else PanelBorder.copy(alpha=.48f),shape)
}

// Kept as a no-op for existing callers: decoration never covers content.
internal fun Modifier.themeOrnamentRP181():Modifier = this

internal fun Modifier.themeBackdropRP181(mobile:Boolean=false):Modifier {
    val active=ThemeRuntimeRP177.id.isNotBlank()
    val base=if(mobile && !active) MobileBgRP133 else Bg
    val accent=Accent
    val secondary=if(ThemeRuntimeRP177.id=="christmas") Color(0xFFB9565E) else Color(ThemeRuntimeRP177.secondary)
    return background(base).drawWithCache {
        val glow=Brush.radialGradient(listOf(accent.copy(alpha=.09f),Color.Transparent),
            center=Offset(size.width*.95f,size.height*.05f),radius=size.maxDimension*.8f)
        val lowGlow=Brush.radialGradient(listOf(secondary.copy(alpha=if(ThemeRuntimeRP177.id=="christmas") .065f else .035f),Color.Transparent),
            center=Offset(0f,size.height),radius=size.maxDimension*.75f)
        onDrawBehind {
            if(active) {drawRect(glow);drawRect(lowGlow)}
        }
    }
}
