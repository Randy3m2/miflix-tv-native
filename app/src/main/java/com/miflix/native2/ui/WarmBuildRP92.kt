package com.miflix.native2.ui

import android.content.Context
import android.os.Process
import android.os.Trace
import androidx.media3.exoplayer.ExoPlayer
import com.miflix.native2.data.LocalCrashRP87
import com.miflix.native2.data.WarmConstructionRP92
import com.miflix.native2.player.SubtitleTiming
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors

/** One low-priority constructor; PlayerView and all player operations remain on Main. */
private object WarmBuilderRP92 {
    private val dispatcher=Executors.newSingleThreadExecutor {task ->
        Thread({
            runCatching {Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)}
            task.run()
        },"BruniO-Prepare").apply {isDaemon=true}
    }.asCoroutineDispatcher()
    val engine=WarmConstructionRP92(dispatcher,Dispatchers.Main.immediate)
}

internal suspend fun createWarmPlaybackRP92(context:Context,state:AppState,request:PlayerRequest,
    timing:SubtitleTiming,gate:WarmLoadControlRP91,valid:()->Boolean):ExoPlayer {
    val prefs=playbackPreferencesRP92(state,request)
    return WarmBuilderRP92.engine.run(
        build={
            Trace.beginSection("BruniO.WarmBuild")
            try {
                val setup=playbackSetupRP92(context,prefs,request,timing,gate)
                setup.builder.build() to setup
            } finally {Trace.endSection()}
        },
        valid=valid,
        configure={player,setup ->
            Trace.beginSection("BruniO.WarmConfigure")
            try {configurePlaybackRP92(player,setup)} finally {Trace.endSection()}
        },
        release={player ->try {player.release()} catch(e:Exception) {LocalCrashRP87.record(context,e)}}
    )
}
