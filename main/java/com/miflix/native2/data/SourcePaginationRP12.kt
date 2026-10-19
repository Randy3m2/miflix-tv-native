package com.miflix.native2.data

import com.miflix.native2.model.StreamChoice

internal object SourcePaginationRP12 {
    const val DEFAULT_SIZE=7
    fun name(row:StreamChoice)=row.addonName.ifBlank {"Add-on"}
    fun key(row:StreamChoice)=name(row)+"|"+row.url
    fun visible(rows:List<StreamChoice>,sizes:Map<String,Int>,pages:Map<String,Int>,playingUrl:String?=null):List<StreamChoice> {
        val counts=mutableMapOf<String,Int>()
        return rows.filter {row ->
            val name=name(row)
            val count=(counts[name] ?: 0)+1;counts[name]=count
            val size=sizes[name] ?: DEFAULT_SIZE
            val limit=size.toLong()*(pages[name] ?: 1).coerceAtLeast(1)
            size<=0 || count<=limit || (playingUrl!=null && row.sourceUrl.ifBlank {row.url}==playingUrl)
        }
    }
}
