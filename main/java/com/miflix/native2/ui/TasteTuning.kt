package com.miflix.native2.ui

import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.Text
import com.miflix.native2.model.MediaSummary
import kotlinx.coroutines.CancellationException

@Composable
internal fun TasteTuning(state:AppState,item:MediaSummary,close:()->Unit) {
    var traits by remember(item.cloudId) {mutableStateOf(state.tasteAspects(item))}
    var working by remember {mutableStateOf(false)}
    PlayerSideSheet(tr("¿Qué te gustó?","What did you love?"),close) {
        item {CinemaBanner("BRUNIO · TASTE LAB",item.title,tr("Cuéntanos qué se quedó contigo.","Tell us what stayed with you."))}
        items(listOf("plot" to tr("Trama y temas","Plot & themes"),"pace" to tr("Ritmo · temas similares","Pacing · similar themes"),"visual" to tr("Fotografía","Cinematography"),"acting" to tr("Actuaciones","Performances"),"ending" to tr("Giros / finales · temas similares","Twists / endings · similar themes"))) {(id,label) ->
            FeatureActionCard(label,if(id in traits) tr("Guardado en tus preferencias","Saved in your taste profile") else tr("Seleccionar este aspecto","Choose this aspect"),if(id in traits) "✓" else "◇") {traits=if(id in traits) traits-id else traits+id;state.saveTasteAspects(item,traits)}
        }
        item {Text(tr("Afinación por metadatos, sin IA de escenas. Actuaciones compara reparto; fotografía compara director de fotografía; trama, ritmo y finales usan temas compartidos como aproximación, no detectan esas cualidades en el vídeo.","Metadata tuning, without scene AI. Acting matches cast; visuals match cinematographers; plot, pacing and endings use shared themes as a proxy, not analysis of those qualities in the video."),color=Muted)}
        item {FocusButton(if(working) tr("Buscando…","Matching…") else tr("Encontrar coincidencias","Find matches"),primary=true) {
            if(!working && traits.isNotEmpty()) {working=true;state.launch {try {state.findTasteMatches(item,traits)} catch(e:CancellationException) {throw e} catch(e:Exception) {state.error=e.message} finally {working=false}}}
        }}
    }
}
