package com.miflix.native2.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.miflix.native2.model.EpisodeSummary

@Composable internal fun NextEpisodePreviewRP167(next:EpisodeSummary,poster:String?,seconds:Long,autoplay:Boolean,modifier:Modifier=Modifier,play:()->Unit,episodes:()->Unit,dismiss:()->Unit) {
 val mobile=LocalMobileBrunioRP133.current
 val shape=RoundedCornerShape(20.dp)
 Column(modifier.widthIn(max=if(mobile) 430.dp else 580.dp).background(Color(0xF20B0D13),shape).border(1.dp,Accent.copy(alpha=.7f),shape).padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
  Text(tr("A CONTINUACIÓN","UP NEXT"),color=Accent,fontSize=12.sp)
  Row(horizontalArrangement=Arrangement.spacedBy(14.dp)) {
   AsyncImage(next.still ?: poster,null,Modifier.width(if(mobile) 105.dp else 150.dp).height(92.dp).clip(RoundedCornerShape(10.dp)),contentScale=ContentScale.Crop)
   Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)) {
    Text("S${next.season} · E${next.episode}  ${next.title}",color=Color.White,fontSize=18.sp,maxLines=2,overflow=TextOverflow.Ellipsis)
    Text(next.overview.ifBlank {tr("El siguiente episodio está listo.","Your next episode is ready.")},color=Muted,fontSize=13.sp,maxLines=3,overflow=TextOverflow.Ellipsis)
   }
  }
  Text(tr("Este episodio termina en ","This episode ends in ")+"%d:%02d".format(seconds/60,seconds%60),color=Color.White,fontSize=14.sp)
  Text(if(autoplay) tr("Después continuará la reproducción automática.","Autoplay continues after the episode ends.") else tr("Reproducción automática desactivada.","Autoplay is off."),color=Muted,fontSize=12.sp)
  if(mobile) {
   MobileButtonRP133(tr("Reproducir siguiente","Play next"),"play",Modifier.fillMaxWidth(),click=play)
   Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {MobileButtonRP133(tr("Episodios","Episodes"),modifier=Modifier.weight(1f),click=episodes);MobileButtonRP133(tr("Ocultar","Hide"),modifier=Modifier.weight(1f),click=dismiss)}
  } else {
   Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {FocusButton(tr("Ver siguiente","Play next"),primary=true,onClick=play);FocusButton(tr("Episodios","Episodes"),onClick=episodes);FocusButton(tr("Ocultar","Hide"),onClick=dismiss)}
  }
 }
}
