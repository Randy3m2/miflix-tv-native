"""Connect real BruniO TV provider rows, custom Home controls and ratings.

Run after patch_home_playback; use exact anchors and fail rather than silently
destroying the pinned Nuvio UI layout when upstream changes.
"""
from pathlib import Path
from patch_runtime import once


def apply(root: Path) -> None:
    app = root / "composeApp/src/commonMain/kotlin/com/nuvio/app"
    repo = app / "features/home/HomeRepository.kt"
    once(repo,
         "import com.nuvio.app.features.brunio.BrunioTmdb",
         "import kotlinx.coroutines.CancellationException\n"
         "import com.nuvio.app.features.brunio.BrunioTmdb\n"
         "import com.nuvio.app.features.brunio.BrunioHomeExtras\n"
         "import com.nuvio.app.features.brunio.BrunioRatingsRecommendations")
    once(repo,
         """                val results = responses.flatMap { it.first }
                val failedFeeds = responses.count { it.second != null }""",
         """                val results = responses.flatMap { it.first }
                val failedFeeds = responses.count { it.second != null }""")
    once(repo,
         """                    sections = results,
                    errorMessage = if (results.isNotEmpty()) null else when {""",
         """                    sections = results,
                    errorMessage = if (results.isNotEmpty()) null else when {""")
    once(repo,
         """                        else -> "TMDB no devolvió resultados. Pulsa Reintentar."
                    },
                )
            }
            return""",
         """                        else -> "TMDB no devolvió resultados. Pulsa Reintentar."
                    },
                )
                // Core Home rows render immediately. Optional provider rows and
                // profile-private recommendations stream in without blocking them.
                val optional = try {
                    BrunioHomeExtras.load()
                } catch (cancel: CancellationException) {
                    throw cancel
                } catch (_: Exception) { emptyList() }
                val recommendations = try {
                    BrunioRatingsRecommendations.load()
                } catch (cancel: CancellationException) {
                    throw cancel
                } catch (_: Exception) { emptyList() }
                val allRows = results + recommendations + optional
                if (allRows.isNotEmpty()) {
                    _uiState.value = _uiState.value.copy(sections = allRows)
                }
            }
            return""")
    home = app / "features/home/HomeScreen.kt"
    once(home,
         "import com.nuvio.app.features.brunio.BrunioCloud",
         "import com.nuvio.app.features.brunio.BrunioCloud\n"
         "import com.nuvio.app.features.brunio.BrunioStreamingRow")
    once(home,
         """                    if (BrunioCloud.enabled) {
                        homeUiState.sections.forEach { section ->""",
         """                    if (BrunioCloud.enabled) {
                        item(key = "brunio_streaming_tv_row", contentType = "streaming") {
                            BrunioStreamingRow(onRefresh = {
                                HomeRepository.refresh(
                                    addonsUiState.addons.enabledAddons(), force = true,
                                )
                            })
                        }
                        homeUiState.sections.forEach { section ->""")
