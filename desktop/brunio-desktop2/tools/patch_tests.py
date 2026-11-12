"""Legacy Nuvio Stremio-search assertions don't apply to BruniO TMDB search."""
from pathlib import Path
from patch_runtime import once

def apply(root: Path) -> None:
    path = root / "composeApp/src/commonTest/kotlin/com/nuvio/app/features/search/SearchRequestStateTest.kt"
    once(path, "import com.nuvio.app.features.addons.AddonManifest",
         "import com.nuvio.app.features.brunio.BrunioCloud\n"
         "import com.nuvio.app.features.addons.AddonManifest")
    once(path, "    fun `unresolved addon stays loading before surfacing manifest failure`() {", "    fun `unresolved addon stays loading before surfacing manifest failure`() {\n        if (BrunioCloud.enabled) return // BruniO searches TMDB, not addon catalogs")
    once(path, "    fun `pending addon settles to catalog capability empty states`() {", "    fun `pending addon settles to catalog capability empty states`() {\n        if (BrunioCloud.enabled) return // BruniO searches TMDB, not addon catalogs")
