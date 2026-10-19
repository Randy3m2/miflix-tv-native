package com.miflix.native2.data

import com.miflix.native2.model.PlaybackProgress

internal object ContinueWatchingPolicyRP10 {
    private val titleId=Regex("^tmdb:(movie|series):\\d+$")
    fun eligible(row:PlaybackProgress):Boolean =
        row.duration>0 && row.position>=5000L && row.percent in 0.0..95.0
    fun ids(progress:Map<String,PlaybackProgress>):List<String> =
        progress.entries.filter {(id,row) ->titleId.matches(id) && eligible(row)}
            .sortedByDescending {it.value.updatedAt}.take(10).map {it.key}
}
