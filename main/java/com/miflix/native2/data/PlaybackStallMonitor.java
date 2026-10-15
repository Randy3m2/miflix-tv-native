package com.miflix.native2.data;

/** Uses elapsed/monotonic time. Pauses, ended playback and suppressed playback reset the timer. */
public final class PlaybackStallMonitor {
    private long lastProgressAt = -1;
    private long lastPosition = -1;
    public boolean stalled(long now, long position, boolean expectedToAdvance, boolean buffering) {
        if (!expectedToAdvance || lastProgressAt < 0 || position != lastPosition) {
            lastProgressAt = now;
            lastPosition = position;
            return false;
        }
        return now - lastProgressAt >= (buffering ? 60000L : 30000L);
    }
}
