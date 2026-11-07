package com.miflix.native2.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Text
import com.miflix.native2.data.ThemeStudioRP173
import com.miflix.native2.data.ThemeDetailsRP182

/** Bounded pages avoid a giant focus group being scrolled by two reveal mechanisms. */
@Composable
internal fun ThemeStudioTvRP182(state:AppState,close:()->Unit) {
    var group by remember {mutableStateOf("all")}
    var page by remember(group) {mutableIntStateOf(0)}
    val choices=ThemeStudioRP173.presets.filter {when(group) {
        "seasonal"->it.id in ThemeDetailsRP182.seasonal
        "sports"->it.id in ThemeDetailsRP182.sports
        else->true
    }}
    val pages=(choices.size+5)/6
    val first=remember {FocusRequester()}
    LaunchedEffect(group,page) {withFrameNanos {};runCatching {first.requestFocus()}}
    Dialog(close,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Column(Modifier.fillMaxWidth(.92f).fillMaxHeight(.92f).background(Bg,RoundedCornerShape(20.dp)).padding(24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Theme Studio",color=Color.White,fontSize=26.sp,fontWeight=FontWeight.Bold)
                    Text(tr("Elige un tema. Los detalles dejan espacio a tus títulos.","Choose a theme. Decorative details leave room for your titles."),color=Muted,fontSize=13.sp)
                }
                FocusButton("×",onClick=close)
            }
            Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                listOf("all" to tr("Todos","All"),"seasonal" to tr("Temporadas","Seasonal"),"sports" to tr("Deportes","Sports")).forEach {(id,label)->
                    FocusButton(label,primary=group==id) {group=id}
                }
            }
            val active=ThemeStudioRP173.presets.firstOrNull {it.id==ThemeRuntimeRP177.id}
            Text(tr("Activo: ","Active: ")+(active?.title ?: "BruniO"),color=Accent,fontSize=15.sp)
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val tileHeight=(maxHeight-12.dp)/2
            LazyVerticalGrid(GridCells.Fixed(3),modifier=Modifier.fillMaxSize(),horizontalArrangement=Arrangement.spacedBy(12.dp),verticalArrangement=Arrangement.spacedBy(12.dp),userScrollEnabled=false) {
                itemsIndexed(choices.drop(page*6).take(6),key={_,preset->preset.id}) {index,preset->
                    var focused by remember(preset.id) {mutableStateOf(false)}
                    val selected=ThemeRuntimeRP177.id==preset.id || (preset.id=="default" && ThemeRuntimeRP177.id.isBlank())
                    val shape=RoundedCornerShape(14.dp)
                    val choose={state.saveFocusColor(preset.focus.toInt())}
                    Column(Modifier.fillMaxWidth().height(tileHeight).then(if(index==0) Modifier.focusRequester(first) else Modifier)
                        .onFocusChanged {focused=it.isFocused}.tvClick(choose).clickable(onClick=choose)
                        .background(Brush.verticalGradient(listOf(Color(preset.surface),Color(preset.background))),shape)
                        .border(if(focused) 3.dp else 1.dp,if(focused || selected) Color(preset.focus) else Color(preset.primary).copy(alpha=.6f),shape)
                        .padding(10.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                            ThemeEmblemRP182(preset.id,Modifier.size(24.dp),Color(preset.secondary))
                            Text(preset.title.substringBefore(" — "),color=Color.White,fontSize=15.sp,fontWeight=FontWeight.Bold,maxLines=2,modifier=Modifier.weight(1f))
                        }
                        Text(ThemeDetailsRP182.forId(preset.id)?.symbol.orEmpty(),color=Color(preset.secondary),fontSize=11.sp,maxLines=1)
                    }
                }
            }
            }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                FocusButton(tr("Anterior","Previous")) {page=(page-1).coerceAtLeast(0)}
                Text("${page+1} / ${pages.coerceAtLeast(1)}",color=Muted)
                FocusButton(tr("Siguiente","Next")) {page=(page+1).coerceAtMost((pages-1).coerceAtLeast(0))}
            }
        }
    }
}

@Composable
internal fun AccentColorTvRP182(state:AppState,close:()->Unit) {
    var color by remember {mutableStateOf("#B792FF")}
    Dialog(close,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Column(Modifier.widthIn(max=660.dp).background(Panel,RoundedCornerShape(20.dp)).padding(26.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
            Text(tr("Color del selector","Focus color"),color=Color.White,fontSize=24.sp)
            NativeTextField(color,{color=it.take(7)},"#RRGGBB")
            FocusButton(tr("Aplicar color","Apply color")) {
                if(color.matches(Regex("#[0-9a-fA-F]{6}"))) state.saveFocusColor(android.graphics.Color.parseColor(color))
            }
            Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                listOf(0xFFFFFFFF to "White",0xFFB792FF to "Purple",0xFF65BFFF to "Blue",0xFF66E6A0 to "Green",0xFFFFBB66 to "Amber",0xFFFF8FB6 to "Rose").chunked(3).forEach {colors ->
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        colors.forEach {(value,label)->FocusButton(label) {state.saveFocusColor(value.toInt())}}
                    }
                }
            }
            FocusButton(tr("Cerrar","Close"),onClick=close)
        }
    }
}
