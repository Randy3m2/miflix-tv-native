package com.miflix.native2.data

/** Explicit titles prevent unrelated zombie, fantasy, or Marvel titles entering these universes. */
internal object FranchiseCatalogRP29 {
    data class Entry(val title:String,val year:Int)
    val brands=listOf("marvel" to "Marvel","dc" to "DC","star_wars" to "Star Wars","xmen" to "X-Men","walking_dead" to "The Walking Dead","game_of_thrones" to "Game of Thrones")
    fun title(brand:String)=brands.firstOrNull {it.first==brand}?.second ?: brand
    fun initialType(brand:String)=if(brand in setOf("walking_dead","game_of_thrones")) "series" else "movie"
    fun supportsMovies(brand:String)=brand !in setOf("walking_dead","game_of_thrones")
    fun curated(brand:String)=brand in setOf("star_wars","xmen","walking_dead","game_of_thrones")
    fun entries(brand:String,type:String):List<Entry> = when(brand+":"+type) {
        "star_wars:movie" -> listOf(Entry("The Mandalorian and Grogu",2026),Entry("Star Wars",1977),Entry("The Empire Strikes Back",1980),Entry("Return of the Jedi",1983),Entry("Star Wars: Episode I - The Phantom Menace",1999),Entry("Star Wars: Episode II - Attack of the Clones",2002),Entry("Star Wars: Episode III - Revenge of the Sith",2005),Entry("Star Wars: The Force Awakens",2015),Entry("Star Wars: The Last Jedi",2017),Entry("Star Wars: The Rise of Skywalker",2019),Entry("Rogue One: A Star Wars Story",2016),Entry("Solo: A Star Wars Story",2018),Entry("Star Wars: The Clone Wars",2008))
        "star_wars:series" -> listOf(Entry("Star Wars: Maul - Shadow Lord",2026),Entry("The Mandalorian",2019),Entry("Andor",2022),Entry("Ahsoka",2023),Entry("Obi-Wan Kenobi",2022),Entry("The Book of Boba Fett",2021),Entry("Star Wars: Skeleton Crew",2024),Entry("The Acolyte",2024),Entry("Star Wars: The Clone Wars",2008),Entry("Star Wars Rebels",2014),Entry("Star Wars: The Bad Batch",2021),Entry("Star Wars: Tales of the Jedi",2022),Entry("Star Wars: Tales of the Empire",2024),Entry("Star Wars: Tales of the Underworld",2025),Entry("Star Wars: Visions",2021),Entry("Star Wars Resistance",2018),Entry("Star Wars: Clone Wars",2003),Entry("Star Wars: Young Jedi Adventures",2023),Entry("Star Wars: Droids",1985),Entry("Star Wars: Ewoks",1985),Entry("Star Wars: Forces of Destiny",2017))
        "xmen:movie" -> listOf(Entry("X-Men",2000),Entry("X2",2003),Entry("X-Men: The Last Stand",2006),Entry("X-Men Origins: Wolverine",2009),Entry("X-Men: First Class",2011),Entry("The Wolverine",2013),Entry("X-Men: Days of Future Past",2014),Entry("Deadpool",2016),Entry("X-Men: Apocalypse",2016),Entry("Logan",2017),Entry("Deadpool 2",2018),Entry("Dark Phoenix",2019),Entry("The New Mutants",2020),Entry("Deadpool & Wolverine",2024))
        "xmen:series" -> listOf(Entry("X-Men",1992),Entry("X-Men: Evolution",2000),Entry("Wolverine and the X-Men",2009),Entry("X-Men '97",2024),Entry("Legion",2017),Entry("The Gifted",2017),Entry("X-Men",2011),Entry("Wolverine",2011))
        "walking_dead:movie" -> listOf()
        "walking_dead:series" -> listOf(Entry("The Walking Dead",2010),Entry("Fear the Walking Dead",2015),Entry("The Walking Dead: World Beyond",2020),Entry("Tales of the Walking Dead",2022),Entry("The Walking Dead: Dead City",2023),Entry("The Walking Dead: Daryl Dixon",2023),Entry("The Walking Dead: The Ones Who Live",2024),Entry("The Walking Dead: Torn Apart",2011),Entry("The Walking Dead: Cold Storage",2012),Entry("The Walking Dead: The Oath",2013),Entry("The Walking Dead: Red Machete",2017),Entry("Fear the Walking Dead: Flight 462",2015),Entry("Fear the Walking Dead: Passage",2016),Entry("Fear the Walking Dead: Dead in the Water",2022))
        "game_of_thrones:movie" -> listOf()
        "game_of_thrones:series" -> listOf(Entry("Game of Thrones",2011),Entry("House of the Dragon",2022),Entry("A Knight of the Seven Kingdoms",2026))
        else -> emptyList()
    }
}
