package com.miflix.native2.data;

/** One bounded pass through warm candidates; never reset the budget on a source error. */
public final class WarmRetryPolicyRP92 {
    private WarmRetryPolicyRP92() {}
    public static final int MAX_ATTEMPTS=5;
    public static int limit(int candidates,boolean autoFallback) {
        return Math.min(Math.max(0,candidates),autoFallback ? MAX_ATTEMPTS : 1);
    }
}
