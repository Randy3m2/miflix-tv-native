package com.miflix.native2.data

/** Premium visual presets. Artwork is optional; the palette is safe to use
 * immediately and can be synced as a small JSON preference through Supabase. */
data class ThemeStudioPresetRP173(
    val id: String,
    val title: String,
    val primary: Long,
    val secondary: Long,
    val background: Long,
    val surface: Long,
    val focus: Long,
    val atmosphere: String,
    val motion: String? = null,
)

object ThemeStudioRP173 {
    val presets: List<ThemeStudioPresetRP173> = listOf(
        ThemeStudioPresetRP173("god_of_war", "God of War", 0xFFB92B35, 0xFF9FC7E6, 0xFF080A0D, 0xFF1A1D22, 0xFFCF3D49, "stone, snow and battle-worn metal", "snow"),
        ThemeStudioPresetRP173("horizon_zero_dawn", "Horizon Zero Dawn", 0xFF19C8C5, 0xFFF28C35, 0xFF071214, 0xFF142529, 0xFF47E2E5, "post-apocalyptic wilderness and machines", "hologram"),
        ThemeStudioPresetRP173("the_last_of_us", "The Last of Us", 0xFF89945A, 0xFF9B7754, 0xFF090B09, 0xFF1B201A, 0xFFC0A45E, "overgrown cities and survival", "dust"),
        ThemeStudioPresetRP173("ghost_of_tsushima", "Ghost of Tsushima", 0xFFB21F32, 0xFFE8DDC6, 0xFF080808, 0xFF1B1917, 0xFFD5AA55, "ink, wind and crimson leaves", "leaves"),
        ThemeStudioPresetRP173("game_of_thrones", "Game of Thrones", 0xFFB08B3E, 0xFF8F1D2C, 0xFF07080A, 0xFF191B20, 0xFFE1B95C, "ancient kingdoms, fire and ice", "embers"),
        ThemeStudioPresetRP173("friends", "Friends", 0xFF8C5BB7, 0xFFF0C34E, 0xFF101015, 0xFF27222A, 0xFFB986E5, "warm New York apartment", "soft_glow"),
        ThemeStudioPresetRP173("dark", "Dark", 0xFF6B88B5, 0xFFB4B8C4, 0xFF06080C, 0xFF171B24, 0xFF86B6E8, "rain, caves and time", "rain"),
        ThemeStudioPresetRP173("chernobyl", "Chernobyl", 0xFFB9A66D, 0xFF65705E, 0xFF080A08, 0xFF1B1D18, 0xFFD0B878, "industrial decay and muted warning light"),
        ThemeStudioPresetRP173("justice_league", "Justice League", 0xFF2E6DCA, 0xFFD9A62E, 0xFF060A14, 0xFF131C30, 0xFF5C9BFF, "heroic steel and electric sky", "energy"),
        ThemeStudioPresetRP173("marvel", "Marvel", 0xFFE23636, 0xFF3F75D6, 0xFF08090D, 0xFF1B1D27, 0xFFFF5252, "comic-book red and cosmic blue", "spark"),
        ThemeStudioPresetRP173("superman", "Superman", 0xFF2F6FCE, 0xFFE5473D, 0xFF071021, 0xFF142643, 0xFF58A0FF, "sky, steel and solar red", "solar"),
        ThemeStudioPresetRP173("batman", "Batman", 0xFF151515, 0xFFD1AE51, 0xFF040404, 0xFF171717, 0xFFE1C26B, "gotham noir and signal gold", "rain"),
    )

    fun byId(id: String): ThemeStudioPresetRP173 = presets.firstOrNull { it.id == id } ?: presets.first()
}
