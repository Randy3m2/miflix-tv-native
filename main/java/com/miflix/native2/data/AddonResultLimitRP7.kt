package com.miflix.native2.data

import com.miflix.native2.model.StreamChoice

internal object AddonResultLimitRP7 {
    fun apply(rows:List<StreamChoice>,limits:Map<String,Int>):List<StreamChoice> {
        val counts=mutableMapOf<String,Int>()
        return rows.filter {row ->
            val count=(counts[row.addonName] ?: 0)+1;counts[row.addonName]=count
            val limit=limits[row.addonName] ?: 0
            limit<=0 || count<=limit
        }
    }
}
