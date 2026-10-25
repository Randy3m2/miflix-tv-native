package com.miflix.native2.data;
public final class HomeStartupPolicyRP129 {
    private HomeStartupPolicyRP129() {}
    public static boolean reserveContinue(boolean ready,boolean hasItems,boolean metadataConfigured) {
        return hasItems || (!ready && metadataConfigured);
    }
    public static boolean revealHeader(float cardTop,float focusTop,float viewportTop,float margin,float firstActionsLimit) {
        return cardTop<viewportTop+margin-1 && focusTop-cardTop<=firstActionsLimit;
    }
}
