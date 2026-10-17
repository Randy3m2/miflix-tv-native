package com.miflix.native2.ui

import androidx.compose.animation.core.tween
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp

internal val LocalHomeShelfPaging=staticCompositionLocalOf {false}
internal val LocalHomeCardRowHeight=staticCompositionLocalOf {188.dp}

/** One catalog group per remaining viewport. Header and artwork move as a unit in either direction. */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
internal fun HomeShelfFrame(index:Int,scroll:LazyListState,modifier:Modifier=Modifier,content:@Composable ()->Unit) {
    var active by remember {mutableStateOf(false)}
    LaunchedEffect(active,index) {
        if(active) {
            withFrameNanos { }
            val row=scroll.layoutInfo.visibleItemsInfo.firstOrNull {it.index==index}
            if(row!=null) scroll.animateScrollBy(row.offset.toFloat(),tween(140))
            else scroll.animateScrollToItem(index)
        }
    }
    BoxWithConstraints(modifier.padding(vertical=8.dp).onFocusChanged {active=it.hasFocus}.focusGroup()) {
        val cardHeight=(maxHeight-64.dp).coerceIn(124.dp,188.dp)
        CompositionLocalProvider(LocalHomeShelfPaging provides true,LocalHomeCardRowHeight provides cardHeight) {content()}
    }
}
