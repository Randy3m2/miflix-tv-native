package com.miflix.native2.ui

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.miflix.native2.R
import com.miflix.native2.data.ThemeMotifsRP186
import com.miflix.native2.data.ThemePhotoPolicyRP188
import kotlin.math.roundToInt

/** Bundled still photographs: one decoded image, no networking or animation. */
@Composable
internal fun Modifier.themeStageRP186(mobile:Boolean):Modifier {
    val resource=when(ThemeRuntimeRP177.id) {
        "christmas"->R.drawable.theme_christmas_rp188
        "halloween"->R.drawable.theme_halloween_rp188
        "summer"->R.drawable.theme_summer_rp188
        "fogata"->R.drawable.theme_fogata_rp188
        "sand"->R.drawable.theme_sand_rp188
        "storm"->R.drawable.theme_storm_rp188
        "nature"->R.drawable.theme_nature_rp188
        "sea"->R.drawable.theme_sea_rp188
        else->null
    }
    val photo=resource?.let {ImageBitmap.imageResource(it)}
    val logo=if(ThemeRuntimeRP177.id=="nba_gsw") ImageBitmap.imageResource(R.drawable.theme_gsw_rp186) else null
    return themeBackdropRP181(mobile).drawWithCache {
        val p=ThemeMotifsRP186.placement(size.width,size.height)
        val scale=if(photo!=null && size.width>0f && size.height>0f) maxOf(size.width/photo.width,size.height/photo.height) else 1f
        val cropWidth=if(photo!=null) (size.width/scale).roundToInt().coerceIn(1,photo.width) else 1
        val cropHeight=if(photo!=null) (size.height/scale).roundToInt().coerceIn(1,photo.height) else 1
        onDrawBehind {
            if(photo!=null && size.width>0f && size.height>0f) {
                drawImage(photo,
                srcOffset=IntOffset(((photo.width-cropWidth)*.78f).roundToInt(),(photo.height-cropHeight)/2),
                srcSize=IntSize(cropWidth,cropHeight),
                dstSize=IntSize(size.width.roundToInt().coerceAtLeast(1),size.height.roundToInt().coerceAtLeast(1)),alpha=ThemePhotoPolicyRP188.photoAlpha)
                drawRect(Color.Black.copy(alpha=ThemePhotoPolicyRP188.shadeAlpha))
            }
            if(logo!=null && p.side>0f) drawImage(logo,dstOffset=IntOffset(p.x.roundToInt(),p.y.roundToInt()),dstSize=IntSize(p.side.roundToInt().coerceAtLeast(1),p.side.roundToInt().coerceAtLeast(1)),alpha=ThemeMotifsRP186.logoAlpha)
        }
    }
}

/** Transparent on an active theme: one continuous canvas, no nested rectangles. */
internal fun Modifier.themePageRP186(mobile:Boolean=false):Modifier =
    if(ThemeRuntimeRP177.id.isNotBlank()) this else background(if(mobile) MobileBgRP133 else Bg)
