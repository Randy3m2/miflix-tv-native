package com.miflix.native2.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.miflix.native2.model.CastMember
import com.miflix.native2.model.MediaSummary
import kotlinx.coroutines.CancellationException

@Composable
internal fun MobileFilmographyRP141(state:AppState,person:CastMember,close:()->Unit) {
    var rows by remember(person.id,person.creditRole,state.interfaceLanguage,state.kidsMode) {mutableStateOf<List<MediaSummary>?>(null)}
    var failure by remember(person.id,person.creditRole,state.interfaceLanguage,state.kidsMode) {mutableStateOf(false)}
    var retry by remember(person.id) {mutableIntStateOf(0)}
    LaunchedEffect(person.id,person.creditRole,state.interfaceLanguage,state.kidsMode,retry) {
        rows=null;failure=false
        try {
            val loaded=state.tmdb.personCreditsRP141(person.id,person.creditRole)
            rows=if(state.kidsMode) state.tmdb.kidsFilter(loaded.take(40)) else loaded
        }
        catch(e:CancellationException) {throw e} catch(_:Exception) {failure=true}
    }
    MobileSheetRP133(person.name,close) {
        Text(tr("Películas y series","Movies and series"),color=Color.White)
        if(failure) MobileButtonRP133(tr("No se pudo cargar. Reintentar","Unable to load. Retry"),"refresh") {retry++}
        else if(rows==null) Text(tr("Cargando filmografía…","Loading filmography…"),color=Muted)
        else if(rows.isNullOrEmpty()) Text(tr("Sin títulos disponibles","No titles available"),color=Muted)
        LazyVerticalGrid(GridCells.Adaptive(105.dp),Modifier.weight(1f),contentPadding=PaddingValues(vertical=12.dp),horizontalArrangement=Arrangement.spacedBy(10.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
            items(rows.orEmpty(),key={it.cloudId}) {title->MobilePosterRP133(state,title,Modifier.fillMaxWidth(),beforeOpenRP141=close)}
        }
    }
}
