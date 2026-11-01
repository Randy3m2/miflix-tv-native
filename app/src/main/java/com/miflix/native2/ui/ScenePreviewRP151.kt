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
    val executor=Executors.newSingleThreadExecutor {r->Thread(r,"brunio-scene-preview").apply {isDaemon=true}}
    val busy=AtomicBoolean(false)
    private var sourceKey:String?=null
    private var reader:MediaMetadataRetriever?=null
    fun release() {executor.execute {runCatching {reader?.release()};reader=null;sourceKey=null}}
    suspend fun frame(source:StreamChoice,position:Long):Bitmap? {
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
                            bitmap=if(android.os.Build.VERSION.SDK_INT>=27) retriever.getScaledFrameAtTime(position*1000L,MediaMetadataRetriever.OPTION_CLOSEST,320,180)
                            else null // Older TVs retain the timestamp without allocating a full-resolution frame.
                        }
                    }
                } catch(_:Exception) {runCatching {reader?.release()};reader=null;sourceKey=null} finally {busy.set(false)}
                if(continuation.isActive) continuation.resumeWith(Result.success(bitmap)) else bitmap?.recycle()
            }
        }
    }
}

@Suppress("UNUSED_PARAMETER")
internal fun scenePreviewEnabledRP151(context:android.content.Context)=true

@Composable
internal fun ScenePreviewRP151(source:StreamChoice,position:Long,modifier:Modifier=Modifier) {
    var image by remember(source.url,source.requestHeaders) {mutableStateOf<Bitmap?>(null)}
    var loading by remember {mutableStateOf(false)}
    val cache=remember(source.url,source.requestHeaders) {linkedMapOf<Long,Bitmap>()}
    // Rounded keyframes are representative previews, not frame-exact promises.
    val key=position/5_000L*5_000L
    DisposableEffect(source.url,source.requestHeaders) {onDispose {SceneFramesRP151.release()}}
    LaunchedEffect(source.url,source.requestHeaders,key) {
        image=cache[key];loading=image==null
        if(image==null) {
            delay(250)
            val frame=withTimeoutOrNull(7000) {SceneFramesRP151.frame(source,key)}
            ensureActive()
            if(frame!=null) {
                if(cache.size>=12) cache.remove(cache.keys.first())
                cache[key]=frame;image=frame
            }
        }
        loading=false
    }
    Column(modifier.width(210.dp).background(Color(0xf0151520),RoundedCornerShape(12.dp)).padding(8.dp),verticalArrangement=Arrangement.spacedBy(5.dp)) {
        val bitmap=image
        if(bitmap!=null) Image(bitmap.asImageBitmap(),null,Modifier.fillMaxWidth().height(100.dp))
        else Text(if(loading) tr("Cargando escena…","Loading scene…") else tr("Vista previa no disponible en esta fuente","Scene preview unavailable for this source"),color=Color.White,fontSize=12.sp,maxLines=3)
        Text("%d:%02d".format(key/60000,(key/1000)%60),color=Accent,fontSize=12.sp)
        Text(tr("OK · Ir aquí  /  Atrás · Cancelar","OK · Seek here  /  Back · Cancel"),color=Color.White,fontSize=10.sp)
    }
}
