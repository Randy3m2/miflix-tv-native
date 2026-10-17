package com.miflix.native2.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.miflix.native2.model.MediaSummary

internal val LocalCardFreshness=staticCompositionLocalOf<(MediaSummary)->String?> {{null}}
internal val LocalCardFreshnessLoad=staticCompositionLocalOf<(MediaSummary)->Unit> {{}}

@Composable
internal fun observeCardFreshness(item:MediaSummary):String? {
    val load=LocalCardFreshnessLoad.current
    LaunchedEffect(item.cloudId,item.releaseDate) {load(item)}
    return LocalCardFreshness.current(item)
}


@Composable
internal fun CardFreshnessTag(label:String,modifier:Modifier=Modifier) {
    Text(label,color=Color.White,fontSize=10.sp,lineHeight=13.sp,fontWeight=FontWeight.Bold,
        maxLines=1,overflow=TextOverflow.Ellipsis,
        modifier=modifier.background(Color(0xEF512785),RoundedCornerShape(5.dp)).padding(horizontal=7.dp,vertical=4.dp))
}
