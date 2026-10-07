package com.miflix.native2.model

data class MediaSummary(
    val id: Int,
    val type: String,
    val title: String,
    val overview: String,
    val backdrop: String?,
    val poster: String?,
    val rating: Double,
    val year: String
) {
    val cloudId: String get() = "tmdb:$type:$id"
}

data class MediaDetails(
    val summary: MediaSummary,
    val imdbId: String?,
    val runtimeText: String,
    val genres: List<String>,
    val seasonCount: Int = 0
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
    val subtitles: List<SubtitleChoice> = emptyList()
)

data class SubtitleChoice(val url: String, val lang: String, val label: String)

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
    val torrentioManifest: String = ""
)

data class PlaybackProgress(
    val percent: Double = 0.0,
    val position: Long = 0L,
    val duration: Long = 0L,
    val updatedAt: Long = 0L,
    val season: Int = 0,
    val episode: Int = 0
)
