"""Resolve BruniO TMDB identities before an in-player source picker request.

Initial BruniO playback converts TMDB to IMDb in StreamsScreen, but the
separate in-player PlayerStreamsRepository did not. This fixes 'No streams
found' after choosing 'Change my source' within an RP1 room, including reload.
"""
from patch_runtime import once

def apply(root):
    app = root / "composeApp/src/commonMain/kotlin/com/nuvio/app"
    sources = app / "features/player/PlayerScreenRuntimeSourceActions.kt"
    once(sources,
         "import com.nuvio.app.features.debrid.DirectDebridPlayableResult",
         "import com.nuvio.app.features.brunio.BrunioCloud\n"
         "import com.nuvio.app.features.tmdb.TmdbService\n"
         "import com.nuvio.app.features.debrid.DirectDebridPlayableResult")
    once(sources,
         '''internal fun PlayerScreenRuntime.openSourcesPanel() {
    val vid = activeVideoId ?: return
    PlayerStreamsRepository.loadSources(
        type = contentType ?: parentMetaType,
        videoId = vid,
        season = activeSeasonNumber,
        episode = activeEpisodeNumber,
    )
    showSourcesPanel = true
    showEpisodesPanel = false
    showChaptersPanel = false
    controlsVisible = false
}''',
         '''/**
 * BruniO keeps TMDB IDs for library/progress, but Stremio add-ons usually
 * require IMDb tt... IDs. Conversion is asynchronous and never shares
 * a URL or a private add-on configuration with a Watch Party room.
 */
internal fun PlayerScreenRuntime.loadBrunioPlayerSources(forceRefresh: Boolean = false) {
    val vid = activeVideoId ?: return
    val requestType = contentType ?: parentMetaType
    val season = activeSeasonNumber
    val episode = activeEpisodeNumber
    val numericId = if (BrunioCloud.enabled) {
        Regex("tmdb:(?:movie|series):([0-9]+)").find(vid)?.groupValues
            ?.getOrNull(1)?.toIntOrNull()
            ?: Regex("tmdb:(?:movie|series):([0-9]+)").find(parentMetaId)
                ?.groupValues?.getOrNull(1)?.toIntOrNull()
    } else null

    if (numericId == null) {
        PlayerStreamsRepository.loadSources(
            type = requestType, videoId = vid,
            season = season, episode = episode, forceRefresh = forceRefresh,
        )
        return
    }
    scope.launch {
        val imdb = TmdbService.tmdbToImdb(numericId, requestType)
        val lookup = if (imdb != null) {
            if (requestType in setOf("series", "tv") && season != null && episode != null)
                "$imdb:$season:$episode"
            else imdb
        } else vid
        PlayerStreamsRepository.loadSources(
            type = requestType, videoId = lookup,
            season = season, episode = episode, forceRefresh = forceRefresh,
        )
    }
}

internal fun PlayerScreenRuntime.openSourcesPanel() {
    if (activeVideoId == null) return
    loadBrunioPlayerSources()
    showSourcesPanel = true
    showEpisodesPanel = false
    showChaptersPanel = false
    controlsVisible = false
}''')

    ui = app / "features/player/PlayerScreenRuntimeUi.kt"
    once(ui,
         '''    PlayerStreamsRepository.loadSources(
        type = requestType,
        videoId = vid,
        season = activeSeasonNumber,
        episode = activeEpisodeNumber,
        forceRefresh = forceRefresh,
    )
}

private fun Color.toCssColorString''',
         '''    loadBrunioPlayerSources(forceRefresh = forceRefresh)
}

private fun Color.toCssColorString''')

    once(ui,
         '''        onReloadSources = {
            val vid = activeVideoId
            if (vid != null) {
                PlayerStreamsRepository.loadSources(
                    type = contentType ?: parentMetaType,
                    videoId = vid,
                    season = activeSeasonNumber,
                    episode = activeEpisodeNumber,
                    forceRefresh = true,
                )
            }
        },''',
         '''        onReloadSources = {
            loadBrunioPlayerSources(forceRefresh = true)
        },''')
