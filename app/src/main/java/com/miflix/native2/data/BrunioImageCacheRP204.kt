package com.miflix.native2.data

import android.content.Context
import android.app.ActivityManager
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.imageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.request.ImageRequest
import java.util.LinkedHashMap
import okio.Path.Companion.toPath

/**
 * RP204: one Coil loader for TV and Mobile. Limits RAM on smaller TV boxes;
 * a shared disk cache avoids fetching the same posters during navigation.
 */
object BrunioImageCacheRP204 {
    fun install(context:Context) {
        SingletonImageLoader.setSafe { app ->
            val lowRam=(app.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)?.isLowRamDevice==true
            ImageLoader.Builder(app)
                .memoryCache {
                    MemoryCache.Builder()
                        .maxSizePercent(app,if(lowRam) 0.10 else 0.18)
                        .build()
                }
                .diskCache {
                    DiskCache.Builder()
                        .directory(app.cacheDir.resolve("brunio_images").absolutePath.toPath())
                        .maxSizeBytes(if(lowRam) 70L*1024*1024 else 150L*1024*1024)
                        .build()
                }
                .build()
        }
    }

    // Constrain preloads: at most the three adjacent posters, with a short
    // same-process suppression window. No cloud logs or Supabase calls.
    private val recent=LinkedHashMap<String,Long>(48,0.75f,true)
    @Synchronized private fun shouldFetch(url:String,now:Long):Boolean {
        val before=recent[url]
        if(before!=null && now-before<120_000L) return false
        recent[url]=now
        while(recent.size>48) recent.remove(recent.keys.first())
        return true
    }
    fun prefetch(context:Context,urls:List<String>) {
        val now=System.currentTimeMillis()
        val loader=context.applicationContext.imageLoader
        urls.distinct().filter {it.startsWith("https://")}.take(3).forEach {url->
            if(shouldFetch(url,now)) {
                loader.enqueue(ImageRequest.Builder(context.applicationContext)
                    .data(url).size(240,360).build())
            }
        }
    }
}
