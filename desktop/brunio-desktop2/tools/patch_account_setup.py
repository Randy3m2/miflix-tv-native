"""Persist BruniO Desktop TMDB and add-on edits back to shared account setup."""
from pathlib import Path
from patch_runtime import once


def apply(root: Path) -> None:
    app = root / "composeApp/src/commonMain/kotlin/com/nuvio/app"

    addon = app / "features/addons/AddonRepository.kt"
    once(addon, "import com.nuvio.app.features.brunio.BrunioCloud\n",
         "import com.nuvio.app.features.brunio.BrunioCloud\n"
         "import com.nuvio.app.features.brunio.BrunioStateWriter\n")
    once(addon,
         '''        if (BrunioCloud.enabled) return // Never call Nuvio sync_push_addons on BruniO
        if (isUsingPrimaryAddonsFromSecondaryProfile()) return''',
         '''        if (BrunioCloud.enabled) {
            val profile = currentProfileId
            val urls = _uiState.value.addons.map(ManagedAddon::manifestUrl)
            pushJobsByProfile[profile]?.cancel()
            val job = scope.launch {
                delay(ADDON_PUSH_DEBOUNCE_MS)
                try {
                    BrunioStateWriter.saveAddonManifests(urls)
                } catch (error: Throwable) {
                    log.e(error) { "Unable to sync BruniO addon settings" }
                }
            }
            pushJobsByProfile[profile] = job
            return
        }
        if (isUsingPrimaryAddonsFromSecondaryProfile()) return''')

    tmdb = app / "features/tmdb/TmdbSettingsRepository.kt"
    once(tmdb, "import com.nuvio.app.features.details.MetaDetailsRepository",
         "import com.nuvio.app.features.brunio.BrunioCloud\n"
         "import com.nuvio.app.features.brunio.BrunioStateWriter\n"
         "import com.nuvio.app.features.details.MetaDetailsRepository")
    once(tmdb, "import kotlinx.coroutines.flow.MutableStateFlow",
         "import co.touchlab.kermit.Logger\n"
         "import kotlinx.coroutines.CoroutineScope\n"
         "import kotlinx.coroutines.Dispatchers\n"
         "import kotlinx.coroutines.SupervisorJob\n"
         "import kotlinx.coroutines.launch\n"
         "import kotlinx.coroutines.flow.MutableStateFlow")
    once(tmdb,
         '''object TmdbSettingsRepository {
    private val _uiState''',
         '''object TmdbSettingsRepository {
    private val brunioSyncScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val brunioLog = Logger.withTag("BrunioTmdbSettings")
    private val _uiState''')
    once(tmdb,
         '''        TmdbSettingsStorage.saveApiKey(normalized)
        invalidateMetadata()''',
         '''        TmdbSettingsStorage.saveApiKey(normalized)
        invalidateMetadata()
        if (BrunioCloud.enabled) {
            brunioSyncScope.launch {
                try {
                    BrunioStateWriter.saveTmdbToken(normalized)
                } catch (error: Throwable) {
                    brunioLog.e(error) { "Unable to sync BruniO TMDB credentials" }
                }
            }
        }''')
