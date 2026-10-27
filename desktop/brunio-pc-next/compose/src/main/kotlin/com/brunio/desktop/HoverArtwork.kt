package com.brunio.desktop

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.*
import java.awt.Toolkit
import java.awt.image.BufferedImage
import java.awt.image.ImageObserver
import java.net.URI
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

private object GifBytes {
    private val items=LinkedHashMap<String,ByteArray>(8,.75f,true)
    @Synchronized fun clear(){items.clear()}
    @Synchronized fun get(url:String):ByteArray?=items[url]
    @Synchronized fun put(url:String,bytes:ByteArray) {
        items[url]=bytes
        while(items.values.sumOf{it.size.toLong()}>32L*1024*1024)items.remove(items.keys.first())
    }
}

fun clearAnimatedArtwork(){GifBytes.clear()}

/** Java's GIF producer composites disposal frames; the observer is detached on hover exit. */
@Composable
fun HoverArtwork(poster:String,animation:String,hovered:Boolean,modifier:Modifier) {
    var bitmap by remember(animation){mutableStateOf<ImageBitmap?>(null)}
    LaunchedEffect(hovered,animation) {
        if(!hovered || animation.isBlank())return@LaunchedEffect
        val active=AtomicBoolean(true)
        var image:java.awt.Image?=null
        val frames=AtomicReference<BufferedImage?>(null)
        try {
            val bytes=withContext(Dispatchers.IO) {
                GifBytes.get(animation) ?: run {
                    val uri=URI(animation);require(uri.scheme=="https" && uri.userInfo==null)
                    val connection=uri.toURL().openConnection();connection.connectTimeout=8000;connection.readTimeout=8000
                    val data=connection.getInputStream().use{it.readNBytes(8*1024*1024+1)}
                    require(data.size<=8*1024*1024 && data.size>=6 && data.copyOfRange(0,3).toString(Charsets.US_ASCII)=="GIF")
                    GifBytes.put(animation,data);data
                }
            }
            val observer=ImageObserver { img,flags,_,_,_,_ ->
                if(flags and (ImageObserver.ERROR or ImageObserver.ABORT)!=0)active.set(false)
                if(active.get() && flags and (ImageObserver.FRAMEBITS or ImageObserver.ALLBITS)!=0) {
                    val width=img.getWidth(null);val height=img.getHeight(null)
                    if(width>0 && height>0 && width.toLong()*height<=2_000_000) {
                        val frame=BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB)
                        val graphics=frame.createGraphics()
                        try{graphics.drawImage(img,0,0,null)}finally{graphics.dispose()}
                        frames.set(frame)
                    }
                }
                active.get()
            }
            image=Toolkit.getDefaultToolkit().createImage(bytes)
            Toolkit.getDefaultToolkit().prepareImage(image,-1,-1,observer)
            while(isActive && active.get()){frames.getAndSet(null)?.let{bitmap=it.toComposeImageBitmap()};delay(33)}
        } catch(cancel:CancellationException){throw cancel}
        catch(_:Exception) { /* Static platform artwork remains visible. */ }
        finally{active.set(false);image?.flush();frames.set(null);bitmap=null}
    }
    Box(modifier) {
        RemoteArtwork(poster,Modifier.fillMaxSize())
        if(hovered)bitmap?.let{Image(it,null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)}
    }
}
