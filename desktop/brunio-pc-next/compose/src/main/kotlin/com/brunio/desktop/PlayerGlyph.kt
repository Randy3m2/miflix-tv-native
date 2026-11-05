package com.brunio.desktop

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

// Glyphs ported from BruniO TV PlayerControls.kt; TV/mobile sources remain unchanged.
@Composable
internal fun PlayerGlyph(icon: String, color: Color, modifier: Modifier) {
    Canvas(modifier) {
        val w=size.width; val h=size.height; val stroke=Stroke(2.dp.toPx())
        fun line(x1:Float,y1:Float,x2:Float,y2:Float)=drawLine(color,Offset(w*x1,h*y1),Offset(w*x2,h*y2),2.dp.toPx())
        fun rectangle(x:Float,y:Float,a:Float,b:Float)=drawRect(color,Offset(w*x,h*y),Size(w*a,h*b),style=stroke)
        when(icon) {
            "search" -> { drawCircle(color,w*.28f,Offset(w*.4f,h*.4f),style=stroke);line(.61f,.61f,.9f,.9f) }
            "calendar" -> { rectangle(.1f,.2f,.8f,.7f);line(.1f,.4f,.9f,.4f);line(.3f,.1f,.3f,.3f);line(.7f,.1f,.7f,.3f) }
            "download" -> {line(.5f,.1f,.5f,.65f);line(.28f,.45f,.5f,.67f);line(.5f,.67f,.72f,.45f);line(.15f,.7f,.15f,.9f);line(.15f,.9f,.85f,.9f);line(.85f,.9f,.85f,.7f)}
            "notifications" -> {drawArc(color,180f,180f,false,Offset(w*.2f,h*.1f),Size(w*.6f,h*.6f),style=stroke);line(.2f,.4f,.2f,.7f);line(.8f,.4f,.8f,.7f);line(.15f,.7f,.85f,.7f);drawArc(color,0f,180f,false,Offset(w*.4f,h*.76f),Size(w*.2f,h*.15f),style=stroke)}
            "settings" -> {drawCircle(color,w*.15f,Offset(w*.5f,h*.5f),style=stroke);drawCircle(color,w*.35f,Offset(w*.5f,h*.5f),style=stroke);line(.5f,.04f,.5f,.18f);line(.5f,.82f,.5f,.96f);line(.04f,.5f,.18f,.5f);line(.82f,.5f,.96f,.5f)}
            "stats" -> {rectangle(.14f,.55f,.16f,.30f);rectangle(.42f,.33f,.16f,.52f);rectangle(.70f,.12f,.16f,.73f)}
            "play" -> drawPath(Path().apply { moveTo(w*.28f,h*.15f); lineTo(w*.85f,h*.5f); lineTo(w*.28f,h*.85f); close() },color)
            "pause" -> { drawRect(color,Offset(w*.22f,h*.17f),Size(w*.18f,h*.66f)); drawRect(color,Offset(w*.60f,h*.17f),Size(w*.18f,h*.66f)) }
            "previous" -> { drawPath(Path().apply {moveTo(w*.84f,h*.15f);lineTo(w*.30f,h*.5f);lineTo(w*.84f,h*.85f);close()},color);drawRect(color,Offset(w*.14f,h*.15f),Size(w*.10f,h*.70f)) }
            "rewind" -> {drawPath(Path().apply {moveTo(w*.48f,h*.2f);lineTo(w*.12f,h*.5f);lineTo(w*.48f,h*.8f);close()},color);drawPath(Path().apply {moveTo(w*.88f,h*.2f);lineTo(w*.52f,h*.5f);lineTo(w*.88f,h*.8f);close()},color)}
            "forward" -> {drawPath(Path().apply {moveTo(w*.12f,h*.2f);lineTo(w*.48f,h*.5f);lineTo(w*.12f,h*.8f);close()},color);drawPath(Path().apply {moveTo(w*.52f,h*.2f);lineTo(w*.88f,h*.5f);lineTo(w*.52f,h*.8f);close()},color)}
            "next" -> { drawPath(Path().apply { moveTo(w*.16f,h*.15f);lineTo(w*.70f,h*.5f);lineTo(w*.16f,h*.85f);close() },color);drawRect(color,Offset(w*.76f,h*.15f),Size(w*.10f,h*.70f)) }
            "queue" -> { line(.12f,.2f,.88f,.2f);line(.12f,.5f,.70f,.5f);line(.12f,.8f,.55f,.8f);line(.78f,.64f,.78f,.94f);line(.64f,.79f,.92f,.79f) }
            "episodes" -> { rectangle(.08f,.12f,.24f,.20f);rectangle(.08f,.42f,.24f,.20f);rectangle(.08f,.72f,.24f,.20f);line(.46f,.22f,.92f,.22f);line(.46f,.52f,.92f,.52f);line(.46f,.82f,.92f,.82f) }
            "moment" -> { rectangle(.12f,.22f,.76f,.56f);line(.3f,.12f,.3f,.88f);line(.7f,.12f,.7f,.88f) }
            "info" -> { drawCircle(color,w*.4f,Offset(w*.5f,h*.5f),style=stroke);line(.5f,.45f,.5f,.75f);drawCircle(color,w*.04f,Offset(w*.5f,h*.27f)) }
            "pip" -> { rectangle(.05f,.15f,.9f,.7f);drawRect(color,Offset(w*.52f,h*.48f),Size(w*.33f,h*.26f)) }
            "score" -> {rectangle(.05f,.18f,.9f,.64f);line(.5f,.18f,.5f,.82f);line(.17f,.38f,.35f,.38f);line(.17f,.58f,.35f,.58f);line(.65f,.38f,.83f,.38f);line(.65f,.58f,.83f,.58f)}
            "grid" -> { rectangle(.08f,.08f,.35f,.35f);rectangle(.57f,.08f,.35f,.35f);rectangle(.08f,.57f,.35f,.35f);rectangle(.57f,.57f,.35f,.35f) }
            "close" -> { line(.2f,.2f,.8f,.8f); line(.8f,.2f,.2f,.8f) }
            "maximize" -> { line(.1f,.35f,.1f,.1f);line(.1f,.1f,.35f,.1f);line(.65f,.1f,.9f,.1f);line(.9f,.1f,.9f,.35f);line(.9f,.65f,.9f,.9f);line(.9f,.9f,.65f,.9f);line(.35f,.9f,.1f,.9f);line(.1f,.9f,.1f,.65f) }
            "resize" -> { rectangle(.1f,.2f,.8f,.6f);line(.22f,.65f,.42f,.45f);line(.22f,.65f,.22f,.46f);line(.22f,.65f,.41f,.65f);line(.78f,.35f,.58f,.55f);line(.78f,.35f,.78f,.54f);line(.78f,.35f,.59f,.35f) }
            "speed" -> { drawArc(color,180f,180f,false,Offset(w*.1f,h*.2f),Size(w*.8f,h*.65f),style=stroke);line(.15f,.8f,.85f,.8f);line(.5f,.7f,.76f,.3f); drawCircle(color,w*.06f,Offset(w*.5f,h*.7f)) }
            "cc" -> { rectangle(.04f,.2f,.92f,.6f);drawArc(color,60f,240f,false,Offset(w*.17f,h*.34f),Size(w*.26f,h*.32f),style=stroke);drawArc(color,60f,240f,false,Offset(w*.56f,h*.34f),Size(w*.26f,h*.32f),style=stroke) }
            "audio" -> { rectangle(.24f,.08f,.52f,.84f);drawCircle(color,w*.08f,Offset(w*.5f,h*.3f),style=stroke);drawCircle(color,w*.16f,Offset(w*.5f,h*.66f),style=stroke) }
            "links" -> { drawRoundRect(color,Offset(w*.04f,h*.38f),Size(w*.55f,h*.3f),cornerRadius=androidx.compose.ui.geometry.CornerRadius(w*.14f),style=stroke);drawRoundRect(color,Offset(w*.41f,h*.26f),Size(w*.55f,h*.3f),cornerRadius=androidx.compose.ui.geometry.CornerRadius(w*.14f),style=stroke);line(.35f,.55f,.65f,.4f) }
            "chat" -> { rectangle(.08f,.12f,.84f,.62f);line(.22f,.74f,.22f,.94f);line(.22f,.94f,.44f,.74f);line(.24f,.34f,.76f,.34f);line(.24f,.52f,.60f,.52f) }
            "reactions" -> { drawCircle(color,w*.42f,Offset(w*.5f,h*.5f),style=stroke);drawCircle(color,w*.035f,Offset(w*.35f,h*.38f));drawCircle(color,w*.035f,Offset(w*.65f,h*.38f));drawArc(color,0f,180f,false,Offset(w*.27f,h*.42f),Size(w*.46f,h*.30f),style=stroke) }
            "party" -> { drawCircle(color,w*.14f,Offset(w*.34f,h*.3f),style=stroke);drawCircle(color,w*.11f,Offset(w*.75f,h*.35f),style=stroke);drawArc(color,180f,180f,false,Offset(w*.08f,h*.52f),Size(w*.52f,h*.38f),style=stroke);drawArc(color,180f,180f,false,Offset(w*.60f,h*.6f),Size(w*.30f,h*.25f),style=stroke) }

        }
    }
}

