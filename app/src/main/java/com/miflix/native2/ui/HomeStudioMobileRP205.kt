package com.miflix.native2.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text

/** RP205: shared Home Studio settings. Phone edits update TV order on next profile load. */
@Composable
internal fun HomeStudioMobileRP205(state:AppState,close:()->Unit) {
    val saved=state.homeRowOrder
    val visibleRows=remember(saved) {
        (listOf("continue")+saved+HomeRows.map {it.first})
            .distinct().filter {key->HomeRows.any {it.first==key} && key!="daily_rotation"}
    }
    val hidden=state.hiddenHomeRows
    MobileSheetRP133(tr("Editar Home","Customize Home"),close) {
        Text(
            tr("Reordena, muestra u oculta filas. Los cambios se guardan en este perfil y se comparten con tu TV al abrir BruniO. Sin sincronización periódica.",
                "Reorder, show or hide rows. Changes are saved for this profile and shared with TV when BruniO opens. No polling."),
            color=Muted,fontSize=13.sp
        )
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            MobileButtonRP133(tr("Restaurar orden","Reset order"),"refresh",Modifier.weight(1f)) {state.saveHomeRowOrder(emptyList())}
            MobileButtonRP133(tr("Mostrar todas","Show all"),"check",Modifier.weight(1f)) {state.restoreHomeRows()}
        }
        LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp),
            contentPadding=PaddingValues(bottom=28.dp)) {
            items(visibleRows,key={it}) {key->
                val index=visibleRows.indexOf(key)
                val label=HomeRows.firstOrNull {it.first==key}?.second ?: key
                Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    Text("${index+1}. $label",color=Color.White,fontSize=15.sp,fontWeight=FontWeight.Bold)
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        MobileButtonRP133("↑","chevron",Modifier.weight(1f),enabled=index>1 && key!="continue") {
                            val order=visibleRows.toMutableList()
                            java.util.Collections.swap(order,index,index-1)
                            state.saveHomeRowOrder(order)
                        }
                        MobileButtonRP133("↓","chevron",Modifier.weight(1f),enabled=key!="continue" && index in 1 until visibleRows.lastIndex) {
                            val order=visibleRows.toMutableList()
                            java.util.Collections.swap(order,index,index+1)
                            state.saveHomeRowOrder(order)
                        }
                        MobileButtonRP133(if(key in hidden) tr("Mostrar","Show") else tr("Ocultar","Hide"),
                            if(key in hidden) "plus" else "close",Modifier.weight(1.4f),enabled=key!="continue") {
                            state.setHomeRowVisible(key,key in hidden)
                        }
                    }
                }
            }
        }
    }
}
