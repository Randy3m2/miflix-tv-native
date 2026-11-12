#!/usr/bin/env python3
"""Keep pinned Nuvio party screen look while routing room UI to BruniO RP1."""
from pathlib import Path


def apply(destination: Path) -> None:
    source = destination / "composeApp/src/commonMain/kotlin/com/nuvio/app/MainAppContent.kt"
    contents = source.read_text(encoding="utf-8")
    anchor = "import com.nuvio.app.features.player.recordExternalPlaybackProgress"
    assert contents.count(anchor) == 1, "Unexpected upstream player imports"
    contents = contents.replace(anchor,
        "import com.nuvio.app.features.brunio.BrunioPartyScreen\n" + anchor, 1)
    original = '''                        PartyScreen(
                            initialCode = route.code,
                            onBack = onBack,
                            onWatch = ::openPartyMedia,
                        )'''
    replacement = '''                        BrunioPartyScreen(
                            initialCode = route.code,
                            onBack = onBack,
                            onWatch = ::openPartyMedia,
                        )'''
    assert contents.count(original) == 1, "Unexpected upstream PartyRoute"
    source.write_text(contents.replace(original, replacement, 1), encoding="utf-8")
