"""BruniO-specific TMDB Home rows and honest source-selection availability.

Nuvio only renders addon-defined catalog rows, but BruniO loads catalogs from
TMDB. Nuvio also checks TMDB IDs *before* BruniO can convert to IMDb stream IDs.
Fail closed when upstream anchors change; do not alter Nuvio on main.
"""
from pathlib import Path
from patch_runtime import once


def apply(root: Path) -> None:
    app = root / "composeApp/src/commonMain/kotlin/com/nuvio/app"

    main = app / "MainAppContent.kt"
    assert "import com.nuvio.app.features.brunio.BrunioCloud" in main.read_text(encoding="utf-8")
    once(main,
        """        if (enabledAddons.isWaitingForFirstEnabledManifest()) return@LaunchedEffect
        HomeCatalogSettingsRepository.syncCatalogs(enabledAddons)
        HomeRepository.refresh(enabledAddons)""",
        """        // TMDB discovery must not be blocked by a pending stream add-on manifest.
        if (!BrunioCloud.enabled && enabledAddons.isWaitingForFirstEnabledManifest()) {
            return@LaunchedEffect
        }
        if (!BrunioCloud.enabled) HomeCatalogSettingsRepository.syncCatalogs(enabledAddons)
        HomeRepository.refresh(enabledAddons)""")

    home = app / "features/home/HomeScreen.kt"
    once(home,
        "import com.nuvio.app.features.addons.AddonRepository",
        "import com.nuvio.app.features.brunio.BrunioCloud\n"
        "import com.nuvio.app.features.addons.AddonRepository")
    once(home,
        """                !hasActiveAddons && !hasServerLibraries && !hasRenderableCollectionRows -> {""",
        """                !BrunioCloud.enabled && !hasActiveAddons &&
                    !hasServerLibraries && !hasRenderableCollectionRows -> {""")
    once(home,
        """                    keyedEnabledHomeItems.forEach { keyedSettingsItem ->""",
        """                    // TMDB rows do not have Nuvio addon catalog settings keys.
                    if (BrunioCloud.enabled) {
                        homeUiState.sections.forEach { section ->
                            if (section.items.isNotEmpty()) {
                                item(key = "brunio_home_" + section.key, contentType = "catalog") {
                                    HomeCatalogRowSection(
                                        section = section,
                                        entries = section.items,
                                        modifier = Modifier.padding(bottom = 12.dp),
                                        sectionPadding = homeSectionPadding,
                                        onViewAllClick = null,
                                        watchedKeys = watchedUiState.watchedKeys,
                                        fullyWatchedSeriesKeys = fullyWatchedSeriesKeys,
                                        onPosterClick = onPosterClick,
                                        onPosterLongClick = onPosterLongClick,
                                    )
                                }
                            }
                        }
                        if (homeUiState.sections.isEmpty() && !homeUiState.isLoading) {
                            item(key = "brunio_catalog_status", contentType = "empty") {
                                HomeEmptyStateCard(
                                    modifier = Modifier.padding(
                                        horizontal = if (isDesktop) homeSectionPadding else 16.dp,
                                    ),
                                    title = stringResource(Res.string.home_load_failed_title),
                                    message = homeUiState.errorMessage
                                        ?: "No se pudo cargar el catálogo de TMDB.",
                                    actionLabel = stringResource(Res.string.action_retry),
                                    onActionClick = {
                                        HomeRepository.refresh(
                                            addonsUiState.addons.enabledAddons(),
                                            force = true,
                                        )
                                    },
                                )
                            }
                        }
                    }

                    if (!BrunioCloud.enabled) keyedEnabledHomeItems.forEach { keyedSettingsItem ->""")

    playback = app / "features/streams/PlaybackAvailability.kt"
    once(playback,
        "import com.nuvio.app.core.build.AppFeaturePolicy",
        "import com.nuvio.app.features.brunio.BrunioCloud\n"
        "import com.nuvio.app.core.build.AppFeaturePolicy")
    once(playback,
        """    fun canStream(type: String, videoId: String): Boolean =
        hasCompatiblePlaybackSource(addons, plugins, type, videoId) ||""",
        """    fun canStream(type: String, videoId: String): Boolean =
        // For BruniO IDs, expose the source picker. StreamsScreen then converts
        // TMDB IDs to IMDb. This does not claim that any stream is available.
        (BrunioCloud.enabled && type in setOf("movie", "series", "tv") &&
            Regex("^tmdb:(?:movie|series):[0-9]+(?::s[0-9]+e[0-9]+)?$")
                .matches(videoId)) ||
        hasCompatiblePlaybackSource(addons, plugins, type, videoId) ||""")

    repository = app / "features/home/HomeRepository.kt"
    once(repository,
        """    fun applyCurrentSettings() {
        publishCurrentState(""",
        """    fun applyCurrentSettings() {
        if (BrunioCloud.enabled) return // Nuvio catalog settings must not erase TMDB rows
        publishCurrentState(""")
    once(repository,
        """        if (BrunioCloud.enabled) {
            activeJob?.cancel()
            _uiState.value = HomeUiState(isLoading = true)""",
        """        if (BrunioCloud.enabled) {
            // Many add-on updates may arrive while TMDB is loading. Avoid
            // cancelling its requests repeatedly during startup.
            if (!force && (activeJob?.isActive == true ||
                    _uiState.value.sections.isNotEmpty())) return
            activeJob?.cancel()
            _uiState.value = HomeUiState(isLoading = true)""")
    once(repository,
        """                val results = feeds.map { (endpoint, name, kind) ->
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
                )""",
        """                val responses = feeds.map { (endpoint, name, kind) ->
                    async {
                        val response = runCatching {
                            BrunioTmdb.catalog(endpoint, kind.takeIf(String::isNotBlank))
                        }
                        val rows = response.getOrDefault(emptyList())
                        val sections = if (kind.isBlank()) BrunioTmdb.sections(name, rows)
                            else listOf(BrunioTmdb.section(endpoint, name, kind, rows))
                        sections.filter { it.items.isNotEmpty() } to response.exceptionOrNull()
                    }
                }.awaitAll()
                val results = responses.flatMap { it.first }
                val failedFeeds = responses.count { it.second != null }
                _uiState.value = HomeUiState(
                    isLoading = false,
                    heroItems = results.flatMap { it.items }.distinctBy { it.id }.take(14),
                    sections = results,
                    errorMessage = if (results.isNotEmpty()) null else when {
                        failedFeeds > 0 -> "No se pudo cargar TMDB. Verifica la clave, " +
                            "la conexión a Internet y pulsa Reintentar."
                        else -> "TMDB no devolvió resultados. Pulsa Reintentar."
                    },
                )""")
