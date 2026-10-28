package com.brunio.desktop
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun MultiViewScreen(controller:AppController,onFullscreen:()->Unit) {
    var active by remember{mutableStateOf(0)}
    var sourceDialog by remember{mutableStateOf<Int?>(null)}
    Column(Modifier.fillMaxSize().background(Color.Black).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Row {Text(ui("Multiview"),Modifier.weight(1f),style=MaterialTheme.typography.titleLarge);TextButton(onClick={controller.multiPicker=true;controller.loadMulti("tv")},enabled=controller.multiview.size<4){Text(ui("Agregar señal"))};TextButton(onClick=onFullscreen){Text(ui("Maximizar"))};TextButton(onClick={controller.exitMulti()}){Text(ui("Volver"))} }
        controller.multiview.toList().chunked(2).forEachIndexed{ rowIndex,row->
            Row(Modifier.weight(1f).fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                row.forEachIndexed{ column,session->
                    val index=rowIndex*2+column
                    key(session.item.optString("cloudId")) {
                        Column(Modifier.weight(1f).fillMaxHeight()) {
                            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                                Text(session.item.optString("title"),Modifier.weight(1f))
                                TextButton(onClick={active=index}){Text(if(active==index)"Audio activo" else "Escuchar")}
                                TextButton(onClick={sourceDialog=index}){Text(ui("Fuente"))}
                                TextButton(onClick={controller.multiview.removeAt(index);active=0}){Text(ui("Cerrar"))}
                            }
                            Box(Modifier.weight(1f).fillMaxWidth().clickable{active=index}) {
                                PlayerScreen(session,{controller.multiview.remove(session)},onFullscreen,{_,_->},{},{controller.message=it},showControls=false,audible=active==index,renderWidth=960)
                            }
                        }
                    }
                }
            }
        }
    }
    if(controller.multiPicker) {
        var query by remember{mutableStateOf("")}
        AlertDialog(onDismissRequest={controller.multiPicker=false},title={Text(ui("Agregar canal o deporte"))},text={Column(Modifier.width(720.dp).heightIn(max=600.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
            Row {TextButton(onClick={controller.loadMulti("tv")}){Text(ui("Canales"))};TextButton(onClick={controller.loadMulti("sports")}){Text(ui("Deportes"))} }
            OutlinedTextField(query,{query=it},label={Text(ui("Buscar"))},singleLine=true)
            LazyVerticalGrid(GridCells.Adaptive(180.dp),horizontalArrangement=Arrangement.spacedBy(12.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                items(controller.multiChoices.filter{it.title.contains(query,true)},key={it.id}) { tile->
                    Surface(onClick={controller.addMulti(tile)},color=Color(0xFF202026)) {Column(Modifier.padding(12.dp)) {RemoteArtwork(tile.image,Modifier.fillMaxWidth().height(80.dp),contentScale=androidx.compose.ui.layout.ContentScale.Fit);Text(tile.title,maxLines=1)} }
                }
            }
        }},confirmButton={TextButton(onClick={controller.multiPicker=false}){Text(ui("Cerrar"))}})
    }
    sourceDialog?.let{index->controller.multiview.getOrNull(index)?.let{session->
        AlertDialog(onDismissRequest={sourceDialog=null},title={Text(ui("Cambiar fuente"))},text={Column {
            session.streams.forEach { stream->TextButton(onClick={controller.multiview[index]=session.copy(streams=listOf(stream)+session.streams.filter{it!==stream});sourceDialog=null}){Text(stream.optString("name","Fuente"))} }
        }},confirmButton={TextButton(onClick={sourceDialog=null}){Text(ui("Cerrar"))}})
    }}
}
