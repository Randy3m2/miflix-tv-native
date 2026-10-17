package com.miflix.native2.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.miflix.native2.data.AwardSelections

@Composable
internal fun OscarsScreen(state:AppState) {
    var winnerOnly by remember {mutableStateOf(false)}
    LaunchedEffect(state.activeProfile.id,state.session?.userId,state.interfaceLanguage) {state.loadOscarYear(state.oscarYear)}
    BackHandler {state.screen=Screen.COLLECTIONS}
    AppShell(state,Screen.COLLECTIONS,{state.screen=it}) {
        LazyColumn(Modifier.fillMaxSize().smoothVerticalDpad(),contentPadding=PaddingValues(vertical=24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            item {Column(Modifier.padding(horizontal=40.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text("OSCARS",color=Color(0xFFFFDEA0),fontSize=34.sp,fontWeight=FontWeight.Black)
                Text(tr("Mejor Película · Ganadora y nominadas por año de ceremonia","Best Picture · Winner and nominees by ceremony year"),color=Muted,fontSize=15.sp)
                FocusButton(tr("Volver a colecciones","Back to collections")) {state.screen=Screen.COLLECTIONS}
            }}
            item {LazyRow(Modifier.fillMaxWidth().smoothHorizontalDpad(),contentPadding=PaddingValues(horizontal=40.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                items(state.oscarYears,key={it}) {year ->FocusButton(year.toString(),primary=year==state.oscarYear) {state.launch {state.loadOscarYear(year)}}}
            }}
            item {Row(Modifier.padding(horizontal=40.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                FocusButton(tr("Todas las nominadas","All nominees"),primary=!winnerOnly) {winnerOnly=false}
                FocusButton(tr("Ganadora de Mejor Película","Best Picture winner"),primary=winnerOnly) {winnerOnly=true}
            }}
            if(state.oscarYearLoading) item {Text(tr("Cargando selección…","Loading selection…"),color=Muted,modifier=Modifier.padding(horizontal=40.dp))}
            if(state.oscarYearMessage.isNotBlank()) item {Column(Modifier.padding(horizontal=40.dp)) {
                Text(state.oscarYearMessage,color=Muted)
                FocusButton(tr("Reintentar","Retry")) {state.launch {state.loadOscarYear(state.oscarYear)}}
            }}
            if(!state.oscarYearLoading && state.oscarYearPicks.isNotEmpty()) item {
                val rows=if(winnerOnly) state.oscarYearPicks.filter {AwardSelections.isWinner(it.id,it.type,state.oscarYear)} else state.oscarYearPicks
                if(rows.isEmpty()) Text(tr("La ficha de la ganadora no pudo cargar.","The winner's title could not load."),color=Muted,modifier=Modifier.padding(horizontal=40.dp))
                else SpotlightRail("Oscars ${state.oscarYear}",rows,awardLabels=true,awardYear=state.oscarYear,
                    onClick={state.launch {state.open(it)}})
            }
        }
    }
}
