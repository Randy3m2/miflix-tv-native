package com.miflix.native2.model

object Catalogs {
    val providers = listOf(
        ProviderConfig("netflix", "Netflix", listOf(8, 175),
            "https://cdn.jsdelivr.net/gh/luckynumb3rs/stremio-perfect-setup/collections/streaming/cover/netflix.png",
            "https://cdn.jsdelivr.net/gh/luckynumb3rs/stremio-perfect-setup/collections/streaming/focused/netflix.gif"),
        ProviderConfig("disney", "Disney+", listOf(337),
            "https://cdn.jsdelivr.net/gh/luckynumb3rs/stremio-perfect-setup/collections/streaming/cover/disney-plus.png",
            "https://cdn.jsdelivr.net/gh/luckynumb3rs/stremio-perfect-setup/collections/streaming/focused/disney-plus.gif"),
        ProviderConfig("prime", "Prime Video", listOf(9, 119),
            "https://cdn.jsdelivr.net/gh/luckynumb3rs/stremio-perfect-setup/collections/streaming/cover/prime-video.png",
            "https://cdn.jsdelivr.net/gh/luckynumb3rs/stremio-perfect-setup/collections/streaming/focused/prime-video.gif"),
        ProviderConfig("apple", "Apple TV+", listOf(350),
            "https://cdn.jsdelivr.net/gh/luckynumb3rs/stremio-perfect-setup/collections/streaming/cover/apple-tv.png",
            "https://cdn.jsdelivr.net/gh/luckynumb3rs/stremio-perfect-setup/collections/streaming/focused/apple-tv.gif"),
        ProviderConfig("hbo", "HBO Max", listOf(1899, 384),
            "https://cdn.jsdelivr.net/gh/luckynumb3rs/stremio-perfect-setup/collections/streaming/cover/hbo-max.png",
            "https://cdn.jsdelivr.net/gh/luckynumb3rs/stremio-perfect-setup/collections/streaming/focused/hbo-max.gif")
    )

    val genres = listOf(
        GenreConfig("action", "Action", 28, 10759),
        GenreConfig("adventure", "Adventure", 12, 10759),
        GenreConfig("animation", "Animation", 16, 16),
        GenreConfig("comedy", "Comedy", 35, 35),
        GenreConfig("crime", "Crime", 80, 80),
        GenreConfig("documentary", "Documentary", 99, 99),
        GenreConfig("drama", "Drama", 18, 18),
        GenreConfig("family", "Family", 10751, 10751),
        GenreConfig("fantasy", "Fantasy", 14, 10765),
        GenreConfig("horror", "Horror", 27, 9648),
        GenreConfig("mystery", "Mystery", 9648, 9648),
        GenreConfig("romance", "Romance", 10749, 18),
        GenreConfig("scifi", "Science Fiction", 878, 10765),
        GenreConfig("thriller", "Thriller", 53, 9648),
        GenreConfig("war", "War", 10752, 10768)
    )

    val years: List<Int> = (2026 downTo 2000).toList()
}
