"""Make PiP survive navigation from a BruniO playback screen.

Nuvio already has native external PiP, but that surface is disposed when the
PlayerRoute is popped. For network video on BruniO Desktop, open one separate
always-on-top Compose window with its own mpv clock, then navigate back to the
regular app. Leave native PiP unchanged for P2P/local content.
"""
from patch_runtime import once


def apply(root):
    app = root / "composeApp/src/commonMain/kotlin/com/nuvio/app"
    ui = app / "features/player/PlayerScreenRuntimeUi.kt"
    once(ui, "import com.nuvio.app.features.brunio.BrunioCloud",
         "import com.nuvio.app.features.brunio.BrunioCloud\n"
         "import com.nuvio.app.features.brunio.BrunioFloatingPiP")
    once(ui,
         """        PlayerControlsAction.PictureInPicture -> {
            togglePlayerPictureInPicture()
        }""",
         """        PlayerControlsAction.PictureInPicture -> {
            val floating = BrunioCloud.enabled && isDesktop &&
                BrunioFloatingPiP.open(
                    title = title,
                    sourceUrl = activeSourceUrl.orEmpty(),
                    streamType = streamType,
                    headers = activeSourceHeaders,
                    responseHeaders = activeSourceResponseHeaders,
                    positionMs = playbackSnapshot.positionMs,
                    playing = shouldPlay,
                )
            if (floating) requestBack() else togglePlayerPictureInPicture()
        }""")

    desktop = root / "composeApp/src/desktopMain/kotlin/com/nuvio/app/Main.kt"
    once(desktop,
         "import androidx.compose.ui.window.application",
         "import androidx.compose.ui.window.Window\n"
         "import androidx.compose.foundation.layout.Column\n"
         "import androidx.compose.foundation.layout.Row\n"
         "import androidx.compose.foundation.layout.Arrangement\n"
         "import androidx.compose.foundation.layout.padding\n"
         "import androidx.compose.foundation.layout.fillMaxWidth\n"
         "import androidx.compose.material3.Button\n"
         "import androidx.compose.material3.Text\n"
         "import androidx.compose.ui.Alignment\n"
         "import com.nuvio.app.features.player.PlayerEngineController\n"
         "import com.nuvio.app.features.brunio.BrunioFloatingPiP\n"
         "import androidx.compose.runtime.mutableStateOf\n"
         "import androidx.compose.runtime.setValue\n"
         "import androidx.compose.ui.window.application")
    once(desktop,
         """        val appIconState by AppIconRepository.state.collectAsState()
        val smokePlayerUrl""",
         """        val appIconState by AppIconRepository.state.collectAsState()
        val brunioFloatingVideo by BrunioFloatingPiP.video.collectAsState()
        val smokePlayerUrl""")
    # Compose child window is an independent lifetime and stays above other apps
    # until the user closes it, even as the main BruniO navigator changes route.
    once(desktop,
         """        SwingWindow(
            onCloseRequest = closeApplication,""",
         """        if (brunioFloatingVideo != null) {
            val floating = brunioFloatingVideo!!
            val floatingState = rememberWindowState(
                width = 460.dp, height = 275.dp,
            )
            Window(
                onCloseRequest = { BrunioFloatingPiP.close() },
                title = "BruniO Mini Player · " + floating.title,
                state = floatingState,
                alwaysOnTop = true,
                resizable = true,
            ) {
                var miniPlayer by remember(floating.generation) {
                    mutableStateOf<PlayerEngineController?>(null)
                }
                var miniPlaying by remember(floating.generation) {
                    mutableStateOf(floating.playing)
                }
                NuvioTheme {
                    Box(modifier = Modifier.fillMaxSize()) {
                        PlatformPlayerSurface(
                            sourceUrl = floating.sourceUrl,
                            streamType = floating.streamType,
                            sourceHeaders = floating.sourceHeaders,
                            sourceResponseHeaders = floating.sourceResponseHeaders,
                            modifier = Modifier.fillMaxSize(),
                            playWhenReady = floating.playing,
                            initialPositionMs = floating.positionMs,
                            onControllerReady = { miniPlayer = it },
                            onSnapshot = {},
                            onError = {},
                        )
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Button(onClick = {
                                if (miniPlaying) miniPlayer?.pause() else miniPlayer?.play()
                                miniPlaying = !miniPlaying
                            }) {
                                Text(if (miniPlaying) "Pausar" else "Reproducir")
                            }
                            Button(onClick = {
                                BrunioFloatingPiP.close()
                            }) {
                                Text("Cerrar PiP")
                            }
                        }
                    }
                }
            }
        }

        SwingWindow(
            onCloseRequest = closeApplication,""")
