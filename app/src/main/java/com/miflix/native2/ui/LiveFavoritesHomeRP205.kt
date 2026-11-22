package com.miflix.native2.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.miflix.native2.data.LiveRepository
import com.miflix.native2.model.MediaSummary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** RP205: optional Home shelf, using the same saved favorites as Live TV. */
@Composable
internal fun LiveFavoritesHomeRP205(state:AppState,mobile:Boolean=false) {
    val context=LocalContext.current
    val revision=LiveLibrary.revision
    val favorites=remember(revision,state.session?.userId,state.activeProfile.id) {
        LiveLibrary.favorites(context,state)
    }
    val height=if(mobile) 130.dp else if(LocalHomeShelfPaging.current) LocalHomeCardRowHeight.current else 188.dp
    val width=if(mobile) 186.dp else 240.dp
    val scope=rememberCoroutineScope()
    var opening by remember {mutableStateOf(false)}
    val title=tr("Mis canales favoritos","Favorite live channels")
    fun open(entry:FavoriteChannel) {
        if(opening) return
        opening=true
        scope.launch {
            val channel=entry.channel
            try {
                val stream=LiveRepository().streams(state.calendarLiveManifest(entry.provider),channel).firstOrNull()
                    ?: error(tr("Canal no disponible","Channel unavailable"))
                val item=MediaSummary(channel.id.hashCode(),"live",channel.name,channel.description,null,channel.poster,0.0,"LIVE")
                state.openLivePlayer(PlayerRequest(item,stream,0,0,0,true,channel.id,entry.provider,liveType=channel.type))
            } catch(e:CancellationException) {throw e}
              catch(e:Exception) {state.error=e.message}
            finally {opening=false}
        }
    }
    if(!mobile) {
        Column(Modifier.fillMaxWidth().height(height+40.dp)) {
            Text(title,color=Color.White,fontSize=22.sp,lineHeight=27.sp,fontWeight=FontWeight.Bold,
                modifier=Modifier.padding(start=40.dp,bottom=8.dp))
            if(favorites.isEmpty()) {
                Text(tr("Añade canales a favoritos desde Live TV para verlos aquí.","Favorite channels in Live TV to show them here."),
                    modifier=Modifier.padding(horizontal=40.dp),color=Muted,fontSize=13.sp)
            } else HomeShortcutRail("live-favorites",favorites,{it.provider+":"+it.channel.type+":"+it.channel.id}) {entry,modifier ->
                val channel=entry.channel
                var focused by remember {mutableStateOf(false)}
                val click={open(entry)}
                Box(modifier.then(homeReturnFocus("live-favorites:${entry.provider}:${channel.type}:${channel.id}"))
                    .width(homePosterSlotWidth()).height(height-12.dp).padding(horizontal=6.dp)
                    .onFocusChanged {focused=it.isFocused}.tvClick(click).clickable(onClick=click)) {
                    Box(Modifier.fillMaxSize().border(2.dp,if(focused) Accent else Color.Transparent,RoundedCornerShape(18.dp))
                        .padding(3.dp).clip(RoundedCornerShape(16.dp)).background(Panel)) {
                        LoadingArtworkRP58(channel.poster,channel.name,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
                        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent,Color(0xD9050505)))))
                        Text(channel.name,color=Color.White,fontSize=12.sp,lineHeight=15.sp,fontWeight=FontWeight.Bold,maxLines=2,
                            modifier=Modifier.align(Alignment.BottomStart).padding(10.dp))
                    }
                }
            }
        }
        return
    }
    Column(Modifier.fillMaxWidth().height(height+40.dp).padding(start=16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text(title,color=Color.White,fontWeight=FontWeight.Bold,fontSize=19.sp)
        if(favorites.isEmpty()) {
            Text(tr("Añade canales a favoritos desde Live TV para verlos aquí.","Favorite channels in Live TV to show them here."),
                color=Muted,fontSize=13.sp)
        } else LazyRow(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp),
            contentPadding=PaddingValues(end=28.dp,bottom=8.dp)) {
            items(favorites,key={it.provider+":"+it.channel.id}) { entry ->
                val channel=entry.channel
                Box(Modifier.width(width).height(height-8.dp).background(Panel,RoundedCornerShape(16.dp))
                    .clickable(enabled=!opening) {open(entry)}) {
                    AsyncImage(channel.poster,channel.name,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=.38f)))
                    Column(Modifier.align(Alignment.BottomStart).padding(12.dp)) {
                        Text("★  LIVE",color=Accent,fontWeight=FontWeight.Bold,fontSize=12.sp)
                        Text(channel.name,color=Color.White,fontWeight=FontWeight.Bold,fontSize=16.sp,maxLines=2)
                    }
                }
            }
        }
    }
}
