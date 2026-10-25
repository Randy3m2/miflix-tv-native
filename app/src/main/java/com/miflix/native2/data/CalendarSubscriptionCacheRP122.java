package com.miflix.native2.data;
import java.util.*;
/** Cache identity includes every followed team and the optional feed. */
public final class CalendarSubscriptionCacheRP122 {
    private CalendarSubscriptionCacheRP122() {}
    public static boolean canReuse(String saved,String current,long last,long now,boolean force) {
        return current.equals(saved) && !CalendarPolicyRP120.refresh(last,now,force);
    }
    public static String signature(Set<String> subscriptions,String feed) {
        List<String> sorted=new ArrayList<>(subscriptions);Collections.sort(sorted);
        StringBuilder out=new StringBuilder("nba-monthly-rp123|");
        for(String key:sorted) out.append(key.length()).append(':').append(key);
        return out.append('|').append(feed.length()).append(':').append(feed).toString();
    }
}
