package com.miflix.native2.ui

import com.miflix.native2.data.TmdbRepository
import com.miflix.native2.model.MediaSummary

/** Public catalog rows only: no social, party, rating or account-service calls. */
internal data class EasyHomeRowRP80(
    val id:String,val spanish:String,val english:String,val kind:String,val genre:Int=0
) {
    suspend fun load(repo:TmdbRepository):List<MediaSummary> = when(kind) {
        "novelas" -> repo.novelaPage(1).rows
        "comfort" -> repo.comfortComedyPage(1).rows
        "la_vega" -> repo.laVegaMoviesRP85()
        "classics","kdrama","romance" -> repo.familiarCatalog(kind)
        "top_movies" -> repo.topRatedMovies()
        "top_series" -> repo.topRatedSeries()
        "now_playing" -> repo.nowPlayingMovies()
        "movie" -> repo.discoverMovieGenre(genre,pages=1,limit=20)
        "series" -> repo.discoverTvGenre(genre,pages=1,limit=20)
        else -> error("Unknown easy catalog row")
    }
}

internal object EasyHomeRowsRP80 {
    // Stable order and keys keep focus/scroll anchors when later rows finish loading.
    val rows=listOf(
        EasyHomeRowRP80("novelas","Novelas que atrapan","Telenovelas to fall for","novelas"),
        EasyHomeRowRP80("classics","Clásicos de siempre","Timeless classics","classics"),
        EasyHomeRowRP80("la_vega","Películas de La Vega","La Vega movies","la_vega")
    )
}
