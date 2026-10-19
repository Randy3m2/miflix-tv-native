package com.miflix.native2.data

import com.miflix.native2.model.StreamChoice
import org.junit.Assert.*
import org.junit.Test

class AddonResultLimitRP7Test {
    private val rows=(1..20).flatMap {n ->
        listOf(StreamChoice("A","https://test/a$n","Latino",addonName="A"),
            StreamChoice("B","https://test/b$n","English",addonName="B"))
    }
    @Test fun presetsAndCustomKeepEachProvidersBestOrderedRows() {
        for(n in listOf(5,10,15,7)) {
            val result=AddonResultLimitRP7.apply(rows,mapOf("A" to n))
            assertEquals(rows.filter {it.addonName=="A"}.take(n),result.filter {it.addonName=="A"})
            assertEquals(20,result.count {it.addonName=="B"})
        }
    }
    @Test fun unlimitedAndIndependentProviderLimits() {
        assertEquals(rows,AddonResultLimitRP7.apply(rows,mapOf("A" to 0)))
        val result=AddonResultLimitRP7.apply(rows,mapOf("A" to 5,"B" to 10))
        assertEquals(5,result.count {it.addonName=="A"})
        assertEquals(10,result.count {it.addonName=="B"})
    }
    @Test fun raisingOrRemovingLimitRestoresCachedRows() {
        assertEquals(5,AddonResultLimitRP7.apply(rows,mapOf("A" to 5)).count {it.addonName=="A"})
        assertEquals(15,AddonResultLimitRP7.apply(rows,mapOf("A" to 15)).count {it.addonName=="A"})
        assertEquals(rows,AddonResultLimitRP7.apply(rows,emptyMap()))
    }
}
