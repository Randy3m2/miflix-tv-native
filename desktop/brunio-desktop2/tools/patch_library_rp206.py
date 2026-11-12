"""Hydrate saved BruniO TMDB IDs without re-adding/deleting library members."""
from pathlib import Path
from patch_runtime import once


def apply(root: Path) -> None:
    app = root / "composeApp/src/commonMain/kotlin/com/nuvio/app"
    state = app / "features/library/LibraryLocalState.kt"
    once(state, "    fun applyDeltaEvents(\n",
         '''    /**
     * Metadata-only update; preserve saved status, membership deltas,
     * source identifiers and saved timestamps.
     */
    fun enrichMetadata(profileId: Int, enriched: Collection<LibraryItem>): LibraryLocalSnapshot? =
        synchronized(lock) {
            if (!hasLoaded || currentProfileId != profileId) return@synchronized null
            var changed = false
            enriched.forEach { meta ->
                val key = libraryItemKey(meta.id, meta.type)
                val previous = itemsById[key] ?: return@forEach
                val updated = previous.copy(
                    name = if (previous.name.startsWith("Movie ") ||
                        previous.name.startsWith("Series ")) meta.name else previous.name,
                    poster = previous.poster ?: meta.poster,
                    banner = previous.banner ?: meta.banner,
                    description = previous.description ?: meta.description,
                    releaseInfo = previous.releaseInfo ?: meta.releaseInfo,
                )
                if (updated != previous) {
                    itemsById[key] = updated
                    changed = true
                }
            }
            if (!changed) return@synchronized null
            revision += 1L
            contentRevision += 1L
            snapshotLocked()
        }

    fun applyDeltaEvents(
''')
    library = app / "features/library/LibraryRepository.kt"
    once(library, "    fun ensureLoaded() {\n",
         '''    /** BruniO background metadata resolution; never emits a library push. */
    fun applyBrunioMetadata(enriched: Collection<LibraryItem>) {
        if (enriched.isEmpty()) return
        val current = ProfileRepository.activeProfileId
        val snapshot = localState.enrichMetadata(current, enriched) ?: return
        persist(snapshot)
        publish()
    }

    fun ensureLoaded() {
''')
    cloud = root / "composeApp/src/commonMain/kotlin/com/nuvio/app/features/brunio/BrunioCloud.kt"
    once(cloud,
         '''        LibraryRepository.applyBrunioSnapshot(BrunioMediaStateWire.favoriteItems(state))
        WatchProgressRepository.applyBrunioSnapshot''',
         '''        val favorites = BrunioMediaStateWire.favoriteItems(state)
        LibraryRepository.applyBrunioSnapshot(favorites)
        BrunioLibraryHydrator.refresh(index, favorites)
        WatchProgressRepository.applyBrunioSnapshot''')
