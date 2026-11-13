"""Add a separate desktop Live Sports add-on screen; never mine the M3U for events."""
from patch_runtime import once


def apply(root):
    app = root / "composeApp/src/commonMain/kotlin/com/nuvio/app"
    routes = app / "navigation/Routes.kt"
    once(routes,
         '@Serializable\ndata class BrunioLiveTvRoute(override val title: String = "Live TV") : AppRoute',
         '@Serializable\ndata class BrunioSportsRoute(override val title: String = "Live Sports") : AppRoute\n\n'
         '@Serializable\ndata class BrunioLiveTvRoute(override val title: String = "Live TV") : AppRoute')

    tabs = app / "MainTabsDestination.kt"
    once(tabs,
         '    onLiveTvClick: () -> Unit = {},\n    liveTvSelected: Boolean = false,',
         '    onLiveTvClick: () -> Unit = {},\n    liveTvSelected: Boolean = false,\n'
         '    onSportsClick: () -> Unit = {},\n    sportsSelected: Boolean = false,')
    once(tabs,
         '                        onLiveTvClick = onLiveTvClick,\n                        liveTvSelected = liveTvSelected,',
         '                        onLiveTvClick = onLiveTvClick,\n                        liveTvSelected = liveTvSelected,\n'
         '                        onSportsClick = onSportsClick,\n                        sportsSelected = sportsSelected,')

    shell = app / "AppShellComponents.kt"
    once(shell,
         '    onLiveTvClick: () -> Unit = {},\n    liveTvSelected: Boolean = false,',
         '    onLiveTvClick: () -> Unit = {},\n    liveTvSelected: Boolean = false,\n'
         '    onSportsClick: () -> Unit = {},\n    sportsSelected: Boolean = false,')
    once(shell,
         'DesktopSidebarItemHeight * (AppScreenTab.entries.size + 1)',
         'DesktopSidebarItemHeight * (AppScreenTab.entries.size + 2)')
    once(shell,
         '                DesktopSidebarItem(\n                    label = "Live TV",',
         '''                DesktopSidebarItem(
                    label = "Live Sports",
                    selected = sportsSelected,
                    expanded = sidebarExpanded,
                    onClick = {
                        onProfileStackVisibleChange(false)
                        onSportsClick()
                    },
                ) { color ->
                    Icon(
                        imageVector = Icons.Filled.LiveTv,
                        contentDescription = "Live Sports",
                        modifier = Modifier.size(DesktopSidebarIconSize),
                        tint = color,
                    )
                }
                DesktopSidebarItem(
                    label = "Live TV",''')

    main = app / "MainAppContent.kt"
    once(main,
         'import com.nuvio.app.features.brunio.BrunioLiveTvScreen',
         'import com.nuvio.app.features.brunio.BrunioSportsAddonScreen\n'
         'import com.nuvio.app.features.brunio.BrunioLiveTvScreen')
    once(main,
         '''                        liveTvSelected = currentRoute is BrunioLiveTvRoute,''',
         '''                        liveTvSelected = currentRoute is BrunioLiveTvRoute,
                        onSportsClick = { navController.navigate(BrunioSportsRoute()) },
                        sportsSelected = currentRoute is BrunioSportsRoute,''')
    once(main,
         '                entry<BrunioLiveTvRoute> { route ->',
         '''                entry<BrunioSportsRoute> { route ->
                    BrunioSportsAddonScreen(
                        onBack = { navController.popBackStack() },
                        onPlay = { eventTitle, url, poster ->
                            val launch = PlayerLaunch(
                                profileId = ProfileRepository.activeProfileId,
                                title = eventTitle,
                                sourceUrl = url,
                                streamType = "hls",
                                logo = poster,
                                streamTitle = eventTitle,
                                providerName = "BruniO Live Sports",
                                contentType = null,
                                videoId = null,
                                parentMetaId = "live:" + BrunioLiveSessionRegistry.register(
                                    eventTitle, url, poster, "Live Sports"),
                                parentMetaType = "live",
                            )
                            val launchId = PlayerLaunchStore.put(launch)
                            navController.navigate(PlayerRoute(
                                launchId = launchId, title = eventTitle,
                            ))
                        },
                    )
                }
                entry<BrunioLiveTvRoute> { route ->''')
