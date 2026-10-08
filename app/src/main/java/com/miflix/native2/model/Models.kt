package com.miflix.native2.model

data class MediaSummary(
    val id: Int,
    val type: String,
    val title: String,
    val overview: String,
    val backdrop: String?,
    val poster: String?,
    val rating: Double,
    val year: String,
    val popularity: Double = 0.0
) {
    val cloudId: String get() = "tmdb:$type:$id"
}

data class CastMember(
    val name: String,
    val character: String,
    val profile: String?
)

data class TrailerSummary(
    val name: String,
    val key: String,
    val site: String,
    val type: String,
    val official: Boolean
) {
    val thumbnail: String?
        get() = if (site.equals("YouTube", ignoreCase = true) && key.isNotBlank()) {
            "https://img.youtube.com/vi/$key/hqdefault.jpg"
        } else null

    val watchUrl: String?
        get() = if (site.equals("YouTube", ignoreCase = true) && key.isNotBlank()) {
            "https://www.youtube.com/watch?v=$key"
        } else null
}

data class MediaDetails(
    val summary: MediaSummary,
    val imdbId: String?,
    val runtimeText: String,
    val genres: List<String>,
    val seasonCount: Int = 0,
    val releaseInfo: String = "",
    val originCountry: String = "",
    val originalLanguage: String = "",
    val cast: List<CastMember> = emptyList(),
    val trailers: List<TrailerSummary> = emptyList()
)

data class EpisodeSummary(
    val season: Int,
    val episode: Int,
    val title: String,
    val overview: String,
    val still: String?,
    val rating: Double,
    val airDate: String
)

data class StreamChoice(
    val name: String,
    val url: String,
    val title: String,
    val subtitles: List<SubtitleChoice> = emptyList(),
    val filename: String = "",
    val videoHash: String = "",
    val videoSize: Long = 0L,
    val requestHeaders: Map<String, String> = emptyMap(),
    val addonName: String = ""
)

data class SubtitleChoice(
    val url: String,
    val lang: String,
    val label: String,
    val source: String = "stream"
)

data class Profile(
    val id: String,
    val name: String,
    val avatarValue: String? = null,
    val primary: Boolean = false
)

data class CloudSession(
    val accessToken: String,
    val refreshToken: String,
    val userId: String,
    val email: String
)

data class PrivateSetup(
    val tmdbToken: String = "",
    val torrentioManifest: String = "",
    val addonManifests: List<String> = emptyList()
)

data class PlaybackProgress(
    val percent: Double = 0.0,
    val position: Long = 0L,
    val duration: Long = 0L,
    val updatedAt: Long = 0L,
    val season: Int = 0,
    val episode: Int = 0
)

data class CollectionTile(
    val id: String,
    val title: String,
    val coverUrl: String,
    val focusGifUrl: String? = null
)

data class UpdateInfo(
    val version: String,
    val downloadUrl: String,
    val isNewer: Boolean
)

data class CatalogSection(
    val id: String,
    val title: String,
    val items: List<MediaSummary>,
    val landscape: Boolean = false
)

data class GenreDefinition(
    val id: String,
    val title: String,
    val movieGenreId: Int,
    val tvGenreId: Int,
    val coverUrl: String = ""
)

data class WatchParty(
    val roomCode: String,
    val hostUserId: String,
    val cloudId: String,
    val season: Int,
    val episode: Int,
    val positionMs: Long,
    val playing: Boolean,
    val updatedAt: Long,
    val liveChannelId: String = "",
    val liveProvider: String = "",
    val liveTitle: String = "",
    val liveType: String = "tv"
)

enum class PartyRole { HOST, GUEST }

data class PairingRequest(
    val code: String,
    val secretBase64Url: String,
    val url: String
)

data class PairingPayload(
    val accessToken: String = "",
    val refreshToken: String = "",
    val userId: String = "",
    val email: String = "",
    val addonManifest: String = ""
)

// Release IDs remain remembered after clearing notifications, so they do not reappear.
data class ReleaseNotice(val id: String, val item: MediaSummary, val message: String, val date: String)
data class SkipSegment(val startMs: Long, val endMs: Long)
data class PlaybackSegments(val intro: SkipSegment? = null, val outro: SkipSegment? = null, val postCredits: SkipSegment? = null)
