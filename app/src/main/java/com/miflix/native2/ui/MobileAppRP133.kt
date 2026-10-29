package com.miflix.native2.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import kotlinx.coroutines.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.tv.material3.Text
import com.miflix.native2.R

@Composable
internal fun MobileAppRP133(state:AppState,destination:Screen) {
    val primary=listOf(Screen.HOME,Screen.SEARCH,Screen.LIVE_TV,Screen.FRIENDS,Screen.MY_LIST,Screen.DOWNLOADS)
    val active=when(destination) {
        Screen.DETAILS->state.detailsReturnScreen.name
        Screen.WATCH_PARTY->Screen.FRIENDS.name
        Screen.PLATFORM_DETAIL,Screen.FRANCHISE,Screen.GENRE_DETAIL,Screen.COLLECTION_DETAIL,Screen.MOVIES,Screen.SERIES->Screen.HOME.name
        else->destination.name
    }
    BackHandler(destination !in listOf(Screen.HOME,Screen.PLAYER,Screen.MULTIVIEW)) {
        state.screen=if(destination==Screen.DETAILS) state.detailsReturnScreen else Screen.HOME
    }
    Column(Modifier.fillMaxSize().background(MobileBgRP133).safeDrawingPadding()) {
        if(destination !in listOf(Screen.DETAILS,Screen.PLAYER,Screen.MULTIVIEW)) Row(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            if(destination !in primary) MobileIconButtonRP133("back",tr("Volver","Back")) {state.screen=Screen.HOME}
            val latestState by rememberUpdatedState(state)
            Image(painterResource(R.drawable.brunio_icon),if(state.easyModeRP79) tr("Mantén 15 segundos para salir del modo fácil","Hold 15 seconds to exit Easy mode") else "BruniO",Modifier.size(46.dp).pointerInput(state.easyModeRP79) {
                detectTapGestures(onPress={
                    coroutineScope {
                        val timer=launch {delay(15_000);if(latestState.easyModeRP79) latestState.launch {latestState.setEasyModeRP79(false)}}
                        try {tryAwaitRelease()} finally {timer.cancel()}
                    }
                })
            })
            Text("BruniO",color=Color.White,fontSize=22.sp,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f))
            Box(Modifier.size(34.dp).clickable(enabled=!state.easyModeRP79) {state.screen=Screen.PROFILES}) {ProfileAvatar(state.activeProfile,Modifier.fillMaxSize())}
            if(!state.kidsMode && !state.easyModeRP79) MobileIconButtonRP133("bell",tr("Notificaciones","Notifications")) {state.screen=Screen.NOTIFICATIONS}
            if(!state.kidsMode && !state.easyModeRP79) MobileIconButtonRP133("gear",tr("Ajustes","Settings")) {state.screen=Screen.SETTINGS}
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when(destination) {
                Screen.HOME->MobileHomeRP133(state)
                Screen.SEARCH->MobileSearchRP133(state)
                Screen.DETAILS->MobileDetailsRP133(state)
                Screen.LIVE_TV,Screen.LIDOM->MobileLiveRP133(state,initial=if(destination==Screen.LIDOM) "LIDOM" else "TV")
                Screen.FRIENDS,Screen.WATCH_PARTY->MobileFriendsRP133(state)
                Screen.DOWNLOADS->MobileDownloadsScreenRP134(state)
                Screen.SETTINGS->MobileSettingsRP133(state)
                Screen.PROFILES->MobileProfilesRP133(state)
                Screen.NOTIFICATIONS->MobileNotificationsRP133(state)
                Screen.MY_LIST->MobileMyListRP133(state)
                Screen.PLATFORM_DETAIL->MobilePlatformRP133(state)
                Screen.FRANCHISE->MobileFranchiseRP133(state)
                Screen.GENRE_DETAIL->MobileCatalogGridRP133(state,state.genreTitle,state.genreItems) {state.launch {state.loadMoreGenre()}}
                Screen.COLLECTION_DETAIL->MobileCatalogGridRP133(state,state.collectionTitle,state.collectionItems)
                Screen.YEAR_DETAIL->MobileCatalogGridRP133(state,state.yearTitle,state.yearItems)
                Screen.DIRECTOR_DETAIL->MobileCatalogGridRP133(state,state.directorTitle,state.directorItems)
                Screen.MOVIES->MobileCatalogGridRP133(state,tr("Películas","Movies"),state.movies)
                Screen.SERIES->MobileCatalogGridRP133(state,tr("Series","Series"),state.series)
                Screen.MULTIVIEW->MobileMultiViewRP133(state)
                Screen.PLAYER->Unit
                Screen.SPLASH->BruniOSplashScreen()
                else->MobileSettingsRP133(state)
            }
        }
        if(destination !in listOf(Screen.PLAYER,Screen.MULTIVIEW)) Row(Modifier.fillMaxWidth().background(Color(0xF00C0E14)).padding(vertical=5.dp),horizontalArrangement=Arrangement.SpaceEvenly) {
            val tabs=listOf(Triple(Screen.HOME,"home",tr("Inicio","Home")),Triple(Screen.SEARCH,"search",tr("Buscar","Search")),Triple(Screen.LIVE_TV,"live",tr("En vivo","Live")),Triple(Screen.FRIENDS,"friends",tr("Amigos","Friends")),Triple(Screen.MY_LIST,"list",tr("Mi lista","My list")),Triple(Screen.DOWNLOADS,"download",tr("Descargas","Downloads")))
            tabs.filter {(!state.kidsMode || it.first !in listOf(Screen.LIVE_TV,Screen.FRIENDS,Screen.DOWNLOADS)) && (!state.easyModeRP79 || it.first!=Screen.FRIENDS)}.forEach {(screen,icon,title)->
                val selected=active==screen.name
                Column(Modifier.weight(1f).heightIn(min=52.dp).clickable {
                    if(screen==Screen.FRIENDS) state.refreshFriendsTab()
                    if(screen==Screen.LIVE_TV) state.normalLiveEntryRP85=true
                    state.screen=screen
                }.padding(vertical=6.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    MobileIconRP133(icon,tint=if(selected) MobileTintRP133 else Muted)
                    Text(title,color=if(selected) MobileTintRP133 else Muted,fontSize=10.sp,maxLines=1)
                }
            }
        }
    }
}

