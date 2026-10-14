package com.miflix.native2.data;

/** Local calendar policy. No backend job or per-navigation network request. */
public final class SeasonalProgramming {
    private SeasonalProgramming() {}
    public static String theme(int month, int day) {
        if (month < 1 || month > 12 || day < 1 || day > 31) throw new IllegalArgumentException("Invalid calendar date");
        if (month == 10) return "halloween";
        if (month == 12 || (month == 11 && day >= 20) || (month == 1 && day <= 6)) return "christmas";
        if (month == 2) return "romance";
        if (month >= 6 && month <= 8) return "adventure";
        return "discovery";
    }
    public static int genre(String theme) {
        switch (theme) {
            case "halloween": return 27;
            case "christmas": return 10751;
            case "romance": return 10749;
            case "adventure": return 12;
            default: return 18;
        }
    }
}
