package com.miflix.native2.data

import org.junit.Assert.*
import org.junit.Test

class StatsViewportRP106Test {
    @Test fun movementInsideViewportDoesNotScroll() {
        for(row in 0..6) assertEquals(0,StatsViewportRP106.firstFor(row,0,6))
        for(row in 20..26) assertEquals(20,StatsViewportRP106.firstFor(row,20,26))
    }
    @Test fun steppingBelowViewportScrollsOnlyOneRow() {
        assertEquals(1,StatsViewportRP106.firstFor(7,0,6))
        assertEquals(21,StatsViewportRP106.firstFor(27,20,26))
    }
    @Test fun steppingAboveViewportScrollsOnlyOneRow() {
        assertEquals(19,StatsViewportRP106.firstFor(19,20,26))
        assertEquals(0,StatsViewportRP106.firstFor(0,1,7))
    }
    @Test fun navigationDownAndBackUpDoesNotJumpToEachSelectedRow() {
        var first=0
        for(row in 0..99) {
            first=StatsViewportRP106.firstFor(row,first,first+6)
            assertTrue(row in first..first+6)
            if(row>6) assertEquals(row-6,first)
        }
        for(row in 99 downTo 0) {
            first=StatsViewportRP106.firstFor(row,first,first+6)
            assertTrue(row in first..first+6)
        }
        assertEquals(0,first)
    }
    @Test fun jumpsAndInvalidNegativeValuesStayBounded() {
        assertEquals(94,StatsViewportRP106.firstFor(100,0,6))
        assertEquals(0,StatsViewportRP106.firstFor(-1,5,11))
    }
}
