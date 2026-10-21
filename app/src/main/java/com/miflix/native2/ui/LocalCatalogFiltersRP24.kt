@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package com.miflix.native2.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Text
import com.miflix.native2.model.MediaSummary
import org.json.JSONObject

@Composable
internal fun LocalCatalogFiltersRP24(state:AppState,key:String,rows:List<MediaSummary>,close:()->Unit) {
    val extras=state.localExtras
    val owner=state.localExtrasScope()
    val f=extras.preference(owner,"filter:$key")
    val first=remember {FocusRequester()}
    val intro=tr("Combina tipo, año y orden para encontrar lo que quieres ver. Solo cambia esta vista, sin modificar tu lista.",
        "Combine type, year and order to find what to watch. Changes this view without changing your list.")
    var help by remember(owner,key) {mutableStateOf(intro)}
    fun save(field:String,value:Any) {
        if(state.localExtrasScope()==owner) extras.savePreference(owner,"filter:$key",JSONObject(extras.preference(owner,"filter:$key").toString()).put(field,value))
    }
    val count=localCatalogRC100(state,key,rows).size
    Dialog(onDismissRequest=close,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        BoxWithConstraints(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) {
            val wide=maxWidth>=720.dp
            val panelWidth=minOf(900.dp,(maxWidth-32.dp).coerceAtLeast(1.dp))
            val panelHeight=minOf(580.dp,(maxHeight-32.dp).coerceAtLeast(1.dp))
            Column(Modifier.consumeOpeningPress().width(panelWidth).height(panelHeight)
                .background(Color(0xFF090909),RoundedCornerShape(24.dp))
                .border(1.dp,Color(0xFF333333),RoundedCornerShape(24.dp)).padding(22.dp),
                verticalArrangement=Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)) {
                    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(3.dp)) {
                        Text(tr("TU CATÁLOGO · A TU MANERA","YOUR CATALOG · YOUR WAY"),color=Muted,fontSize=10.sp,lineHeight=13.sp,fontWeight=FontWeight.Bold)
                        Text(tr("Filtros locales","Local filters"),color=Color.White,fontSize=26.sp,lineHeight=30.sp,fontWeight=FontWeight.Bold)
                    }
                    Text("$count / ${rows.size}",color=Color.White,fontSize=14.sp,lineHeight=18.sp,
                        modifier=Modifier.background(Color(0xFF202020),RoundedCornerShape(12.dp)).padding(horizontal=12.dp,vertical=8.dp))
                    LocalFilterChoiceRP24(tr("Cerrar","Close"),false,modifier=Modifier.focusRequester(first),onFocused={help=intro},onClick=close)
                }
                Row(Modifier.weight(1f).fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(18.dp)) {
                    Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()).focusGroup(),
                        verticalArrangement=Arrangement.spacedBy(12.dp)) {
                        LocalFilterSectionRP24(tr("Tipo de contenido","Content type")) {
                            FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                                listOf("" to tr("Todos","All"),"movie" to tr("Películas","Movies"),"series" to tr("Series","Series")).forEach {(v,label) ->
                                    LocalFilterChoiceRP24(label,f.optString("type")==v,onFocused={help=tr("Elige películas, series o ambas. Se conservan los títulos que guardaste.","Choose movies, series or both. Your saved titles are kept.")}) {save("type",v)}
                                }
                            }
                        }
                        LocalFilterSectionRP24(tr("Visibilidad","Visibility")) {
                            LocalFilterChoiceRP24(tr("Ocultar vistos","Hide watched")+" · "+if(f.optBoolean("unwatched")) tr("Activado","On") else tr("Desactivado","Off"),
                                f.optBoolean("unwatched"),onFocused={help=tr("Oculta los títulos con al menos 90 % de progreso, como antes. No borra ni cambia su historial.","Hides titles with at least 90% progress, as before. Does not delete or change their history.")}) {
                                save("unwatched",!extras.preference(owner,"filter:$key").optBoolean("unwatched"))
                            }
                        }
                        LocalFilterSectionRP24(tr("Ordenar por","Sort by")) {
                            FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                                listOf("" to tr("Orden original","Original order"),"rating" to tr("Valoración","Rating"),"year" to tr("Más recientes","Newest year"),"title" to "A–Z").forEach {(v,label) ->
                                    LocalFilterChoiceRP24(label,f.optString("sort")==v,onFocused={help=tr("Reorganiza los títulos cargados por valoración, año o nombre. Orden original recupera su orden habitual.","Reorder loaded titles by rating, year or name. Original order restores their usual order.")}) {save("sort",v)}
                                }
                            }
                        }
                        LocalFilterSectionRP24(tr("Año de estreno","Release year")) {
                            FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                                LocalFilterChoiceRP24(tr("Todos los años","All years"),f.optString("year").isBlank(),onFocused={help=tr("Los años disponibles corresponden a los títulos cargados en esta vista.","Available years come from titles already loaded in this view.")}) {save("year","")}
                                rows.map {it.year}.filter {it.isNotBlank()}.distinct().sortedDescending().forEach {year ->
                                    LocalFilterChoiceRP24(year,f.optString("year")==year,onFocused={help=tr("Muestra solo títulos de ","Show only titles from ")+year+"."}) {save("year",year)}
                                }
                            }
                        }
                        if(!wide) Text(help,color=Muted,fontSize=13.sp,lineHeight=18.sp,modifier=Modifier.padding(4.dp))
                    }
                    if(wide) Column(Modifier.width(224.dp).fillMaxHeight().background(Color(0xFF121212),RoundedCornerShape(18.dp)).padding(18.dp),
                        verticalArrangement=Arrangement.spacedBy(14.dp)) {
                        Text(tr("TU VISTA","YOUR VIEW"),color=Muted,fontSize=11.sp,lineHeight=14.sp,fontWeight=FontWeight.Bold)
                        Text("$count",color=Color.White,fontSize=40.sp,lineHeight=44.sp,fontWeight=FontWeight.Bold)
                        Text(tr("títulos disponibles","titles available"),color=Muted,fontSize=13.sp,lineHeight=17.sp)
                        Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF353535)))
                        Text(help,color=Color.White,fontSize=14.sp,lineHeight=20.sp)
                        Spacer(Modifier.weight(1f))
                        Text(tr("Solo títulos cargados. Preferencias de este perfil y catálogo.","Loaded titles only. Preferences for this profile and catalog."),color=Muted,fontSize=12.sp,lineHeight=17.sp)
                    }
                }
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    LocalFilterChoiceRP24(tr("Restablecer filtros","Reset filters"),false,onFocused={help=tr("Quita todos los filtros de esta vista y recupera el orden original.","Clear this view's filters and restore its original order.")}) {
                        if(state.localExtrasScope()==owner) extras.savePreference(owner,"filter:$key",JSONObject())
                    }
                    Spacer(Modifier.weight(1f))
                    if(panelWidth>=600.dp) Text(tr("Se guardan automáticamente","Saved automatically"),color=Muted,fontSize=11.sp,lineHeight=15.sp)
                    LocalFilterChoiceRP24(tr("Listo","Done"),false,onFocused={help=intro},onClick=close)
                }
            }
        }
        LaunchedEffect(Unit) {withFrameNanos {};runCatching {first.requestFocus()}}
    }
}

