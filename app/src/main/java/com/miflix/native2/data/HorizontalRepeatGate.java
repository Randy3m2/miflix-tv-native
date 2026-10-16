package com.miflix.native2.data;

/** Coalesces held horizontal repeats; fresh presses and key releases stay immediate. */
public final class HorizontalRepeatGate {
    private int direction=0;
    private long lastAccepted=Long.MIN_VALUE;
    public boolean consume(int code,boolean down,boolean up,int repeat,long time) {
        if(code!=21 && code!=22) return false;
        if(up) {direction=0;return false;}
        if(!down) return false;
        if(repeat==0 || direction!=code) {direction=code;lastAccepted=time;return false;}
        if(time>=lastAccepted && time-lastAccepted<140L) return true;
        lastAccepted=time;
        return false;
    }
    public void reset() {direction=0;lastAccepted=Long.MIN_VALUE;}
}
