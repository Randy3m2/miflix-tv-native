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
        # Do not check out unused macOS/Linux LFS payloads when building Windows.
        # They can exhaust upstream LFS quota and break otherwise valid Windows builds.
        env = os.environ.copy()
        if cmd and cmd[0] == "git":
            env["GIT_LFS_SKIP_SMUDGE"] = "1"
        result = subprocess.run(cmd, cwd=cwd, check=True, capture_output=True, text=True, env=env)
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
    # Nuvio's upstream LFS quota can be exhausted independently of this fork.
    # The Windows DLL is byte-identical to the already verified BruniO 2.0.2
    # build. Verify it against the PINNED Git LFS sha256 OID before using it.
    # Never accept an arbitrary binary supplied by the environment.
    dll = destination / "composeApp/src/desktopMain/native/windows/runtime/libmpv-2.dll"
    pointer = dll.read_text(encoding="ascii") if dll.stat().st_size < 4096 else ""
    import hashlib
    import re
    oid = re.search(r"(?m)^oid sha256:([a-f0-9]{64})$", pointer)
    verified = False
    cached_runtime = os.environ.get("BRUNIO_DESKTOP2_MPV_FALLBACK", "").strip()
    if cached_runtime and oid:
        candidate = Path(cached_runtime).resolve()
        if candidate.is_file() and candidate.stat().st_size >= 1_000_000:
            digest = hashlib.sha256(candidate.read_bytes()).hexdigest()
            if digest == oid.group(1):
                shutil.copy2(candidate, dll)
                verified = True
                print("Windows libmpv restored from pinned, hash-verified BruniO MSI.")
    if not verified and shutil.which("git-lfs"):
        run(
            "git", "lfs", "pull",
            "--include=composeApp/src/desktopMain/native/windows/runtime/libmpv-2.dll",
            "--exclude=",
            cwd=destination,
        )
    if not dll.is_file() or dll.stat().st_size < 1_000_000 or dll.open("rb").read(2) != b"MZ":
        raise RuntimeError(
            "Windows native libmpv DLL unavailable; refusing to package an invalid MSI."
        )
    if oid and hashlib.sha256(dll.read_bytes()).hexdigest() != oid.group(1):
        raise RuntimeError("Windows libmpv SHA-256 differs from pinned upstream LFS OID.")


