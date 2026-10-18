package com.miflix.native2.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import org.json.JSONObject

internal data class LocalListPreviewRC104(val id:String,val name:String,val count:Int,val covers:List<String>)

/** Uses artwork URLs already saved with each title; no catalog lookup or cloud write. */
internal fun localListPreviewRC104(row:JSONObject):LocalListPreviewRC104 {
    val items=row.optJSONArray("items")
    val covers=linkedSetOf<String>()
    if(items!=null) for(i in 0 until items.length()) {
        val item=items.optJSONObject(i) ?: continue
        val poster=item.optString("poster").trim()
        val image=poster.takeIf {it.isNotBlank() && it!="null"} ?: item.optString("backdrop").trim()
        if(image.isNotBlank() && image!="null") covers.add(image)
        if(covers.size==4) break
    }
    return LocalListPreviewRC104(row.optString("id"),row.optString("name"),items?.length() ?: 0,covers.toList())
}

@Composable
internal fun LocalListCardRC104(preview:LocalListPreviewRC104,modifier:Modifier=Modifier,onClick:(()->Unit)?=null) {
    var focused by remember {mutableStateOf(false)}
    val source=remember {MutableInteractionSource()}
    val hovered by source.collectIsHoveredAsState()
    val active=focused || hovered
    val scale by animateFloatAsState(if(active) 1.025f else 1f,label="list-preview-focus")
    val shape=RoundedCornerShape(22.dp)
    Box(modifier.height(208.dp).graphicsLayer {scaleX=scale;scaleY=scale}.onFocusChanged {focused=it.isFocused}
        .clip(shape).background(Panel)
        .then(if(onClick!=null) Modifier.hoverable(source).clickable(role=Role.Button,onClick=onClick) else Modifier)) {
        if(preview.covers.isNotEmpty()) Row(Modifier.fillMaxSize(),horizontalArrangement=Arrangement.spacedBy(2.dp)) {
            preview.covers.forEach {cover ->Box(Modifier.weight(1f).fillMaxHeight().background(Panel)) {
                AsyncImage(cover,contentDescription=null,contentScale=ContentScale.Crop,modifier=Modifier.fillMaxSize())
            }}
        } else Box(Modifier.fillMaxSize().padding(bottom=42.dp),contentAlignment=Alignment.Center) {SettingsIconRC103("lists")}
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha=.08f),Color.Black.copy(alpha=.22f),Bg.copy(alpha=.97f)))))
        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(18.dp),verticalArrangement=Arrangement.spacedBy(7.dp)) {
            Text(tr("TU COLECCIÓN","YOUR COLLECTION"),color=Accent,fontSize=10.sp,fontWeight=FontWeight.Bold)
            Text(preview.name,color=Color.White,fontSize=21.sp,lineHeight=25.sp,fontWeight=FontWeight.Bold,maxLines=2,overflow=TextOverflow.Ellipsis)
            Text(if(preview.count==0) tr("Lista vacía · Añade tus favoritos","Empty list · Add your favorites") else "${preview.count} "+tr("títulos","titles"),color=Muted,fontSize=12.sp)
        }
        Box(Modifier.fillMaxSize().border(if(active) 2.dp else 1.dp,if(active) Color.White else Color.White.copy(alpha=.16f),shape))
    }
}

