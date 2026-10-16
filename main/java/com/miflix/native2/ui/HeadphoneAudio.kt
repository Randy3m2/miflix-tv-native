package com.miflix.native2.ui

import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.media3.exoplayer.ExoPlayer

/** Let Media3/AudioTrack keep the audio clock. Never guess a Bluetooth offset. */
@Composable
internal fun HeadphoneAudioRouting(player: ExoPlayer) {
    val context=LocalContext.current
    DisposableEffect(player) {
        val manager=context.getSystemService(AudioManager::class.java)
        val original=player.trackSelectionParameters.maxAudioChannelCount
        fun refresh() {
            val headphones=manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).any {
                it.type==AudioDeviceInfo.TYPE_WIRED_HEADPHONES || it.type==AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                    it.type==AudioDeviceInfo.TYPE_BLUETOOTH_A2DP || it.type==AudioDeviceInfo.TYPE_USB_HEADSET ||
                    (android.os.Build.VERSION.SDK_INT>=31 && it.type==AudioDeviceInfo.TYPE_BLE_HEADSET)
            }
            val limit=if(headphones) 2 else original
            if(player.trackSelectionParameters.maxAudioChannelCount!=limit) {
                player.trackSelectionParameters=player.trackSelectionParameters.buildUpon().setMaxAudioChannelCount(limit).build()
            }
        }
        val callback=object: AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {refresh()}
            override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {refresh()}
        }
        manager.registerAudioDeviceCallback(callback,Handler(Looper.getMainLooper()))
        refresh()
        onDispose {manager.unregisterAudioDeviceCallback(callback)}
    }
}
