package com.miflix.native2.data;

/** Coalesces held horizontal repeats; fresh presses and key releases stay immediate. */
public final class HorizontalRepeatGate {
    private int direction=0;
    private long lastAccepted=Long.MIN_VALUE;
    private long heldSince=0;
    public boolean consume(int code,boolean down,boolean up,int repeat,long time) {
        return consume(code,down,up,repeat,time,false);
    }
    public boolean consume(int code,boolean down,boolean up,int repeat,long time,boolean scrolling) {
        if(code!=21 && code!=22) return false;
        if(up) {reset();return false;}
        if(!down) return false;
        if(repeat==0 || direction!=code) {direction=code;lastAccepted=time;heldSince=time;return false;}
        // Retarget an in-flight scroll at a bounded cadence instead of dropping every repeat.
        long interval=HoldMotionPolicy.interval(false,scrolling,Math.max(0,time-heldSince));
        if(time>=lastAccepted && time-lastAccepted<interval) return true;
        lastAccepted=time;
        return false;
    }
    public void reset() {direction=0;lastAccepted=Long.MIN_VALUE;heldSince=0;}
}
