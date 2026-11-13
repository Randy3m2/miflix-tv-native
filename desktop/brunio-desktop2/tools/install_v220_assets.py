#!/usr/bin/env python3
"""Install the same Streaming GIFs and Sports default already shipped in BruniO TV.

This reads the constant from TV code at build-time, so Desktop and TV never
silently drift. No keys or personalized URLs are emitted in CI logs.
"""
from pathlib import Path
import re
import shutil

ROOT = Path(__file__).resolve().parents[3]

def install(destination: Path) -> int:
    out = destination / "composeApp/src/desktopMain/resources/brunio"
    stream = out / "streaming"
    stream.mkdir(parents=True, exist_ok=True)
    assets = [
        (ROOT / "app/src/main/assets/streaming_rp48/hulu.gif", "hulu.gif"),
        (ROOT / "app/src/main/assets/streaming_rp48/paramount-plus.gif", "paramount.gif"),
        (ROOT / "app/src/main/res/drawable-nodpi/streaming_amcplus_rp48.png", "amcplus.png"),
    ]
    for id in ("aguilas", "licey", "estrellas", "toros", "escogido", "gigantes"):
        assets.append((
            ROOT / "app/src/main/res/drawable-nodpi" / f"lidom_{id}.png",
            f"lidom_{id}.png",
        ))
    for source, name in assets:
        if not source.is_file() or source.stat().st_size < 1000:
            raise RuntimeError("Missing original BruniO TV streaming art: " + name)
        shutil.copy2(source, stream / name)
    policy = ROOT / "app/src/main/java/com/miflix/native2/data/SportsPolicyRP71.java"
    source = policy.read_text(encoding="utf-8")
    match = re.search(r'public static final String DEFAULT="([^"]+)";', source)
    if not match or not match.group(1).startswith("https://"):
        raise RuntimeError("BruniO TV SportsPolicy DEFAULT not found")
    (out / "default_sports_manifest.txt").write_text(match.group(1) + "\n", encoding="utf-8")
    return len(assets)
