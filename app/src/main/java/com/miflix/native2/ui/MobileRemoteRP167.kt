package com.miflix.native2.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.miflix.native2.data.MobileRemoteRP167
import com.miflix.native2.data.RemoteDiscoveryRP168
import kotlinx.coroutines.*

@Composable internal fun MobileRemoteSheetRP167(close:()->Unit) {
 val context=LocalContext.current
 val prefs=remember {context.getSharedPreferences("brunio_remote_rp167",0)}
 var address by remember {mutableStateOf(prefs.getString("address","").orEmpty())}
 var code by remember {mutableStateOf("")}
 var text by remember {mutableStateOf("")}
 var client by remember {mutableStateOf<MobileRemoteRP167?>(null)}
 var busy by remember {mutableStateOf(false)}
 var error by remember {mutableStateOf("")}
 var scanning by remember {mutableStateOf(false)}
 var devices by remember {mutableStateOf<List<RemoteDiscoveryRP168>>(emptyList())}
 val scope=rememberCoroutineScope()
 DisposableEffect(Unit) {onDispose {client?.disconnect()}}
 fun send(action:String,value:String="") {if(busy) return;val remote=client ?: return;scope.launch {busy=true;try {remote.send(action,value);error=""} catch(e:CancellationException) {throw e} catch(e:Exception) {error=e.message.orEmpty()} finally {busy=false}}}
 MobileSheetRP133("BruniO Remote",close) {
  Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)) {
   if(client==null) {
    Text(tr("Conecta tu teléfono y la TV a la misma red Wi‑Fi. Puedes escanear la red o escribir la IP manualmente.","Connect your phone and TV to the same Wi‑Fi. Scan the network or enter the IP manually."),color=Muted)
    MobileButtonRP133(if(scanning) tr("Buscando TVs…","Scanning for TVs…") else tr("Escanear TVs cercanas","Scan nearby TVs"),"search",Modifier.fillMaxWidth(),enabled=!scanning) {
     scope.launch {scanning=true;error="";devices=runCatching {RemoteDiscoveryRP168.scan()}.getOrElse {error=it.message.orEmpty();emptyList()};scanning=false}
    }
    if(devices.isNotEmpty()) {
     Text(tr("TVs encontradas","Discovered TVs"),color=MobileTintRP133,fontSize=14.sp,fontWeight=androidx.compose.ui.text.font.FontWeight.SemiBold)
     devices.forEach {device -> MobileButtonRP133(device.label,"cast",Modifier.fillMaxWidth(),selected=address==device.address) {address=device.address}}
    } else if(!scanning && error.isBlank()) Text(tr("No se encontró ninguna TV todavía. Asegúrate de que el remoto esté abierto en la TV.","No TV found yet. Make sure the remote pairing screen is open on the TV."),color=Muted)
    MobileFieldRP133(address,{address=it},tr("IP de la TV · 192.168.1.20","TV IP · 192.168.1.20"))
    MobileFieldRP133(code,{code=it.filter(Char::isDigit).take(6)},tr("Código de 6 dígitos","Six-digit code"),keyboard=androidx.compose.ui.text.input.KeyboardType.Number)
    MobileButtonRP133(if(busy) tr("Conectando…","Connecting…") else tr("Conectar TV","Connect TV"),"remote",Modifier.fillMaxWidth(),enabled=!busy) {
     scope.launch {busy=true;try {val remote=MobileRemoteRP167(address);remote.connect(code);client=remote;prefs.edit().putString("address",address).apply();error=""} catch(e:CancellationException) {throw e} catch(e:Exception) {error=e.message.orEmpty()} finally {busy=false}}
    }
   } else {
    Text(tr("Conectado a ","Connected to ")+client!!.endpoint.removePrefix("http://"),color=MobileTintRP133)
    Text(tr("Navegación","Navigation"),color=MobileTintRP133,fontSize=14.sp,fontWeight=androidx.compose.ui.text.font.FontWeight.SemiBold)
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {MobileButtonRP133("←",modifier=Modifier.weight(1f)) {send("back")};MobileButtonRP133("Home","home",Modifier.weight(1f)) {send("home")};MobileButtonRP133("▶ / Ⅱ","play",Modifier.weight(1f)) {send("play_pause")}}
    Row {Spacer(Modifier.weight(1f));MobileButtonRP133("↑",modifier=Modifier.weight(1f)) {send("up")};Spacer(Modifier.weight(1f))}
    Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {MobileButtonRP133("←",modifier=Modifier.weight(1f)) {send("left")};MobileButtonRP133("OK",modifier=Modifier.weight(1f),selected=true) {send("ok")};MobileButtonRP133("→",modifier=Modifier.weight(1f)) {send("right")}}
    Row {Spacer(Modifier.weight(1f));MobileButtonRP133("↓",modifier=Modifier.weight(1f)) {send("down")};Spacer(Modifier.weight(1f))}
    Text(tr("Reproducción y volumen","Playback & volume"),color=MobileTintRP133,fontSize=14.sp,fontWeight=androidx.compose.ui.text.font.FontWeight.SemiBold)
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {MobileButtonRP133("−10s","back",modifier=Modifier.weight(1f)) {send("seek_back")};MobileButtonRP133("+10s","next",modifier=Modifier.weight(1f)) {send("seek_forward")}}
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {MobileButtonRP133("Vol −",modifier=Modifier.weight(1f)) {send("volume_down")};MobileButtonRP133("Mute",modifier=Modifier.weight(1f)) {send("mute")};MobileButtonRP133("Vol +",modifier=Modifier.weight(1f)) {send("volume_up")}}
    Text(tr("Contenido","Content"),color=MobileTintRP133,fontSize=14.sp,fontWeight=androidx.compose.ui.text.font.FontWeight.SemiBold)
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {MobileButtonRP133(tr("Fuentes","Sources"),"link",Modifier.weight(1f)) {send("sources")};MobileButtonRP133(tr("Subtítulos","Subtitles"),"cc",Modifier.weight(1f)) {send("subtitles")}}
    MobileButtonRP133(tr("Subtítulos ON / OFF","Subtitles ON / OFF"),modifier=Modifier.fillMaxWidth()) {send("subtitles_toggle")}
    MobileFieldRP133(text,{text=it.take(512)},tr("Escribe para buscar en TV","Type to search on TV"))
    MobileButtonRP133(tr("Escribir en el campo de TV","Type in focused TV field"),modifier=Modifier.fillMaxWidth(),enabled=text.isNotBlank()) {send("type",text)}
    MobileButtonRP133(tr("Buscar en TV","Search on TV"),"search",Modifier.fillMaxWidth(),enabled=text.isNotBlank()) {send("text",text)}
    MobileButtonRP133(tr("Desconectar","Disconnect"),modifier=Modifier.fillMaxWidth()) {client?.disconnect();client=null}
    Text(tr("Fuentes y subtítulos se abren en la TV; elige con las flechas y OK. No disponible en Kids o Watch Party.","Sources and subtitles open on TV; choose with the arrows and OK. Unavailable in Kids or Watch Party."),color=Muted)
   }
   if(error.isNotBlank()) Text(error,color=Color(0xFFFFB4AB))
  }
 }
}
