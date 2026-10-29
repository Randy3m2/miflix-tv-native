package com.miflix.native2.ui
import org.junit.Assert.*
import org.junit.Test
class TimelineRP150Test {
    @Test fun unknownStoriesAreNotAssignedAnInventedChronologicalPosition() {assertNull(timelineRankRP150("walking_dead","Unknown spinoff"));assertNull(timelineSagaRP150("xmen"))}
    @Test fun knownSagaPreservesBothReleaseAndStoryOrder() {
        val marvel=CinemaJourneys.getValue(timelineSagaRP150("marvel")!!)
        assertEquals("Captain America: The First Avenger",marvel.minBy {it.story}.title)
        assertEquals("Iron Man",marvel.minBy {it.year}.title)
        assertEquals(marvel.size,marvel.map {it.cloud}.distinct().size)
    }
    @Test fun gotHasRoomForTheIntermediatePrequel() {
        assertEquals(1,timelineRankRP150("game_of_thrones","House of the Dragon"));assertEquals(2,timelineRankRP150("game_of_thrones","A Knight of the Seven Kingdoms"));assertEquals(3,timelineRankRP150("game_of_thrones","Game of Thrones"))
    }
}
