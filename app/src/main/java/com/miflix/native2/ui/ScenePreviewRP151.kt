package com.miflix.native2.ui

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.miflix.native2.model.StreamChoice
import kotlinx.coroutines.*
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/** One native extractor at a time, even if a provider ignores coroutine cancellation. */
private object SceneFramesRP151 {
    private val cache=object:android.util.LruCache<String,Bitmap>(12*1024) {override fun sizeOf(key:String,value:Bitmap)=value.allocationByteCount/1024}
    private fun key(source:StreamChoice,time:Long)=source.url+source.requestHeaders.toSortedMap().toString()+"@"+time
    fun cached(source:StreamChoice,time:Long)=cache.get(key(source,time))
    val executor=Executors.newSingleThreadExecutor {r->Thread(r,"brunio-scene-preview").apply {isDaemon=true}}
    val busy=AtomicBoolean(false)
    private var sourceKey:String?=null
    private var reader:MediaMetadataRetriever?=null
    // Cache survives closing the scrub popup; the single reader is released after idle time.
    private val cleanup=Executors.newSingleThreadScheduledExecutor {r->Thread(r,"brunio-preview-idle").apply {isDaemon=true}}
    private var releaseTask:java.util.concurrent.ScheduledFuture<*>?=null
    @Synchronized fun active(){releaseTask?.cancel(false);releaseTask=null}
    @Synchronized fun release(){releaseTask?.cancel(false);releaseTask=cleanup.schedule({executor.execute {runCatching {reader?.release()};reader=null;sourceKey=null}},45,java.util.concurrent.TimeUnit.SECONDS)}
    suspend fun frame(source:StreamChoice,position:Long):Bitmap? {
        cached(source,position)?.let {return it}
        active()
        if(!source.url.startsWith("https://") && !source.url.startsWith("http://")) return null
        while(!busy.compareAndSet(false,true)) {delay(60);currentCoroutineContext().ensureActive()}
        return suspendCancellableCoroutine {continuation ->
            executor.execute {
                var retriever:MediaMetadataRetriever?=null
                var bitmap:Bitmap?=null
                try {
                    if(continuation.isActive) {
                        val key=source.url+source.requestHeaders.toSortedMap().toString()
                        if(sourceKey!=key || reader==null) {
                            runCatching {reader?.release()};reader=null;sourceKey=null
                            val next=MediaMetadataRetriever()
                            try {next.setDataSource(source.url,source.requestHeaders);reader=next;sourceKey=key} catch(e:Exception) {runCatching {next.release()};throw e}
                        }
                        retriever=reader!!
                        if(continuation.isActive) {
                            bitmap=if(android.os.Build.VERSION.SDK_INT>=27) retriever.getScaledFrameAtTime(position*1000L,MediaMetadataRetriever.OPTION_CLOSEST_SYNC,320,180)
                            else null // Older TVs retain the timestamp without allocating a full-resolution frame.
                        }
                    }
                } catch(_:Exception) {runCatching {reader?.release()};reader=null;sourceKey=null} finally {busy.set(false)}
                bitmap?.let {cache.put(key(source,position),it)}
                if(continuation.isActive) continuation.resumeWith(Result.success(bitmap))
            }
        }
    }
}

@Suppress("UNUSED_PARAMETER")
internal fun scenePreviewEnabledRP151(context:android.content.Context)=true

@Composable
internal fun ScenePreviewRP151(source:StreamChoice,position:Long,modifier:Modifier=Modifier) {
    val key=position/5_000L*5_000L
    var image by remember(source.url,source.requestHeaders) {mutableStateOf<Bitmap?>(SceneFramesRP151.cached(source,key))}
    var imageTime by remember(source.url,source.requestHeaders) {mutableLongStateOf(key)}
    var before by remember(source.url,source.requestHeaders) {mutableStateOf<Bitmap?>(null)}
    var after by remember(source.url,source.requestHeaders) {mutableStateOf<Bitmap?>(null)}
    var loading by remember {mutableStateOf(false)}
    DisposableEffect(source.url,source.requestHeaders) {SceneFramesRP151.active();onDispose {SceneFramesRP151.release()}}
    LaunchedEffect(source.url,source.requestHeaders,key) {
        loading=true
        before=SceneFramesRP151.cached(source,(key-5_000).coerceAtLeast(0));after=SceneFramesRP151.cached(source,key+5_000)
        val cached=SceneFramesRP151.cached(source,key)
        if(cached!=null) {image=cached;imageTime=key;loading=false}
        else {
            delay(180)
            val frame=withTimeoutOrNull(6500) {SceneFramesRP151.frame(source,key)}
            ensureActive()
            if(frame!=null) {image=frame;imageTime=key}
            loading=false
        }
        // Prefetch adjacent keyframes only after the selected preview settles.
        if(imageTime==key && image!=null) {
            delay(400)
            if(key>=5_000) before=withTimeoutOrNull(2500) {SceneFramesRP151.frame(source,key-5_000)}
            ensureActive()
            after=withTimeoutOrNull(2500) {SceneFramesRP151.frame(source,key+5_000)}
        }
    }
    Column(modifier.width(280.dp).background(Color(0xF50B0D13),RoundedCornerShape(16.dp)).padding(10.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement=Arrangement.spacedBy(5.dp),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
            before?.let {Image(it.asImageBitmap(),null,Modifier.width(42.dp).height(52.dp),contentScale=androidx.compose.ui.layout.ContentScale.Crop)}
            Box(Modifier.weight(1f).height(108.dp).background(Color.Black,RoundedCornerShape(8.dp)),contentAlignment=androidx.compose.ui.Alignment.Center) {
                image?.let {Image(it.asImageBitmap(),null,Modifier.fillMaxSize(),contentScale=androidx.compose.ui.layout.ContentScale.Fit)}
                    ?: Text(if(loading) tr("Cargando escena…","Loading scene…") else tr("Esta fuente no ofrece miniaturas","This source does not provide thumbnails"),color=Color.White,fontSize=12.sp,modifier=Modifier.padding(10.dp))
            }
            after?.let {Image(it.asImageBitmap(),null,Modifier.width(42.dp).height(52.dp),contentScale=androidx.compose.ui.layout.ContentScale.Crop)}
        }
        Text("%d:%02d".format(position/60000,(position/1000)%60),color=Accent,fontSize=16.sp)
        if(image!=null) Text(if(loading || imageTime!=key) tr("Actualizando · imagen de ","Updating · image at ")+"%d:%02d".format(imageTime/60000,(imageTime/1000)%60) else tr("Escena cercana al punto seleccionado","Scene near the selected time"),color=Color.White,fontSize=10.sp)
        Text(tr("OK · Ir aquí  /  Atrás · Cancelar","OK · Seek here  /  Back · Cancel"),color=Color.White,fontSize=10.sp)
    }
}
