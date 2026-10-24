package com.miflix.native2.player

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.ChannelMixingAudioProcessor
import androidx.media3.common.audio.ChannelMixingMatrix
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.audio.AudioOffloadSupport
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.ForwardingAudioSink

internal fun hasHeadphoneOutput(context: Context): Boolean =
    context.getSystemService(AudioManager::class.java).getDevices(AudioManager.GET_DEVICES_OUTPUTS).any {
        it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES || it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP || it.type == AudioDeviceInfo.TYPE_USB_HEADSET ||
            (android.os.Build.VERSION.SDK_INT >= 31 && it.type == AudioDeviceInfo.TYPE_BLE_HEADSET)
    }

/** Row-major mix includes the centre/dialogue channel, rather than dropping all but L/R. */
internal fun headphoneMixCoefficients(channels: Int, stereo: Boolean): FloatArray {
    val outputs = if (stereo && channels > 2) 2 else channels
    val matrix = FloatArray(channels * outputs)
    if (outputs == channels) {
        for (i in 0 until channels) matrix[i * outputs + i] = 1f
    } else {
        matrix[0] = 1f
        matrix[3] = 1f
        for (i in 2 until channels) {
            when {
                channels == 4 -> matrix[i * 2 + (i - 2)] = .7071f // Quad rear L/R.
                i == 2 || (channels == 7 && i == 6) -> {
                    matrix[i * 2] = .7071f
                    matrix[i * 2 + 1] = .7071f // Include centre/dialogue and rear centre.
                }
                channels >= 6 && i == 3 -> {
                    matrix[i * 2] = .25f
                    matrix[i * 2 + 1] = .25f
                }
                else -> matrix[i * 2 + (if (channels == 5) (i - 3) % 2 else i % 2)] = .7071f
            }
        }
        // Conservative headroom prevents clipping when multiple channels peak together.
        for (out in 0..1) {
            var gain = 0f
            for (i in 0 until channels) gain += matrix[i * 2 + out]
            if (gain > 1f) for (i in 0 until channels) matrix[i * 2 + out] /= gain
        }
    }
    return matrix
}

@UnstableApi
@Suppress("DEPRECATION")
internal fun headphoneAudioProcessor(stereoOutput: () -> Boolean): AudioProcessor {
    val mixer = ChannelMixingAudioProcessor()
    return object : AudioProcessor by mixer {
        override fun configure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
            val channels = inputAudioFormat.channelCount
            if (channels > 0) {
                val stereo = stereoOutput()
                mixer.putChannelMixingMatrix(ChannelMixingMatrix(channels,
                    if (stereo && channels > 2) 2 else channels, headphoneMixCoefficients(channels, stereo)))
            }
            return mixer.configure(inputAudioFormat)
        }
        // Kotlin interface delegation can inherit Java default methods instead of
        // forwarding them. Media3 must flush the mixer, including its pending format.
        @Suppress("OVERRIDE_DEPRECATION") // Required forwarding for legacy Media3 callers.
        override fun flush() { mixer.flush() }
        override fun flush(streamMetadata: AudioProcessor.StreamMetadata) { mixer.flush(streamMetadata) }
    }
}

@UnstableApi
@Suppress("DEPRECATION")
internal fun headphoneAwareAudioSink(context: Context): AudioSink {
    val processor = headphoneAudioProcessor { hasHeadphoneOutput(context) }
    val sink = DefaultAudioSink.Builder(context).setEnableFloatOutput(false)
        .setEnableAudioTrackPlaybackParams(false).setAudioProcessors(arrayOf(processor)).build()
    return object : ForwardingAudioSink(sink) {
        override fun supportsFormat(format: Format): Boolean =
            getFormatSupport(format) != AudioSink.SINK_FORMAT_UNSUPPORTED
        override fun getFormatSupport(format: Format): Int =
            if (hasHeadphoneOutput(context) && format.sampleMimeType != MimeTypes.AUDIO_RAW)
                AudioSink.SINK_FORMAT_UNSUPPORTED else super.getFormatSupport(format)
        override fun getFormatOffloadSupport(format: Format): AudioOffloadSupport =
            if (hasHeadphoneOutput(context)) AudioOffloadSupport.DEFAULT_UNSUPPORTED
            else super.getFormatOffloadSupport(format)
    }
}
