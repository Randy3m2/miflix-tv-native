package com.brunio.desktop

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.json.JSONObject

sealed interface DetailAction {
    data object Close:DetailAction
    data class Play(val season:Int=0,val episode:Int=0):DetailAction
    data class Episodes(val season:Int):DetailAction
    data class Download(val season:Int=0,val episode:Int=0,val fullSeason:Boolean=false):DetailAction
    data class Queue(val season:Int=0,val episode:Int=0):DetailAction
    data class Saved(val enabled:Boolean):DetailAction
    data class Rate(val score:Int):DetailAction
    data class Mark(val watched:Boolean,val season:Int=0,val episode:Int=0):DetailAction
    data class Person(val id:Int,val name:String):DetailAction
}

@Composable
fun DetailsSheet(value:JSONObject,episodes:List<JSONObject>?,onAction:(DetailAction)->Unit,canPlay:Boolean=true) {
    Dialog(onDismissRequest={onAction(DetailAction.Close)},properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Surface(Modifier.fillMaxWidth(.94f).widthIn(max=1120.dp).fillMaxHeight(.94f),shape=RoundedCornerShape(28.dp),color=Color(0xFF0B0B0D),contentColor=Color.White) {
            DetailsContent(value,episodes,onAction,canPlay)
        }
    }
}

