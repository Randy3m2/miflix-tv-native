package com.miflix.native2.ui

import com.miflix.native2.data.ThemeStudioRP173
import org.junit.Assert.*
import org.junit.Test

class ThemeOriginalScreenRP189Test {
    @Test fun everyPresetUsesOriginalValuesOnExcludedPagesAndRestoresOnExit() {
        val previous=FocusTheme.argb
        val previousRoute=ThemeRuntimeRP177.originalPageRP189
        try {
            ThemeRuntimeRP177.originalPageRP189={false}
            ThemeRuntimeRP177.restore(0xFFB792FF.toInt())
            fun style()=listOf(ThemeRuntimeRP177.id,ThemeRuntimeRP177.background,ThemeRuntimeRP177.panel,
                ThemeRuntimeRP177.muted,ThemeRuntimeRP177.focusedPanel,ThemeRuntimeRP177.border,
                ThemeRuntimeRP177.radius,ThemeRuntimeRP177.secondary,FocusTheme.argb)
            val original=style()
            for(preset in ThemeStudioRP173.presets) {
                ThemeRuntimeRP177.apply(preset)
                val selected=style()
                ThemeRuntimeRP177.originalPageRP189={true}
                assertEquals(preset.id,original,style())
                ThemeRuntimeRP177.originalPageRP189={false}
                assertEquals(preset.id,selected,style())
            }
        } finally {
            ThemeRuntimeRP177.originalPageRP189={false}
            ThemeRuntimeRP177.restore(previous)
            ThemeRuntimeRP177.originalPageRP189=previousRoute
        }
    }
}