def patch_visual_brand(destination: Path) -> None:
    desktop_main = destination / "composeApp/src/desktopMain/kotlin/com/nuvio/app/Main.kt"
    gradle = destination / "composeApp/build.gradle.kts"
    replace_exact(
        desktop_main,
        'title = if (smokePlayerUrl == null) "Nuvio" else "Nuvio Player Smoke",',
        'title = if (smokePlayerUrl == null) "BruniO Desktop 2.2.3" else "BruniO Player Smoke",',
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
    # No upstream Nuvio telemetry or Discord application ID in a BruniO binary.
    replace_exact(desktop_main, "    SentryInitializer.start()", "    // BruniO: upstream Sentry disabled")
    replace_exact(desktop_main, "    DiscordPresenceManager.start()", "    // BruniO: upstream Discord RPC disabled")
    replace_exact(desktop_main, "            SentryInitializer.close()", "            // BruniO: upstream Sentry disabled")
    replace_exact(desktop_main, "            DiscordPresenceManager.shutdown()", "            // BruniO: upstream Discord RPC disabled")
    # Exact Nuvio motion/density/theme code is deliberately not touched.
    # UI-visible identity strings only; retain all upstream attribution/license text.
    resources = destination / "composeApp/src/commonMain/composeResources"
    for translations in resources.glob("values*/strings.xml"):
        txt = translations.read_text(encoding="utf-8")
        txt = txt.replace(">Nuvio<", ">BruniO<").replace(">About Nuvio<", ">About BruniO<")
        translations.write_text(txt, encoding="utf-8")


def patch_brand_icon(destination: Path) -> bool:
    """Replace Nuvio-owned brand marks; leave player vector icons unchanged."""
    icon_source = ROOT / "desktop/brunio-pc/assets/brunio_icon.png"
    logo_source = ROOT / "web/brunio-logo.png"
    if not icon_source.exists() or not logo_source.exists():
        raise FileNotFoundError("BruniO-owned app icon / wordmark is missing")
    try:
        from PIL import Image
    except ImportError as err:
        raise RuntimeError("Pillow is required to safely rebrand visual assets") from err

    icons = destination / "composeApp/src/desktopMain/resources/icons"
    common = destination / "composeApp/src/commonMain/composeResources/drawable"
    from install_tv_assets_rp206 import safe_square
    with Image.open(icon_source) as original:
        # Preserve the exact BruniO artwork, without redesigning it.
        # 19% padding on every side shrunk the Windows shortcut to ~62% of
        # the available canvas; 7% gives the icon a normal Windows presence.
        # safe_square crops existing transparent borders and never stretches.
        app_icon = safe_square(original, 512, margin=0.07)
        app_icon.save(icons / "nuvio-app-icon-transparent.png")
        app_icon.save(
            icons / "nuvio-app-icon-transparent.ico", format="ICO",
            sizes=[(16, 16), (32, 32), (48, 48), (64, 64), (128, 128), (256, 256)],
        )
        # BruniO application identity must be consistent in theme variants.
        # Windows's runtime shortcut icon updater loads variant ICO files.
        # The earlier build only rebranded PNG variants, leaving old Nuvio ICOs.
        for p in sorted(icons.glob("app-icon-*-transparent.ico")):
            app_icon.save(
                p, format="ICO",
                sizes=[(16, 16), (32, 32), (48, 48), (64, 64), (128, 128), (256, 256)],
            )
        for p in sorted(icons.glob("app-icon-*-transparent.png")):
            with Image.open(p) as existing:
                size = existing.size
            app_icon.resize(size, Image.Resampling.LANCZOS).save(p)
        for p in sorted(common.glob("app_icon*.png")):
            with Image.open(p) as existing:
                size = existing.size
            # Keep consistent transparent safe area across all in-app sizes.
            app_icon.resize(size, Image.Resampling.LANCZOS).save(p)
    with Image.open(logo_source) as original_logo:
        logo = original_logo.convert("RGBA")
        bbox = logo.getchannel("A").getbbox()
        if bbox:
            logo = logo.crop(bbox)
        for p in sorted(common.glob("app_logo_wordmark*.png")):
            with Image.open(p) as existing:
                size = existing.size
            stamp = logo.copy()
            stamp.thumbnail((max(1, int(size[0]*0.88)), max(1, int(size[1]*0.82))),
                            Image.Resampling.LANCZOS)
            canvas = Image.new("RGBA", size)
            canvas.alpha_composite(stamp, ((size[0]-stamp.width)//2, (size[1]-stamp.height)//2))
            canvas.save(p)
    # Third-party integration marks (Trakt, Jellyfin, IMDb etc.) remain as
    # upstream, subject to their individual attribution and trademark policies.
    return True


def patch_brunioplayer_explicit_autosync(destination: Path) -> None:
    """Keep Nuvio's player design but make BruniO Autosync exclusively click-driven."""
    root = destination / "composeApp/src/commonMain/kotlin/com/nuvio/app"

    prefs = root / "features/autosync/AutoSyncPreferencesRepository.kt"
    replace_exact(prefs,
        "private val _preferredSubtitleAutoSyncOnStart = MutableStateFlow(true)",
        "private val _preferredSubtitleAutoSyncOnStart = MutableStateFlow(false)")
    replace_exact(prefs,
        "_preferredSubtitleAutoSyncOnStart.value = loadPersistedValue?.invoke() ?: true",
        "_preferredSubtitleAutoSyncOnStart.value = false // BruniO: explicit click only")
    replace_exact(prefs,
        """    fun setPreferredSubtitleAutoSyncOnStart(enabled: Boolean) {
        ensureLoaded()
        if (_preferredSubtitleAutoSyncOnStart.value == enabled) return
        _preferredSubtitleAutoSyncOnStart.value = enabled
        savePersistedValue?.invoke(enabled)
    }""",
        """    fun setPreferredSubtitleAutoSyncOnStart(enabled: Boolean) {
        ensureLoaded()
        _preferredSubtitleAutoSyncOnStart.value = false
        savePersistedValue?.invoke(false)
    }""")

    autosync = root / "features/player/PlayerRuntimeAutoSync.kt"
    replace_exact(autosync,
        "playerController?.setSubtitleUriWithSelectedAutoSync(url)",
        "playerController?.setSubtitleUri(url) // BruniO: regular selection stays regular")

    modal = root / "features/player/SubtitleModal.kt"
    # Reuse the upstream action style for native visual fidelity.
    replace_exact(modal,
        "import com.nuvio.app.core.ui.nuvio\n",
        "import com.nuvio.app.core.ui.nuvio\nimport com.nuvio.app.core.ui.NuvioPrimaryButton\n")
    replace_exact(modal,
        "    onAutoSyncCapture: () -> Unit,\n",
        "    onExplicitAutoSync: () -> Unit,\n    onAutoSyncCapture: () -> Unit,\n")
    replace_exact(modal,
        """                            Column(
                                modifier = Modifier
                                    .heightIn(max = railMaxHeight)
                                    .verticalScroll(rememberScrollState()),
                            ) {
                                SubtitleStylePanel(""",
        """                            Column(
                                modifier = Modifier
                                    .heightIn(max = railMaxHeight)
                                    .verticalScroll(rememberScrollState()),
                            ) {
                                if (effectiveSelectedAddonSubtitle != null) {
                                    NuvioPrimaryButton(
                                        text = "Autosync · Detect timing",
                                        onClick = onExplicitAutoSync,
                                    )
                                }
                                SubtitleStylePanel(""")

    hosts = root / "features/player/PlayerScreenModalHosts.kt"
    replace_exact(hosts,
        "    onAutoSyncCapture: () -> Unit,\n",
        "    onExplicitAutoSync: () -> Unit,\n    onAutoSyncCapture: () -> Unit,\n")
    replace_exact(hosts,
        "        onAutoSyncCapture = onAutoSyncCapture,\n",
        "        onExplicitAutoSync = onExplicitAutoSync,\n        onAutoSyncCapture = onAutoSyncCapture,\n")

    ui = root / "features/player/PlayerScreenRuntimeUi.kt"
    replace_exact(ui,
        "        onAutoSyncCapture = { captureSubtitleAutoSyncTime() },\n",
        "        onExplicitAutoSync = { runBrunioExplicitAutoSync() },\n        onAutoSyncCapture = { captureSubtitleAutoSyncTime() },\n")


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
    # Nuvio's Gradle 9 generated-config task declares local.properties as an
    # input even without credentials, so it must always exist.
    (destination / "local.properties").touch(exist_ok=True)
    url = os.environ.get("BRUNIO_SUPABASE_URL", "").strip()
    key = os.environ.get("BRUNIO_SUPABASE_PUBLISHABLE_KEY", "").strip()
    if not url or not key:
        # Reuse the *public* BruniO project configuration already checked into
        # its existing PC client. Do not hardcode private account tokens here.
        import ast
        source = ROOT / "desktop/brunio-pc/brunio/core.py"
        allowed = {"SUPABASE_URL": "url", "SUPABASE_KEY": "key"}
        parsed = {}
        for node in ast.parse(source.read_text(encoding="utf-8")).body:
            if not isinstance(node, ast.Assign) or not isinstance(node.value, ast.Constant):
                continue
            for target in node.targets:
                if isinstance(target, ast.Name) and target.id in allowed and isinstance(node.value.value, str):
                    parsed[target.id] = node.value.value.strip()
        url = url or parsed.get("SUPABASE_URL", "")
        key = key or parsed.get("SUPABASE_KEY", "")
    if not url or not key:
        raise RuntimeError("BruniO public Supabase URL and publishable key unavailable")
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
    patch_brunioplayer_explicit_autosync(output)
    from patch_upstream import apply as patch_upstream_compatibility
    patch_upstream_compatibility(output)
    from patch_runtime import install as patch_brunio_runtime
    patch_brunio_runtime(output)
    from patch_party import apply as patch_brunio_party
    patch_brunio_party(output)
    from patch_login import apply as patch_brunio_login
    patch_brunio_login(output)
    from patch_catalog import apply as patch_brunio_catalog
    patch_brunio_catalog(output)
    from patch_streams import apply as patch_brunio_streams
    patch_brunio_streams(output)
    from patch_state import apply as patch_brunio_state
    patch_brunio_state(output)
    from patch_playback_party import apply as patch_brunio_playback_party
    patch_brunio_playback_party(output)
    from patch_account_setup import apply as patch_brunio_account_setup
    patch_brunio_account_setup(output)
    from patch_tests import apply as patch_brunio_test_policy
    patch_brunio_test_policy(output)
    from patch_home_playback import apply as patch_brunio_home_playback
    patch_brunio_home_playback(output)
    from patch_v220_home import apply as patch_v220_home
    patch_v220_home(output)
    from patch_v221_home import apply as patch_v221_home
    patch_v221_home(output)
    icon_ok = patch_brand_icon(output)
    from patch_v221_brand import apply as patch_v221_brand
    patch_v221_brand(output)
    files_added = install_brunit_overlay(output)
    # These integration patches touch both the pinned upstream and BruniO's
    # typed overlay, so run only once both trees have been staged.
    from patch_party_rp206 import apply as patch_party_rp206
    patch_party_rp206(output)
    from patch_library_rp206 import apply as patch_library_rp206
    patch_library_rp206(output)
    from patch_collections_rp206 import apply as patch_collections_rp206
    patch_collections_rp206(output)
    from patch_livetv_rp206 import apply as patch_livetv_rp206
    patch_livetv_rp206(output)
    from patch_avatars_rp206 import apply as patch_avatars_rp206
    patch_avatars_rp206(output)
    from patch_v220_foundations import apply as patch_v220_foundations
    patch_v220_foundations(output)
    from patch_v220_party import apply as patch_v220_party
    patch_v220_party(output)
    from patch_v221_party import apply as patch_v221_party
    patch_v221_party(output)
    from patch_v221_player_sources import apply as patch_v221_player_sources
    patch_v221_player_sources(output)
    from patch_v220_multiview import apply as patch_v220_multiview
    patch_v220_multiview(output)
    from patch_v221_multiview import apply as patch_v221_multiview
    patch_v221_multiview(output)
    from patch_v222_sports import apply as patch_v222_sports
    patch_v222_sports(output)
    from patch_v220_floating_pip import apply as patch_v220_floating_pip
    patch_v220_floating_pip(output)
    from install_tv_assets_rp206 import install as install_tv_assets
    tv_assets = install_tv_assets(output)
    from install_v220_assets import install as install_v220_assets
    streaming_assets = install_v220_assets(output)
    backend = configure_public_backend(output)
    print("Pinned upstream:", PIN)
    print("Prepared directory:", output)
    print("Windows icon patched:", icon_ok)
    print("Overlay files copied:", files_added)
    print("Original TV avatar assets installed:", tv_assets)
    print("Live TV channel list copied from BruniO TV.")
    print("TV streaming art files installed:", streaming_assets)
    print("TV Sports add-on default bundled.")
    print("Public Supabase auth endpoint configured:", backend)
    print("STATUS: BruniO Desktop service integration candidate (requires account/device acceptance testing).")
    print("CHECK: Verify sign-in, TMDB, stream providers and TV/Mobile interoperability before release.")


if __name__ == "__main__":
    main()
