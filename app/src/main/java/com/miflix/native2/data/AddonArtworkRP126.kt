package com.miflix.native2.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

internal object AddonArtworkRP126 {
    private data class Entry(val value:AddonManifestRP126,val at:Long)
    private val cache=object:LinkedHashMap<String,Entry>(48,.75f,true) {
        override fun removeEldestEntry(eldest:MutableMap.MutableEntry<String,Entry>?)=size>48
    }
    private val slots=Semaphore(3)
    suspend fun load(url:String):AddonManifestRP126=withContext(Dispatchers.IO) {
        synchronized(cache) {cache[url]?.takeIf {System.currentTimeMillis()-it.at<86400000L}?.value}?.let {return@withContext it}
        slots.withPermit {
            val result=AddonManifestRP126.parse(url,AddonHttpRP12.text(url,8000))
            synchronized(cache) {cache[url]=Entry(result,System.currentTimeMillis())}
            result
        }
    }
}
