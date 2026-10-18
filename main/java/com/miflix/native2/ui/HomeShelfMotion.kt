package com.miflix.native2.ui

import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.animateTo
import androidx.compose.animation.core.spring
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.staticCompositionLocalOf
import com.miflix.native2.data.HoldMotionPolicy
import kotlin.math.abs

internal val LocalHomeShelfMotion=staticCompositionLocalOf<HomeShelfMotion?> {null}

/** One shared camera for Home: a new row can inherit the current velocity. */
internal class HomeShelfMotion {
    private var carried=0f
    private var lastFrameAt=0L
    fun reset() {carried=0f;lastFrameAt=0L}
    suspend fun align(scroll:LazyListState,index:Int) {
        if(scroll.layoutInfo.visibleItemsInfo.none {it.index==index}) {
            reset()
            scroll.animateScrollToItem(index)
            return
        }
        scroll.scroll {
            val row=scroll.layoutInfo.visibleItemsInfo.firstOrNull {it.index==index} ?: return@scroll
            val distance=row.offset.toFloat()
            if(abs(distance)<.5f) {reset();return@scroll}
            val recent=android.os.SystemClock.uptimeMillis()-lastFrameAt in 0L..120L
            val motion=AnimationState(initialValue=0f,initialVelocity=HoldMotionPolicy.carriedVelocity(distance,if(recent) carried else 0f))
            var previous=0f
            motion.animateTo(distance,spring(dampingRatio=1f,stiffness=300f),sequentialAnimation=true) {
                val delta=value-previous
                val consumed=scrollBy(delta)
                previous=value
                carried=velocity
                lastFrameAt=android.os.SystemClock.uptimeMillis()
                if(abs(delta-consumed)>.5f) {reset();cancelAnimation()}
            }
            carried=0f
        }
    }
}
