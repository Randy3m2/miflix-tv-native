package com.miflix.native2.data;

/** Plain event state avoids composing the UI again on each held key repeat. */
public final class VerticalRepeatGate {
    private int direction=0;
    private long lastAccepted=Long.MIN_VALUE;
    private long heldSince=0;
    public boolean consume(int code,boolean down,boolean up,int repeat,long time,boolean scrolling) {
        if(code!=19 && code!=20) return false;
        if(up) {reset();return false;}
        if(!down) return false;
        if(repeat==0 || direction!=code) {direction=code;lastAccepted=time;heldSince=time;return false;}
        long interval=HoldMotionPolicy.interval(true,scrolling,Math.max(0,time-heldSince));
        if(time>=lastAccepted && time-lastAccepted<interval) return true;
        lastAccepted=time;
        return false;
    }
    public void reset() {direction=0;lastAccepted=Long.MIN_VALUE;heldSince=0;}
}
