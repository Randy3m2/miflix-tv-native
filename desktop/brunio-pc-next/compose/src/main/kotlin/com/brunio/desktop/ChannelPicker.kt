package com.brunio.desktop
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ChannelPicker(controller:AppController) {
    var query by remember{mutableStateOf("")}
    AlertDialog(onDismissRequest={controller.channelPicker=false},title={Text(ui("Cambiar canal"))},text={
        Column(Modifier.width(720.dp).heightIn(max=600.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            Row {TextButton(onClick={controller.loadMulti("tv")}){Text(ui("Canales"))};TextButton(onClick={controller.loadMulti("sports")}){Text(ui("Deportes"))}}
            OutlinedTextField(query,{query=it},label={Text(ui("Buscar canal"))},singleLine=true)
            LazyVerticalGrid(GridCells.Adaptive(180.dp),horizontalArrangement=Arrangement.spacedBy(12.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                items(controller.multiChoices.filter{it.title.contains(query,true)},key={it.id}){tile->
                    Surface(onClick={controller.selectChannel(tile)},color=Color(0xFF202026)) {
                        Column(Modifier.padding(14.dp)){RemoteArtwork(tile.image,Modifier.fillMaxWidth().height(80.dp));Text(tile.title,maxLines=1)}
                    }
                }
            }
        }
    },confirmButton={TextButton(onClick={controller.channelPicker=false}){Text(ui("Cerrar"))}})
}
