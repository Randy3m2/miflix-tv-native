package com.miflix.native2.player;

/** Prevents the end-position seek from recursively delivering the same completion. */
public final class MomentBoundaryGate {
    private final long endMs;
    private boolean delivered;
    public MomentBoundaryGate(long endMs) {
        if (endMs <= 0) throw new IllegalArgumentException("Positive end required");
        this.endMs=endMs;
    }
    public boolean claimEnd() {
        if (delivered) return false;
        delivered=true;
        return true;
    }
    public void movedTo(long positionMs) {
        if (positionMs < endMs) delivered=false;
    }
}
