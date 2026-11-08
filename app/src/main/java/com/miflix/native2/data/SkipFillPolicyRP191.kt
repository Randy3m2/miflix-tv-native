package com.miflix.native2.data

/** Playback time, not a wall-clock countdown: pauses and seeks remain truthful. */
internal object SkipFillPolicyRP191 {
    fun fraction(positionMs:Long,startMs:Long,endMs:Long):Float {
        if(startMs<0 || endMs<=startMs) return 0f
        return ((positionMs.toDouble()-startMs.toDouble())/(endMs.toDouble()-startMs.toDouble())).coerceIn(0.0,1.0).toFloat()
    }
}
