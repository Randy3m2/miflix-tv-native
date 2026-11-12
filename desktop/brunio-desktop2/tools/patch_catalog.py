"""Replace Nuvio's Stremio-only discovery with BruniO TMDB discovery.

Sources (Torrentio, Comet, etc.) are fetched separately by StreamsRepository.
Keep Nuvio's existing Home/Search UI and all poster click navigation intact.
"""
from pathlib import Path
from patch_runtime import once


def apply(root: Path) -> None:
    app = root / "composeApp/src/commonMain/kotlin/com/nuvio/app"
    home = app / "features/home/HomeRepository.kt"
    once(home,
        "import com.nuvio.app.features.addons.ManagedAddon",
        "import com.nuvio.app.features.brunio.BrunioCloud\n"
        "import com.nuvio.app.features.brunio.BrunioTmdb\n"
        "import com.nuvio.app.features.addons.ManagedAddon")
    once(home,
        '''    fun refresh(addons: List<ManagedAddon>, force: Boolean = false) {
        val activeAddons''',
        '''    fun refresh(addons: List<ManagedAddon>, force: Boolean = false) {
        if (BrunioCloud.enabled) {
            activeJob?.cancel()
            _uiState.value = HomeUiState(isLoading = true)
            activeJob = scope.launch {
                val feeds = listOf(
                    Triple("trending/all/week", "Tendencias", ""),
                    Triple("movie/popular", "Películas populares", "movie"),
                    Triple("tv/popular", "Series populares", "series"),
                    Triple("movie/top_rated", "Mejor valoradas", "movie"),
                    Triple("tv/top_rated", "Series destacadas", "series"),
                )
                val results = feeds.map { (endpoint, name, kind) ->
                    async {
                        val rows = runCatching {
                            BrunioTmdb.catalog(endpoint, kind.takeIf(String::isNotBlank))
                        }.getOrDefault(emptyList())
                        val actualKind = kind.ifBlank { "movie" }
                        val sections = if (kind.isBlank()) BrunioTmdb.sections(name, rows)
                            else listOf(BrunioTmdb.section(endpoint, name, actualKind, rows))
                        sections.filter { it.items.isNotEmpty() }
                    }
                }.awaitAll().flatten()
                _uiState.value = HomeUiState(
                    isLoading = false,
                    heroItems = results.flatMap { it.items }.distinctBy { it.id }.take(14),
                    sections = results,
                    errorMessage = if (results.isEmpty()) {
                        "Sin catálogo de TMDB. Revisa tu token y conexión."
                    } else null,
                )
            }
            return
        }
        val activeAddons''')

    search = app / "features/search/SearchRepository.kt"
    once(search,
        "import com.nuvio.app.core.i18n.localizedMediaTypeLabel",
        "import com.nuvio.app.features.brunio.BrunioCloud\n"
        "import com.nuvio.app.features.brunio.BrunioTmdb\n"
        "import com.nuvio.app.core.i18n.localizedMediaTypeLabel")
    once(search,
        '''        val normalizedQuery = query.trim()
        if (normalizedQuery.isBlank()) {
            clear()
            return
        }

        val enabledAddons''',
        '''        val normalizedQuery = query.trim()
        if (normalizedQuery.isBlank()) {
            clear()
            return
        }
        if (BrunioCloud.enabled) {
            activeJob?.cancel()
            _uiState.value = SearchUiState(isLoading = true)
            activeJob = scope.launch {
                try {
                    val items = BrunioTmdb.search(normalizedQuery)
                    val sections = BrunioTmdb.sections("Resultados", items)
                    _uiState.value = SearchUiState(
                        sections = sections,
                        emptyStateReason = if (sections.isEmpty()) SearchEmptyStateReason.NoResults else null,
                    )
                } catch (cancel: CancellationException) {
                    throw cancel
                } catch (error: Throwable) {
                    _uiState.value = SearchUiState(
                        emptyStateReason = SearchEmptyStateReason.RequestFailed,
                        errorMessage = "No se pudo consultar TMDB. Revisa tu token.",
                    )
                }
            }
            return
        }

        val enabledAddons''')
    once(search,
        '''    fun refreshDiscover(
        addons: List<ManagedAddon>,
        forceRefresh: Boolean = false,
    ) {
        val enabledAddons''',
        '''    private fun refreshBrunioDiscover(kind: String) {
        activeDiscoverJob?.cancel()
        val catalog = DiscoverCatalogOption(
            key = "brunio-tmdb-$kind",
            addonName = "TMDB",
            manifestUrl = "https://api.themoviedb.org/3",
            type = kind,
            catalogId = "trending",
            catalogName = "Tendencias",
        )
        _discoverUiState.value = DiscoverUiState(
            typeOptions = listOf("movie", "series"),
            selectedType = kind,
            catalogOptions = listOf(catalog),
            selectedCatalogKey = catalog.key,
            isLoading = true,
        )
        activeDiscoverJob = scope.launch {
            try {
                val endpoint = if (kind == "series") "trending/tv/week" else "trending/movie/week"
                val items = BrunioTmdb.catalog(endpoint, kind)
                _discoverUiState.value = _discoverUiState.value.copy(
                    items = items,
                    isLoading = false,
                    emptyStateReason = if (items.isEmpty()) DiscoverEmptyStateReason.NoResults else null,
                )
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (error: Throwable) {
                _discoverUiState.value = _discoverUiState.value.copy(
                    isLoading = false,
                    emptyStateReason = DiscoverEmptyStateReason.RequestFailed,
                    errorMessage = "No se pudo cargar TMDB.",
                )
            }
        }
    }

    fun refreshDiscover(
        addons: List<ManagedAddon>,
        forceRefresh: Boolean = false,
    ) {
        if (BrunioCloud.enabled) {
            refreshBrunioDiscover(_discoverUiState.value.selectedType ?: "movie")
            return
        }
        val enabledAddons''')
    once(search,
        '''    fun selectDiscoverType(type: String) {
        val current = _discoverUiState.value''',
        '''    fun selectDiscoverType(type: String) {
        if (BrunioCloud.enabled) {
            if (type in setOf("movie", "series")) refreshBrunioDiscover(type)
            return
        }
        val current = _discoverUiState.value''')
    once(search,
        '''    fun selectDiscoverCatalog(catalogKey: String) {
        val current = _discoverUiState.value''',
        '''    fun selectDiscoverCatalog(catalogKey: String) {
        if (BrunioCloud.enabled) return
        val current = _discoverUiState.value''')
    once(search,
        '''    fun selectDiscoverGenre(genre: String?) {
        val current = _discoverUiState.value''',
        '''    fun selectDiscoverGenre(genre: String?) {
        if (BrunioCloud.enabled) return
        val current = _discoverUiState.value''')
    once(search,
        '''    fun loadMoreDiscover() {
        val current = _discoverUiState.value''',
        '''    fun loadMoreDiscover() {
        if (BrunioCloud.enabled) return
        val current = _discoverUiState.value''')

    # Nuvio previously parsed only "tmdb:123" for standalone details.
    # BruniO uses "tmdb:movie:123"/"tmdb:series:123" throughout the UI.
    details = app / "features/tmdb/TmdbMetadataService.kt"
    once(details,
         '''        val tmdbId = id
            .takeIf { it.startsWith("tmdb:", ignoreCase = true) }
            ?.substringAfter(':')
            ?.substringBefore(':')
            ?.toIntOrNull()
            ?: return null''',
         '''        val tmdbId = id
            .takeIf { it.startsWith("tmdb:", ignoreCase = true) }
            ?.substringAfterLast(':')
            ?.toIntOrNull()
            ?: return null''')

    # Nuvio's metadata endpoints default to a "v3 api_key" URL. BruniO also
    # supports TMDB's v4 access token (JWT) and must use a Bearer header for it.
    for rel in ["features/tmdb/TmdbMetadataService.kt", "features/tmdb/TmdbService.kt"]:
        path = app / rel
        once(path, "import co.touchlab.kermit.Logger",
             "import com.nuvio.app.features.brunio.BrunioCloud\n"
             "import com.nuvio.app.features.brunio.BrunioTmdb\n"
             "import co.touchlab.kermit.Logger")
        once(path, "json.decodeFromString<T>(httpGetText(url))",
             "json.decodeFromString<T>(if (BrunioCloud.enabled) BrunioTmdb.fetchRaw(endpoint, query) else httpGetText(url))")
