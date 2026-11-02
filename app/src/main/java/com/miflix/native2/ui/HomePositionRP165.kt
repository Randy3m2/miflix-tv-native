package com.miflix.native2.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import com.miflix.native2.model.MediaSummary

/** Owned by AppState, so leaving a destination cannot discard the viewport. */
internal class HomePositionRP165 {
    val vertical=LazyListState()
    val rails=mutableMapOf<String,LazyListState>()
    var focusKey:String?=null
    var rowKey:String?=null
    var sectionKeys:List<String> = emptyList()
    var discoveryRows:List<MediaSummary> = emptyList()
    var discoveryMood=""
    fun rail(key:String)=rails.getOrPut(key) {LazyListState()}
}
internal val LocalHomePositionRP165=staticCompositionLocalOf<HomePositionRP165?> {null}
@Composable internal fun homeRailStateRP165(key:String):LazyListState {
    val position=LocalHomePositionRP165.current
    val fallback=androidx.compose.foundation.lazy.rememberLazyListState()
    return position?.rail(key) ?: fallback
}
