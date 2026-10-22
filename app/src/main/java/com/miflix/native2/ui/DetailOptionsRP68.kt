package com.miflix.native2.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage

internal data class DetailOptionRP68(val title:String,val icon:String,val action:()->Unit)

/** Local-only title actions. All playback, queue and profile behavior stays in Screens. */
@Composable
internal fun DetailOptionsPanelRP68(title:String,image:String?,playback:List<DetailOptionRP68>,collection:List<DetailOptionRP68>,close:()->Unit) {
    val first=remember {FocusRequester()}
    LaunchedEffect(Unit) {withFrameNanos { };runCatching {first.requestFocus()}}
    Dialog(onDismissRequest=close,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal=28.dp,vertical=20.dp),contentAlignment=Alignment.Center) {
            val panelHeight=(maxHeight-4.dp).coerceAtMost(490.dp)
            Column(Modifier.consumeOpeningPress().widthIn(max=720.dp).fillMaxWidth().heightIn(max=panelHeight)
                .clip(RoundedCornerShape(22.dp)).background(Color(0xFF121212))
                .border(1.dp,Color(0xFF343434),RoundedCornerShape(22.dp)).padding(22.dp)) {
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                    AsyncImage(image,null,Modifier.size(108.dp,66.dp).clip(RoundedCornerShape(9.dp)).background(Color(0xFF242424)),contentScale=ContentScale.Crop)
                    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)) {
                        Text(tr("Más opciones","More options"),fontSize=24.sp,fontWeight=FontWeight.Bold,color=Color.White)
                        Text(title,fontSize=15.sp,color=Color(0xFFB5B5B5),maxLines=1,overflow=TextOverflow.Ellipsis)
                    }
                    DetailActionRP68(DetailOptionRP68(tr("Cerrar","Close"),"close",close),Modifier.width(92.dp))
                }
                Spacer(Modifier.height(20.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF303030)))
                Spacer(Modifier.height(18.dp))
                // One bounded scroll container keeps every action reachable on smaller TVs.
                Row(Modifier.fillMaxWidth().weight(1f,fill=false).verticalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                    DetailActionGroupRP68(tr("REPRODUCCIÓN","PLAYBACK"),playback,Modifier.weight(1f),first)
                    DetailActionGroupRP68(tr("TU CONTENIDO","YOUR COLLECTION"),collection,Modifier.weight(1f),null)
                }
            }
        }
    }
}

@Composable
private fun DetailActionGroupRP68(title:String,actions:List<DetailOptionRP68>,modifier:Modifier,first:FocusRequester?) {
    Column(modifier,verticalArrangement=Arrangement.spacedBy(7.dp)) {
        Text(title,color=Color(0xFF999999),fontSize=11.sp,fontWeight=FontWeight.SemiBold,letterSpacing=1.sp,modifier=Modifier.padding(start=12.dp,bottom=5.dp))
        actions.forEachIndexed {index,option -> DetailActionRP68(option,Modifier.fillMaxWidth().then(if(index==0&&first!=null)Modifier.focusRequester(first) else Modifier))}
    }
}

@Composable
private fun DetailActionRP68(option:DetailOptionRP68,modifier:Modifier=Modifier) {
    var focused by remember {mutableStateOf(false)}
    val selected=actionFocusVisibleRP64(focused)
    val ink=if(selected)Color(0xFF101010) else Color(0xFFEAEAEA)
    Row(modifier.heightIn(min=58.dp).clip(RoundedCornerShape(10.dp))
        .background(if(selected)Color.White else Color(0xFF1D1D1D))
        .onFocusChanged {focused=it.isFocused}.tvClick(option.action).focusable().clickable(onClick=option.action)
        .padding(horizontal=13.dp,vertical=13.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
        DetailIconRP68(option.icon,ink)
        Text(option.title,color=ink,fontSize=15.sp,fontWeight=if(selected)FontWeight.SemiBold else FontWeight.Medium,maxLines=2,overflow=TextOverflow.Ellipsis,lineHeight=20.sp)
    }
}

@Composable
private fun DetailIconRP68(kind:String,color:Color) {
    Canvas(Modifier.size(20.dp)) {
        val w=size.width;val h=size.height;val stroke=Stroke(1.7.dp.toPx())
        fun line(a:Float,b:Float,c:Float,d:Float)=drawLine(color,Offset(w*a,h*b),Offset(w*c,h*d),1.7.dp.toPx())
        when(kind) {
            "play" -> drawPath(Path().apply {moveTo(w*.28f,h*.16f);lineTo(w*.82f,h*.5f);lineTo(w*.28f,h*.84f);close()},color)
            "check" -> {line(.17f,.53f,.4f,.77f);line(.4f,.77f,.85f,.22f)}
            "close" -> {line(.24f,.24f,.76f,.76f);line(.76f,.24f,.24f,.76f)}
            "link" -> {drawCircle(color,w*.24f,Offset(w*.35f,h*.38f),style=stroke);drawCircle(color,w*.24f,Offset(w*.65f,h*.62f),style=stroke);line(.37f,.4f,.63f,.6f)}
            "share" -> {line(.28f,.5f,.74f,.22f);line(.28f,.5f,.74f,.78f);listOf(Offset(w*.22f,h*.5f),Offset(w*.78f,h*.2f),Offset(w*.78f,h*.8f)).forEach {drawCircle(color,w*.12f,it)}}
            "hide" -> {drawCircle(color,w*.35f,center,style=stroke);line(.25f,.75f,.75f,.25f)}
            else -> {line(.14f,.25f,.86f,.25f);line(.14f,.5f,.7f,.5f);line(.14f,.75f,.58f,.75f);if(kind=="queue"){line(.8f,.62f,.8f,.92f);line(.65f,.77f,.95f,.77f)}}
        }
    }
}
