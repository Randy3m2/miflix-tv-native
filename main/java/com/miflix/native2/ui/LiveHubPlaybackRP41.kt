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
        if(!request.live || request.liveProvider !in (setOf("nauta")+IptvRepository.sources.keys)) return
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
        suspend fun open(provider:String,channel:LiveChannel,category:LiveCategory,rows:List<LiveChannel>):Boolean {
            if(!current()) return false
            val stream=try {repo.streams(state.calendarLiveManifest(provider),channel).firstOrNull()} catch(e:CancellationException) {throw e} catch(_:Exception) {null}
                ?: return false
            if(!current()) return false
            val list=(rows+channel).distinctBy {it.type+":"+it.id}
            LiveChannelNavigationRP40.save(state,LiveBrowseSnapshotRP40(BrowserCategoryRP34(provider,category,if(provider=="nauta") "Nauta" else "IPTV"),list.map {BrowserChannelRP34(provider,it)}))
            state.openLivePlayer(PlayerRequest(MediaSummary(channel.id.hashCode(),"live",channel.name,channel.description,null,channel.poster,0.0,"LIVE"),stream,0,0,0L,live=true,liveChannelId=channel.id,liveProvider=provider,liveType=channel.type,returnScreen=Screen.LIVE_TV))
            return true
        }
        if(saved!=null) {
            val provider=saved.optString("provider")
            if(provider=="nauta" || provider in IptvRepository.sources) {
                val channel=LiveChannel(saved.optString("id"),saved.optString("type","tv"),saved.optString("name"),saved.optString("poster").takeIf {it.isNotBlank()},saved.optString("description"))
                try {
                    val categories=repo.categories(state.calendarLiveManifest(provider))
                    val category=categories.firstOrNull {it.id==saved.optString("catId") && it.genre.orEmpty()==saved.optString("catGenre")} ?: categories.firstOrNull {isMostViewedRP37(it)} ?: categories.firstOrNull()
                    if(category!=null) {
                        val rows=try {repo.channels(state.calendarLiveManifest(provider),category)} catch(e:CancellationException) {throw e} catch(_:Exception) {emptyList()}
                        if(open(provider,channel,category,rows)) return
                    }
                } catch(e:CancellationException) {throw e} catch(_:Exception) { }
            }
        }
        if(!current()) return
        val manifest=state.calendarLiveManifest("nauta")
        val categories=repo.categories(manifest)
        val category=categories.firstOrNull {isMostViewedRP37(it)} ?: categories.firstOrNull() ?: error("No TV categories")
        val rows=repo.channels(manifest,category)
        for(channel in rows.shuffled().take(5)) {if(!current()) return;if(open("nauta",channel,category,rows)) return}
        if(current()) error("No playable TV channel")
    }
}
