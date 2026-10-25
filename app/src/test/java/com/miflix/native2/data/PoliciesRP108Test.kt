package com.miflix.native2.data

import org.junit.Assert.*
import org.junit.Test

class PoliciesRP108Test {
    @Test fun homeClaimsFocusOnlyOncePerEntryOwner() {val p=HeaderFocusRP108();assertTrue(p.claim("a","HOME"));assertFalse(p.claim("a","HOME"))}
    @Test fun friendsHandoffDoesNotClaimHomeInstead() {val p=HeaderFocusRP108();p.claim("a","HOME");p.navigate("a","FRIENDS");assertFalse(p.claim("a","HOME"));assertTrue(p.claim("a","FRIENDS"));assertFalse(p.claim("a","FRIENDS"))}
    @Test fun anotherProfileCannotConsumePendingFocus() {val p=HeaderFocusRP108();p.navigate("a","FRIENDS");assertFalse(p.claim("b","FRIENDS"));assertTrue(p.claim("a","FRIENDS"))}
    @Test fun profileSwitchGetsItsOwnHomeFocus() {val p=HeaderFocusRP108();assertTrue(p.claim("a","HOME"));assertTrue(p.claim("b","HOME"));assertFalse(p.claim("b","HOME"))}
    @Test fun gearAndAlertsKeepTheirOwnFocus() {val p=HeaderFocusRP108();p.claim("a","HOME");for(s in listOf("SETTINGS","NOTIFICATIONS")) {p.navigate("a",s);assertTrue(p.claim("a",s));assertFalse(p.claim("a","HOME"))}}
    @Test fun detailsDoNotReclaimHeaderFocus() {val p=HeaderFocusRP108();p.claim("a","HOME");assertFalse(p.claim("a","DETAILS"))}
    @Test fun sportsRetryIsConsumedExactlyOnce() {val p=SportsRetryRP108();assertTrue(p.take(true,false,"sports"));repeat(10) {assertFalse(p.take(true,false,"sports"))}}
    @Test fun noVodRetry() {assertFalse(SportsRetryRP108().take(false,false,"sports"))}
    @Test fun noRecordingRetry() {assertFalse(SportsRetryRP108().take(true,true,"sports"))}
    @Test fun noRegularTvRetry() {for(s in listOf("nauta","custom:rp45","iptv","lidom")) assertFalse(SportsRetryRP108().take(true,false,s))}
    @Test fun ineligibleCallDoesNotConsumeSportsRetry() {val p=SportsRetryRP108();assertFalse(p.take(false,false,"sports"));assertTrue(p.take(true,false,"sports"))}
    @Test fun newPlaybackRequestHasItsOwnRetryBudget() {val first=SportsRetryRP108();first.take(true,false,"sports");assertTrue(SportsRetryRP108().take(true,false,"sports"))}
}
