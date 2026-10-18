package com.miflix.native2.ui

import androidx.compose.runtime.*
import androidx.tv.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

/** Visibility is stored per profile, independently of the existing row order. */
@Composable
internal fun HomeRowVisibilityRC99(state:AppState,close:()->Unit) {
    val rows=listOf(
        "local_lists" to tr("Mis listas locales","My local lists"),
        "discovery-controls" to tr("Elige tu ánimo · Dazzle Me","Pick your vibe · Dazzle Me"),
        "discovery-picks" to tr("Tu ánimo · Tu selección","Your Mood · Your Picks"),
        "continue" to tr("Seguir viendo","Continue Watching"),
        "streaming" to tr("Plataformas","Streaming"),
        "novelas" to tr("Novelas que atrapan","Telenovelas to fall for"),
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
    SettingsDialogRC103(tr("Tu Home, a tu gusto","Your Home, your way"),tr("Elige las filas que quieres ver. Se guarda por perfil y conserva el orden.","Choose the rows you want to see. Saved per profile, keeping their order."),close) {
        Text("${rows.size-hidden.count {key -> rows.any {it.first==key}}} / ${rows.size} "+tr("filas visibles","rows visible"),color=Color.White,fontSize=15.sp)
        SettingsTileRC103(tr("Mostrar todas","Show all"),tr("Recupera todas las filas de este perfil.","Bring back every row for this profile."),"home") {state.restoreHomeRows()}
        sorted.forEach { (key,title) ->
            androidx.compose.runtime.key(key) {
                val visible=key !in hidden
                SettingsTileRC103(title,if(visible) tr("Visible en tu Home","Visible on your Home") else tr("Oculta de tu Home","Hidden from your Home"),"home",checked=visible) {
                    state.setHomeRowVisible(key,!visible)
                }
            }
        }
    }
}
