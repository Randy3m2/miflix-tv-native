@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.miflix.native2.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.miflix.native2.model.MediaSummary

internal val LocalMobileBrunioRP133=staticCompositionLocalOf {false}
internal val MobileBgRP133=Color(0xFF080A0F)
internal val MobileTintRP133=Color(0xFF9F82F5)
internal val MobilePanelRP133=Color(0xFF141821)
private val mobilePathsRP133=mapOf(
    "home" to "M3 10L12 3L21 10V21H15V14H9V21H3Z",
    "search" to "M21 21L16 16M18 10A8 8 0 1 1 2 10A8 8 0 1 1 18 10",
    "live" to "M4 7C0 10 0 14 4 17M7 9C5 11 5 13 7 15M17 9C19 11 19 13 17 15M20 7C24 10 24 14 20 17M12 9V15",
    "friends" to "M16 21V19C16 16 13 15 9 15C5 15 2 16 2 19V21M13 7A4 4 0 1 1 5 7A4 4 0 1 1 13 7M22 21V19C22 17 21 16 18 15M17 3C21 4 21 10 17 11",
    "list" to "M5 3H19V22L12 17L5 22Z",
    "gear" to "M9 3H15L16 6L19 7L22 10L20 13L20 17L16 18L14 22L10 22L8 18L4 17L4 13L2 10L5 7L8 6ZM16 12A4 4 0 1 1 8 12A4 4 0 1 1 16 12",
    "bell" to "M5 17H19L17 13V8C17 1 7 1 7 8V13ZM10 21H14",
    "play" to "M8 4L21 12L8 20Z",
    "pause" to "M8 4V20M16 4V20",
    "back" to "M15 4L7 12L15 20",
    "close" to "M5 5L19 19M19 5L5 19",
    "plus" to "M12 4V20M4 12H20",
    "heart" to "M12 21L3 12C-2 4 7 0 12 6C17 0 26 4 21 12Z",
    "mic" to "M9 4C9 0 15 0 15 4V12C15 16 9 16 9 12ZM5 10V12C5 22 19 22 19 12V10M12 20V24",
    "link" to "M9 15L15 9M7 16L5 18C0 22 -2 14 3 11L7 7C12 2 17 8 13 12M17 8L19 6C24 2 27 10 22 13L18 17C13 22 8 16 12 12",
    "stats" to "M4 21V13H8V21ZM10 21V7H14V21ZM16 21V3H20V21Z",
    "grid" to "M3 3H10V10H3ZM14 3H21V10H14ZM3 14H10V21H3ZM14 14H21V21H14Z",
    "full" to "M3 9V3H9M15 3H21V9M21 15V21H15M9 21H3V15",
    "volume" to "M3 9H7L12 4V20L7 15H3ZM16 8C20 10 20 14 16 16M19 4C26 9 26 15 19 20",
    "mute" to "M3 9H7L12 4V20L7 15H3ZM17 8L23 16M23 8L17 16",
    "audio" to "M4 15V10C4 0 20 0 20 10V15M4 13H8V21H4ZM16 13H20V21H16Z",
    "cc" to "M3 5H21V19H3ZM10 9C5 6 5 18 10 15M18 9C13 6 13 18 18 15",
    "next" to "M4 4L16 12L4 20ZM20 4V20",
    "download" to "M12 2V16M6 10L12 16L18 10M3 17V22H21V17",
    "refresh" to "M20 6V12H14M4 18V12H10M20 12C20 2 6 0 3 9M4 12C4 22 18 24 21 15",
    "check" to "M4 12L9 17L21 5",
    "cast" to "M3 8V4H22V20H17M2 14C6 14 10 18 10 22M2 18C4 18 6 20 6 22M2 22H3",
    "chevron" to "M9 4L17 12L9 20",
    "calendar" to "M3 5H21V22H3ZM7 2V8M17 2V8M3 11H21",
    "more" to "M5 12H6M11 12H12M17 12H18"
)

@Composable
internal fun MobileIconRP133(name:String,modifier:Modifier=Modifier,tint:Color=Color.White) {
    val path=remember(name) {PathParser().parsePathString(mobilePathsRP133[name] ?: mobilePathsRP133.getValue("more")).toPath()}
    Canvas(modifier.size(22.dp)) {
        val scale=size.minDimension/26f
        with(drawContext.canvas) {save();translate((size.width-24*scale)/2,(size.height-24*scale)/2);scale(scale,scale)
            drawPath(path,Paint().apply {color=tint;style=PaintingStyle.Stroke;strokeWidth=1.7f;strokeCap=StrokeCap.Round;strokeJoin=StrokeJoin.Round})
            restore()
        }
    }
}