@Composable
private fun LocalFilterSectionRP24(title:String,content:@Composable ColumnScope.()->Unit) {
    Column(Modifier.fillMaxWidth().background(Color(0xFF111111),RoundedCornerShape(16.dp))
        .border(1.dp,Color(0xFF282828),RoundedCornerShape(16.dp)).padding(14.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Text(title,color=Color.White,fontSize=15.sp,lineHeight=19.sp,fontWeight=FontWeight.Bold)
        content()
    }
}

@Composable
private fun LocalFilterChoiceRP24(label:String,selected:Boolean,modifier:Modifier=Modifier,onFocused:()->Unit,onClick:()->Unit) {
    var focused by remember {mutableStateOf(false)}
    val active=actionFocusVisibleRP64(focused)
    Box(modifier.onFocusChanged {focused=it.isFocused;if(it.isFocused) onFocused()}.focusable().tvClick(onClick).clickable(onClick=onClick)
        .background(if(active) Color.White else if(selected) Color(0xFF242424) else Color(0xFF171717),RoundedCornerShape(12.dp))
        .border(if(focused) 2.dp else 1.dp,if(focused) Color.White else if(selected) Color(0xFFB0B0B0) else Color(0xFF414141),RoundedCornerShape(12.dp))
        .padding(horizontal=14.dp,vertical=10.dp),contentAlignment=Alignment.Center) {
        Text((if(selected) "✓  " else "")+label,color=if(active) Color.Black else Color.White,fontSize=13.sp,lineHeight=17.sp,fontWeight=if(selected || focused) FontWeight.Bold else FontWeight.Normal)
    }
}
