package com.miflix.native2.data;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/** At most two extra index searches for Spanish; the ordinary English fallback remains. */
public final class NativeSearchPlan {
    private NativeSearchPlan() {}
    public static List<String> queries(List<String> titles, String type, String year, int season, boolean spanish) {
        LinkedHashSet<String> queries = new LinkedHashSet<>();
        List<String> clean = new ArrayList<>();
        for (String title : titles) {
            if (title != null && !title.trim().isEmpty() && !clean.contains(title.trim())) clean.add(title.trim());
            if (clean.size() == 2) break;
        }
        String suffix = "series".equals(type) ? String.format(Locale.ROOT, " S%02d", season)
            : " " + (year == null ? "" : year.substring(0, Math.min(4, year.length())));
        for (String title : clean) queries.add(title + suffix);
        if (spanish && !clean.isEmpty()) {
            String title = clean.get(clean.size() - 1);
            queries.add(title + suffix + " latino");
            queries.add(title + suffix + " castellano");
        }
        return new ArrayList<>(queries);
    }
}
