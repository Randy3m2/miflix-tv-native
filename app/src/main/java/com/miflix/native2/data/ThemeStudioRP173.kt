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
        ThemeStudioPresetRP173("cyberpunk_2077", "Cyberpunk 2077 — Night City", 0xFFFCEE0A, 0xFFFF2A6D, 0xFF08090F, 0xFF1C1B2B, 0xFFFF3B81, "neon megacity and chrome", "scanlines"),
        ThemeStudioPresetRP173("elden_ring", "Elden Ring — The Lands Between", 0xFFC7A85A, 0xFF657B61, 0xFF090B09, 0xFF1A1D18, 0xFFE2C777, "ancient ruins and golden grace", "embers"),
        ThemeStudioPresetRP173("red_dead_redemption_2", "Red Dead Redemption 2 — Outlaw", 0xFFB7653B, 0xFF6F8A85, 0xFF100B08, 0xFF241A15, 0xFFD08A51, "frontier dusk and weathered leather"),
        ThemeStudioPresetRP173("resident_evil", "Resident Evil — Raccoon City", 0xFF9EB7C7, 0xFF8A2735, 0xFF070B10, 0xFF172029, 0xFFBBD9E8, "rain, quarantine and survival", "rain"),
        ThemeStudioPresetRP173("assassins_creed", "Assassin’s Creed — Animus", 0xFFE7E0D0, 0xFF8D1F2D, 0xFF0A0B0E, 0xFF1A1B20, 0xFFDABF8A, "historical memory and hidden blades"),
        ThemeStudioPresetRP173("gta_vice_city", "GTA Vice City — Neon Miami", 0xFFFF65C3, 0xFF21D4FD, 0xFF0C0614, 0xFF25112A, 0xFFFF8EDB, "sunset, palm trees and neon", "scanlines"),
        ThemeStudioPresetRP173("halo", "Halo — Spartan", 0xFF91C9B1, 0xFF7D8D9F, 0xFF07100E, 0xFF172422, 0xFFB1F2D2, "military sci-fi and ringworld skies"),
        ThemeStudioPresetRP173("fallout", "Fallout — Pip-Boy", 0xFF63E69A, 0xFFC5A35A, 0xFF07100B, 0xFF17251A, 0xFF88FFB0, "retro-futurist wasteland", "scanlines"),
        ThemeStudioPresetRP173("silent_hill", "Silent Hill — The Fog", 0xFF9AA4A3, 0xFF8B343D, 0xFF090B0B, 0xFF1C1F1F, 0xFFC0C9C7, "dense fog and industrial dread", "fog"),
        ThemeStudioPresetRP173("the_witcher", "The Witcher — White Wolf", 0xFFC5D1D7, 0xFF8D6F43, 0xFF080A0C, 0xFF1A1E22, 0xFFE2EDF0, "cold steel and monster trails", "snow"),
        ThemeStudioPresetRP173("stranger_things", "Stranger Things — Upside Down", 0xFFE03A3A, 0xFF5A72C7, 0xFF08070E, 0xFF1B1728, 0xFFFF5858, "80s suburbia and the Upside Down", "embers"),
        ThemeStudioPresetRP173("peaky_blinders", "Peaky Blinders — Shelby", 0xFFB5A58A, 0xFF4B5563, 0xFF090A0C, 0xFF1B1C20, 0xFFD1BE99, "Birmingham smoke and tailored noir"),
        ThemeStudioPresetRP173("arcane", "Arcane — Piltover & Zaun", 0xFF4CC9E8, 0xFFE27A4B, 0xFF090A13, 0xFF1B2032, 0xFF6CE6FF, "hextech glow and undercity grit", "energy"),
        ThemeStudioPresetRP173("breaking_bad", "Breaking Bad — Heisenberg", 0xFFB2C15D, 0xFFB58A52, 0xFF0B0C08, 0xFF202219, 0xFFD2E06D, "desert chemistry and caution tape"),
        ThemeStudioPresetRP173("the_mandalorian", "The Mandalorian — Beskar", 0xFF9CA8B0, 0xFFC48B4C, 0xFF080A0C, 0xFF1A1F24, 0xFFC9D5DD, "beskar steel and outer-rim dust"),
        ThemeStudioPresetRP173("dune", "Dune — Arrakis", 0xFFD6A866, 0xFFB96E45, 0xFF110D08, 0xFF292016, 0xFFEBC58A, "desert horizons and spice haze", "dust"),
        ThemeStudioPresetRP173("blade_runner_2049", "Blade Runner 2049 — Neon Noir", 0xFFF05D3D, 0xFF4E8DB6, 0xFF090A12, 0xFF1B1E2A, 0xFFFF765C, "rain-soaked neon dystopia", "rain"),
        ThemeStudioPresetRP173("the_matrix", "The Matrix — Digital Rain", 0xFF31D36B, 0xFF7CFF9B, 0xFF030805, 0xFF0D1D12, 0xFF45F27C, "terminal green and digital rain", "scanlines"),
        ThemeStudioPresetRP173("dragon_ball", "Dragon Ball — Saiyan Legacy", 0xFFFFA62B, 0xFF397DDE, 0xFF080B16, 0xFF18233D, 0xFFFFC34D, "ki energy and sky-blue adventure", "energy"),
        ThemeStudioPresetRP173("naruto", "Naruto — Hidden Leaf", 0xFFE27A30, 0xFF4DA5C8, 0xFF080C10, 0xFF17232A, 0xFFFF9A4D, "hidden villages and shinobi fire", "leaves"),
    )

    fun byId(id: String): ThemeStudioPresetRP173 = presets.firstOrNull { it.id == id } ?: presets.first()
}
