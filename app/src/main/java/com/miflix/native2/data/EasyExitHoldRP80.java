package com.miflix.native2.data;

/** Release-to-confirm gesture. Repeat key-downs never restart or complete the timer. */
public final class EasyExitHoldRP80 {
    public static final long HOLD_MS = 60_000L;
    private int key = -1;
    private long startedAt;

    public boolean down(int code, long now, int repeatCount) {
        if (code < 0 || key != -1 || repeatCount != 0) return false;
        key = code;
        startedAt = now;
        return true;
    }

    public boolean up(int code, long now) {
        boolean unlock = key != -1 && key == code && now >= startedAt && now - startedAt >= HOLD_MS;
        cancel();
        return unlock;
    }

    public long secondsLeft(long now) {
        if (key == -1) return 60L;
        long elapsed = Math.max(0L, now - startedAt);
        return Math.max(0L, (HOLD_MS - elapsed + 999L) / 1000L);
    }

    public void cancel() { key = -1; startedAt = 0L; }
}
