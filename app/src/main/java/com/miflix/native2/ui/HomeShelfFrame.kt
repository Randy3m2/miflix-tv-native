package com.miflix.native2.ui

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp

internal val LocalHomeShelfPaging=staticCompositionLocalOf {false}
internal val LocalHomeShelfKey=staticCompositionLocalOf {""}
internal val LocalHomeShelfMore=staticCompositionLocalOf<((String,String,List<com.miflix.native2.model.MediaSummary>)->Unit)?> {null}

internal val LocalHomeCardRowHeight=staticCompositionLocalOf {188.dp}

/** One catalog group per remaining viewport. Header and artwork move as a unit in either direction. */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
internal fun HomeShelfFrame(@Suppress("UNUSED_PARAMETER") index:Int,@Suppress("UNUSED_PARAMETER") scroll:LazyListState,modifier:Modifier=Modifier,content:@Composable ()->Unit) {
    // Focus moves immediately through Compose's lazy focus search. There is no separate
    // scroll job and no focus request delayed until an animation completes.
    BoxWithConstraints(modifier.padding(vertical=8.dp).focusGroup()) {
        val cardHeight=(maxHeight-64.dp).coerceIn(124.dp,188.dp)
        CompositionLocalProvider(LocalHomeShelfPaging provides true,LocalHomeCardRowHeight provides cardHeight) {content()}
    }
}

/** Restore horizontal focus scrolling only for Home rails inheriting its vertical no-scroll policy. */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
internal fun HomeHorizontalRail(content:@Composable ()->Unit) {
    if(!LocalHomeShelfPaging.current) {
        content()
        return
    }
    val horizontalAnchor=remember {com.miflix.native2.data.HorizontalFocusAnchor()}
    val horizontalSpec=remember {object: BringIntoViewSpec {
        override fun calculateScrollDistance(offset:Float,size:Float,containerSize:Float):Float =
            horizontalAnchor.distance(offset,size,containerSize,android.os.SystemClock.uptimeMillis())
    }}
    CompositionLocalProvider(LocalBringIntoViewSpec provides horizontalSpec,LocalHorizontalFocusAnchor provides horizontalAnchor) {content()}
}

/** Save IDs, not FocusRequester instances, across the details destination. */
internal class HomeReturnFocus(initialKey:String?,val record:(String)->Unit) {
    var pending by mutableStateOf(initialKey)
}
internal val LocalHomeReturnFocus=staticCompositionLocalOf<HomeReturnFocus?> {null}

@Composable
internal fun homeReturnFocus(key:String):Modifier {
    val returning=LocalHomeReturnFocus.current ?: return Modifier
    val requester=remember {FocusRequester()}
    LaunchedEffect(returning.pending,key) {
        if(returning.pending==key) {
            withFrameNanos { }
            withFrameNanos { }
            if(runCatching {requester.requestFocus()}.getOrDefault(false)) returning.pending=null
        }
    }
    return Modifier.focusRequester(requester).onFocusChanged {
        if(it.isFocused && (returning.pending==null || returning.pending==key)) returning.record(key)
    }
}

@Composable
internal fun BrowseReturnScope(owner:String,content:@Composable ()->Unit) {
    var savedKey by androidx.compose.runtime.saveable.rememberSaveable(owner) {mutableStateOf<String?>(null)}
    val returning=remember(owner) {HomeReturnFocus(savedKey) {savedKey=it}}
    LaunchedEffect(returning) {kotlinx.coroutines.delay(1500);returning.pending=null}
    CompositionLocalProvider(LocalHomeReturnFocus provides returning) {content()}
}
