package com.miflix.native2.data;
/** One keyboard for English and Latin Spanish, with an automatic voice request when supported. */
public final class BilingualInputRP85 {
    private BilingualInputRP85() {}
    public static String keyboard() {return "abcdefghijklmnñopqrstuvwxyzáéíóúü1234567890";}
    public static String[] voiceLanguages() {return new String[]{"es-MX","en-US"};}
    public static String fallbackLocale(String audio,String ui) {
        String code=audio==null?"":audio.toLowerCase(java.util.Locale.ROOT);
        if(code.equals("es") || code.startsWith("es-") || code.equals("spa")) return "es-MX";
        if(code.equals("en") || code.startsWith("en-") || code.equals("eng")) return "en-US";
        return "es".equals(ui)?"es-MX":"en-US";
    }
    public static boolean accepts(int point) {
        return point>=32 && point<=126 || "ñÑáÁéÉíÍóÓúÚüÜ".indexOf(point)>=0;
    }
}
