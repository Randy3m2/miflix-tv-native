package com.miflix.native2.ui

import androidx.compose.runtime.*
import androidx.tv.material3.Text
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

/** Visibility is stored per profile, independently of the existing row order. */
@Composable
internal fun HomeRowVisibilityRC99(state:AppState,close:()->Unit) {
    val rows=listOf(
        "discovery-controls" to tr("Elige tu ánimo · Dazzle Me","Pick your vibe · Dazzle Me"),
        "discovery-picks" to tr("Tu ánimo · Tu selección","Your Mood · Your Picks"),
        "continue" to tr("Seguir viendo","Continue Watching"),
        "streaming" to tr("Plataformas","Streaming"),
        "genres_home" to tr("Explorar por género","Browse by genre"),
        "for_you" to tr("Para ti","For you"),
        "because_you_watched" to tr("Porque viste","Because you watched"),
        "ranked_movies" to tr("Top 10 · Películas","Top 10 · Movies"),
        "oscar_picks" to tr("Premios Oscar","Oscar picks"),
        "emmy_picks" to tr("Premios Emmy","Emmy picks"),
        "rewatch_favorites" to tr("Ojalá olvidarlas para verlas otra vez","Make me forget so I can rewatch"),
        "romance_picks" to tr("Historias de amor","Romance & love stories"),
        "korean_picks" to tr("Dramas coreanos","K-dramas"),
        "classic_picks" to tr("Clásicos de siempre","Timeless classics"),
        "franchises_home" to tr("Franquicias","Franchises"),
        "seasonal" to tr("Selección de temporada","Seasonal spotlight"),
        "movies" to tr("Películas populares","Popular movies"),
        "series" to tr("Series populares","Popular series"),
        "top" to tr("Mejor valorados","Top rated"),
        "now_playing" to tr("Estrenos en cartelera","Now playing"),
        "best_series" to tr("Series mejor valoradas","Top rated series"),
        "action" to tr("Acción para esta noche","Action tonight"),
        "scifi" to tr("Ciencia ficción","Science fiction"),
        "comedy" to tr("Algo para reír","Time for a comedy"),
        "thrillers" to tr("Suspenso y misterio","Thrillers & mystery"),
        "horror_movies" to tr("Películas de terror","Horror movies"),
        "christmas_movies" to tr("Películas navideñas","Christmas movies"),
        "coming_soon_home" to tr("Próximamente","Coming soon")
    )
    val hidden=state.hiddenHomeRows
    val order=state.homeRowOrder
    val sorted=rows.sortedBy {order.indexOf(it.first).let {i -> if(i<0) Int.MAX_VALUE else i}}
    Dialog(onDismissRequest=close) {
        val first=remember {FocusRequester()}
        LaunchedEffect(Unit) {withFrameNanos {};runCatching {first.requestFocus()}}
        Column(Modifier.width(550.dp).heightIn(max=520.dp).background(Panel,RoundedCornerShape(24.dp)).padding(24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text(tr("Filas de Home","Home rows"),color=Color.White,fontSize=24.sp)
            Text(tr("Oculta filas y vuelve a mostrarlas cuando quieras. Se guarda por perfil; conserva el orden.","Hide rows and show them again anytime. Saved per profile; keeps your row order."),color=Muted,fontSize=14.sp)
            Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                FocusButton(tr("Cerrar","Close"),modifier=Modifier.focusRequester(first)) {close()}
                FocusButton(tr("Mostrar todas","Show all")) {state.restoreHomeRows()}
            }
            LazyColumn(Modifier.weight(1f,fill=false).fillMaxWidth().focusGroup(),verticalArrangement=Arrangement.spacedBy(10.dp),contentPadding=PaddingValues(vertical=8.dp)) {
                items(sorted,key={it.first}) { (key,title) ->
                    val visible=key !in hidden
                    FocusButton(title+" · "+if(visible) tr("Visible","Visible") else tr("Oculta","Hidden"),primary=visible) {
                        state.setHomeRowVisible(key,!visible)
                    }
                }
            }
        }
    }
}
