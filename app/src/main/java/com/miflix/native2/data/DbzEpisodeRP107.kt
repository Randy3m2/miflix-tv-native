package com.miflix.native2.data

import com.miflix.native2.model.EpisodeSummary

/** Translate at the provider boundary; stored progress keeps its original identity. */
internal object DbzEpisodeRP107 {
    fun enabled(type:String,tmdbId:Int)=type=="series" && tmdbId==12971
    fun coordinates(season:Int,episode:Int,rows:List<EpisodeSummary>):Pair<Int,Int> {
        require(season>0 && episode>0) {"Invalid Dragon Ball Z episode"}
        val row=rows.firstOrNull {it.season==season && it.episode==episode}
        val absolute=row?.absoluteEpisodeRP102?.takeIf {it in 1..291}
            // Also accept a previously imported S1:absolute identity, without adding an offset.
            ?: episode.takeIf {row==null && season==1 && episode in 1..291}
        require(absolute!=null) {"Dragon Ball Z episode mapping unavailable; reload its episode list"}
        return 1 to absolute
    }
}
