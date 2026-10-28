package com.miflix.native2.ui

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.miflix.native2.data.LocalQrNetworkRP129
import com.miflix.native2.data.SearchKeyboardServerRP136
import android.os.Handler
import android.os.Looper
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Inline QR: typing never blocks the keyboard or results behind a dialog. */
@Composable
internal fun SearchPhoneKeyboardRP136(state:AppState,visible:Boolean,received:(String)->Unit) {
    val owner=state.searchHistoryOwner()
    val callback by rememberUpdatedState(received)
    var url by remember {mutableStateOf("")}
    var message by remember {mutableStateOf("")}
    val sessionAlive=remember(owner,visible,state.appForeground) {AtomicBoolean(true)}
    val connection=remember(sessionAlive) {java.util.concurrent.atomic.AtomicReference<SearchKeyboardServerRP136?>(null)}
    val main=remember {Handler(Looper.getMainLooper())}
    DisposableEffect(sessionAlive) {
        onDispose {sessionAlive.set(false);connection.getAndSet(null)?.close()}
    }
    LaunchedEffect(sessionAlive) {
        url="";message=""
        if(!visible || !state.appForeground) return@LaunchedEffect
        var owned:SearchKeyboardServerRP136?=null
        try {
            val created=withContext(Dispatchers.IO) {
                val endpoint=LocalQrNetworkRP129.find().firstOrNull() ?: error("LAN unavailable")
                SearchKeyboardServerRP136(endpoint.address,endpoint.prefixLength) {text ->
                    if(!sessionAlive.get()) false else {
                        main.post {if(sessionAlive.get() && state.appForeground && state.searchHistoryOwner()==owner && state.screen==Screen.SEARCH) callback(text)}
                        true
                    }
                }.also {owned=it;connection.set(it);if(!sessionAlive.get()) it.close()}
            }
            url=created.url
            kotlinx.coroutines.delay(900000)
            created.close();url="";message=tr("QR vencido. Cierra y abre QR.","QR expired. Close and reopen QR.")
        } catch(e:kotlinx.coroutines.CancellationException) {throw e}
        catch(_:Exception) {message=tr("Conecta el celular y TV a la misma red.","Connect phone and TV to the same network.")}
        finally {sessionAlive.set(false);owned?.close();connection.set(null)}
    }
    if(visible) Column(Modifier.fillMaxWidth().padding(8.dp),verticalArrangement=Arrangement.spacedBy(6.dp),horizontalAlignment=androidx.compose.ui.Alignment.CenterHorizontally) {
        if(url.isNotEmpty()) SearchPhoneQrImageRP136(url)
        Text(if(message.isNotEmpty()) message else tr("Escanea y escribe · Misma red","Scan and type · Same network"),fontSize=12.sp)
    }
}

@Composable
private fun SearchPhoneQrImageRP136(url:String) {
    val image by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null,url) {
        value=withContext(Dispatchers.Default) {
            val size=400
            val matrix=com.google.zxing.qrcode.QRCodeWriter().encode(url,com.google.zxing.BarcodeFormat.QR_CODE,size,size,mapOf(com.google.zxing.EncodeHintType.MARGIN to 4))
            val pixels=IntArray(size*size) {i ->if(matrix[i%size,i/size]) android.graphics.Color.BLACK else android.graphics.Color.WHITE}
            android.graphics.Bitmap.createBitmap(pixels,size,size,android.graphics.Bitmap.Config.ARGB_8888).asImageBitmap()
        }
    }
    image?.let {androidx.compose.foundation.Image(it,contentDescription="QR · Phone keyboard",modifier=Modifier.size(144.dp))}
}
