package com.miflix.native2.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text

internal data class HomeSection(val key:String,val content:@Composable ()->Unit)
internal val HomeRows=listOf(
    "local_lists" to "My local lists",
    "discovery-controls" to "Mood selector",
    "discovery-picks" to "Mood picks",
    "continue" to "Continue Watching",
    "streaming" to "Streaming",
    "genres_home" to "Browse by genre",
    "novelas" to "Telenovelas",
    "for_you" to "For you",
    "because_you_watched" to "Because you watched",
    "ranked_movies" to "Top 10 Movies",
    "oscar_picks" to "Oscar picks",
    "emmy_picks" to "Emmy picks",
    "rewatch_favorites" to "Make me forget",
    "romance_picks" to "Romance",
    "korean_picks" to "K-dramas",
    "classic_picks" to "Classics",
    "franchises_home" to "Franchises",
    "seasonal" to "Seasonal picks",
    "movies" to "Popular Movies",
    "series" to "Popular Series",
    "top" to "Top Rated",
    "now_playing" to "Now playing",
    "best_series" to "Top Series",
    "action" to "Action",
    "scifi" to "Science fiction",
    "comedy" to "Comedy",
    "thrillers" to "Thrillers",
    "horror_movies" to "Horror",
    "christmas_movies" to "Christmas Movies",
    "coming_soon_home" to "Coming Soon",
    "simple-actions" to "Simple Home actions",
    "discovery-retry" to "Discovery retry"
)
@Composable
internal fun HomeOrderDialog(state:AppState,close:()->Unit) {
    val default=HomeRows.map {it.first}
    var order by remember {mutableStateOf((state.homeRowOrder+default).distinct().filter {it in default})}
    PlayerSideSheet(tr("Orden de Home","Home row order"),close) {
        item {CinemaBanner("BRUNIO · HOME STUDIO",tr("Tu Home, a tu manera","Your Home, your way"),tr("Pon primero lo que más disfrutas.","Put what you love first."))}
        item {Text(tr("Mueve las filas. Los cambios se guardan solo para este perfil y dispositivo.","Move rows. Changes are saved for this profile on this device."),color=Muted)}
        item {FocusButton(tr("Restablecer orden","Reset order")) {order=default;state.saveHomeRowOrder(emptyList())}}
        items(order,key={it}) {key ->
            val index=order.indexOf(key)
            Row(Modifier.fillMaxWidth().background(Panel,RoundedCornerShape(16.dp)).padding(10.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Text("${index+1}. "+(HomeRows.firstOrNull {it.first==key}?.second ?: key),color=Color.White,modifier=Modifier.weight(1f).padding(top=12.dp))
                fun move(delta:Int) {val target=index+delta;if(target in order.indices) {val copy=order.toMutableList();java.util.Collections.swap(copy,index,target);order=copy;state.saveHomeRowOrder(order)}}
                if(index>0) FocusButton("↑",horizontalPadding=14.dp) {move(-1)}
                if(index<order.lastIndex) FocusButton("↓",horizontalPadding=14.dp) {move(1)}
            }
        }
    }
}
