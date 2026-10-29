package com.miflix.native2.ui
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text

@Composable
internal fun AssistantOrbRP155(onClick:()->Unit) {
    var focused by remember {mutableStateOf(false)}
    Box(Modifier.size(38.dp).onFocusChanged {focused=it.isFocused}.semantics {contentDescription="BruniO Assistant";role=Role.Button}.focusable().tvClick(onClick).clickable(onClick=onClick)
        .background(if(focused) Color.White else Color(0xff16161b),CircleShape).border(if(focused) 2.dp else 1.dp,if(focused) Color.White else Color(0xff77717f),CircleShape),contentAlignment=Alignment.Center) {
        Text("AI",color=if(focused) Color.Black else Color.White,fontSize=13.sp,fontWeight=FontWeight.Black)
        Text("✦",color=if(focused) Color.Black else Color(0xffc7b6e3),fontSize=8.sp,modifier=Modifier.align(Alignment.TopEnd).padding(top=3.dp,end=5.dp))
    }
}
