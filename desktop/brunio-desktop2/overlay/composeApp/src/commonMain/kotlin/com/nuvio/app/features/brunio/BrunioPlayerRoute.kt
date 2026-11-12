package com.nuvio.app.features.brunio

import androidx.compose.ui.Modifier
import com.nuvio.app.features.party.PartyMedia
import com.nuvio.app.features.player.PlayerBackRequest
import com.nuvio.app.features.player.PlayerScreenArgs

/**
 * BruniO PC playback request adapted to the ORIGINAL Nuvio desktop player UI.
 *
 * This is a typed entry-point only: the Nuvio player surface is intentionally
 * unchanged. Full account/profile/progress/stream sourcing is still awaiting
 * integration into the application's navigation and repositories.
 *
 * Important: caller must supply an *authenticated playable* source URL and
 * appropriate access headers. Private URLs must never be logged.
 */
internal data class BrunioPlayerRoute(
    val cloudId: String,
    val title: String,
    val sourceUrl: String,
    val providerName: String,
    val streamTitle: String,
    val streamSubtitle: String? = null,
    val sourceHeaders: Map<String, String> = emptyMap(),
    val responseHeaders: Map<String, String> = emptyMap(),
    val audioUrl: String? = null,
    val artworkLogo: String? = null,
    val artworkPoster: String? = null,
    val artworkBackground: String? = null,
    val season: Int = 0,
    val episode: Int = 0,
    val episodeTitle: String? = null,
    val initialPositionMs: Long = 0L,
    val preferredLanguage: String? = null,
    val streamType: String? = null,
) {
    val mediaKind: String get() = when {
        cloudId.startsWith("tmdb:movie:") -> "movie"
        cloudId.startsWith("tmdb:series:") -> "series"
        else -> error("Unsupported BruniO media ID")
    }

    fun validate(): BrunioPlayerRoute {
        val validId = (cloudId.startsWith("tmdb:movie:") || cloudId.startsWith("tmdb:series:")) &&
            cloudId.substringAfterLast(':').toLongOrNull() != null
        require(validId) { "BruniO player requires an existing TMDB movie/series ID" }
        require(sourceUrl.isNotBlank() && (
            sourceUrl.startsWith("https://", true) ||
            sourceUrl.startsWith("http://", true) ||
            sourceUrl.startsWith("file://", true)
        )) { "Unsupported playback scheme" }
        require(season >= 0 && episode >= 0 && initialPositionMs >= 0L)
        require(sourceHeaders.keys.none { '\n' in it || '\r' in it } &&
            sourceHeaders.values.none { '\n' in it || '\r' in it })
        return this
    }

    /**
     * Render Nuvio's PlayerScreenContent with BruniO media metadata.
     * Uses Nuvio's existing source panel/timeline/subtitles and desktop player.
     */
    fun toNuvioArgs(
        profileIndex: Int,
        onBack: PlayerBackRequest,
        modifier: Modifier = Modifier,
        onPartyMedia: (PartyMedia) -> Unit = {},
    ): PlayerScreenArgs {
        validate()
        require(profileIndex >= 0)
        val kind = mediaKind
        val id = cloudId.substringAfterLast(':')
        val series = kind == "series"
        return PlayerScreenArgs(
            profileId = profileIndex,
            title = title,
            sourceUrl = sourceUrl,
            sourceAudioUrl = audioUrl,
            sourceHeaders = sourceHeaders,
            sourceResponseHeaders = responseHeaders,
            streamType = streamType,
            providerName = providerName,
            streamTitle = streamTitle,
            streamSubtitle = streamSubtitle,
            initialBingeGroup = null,
            pauseDescription = null,
            onBack = onBack,
            onOpenInExternalPlayer = null,
            onOpenExternalUrl = null,
            modifier = modifier,
            logo = artworkLogo,
            poster = artworkPoster,
            background = artworkBackground,
            seasonNumber = season.takeIf { series && it > 0 },
            episodeNumber = episode.takeIf { series && it > 0 },
            episodeTitle = episodeTitle,
            episodeThumbnail = null,
            contentType = kind,
            videoId = if (series && season > 0 && episode > 0) "$cloudId:$season:$episode" else cloudId,
            parentMetaId = id,
            parentMetaType = kind,
            providerAddonId = null,
            torrentInfoHash = null,
            torrentFileIdx = null,
            torrentFilename = null,
            torrentTrackers = emptyList(),
            initialPositionMs = initialPositionMs,
            initialProgressFraction = null,
            contentLanguage = preferredLanguage,
            onOpenPartyMedia = onPartyMedia,
        )
    }
}
