package com.miflix.native2.ui

import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.miflix.native2.player.hasHeadphoneOutput

/** Recreate the AudioTrack after a route change; retain the media, position and play intent. */
@Composable
internal fun HeadphoneAudioRouting(player: ExoPlayer, onRecover: () -> Unit = {}) {
    val context = LocalContext.current
    val recovery = rememberUpdatedState(onRecover)
    DisposableEffect(player) {
        val manager = context.getSystemService(AudioManager::class.java)
        val handler = Handler(Looper.getMainLooper())
        val original = player.trackSelectionParameters.maxAudioChannelCount
        var headphones = hasHeadphoneOutput(context)
        var retries = 0
        var pending: Runnable? = null
        var disposed = false
        fun applyLimit() {
            val limit = if (headphones) 2 else original
            if (player.trackSelectionParameters.maxAudioChannelCount != limit)
                player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                    .setMaxAudioChannelCount(limit).build()
        }
        fun scheduleRestart() {
            if (pending != null || player.mediaItemCount == 0) return
            val position = player.currentPosition.coerceAtLeast(0)
            val playing = player.playWhenReady
            val mediaIndex = player.currentMediaItemIndex
            val task = Runnable {
                pending = null
                if (!disposed) {
                    // stop resets the old renderer/sink. prepare negotiates PCM against the new route.
                    player.stop()
                    applyLimit()
                    player.seekTo(mediaIndex, position)
                    recovery.value()
                    player.prepare()
                    player.playWhenReady = playing
                }
            }
            pending = task
            handler.postDelayed(task, 500)
        }
        val callback = object : AudioDeviceCallback() {
            fun update() {
                val next = hasHeadphoneOutput(context)
                if (next != headphones) {
                    headphones = next
                    retries = 0
                    scheduleRestart()
                }
            }
            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) = update()
            override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) = update()
        }
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                if (hasHeadphoneOutput(context) && retries < 1 &&
                    (error.errorCode == PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED ||
                        error.errorCode == PlaybackException.ERROR_CODE_AUDIO_TRACK_WRITE_FAILED)) {
                    retries++ // Never retry unrelated errors or loop forever on unsupported hardware.
                    headphones = true
                    scheduleRestart()
                }
            }
        }
        applyLimit()
        player.addListener(listener)
        manager.registerAudioDeviceCallback(callback, handler)
        onDispose {
            disposed = true
            pending?.let { handler.removeCallbacks(it) }
            player.removeListener(listener)
            manager.unregisterAudioDeviceCallback(callback)
        }
    }
}
