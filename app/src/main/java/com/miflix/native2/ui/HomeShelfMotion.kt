package com.miflix.native2.ui

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.focus.FocusRequester
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs

internal val LocalHomeShelfMotion=staticCompositionLocalOf<HomeShelfMotion?> {null}

/** RP59: one owner for vertical keys, scrolling and focus. Horizontal focus stays native. */
internal class HomeShelfMotion(private val scope:CoroutineScope) {
    private val requesters=mutableMapOf<Int,FocusRequester>()
    private val focused=mutableSetOf<Int>()
    private var target:Int?=null
    private var running:Job?=null
    private var generation=0L
    private var navigating=false

    fun register(index:Int,requester:FocusRequester) {requesters[index]=requester}
    fun unregister(index:Int,requester:FocusRequester) {
        if(requesters[index]===requester) requesters.remove(index)
        focused.remove(index)
    }
    fun focusChanged(index:Int,active:Boolean) {
        if(active) focused.add(index) else focused.remove(index)
    }
    fun reset() {
        generation++;target=null;navigating=false;running?.cancel();running=null
    }
    /** Consume only vertical arrows while Home content owns focus. Top edge returns to header. */
    fun move(scroll:LazyListState,direction:Int):Boolean {
        val current=if(navigating) target else focused.firstOrNull()
        if(current==null) return false
        val next=current+direction
        if(next<0) {
            // Do not hand Up to a stale row while the first row is still approaching.
            if(navigating || 0 !in focused) {start(scroll,0,requestFocus=true);return true}
            reset();return false
        }
        if(next>=scroll.layoutInfo.totalItemsCount) return true
        start(scroll,next,requestFocus=true,direction=direction)
        return true
    }
    fun align(scroll:LazyListState,index:Int) {
        // A newly focused child must not cancel an arrow movement already in flight.
        if(navigating || (running?.isActive==true && target==index)) return
        start(scroll,index,requestFocus=false)
    }
    private fun start(scroll:LazyListState,index:Int,requestFocus:Boolean,direction:Int=0,hops:Int=0) {
        generation++
        val epoch=generation
        running?.cancel()
        target=index;navigating=requestFocus
        running=scope.launch {
            try {
                scroll.scroll {
                    var previous=0f
                    // Finite ease-out: no spring tail or repeated one-pixel alignment after each row.
                    animate(0f,1f,animationSpec=tween(210,easing=LinearOutSlowInEasing)) {fraction,_ ->
                        val layout=scroll.layoutInfo
                        val visible=layout.visibleItemsInfo
                        val row=visible.firstOrNull {it.index==index}
                        val reference=visible.firstOrNull()
                        if(reference!=null) {
                            val distance=row?.offset?.toFloat() ?: 
                                (reference.offset+(index-reference.index)*reference.size).toFloat()
                            val remaining=1f-previous
                            if(remaining>0f) scrollBy(distance*((fraction-previous)/remaining).coerceIn(0f,1f))
                        }
                        previous=fraction
                    }
                    // Correct only rounding; never perform a second animated scroll.
                    scroll.layoutInfo.visibleItemsInfo.firstOrNull {it.index==index}?.let {
                        if(abs(it.offset)<3) scrollBy(it.offset.toFloat())
                    }
                }
                if(requestFocus) {
                    // Request only the destination row after its slots are measured, never artwork.
                    repeat(3) {
                        withFrameNanos { }
                        if(epoch!=generation) return@launch
                        if(runCatching {requesters[index]?.requestFocus() ?: false}.getOrDefault(false)) return@launch
                    }
                    // An empty informational shelf has no focusable children; skip it in the same direction.
                    val next=index+direction
                    if(direction!=0 && hops<scroll.layoutInfo.totalItemsCount &&
                        next in 0 until scroll.layoutInfo.totalItemsCount && epoch==generation) {
                        start(scroll,next,requestFocus=true,direction=direction,hops=hops+1)
                    }
                }
            } finally {
                if(epoch==generation) {running=null;navigating=false}
            }
        }
    }
}
