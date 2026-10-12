package com.miflix.native2.player

import android.content.Context
import android.os.Looper
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.text.TextOutput

/** Positive delay means later subtitles. Audio/video clocks are never changed. */
class SubtitleTiming {
    @Volatile var delayMs: Long = 0
    fun positionUs(position: Long): Long = position - delayMs.coerceIn(-30000L,30000L)*1000L
}

@UnstableApi
class TimedTextRenderers(context: Context, private val timing: SubtitleTiming) : DefaultRenderersFactory(context) {
    override fun buildTextRenderers(context: Context, output: TextOutput, outputLooper: Looper,
        extensionRendererMode: Int, out: ArrayList<Renderer>) {
        val text=ArrayList<Renderer>()
        super.buildTextRenderers(context,output,outputLooper,extensionRendererMode,text)
        text.forEach { out.add(TimedTextRenderer(it,timing)) }
    }
}

@UnstableApi
private class TimedTextRenderer(private val wrapped: Renderer, private val timing: SubtitleTiming) : Renderer by wrapped {
    override fun render(positionUs: Long, elapsedRealtimeUs: Long) {
        wrapped.render(timing.positionUs(positionUs),elapsedRealtimeUs)
    }
    override fun getDurationToProgressUs(positionUs: Long, elapsedRealtimeUs: Long): Long =
        wrapped.getDurationToProgressUs(timing.positionUs(positionUs),elapsedRealtimeUs)
}
