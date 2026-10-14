package com.miflix.native2.data;

/** Ignore only the tail of the long press that opened a focusable reaction popup. */
public final class QuickReactionGestureGate {
    private boolean opening = true;
    public boolean consume(int keyCode, boolean down, boolean up, int repeatCount) {
        boolean activate = keyCode == 23 || keyCode == 66 || keyCode == 160;
        if (!activate || !opening) return false;
        if (down && repeatCount == 0) {
            opening = false;
            return false;
        }
        if (up) opening = false;
        return true;
    }
}
