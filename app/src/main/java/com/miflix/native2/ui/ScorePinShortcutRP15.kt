package com.miflix.native2.ui

/** Three separate Up presses within one second; held-key repeats never count. */
internal class ScorePinShortcutRP15 {
    private var first=-1L
    private var count=0
    fun reset() {first=-1;count=0}
    fun press(up:Boolean,repeat:Int,now:Long):Boolean {
        if(!up) {reset();return false}
        if(repeat!=0) return false
        if(first<0 || now<first || now-first>1000) {first=now;count=0}
        count++
        if(count<3) return false
        reset();return true
    }
}
