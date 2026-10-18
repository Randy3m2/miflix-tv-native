package com.miflix.native2.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import com.miflix.native2.data.SoftScrollIntegrator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs

internal val LocalHomeShelfMotion=staticCompositionLocalOf<HomeShelfMotion?> {null}

/** One running scroll loop; focus changes only retarget it, without restarting its frame clock. */
internal class HomeShelfMotion(private val scope:CoroutineScope) {
    private var target:Int?=null
    private var running:Job?=null
    private var generation=0L
    private val motion=SoftScrollIntegrator()
    fun reset() {
        generation++;target=null;running?.cancel();running=null;motion.reset()
    }
    fun align(scroll:LazyListState,index:Int) {
        target=index
        if(running?.isActive==true) return
        val epoch=generation
        running=scope.launch {
            try {
                scroll.scroll {
                    var previous=0L
                    while(target!=null && generation==epoch) {
                        val now=withFrameNanos {it}
                        val dt=if(previous==0L) 1f/60f else (now-previous)/1_000_000_000f
                        previous=now
                        val indexNow=target ?: break
                        val layout=scroll.layoutInfo
                        if(indexNow !in 0 until layout.totalItemsCount) break
                        val visible=layout.visibleItemsInfo
                        val row=visible.firstOrNull {it.index==indexNow}
                        // Home items all occupy one viewport: estimate only until the target is measured.
                        val reference=visible.firstOrNull() ?: continue
                        val distance=row?.offset?.toFloat() ?: (reference.offset+(indexNow-reference.index)*reference.size).toFloat()
                        if(abs(distance)<.6f && abs(motion.velocity())<8f) {
                            scrollBy(distance)
                            break
                        }
                        val viewport=(layout.viewportEndOffset-layout.viewportStartOffset).toFloat()
                        val delta=motion.step(distance,dt,viewport)
                        val consumed=scrollBy(delta)
                        val atBound=(delta>0 && !scroll.canScrollForward) || (delta<0 && !scroll.canScrollBackward)
                        if(atBound && abs(delta-consumed)>.6f) break
                    }
                }
            } finally {
                if(generation==epoch) {running=null;motion.reset()}
            }
        }
    }
}
