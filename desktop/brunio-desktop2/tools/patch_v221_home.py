"""Pass Nuvio's original 16:9 provider cards through the existing catalog navigation."""
from patch_runtime import once


def apply(root):
    app = root / "composeApp/src/commonMain/kotlin/com/nuvio/app"
    home = app / "features/home/HomeScreen.kt"
    once(home, "import com.nuvio.app.features.brunio.BrunioStreamingRow",
         "import com.nuvio.app.features.brunio.BrunioStreamingRow\n"
         "import com.nuvio.app.features.brunio.BrunioHomePreferences")
    once(home,
         """                            BrunioStreamingRow(onRefresh = {
                                HomeRepository.refresh(
                                    addonsUiState.addons.enabledAddons(), force = true,
                                )
                            })""",
         """                            BrunioStreamingRow(
                                sectionPadding = homeSectionPadding,
                                onRefresh = {
                                    HomeRepository.refresh(
                                        addonsUiState.addons.enabledAddons(), force = true,
                                    )
                                },
                                onOpenProvider = { provider ->
                                    val catalog = homeUiState.sections.firstOrNull {
                                        it.key == provider.id
                                    }
                                    if (catalog != null) {
                                        onCatalogClick?.invoke(catalog)
                                    } else {
                                        if (provider.id !in BrunioHomePreferences.selected.value) {
                                            BrunioHomePreferences.toggle(provider.id)
                                        }
                                        HomeRepository.refresh(
                                            addonsUiState.addons.enabledAddons(), force = true,
                                        )
                                    }
                                },
                            )""")
