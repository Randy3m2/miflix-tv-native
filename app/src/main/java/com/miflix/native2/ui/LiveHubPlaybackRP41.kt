package com.miflix.native2.ui

import android.content.Context
import com.miflix.native2.data.*
import com.miflix.native2.model.*
import kotlinx.coroutines.CancellationException
import org.json.JSONObject

internal object LiveHubPlaybackRP41 {
    private fun owner(state:AppState)="${state.session?.userId ?: "local"}:${state.activeProfile.id}"
    fun remember(context:Context,state:AppState,request:PlayerRequest) {
        if(state.playerRequest?.playbackId!=request.playbackId) return
        if(!request.live || request.liveProvider !in (setOf("nauta",CustomChannelsRP45.provider)+IptvRepository.sources.keys)) return
        val saved=JSONObject().put("provider",request.liveProvider).put("id",request.liveChannelId).put("type",request.liveType)
            .put("name",request.item.title.take(200)).put("poster",request.item.backdrop.orEmpty().take(2048)).put("description",request.item.overview.take(2048))
        LiveChannelNavigationRP40.matching(state,request)?.category?.value?.let {c ->
            saved.put("catId",c.id).put("catType",c.type).put("catGenre",c.genre.orEmpty())
        }
        context.getSharedPreferences("brunio_live_last_rp41",Context.MODE_PRIVATE).edit().putString(owner(state),saved.toString()).apply()
    }
    suspend fun startTV(context:Context,state:AppState) {
        check(!state.kidsMode && state.partyRole!=PartyRole.GUEST)
        val account=owner(state);val repo=LiveRepository()
        fun current()=owner(state)==account && state.screen==Screen.LIVE_TV && !state.kidsMode && state.partyRole!=PartyRole.GUEST
        val saved=runCatching {JSONObject(context.getSharedPreferences("brunio_live_last_rp41",Context.MODE_PRIVATE).getString(account,"").orEmpty())}.getOrNull()
        suspend fun startChannel(provider:String,channel:LiveChannel,category:LiveCategory,rows:List<LiveChannel>):Boolean {
            if(!current()) return false
            val stream=try {repo.streams(state.calendarLiveManifest(provider),channel).firstOrNull()} catch(e:CancellationException) {throw e} catch(_:Exception) {null}
                ?: return false
            if(!current()) return false
            val list=(rows+channel).distinctBy {it.type+":"+it.id}
            LiveChannelNavigationRP40.save(state,LiveBrowseSnapshotRP40(BrowserCategoryRP34(provider,category,if(provider=="nauta") "Nauta" else "IPTV"),list.map {BrowserChannelRP34(provider,it)}))
            state.openLivePlayer(PlayerRequest(MediaSummary(channel.id.hashCode(),"live",channel.name,channel.description,null,channel.poster,0.0,"LIVE"),stream,0,0,0L,live=true,liveChannelId=channel.id,liveProvider=provider,liveType=channel.type,returnScreen=Screen.LIVE_TV))
            return true
        }
        fun openCustom(channel:CustomChannelRP45):Boolean {
            if(!current()) return false
            val owner=CustomChannelsRP45.owner(state)
            val categories=CustomChannelsRP45.categoriesRP51()+CustomChannelsRP45.categoriesRP61()+M3UListsRP61.categories(context,owner)+CustomChannelsRP45.categoryRP48()+CustomChannelsRP45.categoryNautaSourceRP71()
            val category=categories.firstOrNull {it.id==saved?.optString("catId") && CustomChannelsRP45.matchesRP61(channel,it.id)}
                ?: categories.firstOrNull {CustomChannelsRP45.matchesRP61(channel,it.id)} ?: CustomChannelsRP45.categoryRP48()
            val rows=CustomChannelsRP45.list(context,owner,true).filter {CustomChannelsRP45.matchesRP61(it,category.id)}
                .map {LiveChannel(it.id,"tv",it.name,it.logo,it.category,number=it.number)}
            LiveChannelNavigationRP40.save(state,LiveBrowseSnapshotRP40(BrowserCategoryRP34(CustomChannelsRP45.provider,category,CustomChannelsRP45.sourceRP61(channel)),rows.map {BrowserChannelRP34(CustomChannelsRP45.provider,it)}))
            state.openLivePlayer(CustomChannelsRP45.request(channel))
            return true
        }
        if(!current()) return
        val rows=kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {CustomChannelsRP45.list(context,CustomChannelsRP45.owner(state))}
        if(saved!=null) {
            val previous=rows.firstOrNull {it.id==saved.optString("id")}
                ?: rows.filter {com.miflix.native2.data.FinalPlaylistRP97.nameKey(it.name)==com.miflix.native2.data.FinalPlaylistRP97.nameKey(saved.optString("name"))}.singleOrNull()
            previous?.let {if(openCustom(it)) return}
        }
        val initialGroup=com.miflix.native2.data.FinalPlaylistRP97.initialGroup(com.miflix.native2.data.FinalCatalogRP97.rows())
        (rows.firstOrNull {it.category==initialGroup} ?: rows.firstOrNull())?.let {if(openCustom(it)) return}
        if(current()) error("No playable TV channel")
    }
}
