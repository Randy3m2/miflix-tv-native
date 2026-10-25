package com.miflix.native2.data;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
/** ESPN NBA accepts YYYYMM, but rejects the YYYYMMDD-YYYYMMDD range form. */
public final class NbaCalendarRequestsRP123 {
    private NbaCalendarRequestsRP123() {}
    public static List<String> months(LocalDate start,LocalDate end) {
        if(end.isBefore(start)) throw new IllegalArgumentException("Invalid date range");
        List<String> urls=new ArrayList<>();
        YearMonth last=YearMonth.from(end);
        for(YearMonth month=YearMonth.from(start);!month.isAfter(last);month=month.plusMonths(1))
            urls.add("https://site.api.espn.com/apis/site/v2/sports/basketball/nba/scoreboard?dates="+
                month.format(DateTimeFormatter.ofPattern("yyyyMM"))+"&limit=1000");
        return urls;
    }
}
