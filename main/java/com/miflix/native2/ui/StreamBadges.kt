package com.miflix.native2.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text

/** Small vector/text wordmarks: no GIF, bitmap decoder, network or per-frame animation. */
@Composable
internal fun TechnologyBadge(label: String,ink: Color) {
    Row(Modifier.background(ink.copy(alpha=.07f),RoundedCornerShape(4.dp)).border(.7.dp,ink.copy(alpha=.5f),RoundedCornerShape(4.dp))
        .padding(horizontal=5.dp,vertical=3.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp)) {
        if(label.startsWith("Dolby")) Canvas(Modifier.size(18.dp,11.dp)) {
            val gap=size.width*.12f;val w=(size.width-gap)/2;val h=size.height
            val left=Path().apply {moveTo(0f,0f);lineTo(0f,h);cubicTo(w,h,w,0f,0f,0f);close()}
            val right=Path().apply {moveTo(size.width,0f);lineTo(size.width,h);cubicTo(size.width-w,h,size.width-w,0f,size.width,0f);close()}
            drawPath(left,ink,style=Stroke(1.dp.toPx()));drawPath(right,ink,style=Stroke(1.dp.toPx()))
        }
        Text(if(label=="Dolby Atmos") "ATMOS" else if(label=="Dolby Vision") "VISION" else label,color=ink,fontSize=9.sp,lineHeight=11.sp,fontWeight=FontWeight.Bold,maxLines=1)
    }
}
