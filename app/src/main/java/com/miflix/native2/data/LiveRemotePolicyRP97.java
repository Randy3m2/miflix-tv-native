package com.miflix.native2.data;

/** Key codes are Android DPAD_LEFT=21 and DPAD_RIGHT=22; no Android runtime needed for tests. */
public final class LiveRemotePolicyRP97 {
    private LiveRemotePolicyRP97() {}
    public static boolean revealControls(boolean live, boolean recording, int key, boolean controls, boolean surface, boolean blocked) {
        return live && !recording && !blocked && (key==21 || key==22) && (!controls || surface);
    }
}
