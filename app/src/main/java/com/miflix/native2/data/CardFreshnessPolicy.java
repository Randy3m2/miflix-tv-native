package com.miflix.native2.data;

import java.text.SimpleDateFormat;
import java.text.ParsePosition;
import java.util.Locale;
import java.util.TimeZone;

/** Fixed windows, no timer, no network. Dates use the Dominican catalog day. */
public final class CardFreshnessPolicy {
    private CardFreshnessPolicy() {}
    public static boolean justAdded(long addedAt,long now) {
        return addedAt>0 && now>=addedAt && now-addedAt<14L*86400000L;
    }
    public static boolean recentRelease(String date,long now) {return recentDate(date,now,14);}
    public static boolean newEpisode(String date,long now) {return recentDate(date,now,14);}
    private static boolean recentDate(String date,long now,int windowDays) {
        if(date==null || !date.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) return false;
        SimpleDateFormat format=new SimpleDateFormat("yyyy-MM-dd",Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("America/Santo_Domingo"));format.setLenient(false);
        ParsePosition position=new ParsePosition(0);
        java.util.Date parsed=format.parse(date,position);
        if(parsed==null || position.getIndex()!=date.length()) return false;
        java.util.Date today=format.parse(format.format(new java.util.Date(now)),new ParsePosition(0));
        long days=(today.getTime()-parsed.getTime())/86400000L;
        return days>=0 && days<windowDays;
    }
}
