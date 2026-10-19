package com.miflix.native2.ui

internal object LoadingBackPolicyRP16 {
    const val WINDOW_MS=2200L
    fun exits(first:Long,now:Long)=first>=0 && now>=first && now-first<=WINDOW_MS
}
