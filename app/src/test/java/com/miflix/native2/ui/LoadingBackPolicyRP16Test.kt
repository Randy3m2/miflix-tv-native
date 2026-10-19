package com.miflix.native2.ui
import org.junit.Assert.*
import org.junit.Test
class LoadingBackPolicyRP16Test {
    @Test fun firstBackOnlyArmsExit() {assertFalse(LoadingBackPolicyRP16.exits(-1,100))}
    @Test fun secondBackWithinWindowExits() {assertTrue(LoadingBackPolicyRP16.exits(100,900));assertTrue(LoadingBackPolicyRP16.exits(100,2300))}
    @Test fun expiredOrReversedClockDoesNotExit() {assertFalse(LoadingBackPolicyRP16.exits(100,2301));assertFalse(LoadingBackPolicyRP16.exits(100,99))}
}
