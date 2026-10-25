package com.miflix.native2.data

/** One retry per playback request, never reset by successful frames or recomposition. */
internal class SportsRetryRP108 {
    private var consumed=false
    fun take(live:Boolean,recording:Boolean,provider:String):Boolean {
        if(!live || recording || provider!="sports" || consumed) return false
        consumed=true
        return true
    }
}
