package com.nuvio.app.features.brunio

import java.awt.Desktop
import java.net.URI
import java.time.LocalDate
import java.time.ZoneId

internal actual fun brunioLidomSeason(): Int = LocalDate.now(ZoneId.of("America/Santo_Domingo")).let { if (it.monthValue < 7) it.year - 1 else it.year }

internal actual fun openBrunioLidomYouTube(url: String): Boolean = runCatching {
    val target = URI(url)
    require(target.scheme == "https" &&
        target.host in setOf("www.youtube.com", "youtube.com") &&
        target.rawUserInfo == null)
    if (!Desktop.isDesktopSupported()) return@runCatching false
    Desktop.getDesktop().browse(target)
    true
}.getOrDefault(false)
