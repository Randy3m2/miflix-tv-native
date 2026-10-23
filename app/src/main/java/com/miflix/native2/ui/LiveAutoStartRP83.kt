package com.miflix.native2.ui

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull

/** Consume a fresh entry once; Back from playback must never restart the channel. */
@Composable
internal fun LiveAutoStartRP83(state:AppState,content:@Composable ()->Unit) {
    val context=LocalContext.current
    val owner=state.localExtrasScope()
    LaunchedEffect(owner) {
        if(state.liveAutoEntryRP83 && !state.kidsMode && state.partyRole!=com.miflix.native2.model.PartyRole.GUEST) {
            state.liveAutoEntryRP83=false
            try {
                val started=withTimeoutOrNull(12_000L) {LiveHubPlaybackRP41.startTV(context,state);true} ?: false
                if(started && owner==state.localExtrasScope() && state.screen==Screen.PLAYER) {
                    state.livePanelPlaybackRP83=state.playerRequest?.playbackId
                }
            } catch(e:CancellationException) {throw e} catch(_:Exception) {
                // The normal catalog stays available even if the last source or Nauta is offline.
            }
        }
    }
    content()
}
