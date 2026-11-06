package com.miflix.native2.data

/**
 * Capability-first casting primitives.  These types deliberately contain no
 * Roku ECP calls: Roku Media Player is a DLNA client, not a generic push
 * receiver.  The UI can therefore explain the selected route accurately.
 */
enum class CastDestinationKind { BRUNIO_TV, DLNA_RENDERER, ROKU_MEDIA_PLAYER, SCREEN_MIRROR }
enum class CastMediaKind { MOVIE, EPISODE, LIVE_CHANNEL, LIVE_SPORT }
enum class CastProtocol { MP4, HLS, DASH }

data class CastMediaDescriptor(
    val id: String,
    val title: String,
    val kind: CastMediaKind,
    val url: String,
    val mimeType: String,
    val protocol: CastProtocol,
    val requiresAuth: Boolean = false,
    val drmProtected: Boolean = false,
    val supportsRange: Boolean = false,
    val isLive: Boolean = false,
)

data class CastCapabilities(
    val destination: CastDestinationKind,
    val acceptedMimeTypes: Set<String> = emptySet(),
    val acceptedProtocols: Set<CastProtocol> = emptySet(),
    val supportsSeek: Boolean = false,
    val supportsStop: Boolean = true,
    val supportsLive: Boolean = false,
    val supportsHls: Boolean = false,
    val authenticatedGatewayRequired: Boolean = false,
)

data class CastTarget(
    val id: String,
    val displayName: String,
    val address: String?,
    val capabilities: CastCapabilities,
)

sealed interface CastPlan {
    data class Direct(val target: CastTarget) : CastPlan
    data class RokuMediaPlayer(val target: CastTarget, val needsMediaServer: Boolean = true) : CastPlan
    data class Unsupported(val reason: String) : CastPlan
}

object UniversalCastPlannerRP173 {
    fun plan(target: CastTarget, media: CastMediaDescriptor): CastPlan {
        val c = target.capabilities
        if (media.drmProtected) return CastPlan.Unsupported("DRM content cannot be exposed by the local gateway")
        if (media.requiresAuth && !c.authenticatedGatewayRequired) {
            return CastPlan.Unsupported("The receiver cannot receive authenticated media safely")
        }
        val mimeOk = c.acceptedMimeTypes.isEmpty() || media.mimeType in c.acceptedMimeTypes
        val protocolOk = c.acceptedProtocols.isEmpty() || media.protocol in c.acceptedProtocols
        if (mimeOk && protocolOk && (!media.isLive || c.supportsLive)) return CastPlan.Direct(target)
        if (c.destination == CastDestinationKind.ROKU_MEDIA_PLAYER) {
            // Roku Media Player browses a DLNA MediaServer. It is never reported
            // as direct playback when the content is not advertised as compatible.
            if (media.protocol == CastProtocol.HLS && !c.supportsHls) {
                return CastPlan.Unsupported("This Roku path has not verified HLS playback")
            }
            return CastPlan.RokuMediaPlayer(target)
        }
        return CastPlan.Unsupported("The receiver does not advertise a compatible format")
    }
}
