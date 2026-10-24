package com.miflix.native2.data;

/** Mirrors the primary Play/Continue action in normal-mode details. */
public final class DetailsPrefetchRP90 {
    private DetailsPrefetchRP90() {}
    public static final long DELAY_MS=2000L;
    public static int[] target(String type,int season,int episode,double percent) {
        if(!"series".equals(type)) return new int[]{0,0};
        if(season>0 && episode>0 && percent<96.0) return new int[]{season,episode};
        return new int[]{1,1};
    }
}
