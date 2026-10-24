package com.miflix.native2.data;

/** Warm-up is one paused player, bounded in bytes/time and never a watch-progress event. */
public final class WarmVideoPolicyRP91 {
    private WarmVideoPolicyRP91() {}
    public static final long BUFFER_US=5_000_000L;
    public static final int BUFFER_BYTES=16*1024*1024;
    public static final long TTL_MS=60_000L;
    public static boolean continueWarm(long bufferedUs,int allocatedBytes) {
        return bufferedUs<BUFFER_US && allocatedBytes<BUFFER_BYTES;
    }
    public static boolean fresh(long ageMs,long preparedPosition,long requestedPosition) {
        return ageMs>=0 && ageMs<TTL_MS && preparedPosition==requestedPosition;
    }
}
