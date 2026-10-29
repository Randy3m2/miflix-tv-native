package com.miflix.native2.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.miflix.native2.data.AssistantPickRP150
import kotlinx.coroutines.CancellationException

internal data class AssistantIdeaRP152(val id:String,val es:String,val en:String,val queryEs:String,val queryEn:String,val art:String,val cloud:String) {
    fun title()=tr(es,en)
    fun query()=tr(queryEs,queryEn)
}
internal val AssistantIdeasRP152=listOf(
    AssistantIdeaRP152("time","Un salto en el tiempo","A twist in time","Películas de viajes en el tiempo","Time travel movies","mindbending","tmdb:movie:105"),
    AssistantIdeaRP152("horror","Noche de escalofríos","A little midnight terror","Terror de los 90","90s horror","halloween","tmdb:movie:4232"),
    AssistantIdeaRP152("scifi","Acción de otro mundo","Out-of-this-world action","Ciencia ficción con acción menos de 2 horas en español","Sci-fi action under 2 hours in Spanish","adrenaline","tmdb:movie:603"),
    AssistantIdeaRP152("twist","No verás venir el final","You won't see it coming","Mejores finales inesperados","Thrillers with plot twists","twist","tmdb:movie:745"),
    AssistantIdeaRP152("comedy","Desconecta y ríe","Switch off. Laugh out loud.","Comedia menos de 100 minutos","Comedy under 100 minutes","happy","tmdb:movie:620"),
    AssistantIdeaRP152("date","Una noche para dos","Just the two of you","Romance menos de 2 horas","Romance under 2 hours","romantic","tmdb:movie:11036"),
    AssistantIdeaRP152("anime","Animación con adrenalina","Animation. Maximum energy.","Animación con acción","Animation with action","family","tmdb:movie:129"),
    AssistantIdeaRP152("docs","Algo que te sorprenda","Feed your curiosity","Documental menos de 90 minutos","Documentary under 90 minutes","documentary","tmdb:movie:1667")
)

/** Fixed geometry: remote focus changes the border and tint without shifting adjacent controls. */
@Composable
internal fun AssistantActionRP152(label:String,icon:String?=null,selected:Boolean=false,modifier:Modifier=Modifier,onClick:()->Unit) {
    var focused by remember {mutableStateOf(false)}
    val shape=RoundedCornerShape(14.dp)
    Row(modifier.heightIn(min=42.dp).onFocusChanged {focused=it.isFocused}
        .semantics {contentDescription=label;role=Role.Button}
        .clip(shape).background(if(focused) Color(0xfff0ebff) else if(selected) Color(0xff5e4989) else Color.White.copy(alpha=.055f))
        .border(if(focused) 2.dp else 1.dp,if(focused) Color.White else if(selected) Color(0xffbf9dff) else Color.White.copy(alpha=.12f),shape)
        .tvClick(onClick).clickable(onClick=onClick).padding(horizontal=15.dp,vertical=10.dp),
        horizontalArrangement=Arrangement.spacedBy(8.dp,Alignment.CenterHorizontally),verticalAlignment=Alignment.CenterVertically) {
        if(icon!=null) MobileIconRP133(icon,tint=if(focused) Color(0xff251d3c) else Color.White)
        Text(label,color=if(focused) Color(0xff251d3c) else Color.White,fontSize=13.sp,fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis)
    }
}

@Composable
internal fun AssistantIdeaCardRP152(state:AppState,idea:AssistantIdeaRP152,image:String?,onClick:()->Unit) {
    var art by remember(idea.id,image) {mutableStateOf(image)}
    var focused by remember {mutableStateOf(false)}
    LaunchedEffect(idea.id,image) {if(art.isNullOrBlank()) try {val media=state.tmdb.byCloudId(idea.cloud);art=media?.backdrop ?: media?.poster} catch(e:CancellationException) {throw e} catch(_:Exception) {}}
    val shape=RoundedCornerShape(18.dp)
    Box(Modifier.width(216.dp).height(132.dp).onFocusChanged {focused=it.isFocused}
        .semantics {contentDescription=idea.title();role=Role.Button}.clip(shape)
        .background(Brush.linearGradient(listOf(Color(0xff4c3a71),Color(0xff172738))))
        .border(if(focused) 3.dp else 1.dp,if(focused) Color.White else Color.White.copy(alpha=.12f),shape)
        .tvClick(onClick).clickable(onClick=onClick)) {
        if(!art.isNullOrBlank()) AsyncImage(art,null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha=.08f),Color.Black.copy(alpha=.9f)))))
        Column(Modifier.align(Alignment.BottomStart).padding(15.dp),verticalArrangement=Arrangement.spacedBy(5.dp)) {
            Text(idea.title(),color=Color.White,fontSize=17.sp,fontWeight=FontWeight.Bold,maxLines=2)
            Text(tr("DESCUBRIR  ↗","DISCOVER  ↗"),color=Color(0xffd1baff),fontSize=9.sp,fontWeight=FontWeight.Bold)
        }
    }
}

@Composable
internal fun AssistantPosterRP152(pick:AssistantPickRP150,modifier:Modifier=Modifier,details:()->Unit,play:(()->Unit)?) {
    var focused by remember {mutableStateOf(false)}
    val shape=RoundedCornerShape(16.dp)
    Column(modifier,verticalArrangement=Arrangement.spacedBy(7.dp)) {
        Box(Modifier.fillMaxWidth().heightIn(max=210.dp).aspectRatio(2f/3f).onFocusChanged {focused=it.isFocused}
            .semantics {contentDescription=pick.item.title;role=Role.Button}.clip(shape).background(Panel)
            .border(if(focused) 3.dp else 1.dp,if(focused) Color.White else Color.White.copy(alpha=.12f),shape)
            .tvClick(details).clickable(onClick=details)) {
            AsyncImage(pick.item.poster?.takeIf {it.isNotBlank()} ?: pick.item.backdrop,pick.item.title,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
            Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(70.dp).background(Brush.verticalGradient(listOf(Color.Transparent,Color.Black.copy(alpha=.9f)))))
            Text(if(pick.minutes<=0) pick.item.year else "${pick.minutes} min"+if(pick.resume) tr(" · para terminar"," · left") else "",modifier=Modifier.align(Alignment.BottomStart).padding(12.dp),color=Color.White,fontSize=12.sp,fontWeight=FontWeight.Bold)
        }
        Text(pick.item.title,color=Color.White,fontSize=14.sp,lineHeight=18.sp,fontWeight=FontWeight.SemiBold,minLines=2)
        pick.episode?.let {Text("S${it.season} · E${it.episode}",color=Muted,fontSize=12.sp)}
        if(play!=null) AssistantActionRP152(if(pick.resume) tr("Continuar","Resume") else tr("Ver episodio","Watch episode"),"play",modifier=Modifier.fillMaxWidth(),onClick=play)
    }
}
