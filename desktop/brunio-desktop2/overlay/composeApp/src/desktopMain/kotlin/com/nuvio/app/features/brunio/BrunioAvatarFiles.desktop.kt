package com.nuvio.app.features.brunio

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.ConcurrentHashMap

private val avatarCache = ConcurrentHashMap<String, String>()
private val avatarClassLoader: ClassLoader
    get() = BrunioAvatarRegistry::class.java.classLoader

internal actual fun brunioBuiltInAvatarList(): List<Pair<String, String>> =
    avatarClassLoader.getResourceAsStream("brunio/avatars.tsv")?.bufferedReader(Charsets.UTF_8)
        ?.use { reader ->
            reader.lineSequence().mapNotNull { line ->
                val id = line.substringBefore('\t').trim()
                val name = line.substringAfter('\t', "").trim()
                if (id.isBlank() || name.isBlank()) null else id to name
            }.toList()
        }.orEmpty()

internal actual fun brunioAvatarLocalUrl(id: String): String? {
    if (id.length > 80 || !id.matches(Regex("[a-zA-Z0-9:_-]+"))) return null
    avatarCache[id]?.let { return it }
    val name = id.replace(':', '_') + ".png"
    val resourceName = "brunio/avatars/$name"
    val target = File(System.getProperty("java.io.tmpdir"), "brunio-desktop2-avatars/$name")
    return runCatching {
        target.parentFile.mkdirs()
        if (!target.isFile) {
            avatarClassLoader.getResourceAsStream(resourceName)?.use { input ->
                Files.copy(input, target.toPath(), StandardCopyOption.REPLACE_EXISTING)
            } ?: return null
        }
        target.toURI().toASCIIString().also { avatarCache[id] = it }
    }.getOrNull()
}
