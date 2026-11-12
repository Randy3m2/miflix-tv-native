"""Fix BruniO Desktop real-release UX foundations without altering TV/Mobile.

BruniO screens are dark while upstream Nuvio can supply light MaterialTheme colors.
TV avatar images are bundled inside the Desktop JAR, extracted to local disk;
Coil's standard desktop file fetcher expects an actual File object.
"""
from pathlib import Path
from patch_runtime import once


def apply(root: Path) -> None:
    app = root / "composeApp/src/commonMain/kotlin/com/nuvio/app"
    gate = app / "AppGate.kt"
    once(gate,
         "        if (!BrunioCloud.enabled) AvatarRepository.fetchAvatars()",
         "        AvatarRepository.fetchAvatars() // BruniO uses bundled TV avatars, no Nuvio RPC")

    picker = app / "features/profiles/AvatarPicker.kt"
    once(picker, "import coil3.compose.AsyncImage",
         "import com.nuvio.app.core.ui.NuvioAsyncImage as AsyncImage")

    renderer = root / "composeApp/src/desktopMain/kotlin/com/nuvio/app/core/ui/AsyncImage.desktop.kt"
    once(renderer, "import androidx.compose.foundation.hoverable",
         "import java.io.File\nimport java.net.URI\nimport androidx.compose.foundation.hoverable")
    once(renderer,
         """    val requestModel = remember(context, model, effectiveDesktopImageScaling, animated) {
        model.withDesktopImageOptions(""",
         """    val localBrunioAvatarFile = remember(model) {
        if (model is String && model.startsWith("file:") &&
            (model.contains("brunio-desktop2-avatars/") ||
                model.contains("brunio-desktop2-streaming/"))
        ) {
            runCatching { File(URI(model)) }.getOrNull()?.takeIf { it.isFile }
        } else null
    }
    val supportedModel = localBrunioAvatarFile ?: model
    val requestModel = remember(context, supportedModel, effectiveDesktopImageScaling, animated) {
        supportedModel.withDesktopImageOptions(""")
