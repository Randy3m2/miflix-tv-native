"""Store Nuvio-style Desktop Collections in BruniO per-profile JSON state.

No Nuvio sync_pull_collections or sync_push_collections calls are sent.
Collections remain editable in Nuvio's existing editor and can include TMDB
sources; desktopCollections is intentionally distinct from TV app fields.
"""
from patch_runtime import once


def apply(root):
    app = root / "composeApp/src/commonMain/kotlin/com/nuvio/app"
    writer = root / "composeApp/src/commonMain/kotlin/com/nuvio/app/features/brunio/BrunioStateWriter.kt"
    once(writer, "    suspend fun saveProfiles(payloads: List<ProfilePushPayload>)",
         '''    suspend fun saveDesktopCollections(profileIndex: Int, payload: kotlinx.serialization.json.JsonElement) =
        writer.withLock {
            require(payload is JsonArray && payload.size <= 200)
            val (user, profile) = context(profileIndex)
            val remote = BrunioCloud.loadProfileState(profileIndex)
            save(user, profile, JsonObject(remote + ("desktopCollections" to payload)))
        }

    suspend fun saveProfiles(payloads: List<ProfilePushPayload>)''')
    sync = app / "features/collection/CollectionSyncService.kt"
    once(sync, "import com.nuvio.app.core.auth.AuthRepository",
         "import com.nuvio.app.features.brunio.BrunioCloud\n"
         "import com.nuvio.app.features.brunio.BrunioStateWriter\n"
         "import com.nuvio.app.core.auth.AuthRepository")
    once(sync,
         '''    suspend fun pullFromServer(profileId: Int) {
        if (ProfileRepository.activeProfileId != profileId) return''',
         '''    suspend fun pullFromServer(profileId: Int) {
        if (ProfileRepository.activeProfileId != profileId) return
        if (BrunioCloud.enabled) {
            runCatching {
                val remote = BrunioCloud.loadProfileState(profileId)
                val data = remote["desktopCollections"] as? JsonArray ?: return@runCatching
                val decoded = json.decodeFromString<List<Collection>>(data.toString())
                if (ProfileRepository.activeProfileId != profileId) return@runCatching
                isSyncingFromRemote = true
                try {
                    CollectionRepository.applyFromRemote(decoded, data)
                } finally {
                    isSyncingFromRemote = false
                }
            }.onFailure { error ->
                isSyncingFromRemote = false
                log.w(error) { "Unable to load BruniO Desktop collections" }
            }
            return
        }''')
    once(sync,
         '''    private suspend fun pushToRemote(profileId: Int) {
        runCatching {''',
         '''    private suspend fun pushToRemote(profileId: Int) {
        if (BrunioCloud.enabled) {
            runCatching {
                if (ProfileRepository.activeProfileId != profileId) return@runCatching
                val jsonValue = json.parseToJsonElement(CollectionRepository.exportToJson())
                BrunioStateWriter.saveDesktopCollections(profileId, jsonValue)
            }.onFailure { error ->
                log.w(error) { "Unable to save BruniO Desktop collections" }
            }
            return
        }
        runCatching {''')
    cloud = root / "composeApp/src/commonMain/kotlin/com/nuvio/app/features/brunio/BrunioCloud.kt"
    once(cloud, "import com.nuvio.app.features.library.LibraryRepository",
         "import com.nuvio.app.features.collection.CollectionRepository\n"
         "import com.nuvio.app.features.collection.CollectionSyncService\n"
         "import com.nuvio.app.features.library.LibraryRepository")
    once(cloud,
         '''        BrunioLibraryHydrator.refresh(index, favorites)
        WatchProgressRepository.applyBrunioSnapshot''',
         '''        BrunioLibraryHydrator.refresh(index, favorites)
        CollectionRepository.initialize()
        CollectionSyncService.pullFromServer(index)
        CollectionSyncService.startObserving()
        WatchProgressRepository.applyBrunioSnapshot''')

    tmdb = app / "features/collection/TmdbCollectionSourceResolver.kt"
    once(tmdb, "import com.nuvio.app.features.addons.httpGetText",
         "import com.nuvio.app.features.brunio.BrunioCloud\n"
         "import com.nuvio.app.features.brunio.BrunioTmdb\n"
         "import com.nuvio.app.features.addons.httpGetText")
    once(tmdb, "json.decodeFromString<T>(httpGetText(url))",
         "json.decodeFromString<T>(if (BrunioCloud.enabled) "
         "BrunioTmdb.fetchRaw(endpoint, query) else httpGetText(url))")
