package com.miflix.native2.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Text
import com.miflix.native2.model.MediaSummary

@Composable
fun RatingDialog(state: AppState,item: MediaSummary,close: () -> Unit) {
    var score by remember { mutableStateOf(state.ratings[item.cloudId] ?: 5) }
    var saving by remember { mutableStateOf(false) }
    Dialog(onDismissRequest=close,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Column(Modifier.width(600.dp).background(Panel,RoundedCornerShape(22.dp)).padding(28.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
            Text(item.title,color=Color.White,fontSize=26.sp, lineHeight=31.sp,maxLines=2)
            Text("${tr("Tu puntuación","Your rating")} · $score / 10 ${tr("estrellas","stars")}",color=Color.White,fontSize=20.sp, lineHeight=24.sp)
            Text(tr("Solo para tus recomendaciones en BruniO","Only for your recommendations in BruniO"),color=Muted)
            (1..10).toList().chunked(5).forEach { row ->
                Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) { row.forEach { value -> FocusButton("★ $value",primary=score==value) { score=value } } }
            }
            Row(horizontalArrangement=Arrangement.spacedBy(14.dp)) {
                FocusButton(if(saving) tr("Guardando…","Saving…") else tr("Guardar","Save"),primary=true) { if(!saving) state.launch {
                    saving=true
                    try { state.socialAction { state.rate(item,score); close() } } finally { saving=false }
                } }
                FocusButton(tr("Cancelar","Cancel")) { close() }
            }
        }
    }
}
