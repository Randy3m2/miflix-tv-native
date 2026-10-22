package com.miflix.native2.data;

/** Native VOD only. Language evidence remains label-based, not a probe of audio tracks. */
public final class NativeVodPolicyRP77 {
    private NativeVodPolicyRP77() {}
    public static boolean nativeEntry(boolean pending,String url,String sourceUrl) {
        return pending || (url!=null && url.startsWith("brunio-native://")) ||
            (sourceUrl!=null && sourceUrl.startsWith("brunio-native://"));
    }
    public static boolean accepts(String preferred,boolean spanishOnly,String declared,String labels) {
        return !spanishOnly || !StreamLanguagePriority.prefersSpanish(preferred) ||
            StreamLanguagePriority.hasAudio(declared,labels,"es");
    }
    /** Used as a descending sort key: unknown sizes stay last in ascending mode. */
    public static long sizeKey(long bytes,boolean ascending) {
        return ascending ? (bytes>0 ? -bytes : Long.MIN_VALUE) : bytes;
    }
}
