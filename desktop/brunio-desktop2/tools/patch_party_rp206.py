"""Replace the inherited Nuvio sign-in/party panel in mpv with BruniO RP1."""
from pathlib import Path
from patch_runtime import once


def apply(root: Path) -> None:
    base = root / "composeApp/src/commonMain/kotlin/com/nuvio/app"
    party = base / "features/player/PlayerScreenRuntimeParty.kt"
    once(party, "import com.nuvio.app.features.party.PartyPanel",
         "import com.nuvio.app.features.brunio.BrunioPlayerPartyPanel\n"
         "import com.nuvio.app.features.party.PartyPanel")
    once(party, """    val partyState by PartyRepository.state.collectAsStateWithLifecycle()
    val playback by PartyRepository.playback.collectAsStateWithLifecycle()
    val unread by PartyChat.unread.collectAsStateWithLifecycle()""",
         """    if (BrunioCloud.enabled) {
        BrunioPlayerPartyPanel(
            visible = panelVisible,
            width = panelWidth,
            modal = modal,
            media = runtime.currentPartyMedia(),
            positionMs = runtime.playbackSnapshot.positionMs,
            playing = runtime.shouldPlay,
            onOpenSources = { runtime.openSourcesPanel() },
            onDismiss = { runtime.showPartyPanel = false },
        )
        return
    }
    val partyState by PartyRepository.state.collectAsStateWithLifecycle()
    val playback by PartyRepository.playback.collectAsStateWithLifecycle()
    val unread by PartyChat.unread.collectAsStateWithLifecycle()""")
    ui = base / "features/player/PlayerScreenRuntimeUi.kt"
    once(ui, "import com.nuvio.app.features.party.PartyRepository",
         "import com.nuvio.app.features.brunio.BrunioCloud\n"
         "import com.nuvio.app.features.party.PartyRepository")
    once(ui,
         """            onPartyClick = if (PartyRepository.available.collectAsStateWithLifecycle().value) {""",
         """            onPartyClick = if (BrunioCloud.enabled ||
                PartyRepository.available.collectAsStateWithLifecycle().value) {""")
    # Player-control badges otherwise still use Nuvio's authentication gate.
    controls = base / "features/player/PlayerScreenRuntimePartyControls.kt"
    once(controls, "import com.nuvio.app.core.network.ServerConfigurationRepository",
         "import com.nuvio.app.features.brunio.BrunioCloud\n"
         "import com.nuvio.app.features.brunio.BrunioPartyService\n"
         "import com.nuvio.app.core.auth.AuthRepository\n"
         "import com.nuvio.app.core.auth.AuthState\n"
         "import com.nuvio.app.core.network.ServerConfigurationRepository")
    once(controls,
         """    val base = PlayerControlPartyState(
        available = available,""",
         """    val brunioAuthed = (AuthRepository.state.value as? AuthState.Authenticated)
        ?.isAnonymous == false
    val base = PlayerControlPartyState(
        available = if (BrunioCloud.enabled) true else available,""")
    once(controls,
         """        canStart = canStart,
        startNote = when {
            !available -> stringResource(Res.string.party_unavailable)
            !canStart -> stringResource(Res.string.party_sign_in_required)
            else -> ""
        },""",
         """        canStart = if (BrunioCloud.enabled) brunioAuthed else canStart,
        startNote = if (BrunioCloud.enabled) {
            if (brunioAuthed) "" else "Inicia sesión en BruniO"
        } else when {
            !available -> stringResource(Res.string.party_unavailable)
            !canStart -> stringResource(Res.string.party_sign_in_required)
            else -> ""
        },""")
