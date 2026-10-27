package com.brunio.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.PasswordVisualTransformation
import kotlinx.coroutines.delay
import org.json.JSONObject

@Composable
fun DownloadsScreen(utilities:Utilities,onPlay:(JSONObject)->Unit) {
    LaunchedEffect(utilities) { while(true) { utilities.refreshDownloads();delay(2500) } }
    Column(Modifier.fillMaxSize().padding(36.dp),verticalArrangement=Arrangement.spacedBy(22.dp)) {
        Text("Descargas",style=MaterialTheme.typography.headlineLarge)
        Text("Películas, series y temporadas · Biblioteca de esta versión de PC",color=Color.Gray)
        utilities.error?.let { Text(it,color=MaterialTheme.colorScheme.error) }
        val rows=utilities.downloads?.optJSONArray("files")?.objects().orEmpty()
        if(rows.isEmpty())EmptyState("Tu colección offline","Descarga una película o una temporada desde sus detalles.")
        LazyColumn(verticalArrangement=Arrangement.spacedBy(20.dp)) {
            val groups=rows.groupBy { it.optString("cloud")+":"+it.optInt("season") }.values.toList()
            items(groups) { entries ->
                val first=entries.first()
                Surface(shape=RoundedCornerShape(24.dp),color=Color(0xFF17171C)) {
                    Row(Modifier.fillMaxWidth().padding(22.dp),horizontalArrangement=Arrangement.spacedBy(22.dp)) {
                        RemoteArtwork(first.optString("cover").ifBlank{first.optString("poster")},Modifier.width(100.dp).height(150.dp))
                        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(14.dp)) {
                            Text(first.optString("title"),style=MaterialTheme.typography.titleLarge)
                            if(first.optInt("season")>0)Text("Temporada ${first.optInt("season")}",color=Color.Gray)
                            entries.sortedBy { it.optInt("episode") }.forEach { entry ->
                                val id=entry.getString("id");val status=entry.getString("state")
                                Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                                    Text((if(entry.optInt("episode")>0)"E${entry.optInt("episode")} · " else "")+when(status){"complete"->"Lista";"downloading"->"Descargando";"queued"->"En cola";"failed"->"Falló";else->"Interrumpida"},Modifier.weight(1f))
                                    if(status=="complete")TextButton(onClick={utilities.downloadAction("downloads.open",id,onPlay)}){Text("Reproducir")}
                                    if(status in listOf("queued","downloading"))TextButton(onClick={utilities.downloadAction("downloads.cancel",id)}){Text("Cancelar")}
                                    else TextButton(onClick={utilities.downloadAction("downloads.remove",id)}){Text("Eliminar")}
                                }
                                if(status=="downloading") {
                                    val total=entry.optLong("total");val done=entry.optLong("done")
                                    if(total>0)LinearProgressIndicator(progress={ (done.toDouble()/total).toFloat().coerceIn(0f,1f) },modifier=Modifier.fillMaxWidth(),color=Color.White)
                                    else Text("${done/1048576} MB",color=Color.Gray)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UtilityDialog(utilities:Utilities) {
    val kind=utilities.dialog?:return
    var text by remember(kind){mutableStateOf("")}
    LaunchedEffect(kind) { if(kind=="party")while(true){utilities.pollParty();delay(2500)} }
    LaunchedEffect(kind,utilities.updates?.optString("status")) {
        if(kind=="updates" && utilities.updates?.optString("status")=="downloading")while(true){delay(2000);utilities.pollUpdate()}
    }
    AlertDialog(onDismissRequest={utilities.dialog=null},title={Text(when(kind){"party"->"Friends & Party";"addons"->"Complementos";else->"Actualizaciones"})},
        text={Column(Modifier.widthIn(min=420.dp,max=640.dp).heightIn(max=620.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            if(utilities.busy)LinearProgressIndicator(Modifier.fillMaxWidth())
            utilities.error?.let{Text(it,color=MaterialTheme.colorScheme.error)}
            when(kind) {
                "party" -> {
                    val party=utilities.party;val room=party?.optString("room")?.takeUnless{it.isBlank()||it=="null"}
                    if(room==null) {
                        Text("Crea una sala o solicita acceso usando su código.")
                        OutlinedTextField(text,{text=it.filter{c->c.isDigit()}.take(6)},label={Text("Código de 6 dígitos")},singleLine=true)
                        Row { TextButton(onClick={utilities.create()},enabled=!utilities.busy){Text("Crear sala")};TextButton(onClick={utilities.join(text)},enabled=text.length==6&&!utilities.busy){Text("Solicitar acceso")} }
                        party?.optString("pending")?.takeUnless{it=="null"||it.isBlank()}?.let{Text("Solicitud pendiente · $it")}
                    } else {
                        Text("Sala $room",style=MaterialTheme.typography.headlineMedium)
                        Text("El chat está conectado. La sincronización del vídeo aún requiere integración y pruebas.",color=Color.Gray)
                        party?.optJSONArray("requests")?.objects()?.forEach { request ->
                            Text(request.optString("nickname","Invitado")+" solicita acceso")
                            Row { TextButton(onClick={utilities.decide(request,true)}){Text("Aceptar")};TextButton(onClick={utilities.decide(request,false)}){Text("Rechazar")} }
                        }
                        party?.optJSONArray("events")?.objects()?.forEach{ event->Text(event.optString("user_id").take(8)+" · "+event.optString("body")) }
                        OutlinedTextField(text,{text=it.take(400)},label={Text("Mensaje")})
                        Row { TextButton(onClick={utilities.send(text);text=""},enabled=text.isNotBlank()){Text("Enviar")};listOf("😂","🔥","😱","🍿").forEach{emoji->TextButton(onClick={utilities.send(emoji,"emoji")}){Text(emoji)}} }
                        OutlinedButton(onClick={utilities.leave()}){Text("Salir de la sala")}
                    }
                }
                "addons" -> {
                    val data=utilities.addons;val items=data?.optJSONArray("items")?.objects().orEmpty()
                    Text("Compartidos con tu cuenta de TV. Prioridad por perfil e idioma.",color=Color.Gray)
                    val saved=data?.optJSONArray("order")?.let{a->(0 until a.length()).map{a.getString(it)}}.orEmpty()
                    val rows=items.sortedBy{ row->saved.indexOf(row.getString("id")).takeIf{it>=0}?:Int.MAX_VALUE }
                    rows.forEachIndexed { index,row ->
                        Row {Text(row.optString("name"),Modifier.weight(1f));TextButton(onClick={ val ids=rows.map{it.getString("id")}.toMutableList();java.util.Collections.swap(ids,index,index-1);utilities.order(ids)},enabled=index>0&&!utilities.busy){Text("Subir")};TextButton(onClick={utilities.removeAddon(row.getString("id"))},enabled=!utilities.busy){Text("Quitar")} }
                    }
                    OutlinedTextField(text,{text=it},label={Text("URL privada del manifest")},visualTransformation=PasswordVisualTransformation(),singleLine=true)
                    TextButton(onClick={utilities.add(text.trim());text=""},enabled=text.isNotBlank()&&!utilities.busy){Text("Agregar complemento")}
                }
                "updates" -> {
                    val update=utilities.updates
                    Text("Versión de desarrollo · ${update?.optString("current","2.0.0-rc1")}")
                    val available=update?.optString("available")?.takeUnless{it=="null"||it.isBlank()}
                    when(update?.optString("status")) {
                        "downloading"->Text("Descargando y verificando el instalador…")
                        "verified"->{Text("Instalador verificado. Se abrirá al pulsar Instalar.");Button(onClick={utilities.installUpdate()}){Text("Instalar actualización")}}
                        "failed"->Text("No se pudo descargar o verificar el instalador.")
                        "launched"->Text("Instalador abierto. Cierra BruniO para completar la actualización.")
                        else->if(available==null)Text("No hay una actualización posterior disponible para esta versión.")else{Text("Disponible: $available");Button(onClick={utilities.prepareUpdate()}){Text("Descargar actualización")}}
                    }
                }
            }
        }},confirmButton={TextButton(onClick={utilities.dialog=null}){Text("Cerrar")}})
}
