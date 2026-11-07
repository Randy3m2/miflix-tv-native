package com.miflix.native2.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.miflix.native2.data.RokuCastRP182
import kotlinx.coroutines.delay

@Composable
internal fun RokuCastSheetRP182(request:PlayerRequest,close:()->Unit) {
    val context=LocalContext.current
    val status by RokuCastRP182.status.collectAsState()
    var message by remember {mutableStateOf("")}
    LaunchedEffect(status.sharing) {while(status.sharing) {delay(1000);RokuCastRP182.server?.lastError?.takeIf {it.isNotBlank()}?.let {message=it}}}
    MobileSheetRP133("Roku Media Player",close) {
        LazyColumn(Modifier.weight(1f).fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item {Text(request.item.title,color=Color.White,fontSize=19.sp)}
        item {Text(tr("Conecta el celular y Roku a la misma red Wi-Fi. BruniO compartirá únicamente este contenido.","Connect your phone and Roku to the same Wi-Fi. BruniO will share only this content."),color=Muted,fontSize=14.sp)}
        item {Text(tr("En Roku: abre Roku Media Player → Video → BruniO Mobile → Now Playing. También verás Movies, Episodes, Live TV y Live Sports.","On Roku: open Roku Media Player → Video → BruniO Mobile → Now Playing. You can also browse Movies, Episodes, Live TV and Live Sports."),color=Muted,fontSize=14.sp)}
        item {Text(tr("Los canales HLS en vivo necesitan comprobar compatibilidad en tu Roku. Publicar el canal no significa que la TV ya esté reproduciéndolo. DRM no está soportado.","Live HLS channels need a compatibility check on your Roku. Publishing a channel does not mean the TV has started playback. DRM is unsupported."),color=Muted,fontSize=13.sp)}
        if(status.title.isNotBlank()) item {Text(status.title,color=MobileTintRP133,fontSize=14.sp)}
        if(status.message.isNotBlank()) item {Text(status.message,color=Muted,fontSize=13.sp)}
        if(message.isNotBlank()) item {Text(message,color=Muted,fontSize=13.sp)}
        item {Text(tr("Puedes cerrar este panel: compartir continúa hasta detenerlo desde aquí o desde la notificación. El teléfono debe permanecer conectado a la red.","You can close this panel: sharing continues until stopped here or from the notification. Keep the phone connected to the network."),color=Muted,fontSize=12.sp)}
        }
        if(status.sharing) {
            MobileButtonRP133(tr("Dejar de compartir","Stop sharing"),"close",Modifier.fillMaxWidth()) {RokuCastRP182.stop(context)}
        }
        MobileButtonRP133(if(status.busy) tr("Preparando…","Preparing…") else if(status.sharing) tr("Compartir este contenido","Share this content") else tr("Compartir con Roku Media Player","Share with Roku Media Player"),"cast",Modifier.fillMaxWidth(),enabled=!status.busy) {
            runCatching {RokuCastRP182.start(context,request)}.onFailure {message=it.message.orEmpty()}
        }
    }
}
