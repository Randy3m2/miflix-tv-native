package com.miflix.native2.data

import com.miflix.native2.model.MediaSummary
import kotlin.math.abs

internal object RandomMovieSelector {
    /** A uniform random choice from a taste/score shortlist, excluding finished/disliked titles. */
    fun pool(items: List<MediaSummary>,preferred: Set<String>,target: Double?,excluded: Set<String>,recent: Set<String>): List<MediaSummary> {
        val available=items.distinctBy { it.cloudId }.filter { it.type=="movie" && it.cloudId !in excluded && it.rating>0 }
        val fresh=available.filter { it.cloudId !in recent }.ifEmpty { available }
        val related=fresh.filter { it.cloudId in preferred }.ifEmpty { fresh }
        if(target==null) return related.filter { it.rating>=6.5 }.ifEmpty { related }.take(40)
        val close=related.filter { abs(it.rating-target)<=1.0 }
        return close.ifEmpty { related.sortedBy { abs(it.rating-target) }.take(12) }.take(40)
    }
}
