package com.miflix.native2.ui

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.miflix.native2.R
import com.miflix.native2.data.ThemeMotifsRP186
import kotlin.math.roundToInt

/** Paint the full window once; nested pages must not create inset backdrops. */
@Composable
internal fun Modifier.themeStageRP186(mobile:Boolean):Modifier {
    val id=ThemeRuntimeRP177.id
    val logo=if(id=="nba_gsw") ImageBitmap.imageResource(R.drawable.theme_gsw_rp186) else null
    val color=Color(ThemeRuntimeRP177.secondary)
    return themeBackdropRP181(mobile).drawWithCache {
        val path=ThemeMotifsRP186.paths[id]?.let {PathParser().parsePathString(it).toPath()}
        val p=ThemeMotifsRP186.placement(size.width,size.height)
        onDrawBehind {
            if(p.side>0f) {
                if(logo!=null) drawImage(logo,dstOffset=IntOffset(p.x.roundToInt(),p.y.roundToInt()),dstSize=IntSize(p.side.roundToInt().coerceAtLeast(1),p.side.roundToInt().coerceAtLeast(1)),alpha=ThemeMotifsRP186.logoAlpha)
                else if(path!=null) withTransform({translate(p.x,p.y);scale(p.side/100f,p.side/100f,pivot=androidx.compose.ui.geometry.Offset.Zero)}) {
                    drawPath(path,color.copy(alpha=ThemeMotifsRP186.silhouetteAlpha),style=Stroke(1.4f))
                }
            }
        }
    }
}

/** Transparent on an active theme: one continuous canvas, no nested rectangles. */
internal fun Modifier.themePageRP186(mobile:Boolean=false):Modifier =
    if(ThemeRuntimeRP177.id.isNotBlank()) this else background(if(mobile) MobileBgRP133 else Bg)
