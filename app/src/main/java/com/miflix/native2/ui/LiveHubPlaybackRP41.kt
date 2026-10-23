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
        if(saved!=null && saved.optString("provider")==CustomChannelsRP45.provider) {
            CustomChannelsRP45.findRP48(context,CustomChannelsRP45.owner(state),saved.optString("id"))?.let {if(openCustom(it)) return}
        }
        if(saved!=null) {
            val provider=saved.optString("provider")
            if(provider=="nauta" || provider in IptvRepository.sources) {
                val restored=try {kotlinx.coroutines.withTimeoutOrNull(3_000L) {
                    val channel=LiveChannel(saved.optString("id"),saved.optString("type","tv"),saved.optString("name"),saved.optString("poster").takeIf {it.isNotBlank()},saved.optString("description"))
                    val categories=repo.categories(state.calendarLiveManifest(provider))
                    val category=categories.firstOrNull {it.id==saved.optString("catId") && it.genre.orEmpty()==saved.optString("catGenre")} ?: categories.firstOrNull {isMostViewedRP37(it)} ?: categories.firstOrNull()
                    if(category==null) false else {
                        val rows=try {repo.channels(state.calendarLiveManifest(provider),category)} catch(e:CancellationException) {throw e} catch(_:Exception) {emptyList()}
                        startChannel(provider,channel,category,rows)
                    }
                }} catch(e:CancellationException) {throw e} catch(_:Exception) {false}
                if(restored==true) return
            }
        }
        if(!current()) return
        try {
            kotlinx.coroutines.withTimeoutOrNull(6_000L) {
                val manifest=state.calendarLiveManifest("nauta")
                val categories=repo.categories(manifest)
                val category=categories.firstOrNull {isMostViewedRP37(it)} ?: categories.firstOrNull()
                if(category!=null) {
                    val rows=repo.channels(manifest,category)
                    for(channel in rows.shuffled().take(5)) {if(!current()) return@withTimeoutOrNull;if(startChannel("nauta",channel,category,rows)) return@withTimeoutOrNull}
                }
            }
        } catch(e:CancellationException) {throw e} catch(_:Exception) { }
        if(!current()) return
        CustomChannelsRP45.list(context,CustomChannelsRP45.owner(state)).firstOrNull {it.id.startsWith("magma:rp51:")}?.let {if(openCustom(it)) return}
        if(current()) error("No playable TV channel")
    }
}
