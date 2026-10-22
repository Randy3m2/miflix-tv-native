package com.miflix.native2.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.miflix.native2.data.LiveCategory
import com.miflix.native2.data.LiveChannel
import com.miflix.native2.model.PartyRole

@Composable
internal fun NautaSourceScreenRP71(state:AppState,modifier:Modifier) {
    val context=LocalContext.current
    val owner=CustomChannelsRP45.owner(state)
    val revision by CustomChannelsRP45.changesRP48.collectAsState()
    val rows=remember(owner,revision) {CustomChannelsRP45.list(context,owner)}
    var category by remember(owner) {mutableStateOf(LiveHubMemoryRP46.category(owner,"nauta-source") ?: "Todos")}
    var manage by remember(owner) {mutableStateOf<CustomChannelRP45?>(null)}
    val categories=listOf("Todos")+rows.map {it.category}.distinct().sorted()
    val visible=rows.filter {category=="Todos" || it.category==category}
    Column(modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp),contentPadding=PaddingValues(4.dp)) {
            items(categories) {name ->HubCategoryChipRP42(if(name=="Todos") tr("Todos","All") else name,name==category) {category=name;LiveHubMemoryRP46.category(owner,"nauta-source",name)}}
        }
        LazyVerticalGrid(GridCells.Adaptive(240.dp),Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(14.dp),verticalArrangement=Arrangement.spacedBy(14.dp),contentPadding=PaddingValues(6.dp,6.dp,6.dp,24.dp)) {
            items(visible,key={it.id}) {channel ->
                HubEventCardRP41(CustomChannelsRP45.labelRP50(channel),channel.logo,tr("EN VIVO · Mantén OK para opciones","LIVE · Hold OK for options"),onHoldRP48={manage=channel},logoRP50=true) {
                    if(owner==CustomChannelsRP45.owner(state) && state.partyRole!=PartyRole.GUEST && !state.kidsMode) {
                        val c=LiveCategory("tv","nauta-source:"+category,"Nauta Source")
                        LiveChannelNavigationRP40.save(state,LiveBrowseSnapshotRP40(BrowserCategoryRP34(CustomChannelsRP45.provider,c,""),visible.map {BrowserChannelRP34(CustomChannelsRP45.provider,LiveChannel(it.id,"tv",it.name,it.logo,"Nauta Source",number=it.number))}))
                        state.openLivePlayer(CustomChannelsRP45.request(channel))
                    }
                }
            }
        }
    }
    manage?.let {channel ->CustomChannelManageRP48(state,channel) {manage=null}}
}
