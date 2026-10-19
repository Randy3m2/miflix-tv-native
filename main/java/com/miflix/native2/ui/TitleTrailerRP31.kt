package com.miflix.native2.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.miflix.native2.model.MediaSummary
import com.miflix.native2.model.TrailerSummary
import kotlinx.coroutines.CancellationException

@Composable
internal fun TitleTrailerRP31(state:AppState,item:MediaSummary,close:()->Unit) {
    var trailer by remember(item.cloudId) {mutableStateOf<TrailerSummary?>(null)}
    var loading by remember(item.cloudId) {mutableStateOf(true)}
    var retry by remember {mutableIntStateOf(0)}
    DisposableEffect(state) {state.discoveryPreviewActive=true;onDispose {state.discoveryPreviewActive=false;MenuActivity.touch()}}
    LaunchedEffect(item.cloudId,state.interfaceLanguage,retry) {
        loading=true;trailer=null
        try {trailer=state.tmdb.previewTrailers(item).firstOrNull {it.site.equals("YouTube",true) && it.key.isNotBlank()}}
        catch(e:CancellationException) {throw e} catch(_:Exception) { }
        finally {loading=false}
    }
    val current=trailer
    if(current!=null) LidomYoutubePlayer(current.key,onWorking={},close=close,preview=true,caption=item.title)
    else PlayerMenuDialog(onDismissRequest=close) {
        Column(Modifier.widthIn(max=520.dp).background(Color(0xFF101010)).padding(26.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
            Text(item.title,color=Color.White)
            Text(if(loading) tr("Buscando tráiler…","Finding trailer…") else tr("No hay un tráiler disponible. Puedes reintentar.","No trailer is available. You can retry."),color=Color.White)
            if(!loading) FocusButton(tr("Reintentar","Retry")) {retry++}
            FocusButton(tr("Cerrar","Close"),onClick=close)
        }
    }
}
