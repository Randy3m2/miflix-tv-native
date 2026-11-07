package com.miflix.native2.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp
import com.miflix.native2.data.ThemeDetailsRP182

@Composable
internal fun ThemeEmblemRP182(id:String=ThemeRuntimeRP177.id,modifier:Modifier=Modifier,tint:Color=Color(ThemeRuntimeRP177.secondary)) {
    val detail=ThemeDetailsRP182.forId(id) ?: return
    val path=remember(id) {PathParser().parsePathString(detail.path).toPath()}
    Canvas(modifier.size(18.dp)) {
        val scale=size.minDimension/28f
        with(drawContext.canvas) {
            save();translate((size.width-24*scale)/2,(size.height-24*scale)/2);scale(scale,scale)
            drawPath(path,Paint().apply {color=tint;style=PaintingStyle.Stroke;strokeWidth=1.5f;strokeCap=StrokeCap.Round;strokeJoin=StrokeJoin.Round})
            restore()
        }
    }
}