internal fun Modifier.mobileGlassRP133(selected:Boolean=false,radius:Dp=16.dp)=
    clip(RoundedCornerShape(radius)).background(Brush.verticalGradient(listOf(
        if(selected) Color(0xFF51436F) else Color(0xFF222733),
        if(selected) Color(0xFF242039) else Color(0xFF10141B))))
        .border(1.dp,if(selected) MobileTintRP133 else Color(0xFF353C49),RoundedCornerShape(radius))

@Composable
internal fun MobileButtonRP133(label:String,icon:String?=null,modifier:Modifier=Modifier,selected:Boolean=false,enabled:Boolean=true,click:()->Unit) {
    Row(modifier.heightIn(min=46.dp).mobileGlassRP133(selected)
        .semantics {contentDescription=label;role=Role.Button}
        .clickable(enabled=enabled,onClick=click).padding(horizontal=14.dp,vertical=10.dp),
        verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp,Alignment.CenterHorizontally)) {
        if(icon!=null) MobileIconRP133(icon,tint=if(enabled) Color.White else Muted)
        Text(label,color=if(enabled) Color.White else Muted,fontSize=14.sp,fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis)
    }
}

@Composable
internal fun MobileIconButtonRP133(icon:String,label:String,modifier:Modifier=Modifier,selected:Boolean=false,click:()->Unit) {
    Box(modifier.size(46.dp).mobileGlassRP133(selected,50.dp).semantics {contentDescription=label;role=Role.Button}
        .clickable(onClick=click),contentAlignment=Alignment.Center) {MobileIconRP133(icon,tint=if(selected) MobileTintRP133 else Color.White)}
}

@Composable
internal fun MobileFieldRP133(value:String,change:(String)->Unit,label:String,password:Boolean=false,modifier:Modifier=Modifier,keyboard:KeyboardType=KeyboardType.Text) {
    BasicTextField(value,change,modifier.fillMaxWidth().heightIn(min=48.dp).mobileGlassRP133().padding(14.dp),
        textStyle=TextStyle(color=Color.White,fontSize=16.sp),singleLine=true,cursorBrush=SolidColor(MobileTintRP133),
        visualTransformation=if(password) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions=KeyboardOptions(keyboardType=if(password) KeyboardType.Password else keyboard),
        decorationBox={field->Box {if(value.isEmpty()) Text(label,color=Muted,fontSize=15.sp);field()}})
}

@Composable
internal fun MobileSheetRP133(title:String,close:()->Unit,content:@Composable ColumnScope.()->Unit) {
    Dialog(close,properties=DialogProperties(usePlatformDefaultWidth=false,decorFitsSystemWindows=false)) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=.65f)).clickable(onClick=close),contentAlignment=Alignment.BottomCenter) {
            Column(Modifier.fillMaxWidth().fillMaxHeight(.92f).navigationBarsPadding().imePadding()
                .clip(RoundedCornerShape(topStart=24.dp,topEnd=24.dp)).background(MobileBgRP133)
                .clickable {}.padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Text(title,color=Color.White,fontSize=21.sp,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f))
                    MobileIconButtonRP133("close",tr("Cerrar","Close"),click=close)
                }
                content()
            }
        }
    }
}

@Composable
internal fun MobilePosterRP133(state:AppState,item:MediaSummary,modifier:Modifier=Modifier,landscape:Boolean=false,rank:Int?=null) {
    Column(modifier.width(if(landscape) 210.dp else 115.dp).combinedClickable(
        onClick={state.launch {state.open(item)}},onLongClick={state.titleActions(item,landscape)})) {
        Box(Modifier.fillMaxWidth().aspectRatio(if(landscape) 16f/9f else 2f/3f).clip(RoundedCornerShape(12.dp)).background(MobilePanelRP133)) {
            LoadingArtworkRP58(if(landscape) item.backdrop ?: item.poster else item.poster,item.title,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
            val progress=state.progress[item.cloudId]?.percent ?: 0.0
            if(progress>0) Box(Modifier.align(Alignment.BottomStart).fillMaxWidth((progress/100).toFloat().coerceIn(0f,1f)).height(3.dp).background(Color.White))
            rank?.let {Text(it.toString(),color=Color.White,fontSize=39.sp,fontWeight=FontWeight.Black,modifier=Modifier.align(Alignment.BottomStart).background(Color.Black.copy(alpha=.4f)).padding(horizontal=5.dp))}
        }
        Text(item.title,color=Color.White,fontSize=12.sp,maxLines=2,overflow=TextOverflow.Ellipsis,modifier=Modifier.padding(top=5.dp))
    }
}
