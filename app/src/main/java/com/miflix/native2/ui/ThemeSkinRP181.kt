package com.miflix.native2.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Static vector skins: no animation timer or decoding work on playback threads. */
internal fun themeShapeRP181(radius:Dp=16.dp):Shape = when(ThemeRuntimeRP177.id) {
    "venom" -> RoundedCornerShape(topStart=18.dp,topEnd=10.dp,bottomEnd=18.dp,bottomStart=10.dp)
    "god_of_war","horizon_zero_dawn","halo","cyberpunk_2077","batman","justice_league","the_mandalorian" -> CutCornerShape(12.dp)
    "superman","assassins_creed","arcane" -> CutCornerShape(topStart=18.dp,bottomEnd=18.dp)
    "friends","dragon_ball","gta_vice_city","summer","spring","christmas" -> RoundedCornerShape(22.dp)
    "ghost_of_tsushima","the_last_of_us","chernobyl","fallout","the_matrix" -> RoundedCornerShape(4.dp)
    else -> RoundedCornerShape(radius)
}

internal fun Modifier.themeSurfaceRP181(selected:Boolean=false,radius:Dp=16.dp):Modifier {
    val active=ThemeRuntimeRP177.id.isNotBlank()
    if(!active) return clip(RoundedCornerShape(radius)).background(Brush.verticalGradient(listOf(
        if(selected) Color(0xFF51436F) else Color(0xFF222733),if(selected) Color(0xFF242039) else Color(0xFF10141B))))
        .border(1.dp,if(selected) MobileTintRP133 else Color(0xFF353C49),RoundedCornerShape(radius))
    val shape=themeShapeRP181(radius)
    val accent=Accent; val panel=if(active) Panel else Color(0xFF141821)
    val top=if(selected) androidx.compose.ui.graphics.lerp(panel,accent,.42f) else androidx.compose.ui.graphics.lerp(panel,accent,.12f)
    return clip(shape).background(Brush.verticalGradient(listOf(top,panel)))
        .border(if(selected) 2.dp else 1.dp,if(selected) accent else if(active) PanelBorder.copy(alpha=.65f) else Color(0xFF353C49),shape)
        .themeOrnamentRP181()
}

internal fun Modifier.themeOrnamentRP181():Modifier {
    val id=ThemeRuntimeRP177.id
    val accent=Accent
    val secondary=Color(ThemeRuntimeRP177.secondary)
    if(id.isBlank()) return this
    return drawWithCache {
        val w=size.width; val h=size.height
        val line=Path()
        if(id=="venom") {
            val edge=3.dp.toPx()
            line.moveTo(w*.12f,edge); line.cubicTo(w*.25f,edge*2,w*.28f,0f,w*.4f,edge)
            line.moveTo(w*.6f,h-edge); line.cubicTo(w*.72f,h-edge*2,w*.8f,h,w*.88f,h-edge)
        } else {
            val d=minOf(w,h)*.18f
            line.moveTo(5f,h*.33f);line.lineTo(5f,5f);line.lineTo(d,5f)
            line.moveTo(w-d,h-5f);line.lineTo(w-5f,h-5f);line.lineTo(w-5f,h*.67f)
        }
        onDrawWithContent {
            drawContent()
            drawPath(line,if(id=="venom") accent.copy(alpha=.35f) else secondary.copy(alpha=.3f),style=Stroke(1.dp.toPx()))
        }
    }
}

internal fun Modifier.themeBackdropRP181(mobile:Boolean=false):Modifier {
    val id=ThemeRuntimeRP177.id
    val base=if(mobile && id.isBlank()) MobileBgRP133 else Bg;val accent=Accent
    return background(base).drawWithCache {
        val wash=Brush.radialGradient(listOf(accent.copy(alpha=.1f),Color.Transparent),center=Offset(size.width*.9f,size.height*.15f),radius=size.maxDimension*.85f)
        val ornament=Path()
        if(id=="venom") {
            ornament.moveTo(size.width*.65f,0f)
            ornament.cubicTo(size.width*1.2f,size.height*.2f,size.width*.5f,size.height*.23f,size.width*.95f,size.height*.5f)
            ornament.cubicTo(size.width*1.2f,size.height*.7f,size.width*.6f,size.height*.88f,size.width,size.height)
            ornament.moveTo(0f,size.height*.14f)
            ornament.cubicTo(size.width*.2f,size.height*.25f,-size.width*.1f,size.height*.7f,size.width*.16f,size.height*.94f)
        } else {
            repeat(5) {i->
                val x=size.width*(.55f+i*.13f)
                ornament.moveTo(x,0f);ornament.lineTo(x-size.width*.3f,size.height)
            }
        }
        onDrawBehind {
            if(id.isNotBlank()) {
                drawRect(wash)
                drawPath(ornament,accent.copy(alpha=if(id=="venom") .065f else .035f),style=Stroke(if(id=="venom") 4.dp.toPx() else 1.dp.toPx()))
                if(id=="christmas") repeat(24) {i->
                    val x=size.width*((i*37%101)/100f)
                    val y=size.height*((i*23%97)/100f)
                    drawCircle(Color.White.copy(alpha=.12f),1.5.dp.toPx(),Offset(x,y))
                }
            }
        }
    }
}
