package com.brunio.desktop

import java.io.Closeable
import java.awt.image.BufferedImage
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import com.sun.jna.NativeLibrary
import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormat
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormatCallback
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.RenderCallback
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.format.RV32BufferFormat

/** CPU video frames permit Compose controls to overlay video without heavyweight-window clipping. */
class VlcEngine : Closeable {
    data class Frame(val width: Int, val height: Int, val pixels: IntArray) {
        fun image(): BufferedImage = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB).also {
            it.setRGB(0, 0, width, height, pixels, 0, width)
        }
    }
    val frame = AtomicReference<Frame?>(null)
    val failed = AtomicBoolean(false)
    val finished = AtomicBoolean(false)
    private val factory: MediaPlayerFactory
    val player: uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer
    init {
        System.getenv("BRUNIO_VLC_HOME")?.let { NativeLibrary.addSearchPath("vlc", it) }
        NativeDiscovery().discover()
        factory = MediaPlayerFactory("--no-video-title-show", "--network-caching=1000")
        player = factory.mediaPlayers().newEmbeddedMediaPlayer()
        val format = object : BufferFormatCallback {
            override fun getBufferFormat(width: Int, height: Int): BufferFormat = RV32BufferFormat(width, height)
            override fun allocatedBuffers(buffers: Array<ByteBuffer>) {}
        }
        val renderer = RenderCallback { _, buffers, bufferFormat ->
            val width = bufferFormat.width; val height = bufferFormat.height
            if (width > 0 && height > 0 && width.toLong() * height <= 7680L * 4320L && buffers.isNotEmpty()) {
                val data = IntArray(width * height)
                val source = buffers[0].duplicate().order(ByteOrder.nativeOrder()).asIntBuffer()
                if (source.remaining() >= data.size) { source.get(data); frame.set(Frame(width, height, data)) }
            }
        }
        player.videoSurface().set(factory.videoSurfaces().newVideoSurface(format, renderer, true))
        player.events().addMediaPlayerEventListener(object : MediaPlayerEventAdapter() {
            override fun error(mediaPlayer: MediaPlayer) { failed.set(true) }
            override fun finished(mediaPlayer: MediaPlayer) { finished.set(true) }
        })
    }
    fun play(url: String, options: List<String> = emptyList()) {
        failed.set(false); finished.set(false); frame.set(null)
        if (!player.media().play(url, *options.toTypedArray())) failed.set(true)
    }
    fun toggle() { player.controls().pause() }
    fun seek(ms: Long) { player.controls().setTime(ms.coerceAtLeast(0)) }
    fun volume(value: Float) { player.audio().setVolume((value.coerceIn(0f, 1f) * 100).toInt()) }
    override fun close() { player.controls().stop(); player.release(); factory.release(); frame.set(null) }
}
