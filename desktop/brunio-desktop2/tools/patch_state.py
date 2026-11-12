"""Route writes to BruniO user state, never to Nuvio sync tables."""
from pathlib import Path
from patch_runtime import once


def apply(root: Path) -> None:
    app = root / "composeApp/src/commonMain/kotlin/com/nuvio/app"

    profile = app / "features/profiles/ProfileRepository.kt"
    once(profile, "import com.nuvio.app.features.brunio.BrunioCloud\n",
         "import com.nuvio.app.features.brunio.BrunioCloud\n"
         "import com.nuvio.app.features.brunio.BrunioStateWriter\n")
    once(profile,
         '''        if (BrunioCloud.enabled) return // Remote profile editing disabled until BruniO-safe writer''',
         '''        if (BrunioCloud.enabled) {
            try {
                BrunioStateWriter.saveProfiles(profiles)
                pullProfiles()
            } catch (error: Throwable) {
                log.e(error) { "Unable to save BruniO profiles" }
            }
            return
        }''')
    once(profile,
         '''        if (BrunioCloud.enabled) return // Never delete BruniO profile via Nuvio RPC''',
         '''        if (BrunioCloud.enabled) {
            try {
                BrunioStateWriter.deleteProfile(profileIndex)
                pullProfiles()
            } catch (error: Throwable) {
                log.e(error) { "Unable to remove BruniO profile" }
            }
            return
        }''')

    library = app / "features/library/LibraryRepository.kt"
    once(library, "import com.nuvio.app.features.brunio.BrunioMediaStateWire\n",
         "import com.nuvio.app.features.brunio.BrunioMediaStateWire\n"
         "import com.nuvio.app.features.brunio.BrunioStateWriter\n")
    once(library,
         '''        if (BrunioCloud.enabled) return // Nuvio library RPCs are not BruniO-compatible
        if (!snapshot.hasPendingPush) return''',
         '''        if (BrunioCloud.enabled) {
            if (snapshot.hasPendingPush) {
                syncScope.launch {
                    try {
                        BrunioStateWriter.saveFavorites(snapshot.token.profileId, snapshot.items)
                        localState.markPushCompleted(snapshot)?.let(::persist)
                    } catch (error: Throwable) {
                        log.e(error) { "Unable to sync BruniO favorites" }
                    }
                }
            }
            return
        }
        if (!snapshot.hasPendingPush) return''')

    progress = app / "features/watchprogress/WatchProgressRepository.kt"
    once(progress, "import com.nuvio.app.features.brunio.BrunioMediaStateWire\n",
         "import com.nuvio.app.features.brunio.BrunioMediaStateWire\n"
         "import com.nuvio.app.features.brunio.BrunioStateWriter\n")
    once(progress,
         '''        if (BrunioCloud.enabled) return // Keep local until safe RP1 profile-state merge is tested
        if (ServerItemRef.isServerId(entry.parentMetaId)) return''',
         '''        if (BrunioCloud.enabled) {
            val generation = profileGeneration.takeIf { profileId == currentProfileId }
            accountScopeSnapshot().launch {
                try {
                    BrunioStateWriter.saveProgress(profileId, entry)
                    recordSuccessfulPush(
                        profileId = profileId,
                        operationGeneration = generation,
                        entries = listOf(entry),
                    )
                } catch (error: Throwable) {
                    log.e(error) { "Unable to sync BruniO playback progress" }
                }
            }
            return
        }
        if (ServerItemRef.isServerId(entry.parentMetaId)) return''')
    once(progress,
         '''        if (BrunioCloud.enabled) return // Never delete via incompatible Nuvio sync
        if (activeSource.providerId != null) return''',
         '''        if (BrunioCloud.enabled) {
            val profileId = currentProfileId
            accountScopeSnapshot().launch {
                try {
                    BrunioStateWriter.deleteProgress(profileId, entries)
                } catch (error: Throwable) {
                    log.e(error) { "Unable to delete BruniO playback history" }
                }
            }
            return
        }
        if (activeSource.providerId != null) return''')

    watched = app / "features/watched/WatchedRepository.kt"
    once(watched,
         "import co.touchlab.kermit.Logger",
         "import com.nuvio.app.features.brunio.BrunioCloud\nimport co.touchlab.kermit.Logger")
    once(watched,
         '''    suspend fun pullFromServer(profileId: Int) {''',
         '''    suspend fun pullFromServer(profileId: Int) {
        if (BrunioCloud.enabled) return // BruniO watched status derives from synced progress''')
    once(watched,
         '''    private fun pushMarksToServer(''',
         '''    private fun pushMarksToServer(''')  # anchor availability check only
