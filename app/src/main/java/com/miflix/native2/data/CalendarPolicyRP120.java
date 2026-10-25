package com.miflix.native2.data;
import java.util.*;
/** Shared pure policy for app, receivers and regression tests. */
public final class CalendarPolicyRP120 {
    private CalendarPolicyRP120() {}
    public static boolean follows(Set<String> subscriptions,String league,String id,List<String> teams) {
        if(subscriptions.contains("EVENT:"+league+":"+id) || subscriptions.contains(league+":*")) return true;
        for(String team:teams) if(subscriptions.contains(league+":"+team)) return true;
        return false;
    }
    public static boolean due(long starts,long now,boolean timed,boolean cancelled,boolean sent) {
        return starts>0 && timed && !cancelled && !sent && starts<=now && starts>now-600000L;
    }
    public static boolean refresh(long last,long now,boolean force) {
        return force || last<=0 || now<last || now-last>=21600000L;
    }
    public static String noticeKey(String league,String id,long starts) {return league+":"+id+":"+starts;}
}
