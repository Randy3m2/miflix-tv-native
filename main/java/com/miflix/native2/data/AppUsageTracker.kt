package com.miflix.native2.data

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.os.SystemClock

/** Device-wide visible app time. Menus and playback count; background time does not.
 * A monotonic awake clock excludes deep sleep and changes to the device date/time.
 * Checkpoints bound the unrecorded interval to 30 seconds on an abrupt process kill.
 * Call onStart/onStop once per activity lifecycle, on the main thread.
 */
object AppUsageTracker {
    private var preferences: SharedPreferences? = null
    private var accumulatedMs = 0L
    private var activeSinceMs = 0L
    private var visibleActivities = 0
    private val handler = Handler(Looper.getMainLooper())
    private val checkpoint = object : Runnable {
        override fun run() {
            if (visibleActivities == 0) return
            persist()
            handler.postDelayed(this, 30_000L)
        }
    }

    private fun initialize(context: Context) {
        if (preferences != null) return
        val store = context.applicationContext.getSharedPreferences("brunio_usage_time", Context.MODE_PRIVATE)
        preferences = store
        accumulatedMs = store.getLong("total_ms", 0L).coerceAtLeast(0L)
    }

    fun start(context: Context) {
        initialize(context)
        if (visibleActivities++ == 0) {
            activeSinceMs = SystemClock.uptimeMillis()
            handler.postDelayed(checkpoint, 30_000L)
        }
    }

    fun stop() {
        if (visibleActivities == 0) return
        if (visibleActivities == 1) {
            persist()
            handler.removeCallbacks(checkpoint)
        }
        visibleActivities--
    }

    private fun persist() {
        val now = SystemClock.uptimeMillis()
        if (visibleActivities > 0) {
            accumulatedMs += (now - activeSinceMs).coerceAtLeast(0L)
            activeSinceMs = now
        }
        preferences?.edit()?.putLong("total_ms", accumulatedMs)?.apply()
    }

    fun totalMs(context: Context): Long {
        initialize(context)
        val current = if (visibleActivities > 0)
            (SystemClock.uptimeMillis() - activeSinceMs).coerceAtLeast(0L) else 0L
        return accumulatedMs + current
    }
}
