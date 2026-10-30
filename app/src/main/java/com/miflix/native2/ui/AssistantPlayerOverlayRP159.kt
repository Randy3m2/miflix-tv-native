package com.miflix.native2.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Separate focus window above the existing player; the player and request are never replaced. */
@Composable
internal fun AssistantPlayerOverlayRP159(state:AppState,close:()->Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest=close,properties=androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth=false,decorFitsSystemWindows=false)) {
        Box(Modifier.fillMaxSize(),contentAlignment=Alignment.CenterEnd) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(.68f).widthIn(max=1000.dp).background(Color.Black).padding(10.dp)) {
                AssistantPanelRP150(state,close,guest={},initialQueryRP159=state.assistantQueryRP156.orEmpty())
            }
        }
    }
}
