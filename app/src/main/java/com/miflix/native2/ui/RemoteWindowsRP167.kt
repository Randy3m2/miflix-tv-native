package com.miflix.native2.ui

import android.app.Activity
import android.view.KeyEvent
import android.view.View
import android.view.Window
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider

/** Main-thread registry: native remote events follow the focused Compose dialog window. */
internal object RemoteWindowsRP167 {
 private val windows=linkedMapOf<Window,Int>()
 fun add(window:Window){windows[window]=(windows[window] ?: 0)+1}
 fun remove(window:Window){val n=(windows[window] ?: 1)-1;if(n<=0)windows.remove(window) else windows[window]=n}
 private fun active()=windows.keys.lastOrNull {it.decorView.hasWindowFocus()}
 fun focus(activity:Activity?):View?=active()?.currentFocus ?: activity?.currentFocus
 fun dispatch(activity:Activity?,event:KeyEvent):Boolean {
  val window=active()
  return if(window!=null) window.callback?.dispatchKeyEvent(event) ?: window.superDispatchKeyEvent(event) else activity?.dispatchKeyEvent(event) ?: false
 }
}
@Composable internal fun RemoteDialogInputRP167() {
 if(LocalMobileBrunioRP133.current)return
 val view=LocalView.current
 DisposableEffect(view) {
  val window=(view.parent as? DialogWindowProvider)?.window
  if(window!=null)RemoteWindowsRP167.add(window)
  onDispose {if(window!=null)RemoteWindowsRP167.remove(window)}
 }
}