@Composable
fun DetailsContent(value:JSONObject,episodes:List<JSONObject>?,onAction:(DetailAction)->Unit,canPlay:Boolean=true) {
    val media=value.getJSONObject("item");val details=value.getJSONObject("details")
    val seasons=details.optJSONArray("seasons")?.objects().orEmpty().filter{it.optInt("season_number")>0}
    var season by remember(media.optString("cloudId")){mutableStateOf(seasons.firstOrNull()?.optInt("season_number")?:1)}
    var rating by remember(media.optString("cloudId"),value.optInt("user_rating")){mutableStateOf(value.optInt("user_rating").takeIf{it>0}?.toFloat()?:8f)}
    val series=media.optString("type")=="series"
    LaunchedEffect(media.optString("cloudId"),season){if(series)onAction(DetailAction.Episodes(season))}
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(bottom=28.dp),verticalArrangement=Arrangement.spacedBy(24.dp)) {
        item {
            Box(Modifier.fillMaxWidth().height(320.dp)) {
                RemoteArtwork(media.optString("backdrop"),Modifier.fillMaxSize())
                Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xF00B0B0D),Color(0x440B0B0D)))))
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent,Color(0xFF0B0B0D)))))
                TextButton(onClick={onAction(DetailAction.Close)},modifier=Modifier.align(Alignment.TopEnd).padding(16.dp)){Text(ui("Cerrar"))}
                Column(Modifier.align(Alignment.BottomStart).padding(28.dp).widthIn(max=780.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
                    Text(media.optString("title"),fontSize=38.sp,maxLines=2,overflow=TextOverflow.Ellipsis)
                    val runtime=details.optInt("runtime").takeIf{it>0}?.let{" · $it min"}?:""
                    Text(media.optString("year")+runtime+" · ★ "+"%.1f".format(details.optDouble("vote_average")),color=Color.LightGray)
                    Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                        if(!series)Button(onClick={onAction(DetailAction.Play())},enabled=canPlay){Text(ui("Reproducir"))}
                        OutlinedButton(onClick={onAction(DetailAction.Saved(!value.optBoolean("saved")))}){Text(ui(if(value.optBoolean("saved"))"Quitar de mi lista" else "Añadir a mi lista"))}
                        if(!series)OutlinedButton(onClick={onAction(DetailAction.Download())}){Text(ui("Descargar"))}
                        if(!series)TextButton(onClick={onAction(DetailAction.Queue())}){Text(ui("Añadir a la cola"))}
                    }
                }
            }
        }
        item {
            Column(Modifier.padding(horizontal=28.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
                Text(media.optString("overview"),style=MaterialTheme.typography.bodyLarge)
                val directors=details.optJSONObject("credits")?.optJSONArray("crew")?.objects().orEmpty().filter{it.optString("job")=="Director"}.map{it.optString("name")}.distinct()
                if(directors.isNotEmpty())Text(ui("Dirección")+" · "+directors.joinToString(),color=Color.Gray)
                Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    Text(ui("Tu valoración"))
                    Slider(value=rating,onValueChange={rating=it},valueRange=1f..10f,steps=8,modifier=Modifier.width(180.dp))
                    Text("${rating.toInt()} / 10")
                    TextButton(onClick={onAction(DetailAction.Rate(rating.toInt()))}){Text(ui("Guardar"))}
                    if(!series) {
                        val watched=value.optJSONObject("progress")?.optJSONObject(media.getString("cloudId"))?.optDouble("percent",0.0)?.let{it>=96}?:false
                        TextButton(onClick={onAction(DetailAction.Mark(!watched))}){Text(ui(if(watched)"Marcar como no visto" else "Marcar como visto"))}
                    }
                }
            }
        }
        if(series) {
            item {
                Column(Modifier.padding(horizontal=28.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        Text(ui("Episodios"),style=MaterialTheme.typography.titleLarge,modifier=Modifier.weight(1f))
                        OutlinedButton(onClick={onAction(DetailAction.Download(season=season,fullSeason=true))}){Text(ui("Descargar temporada"))}
                    }
                    LazyRow(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                        items(seasons){row->FilterChip(selected=season==row.getInt("season_number"),onClick={season=row.getInt("season_number")},label={Text(ui("Temporada")+" ${row.getInt("season_number")}")})}
                    }
                }
            }
            if(episodes==null)item{LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal=28.dp),color=Color.White)}
            items(episodes.orEmpty(),key={it.optInt("episode_number")}) { episode ->
                val number=episode.getInt("episode_number")
                val key=media.getString("cloudId")+":s${season}e$number"
                val watched=(value.optJSONObject("progress")?.optJSONObject(key)?.optDouble("percent",0.0)?:0.0)>=96
                val future=episode.optString("air_date").takeIf{it.isNotBlank()}?.let{it>java.time.LocalDate.now().toString()}?:false
                Surface(Modifier.fillMaxWidth().padding(horizontal=28.dp),shape=RoundedCornerShape(18.dp),color=Color(0xFF18181D)) {
                    Row(Modifier.padding(16.dp),horizontalArrangement=Arrangement.spacedBy(18.dp)) {
                        val still=episode.optString("still_path").takeIf{it.isNotBlank()}?.let{"https://image.tmdb.org/t/p/w300$it"}?:media.optString("backdrop")
                        RemoteArtwork(still,Modifier.width(150.dp).aspectRatio(16f/9).clip(RoundedCornerShape(10.dp)).then(if(watched)Modifier else Modifier.blur(14.dp)))
                        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                            Text("$number. "+episode.optString("name"),style=MaterialTheme.typography.titleMedium)
                            if(watched)Text(episode.optString("overview"),maxLines=2,color=Color.Gray)
                            else Text(ui(if(future)"Próximamente" else "Detalles ocultos hasta verlo"),color=Color.Gray)
                            Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                                TextButton(onClick={onAction(DetailAction.Play(season,number))},enabled=canPlay&&!future){Text(ui("Reproducir"))}
                                TextButton(onClick={onAction(DetailAction.Download(season,number))},enabled=!future){Text(ui("Descargar"))}
                                TextButton(onClick={onAction(DetailAction.Queue(season,number))},enabled=!future){Text(ui("Cola"))}
                                TextButton(onClick={onAction(DetailAction.Mark(!watched,season,number))}){Text(ui(if(watched)"Visto" else "Marcar visto"))}
                            }
                        }
                    }
                }
            }
        }
        val cast=details.optJSONObject("credits")?.optJSONArray("cast")?.objects().orEmpty().take(20)
        if(cast.isNotEmpty())item {
            Column(verticalArrangement=Arrangement.spacedBy(18.dp)) {
                Text(ui("Reparto"),Modifier.padding(horizontal=28.dp),style=MaterialTheme.typography.titleLarge)
                LazyRow(contentPadding=PaddingValues(horizontal=28.dp),horizontalArrangement=Arrangement.spacedBy(20.dp)) {
                    items(cast,key={it.getInt("id")}){person->
                        Column(Modifier.width(92.dp).clickable{onAction(DetailAction.Person(person.getInt("id"),person.optString("name")))},horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(10.dp)) {
                            val image=person.optString("profile_path").takeIf{it.isNotBlank()}?.let{"https://image.tmdb.org/t/p/w185$it"}?:""
                            RemoteArtwork(image,Modifier.size(76.dp).clip(RoundedCornerShape(38.dp)))
                            Text(person.optString("name"),maxLines=2,style=MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}
