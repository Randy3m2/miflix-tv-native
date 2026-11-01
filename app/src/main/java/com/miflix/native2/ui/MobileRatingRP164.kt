package com.miflix.native2.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.miflix.native2.model.MediaSummary

@Composable internal fun MobileRatingRP164(state:AppState,item:MediaSummary,close:()->Unit) {
 var score by remember(item.cloudId) {mutableIntStateOf(state.ratings[item.cloudId] ?: 5)}
 var saving by remember {mutableStateOf(false)}
 MobileSheetRP133(tr("Puntuar","Rate"),close) {
  Column(verticalArrangement=Arrangement.spacedBy(14.dp)) {
   Text(item.title,color=androidx.compose.ui.graphics.Color.White)
   Text("★ $score / 10",color=MobileTintRP133)
   (1..10).toList().chunked(5).forEach {row->Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {row.forEach {value->MobileButtonRP133(value.toString(),modifier=Modifier.weight(1f),selected=score==value) {score=value}}}}
   Text(tr("Se guarda en el mismo perfil de TV y móvil.","Saved to the same TV and mobile profile."),color=Muted)
   MobileButtonRP133(if(saving) tr("Guardando…","Saving…") else tr("Guardar puntuación","Save rating"),"check",Modifier.fillMaxWidth(),enabled=!saving) {if(!saving) state.launch {saving=true;try {state.rate(item,score);close()} finally {saving=false}}}
  }
 }
}
