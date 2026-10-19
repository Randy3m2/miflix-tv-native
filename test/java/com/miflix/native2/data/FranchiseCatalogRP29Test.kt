package com.miflix.native2.data
import org.junit.Assert.*
import org.junit.Test
class FranchiseCatalogRP29Test {
    @Test fun allSixBrandsAvailable() {assertEquals(listOf("marvel","dc","star_wars","xmen","walking_dead","game_of_thrones"),FranchiseCatalogRP29.brands.map {it.first})}
    @Test fun walkingDeadIncludesMainShowsAndWebSeries() {
        val titles=FranchiseCatalogRP29.entries("walking_dead","series").map {it.title}
        for(name in listOf("The Walking Dead","Fear the Walking Dead","The Walking Dead: World Beyond","Tales of the Walking Dead","The Walking Dead: Dead City","The Walking Dead: Daryl Dixon","The Walking Dead: The Ones Who Live","Fear the Walking Dead: Dead in the Water")) assertTrue(name,name in titles)
    }
    @Test fun westerosIncludesBothPrequels() {assertEquals(listOf("Game of Thrones","House of the Dragon","A Knight of the Seven Kingdoms"),FranchiseCatalogRP29.entries("game_of_thrones","series").map {it.title})}
    @Test fun tvUniversesOpenSeriesAndHaveNoMovieTab() {
        for(brand in listOf("walking_dead","game_of_thrones")) {assertEquals("series",FranchiseCatalogRP29.initialType(brand));assertFalse(FranchiseCatalogRP29.supportsMovies(brand));assertTrue(FranchiseCatalogRP29.entries(brand,"movie").isEmpty())}
    }
    @Test fun starWarsAndMutantsSupportBothTypes() {
        for(brand in listOf("star_wars","xmen")) {assertEquals("movie",FranchiseCatalogRP29.initialType(brand));assertTrue(FranchiseCatalogRP29.supportsMovies(brand));for(type in listOf("movie","series")) assertTrue(FranchiseCatalogRP29.entries(brand,type).isNotEmpty())}
    }
    @Test fun entriesUniqueAndBounded() {for((brand,_) in FranchiseCatalogRP29.brands) for(type in listOf("movie","series")) {val e=FranchiseCatalogRP29.entries(brand,type);assertEquals(e.size,e.distinct().size);assertTrue(e.size<=25)}}
}