@Composable
private fun MobileMyListRP133(state:AppState) {
    val owner="${state.session?.userId}:${state.activeProfile.id}"
    LaunchedEffect(owner,state.favorites.toList(),state.myListLimit) {state.loadMyListPage(owner,state.favorites.toList())}
    MobileCatalogGridRP133(state,tr("Mi lista","My list"),state.myListTitles,
        more=if(state.myListLimit<state.favorites.size) ({state.myListLimit+=15}) else null)
}

@Composable
private fun MobilePlatformRP133(state:AppState) {
    androidx.compose.foundation.lazy.LazyColumn(verticalArrangement=Arrangement.spacedBy(20.dp),contentPadding=PaddingValues(bottom=20.dp)) {
        item {Text(state.platformTitle,color=Color.White,fontSize=25.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(16.dp))}
        state.platformSections.forEach {section->item(key=section.title) {MobileRailRP133(state,section.title,section.items)}}
    }
}

@Composable
private fun MobileFranchiseRP133(state:AppState) {
    Column {
        Row(Modifier.padding(horizontal=16.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            if(com.miflix.native2.data.FranchiseCatalogRP29.supportsMovies(state.franchiseBrand)) MobileButtonRP133(tr("Películas","Movies"),selected=state.franchiseType=="movie") {state.launch {state.loadFranchise("movie")}}
            MobileButtonRP133(tr("Series","Series"),selected=state.franchiseType=="series") {state.launch {state.loadFranchise("series")}}
        }
        Box(Modifier.weight(1f)) {MobileCatalogGridRP133(state,com.miflix.native2.data.FranchiseCatalogRP29.title(state.franchiseBrand),state.franchiseItems,if(state.franchiseMore) ({state.launch {state.loadFranchise(state.franchiseType,true)}}) else null)}
    }
}
