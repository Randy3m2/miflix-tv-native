package com.miflix.native2.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.GenericShape
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
    "venom" -> GenericShape { size,_ ->
        val w=size.width; val h=size.height
        moveTo(.08f*w,.04f*h)
        cubicTo(.27f*w,-.02f*h,.30f*w,.09f*h,.5f*w,.035f*h)
        cubicTo(.7f*w,-.015f*h,.82f*w,.08f*h,.94f*w,.04f*h)
        cubicTo(1.01f*w,.20f*h,.96f*w,.35f*h,.99f*w,.52f*h)
        cubicTo(.96f*w,.7f*h,1.02f*w,.87f*h,.92f*w,.95f*h)
        cubicTo(.7f*w,1.02f*h,.61f*w,.92f*h,.43f*w,.97f*h)
        cubicTo(.25f*w,1.02f*h,.16f*w,.91f*h,.06f*w,.96f*h)
        cubicTo(-.02f*w,.78f*h,.045f*w,.67f*h,.015f*w,.45f*h)
        cubicTo(-.015f*w,.22f*h,.04f*w,.16f*h,.08f*w,.04f*h)
        close()
    }
    "god_of_war","horizon_zero_dawn","halo","cyberpunk_2077","batman","justice_league","the_mandalorian" -> CutCornerShape(12.dp)
    "superman","assassins_creed","arcane" -> CutCornerShape(topStart=18.dp,bottomEnd=18.dp)
    "friends","dragon_ball","gta_vice_city" -> RoundedCornerShape(28.dp)
    "ghost_of_tsushima","the_last_of_us","chernobyl","fallout","the_matrix" -> RoundedCornerShape(4.dp)
    else -> RoundedCornerShape(radius)
}

internal fun Modifier.themeSurfaceRP181(selected:Boolean=false,radius:Dp=16.dp):Modifier {
    val active=ThemeRuntimeRP177.id.isNotBlank()
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
            line.moveTo(0f,h*.24f); line.cubicTo(w*.2f,h*.04f,w*.14f,h*.45f,w*.32f,h*.11f)
            line.moveTo(w,h*.73f); line.cubicTo(w*.79f,h*.95f,w*.9f,h*.56f,w*.7f,h*.9f)
        } else {
            val d=minOf(w,h)*.18f
            line.moveTo(5f,h*.33f);line.lineTo(5f,5f);line.lineTo(d,5f)
            line.moveTo(w-d,h-5f);line.lineTo(w-5f,h-5f);line.lineTo(w-5f,h*.67f)
        }
        onDrawWithContent {
            drawContent()
            drawPath(line,if(id=="venom") accent.copy(alpha=.6f) else secondary.copy(alpha=.45f),style=Stroke(1.5.dp.toPx()))
        }
    }
}

internal fun Modifier.themeBackdropRP181():Modifier {
    val id=ThemeRuntimeRP177.id
    val base=Bg;val accent=Accent
    return background(base).drawWithCache {
        val wash=Brush.radialGradient(listOf(accent.copy(alpha=.16f),Color.Transparent),center=Offset(size.width*.9f,size.height*.15f),radius=size.maxDimension*.85f)
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
                drawPath(ornament,accent.copy(alpha=if(id=="venom") .18f else .045f),style=Stroke(if(id=="venom") 8.dp.toPx() else 1.dp.toPx()))
            }
        }
    }
}
