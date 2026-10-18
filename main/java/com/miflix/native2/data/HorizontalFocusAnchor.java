package com.miflix.native2.data;

/** Prevent transient width changes from panning against the user's latest arrow press. */
public final class HorizontalFocusAnchor {
    private int direction;
    private long pressedAt;
    public void pressed(int code,long time) {
        if(code==21 || code==22) {direction=code==22?1:-1;pressedAt=time;}
    }
    public void reset() {direction=0;pressedAt=0;}
    public float distance(float offset,float size,float container,long now) {
        float wanted=BrowseFocusMotion.horizontalDistance(offset,size,container);
        if(direction!=0 && now>=pressedAt && now-pressedAt<=350 && wanted*direction<0) {
            if(offset<0) return offset;
            if(offset+size>container) return offset+size-container;
            return 0;
        }
        return wanted;
    }
}
