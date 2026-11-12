package com.nuvio.app.features.brunio

import java.util.prefs.Preferences

internal actual fun bundledBrunioLivePlaylist(): String =
    BrunioLiveTvCatalog::class.java.classLoader
        ?.getResourceAsStream("brunio/live_channels.m3u")
        ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
        .orEmpty()

private val liveSettings: Preferences
    get() = Preferences.userRoot().node("brunio/desktop2/live-tv")

internal actual fun hiddenBrunioChannels(): Set<String> =
    liveSettings.get("hidden", "").split(',').filter(String::isNotBlank).toSet()

internal actual fun saveHiddenBrunioChannels(ids: Set<String>) {
    liveSettings.put("hidden", ids.sorted().joinToString(","))
    liveSettings.flush()
}
