"""Use BruniO Windows shortcut names and BruniO icon variants, not Nuvio's.

The upstream icon updater still writes Nuvio.lnk even after jpackage creates
BruniO.lnk; it therefore never updates the icon Windows shows for BruniO.
"""
from pathlib import Path
from patch_runtime import once

def apply(root: Path):
    path = root / "composeApp/src/desktopMain/kotlin/com/nuvio/app/features/settings/WindowsAppShortcutIconUpdater.kt"
    src = path.read_text(encoding="utf-8")
    for old, new, minimum in [
        ('"Nuvio/icons"', '"BruniO/icons"', 1),
        ('"Nuvio.lnk"', '"BruniO.lnk"', 3),
        ('"Nuvio/Nuvio.lnk"', '"BruniO/BruniO.lnk"', 1),
        ('"nuvio-icon-update-"', '"brunio-icon-update-"', 1),
    ]:
        occurrences = src.count(old)
        if occurrences < minimum:
            raise RuntimeError(f"Windows shortcut branding anchor changed: {old}")
        src = src.replace(old, new)
    path.write_text(src, encoding="utf-8")
