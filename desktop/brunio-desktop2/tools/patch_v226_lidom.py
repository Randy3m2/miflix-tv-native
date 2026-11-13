"""Expose native LIDOM team/live hub in Nuvio Desktop sidebar and navigator."""
from patch_runtime import once

def apply(root):
    app = root / "composeApp/src/commonMain/kotlin/com/nuvio/app"
    routes = app / "navigation/Routes.kt"
    once(routes,
         '@Serializable\ndata class BrunioSportsRoute(override val title: String = "Live Sports") : AppRoute',
         '@Serializable\ndata class BrunioLidomRoute(override val title: String = "LIDOM") : AppRoute\n\n'
         '@Serializable\ndata class BrunioSportsRoute(override val title: String = "Live Sports") : AppRoute')
    tabs = app / "MainTabsDestination.kt"
    once(tabs,
         '    onSportsClick: () -> Unit = {},\n    sportsSelected: Boolean = false,',
         '    onSportsClick: () -> Unit = {},\n    sportsSelected: Boolean = false,\n'
         '    onLidomClick: () -> Unit = {},\n    lidomSelected: Boolean = false,')
    once(tabs,
         '                        onSportsClick = onSportsClick,\n                        sportsSelected = sportsSelected,',
         '                        onSportsClick = onSportsClick,\n                        sportsSelected = sportsSelected,\n'
         '                        onLidomClick = onLidomClick,\n                        lidomSelected = lidomSelected,')
    shell = app / "AppShellComponents.kt"
    once(shell,
         '    onSportsClick: () -> Unit = {},\n    sportsSelected: Boolean = false,',
         '    onSportsClick: () -> Unit = {},\n    sportsSelected: Boolean = false,\n'
         '    onLidomClick: () -> Unit = {},\n    lidomSelected: Boolean = false,')
    once(shell,
         'DesktopSidebarItemHeight * (AppScreenTab.entries.size + 2)',
         'DesktopSidebarItemHeight * (AppScreenTab.entries.size + 3)')
    once(shell,
         '                DesktopSidebarItem(\n                    label = "Live Sports",',
         '''                DesktopSidebarItem(
                    label = "LIDOM",
                    selected = lidomSelected,
                    expanded = sidebarExpanded,
                    onClick = {
                        onProfileStackVisibleChange(false)
                        onLidomClick()
                    },
                ) { color ->
                    Icon(
                        imageVector = Icons.Filled.LiveTv,
                        contentDescription = "LIDOM",
                        modifier = Modifier.size(DesktopSidebarIconSize),
                        tint = color,
                    )
                }
                DesktopSidebarItem(
                    label = "Live Sports",''')
    main = app / "MainAppContent.kt"
    once(main,
         'import com.nuvio.app.features.brunio.BrunioSportsAddonScreen',
         'import com.nuvio.app.features.brunio.BrunioLidomScreen\n'
         'import com.nuvio.app.features.brunio.BrunioSportsAddonScreen')
    once(main,
         '''                        sportsSelected = currentRoute is BrunioSportsRoute,''',
         '''                        sportsSelected = currentRoute is BrunioSportsRoute,
                        onLidomClick = { navController.navigate(BrunioLidomRoute()) },
                        lidomSelected = currentRoute is BrunioLidomRoute,''')
    once(main,
         '                entry<BrunioSportsRoute> { route ->',
         '''                entry<BrunioLidomRoute> { route ->
                    BrunioLidomScreen(
                        onBack = { navController.popBackStack() },
                        onPlay = { title, url, poster ->
                            val launch = PlayerLaunch(
                                profileId = ProfileRepository.activeProfileId,
                                title = title,
                                sourceUrl = url,
                                streamType = "hls",
                                logo = poster,
                                streamTitle = title,
                                providerName = "YouTube · LIDOM",
                                contentType = null,
                                videoId = null,
                                parentMetaId = "live:" + BrunioLiveSessionRegistry.register(
                                    title, url, poster, "LIDOM"),
                                parentMetaType = "live",
                            )
                            val id = PlayerLaunchStore.put(launch)
                            navController.navigate(PlayerRoute(
                                launchId = id, title = title,
                            ))
                        },
                    )
                }
                entry<BrunioSportsRoute> { route ->''')
