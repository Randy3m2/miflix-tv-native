#!/usr/bin/env python3
"""Reproducible, fail-closed Nuvio Desktop UI checkout for BruniO Desktop 2.

This prepares an UNINTEGRATED visual upstream: it does not enable BruniO Watch Party,
account data or releases, and must not be shipped as a final BruniO product.
Only standard-library dependencies, except Pillow for Windows icon conversion.
"""
from __future__ import annotations

import argparse
import os
import shutil
import subprocess
from pathlib import Path

UPSTREAM = "https://github.com/NuvioMedia/NuvioDesktop.git"
PIN = "37ce07d351fc69e17d05a4640ef78d08deba20e2"
MSI_ID = "41d6bc76-7d94-4c54-a9fd-325239cc6d2b"
ROOT = Path(__file__).resolve().parents[3]


class UpstreamChanged(RuntimeError):
    pass


def replace_exact(path: Path, before: str, after: str) -> None:
    content = path.read_text(encoding="utf-8")
    occurrences = content.count(before)
    if occurrences != 1:
        raise UpstreamChanged(
            f"{path}: expected exactly 1 occurrence, found {occurrences}: {before!r}"
        )
    path.write_text(content.replace(before, after, 1), encoding="utf-8")


def run(*cmd: str, cwd: Path | None = None) -> str:
    try:
        result = subprocess.run(cmd, cwd=cwd, check=True, capture_output=True, text=True)
    except subprocess.CalledProcessError as err:
        details = (err.stderr or err.stdout or "").strip()
        raise RuntimeError(f"Command failed: {cmd[0]} {cmd[1]}: {details[-3500:]}") from err
    return result.stdout.strip()


def pin_upstream(destination: Path) -> None:
    if destination.exists():
        raise FileExistsError(f"Destination already exists: {destination}")
    destination.parent.mkdir(parents=True, exist_ok=True)
    run("git", "clone", "--filter=blob:none", "--single-branch", "--branch", "Dev", UPSTREAM, str(destination))
    run("git", "checkout", "--detach", PIN, cwd=destination)
    commit = run("git", "rev-parse", "HEAD", cwd=destination)
    if commit != PIN:
        raise UpstreamChanged(f"Expected {PIN}, got {commit}")
    # MPVKit is unnecessary for Windows Compose compilation, and pulling its
    # Apple-native submodule can fail independently of desktop UI code.
    if os.environ.get("BRUNIO_DESKTOP2_FULL_SUBMODULES") == "1":
        run("git", "submodule", "update", "--init", "--recursive", cwd=destination)
    # Upstream bundles native Windows libmpv through Git LFS; a text pointer
    # must never be treated as a valid native player runtime.
    if shutil.which("git-lfs"):
        run("git", "lfs", "pull", cwd=destination)


def patch_visual_brand(destination: Path) -> None:
    desktop_main = destination / "composeApp/src/desktopMain/kotlin/com/nuvio/app/Main.kt"
    gradle = destination / "composeApp/build.gradle.kts"
    replace_exact(
        desktop_main,
        'title = if (smokePlayerUrl == null) "Nuvio" else "Nuvio Player Smoke",',
        'title = if (smokePlayerUrl == null) "BruniO" else "BruniO Player Smoke",',
    )
    replace_exact(gradle, 'packageName = "Nuvio"', 'packageName = "BruniO"')
    replace_exact(gradle, 'vendor = "Nuvio Media"', 'vendor = "BruniO"')
    replace_exact(
        gradle,
        'val windowsMsiUpgradeUuid = "395990ee-9b8a-3548-922c-e7a23a495b8d"',
        f'val windowsMsiUpgradeUuid = "{MSI_ID}"',
    )
    replace_exact(
        gradle,
        '"Nuvio-Windows-$windowsPlayerBridgeArch-$desktopReleaseVersionName.msi"',
        '"BruniO-Windows-$windowsPlayerBridgeArch-$desktopReleaseVersionName.msi"',
    )
    replace_exact(
        gradle,
        '"Nuvio-$desktopReleasePackageVersion.msi"',
        '"BruniO-$desktopReleasePackageVersion.msi"',
    )
    # Exact Nuvio motion/density/theme code is deliberately not touched.
    # UI-visible identity strings only; retain all upstream attribution/license text.
    resources = destination / "composeApp/src/commonMain/composeResources"
    for translations in resources.glob("values*/strings.xml"):
        txt = translations.read_text(encoding="utf-8")
        txt = txt.replace(">Nuvio<", ">BruniO<").replace(">About Nuvio<", ">About BruniO<")
        translations.write_text(txt, encoding="utf-8")


def patch_brand_icon(destination: Path) -> bool:
    source = ROOT / "desktop/brunio-pc/assets/brunio_icon.png"
    if not source.exists():
        return False
    icons = destination / "composeApp/src/desktopMain/resources/icons"
    shutil.copyfile(source, icons / "nuvio-app-icon-transparent.png")
    try:
        from PIL import Image
        with Image.open(source) as original:
            original.convert("RGBA").save(
                icons / "nuvio-app-icon-transparent.ico",
                format="ICO",
                sizes=[(16, 16), (32, 32), (48, 48), (64, 64), (128, 128), (256, 256)],
            )
    except ImportError:
        # Do not pretend the .ico was replaced if Pillow was unavailable.
        return False
    return True


def install_brunit_overlay(destination: Path) -> int:
    """Copy typed compatibility code without rewriting Nuvio's UI implementation."""
    overlay = ROOT / "desktop/brunio-desktop2/overlay"
    count = 0
    for source in overlay.rglob("*"):
        if not source.is_file():
            continue
        target = destination / source.relative_to(overlay)
        if target.exists():
            raise UpstreamChanged(f"Overlay unexpectedly overwrites upstream file: {target}")
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(source, target)
        count += 1
    return count


def configure_public_backend(destination: Path) -> bool:
    url = os.environ.get("BRUNIO_SUPABASE_URL", "").strip()
    key = os.environ.get("BRUNIO_SUPABASE_PUBLISHABLE_KEY", "").strip()
    if not url or not key:
        return False
    if not url.startswith("https://") or "\n" in url + key:
        raise ValueError("Invalid public Supabase configuration")
    # Nuvio's auth endpoint can be pointed to BruniO for future adapter testing.
    # This alone DOES NOT make Nuvio collections/Watch Party compatible.
    local = destination / "local.properties"
    with local.open("a", encoding="utf-8") as out:
        out.write(f"NUVIO_SUPABASE_URL={url}\n")
        out.write(f"NUVIO_SUPABASE_ANON_KEY={key}\n")
    return True


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    output = args.output.resolve()
    pin_upstream(output)
    patch_visual_brand(output)
    icon_ok = patch_brand_icon(output)
    files_added = install_brunit_overlay(output)
    backend = configure_public_backend(output)
    print("Pinned upstream:", PIN)
    print("Prepared directory:", output)
    print("Windows icon patched:", icon_ok)
    print("Overlay files copied:", files_added)
    print("Public Supabase auth endpoint configured:", backend)
    print("STATUS: visual upstream only. NOT a finished BruniO Desktop build.")
    print("BLOCKER: BruniO repositories and RP1 Party backend adapter not yet wired.")


if __name__ == "__main__":
    main()
