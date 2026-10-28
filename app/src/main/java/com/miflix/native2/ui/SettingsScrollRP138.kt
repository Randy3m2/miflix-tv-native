@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.miflix.native2.ui

import androidx.compose.runtime.*
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Settings are a small form: keeping descendants composed avoids oversized lazy-item focus traps. */
internal class SettingsItemsRP138 {
    @Composable fun item(key:Any?=null,content:@Composable ()->Unit) {
        androidx.compose.runtime.key(key) {content()}
    }
}
@Composable
internal fun SettingsScrollColumnRP138(modifier:Modifier,scroll:ScrollState,content:@Composable SettingsItemsRP138.()->Unit) {
    Column(modifier.verticalScroll(scroll).padding(top=8.dp,bottom=48.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
        remember {SettingsItemsRP138()}.content()
    }
}
internal object SettingsBringRP138:BringIntoViewSpec {
    override fun calculateScrollDistance(offset:Float,size:Float,containerSize:Float):Float {
        val margin=minOf(12f,containerSize/8f)
        return when {
            size>containerSize-2*margin -> offset-margin
            offset<margin ->offset-margin
            offset+size>containerSize-margin ->offset+size-containerSize+margin
            else ->0f
        }
    }
}
