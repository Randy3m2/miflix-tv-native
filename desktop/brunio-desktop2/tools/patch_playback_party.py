"""Connect BruniO RP1 Watch Party state to Nuvio Desktop's native mpv player."""
from pathlib import Path
from patch_runtime import once


def apply(root: Path) -> None:
    app = root / "composeApp/src/commonMain/kotlin/com/nuvio/app"
    player = app / "features/player/PlayerScreenRuntimeParty.kt"
    once(player, "import com.nuvio.app.features.party.OwnSeek",
         "import com.nuvio.app.features.brunio.BrunioCloud\n"
         "import com.nuvio.app.features.brunio.BrunioPartyService\n"
         "import com.nuvio.app.features.party.OwnSeek")
    once(player, "import kotlin.math.abs",
         "import kotlin.math.abs\nimport kotlin.time.Clock")
    once(player,
         '''internal val PlayerScreenRuntime.isPartySynced: Boolean
    get() = PartyRepository.state.value.party?.media?.key == partyKey''',
         '''internal val PlayerScreenRuntime.isPartySynced: Boolean
    get() {
        if (BrunioCloud.enabled) {
            val party = BrunioPartyService.state.value
            val media = party.media ?: return false
            val localId = Regex("tmdb:(?:movie|series):[0-9]+").find(parentMetaId)?.value
                ?: parentMetaId
            return party.roomCode.isNotBlank() && media.metaId == localId &&
                media.season == activeSeasonNumber &&
                media.episode == activeEpisodeNumber
        }
        return PartyRepository.state.value.party?.media?.key == partyKey
    }''')
    once(player,
         '''internal fun PlayerScreenRuntime.openPartyPanel() {
    if (PartyChat.unread.value > 0)''',
         '''internal fun PlayerScreenRuntime.openPartyPanel() {
    if (BrunioCloud.enabled) {
        val room = BrunioPartyService.state.value.roomCode
        showPartyMessage(if (room.isNotBlank()) "BruniO Party: $room · Chat en Watch Party"
            else "Abre Watch Party desde el menú de la aplicación")
        return
    }
    if (PartyChat.unread.value > 0)''')
    once(player,
         '''internal fun PlayerScreenRuntime.leaveParty() {
    showPartyPanel = false
    PartyRepository.leave()
}''',
         '''internal fun PlayerScreenRuntime.leaveParty() {
    showPartyPanel = false
    if (BrunioCloud.enabled) {
        scope.launch { BrunioPartyService.leave() }
    } else PartyRepository.leave()
}''')
    once(player,
         '''internal fun PlayerScreenRuntime.endParty() {
    showPartyPanel = false
    PartyRepository.end()
}''',
         '''internal fun PlayerScreenRuntime.endParty() {
    showPartyPanel = false
    if (BrunioCloud.enabled) {
        scope.launch { BrunioPartyService.leave() }
    } else PartyRepository.end()
}''')
    once(player,
         '''    if (!isPartySynced) return
    PartyRepository.publishPlayback(paused = !shouldPlay, positionMs = positionMs, rate = rate)''',
         '''    if (BrunioCloud.enabled) {
        if (BrunioPartyService.state.value.host &&
            BrunioPartyService.state.value.roomCode.isNotBlank()
        ) {
            scope.launch {
                BrunioPartyService.publishPlayerState(
                    media = currentPartyMedia(), paused = !shouldPlay, positionMs = positionMs,
                )
            }
        }
        return
    }
    if (!isPartySynced) return
    PartyRepository.publishPlayback(paused = !shouldPlay, positionMs = positionMs, rate = rate)''')
    once(player,
         '''internal fun PlayerScreenRuntime.startParty() {
    scope.launch {
        val error = PartyRepository.create(currentPartyMedia()) ?: return@launch''',
         '''internal fun PlayerScreenRuntime.startParty() {
    scope.launch {
        if (BrunioCloud.enabled) {
            try {
                val code = BrunioPartyService.createRoom()
                BrunioPartyService.publishPlayerState(
                    media = currentPartyMedia(),
                    paused = !shouldPlay,
                    positionMs = playbackSnapshot.positionMs,
                )
                showPartyMessage("BruniO Party: $code")
            } catch (error: Throwable) {
                showPartyMessage("No se pudo crear la Party. Revisa tu cuenta.")
            }
            return@launch
        }
        val error = PartyRepository.create(currentPartyMedia()) ?: return@launch''')
    once(player,
         '''internal fun PlayerScreenRuntime.BindPartyEffects() {
    // This binding belongs to the player session, including background PiP.''',
         '''internal fun PlayerScreenRuntime.BindPartyEffects() {
    // BruniO RP1 host pushes periodic position; guests read and correct drift.
    val brunioPartyState by BrunioPartyService.state.collectAsState()
    LaunchedEffect(brunioPartyState.roomCode, partyKey) {
        if (BrunioCloud.enabled && brunioPartyState.roomCode.isNotBlank()) {
            while (isActive) {
                val room = BrunioPartyService.state.value
                val controller = playerController
                if (controller != null) {
                    val localCloud = Regex("tmdb:(?:movie|series):[0-9]+")
                        .find(parentMetaId)?.value ?: parentMetaId
                    if (room.host) {
                        runCatching {
                            BrunioPartyService.publishPlayerState(
                                currentPartyMedia(), !shouldPlay, playbackSnapshot.positionMs,
                            )
                        }
                    } else {
                        runCatching { BrunioPartyService.refresh() }
                        val remote = BrunioPartyService.state.value
                        val pb = remote.playback
                        if (pb != null && remote.media?.metaId == localCloud &&
                            remote.media.season == activeSeasonNumber &&
                            remote.media.episode == activeEpisodeNumber
                        ) {
                            val elapsed = if (pb.paused) 0L else
                                (Clock.System.now().toEpochMilliseconds() - pb.at).coerceIn(0L, 5000L)
                            val expected = pb.positionMs + elapsed
                            if (abs(playbackSnapshot.positionMs - expected) > 1800L) {
                                controller.seekTo(expected.coerceAtLeast(0L))
                            }
                            if (shouldPlay == pb.paused) {
                                shouldPlay = !pb.paused
                                if (pb.paused) controller.pause() else controller.play()
                            }
                        }
                    }
                }
                delay(2250L)
            }
        }
    }
    // This binding belongs to the player session, including background PiP.''')

    main = app / "MainAppContent.kt"
    once(main, "import com.nuvio.app.features.brunio.BrunioPartyScreen",
         "import com.nuvio.app.features.brunio.BrunioCloud\n"
         "import com.nuvio.app.features.brunio.BrunioPartyService\n"
         "import com.nuvio.app.features.brunio.BrunioPartyScreen")
    once(main,
         '''            val resumePositionMs = PartyRepository.playback.value
                ?.takeIf { it.mediaKey == null || it.mediaKey == media.key }
                ?.let { PartySync.expectedPositionMs(it, PartyRepository.nowMs()) }''',
         '''            val resumePositionMs = if (BrunioCloud.enabled) {
                BrunioPartyService.state.value.playback?.positionMs
            } else PartyRepository.playback.value
                ?.takeIf { it.mediaKey == null || it.mediaKey == media.key }
                ?.let { PartySync.expectedPositionMs(it, PartyRepository.nowMs()) }''')