@Composable
private fun CreateListCardRC104(modifier:Modifier=Modifier,onClick:()->Unit) {
    var focused by remember {mutableStateOf(false)}
    val source=remember {MutableInteractionSource()}
    val hovered by source.collectIsHoveredAsState()
    val shape=RoundedCornerShape(22.dp)
    // Home can constrain the shelf height below 208dp. Keep the primary action centered
    // and omit secondary copy when there is insufficient room, instead of clipping it.
    BoxWithConstraints(modifier.height(208.dp).onFocusChanged {focused=it.isFocused}.clip(shape)
        .background(Panel).border(if(focused || hovered) 2.dp else 1.dp,if(focused || hovered) Color.White else PanelBorder,shape)
        .hoverable(source).clickable(role=Role.Button,onClick=onClick),contentAlignment=Alignment.Center) {
        val showSubtitle=maxHeight>=132.dp
        Row(Modifier.fillMaxWidth().padding(horizontal=18.dp,vertical=12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(38.dp).background(Color.White.copy(alpha=.08f),RoundedCornerShape(12.dp)),contentAlignment=Alignment.Center) {
                Text("+",color=Color.White,fontSize=27.sp,lineHeight=30.sp)
            }
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(7.dp)) {
                Text(tr("Crear lista","Create list"),color=Color.White,fontSize=20.sp,lineHeight=25.sp,fontWeight=FontWeight.Bold,maxLines=1,overflow=TextOverflow.Ellipsis)
                if(showSubtitle) Text(tr("Películas y series, a tu manera.","Movies and series, your way."),color=Muted,fontSize=12.sp,lineHeight=17.sp,maxLines=2,overflow=TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
internal fun LocalListsHomeRC104(state:AppState,onOpen:(String?)->Unit) {
    val extras=state.localExtras;val owner=state.localExtrasScope()
    val previews=remember(owner,extras.revision) {extras.lists(owner).map(::localListPreviewRC104)}
    Column(Modifier.fillMaxWidth().padding(vertical=20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        Column(Modifier.padding(horizontal=40.dp),verticalArrangement=Arrangement.spacedBy(5.dp)) {
            Text(tr("HECHAS POR TI","CURATED BY YOU"),color=Accent,fontSize=11.sp,fontWeight=FontWeight.Bold)
            Text(tr("Tus listas, tus historias","Your lists, your stories"),color=Color.White,fontSize=28.sp,fontWeight=FontWeight.Bold)
            Text(tr("Tus colecciones con un vistazo a lo que guardaste.","Your collections, with a peek at what you saved."),color=Muted,fontSize=14.sp)
        }
        LazyRow(Modifier.fillMaxWidth().focusGroup(),horizontalArrangement=Arrangement.spacedBy(18.dp),contentPadding=PaddingValues(horizontal=40.dp,vertical=10.dp)) {
            items(previews,key={it.id}) {preview ->
                LocalListCardRC104(preview,Modifier.width(304.dp).then(homeReturnFocus("local-list:${preview.id}"))) {onOpen(preview.id)}
            }
            item(key="create-local-list") {CreateListCardRC104(Modifier.width(284.dp).then(homeReturnFocus("local-list:create"))) {onOpen(null)}}
        }
    }
}

@Composable
internal fun LocalListsPanelRC104(title:String,close:()->Unit,content:@Composable ColumnScope.()->Unit) {
    Dialog(onDismissRequest=close,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        BoxWithConstraints(Modifier.fillMaxSize().padding(24.dp),contentAlignment=Alignment.Center) {
            val first=remember {FocusRequester()}
            LaunchedEffect(Unit) {withFrameNanos {};runCatching {first.requestFocus()}}
            Column(Modifier.widthIn(max=900.dp).fillMaxWidth().heightIn(max=maxHeight).clip(RoundedCornerShape(26.dp))
                .background(Bg)
                .border(1.dp,PanelBorder,RoundedCornerShape(26.dp)).padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(18.dp)) {
                    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)) {
                        Text(tr("BRUNIO · TUS COLECCIONES","BRUNIO · YOUR COLLECTIONS"),color=Accent,fontSize=10.sp,fontWeight=FontWeight.Bold)
                        Text(title,color=Color.White,fontSize=26.sp,lineHeight=31.sp,fontWeight=FontWeight.Bold,maxLines=2,overflow=TextOverflow.Ellipsis)
                        Text(tr("Listas de este perfil, guardadas en esta TV.","This profile's lists, saved on this TV."),color=Muted,fontSize=13.sp)
                    }
                    FocusButton(tr("Cerrar","Close"),modifier=Modifier.focusRequester(first)) {close()}
                }
                Column(Modifier.weight(1f,fill=false).fillMaxWidth().verticalScroll(rememberScrollState()).focusGroup(),verticalArrangement=Arrangement.spacedBy(14.dp),content=content)
            }
        }
    }
}

@Composable
internal fun LocalListTitleRC104(item:com.miflix.native2.model.MediaSummary,onOpen:()->Unit,onRemove:()->Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Panel).padding(14.dp),
        verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(18.dp)) {
        Box(Modifier.width(66.dp).height(98.dp).clip(RoundedCornerShape(10.dp)).background(Panel),contentAlignment=Alignment.Center) {
            AsyncImage(item.poster ?: item.backdrop,contentDescription=null,contentScale=ContentScale.Crop,modifier=Modifier.fillMaxSize())
        }
        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(7.dp)) {
            Text(item.title,color=Color.White,fontSize=18.sp,fontWeight=FontWeight.Bold,maxLines=2,overflow=TextOverflow.Ellipsis)
            Text(listOf(if(item.type=="movie") tr("Película","Movie") else tr("Serie","Series"),item.year).filter {it.isNotBlank()}.joinToString(" · "),color=Muted,fontSize=13.sp)
        }
        Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
            FocusButton(tr("Ver título","View title")) {onOpen()}
            FocusButton(tr("Quitar de la lista","Remove from list"),destructive=true) {onRemove()}
        }
    }
}
