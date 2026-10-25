package com.miflix.native2.data;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class CalendarSubscriptionsRP122Test {
    private String sig(String... keys) {return CalendarSubscriptionCacheRP122.signature(new HashSet<>(Arrays.asList(keys)),"");}
    @Test public void secondLeagueInvalidatesFreshMonth() {
        assertFalse(CalendarSubscriptionCacheRP122.canReuse(sig("LIDOM:668"),sig("LIDOM:668","NBA:13"),1000,2000,false));
    }
    @Test public void secondTeamInSameLeagueInvalidatesMonth() {
        assertFalse(CalendarSubscriptionCacheRP122.canReuse(sig("NBA:13"),sig("NBA:13","NBA:9"),1000,2000,false));
    }
    @Test public void removingTeamInvalidatesMonth() {
        assertFalse(CalendarSubscriptionCacheRP122.canReuse(sig("NBA:13","LIDOM:668"),sig("NBA:13"),1000,2000,false));
    }
    @Test public void oldVersionCacheIsInvalidated() {
        assertFalse(CalendarSubscriptionCacheRP122.canReuse("",sig("NBA:13"),1000,2000,false));
    }
    @Test public void sameSubscriptionsReuseRegardlessOfOrder() {
        assertTrue(CalendarSubscriptionCacheRP122.canReuse(sig("NBA:13","LIDOM:668"),sig("LIDOM:668","NBA:13"),1000,2000,false));
    }
    @Test public void forceAndExpiryBypassCache() {
        String s=sig("NBA:13");
        assertFalse(CalendarSubscriptionCacheRP122.canReuse(s,s,1000,2000,true));
        assertFalse(CalendarSubscriptionCacheRP122.canReuse(s,s,1000,21601000,false));
    }
    @Test public void feedChangeInvalidatesCache() {
        Set<String> teams=Collections.singleton("NBA:13");
        assertNotEquals(CalendarSubscriptionCacheRP122.signature(teams,"https://a.example/events.json"),CalendarSubscriptionCacheRP122.signature(teams,"https://b.example/events.json"));
    }
    @Test public void followsMultipleLeaguesWithoutReplacingFirstTeam() {
        Set<String> subs=new HashSet<>(Arrays.asList("LIDOM:668","NBA:13","MLB:*"));
        assertTrue(CalendarPolicyRP120.follows(subs,"LIDOM","a",Arrays.asList("668","670")));
        assertTrue(CalendarPolicyRP120.follows(subs,"NBA","b",Arrays.asList("13","9")));
        assertTrue(CalendarPolicyRP120.follows(subs,"MLB","c",Collections.singletonList("147")));
        assertFalse(CalendarPolicyRP120.follows(subs,"NBA","d",Arrays.asList("1","2")));
    }
}
