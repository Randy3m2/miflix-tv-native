package com.miflix.native2.data;

/** A steady leading anchor keeps rows moving continuously instead of only at the right edge. */
public final class BrowseFocusMotion {
    private BrowseFocusMotion() {}
    public static float horizontalDistance(float offset,float size,float container) {
        if(container<=0 || size<=0) return 0f;
        if(size>=container) return offset;
        float anchor=Math.min(container*.20f,container-size);
        return offset-anchor;
    }
}
