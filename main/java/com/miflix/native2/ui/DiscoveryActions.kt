package com.miflix.native2.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text

/** Static geometry keeps D-pad focus smooth; only the border and background change. */
@Composable
private fun ActionSurface(modifier:Modifier,onClick:()->Unit,content:@Composable BoxScope.()->Unit) {
    var focused by remember {mutableStateOf(false)}
    Box(modifier.onFocusChanged {focused=it.isFocused}.focusable().tvClick(onClick).clickable(onClick=onClick)
        .background(Brush.linearGradient(listOf(Accent.copy(alpha=if(focused) .28f else .11f),Color(0xFF15151C))),RoundedCornerShape(20.dp))
        .border(2.dp,if(focused) Accent else Accent.copy(alpha=.3f),RoundedCornerShape(20.dp)),content=content)
}

@Composable
internal fun LoadMoreTile(modifier:Modifier=Modifier,onClick:()->Unit) {
    ActionSurface(modifier,onClick) {
        Column(Modifier.align(Alignment.Center).padding(12.dp),horizontalAlignment=Alignment.CenterHorizontally,
            verticalArrangement=Arrangement.spacedBy(6.dp)) {
            Text("+",color=Accent,fontSize=30.sp,lineHeight=32.sp,fontWeight=FontWeight.Light)
            Text(tr("Cargar más","Load more"),color=Color.White,fontSize=15.sp,fontWeight=FontWeight.Bold)
            Text(tr("Explorar →","Explore →"),color=Muted,fontSize=11.sp)
        }
    }
}

@Composable
internal fun FeatureActionCard(title:String,subtitle:String,glyph:String="+",modifier:Modifier=Modifier,onClick:()->Unit) {
    ActionSurface(modifier,onClick) {
        Row(Modifier.padding(horizontal=18.dp,vertical=14.dp),verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(38.dp).background(Accent.copy(alpha=.17f),RoundedCornerShape(12.dp)),contentAlignment=Alignment.Center) {
                Text(glyph,color=Accent,fontSize=23.sp,fontWeight=FontWeight.Bold)
            }
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(3.dp)) {
                Text(title,color=Color.White,fontSize=16.sp,fontWeight=FontWeight.Bold,maxLines=1,overflow=TextOverflow.Ellipsis)
                Text(subtitle,color=Muted,fontSize=12.sp,maxLines=2,overflow=TextOverflow.Ellipsis)
            }
            Text("›",color=Accent,fontSize=25.sp)
        }
    }
}
