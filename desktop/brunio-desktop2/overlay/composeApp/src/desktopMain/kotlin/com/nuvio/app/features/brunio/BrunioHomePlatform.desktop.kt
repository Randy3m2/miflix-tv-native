package com.nuvio.app.features.brunio

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.ConcurrentHashMap
import java.util.prefs.Preferences

private val homePrefs: Preferences
    get() = Preferences.userRoot().node("brunio/desktop2/home")
private val extracted = ConcurrentHashMap<String, String>()
private val resourceLoader: ClassLoader
    get() = BrunioHomePreferences::class.java.classLoader

internal actual fun loadBrunioHomeRowIds(): Set<String> {
    val raw = homePrefs.get("rows", "")
    return when (raw) {
        "#none" -> emptySet()
        "" -> setOf("netflix", "disney", "prime", "hbo", "hulu")
        else -> raw.split(',').filter(String::isNotBlank).toSet()
    }
}

internal actual fun saveBrunioHomeRowIds(ids: Set<String>) {
    homePrefs.put("rows", if (ids.isEmpty()) "#none" else ids.sorted().joinToString(","))
    homePrefs.flush()
}

private val sportsPrefs: Preferences
    get() = Preferences.userRoot().node("brunio/desktop2/sports")

// Keep private premium manifest URLs on the user's device; never in Git.
internal actual fun brunioSavedSportsManifest(): String =
    sportsPrefs.get("manifest", "").trim()

internal actual fun brunioSaveSportsManifest(url: String) {
    if (url.isBlank()) sportsPrefs.remove("manifest")
    else sportsPrefs.put("manifest", url.trim())
    sportsPrefs.flush()
}

internal actual fun brunioDefaultSportsManifest(): String =
    resourceLoader.getResourceAsStream("brunio/default_sports_manifest.txt")
        ?.bufferedReader(Charsets.UTF_8)?.use { it.readText().trim() }.orEmpty()
        .takeIf { it.startsWith("https://") }.orEmpty()

internal actual fun brunioLidomLogoUri(id: String): String? {
    if (id !in setOf("aguilas", "licey", "estrellas", "toros", "escogido", "gigantes")) return null
    val name = "lidom_$id.png"
    extracted[name]?.let { return it }
    return runCatching {
        val target = File(System.getProperty("java.io.tmpdir"),
            "brunio-desktop2-lidom/$name")
        target.parentFile.mkdirs()
        // Always refresh to avoid a stale cached logo from another application version.
        resourceLoader.getResourceAsStream("brunio/streaming/$name")?.use {
            Files.copy(it, target.toPath(), StandardCopyOption.REPLACE_EXISTING)
        } ?: return null
        target.toURI().toASCIIString().also { extracted[name] = it }
    }.getOrNull()
}

internal actual fun brunioStreamingLogoUri(id: String): String? {
    val name = when (id) {
        "hulu" -> "hulu.gif"
        "paramount" -> "paramount.gif"
        "amcplus" -> "amcplus.png"
        else -> return null
    }
    extracted[name]?.let { return it }
    return runCatching {
        val target = File(System.getProperty("java.io.tmpdir"),
            "brunio-desktop2-streaming/$name")
        target.parentFile.mkdirs()
        if (!target.isFile) {
            resourceLoader.getResourceAsStream("brunio/streaming/$name")?.use {
                Files.copy(it, target.toPath(), StandardCopyOption.REPLACE_EXISTING)
            } ?: return null
        }
        target.toURI().toASCIIString().also { extracted[name] = it }
    }.getOrNull()
}
