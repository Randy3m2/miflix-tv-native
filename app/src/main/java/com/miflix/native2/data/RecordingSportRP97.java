package com.miflix.native2.data;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/** Local classification only; unknown recordings remain accessible in Other / All. */
public final class RecordingSportRP97 {
    private RecordingSportRP97() {}
    private static final String[][] SPORTS={
        {"basketball","nba|wnba|ncaa|basketball|baloncesto|lakers|celtics|knicks|warriors|cavaliers|76ers|mavericks|nuggets|pacers|clippers|grizzlies|timberwolves|pelicans|trail blazers"},
        {"baseball","mlb|lidom|baseball|beisbol|yankees|dodgers|mets|red sox|white sox|astros|padres|phillies|brewers|orioles|blue jays|mariners|cubs|diamondbacks"},
        {"football","nfl|american football|futbol americano|chiefs|49ers|steelers|packers|patriots|cowboys|ravens|bengals|seahawks|buccaneers"},
        {"soccer","soccer|football|futbol|uefa|fifa|premier league|la liga|laliga|champions league|real madrid|barcelona|chelsea|arsenal|liverpool|manchester"},
        {"f1","f1|formula 1|formula one|formula uno|motogp|nascar|indycar|motorsport"},
        {"tennis","tennis|tenis|atp|wta|wimbledon|roland garros"},
        {"hockey","nhl|hockey|stanley cup"},
        {"combat","ufc|mma|boxing|boxeo|wwe|wrestling"}
    };
    private static final Pattern[] PATTERNS=new Pattern[SPORTS.length];
    static {for(int i=0;i<SPORTS.length;i++) PATTERNS[i]=Pattern.compile("\\b(?:"+SPORTS[i][1]+")\\b");}
    public static String group(String... fields) {
        StringBuilder joined=new StringBuilder();for(String field:fields) if(field!=null) joined.append(' ').append(field);
        String text=Normalizer.normalize(joined,Normalizer.Form.NFD).replaceAll("\\p{M}+","")
            .toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+"," ");
        for(int i=0;i<SPORTS.length;i++) if(PATTERNS[i].matcher(text).find()) return SPORTS[i][0];
        return "other";
    }
}
