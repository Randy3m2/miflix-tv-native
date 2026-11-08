package com.miflix.native2.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text

/** White fills the button itself; the label stays legible on both halves. No timer text. */
@Composable
internal fun SegmentSkipButtonRP191(label:String,progress:Float,modifier:Modifier=Modifier,onSkip:()->Unit) {
    val target=progress.takeIf {it.isFinite()}?.coerceIn(0f,1f) ?: 0f
    val fill=remember {Animatable(target)}
    var pressed by remember {mutableStateOf(false)}
    var focused by remember {mutableStateOf(false)}
    val action by rememberUpdatedState(onSkip)
    LaunchedEffect(target,pressed) {
        if(!pressed) fill.animateTo(target,tween(240,easing=LinearEasing))
    }
    LaunchedEffect(pressed) {
        if(pressed) {
            fill.animateTo(1f,tween(140,easing=LinearEasing))
            withFrameNanos { }
            action()
        }
    }
    val activate={if(!pressed) pressed=true;Unit}
    val shape=themeShapeRP181(if(ThemeRuntimeRP177.id.isBlank()) 50.dp else 16.dp)
    Box(modifier.widthIn(min=180.dp,max=420.dp).heightIn(min=48.dp)
        .onFocusChanged {focused=it.isFocused}
        .tvClick(activate).clickable(role=Role.Button,onClick=activate)
        .semantics {contentDescription=label}
        .clip(shape).background(Color(0xE6202020))
        .drawBehind {drawRect(Color.White,size=Size(size.width*fill.value,size.height))}
        .border(if(focused) 2.dp else 1.dp,if(focused) Accent else Color.White.copy(alpha=.65f),shape),
        contentAlignment=Alignment.Center) {
        Text(label,color=Color.White,fontSize=15.sp,lineHeight=20.sp,fontWeight=FontWeight.Bold,
            maxLines=1,overflow=TextOverflow.Ellipsis,modifier=Modifier.padding(horizontal=24.dp,vertical=12.dp).clearAndSetSemantics { })
        Box(Modifier.matchParentSize().drawWithContent {
            clipRect(right=size.width*fill.value) {this@drawWithContent.drawContent()}
        },contentAlignment=Alignment.Center) {
            Text(label,color=Color.Black,fontSize=15.sp,lineHeight=20.sp,fontWeight=FontWeight.Bold,
                maxLines=1,overflow=TextOverflow.Ellipsis,modifier=Modifier.padding(horizontal=24.dp,vertical=12.dp).clearAndSetSemantics { })
        }
    }
}
