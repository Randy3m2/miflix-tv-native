package com.miflix.native2.data
import org.junit.Assert.*
import org.junit.Test
import java.time.*
class FavoriteSyncRP164Test {
 @Test fun firstPageAndNextPageAreThirty() {assertEquals(30,FavoriteSyncRP164.PAGE);val ids=(1..75).map {"tmdb:movie:$it"};assertEquals(30,ids.take(FavoriteSyncRP164.PAGE).size);assertEquals(60,ids.take(FavoriteSyncRP164.PAGE*2).size)}
 @Test fun pendingAddSurvivesAStaleRead() {assertEquals(listOf("tmdb:movie:2","tmdb:movie:1"),FavoriteSyncRP164.merge(listOf("tmdb:movie:1"),mapOf("tmdb:movie:2" to true)))}
 @Test fun pendingRemoveSurvivesOtherDevicesAdds() {assertEquals(listOf("tmdb:movie:3"),FavoriteSyncRP164.merge(listOf("tmdb:movie:1","tmdb:movie:3"),mapOf("tmdb:movie:1" to false)))}
 @Test fun newerTapIsNotAcknowledgedByAnOlderResponse() {assertEquals(mapOf("tmdb:movie:1" to false),FavoriteSyncRP164.acknowledge(mapOf("tmdb:movie:1" to false),mapOf("tmdb:movie:1" to true)))}
 @Test fun acknowledgedChangesClearButUnrelatedPendingChangesRemain() {assertEquals(mapOf("tmdb:series:2" to true),FavoriteSyncRP164.acknowledge(mapOf("tmdb:movie:1" to true,"tmdb:series:2" to true),mapOf("tmdb:movie:1" to true)))}
 @Test fun duplicateImportsDoNotHideOrDuplicateExistingFavorites() {assertEquals(listOf("tmdb:series:2","tmdb:movie:1"),FavoriteSyncRP164.merge(listOf("tmdb:movie:1","tmdb:series:2"),mapOf("tmdb:series:2" to true)))}
 @Test fun noReleaseFloodOnInitialSubscription() {assertFalse(ReleasePolicyRP164.changed(null,"episode1"));assertTrue(ReleasePolicyRP164.changed("pending","episode1"));assertTrue(ReleasePolicyRP164.changed("episode1","episode2"));assertFalse(ReleasePolicyRP164.changed("episode1","episode1"))}
 @Test fun backgroundBatchesRotateWithoutStarvingOlderSubscriptions() {val ids=(1..65).map {"tmdb:movie:$it"};val all=(ReleasePolicyRP164.batch(ids,0)+ReleasePolicyRP164.batch(ids,30)+ReleasePolicyRP164.batch(ids,60)).toSet();assertEquals(ids.toSet(),all)}
 @Test fun calendarGridHasCorrectLeapDayAndMondayAlignment() {val rows=WidgetDatesRP164.days(YearMonth.of(2028,2));assertEquals(42,rows.size);assertNull(rows[0]);assertEquals(LocalDate.of(2028,2,1),rows[1]);assertEquals(29,rows.count {it!=null})}
 @Test fun calendarGridDoesNotInventNextMonthEvents() {val rows=WidgetDatesRP164.days(YearMonth.of(2026,11));assertEquals(LocalDate.of(2026,11,1),rows[6]);assertEquals(30,rows.count {it!=null})}
}
