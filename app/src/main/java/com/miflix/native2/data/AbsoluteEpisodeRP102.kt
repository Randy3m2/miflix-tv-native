package com.miflix.native2.data

import com.miflix.native2.model.EpisodeSummary

/** Presentation override only. Canonical season/episode coordinates remain intact. */
internal object AbsoluteEpisodeRP102 {
    fun count(showId:Int):Int?=when(showId) {12971 ->291;46260 ->220;31910 ->500;else ->null}
    fun enabled(showId:Int)=count(showId)!=null
    fun flatten(showId:Int,rows:List<EpisodeSummary>):List<EpisodeSummary> {
        val expected=count(showId) ?: return rows
        val ordered=rows.filter {it.season>0 && it.episode>0}.sortedWith(compareBy<EpisodeSummary> {it.season}.thenBy {it.episode})
        require(ordered.size==expected && ordered.map {it.season to it.episode}.distinct().size==expected) {"Incomplete anime episode metadata; retry loading episodes"}
        require(ordered.groupBy {it.season}.values.all {season ->season.map {it.episode}==(1..season.size).toList()}) {"Anime episode numbering contains gaps"}
        return ordered.mapIndexed {index,row ->row.copy(absoluteEpisodeRP102=index+1)}
    }
}
