package com.miflix.native2.data;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/** At most four queries: original/translated Latino searches first when playback audio is Spanish. */
public final class NativeSearchPlan {
    private NativeSearchPlan() {}
    public static List<String> queries(List<String> titles,String type,String year,int season,String preferredAudio) {
        if(!StreamLanguagePriority.prefersKorean(preferredAudio)) return queries(titles,type,year,season,StreamLanguagePriority.prefersSpanish(preferredAudio));
        List<String> ordinary=queries(titles,type,year,season,false);
        LinkedHashSet<String> korean=new LinkedHashSet<>();
        for(String query:ordinary) korean.add(query+" korean");
        korean.addAll(ordinary);
        return new ArrayList<>(korean);
    }
    public static List<String> queries(List<String> titles, String type, String year, int season, boolean spanish) {
        LinkedHashSet<String> queries = new LinkedHashSet<>();
        List<String> clean = new ArrayList<>();
        for (String title : titles) {
            if (title != null && !title.trim().isEmpty() && !clean.contains(title.trim())) clean.add(title.trim());
            if (clean.size() == 2) break;
        }
        String suffix = "series".equals(type) ? String.format(Locale.ROOT, " S%02d", season)
            : " " + (year == null ? "" : year.substring(0, Math.min(4, year.length())));
        if (spanish) for (String title : clean) queries.add(title + suffix + " latino");
        // Generic title searches still discover Spain/unspecified Spanish releases and file-level labels.
        for (String title : clean) queries.add(title + suffix);
        return new ArrayList<>(queries);
    }
}
