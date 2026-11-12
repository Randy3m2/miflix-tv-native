#!/usr/bin/env python3
"""Copy the user's already-bundled TV playlist and avatars to Desktop resources.

Atlas portraits are derived only from existing TV project assets; no external
image requests, tokens, or runtime network downloads are needed. Brand safe
padding is applied without recreating or stylizing the logo.
"""
from pathlib import Path
from PIL import Image, ImageOps
import json
import shutil


ROOT = Path(__file__).resolve().parents[3]


def safe_square(source: Image.Image, size: int = 512, margin: float = 0.12) -> Image.Image:
    image = source.convert("RGBA")
    alpha = image.getchannel("A")
    bbox = alpha.point(lambda x: 255 if x > 6 else 0).getbbox()
    if bbox:
        image = image.crop(bbox)
    limit = max(1, int(size * (1.0 - 2 * margin)))
    image.thumbnail((limit, limit), Image.Resampling.LANCZOS)
    out = Image.new("RGBA", (size, size))
    out.alpha_composite(image, ((size - image.width) // 2, (size - image.height) // 2))
    return out


def install(destination: Path) -> int:
    root = destination / "composeApp/src/desktopMain/resources"
    out = root / "brunio"
    out.mkdir(parents=True, exist_ok=True)
    playlist = ROOT / "app/src/main/assets/final_channels_rp97.m3u"
    if not playlist.is_file():
        raise RuntimeError("BruniO TV channels M3U is missing")
    shutil.copy2(playlist, out / "live_channels.m3u")

    avatars = out / "avatars"
    avatars.mkdir(exist_ok=True)
    keys = []
    original = [
        ("ai:astronaut", "Astronaut", "brunio_avatar_astronaut.png"),
        ("ai:fox", "Fox", "brunio_avatar_fox.png"),
        ("ai:robot", "Robot", "brunio_avatar_robot.png"),
        ("ai:dragon", "Dragon", "brunio_avatar_dragon.png"),
        ("ai:special", "Special", "brunio_avatar_special.png"),
    ]
    for key, name, file in original:
        path = ROOT / "app/src/main/res/drawable-nodpi" / file
        if not path.is_file():
            raise RuntimeError(f"Missing original BruniO avatar: {file}")
        with Image.open(path) as image:
            ImageOps.fit(image.convert("RGBA"), (256, 256), method=Image.Resampling.LANCZOS).save(
                avatars / (key.replace(":", "_") + ".png"),
                optimize=True,
            )
        keys.append((key, name))

    catalog = ROOT / "PROFILE_AVATAR_SOURCES.json"
    packs = json.loads(catalog.read_text(encoding="utf-8")).get("packs", [])
    for pack in packs:
        sheet = ROOT / pack["sheet"]
        entries = pack.get("portraits", [])
        if not sheet.is_file() or not entries:
            continue
        with Image.open(sheet) as image:
            atlas = image.convert("RGBA")
            # The TV repository documents tiles arranged horizontally at 256px.
            tile = atlas.width // len(entries)
            if tile < 128 or atlas.height < 128:
                raise RuntimeError(f"Unrecognized TV portrait atlas layout: {sheet}")
            for index, portrait in enumerate(entries):
                key = portrait.get("key", "")
                if not key.startswith("pack:") or index >= len(entries):
                    continue
                name = portrait.get("name", "Avatar").replace("\t", " ").replace("\n", " ")[:80]
                tile_image = atlas.crop((index * tile, 0, (index + 1) * tile, min(tile, atlas.height)))
                ImageOps.fit(tile_image, (256, 256), method=Image.Resampling.LANCZOS).save(
                    avatars / (key.replace(":", "_") + ".png"),
                    optimize=True,
                )
                keys.append((key, name))
    (out / "avatars.tsv").write_text(
        "".join(f"{key}\t{name}\n" for key, name in keys),
        encoding="utf-8",
    )
    return len(keys)
