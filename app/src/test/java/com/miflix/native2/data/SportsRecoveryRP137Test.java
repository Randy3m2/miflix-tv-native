package com.miflix.native2.data;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class SportsRecoveryRP137Test {
    @Test public void continuesAfterCurrentAndSkipsFailedWithoutRepeating() {
        List<String> links=Arrays.asList("a","b","c");Set<String> failed=new HashSet<>();
        failed.add("b");assertEquals(2,SportsRecoveryRP137.next(links,1,failed));
        failed.add("c");assertEquals(0,SportsRecoveryRP137.next(links,2,failed));
        failed.add("a");assertEquals(-1,SportsRecoveryRP137.next(links,0,failed));
    }
    @Test public void signedReplacementIsFreshButOldUrlsAreNotReplayed() {
        Set<String> failed=new HashSet<>(Arrays.asList("channel?token=old","backup"));
        assertEquals(1,SportsRecoveryRP137.next(Arrays.asList("backup","channel?token=new"),-1,failed));
        assertEquals(-1,SportsRecoveryRP137.next(Arrays.asList("backup","channel?token=old"),-1,failed));
        assertEquals(-1,SportsRecoveryRP137.next(Collections.emptyList(),-1,failed));
    }
    @Test public void batchLimitStopsBadSources() {
        assertEquals(-1,SportsRecoveryRP137.next(Arrays.asList("1","2","3","4","5","6"),0,new HashSet<>(Arrays.asList("1","2","3","4","5"))));
    }
    @Test public void detectsMidGameStallEvenAfterHours() {
        SportsRecoveryRP137 watch=new SportsRecoveryRP137();
        assertFalse(watch.stalled(0,500,false,true));
        assertFalse(watch.stalled(7200000,7200500,true,true));
        assertFalse(watch.stalled(7219999,7200500,true,true));
        assertTrue(watch.stalled(7220000,7200500,true,true));
        assertFalse(watch.stalled(7220500,7200600,true,true));
    }
    @Test public void pauseBackgroundAndMenusResetWaiting() {
        SportsRecoveryRP137 watch=new SportsRecoveryRP137();
        assertFalse(watch.stalled(0,100,true,true));
        assertFalse(watch.stalled(30000,100,true,false));
        assertFalse(watch.stalled(60000,100,true,true));
        assertTrue(watch.stalled(80000,100,true,true));
    }
    @Test public void onlyThirtyHealthySecondsRearmTheNextOutage() {
        SportsRecoveryRP137 watch=new SportsRecoveryRP137();
        assertFalse(watch.healthy(0,true,true));
        assertFalse(watch.healthy(29999,true,true));
        assertTrue(watch.healthy(30000,true,true));
        assertFalse(watch.healthy(31000,false,true));
        assertFalse(watch.healthy(32000,true,true));
        assertFalse(watch.healthy(61000,true,true));
        assertTrue(watch.healthy(62000,true,true));
        assertFalse(watch.healthy(99999,true,false));
    }
}
