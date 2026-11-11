/* Adapted from NuvioTV (GPL-3.0), https://github.com/NuvioMedia/NuvioTV .
 * Modified for BruniO RP200. See RP200_NUVIO_ATTRIBUTION.md for license details. */
package com.miflix.native2.data.rp200

import androidx.media3.common.MimeTypes
import android.media.MediaCodecList
import java.util.concurrent.ConcurrentHashMap

private val CODECS_PARAM_REGEX = Regex("codecs=\"([^\"]+)\"", RegexOption.IGNORE_CASE)

/** The `codecs` parameter of a YouTube format's `mimeType`, e.g. `av01.0.12M.10`. */
internal fun videoCodecsParam(formatMimeType: String): String? =
    CODECS_PARAM_REGEX.find(formatMimeType)?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotEmpty() }

/**
 * The decoder MIME type for a `codecs` string, e.g. `av01.0.12M.10` -> `video/av01`. Null when
 * the codec isn't one we know, which callers treat as playable.
 */
internal fun videoCodecMimeType(codecs: String): String? {
    val codec = codecs.trim().lowercase()
    return when {
        codec.startsWith("av01") -> MimeTypes.VIDEO_AV1
        codec.startsWith("vp09") || codec.startsWith("vp9") -> MimeTypes.VIDEO_VP9
        codec.startsWith("avc1") -> MimeTypes.VIDEO_H264
        codec.startsWith("hev1") || codec.startsWith("hvc1") -> MimeTypes.VIDEO_H265
        else -> null
    }
}

/**
 * Asks the same question the player's video renderer asks: is there a decoder for this codec,
 * profile and level at this size and frame rate? YouTube serves many 4K trailers only as AV1,
 * which TV boxes like the SHIELD have no decoder for, so the trailer played its audio over a
 * black screen.
 */
internal object VideoDecoderSupport {
    private val cache = ConcurrentHashMap<String, Boolean>()

    fun canDecode(codecs: String, width: Int, height: Int, fps: Int): Boolean =
        cache.getOrPut("$codecs|$width|$height|$fps") {
            val mimeType = videoCodecMimeType(codecs) ?: return@getOrPut true
            try {
                // Media3 1.11 requires Context for isFormatSupported. Use Android's
                // decoder capabilities instead: no Context dependency or API mismatch.
                MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos.any { info ->
                    !info.isEncoder && info.supportedTypes.any { type ->
                        if (!type.equals(mimeType, ignoreCase = true)) false
                        else runCatching {
                            val video = info.getCapabilitiesForType(type).videoCapabilities
                            when {
                                video == null -> false
                                width <= 0 || height <= 0 -> true
                                fps > 0 -> video.areSizeAndRateSupported(width, height, fps.toDouble())
                                else -> video.isSizeSupported(width, height)
                            }
                        }.getOrDefault(false)
                    }
                }
            } catch (e: Exception) {
                // When capabilities cannot be queried, allow the player to attempt playback.
                true
            }
        }
}
