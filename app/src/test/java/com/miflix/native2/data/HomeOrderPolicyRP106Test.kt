package com.miflix.native2.data

import org.junit.Assert.*
import org.junit.Test

class HomeOrderPolicyRP106Test {
    @Test fun emptyPreferencesPutContinueFirst() {assertEquals(listOf("continue"),HomeOrderPolicyRP106.order(emptyList()))}
    @Test fun oldOrderKeepsOtherRowsInPlace() {
        assertEquals(listOf("continue","streaming","novelas","movies"),HomeOrderPolicyRP106.order(listOf("streaming","novelas","continue","movies")))
    }
    @Test fun duplicatesDoNotCreateDuplicateLazyKeys() {assertEquals(listOf("continue","movies"),HomeOrderPolicyRP106.order(listOf("continue","movies","continue","movies")))}
}
