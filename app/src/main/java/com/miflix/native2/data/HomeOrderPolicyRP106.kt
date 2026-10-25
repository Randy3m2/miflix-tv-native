package com.miflix.native2.data

internal object HomeOrderPolicyRP106 {
    fun order(saved:List<String>):List<String> = (listOf("continue")+saved).distinct()
}
