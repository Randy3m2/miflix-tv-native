package com.miflix.native2.data;

/** Bounded acceleration and non-overshooting momentum transfer, independent of rendering. */
public final class HoldMotionPolicy {
    private HoldMotionPolicy() {}
    public static long interval(boolean vertical,boolean scrolling,long heldMs) {
        if(vertical) return heldMs>=1800 ? (scrolling?170:150) : heldMs>=900 ? (scrolling?210:180) : (scrolling?240:200);
        return heldMs>=1800 ? (scrolling?120:100) : heldMs>=900 ? (scrolling?150:120) : (scrolling?180:140);
    }
    public static float carriedVelocity(float distance,float velocity) {
        if(Float.isNaN(distance) || Float.isInfinite(distance) || Float.isNaN(velocity) || Float.isInfinite(velocity) || distance*velocity<=0) return 0f;
        float limit=Math.min(7000f,Math.abs(distance)*8f);
        return Math.max(-limit,Math.min(limit,velocity));
    }
}
