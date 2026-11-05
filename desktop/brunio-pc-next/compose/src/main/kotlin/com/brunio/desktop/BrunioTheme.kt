package com.brunio.desktop

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun BrunioTheme(content:@Composable ()->Unit) {
    MaterialTheme(colorScheme=darkColorScheme(
        background=Color(0xFF080809),onBackground=Color.White,
        surface=Color(0xFF151518),onSurface=Color.White,
        primary=Color.White,onPrimary=Color.Black,
        secondary=Color.White,onSecondary=Color.Black,
        secondaryContainer=Color(0xFF303036),onSecondaryContainer=Color.White,
        surfaceVariant=Color(0xFF202025),onSurfaceVariant=Color(0xFFC5C5CD),outline=Color(0xFF48484F))) {
        Surface(Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.background,contentColor=MaterialTheme.colorScheme.onBackground,content=content)
    }
}
