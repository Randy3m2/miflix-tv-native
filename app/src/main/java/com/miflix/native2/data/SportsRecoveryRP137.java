package com.miflix.native2.data;

import java.util.*;

/** An outage is bounded to two batches of five links, with one fresh resolution. */
public final class SportsRecoveryRP137 {
    public static final int MAX_LINKS=5;
    public static final long STALL_MS=20000L, HEALTHY_MS=30000L;
    private long waitingSince=-1,healthySince=-1,lastPosition=-1;
    public static int next(List<String> urls,int current,Set<String> failed) {
        int size=Math.min(urls.size(),MAX_LINKS);
        if(size==0)return -1;
        for(int step=1;step<=size;step++) {
            int index=Math.floorMod(current+step,size);
            if(!failed.contains(urls.get(index)))return index;
        }
        return -1;
    }
    /** Suspended UI, deliberate pause and source menus never spend a stall budget. */
    public boolean stalled(long now,long position,boolean waiting,boolean eligible) {
        if(!eligible) {waitingSince=-1;healthySince=-1;lastPosition=position;return false;}
        boolean progressed=lastPosition>=0 && position>lastPosition;
        lastPosition=position;
        if(!waiting) {waitingSince=-1;return false;}
        healthySince=-1;
        if(progressed || waitingSince<0)waitingSince=now;
        return now-waitingSince>=STALL_MS;
    }
    public boolean healthy(long now,boolean playing,boolean eligible) {
        if(!eligible || !playing) {healthySince=-1;return false;}
        if(healthySince<0)healthySince=now;
        return now-healthySince>=HEALTHY_MS;
    }
}
