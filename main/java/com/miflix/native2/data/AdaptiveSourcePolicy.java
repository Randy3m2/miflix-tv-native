package com.miflix.native2.data;
public final class AdaptiveSourcePolicy {
    private AdaptiveSourcePolicy() {}
    public static long rate(long bytes,int seconds,int quality) {
        if(bytes>0 && seconds>0) return (long)(bytes*8.0/seconds);
        return quality>=2160?25000000L:quality>=1080?8000000L:quality>=720?4000000L:2000000L;
    }
    public static boolean fits(long rate,long network) {return network<=0 || rate<=network*.65;}
}
