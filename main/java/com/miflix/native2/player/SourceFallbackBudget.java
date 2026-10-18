package com.miflix.native2.player;

/** Counts the original source as attempt one. No retry can reset the budget. */
public final class SourceFallbackBudget {
    public static final int MAX_ATTEMPTS=10;
    private final int limit;
    private int next=1;
    public SourceFallbackBudget(int candidateCount) { this(candidateCount,false); }
    public SourceFallbackBudget(int candidateCount,boolean exhaustCandidates) { limit=exhaustCandidates ? Math.max(0,candidateCount) : Math.min(MAX_ATTEMPTS,Math.max(0,candidateCount)); }
    public int nextIndex() { return next<limit ? next++ : -1; }
    public static boolean sourceFailure(int errorCode) {
        // General/source/timeouts, I/O=2xxx, parser=3xxx, decoder=4xxx.
        // Exclude failed internal runtime checks (1004), AudioTrack and DRM.
        return errorCode>=1000 && errorCode<5000 && errorCode!=1004;
    }
}
