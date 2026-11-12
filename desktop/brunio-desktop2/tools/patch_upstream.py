#!/usr/bin/env python3
"""Fail-closed BruniO patches for the pinned Nuvio Desktop checkout."""
from pathlib import Path


def once(path: Path, before: str, after: str) -> None:
    value = path.read_text(encoding="utf-8")
    count = value.count(before)
    if count != 1:
        raise RuntimeError(f"Unexpected upstream change {path}: {before!r}: found {count}")
    path.write_text(value.replace(before, after), encoding="utf-8")


def apply(destination: Path) -> None:
    root = destination / "composeApp/src"
    cache = root / "desktopMain/kotlin/com/nuvio/app/core/storage/DesktopCache.kt"
    # JarURLConnection caches jars globally by default, locking the temp files on Windows.
    once(cache, "url.openStream().use { input ->",
        "url.openConnection().apply { useCaches = false }.getInputStream().use { input ->")
    once(cache,
        'is JarURLConnection -> connection.jarEntry.let { entry -> "${entry.size}:${entry.crc}" }',
        'is JarURLConnection -> connection.jarFile.use { jar ->\n'
        '                jar.getJarEntry(connection.entryName).let { entry -> "${entry.size}:${entry.crc}" }\n'
        '            }')
    once(cache, 'when (val connection = url.openConnection()) {',
        'when (val connection = url.openConnection().apply { useCaches = false }) {')

    storage = root / "desktopMain/kotlin/com/nuvio/app/core/storage/DesktopStorage.kt"
    for old, new in [
        ("Library/Application Support/Nuvio", "Library/Application Support/BruniO/Desktop2"),
        ('resolve("Nuvio")', 'resolve("BruniO/Desktop2")'),
        ("Library/Caches/Nuvio", "Library/Caches/BruniO/Desktop2"),
        ('resolve("Nuvio/Cache")', 'resolve("BruniO/Desktop2/Cache")'),
        ("Nuvio desktop preferences", "BruniO Desktop 2 preferences"),
    ]:
        once(storage, old, new)
    # There are two lowercase XDG locations (config and cache).
    value = storage.read_text(encoding="utf-8")
    assert value.count('resolve("nuvio")') == 2, "Upstream XDG location drift"
    storage.write_text(value.replace('resolve("nuvio")', 'resolve("brunio-desktop2")'), encoding="utf-8")

    test = root / "commonTest/kotlin/com/nuvio/app/features/settings/SettingsNavigationTest.kt"
    once(test, "import com.nuvio.app.features.player.AndroidPlaybackEngine",
         "import com.nuvio.app.features.player.AndroidPlaybackEngine\nimport com.nuvio.app.isDesktop")
    once(test,
        "assertEquals(engine != AndroidPlaybackEngine.Libmpv, PlaybackSection.Buffer in android)",
        "assertEquals(!isDesktop && engine != AndroidPlaybackEngine.Libmpv, PlaybackSection.Buffer in android)")

    linux = root / "desktopTest/kotlin/com/nuvio/app/features/updater/LinuxUpdateInstallTest.kt"
    once(linux, 'listOf("xdg-open", "/tmp/updates/Nuvio.rpm")',
        'listOf("xdg-open", File("/tmp/updates/Nuvio.rpm").absolutePath)')
    once(linux, 'listOf("xdg-open", "/tmp/updates/Nuvio.AppImage")',
        'listOf("xdg-open", File("/tmp/updates/Nuvio.AppImage").absolutePath)')
    once(linux,
        'script.contains("cp -f -- \'/tmp/updates/Nuvio.AppImage\' \'/home/user/Apps/Nuvio.AppImage\'")',
        'script.contains("cp -f -- \'${File("/tmp/updates/Nuvio.AppImage").absolutePath}\' \'/home/user/Apps/Nuvio.AppImage\'")')
    once(linux,
        'script.contains("else xdg-open \'/tmp/updates/Nuvio.AppImage\'")',
        'script.contains("else xdg-open \'${File("/tmp/updates/Nuvio.AppImage").absolutePath}\'")')

    progress = root / "commonTest/kotlin/com/nuvio/app/features/watchprogress/WatchProgressIdentityTest.kt"
    # A one-second clip is below upstream's minimum completion sample threshold.
    once(progress, '"lastPositionMs": 940,', '"lastPositionMs": 94000,')
    once(progress, '"durationMs": 1000,', '"durationMs": 100000,')
    once(progress, "assertEquals(940L, decoded.single().lastPositionMs)",
         "assertEquals(94000L, decoded.single().lastPositionMs)")

    updater = root / "desktopMain/kotlin/com/nuvio/app/features/updater/AppUpdaterPlatform.desktop.kt"
    once(updater,
        'get() = currentOs != DesktopUpdaterOs.UNKNOWN && linuxInstallMethod != LinuxInstallMethod.FLATPAK',
        'get() = false // No cross-product Nuvio updates until signed BruniO desktop releases exist')
    once(updater, 'owner = "NuvioMedia",', 'owner = "Randy3m2",')
    once(updater, 'repo = "NuvioDesktop",', 'repo = "miflix-tv-native",')
    once(updater, 'userAgent = "NuvioDesktop",', 'userAgent = "BruniO-Desktop2",')
