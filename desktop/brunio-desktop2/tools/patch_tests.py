"""Legacy Nuvio Stremio-search tests don't apply to BruniO TMDB search."""
from pathlib import Path
from patch_runtime import once

def apply(root: Path) -> None:
    path = root / "composeApp/src/commonTest/kotlin/com/nuvio/app/features/search/SearchRequestStateTest.kt"
    once(path, "import com.nuvio.app.features.addons.AddonManifest",
         "import com.nuvio.app.features.brunio.BrunioCloud\n"
         "import com.nuvio.app.features.addons.AddonManifest")
    once(path, "        val pendingAddon = ManagedAddon(\n",
         "        if (BrunioCloud.enabled) return // BruniO searches TMDB, not Stremio catalogs\n"
         "        val pendingAddon = ManagedAddon(\n")
    once(path, "        val loadedAddon = ManagedAddon(\n",
         "        if (BrunioCloud.enabled) return // BruniO searches TMDB, not Stremio catalogs\n"
         "        val loadedAddon = ManagedAddon(\n")
