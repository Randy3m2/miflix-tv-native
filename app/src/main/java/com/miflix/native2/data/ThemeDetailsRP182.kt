package com.miflix.native2.data

/** Small original vector emblems, drawn inside reserved space instead of over text. */
internal object ThemeDetailsRP182 {
    data class Detail(val symbol:String,val path:String)
    val details=mapOf(
        "default" to Detail("Original BruniO","M4 3H20V21H4ZM9 7L17 12L9 17Z"),
        "halloween" to Detail("Pumpkin","M12 7V3L15 2M12 7C1 1 0 23 8 21L12 22L16 21C24 23 23 1 12 7ZM5 12L8 10L9 13ZM15 13L16 10L19 12ZM8 17L10 18L12 16L14 18L16 17"),
        "summer" to Detail("Sun and waves","M15 7A4 4 0 1 1 7 7A4 4 0 1 1 15 7M11 0V2M11 12V14M4 7H2M20 7H18M5 1L7 3M17 1L15 3M2 18Q5 15 8 18T14 18T22 18M2 22Q5 19 8 22T14 22T22 22"),
        "christmas" to Detail("Evergreen and star","M12 1L14 5H18L15 8L16 12L12 10L8 12L9 8L6 5H10ZM8 12L4 18H8L3 23H21L16 18H20L16 12M12 19V24"),
        "spring" to Detail("Blossom","M12 10C2 -2 0 14 9 13C-1 24 16 28 14 15C26 24 28 6 15 10C24 -2 8 -3 12 10ZM14 12A2 2 0 1 1 10 12A2 2 0 1 1 14 12"),
        "nba" to Detail("Basketball","M22 12A10 10 0 1 1 2 12A10 10 0 1 1 22 12M2 12H22M12 2V22M5 4Q15 12 5 20M19 4Q9 12 19 20"),
        "mlb" to Detail("Baseball seams","M22 12A10 10 0 1 1 2 12A10 10 0 1 1 22 12M5 4Q13 12 5 20M19 4Q11 12 19 20M5 7L9 6M7 11H11M7 15L10 17M19 7L15 6M17 11H13M17 15L14 17"),
        "spider_man" to Detail("Web","M12 2V22M2 12H22M4 4L20 20M20 4L4 20M6 6Q12 10 18 6M6 18Q12 14 18 18M6 6Q10 12 6 18M18 6Q14 12 18 18M3 3Q12 7 21 3M3 21Q12 17 21 21"),
        "venom" to Detail("Symbiote eyes","M2 4L10 10L9 14L5 13ZM22 4L14 10L15 14L19 13ZM6 18L9 20L12 18L15 20L18 18M12 19Q7 25 15 23"),
        "god_of_war" to Detail("Leviathan axe","M6 23L16 2M12 7L18 2Q25 8 20 14L13 10M6 17L10 19M9 12L13 14"),
        "horizon_zero_dawn" to Detail("Focus triangle","M12 2L23 21H1ZM12 8L18 18H6ZM12 12V16"),
        "the_last_of_us" to Detail("Survivor fern","M6 23L18 2M8 18L2 14L10 14M11 12L5 8L13 9M14 7L10 3M9 17L18 16L12 13M13 11L21 9L16 6"),
        "ghost_of_tsushima" to Detail("Katana and wind","M3 22L7 18M5 16L10 21M8 18L21 3Q20 12 10 20M2 6Q6 1 11 5M1 10Q5 5 10 9"),
        "game_of_thrones" to Detail("Crown","M3 8L7 12L12 3L17 12L21 8L19 21H5ZM6 17H18"),
        "friends" to Detail("Coffee cup","M3 8H17V17Q10 24 3 17ZM17 9H20Q25 14 17 16M5 2L6 5M10 1L11 4M15 2L16 5M2 23H19"),
        "dark" to Detail("Time knot","M12 2L23 20H1ZM12 2Q2 13 23 20M1 20Q18 21 12 2M23 20Q6 21 1 20M15 12A3 3 0 1 1 9 12A3 3 0 1 1 15 12"),
        "chernobyl" to Detail("Radiation","M14 12A2 2 0 1 1 10 12A2 2 0 1 1 14 12M9 9L5 3Q12 -1 19 3L15 9M8 12L2 12Q1 20 8 23L11 16M16 12H22Q23 20 16 23L13 16"),
        "justice_league" to Detail("Hero crest","M12 1L22 5L20 17L12 23L4 17L2 5ZM8 7H16M12 7V17M8 17H16"),
        "marvel" to Detail("Comic burst","M12 1L15 7L22 4L19 11L24 15L17 17L16 24L10 19L3 22L5 15L0 10L8 8Z"),
        "superman" to Detail("Solar diamond","M6 3H18L23 9L12 23L1 9ZM6 9L10 6H17L15 9H9L8 12H16L14 16H9"),
        "batman" to Detail("Night wings","M1 6L8 10L10 5L12 8L14 5L16 10L23 6L20 17L16 15L12 21L8 15L4 17Z"),
        "cyberpunk_2077" to Detail("Neon circuit","M2 4H16L22 10V20H10L2 12ZM6 8H14L18 12V16H12L6 10M2 22H8M19 2H24"),
        "elden_ring" to Detail("Golden rings","M18 10A6 6 0 1 1 6 10A6 6 0 1 1 18 10M18 16A6 6 0 1 1 6 16A6 6 0 1 1 18 16M12 1V23M8 3H16"),
        "red_dead_redemption_2" to Detail("Outlaw hat","M2 18Q12 23 22 18L19 14L16 3L12 6L8 3L5 14ZM5 14Q12 18 19 14"),
        "resident_evil" to Detail("Quarantine umbrella","M2 12A10 10 0 0 1 22 12L17 10L12 12L7 10ZM12 2V19Q12 25 17 21M7 10L12 2L17 10"),
        "assassins_creed" to Detail("Hidden blade","M12 2L22 23L15 17L12 8L9 17L2 23ZM7 20H17"),
        "gta_vice_city" to Detail("Palm and sunset","M9 23Q15 12 12 6M12 6Q1 -1 2 12Q5 7 12 6M12 6Q21 -1 23 11Q17 7 12 6M12 6Q14 0 18 1M1 21H5M18 21H23"),
        "halo" to Detail("Spartan visor","M3 6L8 2H16L21 6V20H15L12 23L9 20H3ZM5 7H19V12L15 15H9L5 12ZM8 18H16"),
        "fallout" to Detail("Atomic orbit","M14 12A2 2 0 1 1 10 12A2 2 0 1 1 14 12M1 12C1 4 23 4 23 12S1 20 1 12M6 2C13 -2 24 20 18 22S0 6 6 2M18 2C24 6 13 26 6 22S12 -2 18 2"),
        "silent_hill" to Detail("Fog lantern","M8 6V3H16V6M6 6H18L20 21H4ZM9 9H15V18H9ZM1 23H23M12 11V16"),
        "the_witcher" to Detail("Wolf medallion","M3 2L10 6L12 4L14 6L21 2L19 16L12 23L5 16ZM6 10L10 12M18 10L14 12M9 17L12 15L15 17L12 20Z"),
        "stranger_things" to Detail("Upside-down portal","M6 2H18V22H6ZM9 5H15V19H9ZM1 6L4 9L2 13L5 17M23 6L20 9L22 13L19 17"),
        "peaky_blinders" to Detail("Flat cap","M2 15Q1 4 14 4Q22 5 22 14L15 18H3ZM3 15H22L17 20H8M9 6L12 14"),
        "arcane" to Detail("Hextech crystal","M12 1L22 7V17L12 23L2 17V7ZM12 5L17 9V15L12 19L7 15V9ZM12 5V19M7 9L17 15M17 9L7 15"),
        "breaking_bad" to Detail("Chemistry flask","M9 2H15M10 2V9L3 20Q2 23 6 23H18Q22 23 21 20L14 9V2M7 16H17M10 20H11M15 18H16"),
        "the_mandalorian" to Detail("Beskar helmet","M3 20V9Q3 1 12 1Q21 1 21 9V20L15 23V13H9V23ZM4 8H20V11H14V21H10V11H4"),
        "dune" to Detail("Desert eclipse","M18 8A6 6 0 1 1 6 8A6 6 0 1 1 18 8M1 20Q8 9 15 18Q20 12 24 18M1 24Q12 15 24 23"),
        "blade_runner_2049" to Detail("Origami bird","M1 12L10 10L18 2L17 12L23 16L13 15L7 23L7 16ZM10 10L13 15M17 12L13 15"),
        "the_matrix" to Detail("Digital code","M2 2H6V10H2ZM10 2V10M17 2H21V10H17ZM4 14V22M10 14H14V22H10ZM19 14V22"),
        "dragon_ball" to Detail("Four-star sphere","M23 12A11 11 0 1 1 1 12A11 11 0 1 1 23 12M7 6L8 8L10 8L8 10L9 12L7 11L5 12L6 10L4 8H6ZM16 6L17 8H19L17 10L18 12L16 11L14 12L15 10L13 8H15M7 15L8 18H10L8 20L6 19M16 15L17 18H19L17 20L15 19"),
        "naruto" to Detail("Shuriken","M12 1L14 8L23 12L16 14L12 23L10 16L1 12L8 10ZM14 12A2 2 0 1 1 10 12A2 2 0 1 1 14 12")
    )
    fun forId(id:String)=details[id]
    val seasonal=setOf("halloween","summer","christmas","spring")
    val sports=setOf("nba","mlb")
}
