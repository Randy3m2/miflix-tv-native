package com.miflix.native2.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.miflix.native2.model.MediaSummary

/** RP204: same per-profile custom collections on mobile and TV, including manual order. */
@Composable
internal fun MobileCollectionsRP204(state:AppState,adding:MediaSummary?=null,close:()->Unit) {
    val owner=state.localExtrasScope()
    val extras=state.localExtras
    var selected by remember(owner) {mutableStateOf<String?>(null)}
    var newName by remember(owner) {mutableStateOf("")}
    var rename by remember(owner,selected) {mutableStateOf("")}
    var message by remember(owner) {mutableStateOf("")}
    val rows=extras.lists(owner)
    val current=rows.firstOrNull {it.optString("id")==selected}
    val currentItems=current?.let {extras.listItems(it)}.orEmpty()
    MobileSheetRP133(
        if(adding!=null) tr("Guardar en colección","Save to collection")
        else tr("Mis colecciones","My collections"),close
    ) {
        if(message.isNotBlank()) Text(message,color=MobileTintRP133,fontSize=13.sp)
        LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(10.dp),
            contentPadding=PaddingValues(bottom=20.dp)) {
            if(current==null) {
                item("create") {
                    Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        MobileFieldRP133(newName,{newName=it.take(60)},tr("Nombre de la colección","Collection name"))
                        MobileButtonRP133(tr("Crear colección","Create collection"),"plus",Modifier.fillMaxWidth(),enabled=newName.trim().isNotEmpty()) {
                            runCatching {
                                val id=extras.createList(owner,newName)
                                if(adding!=null) extras.add(owner,id,adding)
                                else selected=id
                                newName=""
                                message=tr("Colección creada","Collection created")
                            }.onFailure {message=it.message.orEmpty()}
                        }
                    }
                }
                itemsIndexed(rows,key={_,row->row.optString("id")}) {index,row->
                    val id=row.optString("id")
                    Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {
                        MobileButtonRP133(row.optString("name")+" · "+extras.listItems(row).size,"list",Modifier.fillMaxWidth()) {
                            if(adding!=null) {
                                runCatching {extras.add(owner,id,adding)}
                                    .onSuccess {message=tr("Título guardado","Title saved")}
                                    .onFailure {message=it.message.orEmpty()}
                            } else {selected=id;rename=row.optString("name");message=""}
                        }
                        Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                            MobileButtonRP133(tr("↑ Subir","↑ Up"),"chevron",Modifier.weight(1f),enabled=index>0) {extras.moveList(owner,id,-1)}
                            MobileButtonRP133(tr("↓ Bajar","↓ Down"),"chevron",Modifier.weight(1f),enabled=index<rows.lastIndex) {extras.moveList(owner,id,1)}
                        }
                    }
                }
            } else {
                item("manage") {
                    Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
                        MobileButtonRP133(tr("← Todas las colecciones","← All collections"),"back",Modifier.fillMaxWidth()) {selected=null}
                        MobileFieldRP133(rename,{rename=it.take(60)},tr("Nombre","Name"))
                        MobileButtonRP133(tr("Guardar nombre","Save name"),"check",Modifier.fillMaxWidth(),enabled=rename.trim().isNotEmpty()) {
                            runCatching {extras.renameList(owner,current.optString("id"),rename)}
                                .onFailure {message=it.message.orEmpty()}
                        }
                        MobileButtonRP133(tr("Eliminar colección","Delete collection"),"close",Modifier.fillMaxWidth()) {
                            extras.deleteList(owner,current.optString("id"));selected=null
                        }
                        Text(tr("Orden de títulos","Title order"),color=Color.White,fontSize=17.sp)
                    }
                }
                itemsIndexed(currentItems,key={_,item->item.cloudId}) {index,item->
                    Column(verticalArrangement=Arrangement.spacedBy(5.dp)) {
                        Text(item.title,color=Color.White,fontSize=15.sp)
                        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                            MobileButtonRP133("↑","chevron",Modifier.weight(1f),enabled=index>0) {extras.moveListItem(owner,current.optString("id"),item.cloudId,-1)}
                            MobileButtonRP133("↓","chevron",Modifier.weight(1f),enabled=index<currentItems.lastIndex) {extras.moveListItem(owner,current.optString("id"),item.cloudId,1)}
                            MobileButtonRP133(tr("Quitar","Remove"),"close",Modifier.weight(1f)) {extras.remove(owner,current.optString("id"),item.cloudId)}
                        }
                    }
                }
            }
        }
    }
}
