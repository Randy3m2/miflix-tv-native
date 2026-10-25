package com.miflix.native2.data

/** Move the viewport only at its edges, never pin every selected row to the top. */
internal object StatsViewportRP106 {
    fun firstFor(target:Int,first:Int,last:Int):Int=when {
        target<first ->target.coerceAtLeast(0)
        target>last ->(first+target-last).coerceAtLeast(0)
        else ->first.coerceAtLeast(0)
    }
}
