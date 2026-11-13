"""Show a native BruniO Multiview picker at the RIGHT of the active player.

All routing is done through the main Compose navigator, not through the
Stremio sources HTML, so existing playback and RPC streams are unaffected.
"""
from patch_runtime import once


def apply(root):
    app = root / "composeApp/src/commonMain/kotlin/com/nuvio/app"

    ui = app / "features/player/PlayerScreenRuntimeUi.kt"
    once(ui, "import androidx.compose.runtime.getValue",
         "import androidx.compose.runtime.getValue\n"
         "import androidx.compose.runtime.setValue")
    once(ui, "import com.nuvio.app.features.brunio.BrunioCloud",
         "import com.nuvio.app.features.brunio.BrunioCloud\n"
         "import com.nuvio.app.features.brunio.BrunioMultiViewPickerPanel")
    once(ui,
         """    val runtime = this
    val systemBackRegistration""",
         """    val runtime = this
    var showBrunioMultiViewPicker by remember { mutableStateOf(false) }
    val systemBackRegistration""")

    # The native panel is drawn after the in-player WebView controls so it
    # receives pointer events, just like the native series episode selector.
    once(ui,
         """        RenderPlayerModals(displayedPositionMs = displayedPositionMs)
        if (BrunioCloud.enabled && isDesktop && !isInPip && !showPartyPanel) {""",
         """        RenderPlayerModals(displayedPositionMs = displayedPositionMs)
        if (BrunioCloud.enabled && isDesktop && !isInPip &&
            parentMetaId.startsWith("live:") && !showBrunioMultiViewPicker
        ) {
            androidx.compose.material3.Button(
                onClick = { showBrunioMultiViewPicker = true },
                modifier = Modifier.align(Alignment.TopEnd)
                    .padding(end = 26.dp, top = 125.dp),
            ) {
                androidx.compose.material3.Text("+ Multiview")
            }
        }
        if (BrunioCloud.enabled && isDesktop && parentMetaId.startsWith("live:")) {
            BrunioMultiViewPickerPanel(
                visible = showBrunioMultiViewPicker && !isInPip,
                modal = true,
                width = 520.dp,
                currentLiveChannelId = parentMetaId.removePrefix("live:"),
                onDismiss = { showBrunioMultiViewPicker = false },
            )
        }
        if (BrunioCloud.enabled && isDesktop && !isInPip && !showPartyPanel) {""")

    once(ui, "import androidx.compose.ui.Modifier",
         "import androidx.compose.ui.Alignment\nimport androidx.compose.ui.Modifier")

    main = app / "MainAppContent.kt"
    once(main,
         "import com.nuvio.app.features.brunio.BrunioMultiViewScreen",
         "import com.nuvio.app.features.brunio.BrunioLiveSessionRegistry\n"
         "import com.nuvio.app.features.brunio.BrunioMultiViewRequests\n"
         "import com.nuvio.app.features.brunio.BrunioMultiViewScreen")
    once(main,
         """        val appUpdaterController = rememberAppUpdaterController()""",
         """        LaunchedEffect(navController) {
            BrunioMultiViewRequests.requests.collectLatest { channelIds ->
                navController.navigate(BrunioMultiViewRoute(
                    channelIds = channelIds,
                ))
            }
        }
        val appUpdaterController = rememberAppUpdaterController()""")

    once(main,
         '''                        route.channelIds.distinct().take(4)
                            .mapNotNull { id -> allLiveChannels.firstOrNull { it.id == id } }''',
         '''                        route.channelIds.distinct().take(4)
                            .mapNotNull { id ->
                                BrunioLiveSessionRegistry.find(id)
                                    ?: allLiveChannels.firstOrNull { it.id == id }
                            }''')
