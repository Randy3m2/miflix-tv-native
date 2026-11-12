"""BruniO 2.2.1: actual Windows player RP1 panel; suppress old Nuvio modal.

The upstream UI only calls RenderParty on non-Desktop platforms. RP206
modified RenderParty but Desktop never invoked it, leaving the Nuvio HTML
watch party modal displayed in the mpv controls. Expose a visible native
BruniO Party entry in the player and route it to the RP1 panel.
"""
from patch_runtime import once


def apply(root):
    app = root / "composeApp/src/commonMain/kotlin/com/nuvio/app"

    ui = app / "features/player/PlayerScreenRuntimeUi.kt"
    once(ui,
         """    Box(modifier = panelModifier) {
        if (!isDesktop) {
            RenderParty(""",
         """    Box(modifier = panelModifier) {
        if (!isDesktop || BrunioCloud.enabled) {
            RenderParty(""")
    once(ui,
         """        RenderPlayerModals(displayedPositionMs = displayedPositionMs)
    }
}""",
         """        RenderPlayerModals(displayedPositionMs = displayedPositionMs)
        if (BrunioCloud.enabled && isDesktop && !isInPip && !showPartyPanel) {
            androidx.compose.material3.Button(
                onClick = { openPartyPanel() },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 26.dp, top = 76.dp),
            ) {
                androidx.compose.material3.Text("Party · BruniO")
            }
        }
    }
}""")

    once(ui,
         """            onPartyClick = if (BrunioCloud.enabled ||
                PartyRepository.available.collectAsStateWithLifecycle().value) {""",
         """            onPartyClick = if (BrunioCloud.enabled ||
                PartyRepository.available.collectAsStateWithLifecycle().value) {""")

    # Restore the built-in player control for Party. BruniO routes its
    # click to the native RP1 panel through RenderParty (not Nuvio RPCs).
    # Desktop currently hides Party altogether because available was false.
    controls = app / "features/player/PlayerScreenRuntimePartyControls.kt"
    once(controls,
         """        available = if (BrunioCloud.enabled) true else available,""",
         """        available = if (BrunioCloud.enabled) true else available,""")
    once(controls,
         """        canStart = if (BrunioCloud.enabled) brunioAuthed else canStart,""",
         """        canStart = if (BrunioCloud.enabled) brunioAuthed else canStart,""")

    # The RP1 panel content must inherit readable foreground, not Nuvio's
    # possibly dark-on-dark LocalContentColor.
    panel = root / "composeApp/src/commonMain/kotlin/com/nuvio/app/features/brunio/BrunioPlayerPartyPanel.kt"
    once(panel,
         """    ) {
        Column(modifier = Modifier.fillMaxSize().padding(22.dp)) {
            PlayerPanelHeader(title = "Watch Party · BruniO")""",
         """    ) {
        BrunioReadableSurface {
        Column(modifier = Modifier.fillMaxSize().padding(22.dp)) {
            PlayerPanelHeader(title = "Watch Party · BruniO")""")
    source = panel.read_text(encoding="utf-8")
    tail = """            }
        }
    }
}
"""
    if not source.endswith(tail):
        raise RuntimeError("RP1 player panel closing braces moved upstream")
    panel.write_text(source[:-len(tail)] + """            }
        }
        }
    }
}
""", encoding="utf-8")
