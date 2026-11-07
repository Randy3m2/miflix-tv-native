package com.miflix.native2.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.miflix.native2.data.ThemeStudioPresetRP173

/** Palette and button-surface samples only. Does not mutate the active theme. */
@Composable
internal fun ThemeSwatchRP185(preset:ThemeStudioPresetRP173,modifier:Modifier=Modifier) {
    val panel=Color(preset.surface);val accent=Color(preset.focus)
    val radius=com.miflix.native2.data.ThemeStudioRP173.cornerRadius(preset.id)
    val shape=RoundedCornerShape(radius.dp)
    Box(modifier.clip(RoundedCornerShape(12.dp)).background(Brush.horizontalGradient(listOf(Color(preset.background),lerp(Color(preset.background),accent,.16f)))).padding(12.dp)) {
        Row(Modifier.fillMaxSize(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f).fillMaxHeight().background(Brush.verticalGradient(listOf(lerp(panel,accent,.24f),panel)),shape).border(1.dp,accent,shape))
            Box(Modifier.weight(1f).fillMaxHeight().background(panel,shape).border(1.dp,Color(preset.primary).copy(alpha=.48f),shape))
            Box(Modifier.weight(.55f).fillMaxHeight().background(panel,shape).border(1.dp,Color(preset.primary).copy(alpha=.48f),shape))
        }
    }
}
