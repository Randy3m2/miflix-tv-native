package com.miflix.native2.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.miflix.native2.data.TvHomeRP167
import kotlinx.coroutines.*

@Composable internal fun TvHomeRuntimeRP167(state:AppState) {
 val context=LocalContext.current.applicationContext
 val owner="${state.session?.userId ?: "local"}:${state.activeProfile.id}"
 val favorites=state.favorites.toList()
 val continuing=state.continueWatching.toList()
 val recommended=(state.forYou.toList()+state.trending.toList()).distinctBy {it.cloudId}
 LaunchedEffect(owner,state.kidsMode,state.appForeground,state.screen==Screen.SETTINGS,favorites,continuing.map {it.cloudId},recommended.map {it.cloudId}) {
  if(!TvHomeRP167.supported(context))return@LaunchedEffect
  if(state.kidsMode || !TvHomeRP167.enabled(context)) {runCatching {TvHomeRP167.clear(context)};return@LaunchedEffect}
  if(!state.appForeground)return@LaunchedEffect
  delay(1800)
  val titles=favorites.take(20).mapNotNull {id->(state.myListTitles+state.continueWatching+state.movies+state.series).firstOrNull {it.cloudId==id} ?: runCatching {state.tmdb.byCloudId(id)}.getOrNull()}
  if(owner!="${state.session?.userId ?: "local"}:${state.activeProfile.id}")return@LaunchedEffect
  runCatching {TvHomeRP167.publish(context,owner,state.activeProfile.name,mapOf("continue" to continuing,"list" to titles,"recommendations" to recommended))}
 }
}
@Composable internal fun TvHomeSettingsRP167(state:AppState) {
 val context=LocalContext.current
 var enabled by remember {mutableStateOf(TvHomeRP167.enabled(context))}
 var message by remember {mutableStateOf("")}
 SettingsSectionCard {
  Text("Android TV Home",color=androidx.compose.ui.graphics.Color.White)
  if(!TvHomeRP167.supported(context)) Text(tr("Este launcher no ofrece los canales de Android TV. El launcher oficial de Fire TV requiere la integración de catálogo de Amazon.","This launcher does not expose Android TV channels. The stock Fire TV launcher requires Amazon catalog integration."),color=Muted)
  else {
   FocusButton(tr("Filas de BruniO: ","BruniO rows: ")+if(enabled) "ON" else "OFF") {enabled=!enabled;TvHomeRP167.setEnabled(context,enabled);state.launch {if(enabled) state.syncTvHomeRP167(context) else TvHomeRP167.clear(context)}}
   for((role,title) in listOf("continue" to "Continue Watching","list" to "My List","recommendations" to "Recommendations")) FocusButton(tr("Mostrar ","Show ")+title) {if(!TvHomeRP167.requestVisible(context,role)) message=tr("Actualiza las filas y habilítalas desde Personalizar canales en el Home de tu TV.","Refresh the rows and enable them under Customize channels on your TV Home.")}
   FocusButton(tr("Actualizar filas","Refresh rows")) {state.launch {state.syncTvHomeRP167(context);message=tr("Filas actualizadas. El launcher controla su visibilidad.","Rows updated. The launcher controls their visibility.")}}
  }
  if(message.isNotBlank())Text(message,color=Muted)
 }
}
internal suspend fun AppState.syncTvHomeRP167(context:android.content.Context) {
 val titles=favorites.take(20).mapNotNull {id->(myListTitles+movies+series).firstOrNull {it.cloudId==id} ?: runCatching {tmdb.byCloudId(id)}.getOrNull()}
 TvHomeRP167.publish(context,"${session?.userId ?: "local"}:${activeProfile.id}",activeProfile.name,mapOf("continue" to continueWatching.toList(),"list" to titles,"recommendations" to (forYou+trending).distinctBy {it.cloudId}))
}
