package com.miflix.native2.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text

@Composable
internal fun KidsHomeScreen(state: AppState) {
    LaunchedEffect(state.activeProfile.id,state.interfaceLanguage) {
        state.loadKidsHome();state.loadKidsContinue()
    }
    AppShell(state,Screen.HOME,{state.screen=it}) {
        LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(top=22.dp,bottom=40.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
            item {
                Column(Modifier.padding(horizontal=40.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Text("BruniO Kids",color=Accent,fontSize=32.sp,fontWeight=FontWeight.Black)
                    Text(tr("Historias para los más pequeños","Little viewers. Big adventures."),color=Color.White,fontSize=19.sp)
                    if(state.kidsLoading) Text(tr("Preparando Kids…","Preparing Kids…"),color=Muted,fontSize=14.sp)
                    if(state.kidsMessage.isNotBlank()) {
                        Text(state.kidsMessage,color=Muted,fontSize=14.sp)
                        FocusButton(tr("Reintentar","Retry")) {state.launch {state.loadKidsHome()} }
                    }
                }
            }
            if(state.kidsContinue.isNotEmpty()) item(key="kids_continue") {
                SpotlightRail(tr("Seguir viendo","Continue Watching"),state.kidsContinue,landscape=true,
                    progressFor={state.progress[it.cloudId]?.percent}) {state.launch {state.open(it)} }
            }
            if(state.kidsMovies.isNotEmpty()) item(key="kids_movies") {
                SpotlightRail(tr("Películas · Kids","Movies · Kids"),state.kidsMovies) {state.launch {state.open(it)} }
            }
            if(state.kidsSeries.isNotEmpty()) item(key="kids_series") {
                SpotlightRail(tr("Series · Kids","Series · Kids"),state.kidsSeries) {state.launch {state.open(it)} }
            }
            item {
                FocusButton(tr("Mi lista","My List"),modifier=Modifier.padding(horizontal=40.dp)) {state.screen=Screen.MY_LIST}
            }
        }
    }
}
