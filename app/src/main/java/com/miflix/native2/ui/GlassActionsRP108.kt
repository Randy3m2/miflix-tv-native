package com.miflix.native2.ui

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

private var paletteColorRP164=0
private var glassPaletteRP108=Array(8) {index ->
    val alpha=if((index and 2)!=0) .32f else if((index and 1)!=0) .19f else .075f
    val tint=if((index and 4)!=0) Color(0xFFFF9EAA) else Color.White
    Brush.verticalGradient(listOf(tint.copy(alpha=alpha),tint.copy(alpha=alpha*.38f)))
}
/** Reused gradients only: no backdrop blur, network requests or repeated shader allocation. */
internal fun GlassActionBrushRP108(focused:Boolean,selected:Boolean=false,destructive:Boolean=false):Brush {
    val color=FocusTheme.argb
    if(paletteColorRP164!=color) {
        paletteColorRP164=color
        glassPaletteRP108=Array(8) {index->
            val alpha=if((index and 2)!=0) .32f else if((index and 1)!=0) .19f else .075f
            val tint=if((index and 4)!=0) Color(0xFFFF9EAA) else Color(color)
            Brush.verticalGradient(listOf(tint.copy(alpha=alpha),tint.copy(alpha=alpha*.38f)))
        }
    }
    return glassPaletteRP108[(if(destructive) 4 else 0)+(if(focused) 2 else 0)+(if(selected) 1 else 0)]
}
