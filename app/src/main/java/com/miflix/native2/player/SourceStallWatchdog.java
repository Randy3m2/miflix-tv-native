package com.miflix.native2.player;

/** Local-only stall clock; pauses, background and source menus reset the deadline. */
public final class SourceStallWatchdog {
    private long since=-1;
    public boolean shouldAdvance(long now,boolean firstFrame,boolean buffering,boolean eligible) {
        if(!eligible || (firstFrame && !buffering)) {since=-1;return false;}
        if(since<0 || now<since) {since=now;return false;}
        long limit=firstFrame ? 20000L : 12000L;
        if(now-since<limit) return false;
        since=now;
        return true;
    }
}
