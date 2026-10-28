package com.brunio.desktop
import androidx.compose.ui.graphics.ImageBitmap

/** Bounded in-memory cache; no URLs or access tokens are written to disk. */
object ArtworkCache {
    private val entries=LinkedHashMap<String,ImageBitmap>(32,.75f,true)
    private var bytes=0L
    private const val LIMIT=96L*1024*1024
    @Synchronized fun get(key:String):ImageBitmap?=entries[key]
    @Synchronized fun put(key:String,value:ImageBitmap) {
        val size=value.width.toLong()*value.height*4
        if(size>LIMIT)return
        entries.remove(key)?.let{bytes-=it.width.toLong()*it.height*4}
        entries[key]=value;bytes+=size
        val cursor=entries.entries.iterator()
        while(bytes>LIMIT && cursor.hasNext()){val old=cursor.next().value;bytes-=old.width.toLong()*old.height*4;cursor.remove()}
    }
    @Synchronized fun clear(){entries.clear();bytes=0}
}
