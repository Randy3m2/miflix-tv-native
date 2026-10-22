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
        EasyHomeRowRP80("comfort","Comedias para desconectar","Comfort comedies","comfort"),
        EasyHomeRowRP80("classics","Clásicos de siempre","Timeless classics","classics"),
        EasyHomeRowRP80("top_movies","Películas mejor valoradas","Top rated movies","top_movies"),
        EasyHomeRowRP80("top_series","Series mejor valoradas","Top rated series","top_series"),
        EasyHomeRowRP80("new_movies","Estrenos en cartelera","Now playing","now_playing"),
        EasyHomeRowRP80("action","Acción para esta noche","Action tonight","movie",28),
        EasyHomeRowRP80("adventure","Grandes aventuras","Great adventures","movie",12),
        EasyHomeRowRP80("comedy","Algo para reír","Time for a comedy","movie",35),
        EasyHomeRowRP80("romance","Historias de amor","Love stories","romance"),
        EasyHomeRowRP80("drama","Películas que emocionan","Moving dramas","movie",18),
        EasyHomeRowRP80("thrillers","Suspenso y emoción","Thrillers","movie",53),
        EasyHomeRowRP80("scifi","Ciencia ficción","Science fiction","movie",878),
        EasyHomeRowRP80("fantasy","Mundos de fantasía","Fantasy worlds","movie",14),
        EasyHomeRowRP80("family","Para ver en familia","Family favorites","movie",10751),
        EasyHomeRowRP80("animation","Películas animadas","Animated movies","movie",16),
        EasyHomeRowRP80("crime","Historias de crimen","Crime stories","movie",80),
        EasyHomeRowRP80("mystery","Misterios por resolver","Mysteries to solve","movie",9648),
        EasyHomeRowRP80("westerns","Películas del oeste","Westerns","movie",37),
        EasyHomeRowRP80("documentaries","Documentales","Documentaries","movie",99),
        EasyHomeRowRP80("korean","Dramas coreanos","K-dramas","kdrama"),
        EasyHomeRowRP80("crime_series","Series de crimen","Crime series","series",80),
        EasyHomeRowRP80("drama_series","Series de drama","Drama series","series",18),
        EasyHomeRowRP80("comedy_series","Series para reír","Comedy series","series",35)
    )
}
