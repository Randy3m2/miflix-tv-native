"""Resolve BruniO tmdb: IDs to IMDb IDs for Stremio stream-only add-ons.

Home/details are TMDB; Torrentio/Comet commonly require tt... streams IDs.
Keep the original TMDB identity for resume/library/metadata.
"""
from pathlib import Path
from patch_runtime import once


def apply(root: Path) -> None:
    app = root / "composeApp/src/commonMain/kotlin/com/nuvio/app"
    stream = app / "features/streams/StreamsScreen.kt"
    once(stream,
         "import com.nuvio.app.features.debrid.DirectDebridPlaybackResolver",
         "import com.nuvio.app.features.brunio.BrunioCloud\n"
         "import com.nuvio.app.features.tmdb.TmdbService\n"
         "import com.nuvio.app.features.debrid.DirectDebridPlaybackResolver")
    once(stream, '''    LaunchedEffect(type, videoId, seasonNumber, episodeNumber, manualSelection) {
        StreamsRepository.load(
            type = type,
            videoId = videoId,
            parentMetaId = parentMetaId,
            season = seasonNumber,
            episode = episodeNumber,
            manualSelection = manualSelection,
        )
    }''',
         '''    var brunioStreamLookupId by remember(type, videoId, seasonNumber, episodeNumber) {
        mutableStateOf(videoId)
    }
    LaunchedEffect(type, videoId, seasonNumber, episodeNumber, manualSelection) {
        val resolved = if (BrunioCloud.enabled) {
            val tmdbId = Regex("tmdb:(?:movie|series):([0-9]+)")
                .find(videoId)?.groupValues?.getOrNull(1)?.toIntOrNull()
                ?: Regex("tmdb:(?:movie|series):([0-9]+)")
                    .find(parentMetaId)?.groupValues?.getOrNull(1)?.toIntOrNull()
            val imdb = tmdbId?.let { TmdbService.tmdbToImdb(it, type) }
            if (imdb != null) {
                if (type == "series" && seasonNumber != null && episodeNumber != null)
                    "$imdb:$seasonNumber:$episodeNumber"
                else imdb
            } else videoId
        } else videoId
        brunioStreamLookupId = resolved
        StreamsRepository.load(
            type = type,
            videoId = resolved,
            parentMetaId = parentMetaId,
            season = seasonNumber,
            episode = episodeNumber,
            manualSelection = manualSelection,
        )
    }''')
    once(stream, '''        StreamsRepository.reload(
            type = type,
            videoId = videoId,''',
         '''        StreamsRepository.reload(
            type = type,
            videoId = brunioStreamLookupId,''')
    streams_repo = app / "features/streams/StreamsRepository.kt"
    once(streams_repo, 'log.d { "Fetching streams from: $url" }',
         'log.d { "Fetching streams from a configured add-on" }')
