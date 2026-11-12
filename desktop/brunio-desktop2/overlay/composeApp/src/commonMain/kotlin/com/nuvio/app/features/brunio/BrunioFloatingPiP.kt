package com.nuvio.app.features.brunio

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** One independent media session retained while users browse BruniO Desktop. */
internal data class BrunioFloatingVideo(
    val generation: Long,
    val title: String,
    val sourceUrl: String,
    val streamType: String?,
    val sourceHeaders: Map<String, String>,
    val sourceResponseHeaders: Map<String, String>,
    val positionMs: Long,
    val playing: Boolean,
)

internal object BrunioFloatingPiP {
    private val _video = MutableStateFlow<BrunioFloatingVideo?>(null)
    val video: StateFlow<BrunioFloatingVideo?> = _video.asStateFlow()
    private var nextGeneration = 0L

    /** Credentials and playback URLs stay in memory; never persisted or logged. */
    fun open(
        title: String,
        sourceUrl: String,
        streamType: String?,
        headers: Map<String, String>,
        responseHeaders: Map<String, String>,
        positionMs: Long,
        playing: Boolean,
    ): Boolean {
        val url = sourceUrl.trim()
        if (url.isEmpty() || (!url.startsWith("https://") && !url.startsWith("http://"))) {
            return false // P2P, downloads and local file streams retain native PiP.
        }
        _video.value = BrunioFloatingVideo(
            generation = ++nextGeneration,
            title = title.take(120),
            sourceUrl = url,
            streamType = streamType,
            sourceHeaders = headers,
            sourceResponseHeaders = responseHeaders,
            positionMs = positionMs.coerceAtLeast(0),
            playing = playing,
        )
        return true
    }

    fun close() { _video.value = null }
}
