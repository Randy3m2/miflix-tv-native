package com.miflix.native2.ui

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.miflix.native2.data.FinalCatalogRP97
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
internal fun FinalM3USettingsRP97(state:AppState) {
    var qr by remember {mutableStateOf(false)}
    var busy by remember {mutableStateOf(false)}
    var message by remember {mutableStateOf("")}
    var url by remember {mutableStateOf(FinalCatalogRP97.source())}
    val scope=rememberCoroutineScope()
    if(qr) FinalM3UQrRP97(state,saved={url=it;message=tr("URL guardada. Pulsa Actualizar canales.","URL saved. Select Update channels.")},close={qr=false})
    Text(tr("Lista principal de Live TV","Main Live TV playlist"),color=Color.White,fontSize=18.sp)
    Text(if(url.isBlank()) tr("Usando la lista incluida en la app.","Using the playlist included in the app.") else
        tr("Fuente: ","Source: ")+runCatching {java.net.URI(url).host}.getOrDefault("M3U"),color=Muted,fontSize=12.sp)
    SettingsTileRC103(tr("Cambiar URL M3U por QR","Change M3U URL by QR"),tr("Pega el enlace Raw de GitHub desde el celular, en la misma red.","Paste the GitHub Raw URL from your phone on the same network."),"lists") {if(!busy) qr=true}
    SettingsTileRC103(if(busy) tr("Actualizando…","Updating…") else tr("Actualizar canales","Update channels"),
        tr("Descarga y guarda la lista en esta TV. Si falla, conserva la última copia válida.","Download and save the playlist on this TV. On failure, keep the last valid copy."),"lists") {
        if(!busy) {
            if(url.isBlank()) {message=tr("Primero agrega una URL con el QR.","First add a URL using the QR.");qr=true}
            else scope.launch {
                busy=true
                try {
                    val count=FinalCatalogRP97.refresh()
                    CustomChannelsRP45.changesRP48.value++
                    LiveLibrary.revision++
                    message=tr("Lista actualizada: ","Playlist updated: ")+count+tr(" canales."," channels.")
                } catch(e:CancellationException) {throw e} catch(_:Exception) {message=tr("No se pudo actualizar. Conservamos la lista anterior.","Could not update. Kept the previous playlist.")}
                finally {busy=false}
            }
        }
    }
    if(message.isNotBlank()) Text(message,color=Color.White,fontSize=13.sp)
}
