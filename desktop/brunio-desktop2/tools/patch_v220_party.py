"""Control BruniO RP1 Watch Party in the actual mpv overlay, not Nuvio's RPC panel."""
from pathlib import Path
from patch_runtime import once


def apply(root: Path) -> None:
    app = root / "composeApp/src/commonMain/kotlin/com/nuvio/app"
    party = app / "features/player/PlayerScreenRuntimeParty.kt"
    once(party,
         '''            onOpenSources = { runtime.openSourcesPanel() },
            onDismiss = { runtime.showPartyPanel = false },''',
         '''            onOpenSources = { runtime.openSourcesPanel() },
            onTogglePlayback = {
                val next = !runtime.shouldPlay
                runtime.shouldPlay = next
                if (next) runtime.playerController?.play()
                else runtime.playerController?.pause()
                runtime.scope.launch {
                    runCatching {
                        BrunioPartyService.publishPlayerState(
                            media = runtime.currentPartyMedia(),
                            paused = !next,
                            positionMs = runtime.playbackSnapshot.positionMs,
                        )
                    }
                }
            },
            onSeekBy = { deltaMs ->
                val next = (runtime.playbackSnapshot.positionMs + deltaMs).coerceAtLeast(0L)
                runtime.playerController?.seekTo(next)
                runtime.scope.launch {
                    runCatching {
                        BrunioPartyService.publishPlayerState(
                            media = runtime.currentPartyMedia(),
                            paused = !runtime.shouldPlay,
                            positionMs = next,
                        )
                    }
                }
            },
            onDismiss = { runtime.showPartyPanel = false },''')
