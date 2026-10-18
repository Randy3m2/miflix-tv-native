package com.miflix.native2.data;

/** Coalesces held horizontal repeats; fresh presses and key releases stay immediate. */
public final class HorizontalRepeatGate {
    private int direction=0;
    private long lastAccepted=Long.MIN_VALUE;
    public boolean consume(int code,boolean down,boolean up,int repeat,long time) {
        return consume(code,down,up,repeat,time,false);
    }
    public boolean consume(int code,boolean down,boolean up,int repeat,long time,boolean scrolling) {
        if(code!=21 && code!=22) return false;
        if(up) {direction=0;return false;}
        if(!down) return false;
        if(repeat==0 || direction!=code) {direction=code;lastAccepted=time;return false;}
        // Retarget an in-flight scroll at a bounded cadence instead of dropping every repeat.
        long interval=scrolling ? 180L : 140L;
        if(time>=lastAccepted && time-lastAccepted<interval) return true;
        lastAccepted=time;
        return false;
    }
    public void reset() {direction=0;lastAccepted=Long.MIN_VALUE;}
}
