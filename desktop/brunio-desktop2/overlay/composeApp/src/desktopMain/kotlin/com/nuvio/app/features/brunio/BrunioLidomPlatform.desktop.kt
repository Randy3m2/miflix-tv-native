package com.nuvio.app.features.brunio

import java.awt.Desktop
import java.net.URI

internal actual fun openBrunioLidomYouTube(url: String): Boolean = runCatching {
    val target = URI(url)
    require(target.scheme == "https" &&
        target.host in setOf("www.youtube.com", "youtube.com") &&
        target.rawUserInfo == null)
    if (!Desktop.isDesktopSupported()) return@runCatching false
    Desktop.getDesktop().browse(target)
    true
}.getOrDefault(false)
