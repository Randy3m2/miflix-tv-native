package com.miflix.native2.ui

import androidx.compose.foundation.background
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
    Column(Modifier.fillMaxWidth().height(height+40.dp).padding(start=if(mobile) 16.dp else 40.dp),
        verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text(tr("Mis canales favoritos","Favorite live channels"),color=Color.White,
            fontWeight=FontWeight.Bold,fontSize=if(mobile) 19.sp else 22.sp)
        if(favorites.isEmpty()) {
            Text(tr("Añade canales a favoritos desde Live TV para verlos aquí.","Favorite channels in Live TV to show them here."),
                color=Muted,fontSize=13.sp)
        } else LazyRow(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp),
            contentPadding=PaddingValues(end=28.dp,bottom=8.dp)) {
            items(favorites,key={it.provider+":"+it.channel.id}) { entry ->
                val channel=entry.channel
                Box(Modifier.width(width).height(height-8.dp)
                    .background(Panel,RoundedCornerShape(16.dp))
                    .clickable(enabled=!opening) {
                        scope.launch {
                            opening=true
                            try {
                                val stream=LiveRepository().streams(state.calendarLiveManifest(entry.provider),channel).firstOrNull()
                                    ?: error(tr("Canal no disponible","Channel unavailable"))
                                val item=MediaSummary(channel.id.hashCode(),"live",channel.name,channel.description,null,channel.poster,0.0,"LIVE")
                                state.openLivePlayer(PlayerRequest(item,stream,0,0,0,true,channel.id,entry.provider,liveType=channel.type))
                            } catch(e:CancellationException) {throw e}
                              catch(e:Exception) {state.error=e.message}
                            finally {opening=false}
                        }
                    }) {
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
