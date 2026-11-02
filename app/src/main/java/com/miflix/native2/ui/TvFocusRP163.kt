@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
package com.miflix.native2.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.WindowInfo
import com.miflix.native2.data.TvFocusRestoreRP163
import kotlinx.coroutines.launch

internal class TvFocusMemoryRP163(val window:WindowInfo,initial:String?,val save:(String)->Unit) {
    val policy=TvFocusRestoreRP163(initial)
    val targets=mutableMapOf<String,()->Boolean>()
    fun focused(key:String) {policy.focused(key);save(key)}
}
internal val LocalTvFocusMemoryRP163=staticCompositionLocalOf<TvFocusMemoryRP163?> {null}

/** Lives inside the existing navigation SaveableStateProvider, isolated by account/profile/screen. */
@Composable
internal fun TvFocusScopeRP163(enabled:Boolean,content:@Composable ()->Unit) {
    if(!enabled) {content();return}
    var saved by rememberSaveable {mutableStateOf<String?>(null)}
    val window=LocalWindowInfo.current
    val memory=remember(window) {TvFocusMemoryRP163(window,saved) {saved=it}}
    val windowFocused=window.isWindowFocused
    LaunchedEffect(windowFocused) {
        if(!windowFocused) {memory.policy.windowLost();return@LaunchedEffect}
        val desired=memory.policy.pending ?: return@LaunchedEffect
        val intent=memory.policy.intent
        // Let the destination's initial/default focus settle, then restore the remembered control.
        repeat(12) {attempt ->
            withFrameNanos {}
            if(memory.policy.intent!=intent) return@LaunchedEffect
            if(attempt>=1 && memory.targets[desired]?.let {runCatching(it).getOrDefault(false)}==true) {
                memory.policy.restored();return@LaunchedEffect
            }
        }
        memory.policy.restored()
    }
    CompositionLocalProvider(LocalTvFocusMemoryRP163 provides memory) {
        Box(Modifier.fillMaxSize().onPreviewKeyEvent {event ->
            if(event.type==KeyEventType.KeyDown) memory.policy.interacted()
            false
        }) {content()}
    }
}

/** Pointer clicks select the SAME target as DPAD, rather than leaving its outline behind. */
internal fun Modifier.tvFocusAnchorRP163(stableKey:String?=null):Modifier=composed {
    if(LocalMobileBrunioRP133.current) return@composed this
    RemoteDialogInputRP167()
    val requester=remember {FocusRequester()}
    val scope=rememberCoroutineScope()
    val input=LocalInputModeManager.current
    val window=LocalWindowInfo.current
    val inherited=LocalTvFocusMemoryRP163.current
    val memory=inherited?.takeIf {it.window===window}
    val generated=rememberSaveable {java.util.UUID.randomUUID().toString()}
    val key=stableKey ?: generated
    val home=LocalHomeReturnFocus.current
    val restore=rememberUpdatedState<()->Boolean>({home?.pending=null;requester.requestFocus()})
    DisposableEffect(memory,key) {
        val target={restore.value()}
        memory?.targets?.set(key,target)
        onDispose {memory?.let {if(it.targets[key]===target) it.targets.remove(key)}}
    }
    this.focusRequester(requester).onFocusChanged {if(it.isFocused) memory?.focused(key)}
        .pointerInput(requester,input,memory) {
            awaitPointerEventScope {while(true) {
                val event=awaitPointerEvent(PointerEventPass.Initial)
                if(event.changes.any {it.pressed && !it.previousPressed}) {
                    memory?.policy?.interacted()
                    home?.pending=null
                    input.requestInputMode(InputMode.Keyboard)
                    if(!runCatching {requester.requestFocus()}.getOrDefault(false)) {
                        val intent=memory?.policy?.intent
                        scope.launch {
                            withFrameNanos {}
                            if(window.isWindowFocused && memory?.policy?.intent==intent) runCatching {requester.requestFocus()}
                        }
                    }
                }
            }}
        }
}
