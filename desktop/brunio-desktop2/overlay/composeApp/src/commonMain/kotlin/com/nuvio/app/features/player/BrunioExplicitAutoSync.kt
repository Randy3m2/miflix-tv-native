package com.nuvio.app.features.player

import com.nuvio.app.features.autosync.AutoSyncPlayerController

/**
 * BruniO preference: never auto-run subtitle alignment when a track is chosen.
 * Autosync is explicitly requested from the subtitle panel.
 * Existing manual timing / subtitle Off and regular selection remain unchanged.
 */
internal fun PlayerScreenRuntime.runBrunioExplicitAutoSync(): Boolean {
    val subtitleUrl = selectedAddonSubtitle?.url ?: return false
    val controller = playerController as? AutoSyncPlayerController ?: return false
    subtitleAutoSyncState = SubtitleAutoSyncUiState()
    controller.setSubtitleUriWithSelectedAutoSync(subtitleUrl)
    return true
}
