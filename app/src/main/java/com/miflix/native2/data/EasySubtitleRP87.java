package com.miflix.native2.data;

/** Easy subtitle defaults and normal-mode series preferences stay independent. */
public final class EasySubtitleRP87 {
    private EasySubtitleRP87() {}
    public static String profileLanguage(boolean easy, String normal, String easySaved) {
        String value=easy ? easySaved : normal;
        return value==null || value.trim().isEmpty() ? "es" : value;
    }
    public static String seriesLanguage(boolean easy, String preference, String savedLanguage, boolean savedOff) {
        if(easy) return "off".equals(preference) ? null : preference;
        if(savedOff) return null;
        if(savedLanguage!=null && !savedLanguage.trim().isEmpty()) return savedLanguage;
        return "off".equals(preference) ? null : preference;
    }
}
