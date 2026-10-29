@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.miflix.native2.ui

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp

internal data class SettingsCategory(val id: String,val english: String,val spanish: String)
internal val SettingsCategories = listOf(
    SettingsCategory("account","Account","Cuenta"),
    SettingsCategory("playback","Playback","Reproducción"),
    SettingsCategory("language","Language","Idioma"),
    SettingsCategory("appearance","Appearance","Apariencia"),
    SettingsCategory("sources","Sources","Fuentes"),
    SettingsCategory("ai","BruniO AI","BruniO AI"),
    SettingsCategory("system","System","Sistema")
)

/** Outlines do not alter the card's measured size or scroll position. */
@Composable
internal fun SettingsSectionCard(onFocused: () -> Unit = {},content: @Composable ColumnScope.() -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().settingsCardHeaderRP129().onFocusChanged {
        focused=it.hasFocus
        if(it.hasFocus) onFocused()
    }.background(Panel,RoundedCornerShape(16.dp))
        .border(1.dp,if(focused) Accent else PanelBorder,RoundedCornerShape(16.dp))
        .padding(20.dp).focusGroup(),verticalArrangement=Arrangement.spacedBy(12.dp),content=content)
}
