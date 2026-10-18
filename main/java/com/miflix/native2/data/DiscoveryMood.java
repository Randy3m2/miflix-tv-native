package com.miflix.native2.data;

import java.text.Normalizer;
import java.util.Locale;

/** Local, bilingual intent rules. Never treats a genre alone as a sentence request. */
public final class DiscoveryMood {
    private DiscoveryMood() {}
    public static String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9 ]", " ").replaceAll(" +", " ").trim();
    }
    public static String intent(String query) {
        String q = normalize(query);
        boolean request = q.contains("algo ") || q.startsWith("something ") || q.contains("quiero ver ")
            || q.startsWith("a thriller ") || q.startsWith("un thriller ");
        if (!request) return "";
        if (q.contains("mama") || q.contains("mom") || q.contains("familia") || q.contains("family")) return "family";
        if (q.contains("final inesperado") || q.contains("unexpected ending") || q.contains("plot twist")) return "twist";
        if (q.contains("30 min") || q.contains("rapido") || q.contains("quick") || q.contains("corto")) return "quick";
        if (q.contains("maraton") || q.contains("marathon") || q.contains("binge")) return "marathon";
        if (q.contains("sentirme bien") || q.contains("feel good") || q.contains("dia pesado") || q.contains("hard day")) return "comfort";
        if (q.contains("sentido") || q.contains("meaningful")) return "meaningful";
        if (q.contains("fondo") || q.contains("background")) return "background";
        return "";
    }
    public static String context(int hour, int month, int day, boolean weekend) {
        if (month == 12 || (month == 11 && day >= 20)) return "christmas";
        if (month == 10) return "halloween";
        if (hour >= 22 || hour < 5) return "background";
        if (weekend && hour >= 18) return "marathon";
        if (hour < 12) return "quick";
        return "comfort";
    }
}
