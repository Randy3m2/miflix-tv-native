"""Wire 2–4 independent live video surfaces to the real Desktop navigator."""
from pathlib import Path
from patch_runtime import once


def apply(root: Path) -> None:
    app = root / "composeApp/src/commonMain/kotlin/com/nuvio/app"
    routes = app / "navigation/Routes.kt"
    once(routes,
         '''@Serializable
data class BrunioLiveTvRoute(override val title: String = "Live TV") : AppRoute''',
         '''@Serializable
data class BrunioLiveTvRoute(override val title: String = "Live TV") : AppRoute

@Serializable
data class BrunioMultiViewRoute(
    val channelIds: List<String>,
    override val title: String = "BruniO Multiview",
) : AppRoute''')
    main = app / "MainAppContent.kt"
    once(main, "import com.nuvio.app.features.brunio.BrunioLiveTvScreen",
         "import com.nuvio.app.features.brunio.BrunioMultiViewScreen\n"
         "import com.nuvio.app.features.brunio.BrunioLiveTvCatalog\n"
         "import com.nuvio.app.features.brunio.bundledBrunioLivePlaylist\n"
         "import com.nuvio.app.features.brunio.BrunioLiveTvScreen")
    once(main,
         '''                entry<BrunioLiveTvRoute> { route ->
                    BrunioLiveTvScreen(
                        onBack = { navController.popBackStack() },
                        onPlay''',
         '''                entry<BrunioLiveTvRoute> { route ->
                    BrunioLiveTvScreen(
                        onBack = { navController.popBackStack() },
                        onMultiView = { channels ->
                            if (channels.size in 2..4) {
                                navController.navigate(BrunioMultiViewRoute(
                                    channelIds = channels.map { it.id },
                                ))
                            }
                        },
                        onPlay''')
    once(main,
         '''                entry<BrunioLiveTvRoute> { route ->''',
         '''                entry<BrunioMultiViewRoute> { route ->
                    val allLiveChannels = remember {
                        BrunioLiveTvCatalog.parse(bundledBrunioLivePlaylist())
                    }
                    val channels = remember(route.channelIds, allLiveChannels) {
                        route.channelIds.distinct().take(4)
                            .mapNotNull { id -> allLiveChannels.firstOrNull { it.id == id } }
                    }
                    BrunioMultiViewScreen(
                        channels = channels,
                        onBack = { navController.popBackStack() },
                        onOpenFullPlayer = { channel ->
                            val launch = PlayerLaunch(
                                profileId = ProfileRepository.activeProfileId,
                                title = channel.name,
                                sourceUrl = channel.url,
                                streamType = "hls",
                                logo = channel.logo,
                                streamTitle = channel.name,
                                providerName = "BruniO Live Multiview",
                                contentType = null,
                                videoId = null,
                                parentMetaId = "live:" + channel.id,
                                parentMetaType = "live",
                            )
                            val id = PlayerLaunchStore.put(launch)
                            navController.navigate(PlayerRoute(
                                launchId = id, title = channel.name,
                            ))
                        },
                    )
                }
                entry<BrunioLiveTvRoute> { route ->''')
