package com.miflix.native2.data;

/** Deterministic controls and subtitle selection, independent of the Android player. */
public final class EasyPlaybackRP89 {
    private EasyPlaybackRP89() {}
    public static int subtitleRank(String language,String label,boolean forced) {
        int rank=SpanishAudioRP83.rank(language,label);
        return rank<0 ? -1 : rank+(forced ? 10 : 0);
    }
    public static long seekTarget(long position,long duration,int direction) {
        if(duration<=0) return 0;
        long current=Math.max(0,Math.min(position,duration));
        if(direction<0) return current<10000 ? 0 : current-10000;
        return duration-current<10000 ? duration : current+10000;
    }
}
