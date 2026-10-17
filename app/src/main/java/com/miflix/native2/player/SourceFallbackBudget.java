package com.miflix.native2.player;

/** Counts the original source as attempt one. No retry can reset the budget. */
public final class SourceFallbackBudget {
    private final int limit;
    private int next=1;
    public SourceFallbackBudget(int candidateCount) { limit=Math.min(5,Math.max(0,candidateCount)); }
    public int nextIndex() { return next<limit ? next++ : -1; }
    public static boolean sourceFailure(int errorCode) {
        // Media3: I/O=2xxx, parser=3xxx, decoder=4xxx. AudioTrack/DRM errors are separate.
        return errorCode>=2000 && errorCode<5000;
    }
}
