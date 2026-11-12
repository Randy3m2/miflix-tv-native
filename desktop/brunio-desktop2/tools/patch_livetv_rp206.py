"""One Live TV entry in the existing Nuvio Desktop sidebar and native mpv route."""
from patch_runtime import once


def apply(root):
    app = root / "composeApp/src/commonMain/kotlin/com/nuvio/app"
    routes = app / "navigation/Routes.kt"
    once(routes,
         '''@Serializable
data class StreamRoute(''',
         '''@Serializable
data class BrunioLiveTvRoute(override val title: String = "Live TV") : AppRoute

@Serializable
data class StreamRoute(''')

    tabs = app / "MainTabsDestination.kt"
    once(tabs,
         '''    onAddProfileRequested: () -> Unit,
) {''',
         '''    onAddProfileRequested: () -> Unit,
    onLiveTvClick: () -> Unit = {},
    liveTvSelected: Boolean = false,
) {''')
    once(tabs,
         '''                    DesktopHoverSidebar(
                        selectedTab = selectedTab,''',
         '''                    DesktopHoverSidebar(
                        selectedTab = selectedTab,
                        onLiveTvClick = onLiveTvClick,
                        liveTvSelected = liveTvSelected,''')

    shell = app / "AppShellComponents.kt"
    once(shell,
         "import androidx.compose.material.icons.filled.Home",
         "import androidx.compose.material.icons.filled.Home\n"
         "import androidx.compose.material.icons.filled.LiveTv")
    once(shell,
         '''internal fun DesktopHoverSidebar(
    selectedTab: AppScreenTab,
    onTabSelected: (AppScreenTab) -> Unit,''',
         '''internal fun DesktopHoverSidebar(
    selectedTab: AppScreenTab,
    onLiveTvClick: () -> Unit = {},
    liveTvSelected: Boolean = false,
    onTabSelected: (AppScreenTab) -> Unit,''')
    once(shell,
         '''            val navColumnHeight = DesktopSidebarItemHeight * AppScreenTab.entries.size''',
         '''            val navColumnHeight = DesktopSidebarItemHeight * (AppScreenTab.entries.size + 1)''')
    once(shell,
         '''                DesktopSidebarItem(
                    label = stringResource(Res.string.compose_settings_page_root),
                    selected = selectedTab == AppScreenTab.Settings,''',
         '''                DesktopSidebarItem(
                    label = "Live TV",
                    selected = liveTvSelected,
                    expanded = sidebarExpanded,
                    onClick = {
                        onProfileStackVisibleChange(false)
                        onLiveTvClick()
                    },
                ) { color ->
                    Icon(
                        imageVector = Icons.Filled.LiveTv,
                        contentDescription = "Live TV",
                        modifier = Modifier.size(DesktopSidebarIconSize),
                        tint = color,
                    )
                }
                DesktopSidebarItem(
                    label = stringResource(Res.string.compose_settings_page_root),
                    selected = selectedTab == AppScreenTab.Settings,''')

    main = app / "MainAppContent.kt"
    once(main,
         "import com.nuvio.app.features.brunio.BrunioPartyScreen",
         "import com.nuvio.app.features.brunio.BrunioLiveTvScreen\n"
         "import com.nuvio.app.features.brunio.BrunioLiveChannel\n"
         "import com.nuvio.app.features.brunio.BrunioPartyScreen")
    once(main,
         '''                        onAddProfileRequested = onSwitchProfile,
                    )''',
         '''                        onAddProfileRequested = onSwitchProfile,
                        onLiveTvClick = {
                            navController.navigate(BrunioLiveTvRoute())
                        },
                        liveTvSelected = currentRoute is BrunioLiveTvRoute,
                    )''')
    once(main,
         '''                entry<DetailRoute> { route ->''',
         '''                entry<BrunioLiveTvRoute> { route ->
                    BrunioLiveTvScreen(
                        onBack = { navController.popBackStack() },
                        onPlay = { channel ->
                            val launch = PlayerLaunch(
                                profileId = ProfileRepository.activeProfileId,
                                title = channel.name,
                                sourceUrl = channel.url,
                                streamType = "hls",
                                logo = channel.logo,
                                streamTitle = channel.name,
                                providerName = "BruniO Live TV",
                                contentType = null, // no VOD resume for a live stream
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
                entry<DetailRoute> { route ->''')
